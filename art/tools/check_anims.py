"""Checks the Skinwalker animations the way Blockbench plays them: reads art/skinwalker_blockout.bbmodel,
samples each animation with Blockbench's catmull-rom interpolation, and poses the rig with the generator's
forward kinematics (art/tools/skinwalker_anim.py).

    python3 art/tools/check_anims.py check   [names]   ground contact and joint jumps per animation
    python3 art/tools/check_anims.py preview names [view] [out.png]   filmstrip of 8 frames each (view: side,
                                                                      front, three; default three)
    python3 art/tools/check_anims.py rest    names     how high each body part ends above the ground (last frame)

names is a comma-separated list of animation names (all animations if left out, for check).
"check" prints, per animation: the lowest point over the whole animation (below 0 = something goes into the
ground; -1 or more is worth a look, except climbing, swimming and hanging from a ledge, which are meant to
be off the ground) and the biggest change of any bone between two samples (240 per animation; a large one
is a snap).
"""
import importlib.util, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
MODEL = os.path.join(HERE, "..", "skinwalker_blockout.bbmodel")

_argv = sys.argv; sys.argv = ["x", "/tmp", "/tmp"]
_spec = importlib.util.spec_from_file_location("skinwalker_anim", os.path.join(HERE, "skinwalker_anim.py"))
g = importlib.util.module_from_spec(_spec); _spec.loader.exec_module(g); sys.argv = _argv

def _catmull(p0, p1, p2, p3, s):
    return 0.5 * (2 * p1 + (-p0 + p2) * s + (2 * p0 - 5 * p1 + 4 * p2 - p3) * s * s + (-p0 + 3 * p1 - 3 * p2 + p3) * s ** 3)

def load(path=MODEL):
    d = json.load(open(path)); groups = {}
    def walk(n):
        groups[n["uuid"]] = (n["name"], n.get("rotation", [0, 0, 0]))
        for c in n["children"]:
            if isinstance(c, dict): walk(c)
    for n in d["outliner"]: walk(n)
    return d, groups

def sampler(anim, groups):
    """pose_at(t) -> (pose, lift) for one animation, as Blockbench would show it."""
    loop = anim["loop"] == "loop"; tracks = {}
    for u, a in anim["animators"].items():
        name, rest = groups[u]
        for ch in ("rotation", "position"):
            ks = sorted([k for k in a["keyframes"] if k["channel"] == ch], key=lambda k: k["time"])
            if ks: tracks[(name, ch)] = (rest, ks)
    def sample(ks, t, axis):
        times = [k["time"] for k in ks]; vals = [float(k["data_points"][0][axis]) for k in ks]; n = len(ks)
        if n == 1: return vals[0]
        i = min(max([j for j in range(n) if times[j] <= t + 1e-9] or [0]), n - 2)
        s = (t - times[i]) / (times[i + 1] - times[i])
        p0 = vals[i - 1] if i > 0 else (vals[-2] if loop else vals[0])
        p3 = vals[i + 2] if i + 2 < n else (vals[1] if loop else vals[-1])
        return _catmull(p0, vals[i], vals[i + 1], p3, s)
    def pose_at(t):
        pose, lift = {}, g.REST_LIFT
        for (name, ch), (rest, ks) in tracks.items():
            x, y, z = (sample(ks, t, a) for a in "xyz")
            if ch == "rotation":
                pose[name] = (rest[0] - x, rest[1] - y, rest[2] + z)
            elif name == "root":
                lift = y + g.REST_LIFT
            else:
                pose["pos:" + name] = (x, y, z)
        return pose, lift
    return pose_at

def check(names=None):
    d, groups = load()
    for anim in d["animations"]:
        if names and anim["name"] not in names: continue
        L = anim["length"]; pose_at = sampler(anim, groups)
        N = 240; lows = []; prev = None; jump, jb, jt = 0.0, None, 0.0
        for i in range(N + 1):
            p, lift = pose_at(i / N * L)
            lows.append(g.lowest(p) + lift)
            if prev:
                for b in p:
                    if ":" in b: continue
                    dd = max(abs(p[b][j] - prev[b][j]) for j in range(3))
                    if dd > jump: jump, jb, jt = dd, b, i / N * L
            prev = p
        lo = min(lows)
        print(f"{anim['name']:28s} {L:5.2f}s  lowest {lo:+7.2f} px at {lows.index(lo) / N * L:5.2f}s"
              f"  |  biggest step {jump:5.1f} deg on {jb} at {jt:.2f}s")

def preview(names, view="three", out="anim_preview.png"):
    from PIL import Image
    d, groups = load(); W, H, N = 200, 240, 8
    sheet = Image.new("RGB", (W * N, H * len(names)), (10, 10, 10))
    for r, name in enumerate(names):
        anim = next(a for a in d["animations"] if a["name"] == name); L = anim["length"]; pose_at = sampler(anim, groups)
        for c in range(N):
            t = L * c / (N - 1); pose, lift = pose_at(t)
            title = f"{name[:22]} {t:.2f}" if c == 0 else f"{t:.2f}"
            sheet.paste(g.render(pose, lift, view, size=(W, H), scale=2.0, title=title), (c * W, r * H))
    sheet.save(out); print("wrote", out)

def rest(names):
    d, groups = load()
    toes = [f"toe{i}" for i in range(1, 6)]
    for name in names:
        anim = next(a for a in d["animations"] if a["name"] == name)
        pose, lift = sampler(anim, groups)(anim["length"])
        print("==", name)
        for side in ("right", "left"):
            parts = [("head", ("head", "jaw")), ("chest", ("chest",)), ("belly/hips", ("torso", "hips")),
                     ("upper arm", (f"{side}_arm",)), ("forearm", (f"{side}_forearm",)),
                     ("hand", (f"{side}_hand",) + tuple(f"{side}_finger{i}{t}" for i in range(1, 5) for t in ("", "_tip"))),
                     ("thigh", (f"{side}_leg",)), ("shin", (f"{side}_shin",)), ("foot", (f"{side}_foot",)),
                     ("toes", tuple(f"{side}_{t}{x}" for t in toes for x in ("", "_tip")))]
            print(f"  {side:5s}", {n: round(min(g.world(q, c[1], pose, lift)[1] for c in g.CUBES if c[1] in b
                                                  for q in g.corners(c)), 1) for n, b in parts})

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "check"
    names = sys.argv[2].split(",") if len(sys.argv) > 2 else None
    if cmd == "check":
        check(names)
    elif cmd == "preview":
        preview(names, sys.argv[3] if len(sys.argv) > 3 else "three", sys.argv[4] if len(sys.argv) > 4 else "anim_preview.png")
    elif cmd == "rest":
        rest(names)
    else:
        print(__doc__)
