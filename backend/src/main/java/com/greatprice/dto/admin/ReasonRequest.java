package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
        @NotBlank(message = "原因不能为空") @Size(max = 500, message = "不能超过 500 个字符") String reason,
        @NotNull(message = "版本号不能为空") Integer version) {
}
