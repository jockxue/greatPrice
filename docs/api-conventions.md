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
| 40400 | 404 | 资源不存在 |
| 40500 | 405 | 请求方法不支持 |
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

请求：`page` 从 1 开始，默认值为 1；`size` 默认 20，最大 100（后端分页插件会强制这个上限）。

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
- 登录态方案（微信登录、token 格式、请求头）将在用户模块实现时补充到本文档。

## 已有接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/health` | 健康检查。`data`：`{ status: "UP", database: "UP" \| "DOWN", time }` |
