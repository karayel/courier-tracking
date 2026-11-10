package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.Store;
import com.migros.couriertracking.domain.distance.HaversineDistanceCalculator;
import com.migros.couriertracking.repository.inmemory.InMemoryCourierDistanceRepository;
import com.migros.couriertracking.repository.inmemory.InMemoryCourierLastLocationRepository;
import com.migros.couriertracking.repository.inmemory.InMemoryStoreEntryRepository;
import com.migros.couriertracking.repository.inmemory.InMemoryStoreVisitRepository;
import com.migros.couriertracking.service.listener.DistanceTrackerListener;
import com.migros.couriertracking.service.listener.StoreProximityListener;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LocationIngestionConcurrencyTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");
    private static final Store STORE = new Store("Test Migros", new GeoPoint(41.0, 29.0));
    private static final double INSIDE_LAT = 41.0005;
    private static final double OUTSIDE_LAT = 41.002;

    private static final class SlowReadLocationRepository extends InMemoryCourierLastLocationRepository {
        @Override
        public Optional<CourierLocation> find(String courier) {
            Optional<CourierLocation> last = super.find(courier);
            LockSupport.parkNanos(100_000);
            return last;
        }
    }

    private static final class Pipeline {
        final List<LocationReceivedEvent> events = Collections.synchronizedList(new ArrayList<>());
        final InMemoryCourierDistanceRepository distances = new InMemoryCourierDistanceRepository();
        final InMemoryStoreEntryRepository entries = new InMemoryStoreEntryRepository();
        final DistanceTrackerListener distanceListener =
                new DistanceTrackerListener(new HaversineDistanceCalculator(), distances);
        final StoreProximityListener proximityListener = new StoreProximityListener(
                () -> List.of(STORE), new InMemoryStoreVisitRepository(), entries, new HaversineDistanceCalculator());
        final LocationIngestionService service = new LocationIngestionService(
                new SlowReadLocationRepository(), event -> {
                    var e = (LocationReceivedEvent) event;
                    events.add(e);
                    distanceListener.onLocationReceived(e);
                    proximityListener.onLocationReceived(e);
                });
    }

    private static CourierLocation loc(String courier, int seconds, double lat) {
        return new CourierLocation(T0.plusSeconds(seconds), courier, new GeoPoint(lat, 29.0));
    }

    private static void runConcurrently(List<CourierLocation> locations, LocationIngestionService service)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<Boolean>> tasks = locations.stream().<Callable<Boolean>>map(l -> () -> {
                start.await();
                return service.ingest(l);
            }).toList();
            List<Future<Boolean>> futures = tasks.stream().map(pool::submit).toList();
            start.countDown();
            for (Future<Boolean> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sameRecordSentConcurrentlyIsProcessedOnce() throws Exception {
        var pipeline = new Pipeline();
        var record = loc("c1", 0, INSIDE_LAT);

        runConcurrently(Collections.nCopies(64, record), pipeline.service);

        assertThat(pipeline.events).hasSize(1);
        assertThat(pipeline.entries.findAll()).hasSize(1);
        assertThat(pipeline.distances.findTotalMeters("c1")).isZero();
    }

    @Test
    void shuffledConcurrentStreamGivesTheSameResultAsASequentialReplay() throws Exception {
        var pipeline = new Pipeline();
        List<CourierLocation> stream = new ArrayList<>(IntStream.range(0, 2_000)
                .mapToObj(i -> loc("c1", i * 40, i % 3 == 0 ? OUTSIDE_LAT : INSIDE_LAT))
                .toList());
        Collections.shuffle(stream, new Random(42));

        runConcurrently(stream, pipeline.service);

        List<LocationReceivedEvent> events = List.copyOf(pipeline.events);
        assertThat(events.getFirst().previous()).isEmpty();
        for (int i = 1; i < events.size(); i++) {
            assertThat(events.get(i).previous()).contains(events.get(i - 1).current());
            assertThat(events.get(i).current().time()).isAfter(events.get(i - 1).current().time());
        }

        var replay = new Pipeline();
        events.forEach(e -> replay.service.ingest(e.current()));
        assertThat(pipeline.entries.findAll()).isEqualTo(replay.entries.findAll());
        assertThat(pipeline.distances.findTotalMeters("c1"))
                .isCloseTo(replay.distances.findTotalMeters("c1"), within(1e-6));
    }

    @Test
    void couriersProcessedInParallelDoNotInterfere() throws Exception {
        var pipeline = new Pipeline();
        int couriers = 50;
        int pointsPerCourier = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = IntStream.range(0, couriers).<Future<?>>mapToObj(c -> pool.submit(() -> {
                start.await();
                for (int p = 0; p < pointsPerCourier; p++) {
                    pipeline.service.ingest(loc("c" + c, p * 120, p % 2 == 0 ? OUTSIDE_LAT : INSIDE_LAT));
                }
                return null;
            })).toList();
            start.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        double leg = new HaversineDistanceCalculator()
                .distanceInMeters(loc("x", 0, OUTSIDE_LAT).point(), loc("x", 0, INSIDE_LAT).point());
        for (int c = 0; c < couriers; c++) {
            String courier = "c" + c;
            assertThat(pipeline.distances.findTotalMeters(courier))
                    .isCloseTo((pointsPerCourier - 1) * leg, within(1e-6));
            assertThat(pipeline.entries.findByCourier(courier)).hasSize(pointsPerCourier / 2);
        }
        assertThat(pipeline.events).hasSize(couriers * pointsPerCourier);
    }
}
