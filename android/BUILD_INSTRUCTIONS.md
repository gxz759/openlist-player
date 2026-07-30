# Android APK 编译指南

## 方法 A：使用 Android Studio（推荐）

1. **打开项目**
   ```bash
   # 在 Android Studio 中打开 /workspace/mobile/android 目录
   ```

2. **等待 Gradle 同步完成**
   - 首次打开会自动下载依赖（约 5-10 分钟）

3. **构建 APK**
   - 菜单栏：Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 或使用快捷键：Ctrl/Cmd + F9

4. **获取 APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## 方法 B：使用命令行

1. **安装 JDK 17+**
   ```bash
   # macOS
   brew install openjdk@17
   
   # Windows
   # 下载 https://adoptium.net/temurin/releases/?version=17
   ```

2. **编译**
   ```bash
   cd /workspace/mobile/android
   ./gradlew assembleDebug
   ```

3. **APK 位置**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## 方法 C：使用预编译版本（最快）

我会提供编译好的 APK 文件（如果云端编译完成）

## 功能清单

✅ 已实现功能：
- 登录界面（可跳过，演示模式）
- 视频内容列表
- 播放器核心功能：
  - ▶️ 播放/暂停
  - ⏪ 双击左侧/后退 10 秒
  - ⏩ 双击右侧/前进 10 秒
  - 👆 水平滑动快进/快退
  - 🔒 锁屏功能（双击解锁）
  - ⚡ 倍速播放 (0.5x - 2.0x)
  - 📊 进度条拖拽
  - 💾 播放进度记忆

## 下一步改进

1. 添加手势亮度/音量控制
2. 支持更多视频格式（MKV, AVI 等）
3. 添加字幕支持
4. 实现播放列表
5. 接入真实后端 API

## 测试视频

应用内置 3 个演示视频用于测试：
- Big Buck Bunny（开源动画）
- 自然风光演示
- 城市夜景

连接真实后端后可浏览 OpenList 服务器内容。
