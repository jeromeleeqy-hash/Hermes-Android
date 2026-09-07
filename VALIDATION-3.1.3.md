# Hermes 3.1.3 验证记录

- 原生 Kotlin / Compose，versionCode 313；测试包 com.qingyu.hermescompanion.debug。
- :app:testDebugUnitTest :app:assembleDebug 成功，129 项通过，0 失败 / 错误 / 跳过。
- 新增 3 项真实 HTTP 协议回归：各 Profile 的 terminal.cwd；目录读取失败不回退服务器根目录；客户端当前 Profile 改变后，显式 Profile 请求保持原作用域。
- 新增 5 项 ViewModel 回归：文件入口优先当前项目、聊天文件保留会话目录、未选项目读 Profile 目录、迟到的旧项目请求不覆盖新目录、切换 Profile 清理目录并恢复它自己的项目选择。
- 新增 5 项原生 UI 回归：回看、文件、执行中心、设置的真实界面渲染，以及四栏导航的点击和选中状态。
- 保留键盘 360dp / 1.3 倍字体的 284px 间距断言、长选项确认、并发会话、历史 404 等既有回归。
- APK 签名验证、16KB zipalign 和 ZIP 完整性通过，与此前交付测试版签名一致。
- 首页女孩源文件与用户原参考图字节一致；桌面图标仍为用户上传的 Hermes 人物原图。

## 本次实现

主题统一控制按钮、开关与语义图标。开关采用白色滑块 / 明蓝轨道；创建按钮采用白图形；设置图标取消高饱和彩色底。回看新建按钮采用 56dp 圆角方形。

导航为助理 / 回看 / 文件 / 我的；未选中描边，选中实体，没有图标底色；选择动画采用弹性缩放、上移和图形渐变。页面进入采用 240ms 淡入上移，启用统一点击反馈。系统动画比例仍由 Compose 遵从。

文件页使用当前项目或当前 Profile 的 terminal.cwd。从聊天进入才继承会话目录。无法确定目录或读取失败时明确报错，不返回服务器根目录。Profile 配置目录（profile.path）没有被误当作工作目录。项目目录选择器保留显式浏览服务器目录的功能。

## 验证边界

截图来自 Robolectric 原生图形模式 / Android View.draw，采用测试示例数据；不是真机截图。尚未连接用户真机或远程 Gateway。动画已实现并验证导航终态，真实设备的帧率、OEM 输入法和用户服务器版本仍需实际使用确认。

Profile 工作目录概念参考 [Hermes 上游问题记录](https://github.com/NousResearch/hermes-agent/issues/40334)；具体请求沿用本项目已有的 /api/config 与 Profile 查询参数协议，并通过 MockWebServer 验证。

APK SHA-256：`eede10e7a1f58df7a01cfa60a5735facd6000972ef05da9aa1316042ba5cde45`。
