package com.migros.couriertracking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourierTrackingIntegrationTest {

    private static final double LNG = 29.1244229;
    private static final double OUTSIDE_LAT = 40.9950;
    private static final double INSIDE_LAT = 40.9927;

    @Autowired
    private MockMvc mvc;

    private void send(String courier, String time, double lat) throws Exception {
        send(courier, time, lat, LNG);
    }

    private void send(String courier, String time, double lat, double lng) throws Exception {
        mvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"time":"%s","courier":"%s","lat":%s,"lng":%s}""".formatted(time, courier, lat, lng)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            "Ataşehir MMM Migros,    40.9923307, 29.1244229",
            "Novada MMM Migros,      40.986106,  29.1161293",
            "Beylikdüzü 5M Migros,   41.0066851, 28.6552262",
            "Ortaköy MMM Migros,     41.055783,  29.0210292",
            "Caddebostan MMM Migros, 40.9632463, 29.0630908"
    })
    void everyStoreInStoresJsonDetectsEntrances(String store, double lat, double lng) throws Exception {
        String courier = "it-store-" + store.hashCode();
        send(courier, "2026-11-09T10:00:00Z", lat + 0.01, lng);
        send(courier, "2026-11-09T10:01:00Z", lat, lng);

        mvc.perform(get("/api/store-entries").param("courierId", courier))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].store").value(store));
    }

    @Test
    void streamProducesStoreEntriesAndDistance() throws Exception {
        String courier = "it-courier-1";
        send(courier, "2026-11-09T10:00:00Z", OUTSIDE_LAT);
        send(courier, "2026-11-09T10:01:00Z", INSIDE_LAT);
        send(courier, "2026-11-09T10:05:00Z", OUTSIDE_LAT);
        send(courier, "2026-11-09T10:05:30Z", INSIDE_LAT);
        send(courier, "2026-11-09T10:04:00Z", OUTSIDE_LAT);
        send(courier, "2026-11-09T10:07:00Z", OUTSIDE_LAT);
        send(courier, "2026-11-09T10:09:00Z", INSIDE_LAT);

        mvc.perform(get("/api/store-entries").param("courierId", courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].store").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$[0].time").value("2026-11-09T10:01:00Z"))
                .andExpect(jsonPath("$[1].time").value("2026-11-09T10:09:00Z"));

        mvc.perform(get("/api/couriers/{id}/distance", courier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meters").value(closeTo(5 * 255.75, 1.0)))
                .andExpect(jsonPath("$.kilometers").value(closeTo(5 * 0.25575, 0.001)));
    }

    @Test
    void unknownCourierHasZeroDistanceAndNoEntries() throws Exception {
        mvc.perform(get("/api/couriers/{id}/distance", "it-nobody"))
                .andExpect(jsonPath("$.meters").value(0.0));
        mvc.perform(get("/api/store-entries").param("courierId", "it-nobody"))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
