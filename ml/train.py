"""Entrena el detector nano de Detengue (PRD §25, §40-§43).

Modelo base: YOLO11n (Ultralytics) a 320x320. Las clases salen de `android/models/detengue_labels.txt`.

    python -m ml.make_data_yaml
    python -m ml.validate_dataset
    python -m ml.train --epochs 100

NOTA: este script NO se ha ejecutado en el entorno donde se escribió (sin dataset ni acceso a PyPI).
"""
from __future__ import annotations

import argparse
from pathlib import Path

from ml.classes import REPO_ROOT

IMG_SIZE = 320  # = tamaño de entrada que espera la app (PRD §25)


def build_train_kwargs(
    data: Path,
    epochs: int,
    batch: int,
    project: Path,
    name: str,
    seed: int = 0,
    patience: int = 30,
) -> dict:
    """Argumentos de `YOLO.train`. Aumentos MODERADOS (PRD §64) y sin volteo vertical:
    una escena real no se ve al revés, y un balde boca abajo es justo una de nuestras clases."""
    return dict(
        data=str(data),
        imgsz=IMG_SIZE,
        epochs=epochs,
        batch=batch,
        seed=seed,
        deterministic=True,
        patience=patience,
        project=str(project),
        name=name,
        exist_ok=True,
        # aumentos moderados
        degrees=10.0,
        translate=0.10,
        scale=0.50,
        fliplr=0.5,
        flipud=0.0,
        hsv_h=0.015,
        hsv_s=0.60,
        hsv_v=0.40,
        mosaic=1.0,
        close_mosaic=10,
    )


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--data", type=Path, default=REPO_ROOT / "ml" / "dataset" / "data.yaml")
    parser.add_argument("--base", default="yolo11n.pt", help="pesos de partida (se descargan solos)")
    parser.add_argument("--epochs", type=int, default=100)
    parser.add_argument("--batch", type=int, default=16)
    parser.add_argument("--name", default="detengue")
    parser.add_argument("--project", type=Path, default=REPO_ROOT / "ml" / "runs")
    args = parser.parse_args(argv)

    from ultralytics import YOLO  # import tardío: el resto del módulo se puede probar sin Ultralytics

    model = YOLO(args.base)
    model.train(**build_train_kwargs(args.data, args.epochs, args.batch, args.project, args.name))
    print(f"\nListo. Pesos en {args.project / args.name / 'weights' / 'best.pt'}")
    print("Siguiente: python -m ml.evaluate  →  python -m ml.export_tflite")


if __name__ == "__main__":
    main()
