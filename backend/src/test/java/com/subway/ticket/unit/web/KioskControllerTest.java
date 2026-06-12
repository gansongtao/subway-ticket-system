package com.subway.ticket.unit.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.subway.ticket.domain.Order;
import com.subway.ticket.domain.QrcodeToken;
import com.subway.ticket.domain.Ticket;
import com.subway.ticket.domain.enums.OrderStatus;
import com.subway.ticket.repository.OrderMapper;
import com.subway.ticket.repository.QrcodeTokenMapper;
import com.subway.ticket.repository.TicketMapper;
import com.subway.ticket.service.QrSignService;
import com.subway.ticket.service.StationService;
import com.subway.ticket.web.KioskController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("自助机控制器单元测试")
class KioskControllerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private QrcodeTokenMapper qrcodeTokenMapper;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private StationService stationService;

    @Mock
    private QrSignService mockQrSignService;

    private KioskController kioskController;

    private static final String ORIGINAL_ENV = System.getenv("QR_SIGNING_SECRET");

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        // 设置环境变量以控制内部创建的 QrSignService 的密钥
        // 但仍然通过反射注入 mockQrSignService 以获得完全控制
        kioskController = new KioskController(orderMapper, qrcodeTokenMapper, ticketMapper, stationService);
        // 通过反射替换 QrSignService 为 mock
        setField(kioskController, "qrSignService", mockQrSignService);
        a.close();
    }

    @AfterEach
    void tearDown() {
        // 清理环境变量
    }

    @Test
    @DisplayName("validate - 有效签名和PAID订单应返回验票通过")
    void validate_validSignatureAndPaidOrder_returnsValidated() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");
        String expectedData = "1:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);

        Order order = createOrder(1L, OrderStatus.PAID);
        when(orderMapper.selectById(1L)).thenReturn(order);

        QrcodeToken token = new QrcodeToken();
        token.setOrderId(1L);
        token.setNonce("nonce123");
        token.setSignature("valid-sign");
        when(qrcodeTokenMapper.selectOne(any(QueryWrapper.class))).thenReturn(token);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().valid);
        assertEquals("OK", response.getBody().reason);
    }

    @Test
    @DisplayName("validate - 签名无效应返回验票失败")
    void validate_invalidSignature_returnsSignInvalid() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "bad-sign");
        String expectedData = "1:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "bad-sign")).thenReturn(false);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("SIGN_INVALID", response.getBody().reason);
        // 签名无效时不应查询订单
        verify(orderMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("validate - 二维码过期应返回EXPIRED")
    void validate_expiredQr_returnsExpired() {
        long pastExpiry = Instant.now().getEpochSecond() - 60; // 1分钟前过期
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", pastExpiry, "valid-sign");
        String expectedData = "1:nonce123:" + pastExpiry;

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("EXPIRED", response.getBody().reason);
        verify(orderMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("validate - 订单不存在应返回ORDER_NOT_FOUND")
    void validate_orderNotFound_returnsOrderNotFound() {
        KioskController.QrPayload qr = createQrPayload(999L, "nonce123", 9999999999L, "valid-sign");
        String expectedData = "999:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);
        when(orderMapper.selectById(999L)).thenReturn(null);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("ORDER_NOT_FOUND", response.getBody().reason);
    }

    @Test
    @DisplayName("validate - 订单已COMPLETED应返回TICKET_ALREADY_ISSUED")
    void validate_orderAlreadyCompleted_returnsAlreadyIssued() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");
        String expectedData = "1:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);

        Order order = createOrder(1L, OrderStatus.COMPLETED);
        when(orderMapper.selectById(1L)).thenReturn(order);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("TICKET_ALREADY_ISSUED", response.getBody().reason);
    }

    @Test
    @DisplayName("validate - 订单未支付(CREATED)应返回ORDER_NOT_PAID")
    void validate_orderNotPaid_returnsOrderNotPaid() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");
        String expectedData = "1:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);

        Order order = createOrder(1L, OrderStatus.CREATED);
        when(orderMapper.selectById(1L)).thenReturn(order);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("ORDER_NOT_PAID", response.getBody().reason);
    }

    @Test
    @DisplayName("validate - TOKEN_NOT_FOUND应返回验票失败")
    void validate_tokenNotFound_returnsTokenNotFound() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");
        String expectedData = "1:nonce123:9999999999";

        when(mockQrSignService.verify(expectedData, "valid-sign")).thenReturn(true);

        Order order = createOrder(1L, OrderStatus.PAID);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(qrcodeTokenMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        ResponseEntity<KioskController.ValidateResp> response = kioskController.validate(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().valid);
        assertEquals("TOKEN_NOT_FOUND", response.getBody().reason);
    }

    @Test
    @DisplayName("issue - 正常出票应返回成功并创建Ticket")
    void issue_normalTicketIssue_returnsSuccess() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");

        Order order = createOrder(1L, OrderStatus.PAID);
        order.setFromStationId(10L);
        order.setToStationId(20L);
        order.setPrice(new BigDecimal("3.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);

        QrcodeToken token = new QrcodeToken();
        token.setId(100L);
        token.setOrderId(1L);
        token.setNonce("nonce123");
        when(qrcodeTokenMapper.selectOne(any(QueryWrapper.class))).thenReturn(token);

        when(stationService.getStationNameById(10L)).thenReturn("龙翔桥");
        when(stationService.getStationNameById(20L)).thenReturn("凤起路");
        when(ticketMapper.insert(any(Ticket.class))).thenReturn(1);
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        ResponseEntity<KioskController.IssueResp> response = kioskController.issue(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().issued);
        assertNotNull(response.getBody().ticket);
        assertEquals("龙翔桥", response.getBody().ticket.fromStation);
        assertEquals("凤起路", response.getBody().ticket.toStation);
        assertEquals(new BigDecimal("3.00"), response.getBody().ticket.price);

        // 验证 Ticket 被插入
        verify(ticketMapper, times(1)).insert(any(Ticket.class));
        // 验证订单状态更新为 COMPLETED
        verify(orderMapper, times(1)).updateById(any(Order.class));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    @DisplayName("issue - 订单已COMPLETED应幂等返回已有票信息")
    void issue_orderAlreadyCompleted_returnsExistingTicketInfo() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");

        Order order = createOrder(1L, OrderStatus.COMPLETED);
        order.setFromStationId(10L);
        order.setToStationId(20L);
        order.setPrice(new BigDecimal("3.00"));
        when(orderMapper.selectById(1L)).thenReturn(order);

        when(stationService.getStationNameById(10L)).thenReturn("龙翔桥");
        when(stationService.getStationNameById(20L)).thenReturn("凤起路");

        ResponseEntity<KioskController.IssueResp> response = kioskController.issue(qr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().issued);
        assertNotNull(response.getBody().ticket);

        // 幂等：不应创建新 Ticket
        verify(ticketMapper, never()).insert(any(Ticket.class));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    @DisplayName("issue - 订单不存在应返回400")
    void issue_orderNotFound_returns400() {
        KioskController.QrPayload qr = createQrPayload(999L, "nonce123", 9999999999L, "valid-sign");

        when(orderMapper.selectById(999L)).thenReturn(null);

        ResponseEntity<KioskController.IssueResp> response = kioskController.issue(qr);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ticketMapper, never()).insert(any(Ticket.class));
    }

    @Test
    @DisplayName("issue - Token无效应返回400（未支付订单直接出票）")
    void issue_invalidToken_returns400() {
        KioskController.QrPayload qr = createQrPayload(1L, "nonce123", 9999999999L, "valid-sign");

        Order order = createOrder(1L, OrderStatus.PAID);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(qrcodeTokenMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        ResponseEntity<KioskController.IssueResp> response = kioskController.issue(qr);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(ticketMapper, never()).insert(any(Ticket.class));
    }

    // === Helper methods ===

    private KioskController.QrPayload createQrPayload(Long orderId, String nonce, long exp, String sign) {
        KioskController.QrPayload qr = new KioskController.QrPayload();
        qr.orderId = orderId;
        qr.nonce = nonce;
        qr.exp = exp;
        qr.sign = sign;
        return qr;
    }

    private Order createOrder(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setPrice(new BigDecimal("3.00"));
        order.setFromStationId(10L);
        order.setToStationId(20L);
        return order;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
