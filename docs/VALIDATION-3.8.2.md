# Hermes Android 3.8.2 验证记录

日期：2026-10-04。构建环境检查，不代表已在用户真实手机或 OSS 上验收。

| 检查 | 结果 |
| --- | --- |
| 全量 Android/JVM/Robolectric 测试 | 438 项通过，0 失败/错误/跳过 |
| 更新模块和 UI 最后复核 | 20 项通过，覆盖末次错误恢复与文案调整 |
| Release lint | 0 Error/Fatal，716 项 Warning |
| Release 构建 | assembleRelease 成功，R8 优化，非 debuggable |
| APK 包名 | com.qingyu.hermescompanion.preview |
| 版本 | 3.8.2 / 382，minSdk 26 |
| 签名 | apksigner 校验通过，与 3.8.1 原证书相同 |
| 证书 SHA-256 | 741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6 |
| 对齐 | zipalign 4 字节 / 16 KiB 页面校验通过 |
| 发布文件 | 版本信息使用此签名 APK 的实际字节数和 SHA-256 |

验证内容：更新源不使用 Agent Cookie/Authorization；HTTPS/域名/路径限制；拒绝重定向、错误包名、非法版本与超大元数据；HTTP 403/404/服务器异常分别处理；下载字节校验、版本匹配、签名匹配；系统下载编号和元数据在重新创建仓库后恢复；仅开放专用安装目录；场景选择不自动发起任务，自由输入可独立进入对话；安装须独立点击，校验中不显示安装按钮。

原生 UI 截图检查：390dp、Android 35 Robolectric，首页半身人物和高对比音波、六项帮我做入口、1.5 倍字号深色单列、更新窗口；既有首页/文件/任务/个人界面回归测试也通过。截图示例为测试内容，不是用户工作资料。

最后全量测试之后，仅调整了更新错误后的“重新检查版本”入口及不兼容提示文案；更新相关 20 项已再次运行。既有 JSON/MD/Cron 协议未改动；无需重复部署服务器文件。

未覆盖：公开域名真实下载、各厂商后台限制、实际系统安装授权和旧版覆盖安装。发布到 OSS 后在真实手机完成验收。本版不声称修复尚未复现的 9119 移动网络问题。

源码 ZIP 重新生成并校验所有条目 CRC；附 SOURCE-MANIFEST.sha256，不包含签名密钥、SDK、构建缓存和服务器凭据。需 JDK 21、Android SDK/Build Tools 36、Gradle 8.13；公开源码自行签名不能覆盖其他签名的已安装 App。

```bash
./gradlew :app:testDebugUnitTest --max-workers=1 -Pkotlin.incremental=false -PhermesPreview=true
./gradlew :app:lintRelease :app:assembleRelease --max-workers=1 -Pkotlin.incremental=false -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```
