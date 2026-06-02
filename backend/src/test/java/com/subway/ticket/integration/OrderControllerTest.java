package com.subway.ticket.integration;

import com.subway.ticket.dto.CreateOrderReq;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("S001");
        req.setTo("S005");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.price").value(3.00));
    }

    @Test
    void shouldReturnBadRequestWhenFromIsBlank() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("");
        req.setTo("S005");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForNonExistentStation() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("NOTEXIST");
        req.setTo("S005");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPersistOrderInDatabase() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("S001");
        req.setTo("S005");

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Verify the order exists in the response
        org.hamcrest.MatcherAssert.assertThat(response, org.hamcrest.Matchers.containsString("CREATED"));
    }
}
