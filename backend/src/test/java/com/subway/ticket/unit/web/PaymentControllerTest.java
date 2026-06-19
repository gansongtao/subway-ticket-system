package com.subway.ticket.unit.web;

import com.subway.ticket.domain.Order;
import com.subway.ticket.domain.Payment;
import com.subway.ticket.domain.enums.OrderStatus;
import com.subway.ticket.repository.OrderMapper;
import com.subway.ticket.repository.PaymentMapper;
import com.subway.ticket.web.PaymentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("支付控制器单元测试")
class PaymentControllerTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private OrderMapper orderMapper;

    private PaymentController paymentController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        paymentController = new PaymentController(paymentMapper, orderMapper);
        a.close();
    }

    @Test
    @DisplayName("mock - 有效订单应返回200且支付成功")
    void mock_validOrder_returns200WithPayment() {
        PaymentController.MockPayReq req = new PaymentController.MockPayReq();
        req.orderId = 1L;

        Order order = new Order();
        order.setId(1L);
        order.setPrice(new BigDecimal("3.00"));
        order.setStatus(OrderStatus.CREATED);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        ResponseEntity<Payment> response = paymentController.mock(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getOrderId());
        assertEquals(new BigDecimal("3.00"), response.getBody().getAmount());
        assertEquals("SUCCESS", response.getBody().getStatus());
        assertEquals("MOCK", response.getBody().getChannel());

        // 验证 Payment 被插入
        verify(paymentMapper, times(1)).insert(any(Payment.class));
        // 验证订单状态变为 PAID
        verify(orderMapper, times(1)).updateById(any(Order.class));
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    @DisplayName("mock - 订单不存在应返回400")
    void mock_orderNotFound_returns400() {
        PaymentController.MockPayReq req = new PaymentController.MockPayReq();
        req.orderId = 999L;

        when(orderMapper.selectById(999L)).thenReturn(null);

        ResponseEntity<Payment> response = paymentController.mock(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());

        // 不应创建 Payment
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    @DisplayName("mock - order价格为null时使用0作为金额")
    void mock_nullPrice_usesZeroAmount() {
        PaymentController.MockPayReq req = new PaymentController.MockPayReq();
        req.orderId = 1L;

        Order order = new Order();
        order.setId(1L);
        order.setPrice(null); // 价格为 null
        order.setStatus(OrderStatus.CREATED);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        ResponseEntity<Payment> response = paymentController.mock(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("0"), response.getBody().getAmount());
    }

    @Test
    @DisplayName("mock - 已支付订单重复支付（幂等性验证）")
    void mock_alreadyPaidOrder_stillProcessesPayment() {
        PaymentController.MockPayReq req = new PaymentController.MockPayReq();
        req.orderId = 1L;

        Order order = new Order();
        order.setId(1L);
        order.setPrice(new BigDecimal("3.00"));
        order.setStatus(OrderStatus.PAID); // 已经支付过
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);

        ResponseEntity<Payment> response = paymentController.mock(req);

        // 当前实现允许重复支付（每次创建新Payment记录）
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(paymentMapper, times(1)).insert(any(Payment.class));
    }
}
