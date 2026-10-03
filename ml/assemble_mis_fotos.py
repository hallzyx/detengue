"""Mezcla las fotos propias revisadas (ml/external/mis_labeled + negativos) en ml/dataset.

El split es POR RÁFAGA: las fotos de una misma sesión (marca de hora IMG_AAAAMMDD_HHMMSS con huecos < --gap segundos)
van enteras a un solo split. Si se repartieran foto a foto, casi-duplicados del mismo balde caerían en train y en
test y las métricas saldrían infladas.

Entrada:  ml/external/mis_labeled/<clase_estado>/{x.jpg,x.txt}   y   ml/external/mis_fotos/negativos/*.jpg (sin caja)
Uso:      python -m ml.assemble_mis_fotos
"""
from __future__ import annotations

import argparse
import re
import shutil
from datetime import datetime
from pathlib import Path

from ml.classes import REPO_ROOT

PREFIX = "mis_"
_STAMP = re.compile(r"(\d{8})_(\d{6})")


def stamp_of(name: str) -> datetime:
    m = _STAMP.search(name)
    if not m:
        raise ValueError(f"sin marca de hora IMG_AAAAMMDD_HHMMSS: {name}")
    return datetime.strptime(m.group(1) + m.group(2), "%Y%m%d%H%M%S")


def group_bursts(names: list[str], gap_s: float) -> list[list[str]]:
    """Agrupa nombres consecutivos en el tiempo; un hueco mayor que gap_s abre grupo nuevo."""
    ordered = sorted(names, key=stamp_of)
    groups: list[list[str]] = []
    for name in ordered:
        if groups and (stamp_of(name) - stamp_of(groups[-1][-1])).total_seconds() <= gap_s:
            groups[-1].append(name)
        else:
            groups.append([name])
    return groups


def split_groups(groups: list[list[str]], val: float = 0.1, test: float = 0.15) -> dict[str, str]:
    """Asigna cada grupo entero a un split, acercándose a las proporciones val/test. Determinista."""
    total = sum(len(g) for g in groups)
    split_of: dict[str, str] = {}
    remaining = sorted(range(len(groups)), key=lambda i: (-len(groups[i]), i))  # grupos grandes primero
    if len(groups) < 3:  # sin grupos suficientes no se puede separar sin fuga: todo a train
        remaining = []
    for split, target in (("test", test * total), ("val", val * total)):
        best = min(remaining, key=lambda i: (abs(len(groups[i]) - target), i), default=None)
        if best is None:
            continue
        remaining.remove(best)
        for n in groups[best]:
            split_of[n] = split
    for g in groups:
        for n in g:
            split_of.setdefault(n, "train")
    return split_of


def _copy(img: Path, label_text: str | None, label_src: Path | None, split: str, dest: Path) -> None:
    stem = f"{PREFIX}{img.stem}"
    (dest / "images" / split).mkdir(parents=True, exist_ok=True)
    (dest / "labels" / split).mkdir(parents=True, exist_ok=True)
    shutil.copyfile(img, dest / "images" / split / f"{stem}.jpg")
    out = dest / "labels" / split / f"{stem}.txt"
    if label_src is not None:
        shutil.copyfile(label_src, out)
    else:
        out.write_text(label_text or "", encoding="utf-8")


def assemble(labeled: Path, negatives: Path, dest: Path, gap_s: float) -> dict[str, dict[str, int]]:
    for old in dest.glob(f"*/*/{PREFIX}*"):  # idempotente: rehace solo lo propio
        old.unlink()
    report: dict[str, dict[str, int]] = {}
    sources = [(d.name, sorted(d.glob("*.jpg")), True) for d in sorted(labeled.iterdir()) if d.is_dir() and not d.name.startswith("_")]
    if negatives.is_dir():
        sources.append(("negativos", sorted(negatives.glob("*.jpg")), False))
    for name, imgs, has_label in sources:
        by_name = {p.name: p for p in imgs}
        split_of = split_groups(group_bursts(list(by_name), gap_s))
        counts = {"train": 0, "val": 0, "test": 0}
        for fname, split in split_of.items():
            img = by_name[fname]
            _copy(img, None, img.with_suffix(".txt") if has_label else None, split, dest)
            counts[split] += 1
        report[name] = counts
    return report


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--labeled", type=Path, default=REPO_ROOT / "ml" / "external" / "mis_labeled")
    ap.add_argument("--negatives", type=Path, default=REPO_ROOT / "ml" / "external" / "mis_fotos" / "negativos")
    ap.add_argument("--dest", type=Path, default=REPO_ROOT / "ml" / "dataset")
    ap.add_argument("--gap", type=float, default=30.0, help="segundos entre fotos de una misma ráfaga")
    args = ap.parse_args(argv)
    report = assemble(args.labeled, args.negatives, args.dest, args.gap)
    for name, c in report.items():
        print(f"{name}: {c}")
    with (args.dest / "SOURCES.md").open("a", encoding="utf-8") as f:
        f.write(f"- **mis_fotos** — fotos propias del equipo, split por ráfaga (gap {args.gap:.0f}s) — {report}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
