package com.migros.couriertracking.repository.inmemory;

import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.repository.StoreEntryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Repository
public class InMemoryStoreEntryRepository implements StoreEntryRepository {

    // shortcut: kept in this instance's memory, lost on restart, grows without bound, and findByCourier
    // scans every entry. Use a PostgreSQL table indexed by (courier, time) with a retention period
    // and paginated queries; entrances are history, so a database fits better than Redis here.
    private final Queue<StoreEntry> entries = new ConcurrentLinkedQueue<>();

    @Override
    public void save(StoreEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<StoreEntry> findAll() {
        return List.copyOf(entries);
    }

    @Override
    public List<StoreEntry> findByCourier(String courier) {
        return entries.stream().filter(e -> e.courier().equals(courier)).toList();
    }
}
