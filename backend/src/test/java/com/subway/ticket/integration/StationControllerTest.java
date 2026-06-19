package com.subway.ticket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StationControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldReturnStationsByLineId() throws Exception {
        mockMvc.perform(get("/api/stations")
                        .param("lineId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldReturnEmptyForNonExistentLineId() throws Exception {
        mockMvc.perform(get("/api/stations")
                        .param("lineId", "999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnAllStations() throws Exception {
        mockMvc.perform(get("/api/stations/all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void shouldSearchStationsByKeyword() throws Exception {
        mockMvc.perform(get("/api/stations/search")
                        .param("keyword", "武林")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("武林广场"));
    }
}
