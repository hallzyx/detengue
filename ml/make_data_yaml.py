"""Genera `ml/dataset/data.yaml` para Ultralytics a partir de `android/models/detengue_labels.txt`."""
from __future__ import annotations

import argparse
from pathlib import Path

from ml.classes import REPO_ROOT, load_labels


def build_yaml(dataset: Path, names: list[str]) -> str:
    lines = [
        f"path: {dataset.resolve().as_posix()}",
        "train: images/train",
        "val: images/val",
        "test: images/test",
        f"nc: {len(names)}",
        "names:",
    ]
    lines += [f"  {i}: '{name}'" for i, name in enumerate(names)]
    return "\n".join(lines) + "\n"


def main(argv: list[str] | None = None) -> Path:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=REPO_ROOT / "ml" / "dataset")
    args = parser.parse_args(argv)

    args.dataset.mkdir(parents=True, exist_ok=True)
    target = args.dataset / "data.yaml"
    target.write_text(build_yaml(args.dataset, [c.name for c in load_labels()]), encoding="utf-8")
    print(f"Escrito {target}")
    return target


if __name__ == "__main__":
    main()
