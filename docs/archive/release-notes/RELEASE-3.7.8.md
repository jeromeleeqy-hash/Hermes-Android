# 3.7.8：修复审批接入与未完成卡片续做

用户反馈：卡片能够提交，Hermes 表示原 Markdown 已更新，但执行 JSON 写回命令时审批被撤回，首页继续显示旧卡片。

## 原因与修改

新版 Hermes 网关将阻塞式输入从 `approval.request` / `clarify.request` 通知迁移为 JSON-RPC 服务器请求。客户端需要在每次连接上发送 `client.capabilities {server_requests:true}`，显示请求，再使用原请求编号回答。3.7.7 只处理旧格式。官方后端会主动撤回无法由客户端回答的审批；这与截图中的提示吻合。本次没有连接用户服务器，无法核对那次执行的原始事件日志。

- 每次实时连接声明请求处理能力；同时兼容旧网关的通知与 respond RPC。
- 新版审批回复保留 JSON-RPC 外层 ID，不误用命令队列的 request_id。尊重服务器给出的允许选项，用户明确选择并确认后才发送答案。
- 支持多题澄清，每题独立提交；重连时恢复未回答问题，跳过已锁定答案。
- 不支持的桌面专有请求明确返回不支持，不假装执行。
- 审批在卡片详情等页面上方直接展示。关闭弹层只延后处理，不自动批准；仍可从首页或任务页打开。
- 将等待输入、请求结束和结果尚未确认分别显示；卡片成功仍只由匹配的 JSON 回执确认。
- “继续未完成部分”先读最新 JSON 与回执、确认原会话空闲，再沿用原会话、输入及 operation_id 续做。已完成且有回执时只更新显示。服务端仍在运行或不能确认空闲时不追加提示词。
- 持久操作日志仍按服务器、账户、Profile 和工作区隔离。跨 Markdown/JSON 仍不是原子事务；继续依赖 Agent 核对原记录，不能承诺任意业务副作用自动回滚。

## 安装与恢复

覆盖安装同签名 APK，不必重建 Cron、清空资料或重新整理所有卡片。打开受阻卡片，点击“继续未完成部分”。若收到审批，审阅具体命令后选择适当选项并确认。等待服务器写回；没有收到回执时仍保留待核对状态。

数据格式继续使用 presentation_version 3，并兼容既有 3.7.7 早晚配置。此次操作自动附上更新后的恢复规则，无需手工替换服务端规范。

## 参考与范围

- [官方 server_requests 协议](https://github.com/NousResearch/hermes-agent/blob/main/tui_gateway/server_requests.py)
- [官方审批撤回实现](https://github.com/NousResearch/hermes-agent/blob/main/tui_gateway/server.py)
- [官方请求字段](https://github.com/NousResearch/hermes-agent/blob/main/tui_gateway/contracts/server_requests.py)

真实 WebSocket 测试使用本地模拟网关；没有实际运行用户服务器的工具、审批、Cron 或文件写入。此次解决审批协议兼容问题，不将此前所有网络波动一并判定为已解决。构建与测试结果见 VALIDATION-3.7.8.md。
