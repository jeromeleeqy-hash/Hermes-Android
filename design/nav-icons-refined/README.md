# Hermes bottom navigation icons — refined study

This design study covers only the four bottom-navigation icons:

- Chat
- Space
- Tasks
- Profile

The masters use professional compound-path geometry from Phosphor Icons Core
2.1.1 (Apache-2.0) and are optically composed at the app's actual 24 px size.
The study deliberately avoids manually stitched strokes: every outline is a
closed compound shape, so joins, tails, overlaps, and corners render cleanly at
small sizes.

Design decisions:

- Regular weight for unselected icons: approximately 1.5 px at 24 px.
- Filled state for selected icons, with a light Telegram-like selection pill.
- A shared 24 px visual box and per-icon optical centering.
- Hermes blue `#2A9FE8`, supported by cyan `#28C7BE` only where two-layer
  semantics benefit from it.
- 180–220 ms cross-fade + 0.96 → 1.0 scale is recommended for the state change.

This is a visual proposal only. No application code is changed in this round.

## Source

Vector geometry is derived from the official Phosphor Icons Core package,
version 2.1.1: <https://github.com/phosphor-icons/core>

