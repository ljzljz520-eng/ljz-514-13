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
        // 默认策略信息
        assertEquals(RouteStrategy.SHORTEST.getCode(), r.getStrategy());
        assertEquals(RouteStrategy.SHORTEST.getLabel(), r.getStrategyLabel());
    }

    private GraphService strategyGraph() {
        // A-B-D：近但慢、有爬坡、无换乘；A-C-D：稍远但快、平缓、需换乘、C 拍照分高
        Map<String, Node> nodes = Map.of(
                "A", new Node("A", "A", 29.5600, 106.5700, "t", "d", 0.0),
                "B", new Node("B", "B", 29.5605, 106.5705, "t", "d", 0.0),
                "C", new Node("C", "C", 29.5610, 106.5710, "t", "d", 10.0),
                "D", new Node("D", "D", 29.5615, 106.5715, "t", "d", 0.0)
        );
        List<Edge> edges = List.of(
                new Edge("A", "B", 1000.0, 20.0, 100.0, 0),
                new Edge("B", "D", 1000.0, 20.0, 100.0, 0),
                new Edge("A", "C", 1500.0, 15.0, 10.0, 1),
                new Edge("C", "D", 1500.0, 15.0, 10.0, 1)
        );
        return new GraphService(nodes, edges);
    }

    @Test
    void defaultShortestPathMatchesShortestStrategy() {
        GraphService g = strategyGraph();
        PathResult a = g.shortestPath("A", "D");
        PathResult b = g.shortestPath("A", "D", RouteStrategy.SHORTEST);
        assertEquals(List.of("A", "B", "D"), a.getPathNodeIds());
        assertEquals(2000.0, a.getTotalDistanceMeters(), 1e-9);
        assertEquals(b.getPathNodeIds(), a.getPathNodeIds());
        assertEquals(b.getTotalDistanceMeters(), a.getTotalDistanceMeters(), 1e-9);
    }

    @Test
    void fastestStrategyMinimizesTime() {
        PathResult r = strategyGraph().shortestPath("A", "D", RouteStrategy.FASTEST);
        assertEquals(List.of("A", "C", "D"), r.getPathNodeIds());
        assertEquals(30.0, r.getTotalTimeMinutes(), 1e-9);
        assertEquals("省时间", r.getStrategyLabel());
    }

    @Test
    void leastClimbStrategyAvoidsClimb() {
        PathResult r = strategyGraph().shortestPath("A", "D", RouteStrategy.LEAST_CLIMB);
        assertEquals(List.of("A", "C", "D"), r.getPathNodeIds());
        assertEquals(20.0, r.getTotalClimbMeters(), 1e-9);
    }

    @Test
    void fewestTransfersStrategyAvoidsTransfers() {
        PathResult r = strategyGraph().shortestPath("A", "D", RouteStrategy.FEWEST_TRANSFERS);
        assertEquals(List.of("A", "B", "D"), r.getPathNodeIds());
        assertEquals(0, r.getTotalTransfers());
    }

    @Test
    void photoStrategyPrefersScenicNodes() {
        PathResult r = strategyGraph().shortestPath("A", "D", RouteStrategy.PHOTO);
        assertEquals(List.of("A", "C", "D"), r.getPathNodeIds());
        assertEquals(RouteStrategy.PHOTO.getCode(), r.getStrategy());
    }

    @Test
    void fromCodeParsesStrategies() {
        assertEquals(RouteStrategy.SHORTEST, RouteStrategy.fromCode(null));
        assertEquals(RouteStrategy.SHORTEST, RouteStrategy.fromCode("  "));
        assertEquals(RouteStrategy.FASTEST, RouteStrategy.fromCode("fastest"));
        assertEquals(RouteStrategy.LEAST_CLIMB, RouteStrategy.fromCode("LEAST_CLIMB"));
        assertThrows(IllegalArgumentException.class, () -> RouteStrategy.fromCode("nope"));
    }
}
