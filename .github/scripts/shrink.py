"""Downscale emulator screenshots so they are quick to fetch."""
import pathlib
import sys

from PIL import Image

root = pathlib.Path(sys.argv[1])
for p in sorted(root.glob("*.png")):
    try:
        im = Image.open(p).convert("RGB")
        w, h = im.size
        if w > 600:
            im = im.resize((540, int(h * 540 / w)), Image.LANCZOS)
        im.save(p.with_suffix(".jpg"), quality=86)
        p.unlink()
    except Exception as e:  # keep going
        print("skip", p, e)
