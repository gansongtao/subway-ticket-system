package com.subway.ticket.unit.impl;

import com.subway.ticket.domain.Order;
import com.subway.ticket.domain.Station;
import com.subway.ticket.domain.enums.OrderStatus;
import com.subway.ticket.dto.CreateOrderReq;
import com.subway.ticket.dto.FareQuote;
import com.subway.ticket.exception.BusinessException;
import com.subway.ticket.repository.OrderMapper;
import com.subway.ticket.repository.StationMapper;
import com.subway.ticket.service.FareService;
import com.subway.ticket.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("订单服务单元测试")
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private StationMapper stationMapper;

    @Mock
    private FareService fareService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        orderService = new OrderServiceImpl(orderMapper, stationMapper, fareService);
        a.close();
    }

    @Test
    @DisplayName("测试创建订单 - 成功")
    void testCreateOrder_Success() {
        // 准备测试数据
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        Station fromStation = createStation(1L, "龙翔桥", "LXQ");
        Station toStation = createStation(2L, "凤起路", "FQL");

        FareQuote quote = new FareQuote();
        quote.setPrice(new BigDecimal("2.00"));

        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(quote);
        when(stationMapper.selectOne(any())).thenReturn(fromStation).thenReturn(toStation);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        // 执行测试
        Order result = orderService.createOrder(req);

        // 验证结果
        assertNotNull(result);
        assertEquals(new BigDecimal("2.00"), result.getPrice());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        // 验证方法被调用
        verify(fareService, times(1)).calculateFare("LXQ", "FQL");
        verify(orderMapper, times(1)).insert(any(Order.class));
    }

    @Test
    @DisplayName("测试创建订单 - 票价计算失败")
    void testCreateOrder_FareCalculationFailed_ThrowsException() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(req));

        assertEquals("无法计算票价，请检查站点是否连通", exception.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("测试创建订单 - 出发站不存在")
    void testCreateOrder_FromStationNotFound_ThrowsException() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("INVALID");
        req.setTo("FQL");

        FareQuote quote = new FareQuote();
        quote.setPrice(new BigDecimal("2.00"));

        when(fareService.calculateFare("INVALID", "FQL")).thenReturn(quote);
        when(stationMapper.selectOne(any())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(req));

        assertTrue(exception.getMessage().contains("出发站不存在"));
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("测试创建订单 - 到达站不存在")
    void testCreateOrder_ToStationNotFound_ThrowsException() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("INVALID");

        FareQuote quote = new FareQuote();
        quote.setPrice(new BigDecimal("2.00"));

        Station fromStation = createStation(1L, "龙翔桥", "LXQ");

        when(fareService.calculateFare("LXQ", "INVALID")).thenReturn(quote);
        when(stationMapper.selectOne(any()))
                .thenReturn(fromStation)
                .thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(req));

        assertTrue(exception.getMessage().contains("到达站不存在"));
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("测试创建订单 - 票价为null时抛出异常")
    void testCreateOrder_NullPrice_ThrowsException() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        FareQuote quote = new FareQuote();
        quote.setPrice(null);

        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(quote);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createOrder(req));

        assertEquals("无法计算票价，请检查站点是否连通", exception.getMessage());
    }

    @Test
    @DisplayName("测试创建订单 - 验证订单状态")
    void testCreateOrder_CorrectStatus_SetCreated() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        Station fromStation = createStation(1L, "龙翔桥", "LXQ");
        Station toStation = createStation(2L, "凤起路", "FQL");

        FareQuote quote = new FareQuote();
        quote.setPrice(new BigDecimal("3.00"));

        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(quote);
        when(stationMapper.selectOne(any()))
                .thenReturn(fromStation)
                .thenReturn(toStation);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        // 执行测试
        Order result = orderService.createOrder(req);

        // 验证订单状态为CREATED
        assertEquals(OrderStatus.CREATED, result.getStatus());
    }

    @Test
    @DisplayName("测试创建订单 - 验证站点ID设置")
    void testCreateOrder_CorrectStationIds_Set() {
        CreateOrderReq req = new CreateOrderReq();
        req.setFrom("LXQ");
        req.setTo("FQL");

        Station fromStation = createStation(100L, "龙翔桥", "LXQ");
        Station toStation = createStation(200L, "凤起路", "FQL");

        FareQuote quote = new FareQuote();
        quote.setPrice(new BigDecimal("2.00"));

        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(quote);
        when(stationMapper.selectOne(any()))
                .thenReturn(fromStation)
                .thenReturn(toStation);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        // 执行测试
        Order result = orderService.createOrder(req);

        // 验证站点ID正确设置
        assertEquals(100L, result.getFromStationId());
        assertEquals(200L, result.getToStationId());
    }

    private Station createStation(Long id, String name, String code) {
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCode(code);
        return station;
    }
}
