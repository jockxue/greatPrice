package com.greatprice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.greatprice.dto.admin.CategoryAdminResponse;
import com.greatprice.entity.Category;
import com.greatprice.mapper.CategoryMapper;
import com.greatprice.security.AdminActors;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryAdminService {

    private final CategoryMapper categoryMapper;
    private final AdminActors adminActors;

    public CategoryAdminService(CategoryMapper categoryMapper, AdminActors adminActors) {
        this.categoryMapper = categoryMapper;
        this.adminActors = adminActors;
    }

    public List<CategoryAdminResponse> list() {
        adminActors.current();
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .map(category -> new CategoryAdminResponse(category.getId(), category.getName(), category.getCode(), category.getStatus()))
                .toList();
    }
}
