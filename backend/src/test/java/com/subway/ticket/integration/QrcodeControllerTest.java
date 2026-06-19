package com.subway.ticket.integration;

import com.subway.ticket.dto.CreateOrderReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QrcodeControllerTest extends AbstractIntegrationTest {

    private Long createAndPayOrder() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("S001");
        req.setTo("S005");

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", orderId));
        mockMvc.perform(post("/api/payments/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payBody))
                .andExpect(status().isOk());

        return orderId;
    }

    @Test
    void shouldGenerateQrcodeForPaidOrder() throws Exception {
        Long orderId = createAndPayOrder();

        mockMvc.perform(get("/api/orders/{id}/qrcode", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.nonce").isNotEmpty())
                .andExpect(jsonPath("$.exp").isNumber())
                .andExpect(jsonPath("$.sign").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundForNonExistentOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}/qrcode", 9999)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestForUnpaidOrder() throws Exception {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("S001");
        req.setTo("S005");

        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/orders/{id}/qrcode", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
