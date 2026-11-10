package com.migros.couriertracking.domain.distance;

import com.migros.couriertracking.domain.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final DistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void samePointIsZero() {
        var point = new GeoPoint(40.99, 29.12);

        assertThat(calculator.distanceInMeters(point, point)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Km() {
        assertThat(calculator.distanceInMeters(new GeoPoint(0, 0), new GeoPoint(1, 0)))
                .isCloseTo(111_194.93, within(0.1));
    }

    @Test
    void atasehirToNovadaIsAbout982Meters() {
        double meters = calculator.distanceInMeters(
                new GeoPoint(40.9923307, 29.1244229), new GeoPoint(40.986106, 29.1161293));

        assertThat(meters).isCloseTo(981.66, within(0.1));
    }

    @Test
    void isSymmetric() {
        var a = new GeoPoint(41.0, 29.0);
        var b = new GeoPoint(41.1, 29.2);

        assertThat(calculator.distanceInMeters(a, b)).isCloseTo(calculator.distanceInMeters(b, a), within(1e-9));
    }
}
