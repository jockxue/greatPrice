package com.greatprice.common.web;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;

import java.net.URI;
import java.net.URISyntaxException;

/** 管理端写入链接时只接受带主机名的 http/https。公开接口仍只回传 https。 */
public final class WebUrls {

    private WebUrls() {
    }

    public static String normalize(String raw, String label, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.strip();
        if (value.length() > maxLength) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "长度超出限制");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "格式不正确");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "只允许 http 或 https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, label + "格式不正确");
        }
        return uri.toString();
    }
}
