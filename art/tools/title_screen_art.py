"""Draws the title screen's art (see client/HorrorTitleScreen.java) into
src/main/resources/assets/horrormod/textures/gui/title/:

    name.png        GIVE ME BACK MY SKIN MARYL in IM Fell English Italic (art/fonts, SIL Open Font License), slanted
                    far over, patchy like old ink, in a dried-blood red
    odie.png        Odie (his real model, from odie_model.py) tipping his hat to the viewer: his hand pinches the brim
                    at his temple, the hat lifted and tilted toward us, his head bowed a little, his face lost in
                    shadow; lit dimly from the front left with a faint cold rim light, fading into the dark at his
                    feet. The screen draws him as tall as itself, toward the right
    blood_0.png ... blood_4.png
                    the five menu buttons' smears of blood (one each, all different), 450x60 per frame (the button's
                    150x20 at 3x), frames stacked top to bottom: BLOOD_FLOW_FRAMES of the hover flowing in (frame 0:
                    at rest, dull and dry; the last: hovered, a brighter wet red, the pool a little longer, its drips
                    run down), then BLOOD_SHEEN_FRAMES of a wet sheen gliding along it, looped while it stays hovered

Run: python3 art/tools/title_screen_art.py   (needs Pillow; re-run after changing Odie's model; takes a minute or two)
"""
import importlib.util, math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "horrormod", "textures")
OUT_DIR = os.path.join(ASSETS, "gui", "title")

_argv = sys.argv; sys.argv = ["x", "/tmp", "/tmp"]
_spec = importlib.util.spec_from_file_location("skinwalker_texture", os.path.join(HERE, "skinwalker_texture.py"))
tx = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(tx); sys.argv = _argv
_spec = importlib.util.spec_from_file_location("odie_model", os.path.join(HERE, "odie_model.py"))
_argv = sys.argv; sys.argv = ["x"]
od = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(od); sys.argv = _argv

def lerp(a, b, t): return a + (b - a) * t
def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(lerp(c1[i], c2[i], t))) for i in range(3))
def smooth(t): t = max(0.0, min(1.0, t)); return t * t * (3 - 2 * t)

# ---------------------------------------------------------------- the name, along the bottom
NAME = "GIVE ME BACK MY SKIN MARYL"
NAME_RED = (138, 18, 24)        # dried-blood red, like the menu buttons
NAME_SLANT = 0.42               # far more slanted than a normal italic (horizontal shift per pixel of height)

def name_text():
    font = ImageFont.truetype(os.path.join(ROOT, "art", "fonts", "IMFellEnglish-Italic.ttf"), 120)
    asc, desc = font.getmetrics()
    pad = 40
    W = int(font.getlength(NAME) + pad * 2 + (asc + desc) * NAME_SLANT); H = asc + desc + pad * 2
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).text((pad, pad), NAME, font=font, fill=255)
    # lean the letters further (top moves right)
    mask = mask.transform((W, H), Image.AFFINE, (1, NAME_SLANT, -NAME_SLANT * H, 0, 1, 0), resample=Image.BICUBIC)
    # faded: soft-edged and see-through, patchy like old ink
    mask = mask.filter(ImageFilter.GaussianBlur(1.2))
    ink = Image.new("L", (W, H)); ip = ink.load(); mp = mask.load()
    for y in range(H):
        for x in range(W):
            n = tx.fbm(x * 0.02, y * 0.02, 0.0, 91)
            ip[x, y] = int(mp[x, y] * (0.72 + 0.28 * n))
    out = Image.new("RGBA", (W, H), NAME_RED + (0,)); out.putalpha(ink)
    return out.crop(out.getbbox())

# ---------------------------------------------------------------- Odie, tipping his hat
HEAD_DIP = 9.0          # degrees his head bows as he tips it
HAT_LIFT = 1.6          # px the hat comes up off his head
HAT_TILT = -16.0        # degrees the hat tilts about its own side-to-side axis (this way: toward the viewer)
HAT_TURN = 40.0         # the hat turned about its own axis, so the point his hand holds is at his temple, not his face

def rx(deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return ((1, 0, 0), (0, c, -s), (0, s, c))

def ry(deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return ((c, 0, s), (0, 1, 0), (-s, 0, c))

def mmul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))

def tip_pose():
    """Standing, head bowed a little, his right hand holding the hat lifted and tilted off his head (the hand placed
    by the same grip he uses to take it off in his wave animation, so it holds the brim)."""
    pose = dict(od.STAND)
    h = pose.get("head", (0, 0, 0))
    pose["head"] = (h[0] + HEAD_DIP, h[1], h[2])
    on_head = od.hat_on_head(pose)
    pv = od.PIVOT["hat"]
    R = mmul(ry(HAT_TURN), rx(HAT_TILT))
    local = (R, od.sub(pv, od.mvec(R, pv)))                 # tilt about the hat's own pivot
    hat = od.compose(on_head, local)
    hat = (hat[0], od.add(hat[1], (0.0, HAT_LIFT, 0.0)))     # and lift it
    od.place_hand(pose, "right", od.hand_for_hat(hat), od.RIGHT_LIFT_POLE)
    od.hat_bone_pose(pose, hat)
    return pose

YAW = -14.0             # turned a little toward the middle of the screen (degrees)
CAM_DIST = 150.0        # px of model space
CAM_Y = 34.0            # the camera at about his chest: looking a little up at his face

def render(pose, size, light_dir=(-0.55, 0.35, -0.75)):
    """His model drawn into a transparent RGBA image of size (w, h), feet near the bottom, 3x supersampled."""
    w, h = size; SS = 3; RW, RH = w * SS, h * SS
    quads = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
    cy, sy = math.cos(math.radians(YAW)), math.sin(math.radians(YAW))
    def view(p):
        x, y, z = -p[0], p[1], p[2]                            # GeckoLib mirrors x: his left on our right
        x, z = x * cy - z * sy, x * sy + z * cy
        return (x, y - CAM_Y, z + CAM_DIST)
    def proj0(v):
        return (v[0] / v[2], -v[1] / v[2])
    L = od.norm(light_dir)
    rim_dir = od.norm((0.7, 0.4, 0.6))
    polys = []
    for name, bone, a, b, mat in od.CUBES:
        R, t = od.transform(bone, pose)
        pts = [view(od.add(od.mvec(R, (x, y, z)), t)) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])]
        for q in quads:
            vq = [pts[i] for i in q]
            n = od.norm(od.cross(od.sub(vq[1], vq[0]), od.sub(vq[3], vq[0])))
            cen = tuple(sum(p[i] for p in vq) / 4 for i in range(3))
            if od.dot(n, cen) > 0:
                n = tuple(-v for v in n)
            if od.dot(n, cen) > 0:
                continue
            diffuse = max(0.0, -od.dot(n, L))
            rim = max(0.0, od.dot(n, rim_dir)) ** 3
            base = od.COLOURS[mat]
            k = 0.55 + 0.75 * diffuse
            col = tuple(min(255, int(c * k)) for c in base)
            col = mix(col, (96, 104, 128), 0.45 * rim)
            # his face stays lost in the hat's shadow
            if mat in ("face", "skin"):
                col = tuple(int(c * 0.55) for c in col)
            polys.append((sum(p[2] for p in vq) / 4, [proj0(p) for p in vq], col))
    # fit him to the frame: 94% of its height, feet 2% from the bottom, centred across
    xs = [p[0] for _, q, _ in polys for p in q]; ys = [p[1] for _, q, _ in polys for p in q]
    k = RH * 0.94 / (max(ys) - min(ys))
    ox = RW * 0.5 - k * (min(xs) + max(xs)) / 2; oy = RH * 0.98 - k * max(ys)
    img = Image.new("RGBA", (RW, RH), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    for _, pts, col in sorted(polys, key=lambda p: -p[0]):
        d.polygon([(ox + k * x, oy + k * y) for x, y in pts], fill=col + (255,))
    img = img.resize((w, h), Image.LANCZOS)
    # fade into the dark toward his feet, and a little everywhere (faded like an old photograph)
    px = img.load()
    for y in range(h):
        k = 1.0 - 0.85 * smooth((y / h - 0.62) / 0.36)
        for x in range(w):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (int(r * 0.9), int(g * 0.9), int(b * 0.92), int(a * k))
    return img

# ---------------------------------------------------------------- the menu buttons: smears of blood
BLOOD_FLOW_FRAMES, BLOOD_SHEEN_FRAMES = 8, 12
BLOOD_W, BLOOD_H = 450, 60

def noise2(seed):
    r = random.Random(seed); grads = {}
    def g(ix, iz):
        if (ix, iz) not in grads:
            a = r.random() * 2 * math.pi; grads[(ix, iz)] = (math.cos(a), math.sin(a))
        return grads[(ix, iz)]
    def nf(x, z):
        ix, iz = math.floor(x), math.floor(z); fx, fz = x - ix, z - iz
        def dot(cx, cz):
            gx, gz = g(ix + cx, iz + cz); return gx * (fx - cx) + gz * (fz - cz)
        u, v = smooth(fx), smooth(fz)
        return lerp(lerp(dot(0, 0), dot(1, 0), u), lerp(dot(0, 1), dot(1, 1), u), v)
    return nf

def blood_smear(w, h, seed, lit=False, flow=None, phase=0.0):
    """Quieter than the second take: a pool against the screen edge running well past the label, darker and
    duller, a short wipe, rarely a drip, a few droplets. flow (0 resting .. 1 hovered) is the hover: the blood
    brightens to a wet red, the pool creeps a little further right, its drips run down and lengthen, and a wet
    sheen slides along it; phase (0..1) moves that sheen while it stays hovered."""
    if flow is None:
        flow = 1.0 if lit else 0.0
    r = random.Random(seed)
    n = noise2(seed + 50)
    blobs = []
    x = -h * 0.1
    reach = 0.6 + 0.07 * flow
    while x < w * 0.67:                                          # well past the longest label
        rad = h * r.uniform(0.36, 0.5) * (1.0 - 0.45 * max(0.0, x) / w)
        if x < w * reach:
            blobs.append((x, h * 0.5 + r.uniform(-0.08, 0.08) * h, rad))
        else:
            r.uniform(-0.08, 0.08)
        x += rad * r.uniform(0.5, 0.85)
    wet = mix((70, 6, 11), (110, 10, 17), flow)
    dried = mix((36, 3, 6), (48, 4, 8), flow)
    sheen_x = (phase * 1.5 - 0.25) * w
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0)); px = img.load()
    for yy in range(h):
        for xx in range(w):
            u = xx / w
            k = 6.0
            tot = sum(math.exp(k * (1.0 - math.hypot(xx - bx, (yy - by) * 1.1) / br)) for bx, by, br in blobs)
            field = math.log(tot) / k - 0.25 if tot > 0 else -1.0
            field += 0.08 * n(xx / 10.0, yy / 10.0) + 0.03 * n(xx / 2.0, yy / 2.0)
            if field > 0:
                a = min(0.92, field * 14.0)
                inner = min(1.0, field / 0.5)
            else:
                streak = n(xx / 60.0, yy / 3.2 + 7.0) + 0.3 * n(xx / 12.0, yy / 2.0)
                band = 1.0 - abs(yy - h * 0.5) / (h * 0.32)
                trail = smooth(1.0 - (u - 0.45 - 0.05 * flow) / 0.5)
                a = min(0.6, max(0.0, streak * 1.8 + 0.25) * max(0.0, band) * trail)
                inner = 0.1
            if a <= 0.02:
                continue
            c = mix(dried, wet, 0.3 + 0.7 * inner)
            c = mix(c, dried, 0.2 * (0.5 + n(xx / 5.0 + 40, yy / 5.0)))
            if flow > 0 and inner > 0.4:                         # the wet sheen sliding along the pool
                g = math.exp(-((xx - sheen_x - (yy - h * 0.3) * 0.8) / (w * 0.05)) ** 2)
                g *= max(0.0, 1.0 - abs(yy - h * 0.34) / (h * 0.22))
                c = mix(c, (176, 46, 50), 0.45 * flow * g * inner)
            px[xx, yy] = c + (int(255 * a),)
    d = ImageDraw.Draw(img)
    drips = r.randint(1, 2)
    for i in range(drips):                                       # drips: short at rest, running down when hovered
        bx, by, br = blobs[min(len(blobs) - 1, r.randint(0, 3))]
        x0 = int(max(4, bx + r.uniform(0.0, 0.4) * br)); y0 = int(by + br * 0.7)
        rest = h * r.uniform(0.05, 0.14) if i == 0 and r.random() < 0.6 else 0.0
        ln = rest + flow * h * r.uniform(0.15, 0.32)
        y1 = min(h - 5, int(y0 + ln))
        if y1 > y0 + 1:
            d.rectangle((x0, y0, x0 + 2, y1), fill=wet + (210,))
            d.ellipse((x0 - 1, y1 - 1, x0 + 3, y1 + 3), fill=wet + (220,))
    for _ in range(r.randint(4, 9)):                             # a few droplets
        x0 = int(w * min(0.95, 0.45 + abs(r.gauss(0, 0.2)))); y0 = r.randint(3, h - 6); sz = r.choice((1, 2, 2, 3))
        d.ellipse((x0, y0, x0 + sz, y0 + sz), fill=wet + (r.randint(120, 200),))
    return img

def blood_strip(index):
    """Button index's smear: the hover flowing in, then the sheen's loop (see the list at the top)."""
    frames = [blood_smear(BLOOD_W, BLOOD_H, 100 + index, flow=smooth(f / (BLOOD_FLOW_FRAMES - 1)), phase=0.0)
              for f in range(BLOOD_FLOW_FRAMES)]
    frames += [blood_smear(BLOOD_W, BLOOD_H, 100 + index, flow=1.0, phase=f / BLOOD_SHEEN_FRAMES)
               for f in range(BLOOD_SHEEN_FRAMES)]
    strip = Image.new("RGBA", (BLOOD_W, BLOOD_H * len(frames)), (0, 0, 0, 0))
    for k, f in enumerate(frames):
        strip.paste(f, (0, k * BLOOD_H))
    return strip

if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    name_text().save(os.path.join(OUT_DIR, "name.png"))
    odie = render(tip_pose(), (1040, 1456)); odie.save(os.path.join(OUT_DIR, "odie.png"))
    for i in range(5):
        blood_strip(i).save(os.path.join(OUT_DIR, f"blood_{i}.png"))
    print("wrote", OUT_DIR, "name.png, odie.png", odie.size, "and blood_0..4.png")
