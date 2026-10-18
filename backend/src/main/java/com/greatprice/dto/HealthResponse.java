package com.greatprice.dto;

import java.time.OffsetDateTime;

/**
 * @param status   应用状态，能响应即为 UP
 * @param database 数据库连通状态：UP / DOWN
 */
public record HealthResponse(String status, String database, OffsetDateTime time) {
}
