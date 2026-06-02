package com.subway.ticket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FareControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldReturnFareQuoteForValidStations() throws Exception {
        mockMvc.perform(get("/api/fares/quote")
                        .param("from", "S001")
                        .param("to", "S005")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("S001"))
                .andExpect(jsonPath("$.to").value("S005"))
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.segments").isNumber())
                .andExpect(jsonPath("$.path").isArray())
                .andExpect(jsonPath("$.path.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void shouldReturnBadRequestForInvalidStation() throws Exception {
        mockMvc.perform(get("/api/fares/quote")
                        .param("from", "INVALID")
                        .param("to", "S005")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForEmptyParams() throws Exception {
        mockMvc.perform(get("/api/fares/quote")
                        .param("from", "")
                        .param("to", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnFareForSameLineStations() throws Exception {
        mockMvc.perform(get("/api/fares/quote")
                        .param("from", "S001")
                        .param("to", "S003")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(2.00))
                .andExpect(jsonPath("$.segments").value(2))
                .andExpect(jsonPath("$.steps").isArray());
    }
}
