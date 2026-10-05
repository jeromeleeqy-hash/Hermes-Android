# Hermes Android 3.7.2 — recovery and upgrade

This is a new reconstruction, not the original 3.7.1 source archive.

## Provenance
- Base: public GitHub main/v3.6.6, commit 589ee0f8efd0e27ca621d33a80eda25e89869214.
- Original 3.7.1 source and unpublished Git objects were not recovered.
- Original APK SHA-256: 36aeca81e1b76b9b9e7081197380c0bfca1e38ed9b6cf569be3b024d220be0e9.
- Recovered original assets: hermes-today-contract.md, hermes-today-writer.py, operation-guide.json, release-history.json. Contract and release notes are extended for this release; the original writer is unchanged.
- Today data, repository, view-model integration, UI, sharing enhancements and tests were reimplemented. No claim of binary equivalence to 3.7.1 is made.

## Behavior
The overview is a read-only view of hermes-today.json in the active Profile workspace. Cards open contextual chat drafts; they do not directly change records, mark tasks complete or create reminders. The supplied contract scopes overview refresh, format repair and selected-record edits separately. Optional presentation.options, presentation.steps and presentation.metrics enhance v1 cards. Missing or malformed presentation falls back to text. Step progress is computed only from explicitly recorded steps. The legacy summary remains available in details.

Memory displays existing top-level directories and Markdown files. Counts represent visible direct children, not extracted facts. At most 48 entries are counted per refresh; others and failed counts remain unknown. No new folder taxonomy is forced on the user's workspace.

System sharing accepts rich text, ClipData, images, text files and binary attachments. Images are limited to 8 MiB; text files to 512 KiB; other files to 16 MiB; shared payloads to 24 MiB. Binary files upload only when the user sends, into the selected conversation workspace using immutable paths and explicit Profile. The app verifies bytes on read-back before submitting the prompt. A proprietary WeChat payload may still require a compatible Agent tool; transport support does not guarantee semantic decoding.

## 本次恢复说明

原来的 3.7.1 源码压缩包已经无法恢复。本包是基于公开保存的 3.6.6 源码、原 3.7.1 APK 中提取的协议/写入脚本/说明资源重新开发的 3.7.2，不是原包换名。

新的今日页默认展示四件重点事项；卡片详情保留完整摘要和来源。资料与记忆通过两列目录连接现有工作区，不创建新的数据库。旧的 hermes-today.json 可继续展示；要生成选项/步骤/数字卡片，在 App 中点击“更新”并发送附带协议的请求。

安装请使用同包名、同签名覆盖升级。不要为解决下载或覆盖安装问题卸载旧 App。源码不包含私钥；原服务器数据也不在源码包中。

远端代码同步需要 GitHub 写入授权。没有推送成功之前，不要把本地分支或下载链接视为远端备份。

## Build
Use JDK 17+ (validated with 21), Android SDK 36, build tools 36.0.0 and the included Gradle wrapper. Set local.properties sdk.dir for your machine.

    ./gradlew :app:testDebugUnitTest :app:assembleRelease -PhermesPreview=true -PhermesSigningFile=/absolute/path/to/your.keystore

The user's installed app uses applicationId com.qingyu.hermescompanion.preview. Use the same signing certificate for an in-place upgrade. Private signing material is intentionally absent from source and Git. No server credentials or user records are included.

## Limits
Self-hosted server connectivity, actual user JSON and on-device WeChat share payloads still require user-device acceptance. Automated tests exercise controlled fixtures and mock gateways. The original 3.7.1 cannot be reproduced bit-for-bit from this reconstruction.
