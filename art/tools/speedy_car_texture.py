"""Paints the Speedy Car's texture: src/main/resources/assets/horrormod/textures/block/speedy_car.png (128x128).

The block model (models/block/speedy_car.json) is a toy-car package after the reference photo: a printed red
cardboard box with a black bottom band, a big clear plastic window on the front, and a low orange-red supercar
on display inside. No real brand names or logos.

Texture regions, in model UV units (16 across the texture, so 8 pixels each), used by the model's faces:
    FRONT   0-10 x 0-7    the front print; its middle (the window) is the printed backdrop inside the box
    SIDE   10-16 x 0-7    a side of the box
    TOP     0-10 x 7-13   the top, with the end flap
    BOTTOM  0-10 x 13-16
    PAINT  10-12 x 7-9    the car's paint      WINDOW 12-14 x 7-9   tinted car windows
    WHEEL  14-16 x 7-9    a wheel from the side   TREAD 10-11 x 9-10   tyre tread
    CARD   11-12 x 9-10   bare cardboard (cut edges and insides)   INNER 12-14 x 9-11   inside walls
    FLOOR  14-16 x 9-11   the display floor   CARSIDE 10-14 x 11-13  the car body from the side
    GLASS  14-16 x 11-13  the plastic window (see-through, with glare)

Run: python3 art/tools/speedy_car_texture.py   (needs Pillow; overwrites the file)
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "horrormod", "textures",
                                    "block", "speedy_car.png"))
U = 8                                       # pixels per UV unit

RED, RED_D, RED_L = (200, 34, 28), (150, 20, 18), (226, 64, 50)
LINE = (240, 150, 70)                       # the orange blueprint line art on the print
BLACK, BLACK_L = (24, 24, 28), (60, 60, 66)
YELLOW = (240, 200, 40)
PAINT, PAINT_L, PAINT_D = (232, 70, 44), (250, 120, 90), (170, 40, 26)

def box(d, r, fill):
    d.rectangle((r[0] * U, r[1] * U, r[2] * U - 1, r[3] * U - 1), fill=fill)

def grain(img, region, amount, seed):
    """Faint print/cardboard grain so flat colours don't look plastic."""
    rnd = random.Random(seed); px = img.load()
    for y in range(int(region[1] * U), int(region[3] * U)):
        for x in range(int(region[0] * U), int(region[2] * U)):
            r, g, b, a = px[x, y]
            k = rnd.randint(-amount, amount)
            px[x, y] = (max(0, min(255, r + k)), max(0, min(255, g + k)), max(0, min(255, b + k)), a)

def blueprint(d, x0, y0, x1, y1, seed):
    """Loose line drawings of car parts, like the technical sketches printed on the box."""
    rnd = random.Random(seed)
    for _ in range(5):
        cx, cy = rnd.uniform(x0, x1), rnd.uniform(y0, y1)
        w, h = rnd.uniform(10, 26), rnd.uniform(6, 14)
        d.arc((cx - w, cy - h, cx + w, cy + h), rnd.randint(180, 220), rnd.randint(320, 360), fill=LINE)
        d.line((cx - w, cy + h * 0.4, cx + w, cy + h * 0.4), fill=LINE)
    for i in range(3):
        y = y0 + (y1 - y0) * (i + 1) / 4
        d.line((x0 + 2, y, x0 + rnd.uniform(8, 20), y), fill=LINE)

def paint():
    img = Image.new("RGBA", (16 * U, 16 * U), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    try:
        font = ImageFont.load_default(size=9)
    except TypeError:
        font = ImageFont.load_default()

    # FRONT: red print; header band at the top (title on a dark winged banner), black base band with a label
    box(d, (0, 0, 10, 7), RED)
    d.rectangle((0, 0, 10 * U - 1, 2 * U - 1), fill=RED_D)
    d.polygon([(6, 3), (74, 3), (68, 13), (12, 13)], fill=BLACK)                       # the banner
    d.text((40, 8), "SPEEDY CAR", font=font, fill=(245, 245, 245), anchor="mm")
    box(d, (0, 5.5, 10, 7), BLACK)
    blueprint(d, 0, 5.5 * U, 10 * U, 7 * U, 3)
    d.rectangle((26, 45, 54, 52), fill=BLACK, outline=YELLOW)                         # the name label
    d.text((40, 48), "1:24", font=font, fill=(245, 245, 245), anchor="mm")
    d.ellipse((66, 44, 76, 54), fill=YELLOW, outline=(200, 60, 40))                    # age badge
    # the backdrop seen through the window: red with line art, darker toward the bottom
    box(d, (1, 2, 9, 5.5), RED_L)
    blueprint(d, 1 * U, 2 * U, 9 * U, 5.5 * U, 7)
    for y in range(int(4.4 * U), int(5.5 * U)):
        d.line((1 * U, y, 9 * U - 1, y), fill=RED_D)
    grain(img, (0, 0, 10, 7), 6, 1)

    # SIDE: red with line art over a black base band
    box(d, (10, 0, 16, 7), RED)
    blueprint(d, 10 * U, 0, 16 * U, 5 * U, 11)
    box(d, (10, 5, 16, 7), BLACK)
    d.text((13 * U, 6 * U), "1:24", font=font, fill=(235, 235, 235), anchor="mm")
    grain(img, (10, 0, 16, 7), 6, 2)

    # TOP: red, the end flap's crease near one end, and a warning sticker
    box(d, (0, 7, 10, 13), RED_D)
    d.line((8 * U, 7 * U, 8 * U, 13 * U - 1), fill=(110, 14, 12), width=2)
    d.rectangle((6, 7 * U + 6, 34, 7 * U + 16), fill=(250, 250, 250))
    d.rectangle((8, 7 * U + 8, 12, 7 * U + 14), fill=YELLOW)
    grain(img, (0, 7, 10, 13), 5, 3)

    # BOTTOM: plain black print
    box(d, (0, 13, 10, 16), BLACK); grain(img, (0, 13, 10, 16), 4, 4)

    # the car
    box(d, (10, 7, 12, 9), PAINT)
    d.rectangle((10 * U, 7 * U, 12 * U - 1, 7 * U + 3), fill=PAINT_L)                 # top highlight
    box(d, (12, 7, 14, 9), (40, 46, 56))
    d.line((12 * U + 2, 7 * U + 12, 12 * U + 12, 7 * U + 2), fill=(120, 130, 150), width=2)   # window glare
    box(d, (14, 7, 16, 9), BLACK)                                                      # wheel: tyre...
    d.ellipse((14 * U + 3, 7 * U + 3, 16 * U - 4, 9 * U - 4), fill=(46, 46, 52))      # ...black rim...
    for a in range(5):                                                                 # ...with spokes
        t = a * 2 * math.pi / 5; c = (15 * U, 8 * U)
        d.line((c[0], c[1], c[0] + math.cos(t) * 5, c[1] + math.sin(t) * 5), fill=(90, 90, 98), width=2)
    box(d, (10, 9, 11, 10), (20, 20, 22))
    box(d, (11, 9, 12, 10), (190, 160, 120)); grain(img, (11, 9, 12, 10), 10, 5)
    box(d, (12, 9, 14, 11), RED_D); grain(img, (12, 9, 14, 11), 5, 6)
    box(d, (14, 9, 16, 11), (30, 28, 30))                                             # display stand / floor
    d.line((14 * U, 9 * U, 16 * U - 1, 9 * U), fill=(70, 66, 70))
    # CARSIDE: body side with a door line, side intake and a darker sill
    box(d, (10, 11, 14, 13), PAINT)
    d.rectangle((10 * U, 11 * U, 14 * U - 1, 11 * U + 3), fill=PAINT_L)
    d.rectangle((10 * U, 13 * U - 4, 14 * U - 1, 13 * U - 1), fill=PAINT_D)
    d.line((12 * U - 4, 11 * U + 3, 12 * U - 2, 13 * U - 4), fill=PAINT_D)            # door line
    d.polygon([(12 * U + 6, 11 * U + 5), (12 * U + 14, 11 * U + 5), (12 * U + 10, 12 * U + 6),
               (12 * U + 4, 12 * U + 6)], fill=BLACK)                                 # side intake
    # GLASS: mostly clear, a faint tint and two glare streaks
    box(d, (14, 11, 16, 13), (230, 236, 240, 26))
    for off in (2, 9):
        d.line((14 * U + off, 13 * U - 1, 14 * U + off + 8, 11 * U), fill=(255, 255, 255, 80), width=2)
    return img

if __name__ == "__main__":
    paint().save(OUT)
    print("wrote", OUT)
