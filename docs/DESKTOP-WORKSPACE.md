> 历史说明：本文描述 3.4.0。3.4.1 已移除旧助理操作页与自动资料同步；当前使用方式见 USE-AND-DEPLOY-3.4.1.md。本文保留供旧资料恢复和源码审阅。

# 可选的电脑端资料工具

`tools/assistant_workspace.py` 使用 Python 3 标准库。将它放在服务器上或让桌面 Hermes 访问源码中的工具路径即可，不需要安装依赖、不启动后台进程。

以下示例中的目录和 Profile 必须替换成 APP 当前显示的实际范围：

```bash
python3 tools/assistant_workspace.py --root /your/workspace --profile work list
python3 tools/assistant_workspace.py --root /your/workspace --profile work list --all
python3 tools/assistant_workspace.py --root /your/workspace --profile work show REVISION_UUID
```

`list` 显示当前分支；`list --all` 包含父版本。新的未确认计划存在时，需要读取父版本才能知道之前确认的安排。也可以直接阅读同目录里的全部资料文件，并按 `docs/ASSISTANT-PROTOCOL.md` 解释。

写入新成果：

```bash
python3 tools/assistant_workspace.py --root /your/workspace --profile work create \
  --kind RESULT --title '内容测试说明' --content-file /your/report.md \
  --source-session YOUR_SESSION_ID
```

修改近期重点（只在用户已经确认时使用 `--confirmed`）：

```bash
python3 tools/assistant_workspace.py --root /your/workspace --profile work create \
  --kind FOCUS --entity focus --parent OLD_REVISION_UUID \
  --title '近期重点' --content-file /your/focus.md --confirmed
```

同时存在两份修改时，先整理内容，再使用两个 `--parent` 显式合并。脚本会验证父版本属于同一个实体和 Profile，并原子创建新文件，拒绝覆盖已存在的版本文件。

跟进提醒请在 APP 中选择日期、时间并确认，或使用 Hermes 已有的原生 Cron 能力。该脚本不会创建、修改或删除 Cron，不会替你联系团队，也不会修改 SOUL.md 或 MEMORY.md。

开发测试：

```bash
python3 -m unittest discover -s tools -p 'test_*.py' -v
```
