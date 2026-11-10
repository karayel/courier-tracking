package com.migros.couriertracking.service.listener;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.distance.DistanceCalculator;
import com.migros.couriertracking.repository.CourierDistanceRepository;
import com.migros.couriertracking.service.LocationReceivedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DistanceTrackerListener {

    private final DistanceCalculator calculator;
    private final CourierDistanceRepository distances;

    public DistanceTrackerListener(DistanceCalculator calculator, CourierDistanceRepository distances) {
        this.calculator = calculator;
        this.distances = distances;
    }

    @EventListener
    public void onLocationReceived(LocationReceivedEvent event) {
        CourierLocation to = event.current();
        event.previous().ifPresent(from ->
                distances.addMeters(to.courier(), calculator.distanceInMeters(from.point(), to.point())));
    }
}
