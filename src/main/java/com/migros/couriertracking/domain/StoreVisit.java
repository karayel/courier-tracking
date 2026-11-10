package com.migros.couriertracking.domain;

import java.time.Instant;
import java.util.Objects;

public record StoreVisit(String courier, String store, boolean inside, Instant lastExit) {

    public static final Instant NEVER_EXITED = Instant.MIN;

    public StoreVisit {
        Objects.requireNonNull(courier, "courier");
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(lastExit, "lastExit");
    }

    public static StoreVisit notVisited(String courier, String store) {
        return new StoreVisit(courier, store, false, NEVER_EXITED);
    }
}
