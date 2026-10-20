"""The Angel: a 'biblically accurate' angel, after the user's drawing, changed as they asked: a great ring hanging in
the air with one huge eye in its middle and three wings on each side; none of the drawing's other eyes (not the four
round the middle one, not the ones on the ring). The ring is pale gold-white, ridged, with grooves round it; the eye
is almond-shaped, a white with faint red veins, a dark red iris and a slit pupil, between pale lids; the wings are
long and feathered, grey-white at the root darkening toward the tips, the long flight feathers fanning out past them.
Inside the ring, hidden until it opens, a white disc: the way to its dimension. About 1.5 blocks across the ring and 5
from wingtip to wingtip.

Writes:
    src/main/resources/assets/horrormod/geo/entity/angel.geo.json              GeckoLib geometry
    src/main/resources/assets/horrormod/animations/entity/angel.animation.json its animations
    src/main/resources/assets/horrormod/textures/entity/angel.png              the texture (cutout: the feather tips)
    art/angel.bbmodel                                                           a Blockbench project to look at (edit
                                                                                this script, not that file)
    [preview.png]                                                               front / three-quarter / side / back
Run: python3 art/tools/angel_model.py [preview.png]  (needs Pillow)
"""
import base64, hashlib, json, math, os, sys, uuid
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod"))
BBMODEL = os.path.normpath(os.path.join(HERE, "..", "angel.bbmodel"))

TEXELS = 4                      # texture pixels per model pixel
TEX_W, TEX_H = 1024, 2048
FACES = ("nz", "pz", "nx", "px", "py", "ny")
JSON_FACE = {"nz": "north", "pz": "south", "nx": "east", "px": "west", "py": "up", "ny": "down"}   # (as in hollow_model.py)

# ---------------------------------------------------------------- rig
C = (0.0, 18.0, 0.0)                                    # the ring's middle
R_OUT, R_IN = 12.0, 7.8                                 # the ring
SLICE = 0.75
WING_TIERS = {"upper": (8.5, 8.0), "middle": (11.4, 1.0), "lower": (9.0, -6.5)}     # root (x out, y up from C)
SEG = (9.0, 9.0, 7.0)                                   # each wing's root, middle and tip lengths
TIER_Z = {"upper": 2.6, "middle": 1.9, "lower": 1.2}    # (layered one behind another, front to back)

BONES = [("root", None, (0, 0, 0)), ("body", "root", C), ("ring", "body", C), ("portal", "body", C),
         ("eye", "body", C), ("eye_blink", "eye", C)]
for side, sx in (("right", -1), ("left", 1)):
    for tier, (rx, ry) in WING_TIERS.items():
        w = f"{side}_wing_{tier}"
        z = TIER_Z[tier]
        BONES += [(w, "body", (sx * rx, C[1] + ry, z)), (w + "_mid", w, (sx * (rx + SEG[0]), C[1] + ry, z)),
                  (w + "_tip", w + "_mid", (sx * (rx + SEG[0] + SEG[1]), C[1] + ry, z))]
PIVOT = {n: p for n, _, p in BONES}
PARENT = {n: p for n, p, _ in BONES}

CUBES = []
def cube(name, bone, a, b, mat):
    CUBES.append((name, bone, tuple(min(a[i], b[i]) for i in range(3)), tuple(max(a[i], b[i]) for i in range(3)), mat))

def disc_slices(name, bone, outer, inner, z0, z1, mat, cy=C[1], shape=None):
    """Horizontal slices filling a ring (or a disc, inner 0) round (0, cy): the stepped circle of a pixel model.
    shape(dy) can give a half-width instead of the circle's."""
    y = cy - outer
    k = 0
    while y < cy + outer - 1e-6:
        y1 = min(cy + outer, y + SLICE)
        dy = abs((y + y1) / 2 - cy)
        xo = shape(dy) if shape else math.sqrt(max(0.0, outer * outer - dy * dy))
        if xo > 0.05:
            if inner > 0 and dy < inner:
                xi = math.sqrt(inner * inner - dy * dy)
                if xo - xi > 0.05:
                    cube(f"{name}_{k}_r", bone, (-xo, y, z0), (-xi, y1, z1), mat)
                    cube(f"{name}_{k}_l", bone, (xi, y, z0), (xo, y1, z1), mat)
            else:
                cube(f"{name}_{k}", bone, (-xo, y, z0), (xo, y1, z1), mat)
        y = y1; k += 1

# the ring: a rounded band (a thick middle layer, a thinner, narrower one either side of it)
disc_slices("ring", "ring", R_OUT, R_IN, -1.4, 1.4, "ring")
disc_slices("ring_round", "ring", R_OUT - 0.9, R_IN + 0.9, -2.1, 2.1, "ring")
# the way through, hidden until the ring opens
disc_slices("portal", "portal", R_IN + 0.2, 0, -0.25, 0.25, "portal")
# the eye: almond-shaped, the white bulging forward, the iris standing out of it, lids above and below
EYE_W, EYE_H = 6.4, 3.5
almond = lambda dy: EYE_W * max(0.0, 1 - (dy / EYE_H) ** 2) ** 0.8
# (it blinks by closing to a slit: the eye_blink bone squashed flat)
disc_slices("eye_rim", "eye_blink", EYE_H + 0.45, 0, -1.2, 0.8, "lid", shape=lambda dy: almond(dy * EYE_H / (EYE_H + 0.45)) + 0.5)
disc_slices("eye_white", "eye_blink", EYE_H, 0, -1.3, 0.6, "eye_white", shape=almond)
disc_slices("eye_bulge", "eye_blink", EYE_H * 0.85, 0, -1.6, -1.3, "eye_white", shape=lambda dy: almond(dy) * 0.8)
disc_slices("iris", "eye_blink", 2.5, 0, -1.85, -1.6, "iris")
# the wings: each a bony leading edge in three parts, with rows of long feathers below it, longest at the tip
for side, sx in (("right", -1), ("left", 1)):
    for tier, (rx, ry) in WING_TIERS.items():
        w = f"{side}_wing_{tier}"
        y0, z0 = C[1] + ry, TIER_Z[tier]
        X = lambda d: sx * (rx + d)
        for seg, (bone, start, length, rows, thick) in enumerate(((w, 0.0, SEG[0], 6, 0.75), (w + "_mid", SEG[0], SEG[1], 7, 0.6),
                                                                   (w + "_tip", SEG[0] + SEG[1], SEG[2], 7, 0.45))):
            cube(f"{bone}_edge", bone, (X(start), y0 - thick, z0 - 0.5), (X(start + length + 0.3), y0 + thick, z0 + 0.5), "wing_bone")
            if seg < 2:
                cube(f"{bone}_covert", bone, (X(start + 0.5), y0 + thick - 0.1, z0 - 0.25), (X(start + length - 1.0), y0 + thick + 0.9, z0 + 0.15), "feather")
            for k in range(rows):
                top = y0 - thick * 0.6 - k * 1.45
                reach = (start + length + (k * 0.7 if seg == 0 else 1.2 + k * 1.1 if seg == 1 else 3.0 + k * 1.6))
                first = start + 0.3 + (k * 0.6 if seg == 0 else 0.0)
                z = z0 - 0.3 - 0.06 * k + (k % 2) * 0.2
                cube(f"{bone}_feather_{k}", bone, (X(first), top - 1.9, z), (X(reach), top, z + 0.3), "feather")

# ---------------------------------------------------------------- painting
def _hash(x, y, k):
    h = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535.0

def _noise(x, y, k):
    ix, iy = math.floor(x), math.floor(y); fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _hash(ix, iy, k), _hash(ix + 1, iy, k); c, d = _hash(ix, iy + 1, k), _hash(ix + 1, iy + 1, k)
    return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy

def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(len(c1)))

def scale(c, k):
    return tuple(v * k for v in c)

def texel(mat, f, s, t, p, i, fw, fh):
    """Colour and alpha (0 or 1) of one texel (p: the point on the model)."""
    a, b = CUBES[i][2], CUBES[i][3]
    shade = {"py": 1.1, "ny": 0.7, "nz": 1.0, "pz": 0.85, "nx": 0.9, "px": 0.9}[f]
    grain = 0.97 + 0.06 * _hash(int(s * fw * TEXELS), int(t * fh * TEXELS), i)
    alpha = 1.0
    x, y, z = p
    if mat == "ring":
        r = math.hypot(x - C[0], y - C[1]); ang = math.degrees(math.atan2(y - C[1], x - C[0])) % 360
        c = mix((172, 166, 150), (122, 116, 104), _noise(ang * 0.1, r * 0.6, 3))     # a pale, worn stone
        if abs(r - (R_IN + 0.7)) < 0.18 or abs(r - (R_OUT - 0.7)) < 0.18:
            c = scale(c, 0.62)                                           # grooves round it
        if (ang % 15.0) < 0.9:
            c = scale(c, 0.7)                                            # ridges across it
        if r < R_IN + 0.3:
            c = scale(c, 0.8)                                            # the inner rim, in its own shadow
    elif mat == "portal":
        r = math.hypot(x, y - C[1]); ang = math.atan2(y - C[1], x)
        swirl = _noise(ang * 2.0 + r * 0.6, r * 0.4, 7)
        c = mix((252, 252, 255), (214, 224, 246), swirl * max(0.0, r / R_IN))
    elif mat == "eye_white":
        dy = abs(y - C[1]); edge = almond(min(dy, EYE_H)) - abs(x)
        c = (236, 234, 226)
        vein = 1.0 - abs(2.0 * _noise(math.atan2(y - C[1], x) * 4.0, abs(x) * 0.8, 11) - 1.0)
        if vein > 0.9 and edge < 2.0:
            c = mix(c, (170, 40, 40), (vein - 0.9) / 0.1 * (1 - edge / 2.0))   # faint veins, toward the corners
        c = mix(c, (150, 140, 130), max(0.0, 1 - edge / 0.6) * 0.5)
    elif mat == "iris":
        r = math.hypot(x, y - C[1])
        c = mix((138, 36, 26), (34, 6, 4), r / 2.5)
        streak = _noise(math.atan2(y - C[1], x) * 6.0, r * 2.0, 13)
        c = scale(c, 0.8 + 0.4 * streak)
        if abs(x) < 0.38 * math.sqrt(max(0.0, 1 - ((y - C[1]) / 2.3) ** 2)):
            c = (6, 2, 2)                                                # the slit pupil
        if math.hypot(x + 0.9, y - C[1] - 0.9) < 0.35:
            c = (250, 250, 250)                                          # the shine on it
    elif mat == "lid":                                                   # the lids round the eye
        c = mix((176, 160, 140), (130, 114, 98), _noise(x * 1.5, y * 3.0, 17))
    elif mat == "wing_bone":
        c = mix((204, 200, 190), (168, 162, 150), _noise(x * 0.8, y * 0.8, 19))
    elif mat == "feather":
        out = abs(x)                                                     # how far out along the wing
        inner, outer = min(abs(a[0]), abs(b[0])), max(abs(a[0]), abs(b[0]))
        across = (y - a[1]) / max(1e-6, b[1] - a[1])
        left = outer - out
        if left < 2.2:                                                   # the pointed tip
            half = 0.5 * left / 2.2
            if abs(across - 0.5) > half + 0.02:
                alpha = 0.0
        c = mix((206, 204, 200), (70, 68, 68), max(0.0, (out - 9.0) / 34.0) ** 0.8)   # pale at the root, dark at the tips
        c = mix(c, scale(c, 1.25), max(0.0, 1 - abs(across - 0.5) / 0.07) * 0.7)  # the shaft
        if ((out * 1.4 - across * 1.2) % 1.0) < 0.12:
            c = scale(c, 0.9)                                            # the barbs, slanting out
        if across < 0.1:
            c = scale(c, 0.6)                                            # the edge under the feather above
        elif across > 0.9:
            c = scale(c, 1.1)
    else:
        raise KeyError(mat)
    c = tuple(v * shade * grain for v in c)
    return tuple(max(0, min(255, int(v))) for v in c), alpha

# ---------------------------------------------------------------- texture layout and painting (as in hollow_model.py)
def face_size(a, b, f):
    w, h, d = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    return {"nz": (w, h), "pz": (w, h), "nx": (d, h), "px": (d, h), "py": (w, d), "ny": (w, d)}[f]

def face_point(a, b, f, s, t):
    if f == "nz":
        return (a[0] + s * (b[0] - a[0]), b[1] - t * (b[1] - a[1]), a[2])
    if f == "pz":
        return (b[0] - s * (b[0] - a[0]), b[1] - t * (b[1] - a[1]), b[2])
    if f == "px":
        return (b[0], b[1] - t * (b[1] - a[1]), a[2] + s * (b[2] - a[2]))
    if f == "nx":
        return (a[0], b[1] - t * (b[1] - a[1]), b[2] - s * (b[2] - a[2]))
    if f == "py":
        return (b[0] - s * (b[0] - a[0]), b[1], a[2] + t * (b[2] - a[2]))
    return (b[0] - s * (b[0] - a[0]), a[1], b[2] - t * (b[2] - a[2]))

BLEED = 1

def layout():
    rects, x, y, shelf = {}, BLEED, BLEED, 0
    W = TEX_W // TEXELS
    for i, (name, bone, a, b, mat) in enumerate(CUBES):
        for f in FACES:
            w, h = face_size(a, b, f)
            cw, ch = math.ceil(w - 1e-6), math.ceil(h - 1e-6)
            if x + cw + BLEED > W:
                x, y, shelf = BLEED, y + shelf, 0
            rects[(i, f)] = (x, y, w, h); x += cw + 2 * BLEED; shelf = max(shelf, ch + 2 * BLEED)
    assert y + shelf <= TEX_H // TEXELS, "texture too small"
    return rects

def paint(rects):
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0)); px = img.load()
    for (i, f), (u, v, w, h) in rects.items():
        name, bone, a, b, mat = CUBES[i]
        x0, y0 = int(u * TEXELS), int(v * TEXELS)
        x1, y1 = x0 + max(1, math.ceil(w * TEXELS)), y0 + max(1, math.ceil(h * TEXELS))
        m = BLEED * TEXELS
        for y in range(y0 - m, y1 + m):
            for x in range(x0 - m, x1 + m):
                cx, cy = min(max(x, x0), x1 - 1), min(max(y, y0), y1 - 1)
                s, t = (cx - x0 + 0.5) / (x1 - x0), (cy - y0 + 0.5) / (y1 - y0)
                c, al = texel(mat, f, s, t, face_point(a, b, f, s, t), i, w, h)
                if 0 <= x < TEX_W and 0 <= y < TEX_H:
                    px[x, y] = c + (255 if al >= 0.5 else 0,)
    return img

# ---------------------------------------------------------------- animations
# GeckoLib's rotation convention (as in the other model scripts): a bone's matrix is Rz(-z) Ry(y) Rx(-x). The wings
# are built straight out to the sides; every animation gives their spread (BASE: the upper pair raised, the lower
# pair lowered, all swept a little back) plus its movement. Scales ("scale:bone") and positions ("pos:bone") too.
def _mmul(a, b): return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def _mvec(m, v): return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))
def _rx(a): c, s_ = math.cos(a), math.sin(a); return [[1, 0, 0], [0, c, -s_], [0, s_, c]]
def _ry(a): c, s_ = math.cos(a), math.sin(a); return [[c, 0, s_], [0, 1, 0], [-s_, 0, c]]
def _rz(a): c, s_ = math.cos(a), math.sin(a); return [[c, -s_, 0], [s_, c, 0], [0, 0, 1]]
def _rot(r):
    x, y, z = (math.radians(v) for v in r)
    return _mmul(_rz(-z), _mmul(_ry(y), _rx(-x)))
def _addv(a, b): return tuple(a[i] + b[i] for i in range(3))
def _sub(a, b): return tuple(a[i] - b[i] for i in range(3))
def _lerp(a, b, u): return tuple(a[i] + (b[i] - a[i]) * u for i in range(len(a)))
def _smooth(u):
    u = max(0.0, min(1.0, u)); return u * u * (3 - 2 * u)

def transform(bone, pose):
    """(M, t): where a bone puts a point p of its geometry: M p + t (rotation and scale, the whole chain)."""
    M, t = [[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0)
    chain = []
    while bone:
        chain.append(bone); bone = PARENT[bone]
    for b in reversed(chain):
        sc = pose.get("scale:" + b, (1, 1, 1))
        r = _mmul(_rot(pose.get(b, (0, 0, 0))), [[sc[0], 0, 0], [0, sc[1], 0], [0, 0, sc[2]]])
        pv = PIVOT[b]; off = pose.get("pos:" + b, (0, 0, 0))
        lt = _addv(off, _sub(pv, _mvec(r, pv)))
        t = _addv(t, _mvec(M, lt)); M = _mmul(M, r)
    return M, t

TIERS = ("upper", "middle", "lower")
SPREAD = {"upper": -36.0, "middle": -8.0, "lower": 26.0}         # (for the left wings; the right ones mirrored)
SWEEP = -14.0                                                    # swept a little back (left wings; right mirrored)

def wings(beat, curl=0.0, spread=0.0, lag=0.5, tier_lag=0.35, sweep=0.0):
    """The six wings: `beat` (a function of a phase offset, giving degrees: + forward) swings each, the middle and
    tip parts following behind (lag); the tiers a little apart (tier_lag); `curl` bends the parts in, `spread`
    opens the upper and lower pairs further apart."""
    pose = {}
    for side, sx in (("right", -1), ("left", 1)):
        for k, tier in enumerate(TIERS):
            w = f"{side}_wing_{tier}"
            off = k * tier_lag
            z = SPREAD[tier] + (-spread if tier == "upper" else spread if tier == "lower" else 0.0)
            pose[w] = (0.0, sx * (SWEEP + sweep + beat(off)), sx * z)
            pose[w + "_mid"] = (0.0, sx * (beat(off + lag) * 0.6 - curl), 0.0)
            pose[w + "_tip"] = (0.0, sx * (beat(off + 2 * lag) * 0.5 - curl * 1.3), 0.0)
    return pose

def closed_portal(pose):
    pose["scale:portal"] = (0.0, 0.0, 0.0)
    return pose

def open_eye(pose, lids=1.0):
    """The eye open (lids 1) or shut to a slit (0): only the eye controller's animations (eye_open, blink) set it, so
    a blink can play over anything."""
    pose["scale:eye_blink"] = (1.0, max(0.06, lids), 1.0)
    return pose

def hover(t):
    """Hanging in the air: the wings beating slowly in a wave from the top pair to the bottom, the whole of it rising
    and falling a little with the beats."""
    ph = 2 * math.pi * t / 4.0
    pose = wings(lambda off: 16 * math.sin(ph - off), curl=4.0)
    pose["pos:body"] = (0.0, 0.8 * math.sin(ph - 1.2), 0.0)
    return closed_portal(pose)

def fly(t):
    """Flying off: hard, deep beats, all six together, leaning into it."""
    ph = 2 * math.pi * t / 1.2
    pose = wings(lambda off: 34 * math.sin(ph - off * 0.6), curl=6.0 + 6 * max(0.0, math.cos(ph)), tier_lag=0.2)
    pose["body"] = (12.0, 0.0, 0.0)
    pose["pos:body"] = (0.0, 1.2 * math.sin(ph - 1.0), 0.0)
    return closed_portal(pose)

def ascend(t):
    """Rising away into the sky: fast, powerful beats, the wings folding a little on each upstroke."""
    ph = 2 * math.pi * t / 0.8
    pose = wings(lambda off: 40 * math.sin(ph - off * 0.5), curl=4.0 + 12 * max(0.0, -math.sin(ph)), tier_lag=0.15)
    pose["body"] = (-10.0, 0.0, 0.0)
    pose["pos:body"] = (0.0, 1.5 * math.sin(ph - 0.8), 0.0)
    return closed_portal(pose)

def descend(t):
    """Coming down: the wings spread wide and held up, gliding, only shifting a little."""
    ph = 2 * math.pi * t / 3.0
    pose = wings(lambda off: 5 * math.sin(ph - off) - 10, curl=-4.0, spread=8.0)
    pose["pos:body"] = (0.0, 0.4 * math.sin(ph), 0.0)
    return closed_portal(pose)

OPEN_RING = 1.55                                     # how much wider the ring grows when it opens

# Open, it stands on the ground like a standing mirror: the ring upright, widened, the white portal in it; the wings
# swung round behind it, the upper and middle pairs folded back, the lower pair angled down behind as its stand.
FOLDED = {"upper": ((0, -96, -40), (0, -28, 0), (0, -30, 0)), "middle": ((0, -102, -6), (0, -26, 0), (0, -26, 0)),
          "lower": ((0, -64, 40), (0, -12, 0), (0, -10, 0))}            # (left wings; the right ones mirrored)

def lowest(pose):
    """The lowest point of the model in this pose (leaving out anything scaled away)."""
    low = 1e9
    for name, bone, a, b, mat in CUBES:
        M, t = transform(bone, pose)
        if abs(M[0][0]) + abs(M[1][1]) < 1e-3:
            continue
        for x in (a[0], b[0]):
            for y in (a[1], b[1]):
                for z in (a[2], b[2]):
                    low = min(low, _addv(_mvec(M, (x, y, z)), t)[1])
    return low

def _open_amount(u, eye_u, portal_u, wings_u, stir=0.0):
    pose = wings(lambda off: 0.0)
    for side, sx in (("right", -1), ("left", 1)):
        for k, tier in enumerate(TIERS):
            w = f"{side}_wing_{tier}"
            for part, folded in zip((w, w + "_mid", w + "_tip"), FOLDED[tier]):
                pose[part] = _lerp(pose[part], (folded[0], sx * folded[1], sx * folded[2]), wings_u)
            pose[w] = _addv(pose[w], (0.0, sx * stir * (1 + k * 0.2), 0.0))
    k = 1 + (OPEN_RING - 1) * u
    pose["scale:ring"] = (k, k, 1.0)
    for side, sx in (("right", -1), ("left", 1)):          # the wings stay fixed to the ring's rim as it widens
        for tier, (rx, ry) in WING_TIERS.items():
            pose[f"pos:{side}_wing_{tier}"] = (sx * rx * (k - 1), ry * (k - 1), 0.0)
    e = 1 - eye_u
    pose["scale:eye"] = (max(0.001, e), max(0.001, e), max(0.001, e))
    p = OPEN_RING * portal_u
    pose["scale:portal"] = (p, p, 1.0)
    pose["pos:body"] = (0.0, 0.0, 0.0)
    pose["pos:body"] = (0.0, -lowest(pose) * wings_u, 0.0)       # (set down on the ground as it opens)
    low = lowest(pose)
    if low < 0:                                                   # (and nothing ever through it)
        pose["pos:body"] = (0.0, pose["pos:body"][1] - low, 0.0)
    return pose

def open_(t):
    """Opening the way (2 s): the eye shrinks away to nothing, the ring widens, and a white disc spreads from its
    middle to fill it; the wings swing round behind it and it settles onto the ground, standing like a mirror."""
    return _open_amount(_smooth((t - 0.3) / 1.4), _smooth(t / 0.9), _smooth((t - 0.6) / 1.4), _smooth(t / 1.6))

def opened(t):
    """Open, standing like a mirror: the white way through it shimmering (the disc swelling and easing), the folded
    wings stirring a little behind it."""
    ph = 2 * math.pi * t / 3.0
    pose = _open_amount(1.0, 1.0, 1.0, 1.0, stir=3 * math.sin(ph))
    p = OPEN_RING * (1 + 0.025 * math.sin(2 * ph))
    pose["scale:portal"] = (p, p, 1.0)
    return pose

def close(t):
    """Closing (1.5 s): the white shrinks away, the ring narrows, the eye comes back, the wings swing out again and it
    lifts off the ground."""
    u = 1 - _smooth(t / 1.5)
    return _open_amount(_smooth(u * 1.1), _smooth(u * 1.3), _smooth(u * 1.2), u)

def eye_open(t):
    """The eye open, between blinks (the eye controller's resting state)."""
    return open_eye({}, 1.0)

def blink(t):
    """A slow blink (on its own controller, over the rest)."""
    shut = math.sin(math.pi * min(1.0, t / 0.5))
    return open_eye({}, 1.0 - shut)

def spin(t):
    """The ring turning slowly round (on its own controller)."""
    return {"ring": (0.0, 0.0, 360.0 * t / 16.0)}

ANIMATIONS = [("hover", True, 4.0, hover), ("fly", True, 1.2, fly), ("ascend", True, 0.8, ascend),
              ("descend", True, 3.0, descend), ("open", "hold_on_last_frame", 2.0, open_), ("opened", True, 3.0, opened),
              ("close", "hold_on_last_frame", 1.5, close), ("eye_open", True, 0.5, eye_open), ("blink", "hold_on_last_frame", 0.5, blink), ("spin", True, 16.0, spin)]

def _continuous(prev, cur):
    if prev is None:
        return cur
    return tuple(cur[i] + round((prev[i] - cur[i]) / 360.0) * 360.0 for i in range(3))

def animations():
    out = {}
    for name, loop, length, fn in ANIMATIONS:
        steps = max(2, round(length * 20))
        frames = [(round(k * length / steps, 4), fn(round(k * length / steps, 4))) for k in range(steps + 1)]
        keys = set().union(*(f.keys() for _, f in frames))
        bones, last = {}, {}
        for t, pose in frames:
            for key in sorted(keys):
                if key.startswith("pos:"):
                    v = pose.get(key, (0, 0, 0))
                    bones.setdefault(key[4:], {}).setdefault("position", {})[str(t)] = [round(c, 3) for c in v]
                elif key.startswith("scale:"):
                    v = pose.get(key, (1, 1, 1))
                    bones.setdefault(key[6:], {}).setdefault("scale", {})[str(t)] = [round(c, 4) for c in v]
                else:
                    v = _continuous(last.get(key), pose.get(key, (0, 0, 0))); last[key] = v
                    bones.setdefault(key, {}).setdefault("rotation", {})[str(t)] = [round(c, 3) for c in v]
        out[name] = {"loop": loop, "animation_length": length, "bones": bones}
    return out

# ---------------------------------------------------------------- export
def geo(rects):
    bones = []
    for name, parent, pivot in BONES:
        b = {"name": name, "pivot": [round(v, 4) for v in pivot]}
        if parent:
            b["parent"] = parent
        cubes = []
        for i, (cname, bone, a, bb, mat) in enumerate(CUBES):
            if bone != name:
                continue
            uv = {}
            for f in FACES:
                u, v, w, h = rects[(i, f)]
                if f in ("py", "ny"):
                    uv[JSON_FACE[f]] = {"uv": [round(u + w, 4), round(v + h, 4)], "uv_size": [round(-w, 4), round(-h, 4)]}
                else:
                    uv[JSON_FACE[f]] = {"uv": [round(u, 4), round(v, 4)], "uv_size": [round(w, 4), round(h, 4)]}
            cubes.append({"origin": [round(c, 4) for c in a], "size": [round(bb[k] - a[k], 4) for k in range(3)], "uv": uv})
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.angel", "texture_width": TEX_W // TEXELS, "texture_height": TEX_H // TEXELS,
                        "visible_bounds_width": 7, "visible_bounds_height": 5, "visible_bounds_offset": [0, 1.2, 0]},
        "bones": bones}]}

def bbmodel(geometry, texture_png, anims):
    """The model as a Blockbench project (Bedrock format, per-face UV), texture and animations built in; Blockbench
    mirrors Bedrock's X."""
    desc = geometry["minecraft:geometry"][0]
    ids = {b["name"]: str(uuid.uuid5(uuid.NAMESPACE_URL, "angel/" + b["name"])) for b in desc["bones"]}
    elements, children = [], {b["name"]: [] for b in desc["bones"]}
    cube_names = {}
    for name, bone, a, b, mat in CUBES:
        cube_names.setdefault(bone, []).append(name)
    for b in desc["bones"]:
        for i, c in enumerate(b.get("cubes", [])):
            o, sz = c["origin"], c["size"]
            faces = {f: {"uv": [fuv["uv"][0], fuv["uv"][1], fuv["uv"][0] + fuv["uv_size"][0], fuv["uv"][1] + fuv["uv_size"][1]], "texture": 0}
                     for f, fuv in c["uv"].items()}
            eid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"angel/{b['name']}/{i}"))
            elements.append({"name": cube_names[b["name"]][i], "box_uv": False, "rescale": False, "locked": False,
                             "from": [-(o[0] + sz[0]), o[1], o[2]], "to": [-o[0], o[1] + sz[1], o[2] + sz[2]],
                             "autouv": 0, "color": 0, "origin": [-b["pivot"][0], b["pivot"][1], b["pivot"][2]],
                             "faces": faces, "type": "cube", "uuid": eid})
            children[b["name"]].append(eid)
    kids = {b["name"]: [] for b in desc["bones"]}
    for b in desc["bones"]:
        if b.get("parent"):
            kids[b["parent"]].append(b["name"])
    def group(name):
        pv = PIVOT[name]
        return {"name": name, "origin": [-pv[0], pv[1], pv[2]], "color": 0, "uuid": ids[name], "export": True,
                "isOpen": True, "locked": False, "visibility": True, "autouv": 0,
                "children": children[name] + [group(k) for k in kids[name]]}
    texture = {"name": "angel.png", "folder": "entity", "namespace": "horrormod", "id": "0",
               "width": TEX_W, "height": TEX_H, "uv_width": TEX_W // TEXELS, "uv_height": TEX_H // TEXELS,
               "particle": False, "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True,
               "saved": True, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "angel.png/" + hashlib.sha1(texture_png).hexdigest())),
               "source": "data:image/png;base64," + base64.b64encode(texture_png).decode()}
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False},
            "name": "angel", "model_identifier": "geometry.angel", "visible_box": [7, 5, 1.2],
            "resolution": {"width": TEX_W // TEXELS, "height": TEX_H // TEXELS},
            "elements": elements, "outliner": [group("root")], "textures": [texture],
            "animations": [{"uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "angel/anim/" + name)), "name": name,
                            "loop": "loop" if a["loop"] is True else "hold", "override": False, "length": a["animation_length"],
                            "snapping": 20, "selected": False,
                            "animators": {ids[bone]: {"name": bone, "type": "bone", "keyframes": [
                                {"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "time": float(t),
                                 "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"angel/{name}/{bone}/{ch}/{t}")),
                                 "color": -1, "interpolation": "linear"}
                                for ch in ("rotation", "position", "scale") for t, v in chans.get(ch, {}).items()]}
                                for bone, chans in a["bones"].items()}}
                           for name, a in anims.items()]}

# ---------------------------------------------------------------- preview
NORMAL = {"nz": (0, 0, -1), "pz": (0, 0, 1), "nx": (-1, 0, 0), "px": (1, 0, 0), "py": (0, 1, 0), "ny": (0, -1, 0)}

def preview(path, pose=None, label="", views=(("front", 0), ("three-quarter", -35), ("side", -90), ("back", 180))):
    W, H, S, CELL = 460, 330, 4.0, 0.22
    sheet = Image.new("RGB", (W * len(views), H), (24, 24, 30))
    for k, (title, yaw) in enumerate(views):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        def view(p):
            x, y, z = p
            return (x * cy - z * sy, y, x * sy + z * cy)
        polys = []
        for i, (name, bone, a, b, mat) in enumerate(CUBES):
            M, tr = transform(bone, pose) if pose is not None else ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0))
            if abs(M[0][0]) + abs(M[1][1]) + abs(M[2][2]) + abs(M[0][1]) + abs(M[1][0]) < 1e-3:
                continue                                           # (scaled away)
            place = lambda q: _addv(_mvec(M, q), tr)
            for f in FACES:
                n = view(_mvec(M, NORMAL[f]))
                if n[2] > 0.01:
                    continue
                fw, fh = face_size(a, b, f)
                nu, nv = max(1, round(fw / CELL)), max(1, round(fh / CELL))
                for iu in range(nu):
                    for iv in range(nv):
                        s0, s1, t0, t1 = iu / nu, (iu + 1) / nu, iv / nv, (iv + 1) / nv
                        quad = [view(place(face_point(a, b, f, ss, tt))) for ss, tt in ((s0, t0), (s1, t0), (s1, t1), (s0, t1))]
                        sc, tc = (s0 + s1) / 2, (t0 + t1) / 2
                        c, al = texel(mat, f, sc, tc, face_point(a, b, f, sc, tc), i, fw, fh)
                        if al >= 0.5:
                            polys.append((sum(p[2] for p in quad) / 4, quad, c))
        img = Image.new("RGB", (W, H), (24, 24, 30)); d = ImageDraw.Draw(img)
        for depth, quad, c in sorted(polys, key=lambda p: -p[0]):
            d.polygon([(W / 2 + p[0] * S, H / 2 - (p[1] - C[1]) * S) for p in quad], fill=c)
        d.text((6, 6), title + (" " + label if label else ""), fill=(170, 170, 190))
        sheet.paste(img, (k * W, 0))
    sheet.save(path)

if __name__ == "__main__":
    rects = layout()
    tex = paint(rects)
    tex_path = os.path.join(ASSETS, "textures", "entity", "angel.png")
    tex.save(tex_path)
    geometry = geo(rects)
    json.dump(geometry, open(os.path.join(ASSETS, "geo", "entity", "angel.geo.json"), "w"), indent=1)
    anims = animations()
    json.dump({"format_version": "1.8.0", "animations": anims},
              open(os.path.join(ASSETS, "animations", "entity", "angel.animation.json"), "w"), separators=(",", ":"))
    json.dump(bbmodel(geometry, open(tex_path, "rb").read(), anims), open(BBMODEL, "w"), indent=1)
    if len(sys.argv) > 1:
        preview(sys.argv[1], pose=hover(0.0), label="hover")
    span = max(abs(c[2][0]) for c in CUBES + [("", "", (0, 0, 0), (0, 0, 0), "")]) * 2
    print(f"wrote angel geo, texture, animations and {BBMODEL}; {len(CUBES)} cubes")
