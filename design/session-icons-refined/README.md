# Hermes home/session icons — refined round 03

This visual round covers home filters and session-management actions. It does
not modify application code.

Included semantics:

- Filters: filter, selected, recent, project, time range, expand, collapse.
- Session actions: pin, AI rename, archive, move to project, delete.
- Archive/project actions: restore and new project.

Interaction decisions:

- Dropdown labels use the same text size as the trigger field.
- Menu text aligns left; the selected check aligns right.
- The dropdown floats 8 px below its trigger instead of touching it.
- Left swipe exposes only Archive and Delete.
- Long press retains the full session action set.
- Color is semantic: blue for navigation, amber for pin/archive, green for
  restore, violet for AI rename, and red only for deletion.

Vector geometry is derived from Phosphor Icons Core 2.1.1 (MIT):
<https://github.com/phosphor-icons/core>

