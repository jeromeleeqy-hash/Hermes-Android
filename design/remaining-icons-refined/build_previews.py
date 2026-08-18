#!/usr/bin/env python3
"""Build the final Hermes Agent, editor, theme and status icon boards."""

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
VIOLET = "#7564E8"
VIOLET_BG = "#EEEAFE"
CYAN = "#20AFC5"
CYAN_BG = "#E3F7F8"
AMBER = "#E79A20"
AMBER_BG = "#FFF2D7"
RED = "#EF5362"
RED_BG = "#FFE8EC"


ICON_SPECS = [
    ("ai", "Agent", "robot", "robot-fill", "Agent", VIOLET, VIOLET_BG),
    ("artifact", "Artifact", "sparkle", "sparkle-fill", "Agent", VIOLET, VIOLET_BG),
    ("council", "Council", "users-three", "users-three-fill", "Agent", VIOLET, VIOLET_BG),
    ("idea", "Idea", "lightbulb", "lightbulb-fill", "Agent", AMBER, AMBER_BG),
    ("summarize", "Summarize", "article", "article-fill", "Agent", ACCENT, BLUE_BG),
    ("plan", "Plan", "list-checks", "list-checks-fill", "Agent", ACCENT, BLUE_BG),
    ("todo", "To-do", "check-square", "check-square-fill", "Agent", GREEN, GREEN_BG),
    ("waveform", "Voice wave", "waveform", "waveform-fill", "Agent", CYAN, CYAN_BG),
    ("bold", "Bold", "text-b", "text-b-fill", "Editor", ACCENT, BLUE_BG),
    ("italic", "Italic", "text-italic", "text-italic-fill", "Editor", ACCENT, BLUE_BG),
    ("bullet-list", "Bullets", "list-bullets", "list-bullets-fill", "Editor", MUTED, "#EEF1F5"),
    ("numbered-list", "Numbers", "list-numbers", "list-numbers-fill", "Editor", MUTED, "#EEF1F5"),
    ("quote", "Quote", "quotes", "quotes-fill", "Editor", MUTED, "#EEF1F5"),
    ("horizontal-rule", "Divider", "minus", "minus-fill", "Editor", MUTED, "#EEF1F5"),
    ("link", "Link", "link", "link-fill", "Editor", ACCENT, BLUE_BG),
    ("drag-handle", "Drag", "dots-six-vertical", "dots-six-vertical-fill", "Editor", MUTED, "#EEF1F5"),
    ("light-mode", "Light", "sun", "sun-fill", "Theme", AMBER, AMBER_BG),
    ("dark-mode", "Dark", "moon", "moon-fill", "Theme", VIOLET, VIOLET_BG),
    ("system-mode", "System", "device-mobile", "device-mobile-fill", "Theme", ACCENT, BLUE_BG),
    ("status-connected", "Connected", "check-circle", "check-circle-fill", "Status", GREEN, GREEN_BG),
    ("status-busy", "Busy", "spinner-gap", "spinner-gap-fill", "Status", ACCENT, BLUE_BG),
    ("status-error", "Error", "x-circle", "x-circle-fill", "Status", RED, RED_BG),
    ("loading", "Loading", "circle-notch", "circle-notch-fill", "Status", ACCENT, BLUE_BG),
    ("sync", "Sync", "arrows-clockwise", "arrows-clockwise-fill", "Status", CYAN, CYAN_BG),
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
    for name in ("paper-plane-tilt-fill", "paperclip", "microphone", "x", "check", "caret-right"):
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
    <filter id="softShadow" x="-40%" y="-40%" width="180%" height="220%"><feDropShadow dx="0" dy="7" stdDeviation="13" flood-color="#273248" flood-opacity="0.10"/></filter>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def text(x: float, y: float, copy: str, size: float, color: str = INK, weight: int = 500,
         anchor: str = "start", letter: float | None = None) -> str:
    spacing = f' letter-spacing="{letter}"' if letter is not None else ""
    return (f'<text x="{x}" y="{y}" text-anchor="{anchor}" font-family="Inter,Arial,sans-serif" '
            f'font-size="{size}" font-weight="{weight}" fill="{color}"{spacing}>{html.escape(copy)}</text>')


def master_sheet() -> str:
    width, height = 1600, 1470
    c = [svg_open(width, height, "Hermes final utility icon masters")]
    c.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    c.append(text(82, 82, "HERMES MOBILE · ROUND 08", 20, ACCENT, 700, letter=5))
    c.append(text(82, 140, "Final utility icon set", 44, INK, 700))
    c.append(text(82, 180, "Agent tools · Markdown editing · theme modes · connection feedback", 21, MUTED))

    card_w, card_h = 166, 328
    for index, (key, label, outline, filled, group, color, tint) in enumerate(ICON_SPECS):
        row, col = divmod(index, 8)
        x = 82 + col * 180
        y = 226 + row * 346
        c.append(f'<g transform="translate({x} {y})"><rect width="{card_w}" height="{card_h}" rx="28" fill="#FFFFFF" filter="url(#shadow)"/>')
        c.append(text(16, 32, group.upper(), 10.5, SOFT, 700, letter=1))
        c.append('<rect x="16" y="50" width="134" height="132" rx="27" fill="#F6F8FC"/>')
        c.append(use(outline, 50, 80, 66, MUTED))
        c.append(f'<circle cx="126" cy="158" r="22" fill="{tint}"/>')
        c.append(use(filled, 114, 146, 24, color))
        c.append(text(16, 218, label, 15, INK, 700))
        c.append(text(16, 246, "24 px master", 13, SOFT))
        c.append(use(outline, 16, 276, 24, MUTED))
        c.append(text(52, 294, "actual size", 12.5, SOFT))
        c.append('</g>')

    c.append('<g transform="translate(82 1290)"><rect width="1426" height="116" rx="31" fill="#FFFFFF" filter="url(#softShadow)"/>')
    notes = [
        ("Closed geometry", "no broken tails"),
        ("Outline first", "quiet default"),
        ("Fill on state", "active only"),
        ("24 px audit", "all masters"),
    ]
    for i, (title, note) in enumerate(notes):
        x = 34 + i * 350
        c.append(f'<circle cx="{x+7}" cy="56" r="7" fill="{ACCENT if i < 2 else GREEN}"/>')
        c.append(text(x + 26, 50, title, 16, INK, 700))
        c.append(text(x + 26, 76, note, 14, MUTED))
    c.append('</g></svg>')
    return "".join(c)


def agent_editor_preview() -> str:
    width, height = 1600, 1160
    c = [svg_open(width, height, "Hermes Agent and editor icon preview")]
    c.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    c.append(text(82, 82, "IN-CONTEXT · AGENT & EDITOR", 20, VIOLET, 700, letter=5))
    c.append(text(82, 140, "Agent tools meet a quiet editor", 44, INK, 700))
    c.append(text(82, 180, "Capability icons carry meaning; formatting controls stay visually subordinate to the text.", 21, MUTED))

    # Assistant panel
    c.append('<g transform="translate(82 224)"><rect width="604" height="854" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    c.append(f'<rect x="34" y="34" width="64" height="64" rx="22" fill="{VIOLET_BG}"/>')
    c.append(use("robot-fill", 50, 50, 32, VIOLET))
    c.append(text(120, 62, "Assistant panel", 24, INK, 700))
    c.append(text(120, 88, "Specialized tools for the next reply", 14, MUTED))
    c.append(f'<line x1="34" y1="122" x2="570" y2="122" stroke="{LINE}" stroke-width="2"/>')
    tools = [
        ("lightbulb", "Idea", "Explore one new angle", AMBER, AMBER_BG),
        ("article", "Summarize", "Compress the conversation", ACCENT, BLUE_BG),
        ("list-checks", "Plan", "Turn the goal into steps", ACCENT, BLUE_BG),
        ("check-square", "To-do", "Extract actionable items", GREEN, GREEN_BG),
        ("users-three", "Expert council", "Compare independent views", VIOLET, VIOLET_BG),
        ("sparkle", "Create artifact", "Produce a reusable result", VIOLET, VIOLET_BG),
    ]
    for i, (icon, title, note, color, tint) in enumerate(tools):
        y = 146 + i * 103
        c.append(f'<rect x="34" y="{y}" width="536" height="84" rx="25" fill="{tint if i == 4 else "#F8FAFC"}"/>')
        c.append(f'<rect x="50" y="{y+14}" width="56" height="56" rx="18" fill="{tint}"/>')
        c.append(use(icon, 64, y + 28, 28, color))
        c.append(text(126, y + 36, title, 17, INK, 700))
        c.append(text(126, y + 62, note, 13.5, MUTED))
        c.append(use("caret-right", 530, y + 30, 24, MUTED))
    c.append(f'<rect x="34" y="782" width="536" height="42" rx="21" fill="{CYAN_BG}"/>')
    c.append(use("waveform", 50, 791, 24, CYAN))
    c.append(text(88, 810, "Live voice is available from the top bar", 13.5, MUTED))
    c.append('</g>')

    # Markdown editor
    c.append('<g transform="translate(728 224)"><rect width="790" height="854" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    c.append(text(38, 62, "Markdown note", 24, INK, 700))
    c.append(text(38, 90, "A formatting toolbar that never competes with content", 14, MUTED))
    c.append(f'<line x1="38" y1="122" x2="752" y2="122" stroke="{LINE}" stroke-width="2"/>')
    c.append(text(46, 180, "Launch checklist", 28, INK, 700))
    c.append(text(46, 232, "A clear release note keeps decisions and actions easy to scan.", 18, MUTED))
    editor_rows = [
        ("1", "Confirm the scope and the model profile."),
        ("2", "Review the generated artifact."),
        ("3", "Share the final link with the team."),
    ]
    for i, (num, copy) in enumerate(editor_rows):
        y = 294 + i * 64
        c.append(f'<circle cx="64" cy="{y-6}" r="17" fill="{BLUE_BG}"/>')
        c.append(text(64, y, num, 13, ACCENT, 700, "middle"))
        c.append(text(98, y, copy, 17, INK, 500))
    c.append(f'<rect x="40" y="512" width="710" height="104" rx="28" fill="{VIOLET_BG}"/>')
    c.append(use("quotes-fill", 62, 536, 28, VIOLET))
    c.append(text(108, 552, "Keep the interface quiet; let the work stay visible.", 17, INK, 650))
    c.append(text(108, 584, "Hermes design principle", 13.5, MUTED))
    c.append('<rect x="40" y="670" width="710" height="98" rx="28" fill="#F6F8FC"/>')
    toolbar = [
        ("text-b-fill", ACCENT, BLUE_BG), ("text-italic", MUTED, "#FFFFFF"),
        ("list-bullets", MUTED, "#FFFFFF"), ("list-numbers", MUTED, "#FFFFFF"),
        ("quotes", MUTED, "#FFFFFF"), ("minus", MUTED, "#FFFFFF"),
        ("link", MUTED, "#FFFFFF"), ("dots-six-vertical", MUTED, "#FFFFFF"),
    ]
    for i, (icon, color, tint) in enumerate(toolbar):
        x = 58 + i * 84
        c.append(f'<rect x="{x}" y="687" width="64" height="64" rx="20" fill="{tint}"/>')
        c.append(use(icon, x + 18, 705, 28, color))
    c.append(text(40, 816, "Default: outline · Selected: filled accent · Press: 0.96 scale", 14, MUTED))
    c.append('</g></svg>')
    return "".join(c)


def theme_status_preview() -> str:
    width, height = 1600, 1160
    c = [svg_open(width, height, "Hermes theme and status icon preview")]
    c.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    c.append(text(82, 82, "IN-CONTEXT · THEME & STATUS", 20, CYAN, 700, letter=5))
    c.append(text(82, 140, "State without ambiguity", 44, INK, 700))
    c.append(text(82, 180, "Theme choices are calm; connection feedback uses color only when the state matters.", 21, MUTED))

    # Theme selector
    c.append('<g transform="translate(82 224)"><rect width="770" height="548" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    c.append(text(38, 62, "Appearance", 24, INK, 700))
    c.append(text(38, 90, "Choose how Hermes follows the device", 14, MUTED))
    themes = [
        ("sun", "Light", "Bright and calm", AMBER, AMBER_BG, False),
        ("moon", "Dark", "Low-light comfort", VIOLET, VIOLET_BG, False),
        ("device-mobile-fill", "System", "Follow device", ACCENT, BLUE_BG, True),
    ]
    for i, (icon, title, note, color, tint, selected) in enumerate(themes):
        y = 132 + i * 120
        c.append(f'<rect x="38" y="{y}" width="694" height="98" rx="28" fill="{tint if selected else "#F8FAFC"}" stroke="{ACCENT if selected else "transparent"}" stroke-width="2"/>')
        c.append(f'<rect x="56" y="{y+17}" width="64" height="64" rx="21" fill="{tint}"/>')
        c.append(use(icon, 73, y + 34, 30, color))
        c.append(text(146, y + 42, title, 18, INK, 700))
        c.append(text(146, y + 70, note, 14, MUTED))
        if selected:
            c.append(use("check", 676, y + 35, 28, ACCENT))
    c.append(text(38, 514, "Only the selected mode uses a filled glyph and tinted surface.", 14, MUTED))
    c.append('</g>')

    # Status panel
    c.append('<g transform="translate(894 224)"><rect width="624" height="548" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    c.append(text(38, 62, "Gateway status", 24, INK, 700))
    c.append(text(38, 90, "Compact, readable and consistent", 14, MUTED))
    statuses = [
        ("check-circle-fill", "Connected", "All services available", GREEN, GREEN_BG),
        ("spinner-gap", "Busy", "Agent is processing", ACCENT, BLUE_BG),
        ("x-circle-fill", "Error", "Connection needs attention", RED, RED_BG),
    ]
    for i, (icon, title, note, color, tint) in enumerate(statuses):
        y = 132 + i * 112
        c.append(f'<rect x="38" y="{y}" width="548" height="90" rx="27" fill="{tint}"/>')
        c.append(use(icon, 60, y + 27, 36, color))
        c.append(text(116, y + 40, title, 18, INK, 700))
        c.append(text(116, y + 68, note, 14, MUTED))
    c.append(f'<line x1="38" y1="486" x2="586" y2="486" stroke="{LINE}" stroke-width="2"/>')
    c.append(use("arrows-clockwise", 44, 506, 26, CYAN))
    c.append(text(84, 526, "Sync", 14, INK, 700))
    c.append(use("circle-notch", 456, 506, 26, ACCENT))
    c.append(text(494, 526, "Loading", 14, INK, 700))
    c.append('</g>')

    # Coverage footer
    c.append('<g transform="translate(82 822)"><rect width="1436" height="256" rx="42" fill="#FFFFFF" filter="url(#softShadow)"/>')
    c.append(text(38, 58, "ICON COVERAGE AUDIT", 14, SOFT, 700, letter=2))
    c.append(text(38, 104, "All current Hermes icon families are now covered", 28, INK, 700))
    families = [
        ("Navigation", "Round 01 + 07"), ("Conversation", "Round 02"),
        ("Sessions", "Round 03"), ("Workspace", "Round 04"),
        ("Tasks", "Round 05"), ("Profile", "Round 06"),
        ("Controls", "Round 07"), ("Utilities", "Round 08"),
    ]
    for i, (name, round_name) in enumerate(families):
        x = 38 + (i % 4) * 344
        y = 144 + (i // 4) * 54
        c.append(f'<circle cx="{x+8}" cy="{y+5}" r="8" fill="{GREEN}"/>')
        c.append(text(x + 28, y + 11, name, 15, INK, 700))
        c.append(text(x + 142, y + 11, round_name, 13.5, MUTED))
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
        "01-remaining-icon-master.svg": master_sheet(),
        "02-agent-editor-preview.svg": agent_editor_preview(),
        "03-theme-status-preview.svg": theme_status_preview(),
    }
    for name, data in outputs.items():
        (OUT / name).write_text(data, encoding="utf-8")


if __name__ == "__main__":
    main()
