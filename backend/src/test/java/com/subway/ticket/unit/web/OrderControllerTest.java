package com.subway.ticket.unit.web;

import com.subway.ticket.domain.Order;
import com.subway.ticket.domain.enums.OrderStatus;
import com.subway.ticket.dto.CreateOrderReq;
import com.subway.ticket.exception.BusinessException;
import com.subway.ticket.service.OrderService;
import com.subway.ticket.web.OrderController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("订单控制器单元测试")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController orderController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderService);
        a.close();
    }

    @Test
    @DisplayName("create - 有效请求应返回200和订单信息")
    void create_validRequest_returns200WithOrder() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        Order mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setFromStationId(10L);
        mockOrder.setToStationId(20L);
        mockOrder.setPrice(new BigDecimal("2.00"));
        mockOrder.setStatus(OrderStatus.CREATED);

        when(orderService.createOrder(req)).thenReturn(mockOrder);

        ResponseEntity<Order> response = orderController.create(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(new BigDecimal("2.00"), response.getBody().getPrice());
        assertEquals(OrderStatus.CREATED, response.getBody().getStatus());
        verify(orderService, times(1)).createOrder(req);
    }

    @Test
    @DisplayName("create - Service抛BusinessException应向上传播")
    void create_serviceThrowsBusinessException_propagates() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("INVALID");
        req.setTo("FQL");

        when(orderService.createOrder(req))
                .thenThrow(new BusinessException("出发站不存在: INVALID"));

        BusinessException exception = assertThrows(BusinessException.class, () -> orderController.create(req));

        assertEquals("出发站不存在: INVALID", exception.getMessage());
        verify(orderService, times(1)).createOrder(req);
    }

    @Test
    @DisplayName("create - 请求正确传递给Service层")
    void create_requestPassedCorrectlyToService() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("STA-A");
        req.setTo("STA-B");

        Order mockOrder = new Order();
        mockOrder.setId(2L);
        mockOrder.setPrice(new BigDecimal("5.00"));
        mockOrder.setStatus(OrderStatus.CREATED);

        when(orderService.createOrder(req)).thenReturn(mockOrder);

        ResponseEntity<Order> response = orderController.create(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // 验证传递给 Service 的参数包含正确的 from 和 to
        verify(orderService).createOrder(argThat(r ->
                "STA-A".equals(r.getFrom()) && "STA-B".equals(r.getTo())
        ));
    }

    @Test
    @DisplayName("create - 不同站点组合创建订单")
    void create_differentStationPairs_createsCorrectOrder() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("001001");
        req.setTo("002005");

        Order mockOrder = new Order();
        mockOrder.setId(3L);
        mockOrder.setPrice(new BigDecimal("4.00"));
        mockOrder.setStatus(OrderStatus.CREATED);

        when(orderService.createOrder(req)).thenReturn(mockOrder);

        ResponseEntity<Order> response = orderController.create(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3L, response.getBody().getId());
    }
}
