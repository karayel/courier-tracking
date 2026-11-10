package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.repository.StoreEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourierQueryService {

    private final CourierDistanceRepository distances;
    private final StoreEntryRepository entries;

    public CourierQueryService(CourierDistanceRepository distances, StoreEntryRepository entries) {
        this.distances = distances;
        this.entries = entries;
    }

    public Double getTotalTravelDistance(String courierId) {
        return distances.findTotalMeters(courierId);
    }

    public List<StoreEntry> getStoreEntries(String courierId) {
        return courierId == null ? entries.findAll() : entries.findByCourier(courierId);
    }
}
