package com.greatprice.dto.admin;

import jakarta.validation.constraints.NotNull;

public record VersionRequest(@NotNull(message = "版本号不能为空") Integer version) {
}
