package com.migros.couriertracking.service.listener;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.Store;
import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.domain.StoreVisit;
import com.migros.couriertracking.domain.distance.DistanceCalculator;
import com.migros.couriertracking.repository.StoreEntryRepository;
import com.migros.couriertracking.repository.StoreRepository;
import com.migros.couriertracking.repository.StoreVisitRepository;
import com.migros.couriertracking.service.LocationReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class StoreProximityListener {

    static final double RADIUS_METERS = 100.0;
    static final Duration REENTRY_COOLDOWN = Duration.ofMinutes(1);

    private static final Logger log = LoggerFactory.getLogger(StoreProximityListener.class);

    private final StoreRepository stores;
    private final StoreVisitRepository visits;
    private final StoreEntryRepository entries;
    private final DistanceCalculator calculator;

    public StoreProximityListener(StoreRepository stores, StoreVisitRepository visits,
                                  StoreEntryRepository entries, DistanceCalculator calculator) {
        this.stores = stores;
        this.visits = visits;
        this.entries = entries;
        this.calculator = calculator;
    }

    @EventListener
    public void onLocationReceived(LocationReceivedEvent event) {
        CourierLocation location = event.current();
        // shortcut: every record is checked against every store; fine for 5 stores. For thousands, look up
        // only nearby stores with a spatial index (geohash buckets in memory, or PostGIS ST_DWithin).
        for (Store store : stores.findAll()) {
            boolean inside = calculator.distanceInMeters(location.point(), store.point()) <= RADIUS_METERS;
            StoreVisit visit = visits.find(location.courier(), store.name())
                    .orElseGet(() -> StoreVisit.notVisited(location.courier(), store.name()));
            if (inside == visit.inside()) {
                continue;
            }
            if (inside) {
                if (Duration.between(visit.lastExit(), location.time()).compareTo(REENTRY_COOLDOWN) >= 0) {
                    entries.save(new StoreEntry(location.courier(), store.name(), location.time()));
                    log.info("Courier {} entered {} at {}", location.courier(), store.name(), location.time());
                }
                visits.save(new StoreVisit(location.courier(), store.name(), true, visit.lastExit()));
            } else {
                visits.save(new StoreVisit(location.courier(), store.name(), false, location.time()));
            }
        }
    }
}
