-- 第三阶段表结构：分类、商品、好价、价格历史。
-- 执行方式：mysql -uroot -p great_price < backend/sql/schema.sql
-- 只使用 CREATE TABLE IF NOT EXISTS，可重复执行，不会删除或修改已有表和数据；
-- 后续表结构变更请新增带版本号的变更脚本，不要直接修改本文件中已上线的表定义。
--
-- 约定：
--   * 不建物理外键，关联关系由应用层保证，关联字段均建索引（便于后续数据迁移和分表，避免级联误删）。
--   * 状态、类型字段使用大写英文字符串，与 API 中的枚举值一致。
--   * 金额使用 DECIMAL(10,2)，时间使用 DATETIME，存储北京时间（与 JDBC serverTimezone=Asia/Shanghai 一致）。

CREATE TABLE IF NOT EXISTS category (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(32)  NOT NULL COMMENT '分类名称',
    code       VARCHAR(32)  NOT NULL COMMENT '分类编码，唯一',
    icon       VARCHAR(512) NULL COMMENT '分类图标 URL',
    sort_order INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    status     VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 停用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code),
    KEY idx_category_status_sort (status, sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类';

CREATE TABLE IF NOT EXISTS product (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    product_code  VARCHAR(64)  NOT NULL COMMENT '商品业务标识，唯一；演示数据以 DEMO- 开头',
    title         VARCHAR(200) NOT NULL COMMENT '商品名称',
    brand         VARCHAR(64)  NULL COMMENT '品牌',
    image_url     VARCHAR(512) NULL COMMENT '商品主图 URL',
    category_id   BIGINT       NOT NULL COMMENT '所属分类 category.id',
    specification VARCHAR(500) NULL COMMENT '商品规格',
    description   TEXT         NULL COMMENT '商品描述',
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 正常 / INACTIVE 下架',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_category (category_id),
    KEY idx_product_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品';

CREATE TABLE IF NOT EXISTS deal (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    product_id       BIGINT        NOT NULL COMMENT '关联商品 product.id',
    current_price    DECIMAL(10,2) NOT NULL COMMENT '当前展示价格',
    original_price   DECIMAL(10,2) NULL COMMENT '参考价格，仅供展示，不能据此认定真实优惠幅度',
    currency         CHAR(3)       NOT NULL DEFAULT 'CNY' COMMENT 'ISO 4217 币种',
    deal_description VARCHAR(500)  NOT NULL COMMENT '优惠说明',
    source_type      VARCHAR(32)   NOT NULL COMMENT '信息来源类型：DEMO 演示 / EDITOR 编辑发布 / USER_SUBMISSION 用户爆料 / AFFILIATE 推广联盟',
    source_name      VARCHAR(64)   NOT NULL COMMENT '来源名称，如商城或店铺名',
    purchase_url     VARCHAR(1024) NULL COMMENT '普通购买链接',
    promotion_url    VARCHAR(1024) NULL COMMENT '推广链接，只能由后端生成或配置',
    featured         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否进入首页编辑精选：0 否 1 是',
    starts_at        DATETIME      NULL COMMENT '促销开始时间，为空表示立即生效',
    expires_at       DATETIME      NULL COMMENT '促销结束时间，为空表示未知或长期有效',
    verified_at      DATETIME      NULL COMMENT '最近一次人工或程序核验价格的时间；演示数据必须为空',
    status           VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿 / PENDING_REVIEW 待审核 / PUBLISHED 已发布 / OFFLINE 已下架 / EXPIRED 已过期',
    published_at     DATETIME      NULL COMMENT '发布时间',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_deal_product (product_id),
    KEY idx_deal_status_published (status, published_at),
    CONSTRAINT ck_deal_current_price CHECK (current_price >= 0),
    CONSTRAINT ck_deal_original_price CHECK (original_price IS NULL OR original_price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好价（一次具体的促销信息）';

-- 只记录真实获取或人工核验过的价格，演示数据和前端 Mock 价格不得写入本表。
CREATE TABLE IF NOT EXISTS price_history (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    product_id          BIGINT        NOT NULL COMMENT '关联商品 product.id',
    deal_id             BIGINT        NULL COMMENT '关联好价 deal.id，非好价来源时为空',
    price               DECIMAL(10,2) NOT NULL COMMENT '价格',
    currency            CHAR(3)       NOT NULL DEFAULT 'CNY',
    source_type         VARCHAR(32)   NOT NULL COMMENT '来源类型，取值同 deal.source_type（不允许 DEMO）',
    source_name         VARCHAR(64)   NOT NULL COMMENT '来源名称',
    verification_status VARCHAR(32)   NOT NULL COMMENT 'MANUAL_VERIFIED 人工核验 / SOURCE_FETCHED 来源接口获取',
    recorded_at         DATETIME      NOT NULL COMMENT '价格对应的时间点',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_price_history_product_time (product_id, recorded_at),
    KEY idx_price_history_deal (deal_id),
    CONSTRAINT ck_price_history_price CHECK (price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '价格历史';
