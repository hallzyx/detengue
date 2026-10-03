# Entrenar el MVP sin fotos propias (Google Colab, GPU gratis)

Fuente usada: dataset público de mosquito de Luis Augusto Silva (Roboflow Universe, ~5.000 imágenes:
botella, balde, llanta, tanque de agua, piscina, charco). **Revisa su licencia antes de publicar la app o el modelo** (el repo asociado es GPL-3.0).

1. En https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p → *Download Dataset* → formato **YOLOv8** → descarga el zip
   (o con API key gratis). Descomprime en `ml/external/silva/` (debe haber `data.yaml` y `train/ valid/ test/`).
2. En Colab (Runtime → GPU):
```
!git clone https://github.com/hallzyx/detengue && cd detengue && pip -q install -r ml/requirements.txt
# sube/descomprime el zip en detengue/ml/external/silva
!cd detengue && python -m ml.import_external --source ml/external/silva --name silva --license GPL-3.0 \
   --url https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p \
   --map "Bottle=bottle,Bucket=bucket,Tire=tire,Water tanks=water_tank"
!cd detengue && python -m ml.validate_dataset && python -m ml.make_data_yaml
!cd detengue && python -m ml.train --epochs 60 && python -m ml.evaluate
!cd detengue && python -m ml.export_tflite   # genera android/models/detengue_detector_int8.tflite
```
3. Descarga `android/models/detengue_detector_int8.tflite`, cópialo al mismo path del repo, sube `MODEL_VERSION` y rellena `docs/MODEL_CARD.md` con los números de `ml.evaluate`.

Notas honestas: las imágenes del dataset incluyen vistas aéreas/dron y son de Brasil; en un balde dentro de una casa el rendimiento puede ser menor. Los nombres de clase de origen deben coincidir con `data.yaml` (el script lista las disponibles si te equivocas). Pasos 2–3 no se ejecutaron aquí.

---
# Mejora: estados del balde con imágenes web (sin fotos propias)

Añade `bucket:open | water | inverted` usando imágenes de licencia abierta (Openverse) + etiquetado asistido (Grounding DINO).
Reemplaza el paso 2 de arriba por este orden:
```
!cd detengue && cp ml/labels_mvp_states.txt android/models/detengue_labels.txt
!cd detengue && pip -q install transformers torch pillow
# 1) Silva SIN balde (el balde con estado viene de la web):
!cd detengue && python -m ml.import_external --source ml/external/silva --name silva --license GPL-3.0 \
   --url https://universe.roboflow.com/luis-augusto-silva-bq4bv/mosquito-suh0p \
   --map "Bottle=bottle,Tire=tire,Water tanks=water_tank"
# 2) Descarga y autoetiqueta
!cd detengue && python -m ml.collect_openverse --per-state 120
!cd detengue && python -m ml.autolabel
```
3. **Revisión (≈10–15 min, obligatoria):** mira `ml/external/web_labeled/_preview/` y borra de
   `ml/external/web_labeled/<estado>/` el `.jpg` de cada foto con caja mala o estado equivocado
   (la búsqueda "empty bucket" trae baldes llenos, juguetes de playa, etc.).
4. `python -m ml.assemble_web_dataset` y luego `validate_dataset → make_data_yaml → train → evaluate → export_tflite` como antes.

Advertencias: el estado sale de la consulta, no de un clasificador; lo que no revises entra como etiqueta ruidosa.
Las fotos web son más "de catálogo" que las de un patio real: mide con tus fotos antes de confiar. Las licencias BY/BY-SA exigen
atribución: conserva `ml/external/web/ATTRIBUTION.csv`. Estos scripts no se ejecutaron aquí (sin acceso a Openverse/Hugging Face);
la lógica pura (licencias, cajas, splits) sí tiene tests.
