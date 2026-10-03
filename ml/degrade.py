"""Degrada imágenes (cámara de baja calidad) para que las sintéticas "de catálogo" se parezcan más a un celular económico.

Solo cambia la foto, no la geometría: las cajas YOLO (.txt) siguen valiendo tal cual.
Efectos: desenfoque, desenfoque de movimiento, baja resolución, ruido de sensor, compresión JPEG fuerte, poca luz y
contraluz/sobreexposición. A cada imagen se le aplican 2-3 efectos elegidos al azar, de forma determinista por nombre.

Entrada:  ml/external/syn_labeled/<clase>/{x.jpg,x.txt}  y  ml/external/syn_neg/*.jpg
Salida:   ml/external/syn_degraded/<clase>/{x_deg.jpg,x_deg.txt}  y  syn_degraded/negativos/x_deg.jpg
Uso:      python -m ml.degrade
"""
from __future__ import annotations

import argparse
import io
import random
import shutil
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

from ml.classes import REPO_ROOT


def _blur(img: Image.Image, rng: random.Random) -> Image.Image:
    return img.filter(ImageFilter.GaussianBlur(radius=rng.uniform(0.8, 2.2)))


def _motion(img: Image.Image, rng: random.Random) -> Image.Image:
    k = rng.choice([7, 9, 11])
    arr = np.asarray(img, dtype=np.float32)
    out = np.zeros_like(arr)
    horizontal = rng.random() < 0.5
    for i in range(k):
        out += np.roll(arr, i - k // 2, axis=1 if horizontal else 0)
    return Image.fromarray(np.clip(out / k, 0, 255).astype(np.uint8))


def _lowres(img: Image.Image, rng: random.Random) -> Image.Image:
    w, h = img.size
    s = rng.uniform(0.25, 0.5)
    return img.resize((max(8, int(w * s)), max(8, int(h * s))), Image.BILINEAR).resize((w, h), Image.BILINEAR)


def _noise(img: Image.Image, rng: random.Random) -> Image.Image:
    arr = np.asarray(img, dtype=np.float32)
    gen = np.random.default_rng(rng.getrandbits(32))
    arr = arr + gen.normal(0, rng.uniform(8, 24), arr.shape)
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))


def _jpeg(img: Image.Image, rng: random.Random) -> Image.Image:
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=rng.randint(12, 35))
    return Image.open(io.BytesIO(buf.getvalue())).convert("RGB")


def _lowlight(img: Image.Image, rng: random.Random) -> Image.Image:
    arr = (np.asarray(img, dtype=np.float32) / 255.0) ** rng.uniform(1.6, 2.4) * rng.uniform(0.5, 0.8)
    return Image.fromarray(np.clip(arr * 255, 0, 255).astype(np.uint8))


def _backlight(img: Image.Image, rng: random.Random) -> Image.Image:
    arr = np.asarray(img, dtype=np.float32)
    h, w, _ = arr.shape
    ramp = np.linspace(0, 1, h if rng.random() < 0.5 else w, dtype=np.float32)
    glow = (ramp[:, None, None] if ramp.size == h else ramp[None, :, None]) * rng.uniform(60, 130)
    flat = arr * rng.uniform(0.75, 0.95) + 25
    return Image.fromarray(np.clip(flat + glow, 0, 255).astype(np.uint8))


EFFECTS = [_blur, _motion, _lowres, _noise, _jpeg, _lowlight, _backlight]


def degrade(img: Image.Image, seed: str) -> Image.Image:
    """Misma resolución y geometría; 2-3 efectos al azar, siempre iguales para el mismo `seed`."""
    rng = random.Random(zlib.crc32(seed.encode()))
    out = img.convert("RGB")
    for effect in rng.sample(EFFECTS, rng.choice([2, 3])):
        out = effect(out, rng)
    return _jpeg(out, rng) if rng.random() < 0.6 else out


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--labeled", type=Path, default=REPO_ROOT / "ml" / "external" / "syn_labeled")
    ap.add_argument("--negatives", type=Path, default=REPO_ROOT / "ml" / "external" / "syn_neg")
    ap.add_argument("--out", type=Path, default=REPO_ROOT / "ml" / "external" / "syn_degraded")
    args = ap.parse_args(argv)
    n = 0
    for img in sorted(args.labeled.glob("*/*.jpg")):
        lbl = img.with_suffix(".txt")
        if not lbl.is_file():
            continue
        dst = args.out / img.parent.name
        dst.mkdir(parents=True, exist_ok=True)
        degrade(Image.open(img), img.name).save(dst / f"{img.stem}_deg.jpg", quality=92)
        shutil.copyfile(lbl, dst / f"{img.stem}_deg.txt")
        n += 1
    for img in sorted(args.negatives.glob("*.jpg")) if args.negatives.is_dir() else []:
        dst = args.out / "negativos"
        dst.mkdir(parents=True, exist_ok=True)
        degrade(Image.open(img), img.name).save(dst / f"{img.stem}_deg.jpg", quality=92)
        n += 1
    print(f"degradadas: {n}  ->  {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
