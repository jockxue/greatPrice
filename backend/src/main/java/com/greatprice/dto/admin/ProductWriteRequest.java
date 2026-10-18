package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 没有 status 和 deleted，启用、停用、删除走单独接口。 */
public record ProductWriteRequest(
        @NotBlank(message = "商品编码不能为空") @Size(max = 64, message = "不能超过 64 个字符") String productCode,
        @NotBlank(message = "商品名称不能为空") @Size(max = 200, message = "不能超过 200 个字符") String title,
        @Size(max = 64, message = "不能超过 64 个字符") String brand,
        @NotNull(message = "分类不能为空") @Positive(message = "分类必须为正整数") Long categoryId,
        @Size(max = 512, message = "不能超过 512 个字符") String imageUrl,
        @Size(max = 500, message = "不能超过 500 个字符") String specification,
        @Size(max = 5000, message = "不能超过 5000 个字符") String description) {
}
