# Hermes 3.1.0 验证记录

验证日期：2026-09-07。

- `:app:testDebugUnitTest :app:assembleDebug` 成功，包含最终 UI 和排版代码。
- 107 项测试通过；0 失败、0 错误、0 跳过。
- 新增 3 项实际 ViewModel 回归：首页草稿带入并返回首页、不自动发送；首页打开运行会话并保持后台任务；运行期间从首页在所选项目创建第二会话。
- 原有项目目录、并行会话隔离、404 历史读取、MockWebServer 流协议、语音路由等测试继续通过。
- APK 签名验证、ZIP 完整性和 16KB zipalign 检查通过。
- 包名 `com.qingyu.hermescompanion.debug`，versionCode `310`，versionName `3.1.0-release-debug`。
- 与此前交付 `Hermes-3.0.4a-test.apk` 的签名 SHA-256 一致，可覆盖升级此前测试包。
- 构建环境：JDK 17、Gradle 8.13、AGP 8.13.2、Kotlin 2.3.20、Android SDK 36。

## 边界

自动测试主要验证状态与协议，不等于 Android 真机视觉和交互验收。没有连接真机、模拟器或用户的真实 Hermes Gateway；没有声称完成原生截图、键盘、深色、字体放大或远程业务验收。此前 HTML 原型的 84 组布局压力测试不计入本 APK 的测试结果。

测试 APK SHA-256：`cdf4384756aa7c0013c31bf8184182f3edcd13d6ef4bc5c8e9140f6dddf33508`。
