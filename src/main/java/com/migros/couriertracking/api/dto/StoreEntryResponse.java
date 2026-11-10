package com.migros.couriertracking.api.dto;

import com.migros.couriertracking.domain.StoreEntry;

import java.time.Instant;

public record StoreEntryResponse(String courier, String store, Instant time) {

    public static StoreEntryResponse from(StoreEntry entry) {
        return new StoreEntryResponse(entry.courier(), entry.store(), entry.time());
    }
}
