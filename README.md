# 航科易网通物联网技术学院考勤系统

## 项目结构
- attendance-backend: Spring Boot + MyBatis Plus + JWT
- attendance-frontend: Vue3 + Element Plus

## 后端启动
1. 创建 MySQL 数据库并执行 `attendance-backend/src/main/resources/sql/schema.sql`
2. 修改 `attendance-backend/src/main/resources/application.yml` 数据库账号密码
3. 启动:
   - `cd attendance-backend`
   - `mvn spring-boot:run`

## 前端启动
1. `cd attendance-frontend`
2. `npm install`
3. `npm run dev`

## 默认账号
- 部长: `admin`
- 密码: `123456`

## 已实现模块
- 登录鉴权(JWT)
- 部员管理
- 学生管理/Excel导入(xlsx)
- 班级查询
- 考勤计划管理
- 部员待点名任务、点名提交
- 考勤记录查询与导出
- 统计分析(按班级与状态)
