package com.subway.ticket.unit.dto;

import com.subway.ticket.dto.CreateOrderReq;
import com.subway.ticket.dto.FareQuote;
import com.subway.ticket.dto.RouteStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("数据传输对象单元测试")
class DtoTest {

    @Test
    @DisplayName("测试FareQuote - 构造函数和Getter/Setter")
    void testFareQuote_ConstructorAndGetters_Success() {
        List<String> path = Arrays.asList("LXQ", "FQL", "WGC");
        List<RouteStep> steps = List.of(
                new RouteStep("1号线", "#DF4749", "龙翔桥", "凤起路", 2)
        );

        FareQuote quote = new FareQuote(
            "LXQ",
            "FQL",
            2,
            new BigDecimal("3.00"),
            "HANGZHOU_RULE",
            path,
            steps
        );

        assertEquals("LXQ", quote.getFrom());
        assertEquals("FQL", quote.getTo());
        assertEquals(2, quote.getSegments());
        assertEquals(new BigDecimal("3.00"), quote.getPrice());
        assertEquals("HANGZHOU_RULE", quote.getMode());
        assertEquals(3, quote.getPath().size());
        assertEquals(1, quote.getSteps().size());
    }

    @Test
    @DisplayName("测试FareQuote - 无参构造函数")
    void testFareQuote_DefaultConstructor_Success() {
        FareQuote quote = new FareQuote();

        assertNotNull(quote);
        assertNull(quote.getFrom());
        assertNull(quote.getTo());
        assertEquals(0, quote.getSegments());
        assertNull(quote.getPrice());
    }

    @Test
    @DisplayName("测试FareQuote - Setter方法")
    void testFareQuote_Setters_Success() {
        FareQuote quote = new FareQuote();

        quote.setFrom("LXQ");
        quote.setTo("FQL");
        quote.setSegments(3);
        quote.setPrice(new BigDecimal("4.00"));
        quote.setMode("TEST_MODE");

        List<String> path = Arrays.asList("LXQ", "FQL");
        quote.setPath(path);

        assertEquals("LXQ", quote.getFrom());
        assertEquals("FQL", quote.getTo());
        assertEquals(3, quote.getSegments());
        assertEquals(new BigDecimal("4.00"), quote.getPrice());
        assertEquals("TEST_MODE", quote.getMode());
        assertEquals(2, quote.getPath().size());
    }

    @Test
    @DisplayName("测试RouteStep - 构造函数和Getter/Setter")
    void testRouteStep_ConstructorAndGetters_Success() {
        RouteStep step = new RouteStep(
            "1号线",
            "#DF4749",
            "龙翔桥",
            "凤起路",
            3
        );

        assertEquals("1号线", step.getLineName());
        assertEquals("#DF4749", step.getLineColor());
        assertEquals("龙翔桥", step.getFromStation());
        assertEquals("凤起路", step.getToStation());
        assertEquals(3, step.getStationCount());
    }

    @Test
    @DisplayName("测试RouteStep - 无参构造函数")
    void testRouteStep_DefaultConstructor_Success() {
        RouteStep step = new RouteStep();

        assertNotNull(step);
        assertNull(step.getLineName());
        assertNull(step.getLineColor());
        assertNull(step.getFromStation());
        assertNull(step.getToStation());
        assertEquals(0, step.getStationCount());
    }

    @Test
    @DisplayName("测试RouteStep - Setter方法")
    void testRouteStep_Setters_Success() {
        RouteStep step = new RouteStep();

        step.setLineName("2号线");
        step.setLineColor("#E57B46");
        step.setFromStation("站点A");
        step.setToStation("站点B");
        step.setStationCount(5);

        assertEquals("2号线", step.getLineName());
        assertEquals("#E57B46", step.getLineColor());
        assertEquals("站点A", step.getFromStation());
        assertEquals("站点B", step.getToStation());
        assertEquals(5, step.getStationCount());
    }

    @Test
    @DisplayName("测试CreateOrderReq - Getter/Setter")
    void testCreateOrderReq_GettersAndSetters_Success() {
        CreateOrderReq req = new CreateOrderReq();

        req.setFrom("LXQ");
        req.setTo("FQL");

        assertEquals("LXQ", req.getFrom());
        assertEquals("FQL", req.getTo());
    }

    @Test
    @DisplayName("测试DTO对象相等性")
    void testFareQuote_EqualsAndHashCode_Works() {
        FareQuote quote1 = new FareQuote(
            "LXQ", "FQL", 2,
            new BigDecimal("3.00"),
            "HANGZHOU_RULE",
            Arrays.asList("LXQ", "FQL"),
            null
        );

        FareQuote quote2 = new FareQuote(
            "LXQ", "FQL", 2,
            new BigDecimal("3.00"),
            "HANGZHOU_RULE",
            Arrays.asList("LXQ", "FQL"),
            null
        );

        assertEquals(quote1, quote2);
        assertEquals(quote1.hashCode(), quote2.hashCode());
    }
}
