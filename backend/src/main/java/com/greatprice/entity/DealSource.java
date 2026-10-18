package com.greatprice.entity;

import java.util.Set;

/** 与 deal.source_type、price_history.source_type 使用同一组取值。 */
public final class DealSource {

    public static final String DEMO = "DEMO";
    public static final String EDITOR = "EDITOR";
    public static final String USER_SUBMISSION = "USER_SUBMISSION";
    public static final String AFFILIATE = "AFFILIATE";

    public static final Set<String> ALL = Set.of(DEMO, EDITOR, USER_SUBMISSION, AFFILIATE);

    /** 可以写入真实价格历史的来源。演示数据不在其中。 */
    public static final Set<String> VERIFIABLE = Set.of(EDITOR, USER_SUBMISSION, AFFILIATE);

    public static final String DEMO_CODE_PREFIX = "DEMO-";

    private DealSource() {
    }

    public static boolean isDemoProductCode(String productCode) {
        return productCode != null && productCode.startsWith(DEMO_CODE_PREFIX);
    }
}
