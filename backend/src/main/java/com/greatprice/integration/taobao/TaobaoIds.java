package com.greatprice.integration.taobao;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;
import com.greatprice.entity.DealSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TaobaoIds {

    public static final String PLATFORM = "TAOBAO";
    public static final String OFFER_PREFIX = "LISTING:";

    private TaobaoIds() {
    }

    public static String requireItemId(String raw) {
        if (raw == null || !raw.strip().matches("[A-Za-z0-9_-]{1,120}")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "淘宝商品 ID 格式不正确");
        }
        return raw.strip();
    }

    public static String productCode(String itemId) {
        String code = "TB-" + itemId;
        if (code.length() <= 64) {
            return code;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(itemId.getBytes(StandardCharsets.UTF_8));
            String hashed = "TB-" + TaobaoSigner.hex(digest).substring(0, 40);
            if (DealSource.isDemoProductCode(hashed)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "不能把淘宝商品写成演示商品");
            }
            return hashed;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用");
        }
    }

    /** 商品详情接口没有 coupon_id，同一商品的未区分优惠用这个稳定键做幂等，不比较价格。 */
    public static String offerKey(String itemId) {
        return OFFER_PREFIX + itemId;
    }
}
