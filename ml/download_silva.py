"""Descarga el dataset de Silva (Roboflow, formato YOLOv8) a ml/external/silva.

Requiere una API key GRATIS de Roboflow:  export ROBOFLOW_API_KEY=...   y  pip install roboflow
Alternativa sin API: descarga el zip desde la web (formato YOLOv8) y descomprímelo en ml/external/silva.
"""
from __future__ import annotations

import os
import sys

from ml.classes import REPO_ROOT

WORKSPACE, PROJECT, VERSION = "luis-augusto-silva-bq4bv", "mosquito-suh0p", 1


def main() -> int:
    key = os.environ.get("ROBOFLOW_API_KEY")
    if not key:
        print("Falta ROBOFLOW_API_KEY (o descarga el zip a mano; ver ml/WSL.md).", file=sys.stderr)
        return 1
    from roboflow import Roboflow

    dest = REPO_ROOT / "ml" / "external" / "silva"
    Roboflow(api_key=key).workspace(WORKSPACE).project(PROJECT).version(VERSION).download("yolov8", location=str(dest))
    print(f"Dataset en {dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
