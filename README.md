# Image/Video API Creator

## 中文

VideoCreator 是一个基于 Jetpack Compose 的 Android AI 图片与视频创作应用。应用在设备上的显示名为 **Studio**。它支持 MiniMax 与 Qwen (DashScope) 模型，并可根据输入和参考图自动识别文生图、图生图、文生视频或图生视频。

### 功能

- Material 3 / Jetpack Compose 界面
- 支持 MiniMax 与 Qwen 图片生成
- 视频生成采用异步任务轮询
- 根据提示词和附件自动判断生成模式
- 本地生成历史记录
- API Key 使用 AndroidKeyStore 加密存储
- 支持默认模型与参数配置

### 环境要求

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 36
- Gradle 8.9（项目已内置 Wrapper）

### 构建

推荐使用 Android Studio 打开项目，也可以在项目根目录运行：

```bash
./gradlew assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
```

构建产物位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### API Key

启动应用后进入 **设置**，填写 MiniMax 和/或 Qwen (DashScope) API Key。使用生成功能前至少需要配置一个平台的 Key。

API Key 会通过 AndroidKeyStore 加密后保存在本地 DataStore 中。请不要把真实 Key 提交到仓库。

### 支持的模型

| 平台 | 模型 |
| --- | --- |
| MiniMax | `image-01`、`image-01-live`、`MiniMax-H3` |
| Qwen | `qwen-image-3.0-pro`、`wan3.0-video` |

### CI

GitHub Actions 会在推送到 `main` 分支或提交 Pull Request 时构建 Debug APK。

### 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

## English

VideoCreator is an Android AI image and video creation app built with Jetpack Compose. The on-device display name is **Studio**. It supports MiniMax and Qwen (DashScope) models and automatically selects text-to-image, image-to-image, text-to-video, or image-to-video mode based on the input and attachments.

### Features

- Material 3 / Jetpack Compose UI
- Image generation with MiniMax and Qwen
- Video generation with asynchronous task polling
- Automatic mode detection from prompt and attachments
- Local generation history
- API keys stored with AndroidKeyStore encryption
- Default model and parameter settings

### Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 36
- Gradle 8.9 (provided by the wrapper)

### Build

Open the project in Android Studio, or run from the repository root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### API Keys

Launch the app, open **Settings**, and enter the API key for the provider you want to use: MiniMax, Qwen (DashScope), or fal.ai for Seedance 2.5.

API keys are stored locally using AndroidKeyStore-encrypted DataStore preferences. Do not commit real keys to this repository.

### Supported Models

| Platform | Models |
| --- | --- |
| MiniMax | `image-01`, `image-01-live`, `MiniMax-H3` |
| Qwen | `qwen-image-3.0-pro`, `wan3.0-video` |
| fal.ai | `Seedance 2.5` (text-to-video and image-to-video) |

### CI

GitHub Actions builds a debug APK on pushes to `main` and on pull requests.

### License

This project is licensed under the [MIT License](LICENSE).
