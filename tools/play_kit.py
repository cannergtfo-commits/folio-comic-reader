"""Builds the Google Play listing graphics for Blazar Reader into play-store-kit/.

Inputs: tools/icon/folio-icon.svg and Roborazzi screenshots (FOLIO_SHOTS dir, default /workspace/play-shots).
Needs: pip install cairosvg pillow   (e.g. python3 -m venv /workspace/play-kit-venv)
"""
import io, os, sys
import cairosvg
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
KIT = os.path.join(ROOT, "play-store-kit")
SHOTS_IN = os.environ.get("FOLIO_SHOTS", "/workspace/play-shots")
FONTS = os.path.join(ROOT, "app", "src", "main", "res", "font")
INK = (0x0E, 0x0D, 0x0B)
GOLD = (0xC9, 0xA9, 0x6E)
GOLD_HI = (0xF3, 0xDF, 0xAE)
CREAM = (0xEF, 0xE8, 0xD8)
MUTED = (0xA3, 0x9A, 0x8A)

SVG = open(os.path.join(ROOT, "tools", "icon", "folio-icon.svg")).read()
BG = '<rect width="108" height="108" fill="url(#bg)"/><circle cx="54" cy="52" r="34" fill="url(#halo)"/>'
assert BG in SVG


def render(svg, size):
    return Image.open(io.BytesIO(cairosvg.svg2png(bytestring=svg.encode(), output_width=size, output_height=size))).convert("RGBA")


def store_icon():
    # Same crop as the shipped launcher/store art (adaptive-icon safe zone), as 32-bit RGBA.
    im = render(SVG.replace('viewBox="0 0 108 108"', 'viewBox="13 13 82 82"'), 512)
    out = os.path.join(KIT, "icon-512-rgba.png")
    im.save(out, optimize=True)
    return out


def emblem(size):
    # Gold page emblem only, transparent background.
    return render(SVG.replace(BG, "").replace('viewBox="0 0 108 108"', 'viewBox="26 22 60 60"'), size)


def font(name, size):
    return ImageFont.truetype(os.path.join(FONTS, name), size)


def radial_glow(w, h, cx, cy, r, color, alpha):
    layer = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(layer)
    steps = 60
    for i in range(steps, 0, -1):
        rr = r * i / steps
        a = int(alpha * (1 - i / steps) ** 1.6)
        d.ellipse((cx - rr, cy - rr, cx + rr, cy + rr), fill=a)
    layer = layer.filter(ImageFilter.GaussianBlur(r / 10))
    glow = Image.new("RGBA", (w, h), color + (0,))
    glow.putalpha(layer)
    return glow


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=255)
    return m


def phone(shot_path, screen_h):
    shot = Image.open(shot_path).convert("RGB")
    sw = round(shot.width * screen_h / shot.height)
    screen = shot.resize((sw, screen_h), Image.LANCZOS)
    bez = 7
    W, H = sw + 2 * bez, screen_h + 2 * bez
    body = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle((0, 0, W - 1, H - 1), 30, fill=(22, 20, 17, 255), outline=(120, 100, 66, 255), width=2)
    d.rounded_rectangle((3, 3, W - 4, H - 4), 27, outline=(58, 50, 38, 255), width=1)
    body.paste(screen, (bez, bez), rounded_mask(screen.size, 24))
    return body


def drop_shadow(img, offset=(0, 14), blur=22, alpha=170):
    pad = blur * 3
    sh = Image.new("RGBA", (img.width + 2 * pad, img.height + 2 * pad), (0, 0, 0, 0))
    a = img.split()[3].point(lambda v: alpha if v else 0)
    blk = Image.new("RGBA", img.size, (0, 0, 0, 255))
    blk.putalpha(a)
    sh.paste(blk, (pad, pad), blk)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    return sh, (pad - offset[0], pad - offset[1])


def spaced(draw, xy, text, fnt, fill, tracking):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=fnt, fill=fill)
        x += draw.textlength(ch, font=fnt) + tracking
    return x


def spaced_width(draw, text, fnt, tracking):
    return sum(draw.textlength(c, font=fnt) for c in text) + tracking * (len(text) - 1)


def feature_graphic():
    W, H = 1024, 500
    canvas = Image.new("RGBA", (W, H), INK + (255,))
    canvas.alpha_composite(radial_glow(W, H, 250, 210, 430, GOLD, 70))
    canvas.alpha_composite(radial_glow(W, H, 800, 280, 360, GOLD, 34))
    # Subtle vignette.
    vig = Image.new("L", (W, H), 0)
    ImageDraw.Draw(vig).rectangle((0, 0, W, H), fill=0)
    edge = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ed = ImageDraw.Draw(edge)
    for i in range(40):
        ed.rectangle((i, i, W - 1 - i, H - 1 - i), outline=(0, 0, 0, int(90 * (1 - i / 40) ** 2)))
    canvas.alpha_composite(edge)

    # Phones (right): Library behind, Discover in front.
    back = phone(os.path.join(SHOTS_IN, "01_library_ink.png"), 336)
    front = phone(os.path.join(SHOTS_IN, "13_discover_ink.png"), 366)
    back = back.rotate(6, resample=Image.BICUBIC, expand=True)
    front = front.rotate(-4, resample=Image.BICUBIC, expand=True)
    for img, (x, y) in ((back, (770, (H - back.height) // 2 + 6)), (front, (600, (H - front.height) // 2))):
        sh, (dx, dy) = drop_shadow(img)
        canvas.alpha_composite(sh, (x - dx, y - dy))
        canvas.alpha_composite(img, (x, y))

    d = ImageDraw.Draw(canvas)
    # Emblem + wordmark (left), inside a ~64px safe margin.
    em = emblem(132)
    canvas.alpha_composite(em, (64, 82))
    title_f = font("cormorant_semibold.ttf", 66)
    x0, y0 = 66, 236
    spaced(d, (x0, y0), "BLAZAR", title_f, CREAM, 7)
    spaced(d, (x0, y0 + 66), "READER", title_f, GOLD_HI, 7)
    # Gold hairline.
    rule_y = y0 + 158
    for i in range(300):
        a = int(255 * (1 - abs(i - 150) / 150) ** 0.7)
        d.point((x0 + i, rule_y), fill=GOLD + (a,))
    sub_f = font("inter_medium.ttf", 19)
    d.text((x0, rule_y + 16), "Editorial Comic Reader · Offline & Online Library", font=sub_f, fill=MUTED)

    out = os.path.join(KIT, "feature-graphic-1024x500.png")
    canvas.convert("RGB").save(out, optimize=True)
    return out


SHOT_PLAN = [
    ("01_discover_comics.png", "13_discover_ink.png"),
    ("02_library_shelf.png", "01_library_ink.png"),
    ("03_series_chapters.png", "14_discover_series_ink.png"),
    ("04_reader_settings.png", "09_reader_settings.png"),
    ("05_about_privacy.png", "25_about_privacy.png"),
]


def phone_screenshots():
    outdir = os.path.join(KIT, "screenshots")
    os.makedirs(outdir, exist_ok=True)
    outs = []
    for dst, src in SHOT_PLAN:
        im = Image.open(os.path.join(SHOTS_IN, src)).convert("RGB")
        # Fit whole screen (no crop, no distortion) inside 1080x1920 on Ink.
        scale = min(1080 / im.width, 1920 / im.height)
        w, h = round(im.width * scale), round(im.height * scale)
        im = im.resize((w, h), Image.LANCZOS)
        bg = Image.new("RGB", (1080, 1920), INK)
        bg.paste(im, ((1080 - w) // 2, (1920 - h) // 2))
        p = os.path.join(outdir, dst)
        bg.save(p, optimize=True)
        outs.append(p)
    return outs


if __name__ == "__main__":
    os.makedirs(KIT, exist_ok=True)
    print(store_icon())
    print(feature_graphic())
    for p in phone_screenshots():
        print(p)
