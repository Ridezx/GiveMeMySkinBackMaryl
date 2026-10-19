"""The Toy Gun as a 3D model for holding (after the reference photo of the toy lever-action rifle), writing
src/main/resources/assets/horrormod/models/item/toy_gun_held.json and textures/item/toy_gun_held.png.

models/item/toy_gun.json (hand-written) shows this model in the hands (first and third person) and the flat
pixel-art icon (textures/item/toy_gun.png) everywhere else: the inventory, on the ground, in item frames.

The rifle is built pointing north (-z: the muzzle at the front), top up, with its grip at the model's centre point
(8, 8, 8 after the display offset), because that's how the hand renderers hold an untransformed item model: north
points straight out of the hand (forward when aiming, into the screen in first person).

Run: python3 art/tools/toy_gun_model.py   (needs Pillow; overwrites both files)
"""
import json, os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod"))
U = 4                                   # texture pixels per UV unit (64x64 texture, 16 UV units)

# texture swatches (UV units): flat colours, the game's lighting shades the faces
SWATCH = {
    "red": (0, 0, 4, 4), "red_light": (4, 0, 8, 4), "black": (8, 0, 12, 4), "black_light": (12, 0, 16, 4),
    "brown": (0, 4, 4, 8), "brown_dark": (4, 4, 8, 8), "brown_checker": (8, 4, 12, 8), "smoke": (12, 4, 16, 8),
    "barrel": (0, 8, 16, 10),           # black with the light dashed stripe along it
}

def texture():
    img = Image.new("RGBA", (16 * U, 16 * U), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    fill = {"red": (214, 40, 36), "red_light": (236, 84, 70), "black": (30, 30, 34), "black_light": (64, 66, 74),
            "brown": (112, 58, 42), "brown_dark": (70, 36, 26), "brown_checker": (100, 52, 38), "smoke": (96, 88, 80)}
    for name, colour in fill.items():
        u0, v0, u1, v1 = SWATCH[name]
        d.rectangle((u0 * U, v0 * U, u1 * U - 1, v1 * U - 1), fill=colour + (255,))
    u0, v0, u1, v1 = SWATCH["brown_checker"]
    for y in range(v0 * U, v1 * U):
        for x in range(u0 * U, u1 * U):
            if (x + y) % 2:
                img.putpixel((x, y), (78, 40, 28, 255))     # the checkered grip
    u0, v0, u1, v1 = SWATCH["barrel"]
    d.rectangle((u0 * U, v0 * U, u1 * U - 1, v1 * U - 1), fill=(30, 30, 34, 255))
    for x in range(u0 * U, u1 * U, 6):
        d.line((x, v0 * U + 3, x + 3, v0 * U + 3), fill=(210, 210, 214, 255))   # the dashed stripe
    return img

# parts: (name, from, to, swatch, optional rotation)
PARTS = [
    ("muzzle cap", (7.2, 8.8, -2.6), (8.8, 10.4, -1.4), "red"),
    ("front sight", (7.7, 10.4, -2.4), (8.3, 11.1, -1.8), "red_light"),
    ("barrel", (7.4, 9.0, -1.4), (8.6, 10.2, 7.0), "barrel"),
    ("tube under the barrel", (7.5, 7.9, -0.6), (8.5, 8.9, 6.5), "red"),
    ("tube cap", (7.4, 7.8, -1.2), (8.6, 9.0, -0.6), "red_light"),
    ("barrel band", (7.3, 7.7, 1.2), (8.7, 10.3, 1.8), "black_light"),
    ("forend", (7.25, 7.0, 4.6), (8.75, 8.2, 7.4), "brown_checker"),
    ("receiver", (7.2, 7.6, 6.5), (8.8, 10.4, 10.6), "black"),
    ("red button", (8.8, 8.9, 9.4), (9.0, 9.5, 10.0), "red"),
    ("scope", (7.4, 10.9, 4.8), (8.6, 12.0, 9.8), "smoke"),
    ("scope objective", (7.2, 10.7, 4.2), (8.8, 12.2, 5.0), "smoke"),
    ("scope eyepiece", (7.3, 10.8, 9.6), (8.7, 12.1, 10.4), "black_light"),
    ("scope mount front", (7.7, 10.4, 5.8), (8.3, 10.9, 6.4), "black"),
    ("scope mount back", (7.7, 10.4, 8.4), (8.3, 10.9, 9.0), "black"),
    ("lever loop bottom", (7.6, 5.6, 8.4), (8.4, 6.0, 10.8), "black"),
    ("lever loop front", (7.6, 6.0, 8.4), (8.4, 7.6, 8.8), "black"),
    ("lever loop back", (7.6, 6.0, 10.4), (8.4, 7.6, 10.8), "black"),
    ("trigger", (7.8, 6.6, 9.2), (8.2, 7.6, 9.6), "red"),
    ("grip", (7.3, 6.6, 10.6), (8.7, 9.6, 12.6), "brown_checker"),
    ("stock", (7.2, 6.0, 12.4), (8.8, 9.4, 17.0), "brown", {"angle": 22.5, "axis": "x", "origin": [8, 9.4, 12.4]}),
    ("butt pad", (7.15, 5.9, 16.8), (8.85, 9.5, 17.5), "brown_dark", {"angle": 22.5, "axis": "x", "origin": [8, 9.4, 12.4]}),
]

GRIP = (8.0, 7.6, 11.4)                 # where the hand holds it
# Third person, held in one hand / shouldered: smaller; first person: a little bigger, close to the eye.
SCALE_THIRD, SCALE_FIRST = 0.6, 0.8

def display(scale):
    # the grip ends up at the hand: scale * (grip - 8) + translation = 0 (in pixels)
    t = [round(-scale * (GRIP[i] - 8.0), 3) for i in range(3)]
    return {"rotation": [0, 0, 0], "translation": t, "scale": [scale, scale, scale]}

def model():
    elements = []
    for part in PARTS:
        name, a, b, swatch = part[:4]
        e = {"__comment": name, "from": list(a), "to": list(b),
             "faces": {f: {"uv": list(SWATCH[swatch]), "texture": "#gun"} for f in ("north", "south", "east", "west", "up", "down")}}
        if len(part) > 4:
            e["rotation"] = part[4]
        elements.append(e)
    return {"textures": {"gun": "horrormod:item/toy_gun_held", "particle": "horrormod:item/toy_gun_held"},
            "gui_light": "front",
            "display": {"thirdperson_righthand": display(SCALE_THIRD), "thirdperson_lefthand": display(SCALE_THIRD),
                        "firstperson_righthand": display(SCALE_FIRST), "firstperson_lefthand": display(SCALE_FIRST)},
            "elements": elements}

if __name__ == "__main__":
    texture().save(os.path.join(ASSETS, "textures", "item", "toy_gun_held.png"))
    m = json.dumps(model(), indent=2)
    import re
    m = re.sub(r"\[\s+([-0-9.,\s]+?)\s+\]", lambda mm: "[" + ", ".join(x.strip() for x in mm.group(1).split(",")) + "]", m)
    open(os.path.join(ASSETS, "models", "item", "toy_gun_held.json"), "w").write(m + "\n")
    print("wrote toy_gun_held.json and toy_gun_held.png")
