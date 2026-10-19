"""Odie, the Hat Man: builds his model, texture and animations for GeckoLib, writing

    src/main/resources/assets/horrormod/geo/entity/odie.geo.json
    src/main/resources/assets/horrormod/animations/entity/odie.animation.json
    src/main/resources/assets/horrormod/textures/entity/odie.png
    art/odie.bbmodel                 the same model, texture and animations as one Blockbench file, to look at him
                                     (File > Open Model); changes made there are overwritten on the next run
    <preview dir>/odie_preview.png   front and side views of key moments (for checking poses)

Run: python3 art/tools/odie_model.py [preview dir]   (needs Pillow; overwrites those files)
Edits made in Blockbench are overwritten the next time this runs: change the rig, colours and poses here instead.

A tall, thin man in a long dark overcoat, dark trousers, a pale striped shirt and a dark striped tie, his face lost
in shadow under a wide-brimmed hat (after the reference photo of "the Hat Man").

Animations
    idle         standing still and watching: slow breaths, the faintest sway, the head drifting a little (loop)
    wave         takes off his hat, holds it to his chest, waves slowly with his other hand, then puts the hat back on
    turn_around  turning round to walk away: head first, shoulders following, the feet shuffling round (the game turns
                 him the half circle over the same 1.4 seconds)
    walk_away    a slow, even walk, the hat back on his head, the arms swinging loosely (loop)

Coordinates are Bedrock's, as GeckoLib loads them: pixels, y up, his front faces -z, +x is his left. A bone turns
by R = Rz(-rz) * Ry(ry) * Rx(-rx) about its pivot (a negative x rotation swings a hanging arm forward; a positive z
swings his right arm out from his side, a negative z his left). The hat is its
own bone under the head, so it can never shift on his head; while it's in his right hand it's placed, every frame,
where that hand holds it.
"""
import base64, hashlib, json, math, os, sys, uuid
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
BBMODEL = os.path.normpath(os.path.join(HERE, "..", "odie.bbmodel"))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod"))
PREVIEW = sys.argv[1] if len(sys.argv) > 1 else "/tmp"

# ---------------------------------------------------------------- rig: bones (name, parent, pivot)
BONES = [
    ("root", None, (0, 0, 0)),
    ("body", "root", (0, 15, 0)),
    ("chest", "body", (0, 22, 0)),
    ("head", "chest", (0, 30, 0)),
    ("right_arm", "chest", (-6.1, 28.4, 0)),                 # shoulder, elbow and wrist in a straight line
    ("right_forearm", "right_arm", (-6.1, 21.4, 0)),
    ("right_hand", "right_forearm", (-6.1, 14.4, 0)),
    ("right_fingers", "right_hand", (-6.1, 11.6, 0)),
    ("left_arm", "chest", (6.1, 28.4, 0)),
    ("left_forearm", "left_arm", (6.1, 21.4, 0)),
    ("left_hand", "left_forearm", (6.1, 14.4, 0)),
    ("left_fingers", "left_hand", (6.1, 11.6, 0)),
    ("right_leg", "root", (-2.0, 15, 0)),
    ("right_shin", "right_leg", (-2.0, 8, 0)),
    ("right_foot", "right_shin", (-2.0, 1.8, 0)),
    ("left_leg", "root", (2.0, 15, 0)),
    ("left_shin", "left_leg", (2.0, 8, 0)),
    ("left_foot", "left_shin", (2.0, 1.8, 0)),
    ("hat", "head", (0, 37.4, 0)),
]
PARENT = {n: p for n, p, _ in BONES}
PIVOT = {n: v for n, _, v in BONES}

# ---------------------------------------------------------------- rig: cubes (name, bone, from, to, material)
# Rounded by stacking slightly smaller cubes on the big ones (the edges step in), so nothing reads as a plain box.
CUBES = []
def cube(name, bone, a, b, mat):
    CUBES.append((name, bone, tuple(a), tuple(b), mat))

def pair(name, bone, a, b, mat):
    """A cube on his right (-x) and its mirror on his left."""
    cube("right_" + name, "right_" + bone, a, b, mat)
    cube("left_" + name, "left_" + bone, (-b[0], a[1], a[2]), (-a[0], b[1], b[2]), mat)

# legs: trousers narrowing a little toward the ankle
pair("thigh", "leg", (-3.7, 8, -1.6), (-0.3, 15, 1.6), "trousers")
pair("thigh_round", "leg", (-3.9, 9, -1.3), (-0.1, 14.6, 1.3), "trousers")
pair("shin", "shin", (-3.5, 1.6, -1.4), (-0.5, 8, 1.4), "trousers")
pair("shin_round", "shin", (-3.7, 2.4, -1.15), (-0.3, 7.6, 1.15), "trousers")
# shoes: a dark sole and heel, the upper, and a toe that rounds off in steps
pair("sole", "foot", (-3.7, 0, -3.9), (-0.3, 0.4, 1.7), "sole")
pair("heel", "foot", (-3.5, 0.4, 0.5), (-0.5, 1.0, 1.7), "sole")
pair("shoe", "foot", (-3.6, 0.4, -2.3), (-0.4, 2.0, 1.6), "shoe")
pair("toe", "foot", (-3.4, 0.4, -3.4), (-0.6, 1.5, -2.3), "shoe")
pair("toe_tip", "foot", (-3.1, 0.4, -3.85), (-0.9, 1.1, -3.4), "shoe")
# the overcoat: flaring a little to the knees, fitted at the waist, shoulders sloping, collar turned up
cube("coat_hem", "body", (-4.75, 8.4, -2.9), (4.75, 11.5, 2.9), "coat")
cube("coat_skirt", "body", (-4.55, 11.5, -2.7), (4.55, 15, 2.7), "coat")
cube("coat_waist", "body", (-4.3, 15, -2.5), (4.3, 22, 2.5), "coat")
cube("coat_waist_round", "body", (-4.5, 15.5, -2.2), (4.5, 21.5, 2.2), "coat")
cube("coat_chest", "chest", (-4.6, 22, -2.6), (4.6, 28.2, 2.6), "coat")
cube("coat_chest_round", "chest", (-4.8, 22.6, -2.3), (4.8, 27.8, 2.3), "coat")
cube("coat_shoulders", "chest", (-4.2, 28.2, -2.3), (4.2, 29.0, 2.3), "coat")
cube("coat_collar", "chest", (-2.3, 28.6, -1.0), (2.3, 30.6, 2.1), "coat")
# shirt and tie showing in the V of the coat, the lapels either side
cube("shirt", "chest", (-1.4, 23.6, -2.8), (1.4, 29.0, -2.6), "shirt")
cube("tie", "chest", (-0.5, 21.8, -2.9), (0.5, 28.0, -2.78), "tie")
cube("tie_knot", "chest", (-0.65, 27.6, -2.95), (0.65, 28.6, -2.78), "tie")
cube("right_lapel", "chest", (-2.6, 22.6, -2.85), (-1.0, 28.8, -2.62), "lapel")
cube("left_lapel", "chest", (1.0, 22.6, -2.85), (2.6, 28.8, -2.62), "lapel")
# head: a long, narrow face in shadow, rounded at the crown, the jaw and the back
cube("neck", "chest", (-1.15, 29.4, -1.2), (1.15, 31.4, 1.2), "skin")
cube("head", "head", (-2.7, 31.4, -2.9), (2.7, 37.2, 2.6), "face")
cube("head_crown", "head", (-2.3, 37.2, -2.5), (2.3, 37.9, 2.2), "skin")
cube("head_sides", "head", (-2.95, 32.2, -2.4), (2.95, 36.8, 2.1), "skin")
cube("head_back", "head", (-2.3, 32.0, 2.6), (2.3, 36.6, 3.0), "skin")
cube("jaw", "head", (-2.2, 30.4, -2.7), (2.2, 31.4, 1.6), "skin")
cube("chin", "head", (-1.2, 30.2, -2.95), (1.2, 31.0, -2.2), "skin")
cube("nose", "head", (-0.45, 32.9, -3.45), (0.45, 34.9, -2.9), "skin")
cube("brow", "head", (-2.4, 35.1, -3.15), (2.4, 35.7, -2.9), "skin")
cube("right_ear", "head", (-3.3, 33.2, -0.5), (-2.95, 35.2, 0.7), "skin")
cube("left_ear", "head", (2.95, 33.2, -0.5), (3.3, 35.2, 0.7), "skin")
# arms: rounded shoulders, sleeves tapering to the wrist, white cuffs (all clear of the coat, which reaches x 4.8)
pair("sleeve", "arm", (-7.4, 21.2, -1.45), (-4.9, 28.4, 1.45), "coat")
pair("shoulder", "arm", (-7.5, 26.2, -1.65), (-4.5, 28.9, 1.65), "coat")
pair("shoulder_top", "arm", (-7.1, 28.9, -1.3), (-4.4, 29.4, 1.3), "coat")
pair("forearm", "forearm", (-7.4, 14.6, -1.3), (-5.0, 21.6, 1.3), "coat")
pair("cuff", "forearm", (-7.2, 14.0, -1.15), (-5.2, 14.8, 1.15), "shirt_plain")
# hands: a palm with a knuckle ridge, four fingers (their own bone, so they can curl round the hat brim) and a thumb
pair("palm", "hand", (-6.9, 11.6, -1.25), (-5.7, 14.2, 1.25), "hand")
pair("knuckles", "hand", (-7.05, 11.4, -1.3), (-5.55, 12.2, 1.3), "hand")
pair("thumb", "hand", (-6.4, 11.7, -1.75), (-5.6, 13.6, -1.15), "hand")
for i, z in enumerate((-1.2, -0.55, 0.1, 0.75)):
    length = (2.6, 2.9, 2.8, 2.3)[i]
    pair(f"finger{i + 1}", "fingers", (-6.8, 11.6 - length, z), (-5.8, 11.6, z + 0.5), "hand")
# the hat: a fedora, the brim just wider than his head, the crown pinched in at the top
cube("hat_brim", "hat", (-4.6, 37.0, -4.8), (4.6, 37.5, 4.4), "hat")
cube("hat_crown", "hat", (-2.9, 37.5, -3.0), (2.9, 40.8, 2.7), "hat")
cube("hat_top", "hat", (-2.5, 40.8, -2.4), (2.5, 41.4, 2.1), "hat")
cube("hat_band", "hat", (-3.0, 37.5, -3.1), (3.0, 38.4, 2.8), "hat_band")

# ---------------------------------------------------------------- maths
def mmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def mvec(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))
def mT(m):
    return [[m[j][i] for j in range(3)] for i in range(3)]
def rx(a):
    c, s = math.cos(a), math.sin(a); return [[1, 0, 0], [0, c, -s], [0, s, c]]
def ry(a):
    c, s = math.cos(a), math.sin(a); return [[c, 0, s], [0, 1, 0], [-s, 0, c]]
def rz(a):
    c, s = math.cos(a), math.sin(a); return [[c, -s, 0], [s, c, 0], [0, 0, 1]]
def rot(r):
    """A bone's rotation matrix from its Bedrock rotation (degrees)."""
    x, y, z = (math.radians(v) for v in r)
    return mmul(rz(-z), mmul(ry(y), rx(-x)))
def euler(m):
    """Bedrock rotation (degrees) for a rotation matrix (the inverse of rot)."""
    b = math.asin(max(-1.0, min(1.0, -m[2][0])))
    a = math.atan2(m[2][1], m[2][2]); c = math.atan2(m[1][0], m[0][0])
    return (math.degrees(-a), math.degrees(b), math.degrees(-c))
def sub(a, b): return tuple(a[i] - b[i] for i in range(3))
def add(a, b): return tuple(a[i] + b[i] for i in range(3))
def mul(a, k): return tuple(v * k for v in a)
def dot(a, b): return sum(a[i] * b[i] for i in range(3))
def cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def norm(a):
    l = math.sqrt(dot(a, a)) or 1.0; return mul(a, 1 / l)

def transform(bone, pose):
    """(R, t): where the bone puts a point p of its geometry: R p + t (whole chain, in model space)."""
    R, t = [[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0.0, 0.0, 0.0)
    chain = []
    while bone:
        chain.append(bone); bone = PARENT[bone]
    for b in reversed(chain):
        r = rot(pose.get(b, (0, 0, 0))); pv = PIVOT[b]; off = pose.get("pos:" + b, (0, 0, 0))
        # local: p -> off + pv + r (p - pv)
        lt = add(off, sub(pv, mvec(r, pv)))
        t = add(t, mvec(R, lt)); R = mmul(R, r)
    return R, t

def world(p, bone, pose):
    R, t = transform(bone, pose)
    return add(mvec(R, p), t)

def aim(pose, bone, direction):
    """Turn a limb bone (which hangs along -y at rest) to point along a model-space direction (no twist)."""
    parent_R, _ = transform(PARENT[bone], pose)
    d = norm(mvec(mT(parent_R), direction))
    rx_ = math.degrees(math.asin(max(-1.0, min(1.0, d[2]))))
    rz_ = math.degrees(math.atan2(-d[0], -d[1]))
    pose[bone] = (rx_, 0.0, rz_)

def ik(shoulder, target, a, b, pole):
    """Elbow position for a two-bone limb from shoulder to target (lengths a, b), bending toward pole."""
    d = sub(target, shoulder); dist = min(math.sqrt(dot(d, d)), a + b - 1e-3); u = norm(d)
    x = (a * a - b * b + dist * dist) / (2 * dist); h = math.sqrt(max(0.0, a * a - x * x))
    p = norm(sub(pole, mul(u, dot(pole, u))))
    return add(add(shoulder, mul(u, x)), mul(p, h))

# shoulder-to-elbow and elbow-to-wrist, taken from the rig so they always match it
UPPER = math.dist(PIVOT["right_arm"], PIVOT["right_forearm"])
FORE = math.dist(PIVOT["right_forearm"], PIVOT["right_hand"])

def reach(pose, side, wrist, pole):
    """Arm on this side ('right'/'left') with the wrist at a model-space point, the elbow bending toward pole. Both
    bones turn so their x axis is the elbow's hinge: the forearm only ever bends about it, and the arm never flips
    round its own length (as simply pointing each bone would when the arm passes straight forward)."""
    sh = world(PIVOT[f"{side}_arm"], f"{side}_arm", pose)
    el = ik(sh, wrist, UPPER, FORE, pole)
    upper, fore = norm(sub(el, sh)), norm(sub(wrist, el))
    # the hinge comes from where the elbow is told to point (steady even when the arm is almost straight)
    toward = norm(sub(wrist, sh))
    hinge = norm(cross(sub(pole, mul(toward, dot(pole, toward))), toward))
    def frame(along):                                        # the bone hangs along -y
        y = mul(along, -1); z = cross(hinge, y)
        return [[hinge[0], y[0], z[0]], [hinge[1], y[1], z[1]], [hinge[2], y[2], z[2]]]
    set_world_rotation(pose, f"{side}_arm", frame(upper))
    set_world_rotation(pose, f"{side}_forearm", frame(fore))

def set_world_rotation(pose, bone, R_world):
    """Give a bone whatever local rotation makes its world rotation R_world."""
    parent_R, _ = transform(PARENT[bone], pose)
    pose[bone] = euler(mmul(mT(parent_R), R_world))

def blend(a, b, u):
    out = {}
    for k in set(a) | set(b):
        va, vb = a.get(k, (0, 0, 0)), b.get(k, (0, 0, 0))
        out[k] = tuple(va[i] + (vb[i] - va[i]) * u for i in range(3))
    return out

def smooth(u):
    u = max(0.0, min(1.0, u)); return u * u * (3 - 2 * u)

# ---------------------------------------------------------------- poses
RELAXED = 3.0                   # how much relaxed fingers curl: just enough not to look flat

def fingers(side, curl, forward=-1.0):
    """The fingers curled toward the palm by curl degrees (0 = straight, 3 relaxed, ~75 gripping). His palms face
    his body, so they close about the hand's z axis, which turns opposite ways on each side."""
    return (forward, 0.0, -curl if side == "right" else curl)

# Standing at rest: arms hanging just clear of the coat, elbows a little bent, wrists and fingers relaxed (a hand at
# rest is half closed, never flat), head down a touch.
STAND = {"right_arm": (0, 0, 3), "left_arm": (0, 0, -3), "right_forearm": (-8, 0, 0), "left_forearm": (-8, 0, 0),
         "right_hand": (-2, 0, -3), "left_hand": (-2, 0, 3),
         "right_fingers": fingers("right", RELAXED), "left_fingers": fingers("left", RELAXED), "head": (4, 0, 0)}

def bump(pose, bone, dx=0.0, dy=0.0, dz=0.0):
    v = pose.get(bone, (0, 0, 0)); pose[bone] = (v[0] + dx, v[1] + dy, v[2] + dz)

IDLE_LENGTH = 8.0

def alive(pose, t, arms=True):
    """What keeps him from looking like a statue (in place, loops every 8 seconds): deep slow breaths, his weight
    shifting from one leg to the other, the head slowly turning to look around and tilting, the arms swaying with
    the body and the fingers flexing."""
    T = IDLE_LENGTH
    w = lambda k, phase=0.0: math.sin(2 * math.pi * (k * t / T + phase))
    breath = w(2)                                            # two breaths per loop
    bump(pose, "chest", -2.2 * breath); bump(pose, "head", 0.7 * breath)
    pose["pos:chest"] = (0, 0.18 * (breath + 1), 0)
    shift = w(1)                                             # weight onto one leg, then the other
    bump(pose, "body", 0, 0, 1.8 * shift); bump(pose, "chest", 0, 0, -1.3 * shift)
    pose["pos:body"] = (0.3 * shift, -0.2 * abs(shift), 0)
    bump(pose, "right_leg", -3.0 * max(0.0, shift), 0, -1.5 * shift); bump(pose, "right_shin", 6.0 * max(0.0, shift))
    bump(pose, "left_leg", -3.0 * max(0.0, -shift), 0, -1.5 * shift); bump(pose, "left_shin", 6.0 * max(0.0, -shift))
    look = w(1, 0.15) * 0.7 + w(3, 0.4) * 0.3               # turns to look one way, holds, then the other
    bump(pose, "head", 0.8 * w(3, 0.1), 9 * look, 2 * w(1, 0.6))
    if arms:
        # the arms hang from the shoulders as the body sways, so they swing out a little, never in toward the coat
        bump(pose, "right_arm", 2.5 * w(2, 0.3), 0, 1.5 * max(0.0, shift)); bump(pose, "left_arm", -2.5 * w(2, 0.3), 0, -1.5 * max(0.0, -shift))
        bump(pose, "right_forearm", -3 * (w(2, 0.35) + 1)); bump(pose, "left_forearm", -3 * (w(2, 0.85) + 1))
        pose["right_fingers"] = fingers("right", RELAXED + 1.5 * w(4, 0.2)); pose["left_fingers"] = fingers("left", RELAXED + 1.5 * w(4, 0.7))

def frame_matrix(x_axis, y_axis):
    z_axis = cross(x_axis, y_axis)
    return [[x_axis[0], y_axis[0], z_axis[0]], [x_axis[1], y_axis[1], z_axis[1]], [x_axis[2], y_axis[2], z_axis[2]]]

IDENTITY = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
def compose(a, b):
    """Transform a then... (R, t) pairs: compose(a, b) applies b first, then a."""
    return mmul(a[0], b[0]), add(mvec(a[0], b[1]), a[1])
def inverse(a):
    Rt = mT(a[0]); return Rt, mul(mvec(Rt, a[1]), -1)
def lerp_transform(a, b, u, about=(0.0, 0.0, 0.0)):
    """Blend two placements, turning about a point of the object (so that point travels in a straight line)."""
    R = slerp_matrix(a[0], b[0], u)
    ca, cb = add(mvec(a[0], about), a[1]), add(mvec(b[0], about), b[1])
    return R, sub(add(mul(ca, 1 - u), mul(cb, u)), mvec(R, about))

# Held to his chest: the open side of the hat against his coat over the heart, the crown pointing out (-z).
HAT_CHEST_R = frame_matrix((1, 0, 0), (0, 0, -1))
HAT_BRIM_CENTRE = (0.0, 37.25, 0.0)                         # the middle of the brim, on the hat at rest
HAT_CHEST_CENTRE = (0.9, 24.6, -4.0)                        # ...where it goes against his chest
GRIP_POINT = (-1.6, 36.4, -5.6)                             # his wrist taking the front edge of the brim (head space)
HAT_CHEST = (HAT_CHEST_R, sub(HAT_CHEST_CENTRE, mvec(HAT_CHEST_R, HAT_BRIM_CENTRE)))

# ---------------------------------------------------------------- the hat and the hand holding it
def hat_on_head(pose):
    """The hat's world transform while it sits on his head."""
    return transform("head", pose)

def hat_bone_pose(pose, hat_world):
    """Rotation and position keys for the hat bone (a child of the head) so it sits at hat_world (R, t). On his head
    that's no change at all, so the hat can't shift on his head; it only gets keys while it's in his hand."""
    R, t = compose(inverse(transform("head", pose)), hat_world)   # relative to his head (its parent bone)
    pv = PIVOT["hat"]
    pose["hat"] = euler(R); pose["pos:hat"] = sub(add(mvec(R, pv), t), pv)

def curve(a, c, b, u):
    """A point u of the way along a smooth curve from a to b bending toward c (keeps hands clear of the shoulder)."""
    return add(add(mul(a, (1 - u) ** 2), mul(c, 2 * u * (1 - u))), mul(b, u * u))

def wrist_of(side, hand_world):
    return add(mvec(hand_world[0], PIVOT[f"{side}_hand"]), hand_world[1])

def place_hand(pose, side, hand_world, pole, wrist=None):
    """Arm on this side reaching so its hand bone ends up at hand_world (R, t): wrist by IK, then the hand turned.
    The wrist can be given separately (to follow a curve while the hand turns)."""
    wrist = wrist or wrist_of(side, hand_world)
    reach(pose, side, wrist, pole)
    set_world_rotation(pose, f"{side}_hand", hand_world[0])

# Where the elbows point: back at rest (like anyone's), out and down while lifting, holding or waving.
RIGHT_REST_POLE, RIGHT_LIFT_POLE = (-0.3, -0.2, 1.0), (-1.0, -0.8, 0.3)
LEFT_REST_POLE, LEFT_WAVE_POLE = (0.3, -0.2, 1.0), (1.0, -1.0, 0.3)
def blend_pole(a, b, u):
    return add(mul(a, 1 - u), mul(b, u))

def _grip():
    """How the hat sits in his right hand: worked out with his hand on the brim and the hat on his head, so taking it
    and putting it back never makes it jump. The fingers point up over the brim, the palm toward his head."""
    pose = dict(STAND)
    fingers_up = norm((0.0, -1.0, 0.3))                      # the hand's -y (fingers) points up and a little forward
    hand_R = frame_matrix(cross(fingers_up, (1.0, 0.0, 0.0)), fingers_up)
    wrist = world(GRIP_POINT, "head", pose)
    hand_t = sub(wrist, mvec(hand_R, PIVOT["right_hand"]))
    return inverse((hand_R, hand_t)), hand_R           # hand^-1 at the grip moment, and the grip rotation

def grip_offset(pose):
    """G with hat = hand * G, for this frame's head pose."""
    hand_inv, hand_R = _GRIP
    return compose(hand_inv, hat_on_head(dict(STAND)))

_GRIP = _grip()
GRIP_G = grip_offset(STAND)

def hand_for_hat(hat_world):
    """Where his right hand must be to hold the hat at hat_world."""
    return compose(hat_world, inverse(GRIP_G))

def hand_resting(pose):
    return transform("right_hand", pose)

# ---------------------------------------------------------------- animations
def idle_frame(t):
    pose = dict(STAND); alive(pose, t)
    hat_bone_pose(pose, hat_on_head(pose))
    return pose

# Wave timings (seconds): reach for the hat, lift it off, bring it to the chest, wave with the other hand, lift it
# back onto his head, let go and lower the arm.
REACH, LIFT, CHEST = 1.4, 1.8, 2.9
WAVE_START, WAVE_END = 3.1, 5.6
RAISE, PUT_ON, LET_GO = 6.9, 7.6, 8.7
WAVE_LENGTH = LET_GO

def wave_frame(t):
    pose = dict(STAND); alive(pose, t, arms=False)
    # a small bow (first: the hand and the hat are placed on the body as it is after bowing) while the hat comes down, and as it goes back on
    bow = smooth((t - LIFT) / 0.7) - smooth((t - CHEST - 0.2) / 0.7) + 0.6 * (smooth((t - RAISE) / 0.4) - smooth((t - PUT_ON) / 0.5))
    bump(pose, "head", 10 * bow); bump(pose, "chest", 3 * bow)
    head_hat = hat_on_head(pose)
    lifted = (head_hat[0], add(head_hat[1], (0.0, 2.2, -0.8)))
    if t < REACH or t >= LET_GO - 1e-9:
        hat = head_hat
    elif t < LIFT:
        hat = lerp_transform(head_hat, lifted, smooth((t - REACH) / (LIFT - REACH)), HAT_BRIM_CENTRE)
    elif t < CHEST:
        hat = lerp_transform(lifted, HAT_CHEST, smooth((t - LIFT) / (CHEST - LIFT)), HAT_BRIM_CENTRE)
    elif t < WAVE_END + 0.2:
        hat = HAT_CHEST
    elif t < RAISE:
        hat = lerp_transform(HAT_CHEST, lifted, smooth((t - WAVE_END - 0.2) / (RAISE - WAVE_END - 0.2)), HAT_BRIM_CENTRE)
    elif t < PUT_ON:
        hat = lerp_transform(lifted, head_hat, smooth((t - RAISE) / (PUT_ON - RAISE)), HAT_BRIM_CENTRE)
    else:
        hat = head_hat
    # the right hand: reaching for the brim, holding the hat, then letting go and dropping back to his side
    rest = dict(pose)
    rest_hand = transform("right_hand", rest)
    if t < REACH:
        u = smooth(t / REACH)
        sh = world(PIVOT["right_arm"], "right_arm", pose)
        wrist = curve(wrist_of("right", rest_hand), add(sh, (-4.5, -5.0, -5.0)), wrist_of("right", hand_for_hat(head_hat)), u)
        place_hand(pose, "right", lerp_transform(rest_hand, hand_for_hat(head_hat), u, PIVOT["right_hand"]),
                   blend_pole(RIGHT_REST_POLE, RIGHT_LIFT_POLE, u), wrist)
        pose["right_fingers"] = fingers("right", RELAXED * (1 - u))   # opening to take the brim
    elif t < PUT_ON:
        place_hand(pose, "right", hand_for_hat(hat), RIGHT_LIFT_POLE)
        pose["right_fingers"] = fingers("right", 75 * smooth((t - REACH) / 0.35))   # closing round the brim
    else:
        u = smooth((t - PUT_ON) / (LET_GO - PUT_ON))
        top = wrist_of("right", hand_for_hat(head_hat))
        sh = world(PIVOT["right_arm"], "right_arm", pose)
        wrist = curve(top, add(sh, (-4.5, -5.0, -5.0)), wrist_of("right", rest_hand), u)   # down and out in front
        place_hand(pose, "right", lerp_transform(hand_for_hat(head_hat), rest_hand, u, PIVOT["right_hand"]),
                   blend_pole(RIGHT_LIFT_POLE, RIGHT_REST_POLE, u), wrist)
        pose["right_fingers"] = fingers("right", 75 + (RELAXED - 75) * u)
    hat_bone_pose(pose, hat)
    # the wave: his left arm out and up, the forearm upright, the hand rocking slowly side to side
    # (the hand's position is blended, and the arm follows it, so no angle ever has to swing round)
    up = smooth((t - WAVE_START) / 1.0) - smooth((t - WAVE_END + 0.2) / 1.0)
    rock = math.sin(2 * math.pi * (t - WAVE_START - 0.8) / 1.0) * smooth((t - WAVE_START - 0.6) / 0.5) * up
    rest_wrist = world(PIVOT["left_hand"], "left_hand", pose)
    sh = world(PIVOT["left_arm"], "left_arm", pose)
    wave_wrist = add(sh, (6.2 + 2.0 * rock, 2.6, -3.6))      # the hand up beside his head, out to the side
    out_to_side = add(sh, (8.5, -6.5, -1.5))                 # the hand swings out to the side on its way up
    reach(pose, "left", curve(rest_wrist, out_to_side, wave_wrist, up), blend_pole(LEFT_REST_POLE, LEFT_WAVE_POLE, math.sqrt(up)))
    bump(pose, "left_hand", 0, 0, -10 * rock)
    pose["left_fingers"] = fingers("left", RELAXED * (1 - up))   # the waving hand opens
    return pose

def wave_frames():
    """Sampled 40 times a second: while the hat is in his hand its keys are relative to his moving head, so they
    need to be close together for hat and hand to stay together in between."""
    return [(i / 40.0, wave_frame(i / 40.0)) for i in range(int(round(WAVE_LENGTH * 40)) + 1)]

def quat_from_matrix(m):
    tr = m[0][0] + m[1][1] + m[2][2]
    if tr > 0:
        s = math.sqrt(tr + 1.0) * 2; w = 0.25 * s
        x = (m[2][1] - m[1][2]) / s; y = (m[0][2] - m[2][0]) / s; z = (m[1][0] - m[0][1]) / s
    elif m[0][0] > m[1][1] and m[0][0] > m[2][2]:
        s = math.sqrt(1.0 + m[0][0] - m[1][1] - m[2][2]) * 2; w = (m[2][1] - m[1][2]) / s
        x = 0.25 * s; y = (m[0][1] + m[1][0]) / s; z = (m[0][2] + m[2][0]) / s
    elif m[1][1] > m[2][2]:
        s = math.sqrt(1.0 + m[1][1] - m[0][0] - m[2][2]) * 2; w = (m[0][2] - m[2][0]) / s
        x = (m[0][1] + m[1][0]) / s; y = 0.25 * s; z = (m[1][2] + m[2][1]) / s
    else:
        s = math.sqrt(1.0 + m[2][2] - m[0][0] - m[1][1]) * 2; w = (m[1][0] - m[0][1]) / s
        x = (m[0][2] + m[2][0]) / s; y = (m[1][2] + m[2][1]) / s; z = 0.25 * s
    return (w, x, y, z)

def matrix_from_quat(q):
    w, x, y, z = q
    return [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
            [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
            [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]

def slerp_matrix(a, b, u):
    qa, qb = quat_from_matrix(a), quat_from_matrix(b)
    d = sum(qa[i] * qb[i] for i in range(4))
    if d < 0:
        qb = tuple(-v for v in qb); d = -d
    if d > 0.9995:
        q = tuple(qa[i] + (qb[i] - qa[i]) * u for i in range(4))
    else:
        th = math.acos(d); s = math.sin(th)
        q = tuple((math.sin((1 - u) * th) * qa[i] + math.sin(u * th) * qb[i]) / s for i in range(4))
    l = math.sqrt(sum(v * v for v in q)); q = tuple(v / l for v in q)
    return matrix_from_quat(q)

WALK_LENGTH = 1.6

def walk_frame(t):
    """A slow, even walk, the hat back on his head: the arms swing loosely from the shoulders, clear of the coat,
    the elbows bending more as each arm comes forward; a gentle bob, a little twist through the body."""
    ph = 2 * math.pi * t / WALK_LENGTH; s = math.sin(ph)
    pose = dict(STAND)
    pose["right_leg"] = (-22 * s, 0, 0); pose["left_leg"] = (22 * s, 0, 0)
    pose["right_shin"] = (max(0.0, 26 * math.sin(ph + 0.9)), 0, 0)
    pose["left_shin"] = (max(0.0, -26 * math.sin(ph + 0.9)), 0, 0)
    pose["right_foot"] = (-10 * max(0.0, -math.sin(ph + 0.3)), 0, 0); pose["left_foot"] = (-10 * max(0.0, math.sin(ph + 0.3)), 0, 0)
    lag = math.sin(ph - 0.35)                                 # the arms trail the legs a little: they swing, not march
    fwd_r, fwd_l = max(0.0, -lag), max(0.0, lag)             # how far forward each arm has swung
    pose["right_arm"] = (18 * lag, 0, 4); pose["left_arm"] = (-18 * lag, 0, -4)
    # the elbow bends more as the arm comes forward, and stays a little bent as it swings back
    pose["right_forearm"] = (-12 - 18 * fwd_r, 0, 0); pose["left_forearm"] = (-12 - 18 * fwd_l, 0, 0)
    pose["right_hand"] = (-3 - 3 * fwd_r, 0, -3); pose["left_hand"] = (-3 - 3 * fwd_l, 0, 3)
    pose["right_fingers"] = fingers("right", RELAXED + 1.5 * fwd_r); pose["left_fingers"] = fingers("left", RELAXED + 1.5 * fwd_l)
    pose["pos:root"] = (0, -0.2 * abs(math.cos(ph)), 0)
    pose["body"] = (0, 4.0 * s, 1.0 * math.cos(ph)); pose["chest"] = (3.0, -4.0 * s, -0.8 * math.cos(ph))
    pose["head"] = (5.0, 0.0, -0.2 * math.cos(ph))          # held steady against the body's twist and sway
    hat_bone_pose(pose, hat_on_head(pose))
    return pose

TURN_LENGTH = 1.4

def turn_frame(t):
    """Turning round on the spot to walk away (the game turns him half a circle over the same 1.4 seconds): his head
    turns first, his shoulders follow, and his feet shuffle round in three small steps."""
    u = t / TURN_LENGTH
    pose = dict(STAND)
    lead = math.sin(math.pi * min(1.0, u * 1.3))              # the head and shoulders turning ahead of the body
    bump(pose, "head", 2, 28 * lead, 0); bump(pose, "chest", 0, 10 * lead, 0); bump(pose, "body", 0, 4 * lead, 0)
    def step(start, side):
        k = math.sin(math.pi * max(0.0, min(1.0, (u - start) / 0.3)))
        bump(pose, f"{side}_leg", -14 * k, 0, 0); bump(pose, f"{side}_shin", 26 * k); bump(pose, f"{side}_foot", -8 * k)
        return k
    step(0.05, "right"); step(0.35, "left"); step(0.65, "right")
    pose["pos:root"] = (0, -0.25 * math.sin(math.pi * u * 3) ** 2, 0)
    bump(pose, "right_arm", 6 * lead, 0, 2 * lead); bump(pose, "left_arm", -6 * lead, 0, -2 * lead)
    hat_bone_pose(pose, hat_on_head(pose))
    return pose

# ---------------------------------------------------------------- texture
TEXELS = 2                      # texture pixels per model pixel
TEX_W = TEX_H = 512
COLOURS = {"coat": (30, 28, 30), "trousers": (24, 24, 27), "shoe": (12, 12, 13), 
           "shirt": (176, 172, 162), "shirt_plain": (170, 166, 158), "tie": (38, 30, 34), "lapel": (24, 22, 24),
           "skin": (44, 38, 36), "face": (34, 30, 29), "hand": (48, 42, 40), "hat": (17, 17, 19), "hat_band": (8, 8, 9),
           "sole": (8, 8, 8)}

def _hash(x, y, seed):
    h = (x * 374761393 + y * 668265263 + seed * 144269504) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0

FACES = ("north", "south", "east", "west", "up", "down")
def face_size(a, b, face):
    w, h, d = (b[0] - a[0], b[1] - a[1], b[2] - a[2])
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[face]

BLEED = 1                       # UV units of each face's colours painted past its edges

def layout():
    """Shelf-pack every face of every cube into the texture. Returns {(cube index, face): (u, v, w, h)} in model px.
    Each face starts on a whole UV unit with room around it, and its colours are painted a unit past its edges
    (see paint), so editors and the game that round thin faces' UVs a little differently still sample the right
    colours (thin faces otherwise showed the neighbouring face, or nothing, in Blockbench)."""
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
    shade = {"up": 1.12, "down": 0.7, "north": 1.0, "south": 0.9, "east": 0.94, "west": 0.94}
    for (i, f), (u, v, w, h) in rects.items():
        name, bone, a, b, mat = CUBES[i]
        base = COLOURS[mat]
        x0, y0 = int(u * TEXELS), int(v * TEXELS); x1, y1 = x0 + max(1, math.ceil(w * TEXELS)), y0 + max(1, math.ceil(h * TEXELS))
        m = BLEED * TEXELS
        for y in range(y0 - m, y1 + m):
            for x in range(x0 - m, x1 + m):
                cx, cy = min(max(x, x0), x1 - 1), min(max(y, y0), y1 - 1)     # the bleed repeats the edge
                s, t = (cx - x0 + 0.5) / (x1 - x0), (cy - y0 + 0.5) / (y1 - y0)
                k = shade[f] * (0.92 + 0.16 * _hash(cx, cy, i))               # fabric grain
                c = base
                if mat == "shirt" and int((cx - x0) / 2) % 3 == 0:            # thin grey stripes
                    c = (120, 116, 110)
                if mat == "tie" and (cx - x0 + cy - y0) % 5 < 1:              # diagonal stripes
                    c = (92, 80, 86)
                if mat == "face" and f == "north":
                    c = face_texel(s, t)
                if mat in ("coat", "trousers", "hat") and f in ("north", "south", "east", "west"):
                    k *= 0.86 + 0.14 * (1 - t)                              # a little lighter at the top
                if mat == "shoe" and f == "up":
                    k *= 1.3                                                # polished
                if 0 <= x < TEX_W and 0 <= y < TEX_H:
                    px[x, y] = tuple(max(0, min(255, int(ch * k))) for ch in c) + (255,)
    return img

def face_texel(s, t):
    """The face, almost lost in shadow: dark sockets, a faint ridge of cheekbone, a thin mouth."""
    c = COLOURS["face"]
    def near(cx, cy, rx, ry):
        return ((s - cx) / rx) ** 2 + ((t - cy) / ry) ** 2 < 1
    if near(0.3, 0.38, 0.13, 0.08) or near(0.7, 0.38, 0.13, 0.08):
        return (16, 14, 14)                                   # eye sockets
    if near(0.3, 0.52, 0.16, 0.05) or near(0.7, 0.52, 0.16, 0.05):
        return (52, 46, 44)                                   # cheekbones catching a little light
    if 0.36 < s < 0.64 and 0.72 < t < 0.75:
        return (20, 17, 17)                                   # mouth
    return c

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
            size = [round(bb[k] - a[k], 4) for k in range(3)]
            uv = {}
            for f in FACES:
                u, v, w, h = rects[(i, f)]
                fw, fh = face_size(a, bb, f)
                if f in ("up", "down"):                    # Bedrock draws these flipped
                    uv[f] = {"uv": [round(u + fw, 4), round(v + fh, 4)], "uv_size": [round(-fw, 4), round(-fh, 4)]}
                else:
                    uv[f] = {"uv": [round(u, 4), round(v, 4)], "uv_size": [round(fw, 4), round(fh, 4)]}
            cubes.append({"origin": [round(c, 4) for c in a], "size": size, "uv": uv})
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.odie", "texture_width": TEX_W // TEXELS, "texture_height": TEX_H // TEXELS,
                        "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}

ANIMATED = [n for n, _, _ in BONES if n != "root"] + ["root"]

def continuous(frames):
    """Keep every bone's rotation continuous from frame to frame: each angle is shifted by whole turns, and the
    equivalent (x - 180, 180 - y, z - 180) form is used where it's closer, so nothing spins the long way round
    between keys (that's what made the arm glitch when reaching for the hat)."""
    last = {}
    for _, pose in frames:
        for b, v in list(pose.items()):
            if ":" in b:
                continue
            prev = last.get(b)
            if prev is not None:
                best = None
                for cand in (v, (v[0] - 180, 180 - v[1], v[2] - 180)):
                    c = tuple(cand[i] + 360 * round((prev[i] - cand[i]) / 360) for i in range(3))
                    d = sum(abs(c[i] - prev[i]) for i in range(3))
                    if best is None or d < best[0]:
                        best = (d, c)
                v = best[1]; pose[b] = v
            last[b] = v
    return frames

def animation(frames, loop):
    frames = continuous(frames)
    bones = {}
    for b in ANIMATED:
        rots, poss = {}, {}
        for t, pose in frames:
            key = f"{round(t, 4)}"
            r = pose.get(b, (0, 0, 0)); rots[key] = [round(v, 3) for v in r]
            p = pose.get("pos:" + b)
            if p is not None or any(f[1].get("pos:" + b) for f in frames):
                p = p or (0, 0, 0); poss[key] = [round(v, 3) for v in p]
        entry = {}
        if any(any(abs(v) > 1e-4 for v in r) for r in rots.values()):
            entry["rotation"] = rots if b == "hat" else thin(rots)      # the hat keeps every key (see wave_frames)
        if poss and any(any(abs(v) > 1e-4 for v in p) for p in poss.values()):
            entry["position"] = poss if b == "hat" else thin(poss)
        if entry:
            bones[b] = entry
    length = frames[-1][0]
    return {"loop": True if loop == "loop" else "hold_on_last_frame", "animation_length": round(length, 4), "bones": bones}

def thin(keys, tol=0.05):
    """Drop keys that lie on a straight line between their neighbours (linear interpolation in between)."""
    items = sorted(keys.items(), key=lambda kv: float(kv[0]))
    out = [items[0]]
    for i in range(1, len(items) - 1):
        (ta, va), (tb, vb), (tc, vc) = out[-1], items[i], items[i + 1]
        u = (float(tb) - float(ta)) / (float(tc) - float(ta))
        if max(abs(va[k] + (vc[k] - va[k]) * u - vb[k]) for k in range(3)) > tol:
            out.append(items[i])
    out.append(items[-1])
    return {k: v for k, v in out}

# ---------------------------------------------------------------- Blockbench file
def bbmodel(geometry, anims, texture_path):
    """The model as a Blockbench project (Bedrock format, per-face UV) with the texture and animations built in.
    Blockbench's space is the game's, which mirrors Bedrock's X: cube and pivot X flip; animation values carry over."""
    desc = geometry["minecraft:geometry"][0]
    ids = {b["name"]: str(uuid.uuid5(uuid.NAMESPACE_URL, "odie/" + b["name"])) for b in desc["bones"]}
    elements, children = [], {b["name"]: [] for b in desc["bones"]}
    for b in desc["bones"]:
        for i, c in enumerate(b.get("cubes", [])):
            o, sz = c["origin"], c["size"]
            frm = [-(o[0] + sz[0]), o[1], o[2]]; to = [-o[0], o[1] + sz[1], o[2] + sz[2]]
            faces = {}
            for f, fuv in c["uv"].items():
                (u, v), (w, h) = fuv["uv"], fuv["uv_size"]
                faces[f] = {"uv": [u, v, u + w, v + h], "texture": 0}
            eid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"odie/{b['name']}/{i}"))
            elements.append({"name": f"{b['name']}_{i}", "box_uv": False, "rescale": False, "locked": False,
                             "from": frm, "to": to, "autouv": 0, "color": 0, "origin": [-b["pivot"][0], b["pivot"][1], b["pivot"][2]],
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
    animations = []
    for name, a in anims.items():
        animators = {}
        for bone, chans in a["bones"].items():
            keys = []
            for ch in ("rotation", "position"):
                for t, v in chans.get(ch, {}).items():
                    keys.append({"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}],
                                 "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"odie/{name}/{bone}/{ch}/{t}")),
                                 "time": float(t), "color": -1, "interpolation": "linear"})
            animators[ids[bone]] = {"name": bone, "type": "bone", "keyframes": keys}
        animations.append({"uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "odie/anim/" + name)), "name": name,
                           "loop": "loop" if a["loop"] is True else "hold", "override": False,
                           "length": a["animation_length"], "snapping": 20, "selected": False, "animators": animators})
    img = Image.open(texture_path)
    data = open(texture_path, "rb").read()
    # Only the embedded image, and an id that changes whenever the image does: Blockbench otherwise reuses a texture
    # it already has loaded under the same id (an older, differently laid out one), scrambling every face.
    texture = {"name": "odie.png", "folder": "entity", "namespace": "horrormod", "id": "0",
               "width": img.width, "height": img.height, "uv_width": desc["description"]["texture_width"], "uv_height": desc["description"]["texture_height"],
               "particle": False, "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True,
               "saved": True, "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "odie.png/" + hashlib.sha1(data).hexdigest())),
               "source": "data:image/png;base64," + base64.b64encode(data).decode()}
    return {"meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": False},
            "name": "odie", "model_identifier": "geometry.odie", "visible_box": [4, 4, 1.5],
            "resolution": {"width": desc["description"]["texture_width"], "height": desc["description"]["texture_height"]},
            "elements": elements, "outliner": [group("root")], "textures": [texture], "animations": animations}

# ---------------------------------------------------------------- preview
def render(pose, view, size=(240, 320), scale=6.0, title=""):
    img = Image.new("RGB", size, (110, 110, 118)); d = ImageDraw.Draw(img)
    # (GeckoLib mirrors x, so +x, his left, shows on the viewer's right when he faces us)
    cam = {"front": lambda p: (p[0], p[1], p[2]), "side": lambda p: (p[2], p[1], -p[0]),
           "three": lambda p: (-p[0] * 0.8 + p[2] * 0.6, p[1], p[2] * 0.8 + p[0] * 0.6)}[view]
    polys = []
    quads = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
    for name, bone, a, b, mat in CUBES:
        R, t = transform(bone, pose)
        pts = [cam(add(mvec(R, (x, y, z)), t)) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])]
        for q in quads:
            qq = [pts[i] for i in q]
            col = tuple(min(255, int(c * 1.8 + 20)) for c in COLOURS[mat])
            polys.append((sum(p[2] for p in qq) / 4, [(size[0] / 2 + p[0] * scale, size[1] - 12 - p[1] * scale) for p in qq], col))
    for _, q, c in sorted(polys, key=lambda x: -x[0]):
        d.polygon(q, fill=c, outline=(max(0, c[0] - 50),) * 3)
    d.text((4, 4), title, fill=(255, 255, 255))
    return img

if __name__ == "__main__":
    rects = layout()
    tex = paint(rects)
    os.makedirs(os.path.join(ASSETS, "geo", "entity"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "animations", "entity"), exist_ok=True)
    tex_path = os.path.join(ASSETS, "textures", "entity", "odie.png")
    tex.save(tex_path)
    geometry = geo(rects)
    json.dump(geometry, open(os.path.join(ASSETS, "geo", "entity", "odie.geo.json"), "w"), indent=1)
    idle = [(i / 20.0, idle_frame(i / 20.0)) for i in range(0, int(IDLE_LENGTH * 20) + 1)]
    walk = [(i / 20.0, walk_frame(i / 20.0)) for i in range(0, int(WALK_LENGTH * 20) + 1)]
    turn = [(i / 20.0, turn_frame(i / 20.0)) for i in range(0, int(TURN_LENGTH * 20) + 1)]
    wave = wave_frames()
    anims = {"idle": animation(idle, "loop"), "wave": animation(wave, "hold"), "turn_around": animation(turn, "hold"),
             "walk_away": animation(walk, "loop")}
    json.dump({"format_version": "1.8.0", "animations": anims},
              open(os.path.join(ASSETS, "animations", "entity", "odie.animation.json"), "w"), separators=(",", ":"))
    json.dump(bbmodel(geometry, anims, tex_path), open(BBMODEL, "w"), indent=1)
    at = lambda t: dict(wave[int(round(t * 40))][1])
    shots = [("idle 0", idle_frame(0)), ("idle 2", idle_frame(2)), ("idle 6", idle_frame(6)), ("reach 1.4", at(1.4)),
             ("lift 1.8", at(1.8)), ("chest 2.9", at(2.9)), ("wave 4.3", at(4.3)), ("raise 6.3", at(6.3)),
             ("on 7.6", at(7.6)), ("end 8.7", at(8.7)), ("turn 0.5", turn_frame(0.5)),
             ("walk 0.4", walk_frame(0.4)), ("walk 1.2", walk_frame(1.2))]
    sheet = Image.new("RGB", (240 * len(shots), 640))
    for i, (title, pose) in enumerate(shots):
        sheet.paste(render(pose, "front", title=title), (i * 240, 0))
        sheet.paste(render(pose, "side", title=title + " side"), (i * 240, 320))
    sheet.save(os.path.join(PREVIEW, "odie_preview.png"))
    print("wrote odie geo, animations, texture and", BBMODEL, "; preview in", PREVIEW)
