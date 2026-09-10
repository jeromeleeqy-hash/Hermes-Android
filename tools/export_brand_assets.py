"""Export approved artwork to Android raster resources without redesigning it."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'design/brand-3.6.2'
RES = ROOT / 'app/src/main/res'

def export(source, destination, size, **options):
    image = Image.open(SOURCE / source).convert('RGB')
    image.resize((size, size), Image.Resampling.LANCZOS).save(RES / destination, **options)

export('launcher-selected.png', 'drawable-nodpi/hermes_launcher_assistant.png', 640, optimize=True)
export('launcher-selected.png', 'drawable-xxxhdpi/hermes_app_icon_art.png', 512, optimize=True)
export('launcher-selected.png', 'drawable-xxxhdpi/hermes_launcher_foreground_art.png', 432, optimize=True)
export('launcher-selected.png', 'drawable-nodpi/hermes_icon_art.webp', 512, quality=95)
export('hermes-avatar-selected.png', 'drawable-nodpi/hermes_default_avatar.png', 512, optimize=True)
export('user-avatar-default.png', 'drawable-nodpi/fixed_user_avatar.jpg', 512, quality=96, subsampling=0)
print('Exported launcher art, Hermes avatar and generic user avatar.')
