package com.migros.couriertracking.service.listener;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.distance.HaversineDistanceCalculator;
import com.migros.couriertracking.repository.inmemory.InMemoryCourierDistanceRepository;
import com.migros.couriertracking.service.LocationReceivedEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceTrackerListenerTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryCourierDistanceRepository distances = new InMemoryCourierDistanceRepository();
    private final DistanceTrackerListener listener =
            new DistanceTrackerListener(new HaversineDistanceCalculator(), distances);

    private static CourierLocation loc(int seconds, double lat) {
        return new CourierLocation(T0.plusSeconds(seconds), "c1", new GeoPoint(lat, 0.0));
    }

    @Test
    void firstLocationAddsNothing() {
        listener.onLocationReceived(new LocationReceivedEvent(Optional.empty(), loc(0, 0.0)));

        assertThat(distances.findTotalMeters("c1")).isZero();
    }

    @Test
    void sumsConsecutiveLegs() {
        listener.onLocationReceived(new LocationReceivedEvent(Optional.of(loc(0, 0.0)), loc(10, 1.0)));
        listener.onLocationReceived(new LocationReceivedEvent(Optional.of(loc(10, 1.0)), loc(20, 0.0)));

        assertThat(distances.findTotalMeters("c1")).isCloseTo(2 * 111_194.93, within(0.5));
    }
}
