package com.greatprice.common.web;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 用字符串解析金额，避免经过 double。 */
public final class MoneyText {

    public static final BigDecimal MAX = new BigDecimal("99999999.99");

    private MoneyText() {
    }

    public static BigDecimal parse(String raw, String label, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw new BizException(ErrorCode.BAD_REQUEST, label + "不能为空");
            }
            return null;
        }
        BigDecimal value;
        try {
            value = new BigDecimal(raw.strip());
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "格式不正确");
        }
        if (value.scale() > 2) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "最多保留两位小数");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(MAX) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "必须在 0 到 99999999.99 之间");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
