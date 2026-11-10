package com.migros.couriertracking.api.dto;

public record DistanceResponse(String courierId, double meters, double kilometers) {

    public static DistanceResponse ofMeters(String courierId, double meters) {
        return new DistanceResponse(courierId, meters, meters / 1000.0);
    }
}
