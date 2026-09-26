package com.cqu.model;

public class Edge {
    private final String fromId;
    private final String toId;
    private final Double distanceMeters;
    private final Double timeMinutes;
    private final Double climbMeters;
    private final Integer transfers;

    public Edge(String fromId, String toId, Double distanceMeters) {
        this(fromId, toId, distanceMeters, null, null, null);
    }

    public Edge(String fromId, String toId, Double distanceMeters, Double timeMinutes, Double climbMeters, Integer transfers) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceMeters = distanceMeters;
        this.timeMinutes = timeMinutes;
        this.climbMeters = climbMeters;
        this.transfers = transfers;
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

    public Double getTimeMinutes() {
        return timeMinutes;
    }

    public Double getClimbMeters() {
        return climbMeters;
    }

    public Integer getTransfers() {
        return transfers;
    }
}
