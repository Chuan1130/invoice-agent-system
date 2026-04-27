# invoice-agent-system

智能发票 / 报销审核多 Agent 系统

## 当前阶段
- 初始化项目结构
- 搭建本地开发环境
- 准备 MySQL / Redis
- 准备 Spring Boot 后端
- 准备前端页面

## 目录结构

- backend: Spring Boot 后端
- frontend: 前端项目
- docs: 项目文档
- sql: 数据库初始化脚本
- docker: 本地依赖容器配置

## 项目目标

第一阶段先完成最小可运行闭环：

1. 上传发票图片
2. OCR 提取发票信息
3. 规则校验
4. 查询历史记录
5. 输出审核结果

## 技术栈（计划）

- Java
- Spring Boot
- MyBatis
- MySQL
- Redis
- Docker Compose
- Vue / React
