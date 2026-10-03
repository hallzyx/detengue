# Model card — detengue-detector

**Estado: modelo MVP entrenado (experimental).** Las clases de estado del balde (`bucket:*`) tienen casi
nada de datos y **no son fiables** (ver Limitaciones). Los campos *pendiente* no se han medido.

| Campo | Valor |
|---|---|
| Versión | `detengue-0.2.0` |
| Arquitectura | YOLO11n (Ultralytics), un solo detector, 2,59 M parámetros |
| Entrenamiento | 30 épocas, batch 16, imgsz 320, GPU RTX 3050 (WSL2); partida desde pesos preentrenados |
| Exportación | litert-torch (Ultralytics 8.4.172), INT8 estático (pesos+activaciones int8, E/S float32), 3,1 MB |
| Entrada | 320×320 RGB, NHWC `[1,320,320,3]`, float32 0..1, letterbox gris 114 |
| Salida | `[1,10,2100]` = 4 coordenadas (normalizadas 0..1) + 6 clases, sin NMS embebido |
| Clases | `bucket:open`, `bucket:water`, `bucket:inverted`, `bottle`, `tire`, `water_tank` |
| Runtime | TensorFlow Lite 2.16.1, CPU + XNNPACK, sin Play Services |
| Verificación del contrato | `ml.verify_tflite`: OK (NHWC, una salida, 4+6 canales, < 15 MB) |
| Latencia en gama baja | *pendiente* (`ml/benchmark/benchmark_tflite.py` + pantalla de diagnóstico; no medida) |

## Datos

| Split | Imágenes | `bucket:open` | `bucket:water` | `bucket:inverted` | bottle | tire | water_tank |
|---|---|---|---|---|---|---|---|
| train | 2683 | 13 | 3 | 3 | 526 | 2604 | 2106 |
| val | 209 | 1 | 2 | 0 | 33 | 133 | 340 |
| test | 109 | 0 | 1 | 1 | 9 | 62 | 173 |

(cajas por clase). Fuentes:
- Dataset público de Silva (Roboflow Universe, GPL-3.0): `bottle`, `tire`, `water_tank` (imágenes con muchas vistas aéreas, de Brasil).
- Imágenes web de Openverse (CC0/PDM/BY/BY-SA, atribuciones en `ml/external/web/ATTRIBUTION.csv`) autoetiquetadas con Grounding DINO para `bucket:*`.
  La revisión humana la hizo el propio asistente (Claude) sobre 155 fotos y solo se conservaron 24
  (4 invertidas, 14 abiertas, 6 con agua); el resto tenía caja o estado erróneo. **No fue una revisión de un experto.**

## Métricas (split de test, `ml.evaluate`, modelo PyTorch `best.pt`)

| Métrica | Valor |
|---|---|
| Precision | 0.882 |
| Recall | 0.770 |
| F1 | 0.822 |
| mAP@0.5 | 0.834 |
| mAP@0.5:0.95 | 0.510 |

| Clase | Instancias test | Precision | Recall | F1 | AP@0.5 |
|---|---|---|---|---|---|
| bucket:open | 0 | sin datos | sin datos | sin datos | sin datos |
| bucket:water | 1 | 0.712 | 1.000 | 0.832 | 0.995 |
| bucket:inverted | 1 | 1.000 | 0.000 | 0.000 | 0.332 |
| bottle | 9 | 0.833 | 0.889 | 0.860 | 0.885 |
| tire | 62 | 0.903 | 0.968 | 0.934 | 0.962 |
| water_tank | 173 | 0.964 | 0.994 | 0.979 | 0.995 |

Las métricas son del modelo en PyTorch FP32. **No se midieron sobre el `.tflite` INT8**; solo se comprobó
a mano que el `.tflite` detecta al menos una clase real en 58 de 60 imágenes de test (confianza > 0,4, sin letterbox).

## Limitaciones conocidas

- **`bucket:water` y `bucket:inverted` no son medibles:** 1 instancia de test cada una (3 de entrenamiento). Los AP de 0.995 y 0.332 son ruido estadístico, no evidencia.
  `bucket:open` tiene 0 instancias de test y 13 de entrenamiento.
- Sin imágenes propias ni de patios reales: las fotos web son "de catálogo" y las de Silva incluyen vistas de dron.
- Sin evaluación con poca luz, objetos lejanos/parciales ni contenedores no vistos.
- Sin latencia ni consumo medidos en un teléfono. El APK no se ha probado en dispositivo.
- Licencia: el dataset de Silva es GPL-3.0; revisar su implicación antes de publicar el modelo.

## Uso previsto y privacidad

Apoyo a la inspección doméstica; no diagnostica dengue ni sustituye criterio humano ni a salud pública.
Inferencia 100 % en el dispositivo; sin INTERNET en el manifiesto.
