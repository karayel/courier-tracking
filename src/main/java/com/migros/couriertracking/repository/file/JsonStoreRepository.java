package com.migros.couriertracking.repository.file;

import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.Store;
import com.migros.couriertracking.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

@Repository
public class JsonStoreRepository implements StoreRepository {

    private record StoreJson(String name, Double lat, Double lng) {

        Store toDomain() {
            if (name == null || lat == null || lng == null) {
                throw new IllegalStateException("Invalid store in stores file: " + this);
            }
            return new Store(name, new GeoPoint(lat, lng));
        }
    }

    private final List<Store> stores;

    public JsonStoreRepository(ObjectMapper objectMapper, @Value("classpath:stores.json") Resource storesFile) {
        try (InputStream in = storesFile.getInputStream()) {
            List<StoreJson> raw = objectMapper.readValue(in, new TypeReference<List<StoreJson>>() { });
            this.stores = raw.stream().map(StoreJson::toDomain).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read stores file: " + storesFile, e);
        }
    }

    @Override
    public List<Store> findAll() {
        return stores;
    }
}
