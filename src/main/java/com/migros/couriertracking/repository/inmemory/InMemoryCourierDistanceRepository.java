package com.migros.couriertracking.repository.inmemory;

import com.migros.couriertracking.repository.CourierDistanceRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCourierDistanceRepository implements CourierDistanceRepository {

    // shortcut: kept in this instance's memory, lost on restart. For persistence or several instances,
    // use Redis (INCRBYFLOAT per courier) or a PostgreSQL table updated with an atomic increment.
    private final Map<String, Double> totals = new ConcurrentHashMap<>();

    @Override
    public double findTotalMeters(String courier) {
        return totals.getOrDefault(courier, 0.0);
    }

    @Override
    public void addMeters(String courier, double meters) {
        totals.merge(courier, meters, Double::sum);
    }
}
