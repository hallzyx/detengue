"""Evalúa el detector sobre el split de TEST (PRD §43): precision, recall, F1, mAP y matriz de confusión.

    python -m ml.evaluate --weights ml/runs/detengue/weights/best.pt

Reporta TODO, también lo malo ("No ocultar resultados malos"). Escribe `metrics.json` y un bloque
Markdown listo para pegar en docs/MODEL_CARD.md.

NOTA: este script NO se ha ejecutado en el entorno donde se escribió (sin dataset ni acceso a PyPI).
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

from ml.classes import REPO_ROOT
from ml.train import IMG_SIZE


def f1_score(precision: float, recall: float) -> float:
    return 0.0 if precision + recall == 0 else 2 * precision * recall / (precision + recall)


def to_markdown(overall: dict, per_class: list[dict]) -> str:
    lines = [
        "| Métrica | Valor |",
        "|---|---|",
        f"| Precision | {overall['precision']:.3f} |",
        f"| Recall | {overall['recall']:.3f} |",
        f"| F1 | {overall['f1']:.3f} |",
        f"| mAP@0.5 | {overall['map50']:.3f} |",
        f"| mAP@0.5:0.95 | {overall['map50_95']:.3f} |",
        "",
        "| Clase | Precision | Recall | F1 | AP@0.5 |",
        "|---|---|---|---|---|",
    ]
    for c in per_class:
        lines.append(f"| {c['name']} | {c['precision']:.3f} | {c['recall']:.3f} | {c['f1']:.3f} | {c['ap50']:.3f} |")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--weights", type=Path, required=True)
    parser.add_argument("--data", type=Path, default=REPO_ROOT / "ml" / "dataset" / "data.yaml")
    parser.add_argument("--split", default="test", choices=["val", "test"])
    parser.add_argument("--out", type=Path, default=None, help="carpeta de salida (por defecto, junto a los pesos)")
    args = parser.parse_args(argv)

    from ultralytics import YOLO

    out = args.out or args.weights.parent.parent / f"eval_{args.split}"
    metrics = YOLO(str(args.weights)).val(
        data=str(args.data), split=args.split, imgsz=IMG_SIZE, plots=True, project=str(out.parent), name=out.name, exist_ok=True
    )

    box = metrics.box
    overall = dict(
        precision=float(box.mp), recall=float(box.mr), f1=f1_score(float(box.mp), float(box.mr)),
        map50=float(box.map50), map50_95=float(box.map),
    )
    per_class = []
    for i, cls_index in enumerate(box.ap_class_index):
        p, r = float(box.p[i]), float(box.r[i])
        per_class.append(dict(
            name=metrics.names[int(cls_index)], precision=p, recall=r, f1=f1_score(p, r), ap50=float(box.ap50[i]),
        ))

    out.mkdir(parents=True, exist_ok=True)
    (out / "metrics.json").write_text(json.dumps(dict(overall=overall, per_class=per_class), indent=2, ensure_ascii=False))
    print(to_markdown(overall, per_class))
    print(f"\nMatriz de confusión y curvas en {out}/ (confusion_matrix.png)")
    print("Pega la tabla en docs/MODEL_CARD.md. Si hay clases con AP bajo, reduce clases o añade datos (PRD §43).")


if __name__ == "__main__":
    main()
