# Hermes Android 1.5.1

本次升级修复移动端语音入口与 Hermes Agent 语音组件的兼容性问题。

## 修复内容

- 聊天输入框的麦克风不再直接依赖 Android 系统语音识别，统一进入 Hermes 语音对话并优先使用服务器 Agent STT。
- 当服务器返回 `transcribe_audio() got an unexpected keyword argument 'source'` 时，应用会识别为 Hermes Agent 与网关版本不匹配，不再暴露底层英文异常。
- 兼容性错误页新增“检查并更新 Hermes Agent”入口；打开远程网关并完成升级后，可原路返回语音页重新测试。
- 仍选择系统识别时，如果手机没有语音助手，会提示改用 Agent 自动识别或启用系统语音服务。

## 服务器要求

截图中的 `source` 参数错误来自服务器端组件版本不一致。请优先在“我的 → 远程网关”更新 Hermes Agent；更新完成、网关重启后再录音。最新版 Hermes Agent 的 `transcribe_audio` 已支持 `source` 参数。

若服务器无法使用应用内更新，请在服务器执行官方升级命令，或把自定义语音网关调用改成与当前 Agent 函数签名一致。

## 安装

版本号 `1.5.1`，`versionCode 151`，最低支持 Android 8.0。安装包沿用工程内固定开发签名，v0.3.1–v1.5.0 可直接覆盖安装。
