-- 开发环境初始化数据库。执行顺序：
--   1. init.sql       创建数据库
--   2. schema.sql     创建业务表（可重复执行，不会修改已有表）
--   3. demo-data.sql  【仅开发环境】写入演示数据（可重复执行）
CREATE DATABASE IF NOT EXISTS great_price DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
