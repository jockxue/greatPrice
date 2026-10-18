# API 约定

小程序、H5 和后续的管理后台共用同一套后端 API，所有接口都遵循本约定。

## 路径

| 前缀 | 调用方 | 示例 |
| --- | --- | --- |
| `/api/v1/**` | 小程序、H5、App（C 端） | `GET /api/v1/deals` |
| `/api/admin/v1/**` | 管理后台（后续） | `POST /api/admin/v1/deals` |

- 资源名使用复数小写名词，多个单词用中划线连接：`/api/v1/deals`、`/api/v1/user-favorites`。
- 版本号放在路径中。只有发生不兼容变更时才升级为 `v2`，并保留 `v1` 一段时间，因为旧版本小程序可能还在用户手机上运行。
- 动作用 HTTP 方法表达：

| 方法 | 用途 | 示例 |
| --- | --- | --- |
| GET | 查询 | `GET /api/v1/deals/{id}` |
| POST | 新建、无法归入 CRUD 的动作 | `POST /api/v1/deals/{id}/favorite` |
| PUT | 整体更新 | `PUT /api/admin/v1/deals/{id}` |
| DELETE | 删除 | `DELETE /api/v1/deals/{id}/favorite` |

## 请求

- 请求体与响应体统一使用 `Content-Type: application/json; charset=utf-8`。
- JSON 字段名使用 camelCase。
- 查询条件和分页参数放在 query string 中：`GET /api/v1/deals?categoryId=3&page=1&size=20`。

## 统一响应格式

所有接口（包括错误响应）都返回如下结构：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | number | `0` 表示成功，其他值为错误码 |
| `message` | string | 结果描述，出错时可以直接展示给用户 |
| `data` | any | 业务数据；出错或没有返回值时为 `null` |

后端：`com.greatprice.common.api.ApiResponse`；前端：`src/types/api.ts`。前端的 `utils/request.ts` 在成功时直接返回 `data`。

## 错误码与 HTTP 状态码

HTTP 状态码表示错误类别，业务 `code` 表示具体原因，`code` 的前三位与 HTTP 状态码一致。

| code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40000 | 400 | 请求参数错误（校验失败、缺少参数、类型错误、JSON 格式错误） |
| 40100 | 401 | 未登录或登录已过期 |
| 40300 | 403 | 无权访问 |
| 40400 | 404 | 资源不存在（含好价不存在、草稿或待审核好价） |
| 40401 | 404 | 好价已下架或已过期（曾经公开过，现在不可访问） |
| 40500 | 405 | 请求方法不支持 |
| 40900 | 409 | 数据已被他人更新，请刷新后重试 |
| 50000 | 500 | 系统内部错误（不返回异常细节） |

新增业务错误码时，在 `ErrorCode` 枚举中按 HTTP 类别分配号段，例如 `40401 好价已下架`，并同步更新本表。

参数校验失败时，`message` 的格式为 `字段: 原因`，多个错误用 `; ` 分隔。校验注解上必须显式写中文 `message`，否则默认提示的语言会随服务器 JVM 的 locale 变化（本机为英文）：

```java
public record DealSubmitRequest(@NotBlank(message = "标题不能为空") String title) {}
```

```json
{ "code": 40000, "message": "title: 标题不能为空", "data": null }
```

## 分页

请求：`page` 从 1 开始，默认值为 1；`size` 默认 20，最大 100（后端分页插件会强制这个上限）。具体接口可以设置更小的上限，例如 `GET /api/v1/deals` 最大 50，超出时返回 40000。页码超过总页数时返回空的 `records`，不报错。

响应 `data`：

```json
{
  "records": [],
  "total": 125,
  "page": 1,
  "size": 20
}
```

后端：`PageResult.of(iPage, converter)`；前端：`PageResult<T>`。

## 数据类型

| 类型 | 约定 |
| --- | --- |
| ID | number，数据库自增主键。不使用雪花 ID，避免超出 JS 安全整数范围 |
| 金额 | string，例如 `"199.00"`。后端字段使用 `BigDecimal`，并加 `@JsonFormat(shape = JsonFormat.Shape.STRING)`（Jackson 默认会输出为数字） |
| 时间 | ISO-8601 字符串并带时区偏移，例如 `"2026-10-18T16:07:15+08:00"`。后端使用 `OffsetDateTime` 输出，前端按偏移解析，不依赖服务器所在时区 |
| 布尔 | `true` / `false`，字段名用 `isXxx` 或形容词，例如 `expired` |
| 枚举 | 大写字符串，例如 `"ONLINE"`、`"EXPIRED"`，不使用数字魔法值 |

## 安全

- 前端不传递、也不保存任何第三方密钥。推广链接由后端生成后返回给前端。
- 浏览器端的跨域白名单通过后端配置 `CORS_ALLOWED_ORIGINS` 设置；小程序请求不受 CORS 限制。
- 管理后台登录使用服务端会话。会话 Cookie 为 HttpOnly，开发环境 `SameSite=Lax`；生产环境额外设置 `Secure`。浏览器不保存密码或长期令牌。
- 变更请求需要 CSRF：先 `GET /api/admin/v1/auth/csrf`，再在后续 POST/PUT/DELETE 带上 `X-XSRF-TOKEN`（值来自 `XSRF-TOKEN` Cookie）。
- 用户端登录尚未实现。

## 已有接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/health` | 健康检查。`data`：`{ status: "UP", database: "UP" \| "DOWN", time }` |
| GET | `/api/v1/categories` | 启用的分类，按 `sort_order` 升序 |
| GET | `/api/v1/deals` | 公开好价分页列表，支持分类筛选、关键词搜索、排序 |
| GET | `/api/v1/deals/{id}` | 公开好价详情 |

### GET /api/v1/categories

```json
{ "code": 0, "message": "成功", "data": [ { "id": 1, "name": "数码家电", "code": "digital", "icon": null } ] }
```

### GET /api/v1/deals

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `page` | number | 默认 1，最小 1 |
| `size` | number | 默认 20，范围 1～50 |
| `categoryId` | number | 可选，正整数 |
| `keyword` | string | 可选，最长 50 个字符，去除首尾空白后为空则忽略。匹配商品名称、品牌、分类名称（不区分大小写，`%`、`_` 按字面匹配） |
| `featured` | boolean | 可选，`true` 只返回编辑精选 |
| `sortBy` | string | 可选，白名单：`LATEST`（默认，发布时间倒序）、`PRICE_ASC`、`PRICE_DESC`；其他值返回 40000 |

只返回公开可见的好价：已发布、已到发布时间、在促销有效期内、关联商品正常且未删除。

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 2,
        "title": "极光 65W 氮化镓三口快充充电器 USB-C 双口 + USB-A",
        "brand": "极光",
        "imageUrl": null,
        "categoryId": 1,
        "categoryName": "数码家电",
        "currentPrice": "89.90",
        "originalPrice": "129.00",
        "currency": "CNY",
        "dealDescription": "店铺满 2 件 9 折，单件券后价约 89.9 元",
        "sourceType": "DEMO",
        "sourceName": "演示商城·旗舰店",
        "expiresAt": null,
        "verifiedAt": null,
        "publishedAt": "2026-10-18T08:30:00+08:00"
      }
    ],
    "total": 12,
    "page": 1,
    "size": 20
  }
}
```

- `originalPrice` 是参考价，可为 `null`，只用于展示，不代表经过核验的优惠幅度。
- `sourceType`：`DEMO`（演示数据，前端必须提示）、`EDITOR`、`USER_SUBMISSION`、`AFFILIATE`。
- `verifiedAt` 为 `null` 表示未核验。

### GET /api/v1/deals/{id}

在列表字段的基础上，增加 `productId`、`specification`、`description`、`startsAt`、`purchaseUrl`、`promotionUrl`：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 1,
    "productId": 1,
    "title": "声途 Air3 主动降噪真无线蓝牙耳机 入耳式 长续航",
    "brand": "声途",
    "imageUrl": null,
    "categoryId": 1,
    "categoryName": "数码家电",
    "specification": "颜色：曜石黑；连接方式：蓝牙 5.3",
    "description": null,
    "currentPrice": "299.00",
    "originalPrice": "399.00",
    "currency": "CNY",
    "dealDescription": "页面领取满 299 减 100 券后到手价，优惠以下单页面为准",
    "sourceType": "DEMO",
    "sourceName": "演示商城·自营",
    "startsAt": null,
    "expiresAt": null,
    "verifiedAt": null,
    "publishedAt": "2026-10-18T09:00:00+08:00",
    "purchaseUrl": null,
    "promotionUrl": null
  }
}
```

- `purchaseUrl` / `promotionUrl` 只有在数据库中是合法 `https://` 地址时才返回，否则为 `null`。前端只有拿到合法链接时才允许跳转，并且优先使用推广链接。
- 错误：id 不是正整数时返回 40000；不存在、草稿或待审核时返回 40400 `好价不存在`；已下架、已过期或商品已下架时返回 40401 `好价已下架或已过期`。

## 管理接口

前缀 `/api/admin/v1/**`。除登录和 CSRF 外都要登录。角色：

| 能力 | ADMIN | OPERATOR |
| --- | --- | --- |
| 商品、好价草稿、提交审核、价格核验、查看审核 | 可以 | 可以 |
| 审核通过、驳回、下架 | 可以，但不能审核自己提交的好价 | 不可以 |
| 账号管理、审计查询 | 可以 | 不可以 |

请求体不能直接修改 `status`、审核人、审核时间、核验人。这些字段由对应动作接口写入。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/auth/csrf` | 签发 CSRF Cookie |
| POST | `/api/admin/v1/auth/login` | 用户名密码登录 |
| POST | `/api/admin/v1/auth/logout` | 退出并作废会话 |
| GET | `/api/admin/v1/auth/me` | 当前账号 |
| GET | `/api/admin/v1/dashboard` | 商品数、发布/待审/下架/过期数量、最近好价 |
| GET | `/api/admin/v1/categories` | 全部分类，含停用 |
| GET/POST | `/api/admin/v1/products` | 商品分页、新建 |
| GET/PUT/DELETE | `/api/admin/v1/products/{id}` | 详情、编辑、逻辑删除 |
| POST | `/api/admin/v1/products/{id}/disable` | 停用 |
| POST | `/api/admin/v1/products/{id}/enable` | 启用 |
| GET/POST | `/api/admin/v1/deals` | 好价分页、新建草稿 |
| GET/PUT | `/api/admin/v1/deals/{id}` | 详情；PUT 只允许草稿和待审核 |
| GET | `/api/admin/v1/deals/{id}/reviews` | 审核历史 |
| POST | `/api/admin/v1/deals/{id}/submit` | 草稿提交审核 |
| POST | `/api/admin/v1/deals/{id}/approve` | 审核通过并发布，ADMIN，且不能自审 |
| POST | `/api/admin/v1/deals/{id}/reject` | 驳回为草稿，原因必填 |
| POST | `/api/admin/v1/deals/{id}/offline` | 下架已发布好价，原因必填 |
| POST | `/api/admin/v1/deals/{id}/revise` | 已发布好价变更后回到待审核 |
| POST | `/api/admin/v1/deals/{id}/reopen` | 已下架或已过期退回草稿，不直接发布 |
| GET | `/api/admin/v1/reviews` | 待审核列表 |
| GET/POST | `/api/admin/v1/price-history` | 查询或写入人工核验；核验人与时间由服务端决定 |
| GET | `/api/admin/v1/audit-logs` | 审计，仅 ADMIN |
| GET/POST | `/api/admin/v1/users` | 账号列表和新建，仅 ADMIN |
| POST | `/api/admin/v1/users/{id}/disable` | 停用账号 |
| POST | `/api/admin/v1/users/{id}/enable` | 启用账号 |

好价状态：`DRAFT → PENDING_REVIEW → PUBLISHED`。驳回为 `PENDING_REVIEW → DRAFT`。下架为 `PUBLISHED → OFFLINE`。`OFFLINE` / `EXPIRED` 只能 reopen 成草稿。没有通用状态修改接口。价格历史只在核验接口写入，浏览和保存草稿不会写。`sourceType=DEMO` 或商品编码以 `DEMO-` 开头时拒绝写入价格历史。
