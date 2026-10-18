package com.greatprice.dto;

/** 好价列表排序方式白名单，未传时为 LATEST。 */
public enum DealSortBy {
    /** 发布时间倒序 */
    LATEST,
    PRICE_ASC,
    PRICE_DESC
}
