package com.migros.couriertracking.api;

import com.migros.couriertracking.domain.CourierLocation;
import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.StoreEntry;
import com.migros.couriertracking.service.CourierQueryService;
import com.migros.couriertracking.service.LocationIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourierTrackingController.class)
class CourierTrackingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LocationIngestionService ingestion;

    @MockitoBean
    private CourierQueryService queries;

    private void postLocation(String json, int expectedStatus) throws Exception {
        mvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void validLocationIsAccepted() throws Exception {
        postLocation("""
                {"time":"2026-11-09T10:00:00Z","courier":"c1","lat":40.99,"lng":29.12}""", 200);

        verify(ingestion).ingest(new CourierLocation(Instant.parse("2026-11-09T10:00:00Z"), "c1", new GeoPoint(40.99, 29.12)));
    }

    @Test
    void invalidLocationIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"time":"2026-11-09T10:00:00Z","courier":"","lat":91,"lng":29.12}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lat").exists())
                .andExpect(jsonPath("$.errors.courier").exists())
                .andExpect(jsonPath("$.errors.lng").doesNotExist());

        verify(ingestion, never()).ingest(any());
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        postLocation("{not json", 400);
        postLocation("""
                {"time":"yesterday","courier":"c1","lat":40.99,"lng":29.12}""", 400);

        verify(ingestion, never()).ingest(any());
    }

    @Test
    void distanceIsReturnedInMetersAndKilometers() throws Exception {
        when(queries.getTotalTravelDistance("c1")).thenReturn(1500.0);

        mvc.perform(get("/api/couriers/c1/distance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value("c1"))
                .andExpect(jsonPath("$.meters").value(1500.0))
                .andExpect(jsonPath("$.kilometers").value(1.5));
    }

    @Test
    void storeEntriesAreListedOptionallyByCourier() throws Exception {
        when(queries.getStoreEntries("c1")).thenReturn(
                List.of(new StoreEntry("c1", "Ataşehir MMM Migros", Instant.parse("2026-11-09T10:00:00Z"))));

        mvc.perform(get("/api/store-entries").param("courierId", "c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courier").value("c1"))
                .andExpect(jsonPath("$[0].store").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].time").value("2026-11-09T10:00:00Z"));
    }

    @Test
    void storeEntriesWithoutCourierListsAll() throws Exception {
        when(queries.getStoreEntries(null)).thenReturn(List.of(
                new StoreEntry("c1", "Ataşehir MMM Migros", Instant.parse("2026-11-09T10:00:00Z")),
                new StoreEntry("c2", "Novada MMM Migros", Instant.parse("2026-11-09T10:01:00Z"))));

        mvc.perform(get("/api/store-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(queries).getStoreEntries(null);
    }

    @Test
    void missingBodyIsRejected() throws Exception {
        mvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(ingestion, never()).ingest(any());
    }

    @Test
    void nonJsonContentTypeIsRejected() throws Exception {
        mvc.perform(post("/api/couriers/locations").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());

        verify(ingestion, never()).ingest(any());
    }
}
