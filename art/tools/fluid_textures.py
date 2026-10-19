"""Paints the animated liquid textures into src/main/resources/assets/horrormod/textures/block/:

    nutrinon_still.png / nutrinon_flowing.png   bright red blood: slow lighter swirls on the surface, darker clots;
                                                streaks running down the flow
    tar_still.png / tar_flowing.png             near-black, thick and glossy: faint oily sheen drifting slowly;
                                                dull streaks creeping down the flow

Like vanilla's: still frames are 16x16, flowing frames 32x32, stacked top to bottom with a .mcmeta saying how long
each frame lasts. Every frame tiles seamlessly, and the animation loops without a jump. The colours are painted
in, so the fluid types don't tint them (NutrinonFluidType, TarFluidType).

Run: python3 art/tools/fluid_textures.py   (needs Pillow; overwrites those files)
"""
import json, math, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod", "textures", "block"))
FRAMES = 32

def _hash(ix, iy, it, seed):
    h = (ix * 374761393 + iy * 668265263 + it * 2147483647 + seed * 144269504) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0

def noise(x, y, t, px, py, pt, seed):
    """Smooth value noise that repeats every px along x, py along y and pt along t (so tiles and loops match)."""
    ix, iy, it = math.floor(x), math.floor(y), math.floor(t)
    fx, fy, ft = x - ix, y - iy, t - it
    sx, sy, st = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), ft * ft * (3 - 2 * ft)
    def c(i, j, k):
        return _hash((ix + i) % px, (iy + j) % py, (it + k) % pt, seed)
    def lerp(a, b, k):
        return a + (b - a) * k
    return lerp(lerp(lerp(c(0, 0, 0), c(1, 0, 0), sx), lerp(c(0, 1, 0), c(1, 1, 0), sx), sy),
                lerp(lerp(c(0, 0, 1), c(1, 0, 1), sx), lerp(c(0, 1, 1), c(1, 1, 1), sx), sy), st)

def fbm(x, y, t, px, py, pt, seed, octaves=3):
    total, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        f = 2 ** o
        total += amp * noise(x * f, y * f, t, px * f, py * f, pt, seed + o * 31); norm += amp; amp *= 0.5
    return total / norm

def mix(a, b, k):
    k = max(0.0, min(1.0, k))
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))

def smoothstep(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)

def still(size, colour, seed):
    """Frames of a slowly churning surface: noise drifting through time, looping after FRAMES frames."""
    cells = 4                                          # noise cells across one tile
    img = Image.new("RGBA", (size, size * FRAMES)); px = img.load()
    for f in range(FRAMES):
        t = f / FRAMES * 4                             # 4 noise steps per loop
        for y in range(size):
            for x in range(size):
                n = fbm(x / size * cells, y / size * cells, t, cells, cells, 4, seed)
                px[x, f * size + y] = colour(n, x, y) + (255,)
    return img

def flowing(size, colour, seed):
    """Frames of liquid running down: noise stretched along the flow and scrolled one pixel per frame, so the
    streaks slide down the texture and line up again when the animation loops."""
    img = Image.new("RGBA", (size, size * FRAMES)); px = img.load()
    across, along = 6, 2                               # streaks: many across, long along the flow
    for f in range(FRAMES):
        shift = f / FRAMES                             # one full tile per loop
        for y in range(size):
            for x in range(size):
                v = (y / size - shift) % 1.0
                n = fbm(x / size * across, v * along, f / FRAMES * 2, across, along, 2, seed)
                px[x, f * size + y] = colour(n, x, y) + (255,)
    return img

# ---------------------------------------------------------------- the liquids
def nutrinon(n, x, y):
    base, light, dark = (196, 14, 28), (236, 64, 66), (118, 4, 14)
    c = mix(base, dark, smoothstep(0.34, 0.18, n) * 0.7)        # a few darker clots in the lows
    return mix(c, light, smoothstep(0.62, 0.8, n))              # lighter swirls on the highs

def nutrinon_flow(n, x, y):
    base, light, dark = (190, 12, 26), (240, 76, 74), (110, 4, 12)
    c = mix(base, dark, smoothstep(0.45, 0.22, n))
    return mix(c, light, smoothstep(0.66, 0.84, n))

def tar(n, x, y):
    base, sheen, deep = (24, 19, 16), (78, 70, 66), (10, 8, 7)
    c = mix(base, deep, smoothstep(0.45, 0.2, n))
    return mix(c, sheen, smoothstep(0.7, 0.86, n) * 0.8)        # a faint oily sheen

def tar_flow(n, x, y):
    base, sheen, deep = (22, 17, 14), (70, 62, 58), (9, 7, 6)
    c = mix(base, deep, smoothstep(0.48, 0.22, n))
    return mix(c, sheen, smoothstep(0.64, 0.82, n) * 0.8)

TEXTURES = {
    "nutrinon_still": (lambda: still(16, nutrinon, 5), 2),       # frame time in ticks: Nutrinon moves like water
    "nutrinon_flowing": (lambda: flowing(32, nutrinon_flow, 6), 2),
    "tar_still": (lambda: still(16, tar, 7), 5),                 # Tar is thick and slow, like lava
    "tar_flowing": (lambda: flowing(32, tar_flow, 8), 4),
}

if __name__ == "__main__":
    for name, (make, frametime) in TEXTURES.items():
        make().save(os.path.join(OUT, name + ".png"))
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w") as f:
            json.dump({"animation": {"frametime": frametime}}, f, indent=2)
            f.write("\n")
        print("wrote", name)
