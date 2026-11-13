# 生成「背景色 A/B 对比」图，用来在手机上看。
#
# 两张真机截图内容完全一致（同一天、同样三种颜色），只差背景色：
#   冷白 #F8F9FA  /  纸白 #FAF8F5
#
# 输出两张图：
#   1. bg-compare-side.png —— 并排全图，看整体气质
#   2. bg-compare-seam.png —— 把两张图各切一半拼起来，中间那道缝就是色差
#      刻意做了两行、左右对调，防止眼睛适应了某一边就看不出来
import os
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SHOTS = r"C:\AndroidProjects\xinxu-calendar\docs\screenshots"


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


F_LABEL = font(30, True)
F_TITLE = font(42, True)

COOL = Image.open(os.path.join(SHOTS, "m3-light-bg.png")).convert("RGB")
PAPER = Image.open(os.path.join(SHOTS, "m3-paper-bg.png")).convert("RGB")
assert COOL.size == PAPER.size, (COOL.size, PAPER.size)

# --- 客观数据：取一片没有任何内容的背景区域，量一下两者到底差多少 ---
BOX = (60, 1150, 400, 1300)


def avg(img):
    r = img.crop(BOX).resize((1, 1), Image.BOX)
    return r.getpixel((0, 0))


ac, ap = avg(COOL), avg(PAPER)
print(f"冷白采样 {ac}   纸白采样 {ap}   ΔR={ap[0]-ac[0]:+d} ΔG={ap[1]-ac[1]:+d} ΔB={ap[2]-ac[2]:+d}")

W, H = COOL.size

# ---------- 1) 并排全图 ----------
LAB = 96
side = Image.new("RGB", (W * 2 + 24, H + LAB + 20), (255, 255, 255))
d = ImageDraw.Draw(side)
d.text((14, 16), "冷白  #F8F9FA", font=F_TITLE, fill=(20, 20, 20))
d.text((W + 26, 16), "纸白  #FAF8F5", font=F_TITLE, fill=(20, 20, 20))
side.paste(COOL, (10, LAB))
side.paste(PAPER, (W + 14, LAB))
side.save(os.path.join(SHOTS, "bg-compare-side.png"))

# ---------- 2) 各切一半拼起来 ----------
TOP, BOT = 60, 1120          # 只取「标题 + 日历网格」这一段，色差在这块最容易被看出来
c, p = COOL.crop((0, TOP, W, BOT)), PAPER.crop((0, TOP, W, BOT))
CW, CH = c.size
half = CW // 2


def seam(left, right):
    im = Image.new("RGB", (CW, CH))
    im.paste(left.crop((0, 0, half, CH)), (0, 0))
    im.paste(right.crop((half, 0, CW, CH)), (half, 0))
    ImageDraw.Draw(im).rectangle([half - 1, 0, half, CH], fill=(150, 150, 150))
    return im


rows = [("第 1 行　左 = 冷白　右 = 纸白", seam(c, p)),
        ("第 2 行　左 = 纸白　右 = 冷白（左右对调，防止眼睛适应）", seam(p, c))]

LEG = 78
out = Image.new("RGB", (CW, LEG * 2 + CH * 2), (255, 255, 255))
d = ImageDraw.Draw(out)
y = 0
for label, im in rows:
    d.text((14, y + 20), label, font=F_LABEL, fill=(20, 20, 20))
    y += LEG
    out.paste(im, (0, y))
    y += CH
out.save(os.path.join(SHOTS, "bg-compare-seam.png"))

print("saved:", os.path.join(SHOTS, "bg-compare-side.png"), side.size)
print("saved:", os.path.join(SHOTS, "bg-compare-seam.png"), out.size)
