# Hermes 3.2.0-preview

## 界面

- 助理页人物从原图中作平直裁切，底边直接接到下方卡片，透明背景适配浅色和深色模式。
- 导航栏保持圆角悬浮外观；从导航上沿至屏幕底部增加页面同色遮挡层，侧边、底部间隙与系统手势区域不再露出滚动内容。
- 底部导航改为助理、回看、任务、文件、我的。任务入口直接进入执行中心，未选中为描边图标，选中为蓝紫渐变实体图标，沿用既有过渡动画。
- 移除首页底部的待确认事项/运行状态与文件按钮；待处理事项仍可在首页决策卡和任务页操作。

## 连续语音

3.1.6 将开方放大后的动画音量用于停顿判断，导致较小的持续背景声也可能超过说话阈值。现在动画与停顿检测使用不同数据：检测读取原始录音峰值的相对分贝，动画仍使用柔和的视觉幅度。

- 开始录音后约半秒估计环境底噪；无需等待再说话，开头录音一直保留。尚未确认发声时继续更新估计，以兼容一开始就说话的情况。
- 发声门限相对于环境底噪设置；开始和持续说话采用不同门限，忽略孤立的敲击声，避免短促杂音不断重置停顿。
- 确认有效说话后停顿约 1.25 秒自动提交。短暂停顿后继续说话，会重新计算停顿时间。
- 连续语音页新增“收音”选项，提供日常、嘈杂、轻声；录音中也能调整，并保存选择。语音设置页提供同一选项。
- 录音界面显示适应环境、正在聆听或停顿待发送等实际状态。

这里的分贝为数字录音相对电平（dBFS），不是经过校准的环境声压分贝。此改动改善持续底噪场景，不等同于能区分用户与周围其他人的说话声。噪声较大时可选“嘈杂”并靠近麦克风；轻声模式适合较安静环境。

以上自适应检测用于 App 内录音并发送到 Agent 的路径。选择手机系统识别时，由系统识别服务决定停顿终点；部分服务会忽略 App 请求的停顿时长。

## 安装与构建

- 应用 ID：`com.qingyu.hermescompanion.preview`
- 版本：`3.2.0-preview`，版本号：320。
- 签名沿用 3.1.6 预览版，可直接覆盖升级并保留已有本机配置。
- 仍可与早期 Hermes 测试版并存；不能覆盖签名和应用 ID 不同的旧测试版。

```bash
./gradlew testDebugUnitTest assembleDebug -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```

源代码不包含签名私钥。重新构建可覆盖升级的预览版时，使用此前单独保存的预览签名备份。

验证结果见 [验证记录](docs/VALIDATION_3.2.0.md)。手机麦克风、环境声与 Hermes 服务端尚需真机联调。

实现参考：[Android MediaRecorder 音量接口](https://developer.android.com/reference/android/media/MediaRecorder#getMaxAmplitude())、[Android 系统识别停顿参数](https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS)。
