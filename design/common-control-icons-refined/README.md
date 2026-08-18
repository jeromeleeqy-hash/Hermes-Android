# Hermes common controls — refined round 07

This visual round completes the high-frequency global controls used across
Hermes Mobile. It does not modify application code.

Included semantics:

- Navigation: back, next level, expand, collapse, close and more.
- Actions: search and add.
- Selection: confirm, unchecked, checked and radio-selected.
- Privacy: show, hide and locked.
- Feedback and utilities: warning, copy and open outside.

Interaction and visual decisions:

- Popup menus float 8 px away from their trigger. Menu labels align left and
  the selected check aligns right.
- Buttons use a subtle 0.96 scale plus reduced icon opacity while pressed. No
  gray rectangular press overlay or delayed ripple block is shown.
- Visible glyphs are 20–24 px inside a minimum 44 px touch target.
- Completion feedback uses one compact status line; it never repeats the full
  Agent response.
- Default controls are neutral outlines. Filled glyphs indicate selected,
  completed, warning or error states.
- Every icon is audited at 24 px actual size.

The package includes 18 outline SVGs and 18 filled SVGs.

Vector geometry is derived from Phosphor Icons Core 2.1.1 (MIT):
<https://github.com/phosphor-icons/core>
