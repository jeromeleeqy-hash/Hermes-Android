# Hermes Android 3.8.4 验证记录

2026-10-05。基于完整 3.8.3 代码。以下记录来自构建环境与模拟网关，不代表用户真机、PC 或 OSS 已验收。

## 已完成测试

- 全量 Android/JVM/Robolectric：472 项通过，0 失败、错误、跳过。
- Python writer / 文件迁移：17 项通过。
- 原生 UI 使用 Android 35、390 × 844dp 及 320 × 640dp，覆盖浅色、深色和字号缩放。
- 授权仅由一个窗口展示；滚动、重新接入运行会话及提交状态切换不改变窗口与底部按钮边界。相同授权重新核对保留选择；请求消失自动关闭。
- 小屏授权卡片按钮为单行，长选项可滚动到达，确认按钮保持可见。
- 首页菜单仅两个入口；一次性收纳显示完成状态，维护工具默认折叠。
- App 任务从普通会话列表排除，在任务记录中可查看；主动聊天保持可见。
- 模拟 WebSocket 网关：PC 已处理后的空快照、取消与无当前消息流的旧协议过期、重连重放、旧服务器不提供快照、捕获正确 Profile、显式审批范围。
- ViewModel：已处理/原会话不存在清除残留；离线/未知状态保留；授权内容变化拒绝旧答复；运行 ID 变化不重复请求；服务器切换忽略迟到结果；任务标记在执行前持久化，按服务器/账号隔离。
- 旧 App 任务只按完整附件标记识别，不按标题猜测；格式错误、其他 Profile/工作区和主动补充进展不被误归类。
- 首页进展刷新排除已知自动任务，不把配置指令当成用户的新进展。3.8.4 配置可通过早晚任务核验，保持旧版本兼容。
- 3.8.3 的提示条宽度、文件迁移、刷新回执、卡片恢复及既有聊天、语音、附件、文件、任务能力通过完整回归。

## 发布包校验

| 检查 | 结果 |
| --- | --- |
| Release 构建 | lintRelease、assembleRelease 成功；R8 优化 |
| Lint | 0 Error/Fatal；720 Warning、5 Hint |
| 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.4 / 384，非 debuggable |
| 系统版本 | minSdk 26、targetSdk 36 |
| 原签名 | APK v2 校验通过 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | zipalign 4 字节及 16 KiB 页面校验通过 |
| APK 字节数 | 64973814 |
| APK SHA-256 | `91b791dbc807e6fbe1be439aca0fda88c79984843b6ceb06371067c70004e918` |
| 随包文件 | 规范、writer、Cron 模板、格式示例、使用说明与版本记录和最终源码逐字节一致 |

更新 JSON 使用实际 APK 的版本、包名、字节数与 SHA-256。源码归档包含校验清单，排除构建缓存、机器 SDK 配置和签名私钥。

## 界面证据

以下使用测试样例，不是用户真实授权命令或业务数据：

- [固定授权窗口](design-3.8.4/approval-fixed.png)
- [小屏授权卡片](design-3.8.4/approval-card-small.png)
- [深色小屏授权](design-3.8.4/approval-large-dark.png)
- [首页设置](design-3.8.4/home-settings.png)
- [任务记录](design-3.8.4/task-history.png)

## 复现

JDK 21、Android SDK Platform / Build Tools 36、Gradle 8.13。签名私钥不在源码包中。

```bash
./gradlew :app:testDebugUnitTest
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore
```

## 现场核对

安装后请以实际网关核对 PC 授权同步和厂商系统弹窗表现。网络失败或旧服务器缺少明确请求列表时，App 保留请求；不会把未知状态当成已同意或已结束。文件收纳及真实任务结果依赖服务器执行和回读，安装包本身不会迁移服务器资料。
