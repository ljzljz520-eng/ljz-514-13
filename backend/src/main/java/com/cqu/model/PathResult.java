package com.cqu.model;

import java.util.List;

public class PathResult {
    private String startId;
    private String endId;
    private double totalDistanceMeters;
    private List<String> pathNodeIds;
    private List<Node> pathNodes;
    private List<Double> segmentDistanceMeters;
    private String strategy;
    private String strategyName;
    private double totalTimeMinutes;
    private double totalClimbMeters;
    private int totalTransfers;

    public PathResult() {
    }

    public PathResult(String startId, String endId, double totalDistanceMeters, List<String> pathNodeIds, List<Node> pathNodes, List<Double> segmentDistanceMeters) {
        this(startId, endId, totalDistanceMeters, pathNodeIds, pathNodes, segmentDistanceMeters,
                RouteStrategy.SHORTEST.getKey(), RouteStrategy.SHORTEST.getDisplayName(), 0.0, 0.0, 0);
    }

    public PathResult(String startId, String endId, double totalDistanceMeters, List<String> pathNodeIds, List<Node> pathNodes, List<Double> segmentDistanceMeters,
                      String strategy, String strategyName, double totalTimeMinutes, double totalClimbMeters, int totalTransfers) {
        this.startId = startId;
        this.endId = endId;
        this.totalDistanceMeters = totalDistanceMeters;
        this.pathNodeIds = pathNodeIds;
        this.pathNodes = pathNodes;
        this.segmentDistanceMeters = segmentDistanceMeters;
        this.strategy = strategy;
        this.strategyName = strategyName;
        this.totalTimeMinutes = totalTimeMinutes;
        this.totalClimbMeters = totalClimbMeters;
        this.totalTransfers = totalTransfers;
    }

    public String getStartId() {
        return startId;
    }

    public void setStartId(String startId) {
        this.startId = startId;
    }

    public String getEndId() {
        return endId;
    }

    public void setEndId(String endId) {
        this.endId = endId;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public List<String> getPathNodeIds() {
        return pathNodeIds;
    }

    public void setPathNodeIds(List<String> pathNodeIds) {
        this.pathNodeIds = pathNodeIds;
    }

    public List<Node> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<Node> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<Double> getSegmentDistanceMeters() {
        return segmentDistanceMeters;
    }

    public void setSegmentDistanceMeters(List<Double> segmentDistanceMeters) {
        this.segmentDistanceMeters = segmentDistanceMeters;
    }

    /** 本次规划使用的策略 key（如 shortest / fastest / less-climb / less-transfer / photo） */
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /** 策略中文名，用于前端展示 */
    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    /** 按速度+爬升+换乘模型估算的总耗时（分钟） */
    public double getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public void setTotalTimeMinutes(double totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    /** 路径总爬升（米） */
    public double getTotalClimbMeters() {
        return totalClimbMeters;
    }

    public void setTotalClimbMeters(double totalClimbMeters) {
        this.totalClimbMeters = totalClimbMeters;
    }

    /** 路径总换乘次数 */
    public int getTotalTransfers() {
        return totalTransfers;
    }

    public void setTotalTransfers(int totalTransfers) {
        this.totalTransfers = totalTransfers;
    }
}
