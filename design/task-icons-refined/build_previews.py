#!/usr/bin/env python3
"""Build refined task/execution-center icon presentation boards."""

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
INK = "#1D2637"
MUTED = "#667287"
SOFT = "#8A95A7"
BG = "#F3F5F9"
PILL = "#E5F3FD"
GREEN = "#24B98A"
GREEN_BG = "#E2F7F0"
AMBER = "#E79A20"
AMBER_BG = "#FFF2D7"
RED = "#EF5362"
RED_BG = "#FFE8EC"
VIOLET = "#7564E8"
VIOLET_BG = "#EEEAFE"


ICON_SPECS = [
    ("pending", "Pending", "hourglass-medium", "hourglass-medium-fill", "Status"),
    ("running", "Running", "spinner-gap", "spinner-gap-fill", "Status"),
    ("scheduled", "Scheduled", "calendar-dots", "calendar-dots-fill", "Status"),
    ("history", "Run history", "clock-counter-clockwise", "clock-counter-clockwise-fill", "Status"),
    ("refresh", "Refresh", "arrows-clockwise", "arrows-clockwise-fill", "Page"),
    ("new-task", "New task", "plus", "plus-fill", "Page"),
    ("approval", "Approval", "shield-check", "shield-check-fill", "Request"),
    ("clarify", "Clarification", "question", "question-fill", "Request"),
    ("run-now", "Run now", "play", "play-fill", "Action"),
    ("pause", "Pause", "pause", "pause-fill", "Action"),
    ("stop", "Stop", "stop", "stop-fill", "Action"),
    ("enable", "Enable", "toggle-right", "toggle-right-fill", "Action"),
    ("success", "Succeeded", "check-circle", "check-circle-fill", "Result"),
    ("failed", "Failed", "x-circle", "x-circle-fill", "Result"),
    ("edit", "Edit schedule", "pencil-simple", "pencil-simple-fill", "Action"),
    ("delete", "Delete", "trash-simple", "trash-simple-fill", "Action"),
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
    for _, _, outline, filled, _ in ICON_SPECS:
        for name in (outline, filled):
            if name not in seen:
                seen.add(name)
                names.append(name)
    for name in ("caret-right", "dots-three", "lock-key", "file-text", "clock"):
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
    <filter id="softShadow" x="-40%" y="-40%" width="180%" height="220%"><feDropShadow dx="0" dy="7" stdDeviation="13" flood-color="#273248" flood-opacity="0.12"/></filter>
    {symbols()}
  </defs>
  <title>{html.escape(title)}</title>
'''


def use(master: str, x: float, y: float, size: float, color: str = MUTED, opacity: float = 1) -> str:
    return f'<use href="#i-{master}" x="{x}" y="{y}" width="{size}" height="{size}" color="{color}" opacity="{opacity}"/>'


def semantic_color(key: str) -> str:
    return {
        "running": ACCENT,
        "scheduled": VIOLET,
        "approval": AMBER,
        "clarify": ACCENT,
        "run-now": GREEN,
        "pause": AMBER,
        "stop": RED,
        "enable": ACCENT,
        "success": GREEN,
        "failed": RED,
        "delete": RED,
    }.get(key, ACCENT)


def semantic_bg(key: str) -> str:
    color = semantic_color(key)
    return {GREEN: GREEN_BG, AMBER: AMBER_BG, RED: RED_BG, VIOLET: VIOLET_BG}.get(color, PILL)


def master_sheet() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes task and execution icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">HERMES MOBILE · ROUND 05</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Tasks &amp; execution icons</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Outline for controls · fill for state · animation reserved for active execution</text>')

    card_w, card_h = 166, 346
    start_x, start_y = 82, 238
    for index, (key, label, outline, filled, group) in enumerate(ICON_SPECS):
        row, col = divmod(index, 8)
        x = start_x + col * 180
        y = start_y + row * 372
        color = semantic_color(key)
        chunks.append(f'<g transform="translate({x} {y})">')
        chunks.append(f'<rect width="{card_w}" height="{card_h}" rx="30" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="18" y="34" font-family="Inter,Arial,sans-serif" font-size="11" font-weight="700" letter-spacing="1.2" fill="#8B96A9">{group.upper()}</text>')
        chunks.append('<rect x="18" y="54" width="130" height="142" rx="28" fill="#F6F8FC"/>')
        chunks.append(use(outline, 49, 89, 68, MUTED))
        chunks.append(f'<circle cx="126" cy="176" r="24" fill="{semantic_bg(key)}"/>')
        chunks.append(use(filled, 112, 162, 28, color))
        chunks.append(f'<text x="18" y="236" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="18" y="267" font-family="Inter,Arial,sans-serif" font-size="14" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 18, 292, 24, MUTED))
        chunks.append(f'<text x="54" y="311" font-family="Inter,Arial,sans-serif" font-size="13" fill="{SOFT}">actual size</text>')
        chunks.append('</g>')

    chunks.append('<g transform="translate(82 1002)"><rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    notes = [("Outline", "default control"), ("Fill", "active state"), ("Blue", "navigation / running"), ("Red", "stop / fail / delete")]
    for i, (value, note) in enumerate(notes):
        xx = 34 + i * 350
        chunks.append(f'<circle cx="{xx}" cy="36" r="6" fill="{GREEN}"/><text x="{xx+20}" y="43" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{value}</text><text x="{xx+104}" y="43" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">{note}</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def execution_preview() -> str:
    width, height = 1600, 1140
    chunks = [svg_open(width, height, "Hermes execution center preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · EXECUTION CENTER</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">State is visible before action</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">The page stays quiet; only the active Agent, approval request and next schedule receive color.</text>')

    chunks.append('<g transform="translate(82 232)"><rect width="1436" height="820" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    # header
    chunks.append(f'<text x="38" y="60" font-family="Inter,Arial,sans-serif" font-size="30" font-weight="700" fill="{INK}">Execution center</text>')
    chunks.append(f'<circle cx="48" cy="91" r="6" fill="{GREEN}"/><text x="66" y="98" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">1 Agent is working</text>')
    chunks.append(use("arrows-clockwise", 1272, 43, 32, MUTED))
    chunks.append(f'<circle cx="1364" cy="58" r="34" fill="{ACCENT}"/>')
    chunks.append(use("plus", 1348, 42, 32, "#FFFFFF"))

    # summary strip
    chunks.append('<rect x="34" y="126" width="1368" height="112" rx="30" fill="#F7F9FC"/>')
    metrics = [("1", "Pending", AMBER), ("1", "Running", ACCENT), ("2", "Scheduled", VIOLET), ("8", "History", MUTED)]
    for i, (value, label, color) in enumerate(metrics):
        cx = 205 + i * 342
        if i:
            chunks.append(f'<line x1="{cx-171}" y1="154" x2="{cx-171}" y2="210" stroke="#E2E7EE" stroke-width="2"/>')
        chunks.append(f'<text x="{cx}" y="176" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="32" font-weight="700" fill="{color}">{value}</text>')
        chunks.append(f'<text x="{cx}" y="211" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">{label}</text>')

    # segment control
    chunks.append('<rect x="34" y="266" width="1368" height="72" rx="26" fill="#F2F4F8"/>')
    labels = ["Pending", "Running", "Scheduled", "History"]
    for i, label in enumerate(labels):
        x = 42 + i * 340
        if i == 1:
            chunks.append(f'<rect x="{x}" y="274" width="324" height="56" rx="20" fill="#FFFFFF" filter="url(#softShadow)"/>')
        chunks.append(f'<text x="{x+162}" y="310" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="{700 if i == 1 else 600}" fill="{ACCENT if i == 1 else MUTED}">{label}</text>')

    # active run card
    chunks.append(f'<text x="38" y="388" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">Running now</text>')
    chunks.append('<rect x="34" y="414" width="836" height="322" rx="30" fill="#FFFFFF" stroke="#E6EAF0" stroke-width="2"/>')
    chunks.append(f'<rect x="58" y="442" width="66" height="66" rx="20" fill="{PILL}"/>')
    chunks.append(use("spinner-gap", 75, 459, 32, ACCENT))
    chunks.append(f'<text x="148" y="469" font-family="Inter,Arial,sans-serif" font-size="23" font-weight="700" fill="{INK}">CRM metrics validation</text>')
    chunks.append(f'<text x="148" y="499" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">work · claude-opus-4-8</text>')
    chunks.append('<rect x="58" y="538" width="764" height="8" rx="4" fill="#E7EDF4"/>')
    chunks.append(f'<rect x="58" y="538" width="516" height="8" rx="4" fill="{ACCENT}"/>')
    chunks.append(f'<text x="58" y="583" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">Checking historical conversion-rate definitions…</text>')
    steps = [("check-circle", GREEN, "Read 6 files"), ("check-circle", GREEN, "Normalize metrics"), ("spinner-gap", ACCENT, "Compare variance")]
    for i, (icon, color, label) in enumerate(steps):
        x = 58 + i * 238
        chunks.append(use(icon, x, 626, 22, color))
        chunks.append(f'<text x="{x+30}" y="644" font-family="Inter,Arial,sans-serif" font-size="14" fill="{color}">{label}</text>')
    chunks.append(f'<rect x="756" y="626" width="66" height="66" rx="20" fill="{RED_BG}"/>')
    chunks.append(use("stop-fill", 775, 645, 28, RED))

    # approval card
    chunks.append(f'<text x="912" y="388" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">Needs confirmation</text>')
    chunks.append('<rect x="908" y="414" width="494" height="322" rx="30" fill="#FFFFFF" stroke="#E6EAF0" stroke-width="2"/>')
    chunks.append(f'<rect x="934" y="442" width="66" height="66" rx="20" fill="{AMBER_BG}"/>')
    chunks.append(use("shield-check", 951, 459, 32, AMBER))
    chunks.append(f'<text x="1022" y="468" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" fill="{INK}">Read sales dataset?</text>')
    chunks.append(f'<text x="1022" y="497" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">Required for the validation run</text>')
    chunks.append(f'<text x="934" y="556" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">Read-only access · this run only</text>')
    chunks.append(f'<rect x="934" y="594" width="204" height="62" rx="20" fill="{ACCENT}"/><text x="1036" y="633" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="#FFFFFF">Allow once</text>')
    chunks.append(f'<rect x="1154" y="594" width="218" height="62" rx="20" fill="#F4F6F9"/><text x="1263" y="633" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{MUTED}">Reject</text>')

    # next schedule rail
    chunks.append(f'<rect x="34" y="738" width="1368" height="1" fill="#E9EDF2"/>')
    chunks.append(f'<rect x="50" y="752" width="54" height="54" rx="18" fill="{VIOLET_BG}"/>')
    chunks.append(use("calendar-dots", 64, 766, 26, VIOLET))
    chunks.append(f'<text x="124" y="777" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">Looki daily report</text>')
    chunks.append(f'<text x="124" y="805" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">Tomorrow 09:00 · enabled</text>')
    chunks.append(use("caret-right", 1346, 768, 26, SOFT))
    chunks.append('</g></svg>')
    return "".join(chunks)


def schedule_preview() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes scheduled task actions preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · SCHEDULED TASKS</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">One primary action, three secondary controls</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Run now stays visible; pause, edit and delete remain compact without colored tiles.</text>')

    chunks.append('<g transform="translate(82 232)"><rect width="936" height="790" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<text x="38" y="62" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" letter-spacing="2" fill="{SOFT}">SCHEDULED TASKS</text>')
    cards = [
        ("Looki daily report", "Every day · 09:00", "Tomorrow 09:00", True),
        ("Weekly image reminder", "Sunday · 09:30", "Aug 24 · 09:30", False),
    ]
    for i, (title, schedule, next_run, active) in enumerate(cards):
        y = 98 + i * 300
        chunks.append(f'<rect x="30" y="{y}" width="876" height="270" rx="30" fill="#FFFFFF" stroke="#E6EAF0" stroke-width="2"/>')
        well = VIOLET_BG if active else "#F0F2F6"
        color = VIOLET if active else SOFT
        chunks.append(f'<rect x="54" y="{y+26}" width="68" height="68" rx="21" fill="{well}"/>')
        chunks.append(use("calendar-dots", 72, y + 44, 32, color))
        chunks.append(f'<text x="148" y="{y+53}" font-family="Inter,Arial,sans-serif" font-size="22" font-weight="700" fill="{INK}">{title}</text>')
        chunks.append(f'<text x="148" y="{y+84}" font-family="Inter,Arial,sans-serif" font-size="15" fill="{VIOLET if active else MUTED}">{schedule}</text>')
        # switch representation
        switch_bg = ACCENT if active else "#C4CBD5"
        knob = 847 if active else 813
        chunks.append(f'<rect x="794" y="{y+35}" width="88" height="48" rx="24" fill="{switch_bg}"/>')
        chunks.append(f'<circle cx="{knob}" cy="{y+59}" r="19" fill="#FFFFFF"/>')
        chunks.append(f'<text x="54" y="{y+146}" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">Next {next_run}</text>')
        chunks.append(f'<line x1="54" y1="{y+171}" x2="882" y2="{y+171}" stroke="#EDF0F4" stroke-width="2"/>')
        actions = [("play", GREEN, "Run now"), ("pause", MUTED, "Pause"), ("pencil-simple", MUTED, "Edit"), ("trash-simple", RED, "Delete")]
        for j, (icon, icon_color, label) in enumerate(actions):
            x = 56 + j * 205
            if j == 0:
                chunks.append(f'<rect x="{x}" y="{y+190}" width="176" height="56" rx="19" fill="{GREEN_BG}"/>')
            chunks.append(use(icon, x + 16, y + 205, 26, icon_color))
            chunks.append(f'<text x="{x+54}" y="{y+225}" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" fill="{icon_color}">{label}</text>')
    chunks.append('</g>')

    # state legend / run history
    chunks.append('<g transform="translate(1062 232)"><rect width="456" height="790" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<text x="34" y="62" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" letter-spacing="2" fill="{SOFT}">RESULT STATES</text>')
    rows = [
        ("Succeeded", "check-circle-fill", GREEN, GREEN_BG, "08-17 09:07"),
        ("Running", "spinner-gap", ACCENT, PILL, "2 min"),
        ("Paused", "pause-fill", AMBER, AMBER_BG, "manual"),
        ("Failed", "x-circle-fill", RED, RED_BG, "08-10 09:30"),
    ]
    for i, (label, icon, color, well, meta) in enumerate(rows):
        y = 104 + i * 116
        chunks.append(f'<rect x="28" y="{y}" width="400" height="96" rx="24" fill="#F8FAFC"/>')
        chunks.append(f'<rect x="46" y="{y+18}" width="60" height="60" rx="19" fill="{well}"/>')
        chunks.append(use(icon, 61, y + 33, 30, color))
        chunks.append(f'<text x="128" y="{y+43}" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="700" fill="{INK}">{label}</text>')
        chunks.append(f'<text x="128" y="{y+69}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{meta}</text>')
    chunks.append(f'<line x1="30" y1="602" x2="426" y2="602" stroke="#E9EDF2" stroke-width="2"/>')
    chunks.append(f'<text x="34" y="650" font-family="Inter,Arial,sans-serif" font-size="19" font-weight="700" fill="{INK}">Motion guidance</text>')
    notes = [
        ("Running", "continuous rotation only"),
        ("Switch", "spring slide, no ripple block"),
        ("Segment", "shared sliding indicator"),
    ]
    for i, (label, note) in enumerate(notes):
        yy = 698 + i * 40
        chunks.append(f'<circle cx="42" cy="{yy-5}" r="5" fill="{ACCENT}"/><text x="58" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="14" font-weight="700" fill="{INK}">{label}</text><text x="150" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">{note}</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def copy_icons() -> None:
    ICONS.mkdir(parents=True, exist_ok=True)
    for key, _, outline, filled, _ in ICON_SPECS:
        shutil.copy2(source_path(outline), ICONS / f"{key}-outline.svg")
        shutil.copy2(source_path(filled), ICONS / f"{key}-filled.svg")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "01-task-icon-master.svg").write_text(master_sheet(), encoding="utf-8")
    (OUT / "02-execution-center-preview.svg").write_text(execution_preview(), encoding="utf-8")
    (OUT / "03-scheduled-task-actions.svg").write_text(schedule_preview(), encoding="utf-8")
    copy_icons()


if __name__ == "__main__":
    main()
