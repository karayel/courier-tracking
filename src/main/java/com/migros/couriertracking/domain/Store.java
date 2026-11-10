package com.migros.couriertracking.domain;

import java.util.Objects;

public record Store(String name, GeoPoint point) {

    public Store {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(point, "point");
    }
}
