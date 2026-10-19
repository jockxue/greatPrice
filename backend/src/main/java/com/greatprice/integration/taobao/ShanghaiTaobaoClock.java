package com.greatprice.integration.taobao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ShanghaiTaobaoClock implements TaobaoClock {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String timestamp() {
        return LocalDateTime.now(com.greatprice.common.web.DbTimes.ZONE).format(FORMATTER);
    }
}
