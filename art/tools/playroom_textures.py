"""Textures for the Child's Playroom.

    textures/block/playroom_exit.png (+ .mcmeta)  16x16, 12 frames: the way out, a dark rot eating into the house
                                                  (near-black, deep red-purple veins crawling and pulsing through it)
    The room's painted outdoors (flat colour, soft brushstrokes: painted, not real; all tile seamlessly):
    textures/block/painted_sky.png                Minecraft's daytime sky blue, faint streaks
    textures/block/painted_cloud.png              a blocky white cloud, shaded grey-blue toward its underside
    textures/block/painted_sun.png                the sun: a warm pale yellow, brushed
    textures/block/painted_leaves.png             a tree's crown, mid green, darker dabs
    textures/block/painted_leaves_dark.png        the same, a deep green (the shaded side, the trees behind)
    textures/block/painted_bark.png               a brown trunk, vertical strokes
    textures/block/painted_grass.png              the painted ground along the foot of the walls
Run: python3 art/tools/playroom_textures.py   (needs Pillow; overwrites those files)
"""
import math, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod", "textures", "block"))
FRAMES = 12

def _hash(x, y, k):
    h = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535.0

def _noise(x, y, k):
    """Smooth value noise, tiling every 16 (so the block's faces join up)."""
    ix, iy = math.floor(x), math.floor(y); fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    w = lambda v: v % 4
    a, b = _hash(w(ix), w(iy), k), _hash(w(ix + 1), w(iy), k); c, d = _hash(w(ix), w(iy + 1), k), _hash(w(ix + 1), w(iy + 1), k)
    return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy

def exit_frame(f):
    img = Image.new("RGBA", (16, 16))
    t = f / FRAMES
    for y in range(16):
        for x in range(16):
            # warped, slowly crawling veins over a near-black body
            wx = x / 4.0 + 0.6 * math.sin(2 * math.pi * (t + y / 16.0))
            wy = y / 4.0 + 0.6 * math.cos(2 * math.pi * (t + x / 16.0))
            n = _noise(wx, wy, 3)
            ridge = 1.0 - abs(2.0 * n - 1.0)
            pulse = 0.5 + 0.5 * math.sin(2 * math.pi * t + n * 6.0)
            base = (14, 6, 12)
            vein = max(0.0, (ridge - 0.72) / 0.28)
            r = base[0] + vein * (110 + 60 * pulse)
            g = base[1] + vein * (10 + 14 * pulse)
            b = base[2] + vein * (40 + 30 * pulse)
            img.putpixel((x, y), (min(255, int(r)), min(255, int(g)), min(255, int(b)), 255))
    return img

def painted(base, dark, light, k, streak=(0.5, 0.5), dabs=0.0):
    """A tiling 16x16 painted surface: base colour, broad soft strokes toward dark and light, optional dabs.
    streak: the noise's scale across and down (multiples of 0.25, so it repeats every 16 pixels): the larger one
    makes the strokes run the other way (horizontal strokes: a small x scale and a larger y one)."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = _noise(x * streak[0], y * streak[1], k)
            c = tuple(base[i] + (dark[i] - base[i]) * max(0.0, 0.5 - n) * 1.4 + (light[i] - base[i]) * max(0.0, n - 0.5) * 1.4
                      for i in range(3))
            if dabs and _noise(x / 2.0, y / 2.0, k + 7) > 1.0 - dabs:
                c = tuple(c[i] * 0.82 for i in range(3))
            img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c) + (255,))
    return img

def cloud():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = _noise(x / 4.0, y / 4.0, 21)
            c = (238 + 14 * n, 242 + 10 * n, 248 + 6 * n)
            img.putpixel((x, y), tuple(max(0, min(255, int(v))) for v in c) + (255,))
    return img

if __name__ == "__main__":
    painted((121, 166, 255), (108, 152, 246), (138, 180, 255), 31, streak=(0.25, 0.75)).save(os.path.join(TEX, "painted_sky.png"))
    cloud().save(os.path.join(TEX, "painted_cloud.png"))
    painted((255, 232, 128), (250, 212, 96), (255, 246, 190), 71, streak=(0.5, 0.5)).save(os.path.join(TEX, "painted_sun.png"))
    painted((72, 136, 52), (50, 104, 38), (102, 164, 70), 41, streak=(0.5, 0.5), dabs=0.3).save(os.path.join(TEX, "painted_leaves.png"))
    painted((44, 94, 40), (30, 70, 30), (62, 118, 52), 43, streak=(0.5, 0.5), dabs=0.3).save(os.path.join(TEX, "painted_leaves_dark.png"))
    painted((112, 78, 48), (84, 56, 34), (138, 100, 64), 51, streak=(0.75, 0.25)).save(os.path.join(TEX, "painted_bark.png"))
    painted((92, 152, 58), (72, 128, 46), (116, 176, 74), 61, streak=(0.25, 0.5)).save(os.path.join(TEX, "painted_grass.png"))
    sheet = Image.new("RGBA", (16, 16 * FRAMES))
    for f in range(FRAMES):
        sheet.paste(exit_frame(f), (0, 16 * f))
    sheet.save(os.path.join(TEX, "playroom_exit.png"))
    open(os.path.join(TEX, "playroom_exit.png.mcmeta"), "w").write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')
    print("wrote playroom_exit.png")
