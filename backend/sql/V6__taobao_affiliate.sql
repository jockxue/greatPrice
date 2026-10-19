-- 第六阶段：淘宝联盟商品关联、优惠标识和同步任务字段。
-- 前提：已执行 schema.sql、V4__admin.sql、V5__deal_expiry.sql。
-- 执行：mysql -uroot -p great_price < backend/sql/V6__taobao_affiliate.sql
-- 只新增表、列和索引，不修改已有商品价格、好价状态和价格历史。
-- 只执行一次。应用启动不会自动执行本脚本。

CREATE TABLE IF NOT EXISTS product_external_ref (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    product_id       BIGINT       NOT NULL COMMENT '站内商品 product.id',
    platform         VARCHAR(16)  NOT NULL COMMENT '平台标识，淘宝为 TAOBAO',
    external_item_id VARCHAR(128) NOT NULL COMMENT '平台商品 ID，不是站内主键',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_item (platform, external_item_id),
    KEY idx_external_ref_product (product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内商品与外部平台商品的对应关系';

ALTER TABLE deal ADD COLUMN external_offer_key VARCHAR(128) NULL COMMENT '平台优惠稳定标识；淘宝商品详情没有 coupon_id 时使用 LISTING:商品ID';
ALTER TABLE deal ADD COLUMN promotion_source VARCHAR(64) NULL COMMENT '推广链接生成来源，如 taobao.tbk.privilege.get';
ALTER TABLE deal ADD COLUMN promotion_generated_at DATETIME NULL COMMENT '推广链接生成时间';
CREATE UNIQUE INDEX uk_deal_external_offer ON deal (product_id, source_type, external_offer_key);

ALTER TABLE job_run ADD COLUMN requested_count INT NOT NULL DEFAULT 0 COMMENT '本次请求数量';
ALTER TABLE job_run ADD COLUMN error_code VARCHAR(64) NULL COMMENT '平台或本地错误码，不含密钥';
ALTER TABLE job_run ADD COLUMN retryable TINYINT NOT NULL DEFAULT 0 COMMENT '1 表示失败后可以按原参数重试';
ALTER TABLE job_run ADD COLUMN retry_payload VARCHAR(500) NULL COMMENT '重试所需的非敏感参数';
