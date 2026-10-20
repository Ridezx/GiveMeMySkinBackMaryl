"""The Hollow: someone who chose the wrong skin too many times. A Skinwalker's victim that kept going, wearing skin
gone rotten (like the Rotted Pelt): a tall, gaunt corpse of a man (about 2 blocks) in what's left of his clothes, after
the user's reference picture. A sunken, skull-like face with empty black eye sockets, a brow ridge and bare
cheekbones, yellowed teeth and the flesh of one cheek rotted away to the jawbone, stringy hair hanging over the brow
and down the back of the head; a sickly green-grey skin, blotched, bruised and dotted with raw sores; an olive field
jacket hanging open over a stained red shirt torn across the ribs, frayed and holed, its hem in rags; khaki trousers
gone yellow with grime and mud, the right one torn open at the knee and short over a bony shin, the left ragged over
the boot; heavy, cracked leather work boots; long arms ending in big bony fists, the right sleeve ripped away from
a forearm rotted to the bone. The jaw is hinged inside the cheeks, so opening it shows a dark mouth, not the whole lower
face dropping.

The model stands normally (arms at its sides, upright); its hunched, deformed stance is in every animation (STANCE).
Its colours are kept dark (DARKEN), the skin a dim, dead green.

Writes:
    src/main/resources/assets/horrormod/geo/entity/hollow.geo.json      GeckoLib geometry
    src/main/resources/assets/horrormod/textures/entity/hollow.png       the texture (cutout: rips and holes in the
                                                                          clothes are see-through)
    art/hollow.bbmodel                                                    a Blockbench project to look at (edit this
                                                                          script, not that file)
    src/main/resources/assets/horrormod/animations/entity/hollow.animation.json   its animations (also in the .bbmodel)
    [preview.png]                                                         front / three-quarter / side / back
    [head_preview.png]                                                    the same, close up on the head
Run: python3 art/tools/hollow_model.py [preview.png [head_preview.png]]  (needs Pillow)
"""
import base64, hashlib, json, math, os, random, sys, uuid
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod"))
BBMODEL = os.path.normpath(os.path.join(HERE, "..", "hollow.bbmodel"))

DARKEN = 0.78                   # everything a shade darker: dim, filthy, dead
TEXELS = 4                      # texture pixels per model pixel
TEX_W, TEX_H = 512, 768        # texture size in pixels (128 x 192 UV units)
# A cube's six faces by where they are (n/p: the negative or positive side of x, y or z), and the face of the
# .geo.json each one is (checked against GeckoLib's cube building: its "west" face is on the +x side).
FACES = ("nz", "pz", "nx", "px", "py", "ny")
JSON_FACE = {"nz": "north", "pz": "south", "nx": "east", "px": "west", "py": "up", "ny": "down"}

# ---------------------------------------------------------------- rig: bones (name, parent, pivot)
BONES = [("root", None, (0, 0, 0)),
         ("hips", "root", (0, 16.0, 0)),
         ("body", "hips", (0, 17.5, 0)),
         ("head", "body", (0, 26.8, 0)),
         ("jaw", "head", (0, 28.0, 0.5)),
         ("right_arm", "body", (-3.8, 25.0, 0)), ("left_arm", "body", (3.8, 25.0, 0)),
         ("right_forearm", "right_arm", (-3.95, 19.8, 0)), ("left_forearm", "left_arm", (3.95, 19.8, 0)),
         ("right_hand", "right_forearm", (-4.05, 14.0, 0)), ("left_hand", "left_forearm", (4.05, 14.0, 0)),
         ("right_leg", "hips", (-1.65, 15.8, 0)), ("left_leg", "hips", (1.65, 15.8, 0)),
         ("right_shin", "right_leg", (-1.65, 8.6, 0)), ("left_shin", "left_leg", (1.65, 8.6, 0)),
         ("right_foot", "right_shin", (-1.65, 1.6, 0.1)), ("left_foot", "left_shin", (1.65, 1.6, 0.1))]
PIVOT = {n: p for n, _, p in BONES}
PARENT = {n: p for n, p, _ in BONES}

# ---------------------------------------------------------------- the cubes (name, bone, from, to, material)
# It faces -z (north); its right is -x. Sizes in model pixels (16 a block); feet at y 0.
CUBES = []
def cube(name, bone, a, b, mat):
    CUBES.append((name, bone, tuple(a), tuple(b), mat))

def pair(name, bone, a, b, mat):
    """A cube on its right (-x) and its mirror on its left (on that side's bone: "leg" means right_leg / left_leg;
    a bone without sides, like body or head, is used as it is)."""
    sided = lambda side: bone if bone in PIVOT else side + "_" + bone
    cube("right_" + name, sided("right"), a, b, mat)
    cube("left_" + name, sided("left"), (-b[0], a[1], a[2]), (-a[0], b[1], b[2]), mat)

# boots: heavy leather work boots, a thick sole and heel, a rounded toe, the shaft with its tongue, laces and a
# collar slumped down
pair("sole", "foot", (-3.15, 0, -3.3), (-0.2, 0.75, 1.75), "sole")
pair("boot_heel", "foot", (-2.85, 0.75, 0.55), (-0.5, 1.5, 1.75), "sole")
pair("boot_foot", "foot", (-3.0, 0.75, -3.1), (-0.35, 2.5, 1.65), "boot")
pair("boot_toe", "foot", (-2.8, 0.75, -3.4), (-0.55, 2.1, -3.1), "boot")
pair("boot_shaft", "shin", (-2.85, 2.5, -1.45), (-0.45, 5.0, 1.55), "boot")
pair("boot_collar", "shin", (-2.95, 4.7, -1.55), (-0.35, 5.5, 1.65), "boot_light")
pair("boot_tongue", "shin", (-2.25, 2.4, -1.65), (-1.05, 5.6, -1.45), "boot_light")
pair("boot_laces", "shin", (-2.05, 2.6, -1.72), (-1.25, 5.2, -1.65), "laces")
pair("boot_foot_laces", "foot", (-2.05, 2.5, -2.8), (-1.25, 2.62, -1.45), "laces")
# shins: bony, the skin gone thin; the right trouser leg torn off short above them, the left ragged to the boot
pair("shin_skin", "shin", (-2.25, 5.0, -0.75), (-1.05, 8.8, 0.85), "skin")
cube("right_shin_bone", "right_shin", (-1.95, 5.6, -0.95), (-1.45, 7.6, -0.75), "bone")     # the tibia, showing through
cube("right_trouser_low", "right_shin", (-2.9, 6.6, -1.4), (-0.4, 8.8, 1.4), "trouser_low")
cube("left_trouser_low", "left_shin", (0.4, 4.9, -1.45), (2.95, 8.8, 1.45), "trouser_low")
# thighs: the trousers (holed, the skin inside showing through), a bagged knee, a cargo pocket on the left
pair("thigh_skin", "leg", (-2.75, 8.6, -1.15), (-0.55, 15.8, 1.15), "skin")
pair("thigh", "leg", (-3.05, 8.6, -1.45), (-0.25, 15.9, 1.45), "trousers_holey")
cube("right_knee", "right_leg", (-2.85, 8.3, -1.65), (-0.45, 9.9, -1.4), "trousers_rag")       # torn open
cube("left_knee", "left_leg", (0.45, 8.3, -1.65), (2.85, 9.9, -1.4), "trousers")
cube("left_pocket", "left_leg", (3.05, 11.0, -0.9), (3.3, 13.4, 0.9), "trousers_dark")
cube("left_pocket_flap", "left_leg", (3.05, 13.0, -1.0), (3.4, 13.6, 1.0), "trousers_dark")
# hips: the trousers' seat, a cracked belt with a rusted buckle
cube("pelvis", "hips", (-3.1, 15.0, -1.55), (3.1, 17.6, 1.55), "trousers")
cube("crotch", "hips", (-0.9, 14.4, -1.3), (0.9, 15.2, 1.3), "trousers")
cube("belt", "hips", (-3.2, 16.7, -1.7), (3.2, 17.4, 1.7), "belt")
cube("buckle", "hips", (-0.5, 16.6, -1.82), (0.5, 17.5, -1.7), "metal")
# the torso: a sunken belly, the chest in a stained red shirt torn open over the ribs
cube("belly", "body", (-2.3, 17.4, -1.15), (2.3, 20.2, 1.25), "skin")
cube("chest", "body", (-2.6, 20.0, -1.45), (2.6, 25.0, 1.45), "shirt")
cube("ribs", "body", (-0.9, 20.4, -1.56), (0.6, 23.8, -1.45), "ribs")
cube("shoulders", "body", (-3.3, 24.0, -1.35), (3.3, 25.5, 1.4), "jacket")
# the field jacket, hanging open: front panels, sides and back down past the hips, ragged and holed; folded
# lapels, a turned-up collar, pockets, a few buttons, rags hanging off the hem
pair("jacket_front", "body", (-3.1, 13.4, -1.95), (-0.95, 24.4, -1.5), "jacket_panel")
pair("jacket_side", "body", (-3.2, 13.6, -1.6), (-2.65, 24.0, 1.65), "jacket_panel")
cube("jacket_back", "body", (-3.1, 13.2, 1.5), (3.1, 25.0, 1.95), "jacket_panel")
pair("lapel", "body", (-2.4, 21.6, -2.12), (-1.0, 25.2, -1.92), "jacket_light")
cube("collar_back", "body", (-2.3, 25.0, 0.4), (2.3, 26.3, 1.75), "jacket_light")
pair("collar_side", "body", (-2.3, 25.0, -1.2), (-1.6, 26.0, 0.5), "jacket_light")
pair("breast_pocket", "body", (-2.9, 19.6, -2.08), (-1.5, 21.2, -1.93), "jacket_dark")
pair("hip_pocket", "body", (-2.95, 15.2, -2.08), (-1.3, 17.4, -1.93), "jacket_dark")
for k, y in enumerate((22.6, 20.4, 18.2)):
    cube(f"button_{k}", "body", (1.0, y, -2.03), (1.35, y + 0.35, -1.92), "button")
cube("jacket_rag_front", "body", (2.1, 11.6, -1.95), (2.8, 13.5, -1.7), "jacket_rag")
cube("jacket_rag_back", "body", (-2.6, 12.2, 1.62), (-1.7, 13.3, 1.9), "jacket_rag")
# the neck: thin, its tendons standing out
cube("neck", "body", (-0.75, 25.3, -0.7), (0.75, 27.1, 0.75), "skin")
pair("neck_tendon", "body", (-0.65, 25.4, -0.85), (-0.3, 26.9, -0.7), "skin_dark")
# the head: a narrow skull under thin skin, temples and the back of the skull standing out; a heavy brow over empty
# sockets (painted), bare cheekbones, what's left of the nose, ears; the upper teeth bared
cube("skull", "head", (-1.8, 28.0, -1.95), (1.8, 31.3, 2.0), "head")
cube("skull_top", "head", (-1.5, 31.3, -1.6), (1.5, 31.8, 1.7), "head")
cube("skull_back", "head", (-1.55, 28.3, 2.0), (1.55, 31.0, 2.4), "head")
pair("temple", "head", (-2.0, 28.7, -1.4), (-1.8, 30.9, 1.6), "head")
cube("brow", "head", (-1.7, 29.85, -2.25), (1.7, 30.35, -1.95), "brow")
pair("cheekbone", "head", (-1.75, 28.55, -2.15), (-0.75, 29.0, -1.95), "bone")
cube("nose", "head", (-0.28, 28.6, -2.18), (0.28, 29.6, -1.95), "nose")
cube("upper_teeth", "head", (-1.0, 27.95, -2.05), (1.0, 28.4, -1.85), "teeth")
pair("ear", "head", (-2.05, 28.5, -0.1), (-1.8, 29.8, 0.7), "skin")
# the cheeks stay with the skull (the left one rotted open); behind the teeth, the mouth: dark, a tongue at its floor
cube("right_cheek", "head", (-1.68, 27.35, -1.55), (-1.3, 28.2, 0.7), "skin")
cube("left_cheek", "head", (1.3, 27.35, -1.55), (1.68, 28.2, 0.7), "cheek_rot")
cube("mouth", "head", (-1.05, 27.1, -1.75), (1.05, 28.0, 0.4), "mouth")
# the jaw: hinged under the ears, inside the cheeks (so only the chin and lower teeth drop when it opens, showing
# the mouth); its left side rotted through to the jawbone and the back teeth
cube("jaw", "jaw", (-1.28, 26.85, -1.9), (1.28, 27.95, 0.75), "jaw")
cube("chin", "jaw", (-0.75, 26.6, -2.12), (0.75, 27.4, -1.9), "skin")
cube("lower_teeth", "jaw", (-0.95, 27.72, -2.05), (0.95, 28.05, -1.88), "teeth")
cube("jaw_bone", "jaw", (0.5, 27.0, -1.96), (1.36, 27.9, -0.75), "jaw_rot")
# stringy hair: thin strands down the back and sides of the head, a few falling over the brow
rnd = random.Random(5)
for k in range(9):                                          # down the back, uneven
    x = -1.35 + k * 0.34 + rnd.uniform(-0.1, 0.1)
    w = rnd.uniform(0.08, 0.16)
    cube(f"hair_back_{k}", "head", (x - w, rnd.choice((26.6, 27.4, 28.1, 28.8, 29.6)), 2.4 + rnd.uniform(0, 0.08)),
         (x + w, 31.0, 2.6 + rnd.uniform(0, 0.08)), "hair")
for side, sx in (("right", -1), ("left", 1)):              # down the sides, behind the ears
    for k, z in enumerate((0.75, 1.15, 1.55, 1.9)):
        top, bottom = 31.0, rnd.uniform(27.6, 28.8)
        x0 = 2.0 if z > 1.0 else 2.05
        cube(f"{side}_hair_{k}", "head", (min(sx * x0, sx * (x0 + 0.22)), bottom, z - 0.11),
             (max(sx * x0, sx * (x0 + 0.22)), top, z + 0.11), "hair")
for k, (x, bottom) in enumerate(((-1.25, 29.35), (-0.9, 29.7), (0.75, 29.55))):   # over the brow
    cube(f"hair_front_{k}", "head", (x - 0.11, bottom, -2.3), (x + 0.11, 31.4, -2.08), "hair")
cube("hair_crown", "head", (-1.35, 31.8, -0.8), (1.25, 31.95, 1.5), "hair")
# arms: long and thin; the sleeves hanging loose (open at the cuff), the right one ripped off below the elbow; long
# forearms, the right rotted to the bone along its outside; big bony hands hanging to the knees, the palms turned in
pair("sleeve", "arm", (-4.95, 19.6, -1.2), (-2.95, 25.3, 1.2), "sleeve")
pair("sleeve_cap", "arm", (-4.6, 25.3, -1.0), (-3.2, 25.85, 1.0), "jacket")
pair("upper_arm", "arm", (-4.6, 19.8, -0.8), (-3.3, 25.0, 0.8), "skin")
cube("right_cuff", "right_forearm", (-5.05, 17.8, -1.25), (-2.85, 20.0, 1.25), "sleeve_torn")
cube("left_cuff", "left_forearm", (2.85, 15.8, -1.25), (5.05, 20.0, 1.25), "sleeve")
pair("forearm", "forearm", (-4.55, 14.0, -0.7), (-3.35, 19.9, 0.7), "skin")
cube("right_radius", "right_forearm", (-4.72, 14.9, -0.35), (-4.5, 17.7, 0.15), "bone")
pair("wrist", "forearm", (-4.45, 13.6, -0.6), (-3.45, 14.3, 0.6), "skin_dark")
pair("palm", "hand", (-4.4, 11.6, -0.95), (-3.7, 14.0, 0.85), "skin")
# the hands clenched into bony fists: each finger bent at the knuckle, its tip tucked against the palm, the thumb
# folded across them
for k, z in enumerate((-0.72, -0.27, 0.18, 0.6)):
    pair(f"finger_{k}", "hand", (-4.52, 10.75, z - 0.21), (-3.62, 11.8, z + 0.21), "finger")
    pair(f"finger_{k}_tip", "hand", (-3.62, 10.85, z - 0.19), (-3.2, 11.6, z + 0.19), "finger_tip")
pair("thumb", "hand", (-4.35, 11.15, -1.32), (-3.4, 12.35, -0.93), "finger")

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

def n3(p, scale, k):
    """Noise over the model's surface (so a pattern runs on across the faces of neighbouring cubes)."""
    x, y, z = p
    return (_noise((x + 0.37 * z) * scale, y * scale, k) + _noise((z - 0.29 * x) * scale + 11.0, y * scale, k + 1)) / 2

def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(len(c1)))

def scale(c, k):
    return tuple(v * k for v in c)

def skin(p):
    """Rotten skin (like the Rotted Pelt): a sickly green-yellow going grey, blotched dark with rot, bruised, with
    dark veins and raw sores."""
    c = mix((108, 112, 78), (70, 80, 50), n3(p, 0.8, 3))
    c = mix(c, (104, 98, 82), max(0.0, n3(p, 0.45, 9) - 0.5) * 1.3)
    c = mix(c, (60, 58, 38), min(1.0, max(0.0, (n3(p, 1.1, 13) - 0.56) / 0.2)) * 0.75)          # rot
    c = mix(c, (98, 74, 86), min(1.0, max(0.0, (n3(p, 0.7, 17) - 0.6) / 0.15)) * 0.45)          # bruising
    ridge = 1.0 - abs(2.0 * n3(p, 1.3, 21) - 1.0)
    c = mix(c, (66, 80, 56), max(0.0, (ridge - 0.9) / 0.1) * 0.6)                               # veins
    sore = n3(p, 3.0, 25)
    if sore > 0.74:
        c = mix(c, (66, 42, 30), min(1.0, (sore - 0.74) / 0.04) * 0.7)                          # a sore's dark rim
    if sore > 0.79:
        c = mix((150, 56, 44), (110, 30, 28), _noise(p[0] * 8, p[1] * 8, 27))                    # raw flesh
    return c

def jacket_colour(p):
    c = mix((74, 80, 48), (106, 106, 68), n3(p, 0.6, 51))
    c = mix(c, (126, 122, 86), max(0.0, (n3(p, 1.6, 53) - 0.64) / 0.12) * 0.6)                  # worn pale
    c = mix(c, (128, 88, 48), max(0.0, (n3(p, 0.5, 55) - 0.58) / 0.12) * 0.55)                   # rust stains
    c = mix(c, (66, 80, 50), max(0.0, (n3(p, 1.9, 57) - 0.68) / 0.1) * 0.6)                      # mould
    c = scale(c, 0.82 + 0.18 * min(1.0, max(0.0, (p[1] - 12.0) / 8.0)))                           # grimier lower down
    if _noise((p[0] + p[2]) * 3.0, p[1] * 0.3, 59) > 0.72:
        c = scale(c, 0.82)                                                                        # drips down it
    return c

def trousers_colour(p):
    c = mix((152, 138, 74), (118, 114, 60), n3(p, 0.7, 61))
    c = mix(c, (174, 158, 90), max(0.0, (n3(p, 1.4, 63) - 0.64) / 0.12) * 0.5)                   # sun-faded
    c = mix(c, (82, 84, 48), max(0.0, (n3(p, 0.9, 65) - 0.66) / 0.1) * 0.55)                     # stains
    c = mix(c, (88, 70, 46), max(0.0, (9.5 - p[1]) / 6.0) * (0.55 + 0.45 * n3(p, 1.2, 66)))      # mud up the legs
    crease = 1.0 - abs(2.0 * _noise(p[1] * 2.2, (p[0] + p[2]) * 0.8, 67) - 1.0)
    return scale(c, 1.0 - 0.15 * max(0.0, (crease - 0.85) / 0.15))

def leather(p, light=False):
    c = mix((116, 86, 60), (86, 62, 42), n3(p, 0.9, 71))
    if light:
        c = mix(c, (150, 120, 86), 0.45)
    crease = 1.0 - abs(2.0 * _noise(p[1] * 2.6, (p[0] + p[2]) * 1.5, 73) - 1.0)
    c = scale(c, 1.0 - 0.25 * max(0.0, (crease - 0.88) / 0.12))
    c = mix(c, (160, 128, 96), max(0.0, (n3(p, 2.2, 75) - 0.7) / 0.1) * 0.5)                     # scuffs
    return mix(c, (82, 66, 46), max(0.0, (1.8 - p[1]) / 1.2) * 0.75)                              # caked mud

# cloth with holes and ragged hems: the hole pattern's threshold (higher, fewer), how far up the hem's rags reach (px),
# and whether its end is open (a sleeve or trouser leg: the arm or leg inside shows)
CLOTH = {"jacket_panel": (0.74, 1.0, False), "jacket_rag": (0.8, 0.9, False), "sleeve": (0.8, 0.8, True),
         "sleeve_torn": (0.76, 1.4, True), "trousers_holey": (0.76, 0.0, False), "trousers_rag": (0.6, 0.5, False),
         "trouser_low": (0.78, 1.1, True)}

def cloth_alpha(mat, f, p, a, b):
    """0 where the cloth is torn away; and how frayed it is there (0..1, darkening the edges of holes and rags)."""
    holes, hem, open_end = CLOTH[mat]
    if f == "ny" and open_end:
        return 0.0, 0.0
    h = n3(p, 1.5, 41)
    if h > holes:
        return 0.0, 0.0
    fray = max(0.0, (h - holes + 0.06) / 0.06)
    if hem > 0 and f not in ("py", "ny"):
        along = p[0] + p[2]
        rag = hem * (0.25 + 0.75 * _noise(along * 2.4, a[1] * 3.0, 43)) * (0.4 + 0.6 * (_noise(along * 7.0, 1.0, 45) > 0.35))
        above = p[1] - a[1]
        if above < rag:
            return 0.0, 0.0
        fray = max(fray, max(0.0, 1.0 - (above - rag) / 0.3))
    return 1.0, fray

def head_features(c, f, p):
    """The face painted on the skull, jaw and nose: empty eye sockets under the brow, hollow cheeks, the nose's
    holes, the rotted-through cheek; patchy, stringy hair over the crown."""
    x, y, z = p
    if f == "nz":
        for ex in (-0.85, 0.85):
            d = ((x - ex) / 0.6) ** 2 + ((y - 29.3) / 0.47) ** 2
            if d < 1.6:
                c = mix(c, scale(c, 0.45), (1.6 - d) / 0.6)                  # the sunken rim
            if d < 1.0:
                c = mix(scale(c, 0.3), (8, 6, 5), min(1.0, (1.0 - d) * 2.5))  # empty: black
        if 0.75 < abs(x) < 1.7 and 28.0 < y < 28.6:
            c = scale(c, 0.72)                                               # hollow cheeks
    if y > 30.3 and not (f == "nz" and y < 31.0):                            # the scalp: thin, stringy hair
        cover = _noise((x * 1.4 + z * 1.1), y * 0.8, 91)
        if cover > 0.33:
            strand = _noise((x + z) * 4.5, y * 0.6, 93)
            c = mix((44, 38, 30), (82, 72, 56), strand)
        else:
            c = scale(c, 0.85)
    return c

def texel(mat, f, s, t, p, i, fw, fh):
    """Colour and alpha (0 or 1) of one texel (s, t across the face 0..1, t from its top; p the point on the model)."""
    a, b = CUBES[i][2], CUBES[i][3]
    shade = {"py": 1.12, "ny": 0.62, "nz": 1.0, "pz": 0.86, "nx": 0.92, "px": 0.92}[f]
    alpha = 1.0
    grain = 0.93 + 0.14 * _hash(int(s * fw * TEXELS), int(t * fh * TEXELS), i)
    if mat in ("skin", "skin_dark", "head", "brow", "nose", "jaw", "finger", "finger_tip"):
        c = skin(p)
        if mat in ("skin_dark", "brow"):
            c = scale(c, 0.74)
        if mat in ("head", "brow", "nose", "jaw"):
            c = head_features(c, f, p)
        if mat == "head" and f == "ny" and p[2] < 0.5:
            c = (52, 18, 18)                                                 # the roof of the mouth
        if mat == "jaw" and f == "py":
            c = (110, 44, 42) if p[2] < 0.2 else (40, 14, 14)                # the tongue on the mouth's floor
        if mat == "nose" and f in ("nz", "ny"):
            for nx_ in (-0.12, 0.12):
                if ((p[0] - nx_) / 0.1) ** 2 + ((p[1] - 28.75) / 0.16) ** 2 < 1.0 or (f == "ny" and abs(p[0] - nx_) < 0.1):
                    c = (16, 10, 8)                                          # the nose's holes
        if mat == "jaw" and f in ("nz", "px"):                              # rotted through around the jawbone: raw,
            edge = 0.35 * _noise(p[1] * 3.0, p[2] * 3.0, 96)                   # with a ragged edge
            if (f == "nz" and p[0] > 0.3 + edge) or (f == "px" and p[2] < -0.45 - edge and p[1] > 26.85 + edge * 0.6):
                sinew = _noise((p[0] + p[2]) * 9.0, p[1] * 2.0, 97)
                c = mix((150, 56, 46), (70, 20, 20), sinew)
        if mat in ("finger", "finger_tip"):
            if f not in ("py", "ny") and abs(p[1] - b[1]) < 0.12:
                c = scale(c, 0.7)                                            # the knuckle's crease
            if mat == "finger_tip" and p[1] - a[1] < 0.4:
                c = mix((150, 130, 82), (86, 72, 46), (p[1] - a[1]) / 0.4)   # a thick, yellowed nail
    elif mat == "bone":
        c = mix((204, 192, 156), (164, 150, 116), n3(p, 2.0, 101))
        crack = 1.0 - abs(2.0 * n3(p, 2.4, 103) - 1.0)
        c = mix(c, (90, 78, 58), max(0.0, (crack - 0.9) / 0.1) * 0.8)
        c = mix(c, (120, 64, 50), max(0.0, (n3(p, 1.3, 105) - 0.62) / 0.15) * 0.5)          # flesh still on it
    elif mat == "mouth":
        c = (110, 44, 42) if p[1] < 27.4 and f in ("nz", "py") else (22, 8, 8)       # the tongue, then the dark
        if f == "nz" and p[1] >= 27.4:
            c = mix((40, 12, 12), (10, 4, 4), (p[1] - 27.4) / 0.6)
    elif mat == "cheek_rot":                                                 # rotted open: raw, the molars showing
        c = mix((140, 52, 44), (66, 20, 20), _noise((p[0] + p[2]) * 9.0, p[1] * 2.0, 97))
        if f == "px" and p[2] < -0.2 and abs(p[1] - 27.85) < 0.2:
            c = (176, 160, 108) if (p[2] * 3.2) % 1.0 > 0.22 else (40, 20, 16)
    elif mat == "jaw_rot":                                                   # back teeth over the bare jawbone
        if p[1] > 27.5 and f in ("nz", "px"):
            along = p[0] if f == "nz" else p[2]
            c = (176, 160, 108) if (along * 3.2) % 1.0 > 0.22 else (44, 22, 18)
        else:
            c = mix((190, 178, 142), (150, 136, 104), n3(p, 2.0, 109))
        c = mix(c, (96, 30, 28), max(0.0, (n3(p, 2.5, 110) - 0.55) / 0.2) * 0.7)                  # shreds of flesh
    elif mat == "ribs":
        if f == "nz":
            rib = ((b[1] - p[1]) % 0.62) < 0.38
            c = mix((196, 184, 146), (160, 146, 112), n3(p, 2.0, 107)) if rib else (62, 22, 20)
            if abs(p[0] + 0.15) < 0.12:
                c = (178, 166, 130)                                          # the breastbone
        else:
            c = (56, 20, 18)
    elif mat == "teeth":
        if f == "nz":
            k = math.floor((p[0] + 1.0) / 0.25)
            c = (188, 172, 116) if _hash(k, int(a[1] * 10), 111) > 0.18 else (36, 18, 14)    # some missing
            if abs((p[0] + 1.0) / 0.25 - round((p[0] + 1.0) / 0.25)) < 0.12:
                c = (70, 52, 36)                                             # the gaps between them
            c = scale(c, 0.8 + 0.2 * _noise(p[0] * 6, 1.0, 113))
        else:
            c = (96, 46, 42)                                                 # gums
    elif mat == "shirt":
        c = mix((114, 44, 40), (144, 88, 74), n3(p, 0.7, 121))
        c = mix(c, (64, 28, 22), max(0.0, (n3(p, 1.2, 123) - 0.58) / 0.12) * 0.7)            # old blood and dirt
    elif mat in CLOTH or mat.startswith("jacket") or mat.startswith("trousers"):
        trousers = mat.startswith("trouser")
        c = trousers_colour(p) if trousers else jacket_colour(p)
        if mat in ("jacket_light",):
            c = scale(c, 1.12)
        if mat in ("jacket_dark", "trousers_dark"):
            c = scale(c, 0.78)
        if f not in ("py", "ny") and min(s * fw, (1 - s) * fw) < 0.1 and mat.startswith("jacket_panel"):
            c = scale(c, 0.75)                                               # seams
        if mat in CLOTH:
            alpha, fray = cloth_alpha(mat, f, p, a, b)
            c = mix(c, scale(c, 0.55), fray)
    elif mat in ("boot", "boot_light"):
        c = leather(p, mat == "boot_light")
        if f != "py" and f != "ny" and abs(p[1] - 0.95) < 0.08 and int(((p[0] + p[2]) * 4)) % 2 == 0:
            c = (150, 130, 96)                                               # the welt's stitching
    elif mat == "sole":
        c = mix((56, 48, 42), (40, 34, 30), n3(p, 1.5, 131))
        if f == "ny" and int(p[2] * 2.5) % 2 == 0:
            c = (30, 26, 22)                                                 # tread
        c = mix(c, (84, 68, 48), 0.4 * n3(p, 1.0, 133))
    elif mat == "laces":
        c = (82, 70, 52) if (int((p[1] + p[2]) * 5) + int(p[0] * 5)) % 3 else (46, 38, 30)
    elif mat == "belt":
        c = mix((58, 44, 34), (40, 30, 24), n3(p, 1.2, 141))
        if f == "nz" and abs(p[1] - 17.05) < 0.12 and int(p[0] * 3) % 2 == 0:
            c = (24, 18, 14)                                                 # its holes
    elif mat == "metal":
        c = mix((126, 112, 88), (118, 70, 40), n3(p, 2.5, 151))              # rusted
    elif mat == "button":
        c = (62, 52, 40)
    elif mat == "hair":
        c = mix((42, 36, 28), (84, 74, 58), _noise((p[0] + p[2]) * 9.0, p[1] * 0.5, 161))
    else:
        raise KeyError(mat)
    c = tuple(v * shade * grain * DARKEN for v in c)
    return tuple(max(0, min(255, int(v))) for v in c), alpha

# ---------------------------------------------------------------- animations
# Bone rotations are GeckoLib's (as they go in the .animation.json); a bone's rotation matrix is Rz(-z) Ry(y) Rx(-x)
# in model space (as in odie_model.py and sandman_model.py, checked in the game): positive X tips the body or head
# forward, negative X swings an arm or leg forward. Positions ("pos:bone") are offsets in the parent bone's frame.
# How every pose is made:
# - The upper body has its stance (hunched over, the hips cocked, the head lolling to one side, the jaw hanging open)
#   plus whatever the animation adds.
# - The legs are solved (two-bone IK, knees forward) so each ankle is where the animation puts the foot, and each
#   foot is then turned so its sole lies flat (or tipped by as much as the animation says): a planted foot stands
#   flat on the ground and doesn't slide (in the walk it moves back at a steady pace, the speed the body goes
#   forward: WALK_SPEED).
# - The arms hang by their own weight (each bone pointed along a direction near straight down, whatever the body
#   does), swinging a little behind the body's movement, the forearms and the fists flopping further behind; the
#   right, rotted arm hangs twisted in.
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
def _addv(a, b): return tuple(a[i] + b[i] for i in range(3))
def _sub(a, b): return tuple(a[i] - b[i] for i in range(3))
def _mul(a, k): return tuple(v * k for v in a)
def _dot(a, b): return sum(a[i] * b[i] for i in range(3))
def _norm(a):
    l = math.sqrt(_dot(a, a)) or 1.0; return _mul(a, 1 / l)
def _lerp(a, b, u): return tuple(a[i] + (b[i] - a[i]) * u for i in range(len(a)))
def _smooth(u):
    u = max(0.0, min(1.0, u)); return u * u * (3 - 2 * u)

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

def _corners(a, b):
    return [(x, y, z) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])]

def lowest(pose, bones=None):
    """The lowest point of the model (or of these bones' cubes) in this pose."""
    low = 1e9
    for name, bone, a, b, mat in CUBES:
        if bones is None or bone in bones:
            R, t = transform(bone, pose)
            low = min(low, min(_addv(_mvec(R, c), t)[1] for c in _corners(a, b)))
    return low

def _solve(f, lo, hi):
    """x in [lo, hi] where f(x) = 0 (bisection; f changes sign across the range)."""
    flo = f(lo)
    for _ in range(40):
        mid = (lo + hi) / 2; fm = f(mid)
        if (fm > 0) == (flo > 0):
            lo, flo = mid, fm
        else:
            hi = mid
    return (lo + hi) / 2

# ---- legs and feet
THIGH = 15.8 - 8.6                                      # hip to knee
SHIN = math.hypot(8.6 - 1.6, 0.1)                       # knee to ankle
ANKLE_Y = 1.6                                           # the ankle's height with the foot flat on the ground

def _leg_bend(pose, side, ankle, splay):
    """The knee bend for an ankle at `ankle`, worked out in the hips' frame (front to back only)."""
    leg, shin = side + "_leg", side + "_shin"
    pose[leg] = (0, 0, 0); pose[shin] = (0, 0, 0)
    hip = world(PIVOT[leg], "hips", pose)
    R_h, _ = transform("hips", pose)
    d = _mvec(_mT(R_h), _sub(ankle, hip))
    dist = max(abs(THIGH - SHIN) + 1e-3, min(math.hypot(d[1], d[2]), THIGH + SHIN - 1e-3))
    phi = math.degrees(math.atan2(d[2], -d[1]))
    a = math.degrees(math.acos((THIGH ** 2 + dist ** 2 - SHIN ** 2) / (2 * THIGH * dist)))
    b = math.degrees(math.acos((SHIN ** 2 + dist ** 2 - THIGH ** 2) / (2 * SHIN * dist)))
    pose[leg] = (phi - a, 0, splay)
    pose[shin] = (a + b, 0, 0)

def leg_ik(pose, side, ankle):
    """Bends the leg (knee forward), and swings it in or out, so its ankle is at `ankle` (model space); a few
    rounds, each aiming off by however far the last one missed (the hips' roll and twist throw it off)."""
    leg, foot = side + "_leg", side + "_foot"
    aim, splay = ankle, 0.0
    for _ in range(5):
        _leg_bend(pose, side, aim, splay)
        rx = pose[leg][0]
        def miss_x(v):
            pose[leg] = (rx, 0, v)
            return world(PIVOT[foot], side + "_shin", pose)[0] - ankle[0]
        lo, hi = miss_x(-30.0), miss_x(30.0)
        splay = _solve(miss_x, -30.0, 30.0) if (lo > 0) != (hi > 0) else 0.0
        pose[leg] = (rx, 0, splay)
        got = world(PIVOT[foot], side + "_shin", pose)
        aim = (aim[0], aim[1] + ankle[1] - got[1], aim[2] + ankle[2] - got[2])

def foot_tip(pose, side, pitch=0.0):
    """Turns the foot so its sole is level from side to side and tipped `pitch` degrees toe-down (0: flat)."""
    foot = side + "_foot"; x0 = PIVOT[foot][0]
    def tilt(rx, rz):
        pose[foot] = (rx, 0, rz)
        f, b = world((x0, 0, -3.3), foot, pose), world((x0, 0, 1.75), foot, pose)
        l, r = world((x0 - 1.3, 0, -0.8), foot, pose), world((x0 + 1.3, 0, -0.8), foot, pose)
        return math.degrees(math.atan2(b[1] - f[1], math.hypot(f[2] - b[2], f[0] - b[0]))), l[1] - r[1]
    rz = 0.0
    for _ in range(2):
        rx = _solve(lambda v: tilt(v, rz)[0] - pitch, -100.0, 100.0)
        lo, hi = tilt(rx, -45.0)[1], tilt(rx, 45.0)[1]
        rz = _solve(lambda v: tilt(rx, v)[1], -45.0, 45.0) if (lo > 0) != (hi > 0) else 0.0
    pose[foot] = (rx, 0, rz)

def place_foot(pose, side, z, lift=0.0, pitch=0.0, drag=False):
    """The foot at depth z (model space; forward is -z), `lift` above the ground, tipped `pitch` toe-down; a
    dragged foot is held down so its toe scrapes the ground."""
    x = PIVOT[side + "_foot"][0]
    y = ANKLE_Y + lift
    for _ in range(3 if drag else 1):
        leg_ik(pose, side, (x, y, z))
        foot_tip(pose, side, pitch)
        if drag:
            y -= lowest(pose, (side + "_foot",))

# ---- arms
def _point_bone(pose, bone, rest_dir, world_dir):
    """Turns a limb bone (no twist) so its rest direction rest_dir (model space) points along world_dir."""
    R_p, _ = transform(PARENT[bone], pose)
    d = _norm(_mvec(_mT(R_p), world_dir)); v = _norm(rest_dir)
    a = math.asin(max(-1.0, min(1.0, d[2] / v[1]))) if abs(v[1]) > 1e-6 else 0.0
    c = math.atan2(d[1], d[0]) - math.atan2(v[1] * math.cos(a), v[0])
    pose[bone] = (-math.degrees(a), 0.0, -math.degrees(c))

def dangle(pose, side, swing=0.0, sway=0.0, fore=0.0, fore_sway=0.0, fist=0.0):
    """Hangs an arm by its own weight: the upper arm a little in front of straight down (the hunch), swung forward
    by `swing` and out by `sway` (fractions of its length); the forearm hanging from the elbow, slightly bent,
    flopping by `fore` / `fore_sway`; the fist flopping by `fist` degrees."""
    sx = -1 if side == "right" else 1
    arm, fore_b, hand = side + "_arm", side + "_forearm", side + "_hand"
    pose[arm] = pose[fore_b] = pose[hand] = (0, 0, 0)
    _point_bone(pose, arm, _sub(PIVOT[fore_b], PIVOT[arm]), (sx * (0.12 + sway), -1.0, -0.22 - swing))
    _point_bone(pose, fore_b, _sub(PIVOT[hand], PIVOT[fore_b]), (sx * (0.06 + sway + fore_sway), -1.0, -0.42 - swing - fore))
    pose[hand] = (10 + fist, 25 if side == "right" else -6, 0)          # (the rotted right one hangs twisted in)

# ---- the upper body
STANCE = {"hips": (6, 0, -4), "body": (24, 6, 6), "head": (-8, -10, 16), "jaw": (12, 0, -3)}

def upper(extra):
    """The stance with these rotations added (and positions set)."""
    pose = dict(STANCE)
    for bone, r in extra.items():
        if bone.startswith("pos:"):
            pose[bone] = r
        else:
            pose[bone] = tuple(pose.get(bone, (0, 0, 0))[i] + r[i] for i in range(3))
    return pose

def _keyed(keys, t):
    """Offsets at time t from [(time, {bone: rotation}), ...], eased between keys."""
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            u = _smooth((t - t0) / (t1 - t0))
            return {k: _lerp(a.get(k, (0, 0, 0)), b.get(k, (0, 0, 0)), u) for k in set(a) | set(b)}
    return keys[-1][1] if t > keys[-1][0] else keys[0][1]

HIP_DROP = -0.7                                         # the hips sit this much low: the knees never straighten
STAND = {"left": -0.9, "right": 1.0}                    # where the feet stand at rest (z): the left a little ahead

def idle(t):
    """Standing in its hunch, feet planted: swaying, slow shallow breaths, the head lolling, the jaw working, the
    arms swaying limply after the body, the fists swinging after them."""
    ph = 2 * math.pi * t / 5.0
    pose = upper({"pos:hips": (0, HIP_DROP + 0.12 * math.sin(2 * ph), 0),
                  "hips": (0, 0, 1.5 * math.sin(ph)), "body": (2.5 * math.sin(2 * ph), 0, 2 * math.sin(ph)),
                  "head": (2 * math.sin(ph + 1.0), 3 * math.sin(ph + 0.6), 6 * math.sin(ph + 1.2)),
                  "jaw": (5 * max(0.0, math.sin(2 * ph)), 0, 0)})
    for side in ("left", "right"):
        place_foot(pose, side, STAND[side])
    lag = 0.9
    dangle(pose, "right", swing=0.05 * math.sin(2 * ph - lag), sway=-0.06 * math.sin(ph - lag),
           fore=0.08 * math.sin(2 * ph - 2 * lag), fore_sway=-0.05 * math.sin(ph - 2 * lag), fist=10 * math.sin(2 * ph - 3 * lag))
    dangle(pose, "left", swing=0.05 * math.sin(2 * ph - lag + 0.7), sway=0.06 * math.sin(ph - lag),
           fore=0.08 * math.sin(2 * ph - 2 * lag + 0.7), fore_sway=0.05 * math.sin(ph - 2 * lag), fist=10 * math.sin(2 * ph - 3 * lag + 0.7))
    return pose

# The walk: one cycle is WALK_CYCLE seconds; each foot spends WALK_DUTY of it on the ground, moving back STRIDE px.
WALK_CYCLE, WALK_DUTY, STRIDE = 1.6, 0.62, 9.0
WALK_SPEED = STRIDE / 16.0 / (WALK_DUTY * WALK_CYCLE) / 20.0      # blocks per tick it covers at normal speed

def _step(phase, lift):
    """(z, lift, pitch, swinging) of a foot at this point of its cycle."""
    if phase < WALK_DUTY:                               # on the ground, flat, going back at a steady pace
        return -STRIDE / 2 + STRIDE * phase / WALK_DUTY, 0.0, 0.0, False
    u = (phase - WALK_DUTY) / (1 - WALK_DUTY)           # swinging forward to the next step
    return STRIDE / 2 - STRIDE * _smooth(u), lift * math.sin(math.pi * u), 26 * math.sin(math.pi * u) * (1 - u), True

def walk(t):
    """The trudge: heavy steps on the good left leg; the stiff right leg dragged forward after it, its toe scraping
    the ground; the body lurching down onto the right leg each time it takes the weight, rolling and twisting with
    the steps; the arms swinging loose and late, the forearms and fists flopping after them."""
    c = t / WALK_CYCLE
    ph = 2 * math.pi * c
    lz, llift, lpitch, lswing = _step(c % 1.0, 2.4)
    rz, rlift, rpitch, rswing = _step((c + 0.5) % 1.0, 0.0)
    limp = max(0.0, math.sin(ph + 0.6))                 # down onto the bad leg (it's under the body mid-cycle)
    pose = upper({"pos:hips": (0, HIP_DROP - 0.35 - 0.55 * limp + 0.25 * math.cos(2 * ph), 0),
                  "hips": (2 * math.cos(2 * ph), 6 * math.sin(ph), -5 * math.sin(ph) - 3 * limp),
                  "body": (3 * math.cos(2 * ph) + 2 * limp, -8 * math.sin(ph), 5 * math.sin(ph) + 3 * limp),
                  "head": (4 * math.sin(2 * ph + 0.5), 6 * math.sin(ph), -8 * math.sin(ph - 0.6) - 4 * limp),
                  "jaw": (5 * abs(math.sin(ph + 0.4)), 0, 0)})
    place_foot(pose, "left", lz, llift, lpitch)
    place_foot(pose, "right", rz, rlift, 20 * math.sin(math.pi * ((c + 0.5) % 1.0 - WALK_DUTY) / (1 - WALK_DUTY)) if rswing else 0.0,
               drag=rswing)
    lag = 1.1
    dangle(pose, "right", swing=0.3 * math.sin(ph - lag), sway=-0.14 * math.sin(ph - lag + 0.8) - 0.05,
           fore=0.28 * math.sin(ph - 2 * lag), fore_sway=-0.12 * math.sin(ph - 2 * lag), fist=26 * math.sin(ph - 3 * lag))
    dangle(pose, "left", swing=-0.32 * math.sin(ph - lag), sway=0.14 * math.sin(ph - lag + 0.8),
           fore=-0.3 * math.sin(ph - 2 * lag), fore_sway=0.12 * math.sin(ph - 2 * lag), fist=-26 * math.sin(ph - 3 * lag))
    return pose

def twitch(t):
    """A spasm (now and then while idle): the head snaps aside twice, a shoulder jerks up, the jaw clacks; the arms
    are flung by it and swing loose until they settle."""
    keys = [(0.0, {}), (0.06, {"head": (-6, 24, -26), "body": (0, 0, -5), "jaw": (-12, 0, 0)}),
            (0.16, {"head": (-2, 18, -20), "jaw": (6, 0, 0)}),
            (0.24, {"head": (-8, 26, -30), "body": (0, 0, -6), "jaw": (-12, 0, 0)}),
            (0.9, {})]
    pose = upper(dict(_keyed(keys, t), **{"pos:hips": (0, HIP_DROP, 0)}))
    for side in ("left", "right"):
        place_foot(pose, side, STAND[side])
    kick = math.exp(-4.0 * t) * math.sin(14.0 * t)      # flung, then swinging less and less
    kick2 = math.exp(-4.0 * t) * math.sin(14.0 * t - 1.2)
    dangle(pose, "right", swing=0.15 * kick, sway=-0.45 * kick, fore=0.3 * kick2, fore_sway=-0.4 * kick2, fist=40 * kick2)
    dangle(pose, "left", swing=-0.1 * kick, sway=0.25 * kick, fore=-0.2 * kick2, fore_sway=0.25 * kick2, fist=-30 * kick2)
    return pose

# ---- death: it falls apart where it hits the ground
# It drops to its knees, then tips forward from them, falling faster and faster. Its arms hang by their own weight:
# when a fist hits the ground the forearm breaks off at the elbow and flops flat; when an upper arm is pressed into
# the ground it breaks off at the shoulder. It lands on its face: the head snaps off and rolls away (and when it
# stops, the jaw comes loose); when the torso comes down it breaks away at the waist and settles face down; the legs
# straighten out flat on the ground behind it, the boots pointed (toes back). Each piece's moment of
# breaking is found by running the fall and seeing when it first touches the ground.
DEATH = 2.2
GRAVITY = 300.0                                         # px/s/s
LOWER = ("root", "hips", "right_leg", "left_leg", "right_shin", "left_shin", "right_foot", "left_foot")
KNEEL_END, PITCH_START, PITCH_ACCEL = 0.45, 0.36, 340.0  # tipping forward from the knees: degrees/s/s
HEAD_ROLL, HEAD_ROLL_TIME, HEAD_RADIUS = 300.0, 0.75, 2.0
SETTLE = 0.2                                            # how long a broken-off limb takes to flop flat
LEGS_DOWN = 0.25                                        # how long the legs take to go flat once the torso's down

def _axis_angle_rot(axis, deg):
    """The rotation matrix turning `deg` degrees about `axis`."""
    x, y, z = _norm(axis); a = math.radians(deg); c, s_ = math.cos(a), math.sin(a); C = 1 - c
    return [[c + x * x * C, x * y * C - z * s_, x * z * C + y * s_],
            [y * x * C + z * s_, c + y * y * C, y * z * C - x * s_],
            [z * x * C - y * s_, z * y * C + x * s_, c + z * z * C]]

def _axis_angle(m):
    """(axis, degrees) of a rotation matrix."""
    angle = math.acos(max(-1.0, min(1.0, (m[0][0] + m[1][1] + m[2][2] - 1) / 2)))
    if angle < 1e-6:
        return (1.0, 0.0, 0.0), 0.0
    axis = (m[2][1] - m[1][2], m[0][2] - m[2][0], m[1][0] - m[0][1])
    return _norm(axis), math.degrees(angle)

def _cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

def _kneel_legs(pose, u):
    """The legs from standing (u 0) to kneeling (u 1): knees on the ground, shins flat behind, toes down."""
    pose["right_leg"] = _lerp((-14, 0, 0), (-6, 0, 0), u)
    pose["left_leg"] = _lerp((-8, 0, 0), (-2, 0, 0), u)
    pose["right_shin"] = _lerp((18, 0, 0), (100, 0, 0), u)
    pose["left_shin"] = _lerp((12, 0, 0), (104, 0, 0), u)
    pose["right_foot"] = _lerp((-4, 0, 0), (32, 0, 0), u)
    pose["left_foot"] = _lerp((-4, 0, 0), (36, 0, 0), u)

def _kneeling(t):
    """Dropping to its knees: they buckle and give (slowly at first) and it lands on them."""
    pose = upper({})
    kneel = min(1.0, t / KNEEL_END)
    u = kneel * kneel * (3 - 2 * kneel)
    _kneel_legs(pose, u)
    pose["hips"] = _lerp(STANCE["hips"], (-6, 0, -3), u)
    pose["body"] = _addv(STANCE["body"], (10 * u, 0, 0))
    pose["head"] = _addv(STANCE["head"], (14 * u, 0, -6 * u))
    pose["jaw"] = _addv(STANCE["jaw"], (8 * u, 0, 0))
    pose["pos:hips"] = (0, 0, 0)
    pose["pos:root"] = (0, -lowest(pose, LOWER), 0)
    return pose

def _fall_forward(pose, deg):
    """Tips everything above the knees forward about them by `deg` (the shins stay where they are on the ground)."""
    knees = [world(PIVOT[s_ + "_shin"], s_ + "_leg", pose) for s_ in ("right", "left")]
    knee = _mul(_addv(knees[0], knees[1]), 0.5)
    shins = {s_: transform(s_ + "_shin", pose)[0] for s_ in ("right", "left")}
    pose["hips"] = _addv(pose["hips"], (deg, 0, 0))
    for _ in range(3):                                  # the knees back where they were
        now = [world(PIVOT[s_ + "_shin"], s_ + "_leg", pose) for s_ in ("right", "left")]
        miss = _sub(knee, _mul(_addv(now[0], now[1]), 0.5))
        R_r, _ = transform("root", pose)
        pose["pos:hips"] = _addv(pose.get("pos:hips", (0, 0, 0)), _mvec(_mT(R_r), miss))
    for s_ in ("right", "left"):                        # the shins kept as they were
        R_leg, _ = transform(s_ + "_leg", pose)
        pose[s_ + "_shin"] = _euler(_mmul(_mT(R_leg), shins[s_]))

_lower_cap = None

def _fall_angle(t):
    """How far it has tipped forward from its knees at time t: faster and faster, until its hips and thighs are
    down on the ground."""
    global _lower_cap
    if _lower_cap is None:
        base, deg = _kneeling(KNEEL_END), 0.0
        while deg < 120.0:
            trial = dict(base); _fall_forward(trial, deg + 1.0)
            if lowest(trial, ("hips", "right_leg", "left_leg")) <= 0.0:
                break
            deg += 1.0
        _lower_cap = deg
    return min(_lower_cap, PITCH_ACCEL * max(0.0, t - PITCH_START) ** 2 / 2)

def _attached(t):
    """The fall with nothing broken off: on its knees, then tipping forward from them, the arms hanging by their own
    weight."""
    pose = _kneeling(t)
    _fall_forward(pose, _fall_angle(t))
    low = lowest(pose, LOWER)
    if low < 0.0:                                       # (never into the ground)
        pose["pos:root"] = _addv(pose["pos:root"], (0, -low, 0))
    u = _smooth(t / KNEEL_END)
    for side in ("right", "left"):
        dangle(pose, side, swing=0.15 * u, sway=0.04)
    return pose

def _first_contact(group, after, until):
    """When this group of bones first touches the ground in the fall (or `until`)."""
    t = after
    while t < until:
        if lowest(_attached(t), group) <= 0.0:
            return round(t, 3)
        t += 0.005
    return until

_contacts = None

def _contact_times():
    """When each piece breaks off: the head when it (or the torso) first hits the ground; the torso when it does; each forearm when its
    fist hits; each upper arm when it hits (or with the torso, if it hasn't yet); the jaw when the head stops."""
    global _contacts
    if _contacts is None:
        face = _first_contact(("body", "head", "jaw"), PITCH_START, 2.5)     # it lands on its face: the head goes
        torso = _first_contact(("body",), face, 2.5)
        c = {"body": torso, "head": face}
        for side in ("right", "left"):
            fore = _first_contact((side + "_forearm", side + "_hand"), PITCH_START, torso)
            c[side + "_forearm"] = fore
            c[side + "_arm"] = _first_contact((side + "_arm",), fore, torso)
        c["jaw"] = face + HEAD_ROLL_TIME
        _contacts = c
    return _contacts

# Pieces in the order they're placed (each after the bone it hangs from), and the bones that move with each.
PIECES = [("body", ("body",)), ("right_arm", ("right_arm",)), ("left_arm", ("left_arm",)),
          ("right_forearm", ("right_forearm", "right_hand")), ("left_forearm", ("left_forearm", "left_hand")),
          ("head", ("head", "jaw")), ("jaw", ("jaw",))]

def _legs_down(pose, dt):
    """Once the torso is down: the legs going flat on the ground behind it, the knees straightening and the boots
    pointing (toes back), the hips turning so the thighs lie along the ground; faster as they drop."""
    u = min(1.0, dt / LEGS_DOWN) ** 2
    for side, leg, shin, foot in (("right", -4, 12, -70), ("left", -2, 8, -72)):
        pose[side + "_leg"] = _lerp(pose[side + "_leg"], (leg, 0, 0), u)
        pose[side + "_shin"] = _lerp(pose[side + "_shin"], (shin, 0, 0), u)
        pose[side + "_foot"] = _lerp(pose[side + "_foot"], (foot, 0, 0), u)
    R0, t0 = transform("hips", pose)
    pv = PIVOT["hips"]
    p0 = _addv(_mvec(R0, pv), t0)
    along = _norm(_sub(world(PIVOT["right_shin"], "right_leg", pose), world(PIVOT["right_leg"], "hips", pose)))
    flat = _norm((along[0], 0.0, along[2])) if abs(along[1]) < 0.999 else (0.0, 0.0, 1.0)
    axis = _cross(along, flat)
    ang = math.degrees(math.acos(max(-1.0, min(1.0, _dot(along, flat)))))
    R_w = _mmul(_axis_angle_rot(axis, ang * u), R0) if _dot(axis, axis) > 1e-9 else R0
    R_p, t_p = transform("root", pose)
    R_l = _mmul(_mT(R_p), R_w)
    pose["hips"] = _euler(R_l)
    pose["pos:hips"] = _addv(_sub(_mvec(_mT(R_p), _sub(_sub(p0, _mvec(R_w, pv)), t_p)), pv), _mvec(R_l, pv))
    low = lowest(pose, LOWER)                           # lying on the ground
    pose["pos:root"] = _addv(pose["pos:root"], (0, -low, 0))

_death_cache = {}

def death(t):
    t = round(t, 4)
    if t in _death_cache:
        return dict(_death_cache[t])
    contacts = _contact_times()
    torso = contacts["body"]
    pose = _attached(min(t, torso))
    if t > torso:
        _legs_down(pose, t - torso)
    _fall_apart(pose, t, contacts, death)
    _death_cache[t] = dict(pose)
    return pose

def _fall_apart(pose, t, contacts, at):
    """Every piece that has broken off by time t (contacts: when each does), placed: falling from where it broke
    off (as `at(that time)` had it), then flopping flat or (the head) rolling away; never into the ground."""
    for bone, group in PIECES:
        start = contacts[bone]
        if t <= start:
            continue
        before = at(start)                              # how the piece was the moment it broke off
        R0, t0 = transform(bone, before)
        pv = PIVOT[bone]
        p0 = _addv(_mvec(R0, pv), t0)                   # its pivot, in model space
        dt = t - start
        if bone == "head":                              # rolls away forward, slowing, a little to one side
            u = min(1.0, dt / HEAD_ROLL_TIME)
            dist = HEAD_ROLL * (1 - (1 - u) ** 2)
            R_w = _mmul(_axis_angle_rot((1, 0, 0.12), -dist), R0)
            run = math.radians(dist) * HEAD_RADIUS
            p = (p0[0] + 0.12 * run, p0[1], p0[2] - run)
        else:                                           # flops down flat where it is
            if bone == "body":
                down = _mvec(R0, (0, 0, -1))           # face down: its front turns to the ground
                axis = _cross(down, (0, -1, 0))
                ang = math.degrees(math.acos(max(-1.0, min(1.0, _dot(_norm(down), (0, -1, 0))))))
            else:                                       # a limb: lies along the ground
                along = _norm(_mvec(R0, _sub(PIVOT[group[1]] if len(group) > 1 else (PIVOT[bone][0], PIVOT[bone][1] - 5.0, PIVOT[bone][2]), pv)))
                flat = _norm((along[0], 0.0, along[2])) if abs(along[1]) < 0.999 else (0.0, 0.0, -1.0)
                axis = _cross(along, flat)
                ang = math.degrees(math.acos(max(-1.0, min(1.0, _dot(along, flat)))))
            u = min(1.0, dt / SETTLE)
            R_w = _mmul(_axis_angle_rot(axis, ang * u * u), R0) if _dot(axis, axis) > 1e-9 else R0
            p = p0
        # its height: falling (from where it broke off), but never into the ground
        low = 1e9
        still_on = [b for b in group if b == bone or not (b in contacts and t > contacts[b])]
        for name, b, a, bb, mat in CUBES:
            if b in still_on:                           # (not counting a part that has come off it since)
                Rb, tb = transform(b, before)
                for c in _corners(a, bb):
                    rel = _mvec(_mT(R0), _sub(_addv(_mvec(Rb, c), tb), p0))
                    low = min(low, _mvec(R_w, rel)[1])
        y = max(p0[1] - GRAVITY * dt * dt / 2, -low)
        p = (p[0], y, p[2])
        t_w = _sub(p, _mvec(R_w, pv))
        R_p, t_p = transform(PARENT[bone], pose)        # (the bone it hung from, wherever that is now)
        R_l = _mmul(_mT(R_p), R_w)
        pose[bone] = _euler(R_l)
        pose["pos:" + bone] = _addv(_sub(_mvec(_mT(R_p), _sub(t_w, t_p)), pv), _mvec(R_l, pv))
        for child in group[1:]:                         # the rest of the piece stays as it was when it broke off
            if not (child in contacts and t > contacts[child]):
                pose[child] = before.get(child, (0, 0, 0))
                pose.pop("pos:" + child, None)
                if "pos:" + child in before:
                    pose["pos:" + child] = before["pos:" + child]

# ---- poses blended, kept on the ground
def blend(a, b, u):
    """Between two poses (rotations and positions), u from 0 (a) to 1 (b)."""
    return {k: _lerp(a.get(k, (0, 0, 0)), b.get(k, (0, 0, 0)), u) for k in set(a) | set(b)}

def on_ground(pose):
    """Raised or lowered so its lowest point is on the ground."""
    r = pose.get("pos:root", (0, 0, 0))
    pose["pos:root"] = (r[0], r[1] - lowest(pose), r[2])
    return pose

# ---- shot dead with a Dust Ball: it shakes violently, then falls apart
SHAKE = 1.0

def death_dust(t):
    """Seized by the Dust: it stands shaking violently, the head jerking, the jaw chattering, the arms flailing,
    for a second; then it falls apart as in `death`."""
    if t >= SHAKE:
        return death(t - SHAKE)
    k = math.sin(math.pi * min(1.0, t / 0.12)) if t < 0.12 else 1.0           # (it seizes up at once)
    j = lambda f, ph, amp: amp * k * math.sin(f * t + ph)
    pose = upper({"pos:hips": (0, HIP_DROP + j(41, 0.3, 0.25), 0),
                  "hips": (j(37, 0, 3), j(29, 1, 4), j(43, 2, 4)),
                  "body": (j(31, 0.5, 8), j(47, 1.3, 9), j(39, 2.1, 9)),
                  "head": (j(53, 0.2, 16), j(44, 2.5, 22), j(58, 1.1, 18)),
                  "jaw": (abs(j(61, 0, 22)), 0, 0)})
    for side in ("left", "right"):
        place_foot(pose, side, STAND[side])
    dangle(pose, "right", swing=j(33, 0, 0.5), sway=-abs(j(27, 1, 0.5)), fore=j(49, 2, 0.6), fore_sway=j(38, 0, 0.4), fist=j(57, 1, 40))
    dangle(pose, "left", swing=j(35, 2, 0.5), sway=abs(j(31, 0, 0.5)), fore=j(46, 1, 0.6), fore_sway=j(42, 2, 0.4), fist=j(51, 0, 40))
    if t > SHAKE - 0.15:                                # (into the start of the fall)
        pose = on_ground(blend(pose, death(0.0), (t - (SHAKE - 0.15)) / 0.15))
    return pose

# ---- hit with salt: it curls up on the ground, hands over its head, then slowly gets up again
FETAL = {"root": (0, 0, 88), "hips": (0, 0, 0), "body": (56, 0, 0), "head": (42, 0, 8), "jaw": (6, 0, 0),
         "right_leg": (-124, 0, 0), "left_leg": (-116, 0, 6), "right_shin": (158, 0, 0), "left_shin": (154, 0, 0),
         "right_foot": (10, 0, 0), "left_foot": (14, 0, 0)}

def _arms_over_head(pose):
    """Its arms wrapped round its head: each elbow by its face, the forearm over the back of the head, the fist
    clenched there."""
    for side, sx in (("right", -1), ("left", 1)):
        arm, fore = side + "_arm", side + "_forearm"
        pose[arm] = pose[fore] = (0, 0, 0)
        elbow = world((sx * 2.6, 28.6, -2.6), "head", pose)          # beside the face
        back = world((sx * 0.7, 31.2, 2.0), "head", pose)            # the back of the head
        shoulder = world(PIVOT[arm], "body", pose)
        _point_bone(pose, arm, _sub(PIVOT[fore], PIVOT[arm]), _sub(elbow, shoulder))
        now = world(PIVOT[fore], arm, pose)
        _point_bone(pose, fore, _sub(PIVOT[side + "_hand"], PIVOT[fore]), _sub(back, now))
        pose[side + "_hand"] = (40, sx * 20, 0)

def fetal(t=0.0):
    """Curled up on its side on the ground: knees to its chest, its fists over its head; trembling, breathing fast."""
    pose = dict(FETAL)
    shiver = lambda f, ph, amp: amp * math.sin(2 * math.pi * f * t + ph)
    _arms_over_head(pose)
    for k, (bone, amp) in enumerate((("body", 2.5), ("head", 3.0), ("right_arm", 2.0), ("left_arm", 2.0), ("right_leg", 1.5), ("left_leg", 1.5))):
        pose[bone] = _addv(pose[bone], (shiver(7.5, k * 1.7, amp), 0, shiver(5.0, k * 2.3, amp * 0.5)))
    pose["body"] = _addv(pose["body"], (2 * math.sin(2 * math.pi * t), 0, 0))       # (a fast, shallow breath)
    pose["pos:root"] = (0, 0, 0)
    hips = world(PIVOT["hips"], "hips", pose)            # lying where it stood
    pose["pos:root"] = (-hips[0], 0, -hips[2] * 0.5)
    return on_ground(pose)

# Killed while curled up, it falls apart where it lies: the head comes away first and rolls off, then the forearms,
# the upper arms, the torso (rolling onto its front), the jaw from the head when it stops; the legs go slack.
CURLED_APART = {"head": 0.12, "right_forearm": 0.2, "left_forearm": 0.3, "right_arm": 0.38, "left_arm": 0.46,
                "body": 0.55}
CURLED_APART["jaw"] = CURLED_APART["head"] + HEAD_ROLL_TIME
CURLED_DEATH = 1.4
_curled_cache = {}

def death_curled(t):
    t = round(t, 4)
    if t in _curled_cache:
        return dict(_curled_cache[t])
    start = fetal(0.0)
    pose = dict(start)
    slack = _smooth(t / 0.8)
    for side, leg, shin in (("right", -80, 110), ("left", -74, 104)):
        pose[side + "_leg"] = _lerp(start[side + "_leg"], (leg, 0, 0), slack)
        pose[side + "_shin"] = _lerp(start[side + "_shin"], (shin, 0, 0), slack)
    off = set()                                         # (whatever's still joined stays on the ground)
    for bone, group in PIECES:
        if t > CURLED_APART[bone]:
            off.update(group)
    r = pose["pos:root"]
    pose["pos:root"] = (r[0], r[1] - lowest(pose, [b for b in PIVOT if b not in off]), r[2])
    _fall_apart(pose, t, CURLED_APART, death_curled)
    _curled_cache[t] = dict(pose)
    return pose

def curl(t):
    """Dropping to the ground and curling up (0.7 s)."""
    u = min(1.0, t / 0.7)
    return on_ground(blend(idle(0.0), fetal(0.0), u * u * (3 - 2 * u)))

def curled(t):
    return fetal(t)

def uncurl(t):
    """Slowly getting back up (3 s): uncurling onto its knees, hunched over, then rising to its feet."""
    kneel = _kneeling(KNEEL_END)
    for side in ("right", "left"):
        dangle(kneel, side, swing=0.25, sway=0.05)
    kneel["body"] = _addv(kneel["body"], (12, 0, 0))
    if t < 1.6:
        return on_ground(blend(fetal(0.0), kneel, _smooth(t / 1.6)))
    return on_ground(blend(kneel, idle(0.0), _smooth((t - 1.6) / 1.4)))

ANIMATIONS = [("idle", True, 5.0, idle), ("walk", True, WALK_CYCLE, walk),
              ("twitch", "hold_on_last_frame", 0.9, twitch), ("death", "hold_on_last_frame", DEATH, death),
              ("death_dust", "hold_on_last_frame", SHAKE + DEATH, death_dust),
              ("death_curled", "hold_on_last_frame", CURLED_DEATH, death_curled),
              ("curl", "hold_on_last_frame", 0.7, curl), ("curled", True, 2.0, curled), ("uncurl", "hold_on_last_frame", 3.0, uncurl)]

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
        steps = max(2, round(length * 20))
        frames = [(round(k * length / steps, 4), fn(round(k * length / steps, 4))) for k in range(steps + 1)]
        keys = set().union(*(f.keys() for _, f in frames))      # every bone keyed in every frame
        bones, last = {}, {}
        for t, pose in frames:
            for key in sorted(keys):
                v = pose.get(key, (0, 0, 0))
                if key.startswith("pos:"):
                    bones.setdefault(key[4:], {}).setdefault("position", {})[str(t)] = [round(c, 3) for c in v]
                else:
                    v = _continuous(last.get(key), v); last[key] = v
                    bones.setdefault(key, {}).setdefault("rotation", {})[str(t)] = [round(c, 3) for c in v]
        out[name] = {"loop": loop, "animation_length": length, "bones": bones}
    return out

# ---------------------------------------------------------------- texture layout and painting
def face_size(a, b, f):
    w, h, d = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    return {"nz": (w, h), "pz": (w, h), "nx": (d, h), "px": (d, h), "py": (w, d), "ny": (w, d)}[f]

def face_point(a, b, f, s, t):
    """The point on the model at s, t of a face, the way GeckoLib lays the face's texture on it (s from the texture
    region's left, t from its top)."""
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
                if f in ("py", "ny"):                     # Bedrock draws these flipped
                    uv[JSON_FACE[f]] = {"uv": [round(u + w, 4), round(v + h, 4)], "uv_size": [round(-w, 4), round(-h, 4)]}
                else:
                    uv[JSON_FACE[f]] = {"uv": [round(u, 4), round(v, 4)], "uv_size": [round(w, 4), round(h, 4)]}
            cubes.append({"origin": [round(c, 4) for c in a], "size": [round(bb[k] - a[k], 4) for k in range(3)], "uv": uv})
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.hollow", "texture_width": TEX_W // TEXELS, "texture_height": TEX_H // TEXELS,
                        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.2, 0]},
        "bones": bones}]}

def bbmodel(geometry, texture_png, anims):
    """The model as a Blockbench project (Bedrock format, per-face UV) with the texture built in. Blockbench's space
    is the game's, which mirrors Bedrock's X: cube and pivot X flip."""
    desc = geometry["minecraft:geometry"][0]
    ids = {b["name"]: str(uuid.uuid5(uuid.NAMESPACE_URL, "hollow/" + b["name"])) for b in desc["bones"]}
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
            eid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"hollow/{b['name']}/{i}"))
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
    texture = {"name": "hollow.png", "folder": "entity", "namespace": "horrormod", "id": "0",
               "width": TEX_W, "height": TEX_H, "uv_width": TEX_W // TEXELS, "uv_height": TEX_H // TEXELS,
               "particle": False, "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True,
               "saved": True, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "hollow.png/" + hashlib.sha1(texture_png).hexdigest())),
               "source": "data:image/png;base64," + base64.b64encode(texture_png).decode()}
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False},
            "name": "hollow", "model_identifier": "geometry.hollow", "visible_box": [3, 3, 1.2],
            "resolution": {"width": TEX_W // TEXELS, "height": TEX_H // TEXELS},
            "elements": elements, "outliner": [group("root")], "textures": [texture],
            "animations": [{"uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "hollow/anim/" + name)), "name": name,
                            "loop": "loop" if a["loop"] is True else "hold", "override": False, "length": a["animation_length"],
                            "snapping": 20, "selected": False,
                            "animators": {ids[bone]: {"name": bone, "type": "bone", "keyframes": [
                                {"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "time": float(t),
                                 "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"hollow/{name}/{bone}/{ch}/{t}")),
                                 "color": -1, "interpolation": "linear"}
                                for ch in ("rotation", "position") for t, v in chans.get(ch, {}).items()]}
                                for bone, chans in a["bones"].items()}}
                           for name, a in anims.items()]}

# ---------------------------------------------------------------- preview (textured, from four sides)
NORMAL = {"nz": (0, 0, -1), "pz": (0, 0, 1), "nx": (-1, 0, 0), "px": (1, 0, 0), "py": (0, 1, 0), "ny": (0, -1, 0)}

def preview(path, head=False, pose=None, label=""):
    """Four views, whole (head=False) or a close-up of the head; posed if a pose is given."""
    views = [("front", 0), ("three-quarter", -35), ("side", -90), ("back", 180)]
    W, H, S, CELL = (300, 560, 15.5, 0.25) if not head else (300, 330, 40.0, 0.08)
    lift = 0.0 if not head else 25.0
    sheet = Image.new("RGB", (W * len(views), H), (24, 24, 30))
    for k, (title, yaw) in enumerate(views):
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        def view(p):
            x, y, z = p
            return (x * cy - z * sy, y, x * sy + z * cy)
        polys = []
        for i, (name, bone, a, b, mat) in enumerate(CUBES):
            R, tr = transform(bone, pose) if pose is not None else ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0))
            place = lambda q: _addv(_mvec(R, q), tr)
            for f in FACES:
                n = view(_mvec(R, NORMAL[f]))
                if n[2] > 0.01:                                      # facing away (the camera looks along +z)
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
            d.polygon([(W / 2 + p[0] * S, H - 30 - (p[1] - lift) * S) for p in quad], fill=c)
        d.line([(0, H - 30), (W, H - 30)], fill=(50, 50, 60))
        if not head:
            d.line([(0, H - 30 - 28.8 * S), (W, H - 30 - 28.8 * S)], fill=(80, 40, 40))
            d.text((6, H - 30 - 28.8 * S - 14), "player height", fill=(140, 80, 80))
        d.text((6, 6), title + (" " + label if label else ""), fill=(170, 170, 190))
        sheet.paste(img, (k * W, 0))
    sheet.save(path)

if __name__ == "__main__":
    rects = layout()
    tex = paint(rects)
    tex_path = os.path.join(ASSETS, "textures", "entity", "hollow.png")
    tex.save(tex_path)
    geometry = geo(rects)
    os.makedirs(os.path.join(ASSETS, "geo", "entity"), exist_ok=True)
    json.dump(geometry, open(os.path.join(ASSETS, "geo", "entity", "hollow.geo.json"), "w"), indent=1)
    anims = animations()
    os.makedirs(os.path.join(ASSETS, "animations", "entity"), exist_ok=True)
    json.dump({"format_version": "1.8.0", "animations": anims},
              open(os.path.join(ASSETS, "animations", "entity", "hollow.animation.json"), "w"), separators=(",", ":"))
    json.dump(bbmodel(geometry, open(tex_path, "rb").read(), anims), open(BBMODEL, "w"), indent=1)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
    if len(sys.argv) > 2:
        preview(sys.argv[2], head=True)
    top = max(c[3][1] for c in CUBES)
    print(f"wrote hollow geo, texture and {BBMODEL}; {len(CUBES)} cubes, {top / 16:.2f} blocks tall")
