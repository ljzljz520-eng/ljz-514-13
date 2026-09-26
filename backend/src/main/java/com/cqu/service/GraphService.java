package com.cqu.service;

import com.cqu.model.Node;
import com.cqu.model.Edge;
import com.cqu.model.PathResult;
import com.cqu.model.RouteStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class GraphService {
    /** 步行速度约 4.5 km/h，缺少时间数据时按距离估算耗时 */
    private static final double WALK_METERS_PER_MINUTE = 75.0;
    /** 少爬坡策略：每米爬升等价于 40 米平路的代价 */
    private static final double CLIMB_PENALTY_PER_METER = 40.0;
    /** 少换乘策略：一次换乘等价于 10 公里的代价 */
    private static final double TRANSFER_PENALTY_METERS = 10000.0;
    /** 适合拍照策略：拍照指数每点让边权获得约 1/(1+0.2*score) 的折扣 */
    private static final double PHOTO_DISCOUNT_PER_POINT = 0.2;

    private final Map<String, Node> nodes;
    private final Map<String, List<Neighbor>> adjacency;

    public GraphService(Map<String, Node> nodes) {
        this.nodes = Map.copyOf(nodes);
        this.adjacency = buildGraph(this.nodes);
    }

    public GraphService(Map<String, Node> nodes, List<Edge> edges) {
        this.nodes = Map.copyOf(nodes);
        if (edges != null && !edges.isEmpty()) {
            this.adjacency = buildGraphFromEdges(this.nodes, edges);
        } else {
            this.adjacency = buildGraph(this.nodes);
        }
    }

    public List<Node> listNodes() {
        return nodes.values().stream().sorted(Comparator.comparing(Node::getName)).toList();
    }

    /**
     * 原有的最短路径入口，行为保持不变（等价于 SHORTEST 策略）。
     */
    public PathResult shortestPath(String fromId, String toId) {
        return shortestPath(fromId, toId, RouteStrategy.SHORTEST);
    }

    /**
     * 按策略规划路线。策略只影响边的权重取值，Dijkstra 算法本身不变。
     */
    public PathResult shortestPath(String fromId, String toId, RouteStrategy strategy) {
        RouteStrategy effective = strategy == null ? RouteStrategy.SHORTEST : strategy;
        if (fromId == null || toId == null || !nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            throw new IllegalArgumentException("起点或终点不存在");
        }
        if (fromId.equals(toId)) {
            List<String> ids = List.of(fromId);
            List<Node> ns = List.of(nodes.get(fromId));
            PathResult r = new PathResult(fromId, toId, 0.0, ids, ns, List.of());
            applyStrategyMeta(r, effective, 0.0, 0.0, 0);
            return r;
        }

        EdgeWeigher weigher = weigherFor(effective);
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        dijkstra(fromId, toId, weigher, dist, prev);

        if (!prev.containsKey(toId)) {
            throw new IllegalStateException("未找到可达路径");
        }

        List<String> pathIds = new ArrayList<>();
        String cur = toId;
        pathIds.add(cur);
        while (!cur.equals(fromId)) {
            cur = prev.get(cur);
            if (cur == null) {
                throw new IllegalStateException("未找到可达路径");
            }
            pathIds.add(cur);
        }
        java.util.Collections.reverse(pathIds);

        List<Node> pathNodes = pathIds.stream().map(nodes::get).toList();
        List<Double> segments = new ArrayList<>();
        double totalDistance = 0.0;
        double totalTime = 0.0;
        double totalClimb = 0.0;
        int totalTransfers = 0;
        for (int i = 1; i < pathNodes.size(); i++) {
            Node a = pathNodes.get(i - 1);
            Node b = pathNodes.get(i);
            Neighbor nb = findNeighbor(a.getId(), b.getId());
            double segMeters = nb != null
                    ? nb.weightMeters
                    : GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            segments.add(segMeters);
            totalDistance += segMeters;
            totalTime += nb != null ? nb.timeMinutes : segMeters / WALK_METERS_PER_MINUTE;
            totalClimb += nb != null ? nb.climbMeters : 0.0;
            totalTransfers += nb != null ? nb.transfers : 0;
        }

        PathResult r = new PathResult(fromId, toId, totalDistance, pathIds, pathNodes, segments);
        applyStrategyMeta(r, effective, totalTime, totalClimb, totalTransfers);
        return r;
    }

    /**
     * 标准 Dijkstra。与原始实现唯一区别：边权由 weigher 提供，
     * SHORTEST 策略下 weigher 返回 nb.weightMeters，逻辑与原来完全一致。
     */
    private void dijkstra(String fromId, String toId, EdgeWeigher weigher,
                          Map<String, Double> dist, Map<String, String> prev) {
        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingDouble(s -> s.distance));

        for (String id : nodes.keySet()) {
            dist.put(id, Double.POSITIVE_INFINITY);
        }
        dist.put(fromId, 0.0);
        pq.add(new State(fromId, 0.0));

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            if (cur.distance > dist.get(cur.id)) {
                continue;
            }
            if (cur.id.equals(toId)) {
                break;
            }
            List<Neighbor> neighbors = adjacency.getOrDefault(cur.id, List.of());
            for (Neighbor nb : neighbors) {
                double nd = cur.distance + weigher.weightOf(cur.id, nb);
                if (nd < dist.get(nb.toId)) {
                    dist.put(nb.toId, nd);
                    prev.put(nb.toId, cur.id);
                    pq.add(new State(nb.toId, nd));
                }
            }
        }
    }

    /**
     * 各策略的边权函数，均保证非负（Dijkstra 前提）。
     */
    private EdgeWeigher weigherFor(RouteStrategy strategy) {
        switch (strategy) {
            case FASTEST:
                return (fromId, nb) -> nb.timeMinutes;
            case LEAST_CLIMB:
                return (fromId, nb) -> nb.climbMeters * CLIMB_PENALTY_PER_METER + nb.weightMeters;
            case FEWEST_TRANSFERS:
                return (fromId, nb) -> nb.transfers * TRANSFER_PENALTY_METERS + nb.weightMeters;
            case PHOTO:
                return (fromId, nb) -> {
                    double score = (photoScoreOf(fromId) + photoScoreOf(nb.toId)) / 2.0;
                    return nb.weightMeters / (1.0 + PHOTO_DISCOUNT_PER_POINT * score);
                };
            case SHORTEST:
            default:
                return (fromId, nb) -> nb.weightMeters;
        }
    }

    private double photoScoreOf(String id) {
        Node n = nodes.get(id);
        if (n == null || n.getPhotoScore() == null) {
            return 0.0;
        }
        return Math.max(0.0, n.getPhotoScore());
    }

    private Neighbor findNeighbor(String fromId, String toId) {
        for (Neighbor nb : adjacency.getOrDefault(fromId, List.of())) {
            if (nb.toId.equals(toId)) {
                return nb;
            }
        }
        return null;
    }

    private static void applyStrategyMeta(PathResult r, RouteStrategy s, double timeMinutes, double climbMeters, int transfers) {
        r.setStrategy(s.getCode());
        r.setStrategyLabel(s.getLabel());
        r.setTotalTimeMinutes(Math.round(timeMinutes * 10.0) / 10.0);
        r.setTotalClimbMeters(Math.round(climbMeters * 10.0) / 10.0);
        r.setTotalTransfers(transfers);
    }

    private static Map<String, List<Neighbor>> buildGraph(Map<String, Node> nodes) {
        int k = Integer.parseInt(System.getenv().getOrDefault("GRAPH_K", "6"));
        double maxDist = Double.parseDouble(System.getenv().getOrDefault("GRAPH_MAX_DISTANCE_METERS", "15000"));

        Map<String, List<Neighbor>> adj = new HashMap<>();
        for (String id : nodes.keySet()) {
            adj.put(id, new ArrayList<>());
        }

        Set<String> undirected = new HashSet<>();
        List<Node> all = new ArrayList<>(nodes.values());

        for (Node a : all) {
            List<Neighbor> candidates = new ArrayList<>();
            for (Node b : all) {
                if (a.getId().equals(b.getId())) {
                    continue;
                }
                double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
                if (d <= maxDist) {
                    candidates.add(new Neighbor(b.getId(), d));
                }
            }
            candidates.sort(Comparator.comparingDouble(n -> n.weightMeters));
            int limit = Math.min(k, candidates.size());
            for (int i = 0; i < limit; i++) {
                Neighbor nb = candidates.get(i);
                String key = pairKey(a.getId(), nb.toId);
                if (undirected.add(key)) {
                    adj.get(a.getId()).add(nb);
                    adj.get(nb.toId).add(new Neighbor(a.getId(), nb.weightMeters));
                }
            }
        }

        ensureConnectivity(adj, nodes);
        return adj;
    }

    private static Map<String, List<Neighbor>> buildGraphFromEdges(Map<String, Node> nodes, List<Edge> edges) {
        Map<String, List<Neighbor>> adj = new HashMap<>();
        for (String id : nodes.keySet()) {
            adj.put(id, new ArrayList<>());
        }

        for (Edge e : edges) {
            if (e == null) {
                continue;
            }
            String from = e.getFromId();
            String to = e.getToId();
            if (from == null || to == null || !nodes.containsKey(from) || !nodes.containsKey(to) || from.equals(to)) {
                continue;
            }

            Node a = nodes.get(from);
            Node b = nodes.get(to);
            double w = e.getDistanceMeters() != null
                    ? e.getDistanceMeters()
                    : GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            double time = e.getTimeMinutes() != null ? e.getTimeMinutes() : w / WALK_METERS_PER_MINUTE;
            double climb = e.getClimbMeters() != null ? e.getClimbMeters() : 0.0;
            int transfers = e.getTransfers() != null ? e.getTransfers() : 0;

            adj.get(from).add(new Neighbor(to, w, time, climb, transfers));
            adj.get(to).add(new Neighbor(from, w, time, climb, transfers));
        }

        boolean ensure = Boolean.parseBoolean(System.getenv().getOrDefault("GRAPH_ENSURE_CONNECTIVITY", "true"));
        if (ensure) {
            ensureConnectivity(adj, nodes);
        }
        return adj;
    }

    private static void ensureConnectivity(Map<String, List<Neighbor>> adj, Map<String, Node> nodes) {
        if (nodes.isEmpty()) {
            return;
        }
        Set<String> visited = new HashSet<>();
        String start = nodes.keySet().iterator().next();
        dfs(start, adj, visited);
        if (visited.size() == nodes.size()) {
            return;
        }

        List<String> remaining = nodes.keySet().stream().filter(id -> !visited.contains(id)).toList();
        Set<String> allVisited = new HashSet<>(visited);
        for (String id : remaining) {
            String connectTo = nearestInSet(id, allVisited, nodes);
            Node a = nodes.get(id);
            Node b = nodes.get(connectTo);
            double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            adj.get(id).add(new Neighbor(connectTo, d));
            adj.get(connectTo).add(new Neighbor(id, d));
            dfs(id, adj, allVisited);
        }
    }

    private static String nearestInSet(String fromId, Set<String> set, Map<String, Node> nodes) {
        Node a = nodes.get(fromId);
        String bestId = null;
        double best = Double.POSITIVE_INFINITY;
        for (String candidate : set) {
            Node b = nodes.get(candidate);
            double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            if (d < best) {
                best = d;
                bestId = candidate;
            }
        }
        if (bestId == null) {
            throw new IllegalStateException("无法确保连通性");
        }
        return bestId;
    }

    private static void dfs(String id, Map<String, List<Neighbor>> adj, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        for (Neighbor nb : adj.getOrDefault(id, List.of())) {
            dfs(nb.toId, adj, visited);
        }
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }

    /** 策略权重函数：给定当前节点与出边，返回该边在本策略下的代价（非负）。 */
    private interface EdgeWeigher {
        double weightOf(String fromId, Neighbor edge);
    }

    private static final class Neighbor {
        private final String toId;
        private final double weightMeters;
        private final double timeMinutes;
        private final double climbMeters;
        private final int transfers;

        private Neighbor(String toId, double weightMeters) {
            this(toId, weightMeters, weightMeters / WALK_METERS_PER_MINUTE, 0.0, 0);
        }

        private Neighbor(String toId, double weightMeters, double timeMinutes, double climbMeters, int transfers) {
            this.toId = toId;
            this.weightMeters = weightMeters;
            this.timeMinutes = timeMinutes;
            this.climbMeters = climbMeters;
            this.transfers = transfers;
        }
    }

    private static final class State {
        private final String id;
        private final double distance;

        private State(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }
}
