# Hermes 3.1.2 验证记录

- `:app:testDebugUnitTest :app:assembleDebug` 成功。
- 116 项测试通过；0 失败、0 错误、0 跳过。
- 6 项已有原生 UI 测试使用 Robolectric 4.16、Android API 35 原生图形模式：真实内容显示和首页附件/语音/输入回调、360dp 宽键盘布局、1.3 倍字体键盘布局、12 项选择滚动和确认、步骤时间线、工作详情布局。
- 2 项已有 ViewModel 测试：首页语音入口只消费一次且保留草稿；搜索返回首页。
- 原有 107 项回归继续通过，包括项目目录、并行会话、历史 404 和首页草稿。
- 键盘布局测试注入 280px IME 占位，输入条下缘与视口下缘相差 284px，即扣除键盘后只保留 4dp 间距。未再叠加导航高度。
- 原生预览通过 Android View.draw / 原生 Canvas 导出，没有使用 HTML 重新绘制或生成式图片代替应用界面。
- 新增 1 项原生配色和图标预览检查；桌面图片使用用户上传原图，Android drawable 负责缩放和裁切，另提供翼盔单色图标。
- 修正旧并发测试的 mock 准备时序：在启动后台流之前完成 createSession stubbing。
- 浅色主按钮 #00A8E8 / 文字 #003047 对比度约 5.13:1，文字链接 #0078AD / 白底约 4.90:1。
- 女孩绘制源文件与用户提供的首页参考图字节一致；只显示原画区域，并通过原生颜色矩阵将浅色背景衔接到冰蓝主题，保留姿势及构图。
- APK 签名验证、ZIP 完整性、16KB zipalign 通过；包名 com.qingyu.hermescompanion.debug，versionCode 312，versionName 3.1.2-release-debug。
- APK 与此前 3.1.0 测试版签名一致，可覆盖安装。

## 验证边界

预览采用测试中的示例数据，应用没有写入这些示例会话。Robolectric 是原生 Android 框架的本地测试环境，不是真机：键盘检查验证指定 inset 下的排版，不替代真实输入法和 OEM 系统事件验证。尚未连接用户真机或远程 Hermes Gateway。

Robolectric 与真机渲染仍可能存在差异，参考 [Android 官方测试说明](https://developer.android.com/training/testing/local-tests/robolectric)。首次导图遇到测试环境的窗口重绘超时，改用已完成布局的原生 View.draw 导出后通过；功能与几何断言均保留。

APK SHA-256：`635ee33fc6ebb1a56cad553fadd592e5e6528b8d3f8fe500e2e79ffd2a2c91a8`。
