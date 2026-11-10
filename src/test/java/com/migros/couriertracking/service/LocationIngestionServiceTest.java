package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.repository.inmemory.InMemoryCourierLastLocationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LocationIngestionServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    private final List<Object> published = new ArrayList<>();
    private final InMemoryCourierLastLocationRepository locations = new InMemoryCourierLastLocationRepository();
    private final LocationIngestionService service = new LocationIngestionService(locations, published::add);

    private static CourierLocation loc(String courier, int seconds) {
        return new CourierLocation(T0.plusSeconds(seconds), courier, new GeoPoint(41.0, 29.0));
    }

    @Test
    void futureStampedRecordMakesEarlierRecordsCountAsOutOfOrder() {
        service.ingest(loc("c1", 3600));

        assertThat(service.ingest(loc("c1", 60))).isFalse();
        assertThat(service.ingest(loc("c1", 3601))).isTrue();
    }

    @Test
    void firstLocationIsPublishedWithoutPrevious() {
        assertThat(service.ingest(loc("c1", 0))).isTrue();

        assertThat(published).containsExactly(new LocationReceivedEvent(Optional.empty(), loc("c1", 0)));
        assertThat(locations.find("c1")).contains(loc("c1", 0));
    }

    @Test
    void eventRejectsNullInsteadOfEmptyPrevious() {
        assertThatNullPointerException().isThrownBy(() -> new LocationReceivedEvent(null, loc("c1", 0)));
        assertThatNullPointerException().isThrownBy(() -> new LocationReceivedEvent(Optional.empty(), null));
    }

    @Test
    void nextLocationCarriesThePreviousOne() {
        service.ingest(loc("c1", 0));
        service.ingest(loc("c1", 10));

        assertThat(published).last().isEqualTo(new LocationReceivedEvent(Optional.of(loc("c1", 0)), loc("c1", 10)));
    }

    @Test
    void olderOrSameTimeLocationIsIgnored() {
        service.ingest(loc("c1", 10));

        assertThat(service.ingest(loc("c1", 5))).isFalse();
        assertThat(service.ingest(loc("c1", 10))).isFalse();
        assertThat(published).hasSize(1);
        assertThat(locations.find("c1")).contains(loc("c1", 10));
    }

    @Test
    void timeOrderIsCheckedPerCourier() {
        service.ingest(loc("c1", 10));

        assertThat(service.ingest(loc("c2", 5))).isTrue();
    }
}
