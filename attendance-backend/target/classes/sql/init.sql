-- 请在 MySQL 8 执行
CREATE DATABASE IF NOT EXISTS attendance_system DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE attendance_system;

SET NAMES utf8mb4;

-- 角色、用户、班级、学生、计划、任务、记录等表结构
SOURCE ./schema.sql;
