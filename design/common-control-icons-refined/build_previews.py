#!/usr/bin/env python3
"""Build Hermes global navigation, selection and feedback icon boards."""

from __future__ import annotations

import html
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
PHOSPHOR = ROOT / ".tooling" / "phosphor-core" / "assets"
OUT = Path(__file__).resolve().parent
ICONS = OUT / "icons"

ACCENT = "#2A9FE8"
INK = "#1D2637"
MUTED = "#667287"
SOFT = "#8A95A7"
BG = "#F3F5F9"
LINE = "#E6EAF0"
BLUE_BG = "#E5F3FD"
GREEN = "#24B98A"
GREEN_BG = "#E2F7F0"
AMBER = "#E79A20"
AMBER_BG = "#FFF2D7"
RED = "#EF5362"
RED_BG = "#FFE8EC"


ICON_SPECS = [
    ("back", "Back", "arrow-left", "arrow-left-fill", "Navigation", MUTED, "#EEF1F5"),
    ("next", "Next level", "caret-right", "caret-right-fill", "Navigation", MUTED, "#EEF1F5"),
    ("expand", "Expand", "caret-down", "caret-down-fill", "Navigation", MUTED, "#EEF1F5"),
    ("collapse", "Collapse", "caret-up", "caret-up-fill", "Navigation", MUTED, "#EEF1F5"),
    ("close", "Close", "x", "x-fill", "Navigation", MUTED, "#EEF1F5"),
    ("more", "More", "dots-three", "dots-three-fill", "Navigation", MUTED, "#EEF1F5"),
    ("search", "Search", "magnifying-glass", "magnifying-glass-fill", "Action", ACCENT, BLUE_BG),
    ("add", "Add", "plus", "plus-fill", "Action", ACCENT, BLUE_BG),
    ("check", "Confirm", "check", "check-fill", "Selection", ACCENT, BLUE_BG),
    ("checkbox-empty", "Unchecked", "square", "square-fill", "Selection", MUTED, "#EEF1F5"),
    ("checkbox-checked", "Checked", "check-square", "check-square-fill", "Selection", ACCENT, BLUE_BG),
    ("radio-selected", "Selected", "radio-button", "radio-button-fill", "Selection", ACCENT, BLUE_BG),
    ("show", "Show", "eye", "eye-fill", "Privacy", MUTED, "#EEF1F5"),
    ("hide", "Hide", "eye-slash", "eye-slash-fill", "Privacy", MUTED, "#EEF1F5"),
    ("lock", "Locked", "lock-key", "lock-key-fill", "Privacy", AMBER, AMBER_BG),
    ("warning", "Warning", "warning", "warning-fill", "Feedback", AMBER, AMBER_BG),
    ("copy", "Copy", "copy", "copy-fill", "Utility", ACCENT, BLUE_BG),
    ("open-external", "Open outside", "arrow-square-out", "arrow-square-out-fill", "Utility", ACCENT, BLUE_BG),
]


def source_path(master: str) -> Path:
    return PHOSPHOR / ("fill" if master.endswith("-fill") else "regular") / f"{master}.svg"


def icon_inner(master: str) -> str:
    root = ET.parse(source_path(master)).getroot()
    return "".join(ET.tostring(child, encoding="unicode") for child in root)


def symbols() -> str:
    names: list[str] = []
    seen: set[str] = set()
    for _, _, outline, filled, _, _, _ in ICON_SPECS:
        for name in (outline, filled):
            if name not in seen:
                names.append(name)
                seen.add(name)
    for name in ("check-circle-fill", "x-circle-fill", "file-text", "image"):
        if name not in seen:
            names.append(name)
            seen.add(name)
    return "".join(
        f'<symbol id="i-{name}" viewBox="0 0 256 256" fill="currentColor">{icon_inner(name)}</symbol>'
        for name in names
    )


def svg_open(width: int, height: int, title: str) -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <defs>
    <filter id="shadow" x="-30%" y="-30%" width="160%" height="190%"><feDropShadow dx="0" dy="14" stdDeviation="24" flood-color="#273248" flood-opacity="0.11"/></filter>
    <filter id="softShadow" x="-40%" y="-40%" width="180%" height="220%"><feDropShadow dx="0" dy="7" stdDeviation="13" flood-color="#273248" flood-opacity="0.11"/></filter>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def master_sheet() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes common control icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">HERMES MOBILE · ROUND 07</text>')
    chunks.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Global controls &amp; feedback icons</text>')
    chunks.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">One optical weight · closed geometry · no crossing strokes · no rectangular press overlay</text>')

    card_w, card_h = 150, 348
    for index, (key, label, outline, filled, group, color, tint) in enumerate(ICON_SPECS):
        row, col = divmod(index, 9)
        x = 82 + col * 160
        y = 226 + row * 372
        chunks.append(f'<g transform="translate({x} {y})"><rect width="{card_w}" height="{card_h}" rx="28" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="16" y="32" font-family="Inter,Arial,sans-serif" font-size="10.5" font-weight="700" letter-spacing="1" fill="#8B96A9">{group.upper()}</text>')
        chunks.append('<rect x="16" y="50" width="118" height="142" rx="27" fill="#F6F8FC"/>')
        chunks.append(use(outline, 42, 84, 66, MUTED))
        chunks.append(f'<circle cx="112" cy="168" r="23" fill="{tint}"/>')
        chunks.append(use(filled, 99, 155, 26, color))
        chunks.append(f'<text x="16" y="230" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="16" y="260" font-family="Inter,Arial,sans-serif" font-size="13" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 16, 289, 24, MUTED))
        chunks.append(f'<text x="52" y="307" font-family="Inter,Arial,sans-serif" font-size="12.5" fill="{SOFT}">actual size</text></g>')

    chunks.append('<g transform="translate(82 1000)"><rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    notes = [
        ("Outline", "default control"),
        ("Fill", "selected / active"),
        ("Scale", "0.96 on press"),
        ("Ripple", "bounded shape disabled"),
    ]
    for i, (label, note) in enumerate(notes):
        x = 36 + i * 350
        chunks.append(f'<circle cx="{x}" cy="36" r="7" fill="{ACCENT if i < 2 else GREEN}"/>')
        chunks.append(f'<text x="{x+20}" y="43" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">{label}</text>')
        chunks.append(f'<text x="{x+98}" y="43" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">{note}</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def navigation_preview() -> str:
    width, height = 1600, 1140
    c = [svg_open(width, height, "Hermes navigation and selection control preview")]
    c.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    c.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · NAVIGATION &amp; SELECTION</text>')
    c.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Quiet controls, unmistakable state</text>')
    c.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">Text aligns left, checks align right, and menus float 8 px away from their trigger.</text>')

    c.append('<g transform="translate(82 224)"><rect width="956" height="838" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    # App bar
    c.append(use("arrow-left", 38, 36, 36, MUTED))
    c.append(f'<text x="96" y="66" font-family="Inter,Arial,sans-serif" font-size="28" font-weight="700" fill="{INK}">Model settings</text>')
    c.append(use("magnifying-glass", 818, 38, 32, MUTED))
    c.append(use("dots-three", 874, 38, 32, MUTED))
    c.append(f'<line x1="32" y1="100" x2="924" y2="100" stroke="{LINE}" stroke-width="2"/>')

    # Dropdown trigger and popup with deliberate gap.
    c.append(f'<text x="42" y="148" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" letter-spacing="1.4" fill="{SOFT}">SORT ORDER</text>')
    c.append('<rect x="40" y="172" width="328" height="64" rx="22" fill="#F4F6F9"/>')
    c.append(f'<text x="64" y="211" font-family="Inter,Arial,sans-serif" font-size="17" fill="{INK}">Most recent</text>')
    c.append(use("caret-down", 322, 191, 24, MUTED))
    c.append('<rect x="40" y="244" width="328" height="202" rx="26" fill="#FFFFFF" filter="url(#softShadow)"/>')
    menu = [("Most recent", True), ("By project", False), ("Oldest first", False)]
    for i, (label, selected) in enumerate(menu):
        y = 274 + i * 58
        c.append(f'<text x="68" y="{y+21}" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="{700 if selected else 500}" fill="{INK}">{label}</text>')
        if selected:
            c.append(use("check", 316, y + 1, 24, ACCENT))

    # Password field
    c.append(f'<text x="420" y="148" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" letter-spacing="1.4" fill="{SOFT}">SECURE FIELD</text>')
    c.append(f'<rect x="418" y="172" width="496" height="76" rx="24" fill="#FFFFFF" stroke="{LINE}" stroke-width="2"/>')
    c.append(use("lock-key", 442, 195, 30, AMBER))
    c.append(f'<text x="492" y="219" font-family="Inter,Arial,sans-serif" font-size="18" fill="{INK}">••••••••••</text>')
    c.append(use("eye-slash", 856, 195, 30, MUTED))

    # Selection group
    c.append(f'<text x="420" y="304" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" letter-spacing="1.4" fill="{SOFT}">SELECTION CONTROLS</text>')
    rows = [
        ("square", MUTED, "Use system default", False),
        ("check-square-fill", ACCENT, "Enable notifications", True),
        ("radio-button-fill", ACCENT, "Simplified Chinese", True),
    ]
    for i, (icon, color, label, selected) in enumerate(rows):
        y = 334 + i * 82
        c.append(f'<rect x="418" y="{y}" width="496" height="68" rx="22" fill="{BLUE_BG if selected else "#F7F9FC"}"/>')
        c.append(use(icon, 442, y + 20, 28, color))
        c.append(f'<text x="490" y="{y+43}" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="{650 if selected else 500}" fill="{INK}">{label}</text>')

    # Hierarchy row
    c.append(f'<text x="42" y="516" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" letter-spacing="1.4" fill="{SOFT}">HIERARCHY</text>')
    hierarchy = [("Remote gateway", "Connected", "caret-right"), ("Advanced options", "6 items", "caret-down")]
    for i, (title, value, icon) in enumerate(hierarchy):
        y = 542 + i * 88
        c.append(f'<rect x="40" y="{y}" width="874" height="72" rx="22" fill="#F8FAFC"/>')
        c.append(f'<text x="66" y="{y+44}" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="650" fill="{INK}">{title}</text>')
        c.append(f'<text x="824" y="{y+44}" text-anchor="end" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{value}</text>')
        c.append(use(icon, 856, y + 23, 26, MUTED))
    c.append('</g>')

    # Behavior rail
    c.append('<g transform="translate(1080 224)"><rect width="438" height="838" rx="42" fill="#FFFFFF" filter="url(#shadow)"/>')
    c.append(f'<text x="34" y="58" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{INK}">Control behavior</text>')
    notes = [
        ("01", "No gray block", "Press feedback uses 0.96 scale", "and 76% icon opacity."),
        ("02", "8 px menu gap", "The popup never touches the", "filter or dropdown trigger."),
        ("03", "Check on the right", "Menu copy remains left aligned", "for faster vertical scanning."),
        ("04", "One optical weight", "Navigation glyphs share the", "same apparent stroke density."),
    ]
    for i, (num, title, line1, line2) in enumerate(notes):
        y = 98 + i * 154
        c.append(f'<circle cx="56" cy="{y+8}" r="22" fill="{BLUE_BG}"/><text x="56" y="{y+14}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="12" font-weight="700" fill="{ACCENT}">{num}</text>')
        c.append(f'<text x="92" y="{y+3}" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{title}</text>')
        c.append(f'<text x="92" y="{y+31}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{line1}</text>')
        c.append(f'<text x="92" y="{y+54}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{line2}</text>')
    c.append(f'<rect x="34" y="718" width="370" height="82" rx="26" fill="{GREEN_BG}"/>')
    c.append(use("check-circle-fill", 56, 743, 32, GREEN))
    c.append(f'<text x="102" y="753" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">Touch target stays 44 px</text>')
    c.append(f'<text x="102" y="779" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">The visible glyph remains 20–24 px.</text></g></svg>')
    return "".join(c)


def dialog_preview() -> str:
    width, height = 1600, 1140
    c = [svg_open(width, height, "Hermes dialog and utility action preview")]
    c.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    c.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · DIALOGS &amp; UTILITIES</text>')
    c.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Feedback without visual noise</text>')
    c.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">Status is compact, warnings are explicit, and utility actions stay neutral until invoked.</text>')

    c.append('<g transform="translate(82 224)"><rect width="1436" height="838" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    # Completed line: explicitly no green rectangle.
    c.append(f'<text x="42" y="66" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" letter-spacing="1.6" fill="{SOFT}">COMPLETION STATUS</text>')
    c.append(use("check-circle-fill", 42, 94, 28, GREEN))
    c.append(f'<text x="84" y="116" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">Round completed</text>')
    c.append(f'<text x="84" y="144" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">One status line only — the Agent response is not repeated.</text>')
    c.append(f'<line x1="42" y1="174" x2="1394" y2="174" stroke="{LINE}" stroke-width="2"/>')

    # Warning dialog
    c.append('<rect x="42" y="212" width="726" height="410" rx="34" fill="#FFFFFF" stroke="#E2E7EE" stroke-width="2" filter="url(#softShadow)"/>')
    c.append(f'<rect x="76" y="246" width="66" height="66" rx="21" fill="{AMBER_BG}"/>')
    c.append(use("warning-fill", 93, 263, 32, AMBER))
    c.append(f'<text x="168" y="275" font-family="Inter,Arial,sans-serif" font-size="23" font-weight="700" fill="{INK}">Allow insecure HTTP?</text>')
    c.append(use("x", 708, 252, 28, MUTED))
    c.append(f'<text x="76" y="356" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Traffic can be observed or modified on public networks.</text>')
    c.append(f'<text x="76" y="388" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Use HTTPS or a trusted VPN whenever possible.</text>')
    c.append(f'<rect x="76" y="500" width="302" height="72" rx="23" fill="#F4F6F9"/><text x="227" y="544" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{MUTED}">Cancel</text>')
    c.append(f'<rect x="394" y="500" width="340" height="72" rx="23" fill="{ACCENT}"/><text x="564" y="544" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="#FFFFFF">Allow once</text>')

    # Utility card
    c.append(f'<text x="820" y="242" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" letter-spacing="1.6" fill="{SOFT}">DOCUMENT UTILITIES</text>')
    c.append('<rect x="814" y="270" width="580" height="168" rx="30" fill="#F8FAFC"/>')
    c.append(f'<rect x="842" y="300" width="72" height="72" rx="22" fill="{BLUE_BG}"/>')
    c.append(use("file-text", 860, 318, 36, ACCENT))
    c.append(f'<text x="940" y="327" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" fill="{INK}">2026-08-17_report.md</text>')
    c.append(f'<text x="940" y="357" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">Markdown · 18 KB</text>')
    c.append(use("copy", 1248, 314, 32, MUTED))
    c.append(use("arrow-square-out", 1324, 314, 32, MUTED))

    # Preview toolbar
    c.append(f'<text x="820" y="492" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" letter-spacing="1.6" fill="{SOFT}">PREVIEW TOOLBAR</text>')
    c.append('<rect x="814" y="520" width="580" height="102" rx="30" fill="#1D2637"/>')
    c.append(use("x", 844, 551, 32, "#FFFFFF"))
    c.append(f'<text x="900" y="576" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="650" fill="#FFFFFF">Image preview</text>')
    c.append(use("copy", 1270, 551, 32, "#FFFFFF", .88))
    c.append(use("dots-three", 1332, 551, 32, "#FFFFFF", .88))

    # State samples
    c.append(f'<line x1="42" y1="670" x2="1394" y2="670" stroke="{LINE}" stroke-width="2"/>')
    samples = [
        ("warning-fill", AMBER, AMBER_BG, "Warning", "Requires attention"),
        ("x-circle-fill", RED, RED_BG, "Error", "Action failed"),
        ("check-circle-fill", GREEN, GREEN_BG, "Success", "Saved locally"),
    ]
    for i, (icon, color, tint, title, body) in enumerate(samples):
        x = 42 + i * 452
        c.append(f'<rect x="{x}" y="706" width="420" height="94" rx="27" fill="{tint}"/>')
        c.append(use(icon, x + 24, 735, 34, color))
        c.append(f'<text x="{x+80}" y="742" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{title}</text>')
        c.append(f'<text x="{x+80}" y="772" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{body}</text>')
    c.append('</g></svg>')
    return "".join(c)


def export_icons() -> None:
    (ICONS / "outline").mkdir(parents=True, exist_ok=True)
    (ICONS / "filled").mkdir(parents=True, exist_ok=True)
    for key, _, outline, filled, _, _, _ in ICON_SPECS:
        shutil.copy2(source_path(outline), ICONS / "outline" / f"{key}.svg")
        shutil.copy2(source_path(filled), ICONS / "filled" / f"{key}.svg")


def main() -> None:
    export_icons()
    outputs = {
        "01-common-control-icon-master.svg": master_sheet(),
        "02-navigation-selection-preview.svg": navigation_preview(),
        "03-dialog-utility-preview.svg": dialog_preview(),
    }
    for name, data in outputs.items():
        (OUT / name).write_text(data, encoding="utf-8")


if __name__ == "__main__":
    main()
