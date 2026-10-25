"""Gera a fonte bitmap (formato BMFont .fnt + .png) a partir da DejaVu Sans Bold (licença livre).

Uso: python3 tools/fonte.py assets/fonts
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

OUT = sys.argv[1]
SIZE = 54
STROKE = 4
TTF = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
chars = [chr(c) for c in range(32, 127)] + list("áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÇ♥×•–")

font = ImageFont.truetype(TTF, SIZE)
ascent, descent = font.getmetrics()
glyphs = []
for ch in chars:
    l, t, r, b = font.getbbox(ch, anchor="ls", stroke_width=STROKE)
    w, h = max(1, r - l), max(1, b - t)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.text((-l, -t), ch, font=font, anchor="ls", fill=(255, 255, 255, 255),
           stroke_width=STROKE, stroke_fill=(25, 20, 35, 255))
    adv = int(round(font.getlength(ch))) + 2
    glyphs.append((ch, img, l, t, adv))

W = 1024
x = y = 2
row_h = 0
pos = []
for ch, img, l, t, adv in glyphs:
    if x + img.width + 2 > W:
        x = 2
        y += row_h + 2
        row_h = 0
    pos.append((x, y))
    x += img.width + 2
    row_h = max(row_h, img.height)
H = 1
while H < y + row_h + 2:
    H *= 2
sheet = Image.new("RGBA", (W, H), (0, 0, 0, 0))
for (ch, img, l, t, adv), (px, py) in zip(glyphs, pos):
    sheet.paste(img, (px, py))
os.makedirs(OUT, exist_ok=True)
sheet.save(os.path.join(OUT, "fonte.png"))

base = ascent + STROKE
lines = [
    f'info face="DejaVu Sans Bold" size={SIZE} bold=1 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=2,2',
    f"common lineHeight={ascent + descent + STROKE * 2} base={base} scaleW={W} scaleH={H} pages=1 packed=0",
    'page id=0 file="fonte.png"',
    f"chars count={len(glyphs)}",
]
for (ch, img, l, t, adv), (px, py) in zip(glyphs, pos):
    lines.append(
        f"char id={ord(ch)} x={px} y={py} width={img.width} height={img.height} "
        f"xoffset={l} yoffset={base + t} xadvance={adv} page=0 chnl=15"
    )
open(os.path.join(OUT, "fonte.fnt"), "w").write("\n".join(lines) + "\n")
print("fonte", W, H, len(glyphs))
