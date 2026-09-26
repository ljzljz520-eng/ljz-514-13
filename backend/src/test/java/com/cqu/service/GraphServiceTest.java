package com.cqu.service;

import com.cqu.model.Edge;
import com.cqu.model.Node;
import com.cqu.model.PathResult;
import com.cqu.model.RouteStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphServiceTest {
    @Test
    void shortestPathReturnsValidResult() {
        Map<String, Node> nodes = Map.of(
                "A", new Node("A", "A", 29.56301, 106.57577, "t", "d"),
                "B", new Node("B", "B", 29.56470, 106.58169, "t", "d"),
                "C", new Node("C", "C", 29.56336, 106.58713, "t", "d")
        );
        GraphService g = new GraphService(nodes);
        PathResult r = g.shortestPath("A", "C");
        assertNotNull(r);
        assertEquals("A", r.getStartId());
        assertEquals("C", r.getEndId());
        assertFalse(r.getPathNodeIds().isEmpty());
        assertEquals(r.getPathNodeIds().size(), r.getPathNodes().size());
    }

    /**
     * 构造受控图：A->B 直达距离短但爬升高、换乘多、景色差；
     * A->C->B 略远但平坦、无换乘、景色好。
     */
    private static GraphService controlledGraph() {
        Map<String, Node> nodes = Map.of(
                "A", new Node("A", "A", 29.5600, 106.5700, "t", "d"),
                "B", new Node("B", "B", 29.5600, 106.5800, "t", "d"),
                "C", new Node("C", "C", 29.5650, 106.5750, "t", "d")
        );
        List<Edge> edges = List.of(
                new Edge("A", "B", 1000.0, 200.0, 2, 1.0),
                new Edge("A", "C", 700.0, 0.0, 0, 9.0),
                new Edge("C", "B", 700.0, 0.0, 0, 9.0)
        );
        return new GraphService(nodes, edges);
    }

    @Test
    void defaultStrategyKeepsOriginalShortestByDistance() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "B");
        // 原始逻辑：按地理距离，直达 1000m 优于绕行 1400m
        assertEquals(List.of("A", "B"), r.getPathNodeIds());
        assertEquals(1000.0, r.getTotalDistanceMeters(), 1e-6);
        assertEquals("shortest", r.getStrategy());
        assertEquals(RouteStrategy.SHORTEST.getDisplayName(), r.getStrategyName());
    }

    @Test
    void explicitShortestStrategyMatchesDefaultBehavior() {
        GraphService g = controlledGraph();
        PathResult def = g.shortestPath("A", "B");
        PathResult exp = g.shortestPath("A", "B", RouteStrategy.SHORTEST);
        assertEquals(def.getPathNodeIds(), exp.getPathNodeIds());
        assertEquals(def.getTotalDistanceMeters(), exp.getTotalDistanceMeters(), 1e-9);
    }

    @Test
    void lessClimbAvoidsSteepEdge() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "B", RouteStrategy.LESS_CLIMB);
        assertEquals(List.of("A", "C", "B"), r.getPathNodeIds());
        assertEquals(0.0, r.getTotalClimbMeters(), 1e-6);
        assertEquals("少爬坡", r.getStrategyName());
    }

    @Test
    void lessTransferAvoidsTransferHeavyEdge() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "B", RouteStrategy.LESS_TRANSFER);
        assertEquals(List.of("A", "C", "B"), r.getPathNodeIds());
        assertEquals(0, r.getTotalTransfers());
        assertEquals("少换乘", r.getStrategyName());
    }

    @Test
    void photoPrefersScenicRoute() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "B", RouteStrategy.PHOTO);
        assertEquals(List.of("A", "C", "B"), r.getPathNodeIds());
        assertEquals("适合拍照", r.getStrategyName());
    }

    @Test
    void fastestMinimizesEstimatedTime() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "B", RouteStrategy.FASTEST);
        // 直达虽近，但爬升+换乘耗时惩罚大，省时间应绕行
        assertEquals(List.of("A", "C", "B"), r.getPathNodeIds());
        assertEquals("省时间", r.getStrategyName());
        assertTrue(r.getTotalTimeMinutes() > 0);
    }

    @Test
    void totalDistanceStaysGeographicForAllStrategies() {
        GraphService g = controlledGraph();
        for (RouteStrategy s : RouteStrategy.values()) {
            PathResult r = g.shortestPath("A", "B", s);
            double segSum = r.getSegmentDistanceMeters().stream().mapToDouble(Double::doubleValue).sum();
            assertEquals(segSum, r.getTotalDistanceMeters(), 1e-6, "strategy=" + s);
            assertEquals(s.getKey(), r.getStrategy());
        }
    }

    @Test
    void sameStartAndEndReturnsZeroResultWithStrategy() {
        GraphService g = controlledGraph();
        PathResult r = g.shortestPath("A", "A", RouteStrategy.PHOTO);
        assertEquals(List.of("A"), r.getPathNodeIds());
        assertEquals(0.0, r.getTotalDistanceMeters(), 1e-9);
        assertEquals(0.0, r.getTotalTimeMinutes(), 1e-9);
        assertEquals("photo", r.getStrategy());
    }

    @Test
    void unknownNodeStillThrows() {
        GraphService g = controlledGraph();
        assertThrows(IllegalArgumentException.class, () -> g.shortestPath("NOPE", "B", RouteStrategy.FASTEST));
    }

    @Test
    void strategyFromKeyParsing() {
        assertEquals(RouteStrategy.SHORTEST, RouteStrategy.fromKey(null));
        assertEquals(RouteStrategy.SHORTEST, RouteStrategy.fromKey("  "));
        assertEquals(RouteStrategy.PHOTO, RouteStrategy.fromKey("photo"));
        assertEquals(RouteStrategy.LESS_CLIMB, RouteStrategy.fromKey("less-climb"));
        assertEquals(RouteStrategy.LESS_CLIMB, RouteStrategy.fromKey("LESS_CLIMB"));
        assertThrows(IllegalArgumentException.class, () -> RouteStrategy.fromKey("nope"));
    }
}
