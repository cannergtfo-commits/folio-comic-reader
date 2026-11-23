# Publishing chapters to Folio's library

Folio's **Discover** tab reads one file: [`catalog/catalog.json`](catalog/catalog.json), served at

    https://raw.githubusercontent.com/cannergtfo-commits/folio-comic-reader/main/catalog/catalog.json

Chapter files are assets on the [`library` release](https://github.com/cannergtfo-commits/folio-comic-reader/releases/tag/library).
Cover thumbnails live in `catalog/covers/`. When the catalog changes, every copy of Folio shows the new chapter
(marked **NEW**) the next time it opens or the reader pulls down to refresh. No app update is needed.

## Add a chapter (one command)

```bash
cd folio-comic-reader            # this repo
python3 tools/publish_chapter.py ~/comics/Lyra_Ch7_BW.cbz --series "Lyra" --series-id lyra-bw
```

The title and number come from the CBZ's `ComicInfo.xml` (the comic maker writes it), or pass `--title "…" --number 7`.
The script makes the cover thumbnail, uploads the CBZ to the `library` release, updates `catalog.json`,
commits, and pushes. Try `--dry-run` first if you like.

One-time setup: `gh auth login` (GitHub CLI) and `pip install pillow`.

## Add a new comic (a new series)

Same command with a new series name and id. Give it a blurb the first time:

```bash
python3 tools/publish_chapter.py issue01.cbz --series "Blazar Bound" --series-id blazar-bound \
  --author "Blazar Force" --description "Two-player co-op …" --tags "sci-fi,co-op"
```

## Fix or replace a chapter

Run the command again with the corrected CBZ (same series id and number). The file on the release is replaced
and the catalog entry updated. Readers who already downloaded the old file keep it until they remove it.

## Remove a chapter

Delete its entry from `catalog/catalog.json` and push. Leave the release asset, or delete it on GitHub afterwards.

## Without the script (GitHub website only)

1. Open the `library` release → **Edit** → drag in the CBZ → **Update release**.
2. Upload a cover JPG into `catalog/covers/`.
3. Edit `catalog/catalog.json` in the browser and copy an existing chapter block. Set `cbz` to
   `https://github.com/cannergtfo-commits/folio-comic-reader/releases/download/library/<file name>`,
   `file` to the file name, `size` to its exact size in bytes, plus `pages`, `title`, `number`, `published`.

## Catalog format

```jsonc
{
  "schema": 1,
  "app": { "version": "1.1", "versionCode": 2, "apk": "https://…/Folio.apk", "notes": "…" }, // update banner
  "series": [{
    "id": "lyra-bw", "title": "Lyra", "author": "…", "description": "…",
    "cover": "https://…/catalog/covers/lyra-bw-1.jpg", "tags": ["…"],
    "chapters": [{
      "id": "lyra-bw-1", "number": 1, "title": "Chapter 1: Alone in the Woods",
      "cover": "https://…jpg", "cbz": "https://…cbz", "file": "Lyra_Ch1_BW.cbz",
      "size": 8257188, "pages": 7, "published": "2026-11-22"
    }]
  }]
}
```

`size` must be the exact byte size. Folio checks it after downloading and uses `file` + `size` to recognise a
chapter the reader already imported by hand. Bump `app.versionCode` when a new APK ships to show the update banner.
