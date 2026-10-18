-- 集成测试数据。公开可见的好价只有 1、2、3，其余每条覆盖一种不可公开的情况。
INSERT INTO category (id, name, code, sort_order, status) VALUES
    (1, '数码家电', 'digital', 20, 'ENABLED'),
    (2, '食品饮料', 'food',    10, 'ENABLED'),
    (3, '停用分类', 'old',      5, 'DISABLED');

INSERT INTO product (id, product_code, title, brand, category_id, specification, description, status, deleted) VALUES
    (1, 'P-1', '降噪耳机 Pro',       '声途', 1, '颜色：黑色', '商品描述', 'ACTIVE',   0),
    (2, 'P-2', '挂耳咖啡 30 袋',     '晨野', 2, NULL,         NULL,       'ACTIVE',   0),
    (3, 'P-3', '充电器 100%_快充版', NULL,   1, NULL,         NULL,       'ACTIVE',   0),
    (4, 'P-4', '已下架商品',         '声途', 1, NULL,         NULL,       'INACTIVE', 0),
    (5, 'P-5', '已删除商品',         '声途', 1, NULL,         NULL,       'ACTIVE',   1);

INSERT INTO deal (id, product_id, current_price, original_price, deal_description, source_type, source_name,
                  purchase_url, promotion_url, featured, starts_at, expires_at, verified_at, status, published_at) VALUES
    (1,  1, 299.00, 399.00, '券后价',         'EDITOR', '测试商城', 'https://item.example.com/1', 'javascript:alert(1)', 1, NULL, '2999-12-31 23:59:59', '2026-01-03 10:00:00', 'PUBLISHED', '2026-01-03 09:00:00'),
    (2,  2,  49.90, NULL,   '会员价',         'DEMO',   '演示超市', 'http://insecure.example.com/2', NULL,               0, NULL, NULL, NULL, 'PUBLISHED', '2026-01-02 09:00:00'),
    (3,  3,  89.90, 129.00, '满减',           'EDITOR', '测试商城', NULL, NULL,                                       0, NULL, NULL, NULL, 'PUBLISHED', '2026-01-01 09:00:00'),
    (4,  4,  10.00, NULL,   '商品已下架',     'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'PUBLISHED',      '2026-01-04 09:00:00'),
    (5,  5,  10.00, NULL,   '商品已删除',     'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'PUBLISHED',      '2026-01-04 09:00:00'),
    (6,  1,  10.00, NULL,   '草稿',           'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'DRAFT',          NULL),
    (7,  1,  10.00, NULL,   '好价已下架',     'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'OFFLINE',        '2026-01-04 09:00:00'),
    (8,  2,  10.00, NULL,   '促销已结束',     'EDITOR', '测试商城', NULL, NULL, 0, NULL, '2000-01-01 00:00:00', NULL, 'PUBLISHED', '2026-01-04 09:00:00'),
    (9,  2,  10.00, NULL,   '促销未开始',     'EDITOR', '测试商城', NULL, NULL, 0, '2999-01-01 00:00:00', NULL, NULL, 'PUBLISHED', '2026-01-04 09:00:00'),
    (10, 2,  10.00, NULL,   '待审核',         'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'PENDING_REVIEW', NULL),
    (11, 2,  10.00, NULL,   '定时发布未到',   'EDITOR', '测试商城', NULL, NULL, 0, NULL, NULL, NULL, 'PUBLISHED',      '2999-01-01 00:00:00');
