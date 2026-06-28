# SavorSight（见味）

<p align="center">
  <strong>基于 Rokid Glasses 的 AI 做菜辅助应用</strong>
</p>

<p align="center">
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="Licensed under Apache 2.0" />
  </a>
  <a href="https://github.com/hx23840/SavorSight">
    <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android" />
  </a>
</p>

---

佩戴眼镜观看做菜视频，眼镜自动采集第一视角的音视频，后端 AI 理解生成个人菜谱。下厨时，眼镜提供菜谱导航、语音提示和实时检查，让烹饪更轻松。

## ✨ 功能特点

- **智能学习**：录制做菜视频，自动生成结构化菜谱
- **步骤导航**：分步指导，语音朗读当前步骤
- **实时检查**：关键步骤拍照，AI 判断是否达标
- **极简交互**：专为眼镜端设计，单击翻页、长按操作

## 📁 项目结构

```
SavorSight/
├── SavorSightApp/          # 裸机 Android App（主应用）
│   ├── app/               # 应用配置
│   ├── activities/        # Activity
│   ├── savorsight/        # 核心业务页面
│   ├── sensor/            # IMU 传感器
│   ├── ui/                # UI 组件和设计系统
│   └── input/             # 眼镜按键分发
├── server/                # Rust 后端服务
│   └── src/
│       ├── routes/        # API 路由
│       ├── models/        # 数据模型
│       └── services/     # QwenVL 多模态理解
├── savorsight-glasses-ui/ # 眼镜端 UI 设计稿（HTML/CSS）
├── aiui-app/              # AIUI 应用（已废弃）
└── docs/                 # 技术文档
    ├── SavorSight-project-architecture.md  # 技术架构
    └── api/               # API 规范
```

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────┐
│            裸机端（眼镜 App）                │
│                                             │
│   采集层：摄像头 + 麦克风 原始数据上传        │
│   交互层：菜谱列表 → 详情 → 做菜导航        │
└─────────────────────────────────────────────┘
                      ↕
┌─────────────────────────────────────────────┐
│              Rust 后端服务                  │
│                                             │
│   QwenVL 多模态理解 → 结构化菜谱           │
│   步骤检查判断                               │
└─────────────────────────────────────────────┘
```

详见 [技术架构文档](docs/SavorSight-project-architecture.md)

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog 或更高版本
- Android SDK API 34+
- Gradle 8.4+

### 构建

```bash
cd SavorSightApp
./gradlew assembleDebug
```

### 安装

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📖 文档

- [技术架构](docs/SavorSight-project-architecture.md)
- [服务端 API](docs/api/savorsight-server-api.md)
- [开发计划](docs/plans/)

## 👏 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

Apache License 2.0 - 详见 [LICENSE](LICENSE)
