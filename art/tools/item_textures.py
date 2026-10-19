"""Draws the item textures that were still missing (see TEXTURES.md) into
src/main/resources/assets/horrormod/textures/item/:

    16x16 pixel art, drawn from the grids below: berry_pile, aspiraton_drop, scalpel, holy_text, salt, crank,
    her_favorite_dress, grape_hugs
    32x32 pixel art, drawn from shapes: toy_gun, heroine, straw
    high-res, made from the Pelt picture (humanskin.png) so they read as the same skin: rotted_pelt, graft

Run: python3 art/tools/item_textures.py [names]   (needs Pillow; all of them if no names are given)
Re-running overwrites those files, including any painting done on them since: give it just the names to redo.

Grids: one character per pixel, '.' is transparent; each grid has its own palette. Shapes drawn with shade()
get an outline and light from the top left automatically.
"""
import math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "horrormod", "textures", "item")

def grid(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                px[x, y] = palette[ch] + (255,) if len(palette[ch]) == 3 else palette[ch]
    return img

def shade(mask_rows, light, mid, dark, outline):
    """A filled shape (any non-'.' character in mask_rows) lit from the top left: an outline where it meets the
    transparent background, highlights toward the top left and shadow toward the bottom right."""
    cells = {(x, y) for y, r in enumerate(mask_rows) for x, c in enumerate(r) if c != "."}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
    xs = [x for x, _ in cells]; ys = [y for _, y in cells]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    for (x, y) in cells:
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and ((x + 1, y) not in cells or (x, y + 1) not in cells):
            col = outline                                   # bottom / right edges: darkest
        elif edge:
            col = dark if (x - cx) + (y - cy) > 0 else mid
        else:
            t = ((x - cx) + (y - cy)) / max(1.0, (max(xs) - min(xs) + max(ys) - min(ys)) / 2)
            col = light if t < -0.45 else mid if t < 0.35 else dark
        px[x, y] = col + (255,)
    return img

# ---------------------------------------------------------------- 16x16 items
def aspiraton_drop():
    """Shaped like a dye blob: a drop of dark, dried, slightly brown blood."""
    mask = ["................",
            "................",
            ".......##.......",
            "......####......",
            "......####......",
            ".....######.....",
            "....########....",
            "...##########...",
            "...##########...",
            "..############..",
            "..############..",
            "..############..",
            "...##########...",
            "....########....",
            "......####......",
            "................"]
    img = shade(mask, (128, 30, 26), (92, 16, 14), (64, 10, 10), (34, 6, 6))
    px = img.load()
    for x, y in ((5, 7), (5, 8), (4, 9), (6, 6)):           # a dull glint: dried, not wet
        px[x, y] = (150, 52, 44, 255)
    for x, y in ((9, 9), (10, 11), (7, 12), (11, 8)):       # crusted, browner flecks
        px[x, y] = (78, 28, 18, 255)
    return img

def scalpel():
    """Diagonal: a short, curved silver blade at the top right on a long, flat, darker metal handle at the bottom
    left (held like a sword)."""
    return grid(["................",
                 "..............w.",
                 ".............wa.",
                 "............wab.",
                 "...........waab.",
                 "..........kabb..",
                 ".........kkbb...",
                 "........hmk.....",
                 ".......hmd......",
                 "......hmd.......",
                 ".....hmd........",
                 "....hmd.........",
                 "...hmd..........",
                 "..hmd...........",
                 "..dd............",
                 "................"],
                {"w": (240, 244, 248), "a": (196, 202, 210), "b": (130, 136, 146), "k": (64, 68, 76),
                 "h": (156, 162, 172), "m": (108, 114, 124), "d": (58, 62, 70)})

def holy_text():
    """A book with a gold cover, a darker spine, pale page edges and a small generic star on the front."""
    return grid(["................",
                 "...lllllllllll..",
                 "..sgggggggggggp.",
                 "..sgggggggggGgp.",
                 "..sgggggwgggGgp.",
                 "..sggggwWwggGgp.",
                 "..sgggwWWWwgGgp.",
                 "..sggggwWwggGgp.",
                 "..sgggggwgggGgp.",
                 "..sggggggggGGgp.",
                 "..sgggggggGGGgp.",
                 "..sGGGGGGGGGGGp.",
                 "..sddddddddddddp",
                 "...pppppppppppp.",
                 "....ooooooooooo.",
                 "................"],
                {"l": (250, 222, 110), "g": (222, 176, 48), "G": (186, 140, 30), "s": (120, 76, 18),
                 "d": (150, 104, 22), "w": (255, 246, 196), "W": (255, 255, 236), "p": (238, 230, 210),
                 "o": (176, 164, 140)})

def salt():
    """A small pile of chunky, cube-shaped white crystals (sugar is powder; these are clearly grains)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
    top, left, right, edge = (255, 255, 255), (224, 228, 232), (176, 182, 190), (128, 134, 144)
    # (x, y, size) from the back of the pile to the front
    cubes = [(6, 5, 3), (3, 8, 3), (9, 7, 3), (6, 9, 3), (11, 10, 3), (2, 11, 3), (8, 11, 2), (5, 12, 2), (12, 13, 2),
             (9, 13, 2)]
    for x0, y0, s in cubes:
        for y in range(y0, y0 + s):
            for x in range(x0, x0 + s):
                if y == y0:
                    c = top
                elif x == x0 + s - 1 or y == y0 + s - 1:
                    c = right if x == x0 + s - 1 else left
                else:
                    c = left
                px[x, y] = c + (255,)
        px[x0 + s - 1, y0 + s - 1] = edge + (255,)
    return img

def crank():
    """A jack-in-the-box, lid shut: a patterned toy box with the crank handle sticking out of its side."""
    return grid(["................",
                 "................",
                 "..LLLLLLLLLL....",
                 ".LlllllllllllL..",
                 ".BBBBBBBBBBBB...",
                 ".BybbbbbbbbyB...",
                 ".BbbbbbbbbbbBmm.",
                 ".BbbyybbbbbbB.m.",
                 ".BbbyybbbbbbB.mK",
                 ".BbbbbbbyybbB..K",
                 ".BbbbbbbyybbB...",
                 ".BybbbbbbbbyB...",
                 ".BbbbbbbbbbbB...",
                 ".DDDDDDDDDDDD...",
                 "................",
                 "................"],
                {"L": (70, 110, 190), "l": (44, 78, 150), "B": (150, 34, 34), "b": (196, 58, 52),
                 "y": (236, 200, 70), "D": (100, 22, 22), "m": (196, 200, 208), "K": (230, 196, 90)})

def her_favorite_dress():
    """A simple dress on a wire hanger, its pink faded toward grey, with a stain near the hem."""
    mask = ["................",
            "................",
            "................",
            "................",
            "................",
            "....###..###....",
            "....########....",
            ".....######.....",
            "......####......",
            ".....######.....",
            "....########....",
            "....########....",
            "...##########...",
            "...##########...",
            "..############..",
            "................"]
    img = shade(mask, (226, 186, 192), (196, 150, 160), (160, 118, 130), (108, 76, 88))
    px = img.load()
    for x in range(6, 10):                                  # a ribbon at the waist
        px[x, 8] = (150, 84, 98, 255)
    for x, y in ((10, 12), (11, 13), (10, 13), (9, 14)):     # an old brown stain
        px[x, y] = (128, 92, 74, 255)
    wire = (150, 154, 162, 255)
    for x, y in ((8, 0), (9, 0), (9, 1), (8, 2), (7, 3), (8, 3), (6, 4), (9, 4), (5, 4), (10, 4), (4, 5), (11, 5)):
        px[x, y] = wire                                     # the hanger's hook and shoulders
    return img

def grape_hugs():
    """A white styrofoam cup seen from a little above, purple drink at the rim and a drip down the side."""
    return grid(["................",
                 "................",
                 "................",
                 "...wwwwwwwwww...",
                 "..wpppppppppPw..",
                 "..wPpppppppPPw..",
                 "...wwwwwwwwww...",
                 "...cdccccccccg..",
                 "...cdcccccccgg..",
                 "....ccccccccg...",
                 "....ccccccccg...",
                 "....cccccccgg...",
                 ".....ccccccg....",
                 ".....ccccccg....",
                 ".....eeeeeee....",
                 "................"],
                {"w": (250, 250, 248), "p": (150, 70, 200), "P": (104, 40, 150), "c": (236, 236, 232),
                 "g": (196, 196, 192), "e": (170, 170, 168), "d": (140, 64, 186)})

def berry_pile():
    """A heap of round red berries, each with a little shine, a few stems and leaves poking out."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
    rim, body, light, shine = (90, 8, 18), (186, 24, 40), (226, 58, 66), (255, 196, 196)
    # (x, y) of each berry's top-left, from the back of the pile to the front
    berries = [(6, 3), (3, 5), (9, 5), (6, 6), (1, 8), (4, 8), (8, 8), (11, 8), (2, 11), (5, 11), (8, 11), (11, 11)]
    shape = [".##.", "####", "####", ".##."]
    for bx, by in berries:                                   # dark shadow filling the gaps between berries
        for dy in range(4 if by < 11 else 3):              # (the front row keeps its rounded bottom)
            for dx in range(4):
                px[bx + dx, by + dy] = rim + (255,)
    for bx, by in berries:
        for dy, row in enumerate(shape):
            for dx, ch in enumerate(row):
                if ch == ".":
                    continue
                col = rim if (dy == 3 or dx == 3 or (dx == 0 and dy == 2)) else light if (dx == 1 and dy == 1) else body
                px[bx + dx, by + dy] = col + (255,)
        px[bx + 1, by + 1] = shine + (255,)
    for x, y, c in ((7, 2, (58, 96, 40)), (8, 2, (78, 128, 52)), (12, 7, (58, 96, 40)), (2, 7, (78, 128, 52)),
                    (13, 7, (78, 128, 52))):
        px[x, y] = c + (255,)                                # stems and leaf tips
    return img

# ---------------------------------------------------------------- 32x32 items drawn from shapes
# Each design is a list of parts in its own frame (u along it, v across it), drawn back to front. A pixel takes
# the colour of the frontmost part covering its centre; the frame is turned and scaled to fill the image.
def _seg_dist(p, a, b):
    ax, ay = b[0] - a[0], b[1] - a[1]
    t = max(0.0, min(1.0, ((p[0] - a[0]) * ax + (p[1] - a[1]) * ay) / (ax * ax + ay * ay or 1)))
    qx, qy = a[0] + ax * t, a[1] + ay * t
    return math.hypot(p[0] - qx, p[1] - qy), t

def _in_poly(p, pts):
    x, y = p; inside = False
    for i in range(len(pts)):
        (x0, y0), (x1, y1) = pts[i], pts[i - 1]
        if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0:
            inside = not inside
    return inside

def rect(u0, u1, v0, v1):
    return lambda p: u0 <= p[0] <= u1 and v0 <= p[1] <= v1

def poly(*pts):
    return lambda p: _in_poly(p, pts)

def capsule(a, b, r):
    return lambda p: _seg_dist(p, a, b)[0] <= r

def disc(c, r):
    return lambda p: math.hypot(p[0] - c[0], p[1] - c[1]) <= r

def ring(c, rx, ry, w):
    return lambda p: abs(math.hypot((p[0] - c[0]) / rx, (p[1] - c[1]) / ry) - 1.0) * min(rx, ry) <= w / 2

def draw(parts, size, angle, margin=1.0, outline=0.55, thick=1.0):
    """parts: (shape, colour) or (shape, colour_function(u, v)) back to front. angle: degrees the frame's u axis
    is turned from pointing right, counter-clockwise on screen; thick > 1 fattens the design across its length. Fits
    everything inside the image."""
    parts = [((lambda sh: lambda p: sh((p[0], p[1] / thick)))(sh),
              (lambda c: (lambda u, v: c(u, v / thick)) if callable(c) else c)(col)) for sh, col in parts]
    ca, sa = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    # find the frame's extent by sampling it, then fit
    pts = [(u / 2.0, v / 2.0) for u in range(-20, 100) for v in range(-30, 30)
           if any(sh((u / 2.0, v / 2.0)) for sh, _ in parts)]
    to_screen = lambda u, v: (u * ca - v * sa, -(u * sa + v * ca))
    xs, ys = zip(*(to_screen(u, v) for u, v in pts))
    scale = (size - 2 * margin) / max(max(xs) - min(xs), max(ys) - min(ys))
    ox = (size - (max(xs) - min(xs)) * scale) / 2 - min(xs) * scale
    oy = (size - (max(ys) - min(ys)) * scale) / 2 - min(ys) * scale
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0)); px = img.load()
    for y in range(size):
        for x in range(size):
            sx, sy = (x + 0.5 - ox) / scale, (y + 0.5 - oy) / scale
            u, v = sx * ca - sy * sa, -sx * sa - sy * ca
            for sh, col in reversed(parts):
                if sh((u, v)):
                    c = col(u, v) if callable(col) else col
                    px[x, y] = tuple(int(max(0, min(255, k))) for k in c) + (255,)
                    break
    # darken the outer edge so the shape reads against any background
    src = img.copy().load()
    for y in range(size):
        for x in range(size):
            if src[x, y][3] and any(not (0 <= x + dx < size and 0 <= y + dy < size) or src[x + dx, y + dy][3] == 0
                                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                r, g, b, a = src[x, y]
                px[x, y] = (int(r * outline), int(g * outline), int(b * outline), a)
    return img

def lit(light, dark, v0, v1):
    """A colour shading from light at the top of a part (v1) to dark at its bottom (v0)."""
    def f(u, v):
        t = max(0.0, min(1.0, (v - v0) / (v1 - v0 or 1)))
        return tuple(dark[i] + (light[i] - dark[i]) * t for i in range(3))
    return f

def toy_gun():
    """A toy lever-action rifle (after the reference photo): red muzzle cap and front sight, black barrel with a red
    tube under it, a smoky see-through scope, black receiver, brown plastic stock with checkered grips, a black
    lever loop with a red trigger. Barrel to the top right, stock to the bottom left."""
    black_l, black_d = (78, 82, 92), (26, 28, 34)
    red_l, red_d = (232, 56, 48), (150, 22, 22)
    brown_l, brown_d = (128, 70, 52), (74, 38, 28)
    smoke_l, smoke_d = (132, 124, 112), (70, 64, 58)
    def stock_col(u, v):
        c = lit(brown_l, brown_d, -8.4, -0.8)(u, v)
        grip = (30.5 <= u <= 33.5 and -5.0 <= v <= -3.0) or (36 <= u <= 42 and -6.6 <= v <= -3.0) or \
               (22.5 <= u <= 26.5 and -3.8 <= v <= -2.7)
        if grip and (int(u * 1.4) + int(v * 1.4)) % 2:
            c = tuple(k * 0.72 for k in c)                  # checkering
        return c
    parts = [
        (rect(20.5, 29.0, 2.0, 4.0), lit(smoke_l, smoke_d, 2.0, 4.0)),        # scope body
        (rect(19.4, 21.0, 1.8, 4.6), lit((150, 142, 130), smoke_d, 1.8, 4.6)),  # objective bell
        (rect(23.0, 25.5, 1.4, 2.1), black_d),                                # scope mount
        (rect(7.0, 22.0, -2.5, -1.1), lit(red_l, red_d, -2.5, -1.1)),          # red tube under the barrel
        (rect(6.0, 7.6, -2.7, -0.9), lit((240, 90, 70), red_d, -2.7, -0.9)),   # its cap
        (rect(10.4, 11.4, -2.8, 1.1), black_d),                               # barrel band
        (rect(1.6, 24.0, -1.1, 1.1), lit(black_l, black_d, -1.1, 1.1)),        # barrel
        (rect(0.0, 2.2, -1.4, 1.4), lit(red_l, red_d, -1.4, 1.4)),             # muzzle cap
        (rect(0.5, 1.5, 1.4, 2.6), red_l),                                    # front sight
        (poly((21.5, -2.5), (27.0, -2.5), (27.0, -4.0), (22.5, -4.0)), stock_col),   # forend
        (ring((28.6, -4.9), 2.0, 1.7, 1.5), (44, 46, 54)),                    # lever loop
        (rect(29.0, 29.6, -4.9, -2.5), red_l),                                # trigger
        (poly((29.0, -2.5), (33.0, -2.5), (46.0, -0.8), (46.6, -8.4), (43.0, -8.4), (34.0, -5.4), (30.0, -5.4)),
         stock_col),                                                          # grip and stock
        (rect(45.6, 46.8, -8.4, -0.8), (60, 30, 22)),                         # butt pad
        (rect(21.0, 32.4, -2.5, 1.7), lit(black_l, black_d, -2.5, 1.7)),       # receiver
        (rect(31.6, 32.4, -0.8, 0.2), red_l),                                 # red button
    ]
    return draw(parts, 32, angle=-45, thick=1.15).transpose(Image.FLIP_LEFT_RIGHT)   # barrel to the top right

def heroine():
    """A worn, secondhand female action figure flying up in the classic pose: one fist thrust up, the other arm at
    her side, legs together, cape streaming out behind her. Colours faded."""
    suit, suit_d = (92, 118, 164), (58, 78, 116)
    cape, cape_d = (172, 72, 66), (112, 40, 38)
    glove = (160, 60, 56)
    skin, skin_d = (218, 184, 154), (176, 140, 114)
    hair = (112, 78, 50)
    gold = (210, 186, 96)
    def body_col(u, v):
        return tuple(suit[i] + (suit_d[i] - suit[i]) * max(0.0, min(1.0, (v + 2.4) / 4.8)) for i in range(3))
    def cape_col(u, v):
        wave = math.sin(u * 0.9) * 0.5 + 0.5
        return tuple(cape[i] + (cape_d[i] - cape[i]) * wave for i in range(3))
    parts = [
        (poly((17.0, -3.0), (17.0, 3.0), (11.0, 5.6), (6.0, 6.6), (2.0, 5.4), (-1.0, 7.6), (0.5, 3.0), (3.0, 1.4),
              (8.0, 0.2), (12.0, -2.8)), cape_col),                             # cape, streaming back and to one side
        (capsule((19.6, 0.0), (15.5, -0.6), 2.4), hair),                        # long hair flowing back
        (capsule((3.0, -1.0), (10.5, -1.0), 1.15), body_col),                   # legs
        (capsule((3.0, 1.0), (10.5, 1.0), 1.15), body_col),
        (capsule((0.6, -1.0), (2.6, -1.0), 1.2), glove),                        # boots
        (capsule((0.6, 1.0), (2.6, 1.0), 1.2), glove),
        (capsule((11.0, 0.0), (16.6, 0.0), 2.5), body_col),                     # body
        (rect(10.2, 11.2, -2.2, 2.2), gold),                                    # belt
        (poly((15.6, 0.0), (14.4, -1.0), (13.2, 0.0), (14.4, 1.0)), gold),      # chest emblem
        (capsule((16.8, 3.0), (11.6, 3.6), 0.9), body_col),                     # the arm at her side
        (disc((11.2, 3.7), 1.1), glove),
        (capsule((16.8, -2.8), (23.0, -3.2), 0.95), body_col),                  # the arm thrust up
        (disc((24.0, -3.2), 1.45), glove),                                      # the fist
        (disc((19.8, 0.0), 2.05), lambda u, v: skin if v < 0.9 else skin_d),    # head
        (poly((20.0, -2.1), (22.2, -1.4), (22.2, 1.4), (20.0, 2.1), (21.0, 0.0)), hair),   # hair on top
    ]
    img = draw(parts, 32, angle=62)
    px = img.load()
    for x, y in ((12, 17), (17, 13), (9, 21)):                               # scuffed paint
        if px[x, y][3]:
            r, g, b, a = px[x, y]; px[x, y] = (min(255, r + 40), min(255, g + 40), min(255, b + 40), a)
    return img

def straw():
    """A bendy drinking straw: a long diagonal tube with long, slow red spiral stripes, a ridged bend, and a short
    end tipped a little to one side."""
    path = [(0.0, 0.0), (20.0, 0.0), (22.0, 0.5), (23.8, 1.5), (26.5, 3.2)]
    lengths = [0.0]
    for a, b in zip(path, path[1:]):
        lengths.append(lengths[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    def nearest(p):
        best = None
        for i, (a, b) in enumerate(zip(path, path[1:])):
            d, t = _seg_dist(p, a, b)
            if best is None or d < best[0]:
                sx, sy = b[0] - a[0], b[1] - a[1]
                side = (p[0] - a[0]) * sy - (p[1] - a[1]) * sx
                best = (d, lengths[i] + t * (lengths[i + 1] - lengths[i]), math.copysign(d, side))
        return best
    R = 1.35
    def col(u, v):
        d, along, w = nearest((u, v))
        shade = 1.06 - 0.32 * (w / R + 1) / 2
        if 20.0 <= along <= 23.8:                           # the ridged bend
            base = (236, 236, 240) if int(along * 1.8) % 2 else (186, 188, 196)
        else:
            base = (232, 50, 46) if (along * 0.22 + w * 0.9) % 1.6 < 0.55 else (250, 250, 250)
        return tuple(k * shade for k in base)
    parts = [(lambda p: nearest(p)[0] <= R, col)]
    return draw(parts, 32, angle=45)

# ---------------------------------------------------------------- made from the Pelt picture
def _pelt():
    return Image.open(os.path.join(OUT, "humanskin.png")).convert("RGBA")

def rotted_pelt():
    """The Pelt, rotting: sickly yellow-green and grey, darker blotches, and a few holes eaten through."""
    img = _pelt(); W, H = img.size
    rnd = random.Random(7)
    noise = Image.effect_noise((W // 8, H // 8), 64).resize((W, H), Image.BICUBIC).filter(ImageFilter.GaussianBlur(6))
    np_, ip = noise.load(), img.load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = ip[x, y]
            if a == 0:
                continue
            grey = (r + g + b) / 3
            n = np_[x, y] / 255.0
            # pale grey-yellow overall, greener and darker where it's rotting most
            rot = max(0.0, min(1.0, (n - 0.45) * 2.2))
            base = (grey * 0.95 + 18, grey * 0.92 + 14, grey * 0.62)
            sick = (grey * 0.55 + 20, grey * 0.62 + 18, grey * 0.32)
            col = tuple(base[i] + (sick[i] - base[i]) * rot for i in range(3))
            ip[x, y] = tuple(max(0, min(255, int(v))) for v in col) + (a,)
    # holes: dark rims around transparent centres
    d_rim = Image.new("L", (W, H), 0); dr = ImageDraw.Draw(d_rim)
    holes = Image.new("L", (W, H), 0); dh = ImageDraw.Draw(holes)
    for _ in range(6):
        cx, cy = rnd.randint(int(W * 0.2), int(W * 0.8)), rnd.randint(int(H * 0.2), int(H * 0.85))
        if ip[cx, cy][3] == 0:
            continue
        rx, ry = rnd.randint(10, 26), rnd.randint(8, 20)
        dr.ellipse((cx - rx * 2, cy - ry * 2, cx + rx * 2, cy + ry * 2), fill=200)
        dh.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=255)
    d_rim = d_rim.filter(ImageFilter.GaussianBlur(10))
    rim, hole = d_rim.load(), holes.filter(ImageFilter.GaussianBlur(2)).load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = ip[x, y]
            if a == 0:
                continue
            k = rim[x, y] / 255.0 * 0.7
            r, g, b = (int(r * (1 - k) + 46 * k), int(g * (1 - k) + 38 * k), int(b * (1 - k) + 22 * k))
            ip[x, y] = (r, g, b, int(a * (1 - hole[x, y] / 255.0)))
    return img

def graft():
    """A ragged flap of skin torn off a body: the Pelt's skin with torn, bloody edges and a little blood running
    from the bottom."""
    pelt = _pelt(); W, H = pelt.size
    S = 256
    piece = pelt.crop((int(W * 0.30), int(H * 0.30), int(W * 0.72), int(H * 0.74))).resize((S, S), Image.LANCZOS)
    rnd = random.Random(11)
    # a lumpy outline with torn, jagged edges
    pts = []
    for i in range(72):
        t = 2 * math.pi * i / 72
        r = 92 + 14 * math.sin(3 * t + 0.6) + 9 * math.sin(5 * t + 2.0) + rnd.uniform(-9, 9)
        pts.append((S / 2 + math.cos(t) * r * 1.05, S / 2 + math.sin(t) * r * 0.86))
    mask = Image.new("L", (S, S), 0); ImageDraw.Draw(mask).polygon(pts, fill=255)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0)); out.paste(piece, (0, 0), mask)
    op = out.load()
    # bloody, raw torn edge
    edge = Image.new("L", (S, S), 0); ImageDraw.Draw(edge).line(pts + [pts[0]], fill=255, width=14)
    edge = edge.filter(ImageFilter.GaussianBlur(4)).load()
    for y in range(S):
        for x in range(S):
            r, g, b, a = op[x, y]
            if not a:
                continue
            k = min(1.0, edge[x, y] / 255.0 * 1.2)
            n = rnd.random() * 0.25
            op[x, y] = (int(r * (1 - k) + (150 - 40 * n) * k), int(g * (1 - k) + (30 + 10 * n) * k),
                        int(b * (1 - k) + (34 + 8 * n) * k), a)
    d = ImageDraw.Draw(out)
    # blood running from the bottom edge
    for x0, ln in ((96, 26), (132, 40), (160, 18)):
        y0 = max(y for y in range(S) if mask.getpixel((x0, y)))
        d.line([(x0, y0 - 6), (x0, y0 + ln)], fill=(120, 14, 18, 255), width=5)
        d.ellipse((x0 - 4, y0 + ln - 3, x0 + 4, y0 + ln + 5), fill=(120, 14, 18, 255))
    return out

def toy_gun_scope():
    """The Toy Gun's scope overlay (256x256, drawn over the middle of the screen while aiming, black around it): a round
    view through cheap smoky plastic, darkening and tinting brown toward its rim, with a thin crosshair."""
    S = 256
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255)); px = img.load()
    c, r = S / 2, S / 2 - 6
    for y in range(S):
        for x in range(S):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c) / r
            if d < 1.0:
                edge = max(0.0, (d - 0.72) / 0.28)
                px[x, y] = (34, 24, 16, int(255 * min(1.0, 0.08 + edge ** 1.6)))
            elif d < 1.04:
                px[x, y] = (12, 9, 7, 255)                   # the scope's rim
    d = ImageDraw.Draw(img)
    for a0, a1 in ((14, 100), (156, 242)):                    # crosshair, with a gap in the middle
        d.line([(a0, c), (a1, c)], fill=(10, 10, 10, 230), width=2)
        d.line([(c, a0), (c, a1)], fill=(10, 10, 10, 230), width=2)
    d.rectangle((c - 2, c - 2, c + 1, c + 1), fill=(180, 30, 24, 230))   # a red dot exactly in the middle (pixels 126-129)
    return img

# Textures that don't go in textures/item/.
ELSEWHERE = {"toy_gun_scope": os.path.join(os.path.dirname(OUT), "misc")}

ITEMS = {f.__name__: f for f in (toy_gun_scope, berry_pile, aspiraton_drop, scalpel, holy_text, salt, straw, toy_gun, heroine, crank,
                                 her_favorite_dress, grape_hugs, rotted_pelt, graft)}

if __name__ == "__main__":
    names = sys.argv[1].split(",") if len(sys.argv) > 1 else list(ITEMS)
    for name in names:
        folder = ELSEWHERE.get(name, OUT)
        os.makedirs(folder, exist_ok=True)
        ITEMS[name]().save(os.path.join(folder, name + ".png"))
        print("wrote", name + ".png")
