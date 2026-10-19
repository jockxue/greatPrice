package com.greatprice.integration.taobao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 淘宝开放平台 TOP 签名。
 * 规则见官方文档《调用 API 基础说明》：参数名 ASCII 排序后拼接，HMAC-SHA256 用 App Secret 作密钥，摘要转大写十六进制。
 * https://developer.alibaba.com/docs/doc.htm?articleId=101617&amp;docType=1
 */
public final class TaobaoSigner {

    public static final String HMAC_SHA256 = "hmac-sha256";
    public static final String MD5 = "md5";

    private TaobaoSigner() {
    }

    public static String sign(Map<String, String> params, String secret, String signMethod) {
        String content = canonical(params);
        try {
            byte[] digest = MD5.equals(signMethod)
                    ? MessageDigest.getInstance("MD5").digest((secret + content + secret).getBytes(StandardCharsets.UTF_8))
                    : hmacSha256(content, secret);
            return hex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("淘宝签名算法不可用");
        }
    }

    public static String canonical(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty() && !"sign".equals(key)) {
                sorted.put(key, value);
            }
        });
        StringBuilder builder = new StringBuilder();
        sorted.forEach((key, value) -> builder.append(key).append(value));
        return builder.toString();
    }

    private static byte[] hmacSha256(String content, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString().toUpperCase();
    }
}
