package com.subway.ticket.unit.exception.analyzer;

import com.subway.ticket.exception.analyzer.DatabaseFailureAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("数据库失败分析器单元测试")
class DatabaseFailureAnalyzerTest {

    private DatabaseFailureAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DatabaseFailureAnalyzer();
    }

    @Test
    @DisplayName("analyze - SQLState 28000应返回认证失败描述")
    void analyze_sqlState28000_returnsAuthFailureDescription() {
        SQLException cause = new SQLException("Access denied for user 'root'", "28000");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("认证失败"));
        assertTrue(result.getDescription().contains("用户名或密码错误"));
        assertTrue(result.getAction().contains("application-db.yml"));
    }

    @Test
    @DisplayName("analyze - SQLState 08001应返回连接失败描述")
    void analyze_sqlState08001_returnsConnectionFailureDescription() {
        SQLException cause = new SQLException("Could not connect to server", "08001");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("无法建立数据库连接"));
        assertTrue(result.getAction().contains("MySQL"));
    }

    @Test
    @DisplayName("analyze - SQLState 08004应返回连接拒绝描述")
    void analyze_sqlState08004_returnsConnectionRefusedDescription() {
        SQLException cause = new SQLException("Connection refused", "08004");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("无法建立数据库连接"));
        assertTrue(result.getAction().contains("MySQL"));
    }

    @Test
    @DisplayName("analyze - SQLState 42S02应返回表不存在描述")
    void analyze_sqlState42S02_returnsTableNotFoundDescription() {
        SQLException cause = new SQLException("Table 'subway.orders' doesn't exist", "42S02");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("表不存在"));
        assertTrue(result.getAction().contains("schema.sql"));
    }

    @Test
    @DisplayName("analyze - SQLState 42S22应返回列不存在描述")
    void analyze_sqlState42S22_returnsColumnNotFoundDescription() {
        SQLException cause = new SQLException("Unknown column 'new_column' in 'field list'", "42S22");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("列不存在"));
        assertTrue(result.getAction().contains("DataInitializer"));
    }

    @Test
    @DisplayName("analyze - SQLState 08S01应返回通信故障描述")
    void analyze_sqlState08S01_returnsCommunicationFailureDescription() {
        SQLException cause = new SQLException("Communications link failure", "08S01");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("通信链路故障"));
        assertTrue(result.getAction().contains("MySQL 服务状态"));
    }

    @Test
    @DisplayName("analyze - 未知SQLState应返回通用描述")
    void analyze_unknownSqlState_returnsGenericDescription() {
        SQLException cause = new SQLException("Some unknown error", "99999");

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        // 通用描述包含原始错误消息
        assertTrue(result.getDescription().contains("数据库操作失败"));
        assertTrue(result.getDescription().contains("Some unknown error"));
    }

    @Test
    @DisplayName("analyze - null SQLState应返回带原始消息的描述")
    void analyze_nullSqlState_returnsDescriptionWithOriginalMessage() {
        SQLException cause = new SQLException("Database error without SQL state", (String) null);

        FailureAnalysis result = analyzer.analyze(null, cause);

        assertNotNull(result);
        assertTrue(result.getDescription().contains("数据库操作失败"));
        assertTrue(result.getDescription().contains("Database error without SQL state"));
        assertTrue(result.getAction().contains("SQL 语法"));
    }
}
