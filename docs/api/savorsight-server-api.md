# 见味服务端 API

## 基础信息

本服务端使用 Rust + Axum 实现。当前阶段只服务“眼镜原始采集数据进入后端，再由后端生成菜谱草稿”的主线。

默认本地地址：

```text
http://127.0.0.1:8080
```

## 健康检查

```http
GET /health
```

返回：

```text
ok
```

## 创建学习会话

```http
POST /api/learning-sessions
Content-Type: application/json
```

请求：

```json
{
  "deviceId": "rokid-glasses-001",
  "sourceMode": "glasses_first_person_stream",
  "capturePolicy": "raw_stream_upload_only"
}
```

返回：

```json
{
  "sessionId": "uuid",
  "uploadEndpoint": "/api/learning-sessions/{sessionId}/raw-stream",
  "status": "created"
}
```

## 查询学习会话

```http
GET /api/learning-sessions/{sessionId}
```

返回：

```json
{
  "sessionId": "uuid",
  "deviceId": "rokid-glasses-001",
  "sourceMode": "glasses_first_person_stream",
  "capturePolicy": "raw_stream_upload_only",
  "status": "capturing",
  "rawVideoChunks": 1,
  "rawAudioChunks": 10,
  "statusEvents": 2,
  "createdAt": "2026-06-27T00:00:00Z",
  "updatedAt": "2026-06-27T00:01:00Z"
}
```

## 上传原始音视频流

```http
WebSocket /api/learning-sessions/{sessionId}/raw-stream
```

当前协议使用轻量 envelope。服务端只负责接收和保存，不理解内容。

二进制消息格式：

```text
raw_video\n<binary payload>
raw_audio\n<binary payload>
```

文本状态消息格式：

```text
任意文本
```

服务端会把文本消息转为 `device_status` 写入会话目录。

保存目录：

```text
server/data/sessions/{sessionId}/
├── raw_video.bin
├── raw_audio.bin
├── raw_unknown.bin
└── device_status.jsonl
```

## 结束学习会话

```http
POST /api/learning-sessions/{sessionId}/finish
```

如果该会话没有任何原始采集数据，返回：

```json
{
  "code": "no_capture_data",
  "message": "session has no captured raw stream data"
}
```

成功返回：

```json
{
  "sessionId": "uuid",
  "status": "processing",
  "rawVideoChunks": 1,
  "rawAudioChunks": 10
}
```

## 获取菜谱草稿

```http
GET /api/learning-sessions/{sessionId}/recipe-draft
```

要求：

- 会话必须已有采集数据。
- 会话必须已经调用 `finish`。

返回：

```json
{
  "recipeId": "recipe-{sessionId}",
  "sourceSessionId": "uuid",
  "dishName": "番茄炒蛋",
  "confidence": 0.72,
  "servings": 2,
  "estimatedTimeMinutes": 15,
  "ingredients": [],
  "seasonings": [],
  "steps": [],
  "uncertainFields": []
}
```

当前实现通过 `MockQwenVlClient` 返回草稿。后续替换真实 QwenVL 时保持接口不变。

## 快速调试 mock 菜谱

```http
GET /api/mock/recipe-draft
```

该接口用于快速调试和联调，不要求先创建学习会话，也不要求上传原始采集数据。它会直接返回固定菜谱，并把菜谱写入服务端内存，因此可以继续用返回的 `recipeId` 调用拍照检查接口。

返回：

```json
{
  "recipeId": "mock-recipe",
  "sourceSessionId": "00000000-0000-0000-0000-000000000000",
  "dishName": "番茄炒蛋",
  "confidence": 0.72,
  "servings": 2,
  "estimatedTimeMinutes": 15,
  "ingredients": [
    {
      "name": "番茄",
      "amount": "2 个",
      "prep": "切块"
    }
  ],
  "steps": [
    {
      "id": "step-01",
      "title": "炒鸡蛋",
      "instruction": "鸡蛋液下锅，炒到半凝固后盛出。",
      "heat": "中火",
      "targetState": "鸡蛋半凝固，表面仍略湿润。",
      "checkable": true,
      "confidence": 0.78
    }
  ]
}
```

正式链路仍然使用：

```http
GET /api/learning-sessions/{sessionId}/recipe-draft
```

## 做菜拍照检查

```http
POST /api/recipes/{recipeId}/steps/{stepId}/check
Content-Type: multipart/form-data
```

字段：

```text
image: 图片文件
targetState: 当前步骤目标状态
```

返回：

```json
{
  "status": "continue",
  "confidence": 0.62,
  "summary": "已收到检查图片。",
  "suggestion": "当前为 mock 判断：继续当前步骤 20 秒后再次检查。",
  "tts": "继续当前步骤二十秒后再次检查。"
}
```

## 错误格式

```json
{
  "code": "session_not_found",
  "message": "session not found"
}
```

常见错误码：

- `session_not_found`
- `no_capture_data`
- `session_not_finished`
- `invalid_request`
- `io_error`
