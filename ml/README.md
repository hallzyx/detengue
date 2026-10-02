# Pipeline ML

Requisitos: `pip install -r ml/requirements.txt` (ultralytics, tensorflow; no se ejecutaron en el entorno de desarrollo).

```
python -m ml.validate_dataset ml/dataset
python -m ml.make_data_yaml
python -m ml.train
python -m ml.evaluate
python -m ml.export_tflite     # copia a android/models/ y verifica contrato
python -m ml.benchmark.benchmark_tflite
```
Tras exportar: subir `MODEL_VERSION` en `DetectionConfig`, rellenar `docs/MODEL_CARD.md` y medir en un dispositivo real.
Tests: `python -m unittest discover -s ml/tests -t .`
