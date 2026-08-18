#!/usr/bin/env python3
"""Convert the reviewed 256×256 SVG icon masters to Android VectorDrawables."""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable"

FAMILIES = {
    "nav": ROOT / "design" / "nav-icons-refined" / "icons",
    "conversation": ROOT / "design" / "conversation-icons-refined" / "icons",
    "session": ROOT / "design" / "session-icons-refined" / "icons",
    "workspace": ROOT / "design" / "workspace-icons-refined" / "icons",
    "task": ROOT / "design" / "task-icons-refined" / "icons",
    "profile": ROOT / "design" / "profile-settings-icons-refined" / "icons",
    "common": ROOT / "design" / "common-control-icons-refined" / "icons",
    "utility": ROOT / "design" / "remaining-icons-refined" / "icons",
}

ANDROID_NS = "http://schemas.android.com/apk/res/android"
SVG_NS = "http://www.w3.org/2000/svg"
ET.register_namespace("android", ANDROID_NS)


def safe_name(value: str) -> str:
    return re.sub(r"[^a-z0-9_]+", "_", value.lower().replace("-", "_")).strip("_")


def asset_identity(family: str, base: Path, path: Path) -> tuple[str, str]:
    relative = path.relative_to(base)
    if len(relative.parts) == 2:
        style = relative.parts[0]
        key = relative.stem
    else:
        match = re.fullmatch(r"(.+)-(outline|filled)", relative.stem)
        if not match:
            raise ValueError(f"Icon name must end in -outline or -filled: {path}")
        key, style = match.groups()
    return safe_name(key), safe_name(style)


def vector_xml(svg_path: Path) -> ET.Element:
    svg = ET.parse(svg_path).getroot()
    view_box = svg.attrib.get("viewBox", "").split()
    if view_box != ["0", "0", "256", "256"]:
        raise ValueError(f"Unsupported viewBox in {svg_path}: {view_box}")

    vector = ET.Element(
        "vector",
        {
            f"{{{ANDROID_NS}}}width": "24dp",
            f"{{{ANDROID_NS}}}height": "24dp",
            f"{{{ANDROID_NS}}}viewportWidth": "256",
            f"{{{ANDROID_NS}}}viewportHeight": "256",
        },
    )
    paths = svg.findall(f"{{{SVG_NS}}}path")
    if not paths:
        raise ValueError(f"No path data in {svg_path}")
    for source_path in paths:
        path_data = source_path.attrib.get("d")
        if not path_data:
            raise ValueError(f"Empty path in {svg_path}")
        ET.SubElement(
            vector,
            "path",
            {
                f"{{{ANDROID_NS}}}fillColor": "#FF000000",
                f"{{{ANDROID_NS}}}pathData": path_data,
            },
        )
    return vector


def write_vector(destination: Path, vector: ET.Element) -> None:
    body = ET.tostring(vector, encoding="unicode", short_empty_elements=True)
    destination.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + body + "\n", encoding="utf-8")


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    generated: set[Path] = set()
    for family, base in FAMILIES.items():
        for svg_path in sorted(base.rglob("*.svg")):
            key, style = asset_identity(family, base, svg_path)
            destination = OUTPUT / f"hermes_refined_{family}_{key}_{style}.xml"
            if destination in generated:
                raise ValueError(f"Duplicate output name: {destination.name}")
            write_vector(destination, vector_xml(svg_path))
            generated.add(destination)

    stale = set(OUTPUT.glob("hermes_refined_*.xml")) - generated
    if stale:
        names = ", ".join(sorted(path.name for path in stale))
        raise RuntimeError(f"Stale generated resources found; remove explicitly: {names}")
    print(f"Generated {len(generated)} refined VectorDrawable resources in {OUTPUT}")


if __name__ == "__main__":
    main()
