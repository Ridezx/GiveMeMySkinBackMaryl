"""Skinwalker ("The Rake") blockout: a dog wearing a stolen human skin, learning to be human.
Writes art/skinwalker_blockout.bbmodel (rig + animations) and preview images.
Coordinates are Blockbench pixels: feet at y=0, the face toward -Z. Rotations are Blockbench group
rotations in degrees [x, y, z], Euler order ZYX (X applied first), same sign as Blockbench's editor.
"""
import base64, itertools, json, math, os, sys, uuid
from PIL import Image, ImageDraw

OUT = sys.argv[1] if len(sys.argv) > 1 else "."
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else "."

# ---------------------------------------------------------------- rig
# (name, parent, pivot)
BONES = [
    ("root", None, (0, 0, 0)),
    ("hips", "root", (0, 38, 0)),
    ("torso", "hips", (0, 41, 0)),
    ("chest", "torso", (0, 48, 0)),
    ("neck", "chest", (0, 60, -1)),
    ("head", "neck", (0, 65, -1)),
    ("jaw", "head", (0, 67.2, 0.5)),   # hinged in front of the ears, so it can open (screaming)
]
CUBES = []  # (name, bone, from, to, tone, inflate)
def cube(name, bone, a, b, tone, inflate=0.0):
    CUBES.append((name, bone, a, b, tone, inflate))

cube("pelvis", "hips", (-4, 38, -2.5), (4, 41, 2.5), 150)
cube("belly", "torso", (-3, 41, -2), (3, 48, 2), 138)              # tucked waist
# Human hips and stomach (final positions; the torso is below the lifted chest): the hip bones jutting at
# the front, rounded buttocks behind, the groin below; a gaunt stomach with three rows of abdominal muscle, a navel,
# and the obliques along the sides of the waist.
cube("groin", "hips", (-2, 36.5, -2), (2, 38, 1), 145)
cube("ab_low", "torso", (-2, 42, -2.5), (2, 45.5, -2), 142)
cube("ab_mid", "torso", (-2, 45.8, -2.5), (2, 49.3, -2), 142)
cube("ab_high", "torso", (-2, 49.6, -2.5), (2, 53, -2), 142)
cube("navel", "torso", (-0.35, 45.85, -2.52), (0.35, 46.5, -2.5), 60)
for side, s in (("right", -1), ("left", 1)):
    x0, x1 = sorted((s * 2.8, s * 4.4)); cube(f"{side}_hip_bone", "hips", (x0, 40, -2.9), (x1, 42, -1.2), 160)
    # each buttock built up in layers so it's rounded: fullest in the middle, a fold underneath, tapering
    # into the lower back above, with a cleft between the two
    for name, (xa, xb), (ya, yb), back in (("buttock", (0.3, 3.9), (35.5, 39.5), 2.9),
                                           ("buttock_full", (0.8, 3.4), (36.3, 38.8), 3.4),
                                           ("buttock_fold", (0.8, 3.3), (34.8, 35.5), 2.4),
                                           ("buttock_top", (0.6, 3.6), (39.5, 40.5), 2.5)):
        x0, x1 = sorted((s * xa, s * xb)); cube(f"{side}_{name}", "hips", (x0, ya, 1.2), (x1, yb, back), 150)
    x0, x1 = sorted((s * 3, s * 3.4)); cube(f"{side}_oblique", "torso", (x0, 43, -1.6), (x1, 53, 1.4), 140)
# Deep, narrow, dog-like chest, caved in down the middle: the sides come forward, the centre sinks back,
# with the breastbone in the hollow; a sloping trapezius up to the neck.
cube("ribcage", "chest", (-4.5, 48, -3), (4.5, 60, 3), 160)
cube("sternum", "chest", (-0.6, 50, -3.5), (0.6, 58.5, -3), 150)
cube("trapezius", "chest", (-3.5, 60, -1), (3.5, 61.5, 2.5), 155)
for side, s in (("right", -1), ("left", 1)):
    x0, x1 = sorted((s * 1.5, s * 4.5)); cube(f"{side}_chest_side", "chest", (x0, 49, -4), (x1, 59, -3), 160)
cube("neck", "neck", (-1.5, 60, -2.5), (1.5, 65, 0.5), 150)        # long neck
# the nape: muscle behind the neck holding up the skull, thicker where it meets the head
cube("nape", "neck", (-2, 60, 0.5), (2, 63.5, 2.2), 150)
cube("upper_nape", "neck", (-2.5, 63.5, 0.5), (2.5, 65.5, 3), 150)
# The human head, built from a skull's side profile: a big domed cranium that bulges out at the back, a
# forehead sloping back from the brow ridge, the face set lower and jutting forward under the eyes, eye
# sockets sunk into the face with a rim around them, cheekbones running back as an arch to the ear, a nose
# that is a hole under projecting nasal bones, teeth curving around the front, the ear hole with the mastoid
# bump behind it. The jaw (an L-shaped mandible hinged in front of the ear) still has skin on it.
# Face toward -Z; the head bone pivots on the neck at (0, 65, -1).
BONE, EYE, DARK, SKIN, TEETH = 200, 255, 35, 170, 230
def sym(name, bone, xa, xb, y, z, tone):
    """A pair of cubes mirrored left/right (xa..xb on the left side, as distances from the middle)."""
    for side, s in (("right", -1), ("left", 1)):
        x0, x1 = sorted((s * xa, s * xb))
        cube(f"{side}_{name}", bone, (x0, y[0], z[0]), (x1, y[1], z[1]), tone)
def mid(name, bone, w, y, z, tone):
    cube(name, bone, (-w, y[0], z[0]), (w, y[1], z[1]), tone)
# cranium, top down: each layer's front steps back (sloping forehead) and the back bulges out (occiput)
mid("crown", "head", 2.5, (74.5, 75.5), (-1.5, 4), BONE)
mid("cranium_top", "head", 3.5, (73, 74.5), (-2.8, 5.6), BONE)
mid("cranium", "head", 4.2, (71, 73), (-3.3, 6.4), BONE)
mid("cranium_back", "head", 4, (68.5, 71), (1, 6.8), BONE)             # behind the temples, widest bulge
mid("temples", "head", 3.7, (68.5, 71), (-2.6, 1), BONE)              # narrower: the hollow at the temple
mid("skull_base", "head", 3.5, (67, 68.5), (-1, 6), BONE)
mid("occiput", "head", 3, (65.5, 67), (0.5, 4.8), BONE)             # base of the skull into the nape
# forehead and brow
mid("forehead", "head", 3.6, (72, 73), (-3.7, -3.3), BONE)
mid("brow_ridge", "head", 4, (71, 72), (-4.3, -3.3), BONE)
# eye sockets: the eyes sit at the back of deep pits, with the bridge of the nose between and a rim outside
sym("eye", "head", 0.7, 3.4, (68.6, 70.8), (-2.9, -2.6), EYE)         # glows
sym("orbit_rim", "head", 3.6, 4.1, (68.3, 71), (-4, -2.5), BONE)
mid("nose_bridge", "head", 0.5, (68.5, 71), (-4.6, -2.6), BONE)
mid("nasal_bones", "head", 0.6, (68, 69), (-5, -2.6), BONE)
# cheekbones and the zygomatic arch running back along the side of the head to just in front of the ear
sym("cheekbone", "head", 2.2, 4.1, (66.8, 68.3), (-4, -1.5), BONE)
sym("cheek_arch", "head", 3.9, 4.4, (67.2, 68.2), (-1.5, 1.2), BONE)
sym("cheek_hollow", "head", 2.4, 3.4, (65.3, 67.2), (-1.5, 0.5), 180)   # recessed under the arch
for (ya, yb), (xa, xb) in itertools.product(((68.3, 68.9), (70.4, 71)), ((0.5, 1.1), (2.9, 3.6))):
    sym(f"orbit_corner_{ya:.0f}_{xa:.0f}", "head", xa, xb, (ya, yb), (-4, -2.6), BONE)   # rounds the sockets
# the face below the eyes jutting forward (maxilla), with the nose hole in it
sym("maxilla", "head", 0.6, 2.4, (66.3, 67.8), (-4.6, -1.5), BONE)
mid("under_nose", "head", 0.6, (65.5, 66.3), (-4.8, -1.5), BONE)
mid("nose_hole", "head", 0.6, (66.3, 68), (-3.4, -3.2), DARK)
mid("palate", "head", 2.4, (65.3, 66.3), (-4.2, -1.5), BONE)
# upper teeth, curving around the front of the mouth
mid("upper_front_teeth", "head", 1.6, (64.8, 65.8), (-4.9, -4.2), TEETH)
sym("upper_side_teeth", "head", 1.6, 2.4, (64.9, 65.8), (-4.3, -2.8), TEETH)
sym("upper_molars", "head", 1.8, 2.6, (64.9, 65.8), (-2.8, -1.5), TEETH)
mid("mouth", "head", 2.2, (63.9, 65.3), (-3.6, -3.3), DARK)            # dark inside of the open mouth
# ear hole with a rim, and the mastoid bump behind it
sym("ear_hole", "head", 3.45, 3.55, (66.9, 67.9), (0.9, 1.8), DARK)
sym("ear_rim_top", "head", 3.5, 3.9, (67.9, 68.3), (0.6, 2.1), 185)
sym("ear_rim_back", "head", 3.5, 3.9, (66.7, 67.9), (1.8, 2.2), 185)
sym("mastoid", "head", 3.0, 3.8, (65.5, 66.9), (1.8, 3.0), BONE)
# the jaw: L-shaped, the back edge rising to the hinge in front of the ear, the corner of the jaw, the body
# along each side to the chin at the front; skin still on it, the lower teeth along the top
sym("jaw_ramus", "jaw", 2.9, 3.6, (63.5, 67.2), (-0.5, 0.8), SKIN)
sym("jaw_body", "jaw", 2.2, 3.3, (62.6, 64), (-4, 0.8), SKIN)
mid("chin", "jaw", 2.2, (62.6, 64), (-4.6, -3.2), SKIN)
mid("chin_point", "jaw", 1.4, (62.2, 63), (-4.9, -3.6), SKIN)
mid("jaw_floor", "jaw", 2.2, (62.6, 63.4), (-3.2, 0.5), SKIN)
mid("lower_front_teeth", "jaw", 1.5, (64, 64.6), (-4.5, -3.9), TEETH)
sym("lower_side_teeth", "jaw", 1.5, 2.2, (64, 64.6), (-4, -2.8), TEETH)
sym("lower_molars", "jaw", 1.8, 2.6, (64, 64.6), (-2.8, -1.5), TEETH)
# A bony back: shoulder blades standing out of the upper back, a ridge of vertebrae down the spine, and a
# muscle either side of the spine on the waist so the spine sits in a groove.
for side, s in (("right", -1), ("left", 1)):
    x0, x1 = sorted((s * 1, s * 4.2)); cube(f"{side}_shoulder_blade", "chest", (x0, 52, 3), (x1, 58.5, 3.6), 165)
    x0, x1 = sorted((s * 0.6, s * 2.6)); cube(f"{side}_back_muscle", "torso", (x0, 41, 2), (x1, 54, 2.5), 140)
for i, y in enumerate((49, 51.5, 54, 56.5)):
    cube(f"vertebra_upper{i + 1}", "chest", (-0.5, y, 3), (0.5, y + 1, 3.6), 170)
for i, y in enumerate((42, 44.5, 47, 49.5, 52)):
    cube(f"vertebra_lower{i + 1}", "torso", (-0.5, y, 2), (0.5, y + 1, 2.8), 165)

for side, s in (("right", -1), ("left", 1)):
    def xs(a, b):
        return tuple(sorted((s * a, s * b)))
    # arms = front legs: shoulder, elbow, wrist (long, bony)
    BONES += [(f"{side}_arm", "chest", (s * 6, 58, 0)),
              (f"{side}_forearm", f"{side}_arm", (s * 6, 43, 0)),
              (f"{side}_hand", f"{side}_forearm", (s * 6, 27, 0))]
    x0, x1 = xs(5, 7)
    cube(f"{side}_upper_arm", f"{side}_arm", (x0, 43, -1), (x1, 58, 1), 150)
    # rounded shoulder: a deltoid cap over the top of the arm, stepped in at the very top
    x0, x1 = xs(4.6, 7.4); cube(f"{side}_deltoid", f"{side}_arm", (x0, 55, -1.6), (x1, 59, 1.6), 152)
    x0, x1 = xs(5, 7); cube(f"{side}_shoulder_cap", f"{side}_arm", (x0, 59, -1.1), (x1, 59.7, 1.1), 152)
    cube(f"{side}_forearm", f"{side}_forearm", (x0, 27, -1), (x1, 43, 1), 145)
    # not flat cubes: a thicker middle on the upper arm (biceps in front, triceps behind), a bony point at
    # the back of the elbow, and a forearm that's thicker near the elbow and thins out to the wrist
    x0, x1 = xs(4.8, 7.2); cube(f"{side}_biceps", f"{side}_arm", (x0, 47, -1.4), (x1, 54, 1.2), 152)
    x0, x1 = xs(5.3, 6.7); cube(f"{side}_elbow", f"{side}_forearm", (x0, 41.5, 1), (x1, 44.5, 1.6), 160)
    x0, x1 = xs(4.8, 7.2); cube(f"{side}_forearm_muscle", f"{side}_forearm", (x0, 35, -1.3), (x1, 42, 1.2), 147)
    # hand: wrist and a thin palm (the palm faces backward when the arm hangs, so laying it flat on the
    # ground is just bending the wrist forward), then four long fingers side by side and a thumb on the inner
    # edge, each in two segments on their own bones so they can bend; finger1 (index) is nearest the body
    x0, x1 = xs(5.1, 6.9); cube(f"{side}_wrist", f"{side}_hand", (x0, 26, -0.6), (x1, 27, 0.6), 155)
    x0, x1 = xs(4.5, 7.5); cube(f"{side}_palm", f"{side}_hand", (x0, 23.5, -0.7), (x1, 26, 0.7), 155)
    for i, (xc, l1, l2) in enumerate(((4.85, 2.1, 1.9), (5.6, 2.4, 2.2), (6.35, 2.3, 2.0), (7.1, 1.8, 1.6))):
        f = f"{side}_finger{i + 1}"; x0, x1 = xs(xc - 0.35, xc + 0.35)
        BONES += [(f, f"{side}_hand", (s * xc, 23.5, 0)), (f + "_tip", f, (s * xc, 23.5 - l1, 0))]
        cube(f, f, (x0, 23.5 - l1, -0.5), (x1, 23.5, 0.5), 155)
        cube(f + "_tip", f + "_tip", (x0, 23.5 - l1 - l2, -0.5), (x1, 23.5 - l1, 0.5), 158)
    x0, x1 = xs(3.8, 4.5)
    BONES += [(f"{side}_thumb", f"{side}_hand", (s * 4.2, 25.2, 0)), (f"{side}_thumb_tip", f"{side}_thumb", (s * 4.2, 23.4, 0))]
    cube(f"{side}_thumb", f"{side}_thumb", (x0, 23.4, -0.4), (x1, 25.2, 0.4), 155)
    cube(f"{side}_thumb_tip", f"{side}_thumb_tip", (x0, 21.8, -0.4), (x1, 23.4, 0.4), 158)
    # dog hind legs: hip, knee, hock; the "foot" is the long ankle bone, toes at its end
    BONES += [(f"{side}_leg", "hips", (s * 3.5, 38, 0)),
              (f"{side}_shin", f"{side}_leg", (s * 3.5, 18, 0)),
              (f"{side}_foot", f"{side}_shin", (s * 3.5, 2, 0))]
    tx0, tx1 = xs(2, 5)
    cube(f"{side}_thigh", f"{side}_leg", (tx0, 18, -2), (tx1, 38, 1), 150)      # long thigh
    sx0, sx1 = xs(2.5, 4.5)
    cube(f"{side}_shin", f"{side}_shin", (sx0, 2, -1.5), (sx1, 18, 0.5), 145)
    # human muscle on the dog legs: quadriceps down the front of the thigh, hamstring behind, a kneecap,
    # a calf behind the shin and a ridge down its front, the heel and Achilles at the back of the hock
    x0, x1 = xs(1.7, 5.3); cube(f"{side}_quadriceps", f"{side}_leg", (x0, 24, -2.6), (x1, 35, 0.8), 152)
    x0, x1 = xs(2.2, 4.8); cube(f"{side}_hamstring", f"{side}_leg", (x0, 21, 1), (x1, 34, 1.7), 148)
    x0, x1 = xs(2.8, 4.2); cube(f"{side}_kneecap", f"{side}_leg", (x0, 17.5, -2.5), (x1, 20.5, -1.9), 160)
    x0, x1 = xs(2.2, 4.8); cube(f"{side}_calf", f"{side}_shin", (x0, 9, 0.4), (x1, 16.5, 1.6), 147)
    x0, x1 = xs(3.2, 3.8); cube(f"{side}_shin_ridge", f"{side}_shin", (x0, 4, -1.9), (x1, 16, -1.5), 150)
    x0, x1 = xs(3.2, 3.8); cube(f"{side}_achilles", f"{side}_shin", (x0, 2, 0.5), (x1, 9, 1.1), 146)  # calf down to the heel
    # A big human foot (to carry its height), standing on its toes. The foot bone runs from the ankle (y=2)
    # down to the ball of the foot (y=-12); the top of the foot faces -Z, the sole +Z. A rounded heel at the
    # back of the ankle, ankle bones either side, a narrow foot that fans out wider toward the toes, an arch in the sole between the heel and the ball, and five toes in two
    # segments each on their own bones (big toe on the inside). Every sole stays at y=-14, so the foot
    # meets the ground as before.
    x0, x1 = xs(2.6, 4.4); cube(f"{side}_heel", f"{side}_foot", (x0, -1, -0.2), (x1, 2.6, 1.6), 142)
    x0, x1 = xs(2.9, 4.1); cube(f"{side}_heel_back", f"{side}_foot", (x0, -0.5, 1.6), (x1, 2.2, 2.0), 142)
    for name, (a, b) in (("inner_ankle", (2.1, 2.6)), ("outer_ankle", (4.4, 4.9))):
        x0, x1 = xs(a, b); cube(f"{side}_{name}", f"{side}_foot", (x0, 0, -1.0), (x1, 2.4, 0.4), 150)
    x0, x1 = xs(2.6, 4.4); cube(f"{side}_foot_upper", f"{side}_foot", (x0, -8, -1.3), (x1, 0, 0.3), 140)    # narrow
    x0, x1 = xs(2.0, 5.0); cube(f"{side}_foot_lower", f"{side}_foot", (x0, -12, -2.0), (x1, -8, 0.6), 141)  # fans out
    x0, x1 = xs(2.8, 4.2); cube(f"{side}_instep", f"{side}_foot", (x0, -9, -1.7), (x1, 0, -1.3), 144)       # top of the foot
    x0, x1 = xs(2.2, 4.8); cube(f"{side}_sole_pad", f"{side}_foot", (x0, -12, 0.6), (x1, -10, 1.1), 141)    # arch behind it
    x0, x1 = xs(1.6, 5.6); cube(f"{side}_ball", f"{side}_foot", (x0, -14, -3.4), (x1, -12, 0.9), 140)       # ball of the foot
    for i, (xa, xb, top, l1, l2) in enumerate(((1.7, 3.0, -12.6, 1.8, 1.6), (3.15, 3.75, -12.9, 1.6, 1.4),
                                               (3.9, 4.45, -12.95, 1.4, 1.2), (4.6, 5.1, -13.0, 1.1, 1.0),
                                               (5.2, 5.6, -13.2, 0.8, 0.7))):
        t = f"{side}_toe{i + 1}"; x0, x1 = xs(xa, xb); yc = (-14 + top) / 2; xc = s * (xa + xb) / 2
        BONES += [(t, f"{side}_foot", (xc, yc, -3.4)), (t + "_tip", t, (xc, yc, -3.4 - l1))]
        cube(t, t, (x0, -14, -3.4 - l1), (x1, top, -3.4), 140)
        cube(t + "_tip", t + "_tip", (x0, -14, -3.4 - l1 - l2), (x1, top - 0.1, -3.4 - l1), 143)

# Longer body, like the drawing: lift the chest and everything attached to it (arms, neck, head)
# and stretch the belly to fill the gap.
BELLY_EXTRA = 8
_parent = {n: par for n, par, _ in BONES}
def _in_chest(bone):
    while bone:
        if bone == "chest":
            return True
        bone = _parent[bone]
    return False
def _up(v):
    return (v[0], v[1] + BELLY_EXTRA, v[2])
BONES = [(n, par, _up(pv) if _in_chest(n) else pv) for n, par, pv in BONES]
CUBES = [(n, b, _up(a), _up(z), t, f) if _in_chest(b) else
         ((n, b, a, (z[0], z[1] + BELLY_EXTRA, z[2]), t, f) if n == "belly" else (n, b, a, z, t, f))
         for n, b, a, z, t, f in CUBES]

PARENT = {b[0]: b[1] for b in BONES}
PIVOT = {b[0]: b[2] for b in BONES}
CHILDREN = {b[0]: [c[0] for c in BONES if c[1] == b[0]] for b in BONES}

def mirror(pose):
    """Copy every right_* entry to left_* with Y and Z rotations mirrored."""
    out = dict(pose)
    for k, v in pose.items():
        if k.startswith("right_"):
            out["left_" + k[6:]] = (v[0], -v[1], -v[2])
    return out

def P(**kw):
    return {k: (v if isinstance(v, tuple) else (v, 0, 0)) for k, v in kw.items()}

# ---------------------------------------------------------------- poses (full group rotations)
# Standing: upright on dog legs (knee forward, hock back, up on its toes), slight stoop, arms hanging.
STAND = mirror(P(chest=-10, neck=5, head=5,
                 right_arm=(10, 0, -3), right_forearm=(-6, 0, 0),
                 right_leg=28, right_shin=-62, right_foot=40))
# All fours (from the annotated photo): the arms go straight down under the shoulders; the torso and chest
# rise in one line from the hip to the neck, with the head on top; the femur runs from the hip (the low point
# of the 'V') up and back to the knee, and the tibia (shin + ankle) drops straight down from the knee.
# The femur angle is solved below so the toes and the hands both reach the ground.
CHEST_ABS_REST, CHEST_ABS_MOVE = -68, -72
ARM_TILT_MOVE = 5     # degrees the arms lean forward when walking (hands land a little ahead of the shoulders)
REST4 = mirror(P(neck=48, head=20,
                 right_arm=(0, 0, -3), right_forearm=0, right_hand=0,
                 right_leg=(0, 0, -6), right_shin=0, right_foot=0))     # hind legs angled slightly outward
# Moving: same shape, limbs spread out to the sides for balance.
MOVE4 = mirror(P(neck=52, head=22,
                 right_arm=(0, 0, -9), right_forearm=(0, 0, 4), right_hand=0,
                 right_leg=(0, 0, -6), right_shin=0, right_foot=0))     # limbs only slightly spread

# ---------------------------------------------------------------- forward kinematics
def rot_zyx(v, r):
    """Rotate vector v by Euler degrees r=(x,y,z), order ZYX: X first, then Y, then Z (three.js)."""
    x, y, z = v
    ax, ay, az = (math.radians(a) for a in r)
    y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return (x, y, z)

def world(p, bone, pose, lift=0.0):
    while bone:
        r = pose.get(bone, (0, 0, 0)); pv = PIVOT[bone]
        d = rot_zyx((p[0] - pv[0], p[1] - pv[1], p[2] - pv[2]), r)
        o = pose.get("pos:" + bone, (0, 0, 0))                 # position keys move a bone within its parent
        p = (pv[0] + d[0] + o[0], pv[1] + d[1] + o[1], pv[2] + d[2] + o[2]); bone = PARENT[bone]
    return (p[0], p[1] + lift, p[2])

def corners(c):
    _, _, a, b, _, inf = c
    a = [a[i] - inf for i in range(3)]; b = [b[i] + inf for i in range(3)]
    return [(x, y, z) for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])]

def lowest(pose):
    return min(world(p, c[1], pose)[1] for c in CUBES for p in corners(c))

def highest(pose, lift):
    return max(world(p, c[1], pose, lift)[1] for c in CUBES for p in corners(c))

# ---------------------------------------------------------------- hands
# Rotations as 3x3 matrices, matching rot_zyx: R = Rz * Ry * Rx (X applied first). A bone's world rotation is
# its parents' times its own.
def rmat(r):
    a, b, c = (math.radians(v) for v in r)
    ca, sa, cb, sb, cc, sc = math.cos(a), math.sin(a), math.cos(b), math.sin(b), math.cos(c), math.sin(c)
    return [[cc * cb, cc * sb * sa - sc * ca, cc * sb * ca + sc * sa],
            [sc * cb, sc * sb * sa + cc * ca, sc * sb * ca - cc * sa],
            [-sb, cb * sa, cb * ca]]
def mmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def mvec(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))
def mT(m):
    return [[m[j][i] for j in range(3)] for i in range(3)]
def world_rot(bone, pose):
    chain = []
    while bone:
        chain.append(bone); bone = PARENT[bone]
    m = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for b in reversed(chain):
        m = mmul(m, rmat(pose.get(b, (0, 0, 0))))
    return m
def euler_zyx(m, near=(0, 0, 0)):
    """Blockbench angles (x, y, z) for a rotation matrix, each taken within 180 of `near` so keyframes
    don't spin the long way round between frames."""
    b = math.degrees(math.asin(max(-1.0, min(1.0, -m[2][0]))))
    if math.cos(math.radians(b)) < 0.035:
        # gimbal lock: only a - c (or a + c) is fixed, so keep x where it was and solve z from it
        a = near[0]
        if b > 0:
            c = a - math.degrees(math.atan2(m[0][1], m[0][2]))
        else:
            c = math.degrees(math.atan2(-m[0][1], -m[0][2])) - a
        c = c + 360 * round((near[2] - c) / 360)
        return (a, b, c)
    a = math.degrees(math.atan2(m[2][1], m[2][2])); c = math.degrees(math.atan2(m[1][0], m[0][0]))
    best = None
    for sol in ((a, b, c), (a + 180, 180 - b, c + 180)):        # the two angle sets for the same rotation
        sol = tuple(v + 360 * round((n - v) / 360) for v, n in zip(sol, near))
        d = sum(abs(v - n) for v, n in zip(sol, near))
        if best is None or d < best[0]:
            best = (d, sol)
    return best[1]

FINGER_BONES = [f"finger{i}" for i in range(1, 5)] + ["thumb"]
HAND_FLAT = 0.7          # wrist height when the palm lies flat on the ground (half the palm's thickness)
PALM_LENGTH = 3.5        # wrist to knuckles
PALM_RAISE = 3.0         # how far the heel of the hand can lift (wrist height gained) with the knuckles down
FINGER_REACH = 2.2       # length of the knuckle segment, which angles down to reach further
FINGER_MAX = 50.0        # ...but never more than this far down (no standing on the fingertips)
RELEASE = 4.0            # once the fingers can't reach, the hand peels off over this much more wrist height

def set_fingers(pose, side, curl, tip, thumb=None):
    """Bend every finger of one hand: curl at the knuckles, tip at the middle joint (degrees, + toward the
    palm). The palm faces +Z, so curling is a -X rotation on both hands."""
    thumb = thumb if thumb is not None else (curl * 0.5, tip * 0.5)
    for f in FINGER_BONES:
        c, t = thumb if f == "thumb" else (curl, tip)
        pose[f"{side}_{f}"] = (-c, 0, 0); pose[f"{side}_{f}_tip"] = (-t, 0, 0)

def flat_hand(side, pose, raise_deg=0.0, near=None):
    """Local rotation of the hand bone that lays the palm flat on the ground, fingers pointing where the body
    faces (splayed a little outward); raise_deg lifts the heel of the hand, pivoting on the knuckles."""
    yaw = pose.get("root", (0, 0, 0))[1] + (8 if side == "right" else -8)
    d = rot_zyx((0, 0, -1), (0, yaw, 0))
    p = math.radians(raise_deg)
    f = tuple(d[i] * math.cos(p) + (0, -1, 0)[i] * math.sin(p) for i in range(3))   # wrist -> knuckles
    n = tuple((0, -1, 0)[i] * math.cos(p) - d[i] * math.sin(p) for i in range(3))   # out of the palm
    y = tuple(-v for v in f)                                                         # the fingers point down local Y
    z = n                                                                            # the palm faces local +Z
    x = (y[1] * z[2] - y[2] * z[1], y[2] * z[0] - y[0] * z[2], y[0] * z[1] - y[1] * z[0])
    target = [[x[i], y[i], z[i]] for i in range(3)]
    return euler_zyx(mmul(mT(world_rot(f"{side}_forearm", pose)), target), near or pose.get(f"{side}_hand", (0, 0, 0)))

def plant_hands(pose):
    """Both palms flat on the ground, fingers flat (used to build the all-fours stances)."""
    for side in ("right", "left"):
        pose[f"{side}_hand"] = flat_hand(side, pose)
        set_fingers(pose, side, 0, 6, (0, 6))
    return pose

def pose_hand(pose, side, lift, mode, near=None):
    """What one hand does in a frame. Near the ground the palm lies flat; as the wrist rises the heel of the
    hand lifts while the fingers stay on the ground (push-off), then the fingers reach down from the
    knuckles, then the hand lets go and the fingers curl loosely while it swings. Reaching upward (climbing)
    the fingers hook to grip; lying on its side (or 'relaxed') the hand just rests half-curled. The
    behaviours blend into each other so nothing snaps."""
    base = pose.get(f"{side}_hand", (0, 0, 0))
    keys = [f"{side}_hand"] + [f"{side}_{f}{t}" for f in FINGER_BONES for t in ("", "_tip")]
    roll = abs(mvec(world_rot("chest", pose), (1, 0, 0))[1])            # 1: lying on its side
    flex = 8 if mode == "relaxed" else 8 * (1 - smooth(max(0.0, min(1.0, (roll - 0.55) / 0.25))))   # limp on its side
    relaxed = {f"{side}_hand": (base[0] - flex, base[1], base[2])}; set_fingers(relaxed, side, 20, 25)
    if f"fingers:{side}" in pose:                    # this frame asks for a fist, claws...
        fc, ft, fth = pose[f"fingers:{side}"]; set_fingers(relaxed, side, fc, ft, (fth, fth))
    grip = {f"{side}_hand": base}; set_fingers(grip, side, 70, 80, (35, 30))
    up = mvec(world_rot(f"{side}_forearm", pose), (0, -1, 0))[1]           # > 0: forearm pointing up
    w_grip = smooth(max(0.0, min(1.0, (up - 0.1) / 0.4)))
    w_rest = 1.0 if mode == "relaxed" else smooth(max(0.0, min(1.0, (roll - 0.55) / 0.25)))
    if w_grip < 1 and w_rest < 1:
        ground = ground_hand(pose, side, lift, near, relaxed)
    else:
        ground = relaxed
    out = {k: tuple(ground[k][i] + (relaxed[k][i] - ground[k][i]) * w_rest for i in range(3)) for k in keys}
    out = {k: tuple(out[k][i] + (grip[k][i] - out[k][i]) * w_grip for i in range(3)) for k in keys}
    pose.update(out)

def ground_hand(pose, side, lift, near, relaxed):
    def reach(hh):
        """The hand reaching hh below its flat-palm position: the heel of the hand lifts first (up to ~60
        degrees, knuckles on the ground), then the fingers angle down from the knuckles, fingertips flat."""
        r = math.degrees(math.asin(max(0.0, min(PALM_RAISE, hh)) / PALM_LENGTH))
        phi = math.degrees(math.asin(max(0.0, min(math.sin(math.radians(FINGER_MAX)), (hh - PALM_RAISE) / FINGER_REACH))))
        q = dict(pose); q[f"{side}_hand"] = flat_hand(side, pose, r, near)
        set_fingers(q, side, phi - r, 6 - phi, (phi - r, 6 - phi))      # the thumb lies flat with the fingers
        low = min(world(c_, c[1], q, lift)[1] for c in CUBES if c[1].startswith((f"{side}_hand", f"{side}_finger", f"{side}_thumb")) for c_ in corners(c))
        return q, low
    full = PALM_RAISE + FINGER_REACH * math.sin(math.radians(FINGER_MAX))
    q, low = reach(0.0)
    if low > 0.02:                                     # the flat hand is above the ground: reach down to it
        q_far, low_far = reach(full)
        if low_far > 0.02:                             # can't reach: letting go, the fingers curling as it lifts
            u = smooth(min(1.0, low_far / RELEASE))
            return {k: tuple(q_far[k][i] + (relaxed[k][i] - q_far[k][i]) * u for i in range(3)) for k in relaxed}
        lo, hi = 0.0, full                             # bisect the reach so the hand just touches the ground
        for _ in range(18):
            mid_ = (lo + hi) / 2
            q, low = reach(mid_)
            lo, hi = (mid_, hi) if low > 0 else (lo, mid_)
        q, low = reach(hi)
    return {k: q[k] for k in relaxed}

for _side in ("right", "left"):
    set_fingers(STAND, _side, 20, 25)          # at rest a real hand hangs half-curled

HAND_KEYS = [f"{sd}_{b}" for sd in ("right", "left") for b in ["hand"] + [f + t for f in FINGER_BONES for t in ("", "_tip")]]

def hand_pass(frames, mode="auto", max_rate=None, fingers_only=False):
    if mode == "keep":
        return frames
    """Pose the hands and fingers in every frame of an animation (see pose_hand). Where a hand would turn more
    than 15 degrees between two keyframes, in-between keys are added so the hand is posed properly along the
    way instead of the interpolation swinging it through the ground."""
    def posed(fr, prev):
        t, pose, lift = fr[:3]
        grounded = abs(lift + lowest(pose)) < 0.01
        if grounded and mode != "relaxed":
            # seat the body as if both palms were flat (the shortest a hand can be); a hand whose wrist ends
            # up higher then reaches down to the ground, so a reaching hand never props the body up
            flat = dict(pose)
            for side in ("right", "left"):
                flat[f"{side}_hand"] = flat_hand(side, flat, 0.0, prev and prev[1][f"{side}_hand"]); set_fingers(flat, side, 0, 6, (0, 6))
            lift = -lowest(flat)
        p = dict(pose)
        for side in ("right", "left"):
            pose_hand(p, side, lift, mode, prev and prev[1][f"{side}_hand"])
        if grounded and mode == "relaxed":
            lift = -lowest(p)
        elif grounded:
            lift = max(lift, -lowest(p))             # a resting (not planted) hand never ends up in the ground
        return (t, p, lift) + tuple(fr[3:])
    def turn(a_, b_):
        return max(abs(a_[1][k][j] - b_[1][k][j]) for k in HAND_KEYS for j in range(3))
    def between(a_, b_, u, pa, pb):
        pose = blend(a_[1], b_[1], u)
        ga = abs(a_[2] + lowest(a_[1])) < 0.01 and abs(b_[2] + lowest(b_[1])) < 0.01
        # off the ground, follow the body heights the neighbouring keys ended up with after posing the hands
        lift = -lowest(pose) if ga else pa[2] + (pb[2] - pa[2]) * u
        return (a_[0] + (b_[0] - a_[0]) * u, pose, lift) + tuple(x + (y - x) * u for x, y in zip(a_[3:], b_[3:]))
    def refine(ra, rb, pa, pb, depth):
        """Keys between two raw frames: split in half while a hand turns more than 12 degrees between keys."""
        if depth == 0 or turn(pa, pb) <= 12:
            return [pb]
        rm = between(ra, rb, 0.5, pa, pb); pm = posed(rm, pa)
        return refine(ra, rm, pa, pm, depth - 1) + refine(rm, rb, pm, pb, depth - 1)
    out = [posed(frames[0], None)]
    for ra, rb in zip(frames, frames[1:]):
        out += refine(ra, rb, out[-1], posed(rb, out[-1]), 5)
    if max_rate:
        # limit how fast a hand can turn (degrees per second), so a shaking arm crossing in and out of reach
        # of the ground flails instead of snapping between planted and lifted
        for i in range(1, len(out)):
            dt = out[i][0] - out[i - 1][0]
            lim = max_rate * dt
            p = dict(out[i][1])
            for k in HAND_KEYS:
                if fingers_only and k.endswith("_hand"):
                    continue
                prev, cur = out[i - 1][1][k], p[k]
                p[k] = tuple(pv + max(-lim, min(lim, cv - pv)) for pv, cv in zip(prev, cur))
            out[i] = (out[i][0], p) + tuple(out[i][2:])
    return out

def stand_on_fours(pose, chest_abs, arm_tilt=0.0):
    """Torso and chest in one straight line at chest_abs, arms straight down with the palms flat on the
    ground, then swing the femur up and back from the hip until the straight-down tibia puts the toes on the
    ground level with the hands."""
    def with_femur(f):
        p = dict(pose); p["hips"] = (chest_abs, 0, 0); p["chest"] = (0, 0, 0)
        for side in ("right", "left"):
            a, l = pose[f"{side}_arm"], pose[f"{side}_leg"]
            p[f"{side}_arm"] = (-chest_abs + arm_tilt, a[1], a[2])   # arm straight down (+ forward tilt)
            p[f"{side}_leg"] = (f - chest_abs, l[1], l[2])     # femur at absolute angle f
            p[f"{side}_shin"] = (-f, 0, 0)                     # tibia straight down from the knee
        return plant_hands(p)                                  # palms flat on the ground
    def gap(p):
        low = lambda part: min(world(q, c[1], p)[1] for c in CUBES if c[1].endswith(part) for q in corners(c))
        return low("foot") - low("hand")                       # >0: toes higher than hands
    lo, hi = -175.0, -91.0          # femur pointing back: from nearly straight up to just above flat
    for _ in range(60):
        mid = (lo + hi) / 2
        # swinging the femur higher lifts the knee and the toes: toes too high (gap > 0) -> lower it
        lo, hi = (mid, hi) if gap(with_femur(mid)) > 0 else (lo, mid)
    return with_femur((lo + hi) / 2)

REST4 = stand_on_fours(REST4, CHEST_ABS_REST)
MOVE4 = stand_on_fours(MOVE4, CHEST_ABS_MOVE, ARM_TILT_MOVE)

# ---------------------------------------------------------------- preview renderer (filled boxes, painter's order)
FACES = [(0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)]
def render(pose, lift, view, size=(300, 420), scale=4.0, title=""):
    img = Image.new("RGB", size, (20, 20, 20)); dr = ImageDraw.Draw(img)
    cam = {"front": lambda p: (-p[0], p[1], p[2]),           # looking at the face
           "side": lambda p: (p[2], p[1], -p[0]),            # from its right; face to the left
           "three": lambda p: rot_zyx((-p[0], p[1], p[2]), (0, 0, 0)) if False else
                    ((-p[0]) * 0.8 + p[2] * 0.6, p[1], p[2] * 0.8 + p[0] * 0.6)}[view]
    floor = size[1] - 30
    polys = []
    for c in CUBES:
        pts = [cam(world(p, c[1], pose, lift)) for p in corners(c)]
        for f in FACES:
            q = [pts[i] for i in f]
            depth = sum(v[2] for v in q) / 4
            polys.append((depth, [(size[0] / 2 + v[0] * scale, floor - v[1] * scale) for v in q], c[4]))
    for depth, q, tone in sorted(polys, key=lambda t: -t[0]):
        shade = int(tone * 0.85)
        dr.polygon(q, fill=(shade, shade, shade), outline=(max(0, tone - 70),) * 3)
    dr.line([0, floor, size[0], floor], fill=(70, 70, 70))
    dr.line([0, floor - 32 * scale, size[0], floor - 32 * scale], fill=(110, 45, 45))
    dr.text((6, 6), title, fill=(210, 210, 210))
    dr.text((6, floor - 32 * scale - 13), "2 blocks", fill=(160, 80, 80))
    return img

# ---------------------------------------------------------------- normalise: stand with the toes on the ground
rest_low = lowest(STAND)
REST_LIFT = -rest_low          # the whole rig sits this far up so standing toes touch y=0

def sheet(path):
    rows = []
    for name, pose in (("STANDING", STAND), ("RESTING ON ALL FOURS", REST4), ("MOVING ON ALL FOURS", MOVE4)):
        lift = REST_LIFT if pose is STAND else -lowest(pose)
        h = highest(pose, lift) / 16
        rows.append([render(pose, lift, v, title=f"{name} - {v} - {h:.1f} blocks tall") for v in ("front", "side", "three")])
    W = sum(i.width for i in rows[0]) + 10 * 4; H = sum(r[0].height for r in rows) + 10 * 4
    out = Image.new("RGB", (W, H), (8, 8, 8)); y = 10
    for r in rows:
        x = 10
        for i in r: out.paste(i, (x, y)); x += i.width + 10
        y += r[0].height + 10
    out.save(path)

# ---------------------------------------------------------------- animations
def walk_frame(t):
    """Moving on all fours: diagonal pairs (right arm + left leg, then left arm + right leg)."""
    ph = 2 * math.pi * t; sw = math.sin(ph); up_a = max(0.0, math.cos(ph)); up_b = max(0.0, -math.cos(ph))
    pose = {k: list(v) for k, v in MOVE4.items()}
    def add(b, dx=0.0, dz=0.0):
        pose[b][0] += dx; pose[b][2] += dz
    add("right_arm", 18 * sw); add("left_leg", -16 * sw)
    add("left_arm", -18 * sw); add("right_leg", 16 * sw)
    add("right_forearm", 22 * up_a); add("left_shin", -22 * up_a)      # swinging limbs lift
    add("left_forearm", 22 * up_b); add("right_shin", -22 * up_b)
    add("head", 4 * math.sin(2 * ph)); add("hips", 2 * math.sin(2 * ph))
    pose = {k: tuple(v) for k, v in pose.items()}
    return pose, -lowest(pose)

def _editable(pose):
    return {k: list(v) for k, v in pose.items()}

def _add(pose, bone, dx=0.0, dy=0.0, dz=0.0):
    v = pose.setdefault(bone, [0.0, 0.0, 0.0]); v[0] += dx; v[1] += dy; v[2] += dz

def _frozen(pose):
    return {k: tuple(v) for k, v in pose.items()}

def rest4_frame(t):
    """Resting on all fours, breathing: the ribs rise and fall over one slow cycle while the arms counter-rotate
    to stay straight down, so the hands and feet stay planted."""
    b = math.sin(2 * math.pi * t)
    pose = _editable(REST4)
    _add(pose, "torso", 0.8 * b); _add(pose, "chest", 1.6 * b)
    _add(pose, "neck", -1.2 * b); _add(pose, "head", -0.8 * b)
    for side in ("right", "left"):
        _add(pose, f"{side}_arm", -2.4 * b)
    pose = _frozen(pose)
    return pose, -lowest(pose)

def rest_standing_frame(t):
    """Standing still, breathing the same way, with the long arms swaying faintly out of step."""
    ph = 2 * math.pi * t; b = math.sin(ph)
    pose = _editable(STAND)
    _add(pose, "torso", 0.6 * b); _add(pose, "chest", 1.4 * b)
    _add(pose, "neck", -1.0 * b); _add(pose, "head", -0.8 * b)
    _add(pose, "right_arm", 1.4 * math.sin(ph + 0.6)); _add(pose, "left_arm", 1.4 * math.sin(ph + 1.1))
    _add(pose, "right_forearm", 0.9 * math.sin(ph + 1.2)); _add(pose, "left_forearm", 0.9 * math.sin(ph + 1.7))
    return _frozen(pose), REST_LIFT

def walk_standing_frame(t):
    """Walking upright on its dog legs: each thigh swings from the hip, and the knee and ankle fold while that
    leg swings forward; the arms swing opposite their legs; the chest twists a little against the hips."""
    ph = 2 * math.pi * t
    pose = _editable(STAND)
    for side, offset in (("right", 0.0), ("left", math.pi)):
        q = ph + offset; sw = math.sin(q); swinging = max(0.0, math.cos(q))
        _add(pose, f"{side}_leg", 24 * sw)
        _add(pose, f"{side}_shin", -22 * swinging)
        _add(pose, f"{side}_foot", 14 * swinging)
        _add(pose, f"{side}_arm", -16 * sw)
        _add(pose, f"{side}_forearm", 6 * max(0.0, -sw))
    _add(pose, "chest", dy=4 * math.sin(ph)); _add(pose, "hips", dy=-3 * math.sin(ph))
    _add(pose, "head", dy=-2 * math.sin(ph)); _add(pose, "torso", 1.5 * math.sin(2 * ph))
    pose = _frozen(pose)
    return pose, -lowest(pose)

# Alert, standing: straightens up out of its stoop, head raised, arms held out a little and tensed with the
# hands lifted, knees bent ready to move. Then it holds perfectly still.
ALERT = mirror(P(chest=-2, neck=0, head=4,
                 right_arm=(6, 0, -10), right_forearm=(22, 0, 0),
                 right_leg=34, right_shin=-72, right_foot=46))
# Alert on all fours: chest and head lift high while all four limbs stay planted.
ALERT4 = stand_on_fours(mirror(P(neck=58, head=3,
                                 right_arm=(0, 0, -3), right_forearm=0, right_hand=0,
                                 right_leg=(0, 0, -6), right_shin=0, right_foot=0)), -56)

def look_around(base, lift, chest_share=0.2):
    """Searching for a sound, starting and ending on the frozen alert pose: snap the head (and a little of the
    chest) to one side and hold still listening with a dog-like head tilt, snap to the other side and hold,
    a short glance up, then settle back. Only the upper body moves, so the feet stay planted. On all fours
    the chest can't turn (it would swing the planted arms), so chest_share is 0 and the neck takes it."""
    def look(turn, tilt=0.0, up=0.0):
        pose = _editable(base)
        _add(pose, "chest", dy=chest_share * turn)         # chest follows a little (standing only)
        _add(pose, "neck", dy=(0.55 - chest_share) * turn)
        _add(pose, "head", up, 0.45 * turn, tilt)          # head does most of the turn, tilts while listening
        return _frozen(pose)
    centre = _frozen(_editable(base))
    keys = [(0.0, centre),
            (0.5, look(55, tilt=8)), (1.4, look(55, tilt=10)),       # turn to one side, hold, listening
            (1.7, look(-60, tilt=-8)), (2.7, look(-60, tilt=-10)),   # snap to the other side, hold
            (3.2, look(-10, up=-12)),                                 # quick glance up toward the middle
            (4.0, centre)]
    return [(t, p, lift) for t, p in keys]

# Galloping: lower and longer than the walk, chest pushed forward, arms reaching ahead.
RUN4 = stand_on_fours(mirror(P(neck=60, head=14,
                               right_arm=(0, 0, -5), right_forearm=0, right_hand=0,
                               right_leg=(0, 0, -4), right_shin=0, right_foot=0)), -80, arm_tilt=12)

def run_frame(t):
    """A gallop: the front legs reach forward together (one just after the other), then the hind legs drive
    together half a cycle later; the back flexes and stretches between them; brief lift off the ground."""
    ph = 2 * math.pi * t
    pose = _editable(RUN4)
    for side, lag in (("right", 0.0), ("left", 0.55)):
        front = ph - lag; back = ph + math.pi - lag
        _add(pose, f"{side}_arm", 32 * math.sin(front))
        _add(pose, f"{side}_forearm", 28 * max(0.0, math.cos(front)))        # tuck the forearm as it swings forward
        _add(pose, f"{side}_leg", 30 * math.sin(back))
        _add(pose, f"{side}_shin", -30 * max(0.0, math.cos(back)))           # fold the hind leg as it swings forward
    _add(pose, "torso", 6 * math.sin(ph))                                     # back flexes and stretches
    _add(pose, "chest", -4 * math.sin(ph))
    _add(pose, "head", -3 * math.sin(ph))                                     # head stays steady on target
    pose = _frozen(pose)
    return pose, -lowest(pose) + 1.5 * max(0.0, math.sin(2 * ph))           # short airborne moments

# Panicked flee: the gallop, but lower and flatter with the head ducked, and everything more frantic.
FLEE4 = stand_on_fours(mirror(P(neck=50, head=24,
                                right_arm=(0, 0, -5), right_forearm=0, right_hand=0,
                                right_leg=(0, 0, -4), right_shin=0, right_foot=0)), -84, arm_tilt=16)

def flee_frame(t):
    """Fleeing: a frantic gallop. Longer, wilder strides with the front legs landing almost together, a deeper
    flex and stretch of the back, bigger airborne moments, a jerky wobble, and the head jerking side to side
    as if glancing back at what scared it."""
    ph = 2 * math.pi * t
    pose = _editable(FLEE4)
    for side, lag in (("right", 0.0), ("left", 0.3)):
        front = ph - lag; back = ph + math.pi - lag
        _add(pose, f"{side}_arm", 40 * math.sin(front))
        _add(pose, f"{side}_forearm", 34 * max(0.0, math.cos(front)))
        _add(pose, f"{side}_leg", 38 * math.sin(back))
        _add(pose, f"{side}_shin", -36 * max(0.0, math.cos(back)))
    _add(pose, "torso", 9 * math.sin(ph)); _add(pose, "chest", -6 * math.sin(ph))
    _add(pose, "hips", dz=3 * math.sin(3 * ph)); _add(pose, "chest", dz=-3 * math.sin(3 * ph))      # jerky wobble
    _add(pose, "head", -4 * math.sin(ph), dy=16 * math.sin(ph) + 7 * math.sin(3 * ph))             # glancing back
    pose = _frozen(pose)
    return pose, -lowest(pose) + 2.2 * max(0.0, math.sin(2 * ph))

# ---------------------------------------------------------------- movement set
def blend(a, b, u):
    """Joint-by-joint blend between two poses (u = 0..1)."""
    out = {}
    for k in set(a) | set(b):
        va, vb = a.get(k, (0, 0, 0)), b.get(k, (0, 0, 0))
        out[k] = tuple(va[i] + (vb[i] - va[i]) * u for i in range(3))
    return out

def smooth(u):
    return u * u * (3 - 2 * u)

def transition(a, b, length, steps=8, through=None):
    """A one-shot move from pose a to pose b (optionally via a middle pose), keeping whatever is lowest
    on the ground at every step."""
    frames = []
    for i in range(steps + 1):
        u = smooth(i / steps)
        if through is None:
            p = blend(a, b, u)
        else:
            p = blend(a, through, u * 2) if u < 0.5 else blend(through, b, (u - 0.5) * 2)
        frames.append((i / steps * length, p, -lowest(p)))
    return frames

# Standing -> crawling passes through a deep forward bend with the hands reaching for the ground.
BEND = mirror(P(hips=-35, torso=-12, chest=-14, neck=30, head=20,
                right_arm=(55, 0, -4), right_forearm=-8, right_hand=20,
                right_leg=(20, 0, -3), right_shin=-40, right_foot=20))

# Climbing a pillar (the climb surface is in front of it): body upright, leaning back a touch, head tilted
# up; arms nearly straight up the wall; legs about one block apart (in game) so it fits a 1-wide gap, knees
# bent so the feet press the wall.
CLIMB = mirror(P(hips=4, neck=-12, head=26,
                 right_arm=(158, 0, 14), right_forearm=0, right_hand=0,   # +Z: outward for a raised arm
                 right_leg=(28, 0, -7), right_shin=(-55, 0, 2), right_foot=40))

def climb_frame(t, down=False):
    """Hand over hand: one arm reaches high while the other pulls down (elbow bending), and the knee on the
    other side drives up the wall. Climbing down runs the same cycle backwards with the head looking down."""
    ph = 2 * math.pi * (1 - t if down else t)
    pose = _editable(CLIMB)
    for side, off in (("right", 0.0), ("left", math.pi)):
        q = ph + off; pull = (1 - math.cos(q)) / 2              # 0 = reaching high, 1 = pulled down
        sign = -1 if side == "right" else 1                      # outward on this side
        # pulling: the upper arm drops and the elbow swings out sideways, the forearm stays against the wall
        _add(pose, f"{side}_arm", -26 * pull, 0, -sign * 16 * pull)   # raised arm: outward is the other way
        _add(pose, f"{side}_forearm", 50 * pull)
        # the opposite knee drives up (only slightly outward, to stay within a one-block gap)
        drive = 1 - pull
        _add(pose, f"{side}_leg", 6 * drive, 0, sign * 3 * drive)
        _add(pose, f"{side}_shin", -10 * drive)
    _add(pose, "chest", dz=3 * math.sin(ph)); _add(pose, "hips", dz=-2 * math.sin(ph))
    if down:
        _add(pose, "head", -40); _add(pose, "neck", -10)       # looking down at where it's going
    return _frozen(pose), REST_LIFT + 2.0 * math.sin(2 * ph)

def ledge_frames():
    """Climbing over the top. The game puts it on the ledge when this starts, so the body begins hanging
    below the edge (root lowered and pushed back) and ends on all fours on top with no offset."""
    reach = dict(CLIMB); reach.update(mirror(P(right_arm=(164, 0, 12), right_forearm=0, right_hand=-20)))
    pull = mirror(P(hips=-25, chest=-25, neck=20, head=15,
                    right_arm=(95, 0, -10), right_forearm=-95, right_hand=40,
                    right_leg=(40, 0, -6), right_shin=-80, right_foot=30))
    knee = mirror(P(hips=-55, chest=-10, neck=40, head=18,
                    right_arm=(60, 0, -5), right_forearm=-20, right_hand=60,
                    right_leg=(70, 0, -6), right_shin=-120, right_foot=40))
    knee["left_leg"] = (20, 0, 6); knee["left_shin"] = (-40, 0, 0)       # one knee up first, the other trails
    # The ledge edge sits LEDGE_EDGE behind the entity's final spot, so it ends with all four feet on top.
    LEDGE_EDGE = 24.0
    ground = -lowest(REST4)
    hands_up = max(world(q, c[1], reach)[1] for c in CUBES if c[1].endswith("hand") for q in corners(c))
    wall_face = min(world(q, c[1], reach)[2] for c in CUBES if c[1].endswith("hand") for q in corners(c))
    # the leading (right) knee rests on top; the trailing leg still hangs over the edge, so ignore it
    knee_low = min(min(world(q, c[1], knee)[1] for c in CUBES if c[1] in ("right_leg", "right_shin") for q in corners(c)),
                   min(world(q, c[1], knee)[1] for c in CUBES if c[1].endswith("hand") for q in corners(c)))
    return [(0.0, reach, -hands_up, LEDGE_EDGE - wall_face),          # hanging on the face, hands gripping the edge
            (0.35, pull, -hands_up * 0.55, LEDGE_EDGE - 2.0),         # chest hauled up to the edge
            (0.7, knee, -knee_low, LEDGE_EDGE * 0.45),                # the leading knee resting on the top surface
            (1.0, REST4, ground, 0.0)]                                # on all fours on top

# Leap (from all fours): gather low, launch stretched out, reach forward in the air, absorb the landing.
def leap_frames():
    gather, _ = run_frame(0.75)
    gather = _editable(gather)
    for side in ("right", "left"):
        _add(gather, f"{side}_forearm", -20); _add(gather, f"{side}_shin", 25)
    gather = _frozen(gather)
    launch, _ = run_frame(0.25)
    launch = _editable(launch)
    _add(launch, "neck", -10)
    for side in ("right", "left"):
        _add(launch, f"{side}_arm", 30); _add(launch, f"{side}_leg", -25)
    launch = _frozen(launch)
    reach = _editable(launch)
    for side in ("right", "left"):
        _add(reach, f"{side}_arm", -25); _add(reach, f"{side}_forearm", 15); _add(reach, f"{side}_leg", 15)
    reach = _frozen(reach)
    land = _editable(RUN4)
    for side in ("right", "left"):
        _add(land, f"{side}_arm", 10); _add(land, f"{side}_forearm", -30)
    land = _frozen(land)
    # In the air the body stays at running height (the game's physics does the actual rise), so the limbs
    # read as stretched out mid-leap rather than the body dropping to the ground.
    air = -lowest(RUN4) + 6.0
    return [(0.0, RUN4, -lowest(RUN4)), (0.15, gather, -lowest(gather)), (0.3, launch, air),
            (0.5, reach, air), (0.65, land, -lowest(land)), (0.85, RUN4, -lowest(RUN4))]

# ---------------------------------------------------------------- belly crawl
def _sub(a, b): return tuple(a[i] - b[i] for i in range(3))
def _addv(a, b): return tuple(a[i] + b[i] for i in range(3))
def _mul(a, k): return tuple(v * k for v in a)
def _dot(a, b): return sum(a[i] * b[i] for i in range(3))
def _norm(a):
    l = math.sqrt(_dot(a, a)) or 1.0
    return _mul(a, 1 / l)

def ik_joint(s, t, a, b, pole):
    """Where the middle joint of a two-bone limb goes: the limb runs from s (lengths a then b) to t, bending
    toward pole."""
    d = _sub(t, s); dist = min(math.sqrt(_dot(d, d)), a + b - 1e-3); u = _norm(d)
    x = (a * a - b * b + dist * dist) / (2 * dist); h = math.sqrt(max(0.0, a * a - x * x))
    p = _norm(_sub(pole, _mul(u, _dot(pole, u))))
    return _addv(_addv(s, _mul(u, x)), _mul(p, h))

def aim(pose, bone, d):
    """Turns a limb bone (which hangs along -Y at rest) so it points along world direction d."""
    loc = mvec(mT(world_rot(PARENT[bone], pose)), _norm(d))
    a = math.degrees(math.asin(max(-1.0, min(1.0, -loc[2])))); c = math.degrees(math.atan2(loc[0], -loc[1]))
    pose[bone] = (a, 0.0, c)

# Crawling on its belly (to get through 1-block-high gaps and 1x1 holes; under 0.9 blocks tall in game): body flat
# on the ground, head low with the face looking ahead, elbows out to the sides dragging it along, hind legs
# splayed out behind like a lizard's, pushing. The hands and feet are placed on the ground and the elbows and
# knees solved to reach them (ik_joint), diagonal pairs moving together, while the spine snakes side to side.
PRONE = mirror(P(hips=-90, neck=-4, head=82))
PRONE_STRIDE = 16.0            # how far each hand and foot travels back along the ground per cycle (px)
PRONE_STANCE = 0.6             # share of the cycle a hand or foot is planted

def body_lift(pose):
    """Height that puts the lowest point of the body (not the limbs) on the ground."""
    return -min(world(q, c[1], pose)[1] for c in CUBES if c[1] in ("hips", "torso", "chest", "neck", "head", "jaw")
                for q in corners(c))

def limb_track(u, front, back, lift_h):
    """Position along the stride (z) and height off the ground for a hand or foot at cycle phase u."""
    if u < PRONE_STANCE:
        k = u / PRONE_STANCE
        return front + (back - front) * k, 0.0
    k = (u - PRONE_STANCE) / (1 - PRONE_STANCE)
    k2 = smooth(k)
    return back + (front - back) * k2, lift_h * math.sin(math.pi * k)

def prone_frame(t):
    """One moment (t = 0..1) of the belly crawl."""
    pose = _editable(PRONE)
    ph = 2 * math.pi * t
    sway = math.sin(ph)
    _add(pose, "hips", 0, 5 * sway, 0)             # the body snakes side to side
    _add(pose, "chest", 0, 0, -8 * sway)
    _add(pose, "neck", 0, 0, 5 * sway)
    _add(pose, "head", 0, 4 * sway, 0)
    pose = _frozen(pose)
    lift = body_lift(pose) + 0.5
    for side, sx, off in (("right", -1, 0.0), ("left", 1, 0.5)):
        # arm: hand planted ahead of the shoulder, dragging the body forward, then reaching ahead again
        s = world(PIVOT[f"{side}_arm"], f"{side}_arm", pose, lift)
        z, y = limb_track((t + off) % 1.0, s[2] - 18, s[2] - 18 + PRONE_STRIDE, 5.0)
        hand = (s[0] + sx * 12, HAND_FLAT + y, z)
        e = ik_joint(s, hand, 15, 16, (sx, 0.45, 0.3))
        aim(pose, f"{side}_arm", _sub(e, s))
        aim(pose, f"{side}_forearm", _sub(hand, e))
        # leg (diagonal pair: this side's leg moves with the other arm): the knee out to the side and
        # forward like a frog's, pushing the foot back along the ground
        h = world(PIVOT[f"{side}_leg"], f"{side}_leg", pose, lift)
        z, y = limb_track((t + off + 0.5) % 1.0, h[2] + 12, h[2] + 12 + PRONE_STRIDE, 4.0)
        ankle_y = 2.5 + y
        for _ in range(3):
            ankle = (h[0] + sx * 17, ankle_y, z)
            k = ik_joint(h, ankle, 20, 16, (sx, 0.3, -0.5))
            aim(pose, f"{side}_leg", _sub(k, h))
            aim(pose, f"{side}_shin", _sub(ankle, k))
            # the foot (it stands on its toes) lies back along the ground, toes pointing back and a little out
            aim(pose, f"{side}_foot", (sx * 0.35, -0.25, 1.0))
            dip = min(world(q, c[1], pose, lift)[1] for c in CUBES
                      if c[1] == f"{side}_foot" or c[1].startswith(f"{side}_toe") for q in corners(c))
            ankle_y += y - dip
            if abs(dip - y) < 0.05:
                break
    return pose, -lowest(pose)

# Swimming: a dog paddle, body flat at the surface, head held up out of the liquid.
SWIM = mirror(P(hips=-86, neck=64, head=18,
                right_arm=(126, 0, -12), right_forearm=-30, right_hand=40,
                right_leg=(40, 0, -8), right_shin=-12, right_foot=20))   # hind legs angled down and back

def swim_frame(t):
    ph = 2 * math.pi * t
    pose = _editable(SWIM)
    for side, off in (("right", 0.0), ("left", math.pi)):
        q = ph + off
        _add(pose, f"{side}_arm", 30 * math.sin(q))                       # paddle forward and back...
        _add(pose, f"{side}_forearm", -30 * max(0.0, math.cos(q)))        # ...folding on the return stroke
        _add(pose, f"{side}_leg", 16 * math.sin(q + math.pi))             # kick down and back
        _add(pose, f"{side}_shin", -16 * max(0.0, math.cos(q + math.pi)))
    _add(pose, "head", 3 * math.sin(2 * ph))
    pose = _frozen(pose)
    return pose, -lowest(SWIM) + 1.5 * math.sin(2 * ph)

# ---------------------------------------------------------------- standing versions of the four-legged moves
def _loop_spline(keys, u):
    """Smooth looping curve through (position 0..1, value) keys: a closed Catmull-Rom spline, so motion
    flows through each key without stopping (no stutter) and wraps seamlessly at the end of the cycle."""
    n = len(keys); u %= 1.0
    i = max(j for j in range(n) if keys[j][0] <= u) if u >= keys[0][0] else n - 1
    x1, v1 = keys[i]; x2, v2 = keys[(i + 1) % n]
    v0 = keys[(i - 1) % n][1]; v3 = keys[(i + 2) % n][1]
    span = (x2 - x1) % 1.0 or 1.0
    s_ = ((u - x1) % 1.0) / span
    return 0.5 * ((2 * v1) + (-v0 + v2) * s_ + (2 * v0 - 5 * v1 + 4 * v2 - v3) * s_ ** 2 + (-v0 + 3 * v1 - 3 * v2 + v3) * s_ ** 3)

RUN_STANCE = 0.28        # share of the stride each foot spends on the ground; the rest is swing, so there's flight
RUN_FLIGHT_HEIGHT = 5.0  # how high the body bounces between steps (Blockbench pixels)
RUN_FOOT_TRAVEL = 0.7    # how much of the stride's length the planted foot covers (so the leg can reach it)
RUN_ANKLE_ROLL = -10.0    # ankle angle at push-off (negative = extended back, pushing off the ground)
RUN_TOE_CLEARANCE = 0.6  # ball of the foot held this far up so the toe edge never dips into the ground
RUN_CROUCH = 10.0         # how much lower the body runs than the plain pose, so the legs reach the ground
# Leg offsets from the standing pose over one stride: planted (0 -> RUN_STANCE) sweeping from under the body to
# pushed back, then swinging: heel kicks up behind, knee drives forward and up, leg reaches out to land.
RUN_THIGH = [(0.0, 30), (0.28, -38), (0.45, -10), (0.75, 52), (0.92, 36)]
RUN_SHIN = [(0.0, -4), (0.28, 34), (0.45, -70), (0.75, -40), (0.92, -8)]
RUN_FOOT = [(0.0, 0), (0.28, -22), (0.45, 34), (0.75, 20), (0.92, 4)]

def _run_upper(t):
    """Everything except the legs: lean, shoulder twist against the hips, arms pumping at the sides."""
    ph = 2 * math.pi * t
    pose = _editable(STAND)
    _add(pose, "hips", -16); _add(pose, "chest", -2); _add(pose, "neck", 10); _add(pose, "head", 6)
    for side, offset in (("right", 0.0), ("left", 0.5)):
        q = 2 * math.pi * ((t + offset) % 1.0)
        _add(pose, f"{side}_arm", -14 - 36 * math.cos(q - 0.6))        # arm swings opposite its own leg, close at the side
        _add(pose, f"{side}_forearm", 102 + 18 * math.cos(q - 0.6))    # elbow bent tight; the forward hand comes up to the chest
    _add(pose, "chest", 2 * math.sin(2 * ph), 13 * math.cos(ph - 0.6))   # shoulder twist
    _add(pose, "hips", dy=-9 * math.cos(ph - 0.6))                         # hips twist the other way
    _add(pose, "head", 0, -8 * math.cos(ph - 0.6))                         # head stays pointed ahead
    return pose

def _run_spline_pose(t):
    """The earlier joint-space run (legs straight from the splines); used to size the stride."""
    pose = _run_upper(t)
    for side, offset in (("right", 0.0), ("left", 0.5)):
        u = (t + offset) % 1.0
        _add(pose, f"{side}_leg", _loop_spline(RUN_THIGH, u))
        _add(pose, f"{side}_shin", _loop_spline(RUN_SHIN, u))
        _add(pose, f"{side}_foot", _loop_spline(RUN_FOOT, u))
    return _frozen(pose)

def _toe(side, pose, lift):
    """The ball of the foot: bottom middle of the ball cube (rest coordinates y -14, z around -2)."""
    x = -3.5 if side == "right" else 3.5
    return world((x, -14.0, -2.0), f"{side}_foot", pose, lift)

def _run_bounce(t):
    """Body height offset: two smooth bounces per stride, lowest mid-stance, highest mid-flight."""
    mid_flight = (RUN_STANCE + 0.5) / 2
    return RUN_FLIGHT_HEIGHT * 0.5 * (1 + math.cos(4 * math.pi * (t - mid_flight)))

def _solve_leg(side, pose, lift, target_z, foot_abs, guess=None):
    """Inverse kinematics: thigh and shin angles that put the ball of the foot on the ground at target_z,
    with the ankle held at foot_abs (measured from straight down)."""
    chain = sum(pose.get(b, (0, 0, 0))[0] for b in ("root", "hips"))
    th, sh = guess if guess else (pose[f"{side}_leg"][0], pose[f"{side}_shin"][0])
    def place(th, sh):
        q = dict(pose)
        q[f"{side}_leg"] = (th,) + tuple(pose[f"{side}_leg"][1:])
        q[f"{side}_shin"] = (sh,) + tuple(pose[f"{side}_shin"][1:])
        q[f"{side}_foot"] = (foot_abs - chain - th - sh,) + tuple(pose[f"{side}_foot"][1:])
        v = _toe(side, q, lift)
        return q, (v[1] - RUN_TOE_CLEARANCE, v[2] - target_z)
    for _ in range(40):
        q, r = place(th, sh)
        if abs(r[0]) < 1e-5 and abs(r[1]) < 1e-5:
            break
        e = 0.01
        _, a = place(th + e, sh); _, b = place(th, sh + e)
        j11, j21 = (a[0] - r[0]) / e, (a[1] - r[1]) / e
        j12, j22 = (b[0] - r[0]) / e, (b[1] - r[1]) / e
        det = j11 * j22 - j12 * j21
        if abs(det) < 1e-9:
            break
        d_th = (r[0] * j22 - r[1] * j12) / det
        d_sh = (-r[0] * j21 + r[1] * j11) / det
        step = max(abs(d_th), abs(d_sh))
        if step > 8.0:                        # cap each correction so it can't spiral away
            d_th, d_sh = d_th * 8.0 / step, d_sh * 8.0 / step
        th -= d_th; sh -= d_sh
    return place(th, sh)[0]

def _build_run(samples=96):
    """Precompute the stride. The body height is a clean two-bounce curve. While a foot is down it's placed
    by IK, sliding back along the ground at a steady speed (no skating) with the ankle rolling forward for
    the push. While it swings, the joints follow a smooth curve from lift-off through the heel kick and
    knee drive to the next landing."""
    mid = RUN_STANCE / 2
    ref_lift = -lowest(_run_spline_pose(mid))
    land_z = _toe("right", _run_spline_pose(0.0), ref_lift)[2]
    off_z = _toe("right", _run_spline_pose(RUN_STANCE), ref_lift)[2]
    centre = (land_z + off_z) / 2                     # planted foot travel, kept within the leg's reach
    land_z, off_z = centre + (land_z - centre) * RUN_FOOT_TRAVEL, centre + (off_z - centre) * RUN_FOOT_TRAVEL
    base = ref_lift - _run_bounce(mid) - RUN_CROUCH
    foot_land, foot_off = 0.0, RUN_ANKLE_ROLL          # ankle angle at landing (straight down) and at push-off (rolled forward)

    def with_stand_legs(t):
        pose = _run_upper(t)
        for side in ("right", "left"):
            for b in ("leg", "shin", "foot"):
                pose[f"{side}_{b}"] = list(STAND[f"{side}_{b}"])
        return _frozen(pose)
    def leg_angles(pose):
        return tuple(pose[f"right_{b}"][0] for b in ("leg", "shin", "foot"))
    def guess(t, side="right"):
        sp = _run_spline_pose(t)
        return sp[f"{side}_leg"][0], sp[f"{side}_shin"][0]
    touch = leg_angles(_solve_leg("right", with_stand_legs(0.0), base + _run_bounce(0.0), land_z, foot_land, guess(0.0)))
    liftoff = leg_angles(_solve_leg("right", with_stand_legs(RUN_STANCE), base + _run_bounce(RUN_STANCE), off_z, foot_off,
                                    guess(RUN_STANCE)))
    std = tuple(STAND[f"right_{b}"][0] for b in ("leg", "shin", "foot"))
    # swing keys (absolute joint angles): lift-off, heel kick, knee drive, reaching out, touchdown
    swing = [(RUN_STANCE, liftoff),
             (0.45, (std[0] - 10, std[1] - 70, std[2] + 34)),
             (0.75, (std[0] + 52, std[1] - 40, std[2] + 20)),
             (0.92, (std[0] + 36, std[1] - 8, std[2] + 4)),
             (1.0, touch)]
    # Speed of each joint (per unit of stride) at lift-off and at touchdown, from the planted phase,
    # so the swing starts and ends moving exactly as the planted leg does (no snap at either handover).
    d = 0.01
    def stance_angles(u):
        lift = base + _run_bounce(u)
        z = land_z + (off_z - land_z) * (u / RUN_STANCE)
        ankle = foot_land + (foot_off - foot_land) * smooth(u / RUN_STANCE)
        return leg_angles(_solve_leg("right", with_stand_legs(u), lift, z, ankle, guess(u)))
    a_off_before, a_touch_after = stance_angles(RUN_STANCE - d), stance_angles(d)
    v_off = [(liftoff[c] - a_off_before[c]) / d for c in range(3)]
    v_touch = [(a_touch_after[c] - touch[c]) / d for c in range(3)]
    def swing_at(u):
        xs = [k[0] for k in swing]; n_ = len(swing)
        i = max(j for j in range(n_ - 1) if xs[j] <= u)
        h = xs[i + 1] - xs[i]; s_ = (u - xs[i]) / h
        h00, h10 = 2 * s_ ** 3 - 3 * s_ ** 2 + 1, s_ ** 3 - 2 * s_ ** 2 + s_
        h01, h11 = -2 * s_ ** 3 + 3 * s_ ** 2, s_ ** 3 - s_ ** 2
        out = []
        for c in range(3):
            def tangent(j):
                if j == 0:
                    return v_off[c]
                if j == n_ - 1:
                    return v_touch[c]
                return (swing[j + 1][1][c] - swing[j - 1][1][c]) / (xs[j + 1] - xs[j - 1])
            out.append(h00 * swing[i][1][c] + h10 * h * tangent(i) + h01 * swing[i + 1][1][c] + h11 * h * tangent(i + 1))
        return out
    frames = []
    for i in range(samples):
        t = i / samples
        lift = base + _run_bounce(t)
        pose = _editable(with_stand_legs(t))
        for side, offset in (("right", 0.0), ("left", 0.5)):
            u = (t + offset) % 1.0
            if u < RUN_STANCE:
                z = land_z + (off_z - land_z) * (u / RUN_STANCE)
                ankle = foot_land + (foot_off - foot_land) * smooth(u / RUN_STANCE)
                solved = _solve_leg(side, _frozen(pose), lift, z, ankle, guess(t, side))
                for b in ("leg", "shin", "foot"):
                    pose[f"{side}_{b}"] = list(solved[f"{side}_{b}"])
            else:
                for b, v in zip(("leg", "shin", "foot"), swing_at(u)):
                    st = STAND[f"{side}_{b}"]
                    pose[f"{side}_{b}"] = [v, st[1], st[2]]
        frames.append((_frozen(pose), lift))
    return frames

_RUN_FRAMES = _build_run()

def run_standing_frame(t, frantic=False):
    """Running upright like a human: each foot is planted only briefly, sliding back along the ground at a
    steady speed while it pushes (no skating), then swings with the heel kicking up and the knee driving
    forward, so there are moments with both feet off the ground; the body bounces in two clean arcs per
    stride. Leaning forward, shoulders twisting against the hips, arms pumping at its sides.
    frantic=True is the flee."""
    if frantic:
        return _flee_standing_frame(t)
    return _RUN_FRAMES[round((t % 1.0) * len(_RUN_FRAMES)) % len(_RUN_FRAMES)]

def _flee_standing_frame(t, frantic=True):
    """Sprinting upright on its dog legs: leaning forward, big strides with the knee and ankle folding high,
    the long arms pumping hard, brief airborne moments. frantic=True is the flee: leaning further, longer
    wilder strides, flailing arms, head ducked and jerking side to side as if glancing back, jerky wobble."""
    ph = 2 * math.pi * t
    k = 1.25 if frantic else 1.0
    pose = _editable(STAND)
    _add(pose, "hips", -22 if frantic else -14); _add(pose, "chest", -12 if frantic else -8)
    _add(pose, "neck", 20 if frantic else 14); _add(pose, "head", 8 if frantic else 4)
    for side, offset in (("right", 0.0), ("left", math.pi)):
        q = ph + offset; sw = math.sin(q); swinging = max(0.0, math.cos(q))
        sign = -1 if side == "right" else 1
        _add(pose, f"{side}_leg", 22 + 36 * k * sw)              # offset leans the legs forward under the lean
        _add(pose, f"{side}_shin", -44 * k * swinging)
        _add(pose, f"{side}_foot", 26 * k * swinging)
        _add(pose, f"{side}_arm", -34 * k * sw, 0, sign * (14 * math.sin(2 * q) if frantic else 0))   # flailing
        _add(pose, f"{side}_forearm", 40 + 10 * k * sw)          # elbows bent and pumping
    _add(pose, "chest", dy=6 * math.sin(ph)); _add(pose, "hips", dy=-4 * math.sin(ph))
    if frantic:
        _add(pose, "hips", dz=3 * math.sin(3 * ph)); _add(pose, "chest", dz=-3 * math.sin(3 * ph))
        _add(pose, "head", -4 * math.sin(ph), dy=16 * math.sin(ph) + 7 * math.sin(3 * ph))
    pose = _frozen(pose)
    return pose, -lowest(pose) + (2.2 if frantic else 1.5) * max(0.0, math.sin(2 * ph))

def leap_standing_frames():
    """Jumping from standing: crouch on the dog legs with the arms swung back, launch stretched out with the
    arms reaching ahead, tuck the knees in the air, land in a crouch, stand back up."""
    crouch = mirror(P(hips=-18, chest=-12, neck=14, head=6,
                      right_arm=(-40, 0, -6), right_forearm=10,
                      right_leg=55, right_shin=-105, right_foot=65))
    launch = mirror(P(hips=-12, chest=-6, neck=6, head=4,
                      right_arm=(150, 0, -10), right_forearm=-10,
                      right_leg=-10, right_shin=-20, right_foot=10))
    tuck = mirror(P(hips=-16, chest=-8, neck=10, head=4,
                    right_arm=(100, 0, -12), right_forearm=-30,
                    right_leg=75, right_shin=-110, right_foot=50))
    land = mirror(P(hips=-14, chest=-10, neck=12, head=6,
                    right_arm=(40, 0, -10), right_forearm=-20,
                    right_leg=48, right_shin=-98, right_foot=58))
    air = REST_LIFT + 6.0       # in the air the body stays at standing height; physics does the actual rise
    return [(0.0, STAND, REST_LIFT), (0.18, crouch, -lowest(crouch)), (0.32, launch, air),
            (0.52, tuck, air), (0.7, land, -lowest(land)), (0.95, STAND, REST_LIFT)]

def ledge_to_stand_frames():
    """Climbing over the top and ending upright: the same hang, pull and knee-up, then it rises onto its feet."""
    frames = ledge_frames()[:3]
    rise = mirror(P(hips=-30, torso=-8, chest=-10, neck=24, head=14,
                    right_arm=(30, 0, -6), right_forearm=-10,
                    right_leg=(45, 0, -3), right_shin=-80, right_foot=40))
    straighten = blend(rise, STAND, 0.75)
    return frames + [(0.95, rise, -lowest(rise), 0.0), (1.2, straighten, -lowest(straighten), 0.0),
                     (1.45, STAND, REST_LIFT, 0.0), (1.6, STAND, REST_LIFT, 0.0)]   # settle upright and hold

# ---------------------------------------------------------------- sleep set
# Lying like a dog, asleep: belly on the ground, upper arms back along the ribs with the forearms flat
# forward, neck stretched straight out flat with the head rolled onto its side on the ground just in front
# of the hands,
# hind legs close together out flat behind (the femur already runs up and back from the hip, so it only has
# to drop, with the shin folded under it and the hock pointing back).
LIE4 = mirror(P(hips=-90, neck=-12, head=(0, 90, 0),
                right_arm=(12, 0, -5), right_forearm=168, right_hand=0,
                right_leg=(-5, 0, -1), right_shin=175, right_foot=-170))
# Sitting on its haunches (standing version): rear on the ground, knees drawn up high, torso upright,
# forearms resting forward over the knees.
SIT = mirror(P(hips=-6, chest=-12, neck=10, head=6,
               right_arm=(22, 0, -14), right_forearm=62, right_hand=10,
               right_leg=(148, 0, -8), right_shin=-136, right_foot=75))   # buttocks and feet on the ground
# Lying on its side like a person, curled a little: the hips roll it over (pivoting at the rear, so the fall
# from sitting tips over sideways instead of swinging), arms folded in front, the head tilted down to rest on
# its cheek, knees drawn up with the legs stacked together. The right side ends up underneath; its arm turns
# in (+Z) so it lies under the body instead of propping it up, and the mirrored left one drapes over the top.
SIDE = mirror(P(hips=(-24, 0, 86), chest=-14, neck=(4, 20, 38), head=(0, -20, -30),
                right_arm=(78, 0, 14), right_forearm=-50,
                right_leg=(76, 0, 6), right_shin=-100, right_foot=40))
SIDE["right_leg"] = SIDE["left_leg"] = (76, -30, -10)     # both legs the same way: parallel, one on the other

# In-betweens (joint blends between these poses would swing the legs straight down through the floor):
# the dog's rear sinking onto its folded hind legs while the arms walk forward,
SINK4 = mirror(P(hips=-60, neck=45, head=30, right_arm=(128, 0, -10), right_forearm=0, right_hand=-38,
                 right_leg=(-35, 0, -16), right_shin=175, right_foot=-170))
# leaning forward out of the sit onto its hands (thighs keep their angle from the sit),
LEAN = mirror(P(hips=-50, neck=45, head=16, right_arm=(114, 0, -10), right_forearm=0, right_hand=-40,
                right_leg=(194, 0, -8), right_shin=-145, right_foot=90))
# on hands and knees, shins flat behind, on the way from the sit up to all fours,
KNEEL4 = mirror(P(hips=-70, neck=48, head=20, right_arm=(45, 0, -4), right_forearm=0, right_hand=0,
                  right_leg=(60, 0, -6), right_shin=-80, right_foot=0))
# and halfway through tipping over from the sit, the arm underneath reaching out to catch it.
TIP = dict(SIT); TIP["hips"] = (-12, 0, 40)

def _turned(pose, yaw):
    pose = dict(pose); pose["root"] = (0, yaw, 0)
    return pose

def densify(frames, steps=4):
    """Adds in-between keys (joint blends, smoothed) with the height recomputed at each one, so the
    interpolation between far-apart poses can't sink the body into the ground or leave it floating."""
    out = [frames[0]]
    for (t0, a, _), (t1, b, l1) in zip(frames, frames[1:]):
        for i in range(1, steps):
            p = blend(a, b, smooth(i / steps))
            out.append((t0 + (t1 - t0) * i / steps, p, -lowest(p)))
        out.append((t1, b, l1))
    return out

def lie_down_on_fours_frames():
    """Like a dog: walks a full tight circle on the spot (turning on the walk cycle), then folds the hind legs
    and settles onto its belly with the chin over the forearms. The turn ends at a full 360 so it doesn't
    unwind as it lies down."""
    frames = []
    for i in range(17):
        u = i / 16
        pose, _ = walk_frame((u * 2.0) % 1.0)
        pose = _turned(pose, -360 * u)
        frames.append((2.4 * u, pose, -lowest(pose)))
    hind_down = _turned(SINK4, -360)                  # the rear sinks first, then the chest goes down
    settle = [(2.4, frames[-1][1], frames[-1][2])]
    for t, p in ((3.0, hind_down), (3.6, _turned(LIE4, -360))):
        settle.append((t, p, -lowest(p)))
    return frames[:-1] + densify(settle)

def lie_down_standing_frames():
    """Lowers itself into a sit, stays there a moment, then tips over onto its side."""
    crouch = blend(STAND, SIT, 0.5); crouch = dict(crouch); crouch["hips"] = (-30, 0, 0)
    slump = _editable(SIT); _add(slump, "chest", -6); _add(slump, "head", -10); slump = _frozen(slump)
    tip = TIP
    return densify([(0.0, STAND, REST_LIFT), (0.5, crouch, -lowest(crouch)), (1.0, SIT, -lowest(SIT)),
                    (1.6, slump, -lowest(slump)), (2.0, tip, -lowest(tip)), (2.4, SIDE, -lowest(SIDE))])

def sleep_frame(base, t):
    """Slow, deep sleeping breaths."""
    b = math.sin(2 * math.pi * t)
    pose = _editable(base)
    _add(pose, "torso", 1.5 * b); _add(pose, "chest", 2.5 * b); _add(pose, "head", -1.0 * b)
    pose = _frozen(pose)
    return pose, -lowest(pose)

def sleep_on_fours_frame(t, rise=1.0):
    """Sleeping like a dog: only the chest rises and falls with each slow breath. The chest bone moves up
    (a position key; with the body lying flat its local +Z points up) and the neck and arms move back down by
    the same amount, so the head and hands stay where they lie and the rest of the body doesn't move."""
    b = (1 - math.cos(2 * math.pi * t)) / 2                 # 0 = breathed out, 1 = breathed in
    pose = dict(LIE4)
    pose["pos:chest"] = (0, 0, rise * b)
    for bone in ("neck", "right_arm", "left_arm"):
        pose["pos:" + bone] = (0, 0, -rise * b)
    return pose, -lowest(LIE4)

def wake_frames(sleep, end):
    """Waking: from either sleeping pose into either stance (the game picks the stance at random)."""
    frames = [(0.0, sleep, -lowest(sleep))]
    def go(dt, p, lift=None):
        frames.append((frames[-1][0] + dt, p, -lowest(p) if lift is None else lift))
    rise = blend(SIT, BEND, 0.5); rise = dict(rise); rise["hips"] = (-40, 0, 0)
    if sleep is SIDE:
        go(0.7, SIT)                                   # rolls up off its side into a sit
        if end is STAND:
            go(0.5, rise); go(0.5, STAND, REST_LIFT)   # leans forward and pushes up onto its feet
        else:
            # leans forward onto its hands, drops onto its knees, then lifts its hips into all fours
            go(0.5, LEAN); go(0.5, KNEEL4); go(0.5, REST4)
    else:
        go(1.0, REST4)                                 # pushes up off its belly
        if end is STAND:
            go(0.4, BEND); go(0.5, STAND, REST_LIFT)   # rears up through the forward bend
    return densify(frames)

# ---------------------------------------------------------------- combat, reactions, deaths
import random

def edit(base, **kw):
    """A copy of a pose with changes (right_* entries are mirrored to the left like in P)."""
    p = dict(base); p.update(mirror(P(**kw)))
    return p

FIST, CLAW = (90, 100, 50), (-12, 45, 5)          # finger poses: knuckle curl, tip curl, thumb
RELAXED_FINGERS = (20, 25, 10)

def with_hands(pose, right=None, left=None):
    p = dict(pose)
    if right: p["fingers:right"] = right
    if left: p["fingers:left"] = left
    return p

TREMOR_BONES = {"head": 1.0, "neck": 0.8, "jaw": 0.6, "chest": 0.5, "torso": 0.4,
                "right_arm": 0.6, "left_arm": 0.6, "right_forearm": 0.7, "left_forearm": 0.7}
SHAKE_BONES = dict(TREMOR_BONES, hips=0.5, right_leg=0.5, left_leg=0.5, right_shin=0.6, left_shin=0.6,
                   right_foot=0.4, left_foot=0.4)

def keyed(keys, step=0.05, shake=None, seed=1, hands_first=False):
    """Frames every `step` seconds from key poses [(time, pose) or (time, pose, {"back": px, "up": px}), ...],
    easing between keys, with the body seated on the ground at every frame (raised by "up", moved backwards
    by "back", forwards if negative). shake(t) -> (amplitude in degrees, bones) adds a random tremor."""
    moves = [k[2] if len(k) > 2 else {} for k in keys]
    keys = [(k[0], k[1]) for k in keys]
    rnd = random.Random(seed)
    for side in ("right", "left"):                  # a key without a finger pose has relaxed fingers
        if any(f"fingers:{side}" in k[1] for k in keys):
            keys = [(t, dict(p, **{f"fingers:{side}": p.get(f"fingers:{side}", RELAXED_FINGERS)})) for t, p in keys]
    frames = []
    n = int(round(keys[-1][0] / step))
    for i in range(n + 1):
        t = i * step
        j = max(k for k in range(len(keys)) if keys[k][0] <= t + 1e-9)
        if j == len(keys) - 1:
            pose = dict(keys[j][1]); mv = dict(moves[j])
        else:
            (t0, a), (t1, b) = keys[j], keys[j + 1]
            u = smooth((t - t0) / (t1 - t0))
            pose = blend(a, b, u)
            mv = {k: moves[j].get(k, 0) + (moves[j + 1].get(k, 0) - moves[j].get(k, 0)) * u for k in ("back", "up")}
        if shake:
            amp, bones = shake(t)
            if amp > 0:
                pose = _editable(pose)
                for bone, w in bones.items():
                    _add(pose, bone, rnd.uniform(-1, 1) * amp * w, rnd.uniform(-1, 1) * amp * w * 0.5,
                         rnd.uniform(-1, 1) * amp * w * 0.6)
                pose = _frozen(pose)
        frames.append((round(t, 4), pose, -lowest(pose) + mv.get("up", 0), mv.get("back", 0)))
    if hands_first:
        # pose the hands on the smooth, unshaken motion, then put the shake on top, so a trembling arm
        # doesn't flick its hand between planted and lifted
        calm = hand_pass(keyed(keys, step), "auto")
        by_time = {round(f[0], 4): f for f in calm}
        for i, (t, pose, lift, back) in enumerate(frames):
            src = by_time[t][1]
            p = dict(pose); p.update({k: src[k] for k in HAND_KEYS})
            frames[i] = (t, p, -lowest(p), back)
    return frames

def ramp(t, t0, t1):
    return smooth(max(0.0, min(1.0, (t - t0) / (t1 - t0)))) if t1 > t0 else float(t >= t0)

# --- the jaw and the head
JAW_PARTS = ("chin", "chin_point", "jaw_body", "jaw_ramus", "lower_front_teeth", "lower_side_teeth", "lower_molars")
NECK_PARTS = [c for c in CUBES if c[1] == "neck"]

def _box_overlap(pose, c_in, c_box, n=3):
    """How many sample points of cube c_in lie inside cube c_box, both posed."""
    a, b = c_box[2], c_box[3]; o = world(a, c_box[1], pose); axes = []
    for i in range(3):
        q = list(a); q[i] = b[i]; w = world(tuple(q), c_box[1], pose)
        d = [w[j] - o[j] for j in range(3)]; L = math.sqrt(sum(x * x for x in d)) or 1e-9
        axes.append(([x / L for x in d], L))
    hits = 0
    for k in itertools.product(range(n), repeat=3):
        pt = world(tuple(c_in[2][i] + (c_in[3][i] - c_in[2][i]) * (k[i] + 0.5) / n for i in range(3)), c_in[1], pose)
        if all(0 < sum((pt[j] - o[j]) * ax[j] for j in range(3)) < L for ax, L in axes):
            hits += 1
    return hits

def open_jaw(pose, degrees):
    """Open the mouth by about `degrees`. A jaw hinged in front of the ear swings its chin back into the
    throat if it only rotates, so like a real jaw it also drops and slides forward: the smallest shift that
    keeps the chin, teeth and jaw sides clear of the neck. If no small shift is enough (the head turned or
    lying on the ground), the mouth opens a little less."""
    parts = [c for c in CUBES if c[1] == "jaw" and any(k in c[0] for k in JAW_PARTS)]
    deg = degrees
    while True:
        p = dict(pose); p["jaw"] = (-deg, 0, 0)
        best = None
        for dz in (0, -0.5, -1, -1.5, -2):
            for dy in (0, -0.5, -1, -1.5):
                p["pos:jaw"] = (0, dy, dz)
                if sum(_box_overlap(p, c, nk) for c in parts for nk in NECK_PARTS) == 0:
                    if best is None or abs(dz) + abs(dy) < best[0]:
                        best = (abs(dz) + abs(dy), (0, dy, dz))
        if best or deg <= 0:
            p["pos:jaw"] = best[1] if best else (0, 0, 0)
            return p
        deg = max(0, deg - 3)

def aim_head(pose, pitch):
    """Turn the head (about its X) so the face points `pitch` degrees above level."""
    def face_pitch(hx):
        q = dict(pose); h = q.get("head", (0, 0, 0)); q["head"] = (hx, h[1], h[2])
        f = mvec(world_rot("head", q), (0, 0, -1))
        return math.degrees(math.asin(max(-1.0, min(1.0, f[1]))))
    lo, hi = -120.0, 120.0
    for _ in range(40):                  # looking further up as the head tips back (+X)
        mid = (lo + hi) / 2
        lo, hi = (mid, hi) if face_pitch(mid) < pitch else (lo, mid)
    p = dict(pose); h = p.get("head", (0, 0, 0)); p["head"] = ((lo + hi) / 2, h[1], h[2])
    return p

def face(pose, pitch, jaw=0):
    """Aim the head and open the jaw."""
    p = aim_head(pose, pitch)
    return open_jaw(p, jaw) if jaw else p

def hind_gathered(pose, base=None):
    base = base or REST4
    p = dict(pose)
    for side in ("right", "left"):
        l, sh = base[f"{side}_leg"], base[f"{side}_shin"]
        p[f"{side}_leg"] = (l[0] + 12, l[1], l[2]); p[f"{side}_shin"] = (sh[0] - 14, sh[1], sh[2])
    return p

def arm_add(pose, side, dx=0.0, dy=0.0, dz=0.0, forearm=None):
    p = dict(pose); a = pose[f"{side}_arm"]
    p[f"{side}_arm"] = (a[0] + dx, a[1] + dy, a[2] + dz)
    if forearm is not None:
        p[f"{side}_forearm"] = (forearm, 0, 0)
    return p

# --- scream: draws breath, then leans in with the face forward, jaw wide open, arms flung out with the
# claws spread (elbows bent forward), trembling with the scream
INHALE = face(edit(STAND, chest=-2, neck=-4, right_arm=(14, 0, -10)), 18, 6)
SCREAM = with_hands(face(edit(STAND, hips=-6, chest=-22, neck=22, right_arm=(30, 0, -28), right_forearm=35,
                              right_leg=36, right_shin=-74, right_foot=44), 12, 38), CLAW, CLAW)
INHALE4 = face(edit(REST4, neck=55), 22, 6)
SCREAM4 = with_hands(face(edit(REST4, neck=38), 12, 40), CLAW, CLAW)

def scream_frames(base, inhale, scream):
    return keyed([(0.0, base), (0.3, inhale), (0.5, scream), (1.7, scream), (2.1, base)],
                 shake=lambda t: (3.0 * ramp(t, 0.45, 0.6) * (1 - ramp(t, 1.6, 1.8)), TREMOR_BONES), seed=3)

# --- enraged wind-up (1.5 s before the charge). Standing: the arms go up overhead, then sweep down into
# claws reaching toward the target as it screams, dropping into a tense crouch, shaking harder as it builds.
# On all fours: head forward and level, jaw open, hind legs gathered under it ready to spring.
ARMS_UP = with_hands(face(edit(STAND, chest=-4, neck=-2, right_arm=(165, 0, -14), right_forearm=15), 22, 8), CLAW, CLAW)
WINDUP = with_hands(face(edit(STAND, hips=-25, chest=-25, neck=40, right_arm=(62, 0, -24), right_forearm=32,
                              right_leg=55, right_shin=-100, right_foot=55), 4, 36), CLAW, CLAW)
WINDUP4 = with_hands(face(hind_gathered(edit(REST4, neck=32)), 4, 38), CLAW, CLAW)

RECOVER = 0.5   # after the 1.5 s wind-up: the scream ends and it settles into the first frame of its run

def _run_start(frame):
    """A run's first frame as a key: its pose, raised to the height the run starts at."""
    pose, lift = frame
    return (pose, {"up": lift + lowest(pose)})

def windup_frames():
    run, up = _run_start(run_standing_frame(0.0))
    return keyed([(0.0, STAND), (0.35, ARMS_UP), (0.6, WINDUP), (1.5, WINDUP), (1.5 + RECOVER, run, up)],
                 shake=lambda t: ((1.5 + 3.5 * ramp(t, 0.6, 1.5)) * ramp(t, 0.5, 0.65) * (1 - ramp(t, 1.5, 1.75)), TREMOR_BONES),
                 seed=5)

def windup4_frames():
    run, up = _run_start(run_frame(0.0))
    return keyed([(0.0, REST4), (0.25, WINDUP4), (1.5, WINDUP4), (1.5 + RECOVER, run, up)],
                 shake=lambda t: ((1.5 + 3.5 * ramp(t, 0.2, 1.5)) * ramp(t, 0.15, 0.3) * (1 - ramp(t, 1.5, 1.75)), TREMOR_BONES),
                 seed=5)

# --- punch (standing): the right fist clenches and cocks back by the chin as the hips and shoulders turn
# away, then the body pivots through and the arm drives straight out; the left arm swings back as follow-
# through. The legs turn against the hips so the feet stay planted.
def _punch_poses():
    cock = with_hands(edit(STAND, chest=(-12, 18, 0), neck=4, head=(5, -14, 0)), FIST, None)
    cock.update({"hips": (0, 10, 0), "right_arm": (-25, 0, -10), "right_forearm": (115, 0, 0),
                 "left_arm": (25, 0, 8), "left_forearm": (60, 0, 0),
                 "right_leg": (28, -10, -3), "left_leg": (28, -10, 3)})
    strike = with_hands(edit(STAND, chest=(-20, -26, 0), neck=12, head=(0, 22, 0), right_shin=-70), FIST, None)
    strike.update({"hips": (-6, -14, 0), "right_arm": (82, 0, -4), "right_forearm": (8, 0, 0),
                   "left_arm": (-28, 0, 10), "left_forearm": (30, 0, 0),
                   "right_leg": (34, 14, -3), "left_leg": (34, 14, 3)})
    follow = dict(strike); follow.update({"left_arm": (-36, 0, 12), "left_forearm": (20, 0, 0), "right_forearm": (20, 0, 0)})
    return cock, strike, follow
COCK, STRIKE, FOLLOW = _punch_poses()

def punch_frames():
    return keyed([(0.0, STAND), (0.25, COCK), (0.37, STRIKE), (0.5, FOLLOW), (0.85, STAND)], step=0.025)

# --- slap (on all fours): the right arm comes up high with the elbow bent, the hand up by the head, then
# slaps down and across at chest height (not down at the ground) and goes back to the ground
def _slap_poses():
    raise_ = with_hands(arm_add(REST4, "right", 100, 0, -25, forearm=75), CLAW, None)
    raise_.update({"chest": (0, 10, 0)})
    swipe = with_hands(arm_add(REST4, "right", 70, 0, 25, forearm=25), CLAW, None)
    swipe.update({"chest": (0, -12, 0)})
    return raise_, swipe
SLAP_RAISE, SLAP_SWIPE = _slap_poses()

def slap_frames():
    return keyed([(0.0, REST4), (0.22, SLAP_RAISE), (0.34, SLAP_SWIPE), (0.6, REST4)], step=0.025)

# --- bite (on all fours): gathers, leaps forward with the head level and the jaw wide open, snaps it shut as
# it lands, then settles back
def _bite_poses():
    gather = face(hind_gathered(edit(REST4, neck=44)), 2, 8)
    lunge = arm_add(arm_add(edit(REST4, neck=34), "right", 25, forearm=10), "left", 25, forearm=10)
    for side in ("right", "left"):
        l, sh = REST4[f"{side}_leg"], REST4[f"{side}_shin"]
        lunge[f"{side}_leg"] = (l[0] - 15, l[1], l[2]); lunge[f"{side}_shin"] = (sh[0] + 10, sh[1], sh[2])
    lunge = face(lunge, 0, 44)
    snap = face(edit(REST4, neck=36), 0)
    return gather, lunge, snap
BITE_GATHER, BITE_LUNGE, BITE_SNAP = _bite_poses()

def bite_frames():
    return keyed([(0.0, REST4), (0.12, BITE_GATHER, {"back": 1}), (0.26, BITE_LUNGE, {"back": -7, "up": 2.5}),
                  (0.34, BITE_SNAP, {"back": -8}), (0.6, REST4)], step=0.025)

# --- hurt: the whole body recoils. Standing: knocked back a step, knees dipping, chest and head thrown back,
# arms jerking up. On all fours: shoved back, the body twisting and the front legs buckling, head jerked aside.
FLINCH = face(edit(STAND, chest=(8, 10, 0), neck=-14, right_arm=(25, 0, -18), right_forearm=55,
                   right_leg=42, right_shin=-86), 30, 15)
FLINCH4 = arm_add(arm_add(edit(REST4, chest=(0, 12, 0), neck=36), "right", -10, forearm=25), "left", -10, forearm=25)
FLINCH4["hips"] = (FLINCH4["hips"][0], 0, 6)
FLINCH4["head"] = (0, 25, -12)
FLINCH4 = face(FLINCH4, 5, 15)

def hurt_frames(base, flinch):
    return keyed([(0.0, base), (0.08, flinch, {"back": 2.5}), (0.2, blend(flinch, base, 0.4), {"back": 2.0}),
                  (0.45, base)], step=0.025)

# ---------------------------------------------------------------- character
PINCH = (50, 45, 45)                            # fingers pinching skin between the index finger and thumb

# --- sniffing: head low, quick little sniffs (the nose pushing forward), turning to follow the scent
def _sniff(base, t_pushes, turns):
    """Keys for a sniffing loop: base pose, nose pushes at the given times, head turns [(time, yaw), ...]."""
    def at(yaw, push):
        p = dict(base)
        n, h = base["neck"], base["head"]
        p["neck"] = (n[0] + 3 * push, n[1] + yaw * 0.4, n[2]); p["head"] = (h[0] - 4 * push, h[1] + yaw * 0.6, h[2])
        return p
    def yaw_at(t):
        pts = sorted(turns)
        for (t0, y0), (t1, y1) in zip(pts, pts[1:]):
            if t0 <= t <= t1:
                return y0 + (y1 - y0) * smooth((t - t0) / (t1 - t0)) if t1 > t0 else y1
        return pts[-1][1]
    times = sorted({round(x, 3) for x in [k for k, _ in turns] + [t for tp in t_pushes for t in (tp - 0.07, tp, tp + 0.07)]})
    keys = []
    for t in times:
        push = 1.0 if any(abs(t - tp) < 1e-6 for tp in t_pushes) else 0.0
        keys.append((t, at(yaw_at(t), push)))
    return keys

SNIFF = face(edit(STAND, hips=-8, chest=-30, neck=36, right_arm=(14, 0, -4), right_leg=34, right_shin=-72), -28)
SNIFF4 = face(edit(REST4, neck=6), -55)

def sniff_frames():
    pushes = [0.15, 0.3, 0.45, 1.1, 1.25, 1.95, 2.1]
    return keyed(_sniff(SNIFF, pushes, [(0.0, 0), (0.75, 0), (0.95, 22), (1.6, 22), (1.8, 0), (2.4, 0)]), step=0.025)

def sniff4_frames():
    pushes = [0.2, 0.35, 0.8, 0.95, 1.5, 1.65, 2.2, 2.35]
    return keyed(_sniff(SNIFF4, pushes, [(0.0, 0), (0.7, -22), (1.4, 0), (2.1, 22), (2.8, 0)]), step=0.025)

# --- head tilt: the slow curious dog tilt, a little twitch, a snap to the other side, back
def _tilt_keys(base, up=0):
    h, n = base["head"], base["neck"]
    tilt_r = dict(base, neck=(n[0] + 4 + up, n[1], n[2] + 5), head=(h[0] + 8, h[1] + 8, h[2] + 26))
    twitch = dict(tilt_r, head=(tilt_r["head"][0], tilt_r["head"][1], tilt_r["head"][2] + 5))
    tilt_l = dict(base, neck=(n[0] + 4 + up, n[1], n[2] - 4), head=(h[0] + 8, h[1] - 6, h[2] - 24))
    return [(0.0, base), (0.5, tilt_r), (0.9, tilt_r), (1.0, twitch), (1.1, tilt_r), (1.45, tilt_r),
            (1.6, tilt_l), (2.1, tilt_l), (2.6, base)]

def tilt_frames():
    return keyed(_tilt_keys(STAND), step=0.025)

def tilt4_frames():
    return keyed(_tilt_keys(REST4, up=6), step=0.025)

# --- adjusting the skin. Standing: pinches the skin of its jaw and tugs it twice (the head leaning into the
# hand), pinches and pulls at the skin of its other forearm, then rolls its shoulders as if settling into the
# skin. On all fours: lifts a front hand to tug at its jaw, puts it back down, then shakes its whole body like
# a wet dog, as if the skin had slipped. (The arm angles that bring the fingers to the jaw were solved so they
# touch it without going inside the head.)
def _skin_poses():
    jaw = with_hands(edit(STAND, neck=16, head=(5, -20, -10)), PINCH, None)
    jaw.update({"right_arm": (67.5, -10, 0), "right_forearm": (150, 0, 0), "right_hand": (90, 0, 0)})
    tug = dict(jaw, right_arm=(59.5, -10, 0), right_forearm=(140, 0, 0), head=(5, -20, -2))
    held = with_hands(edit(STAND, neck=8, head=(5, -15, 12)), None, PINCH)
    held.update({"right_arm": (35, 0, 10), "right_forearm": (85, 0, 0),
                 "left_arm": (-30, 0, -30), "left_forearm": (130, 0, 0), "left_hand": (-26.2, 0, -10)})
    pull = dict(held, left_arm=(-38, 0, -30), left_forearm=(120, 0, 0))
    roll_a = edit(STAND, chest=(-10, 0, 8), neck=(5, 0, -8), head=(5, 0, 6))
    roll_b = edit(STAND, chest=(-10, 0, -8), neck=(5, 0, 8), head=(5, 0, -6))
    return jaw, tug, held, pull, roll_a, roll_b
SKIN_JAW, SKIN_TUG, SKIN_HELD, SKIN_PULL, SKIN_ROLL_A, SKIN_ROLL_B = _skin_poses()

def adjust_skin_frames():
    return keyed([(0.0, STAND), (0.45, SKIN_JAW), (0.65, SKIN_TUG), (0.85, SKIN_JAW), (1.05, SKIN_TUG),
                  (1.4, SKIN_HELD), (1.6, SKIN_PULL), (1.8, SKIN_HELD), (2.0, SKIN_PULL),
                  (2.4, SKIN_ROLL_A), (2.7, SKIN_ROLL_B), (3.1, STAND)], step=0.025)

def _skin4_poses():
    jaw = with_hands(dict(REST4, head=(REST4["head"][0], -15, 12)), PINCH, None)
    jaw.update({"right_arm": (60, 10, -20), "right_forearm": (150, 0, 0), "right_hand": (42.5, 0, 0)})
    tug = dict(jaw, right_arm=(52, 10, -20), right_forearm=(140, 0, 0), head=(REST4["head"][0], -15, 20))
    return jaw, tug
SKIN4_JAW, SKIN4_TUG = _skin4_poses()

def adjust_skin4_frames():
    keys = [(0.0, REST4), (0.4, SKIN4_JAW), (0.6, SKIN4_TUG), (0.8, SKIN4_JAW), (1.0, SKIN4_TUG), (1.35, REST4)]
    # the dog shake: a fast twist rolling from the head down through the body, building up and dying away
    t, step = 1.4, 0.04
    while t < 2.66:
        u = (t - 1.4) / 1.25
        amp = smooth(min(1.0, u / 0.25)) * (1 - smooth(max(0.0, (u - 0.7) / 0.3)))
        w = math.sin(2 * math.pi * 6.0 * (t - 1.4))
        p = _editable(REST4)
        _add(p, "chest", 0, 18 * amp * w, 10 * amp * math.sin(2 * math.pi * 6.0 * (t - 1.4) + 0.6))
        _add(p, "neck", 0, -24 * amp * w, 0); _add(p, "head", 0, -18 * amp * w, 8 * amp * w)
        _add(p, "hips", 0, -10 * amp * w, -6 * amp * math.sin(2 * math.pi * 6.0 * (t - 1.4) - 0.6))
        keys.append((round(t, 3), _frozen(p))); t += step
    keys.append((2.9, REST4))
    return keyed(keys, step=0.02)

# --- deaths. End poses: face down (standing death), on its side (on all fours)
# Face down, everything slumped onto the ground (fitted limb by limb so each part rests on it): head turned
# on its side, arms limp at its sides, legs and feet flat behind it, toes lying along the ground.
DEAD_FRONT = mirror(P(hips=(-90, 0, 0), chest=0, neck=16, head=(0, 80, 0), jaw=-12,
                      right_arm=(-10, 0, -50), right_forearm=30, right_hand=(-10, 0, -60),
                      right_leg=(4, 0, -12), right_shin=-3, right_foot=-20,
                      **{f"right_toe{i}": 20 for i in range(1, 6)}, **{f"right_toe{i}_tip": 20 for i in range(1, 6)}))
DEAD_SIDE = edit(LIE4, hips=(-82, 0, 70), jaw=-15, right_forearm=155)   # the belly-down pose rolled onto its side
# ...then everything slumped onto the ground (fitted limb by limb): the head down, the top arm and leg flopped
# forward onto the ground in front of the body, the bottom leg curled
DEAD_SIDE.update({"neck": (-12, 0, 32), "left_arm": (75, 0, -75), "left_forearm": (20, 0, 0), "left_hand": (0, 0, -30),
                  "left_leg": (0, 0, -30), "left_shin": (300, 0, 0), "left_foot": (-15, 0, 0),
                  "right_leg": (135, 0, 0), "right_shin": (210, 0, 0), "right_foot": (-15, 0, 0)})
# (shins as 300 / 210 rather than -60 / -150: the same angles, but close to where they start lying down, so
# rolling over doesn't swing them the long way round through the ground)
DEAD_SIDE = with_hands(DEAD_SIDE, (0, 4, 0), (0, 4, 0))          # fingers lying flat
STAGGER = edit(STAND, chest=0, neck=-10, head=-20, jaw=-20, right_arm=(20, 0, -18), right_forearm=20)
KNEEL = edit(STAND, hips=-15, chest=-25, neck=25, head=20, jaw=-10, right_arm=(5, 0, -5), right_forearm=10,
             right_leg=15, right_shin=-95, right_foot=10)
DEAD_FRONT = with_hands(DEAD_FRONT, (0, 4, 0), (0, 4, 0))          # fingers lying flat, not curled under
DEAD_FRONT_BOUNCE = edit(DEAD_FRONT, neck=12, chest=-3)
SLUMP4 = edit(SINK4, neck=20, head=0, jaw=-10)
CHEST_DOWN4 = edit(LIE4, jaw=-15)

def _jaws(*names):
    for n in names:
        p = globals()[n]; deg = -p["jaw"][0]
        globals()[n] = open_jaw(p, deg)
_jaws("DEAD_FRONT", "DEAD_SIDE", "STAGGER", "KNEEL", "DEAD_FRONT_BOUNCE", "SLUMP4", "CHEST_DOWN4")

def death_frames():
    """Normal death (sword, bow...): reels back, the knees buckle, falls forward onto its face."""
    return keyed([(0.0, STAND), (0.25, STAGGER), (0.7, KNEEL), (1.2, blend(KNEEL, DEAD_FRONT, 0.55)),
                  (1.35, DEAD_FRONT), (1.5, DEAD_FRONT_BOUNCE), (1.7, DEAD_FRONT), (2.0, DEAD_FRONT)])

def death4_frames():
    """Normal death on all fours: the hind legs give out, the chest goes down, it slumps onto its side."""
    return keyed([(0.0, REST4), (0.3, SLUMP4), (0.8, CHEST_DOWN4), (1.2, DEAD_SIDE),
                  (1.35, edit(DEAD_SIDE, neck=14)), (1.5, DEAD_SIDE), (1.8, DEAD_SIDE)])

# Dust Ball death: shakes violently the whole time while slowly collapsing to the ground, keeps shaking on
# the ground for a second and a half or so, then goes still.
SHAKE_CROUCH = with_hands(edit(STAND, hips=-20, chest=-30, neck=20, head=10, jaw=-25, right_arm=(20, 0, -20),
                               right_forearm=110, right_leg=60, right_shin=-110, right_foot=55), CLAW, CLAW)
DEAD_SIDE_STAND = mirror(P(hips=(-24, 0, 86), chest=-14, neck=(4, 20, 38), head=(0, -20, -30), jaw=-20,
                           right_arm=(60, 0, 14), right_forearm=30, right_leg=(50, 0, 6), right_shin=-70, right_foot=40))
# everything slumped onto the ground: the top arm and leg flopped forward in front of the body
DEAD_SIDE_STAND.update({"neck": (12, 20, 40), "left_arm": (75, 0, -75), "left_forearm": (0, 0, 0), "left_hand": (-20, 0, -90),
                        "left_leg": (120, 0, 45), "left_shin": (-75, 0, 0), "left_foot": (60, 0, 0),
                        "right_leg": (90, 0, -15), "right_shin": (0, 0, 0), "right_foot": (15, 0, 0)})
DEAD_SIDE_STAND = with_hands(DEAD_SIDE_STAND, (0, 4, 0), (0, 4, 0))
_jaws("SHAKE_CROUCH", "DEAD_SIDE_STAND")

def dust_death_frames():
    keys = [(0.0, STAND), (0.4, STAND), (1.5, SHAKE_CROUCH), (2.6, KNEEL), (3.4, DEAD_SIDE_STAND), (5.6, DEAD_SIDE_STAND)]
    return keyed(keys, shake=lambda t: (9.0 * ramp(t, 0.0, 0.3) * (1 - ramp(t, 5.0, 5.4)), SHAKE_BONES), seed=7,
                 hands_first=True)

def dust_death4_frames():
    keys = [(0.0, REST4), (0.4, REST4), (1.6, SLUMP4), (2.6, CHEST_DOWN4), (3.2, DEAD_SIDE), (5.5, DEAD_SIDE)]
    return keyed(keys, shake=lambda t: (9.0 * ramp(t, 0.0, 0.3) * (1 - ramp(t, 4.9, 5.3)), SHAKE_BONES), seed=9,
                 hands_first=True)

# Hands already posed (keyed(..., hands_first=True)) are kept as they are.
HAND_RATE = {}

# Folders in Blockbench's animation list.
GROUP_OF = {}
for _g, _names in (("Idle", ("rest_on_fours", "rest_standing", "alert", "alert_on_fours", "look_around", "look_around_on_fours")),
                   ("Movement", ("walk_on_fours", "walk_standing", "run_on_fours", "flee_on_fours", "run_standing",
                                 "flee_standing", "stand_to_crawl", "crawl_to_stand", "crawl_prone", "crawl_to_prone", "prone_to_crawl",
                                 "climb_up", "climb_down",
                                 "climb_over_ledge", "climb_over_ledge_to_stand", "leap", "leap_standing", "swim")),
                   ("Sleep", ("lie_down_on_fours", "lie_down_standing", "sleep_on_fours", "sleep_standing",
                              "wake_on_fours_to_on_fours", "wake_on_fours_to_standing", "wake_standing_to_on_fours",
                              "wake_standing_to_standing")),
                   ("Combat", ("scream_standing", "scream_on_fours", "enraged_windup_standing", "enraged_windup_on_fours",
                               "punch", "slap_on_fours", "bite_on_fours")),
                   ("Reactions", ("hurt_standing", "hurt_on_fours")),
                   ("Death", ("death_standing", "death_on_fours", "dust_death_standing", "dust_death_on_fours")),
                   ("Character", ("sniff_standing", "sniff_on_fours", "head_tilt_standing", "head_tilt_on_fours",
                                  "adjust_skin_standing", "adjust_skin_on_fours"))):
    for _n in _names:
        GROUP_OF[_n] = _g

# Lying on its side, the arms are folded in front and the hands just rest (no planting on the ground).
RELAXED_HANDS = {"lie_down_standing": "relaxed", "sleep_standing": "relaxed", "wake_standing_to_standing": "relaxed"}
RELAXED_HANDS.update({"dust_death_standing": "keep", "dust_death_on_fours": "keep"})   # posed before the shake
RELAXED_HANDS["death_standing"] = "keep"         # the hands just follow the poses: limp, ending flat on the ground

def keyframe(channel, t, v, interpolation="catmullrom"):
    # Blockbench stores animation rotations in Bedrock's convention: X and Y turn the opposite way to the
    # editor's group rotations (Z is the same), so flip them here.
    if channel == "rotation":
        v = (-v[0], -v[1], v[2])
    return {"channel": channel, "data_points": [{"x": round(v[0], 3), "y": round(v[1], 3), "z": round(v[2], 3)}],
            "uuid": str(uuid.uuid4()), "time": round(t, 4), "color": -1, "interpolation": interpolation}

def animation(name, loop, length, frames, interpolation="catmullrom", hands="auto", group=""):
    """frames: list of (time, pose, lift) or (time, pose, lift, back). Keyframes are offsets from the standing
    rest pose; back moves the whole body backwards (+Z), for climbing over a ledge."""
    frames = hand_pass(frames, hands, *HAND_RATE.get(name, (None, False)))
    anim = {}
    # every bone gets a key on every frame (a bone missing from a pose is at rest), otherwise Blockbench
    # would hold that bone's nearest key instead
    keys = set(STAND).union(*(f[1] for f in frames))
    bones = {k for k in keys if ":" not in k}
    moved = {k for k in keys if k.startswith("pos:")}   # "pos:<bone>": a position offset for that bone
    for frame in frames:
        t, pose, lift = frame[:3]; back = frame[3] if len(frame) > 3 else 0.0
        for b in bones:
            p, r = pose.get(b, (0, 0, 0)), STAND.get(b, (0, 0, 0))
            anim.setdefault(b, []).append(keyframe("rotation", t, (p[0] - r[0], p[1] - r[1], p[2] - r[2]), interpolation))
        anim.setdefault("root", []).append(keyframe("position", t, (0, lift - REST_LIFT, back), interpolation))
        for k in moved:
            anim.setdefault(k[4:], []).append(keyframe("position", t, pose.get(k, (0, 0, 0)), interpolation))
    return {"uuid": str(uuid.uuid4()), "name": name, "group_name": group or GROUP_OF.get(name, ""), "loop": loop, "override": False, "length": length,
            "snapping": 20, "selected": False, "anim_time_update": "", "blend_weight": "", "start_delay": "",
            "loop_delay": "", "animators": {GUUID[b]: {"name": b, "type": "bone", "keyframes": kf} for b, kf in anim.items()}}

# ---------------------------------------------------------------- Blockbench file
GUUID = {b[0]: str(uuid.uuid4()) for b in BONES}
def size(c):
    return [math.ceil(c[3][i] - c[2][i]) for i in range(3)]
order = sorted(range(len(CUBES)), key=lambda i: -(size(CUBES[i])[2] + size(CUBES[i])[1]))
UV = {}; ux = uy = shelf = 0
for i in order:
    w, h, d = size(CUBES[i]); fw, fh = 2 * (w + d), d + h
    if ux + fw > 128: ux, uy, shelf = 0, uy + shelf, 0
    UV[i] = (ux, uy); ux += fw; shelf = max(shelf, fh)
assert uy + shelf <= 128, "UV overflow"

# The texture (painted by art/tools/skinwalker_texture.py, or by hand). The model links to the file rather
# than owning it, so regenerating the model never overwrites painting done on it.
TEXTURE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main",
                                        "resources", "assets", "horrormod", "textures", "entity", "skinwalker.png"))

def textures(out_dir):
    if not os.path.exists(TEXTURE):
        return []
    img = Image.open(TEXTURE)
    return [{"path": TEXTURE, "name": "skinwalker.png", "folder": "entity", "namespace": "horrormod", "id": "0",
             "width": img.width, "height": img.height, "uv_width": 128, "uv_height": 128, "particle": False,
             "render_mode": "default", "render_sides": "auto", "visible": True, "internal": False, "saved": True,
             "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, TEXTURE)),
             "relative_path": os.path.relpath(TEXTURE, os.path.abspath(out_dir)),
             "source": "data:image/png;base64," + base64.b64encode(open(TEXTURE, "rb").read()).decode()}]

def faces(i):
    w, h, d = size(CUBES[i]); u, v = UV[i]
    f = lambda a: {"uv": a, "texture": 0 if os.path.exists(TEXTURE) else False}
    return {"north": f([u+d, v+d, u+d+w, v+d+h]), "east": f([u, v+d, u+d, v+d+h]),
            "south": f([u+2*d+w, v+d, u+2*d+2*w, v+d+h]), "west": f([u+d+w, v+d, u+2*d+w, v+d+h]),
            "up": f([u+d+w, v+d, u+d, v]), "down": f([u+d+2*w, v, u+d+w, v+d])}

def lifted(p):
    return [p[0], round(p[1] + REST_LIFT, 3), p[2]]

def build():
    elements, kids = [], {b[0]: [] for b in BONES}
    for i, (name, bone, a, b, tone, inf) in enumerate(CUBES):
        cid = str(uuid.uuid4())
        elements.append({"name": name, "box_uv": True, "rescale": False, "locked": False, "render_order": "default",
                         "allow_mirror_modeling": True, "from": lifted(a), "to": lifted(b), "autouv": 0, "color": 1,
                         "inflate": inf, "origin": lifted(PIVOT[bone]), "uv_offset": list(UV[i]), "faces": faces(i),
                         "type": "cube", "uuid": cid})
        kids[bone].append(cid)
    def group(name):
        g = {"name": name, "origin": lifted(PIVOT[name]), "color": 0, "uuid": GUUID[name], "export": True,
             "mirror_uv": False, "isOpen": True, "locked": False, "visibility": True, "autouv": 0,
             "children": kids[name] + [group(c) for c in CHILDREN[name]]}
        r = STAND.get(name)
        if r and any(r):
            g["rotation"] = [round(v, 3) for v in r]
        return g
    LEN = 1.0; STEPS = 8
    walk = [(i / STEPS * LEN, *walk_frame(i / STEPS)) for i in range(STEPS + 1)]
    def cycle(fn, length, steps):
        return [(i / steps * length, *fn(i / steps)) for i in range(steps + 1)]
    rest4 = cycle(rest4_frame, 3.0, 8)
    rest_standing = cycle(rest_standing_frame, 3.0, 8)
    walk_standing = cycle(walk_standing_frame, 1.2, 12)
    alert = [(0.0, STAND, REST_LIFT), (0.25, ALERT, -lowest(ALERT)), (1.0, ALERT, -lowest(ALERT))]
    alert4 = [(0.0, REST4, -lowest(REST4)), (0.25, ALERT4, -lowest(ALERT4)), (1.0, ALERT4, -lowest(ALERT4))]
    look = look_around(ALERT, -lowest(ALERT))
    look4 = look_around(ALERT4, -lowest(ALERT4), chest_share=0.0)
    run = cycle(run_frame, 0.5, 10)
    flee = cycle(flee_frame, 0.4, 10)
    stand_to_crawl = transition(STAND, REST4, 0.9, through=BEND)
    crawl_to_stand = transition(REST4, STAND, 0.9, through=BEND)
    prone = cycle(prone_frame, 0.6, 12)
    prone0 = prone[0]
    crawl_to_prone = densify([(0.0, REST4, -lowest(REST4)), (0.8, prone0[1], prone0[2])], steps=6)
    prone_to_crawl = densify([(0.0, prone0[1], prone0[2]), (0.8, REST4, -lowest(REST4))], steps=6)
    climb_up = cycle(lambda t: climb_frame(t), 1.0, 8)
    climb_down = cycle(lambda t: climb_frame(t, down=True), 1.2, 8)
    ledge = ledge_frames()
    leap = leap_frames()
    swim = cycle(swim_frame, 1.0, 8)
    run_standing = cycle(lambda t: run_standing_frame(t), 0.5, 32)
    sleep_set = [("lie_down_on_fours", "hold", 3.6, lie_down_on_fours_frames()),
                 ("lie_down_standing", "hold", 2.4, lie_down_standing_frames()),
                 ("sleep_on_fours", "loop", 6.0, cycle(lambda t: sleep_on_fours_frame(t), 6.0, 8)),
                 ("sleep_standing", "loop", 4.0, cycle(lambda t: sleep_frame(SIDE, t), 4.0, 8))]
    for sleep_name, sleep_pose in (("on_fours", LIE4), ("standing", SIDE)):
        for end_name, end_pose in (("on_fours", REST4), ("standing", STAND)):
            fr = wake_frames(sleep_pose, end_pose)
            sleep_set.append((f"wake_{sleep_name}_to_{end_name}", "hold", fr[-1][0], fr))
    flee_standing = cycle(lambda t: run_standing_frame(t, frantic=True), 0.42, 10)
    combat_set = [("scream_standing", "hold", scream_frames(STAND, INHALE, SCREAM)),
                  ("scream_on_fours", "hold", scream_frames(REST4, INHALE4, SCREAM4)),
                  ("enraged_windup_standing", "hold", windup_frames()),
                  ("enraged_windup_on_fours", "hold", windup4_frames()),
                  ("punch", "hold", punch_frames()),
                  ("slap_on_fours", "hold", slap_frames()),
                  ("bite_on_fours", "hold", bite_frames()),
                  ("hurt_standing", "hold", hurt_frames(STAND, FLINCH)),
                  ("hurt_on_fours", "hold", hurt_frames(REST4, FLINCH4)),
                  ("death_standing", "hold", death_frames()),
                  ("death_on_fours", "hold", death4_frames()),
                  ("dust_death_standing", "hold", dust_death_frames()),
                  ("dust_death_on_fours", "hold", dust_death4_frames()),
                  ("sniff_standing", "loop", sniff_frames()),
                  ("sniff_on_fours", "loop", sniff4_frames()),
                  ("head_tilt_standing", "hold", tilt_frames()),
                  ("head_tilt_on_fours", "hold", tilt4_frames()),
                  ("adjust_skin_standing", "hold", adjust_skin_frames()),
                  ("adjust_skin_on_fours", "hold", adjust_skin4_frames())]
    leap_standing = leap_standing_frames()
    ledge_to_stand = ledge_to_stand_frames()
    return {"meta": {"format_version": "4.10", "model_format": "modded_entity", "box_uv": True},
            "name": "skinwalker_blockout", "model_identifier": "skinwalker", "modded_entity_entity_class": "SkinwalkerEntity",
            "modded_entity_version": "1.17", "modded_entity_flip_y": True, "visible_box": [1, 1, 0],
            "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [], "unhandled_root_fields": {},
            "resolution": {"width": 128, "height": 128}, "elements": elements, "outliner": [group("root")],
            "textures": textures(OUT), "animations": [animation("rest_on_fours", "loop", 3.0, rest4),
                                           animation("walk_on_fours", "loop", LEN, walk),
                                           animation("rest_standing", "loop", 3.0, rest_standing),
                                           animation("walk_standing", "loop", 1.2, walk_standing),
                                           animation("alert", "hold", 1.0, alert),
                                           animation("alert_on_fours", "hold", 1.0, alert4),
                                           animation("look_around", "loop", 4.0, look, "linear"),
                                           animation("look_around_on_fours", "loop", 4.0, look4, "linear"),
                                           animation("run_on_fours", "loop", 0.5, run),
                                           animation("flee_on_fours", "loop", 0.4, flee),
                                           animation("stand_to_crawl", "hold", 0.9, stand_to_crawl),
                                           animation("crawl_to_stand", "hold", 0.9, crawl_to_stand),
                                           animation("crawl_prone", "loop", 0.6, prone),
                                           animation("crawl_to_prone", "hold", 0.8, crawl_to_prone),
                                           animation("prone_to_crawl", "hold", 0.8, prone_to_crawl),
                                           animation("climb_up", "loop", 1.0, climb_up),
                                           animation("climb_down", "loop", 1.2, climb_down),
                                           animation("climb_over_ledge", "hold", 1.0, ledge),
                                           animation("leap", "hold", 0.85, leap),
                                           animation("swim", "loop", 1.0, swim),
                                           animation("run_standing", "loop", 0.5, run_standing),
                                           animation("flee_standing", "loop", 0.42, flee_standing),
                                           animation("leap_standing", "hold", 0.95, leap_standing),
                                           animation("climb_over_ledge_to_stand", "hold", 1.6, ledge_to_stand)]
                          + [animation(n, l, ln, fr, hands=RELAXED_HANDS.get(n, "auto")) for n, l, ln, fr in sleep_set]
                          + [animation(n, l, fr[-1][0], fr, hands=RELAXED_HANDS.get(n, "auto")) for n, l, fr in combat_set]}

# ---------------------------------------------------------------- GeckoLib export (what the game loads)
ASSETS = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main",
                                       "resources", "assets", "horrormod"))
SAMPLE_RATE = 40          # animation samples per second (two per game tick)

def export_geckolib(model):
    """Writes the model and animations in GeckoLib's (Bedrock) format, so the game shows exactly what Blockbench
    shows:
    - geometry: Bedrock mirrors X (pivots and cube origins negated) and turns X/Y rotations the other way;
      textures use per-face UV rectangles at the cubes' exact sizes (GeckoLib's box UV would round them down
      and miss the painted texture)
    - animations: Blockbench's smooth (catmull-rom) curves are sampled at every key and 40 times a second, stored as straight
      lines between samples (GeckoLib's own "catmullrom" is only an ease between two keys), with samples that a
      straight line already reproduces (within 0.15 degrees / 0.02 px) left out to keep the file small."""
    elements = {e["uuid"]: e for e in model["elements"]}
    bones = []
    def walk(grp, parent):
        o = grp["origin"]
        b = {"name": grp["name"], "pivot": [-o[0], o[1], o[2]]}
        if parent:
            b["parent"] = parent
        r = grp.get("rotation")
        if r and any(r):
            b["rotation"] = [-r[0], -r[1], r[2]]
        cubes = []
        for ch in grp["children"]:
            if isinstance(ch, dict):
                continue
            e = elements[ch]
            lo = [min(e["from"][i], e["to"][i]) for i in range(3)]; hi = [max(e["from"][i], e["to"][i]) for i in range(3)]
            w, h, d = (hi[i] - lo[i] for i in range(3))
            u, v = e["uv_offset"]
            faces = {"north": ([u + d, v + d], [w, h]), "east": ([u, v + d], [d, h]),
                     "south": ([u + 2 * d + w, v + d], [w, h]), "west": ([u + d + w, v + d], [d, h]),
                     "up": ([u + d, v], [w, d]), "down": ([u + d + w, v + d], [w, -d])}
            cube_ = {"origin": [round(-hi[0], 4), round(lo[1], 4), round(lo[2], 4)], "size": [round(w, 4), round(h, 4), round(d, 4)],
                     "uv": {k: {"uv": [round(a, 4) for a in uv_], "uv_size": [round(a, 4) for a in sz]} for k, (uv_, sz) in faces.items()}}
            if e.get("inflate"):
                cube_["inflate"] = e["inflate"]
            cubes.append(cube_)
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
        for ch in grp["children"]:
            if isinstance(ch, dict):
                walk(ch, grp["name"])
    for grp in model["outliner"]:
        walk(grp, None)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.skinwalker", "texture_width": 128, "texture_height": 128,
                        "visible_bounds_width": 4, "visible_bounds_height": 7, "visible_bounds_offset": [0, 3.5, 0]},
        "bones": bones}]}

    def catmull(p0, p1, p2, p3, s_):
        return 0.5 * (2 * p1 + (-p0 + p2) * s_ + (2 * p0 - 5 * p1 + 4 * p2 - p3) * s_ * s_ + (-p0 + 3 * p1 - 3 * p2 + p3) * s_ ** 3)
    def sample(ks, t, loop, linear):
        times = [k["time"] for k in ks]; n = len(ks)
        vals = [[float(k["data_points"][0][c]) for c in "xyz"] for k in ks]
        if n == 1:
            return vals[0]
        i = min(max([j for j in range(n) if times[j] <= t + 1e-9] or [0]), n - 2)
        s_ = max(0.0, min(1.0, (t - times[i]) / (times[i + 1] - times[i])))
        if linear:
            return [vals[i][c] + (vals[i + 1][c] - vals[i][c]) * s_ for c in range(3)]
        p0 = vals[i - 1] if i > 0 else (vals[-2] if loop else vals[0])
        p3 = vals[i + 2] if i + 2 < n else (vals[1] if loop else vals[-1])
        return [catmull(p0[c], vals[i][c], vals[i + 1][c], p3[c], s_) for c in range(3)]
    def thin(points, tol):
        """Keep only the samples a straight line between kept neighbours doesn't reproduce (Douglas-Peucker)."""
        keep = {0, len(points) - 1}
        stack = [(0, len(points) - 1)]
        while stack:
            a, b = stack.pop()
            ta, va = points[a]; tb, vb = points[b]; worst, wi = 0.0, None
            for i in range(a + 1, b):
                t, v = points[i]; u = (t - ta) / (tb - ta)
                err = max(abs(v[c] - (va[c] + (vb[c] - va[c]) * u)) for c in range(3))
                if err > worst:
                    worst, wi = err, i
            if wi is not None and worst > tol:
                keep.add(wi); stack += [(a, wi), (wi, b)]
        return [points[i] for i in sorted(keep)]
    anims = {}
    for a in model["animations"]:
        L = a["length"]; loop = a["loop"] == "loop"
        n = max(1, int(round(L * SAMPLE_RATE)))
        out = {}
        for an in a["animators"].values():
            for ch, tol in (("rotation", 0.15), ("position", 0.02)):
                ks = sorted([k for k in an["keyframes"] if k["channel"] == ch], key=lambda k: k["time"])
                if not ks:
                    continue
                linear = all(k.get("interpolation") == "linear" for k in ks)
                times = sorted({round(L * i / n, 5) for i in range(n + 1)} | {round(k["time"], 5) for k in ks if 0 <= k["time"] <= L})
                pts = [(t, sample(ks, t, loop, linear)) for t in times]      # every key, plus 40 samples a second
                if all(max(abs(x) for x in v) < 1e-3 for _, v in pts):
                    continue                                     # never leaves its rest position here
                pts = thin(pts, tol)
                out.setdefault(an["name"], {})[ch] = {f"{round(t, 4)}": [round(x, 3) for x in v] for t, v in pts}
        anims[a["name"]] = {"loop": True if loop else ("hold_on_last_frame" if a["loop"] == "hold" else False),
                            "animation_length": L, "bones": out}
    os.makedirs(os.path.join(ASSETS, "geo", "entity"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "animations", "entity"), exist_ok=True)
    json.dump(geo, open(os.path.join(ASSETS, "geo", "entity", "skinwalker.geo.json"), "w"), separators=(",", ":"))
    json.dump({"format_version": "1.8.0", "animations": anims},
              open(os.path.join(ASSETS, "animations", "entity", "skinwalker.animation.json"), "w"), separators=(",", ":"))

if __name__ == "__main__":
    model = build()
    json.dump(model, open(os.path.join(OUT, "skinwalker_blockout.bbmodel"), "w"), indent=1)
    export_geckolib(model)
    sheet(os.path.join(OUT, "skinwalker_reference.png"))
    frames = [render(*walk_frame(i / 4), "side", title=f"walk {i}/4") for i in range(4)]
    strip = Image.new("RGB", (sum(f.width for f in frames), frames[0].height)); x = 0
    for f in frames: strip.paste(f, (x, 0)); x += f.width
    strip.save(os.path.join(PREVIEW, "walk_strip.png"))
    print("standing height", round(highest(STAND, REST_LIFT) / 16, 2), "blocks; rest-on-fours",
          round(highest(REST4, -lowest(REST4)) / 16, 2), "; moving", round(highest(MOVE4, -lowest(MOVE4)) / 16, 2))
