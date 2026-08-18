#!/usr/bin/env python3
"""Build refined Hermes profile/settings icon presentation boards."""

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
LINE = "#E7EAF0"
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


# Profile identity itself uses the avatar, so this round concentrates on the
# actionable glyphs around the identity and every icon in the separate settings page.
ICON_SPECS = [
    ("change-photo", "Set photo", "camera-plus", "camera-plus-fill", "Profile", ACCENT, BLUE_BG),
    ("edit-profile", "Edit info", "pencil-simple", "pencil-simple-fill", "Profile", ACCENT, BLUE_BG),
    ("settings", "Settings", "gear-six", "gear-six-fill", "Profile", ACCENT, BLUE_BG),
    ("gateway", "Gateway", "cloud-check", "cloud-check-fill", "Connection", ACCENT, BLUE_BG),
    ("appearance", "Appearance", "palette", "palette-fill", "Settings", VIOLET, VIOLET_BG),
    ("notification", "Notifications", "bell", "bell-fill", "Settings", RED, RED_BG),
    ("voice", "Voice", "microphone", "microphone-fill", "Settings", CYAN, CYAN_BG),
    ("skills", "Skills & tools", "toolbox", "toolbox-fill", "Agent", VIOLET, VIOLET_BG),
    ("model", "Models", "stack", "stack-fill", "Agent", ACCENT, BLUE_BG),
    ("chat-style", "Chat style", "chat-circle-text", "chat-circle-text-fill", "Agent", CYAN, CYAN_BG),
    ("approval", "Approval", "shield-check", "shield-check-fill", "Agent", GREEN, GREEN_BG),
    ("memory-context", "Memory context", "brain", "brain-fill", "Agent", ACCENT, BLUE_BG),
    ("archive", "Archived chats", "archive", "archive-fill", "Library", AMBER, AMBER_BG),
    ("changelog", "Changelog", "clock-counter-clockwise", "clock-counter-clockwise-fill", "Support", CYAN, CYAN_BG),
    ("about", "About Hermes", "info", "info-fill", "Support", ACCENT, BLUE_BG),
    ("guide", "User guide", "book-open-text", "book-open-text-fill", "Support", ACCENT, BLUE_BG),
    ("memory-file", "Memory file", "file-text", "file-text-fill", "Agent file", ACCENT, BLUE_BG),
    ("soul-file", "Soul file", "sparkle", "sparkle-fill", "Agent file", VIOLET, VIOLET_BG),
]


def source_path(master: str) -> Path:
    style = "fill" if master.endswith("-fill") else "regular"
    return PHOSPHOR / style / f"{master}.svg"


def icon_inner(master: str) -> str:
    root = ET.parse(source_path(master)).getroot()
    return "".join(ET.tostring(child, encoding="unicode") for child in root)


def symbols() -> str:
    names: list[str] = []
    seen: set[str] = set()
    for _, _, outline, filled, _, _, _ in ICON_SPECS:
        for name in (outline, filled):
            if name not in seen:
                seen.add(name)
                names.append(name)
    for name in ("caret-right", "caret-left", "check-circle-fill", "robot", "device-mobile", "arrow-square-out"):
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
    <linearGradient id="avatar" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#3578F6"/><stop offset="1" stop-color="#20B7C3"/></linearGradient>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def master_sheet() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes profile and settings icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">HERMES MOBILE · ROUND 06</text>')
    chunks.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Profile &amp; settings icons</text>')
    chunks.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">Closed geometry · line default · filled active state · color follows meaning, not decoration</text>')

    card_w, card_h = 150, 348
    start_x, start_y = 82, 226
    for index, (key, label, outline, filled, group, color, tint) in enumerate(ICON_SPECS):
        row, col = divmod(index, 9)
        x = start_x + col * 160
        y = start_y + row * 372
        chunks.append(f'<g transform="translate({x} {y})">')
        chunks.append(f'<rect width="{card_w}" height="{card_h}" rx="28" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="16" y="32" font-family="Inter,Arial,sans-serif" font-size="10.5" font-weight="700" letter-spacing="1" fill="#8B96A9">{group.upper()}</text>')
        chunks.append('<rect x="16" y="50" width="118" height="142" rx="27" fill="#F6F8FC"/>')
        chunks.append(use(outline, 42, 84, 66, MUTED))
        chunks.append(f'<circle cx="112" cy="168" r="23" fill="{tint}"/>')
        chunks.append(use(filled, 99, 155, 26, color))
        chunks.append(f'<text x="16" y="230" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="16" y="260" font-family="Inter,Arial,sans-serif" font-size="13" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 16, 289, 24, MUTED))
        chunks.append(f'<text x="52" y="307" font-family="Inter,Arial,sans-serif" font-size="12.5" fill="{SOFT}">actual size</text>')
        chunks.append('</g>')

    chunks.append('<g transform="translate(82 1000)"><rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    notes = [
        (ACCENT, "Blue", "connection / navigation"),
        (CYAN, "Cyan", "voice / conversation"),
        (VIOLET, "Violet", "agent capability / Soul"),
        (GREEN, "Green", "verified / approval"),
    ]
    for i, (color, label, note) in enumerate(notes):
        x = 36 + i * 350
        chunks.append(f'<circle cx="{x}" cy="36" r="7" fill="{color}"/>')
        chunks.append(f'<text x="{x+20}" y="43" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">{label}</text>')
        chunks.append(f'<text x="{x+88}" y="43" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">{note}</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def avatar(cx: int, cy: int, radius: int) -> str:
    # Neutral brand avatar substitute for the board; the production page keeps the uploaded photo.
    return (
        f'<circle cx="{cx}" cy="{cy}" r="{radius}" fill="url(#avatar)"/>'
        f'<circle cx="{cx+radius*0.62}" cy="{cy-radius*0.62}" r="{radius*0.13}" fill="#8FFFF0" stroke="#FFFFFF" stroke-width="4"/>'
        f'<text x="{cx}" y="{cy+radius*0.16}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="{radius*0.56}" font-weight="700" fill="#FFFFFF">J</text>'
    )


def profile_preview() -> str:
    width, height = 1600, 1140
    chunks = [svg_open(width, height, "Hermes profile home preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · PROFILE HOME</text>')
    chunks.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Identity first, settings one tap away</text>')
    chunks.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">Memory and Soul are direct Agent-file viewers. They are not configuration shortcuts.</text>')

    # Main profile surface.
    chunks.append('<g transform="translate(82 224)"><rect width="956" height="838" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(avatar(478, 132, 74))
    chunks.append(f'<text x="478" y="244" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="32" font-weight="700" fill="{INK}">Jerome</text>')
    chunks.append(f'<text x="478" y="278" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Thoughtful, capable, always close</text>')

    actions = [
        ("camera-plus", "Set photo"),
        ("pencil-simple", "Edit info"),
        ("gear-six", "Settings"),
    ]
    for i, (icon, label) in enumerate(actions):
        x = 64 + i * 284
        chunks.append(f'<rect x="{x}" y="318" width="260" height="108" rx="28" fill="#F6F8FC"/>')
        chunks.append(use(icon, x + 106, 338, 48, INK))
        chunks.append(f'<text x="{x+130}" y="405" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">{label}</text>')

    # Compact information and Agent-file group.
    chunks.append('<rect x="40" y="462" width="876" height="116" rx="28" fill="#F8FAFC"/>')
    chunks.append(f'<rect x="62" y="485" width="70" height="70" rx="22" fill="{BLUE_BG}"/>')
    chunks.append(use("cloud-check", 79, 502, 36, ACCENT))
    chunks.append(f'<text x="158" y="510" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">Remote gateway</text>')
    chunks.append(f'<circle cx="159" cy="539" r="5" fill="{GREEN}"/><text x="174" y="546" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">Connected · Hermes Agent 0.20.1</text>')
    chunks.append(use("caret-right", 866, 506, 30, SOFT))

    files = [
        ("file-text", ACCENT, BLUE_BG, "Memory", "Open MEMORY.md"),
        ("sparkle", VIOLET, VIOLET_BG, "Soul", "Open SOUL.md"),
    ]
    for i, (icon, color, tint, title, subtitle) in enumerate(files):
        y = 604 + i * 104
        chunks.append(f'<rect x="40" y="{y}" width="876" height="92" rx="26" fill="#FFFFFF" stroke="{LINE}" stroke-width="2"/>')
        chunks.append(f'<rect x="62" y="{y+17}" width="58" height="58" rx="18" fill="{tint}"/>')
        chunks.append(use(icon, 77, y + 32, 28, color))
        chunks.append(f'<text x="146" y="{y+39}" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" fill="{INK}">{title}</text>')
        chunks.append(f'<text x="146" y="{y+66}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{subtitle}</text>')
        chunks.append(use("caret-right", 866, y + 31, 30, SOFT))
    chunks.append('</g>')

    # Detail rail.
    chunks.append('<g transform="translate(1080 224)"><rect width="438" height="838" rx="42" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<text x="34" y="58" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{INK}">Interaction notes</text>')
    notes = [
        ("01", "Avatar", ("Shared-element expansion into", "the full photo view.")),
        ("02", "Three actions", ("Profile actions stay visible;", "settings opens a separate page.")),
        ("03", "Agent files", ("Memory and Soul open read-only", "file viewers directly.")),
        ("04", "No duplication", ("Settings does not repeat the", "avatar or identity header.")),
    ]
    for i, (num, title, body_lines) in enumerate(notes):
        y = 98 + i * 154
        chunks.append(f'<circle cx="56" cy="{y+8}" r="22" fill="{BLUE_BG}"/><text x="56" y="{y+14}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="12" font-weight="700" fill="{ACCENT}">{num}</text>')
        chunks.append(f'<text x="92" y="{y+3}" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{title}</text>')
        for line_index, line in enumerate(body_lines):
            chunks.append(f'<text x="92" y="{y+31+line_index*23}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{line}</text>')
    chunks.append(f'<rect x="34" y="718" width="370" height="82" rx="26" fill="{VIOLET_BG}"/>')
    chunks.append(use("sparkle-fill", 56, 743, 32, VIOLET))
    chunks.append(f'<text x="102" y="753" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">Soul remains distinct</text>')
    chunks.append(f'<text x="102" y="779" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">Violet signals identity, not settings.</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def list_row(y: int, icon: str, color: str, tint: str, title: str, subtitle: str = "", value: str = "") -> str:
    chunks = [f'<g transform="translate(0 {y})">']
    chunks.append(f'<rect x="22" y="16" width="58" height="58" rx="18" fill="{tint}"/>')
    chunks.append(use(icon, 37, 31, 28, color))
    chunks.append(f'<text x="102" y="43" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="650" fill="{INK}">{html.escape(title)}</text>')
    if subtitle:
        chunks.append(f'<text x="102" y="68" font-family="Inter,Arial,sans-serif" font-size="13.5" fill="{MUTED}">{html.escape(subtitle)}</text>')
    if value:
        chunks.append(f'<text x="720" y="48" text-anchor="end" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{html.escape(value)}</text>')
    chunks.append(use("caret-right", 734, 31, 28, SOFT))
    chunks.append(f'<line x1="102" y1="88" x2="760" y2="88" stroke="{LINE}" stroke-width="1.5"/>')
    chunks.append('</g>')
    return "".join(chunks)


def settings_preview() -> str:
    width, height = 1600, 1140
    chunks = [svg_open(width, height, "Hermes separate settings list preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="82" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · SETTINGS</text>')
    chunks.append(f'<text x="82" y="140" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">A dedicated settings page</text>')
    chunks.append(f'<text x="82" y="180" font-family="Inter,Arial,sans-serif" font-size="21" fill="{MUTED}">No repeated avatar block. Each color is a small navigation cue, not a full-page decoration.</text>')

    # Phone-like settings list.
    chunks.append('<g transform="translate(82 224)"><rect width="820" height="838" rx="46" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(use("caret-left", 36, 37, 34, MUTED))
    chunks.append(f'<text x="84" y="64" font-family="Inter,Arial,sans-serif" font-size="29" font-weight="700" fill="{INK}">Settings</text>')
    chunks.append(f'<text x="84" y="91" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">Hermes mobile preferences</text>')
    chunks.append('<g transform="translate(20 124)"><rect width="780" height="364" rx="30" fill="#FBFCFE"/>')
    chunks.append(list_row(0, "cloud-check", ACCENT, BLUE_BG, "Remote gateway", "Connected", "0.20.1"))
    chunks.append(list_row(90, "palette", VIOLET, VIOLET_BG, "Appearance"))
    chunks.append(list_row(180, "bell", RED, RED_BG, "Notifications"))
    chunks.append(list_row(270, "microphone", CYAN, CYAN_BG, "Voice"))
    chunks.append('</g>')
    chunks.append('<g transform="translate(20 510)"><rect width="780" height="288" rx="30" fill="#FBFCFE"/>')
    chunks.append(list_row(0, "toolbox", VIOLET, VIOLET_BG, "Skills & tools"))
    chunks.append(list_row(90, "stack", ACCENT, BLUE_BG, "Model settings"))
    chunks.append(list_row(180, "shield-check", GREEN, GREEN_BG, "Approval mode"))
    chunks.append('</g></g>')

    # Semantics board.
    chunks.append('<g transform="translate(944 224)"><rect width="574" height="838" rx="42" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<text x="34" y="58" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{INK}">Remaining destinations</text>')
    entries = [
        ("chat-circle-text", CYAN, CYAN_BG, "Chat style", "conversation behavior"),
        ("brain", ACCENT, BLUE_BG, "Memory & context", "configuration"),
        ("archive", AMBER, AMBER_BG, "Archived chats", "content library"),
        ("clock-counter-clockwise", CYAN, CYAN_BG, "Changelog", "support"),
        ("book-open-text", ACCENT, BLUE_BG, "User guide", "support"),
        ("info", ACCENT, BLUE_BG, "About Hermes", "version & capability"),
    ]
    for i, (icon, color, tint, title, subtitle) in enumerate(entries):
        y = 92 + i * 106
        chunks.append(f'<rect x="34" y="{y}" width="506" height="88" rx="24" fill="#FBFCFE"/>')
        chunks.append(f'<rect x="50" y="{y+15}" width="58" height="58" rx="18" fill="{tint}"/>')
        chunks.append(use(icon, 65, y + 30, 28, color))
        chunks.append(f'<text x="130" y="{y+38}" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{html.escape(title)}</text>')
        chunks.append(f'<text x="130" y="{y+64}" font-family="Inter,Arial,sans-serif" font-size="13.5" fill="{MUTED}">{html.escape(subtitle)}</text>')
        chunks.append(use("caret-right", 494, y + 30, 28, SOFT))
    chunks.append(f'<rect x="34" y="748" width="506" height="54" rx="20" fill="{GREEN_BG}"/>')
    chunks.append(use("check-circle-fill", 54, 763, 24, GREEN))
    chunks.append(f'<text x="92" y="782" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" fill="{INK}">24 px audit passed · no crossing strokes</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def export_icons() -> None:
    (ICONS / "outline").mkdir(parents=True, exist_ok=True)
    (ICONS / "filled").mkdir(parents=True, exist_ok=True)
    for key, _, outline, filled, _, _, _ in ICON_SPECS:
        shutil.copy2(source_path(outline), ICONS / "outline" / f"{key}.svg")
        shutil.copy2(source_path(filled), ICONS / "filled" / f"{key}.svg")


def main() -> None:
    export_icons()
    outputs = {
        "01-profile-settings-icon-master.svg": master_sheet(),
        "02-profile-home-preview.svg": profile_preview(),
        "03-settings-list-preview.svg": settings_preview(),
    }
    for name, data in outputs.items():
        (OUT / name).write_text(data, encoding="utf-8")


if __name__ == "__main__":
    main()
