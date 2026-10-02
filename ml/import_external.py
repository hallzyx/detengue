"""Importa datasets públicos en formato YOLO (export de Roboflow) al dataset de Detengue.

Layout de origen (Roboflow, "YOLOv8"): <src>/{train,valid,test}/{images,labels} + <src>/data.yaml
Destino: ml/dataset/{images,labels}/{train,val,test}, con los ids de `android/models/detengue_labels.txt`.

Reglas (para no enseñarle errores al modelo):
  * Solo se mapean las clases pedidas con --map "NombreOrigen=clase_detengue".
  * Una imagen con CUALQUIER caja de una clase sin mapear se DESCARTA (sería un objeto real sin etiquetar).
  * Una imagen sin cajas se conserva como negativa.
  * Si la clase destino tiene estados en detengue_labels.txt (p. ej. bucket:open), la clase sin estado NO existe:
    se exige que la etiqueta destino exista tal cual (usa "bucket" o "bucket:water" según labels.txt).

Uso:
  python -m ml.import_external --source ml/external/silva --name silva --license GPL-3.0 \
      --url https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p \
      --map "Bottle=bottle,Bucket=bucket,Tire=tire,Water tanks=water_tank"
"""
from __future__ import annotations

import argparse
import re
import shutil
import sys
from dataclasses import dataclass, field
from pathlib import Path

from ml.classes import REPO_ROOT, load_labels

SPLIT_DIRS = {"train": "train", "valid": "val", "val": "val", "test": "test"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


@dataclass
class ImportStats:
    copied: int = 0
    negatives: int = 0
    skipped_unmapped: int = 0
    skipped_no_label_file: int = 0
    boxes: dict[str, int] = field(default_factory=dict)


def read_source_names(data_yaml: Path) -> list[str]:
    """Lee `names:` de data.yaml sin depender de PyYAML (lista `[a, b]` o formato multilínea)."""
    text = data_yaml.read_text(encoding="utf-8")
    m = re.search(r"^names:\s*\[(.*?)\]", text, re.S | re.M)
    if m:
        return [s.strip().strip("'\"") for s in m.group(1).split(",") if s.strip()]
    names: list[str] = []
    in_names = False
    for line in text.splitlines():
        if re.match(r"^names:\s*$", line):
            in_names = True
            continue
        if in_names:
            item = re.match(r"^(?:\s+(?:\d+\s*:\s*)?-?|-)\s*(.+?)\s*$", line)
            if not item:
                break
            names.append(item.group(1).strip("'\""))
    if not names:
        raise ValueError(f"{data_yaml}: no encontré 'names:'")
    return names


def parse_mapping(spec: str) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for pair in spec.split(","):
        if not pair.strip():
            continue
        if "=" not in pair:
            raise ValueError(f"Mapeo inválido {pair!r}; usa Origen=destino")
        src, dst = pair.split("=", 1)
        mapping[src.strip().lower()] = dst.strip()
    if not mapping:
        raise ValueError("Mapeo vacío")
    return mapping


def build_id_map(source_names: list[str], mapping: dict[str, str], target_names: list[str]) -> dict[int, int]:
    """id de origen -> id de Detengue, solo para las clases mapeadas."""
    for dst in mapping.values():
        if dst not in target_names:
            raise ValueError(f"'{dst}' no está en detengue_labels.txt ({target_names})")
    lowered = {n.lower(): i for i, n in enumerate(source_names)}
    missing = [s for s in mapping if s not in lowered]
    if missing:
        raise ValueError(f"Clases de origen inexistentes: {missing}. Disponibles: {source_names}")
    return {lowered[s]: target_names.index(d) for s, d in mapping.items()}


def convert_label_text(text: str, id_map: dict[int, int]) -> list[str] | None:
    """Líneas convertidas, o None si la imagen debe descartarse (contiene una clase sin mapear)."""
    out: list[str] = []
    for raw in text.splitlines():
        parts = raw.split()
        if not parts:
            continue
        if len(parts) != 5:  # polígonos/segmentación: no se mezclan con cajas
            return None
        try:
            cid = int(parts[0])
        except ValueError:
            return None
        if cid not in id_map:
            return None
        out.append(" ".join([str(id_map[cid]), *parts[1:]]))
    return out


def import_source(source: Path, name: str, mapping: dict[str, str], dest: Path, target_names: list[str]) -> ImportStats:
    id_map = build_id_map(read_source_names(source / "data.yaml"), mapping, target_names)
    stats = ImportStats()
    for src_split, dst_split in SPLIT_DIRS.items():
        img_dir, lbl_dir = source / src_split / "images", source / src_split / "labels"
        if not img_dir.is_dir():
            continue
        out_img = dest / "images" / dst_split
        out_lbl = dest / "labels" / dst_split
        out_img.mkdir(parents=True, exist_ok=True)
        out_lbl.mkdir(parents=True, exist_ok=True)
        for img in sorted(p for p in img_dir.iterdir() if p.suffix.lower() in IMAGE_EXTS):
            lbl = lbl_dir / (img.stem + ".txt")
            if not lbl.is_file():
                stats.skipped_no_label_file += 1
                continue
            lines = convert_label_text(lbl.read_text(encoding="utf-8"), id_map)
            if lines is None:
                stats.skipped_unmapped += 1
                continue
            stem = f"{name}_{re.sub(r'[^A-Za-z0-9_.-]', '_', img.stem)}"
            shutil.copyfile(img, out_img / (stem + img.suffix.lower()))
            (out_lbl / (stem + ".txt")).write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")
            stats.copied += 1
            if not lines:
                stats.negatives += 1
            for line in lines:
                cls = target_names[int(line.split()[0])]
                stats.boxes[cls] = stats.boxes.get(cls, 0) + 1
    return stats


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--source", type=Path, required=True)
    p.add_argument("--name", required=True, help="prefijo corto de la fuente, p. ej. silva")
    p.add_argument("--map", required=True, dest="mapping")
    p.add_argument("--license", required=True)
    p.add_argument("--url", required=True)
    p.add_argument("--dest", type=Path, default=REPO_ROOT / "ml" / "dataset")
    args = p.parse_args(argv)

    target = [c.name for c in load_labels()]
    stats = import_source(args.source, args.name, parse_mapping(args.mapping), args.dest, target)
    print(f"{args.name}: {stats.copied} imágenes ({stats.negatives} negativas); "
          f"descartadas por clase sin mapear: {stats.skipped_unmapped}; sin .txt: {stats.skipped_no_label_file}")
    print("cajas por clase:", dict(sorted(stats.boxes.items())))
    sources_md = args.dest / "SOURCES.md"
    with sources_md.open("a", encoding="utf-8") as f:
        f.write(f"- **{args.name}** — {args.url} — licencia: {args.license} — {stats.copied} imágenes, cajas {stats.boxes}\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
