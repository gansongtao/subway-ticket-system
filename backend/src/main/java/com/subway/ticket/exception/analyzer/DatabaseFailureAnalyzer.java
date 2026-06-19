package com.subway.ticket.exception.analyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.sql.SQLException;

/**
 * 自定义数据库连接失败分析器
 * 当数据库配置错误（如密码错误、URL无法连接等）时，提供清晰的报错信息。
 */
public class DatabaseFailureAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    @Override
    public FailureAnalysis analyze(Throwable rootFailure, SQLException cause) {
        String description = "数据库操作失败: " + cause.getMessage();
        String action = "请检查 SQL 语法或数据库表结构是否与代码定义一致。";

        String sqlState = cause.getSQLState();

        if (sqlState == null) {
            return new FailureAnalysis(description, action, cause);
        }

        // 细化错误处理
        switch (sqlState) {
            case "28000" -> {
                description = "数据库认证失败：用户名或密码错误。";
                action = "请检查 application-db.yml 中的 username 和 password 配置，并确保密码正确。";
            }
            case "08001", "08004" -> {
                description = "无法建立数据库连接：URL 错误或数据库服务器拒绝连接。";
                action = "1. 请检查 application-db.yml 中的 url 配置。\n2. 确保 MySQL 服务已启动且网络可达。";
            }
            case "42S02" -> {
                description = "数据库表不存在: " + cause.getMessage();
                action = "请确保已执行完整的 schema.sql 以创建所有必需的表。";
            }
            case "42S22" -> {
                description = "数据库列不存在 (Unknown column): " + cause.getMessage();
                action = "实体类字段与数据库列不匹配。如果是刚 pull 的代码，请重启应用以触发 DataInitializer 的自动升级逻辑。";
            }
            case "08S01" -> {
                description = "数据库通信链路故障 (Communications link failure)。";
                action = "可能是数据库服务中途断开，请检查 MySQL 服务状态或网络稳定性。";
            }
        }

        return new FailureAnalysis(description, action, cause);
    }
}
