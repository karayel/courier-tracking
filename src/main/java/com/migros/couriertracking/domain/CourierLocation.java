package com.migros.couriertracking.domain;

import java.time.Instant;
import java.util.Objects;

public record CourierLocation(Instant time, String courier, GeoPoint point) {

    public CourierLocation {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(courier, "courier");
        Objects.requireNonNull(point, "point");
    }
}
