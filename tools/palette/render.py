# 把 palette.json 画成一张对照图，方便肉眼判断"刺不刺眼"。
import json, os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
data = json.load(open(os.path.join(HERE, "palette.json"), encoding="utf-8"))
colors, ladders = data["colors"], data["ladders"]

def font(size, bold=False):
    for name in (("msyhbd.ttc", "msyh.ttc") if bold else ("msyh.ttc", "msyhl.ttc", "simhei.ttf")):
        p = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", name)
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()

F_TITLE = font(38, True)
F_NAME  = font(30, True)
F_MONO  = font(22)
F_SMALL = font(20)
F_TINY  = font(17)

W, M = 1520, 56
BG = (250, 250, 250)
CARD = (255, 255, 255)

# ---------- 布局计算 ----------
COLS, PAD = 4, 26
cell_w = (W - 2 * M - (COLS - 1) * PAD) // COLS
SW_H = 116
cell_h = SW_H + 116
rows = (len(colors) + COLS - 1) // COLS

y_sec1 = 150
y_sec2 = y_sec1 + 54 + rows * cell_h + 60
LAD_ROW = 74
H = y_sec2 + 60 + len(ladders) * LAD_ROW + 110

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

def rrect(box, r, fill, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)

def temp_color(t):
    return {"warm": (200, 100, 30), "cool": (45, 100, 170), "neutral": (120, 120, 125)}[t]

def temp_label(t):
    return {"warm": "暖", "cool": "冷", "neutral": "中性"}[t]

# ---------- 标题 ----------
d.text((M, 46), "「色彩日历」色板提案", font=F_TITLE, fill=(28, 28, 32))
d.text((M, 96), "基于 Google Material 3 色调体系（HCT）生成 · chroma 上限 48 · tone 60–70 区间为主",
       font=F_SMALL, fill=(120, 120, 128))

# ---------- 第一段：12 色 ----------
d.text((M, y_sec1 - 42), "① 十二色固定色板", font=font(28, True), fill=(28, 28, 32))

for i, c in enumerate(colors):
    r, col = divmod(i, COLS)
    x = M + col * (cell_w + PAD)
    y = y_sec1 + r * cell_h

    rrect([x, y, x + cell_w, y + SW_H], 20, CARD, outline=(226, 226, 232))
    # 左半浅色 = 深色模式值, 右半 = 浅色模式值；中间主色块
    rrect([x + 10, y + 10, x + cell_w - 10, y + SW_H - 10], 14, tuple(int(c["light"][j:j+2], 16) for j in (1, 3, 5)))

    # 左下角小方块显示深色模式值
    dk = tuple(int(c["dark"][j:j+2], 16) for j in (1, 3, 5))
    rrect([x + 18, y + SW_H - 46, x + 62, y + SW_H - 18], 8, dk, outline=(255, 255, 255), width=2)
    d.text((x + 70, y + SW_H - 44), "深色模式", font=F_TINY, fill=(255, 255, 255))

    ty = y + SW_H + 12
    d.text((x, ty), c["name"], font=F_NAME, fill=(28, 28, 32))
    nw = d.textlength(c["name"], font=F_NAME)
    d.text((x + nw + 12, ty + 8), temp_label(c["temperature"]), font=F_SMALL, fill=temp_color(c["temperature"]))
    d.text((x, ty + 40), c["light"].upper(), font=F_MONO, fill=(90, 90, 100))
    d.text((x, ty + 68), c["meaning"], font=F_SMALL, fill=(150, 150, 158))

# ---------- 第二段：色调阶梯 ----------
d.text((M, y_sec2 - 42), "② 同一色相的明度阶梯（tone）—— 挑选「最不刺眼」的那一档", font=font(28, True), fill=(28, 28, 32))

label_w = 130
chip_gap = 12
chip_w = (W - 2 * M - label_w - 7 * chip_gap) // 8
chip_h = 52

for i, lad in enumerate(ladders):
    y = y_sec2 + i * LAD_ROW
    d.text((M, y + 14), lad["name"], font=F_NAME, fill=(28, 28, 32))
    d.text((M, y + 46), f"H{lad['hue']} C{lad['chroma']}", font=F_TINY, fill=(155, 155, 162))
    for j, t in enumerate(lad["tones"]):
        x = M + label_w + j * (chip_w + chip_gap)
        rgb = tuple(int(t["hex"][k:k+2], 16) for k in (1, 3, 5))
        rrect([x, y, x + chip_w, y + chip_h], 10, rgb)
        lum = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]
        fg = (255, 255, 255) if lum < 150 else (40, 40, 45)
        d.text((x + 8, y + 16), str(t["tone"]), font=F_SMALL, fill=fg)

# ---------- 脚注 ----------
d.text((M, H - 78), "HCT = Hue(色相) + Chroma(彩度) + Tone(感知明度)，是 Material 3 动态取色的底层色彩空间。",
       font=F_SMALL, fill=(130, 130, 138))
d.text((M, H - 48), "彩度封顶 48 是 Material 色调板的上限 —— 再高就会开始「尖锐」；这也是自定义颜色也要走同一套约束的原因。",
       font=F_SMALL, fill=(130, 130, 138))

out = os.path.join(HERE, "palette.png")
img.save(out)
print("saved:", out, img.size)
