# 航科易网通物联网技术学院考勤系统

面向管理员和部员的考勤管理系统：管理员维护学生、班级和考勤计划，部员在任务时段内点名；系统提供记录查询、导出和统计。

## 技术栈与目录

| 目录 | 内容 |
| --- | --- |
| `attendance-backend/` | Java 17、Spring Boot、MyBatis-Plus、MySQL、JWT、EasyExcel |
| `attendance-frontend/` | Vue 3、Vite、Element Plus |

需要 JDK 17、Maven、Node.js/npm 和 MySQL。后端默认监听 `8080`，前端开发服务器默认监听 `5173`；生产环境端口以实际启动配置为准。

## 主要功能

- 管理员和部员登录，按角色控制访问权限；管理员可管理部员账号和班级权限。
- 管理学生、班级和考勤计划；支持 `.xlsx` 学生名单导入、模板下载以及重复学号的跳过或覆盖模式。
- 按计划生成点名任务，部员可提交点名结果和班级照片；管理员可查询、导出和统计考勤记录。
- 定时清理历史考勤记录和班级照片。**考勤记录仅保留最近一个月**，请在部署前确认这一数据保留策略符合实际要求。

## 本地启动

### 1. 初始化数据库（仅限全新空库）

创建名为 `attendance_system` 的 MySQL 数据库，然后执行 [`schema.sql`](attendance-backend/src/main/resources/sql/schema.sql)。**该脚本开头包含 `DROP TABLE`，会删除已有数据；升级现有系统时绝对不要重新执行。**

### 2. 配置后端

通过环境变量提供敏感配置，不要把真实密码或密钥写进仓库：

| 变量 | 用途 | 说明 |
| --- | --- | --- |
| `DB_PASSWORD` | 数据库密码 | 必填 |
| `JWT_SECRET` | JWT 签名密钥 | 必填；使用至少 32 字节的随机值，生产环境保持稳定 |
| `DB_USERNAME` | 数据库用户名 | 默认 `root`；生产环境建议使用专用账号 |
| `BOOTSTRAP_ADMIN_PASSWORD` | 首次创建管理员 | 仅当数据库中尚无管理员时设置，长度 10–72 位；创建成功后移除 |
| `BOOTSTRAP_ADMIN_USERNAME` | 初始管理员用户名 | 默认 `admin` |
| `REQUIRE_HTTPS` | 是否由应用强制 HTTPS | 默认 `false`；未配置证书时保持 `false` |

其他默认配置见 [`application.yml`](attendance-backend/src/main/resources/application.yml)，包括登录失败锁定阈值和 JWT 有效期。可以用 `openssl rand -base64 48` 生成随机密钥；不要把生成结果提交到 Git。系统**不提供公开的默认管理员密码**。

```bash
cd attendance-backend
mvn test
mvn spring-boot:run
```

运行前需确保 MySQL 已启动，且上述必填环境变量已注入当前进程。登录接口为 `POST /api/auth/login`；未携带登录凭证请求 `GET /api/auth/me` 返回 `401` 属于正常行为。

### 3. 启动前端

```bash
cd attendance-frontend
npm ci
npm run dev
```

开发环境打开 `http://localhost:5173/`，Vite 会把 `/api` 代理到 `http://localhost:8080`。如果本地后端使用其他端口，同步修改 [`vite.config.js`](attendance-frontend/vite.config.js) 中的开发代理目标。

## 生产部署与升级

生产构建默认使用 `/attendance/` 作为前端访问路径，接口路径为 `/attendance/api/`。以下是通用流程，部署目录和后端端口应以服务器现有配置为准。

1. **备份**数据库、当前后端 JAR、前端静态文件以及班级照片目录。不要删除 `uploads/class-photos`。
2. **升级数据库时**不要执行 `schema.sql`。如果现有 `biz_attendance_record.class_photo_url` 列需要扩展为 `TEXT`，先核对表结构并备份，再按需执行 [`migration_20260416_class_photo_url_text.sql`](attendance-backend/src/main/resources/sql/migration_20260416_class_photo_url_text.sql)。
3. **构建后端**：在 `attendance-backend/` 执行 `mvn test` 和 `mvn package`，上传 `target/attendance-backend-1.0.0.jar`，替换服务器原 JAR，并重启**对应的 Java 进程**。只覆盖文件不会让已运行的进程加载新代码。
4. **构建前端**：在 `attendance-frontend/` 执行 `npm ci` 和 `npm run build`，把 `dist/` **里面的内容**放到 `/attendance/` 对应的网站目录；应能直接找到 `attendance/index.html`，不要多套一层 `dist/`。
5. **核对代理**：Nginx 的 `/attendance/api/` 必须转发到考勤后端的 `/api/`。项目默认后端端口是 `8080`，但现有服务器可能使用 `8081`；不要仅凭默认值修改线上端口，更不要误指向同机其他 Java 系统。修改 Nginx 后先检查配置，再重载；只更新 JAR 和静态文件且代理未变时，通常不需重启 Nginx。
6. **验收**：打开 `http://<域名或IP>/attendance/`，重新登录，检查模板下载、学生导入、班级列表和点名任务。无凭证访问 `/attendance/api/auth/me` 返回 `401`，说明请求已到达受保护的后端接口，并不代表部署失败。

Nginx 路径映射示例（端口和静态目录仅为示例，应与服务器实际配置一致）：

```nginx
location = /attendance {
    return 301 /attendance/;
}

location ^~ /attendance/api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    client_max_body_size 40M;
    proxy_read_timeout 120s;
}

location ^~ /attendance/ {
    alias /www/wwwroot/attendance_frontend/;
    try_files $uri $uri/ /attendance/index.html;
}
```

`/attendance` 到 `/attendance/` 的 `301` 只补路径末尾的斜杠，**不是跳转到 HTTPS**。生产配置示例见 [`application-prod.yml`](attendance-backend/src/main/resources/application-prod.yml)。如果现有启动命令明确指定了服务器上的外部 `application-prod.yml`，还需检查那份外部文件；更新仓库文件不会自动覆盖它。未配置证书时确保 `security.require-https=false`。HTTP 登录不具备传输加密，具备条件后应配置 HTTPS。

## 学生名单导入

管理员在“学生管理”页面点击“下载导入模板”，填写后上传 `.xlsx`。也可以直接导入已有名单：第一行必须包含 **学号、姓名、班级** 三个中文表头，列顺序不限；性别、年级、学院、专业、手机号、状态为可选列。例如四列表头 `姓名、学号、性别、班级` 可直接识别。

- “导入跳过重复”：已有学号不更新，并计入失败数。
- “导入覆盖更新”：已有学号按文件内容更新。上传前请确认不会误覆盖现有资料。
- 导入结束后核对成功、失败数量和班级列表；不要把真实学生名单提交到 Git 仓库。

## 时间与数据保留

- 默认时区为 `Asia/Shanghai`，可通过 `attendance.timezone` 调整；服务器时间也应保持准确。
- 系统每 5 分钟检查并补生成当日任务。计划时段开始后若任务未出现，先检查计划是否启用、生效日期、星期、班级内是否有学生，以及后端进程和日志。
- 考勤记录每天 `03:10` 清理一个月前的数据。默认每月 1 日 `03:20` 检查并删除修改时间超过 30 天的班级照片，因此照片实际保留时间可能长于 30 天。照片目录、保留天数和清理时间由 `attendance.class-photo.*` 配置控制；生产环境务必备份上传目录。

## 常见问题

- **页面能打开，接口却报错**：分别确认运行中的 JAR 路径与启动时间、实际监听端口、Nginx 的 `/attendance/api/` 转发目标；同机其他 Java 应用可能也占用端口。
- **无痕窗口正常、普通窗口异常**：先退出登录并强制刷新，再检查旧浏览器缓存和登录信息。多个应用共用同一域名/IP 时，清除整个站点数据可能让其他应用一并退出。
- **接口返回 `401`**：先确认是否已登录、请求是否携带有效 Token；无凭证请求受保护接口返回 `401` 是预期行为。
- **更新配置没有生效**：检查进程实际加载的外部配置文件，修改后重启后端；Nginx 配置修改则应先测试语法再重载。

不要公开数据库密码、JWT 密钥、管理员密码或生产环境的学生数据。历史 Git 提交中曾出现过的凭据即使在新提交中删除，也可能仍留在历史记录里；线上若沿用这些值，应进行轮换。
