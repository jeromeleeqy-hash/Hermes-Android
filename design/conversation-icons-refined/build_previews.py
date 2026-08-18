#!/usr/bin/env python3
"""Build refined conversation-icon presentation boards from professional masters."""

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
CYAN = "#28C7BE"
INK = "#1B2436"
MUTED = "#687488"
SOFT = "#8792A6"
BG = "#F4F6FA"
PILL = "#E6F4FD"
GREEN = "#25BF91"


ICON_SPECS = [
    ("back", "Back", "arrow-left", None, "Top bar"),
    ("search", "Search", "magnifying-glass", None, "Top bar"),
    ("compose", "New chat", "pencil-simple", "pencil-simple-fill", "Home"),
    ("attach", "Attachment", "paperclip", None, "Composer"),
    ("microphone", "Microphone", "microphone", "microphone-fill", "Composer"),
    ("send", "Send", "paper-plane-tilt", "paper-plane-tilt-fill", "Composer"),
    ("command", "Command", "terminal", None, "Accessory"),
    ("voice", "Live voice", "waveform", None, "Top bar"),
    ("assistant", "Assistant", "robot", "robot-fill", "Top bar"),
    ("down", "Jump down", "caret-down", None, "Chat"),
    ("more", "More", "dots-three-vertical", None, "Top bar"),
    ("close", "Close", "x", None, "Common"),
    ("complete", "Completed", "check-circle", "check-circle-fill", "Status"),
    ("disclosure", "Open card", "caret-right", None, "File card"),
]


def source_path(master: str) -> Path:
    style = "fill" if master.endswith("-fill") else "regular"
    return PHOSPHOR / style / f"{master}.svg"


def icon_inner(master: str) -> str:
    root = ET.parse(source_path(master)).getroot()
    return "".join(ET.tostring(child, encoding="unicode") for child in root)


def symbols() -> str:
    masters = []
    seen = set()
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
    <filter id="softShadow" x="-40%" y="-40%" width="180%" height="220%"><feDropShadow dx="0" dy="8" stdDeviation="14" flood-color="#273248" flood-opacity="0.10"/></filter>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def master_sheet() -> str:
    width, height = 1600, 1130
    chunks = [svg_open(width, height, "Hermes conversation icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append('<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="#2A9FE8">HERMES MOBILE · ROUND 02</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Conversation action icon masters</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Closed compound geometry · 24 px optical audit · Telegram-like restraint</text>')

    card_w, card_h = 196, 346
    gap_x, gap_y = 22, 26
    start_x, start_y = 82, 240
    for index, (key, label, outline, filled, group) in enumerate(ICON_SPECS):
        row, col = divmod(index, 7)
        x = start_x + col * (card_w + gap_x)
        y = start_y + row * (card_h + gap_y)
        chunks.append(f'<g transform="translate({x} {y})">')
        chunks.append(f'<rect width="{card_w}" height="{card_h}" rx="32" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="22" y="36" font-family="Inter,Arial,sans-serif" font-size="13" font-weight="700" letter-spacing="1.5" fill="#8B96A9">{html.escape(group.upper())}</text>')
        chunks.append('<rect x="22" y="56" width="152" height="152" rx="30" fill="#F6F8FC"/>')
        chunks.append(use(outline, 62, 96, 72, MUTED))
        if filled:
            fill_color = GREEN if key == "complete" else ACCENT
            chunks.append(f'<circle cx="151" cy="185" r="26" fill="{PILL}"/>')
            chunks.append(use(filled, 136, 170, 30, fill_color))
        chunks.append(f'<text x="22" y="248" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="22" y="278" font-family="Inter,Arial,sans-serif" font-size="15" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 22, 298, 24, MUTED))
        chunks.append(f'<text x="58" y="317" font-family="Inter,Arial,sans-serif" font-size="14" fill="{SOFT}">actual size</text>')
        chunks.append('</g>')

    chunks.append('<g transform="translate(82 1001)">')
    chunks.append('<rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    chunks.append(f'<circle cx="32" cy="36" r="6" fill="{CYAN}"/><text x="52" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">No open endpoints</text>')
    chunks.append(f'<circle cx="340" cy="36" r="6" fill="{CYAN}"/><text x="360" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">No stacked strokes</text>')
    chunks.append(f'<circle cx="642" cy="36" r="6" fill="{CYAN}"/><text x="662" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Unified visual box</text>')
    chunks.append(f'<circle cx="930" cy="36" r="6" fill="{CYAN}"/><text x="950" y="43" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Outline + active states</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def composer_preview() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes refined conversation controls")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append('<text x="88" y="88" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="#2A9FE8">IN-CONTEXT · CONVERSATION</text>')
    chunks.append(f'<text x="88" y="146" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">A quieter, Telegram-like message surface</text>')
    chunks.append('<text x="88" y="187" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Assistant and live voice stay in the top bar; the composer keeps only immediate actions.</text>')

    # Top app bar
    chunks.append('<g transform="translate(88 252)"><rect width="1424" height="178" rx="40" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(use("arrow-left", 38, 62, 52, MUTED))
    chunks.append(f'<circle cx="138" cy="89" r="32" fill="{PILL}"/><text x="138" y="98" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="22" font-weight="700" fill="{ACCENT}">H</text>')
    chunks.append(f'<text x="192" y="78" font-family="Inter,Arial,sans-serif" font-size="27" font-weight="700" fill="{INK}">Project delivery review</text>')
    chunks.append(f'<circle cx="202" cy="112" r="5" fill="{GREEN}"/><text x="219" y="119" font-family="Inter,Arial,sans-serif" font-size="18" fill="{MUTED}">deepseek-v4-pro · online</text>')
    chunks.append(f'<rect x="1178" y="45" width="88" height="88" rx="28" fill="#E6F8F6"/>{use("waveform", 1200, 67, 44, CYAN)}')
    chunks.append(f'<rect x="1288" y="45" width="88" height="88" rx="28" fill="#F0EBFD"/>{use("robot", 1310, 67, 44, "#7D5CE7")}')
    chunks.append('</g>')

    # Default composer
    chunks.append('<g transform="translate(88 492)"><text x="0" y="0" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" letter-spacing="2" fill="#8B96A9">DEFAULT · VOICE READY</text>')
    chunks.append('<rect x="0" y="30" width="1424" height="150" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(use("paperclip", 34, 76, 54, MUTED))
    chunks.append(f'<text x="112" y="111" font-family="Inter,Arial,sans-serif" font-size="25" fill="#98A1B2">Message Hermes…</text>')
    chunks.append(f'<circle cx="1336" cy="105" r="46" fill="{ACCENT}"/>{use("microphone-fill", 1310, 79, 52, "#FFFFFF")}')
    chunks.append('</g>')

    # Typing composer
    chunks.append('<g transform="translate(88 750)"><text x="0" y="0" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" letter-spacing="2" fill="#8B96A9">TYPING · SEND READY</text>')
    chunks.append('<rect x="0" y="30" width="1424" height="150" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(use("paperclip", 34, 76, 54, MUTED))
    chunks.append(f'<text x="112" y="111" font-family="Inter,Arial,sans-serif" font-size="25" fill="{INK}">Please summarize the next three actions</text>')
    chunks.append(f'<circle cx="1336" cy="105" r="46" fill="{ACCENT}"/>{use("paper-plane-tilt-fill", 1310, 79, 52, "#FFFFFF")}')
    chunks.append('</g>')

    # Accessory disclosure
    chunks.append('<g transform="translate(88 1002)">')
    chunks.append(f'{use("terminal", 0, 0, 30, MUTED)}<text x="44" y="24" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Commands move into the attachment tray instead of occupying the composer.</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def home_preview() -> str:
    width, height = 1500, 980
    chunks = [svg_open(width, height, "Hermes home action preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append('<text x="84" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="#2A9FE8">IN-CONTEXT · HOME</text>')
    chunks.append(f'<text x="84" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Search stays quiet; compose becomes a precise circle</text>')
    chunks.append('<text x="84" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">The two primary actions no longer compete with filters or conversation content.</text>')
    chunks.append('<g transform="translate(84 240)"><rect width="1332" height="630" rx="52" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<circle cx="86" cy="86" r="42" fill="{PILL}"/><text x="86" y="97" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="26" font-weight="700" fill="{ACCENT}">H</text>')
    chunks.append(f'<text x="150" y="78" font-family="Inter,Arial,sans-serif" font-size="31" font-weight="700" fill="{INK}">Hermes</text><circle cx="160" cy="111" r="5" fill="{GREEN}"/><text x="177" y="118" font-family="Inter,Arial,sans-serif" font-size="18" fill="{MUTED}">Profile · work</text>')
    chunks.append(use("magnifying-glass", 1202, 55, 54, MUTED))
    chunks.append('<line x1="36" y1="164" x2="1296" y2="164" stroke="#E9EDF3" stroke-width="2"/>')
    chunks.append(f'<text x="42" y="220" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="700" fill="{SOFT}">RECENT CONVERSATIONS</text>')
    for i, (title, preview) in enumerate([
        ("AI sales pilot", "Confirm the meeting scope and delivery standard"),
        ("Weekly report progress", "Five updates have been organized"),
        ("CRM dashboard redesign", "Continue the metric definition review"),
    ]):
        y = 264 + i * 106
        chunks.append(f'<circle cx="76" cy="{y+31}" r="31" fill="#E9F4FD"/><text x="76" y="{y+39}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{ACCENT}">{title[0]}</text>')
        chunks.append(f'<text x="132" y="{y+24}" font-family="Inter,Arial,sans-serif" font-size="23" font-weight="700" fill="{INK}">{html.escape(title)}</text>')
        chunks.append(f'<text x="132" y="{y+56}" font-family="Inter,Arial,sans-serif" font-size="18" fill="{SOFT}">{html.escape(preview)}</text>')
        chunks.append(f'<line x1="132" y1="{y+86}" x2="1266" y2="{y+86}" stroke="#EEF1F5" stroke-width="2"/>')
    chunks.append(f'<circle cx="1240" cy="538" r="54" fill="{ACCENT}" filter="url(#softShadow)"/>{use("pencil-simple-fill", 1208, 506, 64, "#FFFFFF")}')
    chunks.append('</g>')
    chunks.append(f'<g transform="translate(84 910)">{use("magnifying-glass", 0, 0, 26, MUTED)}<text x="40" y="22" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">24 px search</text>{use("pencil-simple", 270, 0, 26, ACCENT)}<text x="310" y="22" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">24 px compose</text></g>')
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
    write_file(OUT / "01-conversation-icon-master.svg", master_sheet())
    write_file(OUT / "02-chat-toolbar-preview.svg", composer_preview())
    write_file(OUT / "03-home-actions-preview.svg", home_preview())
    copy_icons()


if __name__ == "__main__":
    main()
