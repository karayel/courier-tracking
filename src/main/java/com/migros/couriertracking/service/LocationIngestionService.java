package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.repository.CourierLastLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LocationIngestionService.class);

    private final CourierLastLocationRepository lastLocations;
    private final ApplicationEventPublisher events;
    // shortcut: one lock per courier, local to this JVM. It keeps each courier's records in order
    // (read last location -> check time -> save -> publish) only while a single instance runs.
    // With several instances, pick one of:
    //  - Kafka with courierId as the partition key: one consumer handles a courier's records in order (preferred).
    //  - A shared database with a conditional update (UPDATE ... WHERE time < :newTime) or optimistic locking.
    //  - A distributed lock per courier (e.g. Redis/Redisson): simplest to add, slowest under load.
    // The map keeps one small object per courier seen; evict idle couriers if their number grows without bound.
    private final Map<String, Object> courierLocks = new ConcurrentHashMap<>();

    public LocationIngestionService(CourierLastLocationRepository lastLocations, ApplicationEventPublisher events) {
        this.lastLocations = lastLocations;
        this.events = events;
    }

    public boolean ingest(CourierLocation location) {
        synchronized (courierLocks.computeIfAbsent(location.courier(), c -> new Object())) {
            Optional<CourierLocation> previous = lastLocations.find(location.courier());
            if (previous.isPresent() && !location.time().isAfter(previous.get().time())) {
                log.debug("Ignoring out-of-order location for courier {} at {}", location.courier(), location.time());
                return false;
            }
            lastLocations.save(location);
            // shortcut: under heavy load, write records to Kafka and process them asynchronously.
            events.publishEvent(new LocationReceivedEvent(previous, location));
            return true;
        }
    }
}
