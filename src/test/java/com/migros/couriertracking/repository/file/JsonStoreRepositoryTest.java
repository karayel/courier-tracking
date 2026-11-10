package com.migros.couriertracking.repository.file;

import com.migros.couriertracking.domain.GeoPoint;
import com.migros.couriertracking.domain.Store;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonStoreRepositoryTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private static ByteArrayResource json(String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadsAllStoresFromStoresJson() {
        var repository = new JsonStoreRepository(mapper, new ClassPathResource("stores.json"));

        assertThat(repository.findAll()).hasSize(5)
                .contains(new Store("Ataşehir MMM Migros", new GeoPoint(40.9923307, 29.1244229)));
    }

    @Test
    void mapsFlatLatLngIntoAPoint() {
        var repository = new JsonStoreRepository(mapper, json("""
                [{"name":"S1","lat":41.5,"lng":29.5}]"""));

        assertThat(repository.findAll()).containsExactly(new Store("S1", new GeoPoint(41.5, 29.5)));
    }

    @Test
    void storeWithMissingCoordinateFailsFast() {
        assertThatThrownBy(() -> new JsonStoreRepository(mapper, json("""
                [{"name":"S1","lat":41.5}]""")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("S1");
    }

    @Test
    void missingFileFailsFast() {
        assertThatThrownBy(() -> new JsonStoreRepository(mapper, new ClassPathResource("missing.json")))
                .isInstanceOf(UncheckedIOException.class);
    }
}
