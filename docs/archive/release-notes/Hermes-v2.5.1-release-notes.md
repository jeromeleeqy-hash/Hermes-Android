# Hermes Android 2.5.1

发布日期：2026-08-16

这是针对 2.5.0 Memory / Soul 入口目标错误的修订版本。

## 修正内容

- “我的”主页的 Memory 不再进入“记忆与上下文”配置，而是打开当前 Profile 的真实 `memories/MEMORY.md`。
- “我的”主页的 Soul 不再进入“对话风格”配置，而是打开当前 Profile 根目录的真实 `SOUL.md`。
- 文件使用独立只读 Markdown 页面展示，支持复制服务器路径、系统分享和保存到手机。
- 关闭文件预览后直接返回“我的”，不会误选中“空间”底栏。
- 对普通主机安装和 HERMES_HOME 直接作为托管文件根的部署方式都加入路径兼容。

## Profile 说明

- 默认 Profile：`~/.hermes/SOUL.md` 与 `~/.hermes/memories/MEMORY.md`
- 命名 Profile（例如 work）：`~/.hermes/profiles/work/SOUL.md` 与 `~/.hermes/profiles/work/memories/MEMORY.md`

如果当前 Profile 还没有生成 `MEMORY.md`，APP 会提示先让 Hermes 记录一条记忆；不会回退去打开其他 Profile 的文件。
