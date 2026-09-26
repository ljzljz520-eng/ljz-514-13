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
    /** 步行/接驳速度（米/分钟），用于估算总耗时，可用环境变量覆盖 */
    private static final double WALK_SPEED_METERS_PER_MIN =
            Double.parseDouble(System.getenv().getOrDefault("WALK_SPEED_METERS_PER_MIN", "75"));
    /** 每爬升 1 米的额外耗时（分钟） */
    private static final double CLIMB_MINUTES_PER_METER =
            Double.parseDouble(System.getenv().getOrDefault("CLIMB_MINUTES_PER_METER", "0.1"));
    /** 每次换乘的额外耗时（分钟） */
    private static final double TRANSFER_PENALTY_MINUTES =
            Double.parseDouble(System.getenv().getOrDefault("TRANSFER_PENALTY_MINUTES", "5"));
    /** 少爬坡策略：1 米爬升折算的惩罚距离（米） */
    private static final double CLIMB_PENALTY_METERS =
            Double.parseDouble(System.getenv().getOrDefault("CLIMB_PENALTY_METERS", "100"));
    /** 少换乘策略：1 次换乘折算的惩罚距离（米） */
    private static final double TRANSFER_PENALTY_METERS =
            Double.parseDouble(System.getenv().getOrDefault("TRANSFER_PENALTY_METERS", "3000"));
    /** 适合拍照策略：每 1 分景色评分折算的奖励距离（米） */
    private static final double SCENIC_BONUS_METERS =
            Double.parseDouble(System.getenv().getOrDefault("SCENIC_BONUS_METERS", "300"));
    /** 适合拍照策略：权重下限系数，保证 Dijkstra 边权始终为正 */
    private static final double SCENIC_MIN_WEIGHT_FACTOR = 0.2;

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
     * 原始最短路接口：按地理距离计算，行为与之前完全一致。
     */
    public PathResult shortestPath(String fromId, String toId) {
        return shortestPath(fromId, toId, RouteStrategy.SHORTEST);
    }

    /**
     * 按策略规划路线。Dijkstra 主流程与最短路完全一致，仅边权由策略权重函数决定。
     */
    public PathResult shortestPath(String fromId, String toId, RouteStrategy strategy) {
        if (strategy == null) {
            strategy = RouteStrategy.SHORTEST;
        }
        if (fromId == null || toId == null || !nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            throw new IllegalArgumentException("起点或终点不存在");
        }
        if (fromId.equals(toId)) {
            List<String> ids = List.of(fromId);
            List<Node> ns = List.of(nodes.get(fromId));
            return new PathResult(fromId, toId, 0.0, ids, ns, List.of(),
                    strategy.getKey(), strategy.getDisplayName(), 0.0, 0.0, 0);
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
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
                double nd = cur.distance + strategyWeight(nb, strategy);
                if (nd < dist.get(nb.toId)) {
                    dist.put(nb.toId, nd);
                    prev.put(nb.toId, cur.id);
                    pq.add(new State(nb.toId, nd));
                }
            }
        }

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

        // 无论使用哪种策略，对外统计（距离/耗时/爬升/换乘）都按路径实际经过的边累计，
        // 保证 totalDistanceMeters 始终是真实地理距离。
        List<Node> pathNodes = pathIds.stream().map(nodes::get).toList();
        List<Double> segments = new ArrayList<>();
        double totalDistance = 0.0;
        double totalClimb = 0.0;
        double totalTime = 0.0;
        int totalTransfers = 0;
        for (int i = 1; i < pathNodes.size(); i++) {
            Node a = pathNodes.get(i - 1);
            Node b = pathNodes.get(i);
            Neighbor nb = neighborBetween(a.getId(), b.getId());
            double segDistance;
            double segClimb;
            int segTransfers;
            if (nb != null) {
                segDistance = nb.distanceMeters;
                segClimb = nb.climbMeters;
                segTransfers = nb.transfers;
            } else {
                segDistance = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
                segClimb = 0.0;
                segTransfers = 0;
            }
            segments.add(segDistance);
            totalDistance += segDistance;
            totalClimb += segClimb;
            totalTransfers += segTransfers;
            totalTime += edgeTimeMinutes(segDistance, segClimb, segTransfers);
        }

        return new PathResult(fromId, toId, totalDistance, pathIds, pathNodes, segments,
                strategy.getKey(), strategy.getDisplayName(), totalTime, totalClimb, totalTransfers);
    }

    /**
     * 策略权重函数：Dijkstra 只依赖这里返回的非负权重，主流程不受影响。
     */
    private static double strategyWeight(Neighbor nb, RouteStrategy strategy) {
        switch (strategy) {
            case FASTEST:
                // 省时间：直接最小化预计耗时
                return edgeTimeMinutes(nb.distanceMeters, nb.climbMeters, nb.transfers);
            case LESS_CLIMB:
                // 少爬坡：距离 + 爬升惩罚
                return nb.distanceMeters + nb.climbMeters * CLIMB_PENALTY_METERS;
            case LESS_TRANSFER:
                // 少换乘：距离 + 换乘惩罚
                return nb.distanceMeters + nb.transfers * TRANSFER_PENALTY_METERS;
            case PHOTO:
                // 适合拍照：距离 - 景色奖励，并设置下限保证权重为正
                return Math.max(nb.distanceMeters * SCENIC_MIN_WEIGHT_FACTOR,
                        nb.distanceMeters - nb.scenicScore * SCENIC_BONUS_METERS);
            case SHORTEST:
            default:
                // 原始逻辑：按地理距离
                return nb.distanceMeters;
        }
    }

    /** 单条边的预计耗时（分钟）：距离/速度 + 爬升耗时 + 换乘耗时 */
    private static double edgeTimeMinutes(double distanceMeters, double climbMeters, int transfers) {
        return distanceMeters / WALK_SPEED_METERS_PER_MIN
                + climbMeters * CLIMB_MINUTES_PER_METER
                + transfers * TRANSFER_PENALTY_MINUTES;
    }

    private Neighbor neighborBetween(String fromId, String toId) {
        for (Neighbor nb : adjacency.getOrDefault(fromId, List.of())) {
            if (nb.toId.equals(toId)) {
                return nb;
            }
        }
        return null;
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
            candidates.sort(Comparator.comparingDouble(n -> n.distanceMeters));
            int limit = Math.min(k, candidates.size());
            for (int i = 0; i < limit; i++) {
                Neighbor nb = candidates.get(i);
                String key = pairKey(a.getId(), nb.toId);
                if (undirected.add(key)) {
                    adj.get(a.getId()).add(nb);
                    adj.get(nb.toId).add(new Neighbor(a.getId(), nb.distanceMeters));
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
            double climb = e.getClimbMeters() != null ? Math.max(0.0, e.getClimbMeters()) : 0.0;
            int transfers = e.getTransfers() != null ? Math.max(0, e.getTransfers()) : 0;
            double scenic = e.getScenicScore() != null ? Math.max(0.0, e.getScenicScore()) : 0.0;

            adj.get(from).add(new Neighbor(to, w, climb, transfers, scenic));
            adj.get(to).add(new Neighbor(from, w, climb, transfers, scenic));
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

    private static final class Neighbor {
        private final String toId;
        private final double distanceMeters;
        private final double climbMeters;
        private final int transfers;
        private final double scenicScore;

        private Neighbor(String toId, double distanceMeters) {
            this(toId, distanceMeters, 0.0, 0, 0.0);
        }

        private Neighbor(String toId, double distanceMeters, double climbMeters, int transfers, double scenicScore) {
            this.toId = toId;
            this.distanceMeters = distanceMeters;
            this.climbMeters = climbMeters;
            this.transfers = transfers;
            this.scenicScore = scenicScore;
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
