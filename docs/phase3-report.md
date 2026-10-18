# 第三阶段实施报告：前后端联调与真实数据库接入

日期：2026-10-18

## 结论

首页、分类、搜索、好价详情四个页面已全部改为通过 Spring Boot 接口读取 MySQL 真实数据。后端 23 个测试、前端 32 个测试、类型检查、H5 与微信小程序构建全部通过。H5 已在浏览器中连接真实后端逐项验证；微信小程序只验证到构建成功，未在开发者工具中实际运行。

## 1. 新增和修改文件清单

### 后端（backend/）

新增：

| 文件 | 说明 |
| --- | --- |
| `sql/schema.sql` | 建表脚本：category、product、deal、price_history |
| `sql/demo-data.sql` | 开发环境演示数据（DEMO） |
| `entity/Category.java`、`entity/Deal.java` | 表实体 |
| `entity/DealRow.java` | 好价 + 商品 + 分类联表查询结果 |
| `mapper/CategoryMapper.java`、`mapper/DealMapper.java` | 数据访问 |
| `resources/mapper/DealMapper.xml` | 公开列表、详情 SQL，公开可见规则只定义在此处 |
| `dto/DealQueryRequest.java`、`dto/DealSortBy.java` | 列表查询参数与排序白名单 |
| `dto/DealSummaryResponse.java`、`dto/DealDetailResponse.java`、`dto/CategoryResponse.java` | 接口出参 |
| `service/CategoryService.java`、`service/DealService.java` | 业务规则 |
| `controller/CategoryController.java`、`controller/DealController.java` | REST 接口 |
| `test/.../DealApiIntegrationTest.java` | 接口集成测试（12 个） |
| `test/resources/application-test.yml`、`test/resources/db/test-data.sql` | H2 测试配置与测试数据 |

修改：

| 文件 | 说明 |
| --- | --- |
| `pom.xml` | 新增测试依赖 H2（版本由 Spring Boot 管理） |
| `common/api/ErrorCode.java` | 新增 `40401 好价已下架或已过期` |
| `common/exception/GlobalExceptionHandler.java` | 参数类型转换失败返回“参数格式错误: 字段”，不返回英文异常细节 |
| `sql/init.sql` | 注释说明三个脚本的执行顺序 |

### 前端（frontend/src/）

新增：

| 文件 | 说明 |
| --- | --- |
| `api/deal.ts`、`api/category.ts` | 好价、分类接口模块 |
| `composables/usePagedList.ts` | 分页加载、按 id 去重、丢弃过期请求、失败重试 |
| `components/ListFooter.vue` | 分页列表底部状态 |
| `utils/request.test.ts`、`composables/usePagedList.test.ts` | 单元测试 |

修改：

| 文件 | 说明 |
| --- | --- |
| `pages/index/index.vue` | 接口数据、上拉加载、下拉刷新、错误重试 |
| `pages/category/index.vue` | 接口分类、切换分类请求、分类加载失败重试 |
| `pages/search/index.vue` | 后端 keyword 搜索、分页、错误状态 |
| `pages/deal/detail.vue` | 接口详情，区分不存在 / 已下架 / 网络错误，购买按钮校验链接 |
| `components/DealCard.vue`、`PriceText.vue`、`DealImage.vue` | 适配后端字段，金额改为字符串 |
| `types/deal.ts`、`types/api.ts` | 与后端响应结构一致的类型、错误码常量 |
| `utils/request.ts` | 开发日志、超时与断网提示、清理空参数、生产环境强制 HTTPS |
| `utils/format.ts` | 金额字符串格式化，不经过浮点运算 |
| `services/deal.ts` | 购买链接校验、演示数据判断、示例热门词 |
| `platform/index.ts` | 打开外部链接（H5 新窗口，小程序复制链接） |
| `config/env.ts`、`pages.json` | `isDev` 配置；首页开启下拉刷新 |
| `services/deal.test.ts`、`utils/format.test.ts` | 测试随实现更新 |

删除：`mock/deals.ts`、`mock/categories.ts`（同样的演示商品已迁入 `demo-data.sql`）。

### 文档

`README.md`、`docs/architecture.md`、`docs/development-guide.md`、`docs/api-conventions.md` 已更新。

## 2. 数据库

脚本位置：

| 脚本 | 用途 |
| --- | --- |
| `backend/sql/init.sql` | 创建数据库 `great_price` |
| `backend/sql/schema.sql` | 建表，`CREATE TABLE IF NOT EXISTS`，可重复执行 |
| `backend/sql/demo-data.sql` | 仅开发环境的演示数据，可重复执行，不修改已有数据 |

表结构：

| 表 | 要点 |
| --- | --- |
| `category` | `code` 唯一；`sort_order` 排序；`status`：ENABLED / DISABLED |
| `product` | `product_code` 唯一；`status`：ACTIVE / INACTIVE；`deleted` 逻辑删除 |
| `deal` | 关联 product；价格 `DECIMAL(10,2)`；`purchase_url` 与 `promotion_url` 分开保存；`status`：DRAFT / PENDING_REVIEW / PUBLISHED / OFFLINE / EXPIRED；`featured` 编辑精选 |
| `price_history` | 只记录真实获取或人工核验的价格；本阶段只建表，无写入逻辑 |

规则：

- 索引：分类编码、商品编码唯一索引；分类状态+排序、商品分类、商品状态、好价状态+发布时间、关联字段均建索引。
- 不建物理外键（项目原无约定），关联由应用层保证，理由见 architecture.md。
- `original_price` 只作参考价展示，系统不据此计算“优惠幅度”。
- 演示数据：商品编码以 `DEMO-` 开头，`source_type = DEMO`，`verified_at` 为空，无购买链接，不写价格历史。
- 应用启动时不自动执行任何 SQL；生产环境禁止执行 `demo-data.sql`。

本机执行情况：两份脚本已在本机 `great_price` 库执行（各重复执行两次验证幂等）。当前数据：7 个分类、14 个商品、14 条好价（12 条已发布、1 条已下架、1 条草稿），“母婴用品”分类故意无商品用于展示空状态。

## 3. API 清单

完整说明与响应示例见 `docs/api-conventions.md`。

### GET /api/v1/categories

只返回启用分类，按 `sort_order` 排序。

```json
{ "code": 0, "message": "成功", "data": [ { "id": 1, "name": "数码家电", "code": "digital", "icon": null } ] }
```

### GET /api/v1/deals

| 参数 | 说明 |
| --- | --- |
| `page` | 默认 1，最小 1 |
| `size` | 默认 20，范围 1～50 |
| `categoryId` | 可选，正整数 |
| `keyword` | 可选，最长 50 字符；匹配商品名称、品牌、分类名称；`%`、`_` 按字面匹配 |
| `featured` | 可选，`true` 只返回编辑精选 |
| `sortBy` | `LATEST`（默认）/ `PRICE_ASC` / `PRICE_DESC`，其他值返回 40000 |

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 2, "title": "极光 65W 氮化镓三口快充充电器 USB-C 双口 + USB-A", "brand": "极光",
        "imageUrl": null, "categoryId": 1, "categoryName": "数码家电",
        "currentPrice": "89.90", "originalPrice": "129.00", "currency": "CNY",
        "dealDescription": "店铺满 2 件 9 折，单件券后价约 89.9 元",
        "sourceType": "DEMO", "sourceName": "演示商城·旗舰店",
        "expiresAt": null, "verifiedAt": null, "publishedAt": "2026-10-18T08:30:00+08:00"
      }
    ],
    "total": 12, "page": 1, "size": 20
  }
}
```

只返回公开可见的好价：已发布、已到发布时间、在促销有效期内、关联商品正常且未删除。分页在数据库侧完成（COUNT + LIMIT），一次联表查询，无 N+1。

### GET /api/v1/deals/{id}

在列表字段基础上增加 `productId`、`specification`、`description`、`startsAt`、`purchaseUrl`、`promotionUrl`。链接只有是合法 `https://` 地址时才返回。

| 情况 | HTTP | code |
| --- | --- | --- |
| id 非正整数 | 400 | 40000 |
| 不存在、草稿、待审核 | 404 | 40400 好价不存在 |
| 已下架、已过期、商品已下架 | 404 | 40401 好价已下架或已过期 |

### 与需求原文的差异

- 分页参数沿用项目原有约定 `size`，未改为 `pageSize`。
- 出参沿用 `dto` 包与 `XxxResponse` 命名，未新建 `vo` 包。
- `deal` 表增加 `featured` 字段，用于保留首页“编辑精选”。
- 第二阶段的促销标签无对应字段，已从详情页移除；“规格”由键值列表改为文本。

## 4. 前端 Mock 到 API 的替换范围

- 已替换：首页（分类、精选、最新列表）、分类页、搜索、详情，全部使用接口数据。
- 接口失败时显示错误状态和重试按钮，不会静默切换到 Mock。
- 后端返回 `sourceType = DEMO` 时，页面显示“演示数据”提示。
- 仍为 Mock：搜索页热门搜索词（暂无后端接口，界面标注“示例”）。
- 未实现开发环境 Mock 开关（需求中为可选项）。

## 5. 本地启动与联调步骤

```bash
# 数据库（本机已执行，新环境才需要）
cd backend
mysql -uroot -p < sql/init.sql
mysql -uroot -p great_price < sql/schema.sql
mysql -uroot -p great_price < sql/demo-data.sql

# 后端（JDK 21，工作目录必须是 backend/，否则读不到 .env）
mvn spring-boot:run
curl "http://localhost:8080/api/v1/deals?page=1&size=2"

# 前端
cd ../frontend
npm run dev:h5          # http://localhost:5173
npm run dev:mp-weixin   # 微信开发者工具导入 dist/dev/mp-weixin
```

8080 被占用时：`SERVER_PORT=8081 mvn spring-boot:run`，前端 `VITE_API_BASE_URL=http://localhost:8081 npm run dev:h5`。

## 6. 已执行的测试及结果

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| 后端测试 | `mvn test` | 23 个全部通过（新增 12 个集成测试） |
| 前端单元测试 | `npm test` | 32 个全部通过 |
| 前端类型检查 | `npm run type-check` | 通过 |
| 小程序构建 | `npm run build:mp-weixin` | 成功 |
| H5 构建 | `npm run build:h5` | 成功 |
| 真实 MySQL 接口冒烟 | curl | 全部符合预期 |

后端集成测试覆盖：分类列表、分页列表、分类筛选、关键词搜索（含特殊字符、超长、SQL 注入字符串）、正常详情、不存在 ID、下架商品/未发布/过期/未开始/定时发布未到不出现在列表、分页边界、排序白名单、价格字段映射与链接过滤。测试使用 H2 MySQL 兼容模式直接加载 `schema.sql`（本机无 Docker）。

前端单元测试覆盖：请求封装（成功、业务错误、HTTP 错误、超时、断网、未配置 Base URL、生产环境强制 HTTPS、开发日志）、接口参数拼装、分页组合函数（加载中状态、追加、去重、重复触发、快速切换丢弃旧请求、空结果、失败重试）、金额格式化、购买链接校验。

## 7. H5 与微信小程序验证情况

### H5（已验证）

在浏览器中连接真实后端验证：

- 首页首屏 10 条，滚动加载第 2 页共 12 条，无重复，显示“没有更多了”。
- 分类切换正常；快速连点三个分类，只显示最后一个分类的结果。
- “母婴用品”显示空状态。
- 搜索有结果、无结果、`%` 按字面匹配均正确。
- 详情正常、不存在、已下架三种状态正确；无购买链接时按钮显示“暂无购买链接”且不可点击。
- 断开接口后首页显示“加载失败”，恢复后重试数据正常。
- 跨域仅放行 `http://localhost:5173`。

### 微信小程序（仅构建验证）

- 构建成功，产物检查：小程序包不含 `window.open`，H5 专属代码已被条件编译排除；首页下拉刷新配置生效。
- 未在开发者工具中运行：开发者工具“服务端口”未开启，命令行无法调用。
- 未验证：开发者工具运行、真机预览、HTTPS 合法域名。
- 小程序中点击“去购买”会复制链接，并提示到浏览器或购物 App 打开。

## 8. 尚未解决的问题

- IntelliJ 中 8080 端口的后端实例仍是旧代码，且数据库显示 `DOWN`（工作目录可能不是 `backend/`），需要重启后前端才能获取数据。
- 微信小程序需在开发者工具中导入 `frontend/dist/dev/mp-weixin` 手动验证；上线前需配置 HTTPS request 合法域名。
- 关键词搜索为 `LIKE '%x%'`，无法使用索引，数据量增大后需改用全文索引或搜索引擎。
- H2 与 MySQL 存在差异（测试中已模拟大小写不敏感），上线前需在真实 MySQL 上回归。

## 9. 下一阶段建议

- 管理后台：好价录入、审核、上下架、核验，人工核验时写入价格历史。
- 定时任务：超过有效期的好价自动标记为已过期。
- 服务端生成推广链接（联盟 API 接入）。
- 登录与收藏持久化。

以上均未在本阶段开发。
