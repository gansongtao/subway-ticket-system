package com.subway.ticket.unit.web;

import com.subway.ticket.domain.Order;
import com.subway.ticket.domain.QrcodeToken;
import com.subway.ticket.domain.enums.OrderStatus;
import com.subway.ticket.repository.OrderMapper;
import com.subway.ticket.repository.QrcodeTokenMapper;
import com.subway.ticket.service.QrSignService;
import com.subway.ticket.web.QrcodeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("二维码控制器单元测试")
class QrcodeControllerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private QrcodeTokenMapper qrcodeTokenMapper;

    @Mock
    private QrSignService mockQrSignService;

    private QrcodeController qrcodeController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        qrcodeController = new QrcodeController(orderMapper, qrcodeTokenMapper);
        // 通过反射替换内部创建的 QrSignService 为 mock
        setField(qrcodeController, "qrSignService", mockQrSignService);
        a.close();
    }

    @Test
    @DisplayName("qrcode - PAID订单应生成二维码返回200")
    void qrcode_paidOrder_returns200WithQrData() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PAID);
        when(orderMapper.selectById(1L)).thenReturn(order);

        when(mockQrSignService.sign(anyString())).thenReturn("mock-signature-abc123");
        when(qrcodeTokenMapper.insert(any(QrcodeToken.class))).thenReturn(1);

        ResponseEntity<?> response = qrcodeController.qrcode(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(QrcodeController.QrPayload.class, response.getBody());

        QrcodeController.QrPayload payload = (QrcodeController.QrPayload) response.getBody();
        assertEquals(1L, payload.orderId);
        assertNotNull(payload.nonce);
        assertFalse(payload.nonce.isEmpty());
        assertTrue(payload.exp > 0);
        assertEquals("mock-signature-abc123", payload.sign);

        // 验证 Token 被持久化
        verify(qrcodeTokenMapper, times(1)).insert(any(QrcodeToken.class));
    }

    @Test
    @DisplayName("qrcode - COMPLETED订单应返回409")
    void qrcode_completedOrder_returns409() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderMapper.selectById(1L)).thenReturn(order);

        ResponseEntity<?> response = qrcodeController.qrcode(1L);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(QrcodeController.ErrorResp.class, response.getBody());

        QrcodeController.ErrorResp error = (QrcodeController.ErrorResp) response.getBody();
        assertEquals("ORDER_COMPLETED", error.code);
        assertEquals("订单已出票", error.message);

        // 不应生成新 token
        verify(qrcodeTokenMapper, never()).insert(any(QrcodeToken.class));
    }

    @Test
    @DisplayName("qrcode - CREATED订单未支付应返回400")
    void qrcode_unpaidOrder_returns400() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        when(orderMapper.selectById(1L)).thenReturn(order);

        ResponseEntity<?> response = qrcodeController.qrcode(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());

        verify(qrcodeTokenMapper, never()).insert(any(QrcodeToken.class));
    }

    @Test
    @DisplayName("qrcode - 订单不存在应返回404")
    void qrcode_orderNotFound_returns404() {
        when(orderMapper.selectById(999L)).thenReturn(null);

        ResponseEntity<?> response = qrcodeController.qrcode(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(qrcodeTokenMapper, never()).insert(any(QrcodeToken.class));
    }

    @Test
    @DisplayName("qrcode - 生成的签名数据格式正确")
    void qrcode_generatedSignatureData_hasCorrectFormat() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus(OrderStatus.PAID);
        when(orderMapper.selectById(5L)).thenReturn(order);

        // 捕获签名时的 data 参数
        when(mockQrSignService.sign(anyString())).thenReturn("sig-xyz");
        when(qrcodeTokenMapper.insert(any(QrcodeToken.class))).thenReturn(1);

        ResponseEntity<?> response = qrcodeController.qrcode(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // 验证 sign 被调用的 data 格式为 "orderId:nonce:exp"
        verify(mockQrSignService).sign(argThat(data ->
                data.matches("^5:[0-9a-f\\-]+:\\d+$")
        ));
    }

    @Test
    @DisplayName("qrcode - 相同订单两次请求生成不同nonce")
    void qrcode_sameOrderTwice_generatesDifferentNonces() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PAID);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(mockQrSignService.sign(anyString())).thenReturn("sig-1", "sig-2");
        when(qrcodeTokenMapper.insert(any(QrcodeToken.class))).thenReturn(1);

        ResponseEntity<?> response1 = qrcodeController.qrcode(1L);
        ResponseEntity<?> response2 = qrcodeController.qrcode(1L);

        QrcodeController.QrPayload payload1 = (QrcodeController.QrPayload) response1.getBody();
        QrcodeController.QrPayload payload2 = (QrcodeController.QrPayload) response2.getBody();

        // 两次请求的 nonce 应该不同 (UUID)
        assertNotNull(payload2);
        assertNotNull(payload1);
        assertNotEquals(payload1.nonce, payload2.nonce);
        // 签名也应该不同 (因为 nonce 不同)
        assertNotEquals(payload1.sign, payload2.sign);
    }

    // === Helper methods ===

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
