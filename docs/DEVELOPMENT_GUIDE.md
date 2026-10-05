# Hermes Android 开发指南

## 1. 工程概览

Hermes Android 是连接个人自部署 Hermes Gateway 的原生 Android 客户端。模型推理、Skills、MCP、工具和会话数据主要保留在服务端；客户端负责登录、会话交互、流式回复、文件与 Markdown、定时任务、常用设置和系统通知。

当前交付基线：

- 版本：`3.8.8`（默认 Debug 构建追加 `-debug`）
- `versionCode`：`388`
- 交付 APK applicationId：`com.qingyu.hermescompanion.preview`（`hermesPreview=true`）
- 默认 Release applicationId：`com.qingyu.hermescompanion`
- Debug applicationId：`com.qingyu.hermescompanion.debug`
- namespace：`com.qingyu.hermescompanion`
- minSdk：26
- targetSdk / compileSdk：36
- 构建与 JVM 测试：JDK 21；编译目标：Java 17
- UI：Jetpack Compose + Material 3
- 网络：OkHttp 4.12，HTTP JSON + WebSocket

## 2. 构建环境

使用 Android Studio 与 JDK 21，安装 Android SDK 36 和 Build Tools 36.0.0。工程固定使用 Gradle 8.13、AGP 8.13.2、Kotlin 2.3.20；Wrapper 和依赖首次下载需要可访问相应官方仓库。玻璃外观依赖的 JVM 测试需要 JDK 21。

命令行验证：

```bash
./gradlew testDebugUnitTest lintRelease assembleDebug
python3 -m unittest discover -s tests -v
```

生成 APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug 和 Release 均启用 R8、关闭资源裁剪。源码不包含签名密钥；没有指定密钥时，Debug 使用 Android 默认 Debug 签名，Release 产出未签名包。

维护者构建与已交付 APK 兼容的升级包：

```bash
./gradlew testDebugUnitTest lintRelease assembleRelease \
  -PhermesPreview=true \
  -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```

Release 输出为 `app/build/outputs/apk/release/app-release.apk`。`hermesPreview=true` 在 Release 中仅沿用已安装应用的包名，不添加 Preview 名称或版本后缀。必须使用原密钥才能覆盖已有安装。

当前 Gradle 签名配置沿用原开发密钥的别名和口令约定；`hermesSigningFile` 只指定文件路径。自行发布时用自己的签名配置或 Android Studio 签名流程，不要假定任意密钥只换路径就能使用。密钥和口令不得提交到仓库。

## 3. 工程目录

```text
app/src/main/java/com/qingyu/hermescompanion/
├── HermesCompanionApplication.kt     应用初始化
├── MainActivity.kt                   Compose 入口与顶层返回逻辑
├── data/                             网关请求、流式事件、附件和聊天洞察
├── diagnostics/                      启动期与运行期崩溃记录
├── model/                            UI 与网关数据模型
├── notification/                     对话与 Cron 系统通知
├── storage/                          加密配置和 Cookie 持久化
└── ui/
    ├── HermesApp.kt                  路由、底栏和页面装配
    ├── HermesViewModel.kt            核心状态、业务流程与协程生命周期
    ├── ChatLinks.kt                  聊天文件/图片路径解析
    ├── component/                    图标、头像、Markdown、编辑器等组件
    ├── screen/                       各业务页面
    ├── format/                       时间与标题格式化
    └── theme/                        明暗主题、三套皮肤和字体
```

资源与文档：

- `app/src/main/res/`：图标、默认头像、启动主题、颜色与系统资源。
- `tools/`：图标生成和 APK 辅助脚本。
- `docs/`：产品、界面、图标、网关和交接文档。
- `signing/`：本机可选的签名输入目录，密钥被 Git 忽略，不随源码分发。
- `today/` 与 `assistant/`：首页数据、操作回执、同步策略与提示词。
- `app/src/main/assets/`：操作指南、更新历史、首页协议、writer 和 Cron 模板。

## 4. 应用架构

### 4.1 状态与路由

`HermesViewModel` 持有单一 `AppUiState`，页面不直接操作网络。`HermesApp` 根据 `AppRoute` 装配连接、会话、聊天、空间、任务、我的和各设置页。

主要调用链：

```text
Screen 用户操作
  → HermesViewModel
  → HermesApiClient / SecureConfigStore / HermesNotifications
  → 更新 AppUiState
  → Compose 重组页面
```

在新增页面状态时，优先加入 `AppUiState`，避免在多个 Composable 中维护相互冲突的业务状态。临时弹层开关可保留为页面级 `remember` 状态。

### 4.2 登录与连接

1. 用户填写网关地址、用户名和密码。
2. `HermesApiClient` 调用 `/auth/password-login`。
3. 密码仅参与登录，不持久化。
4. Cookie 通过 `SecureCookieJar` 与 Android Keystore 加密后保存。
5. 启动时读取保存的地址和 Cookie，调用身份/状态接口恢复连接。
6. HTTP 地址必须由用户确认明文传输风险；公网环境建议 HTTPS。

### 4.3 会话与流式回复

- 会话列表、归档、项目和模型来自网关接口。
- 发送消息后，流式任务由 ViewModel 级协程和 `StreamController` 管理，不依赖聊天页面 Composable 生命周期。
- 用户离开聊天页后，请求继续运行；完成时更新会话缓存、未读状态并按设置发送系统通知。
- 网络抖动时通过心跳、重连和服务端消息同步恢复，不应重新提交用户消息。
- 不要把流式 Job 移回 `ChatScreen`，否则返回会话列表会再次中止生成。

### 4.4 Markdown、文件和图片

- 空间页通过文件接口浏览项目目录、读取和保存 Markdown。
- `MarkdownContent` 处理标题、列表、引用、代码块、表格、图片和文档链接。
- 聊天中的 `MEDIA:/...md`、绝对 Markdown 路径和标准链接会变成可点击文档卡。
- `@image:/...png` 与标准 Markdown 图片会由 ViewModel 异步读取，并在当前会话缓存缩略图。
- 从聊天打开 Markdown 时，`workspaceDocumentOrigin` 记录来源；关闭预览必须回到原聊天。从空间打开文档则维持空间层级。
- 图片全屏预览为覆盖式 Dialog；关闭或系统返回时不改变底层聊天路由。

### 4.5 设置与服务端配置

客户端支持模型、对话风格、审批、记忆、Skills、工具集和 MCP 的常用配置。保存前应以服务端返回结构为准，避免把客户端默认值无条件覆盖到网关。

### 4.6 通知与后台行为

- Android 13 及以上需要运行时通知权限。
- 对话回复完成后可发送消息通知，并在会话列表及底栏显示未读角标。
- Cron 使用系统 Receiver 在开机、应用升级和计划触发时恢复补偿检查。
- 当前没有接入厂商云推送；应用进程被系统完全终止时，实时对话完成通知能力受 Android 后台限制。

### 4.7 首页与自动任务

- `HomeMode.kt` 管理简洁首页与深度助理模式，选择保存在本机。简洁模式不被动轮询概览，已发起任务的结果核对仍继续。
- `today/` 从当前 Profile 工作区读取概览，支持加密缓存、旧路径兼容、操作编号和写回回执。当前收纳目录为 `.hermes-app/today/`。
- 卡片关联对话完成或恢复后，按事项、Profile 和工作区合并同步请求；由实际写入结果驱动首页变化，不把普通讨论视为事项完成。派发前的队列只在内存中，进程被终止后可手动刷新核对。
- `AppTaskConversations.kt` 区分主动聊天与 App 自动任务。手机列表过滤不等于删除服务器或 PC 对话。
- `HomePortraitPlayback.kt` 和 `HomePortraitCarousel.kt` 管理紧凑首页人物的轮播、点按回应与生命周期。减少动态效果、Android 8 和解码失败使用静态后备图；不调用 Agent。

## 5. 关键网关接口

实际字段兼容逻辑集中在 `HermesApiClient.kt`，页面不得自行拼接接口。当前主要接口族包括：

| 能力 | 接口族 |
| --- | --- |
| 登录与身份 | `/auth/password-login`、`/auth/logout`、`/api/auth/me`、`/api/auth/ws-ticket` |
| 网关状态 | `/api/status`、`/api/env`、`/api/config` |
| 会话 | `/api/sessions`、`/api/sessions/{id}` |
| 流式通道 | `/api/ws` |
| 文件 | `/api/files`、`/api/files/read`、`/api/files/upload` |
| 模型 | `/api/model/options`、`/api/model/auxiliary`、`/model` |
| 定时任务 | `/api/cron/jobs`、`/api/cron/jobs/{id}` |
| Skills / 工具 / MCP | `/api/skills`、`/api/tools/toolsets`、`/api/mcp/servers` |

不同 Hermes Gateway 版本可能返回不同字段名或嵌套结构。继续沿用现有的容错解析方法，不要在 UI 层假设单一 JSON 结构。

## 6. UI 与资源约束

- 三套皮肤：温暖灵动、液态玻璃、安静耐看；每套保留对应材质、控件和首页布局。首页模式与皮肤选择相互独立。
- 液态玻璃支持材质降级；三套外观均需验证文字可读性、长列表和大字号布局。
- 所有业务图标通过 `HermesIconKind` 和 `HermesMulticolorIcon` 使用 Hermes Light 资源。
- 返回箭头使用中性灰单色；不要重新引入灰蓝拼色或 Material 默认图标。
- 深色模式必须使用语义色和 `values-night` 资源，禁止硬编码浅色文字/分割线颜色。
- 用户与 Hermes 可分别选择头像；选择后由 `AvatarStorage` 缩放并复制到 `filesDir/avatars`，UI 只读取该私有目录中的文件。系统相册 URI 不进入长期配置。
- Compose 的 `painterResource` 只加载位图或 VectorDrawable，不可直接加载 `layer-list` 等 LayerDrawable。

历史视觉规范见 `UI_SYSTEM.md` 和 `ICON_SYSTEM.md`；当前行为及渲染示例以本版发布说明、`docs/design-3.8.8/` 和测试为准。

## 7. 稳定性保护项

以下改动曾在真机触发确定性问题，后续修改需要重点回归：

1. Debug R8 单 DEX：OnePlus / Android 16 曾在多 DEX Debug 包首帧前退出。
2. 启动图标：Compose 不可把 `layer-list` 当作普通 painter 加载。
3. 头像：不可长期保存并直接读取系统照片选择器的临时 `content://` URI；历史失效地址必须在启动时清理并回退默认头像。
4. 流式生成：协程不能绑定聊天页面的进入/退出。
5. 返回键：全屏图片、Markdown 文档、设置详情和主页面需要按由内到外的优先级处理。
6. Markdown 表格：同一行的各单元格必须使用统一最大行高，避免边框断层。
7. 启动页：Android 12+ 的系统启动窗口不能完全移除，只能匹配应用背景并移除多余居中图标。

## 8. 测试

本地单元测试位于 `app/src/test/`，当前覆盖：

- 网关解析与基础客户端行为
- 聊天产物/待办提取
- 流式恢复和中止后的状态整理
- Markdown 文档与图片链接解析
- 会话标题和时间格式化
- 卡片对话自动同步、审批核对、Profile 与工作区隔离
- 简洁/深度首页、三套外观、大字号与动画生命周期

3.8.8 已通过 505 项 Android/JVM/Robolectric 测试和 17 项 Python 测试，以及 `lintRelease`、`assembleRelease`。详见 [本版验证记录](VALIDATION-3.8.8.md)；这些检查不等同于真实网关和手机验收。

执行：

```bash
./gradlew testDebugUnitTest
```

完整构建：

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

真机回归至少包含：

1. 全新安装与覆盖安装各一次。
2. Android 12+ 启动窗口和首屏。
3. HTTP 风险确认、HTTPS 登录、Cookie 自动恢复。
4. 发送消息后立即返回列表，确认后台继续生成、完成通知和未读角标。
5. 进入未读对话后角标清除。
6. `@image:` 内联缩略图、全屏缩放和返回原聊天。
7. 聊天 Markdown 文档卡打开、系统返回和顶部箭头返回原聊天。
8. Markdown 长表格、深色模式和横向滚动。
9. 固定用户/Hermes 头像在列表、我的和聊天中的显示。
10. 断网、恢复网络和长会话重新进入。

## 9. 版本与交付流程

1. 修改 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`。
2. 更新 `app/src/main/assets/release-history.json`，运行 `python3 tools/export_release_history.py` 生成中英文日志；更新 `README.md`、文档索引和本版 release notes。
3. 执行完整构建和测试。
4. 检查 APK applicationId、版本号、签名、zipalign 和 DEX 数量。
5. 生成不包含 `.gradle/`、`.kotlin/`、`build/`、`local.properties` 的源码包。
6. 将 APK、源码、开发文档与完整交接包保存为同一版本号。

仅当前版发布说明留在 `docs/RELEASE-<version>.md`；旧版移到 `docs/archive/release-notes/`，同步修正引用及归档索引。验证报告与截图保留原位置。文档整理不需要提高 App 版本号或重新构建未改变的 APK。

源码同步 GitHub 与 APK 发布是独立步骤。更新 OSS 时先上传并确认 APK 可下载，再替换 `android-latest.json`，避免手机先看到一个尚不可下载的新版本；字段与校验规则见 [更新协议](APP-UPDATE-CONTRACT.md)。

如需正式上架，再补充正式签名、隐私政策、崩溃监控、混淆映射保管和商店素材，不要直接把 Debug 包作为生产发布包。
