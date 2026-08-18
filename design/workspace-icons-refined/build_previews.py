#!/usr/bin/env python3
"""Build refined workspace/file icon presentation boards."""

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
CYAN = "#22BDB4"
INK = "#1D2637"
MUTED = "#667287"
SOFT = "#8A95A7"
BG = "#F3F5F9"
PILL = "#E6F4FD"
GREEN = "#24B98A"
GREEN_BG = "#E2F7F0"
AMBER = "#E79A20"
AMBER_BG = "#FFF2D7"
RED = "#EF5362"
RED_BG = "#FFE8EC"
VIOLET = "#7564E8"
VIOLET_BG = "#EEEAFE"


ICON_SPECS = [
    ("recent-artifacts", "Recent artifacts", "clock-counter-clockwise", "clock-counter-clockwise-fill", "Navigation"),
    ("project-files", "Project files", "folders", "folders-fill", "Navigation"),
    ("folder", "Folder", "folder", "folder-fill", "File type"),
    ("folder-open", "Open folder", "folder-open", "folder-open-fill", "File type"),
    ("markdown", "Markdown", "file-md", "file-md-fill", "File type"),
    ("image", "Image", "file-image", "file-image-fill", "File type"),
    ("web", "HTML / web", "file-html", "file-html-fill", "File type"),
    ("file", "Generic file", "file", "file-fill", "File type"),
    ("source-chat", "Source chat", "chat-circle-dots", "chat-circle-dots-fill", "Context"),
    ("preview", "Preview", "eye", "eye-fill", "Document"),
    ("edit", "Edit", "pencil-simple", "pencil-simple-fill", "Document"),
    ("save", "Save", "floppy-disk", "floppy-disk-fill", "Document"),
    ("copy", "Copy path", "copy", "copy-fill", "Document"),
    ("share", "Share", "arrow-square-out", "arrow-square-out-fill", "Document"),
    ("download", "Save to phone", "download-simple", "download-simple-fill", "Document"),
    ("delete", "Delete", "trash-simple", "trash-simple-fill", "Document"),
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
            if master not in seen:
                seen.add(master)
                masters.append(
                    f'<symbol id="i-{master}" viewBox="0 0 256 256" fill="currentColor">{icon_inner(master)}</symbol>'
                )
    for master in ("caret-right", "arrow-left", "check", "dots-three", "magnifying-glass"):
        if master not in seen:
            masters.append(
                f'<symbol id="i-{master}" viewBox="0 0 256 256" fill="currentColor">{icon_inner(master)}</symbol>'
            )
    return "".join(masters)


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
        "folder": AMBER,
        "folder-open": AMBER,
        "markdown": GREEN,
        "image": VIOLET,
        "web": ACCENT,
        "source-chat": ACCENT,
        "save": GREEN,
        "delete": RED,
    }.get(key, ACCENT)


def semantic_bg(key: str) -> str:
    color = semantic_color(key)
    return {AMBER: AMBER_BG, GREEN: GREEN_BG, VIOLET: VIOLET_BG, RED: RED_BG}.get(color, PILL)


def master_sheet() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes workspace and file icon masters")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">HERMES MOBILE · ROUND 04</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Workspace &amp; file icons</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">One closed silhouette per meaning · quiet file colors · clean at actual size</text>')

    card_w, card_h = 166, 346
    gap_x, gap_y = 14, 26
    start_x, start_y = 82, 238
    for index, (key, label, outline, filled, group) in enumerate(ICON_SPECS):
        row, col = divmod(index, 8)
        x = start_x + col * (card_w + gap_x)
        y = start_y + row * (card_h + gap_y)
        color = semantic_color(key)
        chunks.append(f'<g transform="translate({x} {y})">')
        chunks.append(f'<rect width="{card_w}" height="{card_h}" rx="30" fill="#FFFFFF" filter="url(#shadow)"/>')
        chunks.append(f'<text x="18" y="34" font-family="Inter,Arial,sans-serif" font-size="11" font-weight="700" letter-spacing="1.2" fill="#8B96A9">{html.escape(group.upper())}</text>')
        chunks.append('<rect x="18" y="54" width="130" height="142" rx="28" fill="#F6F8FC"/>')
        chunks.append(use(outline, 49, 89, 68, MUTED))
        chunks.append(f'<circle cx="126" cy="176" r="24" fill="{semantic_bg(key)}"/>')
        chunks.append(use(filled, 112, 162, 28, color))
        chunks.append(f'<text x="18" y="236" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" fill="{INK}">{html.escape(label)}</text>')
        chunks.append(f'<text x="18" y="267" font-family="Inter,Arial,sans-serif" font-size="14" fill="{SOFT}">24 px master</text>')
        chunks.append(use(outline, 18, 292, 24, MUTED))
        chunks.append(f'<text x="54" y="311" font-family="Inter,Arial,sans-serif" font-size="13" fill="{SOFT}">actual size</text>')
        chunks.append('</g>')

    chunks.append('<g transform="translate(82 1002)">')
    chunks.append('<rect width="1436" height="72" rx="28" fill="#FFFFFF" filter="url(#softShadow)"/>')
    notes = [("16–20 px", "list glyph"), ("24 px", "action glyph"), ("1 shape", "per meaning"), ("red only", "for delete")]
    for i, (value, note) in enumerate(notes):
        xx = 34 + i * 350
        chunks.append(f'<circle cx="{xx}" cy="36" r="6" fill="{CYAN}"/><text x="{xx+20}" y="43" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">{value}</text><text x="{xx+112}" y="43" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">{note}</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def list_preview() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes refined workspace list preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · WORKSPACE</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">File type first, source second</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Type color lives in the small icon well; the source-chat action stays singular and unmistakable.</text>')

    # Mobile-like artifact list panel.
    chunks.append('<g transform="translate(82 232)"><rect width="906" height="790" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append('<rect x="34" y="28" width="838" height="72" rx="26" fill="#F1F4F8"/>')
    chunks.append(f'<rect x="42" y="36" width="405" height="56" rx="20" fill="#FFFFFF" filter="url(#softShadow)"/>')
    chunks.append(f'<text x="244" y="73" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{ACCENT}">Recent artifacts</text>')
    chunks.append(f'<text x="660" y="73" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="600" fill="{MUTED}">Project files</text>')
    chunks.append(f'<text x="38" y="146" font-family="Inter,Arial,sans-serif" font-size="15" font-weight="700" letter-spacing="2" fill="{SOFT}">RECENT ARTIFACTS</text>')

    rows = [
        ("2026-08-17 · Weekly report.md", "Markdown · from Weekly report progress", "file-md", GREEN, GREEN_BG),
        ("Looki cover image.jpg", "Image · from Looki daily", "file-image", VIOLET, VIOLET_BG),
        ("CRM dashboard prototype.html", "Web page · from CRM dashboard redesign", "file-html", ACCENT, PILL),
        ("Campaign assets", "Folder · workspace / marketing", "folder", AMBER, AMBER_BG),
    ]
    for i, (name, subtitle, icon, color, well) in enumerate(rows):
        y = 172 + i * 140
        chunks.append(f'<rect x="30" y="{y}" width="846" height="120" rx="24" fill="#FFFFFF"/>')
        chunks.append(f'<rect x="46" y="{y+24}" width="70" height="70" rx="20" fill="{well}"/>')
        chunks.append(use(icon, 65, y + 43, 32, color))
        chunks.append(f'<text x="142" y="{y+50}" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">{html.escape(name)}</text>')
        chunks.append(f'<text x="142" y="{y+82}" font-family="Inter,Arial,sans-serif" font-size="16" fill="{MUTED}">{html.escape(subtitle)}</text>')
        if i < 3:
            # Single closed source-conversation glyph, not a doubled bubble.
            chunks.append(f'<rect x="796" y="{y+29}" width="56" height="56" rx="18" fill="{PILL}"/>')
            chunks.append(use("chat-circle-dots", 811, y + 44, 26, ACCENT))
        else:
            chunks.append(use("caret-right", 811, y + 45, 26, SOFT))
        chunks.append(f'<line x1="142" y1="{y+118}" x2="852" y2="{y+118}" stroke="#EDF0F4" stroke-width="2"/>')
    chunks.append('</g>')

    # Audit panel.
    chunks.append('<g transform="translate(1034 232)"><rect width="484" height="790" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    chunks.append(f'<text x="34" y="60" font-family="Inter,Arial,sans-serif" font-size="16" font-weight="700" letter-spacing="2" fill="{SOFT}">ICON AUDIT</text>')
    chunks.append(f'<text x="34" y="112" font-family="Inter,Arial,sans-serif" font-size="25" font-weight="700" fill="{INK}">Source conversation</text>')
    chunks.append(f'<rect x="34" y="146" width="416" height="136" rx="28" fill="#F7F9FC"/>')
    chunks.append(f'<rect x="60" y="178" width="72" height="72" rx="22" fill="{PILL}"/>')
    chunks.append(use("chat-circle-dots", 79, 197, 34, ACCENT))
    chunks.append(f'<text x="158" y="207" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{INK}">One closed bubble</text>')
    chunks.append(f'<text x="158" y="239" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">No overlap · no broken tail</text>')
    audit = [
        ("20 px", "file-type glyph"),
        ("36–40 px", "quiet icon well"),
        ("Blue", "navigation / source"),
        ("Red", "delete only"),
    ]
    for i, (value, note) in enumerate(audit):
        yy = 350 + i * 82
        chunks.append(f'<text x="36" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="24" font-weight="700" fill="{ACCENT_DARK}">{value}</text>')
        chunks.append(f'<text x="178" y="{yy}" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">{note}</text>')
    chunks.append(f'<rect x="34" y="690" width="416" height="68" rx="22" fill="{PILL}"/>')
    chunks.append(use("eye", 58, 710, 28, ACCENT))
    chunks.append(f'<text x="106" y="733" font-family="Inter,Arial,sans-serif" font-size="18" font-weight="700" fill="{ACCENT_DARK}">Preview before external open</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def document_preview() -> str:
    width, height = 1600, 1120
    chunks = [svg_open(width, height, "Hermes refined document actions preview")]
    chunks.append(f'<rect width="{width}" height="{height}" fill="{BG}"/>')
    chunks.append(f'<text x="82" y="86" font-family="Inter,Arial,sans-serif" font-size="20" font-weight="700" letter-spacing="5" fill="{ACCENT}">IN-CONTEXT · DOCUMENT</text>')
    chunks.append(f'<text x="82" y="144" font-family="Inter,Arial,sans-serif" font-size="44" font-weight="700" fill="{INK}">Calm top bar, explicit document actions</text>')
    chunks.append('<text x="82" y="184" font-family="Inter,Arial,sans-serif" font-size="21" fill="#7A869A">Primary action is Edit or Save; copy, share and download remain compact and secondary.</text>')

    chunks.append('<g transform="translate(82 232)"><rect width="1436" height="790" rx="48" fill="#FFFFFF" filter="url(#shadow)"/>')
    # top bar
    chunks.append(use("arrow-left", 34, 34, 34, MUTED))
    chunks.append(f'<text x="92" y="55" font-family="Inter,Arial,sans-serif" font-size="24" font-weight="700" fill="{INK}">2026-08-17 · Weekly report.md</text>')
    chunks.append(f'<text x="92" y="86" font-family="Inter,Arial,sans-serif" font-size="15" fill="{MUTED}">Markdown preview · work profile</text>')
    # secondary toolbar
    actions = [("copy", "Copy path"), ("arrow-square-out", "Share"), ("download-simple", "Save to phone")]
    for i, (icon, label) in enumerate(actions):
        x = 958 + i * 118
        chunks.append(f'<rect x="{x}" y="26" width="100" height="74" rx="22" fill="#F5F7FA"/>')
        chunks.append(use(icon, x + 35, 36, 30, MUTED))
        chunks.append(f'<text x="{x+50}" y="88" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="11" fill="{SOFT}">{label}</text>')
    chunks.append(f'<rect x="1314" y="26" width="88" height="74" rx="24" fill="{PILL}"/>')
    chunks.append(use("pencil-simple-fill", 1342, 44, 32, ACCENT))

    # source row
    chunks.append(f'<rect x="34" y="128" width="1368" height="82" rx="22" fill="#F7FAFD"/>')
    chunks.append(f'<rect x="52" y="143" width="52" height="52" rx="16" fill="{PILL}"/>')
    chunks.append(use("chat-circle-dots", 66, 157, 24, ACCENT))
    chunks.append(f'<text x="124" y="166" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">From Weekly report progress</text>')
    chunks.append(f'<text x="124" y="192" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">Open the generating message</text>')
    chunks.append(use("caret-right", 1354, 154, 28, SOFT))

    # markdown body
    chunks.append(f'<text x="56" y="270" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" letter-spacing="2" fill="{SOFT}">MARKDOWN PREVIEW</text>')
    chunks.append(f'<text x="56" y="330" font-family="Inter,Arial,sans-serif" font-size="32" font-weight="700" fill="{INK}">Weekly progress · August 17</text>')
    chunks.append(f'<text x="56" y="382" font-family="Inter,Arial,sans-serif" font-size="20" fill="{MUTED}">This week focused on the sales pilot, data access and delivery criteria.</text>')
    sections = [
        ("01", "Pilot scope confirmed", "One sales team, two customer journeys and a seven-day observation window."),
        ("02", "Success metrics aligned", "Response time, qualified-lead rate and assisted handoff are now measurable."),
        ("03", "Next decision", "Confirm the data owner before Monday's review."),
    ]
    for i, (number, title, body) in enumerate(sections):
        y = 440 + i * 102
        chunks.append(f'<circle cx="75" cy="{y}" r="20" fill="{PILL}"/><text x="75" y="{y+6}" text-anchor="middle" font-family="Inter,Arial,sans-serif" font-size="13" font-weight="700" fill="{ACCENT}">{number}</text>')
        chunks.append(f'<text x="112" y="{y-5}" font-family="Inter,Arial,sans-serif" font-size="21" font-weight="700" fill="{INK}">{title}</text>')
        chunks.append(f'<text x="112" y="{y+29}" font-family="Inter,Arial,sans-serif" font-size="17" fill="{MUTED}">{body}</text>')

    # save state callout
    chunks.append(f'<rect x="1020" y="656" width="382" height="92" rx="26" fill="{GREEN_BG}"/>')
    chunks.append(f'<circle cx="1064" cy="702" r="22" fill="{GREEN}"/>')
    chunks.append(use("check", 1052, 690, 24, "#FFFFFF"))
    chunks.append(f'<text x="1102" y="695" font-family="Inter,Arial,sans-serif" font-size="17" font-weight="700" fill="{INK}">Saved</text>')
    chunks.append(f'<text x="1102" y="722" font-family="Inter,Arial,sans-serif" font-size="14" fill="{MUTED}">No duplicate status card</text>')
    chunks.append('</g></svg>')
    return "".join(chunks)


def write_file(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def copy_icons() -> None:
    ICONS.mkdir(parents=True, exist_ok=True)
    for key, _, outline, filled, _ in ICON_SPECS:
        shutil.copy2(source_path(outline), ICONS / f"{key}-outline.svg")
        shutil.copy2(source_path(filled), ICONS / f"{key}-filled.svg")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    write_file(OUT / "01-workspace-icon-master.svg", master_sheet())
    write_file(OUT / "02-artifact-list-preview.svg", list_preview())
    write_file(OUT / "03-document-actions-preview.svg", document_preview())
    copy_icons()


if __name__ == "__main__":
    main()
