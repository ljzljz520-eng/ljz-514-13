package com.cqu.model;

public class Edge {
    private final String fromId;
    private final String toId;
    private final Double distanceMeters;
    private final Double climbMeters;
    private final Integer transfers;
    private final Double scenicScore;

    public Edge(String fromId, String toId, Double distanceMeters) {
        this(fromId, toId, distanceMeters, null, null, null);
    }

    public Edge(String fromId, String toId, Double distanceMeters, Double climbMeters, Integer transfers, Double scenicScore) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceMeters = distanceMeters;
        this.climbMeters = climbMeters;
        this.transfers = transfers;
        this.scenicScore = scenicScore;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    /** 该路段的爬升量（米），用于“少爬坡”策略与耗时估算 */
    public Double getClimbMeters() {
        return climbMeters;
    }

    /** 该路段需要的换乘次数，用于“少换乘”策略与耗时估算 */
    public Integer getTransfers() {
        return transfers;
    }

    /** 该路段的拍照/景色评分（0-10），用于“适合拍照”策略 */
    public Double getScenicScore() {
        return scenicScore;
    }
}
