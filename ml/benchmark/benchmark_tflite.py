"""Latencia del `.tflite` en ESTE equipo (escritorio). Orientativa: NO sustituye al benchmark en el
Android de gama baja (PRD §63), que se hace con la pantalla Diagnóstico de la app.

    python -m ml.benchmark.benchmark_tflite --model android/models/detengue_detector_int8.tflite --threads 4
"""
from __future__ import annotations

import argparse
import time
from pathlib import Path

import numpy as np

from ml.verify_tflite import load_interpreter


def summarize(times_ms: list[float]) -> dict:
    arr = np.sort(np.asarray(times_ms))
    rank = lambda p: float(arr[max(0, int(np.ceil(p / 100 * len(arr))) - 1)])  # rango más cercano, como LatencyStats.kt
    return dict(n=len(arr), mean=float(arr.mean()), p50=rank(50), p95=rank(95), max=float(arr.max()))


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--runs", type=int, default=50)
    parser.add_argument("--warmup", type=int, default=5)
    args = parser.parse_args(argv)

    interpreter = load_interpreter(args.model, threads=args.threads)
    interpreter.allocate_tensors()
    inp = interpreter.get_input_details()[0]
    dummy = np.zeros(inp["shape"], dtype=inp["dtype"])

    times = []
    for i in range(args.warmup + args.runs):
        interpreter.set_tensor(inp["index"], dummy)
        t0 = time.perf_counter()
        interpreter.invoke()
        if i >= args.warmup:
            times.append((time.perf_counter() - t0) * 1000)

    s = summarize(times)
    print(f"{args.model.name}  threads={args.threads}  n={s['n']}")
    print(f"mean={s['mean']:.1f} ms  p50={s['p50']:.1f} ms  p95={s['p95']:.1f} ms  max={s['max']:.1f} ms")
    print("Objetivo del PRD en el teléfono: p50 < 700 ms, p95 < 1.5 s. Mídelo en el dispositivo real.")


if __name__ == "__main__":
    main()
