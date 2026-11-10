package com.migros.couriertracking.domain.distance;

import com.migros.couriertracking.domain.GeoPoint;

public interface DistanceCalculator {

    double distanceInMeters(GeoPoint from, GeoPoint to);

}
