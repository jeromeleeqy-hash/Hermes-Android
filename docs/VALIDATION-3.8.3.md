# Hermes Android 3.8.3 验证记录

日期：2026-10-05。基于用户提供的 3.8.2 完整源码。以下为构建环境实测，不代表用户手机、Hermes 服务器或 OSS 已完成验收。

| 检查 | 结果 |
| --- | --- |
| 全量 Android/JVM/Robolectric 测试 | 454 项通过，0 失败、错误、跳过 |
| Python writer 与迁移测试 | 17 项通过 |
| Release lint | 0 Error/Fatal；709 Warning、5 Hint |
| 发布构建 | `assembleRelease` 成功，R8 优化，非 debuggable |
| APK 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.3 / 383；minSdk 26、targetSdk 36 |
| 签名 | APK v2 校验通过，沿用 3.8.2 原证书 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | zipalign 4 字节及 16 KiB 页面校验通过 |
| APK 字节数 | 64956570 |
| APK SHA-256 | `7b498db357242069b791b901ef6a5b7ea33b1cb07feb7bb48cfcb3b698748de9` |
| 随包规范 | APK 内 contract、writer、Cron 模板、使用说明和发布记录与最终源码逐字节一致 |
| 更新 JSON | 使用上述 APK 的实际包名、版本、大小与 SHA-256 |

## 关键覆盖

- 旧目录兼容、新目录优先、仅明确不存在时回退；权限错误、网络错误、损坏新文件和新旧冲突不会伪装成旧文件正常。
- 文件路径变更时，即使时间和大小相同也重新读取；手机缓存记录真实文件位置。
- 新安装直接写新位置、未迁移工作区仍写旧位置、双锁互斥、明确停止写入前禁止迁移、冲突保护、符号链接拒绝、备份回读、复制中断后恢复、快照保留与幂等迁移。
- 原操作回执与刷新回执分别去重，Cron 写入保留两类回执和已完成状态。
- 原处理对话丢失时留在首页并反馈；恢复沿用原操作编号，重复点击只创建一次；网络失败不误创建替代会话；仍在运行或等待审批时不重发。
- 首页刷新带入真实用户进展并创建一个后台处理会话；会话选择按 Profile、工作区和明确关联限制，排除工具/系统正文；刷新成功须匹配本次回执。
- 原生 UI 测试：提示条与玻璃导航栏左右可见边界一致；结果待核对可打开详情并继续；刷新图标触发整理而非只读同步；深色和放大字号下恢复按钮可滚动到达。

全量测试已在最终 Kotlin 代码上重跑通过。之后仅更新应用内使用说明的文字和收纳条目，完成 JSON 解析核对；发布 APK 已确认包含最终说明。未再变更业务实现。

## 界面证据

Robolectric Android 35、390 × 844dp，内容为测试样例：

- [提示条与底栏宽度](design-3.8.3/notice-width.png)
- [操作处理详情](design-3.8.3/operation-details.png)
- [深色放大字号下的处理详情](design-3.8.3/operation-dark-large.png)

## 尚待现场验证

本开发环境没有访问用户 Hermes 服务器，未迁移真实文件、更新真实 Cron 或核对用户提到的两件待办。安装后通过新增入口执行一次收纳，再用最近的真实进展测试首页刷新。模型如何理解语义仍由实际证据和服务器执行决定，客户端结构校验不能证明业务已完成。

公开域名下载、手机覆盖安装、不同厂商系统安装界面及网关对隐藏子目录的实际访问需现场验收。没有据此宣称修复未复现的其他网络问题。

## 复现构建

使用 JDK 21、Android SDK Platform/Build Tools 36、Gradle 8.13。玻璃效果依赖使用 Java 21 class 格式，JDK 17 无法运行相关原生 UI 测试。签名私钥由发布者在工程外提供。

```bash
./gradlew :app:testDebugUnitTest --max-workers=1 -Pkotlin.incremental=false -PhermesPreview=true
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease --max-workers=1 -Pkotlin.incremental=false -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```

完整源码 ZIP 包含逐文件 `SOURCE-MANIFEST.sha256`，打包后检查所有条目 CRC 与清单 SHA-256；排除签名私钥、构建缓存、SDK、临时配置和服务器凭据。自行签名的 APK 不能覆盖不同证书的已安装版本。
