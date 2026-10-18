# 架构说明

## 总体结构

```
微信小程序 / H5（frontend，uni-app）      管理后台（后续，独立 Vue 3 项目）
                │  HTTPS + JSON                    │
                └──────────────┬───────────────────┘
                               ▼
                 Spring Boot 单体应用（backend）
                               │
                     MySQL（MyBatis-Plus）
                               │
                 推广联盟 API 等第三方服务（后续，仅后端调用）
```

原则：

- **单体后端**：不拆微服务，不引入消息队列、缓存集群等基础设施；确有需要时再评估。
- **前端不持有任何密钥**：推广联盟凭证、AppSecret 只存在于后端配置/环境变量中；小程序只调用自家后端，由后端代理调用联盟 API、生成推广链接。
- **一套 API 多端复用**：小程序、H5、管理后台都走同一个后端，用路径前缀区分 C 端与管理端（见 [API 约定](api-conventions.md)）。

## 技术选型与版本

| 层 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| 前端框架 | uni-app（Vue 3 + Vite CLI 工程） | `@dcloudio/*` 3.0.0-5020420260813003，编译器 5.24 | 来自官方 `dcloudio/uni-preset-vue` 的 `vite-ts` 模板，构建配置未改动 |
| 语言 | TypeScript | 4.9（模板自带） | 模板锁定的版本，未擅自升级 |
| UI | `@dcloudio/uni-ui` | 1.5.12 | 通过 easycom 按需引入 |
| 状态 | Pinia | 2.1.7 | uni-app 锁定 Vue 3.4.21，Pinia ≥2.2.8 要求 Vue ≥3.5，故使用 2.1.7 |
| 样式 | Sass | 1.78.0 | uni-ui 需要；1.79+ 会对 Vite 5 使用的旧 API 输出大量弃用警告 |
| 前端单元测试 | Vitest | 2.1.9 | 兼容 Vite 5，复用工程内的 Vite 5.2.8 |
| 后端框架 | Spring Boot | 4.1.1 | 3.5.x 开源支持已于 2026-06 结束，新项目直接使用 4.x |
| JDK | Java | 21 (LTS) | |
| ORM | MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot4-starter`） | |
| 数据库 | MySQL | 8.x | 驱动 `mysql-connector-j` 由 Spring Boot 管理版本 |

## 前端结构（frontend/src）

```
src/
├── pages/          页面，一个目录一个页面；路由和 TabBar 在 pages.json 中配置
├── components/     跨页面复用的业务组件
├── api/            后端接口请求函数，按业务模块分文件，只负责“调哪个接口、参数和返回类型”
├── services/       业务逻辑：组合多个 api、数据转换、状态判断，页面优先调用 service
├── platform/       平台适配：封装 uni.* 及条件编译（#ifdef MP-WEIXIN 等）
├── stores/         Pinia，仅放跨页面共享状态（目前只有 category：首页 → 分类 Tab 传递选中分类）
├── mock/           【演示数据】分类、好价、热门搜索词；只允许 services 引用
├── types/          与后端约定的公共类型（ApiResponse、PageResult、Deal、Category）
├── utils/          request.ts、format.ts（价格/时间格式化）、navigation.ts（路由）、palette.ts（色板）
├── static/tabbar/  TabBar 图标，由 scripts/generate-tabbar-icons.mjs 生成
├── config/         env.ts：统一读取环境变量
├── styles/         global.scss 全局样式
├── uni.scss        主题变量（自动注入每个 scss 样式块，只能放变量）
├── pages.json      路由、TabBar、easycom 配置
└── manifest.json   应用配置（微信 AppID 等）
```

调用链：`页面 → services → api → utils/request → 后端`。

第二阶段的 `services/deal.ts` 暂由 `mock/` 实现，函数都返回 `Promise`，签名和第三阶段的接口调用一致。接入后端时只需替换 service 的函数体，页面不用改。

- `utils/request.ts` 负责拼接 `VITE_API_BASE_URL`、解析统一响应体；`code === 0` 时只返回 `data`，否则抛出 `ApiError`。它不弹提示，由页面决定怎样提示用户。
- 简单页面可以直接调用 `api`，有业务逻辑时再提取到 `services`，不要为了分层而分层。

页面与路由（路径常量见 `utils/navigation.ts`）：

| 页面 | 路径 | TabBar | 状态 |
| --- | --- | --- | --- |
| 首页 | `pages/index/index` | ✔ | 第二阶段完成（自定义导航栏） |
| 分类 | `pages/category/index` | ✔ | 第二阶段完成 |
| 我的 | `pages/mine/index` | ✔ | 静态占位，入口提示“后续开放” |
| 搜索 | `pages/search/index?keyword=` | | 第二阶段完成，前端静态搜索 |
| 好价详情 | `pages/deal/detail?id=` | | 第二阶段完成 |
| 爆料 | `pages/submit/index` | | 占位，从“我的”进入 |

公共组件：`DealCard`（商品卡片）、`PriceText`（价格）、`DealImage`（商品图片/占位图）、`EmptyState`（空状态）、`CategoryIcon`（分类图标）、`DemoNotice`（演示数据提示）。

## 后端结构（backend/src/main/java/com/greatprice）

```
com.greatprice
├── GreatPriceApplication
├── controller/     REST 接口，只做参数接收、校验和调用 service
├── service/        业务逻辑与事务
├── mapper/         MyBatis-Plus Mapper（BaseMapper<Entity>），复杂 SQL 放 resources/mapper/*.xml
├── entity/         数据库表实体，不直接暴露给接口
├── dto/            接口入参（XxxRequest）与出参（XxxResponse）
├── config/         MybatisPlusConfig（分页、Mapper 扫描）、WebConfig（CORS）
└── common/
    ├── api/        ApiResponse、PageResult、ErrorCode
    └── exception/  BizException、GlobalExceptionHandler
```

已有基础能力：

- **统一响应**：`ApiResponse<T>(code, message, data)`。
- **全局异常处理**：业务异常、参数校验、JSON 格式错误、404、405、未知异常都转为统一响应；未知异常只记录日志，不向客户端暴露细节。
- **参数校验**：`spring-boot-starter-validation`，在 DTO 字段上加 `@NotBlank` 等注解，Controller 参数加 `@Valid`。
- **MyBatis-Plus**：MySQL 分页插件（单页上限 100 条）、下划线转驼峰、主键使用数据库自增。
- **健康检查**：`GET /api/v1/health`，返回应用状态和数据库连通状态。
- **环境分离**：`application.yml`（公共）+ `application-dev.yml` + `application-prod.yml`，敏感值来自环境变量或 `backend/.env`。
- **日志**：使用 Spring Boot 默认的 Logback；dev 输出到控制台并打印 SQL，prod 额外写入滚动日志文件。

## 关键决策

1. **主键用数据库自增而不是雪花 ID**：雪花 ID 超过 JavaScript `Number` 的安全整数范围，前端会丢失精度。自增 ID 足以满足当前规模。
2. **响应同时使用 HTTP 状态码和业务 code**：HTTP 状态码让网关和监控能识别错误，业务 code 让前端可以细分处理。
3. **不使用 Lombok**：DTO 和响应体使用 Java `record`，减少注解处理器带来的 JDK 兼容问题。
4. **健康检查使用自定义接口，没有引入 Actuator**：返回格式与其他接口一致；需要监控指标时再引入 Actuator。
5. **不引入 Maven Wrapper**：本机 Maven 3.9 已满足要求；需要在 CI 中统一 Maven 版本时再添加。
6. **前端保留官方模板的全部依赖和脚本**：模板里包含多端编译包（支付宝、抖音等），虽然暂时用不到，但删除后容易和官方升级路径产生偏差。

## 后续扩展

- **管理后台**：在仓库根目录新增 `admin/`（Vue 3 + TypeScript + Vite），调用 `/api/admin/v1/**`。
- **推广联盟**：在后端新增 `integration/` 或 `client/` 包封装联盟 SDK/HTTP 调用，凭证通过环境变量注入。
- **价格历史、有效性检查**：优先使用 Spring `@Scheduled` 定时任务；只有在单机无法承载时，才考虑引入任务调度组件。
