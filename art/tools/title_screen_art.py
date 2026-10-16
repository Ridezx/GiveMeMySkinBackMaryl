"""Draws the title screen's art (see client/HorrorTitleScreen.java) into
src/main/resources/assets/horrormod/textures/gui/title/:

    name.png        GIVE ME BACK MY SKIN MARYL in IM Fell English Italic (art/fonts, SIL Open Font License), slanted
                    far over, faded, in a deep red that's almost black
    skinwalker.png  the Skinwalker (its real model and texture) leaning out from behind the right edge of the
                    screen, one hand gripping the edge, watching; dark and faded, with its eyes glowing

Run: python3 art/tools/title_screen_art.py   (needs Pillow; re-run after changing the model or its texture)
The model is drawn with the texture painted on (each texture pixel placed on the posed model the same way
skinwalker_texture.py painted it), lit dimly from the front left.
"""
import importlib.util, math, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "horrormod", "textures")
OUT_DIR = os.path.join(ASSETS, "gui", "title")

_argv = sys.argv; sys.argv = ["x", "/tmp", "/tmp"]
_spec = importlib.util.spec_from_file_location("skinwalker_texture", os.path.join(HERE, "skinwalker_texture.py"))
tx = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(tx); sys.argv = _argv
g = tx.g

# ---------------------------------------------------------------- the name, along the bottom
NAME = "GIVE ME BACK MY SKIN MARYL"
NAME_RED = (46, 4, 8)           # deep red, almost black
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
            ip[x, y] = int(mp[x, y] * (0.45 + 0.4 * n))
    out = Image.new("RGBA", (W, H), NAME_RED + (0,)); out.putalpha(ink)
    return out.crop(out.getbbox())

# ---------------------------------------------------------------- the Skinwalker, peeking
def peek_pose():
    """Leaning far out to the left from behind the edge, head cocked, the near arm hanging limp, the far hand
    gripping the edge above its head. Returns the pose and where the screen edge falls (model x)."""
    p = dict(g.STAND)
    p["hips"] = (-4, 0, -24); p["torso"] = (0, 0, -10); p["chest"] = (-16, 8, -22)
    p["neck"] = (12, 10, -12); p["head"] = (6, 16, 36); p["jaw"] = (-16, 0, 0)
    p["left_arm"] = (10, 0, 30); p["left_forearm"] = (-28, 0, 0); p["left_hand"] = (-8, 0, 0)
    g.set_fingers(p, "left", 25, 30)
    head = g.world((0, 79, -2), "head", p, g.REST_LIFT)
    sh = g.world(g.PIVOT["right_arm"], "right_arm", p, g.REST_LIFT)
    edge = sh[0] + 1.5                                  # the screen edge, just past the far shoulder
    hand = (edge + 0.5, head[1] + 9, head[2] - 4)       # wrist just behind the edge, above the head
    e = g.ik_joint(sh, hand, 15, 16, (-0.8, -1.0, 0.5))
    g.aim(p, "right_arm", g._sub(e, sh)); g.aim(p, "right_forearm", g._sub(hand, e))
    # long fingers draped across the edge pointing left and down, palm pressed against it (we see the back of the
    # hand), the fingertips hooking round it
    p["right_hand"] = hand_facing(p, "right", (1.0, -0.7, 0.0), (0.0, 0.0, 1.0))
    g.set_fingers(p, "right", 25, 35, (20, 15))
    return p, edge

def hand_facing(pose, side, fingers, palm):
    """Local hand rotation with the fingers pointing along world direction fingers and the palm facing palm."""
    y = g._norm(tuple(-v for v in fingers))                 # the fingers point down the hand's local -Y
    z = g._norm(g._sub(palm, g._mul(y, g._dot(palm, y))))   # the palm faces local +Z
    x = (y[1] * z[2] - y[2] * z[1], y[2] * z[0] - y[0] * z[2], y[0] * z[1] - y[1] * z[0])
    target = [[x[i], y[i], z[i]] for i in range(3)]
    return g.euler_zyx(g.mmul(g.mT(g.world_rot(f"{side}_forearm", pose)), target))

LIGHT = (0.55, 0.45, -0.7)                              # from the front, up and to the left (screen left = +x)
SCALE = int(os.environ.get("TITLE_ART_SCALE", 26))                                             # image pixels per model pixel (before downsampling)
SS = 2                                                  # supersampling

def render_textured(pose, lift, edge, top, bottom):
    """Front view (facing the camera), screen x = -model x. The image's right side is the screen edge."""
    skin = Image.open(os.path.join(ASSETS, "entity", "skinwalker.png")).convert("RGBA").load()
    glow = Image.open(os.path.join(ASSETS, "entity", "skinwalker_glowmask.png")).convert("RGBA").load()
    s = SCALE * SS
    left = edge + 40                                    # model x shown at the image's left edge
    W = int((left - edge) * s); H = int((top - bottom) * s)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    eyes = Image.new("L", (W, H), 0); de = ImageDraw.Draw(eyes)
    L = g._norm(LIGHT)
    quads = []
    T = tx.TEXELS
    for i, c in enumerate(g.CUBES):
        name, bone, a, b, tone, inf = c
        lo = tuple(min(a[k], b[k]) for k in range(3)); hi = tuple(max(a[k], b[k]) for k in range(3))
        cc = (name, bone, lo, hi)
        rot = g.world_rot(bone, pose)
        for face, (fu, fv, fw, fh) in tx.face_rects(cc, g.UV[i]).items():
            _, n = tx.face_point(cc, face, 0.5, 0.5)
            nw = g.mvec(rot, n)
            if nw[2] > 0.05:                            # facing away from the camera
                continue
            shade = 0.22 + 0.78 * max(0.0, g._dot(nw, L))
            x0t, x1t = int(math.floor(fu * T)), int(math.ceil((fu + fw) * T))
            y0t, y1t = int(math.floor(fv * T)), int(math.ceil((fv + fh) * T))
            for py in range(y0t, max(y1t, y0t + 1)):
                for px in range(x0t, max(x1t, x0t + 1)):
                    def st(qx, qy):
                        ss_ = (qx / T - fu) / fw if fw else 0.5
                        tt = (qy / T - fv) / fh if fh else 0.5
                        return max(0.0, min(1.0, ss_)), max(0.0, min(1.0, tt))
                    pts = []
                    for qx, qy in ((px, py), (px + 1, py), (px + 1, py + 1), (px, py + 1)):
                        sv, tv = st(qx, qy)
                        p3, _ = tx.face_point(cc, face, sv, tv)
                        w = g.world(p3, bone, pose, lift)
                        pts.append(w)
                    col = skin[px, py]
                    if col[3] == 0:
                        continue
                    lit = glow[px, py][3] > 0
                    depth = sum(q[2] for q in pts) / 4
                    scr = [((left - q[0]) * s, (top - q[1]) * s) for q in pts]
                    rgb = col[:3] if lit else tuple(int(v * shade) for v in col[:3])
                    quads.append((depth, scr, rgb, lit))
    for depth, scr, rgb, lit in sorted(quads, key=lambda q: -q[0]):
        d.polygon(scr, fill=rgb + (255,))
        de.polygon(scr, fill=255 if lit else 0)
    return img.resize((W // SS, H // SS), Image.LANCZOS), eyes.resize((W // SS, H // SS), Image.LANCZOS)

def skinwalker():
    pose, edge = peek_pose()
    lift = g.REST_LIFT
    top = g.highest(pose, lift) + 6
    bottom = top - 56                                   # hand, head, shoulders and chest; the rest is below the screen
    img, eyes = render_textured(pose, lift, edge, top, bottom)
    W, H = img.size
    # dark and faded: desaturated, dimmed, and fading out toward the bottom and away from the edge
    r, gch, b, a = img.split()
    grey = Image.merge("RGB", (r, gch, b)).convert("L")
    rgb = Image.blend(Image.merge("RGB", (r, gch, b)), Image.merge("RGB", (grey, grey, grey)), 0.35)
    rgb = rgb.point(lambda v: int(v * 0.62))
    fade = Image.new("L", (W, H)); fp = fade.load()
    for y in range(H):
        for x in range(W):
            down = 1.0 - max(0.0, (y / H - 0.45) / 0.55) ** 1.4
            across = min(1.0, 0.35 + 0.65 * (x / W) ** 0.6)
            fp[x, y] = int(255 * 0.78 * down * across)
    a = Image.composite(Image.new("L", (W, H), 0), a, Image.new("L", (W, H), 0))
    a = Image.fromarray if False else a
    alpha = Image.new("L", (W, H)); ap = alpha.load(); aa = a.load(); ff = fade.load()
    for y in range(H):
        for x in range(W):
            ap[x, y] = aa[x, y] * ff[x, y] // 255
    out = rgb.convert("RGBA"); out.putalpha(alpha)
    # the eyes stay bright, with a soft glow around them
    eye_rgb = Image.new("RGBA", (W, H), tx.EYE_EDGE + (0,))
    bloom = eyes.filter(ImageFilter.GaussianBlur(14)).point(lambda v: min(255, v * 2))
    eye_rgb.putalpha(bloom)
    out = Image.alpha_composite(out, eye_rgb)
    core = img.copy(); core.putalpha(eyes)
    out = Image.alpha_composite(out, core)
    return out

if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    name_text().save(os.path.join(OUT_DIR, "name.png"))
    sw = skinwalker(); sw.save(os.path.join(OUT_DIR, "skinwalker.png"))
    print("wrote", OUT_DIR, "name.png and skinwalker.png", sw.size)
