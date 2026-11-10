package com.migros.couriertracking.api;

import com.migros.couriertracking.api.dto.DistanceResponse;
import com.migros.couriertracking.api.dto.LocationRequest;
import com.migros.couriertracking.api.dto.StoreEntryResponse;
import com.migros.couriertracking.service.CourierQueryService;
import com.migros.couriertracking.service.LocationIngestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourierTrackingController {

    private final LocationIngestionService ingestion;
    private final CourierQueryService queries;

    public CourierTrackingController(LocationIngestionService ingestion, CourierQueryService queries) {
        this.ingestion = ingestion;
        this.queries = queries;
    }

    @PostMapping("/couriers/locations")
    public void receiveLocation(@Valid @RequestBody LocationRequest request) {
        ingestion.ingest(request.toDomain());
    }

    @GetMapping("/couriers/{courierId}/distance")
    public DistanceResponse getTotalTravelDistance(@PathVariable String courierId) {
        return DistanceResponse.ofMeters(courierId, queries.getTotalTravelDistance(courierId));
    }

    @GetMapping("/store-entries")
    public List<StoreEntryResponse> getStoreEntries(@RequestParam(required = false) String courierId) {
        return queries.getStoreEntries(courierId).stream().map(StoreEntryResponse::from).toList();
    }
}
