#!/usr/bin/env python3
"""Build refined home/session-management icon presentation boards."""

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
ACCENT_DARK = "#188BCF"
CYAN = "#28C7BE"
INK = "#1D2637"
MUTED = "#667287"
SOFT = "#8A95A7"
BG = "#F3F5F9"
PILL = "#E5F3FD"
GREEN = "#25BF91"
AMBER = "#F4A522"
AMBER_BG = "#FFF3D9"
RED = "#F05261"
RED_BG = "#FFE8EC"
VIOLET = "#7C63E8"


ICON_SPECS = [
    ("filter", "Filter", "funnel-simple", "funnel-simple-fill", "Filters"),
    ("selected", "Selected", "check", None, "Filters"),
    ("recent", "Recent", "clock-counter-clockwise", "clock-counter-clockwise-fill", "Filters"),
    ("project", "Project", "folder", "folder-fill", "Filters"),
    ("time", "Time range", "clock", "clock-fill", "Filters"),
    ("expand", "Expand", "caret-down", None, "Filters"),
    ("collapse", "Collapse", "caret-right", None, "Filters"),
    ("pin", "Pin", "push-pin-simple", "push-pin-simple-fill", "Session"),
    ("rename", "AI rename", "magic-wand", "magic-wand-fill", "Session"),
    ("archive", "Archive", "archive", "archive-fill", "Session"),
    ("move", "Move project", "folders", "folders-fill", "Session"),
    ("delete", "Delete", "trash-simple", "trash-simple-fill", "Session"),
    ("restore", "Restore", "arrow-counter-clockwise", "arrow-counter-clockwise-fill", "Archive"),
    ("new-project", "New project", "folder-plus", "folder-plus-fill", "Project"),
]


def source_path(master: str) -> Path:
    style = "fill" if master.endswith("-fill") else "regular"
    return PHOSPHOR / style / f"{master}.svg"


def icon_inner(master: str) -> str:
    root = ET.parse(source_path(master)).getroot()
    return "".join(ET.tostring(child, encoding="unicode") for child in root)


def symbols() -> str:
    masters: list[str] = []
    seen: set[str] = set()
    for _, _, outline, filled, _ in ICON_SPECS:
        for master in (outline, filled):
            if master and master not in seen:
                seen.add(master)
                masters.append(
                    f'<symbol id="i-{master}" viewBox="0 0 256 256" fill="currentColor">{icon_inner(master)}</symbol>'
                )
    return "".join(masters)


def svg_open(width: int, height: int, title: str) -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <defs>
    <filter id="shadow" x="-30%" y="-30%" width="160%" height="190%"><feDropShadow dx="0" dy="14" stdDeviation="24" flood-color="#273248" flood-opacity="0.11"/></filter>
    <filter id="softShadow" x="-40%" y="-40%" width="180%" height="220%"><feDropShadow dx="0" dy="7" stdDeviation="13" flood-color="#273248" flood-opacity="0.12"/></filter>
    <clipPath id="listClip" clipPathUnits="userSpaceOnUse"><rect width="876" height="800" rx="48"/></clipPath>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def master_sheet() -> str:
    width, height = 1600, 1130
    chunks = [svg_open(width, height, "Hermes home and session icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">HERMES MOBILE · ROUND 03</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Home &amp; session management icons</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Closed geometry · calm defaults · color reserved for state and consequence</text>')

    card_w, card_h = 196, 346
    gap_x, gap_y = 22, 26
    start_x, start_y = 82, 240
    state_colors = {
        "pin": AMBER,
        "rename": VIOLET,
        "archive": AMBER,
        "delete": RED,
        "restore": GREEN,
        "new-project": ACCENT,
    }
    for index, (key, label, outline, filled, group) in enumerate(ICON_SPECS):
        row, col = divmod(index, 7)
        x = start_x + col * (card_w + gap_x)
        y = start_y + row * (card_h + gap_y)
        active_color = state_colors.get(key, ACCENT)
        chunks.append(f'<g transform="translate({x} {y})">')
        chunks.append(f'<rect width="{card_w}" height="{card_h}" rx="32" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="22" y="36" font-family="Inter,Arial,sans-serif" font-size="13" font-weight="700" letter-spacing="1.5" fill="#8B96A9">{html.escape(group.upper())}</text>')
        chunks.append('<rect x="22" y="56" width="152" height="152" rx="30" fill="#F6F8FC"/>')
        chunks.append(use(outline, 62, 96, 72, MUTED))
        if filled:
            chunks.append(f'<circle cx="151" cy="185" r="26" fill="{PILL}"/>')
            chunks.append(use(filled, 136, 170, 30, active_color))
        chunks.append(f'<text x="22" y="248" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="22" y="278" font-family="Inter,Arial,sans-serif" font-size="15" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 22, 298, 24, MUTED))
        chunks.append(f'<text x="58" y="317" font-family="Inter,Arial,sans-serif" font-size="14" fill="{SOFT}">actual size</text>')
        chunks.append('</g>')

    chunks.append('<g transform="translate(82 1001)">')
    chunks.append('<rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    chunks.append(f'<circle cx="32" cy="36" r="6" fill="{CYAN}"/><text x="52" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">16 px filter-safe</text>')
    chunks.append(f'<circle cx="340" cy="36" r="6" fill="{CYAN}"/><text x="360" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">24 px action-safe</text>')
    chunks.append(f'<circle cx="642" cy="36" r="6" fill="{CYAN}"/><text x="662" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Destructive red only</text>')
    chunks.append(f'<circle cx="930" cy="36" r="6" fill="{CYAN}"/><text x="950" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Outline + active state</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def filter_preview() -> str:
    width, height = 1600, 1080
    chunks = [svg_open(width, height, "Hermes refined filter menu preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · FILTERS</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Menus align with their trigger — without sticking to it</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Same text scale in trigger and menu; label stays left, selected check stays right.</text>')

    # Main interface card
    chunks.append('<g transform="translate(82 238)"><rect width="1436" height="724" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<circle cx="78" cy="74" r="38" fill="{PILL}"/><text x="78" y="84" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="23" font-weight="700" fill="{ACCENT}">H</text>')
    chunks.append(f'<text x="136" y="68" font-family="Inter,Arial,sans-serif" font-size="29" font-weight="700" fill="{INK}">Hermes</text>')
    chunks.append(f'<circle cx="146" cy="101" r="5" fill="{GREEN}"/><text x="163" y="108" font-family="Inter,Arial,sans-serif" font-size="18" fill="{MUTED}">Profile · work</text>')

    # Filter triggers
    trigger_y = 142
    trigger_specs = [("Recent", 42, 286), ("All projects", 344, 468), ("All time", 828, 340)]
    for label, x, w in trigger_specs:
        chunks.append(f'<rect x="{x}" y="{trigger_y}" width="{w}" height="68" rx="22" fill="#F4F6F9"/>')
        chunks.append(f'<text x="{x+24}" y="{trigger_y+43}" font-family="Inter,Arial,sans-serif" font-size="18" fill="{MUTED}">{html.escape(label)}</text>')
        chunks.append(use("caret-down", x + w - 42, trigger_y + 24, 20, MUTED))

    # 8 px air gap, project menu
    menu_x, menu_y, menu_w = 344, 218, 468
    chunks.append(f'<rect x="{menu_x}" y="{menu_y}" width="{menu_w}" height="250" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    items = [("All projects", True), ("workspace", False), ("Looki daily", False)]
    for i, (label, selected) in enumerate(items):
        iy = menu_y + i * 78
        if selected:
            chunks.append(f'<rect x="{menu_x+10}" y="{iy+8}" width="{menu_w-20}" height="62" rx="18" fill="#F5FAFE"/>')
        chunks.append(f'<text x="{menu_x+28}" y="{iy+48}" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="{700 if selected else 500}" fill="{INK}">{html.escape(label)}</text>')
        if selected:
            chunks.append(use("check", menu_x + menu_w - 55, iy + 26, 24, ACCENT))

    # Recent menu compact sample
    compact_x, compact_y, compact_w = 42, 492, 286
    chunks.append(f'<rect x="{compact_x}" y="{compact_y}" width="{compact_w}" height="178" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    for i, (label, icon, selected) in enumerate([("Recent", "clock-counter-clockwise", True), ("Projects", "folder", False)]):
        iy = compact_y + i * 78
        chunks.append(use(icon, compact_x + 24, iy + 26, 24, ACCENT if selected else MUTED))
        chunks.append(f'<text x="{compact_x+64}" y="{iy+49}" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="{700 if selected else 500}" fill="{INK}">{label}</text>')
        if selected:
            chunks.append(use("check", compact_x + compact_w - 50, iy + 27, 22, ACCENT))

    # explanatory rail
    chunks.append(f'<line x1="902" y1="266" x2="902" y2="650" stroke="#E8EDF3" stroke-width="2"/>')
    chunks.append(f'<text x="950" y="308" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{INK}">Spacing &amp; type audit</text>')
    audit = [
        ("8 px", "trigger-to-menu gap"),
        ("18 px", "trigger and menu labels"),
        ("24 px", "left / right menu inset"),
        ("62 px", "compact selected row"),
    ]
    for i, (value, note) in enumerate(audit):
        yy = 364 + i * 70
        chunks.append(f'<text x="950" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="24" font-weight="700" fill="{ACCENT_DARK}">{value}</text>')
        chunks.append(f'<text x="1058" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">{note}</text>')

    chunks.append('</g>')
    chunks.append(f'<g transform="translate(82 1008)">{use("caret-down", 0, 0, 20, MUTED)}<text x="34" y="18" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">16–20 px filter glyph</text>{use("check", 298, -2, 24, ACCENT)}<text x="336" y="18" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">24 px selected state</text></g>')
    chunks.append('</svg>')
    return "".join(chunks)


def session_actions_preview() -> str:
    width, height = 1600, 1140
    chunks = [svg_open(width, height, "Hermes session gesture and action preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · SESSION ACTIONS</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">One gesture for speed, one sheet for full control</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Swipe exposes Archive and Delete; long press keeps the complete session action set.</text>')

    # Left panel: list and swipe
    chunks.append('<g transform="translate(82 236)"><rect width="876" height="800" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append('<g clip-path="url(#listClip)">')
    chunks.append(f'<text x="38" y="58" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" letter-spacing="2" fill="{SOFT}">CONVERSATION LIST</text>')
    rows = [
        ("AI sales pilot", "Confirm the delivery boundary and acceptance criteria", "A", "#DFF6F2", "#129E8F"),
        ("Weekly report progress", "Five updates and three follow-ups are ready", "W", "#FFF0D5", "#D48713"),
        ("CRM dashboard redesign", "Continue the conversion metric review", "C", "#EEE9FF", "#6B57D9"),
    ]
    for i, (title, preview, initial, avatar_bg, avatar_fg) in enumerate(rows):
        y = 92 + i * 170
        if i == 1:
            # Actions are behind the swiped row.
            chunks.append(f'<rect x="646" y="{y}" width="92" height="136" rx="24" fill="{AMBER}"/>')
            chunks.append(use("archive-fill", 674, y + 34, 36, "#FFFFFF"))
            chunks.append(f'<text x="692" y="{y+102}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" fill="#FFFFFF">Archive</text>')
            chunks.append(f'<rect x="738" y="{y}" width="100" height="136" rx="24" fill="{RED}"/>')
            chunks.append(use("trash-simple-fill", 769, y + 34, 36, "#FFFFFF"))
            chunks.append(f'<text x="788" y="{y+102}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" fill="#FFFFFF">Delete</text>')
        offset = -192 if i == 1 else 0
        content_shift = 172 if i == 1 else 0
        avatar_x = 48 + content_shift
        avatar_cx = 83 + content_shift
        text_x = 142 + content_shift
        line_end = 816
        chunks.append(f'<g transform="translate({offset} 0)"><rect x="28" y="{y}" width="810" height="136" rx="24" fill="#FFFFFF"/>')
        chunks.append(f'<rect x="{avatar_x}" y="{y+24}" width="70" height="70" rx="22" fill="{avatar_bg}"/><text x="{avatar_cx}" y="{y+69}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="22" font-weight="700" fill="{avatar_fg}">{initial}</text>')
        chunks.append(f'<text x="{text_x}" y="{y+46}" font-family="Inter,Arial,sans-serif" font-size="22" font-weight="700" fill="{INK}">{html.escape(title)}</text>')
        chunks.append(f'<text x="{text_x}" y="{y+82}" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">{html.escape(preview)}</text>')
        if i == 0:
            chunks.append(use("push-pin-simple-fill", 782, y + 27, 24, AMBER))
        chunks.append(f'<line x1="{text_x}" y1="{y+122}" x2="{line_end}" y2="{y+122}" stroke="#EDF0F4" stroke-width="2"/></g>')
    # archived state row
    y = 616
    chunks.append(f'<rect x="28" y="{y}" width="810" height="124" rx="24" fill="#F8FAFC"/>')
    chunks.append(use("archive", 54, y + 34, 42, AMBER))
    chunks.append(f'<text x="124" y="{y+49}" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">Archived conversation</text>')
    chunks.append(f'<text x="124" y="{y+81}" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">Restore without changing the conversation content</text>')
    chunks.append(use("arrow-counter-clockwise", 766, y + 40, 32, GREEN))
    chunks.append('</g></g>')

    # Right panel: long press sheet
    chunks.append('<g transform="translate(1002 236)"><rect width="516" height="800" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append('<rect x="210" y="20" width="96" height="6" rx="3" fill="#CBD2DD"/>')
    chunks.append(f'<text x="34" y="76" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" letter-spacing="2" fill="{SOFT}">SESSION ACTIONS</text>')
    action_rows = [
        ("AI rename", "magic-wand", VIOLET),
        ("Pin conversation", "push-pin-simple", AMBER),
        ("Archive", "archive", AMBER),
        ("Move to project", "folders", ACCENT),
        ("Delete", "trash-simple", RED),
    ]
    for i, (label, icon, color) in enumerate(action_rows):
        y = 110 + i * 116
        if i == 4:
            chunks.append(f'<line x1="30" y1="{y-10}" x2="486" y2="{y-10}" stroke="#EDF0F4" stroke-width="2"/>')
        well_bg = RED_BG if i == 4 else (AMBER_BG if icon in {"archive", "push-pin-simple"} else PILL)
        chunks.append(f'<rect x="30" y="{y+10}" width="64" height="64" rx="20" fill="{well_bg}"/>')
        chunks.append(use(icon, 46, y + 26, 32, color))
        chunks.append(f'<text x="120" y="{y+53}" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="600" fill="{RED if i == 4 else INK}">{label}</text>')
        if i != 4:
            chunks.append(use("caret-right", 452, y + 29, 26, SOFT))
    chunks.append(f'<rect x="30" y="708" width="456" height="62" rx="22" fill="#F5F7FA"/><text x="258" y="747" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{MUTED}">Cancel</text>')
    chunks.append('</g>')

    chunks.append(f'<g transform="translate(82 1084)">{use("archive", 0, -4, 28, AMBER)}<text x="42" y="18" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">recoverable</text>{use("trash-simple", 212, -4, 28, RED)}<text x="254" y="18" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">destructive</text>{use("push-pin-simple-fill", 420, -4, 28, AMBER)}<text x="462" y="18" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">persistent state</text></g>')
    chunks.append('</svg>')
    return "".join(chunks)


def write_file(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def copy_icons() -> None:
    ICONS.mkdir(parents=True, exist_ok=True)
    for key, _, outline, filled, _ in ICON_SPECS:
        shutil.copy2(source_path(outline), ICONS / f"{key}-outline.svg")
        if filled:
            shutil.copy2(source_path(filled), ICONS / f"{key}-filled.svg")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    write_file(OUT / "01-session-icon-master.svg", master_sheet())
    write_file(OUT / "02-filter-menu-preview.svg", filter_preview())
    write_file(OUT / "03-session-actions-preview.svg", session_actions_preview())
    copy_icons()


if __name__ == "__main__":
    main()
