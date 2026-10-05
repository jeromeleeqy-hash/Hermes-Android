# 3.7.5 验证记录

日期：2026-10-03。产品代码提交：`6cbc271c071a66f02f6904a2fcbdde6cab8214a5`。

- JDK 21、Android SDK / build-tools 36、Gradle 8.13；关闭增量 Kotlin 编译后运行 clean、testDebugUnitTest、lintRelease、assembleRelease，构建成功。
- 371 项 Android 单元及 Robolectric 原生渲染测试通过，零失败、零错误、零跳过；7 项 Python 写入器测试通过。
- 新增验证：并发 RPC 等待 gateway.ready；连接检查保留等待中的 RPC；断连后下一次请求重新连接且不重放旧请求；首页同步静默、合并写回触发、进入冷却时间；非首页停止轮询；失败保留旧卡片；文件元数据未变时复用内容、变化或超时后重读。
- 原生渲染检查覆盖三套皮肤的明暗主题、390dp 首页和 360dp / 1.4 倍字体；移除的重复导航不再出现，菜单、卡片选项及展开操作仍可达。截图使用明确的测试示例，非用户真实数据。
- lint：0 Error / Fatal，656 Warning（数量与 3.7.4 一致）；仍有已有弃用与平台提示，未声称全项目无警告。
- APK 包名 com.qingyu.hermescompanion.preview，versionCode 375，versionName 3.7.5。ZIP CRC、单 DEX、16 KiB ZIP 对齐、v2 签名及内置规范/脚本/示例/指南/版本记录和源码一致性均通过。
- 签名证书与已交付 3.7.4 一致：SHA-256 741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6。
- APK：64,870,972 字节；SHA-256 d9269709ba6003952401d76c22c6478e03520018b4083b7562c780f1f12b239b。

限制：未在用户实体手机或真实网关上验证 Wi-Fi/移动网络切换、服务器负载、代理超时和 OEM 后台限制。模拟网关回归通过只验证列出的客户端行为，不代表已排除服务器或网络因素。源码包不含签名私钥、手机缓存或服务器用户数据。
