package com.migros.couriertracking.repository.inmemory;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.domain.StoreVisit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRepositoriesTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    @Nested
    class CourierLocations {
        private final InMemoryCourierLastLocationRepository repository = new InMemoryCourierLastLocationRepository();

        @Test
        void unknownCourierHasNoLocation() {
            assertThat(repository.find("c1")).isEmpty();
        }

        @Test
        void keepsOnlyTheLastLocationPerCourier() {
            repository.save(new CourierLocation(T0, "c1", new GeoPoint(1.0, 1.0)));
            repository.save(new CourierLocation(T0.plusSeconds(1), "c1", new GeoPoint(2.0, 2.0)));
            repository.save(new CourierLocation(T0, "c2", new GeoPoint(3.0, 3.0)));

            assertThat(repository.find("c1")).contains(new CourierLocation(T0.plusSeconds(1), "c1", new GeoPoint(2.0, 2.0)));
            assertThat(repository.find("c2")).contains(new CourierLocation(T0, "c2", new GeoPoint(3.0, 3.0)));
        }
    }

    @Nested
    class CourierDistances {
        private final InMemoryCourierDistanceRepository repository = new InMemoryCourierDistanceRepository();

        @Test
        void unknownCourierIsZero() {
            assertThat(repository.findTotalMeters("c1")).isZero();
        }

        @Test
        void addsUpPerCourier() {
            repository.addMeters("c1", 10.5);
            repository.addMeters("c1", 4.5);
            repository.addMeters("c2", 1.0);

            assertThat(repository.findTotalMeters("c1")).isEqualTo(15.0);
            assertThat(repository.findTotalMeters("c2")).isEqualTo(1.0);
        }

        @Test
        void concurrentAddsAreNotLost() {
            IntStream.range(0, 10_000).parallel().forEach(i -> repository.addMeters("c1", 1.0));

            assertThat(repository.findTotalMeters("c1")).isEqualTo(10_000.0);
        }
    }

    @Nested
    class StoreVisits {
        private final InMemoryStoreVisitRepository repository = new InMemoryStoreVisitRepository();

        @Test
        void isKeyedByCourierAndStore() {
            var visit = new StoreVisit("c1", "s1", true, StoreVisit.NEVER_EXITED);
            repository.save(visit);

            assertThat(repository.find("c1", "s1")).contains(visit);
            assertThat(repository.find("c1", "s2")).isEmpty();
            assertThat(repository.find("c2", "s1")).isEmpty();
        }

        @Test
        void saveReplacesPreviousState() {
            repository.save(new StoreVisit("c1", "s1", true, StoreVisit.NEVER_EXITED));
            repository.save(new StoreVisit("c1", "s1", false, T0));

            assertThat(repository.find("c1", "s1")).contains(new StoreVisit("c1", "s1", false, T0));
        }
    }

    @Nested
    class StoreEntries {
        private final InMemoryStoreEntryRepository repository = new InMemoryStoreEntryRepository();

        @Test
        void findsAllAndByCourierInInsertionOrder() {
            var first = new StoreEntry("c1", "s1", T0);
            var second = new StoreEntry("c2", "s1", T0.plusSeconds(5));
            var third = new StoreEntry("c1", "s2", T0.plusSeconds(10));
            repository.save(first);
            repository.save(second);
            repository.save(third);

            assertThat(repository.findAll()).containsExactly(first, second, third);
            assertThat(repository.findByCourier("c1")).containsExactly(first, third);
            assertThat(repository.findByCourier("unknown")).isEmpty();
        }
    }
}
