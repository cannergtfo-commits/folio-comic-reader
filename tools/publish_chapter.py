#!/usr/bin/env python3
"""Publish a comic chapter to Folio's online library.

    python3 tools/publish_chapter.py path/to/Chapter.cbz --series "Lyra"

What it does:
  1. Reads the CBZ (page count, ComicInfo.xml title/number, cover page).
  2. Saves a cover thumbnail to catalog/covers/.
  3. Uploads the CBZ to the GitHub release "library" (created if missing).
  4. Adds or updates the chapter in catalog/catalog.json.
  5. Commits catalog/ and pushes to main. Every Folio app shows the chapter on its next refresh.

Needs: git, the GitHub CLI (`gh auth login` once), and Pillow (`pip install pillow`).
Run it from anywhere inside the repo. Use --dry-run to see what would change.
"""
import argparse, datetime, io, json, os, re, subprocess, sys, zipfile
import xml.etree.ElementTree as ET

REPO = "cannergtfo-commits/folio-comic-reader"
TAG = "library"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "catalog", "catalog.json")
COVERS = os.path.join(ROOT, "catalog", "covers")
IMG = (".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")


def natural(s):
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r"(\d+)", s)]


def slug(s):
    return re.sub(r"-+", "-", re.sub(r"[^a-z0-9]+", "-", s.lower().replace("&", " and "))).strip("-")


def run(cmd, check=True, capture=False):
    r = subprocess.run(cmd, cwd=ROOT, text=True, capture_output=capture)
    if check and r.returncode != 0:
        sys.exit(f"failed: {' '.join(cmd)}\n{(r.stderr or '') if capture else ''}")
    return r


def read_cbz(path):
    with zipfile.ZipFile(path) as z:
        names = sorted([n for n in z.namelist() if n.lower().endswith(IMG) and not n.startswith("__MACOSX")], key=natural)
        if not names:
            sys.exit("That CBZ has no page images.")
        info = {}
        ci = next((n for n in z.namelist() if n.lower().endswith("comicinfo.xml")), None)
        cover = None
        if ci:
            root = ET.fromstring(z.read(ci))
            for k in ("Title", "Series", "Number", "Writer", "Summary", "Manga"):
                el = root.find(k)
                if el is not None and (el.text or "").strip():
                    info[k] = el.text.strip()
            for pg in root.iter("Page"):
                if pg.get("Type") == "FrontCover" and (pg.get("Image") or "").isdigit():
                    i = int(pg.get("Image"))
                    if i < len(names):
                        cover = names[i]
        cover = cover or next((n for n in names if os.path.basename(n).lower().startswith("cover")), names[0])
        return names, info, z.read(cover)


def thumb(data, dest, width=360):
    from PIL import Image
    im = Image.open(io.BytesIO(data)).convert("RGB")
    h = round(im.height * width / im.width)
    im.resize((width, h), Image.LANCZOS).save(dest, "JPEG", quality=84, optimize=True, progressive=True)


def load_catalog():
    if os.path.exists(CATALOG):
        with open(CATALOG, encoding="utf-8") as f:
            return json.load(f)
    return {"schema": 1, "name": "Folio Library", "publisher": "Blazar Force", "series": []}


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("cbz")
    ap.add_argument("--series", help="Series title (default: ComicInfo Series)")
    ap.add_argument("--title", help="Chapter title (default: ComicInfo Title)")
    ap.add_argument("--number", type=float, help="Chapter number (default: ComicInfo Number)")
    ap.add_argument("--series-id", help="Stable id for the series (default: from the title)")
    ap.add_argument("--author", help="Shown under the series title")
    ap.add_argument("--description", help="Series blurb (set once; later runs keep it unless you pass it again)")
    ap.add_argument("--tags", help="Comma-separated, e.g. 'fantasy,black & white'")
    ap.add_argument("--published", help="YYYY-MM-DD (default: today)")
    ap.add_argument("--no-push", action="store_true", help="Commit locally but don't push")
    ap.add_argument("--dry-run", action="store_true", help="Show the catalog change, upload nothing")
    a = ap.parse_args()

    path = os.path.abspath(a.cbz)
    if not path.lower().endswith(".cbz") or not os.path.isfile(path):
        sys.exit("Give me a .cbz file.")
    pages, info, cover_bytes = read_cbz(path)
    cat = load_catalog()
    known = next((x for x in cat["series"] if a.series_id and x["id"] == a.series_id), None)
    # An existing series keeps its display name unless --series is passed (ComicInfo may say otherwise).
    series = a.series or (known or {}).get("title") or info.get("Series") or sys.exit("Pass --series (no Series in ComicInfo.xml).")
    number = a.number if a.number is not None else (float(info["Number"]) if "Number" in info else None)
    if number is None:
        m = re.search(r"(?:ch|chapter|issue|_)\s*0*(\d+(?:\.\d+)?)", os.path.basename(path), re.I)
        number = float(m.group(1)) if m else sys.exit("Pass --number (none in ComicInfo.xml or the file name).")
    num = int(number) if number == int(number) else number
    title = a.title or info.get("Title") or f"Chapter {num}"
    sid = a.series_id or slug(series)
    file_name = os.path.basename(path)
    size = os.path.getsize(path)
    cid = f"{sid}-{num}"
    cover_name = f"{cid}.jpg"
    cover_url = f"https://raw.githubusercontent.com/{REPO}/main/catalog/covers/{cover_name}"
    cbz_url = f"https://github.com/{REPO}/releases/download/{TAG}/{file_name}"

    s = next((x for x in cat["series"] if x["id"] == sid), None)
    if s is None:
        s = {"id": sid, "title": series, "author": a.author, "description": a.description,
             "cover": cover_url, "tags": [], "chapters": []}
        cat["series"].append(s)
    s["title"] = series
    if a.author: s["author"] = a.author
    if a.description: s["description"] = a.description
    if a.tags: s["tags"] = [t.strip() for t in a.tags.split(",") if t.strip()]
    old = next((c for c in s["chapters"] if c["id"] == cid), None)
    ch = {"id": cid, "number": num, "title": title, "cover": cover_url, "cbz": cbz_url, "file": file_name,
          "size": size, "pages": len(pages),
          "published": a.published or (old or {}).get("published") or datetime.date.today().isoformat()}
    s["chapters"] = sorted([c for c in s["chapters"] if c["id"] != cid] + [ch], key=lambda c: c["number"])
    if not s.get("cover"): s["cover"] = cover_url
    cat["updated"] = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    print(f"{'Updating' if old else 'Adding'} {series} · chapter {num}: {title} ({len(pages)} pages, {size/1048576:.1f} MB)")
    if a.dry_run:
        print(json.dumps(ch, indent=2)); return

    os.makedirs(COVERS, exist_ok=True)
    thumb(cover_bytes, os.path.join(COVERS, cover_name))

    if run(["gh", "release", "view", TAG, "--repo", REPO], check=False, capture=True).returncode != 0:
        run(["gh", "release", "create", TAG, "--repo", REPO, "--title", "Folio library (chapter files)",
             "--notes", "Chapter files that Folio's Discover tab downloads. Listed in catalog/catalog.json. Don't delete these assets."])
    print("Uploading to the release…")
    run(["gh", "release", "upload", TAG, path, "--repo", REPO, "--clobber"])

    with open(CATALOG, "w", encoding="utf-8") as f:
        json.dump(cat, f, indent=2, ensure_ascii=False); f.write("\n")
    run(["git", "add", "catalog"])
    if run(["git", "diff", "--cached", "--quiet"], check=False).returncode != 0:
        run(["git", "commit", "-q", "-m", f"Publish {series} chapter {num}" + (f": {title}" if not re.match(r"(?i)chapter", title) else f" ({title})")])
        if not a.no_push:
            run(["git", "pull", "-q", "--rebase", "origin", "main"])
            run(["git", "push", "-q", "origin", "HEAD:main"])
            print("Pushed. Folio picks it up on the next refresh (GitHub's raw cache can take ~5 minutes).")
    else:
        print("Catalog unchanged.")
    print(f"CBZ:   {cbz_url}\nCover: {cover_url}")


if __name__ == "__main__":
    main()
