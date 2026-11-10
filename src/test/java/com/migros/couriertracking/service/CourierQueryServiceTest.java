package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.repository.inmemory.InMemoryCourierDistanceRepository;
import com.migros.couriertracking.repository.inmemory.InMemoryStoreEntryRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CourierQueryServiceTest {

    private final InMemoryCourierDistanceRepository distances = new InMemoryCourierDistanceRepository();
    private final InMemoryStoreEntryRepository entries = new InMemoryStoreEntryRepository();
    private final CourierQueryService service = new CourierQueryService(distances, entries);

    @Test
    void totalTravelDistanceIsZeroForUnknownCourier() {
        assertThat(service.getTotalTravelDistance("nobody")).isZero();
    }

    @Test
    void totalTravelDistanceReturnsStoredMeters() {
        distances.addMeters("c1", 1234.5);

        assertThat(service.getTotalTravelDistance("c1")).isEqualTo(1234.5);
    }

    @Test
    void storeEntriesCanBeFilteredByCourier() {
        var a = new StoreEntry("c1", "s1", Instant.EPOCH);
        var b = new StoreEntry("c2", "s1", Instant.EPOCH);
        entries.save(a);
        entries.save(b);

        assertThat(service.getStoreEntries(null)).containsExactly(a, b);
        assertThat(service.getStoreEntries("c2")).containsExactly(b);
    }
}
