package com.migros.couriertracking.repository.inmemory;

import com.migros.couriertracking.domain.StoreVisit;
import com.migros.couriertracking.repository.StoreVisitRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryStoreVisitRepository implements StoreVisitRepository {

    private record Key(String courier, String store) { }

    // shortcut: kept in this instance's memory, lost on restart. For persistence or several instances,
    // use Redis (hash per courier, field per store) or a PostgreSQL table keyed by (courier, store).
    private final Map<Key, StoreVisit> visits = new ConcurrentHashMap<>();

    @Override
    public Optional<StoreVisit> find(String courier, String store) {
        return Optional.ofNullable(visits.get(new Key(courier, store)));
    }

    @Override
    public void save(StoreVisit visit) {
        visits.put(new Key(visit.courier(), visit.store()), visit);
    }
}
