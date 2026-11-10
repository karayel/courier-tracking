package com.migros.couriertracking.service.listener;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.Store;
import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.domain.distance.DistanceCalculator;
import com.migros.couriertracking.domain.distance.HaversineDistanceCalculator;
import com.migros.couriertracking.repository.inmemory.InMemoryStoreEntryRepository;
import com.migros.couriertracking.repository.inmemory.InMemoryStoreVisitRepository;
import com.migros.couriertracking.service.LocationReceivedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoreProximityListenerTest {

    private static final Store STORE = new Store("Test Migros", new GeoPoint(41.0, 29.0));
    private static final Store OTHER_STORE = new Store("Far Migros", new GeoPoint(41.1, 29.1));
    private static final double INSIDE_LAT = 41.0005;
    private static final double OUTSIDE_LAT = 41.002;
    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryStoreEntryRepository entries = new InMemoryStoreEntryRepository();
    private final StoreProximityListener listener = new StoreProximityListener(
            () -> List.of(STORE, OTHER_STORE), new InMemoryStoreVisitRepository(), entries,
            new HaversineDistanceCalculator());

    private void at(int secondsFromT0, double lat) {
        listener.onLocationReceived(new LocationReceivedEvent(Optional.empty(),
                new CourierLocation(T0.plusSeconds(secondsFromT0), "c1", new GeoPoint(lat, 29.0))));
    }

    @Test
    void outsideOnlyProducesNoEntry() {
        at(0, OUTSIDE_LAT);
        at(10, OUTSIDE_LAT);

        assertThat(entries.findAll()).isEmpty();
    }

    @Test
    void movingFromOutsideToInsideIsAnEntry() {
        at(0, OUTSIDE_LAT);
        at(10, INSIDE_LAT);

        assertThat(entries.findAll()).containsExactly(new StoreEntry("c1", "Test Migros", T0.plusSeconds(10)));
    }

    @Test
    void firstLocationAlreadyInsideIsAnEntry() {
        at(0, INSIDE_LAT);

        assertThat(entries.findAll()).hasSize(1);
    }

    @Test
    void stayingInsideIsASingleEntry() {
        at(0, INSIDE_LAT);
        at(60, INSIDE_LAT);
        at(600, INSIDE_LAT);

        assertThat(entries.findAll()).hasSize(1);
    }

    @Test
    void reEntryWithinOneMinuteOfExitIsNotCounted() {
        at(0, INSIDE_LAT);
        at(300, OUTSIDE_LAT);
        at(330, INSIDE_LAT);

        assertThat(entries.findAll()).hasSize(1);
    }

    @Test
    void reEntryExactlyOneMinuteAfterExitIsCounted() {
        at(0, INSIDE_LAT);
        at(300, OUTSIDE_LAT);
        at(360, INSIDE_LAT);

        assertThat(entries.findAll()).extracting(StoreEntry::time).containsExactly(T0, T0.plusSeconds(360));
    }

    @Test
    void cooldownCountsFromLastExitNotFromEntry() {
        at(0, INSIDE_LAT);
        at(300, OUTSIDE_LAT);
        at(330, INSIDE_LAT);
        at(420, OUTSIDE_LAT);
        at(540, INSIDE_LAT);

        assertThat(entries.findAll()).extracting(StoreEntry::time).containsExactly(T0, T0.plusSeconds(540));
    }

    @Test
    void couriersAreTrackedIndependently() {
        at(0, INSIDE_LAT);
        listener.onLocationReceived(new LocationReceivedEvent(Optional.empty(),
                new CourierLocation(T0.plusSeconds(5), "c2", new GeoPoint(INSIDE_LAT, 29.0))));

        assertThat(entries.findAll()).extracting(StoreEntry::courier).containsExactly("c1", "c2");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void countedEntryIsWrittenToTheApplicationLog(CapturedOutput output) {
        at(0, INSIDE_LAT);

        assertThat(output).contains("Courier c1 entered Test Migros at 2026-11-09T10:00:00Z");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void ignoredReEntryIsNotLogged(CapturedOutput output) {
        at(0, INSIDE_LAT);
        at(300, OUTSIDE_LAT);
        at(330, INSIDE_LAT);

        assertThat(output).doesNotContain("at 2026-11-09T10:05:30Z");
    }

    @Nested
    class RadiusBoundary {

        private StoreProximityListener listenerAtDistance(double meters) {
            DistanceCalculator fixed = (from, to) -> meters;
            return new StoreProximityListener(() -> List.of(STORE), new InMemoryStoreVisitRepository(), entries, fixed);
        }

        private void sendTo(StoreProximityListener l) {
            l.onLocationReceived(new LocationReceivedEvent(Optional.empty(), new CourierLocation(T0, "c1", new GeoPoint(41.0, 29.0))));
        }

        @Test
        void exactly100MetersIsInside() {
            sendTo(listenerAtDistance(100.0));

            assertThat(entries.findAll()).hasSize(1);
        }

        @Test
        void justOver100MetersIsOutside() {
            sendTo(listenerAtDistance(100.001));

            assertThat(entries.findAll()).isEmpty();
        }
    }

    @Nested
    class MultipleStores {

        private static final Store A = new Store("A Migros", new GeoPoint(41.0, 29.0));
        private static final Store B = new Store("B Migros", new GeoPoint(41.0, 29.003));
        private static final Store C = new Store("C Migros", new GeoPoint(41.0, 29.0015));
        private static final double AT_A = 29.0;
        private static final double AT_B = 29.003;
        private static final double BETWEEN_A_AND_C = 29.00075;
        private static final double AWAY = 29.01;

        private final StoreProximityListener multi = new StoreProximityListener(
                () -> List.of(A, B, C), new InMemoryStoreVisitRepository(), entries, new HaversineDistanceCalculator());

        private void atLng(int secondsFromT0, double lng) {
            multi.onLocationReceived(new LocationReceivedEvent(Optional.empty(),
                    new CourierLocation(T0.plusSeconds(secondsFromT0), "c1", new GeoPoint(41.0, lng))));
        }

        private List<String> enteredStores() {
            return entries.findAll().stream().map(StoreEntry::store).toList();
        }

        @Test
        void enteringAnotherStoreRightAfterLeavingOneIsCounted() {
            atLng(0, AT_A);
            atLng(10, AWAY);
            atLng(20, AT_B);

            assertThat(enteredStores()).containsExactly("A Migros", "B Migros");
        }

        @Test
        void cooldownIsTrackedPerStore() {
            atLng(0, AT_A);
            atLng(10, AWAY);
            atLng(20, AT_B);
            atLng(30, AWAY);
            atLng(40, AT_A);
            atLng(100, AWAY);
            atLng(130, AT_B);

            assertThat(enteredStores()).containsExactly("A Migros", "B Migros", "B Migros");
        }

        @Test
        void beingInsideOverlappingRadiiCountsEachStore() {
            atLng(0, BETWEEN_A_AND_C);

            assertThat(enteredStores()).containsExactlyInAnyOrder("A Migros", "C Migros");
        }
    }
}
