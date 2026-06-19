package com.subway.ticket.unit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.subway.ticket.domain.Line;
import com.subway.ticket.domain.Station;
import com.subway.ticket.repository.LineMapper;
import com.subway.ticket.repository.StationMapper;
import com.subway.ticket.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("站点服务单元测试")
class StationServiceTest {

    @Mock
    private StationMapper stationMapper;

    @Mock
    private LineMapper lineMapper;

    private StationService stationService;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        stationService = new StationService(stationMapper, lineMapper);
        a.close();
    }

    @Test
    @DisplayName("getStationsByLine - 有效线路ID应返回站点列表含线路名")
    void getStationsByLine_validLineId_returnsStationsWithLineNames() {
        Station s1 = createStation(1L, "龙翔桥", "LXQ", 1L);
        Station s2 = createStation(2L, "凤起路", "FQL", 1L);

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(s1, s2));

        Line line = createLine(1L, "1号线", "DF4749");
        when(lineMapper.selectByIds(anyList())).thenReturn(List.of(line));

        List<Station> result = stationService.getStationsByLine(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("1号线", result.getFirst().getLineName());
        assertEquals("#DF4749", result.getFirst().getLineColor());
    }

    @Test
    @DisplayName("getStationsByLine - 无效线路ID应返回空列表")
    void getStationsByLine_invalidLineId_returnsEmptyList() {
        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<Station> result = stationService.getStationsByLine(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllStations - 应返回全部活跃站点")
    void getAllStations_returnsAllActiveStations() {
        Station s1 = createStation(1L, "站点A", "STA", 1L);
        Station s2 = createStation(2L, "站点B", "STB", 1L);
        Station s3 = createStation(3L, "站点C", "STC", 2L);

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(s1, s2, s3));

        Line line1 = createLine(1L, "1号线", "DF4749");
        Line line2 = createLine(2L, "2号线", "E57B46");
        when(lineMapper.selectByIds(anyList())).thenReturn(Arrays.asList(line1, line2));

        List<Station> result = stationService.getAllStations();

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("searchStations - 匹配关键字应返回去重结果")
    void searchStations_matchingKeyword_returnsDeduplicatedResults() {
        // 模拟换乘站有两条记录（同站名不同lineId）
        Station s1 = createStation(1L, "凤起路", "FQL", 1L);
        Station s2 = createStation(2L, "凤起路", "FQL", 2L); // 同站名不同线路
        Station s3 = createStation(3L, "龙翔桥", "LXQ", 1L);

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(s1, s2, s3));

        Line line1 = createLine(1L, "1号线", "DF4749");
        Line line2 = createLine(2L, "2号线", "E57B46");
        when(lineMapper.selectByIds(anyList())).thenReturn(Arrays.asList(line1, line2));

        List<Station> result = stationService.searchStations("凤");

        assertNotNull(result);
        // 去重后"凤起路"只出现一次
        long fengCount = result.stream()
                .filter(s -> "凤起路".equals(s.getName()))
                .count();
        assertEquals(1, fengCount);
    }

    @Test
    @DisplayName("searchStations - 无匹配关键字应返回空列表")
    void searchStations_noMatch_returnsEmptyList() {
        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<Station> result = stationService.searchStations("不存在的站点");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchStations - 空关键字应返回全部结果（不限关键字但限50）")
    void searchStations_emptyKeyword_returnsAllWithLimit() {
        List<Station> stations = createStationList(5);
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(stations);
        when(lineMapper.selectByIds(anyList())).thenReturn(Collections.emptyList());

        List<Station> result = stationService.searchStations("");

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    @DisplayName("searchStations - null关键字应返回全部结果")
    void searchStations_nullKeyword_returnsAllWithLimit() {
        List<Station> stations = createStationList(3);
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(stations);
        when(lineMapper.selectByIds(anyList())).thenReturn(Collections.emptyList());

        List<Station> result = stationService.searchStations(null);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getStationNameById - 有效ID应返回站名")
    void getStationNameById_validId_returnsStationName() {
        Station station = createStation(1L, "长春站", "CCZ", 1L);
        when(stationMapper.selectById(1L)).thenReturn(station);

        String name = stationService.getStationNameById(1L);

        assertEquals("长春站", name);
    }

    @Test
    @DisplayName("getStationNameById - 无效ID应返回Unknown")
    void getStationNameById_invalidId_returnsUnknown() {
        when(stationMapper.selectById(999L)).thenReturn(null);

        String name = stationService.getStationNameById(999L);

        assertEquals("Unknown", name);
    }

    @Test
    @DisplayName("populateLineNames - 颜色无#前缀应自动添加")
    void populateLineNames_colorWithoutHash_addsHashPrefix() {
        Station s1 = createStation(1L, "站点A", "STA", 1L);
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(s1));

        Line line = createLine(1L, "1号线", "DF4749"); // 不带 # 前缀
        when(lineMapper.selectByIds(anyList())).thenReturn(List.of(line));

        List<Station> result = stationService.getStationsByLine(1L);

        assertEquals("#DF4749", result.getFirst().getLineColor());
    }

    @Test
    @DisplayName("populateLineNames - 颜色已有#前缀应保持不变")
    void populateLineNames_colorWithHash_keepsHashPrefix() {
        Station s1 = createStation(1L, "站点A", "STA", 1L);
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(s1));

        Line line = createLine(1L, "1号线", "#DF4749"); // 带 # 前缀
        when(lineMapper.selectByIds(anyList())).thenReturn(List.of(line));

        List<Station> result = stationService.getStationsByLine(1L);

        assertEquals("#DF4749", result.getFirst().getLineColor());
    }

    @Test
    @DisplayName("populateLineNames - null颜色应返回默认颜色")
    void populateLineNames_nullColor_returnsDefaultColor() {
        Station s1 = createStation(1L, "站点A", "STA", 1L);
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(s1));

        Line line = createLine(1L, "1号线", null); // 颜色为 null
        when(lineMapper.selectByIds(anyList())).thenReturn(List.of(line));

        List<Station> result = stationService.getStationsByLine(1L);

        assertEquals("#999", result.getFirst().getLineColor());
    }

    @Test
    @DisplayName("populateLineNames - 空列表输入不抛异常")
    void populateLineNames_emptyList_noException() {
        when(stationMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Station> result = stationService.getStationsByLine(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        // 不应调用 lineMapper
        verify(lineMapper, never()).selectByIds(anyList());
    }

    @Test
    @DisplayName("searchStations - 去重保留第一个出现的记录")
    void searchStations_deduplication_keepsFirstOccurrence() {
        // 同站名 2个记录，lineId不同
        Station s1 = createStation(1L, "换乘站", "HCZ", 1L);
        Station s2 = createStation(2L, "换乘站", "HCZ", 2L);

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(s1, s2));

        Line line1 = createLine(1L, "1号线", "DF4749");
        when(lineMapper.selectByIds(anyList())).thenReturn(List.of(line1));

        List<Station> result = stationService.searchStations("换乘");

        assertEquals(1, result.size());
        // 保留第一个出现的记录（lineId=1L）
        assertEquals(1L, result.getFirst().getLineId());
    }

    @Test
    @DisplayName("getAllStations - 多个线路颜色正确填充")
    void getAllStations_multipleLines_allColorsPopulated() {
        Station s1 = createStation(1L, "站点A", "STA", 1L);
        Station s2 = createStation(2L, "站点B", "STB", 2L);

        when(stationMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(s1, s2));

        Line line1 = createLine(1L, "1号线", "DF4749");
        Line line2 = createLine(2L, "2号线", "E57B46");
        when(lineMapper.selectByIds(anyList())).thenReturn(Arrays.asList(line1, line2));

        List<Station> result = stationService.getAllStations();

        assertEquals("#DF4749", result.getFirst().getLineColor());
        assertEquals("1号线", result.getFirst().getLineName());
        assertEquals("#E57B46", result.get(1).getLineColor());
        assertEquals("2号线", result.get(1).getLineName());
    }

    // === Helper methods ===

    private Station createStation(Long id, String name, String code, Long lineId) {
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCode(code);
        station.setLineId(lineId);
        return station;
    }

    private Line createLine(Long id, String name, String color) {
        Line line = new Line();
        line.setId(id);
        line.setName(name);
        line.setColor(color);
        return line;
    }

    private List<Station> createStationList(int count) {
        java.util.ArrayList<Station> list = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Station s = new Station();
            s.setId((long) i);
            s.setName("站点" + i);
            s.setCode("ST" + i);
            s.setLineId(1L);
            list.add(s);
        }
        return list;
    }
}
