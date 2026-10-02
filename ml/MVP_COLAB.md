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
