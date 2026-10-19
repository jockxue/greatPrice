package com.greatprice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TaobaoJobQuery(
        @Min(value = 1, message = "不能小于 1") Integer page,
        @Min(value = 1, message = "不能小于 1") @Max(value = 50, message = "不能超过 50") Integer size,
        @Size(max = 16, message = "不能超过 16 个字符") String status,
        @Size(max = 64, message = "不能超过 64 个字符") String jobName) {

    public TaobaoJobQuery {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        status = status == null || status.isBlank() ? null : status.strip();
        jobName = jobName == null || jobName.isBlank() ? null : jobName.strip();
    }
}
