# Hermes 3.0.4a 验证记录

- Gradle `:app:testDebugUnitTest :app:assembleDebug` 成功。本环境关闭 Kotlin 增量编译，并重新生成 APK 打包缓存。
- 104 项测试全部通过，0 失败、0 错误、0 跳过。
- 新增覆盖：空的新会话首次打开及再次打开不请求未落库历史；任务 A 运行时创建并启动 B；真正缺失的历史会话仍显示错误。
- 测试收尾等待 ViewModel 协程结束，防止异步回调跨测试泄漏。
- APK 签名验证通过，证书与上一份 Hermes-3.0.4-test.apk 一致。
- 包名 com.qingyu.hermescompanion.debug，版本 3.0.4a-release-debug，versionCode 306。
- APK ZIP 完整性与 16 KB 页对齐检查通过。
- APK SHA-256：`fc41bc2525c75b0c7d58498a5b568280a1667049ab9774afff967f58491eb9b6`。

## 限制

未连接真实 Hermes Gateway，未执行 Android 真机界面验收。运行栏与新建按钮的位置依据 Compose 的实际占位布局修复，仍需按修复说明在手机上确认。原正式版签名未提供；本次 APK 仅保证签名和版本条件满足覆盖此前提供的 3.0.4 测试包。
