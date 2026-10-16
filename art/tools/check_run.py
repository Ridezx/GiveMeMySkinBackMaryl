import sys, json, importlib.util
spec = importlib.util.spec_from_file_location("g", sys.argv[1] + "/skinwalker_anim.py"); g = importlib.util.module_from_spec(spec)
out = "/tmp"; path = sys.argv[2]; sys.argv = ["x", out, out]; spec.loader.exec_module(g)
d = json.load(open(path)); groups = {}
def walk(n):
    groups[n["uuid"]] = (n["name"], n.get("rotation", [0, 0, 0]))
    for c in n["children"]:
        if isinstance(c, dict): walk(c)
for n in d["outliner"]: walk(n)
anim = [a for a in d["animations"] if a["name"] == "run_standing"][0]; L = anim["length"]
tracks = {}
for u, a in anim["animators"].items():
    name, rest = groups[u]
    for ch in ("rotation", "position"):
        ks = sorted([k for k in a["keyframes"] if k["channel"] == ch], key=lambda k: k["time"])
        if ks: tracks[(name, ch)] = (rest, ks)
def cr(p0, p1, p2, p3, s): return 0.5 * (2 * p1 + (-p0 + p2) * s + (2 * p0 - 5 * p1 + 4 * p2 - p3) * s * s + (-p0 + 3 * p1 - 3 * p2 + p3) * s ** 3)
def sample(ks, t, axis):
    times = [k["time"] for k in ks]; vals = [float(k["data_points"][0][axis]) for k in ks]; n = len(ks)
    i = min(max(j for j in range(n) if times[j] <= t + 1e-9), n - 2); s = (t - times[i]) / (times[i + 1] - times[i])
    return cr(vals[i - 1] if i > 0 else vals[-2], vals[i], vals[i + 1], vals[i + 2] if i + 2 < n else vals[1], s)
def pose_at(t):
    pose, lift = {}, g.REST_LIFT
    for (name, ch), (rest, ks) in tracks.items():
        x, y, z = (sample(ks, t, a) for a in "xyz")
        if ch == "rotation": pose[name] = (rest[0] - x, rest[1] - y, rest[2] + z)
        else: lift = y + g.REST_LIFT
    return pose, lift
N = 600; prev = None; spikes = 0; lows = []; heights = []; toes = []
for i in range(N):
    p, l = pose_at(i / N * L)
    lows.append(min(g.world(q, c[1], p, l)[1] for c in g.CUBES for q in g.corners(c))); heights.append(l)
    toes.append(g._toe("right", p, l))
    if prev and max(abs(p[b][j] - prev[b][j]) for b in p for j in range(3)) > 1.5: spikes += 1
    prev = p
dirs = [1 if b > a else -1 for a, b in zip(heights, heights[1:]) if abs(b - a) > 1e-4]
print(f"jumps >1.5 deg between samples: {spikes} | body height direction changes per loop: {sum(1 for a, b in zip(dirs, dirs[1:]) if a != b)}")
print(f"lowest point {min(lows):+.2f} px | on ground (<0.5 px) {sum(v < 0.5 for v in lows)/N:.0%} | airborne peak {max(lows):.1f} px")
glide = [toes[i + 1][2] - toes[i][2] for i in range(N - 1) if toes[i][1] < 0.9 and toes[i + 1][1] < 0.9]
print(f"right foot while planted: per-step glide {min(glide):+.3f} .. {max(glide):+.3f} px, height on ground {max(t[1] for t in toes if t[1] < 0.9):.2f} max")
