"""Simula lo que haría la APP con un .tflite sobre fotos reales: letterbox gris 114, umbrales y veredicto de RiskRuleEngine.

Existe porque las métricas de `ml.evaluate` (PyTorch FP32) no garantizan que el .tflite se comporte igual: el INT8 estático
recortó la confianza a ~0,50 y la app respondía siempre "no determinado". Usar SIEMPRE antes de publicar un modelo.

Uso:  python -m ml.simulate_app --model android/models/detengue_detector_int8.tflite
Umbrales = android/.../DetectionConfig.kt (detección 0,40; incierto 0,50; riesgo/seguro 0,70). Solo fotos propias `mis_*` de val+test.
"""
from __future__ import annotations

import argparse
import collections
import glob
from pathlib import Path

import numpy as np
from PIL import Image

from ml.classes import REPO_ROOT, load_labels

DETECT, UNCERTAIN, CONFIDENT = 0.40, 0.50, 0.70


def letterbox(img: Image.Image, size: int = 320) -> Image.Image:
    w, h = img.size
    r = min(size / w, size / h)
    nw, nh = round(w * r), round(h * r)
    canvas = Image.new("RGB", (size, size), (114, 114, 114))
    canvas.paste(img.resize((nw, nh), Image.BILINEAR), ((size - nw) // 2, (size - nh) // 2))
    return canvas


def verdict(name: str, score: float) -> str:
    if score < DETECT:
        return "sin detección"
    if score < UNCERTAIN:
        return "NO DETERMINADO"
    confident = score >= CONFIDENT
    if name.endswith(":water"):
        return "RIESGO" if confident else "revisar"
    if name.endswith(":inverted"):
        return "SEGURO" if confident else "revisar"
    return "revisar"


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--model", type=Path, required=True)
    ap.add_argument("--dataset", type=Path, default=REPO_ROOT / "ml" / "dataset")
    args = ap.parse_args(argv)
    from ai_edge_litert.interpreter import Interpreter

    names = [c.name for c in load_labels()]
    it = Interpreter(model_path=str(args.model))
    it.allocate_tensors()
    inp, out = it.get_input_details()[0], it.get_output_details()[0]
    rows: dict[str, list[tuple[str, float]]] = collections.defaultdict(list)
    for split in ("test", "val"):
        for img in sorted(glob.glob(str(args.dataset / "images" / split / "mis_*.jpg"))):
            label = Path(img.replace("/images/", "/labels/")).with_suffix(".txt")
            gt = {int(line.split()[0]) for line in label.read_text().splitlines() if line.strip()}
            truth = "negativo" if not gt else names[min(gt)]
            frame = np.asarray(letterbox(Image.open(img).convert("RGB")), np.float32) / 255
            it.set_tensor(inp["index"], frame[None])
            it.invoke()
            scores = it.get_tensor(out["index"])[0][4:]
            k = scores.max(0).argmax()
            rows[truth].append((names[int(scores[:, k].argmax())], float(scores[:, k].max())))
    allscores = [s for v in rows.values() for _, s in v]
    print(f"confianza del mejor candidato: mediana {np.median(allscores):.2f}, máx {max(allscores):.2f}")
    for truth, items in rows.items():
        counts = collections.Counter(verdict(n, s) for n, s in items)
        hit = "" if truth == "negativo" else f" clase correcta {sum(n == truth and s >= DETECT for n, s in items)}/{len(items)} |"
        print(f"  {truth:16s} n={len(items):2d}{hit} veredicto de la app: {dict(counts)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
