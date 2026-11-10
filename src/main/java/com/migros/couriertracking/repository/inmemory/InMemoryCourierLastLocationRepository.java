package com.migros.couriertracking.repository.inmemory;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.repository.CourierLastLocationRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCourierLastLocationRepository implements CourierLastLocationRepository {

    // shortcut: kept in this instance's memory, lost on restart. For persistence or several instances,
    // use Redis (one key per courier) or PostgreSQL (one row per courier, updated only if the new time is later).
    private final Map<String, CourierLocation> lastLocations = new ConcurrentHashMap<>();

    @Override
    public Optional<CourierLocation> find(String courier) {
        return Optional.ofNullable(lastLocations.get(courier));
    }

    @Override
    public void save(CourierLocation location) {
        lastLocations.put(location.courier(), location);
    }
}
