#!/usr/bin/env python3
"""Regenerates every launcher/logo asset from assets/SkyPulse_logo.png (needs: pip install pillow).
Run from the project root:  python3 tools/make_icons.py"""
import os
from PIL import Image, ImageDraw

SRC = "assets/SkyPulse_logo.png"
RES = "app/src/main/res"
src = Image.open(SRC).convert("RGB")

for density, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    d = f"{RES}/mipmap-{density}"
    os.makedirs(d, exist_ok=True)
    im = src.resize((size, size), Image.LANCZOS)
    im.save(f"{d}/ic_launcher.png")
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    round_im = im.convert("RGBA")
    round_im.putalpha(mask.resize((size, size), Image.LANCZOS))
    round_im.save(f"{d}/ic_launcher_round.png")

# Adaptive icon foreground: 432 px canvas (108 dp @ xxxhdpi); artwork kept inside the 66 % safe zone.
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
fg.paste(src.resize((286, 286), Image.LANCZOS).convert("RGBA"), (73, 73))
os.makedirs(f"{RES}/drawable", exist_ok=True)
fg.save(f"{RES}/drawable/ic_launcher_foreground.png")

os.makedirs(f"{RES}/drawable-nodpi", exist_ok=True)
src.resize((256, 256), Image.LANCZOS).save(f"{RES}/drawable-nodpi/skypulse_logo.png")
print("icons regenerated")
