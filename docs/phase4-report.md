# 第四阶段报告：管理后台

## 架构取舍

仓库里没有管理后台，也没有登录组件。公开接口保持 `/api/v1/**`。管理接口使用文档中已约定的 `/api/admin/v1/**`，而不是另起 `/api/v1/admin/**`。

认证使用 Spring Security 的 HttpOnly 会话，密码只存 BCrypt。没有把默认管理员密码写进代码。第一个账号由 `ADMIN_INIT_USERNAME` 和 `ADMIN_INIT_PASSWORD` 在账号表为空时创建。

`schema.sql` 不改。新增 `sql/V4__admin.sql`：`admin_user`、`deal_review`、`admin_audit_log`，以及 `deal` 的驳回原因、提交人、审核人、下架原因、乐观锁版本，`price_history` 的核验人和说明。

## 状态

`DRAFT → PENDING_REVIEW → PUBLISHED`。驳回回到 `DRAFT` 并记录原因。下架只作用于已发布记录。已下架或已过期只能重新打开为草稿。已发布内容的修改走变更审核，期间公开接口不再返回。没有按客户端传入状态直接改发布的接口。

本阶段没有定时过期任务。到期仍由公开查询的 `expires_at` 过滤。

## 验证

后端 `mvn test`（JDK 21）通过，含原有公开接口测试和管理接口测试。`admin` 的 `npm test`、`npm run type-check`、`npm run build` 通过。`frontend` 的 `npm test` 通过，用户端页面未改。

本机未找到 `mysql` 客户端，因此没有在真实 MySQL 上点击管理后台。H2 集成测试覆盖了登录、权限、商品、审核、下架、核验和审计。
