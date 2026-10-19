-- 第五阶段：自动过期索引、任务执行记录、核验幂等键。
-- 前提：已执行 schema.sql 和 V4__admin.sql。
-- 执行：mysql -uroot -p great_price < backend/sql/V5__deal_expiry.sql
-- 只新增索引、表和列，不修改已有商品与好价的价格、状态。
-- 只执行一次。应用启动不会自动执行本脚本。

CREATE INDEX idx_deal_status_expires ON deal (status, expires_at);

CREATE TABLE IF NOT EXISTS job_run (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    job_name        VARCHAR(64)  NOT NULL COMMENT '任务名，如 DEAL_EXPIRE',
    status          VARCHAR(16)  NOT NULL COMMENT 'SUCCESS / FAILED',
    processed_count INT          NOT NULL DEFAULT 0 COMMENT '本次成功处理数量',
    failed_count    INT          NOT NULL DEFAULT 0 COMMENT '本次失败批次数',
    message         VARCHAR(500) NULL COMMENT '失败摘要，不含密码和令牌',
    started_at      DATETIME     NOT NULL,
    finished_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_job_run_name (job_name, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '定时任务最近执行记录';

ALTER TABLE price_history ADD COLUMN idempotency_key VARCHAR(64) NULL;
CREATE UNIQUE INDEX uk_price_history_actor_key ON price_history (verified_by, idempotency_key);
