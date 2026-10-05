# Hermes Android 3.8.7 验证记录

2026-10-05，基于完整 3.8.6 源码。验证在构建环境、Robolectric 与模拟网关完成，未连接用户的实际手机或 Agent。

## 结果

- 全量 Android/JVM/Robolectric：498 项，0 失败、错误或跳过。
- Python writer 与文件迁移：17 项通过。持久协议、writer 和 Cron 模板保持不变。
- testDebugUnitTest、lintRelease、assembleRelease 成功。

## 本次关键验证

- 卡片对话入口保存按服务器、账户和 Profile 隔离的关联。聊天附件不再附带 JSON writer、操作协议和布局字段。
- 实时完成与恢复完成都进入同一条同步路径。已提交的卡片按钮操作自动读取实际回执，切到简洁首页后也能收取已有工作的结果。
- 聊天完成不直接标记事项已完成。只有服务器概览和对应回执返回后，首页才展示核对后的状态。
- 刚完成的消息直接作为有界证据传递，避免服务器历史延迟遗漏新进展；只传本次卡片关联对话，不扫描其他聊天。
- 连续补充合并为一次同步，运行中的刷新后最多按新的进展补一次后续同步；后台刷新完成不会递归新建刷新。
- 普通聊天、错误工作区关联不自动新建首页 Agent。原卡片 ID 与来源范围保持约束。
- 后台同步保留当前路由、所选聊天、未发送草稿；临时任务分类仍隐藏手机普通会话列表。
- 手动重命名使用原始输入，空白禁用、长名与标点保留，API 明确携带 Profile；没有 AI 标题生成请求。重新读取服务器元数据保持名称，日常对话保留绑定。
- 实际打开重命名输入窗并保存，确认不会陷入布局循环。
- 首页三套外观完成渲染；完成后的状态入口消失，处理中和需要确认时仍可打开。
- 320 × 640dp、1.5 倍字号和深色模式下，更新与处理入口保持单行，内容可达；文本宽度检查容许物理像素取整产生的不足 1px 差异。
- 旧版授权核对、任务分类、文件收纳、主页切换、并发聊天与写入回执逻辑纳入全量回归。

## 发布包

| 项目 | 结果 |
| --- | --- |
| 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.7 / 387，非 debuggable |
| 系统版本 | minSdk 26，targetSdk 36 |
| 构建 | Release / R8 优化 |
| Lint | 0 Error/Fatal；727 Warning、5 Hint |
| 签名 | 原证书，APK v2 验证通过 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | 4 字节与 16 KiB 页面校验通过 |
| APK 字节数 | 65009622 |
| APK SHA-256 | `b7a279ca0b3bb5f36c0afac39309c4f1b86497d97c47f00dad9d8ed896e1d226` |
| 随包资源 | 规范、writer、Cron、示例、操作说明与版本记录逐字节匹配源码 |

更新 JSON 从实际 APK 生成版本、字节数和 SHA-256。全量源码 ZIP 附校验清单，排除签名私钥、构建产物、机器配置和缓存。

## 渲染截图

- [温暖灵动首页](design-3.8.7/home-clean.png)
- [液态玻璃首页](design-3.8.7/home-glass.png)
- [安静耐看首页](design-3.8.7/home-paper.png)
- [手动重命名](design-3.8.7/manual-rename.png)
- [小屏、大字号和深色模式](design-3.8.7/header-small-dark-large-text.png)

使用测试样例，无真实用户聊天。截图聚焦组件；首页未包含全局底栏，输入窗截图仅包含窗口本身。

## 复现

JDK 21、Android Platform / Build Tools 36、Gradle 8.13。

```bash
./gradlew :app:testDebugUnitTest
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore
```

## 实际使用边界

真实 Agent 是否按预期措辞、写回耗时、授权配置及厂商后台限制仍需安装后核对。自动更新为后台核对，需要网络和 Agent，并非本地立即勾选完成。旧版未关联的卡片聊天可手动刷新或从卡片重新进入。App 被强制结束前尚未发出的合并队列不持久化；恢复中的聊天继续采用原有恢复机制。PC 端临时会话记录保留现有行为。
