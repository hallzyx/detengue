"""Añade al dataset las imágenes SINTÉTICAS (generadas con IA) revisadas, SOLO al split train.

Nunca van a val/test: esos splits deben seguir siendo fotos reales para que las métricas signifiquen algo.
Entrada:  ml/external/syn_labeled/<clase_estado>/{x.jpg,x.txt}  y  ml/external/syn_neg/*.jpg (negativos, sin caja)
Uso:      python -m ml.assemble_synthetic
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ml.classes import REPO_ROOT

PREFIX = "syn_"


def assemble(labeled: Path, negatives: Path, dest: Path) -> dict[str, int]:
    for split in ("train", "val", "test"):  # idempotente
        for old in list((dest / "images" / split).glob(f"{PREFIX}*")) + list((dest / "labels" / split).glob(f"{PREFIX}*")):
            old.unlink()
    (dest / "images" / "train").mkdir(parents=True, exist_ok=True)
    (dest / "labels" / "train").mkdir(parents=True, exist_ok=True)
    counts = {"con_caja": 0, "negativas": 0}
    for img in sorted(labeled.glob("*/*.jpg")):
        lbl = img.with_suffix(".txt")
        if not lbl.is_file():
            continue
        shutil.copyfile(img, dest / "images" / "train" / f"{PREFIX}{img.stem}.jpg")
        shutil.copyfile(lbl, dest / "labels" / "train" / f"{PREFIX}{img.stem}.txt")
        counts["con_caja"] += 1
    for img in sorted(negatives.glob("*.jpg")) if negatives.is_dir() else []:
        shutil.copyfile(img, dest / "images" / "train" / f"{PREFIX}{img.stem}.jpg")
        (dest / "labels" / "train" / f"{PREFIX}{img.stem}.txt").write_text("", encoding="utf-8")
        counts["negativas"] += 1
    return counts


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--labeled", type=Path, default=REPO_ROOT / "ml" / "external" / "syn_labeled")
    ap.add_argument("--negatives", type=Path, default=REPO_ROOT / "ml" / "external" / "syn_neg")
    ap.add_argument("--dest", type=Path, default=REPO_ROOT / "ml" / "dataset")
    args = ap.parse_args(argv)
    counts = assemble(args.labeled, args.negatives, args.dest)
    print("sintéticas añadidas a train:", counts)
    with (args.dest / "SOURCES.md").open("a", encoding="utf-8") as f:
        f.write(f"- **sintéticas** — imágenes generadas con IA (ChatGPT), SOLO en train, revisadas una a una — {counts}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
