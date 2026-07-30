# Content Player Android MVP

原生 Android 客户端，提供内容根目录浏览、搜索、播放、收藏和历史入口。客户端只访问业务服务的 `/api/v1`，不保存或传递 OpenList 凭据。

## 环境

- Android Studio Koala 或更新版本
- JDK 17
- Android SDK 35
- minSdk 26

项目使用 Gradle Kotlin DSL。仓库未包含 Gradle Wrapper，可直接用 Android Studio 打开本目录并使用 IDE 配置的 Gradle 8.7，或在已有 Gradle 8.7 环境中生成 Wrapper。

## BASE_URL

默认地址为 Android 模拟器访问宿主机的 `http://10.0.2.2:8080/`。通过 Gradle 属性覆盖，末尾斜杠可省略：

```bash
gradle assembleDebug -PBASE_URL=https://media.example.com/
```

也可以在用户级 `~/.gradle/gradle.properties` 中配置：

```properties
BASE_URL=https://media.example.com/
```

Debug MVP 允许 HTTP 明文流量，生产构建应使用 HTTPS 并收紧 Network Security Config。

## 构建

```bash
gradle assembleDebug -PBASE_URL=https://media.example.com/
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## API 契约

- `GET /api/v1/content?path=/` -> `ContentItem[]`
- `GET /api/v1/content?path=/&q=keyword` -> `ContentItem[]`
- `POST /api/v1/playback/resolve` body `{ server_id, path }` -> `{ url?, proxy_url?, headers }`
- `PUT /api/v1/progress` body `{ server_id, path, position_seconds, duration_seconds }`
- `GET /api/v1/favorites` -> 收藏条目数组
- `POST /api/v1/favorites` body `{ server_id, path, name }`
- `GET /api/v1/history` -> 历史条目数组

`ContentItem` 字段为 `server_id`、`path`、`name`、`is_dir`、可选 `size` 和可选 `modified`。`resolve.headers` 只用于当前媒体请求，客户端不持久化该值。相对 `proxy_url` 会基于 `BASE_URL` 补全。Media3 原生支持 HTTP Range；默认播放器控制器提供音轨和字幕选择。媒体请求收到 401 或 403 时，客户端重新调用 `resolve` 并恢复原播放位置。
