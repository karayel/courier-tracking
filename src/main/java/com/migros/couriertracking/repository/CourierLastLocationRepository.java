package com.migros.couriertracking.repository;

import com.migros.couriertracking.domain.CourierLocation;

import java.util.Optional;

public interface CourierLastLocationRepository {

    Optional<CourierLocation> find(String courier);

    void save(CourierLocation location);
}
