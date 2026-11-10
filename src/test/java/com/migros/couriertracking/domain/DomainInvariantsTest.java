package com.migros.couriertracking.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainInvariantsTest {

    private static final GeoPoint POINT = new GeoPoint(41.0, 29.0);

    @Test
    void courierLocationRejectsMissingParts() {
        assertThatNullPointerException().isThrownBy(() -> new CourierLocation(null, "c1", POINT)).withMessage("time");
        assertThatNullPointerException().isThrownBy(() -> new CourierLocation(Instant.EPOCH, null, POINT)).withMessage("courier");
        assertThatNullPointerException().isThrownBy(() -> new CourierLocation(Instant.EPOCH, "c1", null)).withMessage("point");
    }

    @Test
    void storeRejectsMissingParts() {
        assertThatNullPointerException().isThrownBy(() -> new Store(null, POINT)).withMessage("name");
        assertThatNullPointerException().isThrownBy(() -> new Store("s1", null)).withMessage("point");
    }

    @Test
    void storeVisitRejectsMissingParts() {
        assertThatNullPointerException().isThrownBy(() -> new StoreVisit(null, "s1", false, Instant.EPOCH)).withMessage("courier");
        assertThatNullPointerException().isThrownBy(() -> new StoreVisit("c1", null, false, Instant.EPOCH)).withMessage("store");
        assertThatNullPointerException().isThrownBy(() -> new StoreVisit("c1", "s1", false, null)).withMessage("lastExit");
    }

    @Test
    void notVisitedStartsOutsideAndNeverExited() {
        assertThat(StoreVisit.notVisited("c1", "s1"))
                .isEqualTo(new StoreVisit("c1", "s1", false, StoreVisit.NEVER_EXITED));
    }

    @Test
    void neverExitedIsLongerAgoThanAnyCooldownWithoutOverflow() {
        assertThat(Duration.between(StoreVisit.NEVER_EXITED, Instant.MAX)).isPositive();
        assertThat(Duration.between(StoreVisit.NEVER_EXITED, Instant.EPOCH)).isGreaterThan(Duration.ofDays(365_000));
    }
}
