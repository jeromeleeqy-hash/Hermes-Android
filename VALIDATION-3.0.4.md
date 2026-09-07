# Hermes 3.0.4 验证记录

验证日期：2026-09-07。

## 结果

- `:app:testReleaseUnitTest :app:assembleRelease` 执行成功。
- 101 项单元测试全部通过，0 失败、0 错误、0 跳过。
- Release Kotlin / Java 编译、Android 资源处理、Release 严重问题检查（lintVital）以及 APK 打包均通过。
- 构建环境：JDK 17、Gradle 8.13、Android Gradle Plugin 8.13.2、Kotlin 2.3.20、Android SDK / Build Tools 36。
- 版本：`3.0.4-release`；versionCode：`305`。

## 本次新增的回归覆盖

| 测试组 | 数量 | 验证内容 |
| --- | ---: | --- |
| ConcurrentGatewayTest | 6 | 真实 WebSocket 协议的模拟网关：双会话交错事件、单独停止、断线影响全部活动连接、项目创建与根目录、目录绑定失败阻止提交、发送前已停止的任务不提交 |
| ConcurrentSessionsTest | 4 | 实际 ViewModel：双会话切换和停止、迟到回调隔离、队列与草稿归属、后台失败不影响前台、所选项目目录传入新会话 |
| SessionRoutingTest | 4 | 运行 ID 的精确路由、无 ID 事件处理、旧注册清理不删除新注册、中文路径和根目录 |
| ProjectSelectionTest | 3 | 嵌套项目优先选择最具体目录、相似目录名不误匹配、额外目录和根目录 |

其余 84 项为原源码已有的回归测试，均通过。原有分段回复合并、长任务完成判定、引用展示和语音相关辅助逻辑的测试继续保留。

## 验证范围

模拟网关验证客户端的协议发送与事件分发，ViewModel 测试使用替代的网络与本机存储服务。没有连接真实部署的 Hermes Gateway，没有在 Android 真机上完成目录执行、网络切换或界面验收。真机验收步骤见 `Hermes-v3.0.4-release-notes.md`。

## 发布签名

提供的原始源码包未包含 `signing/hermes-debug.keystore`。本次构建产生的是未签名 Release APK，不能直接安装；源码包不附带该 APK。

发布覆盖升级时需要恢复原发布签名。使用新密钥签名不能覆盖旧 App，请勿为升级而先卸载应用。源码包保留了原有签名读取位置与 applicationId。
