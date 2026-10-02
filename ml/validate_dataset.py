"""Valida un dataset en formato YOLO antes de entrenar (PRD §40-§42).

Estructura esperada:
    <dataset>/images/{train,val,test}/*.jpg
    <dataset>/labels/{train,val,test}/*.txt     (una línea por objeto: `clase cx cy w h` normalizados)

Un `.txt` VACÍO es válido y significa "imagen negativa" (sin objetos relevantes): son muy importantes
para que el modelo no marque cualquier objeto redondo (PRD §42).

Uso:  python -m ml.validate_dataset [--dataset ml/dataset] [--fail-on-warnings]
Sale con código 1 si hay errores.
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

from ml.classes import REPO_ROOT, load_labels

SPLITS = ("train", "val", "test")
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}
MIN_INSTANCES_PER_CLASS_TRAIN = 50
MIN_NEGATIVE_RATIO_TRAIN = 0.05
COORD_TOLERANCE = 1e-3


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    images: Counter = field(default_factory=Counter)  # por split
    negatives: Counter = field(default_factory=Counter)  # por split
    instances: dict[str, Counter] = field(default_factory=lambda: defaultdict(Counter))  # split -> clase -> n

    @property
    def ok(self) -> bool:
        return not self.errors


def _sha1(path: Path) -> str:
    return hashlib.sha1(path.read_bytes()).hexdigest()


def _check_label_file(path: Path, num_classes: int, report: Report, split: str) -> bool:
    """Devuelve True si la etiqueta es válida. Acumula errores en `report`."""
    valid = True
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line:
            continue
        where = f"{path.parent.parent.name}/{split}/{path.name}:{number}"
        tokens = line.split()
        if len(tokens) != 5:
            report.errors.append(f"{where}: se esperaban 5 valores, hay {len(tokens)}")
            valid = False
            continue
        try:
            cls = int(tokens[0])
            cx, cy, w, h = (float(t) for t in tokens[1:])
        except ValueError:
            report.errors.append(f"{where}: valores no numéricos")
            valid = False
            continue
        if not 0 <= cls < num_classes:
            report.errors.append(f"{where}: clase {cls} fuera de rango 0..{num_classes - 1}")
            valid = False
            continue
        if not all(0.0 <= v <= 1.0 for v in (cx, cy, w, h)):
            report.errors.append(f"{where}: coordenadas fuera de 0..1 (¿en píxeles?)")
            valid = False
            continue
        if w <= 0 or h <= 0:
            report.errors.append(f"{where}: caja sin área")
            valid = False
            continue
        if cx - w / 2 < -COORD_TOLERANCE or cx + w / 2 > 1 + COORD_TOLERANCE \
                or cy - h / 2 < -COORD_TOLERANCE or cy + h / 2 > 1 + COORD_TOLERANCE:
            report.warnings.append(f"{where}: la caja se sale de la imagen")
        report.instances[split][cls] += 1
    return valid


def validate(dataset: Path, num_classes: int) -> Report:
    report = Report()
    hashes: dict[str, list[tuple[str, str]]] = defaultdict(list)  # sha1 -> [(split, nombre)]
    stems: dict[str, set[str]] = defaultdict(set)  # stem -> splits

    for split in SPLITS:
        images_dir = dataset / "images" / split
        labels_dir = dataset / "labels" / split
        if not images_dir.is_dir() or not labels_dir.is_dir():
            report.errors.append(f"Falta la carpeta del split '{split}' (images/{split} y labels/{split})")
            continue

        images = sorted(p for p in images_dir.iterdir() if p.suffix.lower() in IMAGE_EXTS)
        if not images:
            report.errors.append(f"El split '{split}' no tiene imágenes")
        image_stems = {p.stem for p in images}

        for image in images:
            report.images[split] += 1
            label = labels_dir / f"{image.stem}.txt"
            if not label.is_file():
                report.errors.append(f"{split}/{image.name}: no tiene archivo de etiqueta (.txt)")
                continue
            if _check_label_file(label, num_classes, report, split) and not label.read_text().strip():
                report.negatives[split] += 1
            hashes[_sha1(image)].append((split, image.name))
            stems[image.stem].add(split)

        for label in labels_dir.glob("*.txt"):
            if label.stem not in image_stems:
                report.warnings.append(f"{split}/{label.name}: etiqueta sin imagen")

    # Fuga de datos: la MISMA imagen en más de un split invalida las métricas (PRD §41).
    for digest, places in hashes.items():
        splits_with_it = {s for s, _ in places}
        if len(splits_with_it) > 1:
            names = ", ".join(f"{s}/{n}" for s, n in places)
            report.errors.append(f"Imagen idéntica en varios splits (fuga de datos): {names}")
    for stem, splits_with_it in stems.items():
        if len(splits_with_it) > 1:
            report.warnings.append(f"El nombre '{stem}' aparece en {sorted(splits_with_it)}: confirma que no es la misma foto")

    # Calidad de la cobertura.
    train_images = report.images["train"]
    if train_images and report.negatives["train"] / train_images < MIN_NEGATIVE_RATIO_TRAIN:
        report.warnings.append(
            f"Solo {report.negatives['train']}/{train_images} imágenes de train son negativas "
            f"(< {MIN_NEGATIVE_RATIO_TRAIN:.0%}). Añade negativos difíciles: tazas, ollas, cajas, reflejos (PRD §42)."
        )
    for cls in range(num_classes):
        n = report.instances["train"][cls]
        if n < MIN_INSTANCES_PER_CLASS_TRAIN:
            report.warnings.append(f"Clase {cls}: {n} instancias en train (recomendado ≥ {MIN_INSTANCES_PER_CLASS_TRAIN})")
        for split in ("val", "test"):
            if report.images[split] and report.instances[split][cls] == 0:
                report.warnings.append(f"Clase {cls} no aparece en {split}: sus métricas no serán fiables")
    return report


def format_report(report: Report, names: list[str]) -> str:
    lines = ["== Dataset Detengue =="]
    for split in SPLITS:
        lines.append(
            f"{split:5} imágenes={report.images[split]:5} negativas={report.negatives[split]:4} "
            f"instancias={sum(report.instances[split].values())}"
        )
    lines.append("\nInstancias por clase (train / val / test):")
    for i, name in enumerate(names):
        counts = " / ".join(str(report.instances[s][i]) for s in SPLITS)
        lines.append(f"  {i:2} {name:22} {counts}")
    for title, items in (("ERRORES", report.errors), ("AVISOS", report.warnings)):
        if items:
            lines.append(f"\n{title} ({len(items)}):")
            lines += [f"  - {m}" for m in items[:50]]
            if len(items) > 50:
                lines.append(f"  … y {len(items) - 50} más")
    lines.append("\nRESULTADO: " + ("OK" if report.ok else "CON ERRORES"))
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dataset", type=Path, default=REPO_ROOT / "ml" / "dataset")
    parser.add_argument("--fail-on-warnings", action="store_true")
    args = parser.parse_args(argv)

    names = [c.name for c in load_labels()]
    report = validate(args.dataset, len(names))
    print(format_report(report, names))
    if not report.ok or (args.fail_on_warnings and report.warnings):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
