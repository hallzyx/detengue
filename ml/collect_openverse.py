"""Descarga imágenes con licencia abierta (CC0 / PDM / CC BY / CC BY-SA) de Openverse por estado.

Salida:  ml/external/web/<clase:estado>/<id>.jpg   +  ml/external/web/ATTRIBUTION.csv (autor, licencia, fuente)
Uso (Colab o PC con internet):  python -m ml.collect_openverse --per-state 120
Respeta el límite anónimo de Openverse (pausa entre páginas). Las licencias BY/BY-SA exigen atribución:
conserva ATTRIBUTION.csv junto al modelo/dataset si lo distribuyes.
"""
from __future__ import annotations

import argparse
import csv
import io
import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

from ml.classes import REPO_ROOT
from ml.webdata import STATE_PLAN, accept_result, attribution_row, build_search_params, safe_name

API = "https://api.openverse.org/v1/images/"
UA = {"User-Agent": "detengue-dataset-collector/0.1 (research; contact via github.com/hallzyx/detengue)"}
MAX_SIDE = 1024


def _get(url: str, timeout: int = 30) -> bytes:
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=timeout) as r:
        return r.read()


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--per-state", type=int, default=120)
    ap.add_argument("--pages", type=int, default=5, help="páginas por consulta")
    ap.add_argument("--out", type=Path, default=REPO_ROOT / "ml" / "external" / "web")
    args = ap.parse_args(argv)

    from PIL import Image  # import tardío: los tests de lógica no lo necesitan

    args.out.mkdir(parents=True, exist_ok=True)
    attribution = args.out / "ATTRIBUTION.csv"
    seen: set[str] = set()
    if attribution.exists():
        seen = {row[0] for row in csv.reader(attribution.open(encoding="utf-8"))}
    new_file = not attribution.exists()
    with attribution.open("a", newline="", encoding="utf-8") as af:
        writer = csv.writer(af)
        if new_file:
            writer.writerow(["id", "state", "query", "title", "creator", "license", "source"])
        for state, plan in STATE_PLAN.items():
            folder = args.out / state.replace(":", "_")
            folder.mkdir(exist_ok=True)
            got = len(list(folder.glob("*.jpg")))
            for query in plan["queries"]:
                for page in range(1, args.pages + 1):
                    if got >= args.per_state:
                        break
                    url = API + "?" + urllib.parse.urlencode(build_search_params(query, page))
                    try:
                        results = json.loads(_get(url))["results"]
                    except Exception as e:  # red, límite de tasa, fin de resultados
                        print(f"  [{state}] '{query}' p{page}: {e}")
                        break
                    time.sleep(1.0)
                    for item in results:
                        name = safe_name(item.get("id", ""))
                        if got >= args.per_state or not name or name in seen or not accept_result(item):
                            continue
                        try:
                            img = Image.open(io.BytesIO(_get(item["url"]))).convert("RGB")
                            img.thumbnail((MAX_SIDE, MAX_SIDE))
                            img.save(folder / f"{name}.jpg", quality=90)
                        except Exception:
                            continue
                        seen.add(name)
                        writer.writerow(attribution_row(item, state, query))
                        got += 1
            print(f"{state}: {got} imágenes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
