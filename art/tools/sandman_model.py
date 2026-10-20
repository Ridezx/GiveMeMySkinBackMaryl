"""The Sandman: a ghost of a soldier, built after Haze from Deadlock (the user's turnaround sheet) but made of cold
fire. A little shorter than a player (the flames on her head reach about 1.8 blocks). Her head is a flame in the
shape of one continuous flame, not a human head (after Ember in Pixar's Elemental): a round bulb rising into a single
flame that leans back, a pale hot blue at its base turning deep blue and then black (where a flame turns orange and
red), with two big glowing eyes that blink and narrow when she goes for someone. The rest of her is Haze's kit, dark
(and solid): rounded black sleeves and long gloves, a plate-carrier bulletproof vest (where Haze has her
backpack) with its pouches, a utility belt, deep blue trousers with a slight flare at the thigh, knee pads and
laced, heeled combat boots, every edge touched with a faint blue glow.

Writes:
    src/main/resources/assets/horrormod/geo/entity/sandman.geo.json      GeckoLib geometry
    src/main/resources/assets/horrormod/textures/entity/sandman.png       the texture (all solid)
    src/main/resources/assets/horrormod/textures/entity/sandman_glow.png  what glows in the dark (eyes, fire, a faint rim),
                                                                           added on top as light by SandmanRenderer
                                                                           (not a GeckoLib glowmask: that layer erases the
                                                                           texture under every pixel it has)
    art/sandman.bbmodel                                                    a Blockbench project to look at (edit this
                                                                           script, not that file)
    [preview.png]                                                          front / three-quarter / side / back
    [head_preview.png]                                                     the same, close up on the head and hair
    src/main/resources/assets/horrormod/animations/entity/sandman.animation.json   her animations (also in the .bbmodel)
Run: python3 art/tools/sandman_model.py [preview.png [head_preview.png]]  (needs Pillow)
"""
import base64, hashlib, io, json, math, os, random, sys, uuid
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod"))
BBMODEL = os.path.normpath(os.path.join(HERE, "..", "sandman.bbmodel"))

TEXELS = 4                      # texture pixels per model pixel
TEX_W, TEX_H = 512, 768        # texture size in pixels (128 x 192 UV units)
FACES = ("north", "south", "east", "west", "up", "down")

# ---------------------------------------------------------------- rig: bones (name, parent, pivot)
BONES = [("root", None, (0, 0, 0)),
         ("body", "root", (0, 13.6, 0)),
         ("head", "body", (0, 21.0, 0)),
         ("flame", "head", (0, 25.0, 0.2)),
         ("right_arm", "body", (-2.9, 20.6, 0)), ("left_arm", "body", (2.9, 20.6, 0)),
         ("right_forearm", "right_arm", (-3.8, 17.3, 0)), ("left_forearm", "left_arm", (3.8, 17.3, 0)),
         ("right_eye", "head", (-0.95, 23.95, -2.2)), ("left_eye", "head", (0.95, 23.95, -2.2)),   # (they blink and squint)
         ("gun", "right_forearm", (-3.8, 12.2, -0.2)),            # her rifle, its grip in her right fist
         ("right_leg", "root", (-1.35, 13.2, 0)), ("left_leg", "root", (1.35, 13.2, 0)),
         ("right_shin", "right_leg", (-1.35, 7.6, 0)), ("left_shin", "left_leg", (1.35, 7.6, 0))]
PIVOT = {n: p for n, _, p in BONES}

# ---------------------------------------------------------------- the cubes (name, bone, from, to, material)
# She faces -z (north); her right is -x. Sizes in model pixels (16 a block); feet at y 0.
CUBES = []
def cube(name, bone, a, b, mat):
    CUBES.append((name, bone, tuple(a), tuple(b), mat))

def pair(name, bone, a, b, mat):
    """A cube on her right (-x) and its mirror on her left (on that side's bone: "leg" means right_leg / left_leg;
    a bone without sides, like body or head, is used as it is)."""
    sided = lambda side: bone if bone in PIVOT else side + "_" + bone
    cube("right_" + name, sided("right"), a, b, mat)
    cube("left_" + name, sided("left"), (-b[0], a[1], a[2]), (-a[0], b[1], b[2]), mat)

# boots: thick sole and heel, toe cap, the shaft up to the knee with its cuff, laces and a buckled strap
pair("sole", "shin", (-2.55, 0, -2.5), (-0.15, 0.7, 1.5), "sole")
pair("heel", "shin", (-2.45, 0.7, 0.4), (-0.25, 1.7, 1.45), "sole")
pair("toe", "shin", (-2.45, 0.7, -2.45), (-0.25, 2.1, -0.9), "boot")
pair("boot", "shin", (-2.35, 0.7, -1.15), (-0.35, 6.8, 1.15), "boot")
pair("boot_cuff", "shin", (-2.5, 6.4, -1.3), (-0.2, 7.7, 1.3), "boot")
pair("laces", "shin", (-1.85, 2.0, -1.25), (-0.85, 6.3, -1.15), "laces")
pair("boot_strap", "shin", (-2.42, 4.4, -1.22), (-0.28, 4.9, 1.22), "strap")
pair("strap_buckle", "shin", (-2.55, 4.3, -0.4), (-2.4, 5.0, 0.4), "metal")
# knees and the trousers: narrow at the knee, a slight jodhpur flare over the thigh, gathering at the hip
pair("knee_pad", "leg", (-2.2, 7.1, -1.55), (-0.5, 9.0, -1.0), "pad")
pair("shin_cloth", "leg", (-2.25, 7.4, -1.15), (-0.45, 9.8, 1.15), "trousers")
pair("thigh_flare", "leg", (-2.6, 9.6, -1.25), (-0.25, 12.4, 1.3), "trousers")
pair("thigh_outer", "leg", (-2.8, 10.4, -1.0), (-2.5, 11.8, 1.0), "trousers")     # a slight flare, close to the leg
pair("thigh_top", "leg", (-2.5, 12.2, -1.25), (-0.1, 13.6, 1.3), "trousers")
pair("holster_strap", "leg", (-2.85, 10.9, -1.35), (-0.2, 11.3, 1.4), "strap")
cube("pelvis", "body", (-2.6, 12.9, -1.35), (2.6, 14.3, 1.4), "trousers")
# belt with buckle and pouches
cube("belt", "body", (-2.75, 14.0, -1.5), (2.75, 14.8, 1.55), "belt")
cube("buckle", "body", (-0.55, 14.0, -1.62), (0.55, 14.8, -1.5), "metal")
pair("belt_pouch", "body", (-2.95, 13.3, -0.9), (-2.6, 14.9, 0.7), "pouch")
cube("belt_pouch_back", "body", (-1.0, 13.4, 1.5), (1.0, 14.9, 1.95), "pouch")
# torso in a black shirt
cube("waist", "body", (-2.2, 14.8, -1.25), (2.2, 16.3, 1.25), "shirt")
cube("chest", "body", (-2.55, 16.2, -1.4), (2.55, 20.6, 1.4), "shirt")
cube("shoulders", "body", (-2.6, 19.6, -1.25), (2.6, 20.6, 1.25), "shirt")
# the plate carrier: front and back plates, wrap-around cummerbund, shoulder straps, pouches, a radio, a collar
cube("vest_front", "body", (-2.35, 16.0, -1.9), (2.35, 20.3, -1.4), "vest")
cube("vest_back", "body", (-2.35, 16.0, 1.4), (2.35, 20.6, 1.95), "vest")
cube("cummerbund", "body", (-2.8, 15.6, -1.75), (2.8, 17.4, 1.8), "vest")
pair("vest_shoulder", "body", (-2.4, 20.3, -1.9), (-1.3, 21.0, 1.95), "strap")
cube("mag_pouch_1", "body", (-2.2, 16.3, -2.5), (-0.85, 18.2, -1.9), "pouch")
cube("mag_pouch_2", "body", (-0.65, 16.3, -2.5), (0.65, 18.2, -1.9), "pouch")
cube("mag_pouch_3", "body", (0.85, 16.3, -2.5), (2.2, 18.2, -1.9), "pouch")
cube("radio", "body", (1.2, 18.6, -2.35), (2.1, 20.0, -1.9), "pouch")
cube("radio_aerial", "body", (1.75, 20.0, -2.2), (1.95, 21.4, -2.0), "metal")
cube("chest_panel", "body", (-1.9, 18.5, -2.05), (0.6, 19.9, -1.9), "patch")
cube("collar", "body", (-1.4, 20.6, -1.35), (1.4, 21.6, 1.35), "vest")
# arms: rounded sleeves (stepping in at the shoulder and the outside), long gloves with a flared cuff, hands
pair("sleeve", "arm", (-4.6, 17.6, -1.1), (-2.65, 20.3, 1.1), "shirt")
pair("sleeve_cap", "arm", (-4.3, 20.3, -0.95), (-2.65, 20.85, 0.95), "shirt")    # the round of the shoulder
pair("sleeve_puff", "arm", (-4.8, 18.3, -0.85), (-4.55, 19.9, 0.85), "shirt")
pair("sleeve_band", "arm", (-4.5, 17.1, -0.95), (-2.8, 17.7, 0.95), "strap")
pair("glove_cuff", "forearm", (-4.85, 15.9, -1.05), (-2.75, 17.2, 1.05), "glove")
pair("glove", "forearm", (-4.6, 12.9, -0.85), (-3.0, 16.0, 0.85), "glove")
pair("hand", "forearm", (-4.55, 11.4, -0.8), (-3.05, 13.0, 0.75), "glove")
pair("knuckles", "forearm", (-4.6, 11.6, -0.9), (-3.0, 12.5, -0.7), "glove")
# her Toy Gun, modelled into her so it's always held right (and shows in Blockbench): toy_gun_model.py's parts, scaled
# to her, built pointing forward (-z) with its grip in her right fist (the gun bone's pivot); the animations turn the
# gun bone to point it, and the arms reach for it (see the animations below)
GUN_SCALE = 0.55
GUN_GRIP = (-3.8, 12.2, -0.2)
def _gun_parts():
    import importlib.util
    spec = importlib.util.spec_from_file_location("toy_gun_model", os.path.join(HERE, "toy_gun_model.py"))
    tg = importlib.util.module_from_spec(spec); spec.loader.exec_module(tg)
    for part in tg.PARTS:
        name, a, b, swatch = part[:4]
        lo = tuple(GUN_GRIP[i] + GUN_SCALE * (a[i] - tg.GRIP[i]) for i in range(3))
        hi = tuple(GUN_GRIP[i] + GUN_SCALE * (b[i] - tg.GRIP[i]) for i in range(3))
        cube("gun_" + name.replace(" ", "_"), "gun", lo, hi, "gun_" + swatch)
    return tg
GUN = _gun_parts()
GUN_FOREND = GUN_SCALE * (GUN.GRIP[2] - 6.0)          # how far in front of the grip her left hand holds the forend (px)
GUN_STOCK = GUN_SCALE * (17.5 - GUN.GRIP[2])           # how far behind the grip the butt is
# the head: one continuous flame, not a human head (after Ember in Pixar's Elemental), shaped from the side like the
# user's brushed outline: a round bulb low at the front where the face would be, bulging forward most at the eyes, the
# whole flame swept up and far back to a point behind the head as if blown by the wind, its underside running
# straight up from the back of the neck to the tip; two flame horns rising off the top above the brow, swept back
# with it. Built from thin slices stacked up, each rounded by a cross of two cubes; two big glowing eyes are its only
# features. The profile: height -> (front edge z, back edge z, half width), straight lines between.
HEAD_PROFILE = [(21.0, -0.7, 0.8, 0.75), (21.8, -1.4, 1.4, 1.3), (22.8, -2.0, 2.0, 1.9), (23.9, -2.25, 2.6, 2.1),
                (24.9, -2.1, 3.2, 1.95), (25.7, -1.6, 3.8, 1.6), (26.4, -0.6, 4.3, 1.2), (27.0, 1.2, 4.7, 0.8),
                (27.5, 3.2, 4.9, 0.45), (27.8, 4.6, 5.0, 0.2)]
SLICE = 0.3

def head_shape(y):
    """(front z, back z, half width) of the head's flame at height y."""
    for (y0, *a), (y1, *b) in zip(HEAD_PROFILE, HEAD_PROFILE[1:]):
        if y0 <= y <= y1:
            u = (y - y0) / (y1 - y0)
            return tuple(a[k] + (b[k] - a[k]) * u for k in range(3))
    return tuple(HEAD_PROFILE[-1][1:])

y, k = HEAD_PROFILE[0][0], 0
while y < HEAD_PROFILE[-1][0] - 1e-6:
    y1 = min(HEAD_PROFILE[-1][0], y + SLICE)
    front, back, hw = head_shape((y + y1) / 2)
    zc, hd = (front + back) / 2, (back - front) / 2
    bone = "head" if y < 25.0 else "flame"
    cube(f"flame_{k}", bone, (-hw, y, zc - hd * 0.9), (hw, y1, zc + hd * 0.9), "fire")
    cube(f"flame_{k}_round", bone, (-hw * 0.72, y, front), (hw * 0.72, y1, back), "fire")
    y = y1; k += 1
# the horns: rising off the top above the brow, where horns grow, curling up and back with the flame
for side, sx in (("right", -1), ("left", 1)):
    for n, (y0, y1, x, w, z) in enumerate([(25.4, 26.2, 1.0, 0.42, -0.9), (26.2, 26.9, 1.08, 0.34, -0.5),
                                           (26.9, 27.5, 1.12, 0.26, 0.0), (27.5, 27.95, 1.1, 0.15, 0.55)]):
        cube(f"{side}_horn_{n}", "flame", (sx * x - w, y0, z - w * 0.8), (sx * x + w, y1, z + w * 0.8), "fire")
# the eyes: big and round, glowing, set into the front of the bulb
for side, sx in (("right", -1), ("left", 1)):
    zf = head_shape(23.9)[0] - 0.04
    cube(f"{side}_eye", f"{side}_eye", (sx * 0.95 - 0.5, 23.35, zf), (sx * 0.95 + 0.5, 24.55, zf + 0.12), "eye")
    cube(f"{side}_eye_round", f"{side}_eye", (sx * 0.95 - 0.36, 23.15, zf + 0.02), (sx * 0.95 + 0.36, 24.75, zf + 0.12), "eye")

# ---------------------------------------------------------------- painting
def _hash(x, y, k):
    h = (x * 374761393 + y * 668265263 + k * 2147483647) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535.0

def _noise(x, y, k):
    """Smooth value noise."""
    ix, iy = math.floor(x), math.floor(y); fx, fy = x - ix, y - iy
    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _hash(ix, iy, k), _hash(ix + 1, iy, k); c, d = _hash(ix, iy + 1, k), _hash(ix + 1, iy + 1, k)
    return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy

def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(len(c1)))

GHOST_RIM = (70, 130, 255)
FIRE_LOW, FIRE_MID, FIRE_HIGH = (90, 175, 255), (26, 70, 190), (6, 8, 16)
BASE = {"gun_red": (190, 36, 32), "gun_red_light": (220, 76, 64), "gun_black": (28, 28, 32), "gun_black_light": (60, 62, 70),
        "gun_brown": (104, 54, 40), "gun_brown_dark": (66, 34, 24), "gun_brown_checker": (92, 48, 36), "gun_smoke": (88, 82, 76),
        "gun_barrel": (28, 28, 32),
        "shirt": (22, 22, 28), "vest": (30, 32, 36), "pouch": (36, 36, 42), "strap": (13, 13, 17), "belt": (18, 18, 22),
        "metal": (96, 102, 118), "trousers": (24, 40, 98), "pad": (20, 24, 36), "boot": (17, 17, 21), "sole": (42, 42, 48),
        "laces": (30, 32, 40), "glove": (11, 11, 15), "patch": (24, 40, 98)}

def fire(y, s, t, x):
    """The flame of her head at height y (model px), coloured the way a flame is: hottest, a pale blue, at its base,
    a deep blue above that, and where a flame would turn orange and red, black; streaks flicker up it, and it gets
    thinner (more see-through) toward its tip."""
    stops = [(21.0, (150, 215, 255)), (22.2, (70, 150, 255)), (23.3, (28, 70, 200)), (24.3, (12, 24, 84)),
             (25.3, (5, 6, 14)), (28.8, (2, 2, 4))]                    # (black from about the eyes up)
    c = stops[-1][1]
    for (y0, c0), (y1, c1) in zip(stops, stops[1:]):
        if y0 <= y <= y1:
            c = mix(c0, c1, (y - y0) / (y1 - y0)); break
    if y < stops[0][0]:
        c = stops[0][1]
    streak = _noise(x * 1.3, y * 0.45 - x * 0.2, 7)
    hot = max(0.0, streak - 0.5) * 2
    c = mix(c, (190, 235, 255) if y < 25.0 else (40, 90, 220), 0.4 * hot * max(0.0, 1.0 - (y - 21) / 7))
    alpha = 1.0                                                      # solid
    # dark veins winding up through the flame like nerves: thin ridges of a warped noise, soft-edged (so they seem to
    # lie inside the fire, not on it), branching as they go; one pattern across all the slices, so each runs unbroken
    wx, wy = x + 0.6 * _noise(y * 0.9, x * 0.9, 41), y + 0.6 * _noise(x * 0.9 + 5, y * 0.9, 43)
    vein = 0.0
    for scale, k, weight in ((0.55, 51, 1.0), (1.1, 53, 0.6)):
        n = _noise(wx * scale, wy * scale * 0.6, k)
        ridge = 1.0 - abs(2.0 * n - 1.0)                              # 1 along the noise's midlines
        vein = max(vein, weight * max(0.0, (ridge - 0.86) / 0.12))
    c = mix(c, (2, 4, 14), min(1.0, vein) * 0.85)
    return c, alpha

def texel(mat, f, s, t, wy, wx, i, fw, fh):
    """Colour and alpha (0..1) of one texel of a face (s, t across it 0..1, t from its top; wy, wx where it is)."""
    alpha = 1.0                                                      # solid (see-through overlapping parts showed each other)
    shade = {"up": 1.15, "down": 0.6, "north": 1.0, "south": 0.86, "east": 0.92, "west": 0.92}[f]
    if mat == "fire":
        a, b = CUBES[i][2], CUBES[i][3]
        across = a[2] + s * (b[2] - a[2]) + 9.0 if f in ("east", "west") else wx   # (side faces run along z)
        c, alpha = fire(wy, s, t, across)
        shade = 1.0 + (shade - 1.0) * 0.4                            # fire is its own light
    elif mat == "eye":
        c, alpha, shade = (215, 245, 255), 1.0, 1.0
    elif mat in ("flame", "flame_tip"):
        u = 1.0 - t if f in FACES[:4] else (1.0 if f == "up" else 0.0)       # up the flame
        if mat == "flame_tip":
            u = 0.7 + 0.3 * u
        c = mix(mix(FIRE_MID, (10, 14, 34), u * 1.5), FIRE_HIGH, max(0.0, u - 0.5) * 2)
        alpha = 0.85 * (1.0 - 0.75 * u ** 1.5)
        streak = _noise(s * 3 + i, t * 1.2 + i * 0.7, 11)
        c = mix(c, (110, 180, 255), 0.4 * max(0.0, streak - 0.55) * 2 * (1 - min(1.0, (wy - 24) / 5)))
        alpha *= 0.7 + 0.3 * streak
        shade = 1.0
    else:
        c = BASE[mat]
        grain = _hash(int(s * fw * TEXELS), int(t * fh * TEXELS), i)
        c = tuple(v * (0.9 + 0.2 * grain) for v in c)
        if mat in ("shirt", "trousers"):                             # soft folds
            fold = _noise(s * fw * 0.8, t * fh * 1.6, i)
            c = tuple(v * (0.85 + 0.3 * fold) for v in c)
            if mat == "trousers" and f in ("east", "west") and abs(s - 0.5) < 0.04:
                c = tuple(v * 0.7 for v in c)                         # the side seam
        if mat == "vest" and f in ("north", "south") and int(t * fh * 2) % 2 == 0:
            c = tuple(v * 0.78 for v in c)                            # webbing rows
        if mat == "pouch" and t < 0.3:
            c = tuple(v * 1.25 for v in c)                            # the flap
        if mat == "pouch" and abs(t - 0.3) < 0.05:
            c = tuple(v * 0.6 for v in c)
        if mat in ("boot", "glove") and f in ("north", "east", "west") and 0.25 < s < 0.45:
            c = tuple(v * 1.7 for v in c)                             # a leather sheen
        if mat == "laces" and f == "north" and (int(t * fh * TEXELS) + int(s * fw * TEXELS * 0.8)) % 4 < 1:
            c = (58, 62, 76)                                          # crossed laces
        if mat == "gun_barrel" and f in ("north", "south", "east", "west", "up") and abs(t - 0.5) < 0.12 and int(s * fw * 2) % 3 == 0:
            c = (200, 200, 206)                                       # the toy rifle's dashed stripe
        if mat == "gun_brown_checker" and (int(s * fw * TEXELS) + int(t * fh * TEXELS)) % 2:
            c = tuple(v * 0.78 for v in c)                            # the checkered grip
        if mat == "patch":                                            # the little blue screen on the chest
            if 0.2 < t < 0.8 and 0.1 < s < 0.9:
                n1 = _noise(s * 6, t * 4, 31)
                c = mix((18, 34, 92), (50, 100, 200), n1)                 # shades of blue
                c = mix(c, (110, 170, 255), 0.35 * max(0.0, 1 - abs(t - 0.35) / 0.08))   # a lighter band
                if int(t * fh * TEXELS) % 2 == 0:
                    c = tuple(v * 0.85 for v in c)                    # faint scan lines
                c = mix(c, (10, 20, 60), 0.4 * max(0.0, min(s, 1 - s, t - 0.2, 0.8 - t) < 0.05))  # its dark rim
            else:
                c = (14, 14, 18)
        # the ghost: a faint blue glow along the edges, and a cold tint rising from the feet (not on her gun)
        edge = min(s * fw, (1 - s) * fw, t * fh, (1 - t) * fh) if not mat.startswith("gun_") else 9.0
        c = mix(c, GHOST_RIM, 0.1 * max(0.0, 1.0 - edge / 0.15))
        c = mix(c, (30, 60, 140), 0.12 * max(0.0, 1.0 - wy / 10.0))
    c = tuple(v * shade for v in c)
    return tuple(max(0, min(255, int(v))) for v in c), max(0.0, min(1.0, alpha))

def glow_texel(mat, f, s, t, wy, wx, i, fw, fh):
    """What glows in the dark, as light added on top (black adds nothing; the colour is already scaled by how
    strongly it glows): the eyes, the fire's blue, the rim of the ghost (not her gun)."""
    c, a = texel(mat, f, s, t, wy, wx, i, fw, fh)
    if mat == "eye":
        k = 1.0
    elif mat in ("fire", "flame", "flame_tip"):
        k = min(1.0, max(0.0, (c[2] - c[0] - 40) / 200.0) * 1.4)
    elif mat.startswith("gun_"):
        k = 0.0
    else:
        c = GHOST_RIM
        k = 0.15 * max(0.0, 1.0 - min(s * fw, (1 - s) * fw, t * fh, (1 - t) * fh) / 0.15)
    return tuple(int(v * k) for v in c), 1.0

# ---------------------------------------------------------------- animations
# Bone rotations are GeckoLib's (as they go in the .animation.json); a bone's rotation matrix is
# Rz(-z) Ry(y) Rx(-x) in model space (the same convention as odie_model.py, checked in the game): negative X swings
# an arm or leg forward, positive X tips the body forward, positive Z swings her right arm out. Each animation gives
# the body and legs, and where the rifle is (its grip point, which way it points, which way its top faces, relative
# to her body); her right arm is then solved so her fist closes on the grip, the gun bone turned to point the rifle,
# and her left arm solved so her left hand holds the forend under the barrel. (Her head's turn comes from where she
# looks, in the game.)
PARENT = {n: p for n, p, _ in BONES}
def _mmul(a, b): return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def _mvec(m, v): return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))
def _mT(m): return [[m[j][i] for j in range(3)] for i in range(3)]
def _rx(a): c, s_ = math.cos(a), math.sin(a); return [[1, 0, 0], [0, c, -s_], [0, s_, c]]
def _ry(a): c, s_ = math.cos(a), math.sin(a); return [[c, 0, s_], [0, 1, 0], [-s_, 0, c]]
def _rz(a): c, s_ = math.cos(a), math.sin(a); return [[c, -s_, 0], [s_, c, 0], [0, 0, 1]]
def _rot(r):
    x, y, z = (math.radians(v) for v in r)
    return _mmul(_rz(-z), _mmul(_ry(y), _rx(-x)))
def _euler(m):
    b = math.asin(max(-1.0, min(1.0, -m[2][0]))); a = math.atan2(m[2][1], m[2][2]); c = math.atan2(m[1][0], m[0][0])
    return (math.degrees(-a), math.degrees(b), math.degrees(-c))
def _sub(a, b): return tuple(a[i] - b[i] for i in range(3))
def _addv(a, b): return tuple(a[i] + b[i] for i in range(3))
def _mul(a, k): return tuple(v * k for v in a)
def _dot(a, b): return sum(a[i] * b[i] for i in range(3))
def _cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def _norm(a):
    l = math.sqrt(_dot(a, a)) or 1.0; return _mul(a, 1 / l)

def transform(bone, pose):
    """(R, t): where a bone puts a point p of its geometry: R p + t (the whole chain, in model space)."""
    R, t = [[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0)
    chain = []
    while bone:
        chain.append(bone); bone = PARENT[bone]
    for b in reversed(chain):
        r = _rot(pose.get(b, (0, 0, 0))); pv = PIVOT[b]; off = pose.get("pos:" + b, (0, 0, 0))
        lt = _addv(off, _sub(pv, _mvec(r, pv)))
        t = _addv(t, _mvec(R, lt)); R = _mmul(R, r)
    return R, t

def world(p, bone, pose):
    R, t = transform(bone, pose)
    return _addv(_mvec(R, p), t)

def _point_bone(pose, bone, rest_dir, world_dir):
    """Turn a limb bone (no twist) so its rest direction rest_dir (model space) points along world_dir."""
    Rp, _ = transform(PARENT[bone], pose)
    d = _norm(_mvec(_mT(Rp), world_dir)); v = _norm(rest_dir)
    a = math.asin(max(-1.0, min(1.0, d[2] / v[1]))) if abs(v[1]) > 1e-6 else 0.0
    c = math.atan2(d[1], d[0]) - math.atan2(v[1] * math.cos(a), v[0])
    pose[bone] = (-math.degrees(a), 0.0, -math.degrees(c))

def _ik(s_, target, a, b, pole):
    d = _sub(target, s_); dist = min(math.sqrt(_dot(d, d)), a + b - 1e-3); u = _norm(d)
    x = (a * a - b * b + dist * dist) / (2 * dist); h = math.sqrt(max(0.0, a * a - x * x))
    p = _norm(_sub(pole, _mul(u, _dot(pole, u))))
    return _addv(_addv(s_, _mul(u, x)), _mul(p, h))

HAND = {"right": GUN_GRIP, "left": (-GUN_GRIP[0], GUN_GRIP[1], GUN_GRIP[2])}
POLE = {"right": (-0.7, -0.3, 0.6), "left": (0.6, -0.6, 0.3)}       # where the elbows point: out, down and back

def _reach(pose, side, target):
    """Her arm on this side reaching so her hand gets to target (as near as it can)."""
    s0, e0, h0 = PIVOT[f"{side}_arm"], PIVOT[f"{side}_forearm"], HAND[side]
    a, b = math.dist(s0, e0), math.dist(e0, h0)
    shoulder = world(s0, "body", pose)
    elbow = _ik(shoulder, target, a, b, POLE[side])
    pose[f"{side}_arm"] = (0, 0, 0); pose[f"{side}_forearm"] = (0, 0, 0)
    _point_bone(pose, f"{side}_arm", _sub(e0, s0), _sub(elbow, shoulder))
    elbow_now = world(e0, f"{side}_arm", pose)
    reach = _sub(target, elbow_now)
    if _dot(reach, reach) > 1e-6:
        _point_bone(pose, f"{side}_forearm", _sub(h0, e0), reach)

def hold(pose, grip, aim, up):
    """Puts the rifle in her hands: grip, aim (muzzle direction) and up given relative to her body."""
    R_body, _ = transform("body", pose)
    g = world(grip, "body", pose)
    d = _norm(_mvec(R_body, aim)); u = _mvec(R_body, up)
    u = _norm(_sub(u, _mul(d, _dot(u, d))))
    _reach(pose, "right", g)
    # turn the gun bone so the rifle (built pointing -z, top up) points along d with its top along u
    R_fore, _ = transform("right_forearm", pose)
    rest = [(0.0, 0.0, -1.0), (0.0, 1.0, 0.0), _cross((0.0, 0.0, -1.0), (0.0, 1.0, 0.0))]
    want = [d, u, _cross(d, u)]
    M = [[sum(want[k][i] * rest[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    pose["gun"] = _euler(_mmul(_mT(R_fore), M))
    # the left hand under the barrel, toward the front of the forend
    forend = _addv(_addv(g, _mul(d, GUN_FOREND)), _mul(u, -0.45))
    _reach(pose, "left", forend)
    return pose

# where the rifle is held (grip point, muzzle direction, top), relative to her unturned body
LOW_READY = ((-1.0, 16.6, -3.8), _norm((0.55, -0.55, -0.62)), (0.0, 1.0, 0.0))     # a SWAT low ready: butt at her
                                                                                     # right chest, muzzle down to her left
AIM = ((-1.5, 19.3, -4.4), _norm((0.06, 0.0, -1.0)), (0.0, 1.0, 0.0))              # shouldered, sighting along it

def _smooth(u):
    u = max(0.0, min(1.0, u)); return u * u * (3 - 2 * u)

def _lerp_hold(a, b, u):
    return (tuple(a[0][i] + (b[0][i] - a[0][i]) * u for i in range(3)), _norm(tuple(a[1][i] + (b[1][i] - a[1][i]) * u for i in range(3))),
            _norm(tuple(a[2][i] + (b[2][i] - a[2][i]) * u for i in range(3))))

def _keyed(keys, t):
    """Value at time t from [(time, value), ...] with smoothstep easing (values: dicts of tuples, or holds)."""
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            u = _smooth((t - t0) / (t1 - t0))
            if isinstance(a, dict):
                return {k: tuple(a.get(k, (0, 0, 0))[i] + (b.get(k, (0, 0, 0))[i] - a.get(k, (0, 0, 0))[i]) * u for i in range(3))
                        for k in set(a) | set(b)}
            return _lerp_hold(a, b, u)
    return keys[-1][1] if t > keys[-1][0] else keys[0][1]

def _with(body, rifle):
    return hold(dict(body), *rifle)

def idle(t):
    ph = 2 * math.pi * t / 4.0
    g, d, u = LOW_READY
    return _with({"body": (1.2 * math.sin(ph), 0, 0)}, ((g[0], g[1] + 0.15 * math.sin(ph), g[2]), d, u))

def stride(t, length, swing, knee, lean, lift=0.0):
    ph = 2 * math.pi * t / length
    body = {"right_leg": (-swing * math.sin(ph), 0, 0), "left_leg": (swing * math.sin(ph), 0, 0),
            "right_shin": (knee * max(0.0, math.cos(ph)), 0, 0), "left_shin": (knee * max(0.0, -math.cos(ph)), 0, 0),
            "body": (lean, 3 * math.sin(ph), 0), "pos:body": (0, -0.25 * abs(math.cos(ph)), 0)}
    g, d, u = LOW_READY
    return _with(body, ((g[0], g[1] + lift + 0.2 * math.sin(2 * ph), g[2]), d, u))

def aim(t):
    return _with({}, _keyed([(0.0, LOW_READY), (0.3, AIM)], t))

def shoot(t):
    g, d, u = AIM
    kick = ((g[0], g[1] + 0.25, g[2] + 0.7), _norm((d[0], 0.2, d[2])), u)          # the recoil: back and muzzle up
    body = _keyed([(0.0, {"body": (0, 0, 0)}), (0.05, {"body": (-5, 0, 0)}), (0.45, {"body": (0, 0, 0)}), (0.5, {"body": (0, 0, 0)})], t)
    return _with(body, _keyed([(0.0, AIM), (0.05, kick), (0.45, AIM), (0.5, AIM)], t))

def strike(t):
    wind = ((-1.4, 17.4, -2.0), _norm((0.25, 0.1, -1.0)), (0.0, 1.0, 0.0))      # drawn back
    thrust = ((-0.9, 17.0, -6.0), _norm((0.05, -0.15, -1.0)), (0.0, 1.0, 0.0))   # driven forward
    body = _keyed([(0.0, {}), (0.25, {"body": (-3, 20, 0)}), (0.38, {"body": (10, -15, 0)}), (0.7, {})], t)
    return _with(body, _keyed([(0.0, LOW_READY), (0.25, wind), (0.38, thrust), (0.7, LOW_READY)], t))

def hurt(t):
    body = _keyed([(0.0, {}), (0.1, {"body": (-10, 0, 0)}), (0.35, {})], t)
    return _with(body, LOW_READY)

def death(t):
    body = _keyed([(0.0, {}), (0.3, {"body": (-10, 0, 0), "root": (0, 0, 0)}), (1.1, {"body": (0, 0, 0), "root": (88, 0, 0)}),
                   (2.0, {"body": (0, 0, 0), "root": (88, 0, 0)})], t)            # staggers, then falls flat on her front
    return _with(body, LOW_READY)

def flicker(t):
    ph = 2 * math.pi * t / 1.6
    return {"flame": (5 * math.sin(ph) + 2 * math.sin(3 * ph), 3 * math.sin(ph + 2), 4 * math.sin(2 * ph + 1))}

# her eyes (on their own controller, over whatever else she's doing): open, a blink (every 15-25 seconds, picked in
# the game), and narrowed while she goes after someone, focused on them
def _eyes(open_):
    return {"scale:right_eye": (1, open_, 1), "scale:left_eye": (1, open_, 1)}

def blink(t):
    return _eyes(_keyed([(0.0, {"v": (1, 0, 0)}), (0.08, {"v": (0.08, 0, 0)}), (0.13, {"v": (0.08, 0, 0)}), (0.25, {"v": (1, 0, 0)})], t)["v"][0])

ANIMATIONS = [("idle", True, 4.0, idle), ("walk", True, 1.0, lambda t: stride(t, 1.0, 24, 30, 2)),
              ("run", True, 0.6, lambda t: stride(t, 0.6, 42, 70, 10, 0.6)), ("aim", "hold_on_last_frame", 0.4, aim),
              ("shoot", "hold_on_last_frame", 0.5, shoot), ("strike", "hold_on_last_frame", 0.7, strike),
              ("hurt", "hold_on_last_frame", 0.35, hurt), ("death", "hold_on_last_frame", 2.0, death),
              ("flicker", True, 1.6, flicker), ("eyes_open", True, 0.5, lambda t: _eyes(1.0)),
              ("blink", "hold_on_last_frame", 0.25, blink), ("squint", True, 0.5, lambda t: _eyes(0.4))]

def _continuous(prev, cur):
    """The same rotation written closest to the last frame's (so a bone never spins the long way round)."""
    if prev is None:
        return cur
    alt = (cur[0] - 180, 180 - cur[1], cur[2] - 180)
    def near(a, b):
        return tuple(b[i] + ((a[i] - b[i] + 180) % 360 - 180) for i in range(3))
    c1, c2 = near(cur, prev), near(alt, prev)
    return c1 if sum(abs(c1[i] - prev[i]) for i in range(3)) <= sum(abs(c2[i] - prev[i]) for i in range(3)) else c2

def animations():
    out = {}
    for name, loop, length, fn in ANIMATIONS:
        bones, last = {}, {}
        steps = max(2, round(length * 20))
        for k in range(steps + 1):
            t = round(k * length / steps, 4)
            pose = fn(t)
            for key, v in pose.items():
                if key.startswith("pos:"):
                    bones.setdefault(key[4:], {}).setdefault("position", {})[str(t)] = [round(c, 3) for c in v]
                elif key.startswith("scale:"):
                    bones.setdefault(key[6:], {}).setdefault("scale", {})[str(t)] = [round(c, 3) for c in v]
                else:
                    v = _continuous(last.get(key), v); last[key] = v
                    bones.setdefault(key, {}).setdefault("rotation", {})[str(t)] = [round(c, 3) for c in v]
        out[name] = {"loop": loop, "animation_length": length, "bones": bones}
    return out

# ---------------------------------------------------------------- texture layout and painting
def face_size(a, b, f):
    w, h, d = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[f]

BLEED = 1

def layout():
    """Shelf-pack every face into the texture (UV units = model px), with a unit of bleed around each."""
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

def face_point(a, b, f, s, t):
    """Model-space point (x, y) at s, t on a face (t from the top for side faces)."""
    if f in ("north", "south"):
        return a[0] + s * (b[0] - a[0]), b[1] - t * (b[1] - a[1])
    if f in ("east", "west"):
        return (a[0] + b[0]) / 2, b[1] - t * (b[1] - a[1])
    return a[0] + s * (b[0] - a[0]), b[1] if f == "up" else a[1]

def paint(rects, fn):
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
                wx, wy = face_point(a, b, f, s, t)
                c, al = fn(mat, f, s, t, wy, wx, i, w, h)
                if 0 <= x < TEX_W and 0 <= y < TEX_H:
                    px[x, y] = c + (int(255 * al),)
    return img

# ---------------------------------------------------------------- export
def geo(rects):
    bones = []
    for name, parent, pivot in BONES:
        b = {"name": name, "pivot": list(pivot)}
        if parent:
            b["parent"] = parent
        cubes = []
        for i, (cname, bone, a, bb, mat) in enumerate(CUBES):
            if bone != name:
                continue
            uv = {}
            for f in FACES:
                u, v, w, h = rects[(i, f)]
                fw, fh = face_size(a, bb, f)
                if f in ("up", "down"):                    # Bedrock draws these flipped
                    uv[f] = {"uv": [round(u + fw, 4), round(v + fh, 4)], "uv_size": [round(-fw, 4), round(-fh, 4)]}
                else:
                    uv[f] = {"uv": [round(u, 4), round(v, 4)], "uv_size": [round(fw, 4), round(fh, 4)]}
            cubes.append({"origin": [round(c, 4) for c in a], "size": [round(bb[k] - a[k], 4) for k in range(3)], "uv": uv})
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.sandman", "texture_width": TEX_W // TEXELS, "texture_height": TEX_H // TEXELS,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.2, 0]},
        "bones": bones}]}

def bbmodel(geometry, texture_png, anims):
    """The model as a Blockbench project (Bedrock format, per-face UV) with the texture built in. Blockbench's space
    is the game's, which mirrors Bedrock's X: cube and pivot X flip."""
    desc = geometry["minecraft:geometry"][0]
    ids = {b["name"]: str(uuid.uuid5(uuid.NAMESPACE_URL, "sandman/" + b["name"])) for b in desc["bones"]}
    elements, children = [], {b["name"]: [] for b in desc["bones"]}
    cube_names = {}
    for name, bone, a, b, mat in CUBES:
        cube_names.setdefault(bone, []).append(name)
    for b in desc["bones"]:
        for i, c in enumerate(b.get("cubes", [])):
            o, sz = c["origin"], c["size"]
            faces = {}
            for f, fuv in c["uv"].items():
                (u, v), (w, h) = fuv["uv"], fuv["uv_size"]
                faces[f] = {"uv": [u, v, u + w, v + h], "texture": 0}
            eid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"sandman/{b['name']}/{i}"))
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
    texture = {"name": "sandman.png", "folder": "entity", "namespace": "horrormod", "id": "0",
               "width": TEX_W, "height": TEX_H, "uv_width": TEX_W // TEXELS, "uv_height": TEX_H // TEXELS,
               "particle": False, "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True,
               "saved": True, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "sandman.png/" + hashlib.sha1(texture_png).hexdigest())),
               "source": "data:image/png;base64," + base64.b64encode(texture_png).decode()}
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False},
            "name": "sandman", "model_identifier": "geometry.sandman", "visible_box": [3, 3, 1.2],
            "resolution": {"width": TEX_W // TEXELS, "height": TEX_H // TEXELS},
            "elements": elements, "outliner": [group("root")], "textures": [texture],
            "animations": [{"uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "sandman/anim/" + name)), "name": name,
                            "loop": "loop" if a["loop"] is True else "hold", "override": False, "length": a["animation_length"],
                            "snapping": 20, "selected": False,
                            "animators": {ids[bone]: {"name": bone, "type": "bone", "keyframes": [
                                {"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "time": float(t),
                                 "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"sandman/{name}/{bone}/{ch}/{t}")),
                                 "color": -1, "interpolation": "linear"}
                                for ch in ("rotation", "position", "scale") for t, v in chans.get(ch, {}).items()]}
                                for bone, chans in a["bones"].items()}}
                           for name, a in anims.items()]}

# ---------------------------------------------------------------- preview (textured, from four sides)
def preview(path, head=False, pose=None, title=""):
    """Four views, whole (head=False) or a close-up of the head and hair; posed if a pose is given."""
    views = [("front", 0), ("three-quarter", -35), ("side", -90), ("back", 180)]
    W, H, S, CELL = (300, 520, 15.0, 0.25) if not head else (300, 330, 30.0, 0.12)
    lift = 0.0 if not head else 19.6
    sheet = Image.new("RGB", (W * len(views), H), (10, 10, 14))
    for k, (title, yaw) in enumerate(views):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        def view(p):
            x, y, z = p
            return (x * cy - z * sy, y, x * sy + z * cy)
        polys = []
        normals = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
        for i, (name, bone, a, b, mat) in enumerate(CUBES):
            corners = {"north": [(a[0], b[1], a[2]), (b[0], b[1], a[2]), (b[0], a[1], a[2]), (a[0], a[1], a[2])],
                       "south": [(b[0], b[1], b[2]), (a[0], b[1], b[2]), (a[0], a[1], b[2]), (b[0], a[1], b[2])],
                       "west": [(a[0], b[1], b[2]), (a[0], b[1], a[2]), (a[0], a[1], a[2]), (a[0], a[1], b[2])],
                       "east": [(b[0], b[1], a[2]), (b[0], b[1], b[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2])],
                       "up": [(a[0], b[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2]), (a[0], b[1], a[2])],
                       "down": [(a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2])]}
            if pose is not None:
                R_b, t_b = transform(bone, pose)
                corners = {f: [_addv(_mvec(R_b, c), t_b) for c in q] for f, q in corners.items()}
                normals_b = {f: _mvec(R_b, n) for f, n in normals.items()}
            else:
                normals_b = normals
            for f, q in corners.items():
                if view(normals_b[f])[2] > 0.01:                      # facing away (the camera looks along +z)
                    continue
                fw, fh = face_size(a, b, f)
                nu, nv = max(1, round(fw / CELL)), max(1, round(fh / CELL))
                p0, p1, p3 = q[0], q[1], q[3]
                def pt(s, t):
                    return tuple(p0[j] + (p1[j] - p0[j]) * s + (p3[j] - p0[j]) * t for j in range(3))
                for iu in range(nu):
                    for iv in range(nv):
                        s0, s1, t0, t1 = iu / nu, (iu + 1) / nu, iv / nv, (iv + 1) / nv
                        quad = [view(pt(s0, t0)), view(pt(s1, t0)), view(pt(s1, t1)), view(pt(s0, t1))]
                        sc, tc = (s0 + s1) / 2, (t0 + t1) / 2
                        wx, wy = face_point(a, b, f, sc, tc)
                        c, al = texel(mat, f, sc, tc, wy, wx, i, fw, fh)
                        polys.append((sum(p[2] for p in quad) / 4, quad, c, al))
        img = Image.new("RGBA", (W, H), (10, 10, 14, 255)); d = ImageDraw.Draw(img, "RGBA")
        for depth, quad, c, al in sorted(polys, key=lambda p: -p[0]):
            d.polygon([(W / 2 + p[0] * S, H - 30 - (p[1] - lift) * S) for p in quad], fill=c + (int(255 * al),))
        dd = ImageDraw.Draw(img)
        dd.line([(0, H - 30), (W, H - 30)], fill=(40, 40, 48))
        dd.line([(0, H - 30 - (28.8 - lift) * S), (W, H - 30 - (28.8 - lift) * S)], fill=(60, 30, 30))
        dd.text((6, H - 30 - (28.8 - lift) * S - 14), "player height", fill=(120, 70, 70))
        dd.text((6, 6), title + (" " + globals().get("_PREVIEW_TITLE", "") if pose is not None else ""), fill=(150, 150, 170))
        sheet.paste(img.convert("RGB"), (k * W, 0))
    sheet.save(path)

if __name__ == "__main__":
    rects = layout()
    tex = paint(rects, texel); glow = paint(rects, glow_texel)
    tex_path = os.path.join(ASSETS, "textures", "entity", "sandman.png")
    tex.save(tex_path); glow.save(os.path.join(ASSETS, "textures", "entity", "sandman_glow.png"))
    geometry = geo(rects)
    os.makedirs(os.path.join(ASSETS, "geo", "entity"), exist_ok=True)
    json.dump(geometry, open(os.path.join(ASSETS, "geo", "entity", "sandman.geo.json"), "w"), indent=1)
    anims = animations()
    os.makedirs(os.path.join(ASSETS, "animations", "entity"), exist_ok=True)
    json.dump({"format_version": "1.8.0", "animations": anims},
              open(os.path.join(ASSETS, "animations", "entity", "sandman.animation.json"), "w"), separators=(",", ":"))
    json.dump(bbmodel(geometry, open(tex_path, "rb").read(), anims), open(BBMODEL, "w"), indent=1)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    if len(sys.argv) > 2:
        preview(sys.argv[2], head=True)
    top = max(c[3][1] for c in CUBES)
    print(f"wrote sandman geo, texture, glow and {BBMODEL}; {len(CUBES)} cubes, {top / 16:.2f} blocks tall")
