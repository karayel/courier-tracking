package com.migros.couriertracking.repository;

import com.migros.couriertracking.domain.Store;

import java.util.List;

public interface StoreRepository {

    List<Store> findAll();
}
