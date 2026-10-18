-- 第四阶段：管理后台账号、审核记录、操作审计，以及好价/价格历史上缺少的字段。
-- 前提：已执行 schema.sql。
-- 执行：mysql -uroot -p great_price < backend/sql/V4__admin.sql
-- 本脚本只新增表和列，不修改已有商品、好价的价格与状态，不写入演示数据。
-- 只执行一次。重复执行会因为列已存在而失败。应用启动不会自动执行本脚本。

CREATE TABLE IF NOT EXISTS admin_user (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    username           VARCHAR(32)  NOT NULL COMMENT '登录名，区分大小写',
    password_hash      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希，不明文保存',
    display_name       VARCHAR(32)  NOT NULL COMMENT '显示名称',
    role               VARCHAR(16)  NOT NULL COMMENT 'ADMIN / OPERATOR',
    status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
    failed_login_count INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until       DATETIME     NULL COMMENT '锁定截止时间，为空表示未锁定',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理后台账号';

CREATE TABLE IF NOT EXISTS deal_review (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    deal_id     BIGINT       NOT NULL COMMENT '好价 deal.id',
    action      VARCHAR(16)  NOT NULL COMMENT 'APPROVE 通过 / REJECT 驳回',
    reviewer_id BIGINT       NOT NULL COMMENT '审核人 admin_user.id',
    reason      VARCHAR(500) NULL COMMENT '驳回原因，通过时为空',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_deal_review_deal (deal_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '好价审核记录，只追加不修改';

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id      BIGINT       NULL COMMENT '操作人 admin_user.id，未知用户登录失败时为空',
    actor_name    VARCHAR(32)  NOT NULL COMMENT '操作人登录名',
    action        VARCHAR(32)  NOT NULL COMMENT '动作，如 DEAL_APPROVE',
    resource_type VARCHAR(32)  NOT NULL COMMENT '资源类型，如 DEAL / PRODUCT / AUTH',
    resource_id   BIGINT       NULL COMMENT '资源主键',
    detail        VARCHAR(1000) NULL COMMENT '补充说明，不记录密码和令牌',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_created (created_at),
    KEY idx_audit_resource (resource_type, resource_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理操作审计，只追加不修改';

ALTER TABLE deal ADD COLUMN reject_reason VARCHAR(500) NULL;
ALTER TABLE deal ADD COLUMN submitted_by BIGINT NULL;
ALTER TABLE deal ADD COLUMN reviewed_by BIGINT NULL;
ALTER TABLE deal ADD COLUMN reviewed_at DATETIME NULL;
ALTER TABLE deal ADD COLUMN offline_reason VARCHAR(500) NULL;
ALTER TABLE deal ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE price_history ADD COLUMN verified_by BIGINT NULL;
ALTER TABLE price_history ADD COLUMN note VARCHAR(500) NULL;
