#!/usr/bin/env python3
"""Minimal SVG-to-PNG renderer using the system librsvg/cairo libraries."""

from __future__ import annotations

import ctypes
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


class RsvgRectangle(ctypes.Structure):
    _fields_ = [
        ("x", ctypes.c_double),
        ("y", ctypes.c_double),
        ("width", ctypes.c_double),
        ("height", ctypes.c_double),
    ]


def render(source: Path, target: Path) -> None:
    root = ET.parse(source).getroot()
    if "width" in root.attrib and "height" in root.attrib:
        width = int(float(root.attrib["width"]))
        height = int(float(root.attrib["height"]))
    else:
        view_box = root.attrib.get("viewBox", "0 0 256 256").split()
        width = int(float(view_box[2]))
        height = int(float(view_box[3]))

    rsvg = ctypes.CDLL("librsvg-2.so.2")
    cairo = ctypes.CDLL("libcairo.so.2")
    gobject = ctypes.CDLL("libgobject-2.0.so.0")

    rsvg.rsvg_handle_new_from_file.argtypes = [ctypes.c_char_p, ctypes.POINTER(ctypes.c_void_p)]
    rsvg.rsvg_handle_new_from_file.restype = ctypes.c_void_p
    rsvg.rsvg_handle_render_document.argtypes = [
        ctypes.c_void_p,
        ctypes.c_void_p,
        ctypes.POINTER(RsvgRectangle),
        ctypes.POINTER(ctypes.c_void_p),
    ]
    rsvg.rsvg_handle_render_document.restype = ctypes.c_bool

    cairo.cairo_image_surface_create.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_int]
    cairo.cairo_image_surface_create.restype = ctypes.c_void_p
    cairo.cairo_create.argtypes = [ctypes.c_void_p]
    cairo.cairo_create.restype = ctypes.c_void_p
    cairo.cairo_surface_write_to_png.argtypes = [ctypes.c_void_p, ctypes.c_char_p]
    cairo.cairo_surface_write_to_png.restype = ctypes.c_int
    cairo.cairo_destroy.argtypes = [ctypes.c_void_p]
    cairo.cairo_surface_destroy.argtypes = [ctypes.c_void_p]
    gobject.g_object_unref.argtypes = [ctypes.c_void_p]

    error = ctypes.c_void_p()
    handle = rsvg.rsvg_handle_new_from_file(str(source).encode(), ctypes.byref(error))
    if not handle:
        raise RuntimeError(f"Could not parse {source}")

    surface = cairo.cairo_image_surface_create(0, width, height)  # CAIRO_FORMAT_ARGB32
    context = cairo.cairo_create(surface)
    viewport = RsvgRectangle(0.0, 0.0, float(width), float(height))
    ok = rsvg.rsvg_handle_render_document(handle, context, ctypes.byref(viewport), ctypes.byref(error))
    if not ok:
        raise RuntimeError(f"Could not render {source}")
    status = cairo.cairo_surface_write_to_png(surface, str(target).encode())
    if status != 0:
        raise RuntimeError(f"Could not write {target}; cairo status {status}")

    cairo.cairo_destroy(context)
    cairo.cairo_surface_destroy(surface)
    gobject.g_object_unref(handle)


if __name__ == "__main__":
    for source_name in sys.argv[1:]:
        source_path = Path(source_name).resolve()
        render(source_path, source_path.with_suffix(".png"))
