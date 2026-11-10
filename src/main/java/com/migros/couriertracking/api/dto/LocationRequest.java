package com.migros.couriertracking.api.dto;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record LocationRequest(
        @NotNull Instant time,
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String courier,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lng) {

    public CourierLocation toDomain() {
        return new CourierLocation(time, courier, new GeoPoint(lat, lng));
    }
}
