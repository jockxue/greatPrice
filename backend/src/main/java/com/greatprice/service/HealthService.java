package com.greatprice.service;

import com.greatprice.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private static final int DB_VALID_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public HealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthResponse check() {
        return new HealthResponse("UP", checkDatabase(), OffsetDateTime.now());
    }

    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_VALID_TIMEOUT_SECONDS) ? "UP" : "DOWN";
        } catch (Exception ex) {
            log.warn("数据库连通性检查失败: {}", ex.getMessage());
            return "DOWN";
        }
    }
}
