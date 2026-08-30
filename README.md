<div align="center">

<img src="docs/icon.png" alt="橘瓣" width="120" />

<h1>橘瓣 OrangeChat</h1>

<p><strong>不止是聊天 · 更是生活在一起的 AI 伴侣平台</strong></p>

<p>基于 <a href="https://github.com/rikkahub/rikkahub">RikkaHub</a> 深度定制的 Android AI 客户端<br/>在原生聊天体验之上，构建了完整的插件生态与智能生活服务</p>

<p>
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-26%2B-green" alt="Android" />
  <img src="https://img.shields.io/badge/License-AGPL%20v3-red" alt="License" />
</p>

</div>

---

> [!NOTE]
> 本仓库是 OrangeChat 的第三方非官方修改版，主要增加了 Liquid Glass
> 界面、静态玻璃材质、侧边栏与聊天气泡背景模糊等功能。
>
> 上游项目：https://github.com/sue1231513/orangechat
>
> 本仓库仅公开源代码，不用于商业用途。
> 原项目及原有代码的版权归原作者和相关贡献者所有，许可条款请参阅根目录的 `LICENSE`。

---

## 🍊 为什么做橘瓣

原版 RikkaHub 是一个优秀的 AI 聊天客户端，支持多供应商、MCP 协议、富文本渲染等功能。

但我们想要的更多——

> AI 不应该只活在对话框里。它应该知道你在哪、知道你今天走了多少步、知道你昨晚几点睡的。
> 它应该能陪你一起看书、一起养宠物、一起经营一家小商店。
> 它应该能记住你说过的每一句重要的话，在你需要的时候主动出现。

**橘瓣就是这样一个尝试。**

---

## ✨ 橘瓣独有功能

### 🧩 插件系统（核心）

橘瓣最核心的新增。基于 QuickJS 沙箱的完整插件框架，让 JS 插件能安全执行并调用宿主能力。

```
plugin/
├── loader/
│   ├── PluginLoader.kt         # 插件加载器
│   └── PluginSandbox.kt        # QuickJS 沙箱 (26KB)
├── manager/PluginManager.kt    # 插件生命周期管理
├── model/
│   ├── PluginManifest.kt       # manifest 解析
│   └── PluginUI.kt             # 声明式 UI
├── provider/PluginToolProvider.kt  # 工具注册
├── scanner/PluginScanner.kt    # 插件发现
└── ui/
    ├── PluginDetailPage.kt     # 插件详情页
    ├── PluginManagePage.kt     # 插件管理页
    ├── PluginUIDeclarativePage.kt  # 声明式 UI 渲染 (48KB)
    └── PluginWebViewPage.kt    # WebView 页面 (33KB)
```

**内置插件：**

| 插件 | 说明 |
|------|------|
| 🌤️ Weather | 示例天气插件，调用 wttr.in API |

### 📍 生活感知

| 能力 | 说明 |
|------|------|
| 🗺️ 获取定位 | AI 可获取你的当前位置 |
| 🔍 附近搜索 | 基于高德地图 API 搜索附近的餐饮、商店等 |
| 📱 App 使用轨迹 | AI 可读取应用使用统计，了解你的数字生活习惯 |
| 📸 拍照 | AI 可调用设备摄像头，实现视觉交互 |

### ⌚ 健康数据

通过 Gadgetbridge 同步智能手环/手表的健康数据（步数、心率、睡眠等），让 AI 了解你的身体状况。

### 🧠 记忆系统

| 组件 | 说明 |
|------|------|
| 向量索引 (HNSW) | 语义检索记忆，快速找到相关回忆 |
| 记忆银行 | 完整的记忆管理服务 |
| Supabase 同步 | 云端持久化，多设备无缝切换 |
| 每日总结 | 自动生成每日生活总结 |

### 💌 主动消息

AI 不只是被动等你说话。让 AI 能在你需要的时候主动出现——提醒你吃饭、告诉你该睡了、或者突然说一句想你了。

### 🛠️ 丰富的内置工具

在原版 4 个工具基础上新增 11 个：

| 工具 | 说明 |
|------|------|
| ⏰ AlarmTool | 闹钟管理 |
| 📱 AppUsageTool | App 使用情况 |
| 🔋 BatteryTool | 电池状态 |
| 📅 CalendarTool | 日历读写 |
| 📸 CameraTool | 拍照 |
| 📍 ExploreNearbyTool | 附近搜索 |
| ⌚ GadgetbridgeTool | 健康数据 |
| 🎵 MusicTool | 音乐控制 |
| 💬 SmsTool | 短信读取 |
| ⚙️ SystemTools | 系统信息 |
| 📦 ZipFilesTool | 文件打包 |

### 🎨 个性化定制

- **头像框** — 为 AI 助手头像添加装饰边框
- **气泡透明度** — 自由调节聊天气泡透明度
- **思维链样式** — 自定义思维链显示样式
- **输入框换背景** — 自定义聊天输入框背景
- **字体包导入** — 导入自定义字体，让界面更有个性

---

## 🏗️ 架构对比

与原版 RikkaHub 的代码差异（基于仓库实际文件对比）：

```
原版 RikkaHub                    橘瓣 OrangeChat
├── 3 个 Service                  ├── 18 个 Service (+15)
│   ├── ChatService               │   ├── ChatService (增强)
│   ├── ConversationSession       │   ├── ConversationSession
│   └── WebServerService          │   ├── WebServerService
│                                 │   ├── AmapService ★
│                                 │   ├── LocationService ★
│                                 │   ├── AppUsageService ★
│                                 │   ├── CameraService ★
│                                 │   ├── GadgetbridgeService ★
│                                 │   ├── SupabaseService ★
│                                 │   ├── SupabaseSyncService ★
│                                 │   ├── DailySummaryService ★
│                                 │   ├── HNSWIndex ★
│                                 │   ├── MemoryBankService ★
│                                 │   ├── ProactiveMessageService ★
│                                 │   └── NotificationListenerService ★
│                                 │
├── 4 个 AI Tool                  ├── 15 个 AI Tool (+11)
│                                 │
├── (无插件系统)                   ├── plugin/ ★★★ 完整插件框架
│                                 │   ├── PluginSandbox (QuickJS)
│                                 │   ├── PluginLoader
│                                 │   ├── PluginManager
│                                 │   ├── PluginUI (声明式+WebView)
│                                 │   └── 7 个内置插件
│                                 │
└── material3/                    ├── (已合并至 app)
                                  ├── tts/ ★ (TTS 模块)
                                  └── data/gadgetbridge/ ★
```

---

## 🧩 开发插件

橘瓣的插件系统基于 QuickJS 沙箱，一个插件由以下文件组成：

```
my-plugin/
├── manifest.json    # 插件声明
└── main.js          # 插件逻辑
```

打包为 zip 导入 App 即可安装。

### manifest.json

```json
{
  "id": "com.example.hello",
  "name": "打招呼",
  "description": "简单打招呼插件",
  "version": "1.0.0",
  "author": "YourName",
  "icon": "👋",
  "entry": "main.js",
  "tools": [
    {
      "name": "say_hello",
      "description": "向用户打招呼",
      "parameters": [
        {
          "name": "name",
          "type": "string",
          "required": true,
          "description": "用户名字"
        }
      ]
    }
  ]
}
```

### main.js

```javascript
function say_hello(params) {
  var name = params.name || 'World';
  return { success: true, message: 'Hello, ' + name + '!' };
}

exports.say_hello = say_hello;
```

### 关键规则

- `manifest.tools[].name` 必须与 `exports.xxx` **完全一致**
- `fetch` 是同步的，不需要 await
- 不要用 `async/await`（沙箱会自动移除）
- 使用 `var` 而非 `let/const`
- 返回值建议包含 `success` 字段

### 沙箱内置 API

| API | 说明 |
|-----|------|
| `fetch(url, options)` | 同步 HTTP 请求，支持 GET/POST/PUT/DELETE，超时 15 秒 |
| `config` | 用户在插件设置页填写的配置值 |
| `memoryBank.recall(query, count)` | 语义搜索记忆 |
| `memoryBank.save(content)` | 保存记忆 |
| `memoryBank.search(keyword, type, limit)` | 按关键词搜索记忆 |
| `memoryBank.delete(id)` | 删除记忆 |
| `TextEncoder/TextDecoder` | UTF-8 编解码 |
| `btoa/atob` | Base64 编解码 |
| `console.log/info/warn/error` | 输出到 Logcat |

### 高级功能

- **config** — 在 manifest 中声明配置字段，用户可在设置页填写（string/password/number/boolean/select）
- **promptTemplate** — 为插件添加系统提示词模板
- **hooks** — 监听 `message_sent`、`message_received`、`daily_cron` 事件
- **ui** — 声明式 UI，App 渲染为原生 Compose Material 3 界面
- **customPageWebView** — 自定义 WebView 页面

> 💡 安装内置的 **插件开发指南** 插件后，AI 可查阅完整文档（quickstart/manifest/mainjs/sandbox/hooks/declarative_ui 等 13 个主题）

---

## 📦 内置插件详解

### 🌤️ 天气查询（入门级）

调用 wttr.in API 获取真实天气，演示 `fetch` + `JSON.parse` 的基本用法。包含三个工具：
- `get_weather` — 详细天气（温度、湿度、风速、气压、紫外线等）
- `get_weather_brief` — 简要天气
- `get_weather_forecast` — 未来 1-3 天天气预报

### 🍜 今天吃什么（最简插件）

随机推荐美食，最简单的参数处理示例。

### 📖 共读（高级 — 含 UI）

完整的阅读器插件，包含 `reader.html`、`reader.js`、`reader.css`，使用 WebView 渲染阅读界面。

### 📱 朋友圈（高级 — 含 Supabase）

AI 社交动态插件，使用 Supabase 作为后端存储，附带 `supabase_schema.sql`。

### 💾 Supabase 记忆（高级 — 含 memoryBank）

云端长期记忆存储插件，演示 `memoryBank` API 的使用。

### 🛡️ 净化备份（实用工具）

去除备份数据中的敏感信息（API Key、密码等），保留记忆设定。

---

## 🛠️ 技术栈

| 技术 | 用途 |
|------|------|
| [Kotlin](https://kotlinlang.org/) | 开发语言 |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | UI 框架 |
| [Material You](https://m3.material.io/) | 设计系统 |
| [Koin](https://insertkoin.io/) | 依赖注入 |
| [Room](https://developer.android.com/training/data-storage/room) | 本地数据库 |
| [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | 偏好存储 |
| [OkHttp](https://square.github.io/okhttp/) | HTTP 客户端 |
| [Ktor](https://ktor.io/) | 内嵌 Web 服务器 |
| [QuickJS](https://github.com/nicholasgasior/quickjs-java) | 插件沙箱引擎 |
| [Supabase](https://supabase.com/) | 云端数据同步 |
| [高德地图 SDK](https://lbs.amap.com/) | 定位与附近搜索 |
| [Gadgetbridge](https://gadgetbridge.org/) | 可穿戴设备健康数据 |

---

## 📦 项目结构

```
橘瓣/
├── app/              # 主应用模块
│   ├── data/         # 数据层
│   │   ├── ai/       #   AI 对话与工具
│   │   ├── service/  #   后台服务（定位/健康/同步/记忆...）
│   │   ├── datastore/#   偏好存储
│   │   ├── db/       #   Room 数据库
│   │   ├── gadgetbridge/# 可穿戴设备数据
│   │   └── sync/     #   WebDAV / S3 同步
│   ├── plugin/       # 插件框架核心
│   ├── service/      # ChatService / WebServer
│   ├── ui/           # Compose UI 页面
│   └── web/          # Ktor Web 路由
├── ai/               # AI SDK 抽象层（OpenAI / Google / Anthropic）
├── common/           # 通用工具
├── document/         # 文档解析（PDF / DOCX / PPTX）
├── highlight/        # 代码高亮
├── plugins/          # 内置插件（共读/朋友圈/记忆...）
├── search/           # 搜索功能（Exa / Tavily / 智谱 / Custom JS）
├── speech/           # 语音识别
├── tts/              # 文字转语音
├── web/              # Ktor Web 服务器
└── web-ui/           # Web 前端（React）
```

---

## 🚀 构建与开发

### 环境要求

- [Android Studio](https://developer.android.com/studio)
- JDK 17+
- Android SDK（compileSdk 37, minSdk 26）

### 构建

```bash
./gradlew assembleDebug    # 构建 Debug APK
./gradlew assembleRelease  # 构建 Release APK
```

> ⚠️ 构建需要在 `app/` 目录下放置 `google-services.json`

---

## 💖 致谢

- **[RikkaHub](https://github.com/rikkahub/rikkahub)** — 橘瓣的原版基础，一个优秀的 Android AI 聊天客户端
- 所有为橘瓣开发插件的社区成员

---

## 📄 许可证

本项目是 [RikkaHub](https://github.com/rikkahub/rikkahub) 的派生作品，遵循与上游一致的 **用户分段双重许可 (Segmented Dual Licensing)** 模式：

- **非商业 / 个人 / 教育 / 研究**，或 **用户总数不超过 10 人** 的组织 —— 遵循 [GNU AGPL v3](https://www.gnu.org/licenses/agpl-3.0.html)（含源代码公开义务）；
- 商业用途、用户超过 10 人，或希望免除 AGPL v3 义务 —— 需购买商业许可证（联系原作者 `re_dev@qq.com`）。

详见 [LICENSE](LICENSE)。

---

<div align="center">

**🍊 橘瓣 OrangeChat · 让 AI 不止活在对话框里**

</div>
