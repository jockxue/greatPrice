package com.greatprice.integration.taobao;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;

import java.net.URI;

public final class TaobaoLinks {

    public static final String MINI_PROGRAM_NOTE =
            "官方文档写明 taobao.tbk.privilege.get 的 mini_program_link 暂未对外开放。当前链接不能直接在微信小程序内打开，需复制到浏览器或淘宝 App。";

    private TaobaoLinks() {
    }

    public static String image(String raw) {
        return absolute(raw, true, 512, true);
    }

    public static String purchase(String raw) {
        return absolute(raw, false, 1024, false);
    }

    /** 非空但不合法时抛错，避免把残缺结果存成推广链接。空白表示接口没返回该字段。 */
    public static String promotion(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = absolute(raw, true, 1024, false);
        if (value == null) {
            throw new BizException(ErrorCode.UPSTREAM_ERROR, "推广链接为空、协议异常或超出站内长度，未保存");
        }
        return value;
    }

    private static String absolute(String raw, boolean httpsOnly, int maxLength, boolean image) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        if (value.startsWith("//")) {
            value = "https:" + value;
        }
        if (value.length() > maxLength) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (Exception ex) {
            return null;
        }
        String scheme = uri.getScheme();
        boolean schemeOk = "https".equalsIgnoreCase(scheme) || (!httpsOnly && "http".equalsIgnoreCase(scheme));
        if (!schemeOk || uri.getHost() == null || uri.getUserInfo() != null || !allowedHost(uri.getHost(), image)) {
            return null;
        }
        return value;
    }

    private static boolean allowedHost(String host, boolean image) {
        if (taobaoHost(host)) {
            return true;
        }
        if (!image) {
            return false;
        }
        String value = host.toLowerCase();
        return value.equals("alicdn.com") || value.endsWith(".alicdn.com");
    }

    static boolean taobaoHost(String host) {
        String value = host.toLowerCase();
        return value.equals("taobao.com")
                || value.equals("tmall.com")
                || value.endsWith(".taobao.com")
                || value.endsWith(".tmall.com");
    }
}
