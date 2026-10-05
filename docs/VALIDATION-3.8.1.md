# Hermes Android 3.8.1 验证记录

验证日期：2026-10-04。以下检查在构建环境完成，没有连接用户的真实服务器或手机。

## 结果

| 检查 | 结果 |
| --- | --- |
| 全量 Android/JVM/Robolectric 测试 | 418 项通过，0 失败、0 错误、0 跳过 |
| Python writer 数据保护测试 | 8 项通过 |
| Release lint | 0 Error / Fatal；706 Warning、0 Hint，主要为未使用资源及依赖更新提示 |
| Release 构建 | assembleRelease 成功，R8 优化开启，非 debuggable |
| APK 签名 | apksigner verify 通过，原覆盖升级证书 |
| APK 对齐 | zipalign 4 字节 / 16 KiB 页面校验通过 |
| 包名 | com.qingyu.hermescompanion.preview |
| 版本 | 3.8.1 / versionCode 381 |
| 证书 SHA-256 | 741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6 |

## 覆盖范围

- 首页阅读优先、旧 v3 与新 v4 结构兼容、显式关注优先级、详情打开不提交动作。短详情窗口高度固定，滚动不改变锚点。
- 对话本轮资料去重及默认收起、内部背景不泄漏到恢复后的用户消息、文件仍能打开。新增明确日志折叠、失败审批保留、代码围栏不误判及真实结果摘录检查。
- 文件页目录双列，隐藏与系统文件可重新显示，附件选择正确路由；任务页排序、暂停项折叠及周计划显示，原管理动作保留。
- 现有网关、并行会话、服务器审批及卡片操作测试继续通过。
- writer 检查并发版本冲突、快照、回执幂等、保留新卡补旧动作回执、已关闭卡保护、示例写入拒绝及越界路径拒绝。

原生 UI 测试渲染使用 Android 35 的 Robolectric、390dp 屏宽；检查首页、事项详情、对话、文件、个人页和定时任务，另检查深色及大字号。截图为明确标注的虚构示例，不代表用户服务器内容。不是物理手机或端到端服务器测试。

## 复现

需要 JDK 21、Android SDK 36、Build Tools 36.0.0；Gradle Wrapper 8.13 已包含。内存较少时分步执行，避免测试、lint 和压缩并行占用过多内存。

```bash
./gradlew :app:testDebugUnitTest --max-workers=1 -Pkotlin.incremental=false -PhermesPreview=true
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease --max-workers=1 -Pkotlin.incremental=false -Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8" -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```

公开源码不含签名密钥；自行签名的包无法覆盖使用其他证书的已安装版本。交付 APK 已使用原密钥签名。

## 源码包

来源为已提供的 3.7.8 源码归档。原归档尾部不完整，恢复了 1407 个 CRC 正确的完整条目，补齐 Gradle Wrapper 和构建配置后编译验证。本次重新生成完整 ZIP，打包程序验证中央目录和每项 CRC，并随包附 SOURCE-MANIFEST.sha256。签名密钥、SDK、构建缓存、服务器凭据不进入源码包。

## 未覆盖

Agent 确认连续性的语义要求通过模板提供，不声称客户端能够理解或自动裁决矛盾事实。未远程安装服务端规范，未改变用户 Cron，也未验证真实 9119 网关和移动网络稳定性。安装后须通过首页“调整首页整理方式”交给 Hermes 执行并核验。本次客户端和模板升级不代表已经重新整理了用户的全部记忆。
