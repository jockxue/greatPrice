# greatPrice 好价导购平台

类似“什么值得买”的商品好价发现与导购平台（非自营电商）。首期为微信小程序，同一套前端代码可构建 H5，后续可扩展 App；管理后台后续作为独立 Vue 3 项目复用同一套后端 API。

当前处于 **第二阶段：核心页面 UI 与静态交互**。首页、分类、搜索、详情已完成，数据来自前端演示数据（`frontend/src/mock/`），尚未接入后端业务接口。

## 目录

```
greatPrice/
├── frontend/   uni-app + Vue 3 + TypeScript（微信小程序 / H5）
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
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn spring-boot:run
curl http://localhost:8080/api/v1/health
```

前端：

```bash
cd frontend
npm ci
npm run dev:h5                # 浏览器访问 http://localhost:5173
npm run dev:mp-weixin         # 然后用微信开发者工具导入 frontend/dist/dev/mp-weixin
```

## 常用命令

| 目的 | 命令（在对应目录下执行） |
| --- | --- |
| 前端类型检查 | `npm run type-check` |
| 前端单元测试 | `npm test` |
| 构建微信小程序 | `npm run build:mp-weixin` → `frontend/dist/build/mp-weixin` |
| 构建 H5 | `npm run build:h5` → `frontend/dist/build/h5` |
| 后端测试 | `mvn test` |
| 后端打包 | `mvn package` → `backend/target/great-price-backend-0.0.1-SNAPSHOT.jar` |

生产构建前需要先配置 `frontend/.env.production.local`，详见开发指南。

## 文档

- [架构说明](docs/architecture.md)
- [开发指南](docs/development-guide.md)：启动、环境切换、测试、微信开发者工具导入、常见问题
- [API 约定](docs/api-conventions.md)
