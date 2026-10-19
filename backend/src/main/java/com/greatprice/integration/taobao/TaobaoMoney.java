package com.greatprice.integration.taobao;

import com.greatprice.common.web.MoneyText;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 只接受接口返回的十进制金额。不把佣金比率或说明文字当成价格。 */
public final class TaobaoMoney {

    public static final String OK = "OK";
    public static final String MISSING = "MISSING";
    public static final String INVALID = "INVALID";

    private TaobaoMoney() {
    }

    public record Parsed(String status, String value) {
    }

    public static Parsed parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Parsed(MISSING, null);
        }
        String text = raw.strip();
        if (!text.matches("\\d{1,8}(\\.\\d{1,2})?")) {
            return new Parsed(INVALID, null);
        }
        BigDecimal value = new BigDecimal(text);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MoneyText.MAX) > 0) {
            return new Parsed(INVALID, null);
        }
        return new Parsed(OK, value.setScale(2, RoundingMode.UNNECESSARY).toPlainString());
    }
}
