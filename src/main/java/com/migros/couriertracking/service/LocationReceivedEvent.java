package com.migros.couriertracking.service;

import com.migros.couriertracking.domain.CourierLocation;

import java.util.Objects;
import java.util.Optional;

public record LocationReceivedEvent(Optional<CourierLocation> previous, CourierLocation current) {

    public LocationReceivedEvent {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
    }
}
