package com.migros.couriertracking.api.dto;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRequestTest {

    private static final Instant T0 = Instant.parse("2026-11-09T10:00:00Z");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> invalidFields(LocationRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toSet());
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(invalidFields(new LocationRequest(T0, "courier-1", 40.99, 29.12))).isEmpty();
    }

    @Test
    void missingFieldsAreRejected() {
        assertThat(invalidFields(new LocationRequest(null, " ", null, null)))
                .containsExactlyInAnyOrder("time", "courier", "lat", "lng");
    }

    @Test
    void outOfRangeCoordinatesAreRejected() {
        assertThat(invalidFields(new LocationRequest(T0, "courier-1", 90.1, -180.1)))
                .containsExactlyInAnyOrder("lat", "lng");
    }

    @Test
    void boundaryCoordinatesAreAccepted() {
        assertThat(invalidFields(new LocationRequest(T0, "courier-1", -90.0, 180.0))).isEmpty();
    }

    @Test
    void courierWithLineBreakOrSpecialCharactersIsRejected() {
        for (String courier : new String[] {"c1\nFAKE LOG LINE", "c1 c2", "c1;drop", "ö"}) {
            assertThat(invalidFields(new LocationRequest(T0, courier, 41.0, 29.0))).as(courier).containsExactly("courier");
        }
    }

    @Test
    void courierLengthIsLimitedTo64() {
        assertThat(invalidFields(new LocationRequest(T0, "a".repeat(64), 41.0, 29.0))).isEmpty();
        assertThat(invalidFields(new LocationRequest(T0, "a".repeat(65), 41.0, 29.0))).containsExactly("courier");
    }

    @Test
    void typicalCourierIdsAreAccepted() {
        for (String courier : new String[] {"c1", "courier-42", "COURIER_7", "-123"}) {
            assertThat(invalidFields(new LocationRequest(T0, courier, 41.0, 29.0))).as(courier).isEmpty();
        }
    }

    @Test
    void mapsToDomainKeepingLatAndLngInPlace() {
        assertThat(new LocationRequest(T0, "courier-1", 40.99, 29.12).toDomain())
                .isEqualTo(new CourierLocation(T0, "courier-1", new GeoPoint(40.99, 29.12)));
    }
}
