package com.migros.couriertracking.repository;

import com.migros.couriertracking.domain.StoreVisit;

import java.util.Optional;

public interface StoreVisitRepository {

    Optional<StoreVisit> find(String courier, String store);

    void save(StoreVisit visit);
}
