package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.dto.CategoryResponse;
import com.greatprice.entity.Category;
import com.greatprice.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /** 只返回启用的分类，按 sort_order 升序。 */
    public List<CategoryResponse> listEnabled() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, Category.STATUS_ENABLED)
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getCode(), c.getIcon()))
                .toList();
    }
}
