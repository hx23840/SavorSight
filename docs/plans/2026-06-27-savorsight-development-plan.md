# 见味开发计划

**目标：** 打通"眼镜裸机端采集原始音视频流 → Rust 服务端处理并调用 QwenVL → 裸机 App 展示菜谱与做菜检查"的 MVP 闭环。

**架构：** 裸机 App 基于官方 demo 改造（项目名 `SavorSightApp`），负责采集原始音视频流、菜谱展示、做菜导航和拍照检查。Rust 服务端负责接收原始流、保存学习会话、调用 QwenVL 或 mock 推理服务生成菜谱。

**技术栈：** Android Kotlin、Rokid Glasses 裸机 demo、Rust、Axum、Tokio、Serde、WebSocket、QwenVL 多模态服务。

---

## 1. 代码目录规划

当前已有：

```text
SavorSightApp/
└── SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/
    ├── camera/
    ├── input/
    ├── navigation/
    ├── sensor/
    ├── savorsight/          # 见味业务模块(SavorSight)
    │   ├── LearningScreen.kt         # 学习采集页面
    │   ├── RecipeDraftScreen.kt       # 菜谱草稿确认页面
    │   ├── CookingGuideScreen.kt      # 做菜步骤导航页面
    │   ├── CheckCameraScreen.kt       # 拍照检查页面
    │   ├── CheckResultScreen.kt       # 检查结果页面
    │   ├── SavorSightApiClient.kt       # 服务端 API 客户端
    │   ├── SavorSightCaptureViewModel.kt # 采集业务逻辑
    │   ├── SavorSightCaptureState.kt    # 采集状态定义
    │   ├── SavorSightRawStreamUploader.kt # 原始流上传器
    │   ├── RecipeViewModel.kt        # 菜谱业务逻辑
    │   ├── CheckViewModel.kt         # 检查业务逻辑
    │   └── SpeechHelper.kt           # 语音播报辅助
    └── utils/
```

建议新增 `server/` 目录：

```text
server/
├── Cargo.toml
├── src/
│   ├── main.rs
│   ├── app_state.rs
│   ├── routes/
│   │   ├── mod.rs
│   │   ├── learning_sessions.rs
│   │   ├── recipes.rs
│   │   └── checks.rs
│   ├── services/
│   │   ├── mod.rs
│   │   ├── storage.rs
│   │   ├── qwen_vl.rs
│   │   └── recipe_builder.rs
│   └── models/
│       ├── mod.rs
│       ├── session.rs
│       ├── recipe.rs
│       └── check.rs
└── tests/
    ├── learning_sessions_api.rs
    └── recipe_api.rs

docs/
├── api/
│   └── savorsight-api.md
└── schemas/
    ├── learning-session.schema.json
    ├── recipe.schema.json
    └── check-result.schema.json
```

## 2. 开发顺序

优先打通"真实采集数据 → 后端生成菜谱"的主线。没有采集数据就没有菜谱，所以菜谱展示不能先于采集主线成为第一阶段核心。

```text
Rust 服务端原始流接入骨架
        ↓
裸机 App 创建学习会话
        ↓
裸机 App 上传原始音视频流
        ↓
服务端保存并处理采集数据
        ↓
QwenVL / mock-from-captured-data 生成菜谱草稿
        ↓
裸机 App 展示菜谱草稿与做菜检查
```

第一阶段可以用 mock QwenVL，但 mock 的输入必须来自一次真实采集会话，而不是凭空返回固定菜谱。这样开发顺序和产品逻辑一致：先有用户观看数据，再有菜谱。

## 3. 里程碑

### 里程碑一：服务端原始流接入骨架

目标：Rust 服务端可启动，能创建学习会话，并提供原始流上传入口。这个阶段不要求生成菜谱，但必须能接住设备端数据。

产物：

- `server/` Rust 项目。
- `POST /api/learning-sessions`
- `WebSocket /api/learning-sessions/{session_id}/raw-stream`
- `POST /api/learning-sessions/{session_id}/finish`
- 本地会话目录或对象存储抽象。
- 原始流写入能力。
- 设备状态写入能力。

验证方式：

```bash
cd server
cargo test
cargo run
curl -X POST http://127.0.0.1:8080/api/learning-sessions
```

### 里程碑二：裸机 App 学习采集

目标：基于官方 demo 增加“学习模式”，能创建学习会话，并把摄像头、麦克风获取到的原始数据上传后端。

产物：

- `SavorSightApp` 内新增 `savorsight/` 相关模块。
- 学习模式页面或入口。
- 后端地址配置。
- 学习会话创建。
- 原始视频流上传。
- 原始音频流上传。
- 状态上报。

验证方式：

```bash
cd SavorSightApp
./gradlew :SavorSightApp:assembleDebug
```

眼镜端运行后：

- 点击或按键开始学习。
- 服务端收到学习会话。
- 服务端收到原始视频数据。
- 服务端收到原始音频数据。
- 停止学习后，服务端进入 `processing` 状态。

### 里程碑三：基于采集数据生成菜谱草稿

目标：服务端基于一次真实学习会话的数据生成菜谱草稿。第一版可以使用 mock QwenVL，但 mock 逻辑必须绑定 `session_id` 和已采集数据状态。

产物：

- `GET /api/learning-sessions/{session_id}/recipe-draft`
- 会话状态：`created`、`capturing`、`uploaded`、`processing`、`draft_ready`、`failed`
- mock-from-captured-data 菜谱生成器。
- 后续可替换为真实 QwenVL 的接口边界。

验证方式：

- 没有采集数据的 session 不能返回菜谱草稿。
- 有采集数据并 finish 的 session 可以返回菜谱草稿。
- 服务端日志能看到菜谱来自哪个 `session_id`。

### 里程碑四：QwenVL 服务接入

目标：服务端用 QwenVL 替换 mock-from-captured-data 菜谱生成，并接入真实检查判断。

产物：

- `QwenVlClient` trait。
- mock 实现。
- HTTP 实现。
- 视频理解 prompt。
- 拍照检查 prompt。
- JSON Schema 校验。

验证方式：

- 给服务端一段测试音视频，能返回结构化菜谱。
- 给服务端一张检查图片和步骤目标，能返回 `pass` / `continue` / `adjust` / `uncertain`。
- 模型输出不合法时，服务端能返回可诊断错误，不让裸机 App 崩溃。

### 里程碑五：端到端演示

目标：完成比赛演示链路。

演示流程：

```text
1. 眼镜端开始学习
2. 用户正常观看做菜视频
3. 裸机端上传原始音视频流
4. 服务端生成菜谱草稿
5. 裸机 App 展示菜谱草稿
6. 用户确认并进入做菜导航
7. 用户说"检查一下"
8. 裸机 App 拍照上传
9. 服务端返回状态判断
10. 裸机 App 展示并播报建议
```

### 里程碑六：裸机 App 菜谱与检查体验完善

目标：在端到端链路跑通后，完善裸机 App 的菜谱确认、步骤导航、拍照检查和异常状态显示。

产物：

- `savorsight/RecipeDraftScreen.kt` - 菜谱草稿确认页面
- `savorsight/CookingGuideScreen.kt` - 做菜步骤导航页面
- `savorsight/CheckCameraScreen.kt` - 拍照检查页面
- `savorsight/CheckResultScreen.kt` - 检查结果页面
- `savorsight/RecipeViewModel.kt` - 菜谱业务逻辑
- `savorsight/CheckViewModel.kt` - 检查业务逻辑
- 裸机端 API client (`SavorSightApiClient.kt`)

验证方式：

- 裸机 App 打开菜谱草稿页能看到真实 session 生成的菜名、食材、步骤。
- 做菜页能切换步骤。
- 检查页能调用相机拍照并上传。
- 服务端返回判断后，裸机 App 能显示结果。

## 4. 服务端开发计划

### Task 1：创建 Rust 服务端项目

**文件：**

- 创建：`server/Cargo.toml`
- 创建：`server/src/main.rs`
- 创建：`server/src/app_state.rs`
- 创建：`server/src/routes/mod.rs`

**内容：**

使用 `axum` 提供 HTTP API，使用 `tokio` 运行异步服务。

建议依赖：

```toml
[dependencies]
axum = { version = "0.7", features = ["ws", "multipart"] }
tokio = { version = "1", features = ["full"] }
serde = { version = "1", features = ["derive"] }
serde_json = "1"
uuid = { version = "1", features = ["v4", "serde"] }
tracing = "0.1"
tracing-subscriber = "0.3"
thiserror = "1"
tower-http = { version = "0.5", features = ["cors", "trace"] }
reqwest = { version = "0.12", features = ["json", "multipart", "stream"] }
```

**验证：**

```bash
cd server
cargo run
```

预期：

```text
listening on 127.0.0.1:8080
```

### Task 2：定义核心数据模型

**文件：**

- 创建：`server/src/models/mod.rs`
- 创建：`server/src/models/session.rs`
- 创建：`server/src/models/recipe.rs`
- 创建：`server/src/models/check.rs`
- 创建：`docs/schemas/recipe.schema.json`

**模型：**

- `LearningSession`
- `LearningSessionStatus`
- `Recipe`
- `Ingredient`
- `Seasoning`
- `RecipeStep`
- `UncertainField`
- `CheckRequest`
- `CheckResult`

**验证：**

```bash
cd server
cargo test
```

预期：模型序列化、反序列化测试通过。

### Task 3：实现学习会话 API

**文件：**

- 创建：`server/src/routes/learning_sessions.rs`
- 修改：`server/src/routes/mod.rs`
- 修改：`server/src/main.rs`

**接口：**

```http
POST /api/learning-sessions
POST /api/learning-sessions/{session_id}/finish
GET /api/learning-sessions/{session_id}/recipe-draft
```

**验证：**

```bash
curl -X POST http://127.0.0.1:8080/api/learning-sessions
```

预期返回：

```json
{
  "sessionId": "...",
  "uploadEndpoint": "ws://127.0.0.1:8080/api/learning-sessions/.../raw-stream",
  "status": "created"
}
```

### Task 4：实现原始流接入接口

**文件：**

- 修改：`server/src/routes/learning_sessions.rs`
- 创建：`server/src/services/storage.rs`

**接口：**

```http
WebSocket /api/learning-sessions/{session_id}/raw-stream
```

**第一版策略：**

- 接收 binary message。
- 根据消息前缀或 envelope 区分 `raw_video`、`raw_audio`、`device_status`。
- 保存到本地 `server/data/sessions/{session_id}/`。
- 不在接入层理解数据。

**验证：**

使用本地 WebSocket client 发送测试二进制数据，确认服务端写入文件。

### Task 5：实现基于采集会话的菜谱草稿生成

**文件：**

- 创建：`server/src/services/recipe_builder.rs`
- 修改：`server/src/routes/learning_sessions.rs`

**逻辑：**

`finish` 后检查当前 `session_id` 是否已经收到原始视频或音频数据。第一版可以暂时不调用 QwenVL，但不能无条件返回固定菜谱；mock 菜谱必须由一个已完成采集的 session 触发。

规则：

- 没有 raw stream 的 session 返回 `409 no_capture_data`。
- 未 finish 的 session 返回 `409 session_not_finished`。
- 已 finish 且有 raw stream 的 session 返回菜谱草稿。
- 菜谱草稿带上 `sourceSessionId`，方便联调追踪。

第一版返回示例：

```json
{
  "sourceSessionId": "learn-2026-001",
  "dishName": "番茄炒蛋",
  "steps": [
    {
      "id": "step-01",
      "title": "炒鸡蛋",
      "instruction": "鸡蛋液下锅，炒到半凝固后盛出。",
      "targetState": "鸡蛋半凝固，表面仍略湿润",
      "checkable": true
    }
  ]
}
```

**验证：**

```bash
curl http://127.0.0.1:8080/api/learning-sessions/{session_id}/recipe-draft
```

预期：

- 对没有采集数据的 session，请求失败。
- 对完成采集的 session，能拿到带 `sourceSessionId` 的菜谱草稿。

### Task 6：实现做菜检查 API

**文件：**

- 创建：`server/src/routes/checks.rs`
- 修改：`server/src/routes/mod.rs`

**接口：**

```http
POST /api/recipes/{recipe_id}/steps/{step_id}/check
```

**第一版策略：**

- 支持 multipart 图片上传。
- 接收 `targetState`。
- 返回 mock 判断。

**验证：**

```bash
curl -F "image=@test.jpg" \
  -F "targetState=鸡蛋半凝固，表面仍略湿润" \
  http://127.0.0.1:8080/api/recipes/recipe-001/steps/step-01/check
```

预期返回：

```json
{
  "status": "continue",
  "summary": "还差一点",
  "suggestion": "继续中火翻炒 20 秒",
  "tts": "还差一点，继续中火翻炒二十秒。"
}
```

### Task 7：接入 QwenVL

**文件：**

- 创建：`server/src/services/qwen_vl.rs`
- 修改：`server/src/services/recipe_builder.rs`
- 修改：`server/src/routes/checks.rs`

**设计：**

定义 trait：

```rust
pub trait QwenVlClient {
    async fn generate_recipe_from_session(&self, session_id: &str) -> Result<Recipe>;
    async fn check_step_image(&self, request: CheckRequest) -> Result<CheckResult>;
}
```

实现：

- `MockQwenVlClient`
- `HttpQwenVlClient`

**配置：**

```text
QWENVL_ENDPOINT=
QWENVL_API_KEY=
QWENVL_MODEL=
```

**验证：**

- mock 测试稳定通过。
- HTTP 实现可以在有模型服务时单独验证。
- 模型返回 JSON 不合法时，服务端返回 `502 model_output_invalid`。

## 5. 裸机 App 开发计划

### Task 1：确认 demo 可构建

**目录：**

- `SavorSightApp`

**命令：**

```bash
cd SavorSightApp
./gradlew :SavorSightApp:assembleDebug
```

预期：

```text
BUILD SUCCESSFUL
```

### Task 2：新增见味模块目录

**文件：**

- 创建：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/savorsight/SavorSightCaptureController.kt`
- 创建：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/savorsight/SavorSightApiClient.kt`
- 创建：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/savorsight/SavorSightCaptureState.kt`
- 创建：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/savorsight/SavorSightRawStreamUploader.kt`

**边界：**

这些类只做采集控制和上传，不做音视频处理。

### Task 3：接入学习会话创建

**文件：**

- 修改：`SavorSightApiClient.kt`

**功能：**

- 调用 `POST /api/learning-sessions`。
- 保存 `sessionId`。
- 保存 `uploadEndpoint`。

**验证：**

- App 启动学习模式后，Rust 服务端日志出现新会话。

### Task 4：接入摄像头原始流

**文件：**

- 参考：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/camera/CameraBind.kt`
- 修改：`SavorSightCaptureController.kt`

**功能：**

- 复用 demo 中已有相机绑定方式。
- 获取摄像头原始数据。
- 直接交给 `SavorSightRawStreamUploader`。

**禁止：**

- 不在设备端抽帧。
- 不在设备端裁剪。
- 不在设备端转码。
- 不在设备端识别画面。

### Task 5：接入麦克风原始流

**文件：**

- 修改：`SavorSightCaptureController.kt`
- 修改：`SavorSightRawStreamUploader.kt`

**功能：**

- 采集麦克风原始音频。
- 直接交给上传器。

**禁止：**

- 不在设备端降噪。
- 不在设备端转写。
- 不在设备端做音视频对齐。

### Task 6：接入按键控制

**文件：**

- 参考：`SavorSightApp/SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/input/BareGlassesInputDispatcher.kt`
- 修改或新增：`SavorSightCaptureController.kt`

**功能：**

- 按键开始学习。
- 按键停止学习。
- 异常时允许重新开始。

**验证：**

- 眼镜上按键后服务端出现会话。
- 再次按键后服务端会话进入 `processing`。

### Task 7：采集状态 UI 或日志

**文件：**

- 视 demo 当前 UI 结构决定。
- 如果 demo 使用 Compose，则新增学习状态页。

**状态：**

- `idle`
- `creating_session`
- `capturing`
- `uploading`
- `stopping`
- `error`

**验证：**

- 每个状态在设备上可见或在日志中可追踪。

## 6. 裸机 App 菜谱与检查功能开发

本模块负责菜谱展示、做菜导航和拍照检查功能，全部在裸机 App 中实现。

### Task 1：菜谱草稿确认页面

**文件：**

- 创建或修改：`savorsight/RecipeDraftScreen.kt`
- 创建或修改：`savorsight/RecipeViewModel.kt`

**功能：**

- 调用 `GET /api/learning-sessions/{session_id}/recipe-draft` 获取菜谱。
- 展示菜名、食材、步骤、不确定字段。
- "确认并开始做菜"跳转。

**验证：**

- Rust 服务端 mock 菜谱能展示出来。

### Task 2：做菜步骤导航页面

**文件：**

- 创建或修改：`savorsight/CookingGuideScreen.kt`

**功能：**

- 当前步骤标题、说明、火候、目标状态。
- 下一步、上一步、重复当前步骤。
- "检查一下"跳转。

### Task 3：拍照检查页面

**文件：**

- 创建或修改：`savorsight/CheckCameraScreen.kt`
- 创建或修改：`savorsight/CheckViewModel.kt`

**功能：**

- 调用眼镜相机拍照。
- 上传图片到 `POST /api/recipes/{recipe_id}/steps/{step_id}/check`。
- 接收检查结果并跳转。

**验证：**

- 服务端收到图片。
- 裸机 App 收到检查结果。

### Task 4：检查结果页面

**文件：**

- 创建或修改：`savorsight/CheckResultScreen.kt`

**功能：**

- 展示状态：可以进入下一步 / 继续当前步骤 / 需要调整 / 无法判断。
- 简短解释、建议。
- 返回当前步骤。

### Task 5：语音播报集成

**文件：**

- 创建或修改：`savorsight/SpeechHelper.kt`

**功能：**

- 检查结果语音播报。
- 步骤切换语音提示。

## 7. 联调计划

### 联调一：服务端和裸机 App

目标：

- 裸机 App 能创建学习会话。
- 裸机 App 能上传原始视频和音频。
- 服务端能保存原始数据。

步骤：

```text
1. 启动 Rust 服务端
2. 在眼镜上安装 demo 改造版
3. 开始学习
4. 服务端检查 session 创建
5. 服务端检查 raw stream 写入
6. 停止学习
7. 服务端标记 session processing
```

### 联调二：完整闭环

目标：

- 从眼镜学习采集到裸机 App 做菜检查打通。

步骤：

```text
1. 眼镜端开始学习
2. 用户观看做菜视频
3. 裸机上传原始流
4. 服务端生成 mock 或真实菜谱
5. 裸机 App 展示菜谱
6. 用户进入做菜导航
7. 裸机 App 拍照检查
8. 服务端返回判断
9. 裸机 App 展示结果
```

## 8. 测试策略

### Rust 服务端

单元测试：

- 数据模型序列化。
- 菜谱 JSON Schema 校验。
- QwenVL mock client。
- 检查结果解析。

集成测试：

- 创建学习会话。
- 结束学习会话。
- 获取菜谱草稿。
- 上传检查图片。

命令：

```bash
cd server
cargo fmt
cargo clippy
cargo test
```

### 裸机 App

测试重点：

- 官方 demo 改造后仍可构建。
- 权限申请正常。
- 采集开始和停止稳定。
- 网络断开后状态可恢复。

命令：

```bash
cd SavorSightApp
./gradlew :SavorSightApp:assembleDebug
./gradlew :SavorSightApp:testDebugUnitTest
```

## 9. 开发优先级

### P0

- Rust 服务端 API 骨架。
- Rust 服务端原始流接入。
- 裸机 App 创建学习会话。
- 裸机 App 原始流上传。
- 基于真实采集 session 返回菜谱草稿。
- 裸机 App 菜谱展示与拍照检查。

### P1

- QwenVL 真实接入。
- 菜谱 JSON Schema 校验。
- 服务端会话状态管理。
- 裸机端异常重连。
- 裸机 App 步骤切换和语音提示。
- mock 检查结果替换为 QwenVL 检查结果。

### P2

- 更细的模型 prompt。
- 检查结果置信度。
- 多菜谱管理。
- 用户历史菜谱。
- 更完整的权限与隐私提示。

## 10. 风险与处理

### 原始流上传带宽压力

先用短时学习演示控制风险。不要把对齐、编码、抽帧等处理放回设备端。若实测无法稳定传输，再讨论是否引入系统层可用的最小封装能力，但不能改变"理解和处理在后端"的架构边界。

### QwenVL 输出不稳定

服务端必须做 JSON 校验和 fallback。模型输出无法解析时返回明确错误，并允许裸机 App 显示"菜谱整理失败，请重试"。

### 裸机端相机能力限制

如果裸机端拍照在设备上能力不足，再考虑其他方案。MVP 设计先以裸机端相机能力为准。

### 裸机 demo 结构变化

所有改造尽量放在 `savorsight/` 新目录，少改 demo 原有模块。需要复用 camera/input 能力时，通过封装调用，避免大范围重写官方 demo。

## 11. 建议提交顺序

```text
feat(server): scaffold rust api service
feat(server): add learning session and raw stream ingest
feat(server): add recipe and check models
feat(glasses): add savorsight capture module
feat(glasses): create learning session from device
feat(glasses): upload raw streams to server
feat(server): generate recipe draft from captured session
feat(glasses): add recipe draft and cooking guide screens
feat(glasses): add check camera and result screens
feat(server): integrate qwenvl client behind trait
```

## 12. 第一轮实现建议

第一轮可以不接真实 QwenVL，但必须先做真实采集链路。服务端可以用 mock-from-captured-data 生成菜谱草稿，但这个 mock 必须依赖裸机端真实上传过的学习会话。

```text
Rust 原始流接入
        ↓
裸机学习会话和原始流上传
        ↓
Rust 基于 captured session 返回菜谱草稿
        ↓
裸机 App 菜谱和拍照检查
        ↓
QwenVL 替换 mock
```

这个顺序保证主线不脱离数据来源：先采集，再生成，再展示。
