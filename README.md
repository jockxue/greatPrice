# greatPrice 好价导购平台

类似“什么值得买”的商品好价发现与导购平台（非自营电商）。首期为微信小程序，同一套前端代码可构建 H5，后续可扩展 App；管理后台后续作为独立 Vue 3 项目复用同一套后端 API。

当前处于 **第四阶段：管理后台**。用户端首页、分类、搜索、详情继续读取 MySQL；运营后台在 `admin/`，通过 `/api/admin/v1/**` 管理商品、好价审核、发布和下架、价格核验。开发库中的商品仍是标注了 `DEMO` 的演示数据。用户端登录、收藏和推广联盟尚未开发。

## 目录

```
greatPrice/
├── frontend/   uni-app + Vue 3 + TypeScript（微信小程序 / H5）
├── admin/      Vue 3 + TypeScript + Vite + Element Plus（运营后台）
├── backend/    Spring Boot + MyBatis-Plus + MySQL
└── docs/       架构、开发指南、API 约定
```

## 环境要求

| 工具 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 后端编译和运行 |
| Maven | 3.9+ | |
| Node.js | 20+（当前在 24 上验证） | |
| npm | 随 Node 安装 | 使用 `package-lock.json` 锁定依赖 |
| MySQL | 8.x | |
| 微信开发者工具 | 最新稳定版 | 预览和调试小程序 |

## 快速开始

后端（默认 `dev` 环境，端口 8080）：

```bash
cd backend
cp .env.example .env          # 填写本地 MySQL 账号密码
# 首次：建库、建表、写入演示数据（三个脚本均可重复执行）
mysql -uroot -p < sql/init.sql
mysql -uroot -p great_price < sql/schema.sql
mysql -uroot -p great_price < sql/V4__admin.sql
mysql -uroot -p great_price < sql/demo-data.sql
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
# IDEA 直接运行 GreatPriceApplication 时，工作目录为仓库根目录或 backend/ 都可以读到 backend/.env
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/deals?page=1&size=2"
```

用户端：

```bash
cd frontend
npm ci
npm run dev:h5                # 浏览器访问 http://localhost:5173
npm run dev:mp-weixin         # 然后用微信开发者工具导入 frontend/dist/dev/mp-weixin
```

管理后台（先启动后端；第一个管理员用环境变量创建，见开发指南）：

```bash
cd admin
npm ci
npm run dev                   # 浏览器访问 http://localhost:5174
```

## 常用命令

| 目的 | 命令（在对应目录下执行） |
| --- | --- |
| 前端类型检查 | `npm run type-check` |
| 用户端单元测试 | 在 `frontend/` 执行 `npm test` |
| 管理后台单元测试 | 在 `admin/` 执行 `npm test` |
| 管理后台类型检查 | 在 `admin/` 执行 `npm run type-check` |
| 构建微信小程序 | `npm run build:mp-weixin` → `frontend/dist/build/mp-weixin` |
| 构建 H5 | `npm run build:h5` → `frontend/dist/build/h5` |
| 构建管理后台 | 在 `admin/` 执行 `npm run build` |
| 后端测试 | `mvn test` |
| 后端打包 | `mvn package` → `backend/target/great-price-backend-0.0.1-SNAPSHOT.jar` |

生产构建前需要先配置 `frontend/.env.production.local`，详见开发指南。

## 文档

- [架构说明](docs/architecture.md)
- [开发指南](docs/development-guide.md)：启动、环境切换、测试、微信开发者工具导入、常见问题
- [API 约定](docs/api-conventions.md)
