# Hermes Android 0.6.5.11

本版本增加 Hermes Profile 切换。点击会话页左上角 Hermes 名称下方的 `Profile · 当前名称`，即可查看并切换服务器上的 Profile。

## 主要变化

- 自动读取 `/api/profiles`，显示 Profile 名称、说明与模型信息。
- 记住最近选择，重新启动后恢复。
- 会话、项目、设置、Skills、MCP、Cron 与实时对话均显式携带 Profile。
- 消息缓存与未读标记按 Profile 隔离。
- 回复生成期间不会允许切换，避免任务被错误归入另一个 Profile。
- 旧版网关缺少 Profile 接口时继续使用 `default`。

## 构建说明

版本号为 `0.6.5.11`，`versionCode` 为 `41`。使用 JDK 17、Android SDK 36 执行：

```bash
./gradlew clean testDebugUnitTest assembleDebug
```
