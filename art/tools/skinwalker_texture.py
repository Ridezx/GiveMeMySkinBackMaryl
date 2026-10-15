"""Paints the Skinwalker's texture straight from the model's cubes (art/tools/skinwalker_anim.py).
Writes src/main/resources/assets/horrormod/textures/entity/skinwalker.png (the skin) and skinwalker_eyes.png
(only the eyes, for a glow-in-the-dark layer in game). Only run this to start the texture over: it
overwrites any painting done on those files since.

The look follows the trail-camera photo and the Rake: sickly pale skin stretched over a gaunt frame, cool
grey-purple mottling, faint veins, ribs and spine pressing through, raw joints, filthy hands and feet,
yellowed nails; the head is stained bone (a skull for a face) with dark sockets around glowing eyes, and
the jaw still has skin on it.

Every texture pixel is painted from the 3D point it covers on the model, so patterns run across
neighbouring cubes. Run: python3 art/tools/skinwalker_texture.py
"""
import importlib.util, math, os, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "horrormod", "textures", "entity")
TEXELS = 2                      # texture pixels per model pixel (the UV layout is 128x128, the image 256x256)

_argv = sys.argv; sys.argv = ["x", "/tmp", "/tmp"]
_spec = importlib.util.spec_from_file_location("skinwalker_anim", os.path.join(HERE, "skinwalker_anim.py"))
g = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(g); sys.argv = _argv

# ---------------------------------------------------------------- noise
def _hash(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144269504) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0

def noise(x, y, z, seed=0):
    """Smooth value noise, 0..1."""
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    sx, sy, sz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    def lerp(a, b, t): return a + (b - a) * t
    c = [[[_hash(ix + i, iy + j, iz + k, seed) for k in (0, 1)] for j in (0, 1)] for i in (0, 1)]
    return lerp(lerp(lerp(c[0][0][0], c[0][0][1], sz), lerp(c[0][1][0], c[0][1][1], sz), sy),
                lerp(lerp(c[1][0][0], c[1][0][1], sz), lerp(c[1][1][0], c[1][1][1], sz), sy), sx)

def fbm(x, y, z, seed=0, octaves=3):
    total, amp, freq, norm = 0.0, 1.0, 1.0, 0.0
    for o in range(octaves):
        total += amp * noise(x * freq, y * freq, z * freq, seed + o * 17); norm += amp
        amp *= 0.5; freq *= 2.0
    return total / norm

def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))

def smoothstep(e0, e1, x):
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)

# ---------------------------------------------------------------- palette
SKIN = (201, 192, 179)          # pale, with just a little warmth
SKIN_SHADOW = (162, 152, 156)   # cool grey-purple mottling
SKIN_SICK = (190, 186, 160)     # a faint jaundiced tint here and there
SKIN_YELLOW = (220, 210, 178)   # where bone presses through
VEIN = (158, 160, 178)
RAW = (204, 168, 166)           # raw, chafed joints
DIRT = (104, 92, 78)
NAIL = (186, 174, 140)
BONE = (224, 214, 186)          # the skull: stained ivory
GRIME = (140, 124, 100)
SUTURE = (96, 82, 66)
SOCKET = (70, 60, 58)
EYE_CORE = (255, 253, 232)
EYE_EDGE = (214, 232, 176)      # pale yellow-green glow at the edge, like the trail camera
TEETH = (212, 198, 156)
TEETH_GAP = (88, 70, 58)
JAW_SKIN = (208, 196, 188)
LIP = (150, 108, 106)
HOLE = (20, 12, 12)
MOUTH = (34, 12, 14)

BONY = ("vertebra", "shoulder_blade", "hip_bone", "kneecap", "inner_ankle", "outer_ankle", "elbow", "shin_ridge",
        "sternum", "trapezius")
RAW_JOINTS = ("kneecap", "elbow", "heel")

def region(name, bone, tone):
    if name.endswith("_eye"):
        return "eye"
    if "teeth" in name or "molars" in name:
        return "teeth"
    if name in ("mouth",):
        return "mouth"
    if tone <= 40 or name in ("navel",):
        return "hole"
    if bone == "jaw":
        return "jaw"
    if bone == "head":
        return "bone"
    if any(k in bone for k in ("_hand", "_finger", "_thumb")):
        return "hand"
    if bone.endswith("_foot") or "_toe" in bone:
        return "foot"
    return "skin"

# ---------------------------------------------------------------- geometry: which 3D point a texel covers
def face_point(c, face, s, t):
    """Point on cube c's face at (s, t) in 0..1 across/down the face's texture rectangle, and the face normal.
    Side faces as seen from outside: across goes to the viewer's right, down goes down."""
    (x0, y0, z0), (x1, y1, z1) = c[2], c[3]
    if face == "north": return (x1 - s * (x1 - x0), y1 - t * (y1 - y0), z0), (0, 0, -1)
    if face == "south": return (x0 + s * (x1 - x0), y1 - t * (y1 - y0), z1), (0, 0, 1)
    if face == "east":  return (x1, y1 - t * (y1 - y0), z1 - s * (z1 - z0)), (1, 0, 0)
    if face == "west":  return (x0, y1 - t * (y1 - y0), z0 + s * (z1 - z0)), (-1, 0, 0)
    if face == "up":    return (x0 + s * (x1 - x0), y1, z0 + t * (z1 - z0)), (0, 1, 0)
    return (x0 + s * (x1 - x0), y0, z0 + t * (z1 - z0)), (0, -1, 0)

def face_rects(c, uv):
    """Box-UV rectangles (u, v, width, height) in UV units for each face, from the cube's real size."""
    (x0, y0, z0), (x1, y1, z1) = c[2], c[3]
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    u, v = uv
    return {"east": (u, v + d, d, h), "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "up": (u + d, v, w, d), "down": (u + d + w, v, w, d)}

BOXES = [(c, tuple(min(c[2][i], c[3][i]) for i in range(3)), tuple(max(c[2][i], c[3][i]) for i in range(3))) for c in g.CUBES]

def occlusion(c, p, n, near):
    """Contact shadow: how much other cubes crowd the surface just outside this point."""
    occ = 0.0
    for dist, weight in ((0.35, 0.55), (0.9, 0.3)):
        q = (p[0] + n[0] * dist, p[1] + n[1] * dist, p[2] + n[2] * dist)
        for other, lo, hi in near:
            pad = 0.15
            if all(lo[i] - pad <= q[i] <= hi[i] + pad for i in range(3)):
                occ += weight
                break
    return min(1.0, occ)

# ---------------------------------------------------------------- painting
def skin_colour(p, n, name, bone):
    x, y, z = p
    col = SKIN
    m = fbm(x * 0.18, y * 0.18, z * 0.18, 1)                         # broad mottling
    col = mix(col, SKIN_SHADOW, smoothstep(0.42, 0.75, m) * 0.7)
    col = mix(col, SKIN_SICK, smoothstep(0.55, 0.8, fbm(x * 0.11 + 5, y * 0.11, z * 0.11, 2)) * 0.45)
    if any(k in name for k in BONY):                                   # bone pressing through
        col = mix(col, SKIN_YELLOW, 0.45)
    if any(k in name for k in RAW_JOINTS):
        col = mix(col, RAW, 0.35 + 0.3 * fbm(x * 0.6, y * 0.6, z * 0.6, 4))
    # ribs showing through the sides and front of the chest
    if bone == "chest" and n[1] == 0 and 56 <= y <= 66.5 and any(k in name for k in ("ribcage", "chest_side")):
        r = 0.5 + 0.5 * math.cos((y - 56) * 2 * math.pi / 1.7)
        strength = 0.6 if n[0] != 0 else 0.22                         # mostly along the sides
        col = mix(col, (150, 140, 146), (1 - r) * strength)
    # faint veins on the limbs, neck and temples
    if any(k in bone for k in ("arm", "forearm", "leg", "shin", "neck")) and n[1] == 0:
        v = abs(fbm(x * 0.35 + 3.1, y * 0.12, z * 0.35, 7) - 0.5)
        col = mix(col, VEIN, smoothstep(0.025, 0.0, v) * 0.45)
    # grubby where it touches the ground: hands and feet (and a little up the shins)
    return col

def hand_colour(p, n, name, bone):
    x, y, z = p
    col = skin_colour(p, n, name, bone)
    dirt = 0.15 + 0.25 * fbm(x * 0.9, y * 0.9, z * 0.9, 9)
    if n[2] > 0:                                                       # palm side
        dirt += 0.35
    if bone.endswith("_tip"):
        dirt += 0.3
    if "finger" in bone or "thumb" in bone:                            # knuckle creases
        dirt += 0.1
    col = mix(col, DIRT, dirt)
    if bone.endswith("_tip") and n[2] < 0:                             # nail on the back of each fingertip
        y0, y1 = min(next(c for c in g.CUBES if c[0] == name)[2][1], next(c for c in g.CUBES if c[0] == name)[3][1]), \
                 max(next(c for c in g.CUBES if c[0] == name)[2][1], next(c for c in g.CUBES if c[0] == name)[3][1])
        if y < y0 + (y1 - y0) * 0.5:
            col = mix(NAIL, (120, 104, 84), smoothstep(y0 + (y1 - y0) * 0.15, y0, y))
    return col

def foot_colour(p, n, name, bone):
    x, y, z = p
    col = skin_colour(p, n, name, bone)
    dirt = 0.12 + 0.3 * fbm(x * 0.9, y * 0.9, z * 0.9, 11) + smoothstep(-6, -14, y) * 0.45
    if n[2] > 0 or n[1] < 0:                                           # sole and underside
        dirt += 0.3
    col = mix(col, DIRT, dirt)
    if bone.endswith("_tip") and n[1] > 0:                             # toenails on top of the toe tips
        col = mix(NAIL, (120, 104, 84), 0.25)
    return col

def bone_colour(p, n, name):
    x, y, z = p
    col = BONE
    col = mix(col, GRIME, smoothstep(0.5, 0.8, fbm(x * 0.35, y * 0.35, z * 0.35, 21)) * 0.6)
    col = mix(col, (236, 230, 210), smoothstep(0.55, 0.85, fbm(x * 0.8, y * 0.8, z * 0.8, 22)) * 0.25)
    # skull sutures across the top and back of the head
    if name in ("crown", "cranium_top", "cranium", "cranium_back") and (n[1] > 0 or n[2] > 0 or n[0] != 0):
        s = abs(fbm(x * 0.5 + 9, y * 0.5, z * 0.5, 23) - 0.5)
        col = mix(col, SUTURE, smoothstep(0.022, 0.0, s) * 0.85)
    # darkness pooling around the eye sockets
    if "orbit" in name or name in ("nose_bridge", "brow_ridge") or "cheekbone" in name:
        eye_y = 77.7
        d = abs(y - eye_y)
        col = mix(col, SOCKET, smoothstep(3.0, 0.5, d) * 0.55)
    if n[1] < 0:                                                       # undersides of brow and cheekbones
        col = mix(col, SOCKET, 0.35)
    return col

def jaw_colour(p, n, name, t):
    x, y, z = p
    col = mix(JAW_SKIN, SKIN_SHADOW, fbm(x * 0.4, y * 0.4, z * 0.4, 31) * 0.5)
    if name in ("chin", "jaw_body", "right_jaw_body", "left_jaw_body") or "jaw_body" in name or name == "chin":
        col = mix(col, LIP, smoothstep(71.5, 72.0, y) * 0.8)          # lips along the top edge
    return col

def eye_colour(s, t):
    d = math.hypot(s - 0.5, t - 0.5) / 0.7
    return mix(EYE_CORE, EYE_EDGE, smoothstep(0.35, 1.0, d))

def teeth_colour(p, n, s, t, c):
    col = mix(TEETH, (180, 162, 120), fbm(p[0] * 2, p[1] * 2, p[2] * 2, 41) * 0.5)
    w = abs(c[3][0] - c[2][0]) if n[0] == 0 else abs(c[3][2] - c[2][2])
    k = (s * w * TEXELS) % 2.0                                         # a dark gap every other texel column
    if n[1] == 0 and k < 0.6:
        col = mix(col, TEETH_GAP, 0.85)
    if n[1] == 0 and (t < 0.15 or t > 0.85):
        col = mix(col, (118, 84, 78), 0.6)                             # gums at the roots
    return col

SHADE = {"up": 1.03, "down": 0.80, "north": 0.98, "south": 0.95, "east": 0.96, "west": 0.96}

def paint():
    W = H = 128 * TEXELS
    skin = Image.new("RGBA", (W, H), (0, 0, 0, 0)); sp = skin.load()
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0)); gp = glow.load()
    for i, c in enumerate(g.CUBES):
        name, bone, a, b, tone, inf = c
        reg = region(name, bone, tone)
        u0, v0 = g.UV[i]
        lo = tuple(min(a[k], b[k]) for k in range(3)); hi = tuple(max(a[k], b[k]) for k in range(3))
        near = [(o, olo, ohi) for o, olo, ohi in BOXES if o is not c and o[1] in (bone, g.PARENT.get(bone)) or
                (o is not c and all(olo[k] - 1.2 <= hi[k] and lo[k] <= ohi[k] + 1.2 for k in range(3)))]
        # fill the cube's whole allocated area first, so rounding of face edges never shows a gap
        sw, sh, sd = g.size(c)
        for py in range(int(v0 * TEXELS), int((v0 + sd + sh) * TEXELS)):
            for px in range(int(u0 * TEXELS), int((u0 + 2 * (sw + sd)) * TEXELS)):
                if 0 <= px < W and 0 <= py < H:
                    sp[px, py] = (170, 166, 160, 255)
        for face, (fu, fv, fw, fh) in face_rects(c, (u0, v0)).items():
            x0t, x1t = int(math.floor(fu * TEXELS)), int(math.ceil((fu + fw) * TEXELS))
            y0t, y1t = int(math.floor(fv * TEXELS)), int(math.ceil((fv + fh) * TEXELS))
            for py in range(y0t, max(y1t, y0t + 1)):
                for px in range(x0t, max(x1t, x0t + 1)):
                    s = ((px + 0.5) / TEXELS - fu) / fw if fw else 0.5
                    t = ((py + 0.5) / TEXELS - fv) / fh if fh else 0.5
                    s, t = max(0.0, min(1.0, s)), max(0.0, min(1.0, t))
                    p, n = face_point((name, bone, lo, hi), face, s, t)
                    if reg == "eye":
                        col = eye_colour(s, t)
                        gp[px, py] = tuple(int(v) for v in col) + (255,)
                        sp[px, py] = tuple(int(v) for v in col) + (255,)
                        continue
                    if reg == "hole": col = HOLE
                    elif reg == "mouth": col = MOUTH
                    elif reg == "teeth": col = teeth_colour(p, n, s, t, (name, bone, lo, hi))
                    elif reg == "bone": col = bone_colour(p, n, name)
                    elif reg == "jaw": col = jaw_colour(p, n, name, t)
                    elif reg == "hand": col = hand_colour(p, n, name, bone)
                    elif reg == "foot": col = foot_colour(p, n, name, bone)
                    else: col = skin_colour(p, n, name, bone)
                    shade = SHADE[face] * (1 - 0.45 * occlusion(c, p, n, near))
                    grain = (fbm(p[0] * 3.1, p[1] * 3.1, p[2] * 3.1, 51, 1) - 0.5) * 10
                    col = tuple(max(0, min(255, int(v * shade + grain))) for v in col)
                    if 0 <= px < W and 0 <= py < H:
                        sp[px, py] = col + (255,)
    return skin, glow

if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    skin, glow = paint()
    skin.save(os.path.join(OUT_DIR, "skinwalker.png"))
    glow.save(os.path.join(OUT_DIR, "skinwalker_eyes.png"))
    print("wrote", os.path.join(OUT_DIR, "skinwalker.png"), "and skinwalker_eyes.png", skin.size)
