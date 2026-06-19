package com.subway.ticket.unit.web;

import com.subway.ticket.dto.FareQuote;
import com.subway.ticket.service.FareService;
import com.subway.ticket.web.FareController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("票价控制器单元测试")
class FareControllerTest {

    @Mock
    private FareService fareService;

    private FareController fareController;

    @BeforeEach
    void setUp() throws Exception {
        AutoCloseable a = MockitoAnnotations.openMocks(this);
        fareController = new FareController(fareService);
        a.close();
    }

    @Test
    @DisplayName("quote - 正常站码应返回200和票价信息")
    void quote_validStationCodes_returns200WithFareQuote() {
        FareQuote mockQuote = new FareQuote("LXQ", "FQL", 1,
                new BigDecimal("2.00"), "HANGZHOU_RULE",
                List.of("LXQ", "FQL"), List.of());
        when(fareService.calculateFare("LXQ", "FQL")).thenReturn(mockQuote);

        ResponseEntity<FareQuote> response = fareController.quote("LXQ", "FQL");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("LXQ", response.getBody().getFrom());
        assertEquals("FQL", response.getBody().getTo());
        assertEquals(new BigDecimal("2.00"), response.getBody().getPrice());
        verify(fareService, times(1)).calculateFare("LXQ", "FQL");
    }

    @Test
    @DisplayName("quote - 出发站不存在应返回400含STATION_NOT_FOUND")
    void quote_fromStationNotFound_returns400WithError() {
        FareQuote errorQuote = new FareQuote("INVALID", "FQL", 0,
                BigDecimal.ZERO, "STATION_NOT_FOUND", null, null);
        when(fareService.calculateFare("INVALID", "FQL")).thenReturn(errorQuote);

        ResponseEntity<FareQuote> response = fareController.quote("INVALID", "FQL");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STATION_NOT_FOUND", response.getBody().getMode());
    }

    @Test
    @DisplayName("quote - 到达站不存在应返回400含STATION_NOT_FOUND")
    void quote_toStationNotFound_returns400WithError() {
        FareQuote errorQuote = new FareQuote("LXQ", "INVALID", 0,
                BigDecimal.ZERO, "STATION_NOT_FOUND", null, null);
        when(fareService.calculateFare("LXQ", "INVALID")).thenReturn(errorQuote);

        ResponseEntity<FareQuote> response = fareController.quote("LXQ", "INVALID");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("STATION_NOT_FOUND", response.getBody().getMode());
    }

    @Test
    @DisplayName("quote - 两站不可达应返回400含UNREACHABLE")
    void quote_unreachableStations_returns400WithUnreachable() {
        FareQuote errorQuote = new FareQuote("STA", "STB", 0,
                BigDecimal.ZERO, "UNREACHABLE", null, null);
        when(fareService.calculateFare("STA", "STB")).thenReturn(errorQuote);

        ResponseEntity<FareQuote> response = fareController.quote("STA", "STB");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNREACHABLE", response.getBody().getMode());
    }

    @Test
    @DisplayName("quote - NODES_NOT_FOUND应返回400")
    void quote_nodesNotFound_returns400() {
        FareQuote errorQuote = new FareQuote("STA", "STB", 0,
                BigDecimal.ZERO, "NODES_NOT_FOUND", null, null);
        when(fareService.calculateFare("STA", "STB")).thenReturn(errorQuote);

        ResponseEntity<FareQuote> response = fareController.quote("STA", "STB");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("quote - from含HTML标签应被转义后正常处理")
    void quote_fromWithHtmlTags_escapesInput() {
        // HTML标签会被转义为 &lt;script&gt;，然后因为不匹配正则 ^[a-zA-Z0-9\\-]+$ 而返回400
        ResponseEntity<FareQuote> response = fareController.quote("<script>", "FQL");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        // 验证 Service 未被调用
        verify(fareService, never()).calculateFare(anyString(), anyString());
    }

    @Test
    @DisplayName("quote - from含非法字符应返回400")
    void quote_fromWithIllegalCharacters_returns400() {
        // 分号和空格不在允许的字符集中 [a-zA-Z0-9\-]
        ResponseEntity<FareQuote> response = fareController.quote("test;DROP", "FQL");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        // 验证 Service 未被调用
        verify(fareService, never()).calculateFare(anyString(), anyString());
    }

    @Test
    @DisplayName("quote - from含中文应返回400")
    void quote_fromWithChinese_returns400() {
        ResponseEntity<FareQuote> response = fareController.quote("长春站", "FQL");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(fareService, never()).calculateFare(anyString(), anyString());
    }

    @Test
    @DisplayName("quote - from为空字符串应返回400")
    void quote_emptyFrom_returns400() {
        // 空字符串不匹配正则 ^[a-zA-Z0-9\\-]+$ (至少需要一个字符)
        ResponseEntity<FareQuote> response = fareController.quote("", "FQL");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(fareService, never()).calculateFare(anyString(), anyString());
    }

    @Test
    @DisplayName("quote - Service抛出RuntimeException应返回500")
    void quote_serviceThrowsRuntimeException_returns500() {
        when(fareService.calculateFare("LXQ", "FQL"))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<FareQuote> response = fareController.quote("LXQ", "FQL");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("quote - 站码含连字符应正常处理返回200")
    void quote_stationCodeWithHyphen_returns200() {
        FareQuote mockQuote = new FareQuote("line-1-A", "line-1-B", 3,
                new BigDecimal("3.00"), "HANGZHOU_RULE",
                List.of("line-1-A", "line-1-B"), List.of());
        when(fareService.calculateFare("line-1-A", "line-1-B")).thenReturn(mockQuote);

        ResponseEntity<FareQuote> response = fareController.quote("line-1-A", "line-1-B");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("quote - 站码纯数字应正常处理返回200")
    void quote_numericStationCode_returns200() {
        FareQuote mockQuote = new FareQuote("001002", "001003", 1,
                new BigDecimal("2.00"), "HANGZHOU_RULE",
                List.of("001002", "001003"), List.of());
        when(fareService.calculateFare("001002", "001003")).thenReturn(mockQuote);

        ResponseEntity<FareQuote> response = fareController.quote("001002", "001003");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("quote - to含非法字符应返回400（to也经过校验）")
    void quote_toWithIllegalCharacters_returns400() {
        ResponseEntity<FareQuote> response = fareController.quote("LXQ", "<script>");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(fareService, never()).calculateFare(anyString(), anyString());
    }
}
