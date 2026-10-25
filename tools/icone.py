"""Desenha o ícone do jogo (tatu-bola enrolado com um açaí) — arte própria feita por código.

Uso: python3 tools/icone.py android/res
"""
import os
import sys

from PIL import Image, ImageDraw

RES = sys.argv[1]
S = 432  # 108 dp em xxxhdpi


def draw_ball(d, cx, cy, r):
    # sombra
    d.ellipse([cx - r * 0.95, cy + r * 0.78, cx + r * 0.95, cy + r * 1.02], fill=(20, 60, 40, 90))
    # casco
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(205, 153, 84, 255), outline=(110, 70, 35, 255), width=int(r * 0.07))
    # faixas
    for k in (-0.42, 0.0, 0.42):
        y = cy + k * r
        w = (1 - (k * 1.0) ** 2) ** 0.5 * r
        d.rounded_rectangle([cx - w * 0.98, y - r * 0.08, cx + w * 0.98, y + r * 0.08], radius=r * 0.08, fill=(140, 92, 46, 255))
    # brilho
    d.ellipse([cx - r * 0.62, cy - r * 0.78, cx - r * 0.15, cy - r * 0.48], fill=(236, 196, 128, 255))
    # carinha: olhos e focinho saindo da bola
    for s in (-1, 1):
        ex = cx + s * r * 0.26
        d.ellipse([ex - r * 0.13, cy + r * 0.08, ex + r * 0.13, cy + r * 0.36], fill=(250, 250, 245, 255))
        d.ellipse([ex - r * 0.06, cy + r * 0.16, ex + r * 0.06, cy + r * 0.31], fill=(25, 20, 30, 255))
    d.ellipse([cx - r * 0.13, cy + r * 0.45, cx + r * 0.13, cy + r * 0.62], fill=(240, 150, 140, 255))


def draw_acai(d, cx, cy, r):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(84, 26, 108, 255), outline=(50, 12, 66, 255), width=int(r * 0.12))
    d.ellipse([cx - r * 0.55, cy - r * 0.6, cx - r * 0.1, cy - r * 0.2], fill=(170, 120, 190, 255))
    d.polygon([(cx + r * 0.1, cy - r * 0.9), (cx + r * 0.9, cy - r * 1.6), (cx + r * 0.55, cy - r * 0.7)], fill=(60, 150, 70, 255))


def foreground():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_ball(d, S * 0.47, S * 0.53, S * 0.22)
    draw_acai(d, S * 0.67, S * 0.36, S * 0.065)
    return img


def background():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for y in range(S):
        t = y / S
        c = (int(70 + 40 * (1 - t)), int(170 + 40 * (1 - t)), int(120 + 60 * (1 - t)), 255)
        d.line([(0, y), (S, y)], fill=c)
    d.ellipse([-S * 0.2, S * 0.72, S * 1.2, S * 1.4], fill=(60, 135, 70, 255))
    return img


fg = foreground()
bg = background()
os.makedirs(f"{RES}/drawable-nodpi", exist_ok=True)
fg.save(f"{RES}/drawable-nodpi/ic_launcher_foreground.png")
bg.save(f"{RES}/drawable-nodpi/ic_launcher_background.png")

full = bg.copy()
full.alpha_composite(fg)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle([S * 0.09, S * 0.09, S * 0.91, S * 0.91], radius=S * 0.18, fill=255)
legacy = Image.new("RGBA", (S, S), (0, 0, 0, 0))
legacy.paste(full, (0, 0), mask)
legacy = legacy.crop((int(S * 0.09), int(S * 0.09), int(S * 0.91), int(S * 0.91)))
for name, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    os.makedirs(f"{RES}/mipmap-{name}", exist_ok=True)
    legacy.resize((px, px), Image.LANCZOS).save(f"{RES}/mipmap-{name}/ic_launcher.png")

os.makedirs(f"{RES}/mipmap-anydpi-v26", exist_ok=True)
open(f"{RES}/mipmap-anydpi-v26/ic_launcher.xml", "w").write(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '    <background android:drawable="@drawable/ic_launcher_background" />\n'
    '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
    '</adaptive-icon>\n'
)
legacy.resize((512, 512), Image.LANCZOS).save("jogo/icone.png")
print("ok")
