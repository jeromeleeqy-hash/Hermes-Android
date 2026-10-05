#!/usr/bin/env python3
"""Convert the approved 2026-10-05 portrait videos into transparent app assets.

Requires ffmpeg, Pillow, numpy and scipy. Input: numbered 1.mp4 ... 7.mp4.
4.mp4 duplicates 3.mp4's decoded video and is deliberately not packaged.
Background removal is restricted to pixels connected to the canvas boundary;
dark clothing and the interior of the headphones are never globally keyed out.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage as ndi

CLIPS = {1: "breathe", 2: "greet", 3: "listen", 5: "think", 6: "nod", 7: "wink"}
SIDE = 576
OUTPUT = 320
FPS = 20


def frames(path: Path, fps: int = FPS, limit: int | None = None):
    command = ["ffmpeg", "-v", "error", "-threads", "2", "-i", str(path), "-an",
               "-vf", f"fps={fps},scale={SIDE}:{SIDE}:flags=lanczos", "-f", "rawvideo", "-pix_fmt", "rgb24", "-"]
    if limit:
        command[-1:-1] = ["-frames:v", str(limit)]
    process = subprocess.Popen(command, stdout=subprocess.PIPE)
    try:
        while True:
            raw = process.stdout.read(SIDE * SIDE * 3)
            if not raw:
                break
            if len(raw) != SIDE * SIDE * 3:
                raise RuntimeError("Truncated video frame")
            yield np.frombuffer(raw, np.uint8).reshape(SIDE, SIDE, 3).copy()
    finally:
        process.stdout.close()
        process.wait()
    if process.returncode:
        raise RuntimeError(f"ffmpeg failed for {path.name}")


def remove_background(rgb: np.ndarray) -> Image.Image:
    color = rgb.astype(np.float32)
    # All supplied videos have a neutral, uniform background. Read the top corners
    # rather than the bottom boundary, which contains the cropped upper chest.
    samples = np.concatenate((color[:12, :12].reshape(-1, 3), color[:12, -12:].reshape(-1, 3)))
    background = np.median(samples, axis=0)
    difference = np.max(np.abs(color - background), axis=2)
    candidates = difference < (19 if background.mean() > 60 else 50)
    seed = np.zeros(candidates.shape, bool)
    seed[0] = candidates[0]
    seed[:, 0] = candidates[:, 0]
    seed[:, -1] = candidates[:, -1]
    seed[-1] = candidates[-1]
    outside = ndi.binary_propagation(seed, mask=candidates)
    mask = ~outside
    if background.mean() < 60:
        # The black jacket touches the bottom edge and shares the backdrop's
        # colour. Preserve the inside of this single, front-facing bust silhouette
        # rather than letting flood fill leak through seams into the clothing.
        labels, count = ndi.label(mask)
        sizes = np.bincount(labels.ravel()); sizes[0] = 0
        mask = sizes[labels] >= 12
        for row in mask:
            occupied = np.flatnonzero(row)
            if occupied.size:
                row[occupied[0]:occupied[-1]+1] = True
        # Compression can make one horizontal row of a dark sleeve disappear.
        # Stabilise only the torso outline, leaving the fine hair silhouette alone.
        lower = int(mask.shape[0] * .68)
        left = np.argmax(mask, axis=1)
        right = mask.shape[1] - 1 - np.argmax(mask[:, ::-1], axis=1)
        left = ndi.median_filter(left, size=7)
        right = ndi.median_filter(right, size=7)
        for y in range(lower, mask.shape[0]):
            mask[y] = False
            mask[y, left[y]:right[y]+1] = True
    mask = ndi.binary_fill_holes(mask)
    # Estimate edge coverage using the nearest opaque foreground colour, and
    # unpremultiply it against the measured background to avoid black/grey halos.
    core = ndi.binary_erosion(mask, iterations=2, border_value=1)
    if background.mean() < 60:
        lower = int(mask.shape[0] * .68)
        core[lower:] = ndi.binary_erosion(mask, structure=np.ones((1,5)), border_value=1)[lower:]
    distance, indices = ndi.distance_transform_edt(~core, return_indices=True)
    foreground = color[indices[0], indices[1]]
    direction = foreground - background
    alpha = np.sum((color - background) * direction, axis=2) / np.maximum(np.sum(direction ** 2, axis=2), 1)
    alpha = np.clip(alpha, 0, 1)
    alpha[core] = 1
    alpha[(distance > 5) | ~mask] = 0
    alpha[alpha < .07] = 0
    restored = np.clip((color - background * (1 - alpha[..., None])) / np.maximum(alpha[..., None], .07), 0, 255)
    rgba = np.dstack((restored, alpha * 255)).astype(np.uint8)
    return Image.fromarray(rgba).resize((OUTPUT, OUTPUT), Image.Resampling.LANCZOS)


def premultiplied_blend(a: Image.Image, b: Image.Image, amount: float) -> Image.Image:
    first = np.asarray(a, dtype=np.float32) / 255
    second = np.asarray(b, dtype=np.float32) / 255
    alpha = first[..., 3:] * (1 - amount) + second[..., 3:] * amount
    color = first[..., :3] * first[..., 3:] * (1 - amount) + second[..., :3] * second[..., 3:] * amount
    rgba = np.concatenate((color / np.maximum(alpha, .001), alpha), axis=2)
    return Image.fromarray(np.clip(rgba * 255, 0, 255).astype(np.uint8))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--preview", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.preview:
        sheet = Image.new("RGB", (6 * 256, 3 * 280), "#eeeeee")
        draw = ImageDraw.Draw(sheet)
        for x, (number, name) in enumerate(CLIPS.items()):
            samples = list(frames(args.input / f"{number}.mp4", fps=1))
            for y, index in enumerate([0, len(samples)//2, len(samples)-1]):
                cutout = remove_background(samples[index])
                cutout.save(args.output / f"sample-{number}-{y}.png")
                background = Image.new("RGBA", cutout.size, ["#fff8ef", "#1f252b", "#dfeeea"][y])
                background.alpha_composite(cutout)
                sheet.paste(background.convert("RGB").resize((256,256)), (x*256,y*280+24))
                draw.text((x*256+6,y*280+6), f"{number} {name} / {index}s", fill="black")
        sheet.save(args.output / "matting-review.jpg", quality=94)
        return
    neutral = remove_background(list(frames(args.input / "1.mp4", limit=1))[0])
    neutral.save(args.output / "home_portrait_poster.png")
    manifest = []
    for number, name in CLIPS.items():
        source = args.input / f"{number}.mp4"
        animation = [remove_background(frame) for frame in frames(source)]
        # A short shared neutral pose at both ends gives all clips the same anchor.
        # Brief premultiplied dissolves retain the original forward-only motion.
        transition = 5
        entry = [premultiplied_blend(neutral, animation[0], k/transition) for k in range(transition)]
        exit_frames = [premultiplied_blend(animation[-1], neutral, (k+1)/transition) for k in range(transition)]
        animation = entry + animation + exit_frames + [neutral]
        path = args.output / f"home_portrait_{name}.webp"
        animation[0].save(path, save_all=True, append_images=animation[1:], duration=1000//FPS,
                          loop=1, quality=84, method=4, minimize_size=False, allow_mixed=False)
        with Image.open(path) as decoded:
            duration = 0
            for i in range(decoded.n_frames):
                decoded.seek(i); decoded.load()
                duration += decoded.info.get("duration", 0)
            assert decoded.size == (OUTPUT, OUTPUT)
        item = {"source":source.name,"sourceSha256":hashlib.sha256(source.read_bytes()).hexdigest(),
                "asset":path.name,"bytes":path.stat().st_size,"durationMs":duration,"frames":decoded.n_frames,"timelineFrames":len(animation)}
        manifest.append(item)
        print(json.dumps(item), flush=True)
    (args.output / "portrait-assets.json").write_text(json.dumps(manifest, indent=2)+"\n")


if __name__ == "__main__":
    main()
