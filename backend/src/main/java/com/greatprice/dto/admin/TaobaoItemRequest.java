package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaobaoItemRequest(
        @NotBlank(message = "商品 ID 不能为空") @Size(max = 120, message = "不能超过 120 个字符") String itemId) {
}
