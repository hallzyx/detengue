"""Clases del detector, leídas de `android/models/detengue_labels.txt` (única fuente de verdad).

Espejo en Python de `ModelClass.parse` (Kotlin). A diferencia de la app, que ignora etiquetas
desconocidas, el entrenamiento es ESTRICTO: una línea inválida debe romper, no ignorarse.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

OBJECTS = frozenset(
    {"bucket", "container", "plant_pot", "bottle", "tire", "pet_bowl", "drum", "water_tank"}
)
STATES = frozenset({"open", "water", "closed", "inverted"})

REPO_ROOT = Path(__file__).resolve().parents[1]
LABELS_FILE = REPO_ROOT / "android" / "models" / "detengue_labels.txt"


@dataclass(frozen=True)
class ModelClass:
    obj: str
    state: str | None = None  # None = el modelo no distingue estado

    @property
    def name(self) -> str:
        return self.obj if self.state is None else f"{self.obj}:{self.state}"


def parse_label(line: str) -> ModelClass | None:
    """Mismo contrato que `ModelClass.parse` en Kotlin. `None` si el objeto o el estado no existen."""
    parts = line.strip().split(":", 1)
    obj = parts[0].strip()
    if obj not in OBJECTS:
        return None
    if len(parts) == 1:
        return ModelClass(obj)
    state = parts[1].strip()
    if state not in STATES:
        return None
    return ModelClass(obj, state)


def load_labels(path: Path = LABELS_FILE) -> list[ModelClass]:
    """Devuelve las clases en el orden en que las emite el modelo (índice = posición)."""
    classes: list[ModelClass] = []
    for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parsed = parse_label(line)
        if parsed is None:
            raise ValueError(f"{path.name}:{number}: etiqueta inválida {line!r}")
        if parsed in classes:
            raise ValueError(f"{path.name}:{number}: etiqueta duplicada {line!r}")
        classes.append(parsed)
    if not classes:
        raise ValueError(f"{path.name} no define ninguna clase")
    return classes
