"""Builds screenshots/polish_contact_sheet.png from before/after shot folders (1.1 vs 1.2)."""
from PIL import Image, ImageDraw, ImageFont
import os, glob, sys
B = os.environ.get("BEFORE", "/workspace/folio-shots/before")
A = os.environ.get("AFTER", "/workspace/folio-shots/after")
OUT = sys.argv[1] if len(sys.argv) > 1 else "screenshots/polish_contact_sheet.png"
names = sorted(os.listdir(A))
serif = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
sans = (glob.glob("/usr/share/fonts/truetype/sand-box/google/Inter/Inter-Variable*") or [serif])[0]
F1, F2, F3 = ImageFont.truetype(serif, 46), ImageFont.truetype(sans, 22), ImageFont.truetype(sans, 18)
TH, MW = 560, 259
def thumb(p):
    im = Image.open(p).convert("RGB"); r = min(TH / im.height, MW / im.width)
    return im.resize((int(im.width * r), int(im.height * r)), Image.LANCZOS)
CW, CH = MW * 2 + 12, TH + 70
cells = []
for n in names:
    a = thumb(f"{A}/{n}"); bp = f"{B}/{n}"; b = thumb(bp) if os.path.exists(bp) else None
    c = Image.new("RGB", (CW, CH), (22, 19, 15)); d = ImageDraw.Draw(c)
    d.text((0, 4), f"{n[:2]}  " + n[3:-4].replace("_", " "), font=F2, fill=(233, 207, 150))
    def put(im, x, lab, col):
        c.paste(im, (x + (MW - im.width) // 2, 36 + (TH - im.height) // 2)); d.text((x + 4, TH + 42), lab, font=F3, fill=col)
    if b: put(b, 0, "BEFORE (1.1)", (150, 140, 125)); put(a, MW + 12, "AFTER (1.2)", (233, 207, 150))
    else: put(a, (CW - MW) // 2, "NEW IN 1.2", (233, 207, 150))
    cells.append(c)
cols, pad = 5, 36; rows = (len(cells) + cols - 1) // cols
sheet = Image.new("RGB", (cols * CW + (cols + 1) * pad, 150 + rows * (CH + pad) + pad), (12, 11, 10)); d = ImageDraw.Draw(sheet)
d.text((pad, 40), "Folio 1.2 — UI polish: before / after", font=F1, fill=(243, 223, 174))
d.text((pad, 100), "Ink & gold · paper theme kept · same catalog data (Lyra chapters 1–4) · 17–20 are new states", font=F2, fill=(170, 160, 140))
for i, c in enumerate(cells): sheet.paste(c, (pad + (i % cols) * (CW + pad), 150 + (i // cols) * (CH + pad)))
sheet.save(OUT, optimize=True); print(OUT, sheet.size)
