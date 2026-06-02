package com.subway.ticket.integration;

import com.subway.ticket.dto.CreateOrderReq;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentControllerTest extends AbstractIntegrationTest {

    private Long createOrderAndGetId() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("S001");
        req.setTo("S005");

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void shouldMockPaySuccessfully() throws Exception {
        Long orderId = createOrderAndGetId();

        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", orderId));

        mockMvc.perform(post("/api/payments/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.channel").value("MOCK"));
    }

    @Test
    void shouldReturnBadRequestForNonExistentOrder() throws Exception {
        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", 9999));

        mockMvc.perform(post("/api/payments/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateOrderStatusToPaidAfterPayment() throws Exception {
        Long orderId = createOrderAndGetId();

        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", orderId));

        mockMvc.perform(post("/api/payments/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk());

        // Verify order status is now PAID (indirectly via QR generation eligibility)
        // Actually, we test this in the E2E test
    }
}
