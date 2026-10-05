# Hermes Android 3.6.6 正式版

版本名称 3.6.6，内部版本号 366。基于上传的 3.6.5 对话菜单修订版更新。

## 本次改动

1. **英文思考过程恢复正常间隔。** `reasoning.delta` 与 `thinking.delta` 过去复用了会执行 `trim()` 的元数据读取函数，导致词间空格和纯空白片段丢失。本版单独读取流式文本，保留原始空格、换行和缩进。普通回答的读取逻辑保持原样。以前已经保存为粘连文本的记录不会被猜测性重写；修复作用于新接收的流式内容。
2. **完整回答下方增加复制、朗读与停止。** 历史消息和集中阅读视图均可操作。复制仅包含回答正文，保留 Markdown 与换行；不包含单独存储的思考过程。朗读转换常见 Markdown 后分段播放完整答案，不偷偷截断。准备音频时也能停止；切换回答、退出对话、开始录音或应用退到后台时停止，取消的旧请求不会恢复播放。
3. **语音语言入口更清楚。** 语音设置顶部直接显示 Hermes 和手机识别语言。Hermes 可选 English 或自动检测，确认后通过已有网关配置接口保存到当前 Profile；成功返回后才更新已保存值。手机默认跟随手机语言，已有用户选择继续保留，并增加更多语言选项。
4. **中文文字样式与识别语言分开。** 简体、繁体只影响转写文字，不翻译英文，不再把粤语识别切换为普通话。纯英文文本直接保留。
5. **朗读语言匹配。** 手机 TTS 会根据中英文回答匹配语言，避免用普通话发音人朗读整段英文。语音设置补充英语 Edge 发音人选项。手动朗读使用“自动”引擎时先尝试手机发音人，再回退 Hermes；明确选择 Hermes 时先用服务器，失败可回退手机。

## 给英语测试者的操作说明

Open **Me → Settings → Voice**. Set **Hermes recognition language** to **English** or **Detect automatically**, confirm and wait for the saved notice. Set **Phone recognition language** to **English (US)**, **English (UK)** or **Follow phone language**. Chinese text style is only a script conversion setting; it does not limit recognition to Mandarin.

Use **Copy** or **Read aloud** below a completed answer. Tap **Stop reading** to stop, including while audio is being prepared. Recognition and synthesis still require a language supported by the configured server model or the installed phone service.

## 安装包与签名

日常使用安装 `Hermes-Android-3.6.6-release.apk`：不可调试的 Release 构建，桌面显示 **Hermes**，版本 3.6.6 / 366，要求 Android 8.0 或更新系统。

发行包沿用 `com.qingyu.hermescompanion.preview` 与 3.6.5 原签名。已核对包名、版本号和证书，支持覆盖同包名、同签名的旧版并保留本机数据，不需要卸载原版。此前独立的 Hermes Test 使用不同包名，不属于覆盖升级对象。

同时提供 `Hermes-Android-3.6.6-debug.apk` 供问题诊断：使用同一包名和签名，保留项目原有 `-preview` 调试版本后缀，与正式包不能并存。普通用户选择 Release APK 即可。

原签名证书 SHA-256：

`741c1844bb9267547e06af30d56c0da5bfcbf01491d3670496d88837a2860ad6`

源码和发布附件不包含签名私钥或本机构建缓存。安装包包含 armeabi-v7a、arm64-v8a、x86_64，继续保留 x86 兼容库。

## 构建

JDK 21、Gradle 8.13、Android SDK / Build Tools 36。工程包含 Gradle Wrapper。请使用各自保存的签名文件路径，不要将私钥提交到源码。

正式覆盖升级（必须使用原 3.6.5 签名）：

```sh
./gradlew -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore :app:testDebugUnitTest :app:assembleRelease
```

调试包（同签名）：

```sh
./gradlew -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore :app:assembleDebug
```

不传签名文件且工程内无签名时，Release 输出为未签名 APK，不能直接安装。Windows 使用 `gradlew.bat` 代替 `./gradlew`。

## 验证范围

自动化测试与正式安装包检查结果见 `VALIDATION-3.6.6.json`。自动化包含真实本地 WebSocket 帧、HTTP 配置请求、朗读取消与迟到结果、复制正文、语言选择及三套主题渲染。未连接用户实际网关录制英语音频，也未进行真机扬声器、麦克风和覆盖安装验证；这些结果不由模拟测试替代。
