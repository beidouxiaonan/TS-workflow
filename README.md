# TS-workflow

知识工单系统公开源码快照，后端版本 1.0.9。

- Web 界面原型：app/
- Java 8 / Spring Boot REST API：backend/
- API 参数文档：backend/docs/rest-api.md
- MySQL 8.0初始化 SQL：backend/sql/init-mysql.sql
- 部署说明：backend/README.md

本仓库为脱敏后的最新源码，不包含原始提交历史、真实环境配置、运行日志、构建产物或第三方驱动二进制。页面示例仅用于演示。

## 后端构建

在 backend/ 执行 mvn package，Maven 自动获取 MySQL Connector/J 8.0.33 并打入可执行 JAR。无需放置本地驱动。请先配置独立测试数据库，再运行部署脚本。

## 安全提示

当前版本未实现完整登录认证和审批授权。不可直接以公网生产服务部署，需接入可信认证、授权及环境安全配置。所有 deployment 示例中的账号和地址均须按自己的环境填写；不要提交真实密码。
