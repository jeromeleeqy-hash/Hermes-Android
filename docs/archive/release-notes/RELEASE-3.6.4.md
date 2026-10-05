# Hermes 3.6.4

本版加入两款可切换的桌面图标和两段人物互动，并修复玻璃底栏下圆角与首页问候语刷新。

| 项目 | 本版行为 |
| --- | --- |
| 桌面图标 | “我的 → 设置 → 外观 → 桌面图标”可选择“白发伙伴”或“耳机精灵”。默认白发伙伴，选择保留至下次启动和升级。 |
| 点击人物 | 助理首页点击人物，交替播放“抱臂挑眉”“伸懒腰”。每段约 5 秒，连续点击不会重播或排队；播完回待机。 |
| 三套首页 | 玻璃、温暖首页使用全身动作，安静首页保留半身展示。离开首页或应用退到后台结束互动；减少动态效果和 Android 8 使用静态人物。 |
| 玻璃底栏 | 根据实际底栏位置与圆角轮廓裁剪列表。两处下圆角外侧和手势区域显示同一张页面渐变，栏内仍能透出滚动内容。所有使用底栏的长页面共用此处理。 |
| 时间问候 | 软件保持打开时，每分钟刷新问候语；修改系统时间、时区或回到前台也会立即更新。 |
| 使用说明 | 增加图标切换及人物互动两篇操作指引，保留中英文与分级导航。 |

桌面图标由系统 Launcher 刷新，部分手机会延迟几秒。两款图标不会覆盖用户自行设置的对话头像。Android 自适应图标采用系统蒙版，并提供单色主题图标资源。

## 安装

`Hermes-3.6.4.apk` 为正式 Release 构建，内部版本号 364。沿用 `com.qingyu.hermescompanion.preview` 和原预览签名，适用于覆盖同签名的 3.6.3 及此前版本。无需卸载已有软件。

证书 SHA-256：`741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6`。

## 构建

JDK 21、Gradle 8.13、Android SDK / Build Tools 36。源码不包含签名私钥、连接凭据或构建缓存。

```sh
./gradlew -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/hermes-preview.keystore :app:testDebugUnitTest :app:assembleRelease
```

`tools/export_launcher_364.py` 从两款确认原图导出图标资源。`tools/prepare_interactions_364.py /path/to/upload` 从两段确认视频生成透明 WebP 和静态首帧。应用内包含 65 条有记录的历史版本，完整记录见 `CHANGELOG.md` 和 `CHANGELOG.en.md`。

图标切换使用 Android 的 [activity-alias](https://developer.android.com/guide/topics/manifest/activity-alias-element)，真实入口 Activity 保持可用，分享与通知不随图标切换禁用。

## 验证结果

- 54 个测试套件，307 项测试全部通过，0 失败、0 跳过。
- 三套首页覆盖中英文、360×780 与 390×844；玻璃底栏覆盖浅深色及 0/24/48 dp 底部安全区。
- Android API 26/28/35 图标资源加载通过；API 28/35 图标切换、异常恢复和分享入口验证通过。
- Release 构建与 Lint Vital 通过；APK v2 签名、ZIP CRC、DEX 校验和及 16 KB ZIP 对齐通过。
- 与 3.6.3 包名和签名一致。尚未进行真机覆盖安装和各厂商桌面刷新验证。

APK SHA-256：`e228eebad3a513598c635f238915eec2dfbe249ed830c90767e6d3b30e5c34ec`。
