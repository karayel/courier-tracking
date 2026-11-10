package com.migros.couriertracking.domain;

import java.time.Instant;

public record StoreEntry(String courier, String store, Instant time) {
}
