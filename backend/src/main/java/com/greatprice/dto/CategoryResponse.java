package com.greatprice.dto;

/**
 * @param icon 分类图标 URL，未配置时为 null，前端使用默认图标
 */
public record CategoryResponse(Long id, String name, String code, String icon) {
}
