#!/usr/bin/env bash
# Pipeline completo del MVP en WSL2/Linux con GPU NVIDIA. Se puede reanudar: cada etapa se salta si ya está hecha.
# Uso:  ml/run_local.sh [--epochs 60] [--batch 16]
# Pausa tras el etiquetado para que REVISES ml/external/web_labeled/_preview (ver ml/WSL.md).
set -euo pipefail
cd "$(dirname "$0")/.."

EPOCHS=60; BATCH=16
while [[ $# -gt 0 ]]; do
  case "$1" in
    --epochs) EPOCHS="$2"; shift 2 ;;
    --batch) BATCH="$2"; shift 2 ;;
    *) echo "Argumento desconocido: $1" >&2; exit 2 ;;
  esac
done
step() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }

python3 -c "import torch; assert torch.cuda.is_available(), 'CUDA no disponible'; print('GPU:', torch.cuda.get_device_name(0))" \
  || { echo "PyTorch no ve la GPU. Revisa ml/WSL.md (sección 2)." >&2; exit 1; }

step "0. Clases con estado"
cp ml/labels_mvp_states.txt android/models/detengue_labels.txt

step "1. Dataset público (Silva)"
if [[ ! -f ml/external/silva/data.yaml ]]; then python3 -m ml.download_silva; fi
if [[ ! -f ml/dataset/.silva_done ]]; then
  python3 -m ml.import_external --source ml/external/silva --name silva --license GPL-3.0 \
    --url https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p \
    --map "Water tanks=water_tank"
  touch ml/dataset/.silva_done
fi

step "2. Imágenes web (Openverse) + autoetiquetado"
if [[ ! -d ml/external/web ]]; then python3 -m ml.collect_openverse --per-state 120; fi
if [[ ! -d ml/external/web_labeled ]]; then python3 -m ml.autolabel; fi

if [[ ! -f ml/dataset/.web_done ]]; then
  echo
  echo ">> REVISIÓN HUMANA: abre ml/external/web_labeled/_preview y borra de ml/external/web_labeled/<estado>/"
  echo ">> el .jpg de toda foto con caja mala o estado equivocado."
  read -r -p ">> Cuando termines, pulsa ENTER para continuar (Ctrl+C para salir y reanudar luego)... " _
  python3 -m ml.assemble_web_dataset
  touch ml/dataset/.web_done
fi

step "3. Validar dataset"
python3 -m ml.make_data_yaml
python3 -m ml.validate_dataset || { echo "Corrige los errores y vuelve a lanzar." >&2; exit 1; }

step "4. Entrenar ($EPOCHS épocas, batch $BATCH)"
python3 -m ml.train --epochs "$EPOCHS" --batch "$BATCH"
W=ml/runs/detengue/weights/best.pt

step "5. Evaluar"
python3 -m ml.evaluate --weights "$W"

step "6. Exportar TFLite INT8 y verificar"
python3 -m ml.export_tflite --weights "$W"

echo
echo "Listo: android/models/detengue_detector_int8.tflite"
echo "Siguiente: sube MODEL_VERSION en DetectionConfig.kt, rellena docs/MODEL_CARD.md con ml/runs/**/metrics y compila la app."
