# Correr todo en WSL2 (Windows + GPU NVIDIA, p. ej. RTX 3050)

## 1. Instalar WSL2 (una vez)
En PowerShell **como administrador**: `wsl --install -d Ubuntu-22.04`, reinicia y crea tu usuario.
Instala/actualiza el **driver NVIDIA para Windows** (el normal de GeForce; NO instales driver de Linux dentro de WSL).

## 2. Dentro de Ubuntu (WSL)
```bash
sudo apt update && sudo apt install -y python3-venv python3-pip git
git clone https://github.com/hallzyx/detengue ~/detengue && cd ~/detengue     # trabaja en ~/, NO en /mnt/c (muy lento)
python3 -m venv .venv && source .venv/bin/activate
pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121   # PyTorch con CUDA
pip install -r ml/requirements.txt transformers roboflow
python3 -c "import torch; print(torch.cuda.is_available(), torch.cuda.get_device_name(0))"   # debe decir True y tu GPU
```
Si da `False`: actualiza el driver de Windows y ejecuta `wsl --shutdown` en PowerShell.

## 3. Dataset de Silva
Opción A (API): crea cuenta gratis en Roboflow, copia tu API key y `export ROBOFLOW_API_KEY=...`.
Opción B (manual): en https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p → *Download Dataset* → **YOLOv8** → descomprime en `ml/external/silva/` (debe haber `data.yaml`).
Revisa la licencia antes de publicar (el repo asociado es GPL-3.0).

## 4. Lanzar
```bash
ml/run_local.sh --epochs 60 --batch 16
```
Se detiene tras el autoetiquetado: abre desde Windows `\\wsl$\Ubuntu-22.04\home\<usuario>\detengue\ml\external\web_labeled\_preview`,
borra las fotos malas en `web_labeled/<estado>/` y pulsa ENTER. Si algo falla, corrige y relanza: salta lo ya hecho.

## 5. Ajustes
- GPU de 4 GB sin memoria: `--batch 8`. Mucho tiempo: baja `--epochs` (30 ya da algo usable para demo).
- Si el export a TFLite falla instalando TensorFlow, pega el error: suele ser versión de Python (usa 3.10–3.11).
- El `.tflite` queda en `android/models/`; cópialo de WSL a Windows o haz commit desde WSL.

Ningún paso de este flujo se ejecutó en el entorno donde se escribió (sin GPU ni acceso a estos sitios).
