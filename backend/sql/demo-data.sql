-- 【仅限开发环境】演示数据。禁止在生产库执行。
-- 执行方式：mysql -uroot -p great_price < backend/sql/demo-data.sql（需先执行 schema.sql）
--
-- 说明：
--   * 品牌、商城、价格、优惠全部为虚构，不代表任何真实商品行情。
--   * 商品编码统一以 DEMO- 开头，好价 source_type 统一为 DEMO，来源名称带“演示”字样。
--   * verified_at 全部为空：演示数据未经核验，不得标记为已核验。
--   * 不包含购买链接和推广链接，不写入 price_history。
--   * 可重复执行：分类、商品按唯一编码跳过已存在的记录；好价只在对应商品还没有 DEMO 好价时插入。
--     不会修改或删除任何已有数据。

INSERT INTO category (name, code, sort_order, status) VALUES
    ('数码家电', 'digital', 10, 'ENABLED'),
    ('食品饮料', 'food',    20, 'ENABLED'),
    ('家居日用', 'home',    30, 'ENABLED'),
    ('美妆个护', 'beauty',  40, 'ENABLED'),
    ('运动户外', 'sports',  50, 'ENABLED'),
    ('母婴用品', 'baby',    60, 'ENABLED'),
    ('服饰鞋包', 'fashion', 70, 'ENABLED')
ON DUPLICATE KEY UPDATE id = id;

SET @c_digital = (SELECT id FROM category WHERE code = 'digital');
SET @c_food    = (SELECT id FROM category WHERE code = 'food');
SET @c_home    = (SELECT id FROM category WHERE code = 'home');
SET @c_beauty  = (SELECT id FROM category WHERE code = 'beauty');
SET @c_sports  = (SELECT id FROM category WHERE code = 'sports');
SET @c_fashion = (SELECT id FROM category WHERE code = 'fashion');

-- 「母婴用品」故意没有商品，用于展示空列表状态。
INSERT INTO product (product_code, title, brand, category_id, specification, status) VALUES
    ('DEMO-1001', '声途 Air3 主动降噪真无线蓝牙耳机 入耳式 长续航', '声途', @c_digital, '颜色：曜石黑；连接方式：蓝牙 5.3', 'ACTIVE'),
    ('DEMO-1002', '极光 65W 氮化镓三口快充充电器 USB-C 双口 + USB-A', '极光', @c_digital, '65W 三口', 'ACTIVE'),
    ('DEMO-1003', '晨野 挂耳咖啡 中度烘焙 10g×30 袋 混合装', '晨野', @c_food, '净含量：10g × 30 袋；烘焙度：中度', 'ACTIVE'),
    ('DEMO-1004', '清泉 无糖苏打气泡水 原味 330ml×24 罐 整箱', '清泉', @c_food, '330ml × 24 罐', 'ACTIVE'),
    ('DEMO-1005', '木屿 可叠加透明收纳箱 带轮 55L 3 只装 衣物整理箱超长标题用于测试截断效果', '木屿', @c_home, '容量：55L；数量：3 只', 'ACTIVE'),
    ('DEMO-1006', '木屿 加厚抽纸 3 层 120 抽×24 包', '木屿', @c_home, NULL, 'ACTIVE'),
    ('DEMO-1007', '露颜 氨基酸洗发水 控油蓬松 500ml×2 瓶', '露颜', @c_beauty, '500ml × 2 瓶', 'ACTIVE'),
    ('DEMO-1008', '露颜 温和保湿洁面乳 120g', '露颜', @c_beauty, NULL, 'ACTIVE'),
    ('DEMO-1009', '迅步 Flow 2 男女款缓震轻量跑鞋 透气网面', '迅步', @c_sports, '适用人群：男女同款；鞋面材质：透气网布', 'ACTIVE'),
    ('DEMO-1010', '山行 户外折叠露营椅 铝合金 承重 120kg', '山行', @c_sports, NULL, 'ACTIVE'),
    ('DEMO-1011', '素衣 男士纯棉圆领短袖 T 恤 3 件装', '素衣', @c_fashion, '面料：100% 棉；数量：3 件', 'ACTIVE'),
    ('DEMO-1012', '素衣 双肩通勤背包 15.6 英寸电脑仓 防泼水', '素衣', @c_fashion, NULL, 'ACTIVE'),
    -- 以下两条用于验证非公开数据不会出现在列表中
    ('DEMO-1013', '极光 无线充电板 15W（演示：已下架好价）', '极光', @c_digital, NULL, 'ACTIVE'),
    ('DEMO-1014', '晨野 冻干咖啡粉 2g×20 颗（演示：草稿好价）', '晨野', @c_food, NULL, 'ACTIVE')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO deal (product_id, current_price, original_price, currency, deal_description,
                  source_type, source_name, featured, status, published_at)
SELECT p.id, v.current_price, v.original_price, 'CNY', v.deal_description,
       'DEMO', v.source_name, v.featured, v.status, v.published_at
FROM (
    SELECT 'DEMO-1001' AS product_code, 299.00 AS current_price, 399.00 AS original_price,
           '页面领取满 299 减 100 券后到手价，优惠以下单页面为准' AS deal_description,
           '演示商城·自营' AS source_name, 1 AS featured, 'PUBLISHED' AS status, '2026-10-18 09:00:00' AS published_at
    UNION ALL SELECT 'DEMO-1002', 89.90, 129.00, '店铺满 2 件 9 折，单件券后价约 89.9 元', '演示商城·旗舰店', 0, 'PUBLISHED', '2026-10-18 08:30:00'
    UNION ALL SELECT 'DEMO-1003', 49.90, 69.90, '叠加满 99 减 20 跨店优惠，折合每袋约 1.66 元', '演示超市', 1, 'PUBLISHED', '2026-10-18 07:45:00'
    UNION ALL SELECT 'DEMO-1004', 39.90, NULL, '会员价，新用户首单可再减 5 元', '演示超市', 0, 'PUBLISHED', '2026-10-17 21:10:00'
    UNION ALL SELECT 'DEMO-1005', 79.00, 119.00, '拍 3 件自动减 40 元，适合换季整理衣物、被褥', '演示商城·旗舰店', 1, 'PUBLISHED', '2026-10-17 19:30:00'
    UNION ALL SELECT 'DEMO-1006', 45.80, 59.90, 'PLUS 会员专享价，非会员价格以页面为准', '演示商城·自营', 0, 'PUBLISHED', '2026-10-17 16:00:00'
    UNION ALL SELECT 'DEMO-1007', 69.00, 98.00, '第二件半价，两瓶到手价', '演示海外购', 0, 'PUBLISHED', '2026-10-17 15:00:00'
    UNION ALL SELECT 'DEMO-1008', 35.50, NULL, '店铺新客券后价', '演示海外购', 0, 'PUBLISHED', '2026-10-16 22:15:00'
    UNION ALL SELECT 'DEMO-1009', 259.00, 459.00, '旗舰店换季清仓价，尺码以页面库存为准', '演示商城·旗舰店', 0, 'PUBLISHED', '2026-10-16 17:20:00'
    UNION ALL SELECT 'DEMO-1010', 129.00, 169.00, '叠加满 199 减 30 券可凑单使用', '演示商城·自营', 0, 'PUBLISHED', '2026-10-16 12:00:00'
    UNION ALL SELECT 'DEMO-1011', 99.00, 149.00, '3 件装到手价，平均每件 33 元', '演示商城·旗舰店', 0, 'PUBLISHED', '2026-10-15 20:00:00'
    UNION ALL SELECT 'DEMO-1012', 159.00, NULL, '页面直降，无需领券', '演示商城·自营', 0, 'PUBLISHED', '2026-10-15 11:00:00'
    UNION ALL SELECT 'DEMO-1013', 59.00, 79.00, '演示：该好价已下架，不应出现在列表中', '演示商城·自营', 0, 'OFFLINE', '2026-10-14 10:00:00'
    UNION ALL SELECT 'DEMO-1014', 39.00, NULL, '演示：该好价为草稿，不应出现在列表中', '演示超市', 0, 'DRAFT', NULL
) v
JOIN product p ON p.product_code = v.product_code
WHERE NOT EXISTS (SELECT 1 FROM deal d WHERE d.product_id = p.id AND d.source_type = 'DEMO');
