# 知识工单系统 REST API 参数文档

## 1.0.5 新增：撤销申请和删除草稿

### POST /api/v1/tickets/{id}/withdraw

id：路径参数，必填，最多 32 字符。请求体：

```json
{"applicant":"USER-001","comment":"申请内容需要调整"}
```

applicant：必填字符串，1～64 字符，不可全为空白，必须与工单申请者完全一致。comment：选填字符串，最多 1000 字符。

只允许 status=审批中 且 currentNode=领导审批/目标知识库管理员审批。成功返回 200 及更新后的工单：status=草稿、currentNode=填写申请、owner=待指派。保留原工单编号、业务内容、审批人和附件，追加撤销流程记录；可通过 applicant 与 status=草稿 查询。知识上传、验收、已完成、已退回及草稿不可撤销。

### DELETE /api/v1/tickets/{id}?applicant=USER-001

id：必填路径参数，最多 32 字符；applicant：必填 query 字符串，1～64 字符，不可全为空白，与工单申请者完全一致；无请求体。

仅允许 status=草稿或已退回，且 currentNode=填写申请。成功返回 204，无响应体；永久删除工单及关联流程记录，不调用附件服务删除文件。再次删除返回 404。

两个接口：申请者不符返回 403，状态不允许返回 400，工单不存在返回 404。当前未接入登录认证，applicant 是调用方提供的标识，比对不等于身份认证，必须由可信调用方传入；正式鉴权需对接登录身份。并发写入通过事务和工单行锁串行执行。无需数据库结构变更。

## 1. 通用约定

| 项目 | 说明 |
| --- | --- |
| 服务根地址 | `http://{host}:{port}`，默认端口 `8080` |
| API 前缀 | `/api/v1`；健康检查除外 |
| 请求格式 | 有 JSON 请求体的接口使用 `Content-Type: application/json`；submit 无请求体 |
| 响应格式 | `application/json`，UTF-8 |
| 时间格式 | `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`，时区 `Asia/Shanghai` |
| 鉴权 | 当前版本未实现鉴权，调用方不需要上传 Token |
| 文件上传 | 本文接口不接收文件二进制；文件先由附件服务上传，再传附件 ID 字符串 `attachmentIds` |
| 空值约定 | 选传字符串可省略或传 `null`；不建议传空字符串 |

### 1.1 通用工单请求字段

用于 `POST /api/v1/tickets` 和 `PUT /api/v1/tickets/{id}`。

| 字段 | 类型 | 是否必传 | 长度/数量限制 | 含义与规则 |
| --- | --- | --- | --- | --- |
| `title` | string | 是 | 1～200 字符 | 工单标题，不能只包含空白字符 |
| `applicant` | string | 是 | 1～64 字符 | 申请人姓名、工号或调用方认可的用户唯一标识 |
| `leaderId` | string | 是 | 1～64 字符，不可全为空白 | 部门领导用户 ID |
| `knowledgeBaseOwnerId` | string | 是 | 1～64 字符，不可全为空白 | 知识库拥有者用户 ID |
| `department` | string | 否 | 最多 128 字符 | 申请人所属部门 |
| `team` | string | 否 | 最多 128 字符 | 页面“团队选择”的值 |
| `knowledgeBase` | string | 否 | 最多 200 字符 | 页面“知识库选择”的目标知识库名称或标识 |
| `fileType` | string | 否 | 最多 64 字符 | 页面“文件类型选择”的值，例如 `DOCX / PDF` |
| `supplementType` | string | 否 | 最多 64 字符 | 页面“补充类型”的值，例如 `操作指引` |
| `attachmentIds` | string | 否 | 最多 16000 字符 | 附件 ID 字符串。多个 ID 可使用逗号分隔，如 `"ATT-001,ATT-002"`；空字符串表示清空附件。旧数据中的 JSON 数组文本仍会原样返回 |
| `reason` | string | 是 | 1～16000 字符 | 申请原因和具体内容，不能只包含空白字符 |
| `solution` | string | 否 | 最多 16000 字符 | 查询、实施或处理方案 |
| `expectedCompletionTime` | string(date-time) | 否 | 毫秒精度，格式 yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX | 期望完成时间，例如 2026-09-30T18:00:00.000+08:00；历史数据为 null；PUT 省略或 null 清空，保留需传原值。不自动触发超时或改变排序 |
| `priority` | string | 否 | 最多 16 字符 | 仅允许 `普通`、`高`、`紧急`；省略时为 `普通` |

以下字段由服务端维护，只在响应中返回，客户端不需要且不应上传：

| 字段 | 类型 | 数据库限制 | 含义 |
| --- | --- | --- | --- |
| `id` | string | 32 字符 | 服务端生成的工单编号 |
| `status` | string | 32 字符 | 工单状态：`草稿`、`审批中`、`处理中`、`待验收`、`已完成`、`已退回` |
| `currentNode` | string | 64 字符 | 当前流程节点 |
| `owner` | string | 64 字符 | 当前处理人，初始值为 `待指派` |
| `createdAt` | string(date-time) | 毫秒精度 | 创建时间 |
| `updatedAt` | string(date-time) | 毫秒精度 | 最后更新时间 |
| `process` | ProcessRecord[] | — | 按时间升序返回的审批、处理记录 |

### 1.2 ProcessRecord 返回字段

| 字段 | 类型 | 是否返回 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- |
| `node` | string | 是 | 最多 64 字符 | 操作发生时所在流程节点 |
| `operator` | string | 是 | 最多 64 字符 | 操作人姓名或唯一标识 |
| `action` | string | 是 | 最多 32 字符 | 操作名称，如 `提交申请`、`审核通过`、`退回修改`、`提交验收` |
| `comment` | string/null | 是 | 最多 1000 字符 | 操作或审批意见 |
| `operatedAt` | string(date-time) | 是 | 毫秒精度 | 操作时间 |

### 1.3 错误响应

```json
{
  "timestamp": "2026-09-15T10:30:00.000+08:00",
  "status": 400,
  "error": "Bad Request",
  "message": "参数校验失败信息"
}
```

| HTTP 状态 | 场景 |
| --- | --- |
| `400` | 必传字段缺失、字段超长、枚举值错误、当前流程节点不允许执行该动作 |
| `404` | 指定的工单 ID 不存在 |
| `500` | 未处理的服务端异常或数据库异常 |

## 2. 健康检查

### `GET /api/health`

请求参数：无。请求体：无。

成功响应 `200`：

```json
{"status":"UP"}
```

说明：只表示 Web 服务已启动，不执行 OceanBase 连通性检查。

## 3. 查询工单列表

### `GET /api/v1/tickets`

| 参数 | 位置 | 类型 | 是否必传 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `status` | query | string | 否 | 最多 32 字符 | 按工单状态精确筛选；省略、空字符串或全空白时排除草稿，显式传草稿查询草稿 |
| `title` | query | string | 否 | 最多 200 字符 | 标题包含搜索，去除两端空格，空白不筛选；% 和 _ 视为普通字符 |
| `page` | query | integer | 否 | 1～2147483647，默认 1 | 页码，从 1 开始 |
| `pageSize` | query | integer | 否 | 1～100，默认 20 | 每页条数 |
| `applicant` | query | string | 否 | 最多 64 字符 | 按申请人精确筛选 |
| `currentNode` | query | string | 否 | 最多 64 字符 | 当前流程节点，可与 owner、status、applicant 组合 |
| `owner` | query | string | 否 | 最多 64 字符 | 按当前处理人用户 ID 精确筛选，用于“指派给我的工单” |

示例：

```http
GET /api/v1/tickets?owner=USER-001
GET /api/v1/tickets?owner=USER-001&status=审批中
GET /api/v1/tickets?applicant=付源
```

成功响应 `200`：自 1.0.6 起返回分页对象，不再直接返回数组，前端须改用 items。按紧急、高、普通排序，未知优先级排最后；同优先级按 created_at DESC、id DESC 排序，越界页返回空 items，total 仍为所有匹配工单数量。

```json
{"items":[],"total":0,"page":1,"pageSize":20,"totalPages":0}
```

items：当前页工单对象数组；total：符合全部条件的总条数；page：请求页码；pageSize：请求每页条数；totalPages：总页数，无数据时为 0。非法页码或页大小返回 400。分页仅用于工单列表搜索，详情中的 process 保留完整记录；详情、流程定义及提交/审批等操作接口无需分页。

“我的工单”和“审核工单”共用此查询接口，不新增重复接口：

```http
GET /api/v1/tickets?owner=USER-001&title=操作指引&page=1&pageSize=20
GET /api/v1/tickets?owner=USER-001&currentNode=领导审批&applicant=USER-002&page=1&pageSize=20
GET /api/v1/tickets?applicant=USER-001&status=草稿&title=操作指引&page=2&pageSize=10
```

title 与 applicant 可以单独使用，也可以同时使用（AND）。applicant 按数据库已保存的申请者标识精确查询，不提供姓名与用户 ID 的自动转换。切换搜索条件或页大小时，前端应重置 page=1。中文与特殊字符需 URL 编码。

审核工单页三个标签的调用示例（中文参数由客户端进行 URL 编码）：

| 页面标签 | currentNode |
| --- | --- |
| 所在部门审批 | 领导审批 |
| 知识库管理员审批 | 目标知识库管理员审批 |
| 知识上传 | 知识上传 |

```http
GET /api/v1/tickets?owner=USER-001&currentNode=领导审批
GET /api/v1/tickets?owner=USER-001&currentNode=目标知识库管理员审批
GET /api/v1/tickets?owner=USER-001&currentNode=知识上传
```

currentNode 省略或空字符串时不限制节点；也支持填写申请、验收审批、完成。查询领导审批时兼容历史节点“知识库拥有者”，查询知识上传时兼容历史节点“任务处理”。返回节点名称仍使用新版名称。知识上传不是审批中状态，不应为三个标签统一附加 status=审批中。

所有传入条件按 AND 组合。除 status 外，参数省略或空字符串不筛选该字段。status 省略、空字符串或全空白时排除草稿；分页 total 同样不包含草稿。“指派给我的”传当前登录用户 ID 为 `owner`；“我提交的”传 `applicant`。不要同时传两者，除非要查询自己提交且当前指派给自己的工单。`owner` 匹配当前保存的处理人，不查询历史经办人；状态不传时不排除已完成工单。领导审批通过并转交知识库拥有者后，工单将归入后者的查询结果。

注意：该查询条件不是权限校验。当前接口没有登录用户绑定，调用方需提供用户 ID；正式访问控制仍需由后端认证授权实现。

## 4. 查询工单详情及审批记录

### `GET /api/v1/tickets/{id}`

| 参数 | 位置 | 类型 | 是否必传 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | string | 是 | 1～32 字符 | 工单编号，例如 `KB-20260915103000-1001` |

请求体：无。成功响应 `200`：一个完整工单对象，`process` 包含审批和处理记录。不存在时返回 `404`。

## 5. 创建工单草稿

### `POST /api/v1/tickets`

请求体使用“通用工单请求字段”。虽然接口名称为创建草稿，`title`、`applicant`、`reason`、`leaderId`、`knowledgeBaseOwnerId` 为必传字段。

```json
{
  "title": "新增客户准入流程操作指引",
  "expectedCompletionTime": "2026-09-30T18:00:00.000+08:00",
  "leaderId": "USER-001",
  "knowledgeBaseOwnerId": "USER-002",
  "applicant": "付源",
  "department": "数据智能中心",
  "team": "知识运营团队",
  "knowledgeBase": "客户准入知识库",
  "fileType": "DOCX / PDF",
  "supplementType": "操作指引",
  "attachmentIds": "ATT-20260915-001,ATT-20260915-002",
  "reason": "补充最新客户准入操作说明",
  "solution": "核对制度文件后更新知识条目",
  "priority": "普通"
}
```

成功响应：`201 Created`，返回完整工单对象，初始状态为 `草稿`，节点为 `填写申请`。

## 6. 更新工单

### `PUT /api/v1/tickets/{id}`

| 参数 | 位置 | 类型 | 是否必传 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | string | 是 | 1～32 字符 | 要更新的工单编号 |

请求体使用“通用工单请求字段”。这是完整更新接口，因此 `title`、`applicant`、`reason`、`leaderId`、`knowledgeBaseOwnerId` 必传。

附件规则：

- 保留现有附件：必须传回现有的完整 `attachmentIds` 字符串。
- 增加或替换附件：传修改后的完整字符串。
- 删除部分附件：从字符串中的 ID 列表移除相应 ID 后传完整字符串。
- 删除全部附件：传 `"attachmentIds": ""`。
- 省略 `attachmentIds`：由于 PUT 是完整更新，当前实现会保存为 `null`，不用于“保持不变”。

成功响应：`200 OK`，返回更新后的完整工单对象。不存在时返回 `404`。

## 7. 提交申请

### `POST /api/v1/tickets/{id}/submit`

| 参数 | 位置 | 类型 | 是否必传 | 长度/数量限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | string | 是 | 1～32 字符 | 要提交的工单编号 |

请求体：无。不传 leaderId、knowledgeBaseOwnerId 或 attachmentIds；直接使用创建/更新工单时已保存的数据，不覆盖附件。历史草稿缺少审批人时返回 400，先通过 PUT 补齐后提交。

```http
POST /api/v1/tickets/KB-20260915103000-1001/submit
```

成功响应：`200 OK`，状态变为 `审批中`，当前节点变为 `领导审批`，`owner` 设置为 `leaderId`。领导通过后，`owner` 设置为 `knowledgeBaseOwnerId`。两个 ID 均保存到数据库，并在列表和详情中返回；创建时指定；PUT 在填写申请节点可修改，其他节点保留原审批人。此处指派不代表已实现登录身份及审批权限校验。

部署已有数据库前，先执行 `src/main/resources/db/migration/V4__add_ticket_approvers.sql`（仅一次）；全新库使用更新后的建表脚本，不重复执行 V4。旧工单不会自动推断审批人；缺少审批人的草稿需先 PUT 补齐再提交。

## 8. 审核通过

### `POST /api/v1/tickets/{id}/approve`

| 参数 | 位置 | 类型 | 是否必传 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | string | 是 | 1～32 字符 | 工单编号 |
| `reviewer` | body | string | 是 | 1～64 字符 | 审核人姓名或唯一标识，不能只包含空白字符 |
| `comment` | body | string | 否 | 最多 1000 字符 | 审核意见 |

```json
{"reviewer":"付源","comment":"内容核验通过"}
```

允许的节点流转：`领导审批 → 目标知识库管理员审批 → 知识上传`，以及 `验收审批 → 完成`。其他节点调用返回 `400`。

## 9. 退回修改

### `POST /api/v1/tickets/{id}/reject`

请求字段与“审核通过”一致：`reviewer` 必传，`comment` 选传。成功后状态变为 `已退回`，当前节点回到 `填写申请`，并写入流程记录。

成功响应：`200 OK`。工单不存在返回 `404`。

## 10. 完成任务并提交验收

### `POST /api/v1/tickets/{id}/complete-task`

请求字段与“审核通过”一致：

| 字段 | 位置 | 类型 | 是否必传 | 长度限制 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `id` | path | string | 是 | 1～32 字符 | 工单编号 |
| `reviewer` | body | string | 是 | 1～64 字符 | 实际完成人或提交验收人 |
| `comment` | body | string | 否 | 最多 1000 字符 | 完成说明、处置结果或验收说明 |

仅当当前节点为 `知识上传` 时允许调用。成功后状态变为 `待验收`，当前节点变为 `验收审批`。否则返回 `400`。

## 11. 查询流程节点

### `GET /api/v1/tickets/workflow`

请求参数：无。请求体：无。

成功响应 `200`：

```json
{
  "nodes": [
    "填写申请",
    "领导审批",
    "目标知识库管理员审批",
    "知识上传",
    "验收审批",
    "完成"
  ]
}
```

数组顺序即标准工单流程顺序。

## 1.0.8 期望完成时间

创建、更新工单可传 expectedCompletionTime。列表 items、详情以及提交/审核/退回/完成任务/撤销返回的工单对象均包含该字段；操作接口仍使用已保存值，不新增动作请求参数。日期格式错误返回 400。未增加必须为未来时间的限制，避免历史数据更新受阻。

已有库先执行 db/migration/V5__add_expected_completion_time.sql 一次，再部署新 JAR；新库使用 backend/sql/init-oceanbase.sql。数据库变更需由部署人员执行。
