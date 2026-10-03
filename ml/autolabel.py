"""Etiquetado asistido: Grounding DINO (cero-disparos) propone la caja del balde; la clase-estado viene de la carpeta.

Entrada:  ml/external/web/<clase_estado>/*.jpg     (de collect_openverse)
Salida:   ml/external/web_labeled/<clase_estado>/{id.jpg,id.txt}  +  _preview/<id>.jpg con la caja dibujada

REVISIÓN HUMANA OBLIGATORIA: abre _preview/, y BORRA de web_labeled/<clase_estado>/ la imagen (.jpg) de toda foto
donde la caja esté mal o el estado no sea el de la carpeta (ej. un balde con agua dentro de la carpeta 'open').
assemble_web_dataset ignora los .txt sin imagen.

Requiere: pip install transformers torch pillow   (GPU recomendada; el modelo se descarga de Hugging Face).
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ml.classes import REPO_ROOT, load_labels
from ml.webdata import STATE_PLAN, pick_best, xyxy_to_yolo

MODEL_ID = "IDEA-Research/grounding-dino-tiny"


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--src", type=Path, default=REPO_ROOT / "ml" / "external" / "web")
    ap.add_argument("--out", type=Path, default=REPO_ROOT / "ml" / "external" / "web_labeled")
    ap.add_argument("--min-score", type=float, default=0.35)
    args = ap.parse_args(argv)

    import torch
    from PIL import Image, ImageDraw
    from transformers import AutoModelForZeroShotObjectDetection, AutoProcessor

    names = [c.name for c in load_labels()]
    device = "cuda" if torch.cuda.is_available() else "cpu"
    proc = AutoProcessor.from_pretrained(MODEL_ID)
    model = AutoModelForZeroShotObjectDetection.from_pretrained(MODEL_ID).to(device).eval()

    for state, plan in STATE_PLAN.items():
        if state not in names:
            print(f"AVISO: {state} no está en detengue_labels.txt; copia ml/labels_mvp_states.txt primero. Se omite.")
            continue
        class_id = names.index(state)
        src, dst = args.src / state.replace(":", "_"), args.out / state.replace(":", "_")
        prev = args.out / "_preview"
        dst.mkdir(parents=True, exist_ok=True)
        prev.mkdir(parents=True, exist_ok=True)
        kept = dropped = 0
        for path in sorted(src.glob("*.jpg")):
            img = Image.open(path).convert("RGB")
            inputs = proc(images=img, text=plan["object_prompt"], return_tensors="pt").to(device)
            with torch.no_grad():
                outputs = model(**inputs)
            kwargs = dict(target_sizes=[img.size[::-1]])
            try:  # el nombre de los umbrales cambió entre versiones de transformers
                res = proc.post_process_grounded_object_detection(outputs, inputs.input_ids, threshold=0.25, text_threshold=0.25, **kwargs)[0]
            except TypeError:
                res = proc.post_process_grounded_object_detection(outputs, inputs.input_ids, box_threshold=0.25, text_threshold=0.25, **kwargs)[0]
            boxes = [tuple(b.tolist()) for b in res["boxes"]]
            best = pick_best(boxes, [float(s) for s in res["scores"]], *img.size, min_score=args.min_score)
            if best is None:
                dropped += 1
                continue
            box, _ = best
            cx, cy, w, h = xyxy_to_yolo(box, *img.size)
            shutil.copyfile(path, dst / path.name)
            (dst / f"{path.stem}.txt").write_text(f"{class_id} {cx:.6f} {cy:.6f} {w:.6f} {h:.6f}\n", encoding="utf-8")
            shown = img.copy()
            ImageDraw.Draw(shown).rectangle(box, outline=(255, 0, 0), width=4)
            ImageDraw.Draw(shown).text((6, 6), state, fill=(255, 255, 0))
            shown.thumbnail((512, 512))
            shown.save(prev / f"{state.replace(':', '_')}__{path.name}", quality=80)
            kept += 1
        print(f"{state}: {kept} etiquetadas, {dropped} descartadas (sin caja fiable)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
