# Hermes Android 3.8.5 验证记录

2026-10-05。基于完整 3.8.4 源码。测试在构建环境和模拟网关完成，未连接用户的实际 PC、手机或服务器。

## 回归结果

- 全量 Android/JVM/Robolectric：481 项，0 失败、错误或跳过。
- Python writer 与文件迁移：17 项通过。
- 模拟官方响应：`session.resume` 省略空 `open_requests`，随后 `session.events.since` 返回显式空列表。旧请求正确清除，没有新建任务或提交授权。
- 备用查询携带恢复后的 runtime ID 和原 Profile。仍待确认的授权保留；协议不支持、权限错误、缺少列表、格式错误均不冒充已解决。
- 授权核对进度与结果直接在 Dialog 显示。重复点击不发起重复查询；小屏 320 × 640dp、字号缩放 1.5 的核对、移除和确认按钮保持可见。
- 本地移除需要明确确认，仅保存手机提醒变化，不调用服务器授权接口。后台核对再次得到待确认请求时，撤下本地移除入口。
- 用户未配置首页也能打开基础功能；齿轮直达首页设置，早晚整理从设置进入，打开只读核对，不自动调用配置任务。
- 已有早晚时间与时区可回填；迟到的配置读取不会覆盖已修改的输入；提交仍需明确点击。
- 卡片背景与来源操作的图标区域沿正文左边排列，点击目标至少 48dp；展开来源仍能打开对应文件。
- 3.8.4 的窗口稳定性、请求内容更新、跨连接隔离、任务归类，以及 3.8.3 的文件收纳、刷新回执和操作恢复均包含在全量回归内。

## 发布包校验

| 项目 | 结果 |
| --- | --- |
| 构建 | lintRelease、assembleRelease 成功，R8 优化 |
| Lint | 0 Error/Fatal；723 Warning、5 Hint |
| 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.5 / 385，非 debuggable |
| 系统版本 | minSdk 26、targetSdk 36 |
| 签名 | 原证书，APK v2 验证通过 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | 4 字节及 16 KiB 页面校验通过 |
| APK 字节数 | 64974642 |
| APK SHA-256 | `39651b258a0e3d66dafb5242a18997b8db321cf88f8337faad0a4f14fa5658bb` |
| 随包资源 | 首页规范、writer、Cron 模板、示例、使用说明与版本记录和最终源码逐字节一致 |

首页协议保持 3.8.4，旧有早晚配置无需为了此次 UI 更新重做。更新 JSON 使用本次 APK 的实际版本、大小与 SHA-256。源码归档包含全部源文件及校验清单，排除构建缓存、机器配置和签名私钥。

## 原生 UI 截图

使用测试样例，没有写入用户真实命令或业务数据。

- [首页齿轮](design-3.8.5/home-gear.png)
- [首页配置说明](design-3.8.5/home-setup.png)
- [图标与正文左对齐](design-3.8.5/aligned-card-actions.png)
- [小屏授权核对与移除提醒](design-3.8.5/approval-status-small-dark.png)

## 复现

JDK 21、Android Platform / Build Tools 36、Gradle 8.13。签名私钥不包含在源码中。

```bash
./gradlew :app:testDebugUnitTest
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore
```

## 验证边界

以上确认了模拟网关协议行为与 Android 原生组件渲染，不等于已在用户实际网关及手机厂商系统上验收。安装后需复核旧授权的真实跨端状态。PC 对话列表仍由 PC/服务器管理；手机任务分类不会删除服务器记录。
