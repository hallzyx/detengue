"""Exporta el detector a TFLite INT8 de 320x320 y lo deja en `android/models/` (PRD §25).

    python -m ml.export_tflite --weights ml/runs/detengue/weights/best.pt

La cuantización INT8 usa imágenes del split `val` de data.yaml como calibración: deben ser
representativas de la escena real. Tras exportar, ejecuta `ml.verify_tflite` automáticamente.

NOTA: este script NO se ha ejecutado en el entorno donde se escribió (sin dataset ni acceso a PyPI).
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from ml.classes import REPO_ROOT
from ml.train import IMG_SIZE

DEFAULT_OUT = REPO_ROOT / "android" / "models" / "detengue_detector_int8.tflite"


def find_int8_tflite(search_dir: Path) -> Path | None:
    """Ultralytics deja `<pesos>_saved_model/<pesos>_int8.tflite`; devuelve el primero que encuentre."""
    matches = sorted(search_dir.rglob("*_int8.tflite"))
    return matches[0] if matches else None


def _patch_litert_nhwc() -> None:
    """Ultralytics >= 8.4 exporta TFLite con litert-torch y entrada NCHW; la app exige NHWC [1,S,S,3].

    Envuelve el modelo con una permutación NHWC->NCHW y permuta igual las imágenes de calibración.
    """
    import litert_torch
    import torch
    from ultralytics.engine.exporter import Exporter

    class _NhwcInput(torch.nn.Module):
        def __init__(self, model):
            super().__init__()
            self.model = model

        def forward(self, x):
            return self.model(x.permute(0, 3, 1, 2))

    convert = litert_torch.convert
    litert_torch.convert = lambda model, args, **kw: convert(
        _NhwcInput(model).eval(), (args[0].permute(0, 2, 3, 1).contiguous(),), **kw
    )

    get_loader = Exporter.get_int8_calibration_dataloader

    def nhwc_loader(self, *a, **kw):
        loader = get_loader(self, *a, **kw)

        class _Nhwc:
            def __iter__(_):
                for batch in loader:
                    yield {**batch, "img": batch["img"].permute(0, 2, 3, 1).contiguous()}

        return _Nhwc()

    Exporter.get_int8_calibration_dataloader = nhwc_loader


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--weights", type=Path, required=True)
    parser.add_argument("--data", type=Path, default=REPO_ROOT / "ml" / "dataset" / "data.yaml")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--image", type=Path, default=None, help="foto de prueba para la verificación posterior")
    args = parser.parse_args(argv)

    from ultralytics import YOLO

    _patch_litert_nhwc()
    exported = Path(YOLO(str(args.weights)).export(format="tflite", imgsz=IMG_SIZE, int8=True, data=str(args.data), batch=1))
    candidate = exported if exported.suffix == ".tflite" and "int8" in exported.name else None
    candidate = candidate or find_int8_tflite(exported if exported.is_dir() else exported.parent)
    if candidate is None:
        raise SystemExit(f"No encontré un *_int8.tflite tras exportar (salida: {exported}). Revisa el log de Ultralytics.")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(candidate, args.out)
    print(f"\nModelo copiado a {args.out} ({args.out.stat().st_size / 1e6:.1f} MB)")

    from ml.verify_tflite import main as verify

    verify(["--model", str(args.out)] + (["--image", str(args.image)] if args.image else []))
    print(
        "\nAntes de instalar en el teléfono:\n"
        "  1. Sube DetectionConfig.MODEL_VERSION (android/.../core/config/DetectionConfig.kt).\n"
        "  2. Actualiza docs/MODEL_CARD.md con las métricas de `ml.evaluate`.\n"
        "  3. Mide latencia REAL en el Android de gama baja con la pantalla Diagnóstico (PRD §63)."
    )


if __name__ == "__main__":
    main()
