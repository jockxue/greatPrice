package com.greatprice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * GET /api/v1/deals 查询参数。keyword 去除首尾空白后为空时视为未传。
 *
 * @param featured 为 true 时只返回编辑精选
 */
public record DealQueryRequest(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = DealQueryRequest.MAX_SIZE, message = "不能超过 50") Integer size,
        @Positive(message = "必须为正整数") Long categoryId,
        @Size(max = DealQueryRequest.MAX_KEYWORD_LENGTH, message = "不能超过 50 个字符") String keyword,
        Boolean featured,
        DealSortBy sortBy) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;
    public static final int MAX_KEYWORD_LENGTH = 50;

    public DealQueryRequest {
        page = page == null ? 1 : page;
        size = size == null ? DEFAULT_SIZE : size;
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip();
        sortBy = sortBy == null ? DealSortBy.LATEST : sortBy;
    }
}
