package com.migros.couriertracking.repository;

import com.migros.couriertracking.domain.StoreEntry;

import java.util.List;

public interface StoreEntryRepository {

    void save(StoreEntry entry);

    List<StoreEntry> findAll();

    List<StoreEntry> findByCourier(String courier);
}
