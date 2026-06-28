# Rokid Glasses 裸机开发规范

版本：1.0.0

---

## 概述

Rokid Glasses 为单绿色显示：Compose/Android 中黑色视为透明（不发光），界面元素应使用线条描边，避免大面积实心填充。

---

## 分辨率与安全区

| 项 | 值 |
|---|---|
| 物理分辨率 | 480 × 640 px |
| 上边距（不推荐绘制） | 80 px |
| 下边距（不推荐绘制） | 80 px |
| 推荐内容区 | 宽 480 × 高 480（y：80～560） |

```
┌──────────────── 480 ────────────────┐  y=0
│░░░░░░░░░░ 上禁区 80px ░░░░░░░░░░░░░│
├─────────────────────────────────────┤  y=80
│                                     │
│         推荐 UI 内容区 480×480       │
│                                     │
├─────────────────────────────────────┤  y=560
│░░░░░░░░░░ 下禁区 80px ░░░░░░░░░░░░░│
└─────────────────────────────────────┘  y=640
```

---

## Compose 实现约定

- **背景**：`Color(0xFF000000)`（透明/不点亮像素）
- **前景**：单色绿，如 `Color(0xFF00FF00)`
- **图形**：优先 `Canvas` + `drawRect` / `drawCircle` / `drawLine` 且 `style = Stroke(...)`
- **文本**：小字号、高对比；避免 Material 实心 Card 色块
- **全屏**：可隐藏系统栏并保持常亮（演示场景）

---

## 交互（眼镜端）

眼镜屏无触控焦点，不使用列表滚动、滑动手势或可点击按钮作为唯一入口。

| 输入 | 语义 |
|---|---|
| 单击 / Enter | 触控板单指 `KEYCODE_ENTER` 或镜腿 `ACTION_SPRITE_BUTTON_CLICK` → 主操作 |
| 双击（单指） | 触控板单指双击 · `KEYCODE_BACK` → Hub 进入 / 子页返回 |
| 长按 | TouchPad 单指长按 · `ACTION_AI_START` → 次要操作 |
| 前滑 / 后滑（双指） | 双指滑动广播 → 下一屏 / 上一屏（产品应用可选用） |
| 物理返回键 | `KEYCODE_BACK`，与单指双击等价 |

约定：
- 每屏仅展示一屏内容（480×480 安全区内），多信息用分页（单击翻页），不用上下滑动
- 底部固定按键提示条（BareKeyHintBar），文案与当前页行为一致
- 由 `BareGlassesInputDispatcher` 统一分发

---

## Sample 工程与页面说明

### 工程信息

| 项 | 值 |
|---|---|
| 工程名 | SavorSightApp |
| 包名 | com.savorsight |
| minSdk | 31（Android 12） |
| targetSdk | 36 |
| UI | Jetpack Compose，黑底绿线框 |
| 架构 | 单 Activity + NavHost + 各能力 ViewModel |

### 模块结构

```
SavorSightApp/src/main/java/com/rokid/glassesbaredevsample/
├── app/CONSTANT.kt
├── camera/CameraBind.kt              # 异步 CameraX 绑定（rememberCameraBound）
├── input/BareGlassesInputDispatcher.kt  # KeyEvent + 广播
├── navigation/BareSceneRoutes.kt
├── ui/design/BareDesign.kt
├── ui/design/BareSavedPathBlock.kt  # 落盘路径展示
├── ui/theme/
├── ui/imu/
├── utils/BareMediaStorage.kt         # 拍照/录像落盘目录选择
├── activities/main/MainActivity.kt     # NavHost + Hub
├── activities/keys/                  # 按键与佩戴/折叠
├── activities/audio/
├── activities/photo/
├── activities/video/
├── activities/imu/
└── sensor/                           # IMU 演示与轴向映射
```

### Sample 输入模型（简化）

为便于理解，Sample 导航使用 TouchPad 单指单击、双击与长按；镜腿长按与双指相关系统广播在 `BareGlassesInputDispatcher` 中注册并 `abortBroadcast()`，不参与 UI。

| 语义 | 来源 |
|---|---|
| 单击 / Enter | TouchPad `KEYCODE_ENTER`、镜腿 `ACTION_SPRITE_BUTTON_CLICK` |
| 双击 / 返回 | TouchPad `KEYCODE_BACK` |
| 长按 | TouchPad `ACTION_AI_START` |

### Hub 与路由

| 路由 | 页面 | 单击/Enter | 双击 | 长按 |
|---|---|---|---|---|
| hub | 能力 1/5 | 下一项（循环） | 进入 | — |
| keys_wear | 佩戴/按键日志 | 下一屏（循环） | 返回 | — |
| audio | 录音 | 开始/停止 | 返回 | — |
| photo | 拍照 | 单击拍摄 | 返回 | — |
| video | 录像 | 单击开始/停止 | 返回 | — |
| imu | 验证/演示 5 屏 | 见 IMU 与传感器专章 | 返回 | 见 IMU 与传感器专章 |

### 拍照 / 录像页说明

- **无取景预览**；相机就绪后 TouchPad 单击触发（非线框按钮）
- 绑定完成后副标题为「相机就绪 · 单击拍照」或「待机 · 单击开始」
- 成功后落盘路径区块显示完整 `.jpg` / `.mp4` 路径；失败时状态区展示错误信息
- 默认落盘：`/sdcard/Pictures/bare_photo/`、`/sdcard/Video/bare_video/`（不可写时回退应用目录）
- 进入子页后约 400ms 内忽略重复双击（`rememberSubPageEnterDebounce`），避免 Hub 双击进入时立刻返回

---

## 原始音频

### 概述

眼镜支持 8 通道 PCM 采集。使用标准 `android.media.AudioRecord`，通过 `AudioFormat.Builder.setChannelMask(...)` 指定设备通道掩码。

### 参数

| 参数 | 推荐值 |
|---|---|
| ChannelMask | 0x6000FC |
| 采样率 | 16000 Hz |
| 编码 | ENCODING_PCM_16BIT |
| 音源 | MediaRecorder.AudioSource.MIC |

### 通道含义

| 通道 | 内容 |
|---|---|
| 0 / 1 | 算法处理后音频 |
| 2–5 | 四路麦克风原始数据 |
| 6 / 7 | 硬件回声 |

### 前置条件

`RECORD_AUDIO` 运行时权限。

### 示例要点

```kotlin
val channelMask = 0x6000FC
val recorder = AudioRecord.Builder()
    .setAudioSource(MediaRecorder.AudioSource.MIC)
    .setAudioFormat(
        AudioFormat.Builder()
            .setSampleRate(16_000)
            .setChannelMask(channelMask)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
    )
    .build()
recorder.startRecording()
// 后台线程 read(buffer) 写入 PCM 文件
```

### 实践建议

- 页面退出时 `stop()` + `release()`，避免泄漏
- 大文件写入注意存储路径与权限（Android 10+ 分区存储）

---

## 拍照

### 概述

在 YodaOS-Sprite 本机使用 AndroidX CameraX 的 `ImageCapture` 完成静态拍照，为标准 `android.hardware.camera2` 封装。

单色绿线屏不适合做实时取景预览；Sample 仅绑定 `ImageCapture`（无需 Preview），TouchPad 单击触发快门。

### 前置条件

- `CAMERA` 权限（运行时请求）
- `ProcessCameraProvider` 异步绑定（使用 `rememberCameraBound`，勿在 Compose 主线程同步调用 `ListenableFuture.get()`）

### 流程

1. 请求 `CAMERA` → 异步获取 `ProcessCameraProvider`
2. `unbindAll` 后仅绑定 `ImageCapture` 到 `LifecycleOwner`（`CameraSelector.DEFAULT_BACK_CAMERA`）
3. 配置建议：`CAPTURE_MODE_MAXIMIZE_QUALITY`、`setJpegQuality(100)`
4. TouchPad 单击 → `ImageCapture.takePicture` 写入落盘目录
5. 在 `OnImageSavedCallback` 中更新 UI；失败时在 `onError` 展示 `ImageCaptureException` 信息

### 落盘路径

| 优先级 | 路径 |
|---|---|
| 1 | `/sdcard/Pictures/bare_photo/yyyyMMdd_HHmmss.jpg` |
| 2 | `…/Android/data/com.savorsight/files/Pictures/bare_photo/` |

### UI 反馈（Sample）

| 区域 | 内容 |
|---|---|
| 主标题 | 准备中… → 相机就绪 → 拍摄中 → 已保存 / 拍照失败 |
| 副标题 | 与 ViewModel 状态同步（如 相机就绪 · 单击拍照） |
| 落盘路径 | BareSavedPathBlock 展示完整绝对路径 |

### 与系统拍照键

镜腿上方单击可能触发系统拍照；应用内拍照为 CameraX 独立流程（TouchPad 单击），二者可并存。

---

## 录像

### 概述

使用 CameraX `VideoCapture`（Recorder + VideoRecordEvent）录制 MP4。

不做取景预览。Sample 录制纯视频轨（不在 VideoCapture 上启用 `withAudioEnabled()`）。

### 前置条件

- `CAMERA` 权限
- `Manifest` 建议声明 `RECORD_VIDEO_OUTPUT` 及存储相关权限
- `ProcessCameraProvider` 异步绑定

### 流程

1. 异步获取 `ProcessCameraProvider`，仅绑定 `VideoCapture`（无需 Preview）
2. 配置 `Recorder` + `QualitySelector`（Sample：HD / SD + FallbackStrategy）
3. `prepareRecording(context, FileOutputOptions)` → `start`；单击再次 `Recording.stop()`
4. 在 `VideoRecordEvent.Finalize` 中判断 `hasError()`，成功则写入 `.mp4` 完整路径
5. 页面销毁时 `stop()` 并 `unbindAll`

### 落盘路径

| 优先级 | 路径 |
|---|---|
| 1 | `/sdcard/Video/bare_video/yyyyMMdd_HHmmss.mp4` |
| 2 | `…/Android/data/com.savorsight/files/Movies/bare_video/` |

### UI 反馈（Sample）

| 阶段 | 主标题 / 状态 |
|---|---|
| 绑定中 | 准备中… |
| 就绪 | 待机 · 单击开始 |
| 录制中 | 录制中 |
| 停止后 | 停止中… → 已保存 + 落盘路径，或 保存失败(…) |

### 与系统录像键

镜腿上方长按可能触发系统录像；应用内录像为 CameraX 独立控制（TouchPad 单击开始/停止）。

---

## 组件对照

| 组件 | 作用 |
|---|---|
| BareScreenLayout | 安全区内标题 + 线框内容面板 + 底部按键指引 |
| BareKeyLegendBar | 分行展示「单击 / 双击 / 长按」与当前动作文案 |
| BareContentPanel | 主内容线框（Stroke 描边，无实心 Card） |
| BareSavedPathBlock | 拍照/录音/录像完成后展示落盘绝对路径 |
| BarePageDots | 分页指示（Hub、子页） |
| SafeAreaFrame | 安全区参考描边（y=80～560） |
| rememberCameraBound | 异步 CameraX 绑定（务必使用此封装） |

---

## 构建安装

见快速开始专章。另见眼镜 UI 设计规范专章。
