# 航科易网通物联网技术学院考勤系统

## 项目结构
- attendance-backend: Spring Boot + MyBatis Plus + JWT
- attendance-frontend: Vue3 + Element Plus

## 后端启动
1. 创建 MySQL 数据库并执行 `attendance-backend/src/main/resources/sql/schema.sql`
2. 设置运行环境变量（不要把真实密码提交到仓库）:
   - `DB_PASSWORD`: MySQL 密码
   - `JWT_SECRET`: 至少 32 字节的随机 JWT 密钥，可用 `openssl rand -base64 48` 生成
   - 首次初始化且数据库内没有管理员时，设置 `BOOTSTRAP_ADMIN_PASSWORD`（至少 10 位）；创建成功后删除该变量
3. 启动:
   - `cd attendance-backend`
   - `mvn spring-boot:run`

生产环境启用 `prod` Profile。当前未配置 HTTPS，应用不会将 HTTP 强制跳转到 HTTPS；日后配置证书后可将 `REQUIRE_HTTPS` 设为 `true`，并由反向代理转发 `X-Forwarded-Proto`。

## 前端启动
1. `cd attendance-frontend`
2. `npm install`
3. `npm run dev`

## 管理员账号
- 默认用户名为 `admin`，首次启动密码由 `BOOTSTRAP_ADMIN_PASSWORD` 提供
- 系统不再内置或公开默认密码

## 已实现模块
- 登录鉴权(JWT)
- 部员管理
- 学生管理/Excel导入(xlsx，支持下载模板；学号、姓名、班级为必填表头，列顺序不限)
- 班级查询
- 考勤计划管理
- 部员待点名任务、点名提交
- 考勤记录查询与导出
- 统计分析(按班级与状态)
