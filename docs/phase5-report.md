# 第五阶段报告：有效期、价格历史闭环与自动过期

## 检查结论

第四阶段已经能人工核验并写入 `price_history`（核验人、时间、来源、说明），公开接口也已经按 `starts_at` / `expires_at` 过滤。缺的是把到期的 `PUBLISHED` 改成 `EXPIRED` 的定时任务，以及同一次核验重复提交的幂等保护。

`expires_at` 为空表示未设置结束时间，不自动过期。`starts_at` 为空表示立即生效。这些约定没有改。

## 自动过期

- 类：`DealExpiryJob`，`@Scheduled`，默认每 60 秒。可用 `DEAL_EXPIRY_INTERVAL_MS`、`DEAL_EXPIRY_BATCH_SIZE`（默认 200）、`DEAL_EXPIRY_ENABLED` 调整。
- 时区：比较用 `Asia/Shanghai`（`DbTimes`）。调度时区同样是 `Asia/Shanghai`。
- 只更新 `status = PUBLISHED AND expires_at IS NOT NULL AND expires_at <= 当前时间`。按 id 分批，更新时再次带上同样条件。
- 不改价格、链接、核验时间和审核记录。不下架草稿、待审核、已下架记录，也不重复处理已过期记录。
- 每批独立提交。某一批失败时，已成功的批次保留，本次记为失败，下一轮继续处理剩下的。
- 多实例同时跑只会命中仍是 `PUBLISHED` 的行，不会把同一条过期两次。没有引入分布式锁。
- 执行结果写入 `job_run`。有实际过期时再写一条审计，操作人是 `system`，动作 `DEAL_EXPIRE`，不冒充管理员。

## 价格历史

沿用 `POST /api/admin/v1/price-history`。新增可选 `idempotencyKey`：同一个人用同一个键重复提交，返回第一次的记录，不再插第二条。不带键的每次提交仍是独立核验。不能按“商品 + 价格”去重。

核验过期或下架好价只更新 `verified_at`，不恢复发布。浏览和编辑草稿不写历史。`DEMO` 仍拒绝写入。

## 迁移

`backend/sql/V5__deal_expiry.sql` 已在本机 `great_price` 执行。新增 `job_run`、`deal` 索引 `idx_deal_status_expires`、`price_history.idempotency_key`。

当前正在跑的后端进程是这次改动之前启动的，需要再重启一次才会加载定时任务。

## 测试

| 命令 | 结果 |
| --- | --- |
| `backend` `mvn test` | 46 通过，0 失败 |
| `admin` `npm test` | 6 通过 |
| `admin` `npm run type-check`、`npm run build` | 通过 |
| `frontend` `npm test` | 32 通过 |
| `frontend` `npm run type-check`、`npm run build:h5`、`npm run build:mp-weixin` | 通过 |
