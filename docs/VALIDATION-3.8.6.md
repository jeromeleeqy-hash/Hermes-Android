# Hermes Android 3.8.6 验证记录

2026-10-05，基于完整 3.8.5 源码。测试在构建环境及模拟网关完成，未连接用户的实际 PC、手机或 Hermes 服务器。

## 回归结果

- 全量 Android/JVM/Robolectric：489 项，0 失败、错误或跳过。
- Python writer 与文件迁移：17 项通过。本次未改变首页协议、writer 或 Cron 模板。
- 实际共享首页入口在三套皮肤下从简洁切到深度、再切回简洁；日常对话按钮仍可点击，未调用生成概览或配置定时任务。
- 简洁首页过滤 App 任务标记及 Cron 来源，保留主动聊天及待确认授权；点击最近聊天打开对应会话。
- 320 × 640dp、1.5 倍字号下两种模式均可滚动到达并选择；深度首页首次整理入口仍可用。
- 新用户默认简洁；旧深度使用记录选择深度；有效的显式选择优先于新旧记录，未知值安全回退。
- 模式偏好重开配置存储与退出连接后保留；更改模式不改变外观或日常对话绑定。普通已检查聊天与空任务列表不作为深度使用证据。
- 简洁模式阻止常规及强制后台概览读取；切回深度只读取已有概览，没有创建会话。
- 运行中的两个会话、未发送草稿、已有概览与操作记录在切换模式后保留。
- 3.8.5 授权核对、跨端请求、内容操作对齐与早晚设置，以及旧版文件收纳、刷新回执、任务归类和操作恢复均纳入全量回归。

## 发布包校验

| 项目 | 结果 |
| --- | --- |
| 构建 | testDebugUnitTest、lintRelease、assembleRelease 成功，R8 优化 |
| Lint | 0 Error/Fatal；723 Warning、5 Hint |
| 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.6 / 386，非 debuggable |
| 系统版本 | minSdk 26、targetSdk 36 |
| 签名 | 原证书，APK v2 验证通过 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | 4 字节与 16 KiB 页面校验通过 |
| APK 字节数 | 65008426 |
| APK SHA-256 | `db63f5d6a34d326e6980b526f8ace6ef60e6766d9d9a33cddfbe6e3be613a292` |
| 随包资源 | 首页规范、writer、Cron 模板、示例、使用说明和版本记录与最终源码逐字节一致 |

更新 JSON 根据实际 APK 生成版本、大小和 SHA-256。源码归档包含全量源文件及校验清单，排除构建缓存、机器配置与签名私钥，保留上一版本所有源码文件。

## 原生 UI 截图

使用测试样例生成并检查，无用户真实聊天或业务文件。

- [温暖灵动简洁首页](design-3.8.6/simple-clean.png)
- [液态玻璃简洁首页](design-3.8.6/simple-glass.png)
- [安静耐看简洁首页](design-3.8.6/simple-paper.png)
- [模式选择](design-3.8.6/home-modes.png)
- [小屏、大字号和深色设置](design-3.8.6/home-modes-small-dark.png)

截图聚焦首页与设置组件，不包含 App 全局底部导航。

## 复现

JDK 21、Android Platform / Build Tools 36、Gradle 8.13。源码不含签名私钥。

```bash
./gradlew :app:testDebugUnitTest
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore
```

## 验证边界

模拟测试不等于已在手机厂商系统上验收。安装后需核对覆盖升级的默认模式、重开 App 后的选择、真实会话与运行任务。已有早晚任务运行在服务器，模式切换不会暂停它们；PC 对话列表仍由 PC/服务器管理。
