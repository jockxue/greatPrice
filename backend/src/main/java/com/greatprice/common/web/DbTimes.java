package com.greatprice.common.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** 数据库 DATETIME 按北京时间存储，与 JDBC serverTimezone=Asia/Shanghai 一致。 */
public final class DbTimes {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private DbTimes() {
    }

    /** 库字段是秒级 DATETIME。截断到秒，避免四舍五入后比当前时间还晚，导致刚发布的好价暂时不可见。 */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    public static OffsetDateTime toOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toOffsetDateTime();
    }

    public static LocalDateTime toLocal(OffsetDateTime time) {
        return time == null ? null : time.atZoneSameInstant(ZONE).toLocalDateTime();
    }
}
