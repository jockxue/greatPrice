# 开发指南

## 1. 环境准备

### JDK 21

本机安装了多个 JDK，但默认没有设置 `JAVA_HOME`。Homebrew 安装的 Maven 默认使用 Homebrew 的 OpenJDK 26，项目统一使用 JDK 21：

```bash
# 加到 ~/.zshrc 中可永久生效
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
java -version   # 应显示 21.x
mvn -v          # Java version 应显示 21.x
```

在 IntelliJ IDEA 中打开 `backend/pom.xml`，将 Project SDK 设置为 21。

### Node.js 与 npm

Node 20 及以上版本均可（当前在 Node 24.19 / npm 11.17 上验证）。项目使用 npm 和 `package-lock.json`，不要混用 yarn 或 pnpm。

### MySQL

需要 MySQL 8.x。依次创建数据库、业务表和演示数据：

```bash
mysql -uroot -p < backend/sql/init.sql                       # 建库
mysql -uroot -p great_price < backend/sql/schema.sql          # 建表（CREATE TABLE IF NOT EXISTS）
mysql -uroot -p great_price < backend/sql/V4__admin.sql       # 第四阶段：管理员、审核、审计字段（只执行一次）
mysql -uroot -p great_price < backend/sql/demo-data.sql       # 【仅开发环境】演示数据
```

三个脚本都可以重复执行，不会删除或修改已有数据：已存在的表会跳过；演示分类和商品按唯一编码跳过；某个演示商品已经有 `DEMO` 好价时，不会再插入。

- 演示数据共 7 个分类、14 个商品：12 条已发布好价，另有 1 条已下架、1 条草稿，用来验证它们不会出现在列表中。其中“母婴用品”分类故意没有商品，用来展示空状态。
- **生产环境禁止执行 `demo-data.sql`**。应用启动时不会自动执行任何 SQL 脚本。
- 修改已上线的表结构时，新增带版本号的变更脚本（如 `sql/V4__admin.sql`），不要直接改 `schema.sql` 中的表定义。`V4__admin.sql` 只加表和列，不改已有商品价格和状态。重复执行会因为列已存在而失败。

### 微信开发者工具

从 <https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html> 下载稳定版并安装。这是独立的 IDE，和微信客户端 `WeChat.app` 不是同一个应用。

## 2. 启动后端

```bash
cd backend
cp .env.example .env            # 修改 DB_USERNAME / DB_PASSWORD
mvn spring-boot:run
```

验证：

```bash
curl http://localhost:8080/api/v1/health
# {"code":0,"message":"成功","data":{"status":"UP","database":"UP","time":"..."}}
```

`database` 为 `DOWN` 时，说明应用已启动但连不上 MySQL。可以在控制台日志中搜索“数据库连通性检查失败”查看原因。

确认业务接口和演示数据：

```bash
curl http://localhost:8080/api/v1/categories
curl "http://localhost:8080/api/v1/deals?page=1&size=2"
curl http://localhost:8080/api/v1/deals/1
```

如果 8080 已被占用（例如 IDEA 中已有一个调试实例），可以用 `SERVER_PORT=8081 mvn spring-boot:run` 换端口，前端同时用 `VITE_API_BASE_URL=http://localhost:8081 npm run dev:h5` 指向新端口（已存在的环境变量优先于 `.env.*` 文件）。

在 IDEA 中启动：直接运行 `GreatPriceApplication`。工作目录可以是仓库根目录（IDEA 打开整个项目时的默认值）或 `backend/`，两种情况下都会读取 `backend/.env`。系统环境变量的优先级高于 `.env`。

### 配置加载规则

| 来源 | 说明 |
| --- | --- |
| `application.yml` | 公共配置，`SPRING_PROFILES_ACTIVE` 默认为 `dev` |
| `application-dev.yml` | 开发环境：本机 MySQL 默认值、打印 SQL、允许 `http://localhost:5173` 和 `http://localhost:5174` 跨域 |
| `application-prod.yml` | 生产环境：数据库配置没有默认值，日志写入 `${LOG_PATH}/great-price-backend.log` |
| `backend/.env` | 本地覆盖（不提交），格式见 `.env.example`。从仓库根目录或 `backend/` 启动都会加载 |
| 系统环境变量 | 优先级最高，生产环境使用这种方式 |

## 3. 启动前端

```bash
cd frontend
npm ci                 # 首次或 package-lock.json 变化后执行
npm run dev:h5         # H5：http://localhost:5173
npm run dev:mp-weixin  # 小程序：监听文件变化，输出到 dist/dev/mp-weixin
```

“我的”页中的“后端服务状态”用于确认前后端是否连通。首页、分类、搜索、详情的数据都来自后端接口，**需要先启动后端**；后端不可用时页面显示“加载失败”和重试按钮，不会切换到 Mock 数据。开发环境会在浏览器或开发者工具控制台输出 `[request]` 请求日志。

### 管理后台

```bash
cd admin
npm ci
npm run dev    # http://localhost:5174 ，开发请求由 Vite 代理到 http://localhost:8080
```

第一个管理员不会写在代码里。在 `backend/.env` 增加下面两项后启动后端；仅当 `admin_user` 表为空时创建，密码会做成 BCrypt 哈希，日志只打印用户名：

```bash
ADMIN_INIT_USERNAME=你的用户名
ADMIN_INIT_PASSWORD=至少10位的密码
```

已有账号后，这两个变量不会重置密码。生产环境用系统环境变量注入，会话 Cookie 会加 `Secure`。

## 4. 切换环境

### 前端

前端的环境由 Vite 的 mode 决定：`dev:*` 命令使用 `development`，`build:*` 命令使用 `production`。

| 文件 | 是否提交 | 用途 |
| --- | --- | --- |
| `.env.example` | 是 | 变量说明模板 |
| `.env.development` | 是 | 开发默认值：`VITE_API_BASE_URL=http://localhost:8080` |
| `.env.development.local` | 否 | 本机覆盖，例如真机调试时改为局域网 IP |
| `.env.production.local` | 否 | 生产 API 地址，**执行生产构建前必须创建** |

```bash
cp .env.example .env.production.local   # 修改 VITE_API_BASE_URL 为正式域名
npm run build:mp-weixin
```

如果没有配置 `VITE_API_BASE_URL`，构建仍会成功，但所有请求都会直接返回错误“VITE_API_BASE_URL 未配置”。生产构建（`build:*`）中如果地址不是 `https://` 开头，请求同样会被拒绝。

> `VITE_` 开头的变量会被打包进前端代码，**不能存放任何密钥**。

### 后端

```bash
SPRING_PROFILES_ACTIVE=prod DB_URL=... DB_USERNAME=... DB_PASSWORD=... \
  java -jar target/great-price-backend-0.0.1-SNAPSHOT.jar
```

## 5. 运行测试与检查

```bash
# 后端：单元测试 + Web 层切片测试 + 上下文加载测试 + 接口集成测试（无需启动 MySQL）
cd backend && mvn test

# 前端：TypeScript 类型检查（vue-tsc）
cd frontend && npm run type-check

# 前端：构建检查
npm run build:mp-weixin
npm run build:h5
```

后端集成测试使用 H2 内存库（MySQL 兼容模式，配置见 `src/test/resources/application-test.yml`）。表结构加载 `sql/schema.sql` 和 `sql/V4__admin.sql`，测试数据为 `src/test/resources/db/test-data.sql`。`DealApiIntegrationTest` 覆盖公开接口，`AdminApiIntegrationTest` 使用另一份 H2 库覆盖管理接口。修改 SQL 时请确认 H2 仍能解析。

```bash
cd admin && npm test && npm run type-check
```

```bash
# 前端：单元测试（Vitest，覆盖请求封装、api 参数、分页组合函数、格式化和业务规则）
cd frontend && npm test
```

前端单元测试说明：

- 测试文件与被测文件放在一起，命名为 `xxx.test.ts`。
- 只测试纯 TS 逻辑；`uni.request` 通过 `vi.stubGlobal('uni', ...)` 模拟。页面交互通过 H5 和微信开发者工具手动验证。
- 测试使用独立的 `vitest.config.mts`，不加载 uni-app 的 Vite 插件。
- `*.test.ts` 不参与 `npm run type-check`：Vitest 的类型声明会引入较新的 `@types/node`，与项目锁定的 TypeScript 4.9 不兼容。测试代码由 Vitest 直接编译执行。

## 6. 在微信开发者工具中导入

1. 先执行 `npm run dev:mp-weixin`（开发）或 `npm run build:mp-weixin`（发布）。
2. 打开微信开发者工具，选择“导入项目”。
3. 目录选择：
   - 开发：`frontend/dist/dev/mp-weixin`
   - 发布：`frontend/dist/build/mp-weixin`
4. AppID：使用自己的小程序 AppID；也可以先选择“测试号”。正式 AppID 填写在 `frontend/src/manifest.json` 的 `mp-weixin.appid` 中，避免每次构建后都要重新设置。
5. 本地开发时，在“详情 → 本地设置”中勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”，项目的 `manifest.json` 已默认设置 `urlCheck: false`。
6. 开发模式下保持 `npm run dev:mp-weixin` 运行，修改代码后开发者工具会自动刷新。

关闭域名校验只能用于本地调试，**不能作为上线方案**。正式发布前：

- 在微信公众平台“开发管理 → 开发设置 → 服务器域名”中，把后端 HTTPS 域名配置为 request 合法域名（必须是已备案的 HTTPS 域名，不能使用 IP 地址或 localhost）。
- `.env.production.local` 中的 `VITE_API_BASE_URL` 使用同一个 HTTPS 域名。
- `<image>` 加载网络图片不需要配置合法域名，但图片地址需要是 HTTPS。
- 小程序无法直接打开第三方电商网页，详情页“去购买”在小程序中会复制链接并提示用户到浏览器或购物 App 中打开；H5 中会新开窗口。
- 真机预览时，`localhost` 指向手机本身，参见下文“真机预览请求失败”。

## 7. 常见开发任务

### 新增页面

1. 新建 `src/pages/xxx/index.vue`。
2. 在 `src/pages.json` 的 `pages` 数组中注册该页面（第一项是启动页）。
3. 使用 `uni.navigateTo({ url: '/pages/xxx/index' })` 跳转；TabBar 页面需要使用 `uni.switchTab`。

### 新增接口（前后端）

1. 后端：`dto` 中定义 `XxxRequest` / `XxxResponse` → `service` → `controller`，返回 `ApiResponse<XxxResponse>`。
2. 前端：在 `src/types/xxx.ts` 中定义对应类型，在 `src/api/xxx.ts` 中写请求函数，需要业务逻辑时再写 `src/services/xxx.ts`。

### 使用 uni-ui 组件

可以直接在模板中写 `<uni-xxx>`，easycom 会自动引入组件，无需 import。组件文档：<https://uniapp.dcloud.net.cn/component/uniui/uni-ui.html>。主题色通过 `src/uni.scss` 中的 `$uni-primary` 统一修改。

## 8. 常见问题

**`npm install pinia` 报 ERESOLVE**
Pinia 2.1.7 对 `@vue/composition-api`（仅 Vue 2 使用）声明了可选 peer 依赖，npm 会误判为冲突。项目已通过 `npm install --legacy-peer-deps pinia@2.1.7` 写入 `package-lock.json`，之后正常执行 `npm ci` / `npm install` 即可。不要把 Pinia 升级到 2.2.8 及以上版本，这些版本要求 Vue 3.5。

**npm 提示 esbuild / core-js 的 install scripts 未被允许**
npm 11 默认不执行依赖包的安装脚本。esbuild 的二进制文件通过 `@esbuild/darwin-arm64` 等平台包获取，不依赖这个脚本，目前构建正常。

**构建时提示“uni-app 有新版本发布”**
这只是提示，不影响使用。升级 uni-app 需要用 `npx @dcloudio/uvm@latest` 统一升级所有 `@dcloudio/*` 包，属于单独的变更，需要先评估再执行。

**真机预览请求失败**
手机上的 `localhost` 指向手机本身。需要在 `.env.development.local` 中把地址改为电脑的局域网 IP（例如 `http://192.168.1.10:8080`），并确保手机和电脑在同一网络。

**`mvn test` 输出 Mockito 动态加载 agent 的 WARNING**
JDK 21 对动态加载 Java Agent 的提示，不影响测试结果。
