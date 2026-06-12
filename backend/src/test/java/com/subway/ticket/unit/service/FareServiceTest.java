package com.subway.ticket.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.subway.ticket.config.FareProperties;
import com.subway.ticket.domain.Station;
import com.subway.ticket.dto.FareQuote;
import com.subway.ticket.repository.StationMapper;
import com.subway.ticket.service.FareService;
import com.subway.ticket.service.GraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@DisplayName("票价服务单元测试")
class FareServiceTest {

    @Mock
    private GraphService graphService;

    @Mock
    private StationMapper stationMapper;

    @Mock
    private FareProperties fareProperties;

    private FareService fareService;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        fareService = new FareService(graphService, stationMapper, fareProperties);

        // 配置默认的票价规则
        configureDefaultFareRules();
        a.close();
    }

    private void configureDefaultFareRules() {
        when(fareProperties.getBasePrice()).thenReturn(new BigDecimal("2.00"));
        when(fareProperties.getBaseDistance()).thenReturn(2);

        FareProperties.Rule rule1 = new FareProperties.Rule();
        rule1.setDistance(2);
        rule1.setPrice(new BigDecimal("2.00"));

        FareProperties.Rule rule2 = new FareProperties.Rule();
        rule2.setDistance(4);
        rule2.setPrice(new BigDecimal("3.00"));

        FareProperties.Rule rule3 = new FareProperties.Rule();
        rule3.setDistance(7);
        rule3.setPrice(new BigDecimal("4.00"));

        List<FareProperties.Rule> rules = List.of(rule1, rule2, rule3);
        when(fareProperties.getRules()).thenReturn(rules);

        FareProperties.ExtraRule extraRule = new FareProperties.ExtraRule();
        extraRule.setStartDistance(16);
        extraRule.setInterval(4);
        extraRule.setPricePerInterval(new BigDecimal("1.00"));
        extraRule.setBasePriceForExtra(new BigDecimal("6.00"));
        when(fareProperties.getExtra()).thenReturn(extraRule);
    }

    @Test
    @DisplayName("测试正常票价计算 - 短途")
    void testCalculateFare_ShortDistance_Success() {
        // 准备测试数据
        Station fromStation = createStation(1L, "龙翔桥", "LXQ");
        Station toStation = createStation(2L, "凤起路", "FQL");

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(fromStation))
                .thenReturn(List.of(toStation));

        when(graphService.isEmpty()).thenReturn(false);
        when(graphService.getIdsByName("龙翔桥")).thenReturn(List.of(100L));
        when(graphService.getIdsByName("凤起路")).thenReturn(List.of(200L));

        GraphService.PathResult pathResult = new GraphService.PathResult(1, List.of(100L, 200L));
        when(graphService.findPath(anyList(), anyList())).thenReturn(pathResult);
        when(graphService.getCodeById(100L)).thenReturn("LXQ");
        when(graphService.getCodeById(200L)).thenReturn("FQL");
        when(graphService.getNameById(100L)).thenReturn("龙翔桥");
        when(graphService.getNameById(200L)).thenReturn("凤起路");
        when(graphService.getLineIdByNodeId(100L)).thenReturn(1L);
        when(graphService.getLineIdByNodeId(200L)).thenReturn(1L);

        // 执行测试
        FareQuote result = fareService.calculateFare("LXQ", "FQL");

        // 验证结果
        assertNotNull(result);
        assertEquals("LXQ", result.getFrom());
        assertEquals("FQL", result.getTo());
        assertEquals(1, result.getSegments());
        assertEquals(new BigDecimal("2.00"), result.getPrice());
        assertEquals("HANGZHOU_RULE", result.getMode());
        assertNotNull(result.getPath());
        assertEquals(2, result.getPath().size());
    }

    @Test
    @DisplayName("测试站点不存在的情况")
    void testCalculateFare_StationNotFound_ReturnsError() {
        // 模拟站点不存在
        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        when(graphService.isEmpty()).thenReturn(false);

        // 执行测试
        FareQuote result = fareService.calculateFare("INVALID", "FQL");

        // 验证返回错误码
        assertNotNull(result);
        assertEquals("STATION_NOT_FOUND", result.getMode());
        assertEquals(BigDecimal.ZERO, result.getPrice());
    }

    @Test
    @DisplayName("测试路径不可达的情况")
    void testCalculateFare_Unreachable_ReturnsError() {
        Station fromStation = createStation(1L, "站点A", "STA");
        Station toStation = createStation(2L, "站点B", "STB");

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(fromStation))
                .thenReturn(List.of(toStation));

        when(graphService.isEmpty()).thenReturn(false);
        when(graphService.getIdsByName("站点A")).thenReturn(List.of(100L));
        when(graphService.getIdsByName("站点B")).thenReturn(List.of(200L));

        // 模拟路径不可达
        when(graphService.findPath(anyList(), anyList())).thenReturn(null);

        // 执行测试
        FareQuote result = fareService.calculateFare("STA", "STB");

        // 验证结果
        assertNotNull(result);
        assertEquals("UNREACHABLE", result.getMode());
        assertEquals(BigDecimal.ZERO, result.getPrice());
    }

    @Test
    @DisplayName("测试中距离票价计算")
    void testCalculateFare_MediumDistance_CorrectPrice() {
        Station fromStation = createStation(1L, "火车东站", "HCD");
        Station toStation = createStation(2L, "西湖文化广场", "XWH");

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(fromStation))
                .thenReturn(List.of(toStation));

        when(graphService.isEmpty()).thenReturn(false);
        when(graphService.getIdsByName("火车东站")).thenReturn(List.of(300L));
        when(graphService.getIdsByName("西湖文化广场")).thenReturn(List.of(400L));

        // 模拟5站距离
        GraphService.PathResult pathResult = new GraphService.PathResult(5, List.of(300L, 301L, 302L, 303L, 400L));
        when(graphService.findPath(anyList(), anyList())).thenReturn(pathResult);
        when(graphService.getCodeById(anyLong())).thenAnswer(invocation -> "CODE_" + invocation.getArgument(0));
        when(graphService.getNameById(anyLong())).thenAnswer(invocation -> "Station_" + invocation.getArgument(0));
        when(graphService.getLineIdByNodeId(anyLong())).thenReturn(1L);

        // 执行测试
        FareQuote result = fareService.calculateFare("HCD", "XWH");

        // 验证票价应为4.00（5站在4-7站范围内）
        assertNotNull(result);
        assertEquals(new BigDecimal("4.00"), result.getPrice());
    }

    @Test
    @DisplayName("测试图初始化触发")
    void testCalculateFare_GraphEmpty_InitializesGraph() {
        Station fromStation = createStation(1L, "站点C", "STC");
        Station toStation = createStation(2L, "站点D", "STD");

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(fromStation))
                .thenReturn(List.of(toStation));

        when(graphService.isEmpty()).thenReturn(true).thenReturn(false);
        when(graphService.getIdsByName("站点C")).thenReturn(List.of(500L));
        when(graphService.getIdsByName("站点D")).thenReturn(List.of(600L));

        GraphService.PathResult pathResult = new GraphService.PathResult(2, List.of(500L, 600L));
        when(graphService.findPath(anyList(), anyList())).thenReturn(pathResult);
        when(graphService.getCodeById(anyLong())).thenReturn("CODE");
        when(graphService.getNameById(anyLong())).thenReturn("Station");
        when(graphService.getLineIdByNodeId(anyLong())).thenReturn(1L);

        // 执行测试
        fareService.calculateFare("STC", "STD");

        // 验证调用了initGraph
        verify(graphService, times(1)).initGraph();
    }

    @Test
    @DisplayName("测试路线步骤构建")
    void testCalculateFare_WithRouteSteps_Success() {
        Station fromStation = createStation(1L, "起点", "QD");
        Station toStation = createStation(2L, "终点", "ZD");

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(fromStation))
                .thenReturn(List.of(toStation));

        when(graphService.isEmpty()).thenReturn(false);
        when(graphService.getIdsByName("起点")).thenReturn(List.of(700L));
        when(graphService.getIdsByName("终点")).thenReturn(List.of(800L));

        GraphService.PathResult pathResult = new GraphService.PathResult(3, List.of(700L, 701L, 800L));
        when(graphService.findPath(anyList(), anyList())).thenReturn(pathResult);
        when(graphService.getCodeById(anyLong())).thenReturn("CODE");
        when(graphService.getNameById(anyLong())).thenReturn("Station");
        when(graphService.getLineIdByNodeId(anyLong())).thenReturn(1L);

        com.subway.ticket.domain.Line line = new com.subway.ticket.domain.Line();
        line.setName("1号线");
        line.setColor("DF4749");
        when(graphService.getLineInfo(1L)).thenReturn(line);
        when(graphService.getLineColor(anyString(), anyString())).thenReturn("#DF4749");

        // 执行测试
        FareQuote result = fareService.calculateFare("QD", "ZD");

        // 验证路线步骤
        assertNotNull(result);
        assertNotNull(result.getSteps());
    }

    private Station createStation(Long id, String name, String code) {
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCode(code);
        return station;
    }
}
