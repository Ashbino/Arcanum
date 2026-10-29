import os, json, colorsys, zipfile
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "resourcepack")
TEX = os.path.join(ROOT, "assets/arcanum/textures/item")
MOD = os.path.join(ROOT, "assets/arcanum/models/item")
ITM = os.path.join(ROOT, "assets/arcanum/items")
for d in (TEX, MOD, ITM): os.makedirs(d, exist_ok=True)

def hx(s): s = s.lstrip('#'); return tuple(int(s[i:i+2], 16) for i in (0, 2, 4)) + (255,)

# outline, dark, mid, light, highlight
PAL = {
 'fire':      [hx(c) for c in ('4a0d00','b32400','ff5a1a','ffa42b','fff2a8')],
 'ice':       [hx(c) for c in ('0b2a4a','2a7fbf','5ec8f2','a8ecff','ffffff')],
 'dark':      [hx(c) for c in ('12001f','3b0f63','6a1fa8','a24fe0','e5b3ff')],
 'light':     [hx(c) for c in ('6b4a00','e0a800','ffd93a','fff08a','ffffff')],
 'lightning': [hx(c) for c in ('1c2a63','3a5bd9','ffd21a','fff27a','ffffff')],
 'nature':    [hx(c) for c in ('0f3a14','2a8a2e','55cf3c','a6f06a','f0ffc0')],
 'wind':      [hx(c) for c in ('1f4a52','4fa8a8','a0eadb','dffff8','ffffff')],
 'earth':     [hx(c) for c in ('2b1a0c','6b4a2b','a37a45','d1a86a','f2dca0')],
}
ELEMENTS = list(PAL)
WOOD = [hx(c) for c in ('2e1a0c','5a3a1c','8a5a2e','b07d44')]
GOLD = [hx(c) for c in ('6b4208','b07a1c','e0a82e','ffe27a')]

SHAPES = {  # 5x5, d/m/l/h shading
 'fire':      ["..l..", ".mlm.", "mllhm", "mmlmd", ".mdd."],
 'ice':       ["..h..", ".lhm.", "lhlmd", ".mmdd", "..d.."],
 'dark':      [".lll.", "lmmml", "lmdml", "lmmml", ".lll."],
 'light':     ["..h..", ".mhm.", "hhhhh", ".mhm.", "..h.."],
 'lightning': ["..hll", ".hll.", "hlllm", ".llm.", "llm.."],
 'nature':    ["...ll", "..lhl", ".lhlm", "lhlm.", "mlm.."],
 'wind':      [".hll.", "l...m", "l.l.m", "l..lm", ".mmm."],
 'earth':     ["..mm.", ".mlhm", "mlllm", "mlmdd", ".ddd."],
}
CH = {'d': 1, 'm': 2, 'l': 3, 'h': 4}

def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), c)
def occupied(im, x, y): return 0 <= x < 16 and 0 <= y < 16 and im.getpixel((x, y))[3] > 0

def outline(im, col, mask=None):
    pts = []
    for y in range(16):
        for x in range(16):
            if not occupied(im, x, y):
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    if occupied(im, x+dx, y+dy): pts.append((x, y)); break
    for p in pts: put(im, p[0], p[1], col)

def shaft(im, dark=False):
    for i in range(9):
        x, y = 1+i, 14-i
        put(im, x, y, WOOD[2]); put(im, x+1, y, WOOD[1]); put(im, x, y-1, WOOD[3]) if i % 2 == 0 else None
    put(im, 0, 15, GOLD[2]); put(im, 1, 15, GOLD[1]); put(im, 0, 14, GOLD[3])
    for (x, y) in ((3, 12), (4, 11)): put(im, x, y, GOLD[2]); put(im, x+1, y, GOLD[1])

def gem(im, shape, pal, ox, oy, rainbow=False):
    for r, row in enumerate(shape):
        for c, ch in enumerate(row):
            if ch in CH:
                col = pal[CH[ch]]
                if rainbow:
                    h = ((r*5 + c*3) % 12) / 12.0
                    rr, gg, bb = colorsys.hsv_to_rgb(h, 0.65 if ch != 'h' else 0.15, 1.0 if ch in 'lh' else 0.85 if ch == 'm' else 0.6)
                    col = (int(rr*255), int(gg*255), int(bb*255), 255)
                put(im, ox+c, oy+r, col)

def staff(el, level, multi=False):
    im = new()
    pal = PAL['dark'] if multi else PAL[el]
    shaft(im)
    if multi:  # obsidian shaft with gold rings
        for i in range(9):
            x, y = 1+i, 14-i
            put(im, x, y, hx('3a2a55')); put(im, x+1, y, hx('1c1230'))
        for (x, y) in ((3, 12), (5, 10), (7, 8)): put(im, x, y, GOLD[3]); put(im, x+1, y, GOLD[2])
    shape = SHAPES['light'] if multi else SHAPES[el]
    # gold setting first (behind gem)
    if level >= 2:
        for (x, y) in ((8, 3), (8, 6), (14, 3), (14, 6)): put(im, x, y, GOLD[2])
        put(im, 9, 7, GOLD[1]); put(im, 13, 7, GOLD[1])
    if level >= 3:
        for (x, y) in ((9, 1), (13, 1), (8, 2), (14, 2)): put(im, x, y, GOLD[3] if (x+y) % 2 else GOLD[2])
        put(im, 11, 8, GOLD[1]); put(im, 10, 8, GOLD[2]) ; put(im, 12, 8, GOLD[2])
    if level >= 4:
        for x in (10, 11, 12): put(im, x, 0, GOLD[3] if x == 11 else GOLD[2])
        put(im, 9, 0, GOLD[1]); put(im, 13, 0, GOLD[1])
    gem(im, shape, pal, 9, 2, rainbow=multi)
    outline(im, pal[0])
    if level == 1:
        put(im, 8, 8, GOLD[2]); put(im, 7, 9, GOLD[1])  # ferrule
    else:
        put(im, 8, 8, GOLD[3]); put(im, 7, 9, GOLD[2]); put(im, 6, 10, GOLD[1])
    if level >= 4:  # glow aura + sparkles
        glow = pal[3][:3] + (110,)
        add = []
        for y in range(16):
            for x in range(16):
                if not occupied(im, x, y) and y <= 9 and x >= 7:
                    if any(occupied(im, x+dx, y+dy) and im.getpixel((x+dx, y+dy))[3] == 255 and (x+dx, y+dy) != (0, 0)
                           for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))) and (x, y) not in ((0, 0),):
                        add.append((x, y))
        for p in add:
            if p[0] >= 8 or p[1] <= 8: put(im, p[0], p[1], glow)
        put(im, 15, 1, (255, 255, 255, 255)); put(im, 8, 0, (255, 255, 255, 255)); put(im, 15, 7, pal[4])
    if level == 3: put(im, 15, 2, pal[4])
    return im

def scroll(el):
    pal = PAL[el]; im = new()
    P = [hx(c) for c in ('5c4420','a68a4e','cdb27a','e8d5a3','fff3d1')]
    for i in range(12):  # diagonal body
        x, y = 2+i, 12-i
        for t in (-1, 0, 1):
            put(im, x, y+t, P[2])
        put(im, x, y-1, P[3]); put(im, x, y+1, P[1])
    for (x, y) in ((1, 13), (2, 13), (1, 12), (2, 12), (13, 2), (14, 2), (13, 1), (14, 1)):
        put(im, x, y, P[3] if (x+y) % 2 == 0 else P[2])
    for (x, y) in ((1, 13), (13, 1)): put(im, x, y, P[4])
    for (x, y) in ((8, 6), (7, 7), (6, 8)):  # ribbon
        put(im, x, y, pal[2]); put(im, x, y-1, pal[3]); put(im, x, y+1, pal[1])
    put(im, 7, 6, pal[4]); put(im, 8, 7, pal[1]); put(im, 7, 5, pal[2]); put(im, 6, 7, pal[2])  # seal
    outline(im, P[0])
    return im

def book(el):
    pal = PAL[el]; im = new()
    for y in range(1, 15):
        for x in range(3, 13):
            im.putpixel((x, y), pal[2])
    for y in range(1, 15):
        im.putpixel((3, y), pal[1]); im.putpixel((4, y), pal[1])
    for x in range(3, 13): im.putpixel((x, 1), pal[3] if x > 4 else pal[2])
    for y in range(14, 15):
        for x in range(4, 13): im.putpixel((x, y), hx('e8d5a3'))
    for y in range(2, 15): im.putpixel((12, y), hx('e8d5a3')) if y > 2 else None
    for (x, y) in ((5, 2), (11, 2), (5, 13), (11, 13), (6, 2), (10, 2), (5, 3), (11, 3), (5, 12), (11, 12)):
        put(im, x, y, GOLD[2])
    gem(im, SHAPES[el], pal, 6, 5)
    # re-shade the gem so it stands out from the cover: dark backing ring
    for (x, y) in ((5, 4), (11, 4), (5, 10), (11, 10)): put(im, x, y, GOLD[1])
    outline_pts = []
    for y in range(16):
        for x in range(16):
            if not occupied(im, x, y):
                if any(occupied(im, x+dx, y+dy) for dx, dy in ((1,0),(-1,0),(0,1),(0,-1))): outline_pts.append((x, y))
    for p in outline_pts: put(im, p[0], p[1], pal[0])
    return im

def crystal(tier):
    pal = [hx(c) for c in ('1a0a33', '5a2a9a', '9d4edd', 'd9a8ff', 'ffffff')]
    im = new()
    shapes = {
      2: [(7,4),(7,5),(8,5),(6,6),(7,6),(8,6),(9,6),(6,7),(7,7),(8,7),(9,7),(6,8),(7,8),(8,8),(9,8),(7,9),(8,9)],
      3: [(7,2),(7,3),(6,4),(7,4),(8,4),(6,5),(7,5),(8,5),(5,6),(6,6),(7,6),(8,6),(9,6),(5,7),(6,7),(7,7),(8,7),(9,7),(6,8),(7,8),(8,8),(6,9),(7,9),(8,9),(7,10)],
      4: [(8,1),(8,2),(7,3),(8,3),(9,3),(7,4),(8,4),(9,4),(6,5),(7,5),(8,5),(9,5),(10,5),(6,6),(7,6),(8,6),(9,6),(10,6),(6,7),(7,7),(8,7),(9,7),(10,7),(7,8),(8,8),(9,8),(7,9),(8,9),(9,9),(8,10)],
    }[tier]
    xs = [p[0] for p in shapes]; xmid = (min(xs) + max(xs)) / 2
    for (x, y) in shapes:
        c = pal[3] if x < xmid - 0.5 else pal[2] if x <= xmid + 0.5 else pal[1]
        put(im, x, y, c)
    top = min(p[1] for p in shapes); put(im, int(xmid), top+1, pal[4])
    put(im, int(xmid), top+2, pal[3]) if tier >= 3 else None
    base_y = max(p[1] for p in shapes) + 2
    for x in range(int(xmid)-3, int(xmid)+4): put(im, x, base_y, GOLD[2] if x % 2 else GOLD[1])
    for x in range(int(xmid)-2, int(xmid)+3): put(im, x, base_y+1, GOLD[1])
    if tier >= 3:
        put(im, 3, 5, pal[4]); put(im, 12, 3 if tier == 4 else 8, pal[4])
    if tier == 4:
        for p in ((2, 9), (13, 9), (4, 2), (12, 12)): put(im, p[0], p[1], pal[4])
    outline(im, pal[0])
    return im

def save_item(name, im, parent="item/generated"):
    im.save(os.path.join(TEX, name + ".png"))
    with open(os.path.join(MOD, name + ".json"), "w") as f:
        json.dump({"parent": "minecraft:" + parent, "textures": {"layer0": "arcanum:item/" + name}}, f, indent=2)
    with open(os.path.join(ITM, name + ".json"), "w") as f:
        json.dump({"model": {"type": "minecraft:model", "model": "arcanum:item/" + name}}, f, indent=2)

for el in ELEMENTS:
    for lv in range(1, 5): save_item(f"staff_{el}_{lv}", staff(el, lv), "item/handheld")
    save_item(f"scroll_{el}", scroll(el))
    save_item(f"book_{el}", book(el))
for lv in range(2, 5): save_item(f"staff_multi_{lv}", staff(None, lv, True), "item/handheld")
for t in range(2, 5): save_item(f"crystal_{t}", crystal(t))

with open(os.path.join(ROOT, "pack.mcmeta"), "w") as f:
    json.dump({"pack": {"min_format": [75, 0], "max_format": [75, 0],
                        "description": "\u00a76\u00a7lArcanum\u00a7r \u00a77- staffs, scrolls and tomes"}}, f, indent=2)
icon = staff('fire', 4).resize((64, 64), Image.NEAREST)
icon.save(os.path.join(ROOT, "pack.png"))

# preview sheet
names = sorted(os.listdir(TEX))
cols = 8; rows = (len(names) + cols - 1) // cols; S = 8
sheet = Image.new("RGBA", (cols*(16*S+8)+8, rows*(16*S+8)+8), (46, 46, 56, 255))
for i, n in enumerate(names):
    t = Image.open(os.path.join(TEX, n)).resize((16*S, 16*S), Image.NEAREST)
    sheet.alpha_composite(t, (8 + (i % cols)*(16*S+8), 8 + (i // cols)*(16*S+8)))
sheet.save("/tmp/preview.png")
print(len(names), "textures")
