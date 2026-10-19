package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TaobaoImportRequest(
        @NotBlank(message = "商品 ID 不能为空") @Size(max = 120, message = "不能超过 120 个字符") String itemId,
        @NotNull(message = "分类不能为空") @Positive(message = "分类必须为正整数") Long categoryId) {
}
