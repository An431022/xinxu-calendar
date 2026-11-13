# 渲染「背景色候选」对照图。
#
# 要回答的问题只有一个：**十二色摆在这个背景上好不好看、准不准。**
# 背景越中性，色块的颜色就越是你定的那个颜色；背景一旦带色相，
# 它就会悄悄给所有色块加一层底色 —— 冷色被暖背景"污染"是最容易看出来的。
#
# 所以每一行都把 12 个色点直接铺在该背景上，另外附两个日期格（未涂色 / 涂了三色），
# 用来看新的"极短灰条"在实际背景上还剩多少存在感。
import json, os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
data = json.load(open(os.path.join(HERE, "palette.json"), encoding="utf-8"))
colors = data["colors"]


def font(size, bold=False):
    names = ("msyhbd.ttc", "msyh.ttc") if bold else ("msyh.ttc", "msyhl.ttc", "simhei.ttf")
    for name in names:
        p = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", name)
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()


F_TITLE = font(38, True)
F_H2    = font(27, True)
F_NAME  = font(29, True)
F_MONO  = font(21)
F_SMALL = font(19)
F_TINY  = font(16)
F_NUM   = font(30, True)

W, M = 1520, 56
PAGE = (233, 233, 235)
BAND_H = 192
GAP = 14

LIGHT = [
    ("#FDF8F6", "暖白（现状）", "带粉橘调，等于给冷色垫了层暖底"),
    ("#FAF8F5", "纸白",         "暖度减半，几乎中性，像一张纸"),
    ("#F5F1EC", "米白",         "更实的暖，整屏会明显偏黄"),
    ("#FFFFFF", "纯白",         "最中性最准，但偏冷偏亮"),
    ("#F8F9FA", "冷白",         "极轻冷灰，冷色更干净"),
    ("#F2F1F0", "中性浅灰",     "背景最不抢戏，色块最突出"),
]

DARK = [
    ("#17120F", "暖黑（现状）", "暖调深底，跟浅色模式的暖白成对"),
    ("#1C1A18", "暖深灰",       "比现状亮一档，暗部细节更清楚"),
    ("#121212", "纯中性黑",     "最中性，但偏「硬」"),
    ("#101418", "冷黑",         "极轻冷调，深色模式下冷色更干净"),
]


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def is_dark(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2] < 128


def empty_mark(bg):
    """未涂色日期下方那条极短灰条的颜色：比背景深/浅少许，尽量没有存在感。"""
    if is_dark(bg):
        return tuple(min(255, round(c + (255 - c) * 0.11)) for c in bg)
    return tuple(round(c * 0.905) for c in bg)


def fg_on(bg):
    return (235, 232, 230) if is_dark(bg) else (32, 30, 30)


def sub_on(bg):
    return (150, 146, 142) if is_dark(bg) else (140, 136, 132)


rows = len(LIGHT) + len(DARK)
H = 150 + 46 + len(LIGHT) * (BAND_H + GAP) + 56 + 46 + len(DARK) * (BAND_H + GAP) + 96

img = Image.new("RGB", (W, H), PAGE)
d = ImageDraw.Draw(img)

# ---------- 标题 ----------
d.text((M, 44), "「心绪日历」背景色候选", font=F_TITLE, fill=(26, 26, 28))
d.text((M, 96), "每一行都把十二色直接铺在该背景上 —— 背景越中性，色块越是你定的那个颜色",
       font=F_SMALL, fill=(110, 110, 116))

y = 150


def draw_band(y, hexcode, name, note, dark_mode):
    bg = rgb(hexcode)
    fg, sub = fg_on(bg), sub_on(bg)

    d.rounded_rectangle([M, y, W - M, y + BAND_H], radius=22, fill=bg)

    # --- 左侧：名称 / hex / 说明 ---
    tx = M + 30
    d.text((tx, y + 34), name, font=F_NAME, fill=fg)
    d.text((tx, y + 76), hexcode.upper(), font=F_MONO, fill=sub)
    d.text((tx, y + 112), note, font=F_TINY, fill=sub)
    d.text((tx, y + 138), "未涂色 ↓", font=F_TINY, fill=sub)

    # 未涂色的极短灰条，直接画在这个背景上，看它还剩多少存在感
    mark = empty_mark(bg)
    d.rounded_rectangle([tx + 92, y + 146, tx + 92 + 26, y + 152], radius=3, fill=mark)

    # --- 中间：十二色 ---
    dot_d, dot_gap = 50, 17
    dx = M + 390
    cy = y + BAND_H // 2
    for i, c in enumerate(colors):
        cx = dx + i * (dot_d + dot_gap)
        fill = rgb(c["dark"] if dark_mode else c["light"])
        d.ellipse([cx, cy - dot_d // 2, cx + dot_d, cy + dot_d // 2], fill=fill)

    # --- 右侧：两个日期格（未涂色 / 涂三色）---
    gw, gh = 88, 112
    gx = W - M - 18 - gw * 2 - 18
    gy = y + (BAND_H - gh) // 2
    sel_fill = (46, 39, 36) if not dark_mode else (232, 226, 224)
    num_fg = (255, 255, 255) if not dark_mode else (46, 39, 36)
    num_normal = fg

    for k in range(2):
        x0 = gx + k * (gw + 22)
        box = [x0, gy, x0 + gw, gy + gh]
        if k == 0:
            # 未涂色：数字 + 极短灰条（无选中底）
            d.text((x0 + gw / 2, gy + 26), "12", font=F_NUM, fill=num_normal, anchor="mm")
            bar_w = 26
            d.rounded_rectangle([x0 + (gw - bar_w) / 2, gy + 82, x0 + (gw + bar_w) / 2, gy + 88],
                                radius=3, fill=mark)
        else:
            # 涂了三色 + 选中态：等分三段，选中时色条更宽
            d.rounded_rectangle(box, radius=16, fill=sel_fill)
            d.text((x0 + gw / 2, gy + 26), "12", font=F_NUM, fill=num_fg, anchor="mm")
            bar_w = int(gw * 0.78)
            bx = x0 + (gw - bar_w) / 2
            seg = bar_w / 3
            for j, cid in enumerate(("warm_rose", "warm_amber", "cool_sky")):
                c = next(cc for cc in colors if cc["id"] == cid)
                col = rgb(c["dark"] if dark_mode else c["light"])
                x1 = bx + j * seg
                d.rounded_rectangle([x1, gy + 82, x1 + seg, gy + 88], radius=3, fill=col)

    d.text((gx + gw + 11, gy + gh + 10), "未涂色 / 涂三色", font=F_TINY, fill=sub, anchor="ma")


# 浅色模式
d.text((M, y - 4), "① 浅色模式背景", font=F_H2, fill=(26, 26, 28))
y += 46
for hexcode, name, note in LIGHT:
    draw_band(y, hexcode, name, note, dark_mode=False)
    y += BAND_H + GAP

# 深色模式
y += 42
d.text((M, y - 4), "② 深色模式背景", font=F_H2, fill=(26, 26, 28))
y += 46
for hexcode, name, note in DARK:
    draw_band(y, hexcode, name, note, dark_mode=True)
    y += BAND_H + GAP

# ---------- 脚注 ----------
d.text((M, H - 76), "看这张图的诀窍：不要盯着单个色块，横向比较同一列色块在各行里的差别 —— 尤其是深海、雾紫、晴空这三个冷色。",
       font=F_SMALL, fill=(110, 110, 116))
d.text((M, H - 46), "它们最容易被暖背景「染」上一层脏调。如果某一行里它们看起来最干净、最接近你想要的蓝，那一行就是对的。",
       font=F_SMALL, fill=(110, 110, 116))

out = os.path.join(HERE, "backgrounds.png")
img.save(out)
print("saved:", out, img.size)
