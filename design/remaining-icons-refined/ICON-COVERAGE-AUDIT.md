# Hermes icon coverage audit

The current `HermesIconKind` surface is covered by eight refined design rounds.
Semantically equivalent icons intentionally reuse one master instead of creating
near-duplicate artwork.

| Family | Design round | Coverage |
| --- | --- | --- |
| Bottom navigation | 01 | chat, space, task, profile; outline and filled |
| Conversation | 02 | compose, attachment, microphone, send, command, voice, assistant and chat controls |
| Session list | 03 | recent, project, time, pin, rename, archive, move, delete and restore |
| Workspace | 04 | folders, file types, source chat, preview, save, share, download and document actions |
| Tasks | 05 | pending, running, scheduled, history, approval, run controls, toggle, success and error |
| Profile and settings | 06 | profile actions, gateway, appearance, notifications, models, memory, soul, guide and settings entries |
| Common controls | 07 | navigation, disclosure, search, add, selection, privacy, warning and utilities |
| Remaining utilities | 08 | Agent actions, Markdown editor, theme modes, connected/busy/error, loading and sync |

Shared masters cover intentional aliases such as `CHECK` / `CHECK_CIRCLE`,
`ERROR` / `STATUS_ERROR`, `REFRESH` / `SYNC`, `PENDING` / `STATUS_BUSY`, and
`SWITCH_ON` / `SWITCH_OFF` through the task toggle family.
