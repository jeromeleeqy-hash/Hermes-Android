# Hermes remaining icons — refined round 08

Round 08 closes the remaining low-frequency icon families in Hermes Mobile.
It is a visual-design package only and does not modify application code.

Included semantics:

- Agent actions: Agent, artifact, expert council, idea, summarize, plan, to-do
  and live voice waveform.
- Markdown editor: bold, italic, bullets, numbers, quote, divider, link and
  drag handle.
- Appearance: light, dark and system theme.
- Runtime state: connected, busy, error, loading and sync.

Visual and interaction decisions:

- Every outline and filled glyph is derived from one coherent Phosphor Core
  master; no manually stitched paths or intersecting chat tails are used.
- Neutral outlines are the default. Filled glyphs are reserved for selection,
  active tools and meaningful runtime state.
- All icons are checked at 24 px actual size inside a minimum 44 px touch
  target.
- Busy and loading are the only continuously rotating glyphs. Sync animates
  only after an explicit refresh action.
- Press feedback uses a subtle 0.96 scale and reduced opacity. No gray
  rectangular overlay is shown.

The package includes 24 outline SVGs and 24 filled SVGs, plus three visual
boards and their PNG previews.

Vector geometry is derived from Phosphor Icons Core 2.1.1 (MIT):
<https://github.com/phosphor-icons/core>
