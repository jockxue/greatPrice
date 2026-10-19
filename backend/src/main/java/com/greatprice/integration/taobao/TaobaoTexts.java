package com.greatprice.integration.taobao;

public final class TaobaoTexts {

    private TaobaoTexts() {
    }

    public static String redact(String text, String secret, String session) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String value = text;
        value = mask(value, secret);
        value = mask(value, session);
        return value.length() > 300 ? value.substring(0, 300) : value;
    }

    public static String limit(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String mask(String text, String secret) {
        if (secret == null) {
            return text;
        }
        String value = secret.strip();
        if (value.length() < 8) {
            return text;
        }
        return text.replace(value, "***");
    }
}
