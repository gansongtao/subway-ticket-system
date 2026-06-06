package com.subway.ticket.unit;

import com.subway.ticket.domain.Line;
import com.subway.ticket.domain.LineStation;
import com.subway.ticket.domain.Station;
import com.subway.ticket.repository.LineMapper;
import com.subway.ticket.repository.LineStationMapper;
import com.subway.ticket.repository.StationMapper;
import com.subway.ticket.service.GraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("图算法服务单元测试")
class GraphServiceTest {

    @Mock
    private StationMapper stationMapper;

    @Mock
    private LineStationMapper lineStationMapper;

    @Mock
    private LineMapper lineMapper;

    private GraphService graphService;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        graphService = new GraphService(stationMapper, lineStationMapper, lineMapper);

        // 通过反射设置配置值
        setField(graphService, "costPerStation", 100);
        setField(graphService, "costPerTransfer", 350);
        a.close();
    }

    @Test
    @DisplayName("测试图初始化 - 基本结构")
    void testInitGraph_BasicStructure_Success() {
        // 准备测试数据
        List<Line> lines = createTestLines();
        List<Station> stations = createTestStations();
        List<LineStation> lineStations = createTestLineStations();

        when(lineMapper.selectList(null)).thenReturn(lines);
        when(stationMapper.selectList(null)).thenReturn(stations);
        when(lineStationMapper.selectList(null)).thenReturn(lineStations);

        // 执行初始化
        graphService.initGraph();

        // 验证图不为空
        assertFalse(graphService.isEmpty());

        // 验证节点名称映射
        assertEquals("龙翔桥", graphService.getNameById(1L));
        assertEquals("凤起路", graphService.getNameById(2L));
    }

    @Test
    @DisplayName("测试根据名称获取节点ID")
    void testGetIdsByName_ExistingStation_ReturnsIds() {
        setupGraphData();
        graphService.initGraph();

        List<Long> ids = graphService.getIdsByName("龙翔桥");

        assertNotNull(ids);
        assertFalse(ids.isEmpty());
        assertTrue(ids.contains(1L));
    }

    @Test
    @DisplayName("测试根据名称获取节点ID - 不存在的站点")
    void testGetIdsByName_NonExistentStation_ReturnsEmpty() {
        setupGraphData();
        graphService.initGraph();

        List<Long> ids = graphService.getIdsByName("不存在的站点");

        assertNotNull(ids);
        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("测试Dijkstra算法 - 最短路径")
    void testFindPath_ShortestPath_Success() {
        setupGraphData();
        graphService.initGraph();

        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(3L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNotNull(result);
        assertNotNull(result.pathIds);
        assertFalse(result.pathIds.isEmpty());
        assertTrue(result.distance >= 0);
    }

    @Test
    @DisplayName("测试Dijkstra算法 - 不可达路径")
    void testFindPath_Unreachable_ReturnsNull() {
        // 不初始化图，保持为空

        List<Long> startIds = List.of(999L);
        List<Long> endIds = List.of(888L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNull(result);
    }

    @Test
    @DisplayName("测试换乘检测")
    void testIsTransfer_DifferentLines_True() {
        setupGraphData();
        graphService.initGraph();

        // 假设节点1和节点2属于不同线路
        boolean isTransfer = graphService.isTransfer(1L, 4L);

        assertTrue(isTransfer);
    }

    @Test
    @DisplayName("测试同线路非换乘")
    void testIsTransfer_SameLine_False() {
        setupGraphData();
        graphService.initGraph();

        // 假设节点1和节点2属于同一线路
        boolean isTransfer = graphService.isTransfer(1L, 2L);

        assertFalse(isTransfer);
    }

    @Test
    @DisplayName("测试获取线路颜色")
    void testGetLineColor_KnownLine_ReturnsColor() {
        setupGraphData();

        String color = graphService.getLineColor("1号线", null);

        assertEquals("#DF4749", color);
    }

    @Test
    @DisplayName("测试获取线路颜色 - 带括号线路名")
    void testGetLineColor_WithParentheses_ReturnsColor() {
        setupGraphData();

        String color = graphService.getLineColor("3号线 (石马-星桥)", null);

        assertEquals("#FFCD00", color);
    }

    @Test
    @DisplayName("测试获取线路信息")
    void testGetLineInfo_ExistingLine_ReturnsLine() {
        setupGraphData();
        graphService.initGraph();

        Line line = graphService.getLineInfo(1L);

        assertNotNull(line);
        assertEquals("1号线", line.getName());
    }

    @Test
    @DisplayName("测试路径距离计算")
    void testFindPath_CorrectDistance_Calculated() {
        setupGraphData();
        graphService.initGraph();

        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(2L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNotNull(result);
        // 验证距离是合理的（应该大于0）
        assertTrue(result.distance > 0);
    }

    private void setupGraphData() {
        List<Line> lines = createTestLines();
        List<Station> stations = createTestStations();
        List<LineStation> lineStations = createTestLineStations();

        when(lineMapper.selectList(null)).thenReturn(lines);
        when(stationMapper.selectList(null)).thenReturn(stations);
        when(lineStationMapper.selectList(null)).thenReturn(lineStations);
    }

    private List<Line> createTestLines() {
        Line line1 = new Line();
        line1.setId(1L);
        line1.setName("1号线");
        line1.setColor("DF4749");

        Line line2 = new Line();
        line2.setId(2L);
        line2.setName("2号线");
        line2.setColor("E57B46");

        return List.of(line1, line2);
    }

    private List<Station> createTestStations() {
        Station s1 = new Station();
        s1.setId(10L);
        s1.setName("龙翔桥");
        s1.setCode("LXQ");

        Station s2 = new Station();
        s2.setId(11L);
        s2.setName("凤起路");
        s2.setCode("FQL");

        Station s3 = new Station();
        s3.setId(12L);
        s3.setName("武林广场");
        s3.setCode("WGC");

        return List.of(s1, s2, s3);
    }

    private List<LineStation> createTestLineStations() {
        LineStation ls1 = new LineStation();
        ls1.setId(1L);
        ls1.setLineId(1L);
        ls1.setStationId(10L);
        ls1.setSeq(1);

        LineStation ls2 = new LineStation();
        ls2.setId(2L);
        ls2.setLineId(1L);
        ls2.setStationId(11L);
        ls2.setSeq(2);

        LineStation ls3 = new LineStation();
        ls3.setId(3L);
        ls3.setLineId(1L);
        ls3.setStationId(12L);
        ls3.setSeq(3);

        LineStation ls4 = new LineStation();
        ls4.setId(4L);
        ls4.setLineId(2L);
        ls4.setStationId(11L);
        ls4.setSeq(1);

        return List.of(ls1, ls2, ls3, ls4);
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
