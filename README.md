# AIHospital

## Conversation History Feature

Doctor–AI chats now reuse existing `spring_ai_chat_memory` data:

```
CREATE TABLE spring_ai_chat_memory (
    conversation_id varchar(36) NOT NULL,
    content text NOT NULL,
    type varchar(10) NOT NULL,
    timestamp timestamp NOT NULL
);
```

- `conversation_id` 格式：`register-<registerId>` 或 `patient-<patientId>`，若都缺失则 `general`。
- `type`：`user`、`assistant`、`assistant_error` 三种。

### REST Endpoints

- `POST /prescription/diagnosis` — 接收 `msg` 以及可选 `registerId/patientId`，并把医生输入与 AI 输出写入 `spring_ai_chat_memory`。
- `GET /prescription/conversations` — 通过 `registerId` 或 `patientId` 查询历史对话；支持 `page/size` 分页，响应带 `records/total/page/size`。

### 手动验证步骤

1. 调用 `POST /prescription/diagnosis`，设置 `registerId=1001`（或任意数值），`patientId=5001`，并填写 `msg`。
2. 查询数据库：`SELECT * FROM spring_ai_chat_memory WHERE conversation_id='register-1001';` 应看到 user 与 assistant 两条记录。
3. 调用 `GET /prescription/conversations?registerId=1001&page=1&size=10`，响应中的 `records` 与数据库一致。