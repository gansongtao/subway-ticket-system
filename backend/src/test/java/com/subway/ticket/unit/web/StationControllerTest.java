package com.subway.ticket.unit.web;

import com.subway.ticket.domain.Station;
import com.subway.ticket.service.StationService;
import com.subway.ticket.web.StationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("站点控制器单元测试")
class StationControllerTest {

    @Mock
    private StationService stationService;

    private StationController stationController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        stationController = new StationController(stationService);
        a.close();
    }

    @Test
    @DisplayName("stations - 按线路ID查询应返回站点列表")
    void stations_byLineId_returnsStations() {
        Station s1 = new Station();
        s1.setId(1L);
        s1.setName("龙翔桥");

        Station s2 = new Station();
        s2.setId(2L);
        s2.setName("凤起路");

        when(stationService.getStationsByLine(1L))
                .thenReturn(Arrays.asList(s1, s2));

        ResponseEntity<List<Station>> response = stationController.stations(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(stationService, times(1)).getStationsByLine(1L);
    }

    @Test
    @DisplayName("stations - 无效线路ID应返回空列表")
    void stations_invalidLineId_returnsEmptyList() {
        when(stationService.getStationsByLine(999L))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<Station>> response = stationController.stations(999L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("all - 应返回全部站点")
    void all_returnsAllStations() {
        Station s1 = new Station();
        s1.setId(1L);
        s1.setName("站点A");

        when(stationService.getAllStations()).thenReturn(List.of(s1));

        ResponseEntity<List<Station>> response = stationController.all();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(stationService, times(1)).getAllStations();
    }

    @Test
    @DisplayName("search - 关键字搜索应返回匹配结果")
    void search_keyword_returnsMatchingStations() {
        Station s1 = new Station();
        s1.setId(1L);
        s1.setName("长春站");

        when(stationService.searchStations("长春"))
                .thenReturn(List.of(s1));

        ResponseEntity<List<Station>> response = stationController.search("长春");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("长春站", response.getBody().getFirst().getName());
        verify(stationService, times(1)).searchStations("长春");
    }

    @Test
    @DisplayName("search - 无关键字时应正常返回")
    void search_noKeyword_returnsResults() {
        when(stationService.searchStations(null))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<Station>> response = stationController.search(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
