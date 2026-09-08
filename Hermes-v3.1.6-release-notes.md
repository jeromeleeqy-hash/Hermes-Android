# Hermes Android 3.1.6

本次集中调整首页、悬浮导航、语音交互和图标细节。

## 界面

- 助理首页去掉输入框和重复的“接着聊聊”链接，保留新话题与继续会话入口。
- 添加到对话的三个提示词片段去掉开头的“请”。
- 首页、回看、文件、执行中心和个人页面的内容延伸到悬浮导航后方；列表末尾保留滚动空间。
- 移除固定区域上方分隔线。导航栏外围透明，白色/深色浮动面板带轻微阴影。
- 全局移除灰色点击波纹；普通可点击元素保留轻微缩放反馈。分段切换使用淡蓝选中样式。
- 统一流式回复中的思考指示，不再同时出现两条“正在思考”。
- 桌面图标扩大女孩头像，缩小留白，增加柔和阴影；首页人物通过原生轮廓裁切显示，深色模式不再有白色矩形底。

## 连续语音

- 检测有效讲话后，停顿约 1.25 秒自动提交；短暂停顿和单次噪声不会触发提交。仍可点按提前结束。
- 默认开启连续聆听，新版保留已保存的连续对话开关选择；开启后朗读完成自动进入下一轮识别。
- “快速回答”在支持的 Hermes Agent 上临时关闭本轮会话思考，保留原模型；结束后恢复原设置。普通文字对话和其他档案不修改全局设置。
- 恢复信息先保存在手机，再请求临时设置；断线或停止后，下次发送前先恢复。旧网关不支持时显示说明并沿用原设置。
- 中文自动模式优先使用匹配的中文手机发音人；无法使用时检查 Agent 发音人是否支持中文，避免英文引擎只读出数字和英文。
- 朗读实际回答，清理 Markdown 标记、链接地址和代码块，长内容分段处理，不再截断到前 8000 字。
- 播放失败和中文语音包缺失会显示原因；取消播放后不继续自动聆听。
- 退出语音或切换会话后忽略迟到结果；语音发送保留原本未发送的文字和附件。

## 构建与安装

本次预览包使用独立应用 ID `com.qingyu.hermescompanion.preview`，桌面名称为“Hermes 预览”，可与原 App 并存。预览版首次打开需要重新连接网关，原 App 的本机数据不受影响。

当前工程没有上一版测试包的签名私钥。直接覆盖原测试版需要恢复原 `signing/hermes-debug.keystore`，随后按默认构建方式打包。新生成的预览签名备份单独保存，不能提交到公开仓库。

```sh
gradle :app:assembleDebug -PhermesPreview=true -PhermesSigningFile=/absolute/path/hermes-preview.keystore
```

默认不传 `hermesPreview` 时仍使用原来的 `.debug` 应用 ID。162 项自动化测试全部通过；完整记录见 [验证报告](docs/VALIDATION_3.1.6.md)。

## 验证边界

自动化验证覆盖静音端点、中文文本清洗、长文本分段、会话级思考恢复、语音结果隔离、草稿保留和页面布局。手机麦克风、OEM 中文 TTS、真实 Hermes 服务器与 DeepSeek 模型的端到端效果仍需连接实际设备验证。

协议实现参照 [Hermes Agent 会话设置](https://github.com/NousResearch/hermes-agent/blob/main/tui_gateway/methods_config_set.py)、[音频接口](https://github.com/NousResearch/hermes-agent/blob/main/hermes_cli/web_routers/audio.py) 和 [DeepSeek 思考模式](https://api-docs.deepseek.com/guides/thinking_mode/)。
