# Hermes Android 3.8.6

2026-10-05，基于完整 3.8.5 源码。新增可选择的简洁首页，用户可以把 App 当作通用 Hermes 移动终端，也可以继续使用深度助理。

## 首页模式

首页右上角齿轮 → 首页模式 → 简洁首页 / 深度助理。选择立即生效并保存到本机；两边的齿轮都可切回。模式独立于温暖灵动、液态玻璃、安静耐看三套皮肤，不改变主题、日常对话绑定、草稿或 Profile。

简洁首页保留原版人物、问候、日期、“跟我说”和最近主动聊天。聊天、文件、任务、搜索与待确认授权均可继续使用。最近聊天使用现有的 App 任务标记与 Cron 来源过滤，不会因恢复旧首页重新展示自动任务。

深度助理继续显示事项卡片、进展及主动刷新。首页设置仍包含首次整理、早晚整理、文件收纳与默认折叠的维护工具；已有内容或文件已收纳时，不重复提供一次性按钮。

## 默认值与升级

- 新安装、没有本机深度首页使用记录的设备：默认简洁首页。
- 首次升级检测到旧版加密概览文件或 App 整理任务记录：保留深度助理。
- 已保存的选择优先，后续任务完成、重启或更换 Profile 不会把简洁首页切回深度助理。
- 只检查手机本机记录，不扫描服务器来推测偏好。因此换手机或清除 App 数据后需重新选择。

简洁模式不定时读取深度首页 JSON，也不在恢复登录时加载它的加密缓存。切换到深度助理只读取已有概览，不自动开始首次整理或配置早晚任务。已开始的任务和结果核对继续运行。

切换不会删除资料、停止 Cron 或清理服务器对话。已有早晚任务仍在服务器执行；需要暂停时，到任务页操作。PC 端对话列表仍由 PC/服务器管理。

## 原版核对

核对仓库 [jeromeleeqy-hash/Hermes-Android](https://github.com/jeromeleeqy-hash/Hermes-Android)，当时 main 为 `589ee0f8efd0e27ca621d33a80eda25e89869214`，版本 3.6.6。

本地原 `AssistantHomeScreen.kt` 与该提交的 [原版首页](https://github.com/jeromeleeqy-hash/Hermes-Android/blob/589ee0f8efd0e27ca621d33a80eda25e89869214/app/src/main/java/com/qingyu/hermescompanion/ui/screen/AssistantHomeScreen.kt) 逐字节一致。本版复用这套布局，只加入共享设置入口与自动任务过滤；保留后续版本的连接、授权、文件与任务修复。

## 安装和兼容

版本 3.8.6 / 386。沿用 `com.qingyu.hermescompanion.preview` 与原签名，可覆盖同包名、同签名旧版本。无需卸载。

首页协议、writer、Cron 模板与持久提示词保持 3.8.4，无需为此次首页模式切换重新收纳文件或配置定时任务。

验证结果见 [VALIDATION-3.8.6.md](VALIDATION-3.8.6.md)，操作与上传步骤见 [HERMES-3.8.6-INSTRUCTIONS.txt](HERMES-3.8.6-INSTRUCTIONS.txt)。
