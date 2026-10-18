package com.greatprice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.api.PageResult;
import com.greatprice.common.exception.BizException;
import com.greatprice.common.web.DbTimes;
import com.greatprice.common.web.WebUrls;
import com.greatprice.dto.admin.ProductQuery;
import com.greatprice.dto.admin.ProductResponse;
import com.greatprice.dto.admin.ProductWriteRequest;
import com.greatprice.entity.Category;
import com.greatprice.entity.DealSource;
import com.greatprice.entity.Product;
import com.greatprice.entity.ProductRow;
import com.greatprice.mapper.CategoryMapper;
import com.greatprice.mapper.ProductMapper;
import com.greatprice.security.AdminActors;
import com.greatprice.security.AdminUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductAdminService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final AuditService auditService;
    private final AdminActors adminActors;

    public ProductAdminService(ProductMapper productMapper, CategoryMapper categoryMapper,
                               AuditService auditService, AdminActors adminActors) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.auditService = auditService;
        this.adminActors = adminActors;
    }

    public PageResult<ProductResponse> page(ProductQuery query) {
        adminActors.current();
        if (query.status() != null
                && !Product.STATUS_ACTIVE.equals(query.status())
                && !Product.STATUS_INACTIVE.equals(query.status())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品状态不合法");
        }
        IPage<ProductRow> page = productMapper.selectAdminPage(
                Page.of(query.page(), query.size()),
                query.categoryId(),
                query.status(),
                DealService.escapeLike(query.keyword()));
        return PageResult.of(page, this::toResponse);
    }

    public ProductResponse get(Long id) {
        adminActors.current();
        return toResponse(require(id));
    }

    @Transactional
    public ProductResponse create(ProductWriteRequest request) {
        AdminUserDetails actor = adminActors.current();
        requireEnabledCategory(request.categoryId());
        String code = request.productCode().strip();
        ensureCodeAvailable(code, null);
        Product product = new Product();
        apply(product, request, code);
        product.setStatus(Product.STATUS_ACTIVE);
        product.setDeleted(0);
        try {
            productMapper.insert(product);
        } catch (DataIntegrityViolationException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品编码已存在");
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRODUCT_CREATE, "PRODUCT", product.getId(), code);
        return toResponse(require(product.getId()));
    }

    @Transactional
    public ProductResponse update(Long id, ProductWriteRequest request) {
        AdminUserDetails actor = adminActors.current();
        require(id);
        requireEnabledCategory(request.categoryId());
        String code = request.productCode().strip();
        ensureCodeAvailable(code, id);
        int rows = productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, id)
                .eq(Product::getDeleted, 0)
                .set(Product::getProductCode, code)
                .set(Product::getTitle, request.title().strip())
                .set(Product::getBrand, blankToNull(request.brand()))
                .set(Product::getCategoryId, request.categoryId())
                .set(Product::getImageUrl, WebUrls.normalize(request.imageUrl(), "图片链接", 512))
                .set(Product::getSpecification, blankToNull(request.specification()))
                .set(Product::getDescription, blankToNull(request.description())));
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT);
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRODUCT_UPDATE, "PRODUCT", id, code);
        return toResponse(require(id));
    }

    @Transactional
    public ProductResponse disable(Long id) {
        AdminUserDetails actor = adminActors.current();
        Product product = require(id);
        if (!Product.STATUS_ACTIVE.equals(product.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品已停用");
        }
        if (productMapper.disable(id) != 1) {
            throw new BizException(ErrorCode.CONFLICT);
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRODUCT_DISABLE, "PRODUCT", id, product.getProductCode());
        return toResponse(require(id));
    }

    @Transactional
    public ProductResponse enable(Long id) {
        AdminUserDetails actor = adminActors.current();
        Product product = require(id);
        if (!Product.STATUS_INACTIVE.equals(product.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品未停用");
        }
        if (productMapper.enable(id) != 1) {
            throw new BizException(ErrorCode.CONFLICT);
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRODUCT_ENABLE, "PRODUCT", id, product.getProductCode());
        return toResponse(require(id));
    }

    @Transactional
    public void delete(Long id) {
        AdminUserDetails actor = adminActors.current();
        Product product = require(id);
        if (productMapper.markDeleted(id) != 1) {
            throw new BizException(ErrorCode.CONFLICT);
        }
        auditService.record(actor.getId(), actor.getUsername(), AuditService.PRODUCT_DELETE, "PRODUCT", id, product.getProductCode());
    }

    private Product require(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted() == null || product.getDeleted() != 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        return product;
    }

    private void requireEnabledCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Category.STATUS_ENABLED.equals(category.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "分类不存在或已停用");
        }
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        Product existing = productMapper.selectOne(Wrappers.<Product>lambdaQuery().eq(Product::getProductCode, code));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "商品编码已存在");
        }
    }

    private void apply(Product product, ProductWriteRequest request, String code) {
        product.setProductCode(code);
        product.setTitle(request.title().strip());
        product.setBrand(blankToNull(request.brand()));
        product.setCategoryId(request.categoryId());
        product.setImageUrl(WebUrls.normalize(request.imageUrl(), "图片链接", 512));
        product.setSpecification(blankToNull(request.specification()));
        product.setDescription(blankToNull(request.description()));
    }

    private ProductResponse toResponse(Product product) {
        Category category = categoryMapper.selectById(product.getCategoryId());
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getTitle(),
                product.getBrand(),
                product.getImageUrl(),
                product.getCategoryId(),
                category == null ? null : category.getName(),
                product.getSpecification(),
                product.getDescription(),
                product.getStatus(),
                DbTimes.toOffset(product.getCreatedAt()),
                DbTimes.toOffset(product.getUpdatedAt()),
                DealSource.isDemoProductCode(product.getProductCode()));
    }

    private ProductResponse toResponse(ProductRow row) {
        return new ProductResponse(
                row.getId(),
                row.getProductCode(),
                row.getTitle(),
                row.getBrand(),
                row.getImageUrl(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getSpecification(),
                row.getDescription(),
                row.getStatus(),
                DbTimes.toOffset(row.getCreatedAt()),
                DbTimes.toOffset(row.getUpdatedAt()),
                DealSource.isDemoProductCode(row.getProductCode()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
