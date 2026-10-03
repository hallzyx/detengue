"""Mezcla las imágenes revisadas de web_labeled en ml/dataset (split determinista 80/10/10) y registra la fuente.

Ignora .txt sin imagen (las que borraste en la revisión) e imágenes sin .txt. Uso: python -m ml.assemble_web_dataset
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ml.classes import REPO_ROOT
from ml.webdata import split_for_stem


def assemble(src: Path, dest: Path) -> dict[str, int]:
    counts = {"train": 0, "val": 0, "test": 0}
    for img in sorted(src.glob("*/*.jpg")):
        lbl = img.with_suffix(".txt")
        if not lbl.is_file():
            continue
        stem = f"web_{img.stem}"
        split = split_for_stem(stem)
        (dest / "images" / split).mkdir(parents=True, exist_ok=True)
        (dest / "labels" / split).mkdir(parents=True, exist_ok=True)
        shutil.copyfile(img, dest / "images" / split / f"{stem}.jpg")
        shutil.copyfile(lbl, dest / "labels" / split / f"{stem}.txt")
        counts[split] += 1
    return counts


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--src", type=Path, default=REPO_ROOT / "ml" / "external" / "web_labeled")
    ap.add_argument("--dest", type=Path, default=REPO_ROOT / "ml" / "dataset")
    args = ap.parse_args(argv)
    counts = assemble(args.src, args.dest)
    print("imágenes web añadidas:", counts)
    args.dest.mkdir(parents=True, exist_ok=True)
    with (args.dest / "SOURCES.md").open("a", encoding="utf-8") as f:
        f.write(f"- **web** — Openverse (CC0/PDM/BY/BY-SA), atribuciones en ml/external/web/ATTRIBUTION.csv — {counts}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
