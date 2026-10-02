"""Port en Python (numpy) del preprocesado letterbox + postprocesado YOLO + NMS de la app.

Sirve para verificar un `.tflite` en el escritorio ANTES de instalarlo en el teléfono, y para
comprobar que Python y Kotlin interpretan la salida del modelo exactamente igual (los tests usan
los mismos vectores que `YoloPostprocessorTest.kt`).
"""
from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np

from ml.classes import ModelClass

DETECTION_THRESHOLD = 0.40  # = DetectionConfig.DETECTION_THRESHOLD
NMS_THRESHOLD = 0.45  # = DetectionConfig.NMS_THRESHOLD
MAX_DETECTIONS = 20
MAX_CANDIDATES = 100
MIN_BOX_SIZE = 0.01
NORMALIZED_MAX = 1.5


def _round_half_up(x: float) -> int:
    """Igual que `roundToInt()` de Kotlin (Python redondea a par, Kotlin hacia arriba)."""
    return int(math.floor(x + 0.5))


@dataclass(frozen=True)
class Letterbox:
    src_w: int
    src_h: int
    dst: int

    @property
    def scale(self) -> float:
        return min(self.dst / self.src_w, self.dst / self.src_h)

    @property
    def scaled_w(self) -> int:
        return min(max(_round_half_up(self.src_w * self.scale), 1), self.dst)

    @property
    def scaled_h(self) -> int:
        return min(max(_round_half_up(self.src_h * self.scale), 1), self.dst)

    @property
    def pad_x(self) -> float:
        return (self.dst - self.scaled_w) / 2.0

    @property
    def pad_y(self) -> float:
        return (self.dst - self.scaled_h) / 2.0

    def unmap_x(self, x: float) -> float:
        return min(max((x - self.pad_x) / self.scaled_w, 0.0), 1.0)

    def unmap_y(self, y: float) -> float:
        return min(max((y - self.pad_y) / self.scaled_h, 0.0), 1.0)


@dataclass(frozen=True)
class Detection:
    obj: str
    state: str | None
    confidence: float
    x: float
    y: float
    w: float
    h: float


def _iou(a: Detection, b: Detection) -> float:
    iw = min(a.x + a.w, b.x + b.w) - max(a.x, b.x)
    ih = min(a.y + a.h, b.y + b.h) - max(a.y, b.y)
    if iw <= 0 or ih <= 0:
        return 0.0
    inter = iw * ih
    union = a.w * a.h + b.w * b.h - inter
    return 0.0 if union <= 0 else inter / union


def postprocess(
    output: np.ndarray,
    classes: list[ModelClass | None],
    input_size: int,
    box: Letterbox,
    detection_threshold: float = DETECTION_THRESHOLD,
    nms_threshold: float = NMS_THRESHOLD,
    max_detections: int = MAX_DETECTIONS,
) -> list[Detection]:
    """Decodifica `[1, 4+nc, N]` o `[1, N, 4+nc]`. Misma lógica que `YoloPostprocessor.kt`."""
    out = np.asarray(output, dtype=np.float32)
    out = out.reshape(out.shape[-2], out.shape[-1])
    channels = 4 + len(classes)

    if out.shape[0] == channels:
        table = out.T  # -> [N, C]
    elif out.shape[1] == channels:
        table = out
    else:
        raise ValueError(f"Salida {tuple(out.shape)} no coincide con 4 + {len(classes)} clases")

    candidates = []  # (score, ModelClass, cx, cy, w, h)
    for row in table:
        best_score, best_class = 0.0, None
        for k, cls in enumerate(classes):
            if cls is None:
                continue
            score = float(row[4 + k])
            if score > best_score:
                best_score, best_class = score, cls
        if best_class is not None and best_score >= detection_threshold:
            candidates.append((best_score, best_class, *map(float, row[:4])))
    if not candidates:
        return []

    candidates.sort(key=lambda c: -c[0])
    candidates = candidates[:MAX_CANDIDATES]

    max_coord = max(max(c[2:6]) for c in candidates)
    to_pixels = float(input_size) if max_coord <= NORMALIZED_MAX else 1.0

    boxed: list[Detection] = []
    for score, cls, cx, cy, w, h in candidates:
        x1 = box.unmap_x((cx - w / 2) * to_pixels)
        y1 = box.unmap_y((cy - h / 2) * to_pixels)
        x2 = box.unmap_x((cx + w / 2) * to_pixels)
        y2 = box.unmap_y((cy + h / 2) * to_pixels)
        bw, bh = x2 - x1, y2 - y1
        if bw < MIN_BOX_SIZE or bh < MIN_BOX_SIZE:
            continue
        boxed.append(Detection(cls.obj, cls.state, score, x1, y1, bw, bh))

    kept: list[Detection] = []
    for det in boxed:  # NMS por OBJETO (no por clase completa), ya ordenado por confianza
        if any(k.obj == det.obj and _iou(k, det) > nms_threshold for k in kept):
            continue
        kept.append(det)
        if len(kept) >= max_detections:
            break
    return kept
