"""First versions of the Sandman's gear and the bleeding effect's textures (all can be painted over):

    textures/item/tactical_vest.png           16x16  the plate-carrier vest: black, mag pouches, a little blue screen
    textures/item/tactical_boots.png          16x16  a pair of black laced combat boots with heavy soles
    textures/models/armor/tactical_layer_1.png 64x32 the gear as worn (vanilla's armor layout): the vest over the
                                                     body (no sleeves), the boots on the lower legs
    textures/mob_effect/bleeding.png          18x18  a drop of blood (the effect's icon)

Run: python3 art/tools/tactical_textures.py   (needs Pillow; overwrites those files)
"""
import os, random
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod", "textures"))
rnd = random.Random(3)

VEST, VEST_DARK, VEST_LIGHT = (36, 38, 44), (20, 21, 25), (58, 60, 68)
STRAP, POUCH, SCREEN, SCREEN_LIGHT = (14, 14, 18), (44, 45, 52), (40, 80, 180), (110, 160, 240)
BOOT, BOOT_LIGHT, SOLE, LACE = (18, 18, 22), (44, 44, 52), (52, 52, 58), (70, 74, 88)
BLOOD, BLOOD_DARK, BLOOD_LIGHT = (150, 12, 16), (86, 4, 8), (220, 70, 70)

def grid(rows, palette):
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                c = palette[ch]
                v = rnd.randint(-4, 4)
                img.putpixel((x, y), tuple(max(0, min(255, k + v)) for k in c) + (255,))
    return img

def vest_icon():
    return grid([
        "................",
        "...ss......ss...",
        "...ss......ss...",
        "..dvvd....dvvd..",
        "..dvvvdddddvvvd.",
        ".dvvvvvvvvvvvvd.",
        ".dvlllvvvvvlllvd",
        ".dvvvvvqbbqvvvvd",
        ".dvvvvvqBbqvvvvd",
        ".dvvvvvvvvvvvvvd",
        ".dppppppppppppd.",
        ".dpLpLppLpLppLd.",
        ".dppppppppppppd.",
        ".ddddddddddddd..",
        "................",
        "................"],
        {"s": STRAP, "d": VEST_DARK, "v": VEST, "l": VEST_LIGHT, "q": STRAP, "b": SCREEN, "B": SCREEN_LIGHT,
         "p": POUCH, "L": VEST_LIGHT})

def boots_icon():
    return grid([
        "................",
        "..kkkk...kkkk...",
        "..kbbk...kbbk...",
        "..kblk...kblk...",
        "..kbbk...kbbk...",
        "..klbk...klbk...",
        "..kbbk...kbbk...",
        "..klbk...klbk...",
        "..kbbk...kbbk...",
        "..kbbbk..kbbbk..",
        "..kbbbbk.kbbbbk.",
        "..kbbbbbkkbbbbbk",
        "..ssssssksssssss",
        "..ssssss.ssssss.",
        "................",
        "................"],
        {"k": BOOT, "b": BOOT, "l": LACE, "s": SOLE})

def armor_layer():
    """Vanilla's armor texture layout (64x32): the body at (16,16) (faces 8x12 front/back, 4x12 sides, 8x4 top and
    bottom), legs at (0,16) (4x12 faces, both legs share it). Head and arms left empty: no helmet, no sleeves."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0)); px = img.load()
    def fill(x0, y0, w, h, fn):
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                c = fn(x - x0, y - y0, w, h)
                if c:
                    v = rnd.randint(-4, 4)
                    px[x, y] = tuple(max(0, min(255, k + v)) for k in c) + (255,)
    def vest(x, y, w, h, front=False):
        if y < 1 and (x < 2 or x >= w - 2):
            return STRAP                                          # shoulder straps
        if y < 1:
            return None
        if x == 0 or x == w - 1 or y == h - 1:
            return VEST_DARK
        if front and 7 <= y <= 9:
            return POUCH if (x % 3) else VEST_DARK                # mag pouches
        if front and 2 <= y <= 3 and 2 <= x <= 4:
            return SCREEN_LIGHT if (x + y) % 2 else SCREEN       # the little screen
        if y % 3 == 0:
            return VEST_DARK                                      # webbing rows
        return VEST
    fill(20, 20, 8, 12, lambda x, y, w, h: vest(x, y, w, h, front=True))    # body front
    fill(32, 20, 8, 12, lambda x, y, w, h: vest(x, y, w, h))                # back
    fill(16, 20, 4, 12, lambda x, y, w, h: vest(x, y, w, h))                # sides
    fill(28, 20, 4, 12, lambda x, y, w, h: vest(x, y, w, h))
    fill(20, 16, 8, 4, lambda x, y, w, h: STRAP if x < 2 or x >= w - 2 else None)   # over the shoulders
    def boot(x, y, w, h, front=False):
        if y < 5:
            return None                                           # the boots come up to mid-shin
        if y == 5:
            return BOOT_LIGHT                                     # the cuff
        if y == h - 1:
            return SOLE
        if front and 6 <= y <= 9 and x in (1, 2):
            return LACE if (y + x) % 2 else BOOT
        return BOOT
    for x0, front in ((0, False), (4, True), (8, False), (12, False)):    # leg faces: right side, front, left, back
        fill(x0, 20, 4, 12, lambda x, y, w, h, f=front: boot(x, y, w, h, f))
    fill(8, 16, 4, 4, lambda x, y, w, h: SOLE)                   # the leg's bottom: soles
    return img

def bleeding_icon():
    return grid([
        "..................",
        "........dd........",
        "........dd........",
        ".......drrd.......",
        ".......drrd.......",
        "......drrrrd......",
        "......drrlrd......",
        ".....drrrllrd.....",
        ".....drrrrlrd.....",
        "....drrrrrrrrd....",
        "....drrrrrrrrd....",
        "....drrrrrrrrd....",
        "....ddrrrrrrdd....",
        ".....ddrrrrdd.....",
        "......dddddd......",
        "..................",
        "..................",
        ".................."],
        {"d": BLOOD_DARK, "r": BLOOD, "l": BLOOD_LIGHT})

if __name__ == "__main__":
    out = {"item/tactical_vest.png": vest_icon(), "item/tactical_boots.png": boots_icon(),
           "models/armor/tactical_layer_1.png": armor_layer(), "mob_effect/bleeding.png": bleeding_icon()}
    for path, img in out.items():
        full = os.path.join(TEX, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        img.save(full)
    print("wrote", ", ".join(out))
