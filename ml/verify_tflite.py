"""Comprueba que un `.tflite` cumple el CONTRATO que asume la app (ver TfliteObjectDetector.kt) y,
opcionalmente, ejecuta una foto de prueba con el mismo postprocesado que Kotlin.

    python -m ml.verify_tflite --model android/models/detengue_detector_int8.tflite [--image foto.jpg]

Contrato: entrada NHWC cuadrada de 3 canales (float32 o cuantizada), UNA salida `[1,4+nc,N]` o
`[1,N,4+nc]`, con `nc` = número de clases de detengue_labels.txt.

NOTA: este script NO se ha ejecutado contra un modelo real en el entorno donde se escribió.
"""
from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np

from ml.classes import LABELS_FILE, ModelClass, load_labels
from ml.postprocess import NORMALIZED_MAX, Letterbox, postprocess


@dataclass(frozen=True)
class Check:
    name: str
    ok: bool
    detail: str
    fatal: bool = True


def check_contract(input_shape, output_shapes, num_classes: int, size_mb: float) -> list[Check]:
    """Reglas puras (sin TFLite) para poder probarlas."""
    shape = list(input_shape)
    checks = [
        Check("Entrada NHWC cuadrada de 3 canales", len(shape) == 4 and shape[3] == 3 and shape[1] == shape[2], f"shape={shape}"),
        Check("Una sola salida (sin NMS embebido)", len(output_shapes) == 1, f"{len(output_shapes)} salida(s)"),
    ]
    if len(output_shapes) == 1:
        channels = 4 + num_classes
        last_two = list(output_shapes[0])[-2:]
        checks.append(Check(f"La salida contiene 4+{num_classes}={channels} canales", channels in last_two, f"shape={list(output_shapes[0])}"))
    checks.append(Check("Tamaño < 15 MB (objetivo PRD §27)", size_mb < 15, f"{size_mb:.1f} MB", fatal=False))
    return checks


def load_interpreter(model_path: Path, threads: int = 1):
    """Usa el primer runtime disponible: ai-edge-litert, tflite-runtime o tensorflow."""
    for module, attr in (("ai_edge_litert.interpreter", "Interpreter"), ("tflite_runtime.interpreter", "Interpreter")):
        try:
            return getattr(__import__(module, fromlist=[attr]), attr)(model_path=str(model_path), num_threads=threads)
        except ImportError:
            continue
    try:
        import tensorflow as tf
    except ImportError as e:
        raise SystemExit("Instala ai-edge-litert, tflite-runtime o tensorflow para verificar el .tflite") from e
    return tf.lite.Interpreter(model_path=str(model_path), num_threads=threads)


def preprocess_image(path: Path, size: int, dtype, scale: float, zero_point: int) -> tuple[np.ndarray, Letterbox]:
    """Letterbox con relleno gris 114 y normalización 0..1, igual que Preprocessor.kt."""
    from PIL import Image

    img = Image.open(path).convert("RGB")
    box = Letterbox(img.width, img.height, size)
    canvas = Image.new("RGB", (size, size), (114, 114, 114))
    canvas.paste(img.resize((box.scaled_w, box.scaled_h), Image.BILINEAR), (int(box.pad_x), int(box.pad_y)))
    arr = np.asarray(canvas, dtype=np.float32) / 255.0
    if np.issubdtype(dtype, np.floating):
        return arr[np.newaxis].astype(dtype), box
    info = np.iinfo(dtype)
    q = np.clip(np.round(arr / scale) + zero_point, info.min, info.max)
    return q[np.newaxis].astype(dtype), box


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--labels", type=Path, default=LABELS_FILE)
    parser.add_argument("--image", type=Path, default=None)
    args = parser.parse_args(argv)

    classes = load_labels(args.labels)
    interpreter = load_interpreter(args.model)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    outs = interpreter.get_output_details()

    print(f"Modelo: {args.model.name}")
    print(f"Entrada : shape={list(inp['shape'])} dtype={np.dtype(inp['dtype']).name} cuant={inp['quantization']}")
    for o in outs:
        print(f"Salida  : shape={list(o['shape'])} dtype={np.dtype(o['dtype']).name} cuant={o['quantization']}")

    checks = check_contract(inp["shape"], [o["shape"] for o in outs], len(classes), args.model.stat().st_size / 1e6)
    for c in checks:
        print(f"[{'OK' if c.ok else ('FALLO' if c.fatal else 'AVISO')}] {c.name} — {c.detail}")
    if any(not c.ok and c.fatal for c in checks):
        print("\nEl modelo NO cumple el contrato de la app: no lo instales en el teléfono.")
        return 1

    if args.image:
        size = int(inp["shape"][1])
        scale, zero_point = inp["quantization"]
        tensor, box = preprocess_image(args.image, size, inp["dtype"], scale or 1.0, zero_point)
        interpreter.set_tensor(inp["index"], tensor)
        interpreter.invoke()
        out = interpreter.get_tensor(outs[0]["index"]).astype(np.float32)
        o_scale, o_zero = outs[0]["quantization"]
        if np.issubdtype(outs[0]["dtype"], np.integer) and o_scale:
            out = (out - o_zero) * o_scale

        dets = postprocess(out, list(classes), size, box)
        coord_max = float(out.reshape(out.shape[-2], out.shape[-1]).max()) if out.size else 0.0
        print(f"\nImagen: {args.image.name}  → {len(dets)} detección(es)")
        print(f"Coordenadas de caja: {'normalizadas 0..1' if coord_max <= NORMALIZED_MAX else 'en píxeles'} (valor máx. del tensor {coord_max:.2f})")
        for d in dets:
            label = d.obj if d.state is None else f"{d.obj}:{d.state}"
            print(f"  {label:20} conf={d.confidence:.2f}  x={d.x:.2f} y={d.y:.2f} w={d.w:.2f} h={d.h:.2f}")
    print("\nContrato OK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
