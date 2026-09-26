package com.cqu.model;

import java.util.List;

public class PathResult {
    private String startId;
    private String endId;
    private double totalDistanceMeters;
    private List<String> pathNodeIds;
    private List<Node> pathNodes;
    private List<Double> segmentDistanceMeters;

    /** 本次规划使用的策略代码与展示名 */
    private String strategy;
    private String strategyLabel;
    /** 沿路径累计的通行时间（分钟） */
    private double totalTimeMinutes;
    /** 沿路径累计的爬升高度（米） */
    private double totalClimbMeters;
    /** 沿路径累计的换乘次数 */
    private int totalTransfers;

    public PathResult() {
    }

    public PathResult(String startId, String endId, double totalDistanceMeters, List<String> pathNodeIds, List<Node> pathNodes, List<Double> segmentDistanceMeters) {
        this.startId = startId;
        this.endId = endId;
        this.totalDistanceMeters = totalDistanceMeters;
        this.pathNodeIds = pathNodeIds;
        this.pathNodes = pathNodes;
        this.segmentDistanceMeters = segmentDistanceMeters;
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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getStrategyLabel() {
        return strategyLabel;
    }

    public void setStrategyLabel(String strategyLabel) {
        this.strategyLabel = strategyLabel;
    }

    public double getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public void setTotalTimeMinutes(double totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public double getTotalClimbMeters() {
        return totalClimbMeters;
    }

    public void setTotalClimbMeters(double totalClimbMeters) {
        this.totalClimbMeters = totalClimbMeters;
    }

    public int getTotalTransfers() {
        return totalTransfers;
    }

    public void setTotalTransfers(int totalTransfers) {
        this.totalTransfers = totalTransfers;
    }
}
