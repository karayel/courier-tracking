package com.migros.couriertracking.api.dto;

import com.migros.couriertracking.domain.StoreEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseMappingTest {

    @Test
    void distanceIsGivenInMetersAndKilometers() {
        assertThat(DistanceResponse.ofMeters("c1", 1500.0)).isEqualTo(new DistanceResponse("c1", 1500.0, 1.5));
    }

    @Test
    void storeEntryResponseCopiesAllFields() {
        var time = Instant.parse("2026-11-09T10:00:00Z");

        assertThat(StoreEntryResponse.from(new StoreEntry("c1", "Ataşehir MMM Migros", time)))
                .isEqualTo(new StoreEntryResponse("c1", "Ataşehir MMM Migros", time));
    }
}
