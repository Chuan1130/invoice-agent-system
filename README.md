# invoice-agent-system

智能发票 / 报销审核多 Agent 系统。

本项目目标是构建一个面向企业报销场景的智能发票审核系统。系统支持发票图片上传、OCR 信息提取、发票字段结构化存储、审核规则校验、风险识别，并计划进一步引入 Agent 编排能力，实现自动化报销审核流程。

## 当前进度

已完成：

* 初始化 GitHub 仓库与本地项目结构
* 使用 Docker Compose 搭建 MySQL / Redis 本地开发环境
* 创建 Spring Boot 后端项目
* 完成 `/health` 健康检查接口
* 完成发票图片上传接口
* 支持将上传图片保存到后端本地 `uploads/invoices` 目录
* 支持创建审核任务并写入 MySQL `audit_task` 表
* 使用 Postman 完成上传接口测试

当前开发阶段：

* 正在实现 OCR mock 与 `invoice_info` 发票信息落库

## 技术栈

后端：

* Java
* Spring Boot
* Spring Web
* MyBatis
* MySQL
* Redis
* Maven

开发与部署：

* Docker Compose
* Git / GitHub
* IntelliJ IDEA
* Postman

计划引入：

* OCR 服务
* 审核规则引擎
* Spring AI / Agent 编排
* 前端页面
* 报销审核报告生成

## 项目结构

```text
invoice-agent-system
├─ backend
│  └─ invoice-agent-backend
├─ frontend
├─ docker
│  └─ docker-compose.yml
├─ sql
│  └─ init.sql
├─ docs
├─ README.md
└─ .gitignore
```

## 当前接口

### 健康检查

```http
GET /health
```

返回：

```text
ok
```

### 上传发票图片

```http
POST /api/invoices/upload
```

请求格式：

```text
Body: form-data
key: file
type: File
```

当前功能：

* 接收发票图片
* 保存图片到本地目录
* 创建审核任务
* 将任务信息写入 MySQL `audit_task` 表
* 返回任务 ID、任务编号、任务状态和文件路径

## 数据库设计

当前已使用：

### audit_task

用于记录一次发票上传 / 审核任务。

核心字段：

* id
* task_no
* status
* original_file_path
* ocr_raw_text
* final_decision
* need_human_review
* created_at
* updated_at

计划使用：

### invoice_info

用于保存 OCR 后提取出的发票结构化字段。

计划字段：

* task_id
* invoice_no
* invoice_date
* amount
* tax_amount
* buyer_name
* seller_name
* invoice_type
* raw_json

### audit_rule_hit

用于保存审核规则命中结果。

### audit_task_log

用于保存审核流程日志。

## 下一步计划

1. 实现 OCR mock 服务
2. 将 mock 发票字段写入 `invoice_info` 表
3. 增加根据 taskId 查询发票详情接口
4. 接入真实 OCR 服务
5. 实现基础审核规则
6. 增加重复发票检测
7. 引入 Agent 编排能力
8. 构建前端页面
