# Model card — detengue-detector

**Estado: NO HAY MODELO ENTRENADO.** Este repositorio no incluye ningún `.tflite`. Los campos marcados *pendiente* se llenan con métricas reales tras entrenar; no se inventan.

| Campo | Valor |
|---|---|
| Versión | `detengue-0.1.0` (se incrementa al publicar un modelo real) |
| Arquitectura | YOLO nano (Ultralytics), un solo detector |
| Entrada | 320×320 RGB, NHWC, INT8, letterbox gris 114 |
| Salida | `[1,4+nc,N]` o `[1,N,4+nc]` (se autodetecta) |
| Clases | `android/models/detengue_labels.txt` (objeto:estado) |
| Runtime | TensorFlow Lite 2.16.1, CPU + XNNPACK, sin Play Services |
| Datos de entrenamiento | *pendiente* (fotos propias, ver `ml/dataset/README.md`) |
| Tamaño de dataset / splits | *pendiente* |
| Métricas (mAP50, P/R/F1 por clase) | *pendiente* (`ml/evaluate.py`) |
| Latencia en gama baja | *pendiente* (`ml/benchmark/benchmark_tflite.py` + pantalla de diagnóstico) |
| Limitaciones conocidas | Sin evidencia aún. Esperable: fallos con poca luz, objetos lejanos/parciales, contenedores no vistos. |
| Uso previsto | Apoyo a la inspección doméstica; no sustituye criterio humano ni a salud pública. |
| Privacidad | Inferencia 100 % en el dispositivo; sin INTERNET en el manifiesto. |
