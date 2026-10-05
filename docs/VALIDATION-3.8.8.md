# Hermes Android 3.8.8 验证记录

2026-10-05，基于完整 3.8.7 源码。验证在构建环境、Robolectric 与模拟网关完成，未连接用户的实际手机或 Agent。

## 结果

- Android/JVM/Robolectric 全量：505 项，0 失败、错误或跳过。其中新增 7 项覆盖本次人物与播放逻辑。
- Python writer 与文件迁移：17 项通过。
- testDebugUnitTest、lintRelease、assembleRelease 成功。
- 6 段成品全部为 320 × 320 透明 Animated WebP，原生 ImageDecoder 可解码。
- 原始视频 3 与 4 的解码帧 SHA-256 相同，4 未重复打包。

## 关键验证

- 每轮四段轻柔动作各播放一次，转头和眨眼交替作为点缀，跨轮不会连续重复。
- 点按切入眨眼，连续点按不重启；旧动画结束回调不能提前结束新的眨眼。
- 动作结束后等待 1–3 秒，再进入下一段；点击插入后自动轮播继续。
- 可见 → 不可见 → 可见、前台 → 后台 → 前台，以及减少动态效果均完成组件验证。测试同步推进 Compose 帧与 Android 主队列，涵盖 AndroidView 的恢复过程。
- 6 段原生动画能解码，尺寸一致；编码后每帧四角透明，片头片尾 alpha 完全一致，可见 RGB 的平均差异低于 2/255（有损压缩）。
- 六套首页预览（3 套外观 × 浅色/深色）检查人物、发丝和衣服边缘；76 × 82dp 区域直接显示新构图，没有沿用旧身体裁切。
- 原简洁首页、帮助入口、使用说明双语结构与旧功能全量回归通过。旧半身资源断言更新为新紧凑人物断言，使用说明数量随新增文章更新。
- 概览同步、卡片对话、服务器协议与 writer 未修改。

## 发布包

| 项目 | 结果 |
| --- | --- |
| 包名 | `com.qingyu.hermescompanion.preview` |
| 版本 | 3.8.8 / 388，非 debuggable |
| 系统 | minSdk 26，targetSdk 36 |
| 构建 | Release / R8，单 DEX |
| 签名 | 原证书，APK v2 验证通过 |
| 证书 SHA-256 | `741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6` |
| 对齐 | 4 字节与 16 KiB 页面校验通过 |
| Lint | 0 Error/Fatal；728 Warning、5 Hint |
| APK 字节数 | 81602836 |
| APK SHA-256 | `4c752d26772881f71e83ffb25fe72c4ea3aba6ad0cd78ae089c8ea25cdcead97` |
| 资源 | 规范、writer、Cron、示例、指南、版本记录及 6 段动画逐字节匹配源码 |

更新 JSON 从实际 APK 生成大小与 SHA-256。源码 ZIP 包含素材生成脚本与校验清单，不包含签名私钥、原 MP4、构建产物或机器缓存。

## 预览与素材记录

- [温暖灵动 · 浅色](design-3.8.8/home-clean-light.png) / [深色](design-3.8.8/home-clean-dark.png)
- [液态玻璃 · 浅色](design-3.8.8/home-glass-light.png) / [深色](design-3.8.8/home-glass-dark.png)
- [安静耐看 · 浅色](design-3.8.8/home-paper-light.png) / [深色](design-3.8.8/home-paper-dark.png)
- [素材来源、大小与时长](portrait-assets-3.8.8.json)
- [透明边缘与首尾验证](portrait-validation-3.8.8.json)

截图使用测试卡片，聚焦首页组件，未包含全局底栏。真实手机上的帧率、功耗和厂商后台行为仍以安装体验为准；本次没有用模拟截图声称完成真机测试。

## 复现

JDK 21、Android Platform / Build Tools 36、Gradle 8.13。

```bash
./gradlew :app:testDebugUnitTest
python3 -m unittest discover -s tests -v
./gradlew :app:lintRelease :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore
```
