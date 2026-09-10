"""Export the approved 9:16 welcome video for Android. Requires ffmpeg on PATH."""
import argparse
import subprocess
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('input', type=Path, help='Approved source MP4')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
raw = root / 'app/src/main/res/raw/hermes_intro.mp4'
poster = root / 'app/src/main/res/drawable-nodpi/hermes_intro_poster.jpg'
raw.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(['ffmpeg', '-y', '-i', str(args.input), '-vf',
    'scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,fps=30',
    '-an', '-c:v', 'libx264', '-preset', 'medium', '-crf', '22', '-pix_fmt', 'yuv420p',
    '-movflags', '+faststart', str(raw)], check=True)
subprocess.run(['ffmpeg', '-y', '-i', str(raw), '-frames:v', '1', '-q:v', '2', str(poster)], check=True)
print(f'Wrote {raw.name} ({raw.stat().st_size:,} bytes) and {poster.name}')
