# -*- coding: utf-8 -*-
"""把选中态试验台的两段截图拼成一张对照图，方便在手机上一眼看完。

两张源图分别是"形状一览表头对着屏幕顶部"和"填充一览表头对着屏幕顶部"的截图，
所以各自裁掉顶部空白、再自动裁掉底部的空白，拼起来就是完整的两节。
"""
from pathlib import Path

from PIL import Image, ImageDraw

SHOTS = Path(__file__).resolve().parents[2] / "docs" / "screenshots"

BG = (248, 249, 250)  # MaterialTheme.colorScheme.background
TOP_PAD = 40
BOTTOM_LIMIT = 2300  # 避开底部的手势条（白色药丸在 y≈2360，会把裁剪探针骗过去）
SAMPLE_STEP = 13


def trim_bottom(img: Image.Image, tol: int = 8) -> Image.Image:
    """从下往上找到第一行有内容的像素，把下面整片背景裁掉。"""
    px = img.load()
    w, h = img.size
    for y in range(h - 1, -1, -1):
        for x in range(0, int(w * 0.75), SAMPLE_STEP):
            r, g, b = px[x, y]
            if abs(r - BG[0]) > tol or abs(g - BG[1]) > tol or abs(b - BG[2]) > tol:
                return img.crop((0, 0, w, y + 1))
    return img


def prepare(name: str) -> Image.Image:
    img = Image.open(SHOTS / name).convert("RGB")
    part = img.crop((0, TOP_PAD, img.width, min(BOTTOM_LIMIT, img.height)))
    return trim_bottom(part)


shapes = prepare("m4-lab-shapes.png")
fills = prepare("m4-lab-fills.png")

GAP = 6
sheet = Image.new("RGB", (shapes.width, shapes.height + GAP + fills.height), (190, 190, 190))
sheet.paste(shapes, (0, 0))
sheet.paste(fills, (0, shapes.height + GAP))
ImageDraw.Draw(sheet).line(
    [(0, shapes.height + GAP // 2), (sheet.width, shapes.height + GAP // 2)],
    fill=(120, 120, 120),
    width=2,
)

out = SHOTS / "m4-selection-compare.png"
sheet.save(out)
print(f"{out.name}  {sheet.width}x{sheet.height}  {out.stat().st_size} B")
