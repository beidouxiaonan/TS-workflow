# 知识工单 REST API

独立 Spring Boot 2.7 后端，仅提供 JSON API，不包含前端和 Docker 配置。运行环境为 Java 8+，数据持久化使用 OceanBase MySQL 兼容模式。

## 构建与运行

```bash
mvn clean package
java -jar target/knowledge-ticket-api.jar
```

### 一键部署

### 多环境配置（test1 / test2 / prod）

三套环境共用同一个 JAR。`application.yml` 保存公共配置，环境文件覆盖数据源：

| 环境 | 配置文件 | 连接配置 |
| --- | --- | --- |
| 测试一 | `application-test1.yml` | URL、用户名、密码均从 `deployment.test1.env` 读取 |
| 测试二 | `application-test2.yml` | URL、用户名、密码均由本环境提供 |
| 生产 | `application-prod.yml` | URL、用户名、密码均由本环境提供；禁止自动建表 |

三套 YAML 均引用 `NUZAR_DB_URL`、`NUZAR_DB_USERNAME`、`NUZAR_DB_PASSWORD`，连接信息统一写入各环境的 `deployment.<环境>.env`。部署脚本自动加载并导出这些变量，无需重新编译即可更换连接。启用这些环境后不再使用默认配置中的 `DB_*`。

```bash
# 首次缺少配置文件时才复制，避免覆盖已有连接信息
test -f deployment.test1.env || cp deployment.test1.env.example deployment.test1.env
chmod 600 deployment.test1.env
# 编辑 deployment.test1.env，确认 URL、用户名和密码
bash deploy.sh deploy test1
bash deploy.sh status test1
bash deploy.sh restart test1
```

测试二、生产同理，分别复制相应 `.env.example`，填写连接信息后执行 `bash deploy.sh deploy test2` 或 `bash deploy.sh deploy prod`。真实配置文件不提交版本库。

直接启动 JAR 也可以通过参数选择环境（先设置对应 NUZAR_DB_* 环境变量）：

```bash
java -jar target/knowledge-ticket-api.jar --spring.profiles.active=test1
```

运行目录、PID、备份和日志按环境隔离在 `runtime/test1/`、`runtime/test2/`、`runtime/prod/`。同一服务器并行运行时须设置不同 `SERVER_PORT`。首次从旧默认部署切换时，先执行 `bash deploy.sh stop` 停止旧 `runtime/` 服务，再部署所选环境。以后所有运维命令都携带环境名。

### 默认环境（兼容原部署方式）

```bash
cp deployment.env.example deployment.env
# 编辑 deployment.env 中的 OceanBase 连接信息
./deploy.sh
```

常用运维命令：

```bash
./deploy.sh status
./deploy.sh logs
./deploy.sh restart
./deploy.sh stop
```

脚本会检查 Java 8、备份旧 JAR、使用 TERM 信号平滑停止、原子替换 JAR、启动服务，并等待 `/api/health` 健康检查通过。旧版本保存在 `runtime/backup/`。

默认监听 `8080`。更换 OceanBase 时只需修改环境变量，无需重新编译：

```bash
export DB_URL='jdbc:oceanbase://oceanbase-host:2881/knowledge_ticket?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
export DB_USERNAME='app_user@tenant'
export DB_PASSWORD='your-password'
java -jar target/knowledge-ticket-api.jar
```

项目使用随包提供的 `oceanbase-client-2.4.14.1.jar`，驱动类为 `com.oceanbase.jdbc.Driver`，最终可执行 JAR 已内嵌该驱动，不需要在服务器额外安装 JDBC 包。

全新环境执行 [init-oceanbase.sql](sql/init-oceanbase.sql)，包含建库、选择数据库和最新两张业务表，默认库名 ekbdb，需与 JDBC URL 一致。没有建库权限时由 DBA 建库，然后在目标库执行 [schema-oceanbase.sql](src/main/resources/db/schema-oceanbase.sql)。生产环境默认不自动建表；临时环境可设置 `DB_INIT_MODE=always`。CREATE TABLE IF NOT EXISTS 不会升级已有表：已有库仍需按实际结构执行未执行的 V2/V3/V4 迁移；新库不需要重复执行这些迁移。

首次启动执行 `bash deploy.sh start test1` 或 `bash deploy.sh restart test1`：若 runtime/test1/knowledge-ticket-api.jar 不存在，自动从 target/knowledge-ticket-api.jar 复制后启动。可在 deployment.test1.env 配置 SOURCE_JAR 指向已有发布包，相对路径以 deploy.sh 所在目录为准。脚本不自动编译，也不执行数据库 SQL；源包不存在会明确报错。runtime 已有 JAR 时 start/restart 不替换它，更新版本仍使用 deploy。省略环境名则使用 runtime 目录。

## Swagger / OpenAPI

- Swagger UI：`http://服务器地址:8080/swagger-ui.html`
- OpenAPI JSON：`http://服务器地址:8080/v3/api-docs`

## 日志与清理策略

日志同时输出到控制台和文件。默认文件为运行目录下的 `./logs/knowledge-ticket-api.log`，历史日志位于 `./logs/archive/`。

- 单文件达到 `50MB` 或跨天时滚动并 gzip 压缩
- 默认保留 `14` 天
- 全部归档总容量最多 `2GB`，超限自动删除最旧文件
- 文件日志异步写入，队列拥塞时丢弃低级别日志，避免异常日志拖垮接口线程
- Tomcat、连接池、JDBC 和 Swagger 默认降为 `WARN`

可以通过环境变量覆盖：

```bash
export LOG_PATH=/data/logs/knowledge-ticket
export LOG_LEVEL=INFO
export LOG_MAX_FILE_SIZE=50MB
export LOG_MAX_HISTORY=14
export LOG_TOTAL_SIZE_CAP=2GB
```

## API

完整字段级参数文档见 [docs/rest-api.md](docs/rest-api.md)，范围仅包含下表接口。

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查 |
| GET | `/api/v1/tickets` | 工单列表，可用 `status`、`applicant` 筛选 |
| GET | `/api/v1/tickets/{id}` | 工单详情及审批记录 |
| POST | `/api/v1/tickets` | 创建草稿 |
| PUT | `/api/v1/tickets/{id}` | 更新工单 |
| POST | `/api/v1/tickets/{id}/submit` | 提交申请，请求体可携带 `attachmentIds` |
| POST | `/api/v1/tickets/{id}/approve` | 审核通过 |
| POST | `/api/v1/tickets/{id}/reject` | 退回修改 |
| POST | `/api/v1/tickets/{id}/complete-task` | 完成任务并提交验收 |
| GET | `/api/v1/tickets/workflow` | 查询流程节点 |

创建或更新工单时，`attachmentIds` 是普通字符串字段；submit 无请求体，使用已保存的数据。多个附件 ID 可使用逗号分隔；后端原样保存到 OceanBase `TEXT` 列，不解析为 Java List。旧版本已保存的 JSON 数组文本也会原样读取。再次更新时以最新字符串覆盖数据库内容，因此新增、替换和删除附件都会被持久化：

```json
{
  "title": "新增知识库操作指引",
  "applicant": "付源",
  "reason": "补充最新操作说明",
  "attachmentIds": "ATT-20260910-000128,ATT-20260910-000129"
}
```

新数据库直接执行完整建表脚本。已经执行过 V2 的数据库，请继续执行 `src/main/resources/db/migration/V3__support_multiple_attachment_ids.sql`，原单个附件 ID 会自动转为数组。

提交申请示例：

```json
{"attachmentIds":"ATT-20260910-000128,ATT-20260910-000129"}
```

审核请求示例：

```json
{"reviewer":"付源","comment":"内容核验通过"}
```
### 部署脚本环境与 Java 路径

1.0.6 列表接口变更：GET /api/v1/tickets 支持 title 包含搜索、applicant 精确筛选和 page/pageSize 分页（默认 1/20，每页最多 100）。返回值改为 {items,total,page,pageSize,totalPages}，前端须从 items 读取列表。owner、status、currentNode 筛选仍有效；无需数据库结构升级。

在对应 `deployment.test1.env`、`deployment.test2.env` 或 `deployment.prod.env` 中填写 `JAVA_HOME="实际 JDK 8 安装目录"`，例如 `/opt/jdk1.8.0_422`。deploy.sh 直接读取该配置并调用其 bin/java，无需修改脚本。留空则使用 PATH 中的 java。stop/status 不需要 Java 环境。仅修改部署脚本或环境配置时无需递增版本号或重新打包 JAR。

使用 Bash 执行且各操作必须携带一致的环境名，例如 `bash deploy.sh status test1`、`bash deploy.sh stop test1`、`bash deploy.sh deploy test1`。省略环境名且未设置 SPRING_PROFILES_ACTIVE 时只检查默认 runtime 目录，不会操作其他环境。多套环境在同一主机运行时须分别配置不同 SERVER_PORT。

PID 文件缺失、路径更改或手动启动的进程不能被可靠识别，脚本不会按模糊名称批量杀进程。此时请核对运行命令和环境目录后人工处理。服务存活检查不等于健康检查。

### 1.0.8 期望完成时间

新增可选字段 expectedCompletionTime，创建/更新接收、所有工单对象响应返回。示例 `2026-09-30T18:00:00.000+08:00`。已有库部署前执行 `src/main/resources/db/migration/V5__add_expected_completion_time.sql` 一次；新库使用最新 `sql/init-oceanbase.sql`，无需再执行 V5。
