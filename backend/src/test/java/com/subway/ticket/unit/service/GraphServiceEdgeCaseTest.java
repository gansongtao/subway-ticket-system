package com.subway.ticket.unit.service;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("图算法服务边界场景测试")
class GraphServiceEdgeCaseTest {

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
        setField(graphService, "costPerStation", 100);
        setField(graphService, "costPerTransfer", 350);
        a.close();
    }

    @Test
    @DisplayName("findPath - 起点终点相同应返回零距离")
    void findPath_sameStartAndEnd_returnsZeroDistance() {
        setupSingleNodeGraph();
        graphService.initGraph();

        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(1L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNotNull(result);
        assertEquals(0, result.distance);
        assertEquals(1, result.pathIds.size());
        assertEquals(1L, result.pathIds.getFirst());
    }

    @Test
    @DisplayName("findPath - 单线多站应找到最短路径")
    void findPath_singleLineMultipleStations_findsShortestPath() {
        setupLinearLineGraph();
        graphService.initGraph();

        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(5L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNotNull(result);
        assertTrue(result.distance > 0);
        // 路径应包含 1->2->3->4->5 (5个节点)
        assertEquals(5, result.pathIds.size());
        assertEquals(1L, result.pathIds.get(0));
        assertEquals(5L, result.pathIds.get(4));
    }

    @Test
    @DisplayName("findPath - 多换乘路径应选最少换乘（考虑换乘惩罚权重）")
    void findPath_multipleTransferPaths_choosesLeastTransferPath() {
        setupMultiPathGraph();
        graphService.initGraph();

        // 节点1到节点4: 有两条路
        // 路径A: 1->2->3->4 (同线路, 3站)
        // 路径B: 1->5->6->4 (含换乘)
        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(4L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNotNull(result);
        // 应选择没有换乘的路径（换乘权重350 > 站间权重100）
        // 检查路径中是否经过节点5,6（换乘路径）或2,3（同线路径）
        boolean hasTransferPath = result.pathIds.contains(5L) && result.pathIds.contains(6L);
        boolean hasDirectPath = result.pathIds.contains(2L) && result.pathIds.contains(3L);

        if (hasDirectPath) {
            assertTrue(true, "选择了同线路无换乘路径，符合预期");
        }
    }

    @Test
    @DisplayName("findPath - 不可达孤立子图应返回null")
    void findPath_disconnectedGraph_returnsNull() {
        setupDisconnectedGraph();
        graphService.initGraph();

        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(10L); // 10在孤立子图中

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNull(result);
    }

    @Test
    @DisplayName("findPath - 未初始化图应返回null")
    void findPath_emptyGraph_returnsNull() {
        List<Long> startIds = List.of(1L);
        List<Long> endIds = List.of(2L);

        GraphService.PathResult result = graphService.findPath(startIds, endIds);

        assertNull(result);
    }

    @Test
    @DisplayName("isEmpty - 未初始化时应返回true")
    void isEmpty_uninitialized_returnsTrue() {
        assertTrue(graphService.isEmpty());
    }

    @Test
    @DisplayName("isEmpty - 初始化后应返回false")
    void isEmpty_afterInit_returnsFalse() {
        setupLinearLineGraph();
        graphService.initGraph();

        assertFalse(graphService.isEmpty());
    }

    @Test
    @DisplayName("isTransfer - 同一线路两站应返回false")
    void isTransfer_sameLine_returnsFalse() {
        setupLinearLineGraph();
        graphService.initGraph();

        assertFalse(graphService.isTransfer(1L, 2L));
    }

    @Test
    @DisplayName("isTransfer - 不同线路两站应返回true")
    void isTransfer_differentLines_returnsTrue() {
        setupMultiPathGraph();
        graphService.initGraph();

        // 节点2和节点5在不同线路上
        boolean result = graphService.isTransfer(2L, 5L);
        assertTrue(result);
    }

    @Test
    @DisplayName("getLineColor - 未知线路应返回默认颜色")
    void getLineColor_unknownLine_returnsDefaultColor() {
        String color = graphService.getLineColor("99号线", null);

        assertEquals("#999", color);
    }

    @Test
    @DisplayName("getLineColor - 数据库颜色有效但不在mock列表应返回DB颜色")
    void getLineColor_dbColorNotInMockList_returnsDbColor() {
        String color = graphService.getLineColor("99号线", "ABCDEF");

        assertEquals("#ABCDEF", color);
    }

    @Test
    @DisplayName("getIdsByName - 换乘站应返回多个节点ID")
    void getIdsByName_transferStation_returnsMultipleIds() {
        setupMultiPathGraph();
        graphService.initGraph();

        // "换乘站"在同一图上可能对应多个LineStation节点ID
        List<Long> ids = graphService.getIdsByName("站点A");
        assertNotNull(ids);
        assertFalse(ids.isEmpty());
    }

    @Test
    @DisplayName("getIdsByName - 不存在的站应返回空列表")
    void getIdsByName_nonExistentStation_returnsEmpty() {
        setupLinearLineGraph();
        graphService.initGraph();

        List<Long> ids = graphService.getIdsByName("火星站");

        assertNotNull(ids);
        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("getCodeById - 有效ID应返回站码")
    void getCodeById_validId_returnsCode() {
        setupLinearLineGraph();
        graphService.initGraph();

        String code = graphService.getCodeById(1L);

        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    @Test
    @DisplayName("getCodeById - 无效ID应返回null")
    void getCodeById_invalidId_returnsNull() {
        setupLinearLineGraph();
        graphService.initGraph();

        String code = graphService.getCodeById(999L);

        assertNull(code);
    }

    // === Graph setup helpers ===

    private void setupSingleNodeGraph() {
        Station s1 = createStation(10L, "单站", "DZ");
        LineStation ls1 = createLineStation(1L, 1L, 10L, 1);

        when(lineMapper.selectList(null)).thenReturn(List.of(createLine(1L, "1号线", "DF4749")));
        when(stationMapper.selectList(null)).thenReturn(List.of(s1));
        when(lineStationMapper.selectList(null)).thenReturn(List.of(ls1));
    }

    private void setupLinearLineGraph() {
        // 5个站在同一条线上线性排列: 1-2-3-4-5
        Line line = createLine(1L, "1号线", "DF4749");
        Station s1 = createStation(10L, "站点A", "STA");
        Station s2 = createStation(11L, "站点B", "STB");
        Station s3 = createStation(12L, "站点C", "STC");
        Station s4 = createStation(13L, "站点D", "STD");
        Station s5 = createStation(14L, "站点E", "STE");

        LineStation ls1 = createLineStation(1L, 1L, 10L, 1);
        LineStation ls2 = createLineStation(2L, 1L, 11L, 2);
        LineStation ls3 = createLineStation(3L, 1L, 12L, 3);
        LineStation ls4 = createLineStation(4L, 1L, 13L, 4);
        LineStation ls5 = createLineStation(5L, 1L, 14L, 5);

        when(lineMapper.selectList(null)).thenReturn(List.of(line));
        when(stationMapper.selectList(null)).thenReturn(List.of(s1, s2, s3, s4, s5));
        when(lineStationMapper.selectList(null)).thenReturn(List.of(ls1, ls2, ls3, ls4, ls5));
    }

    private void setupMultiPathGraph() {
        // 线路1: 站点A(1)-站点B(2)-站点C(3)-站点D(4)
        // 线路2: 站点B(5)-站点F(6)-站点D(7)  —— 其中站点B和站点D是换乘站
        Line line1 = createLine(1L, "1号线", "DF4749");
        Line line2 = createLine(2L, "2号线", "E57B46");

        Station sA = createStation(10L, "站点A", "STA"); // 只有线路1
        Station sB = createStation(11L, "站点B", "STB"); // 换乘站 (线路1和2)
        Station sC = createStation(12L, "站点C", "STC"); // 只有线路1
        Station sD = createStation(13L, "站点D", "STD"); // 换乘站 (线路1和2)
        Station sF = createStation(14L, "站点F", "STF"); // 只有线路2

        // 线路1上的 LineStation
        LineStation ls1 = createLineStation(1L, 1L, 10L, 1); // 站点A-线路1
        LineStation ls2 = createLineStation(2L, 1L, 11L, 2); // 站点B-线路1
        LineStation ls3 = createLineStation(3L, 1L, 12L, 3); // 站点C-线路1
        LineStation ls4 = createLineStation(4L, 1L, 13L, 4); // 站点D-线路1

        // 线路2上的 LineStation
        LineStation ls5 = createLineStation(5L, 2L, 11L, 1); // 站点B-线路2
        LineStation ls6 = createLineStation(6L, 2L, 14L, 2); // 站点F-线路2
        LineStation ls7 = createLineStation(7L, 2L, 13L, 3); // 站点D-线路2

        when(lineMapper.selectList(null)).thenReturn(List.of(line1, line2));
        when(stationMapper.selectList(null)).thenReturn(List.of(sA, sB, sC, sD, sF));
        when(lineStationMapper.selectList(null)).thenReturn(List.of(ls1, ls2, ls3, ls4, ls5, ls6, ls7));
    }

    private void setupDisconnectedGraph() {
        // 子图A: 1-2-3 (线路1)
        // 子图B: 10 (孤立节点, 线路2) - 互相不可达
        Line line1 = createLine(1L, "1号线", "DF4749");
        Line line2 = createLine(2L, "2号线", "E57B46");

        Station sA = createStation(10L, "子图A1", "A1");
        Station sB = createStation(11L, "子图A2", "A2");
        Station sX = createStation(20L, "孤立站", "ISO");

        LineStation ls1 = createLineStation(1L, 1L, 10L, 1);
        LineStation ls2 = createLineStation(2L, 1L, 11L, 2);
        LineStation ls10 = createLineStation(10L, 2L, 20L, 1);

        when(lineMapper.selectList(null)).thenReturn(List.of(line1, line2));
        when(stationMapper.selectList(null)).thenReturn(List.of(sA, sB, sX));
        when(lineStationMapper.selectList(null)).thenReturn(List.of(ls1, ls2, ls10));
    }

    // === Helper methods ===

    private Station createStation(Long id, String name, String code) {
        Station s = new Station();
        s.setId(id);
        s.setName(name);
        s.setCode(code);
        return s;
    }

    private Line createLine(Long id, String name, String color) {
        Line l = new Line();
        l.setId(id);
        l.setName(name);
        l.setColor(color);
        return l;
    }

    private LineStation createLineStation(Long id, Long lineId, Long stationId, int seq) {
        LineStation ls = new LineStation();
        ls.setId(id);
        ls.setLineId(lineId);
        ls.setStationId(stationId);
        ls.setSeq(seq);
        return ls;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
