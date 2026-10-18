package com.greatprice.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应数据，作为 {@link ApiResponse#data()} 返回。page 从 1 开始。
 */
public record PageResult<T>(List<T> records, long total, long page, long size) {

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> records = page.getRecords().stream().map(converter).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
