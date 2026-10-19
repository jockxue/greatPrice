package com.greatprice.integration.taobao;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** application/x-www-form-urlencoded，空格编码为 +，与 TOP 文档示例一致。 */
public final class TaobaoForm {

    private TaobaoForm() {
    }

    public static String encode(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        params.forEach((key, value) -> {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return builder.toString();
    }
}
