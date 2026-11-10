package com.migros.couriertracking.repository;

public interface CourierDistanceRepository {

    double findTotalMeters(String courier);

    void addMeters(String courier, double meters);
}
