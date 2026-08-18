# Hermes task/execution icons — refined round 05

This visual round covers Tasks and the execution center. It does not modify
application code.

Included semantics:

- Status: pending, running, scheduled and run history.
- Page actions: refresh and new task.
- Agent requests: approval and clarification.
- Task actions: run now, pause, stop, enable, edit schedule and delete.
- Results: succeeded and failed.

Interaction and visual decisions:

- Default controls use neutral outlines; filled icons are reserved for active
  or completed states.
- Blue indicates navigation/running, green indicates run/success, violet marks
  schedules, amber marks approval/pause, and red is limited to stop/fail/delete.
- The running icon is the only continuously rotating glyph.
- Segmented controls use one shared sliding indicator. Switches use a spring
  slide without a temporary rectangular ripple overlay.
- Every icon is audited at 24 px actual size.

Vector geometry is derived from Phosphor Icons Core 2.1.1 (MIT):
<https://github.com/phosphor-icons/core>
