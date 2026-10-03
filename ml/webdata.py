"""Lógica pura (sin red ni torch) para armar datos de estado con imágenes de licencia abierta.

Flujo:  collect_openverse  ->  autolabel (Grounding DINO)  ->  revisión humana  ->  assemble_web_dataset
La clase-con-estado de cada imagen sale de la CONSULTA con que se buscó ("empty bucket" -> bucket:open),
por eso la revisión humana es obligatoria: una búsqueda trae ruido.
"""
from __future__ import annotations

import hashlib
import re

# clase de Detengue -> (consultas de búsqueda, prompt de detección del objeto)
STATE_PLAN: dict[str, dict] = {
    "bucket:water": {
        "object_prompt": "a bucket.",
        "queries": ["bucket full of water", "bucket of rainwater", "plastic bucket filled with water",
                    "water bucket outdoors", "balde con agua"],
    },
    "bucket:open": {
        "object_prompt": "a bucket.",
        "queries": ["empty bucket", "empty plastic bucket", "empty bucket on ground", "balde vacío"],
    },
    "bucket:inverted": {
        "object_prompt": "a bucket.",
        "queries": ["upside down bucket", "overturned bucket", "bucket turned upside down", "balde boca abajo"],
    },
}

ACCEPTED_LICENSES = ("cc0", "pdm", "by", "by-sa")  # permiten uso comercial con atribución
IMAGE_EXTS = {"jpg", "jpeg", "png", "webp"}


def build_search_params(query: str, page: int, page_size: int = 20) -> dict[str, str]:
    return {
        "q": query,
        "license": ",".join(ACCEPTED_LICENSES),
        "page": str(page),
        "page_size": str(page_size),
        "mature": "false",
    }


def accept_result(item: dict) -> bool:
    """Una fila de Openverse sirve si tiene URL de imagen, licencia aceptada y extensión conocida."""
    url = item.get("url") or ""
    lic = (item.get("license") or "").lower()
    ext = url.rsplit(".", 1)[-1].lower().split("?")[0] if "." in url else ""
    return bool(url) and lic in ACCEPTED_LICENSES and ext in IMAGE_EXTS


def safe_name(item_id: str) -> str:
    return re.sub(r"[^A-Za-z0-9_-]", "_", item_id)[:64]


def attribution_row(item: dict, state: str, query: str) -> list[str]:
    return [safe_name(item["id"]), state, query, item.get("title") or "", item.get("creator") or "",
            f'{item.get("license", "")} {item.get("license_version", "")}'.strip(),
            item.get("foreign_landing_url") or item.get("url") or ""]


def xyxy_to_yolo(box: tuple[float, float, float, float], width: int, height: int) -> tuple[float, float, float, float]:
    """(x1,y1,x2,y2) en píxeles -> (cx,cy,w,h) normalizados, recortados a [0,1]."""
    x1, y1, x2, y2 = box
    x1, x2 = sorted((min(max(x1, 0), width), min(max(x2, 0), width)))
    y1, y2 = sorted((min(max(y1, 0), height), min(max(y2, 0), height)))
    return ((x1 + x2) / 2 / width, (y1 + y2) / 2 / height, (x2 - x1) / width, (y2 - y1) / height)


def pick_best(boxes: list[tuple[float, float, float, float]], scores: list[float], width: int, height: int,
              min_score: float = 0.35, min_area: float = 0.04, max_area: float = 0.95):
    """Caja con mayor score dentro de un rango de área razonable. Solo UNA caja por imagen: la búsqueda
    apunta a un balde protagonista; imágenes con varios baldes se dejan para revisión/descarte."""
    best = None
    for box, score in zip(boxes, scores):
        _, _, w, h = xyxy_to_yolo(box, width, height)
        if score >= min_score and min_area <= w * h <= max_area and (best is None or score > best[1]):
            best = (box, score)
    return best


def split_for_stem(stem: str, val: float = 0.1, test: float = 0.1) -> str:
    """Split determinista por hash del nombre: la misma imagen cae siempre en el mismo split."""
    r = int(hashlib.sha1(stem.encode()).hexdigest()[:8], 16) / 0xFFFFFFFF
    if r < test:
        return "test"
    if r < test + val:
        return "val"
    return "train"
