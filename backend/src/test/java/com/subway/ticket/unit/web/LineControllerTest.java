package com.subway.ticket.unit.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.subway.ticket.domain.Line;
import com.subway.ticket.repository.LineMapper;
import com.subway.ticket.web.LineController;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("线路控制器单元测试")
class LineControllerTest {

    @Mock
    private LineMapper lineMapper;

    private LineController lineController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        lineController = new LineController(lineMapper);
        a.close();
    }

    @Test
    @DisplayName("lines - 应返回活跃线路列表和200")
    void lines_returnsActiveLinesWith200() {
        Line line1 = new Line();
        line1.setId(1L);
        line1.setName("1号线");
        line1.setColor("DF4749");

        Line line2 = new Line();
        line2.setId(2L);
        line2.setName("2号线");
        line2.setColor("E57B46");

        when(lineMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(line1, line2));

        ResponseEntity<List<Line>> response = lineController.lines();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("1号线", response.getBody().get(0).getName());
        assertEquals("2号线", response.getBody().get(1).getName());
    }

    @Test
    @DisplayName("lines - 无活跃线路应返回空列表")
    void lines_noActiveLines_returnsEmptyList() {
        when(lineMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<Line>> response = lineController.lines();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}
