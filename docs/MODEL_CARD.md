# Model card — detengue-detector

**Estado: modelo MVP entrenado (experimental).** Enfocado en baldes (3 estados) y tanques de agua. Los tests de
baldes son muy pequeños (ver Limitaciones). Los campos *pendiente* no se han medido.

| Campo | Valor |
|---|---|
| Versión | `detengue-0.4.0` |
| Arquitectura | YOLO11n (Ultralytics), un solo detector, ~2,6 M parámetros |
| Entrenamiento | 30 épocas, batch 16, imgsz 320, GPU RTX 3050 (WSL2); partida desde pesos preentrenados |
| Exportación | litert-torch (Ultralytics 8.4.172), INT8 estático (pesos+activaciones int8, E/S float32), 3,1 MB; entrada permutada a NHWC (`ml/export_tflite.py`) |
| Entrada | 320×320 RGB, NHWC `[1,320,320,3]`, float32 0..1, letterbox gris 114 |
| Salida | `[1,8,2100]` = 4 coordenadas (normalizadas 0..1) + 4 clases, sin NMS embebido |
| Clases | `bucket:open`, `bucket:water`, `bucket:inverted`, `water_tank` |
| Runtime | TensorFlow Lite 2.16.1, CPU + XNNPACK, sin Play Services |
| Verificación del contrato | `ml.verify_tflite`: OK (NHWC, una salida, 4+4 canales, < 15 MB) |
| Latencia en gama baja | *pendiente* (`ml/benchmark/benchmark_tflite.py` + pantalla de diagnóstico; no medida) |

Clases retiradas respecto a v0.2: `tire` (poco relevante en una casa) y `bottle` (las 568 cajas de Silva miden 4–15 px,
vista de dron: inservibles para reconocer una botella real).

## Datos

| Split | Imágenes | `bucket:open` | `bucket:water` | `bucket:inverted` | `water_tank` |
|---|---|---|---|---|---|
| train | 995 | 130 | 81 | 54 | 2052 |
| val | 128 | 10 | 5 | 2 | 337 |
| test | 93 | 16 | 9 | 3 | 164 |

(cajas por clase; 63 imágenes negativas en total: 56 train, 4 val, 3 test). Fuentes:
- **Fotos propias reales** (167 en train): baldes y tinas en el suelo de una casa y alrededores, con un teléfono, más 21 negativos.
  Estado fijado por carpeta al fotografiar. Cajas propuestas por Grounding DINO y revisadas por el asistente (Claude), no por un experto
  (de 203 fotos se descartaron 20). Split por ráfaga. **Todo val y todo test son fotos reales.**
- **Imágenes SINTÉTICAS (generadas con IA, ChatGPT), solo en train:** 93 con caja (24 abiertas, 24 invertidas, 45 con agua) y 38 negativos (cestos, ollas, sartenes, tazones, macetas, jarras, bacinillas), revisadas una a una.
  Se descartaron las tinas anchas del lote de negativos porque contradecían las fotos reales (tinas = balde).
  Son fotorrealistas pero "de catálogo" (objeto centrado y completo); algunas de agua tienen el nivel muy tenue, así que pueden ser etiquetas ruidosas.
  Prompts y hashes en la carpeta de origen (`manifest.csv`, `prompts.json`); no se versionan aquí.
- **Imágenes web** (Openverse, CC0/PDM/BY/BY-SA; atribuciones en `ml/external/web/ATTRIBUTION.csv`): 24 fotos de balde.
- **Silva** (Roboflow Universe, GPL-3.0): solo `water_tank` (vistas de dron y de Brasil).

## Métricas (split de test real, `ml.evaluate`, modelo PyTorch `best.pt`)

| Métrica | v0.4.0 | v0.3.0 (referencia) |
|---|---|---|
| Precision | 0.841 | 0.900 |
| Recall | 0.810 | 0.778 |
| F1 | 0.825 | 0.835 |
| mAP@0.5 | 0.919 | 0.882 |
| mAP@0.5:0.95 | 0.779 | 0.754 |

| Clase | Instancias test | Precision | Recall | F1 | AP@0.5 (v0.3.0) |
|---|---|---|---|---|---|
| bucket:open | 16 | 0.807 | 1.000 | 0.893 | 0.946 (0.968) |
| bucket:water | 9 | 0.841 | 0.592 | 0.695 | 0.871 (0.849) |
| bucket:inverted | 3 | 0.728 | 0.667 | 0.696 | 0.863 (0.715) |
| water_tank | 164 | 0.989 | 0.982 | 0.986 | 0.994 (0.995) |

Cifras del modelo FP32. **No se evaluó el `.tflite` con `ml.evaluate`.** Comparación aparte de los `.tflite` INT8 v0.3 y v0.4 sobre las
**mismas 47 fotos reales de val y test** (clase de mayor confianza por imagen, umbral 0,4, sin letterbox):

| Foto real | Aciertos v0.3 | Aciertos v0.4 |
|---|---|---|
| `bucket:open` (25) | 20 | 24 |
| `bucket:water` (11) | 7 | 6 (4 salen como `open`) |
| `bucket:inverted` (4) | 4 | 4 |
| No son balde (7): falsos positivos | 4 | 4 |

Prueba de humo con `test-nuevo` sintético (40 imágenes; **no mide rendimiento real**, es el mismo dominio que parte del train):
v0.4 acierta 10/10 abiertas, 6/10 con agua, 10/10 invertidas y da 5/10 falsos positivos en negativos (v0.3: 2, 7, 3 y 8/10).

**Lectura honesta:** v0.4 mejora `bucket:open` y poco más. Las debilidades principales (agua poco profunda confundida con abierto y
falsos positivos con otros recipientes) **no mejoraron en fotos reales**. Las diferencias de 1 foto no son significativas.

## Limitaciones conocidas

- **Test muy pequeño:** `bucket:inverted` tiene 3 instancias y `bucket:water` 9, de pocas ráfagas (5 a 8 por clase en total, o sea pocos baldes distintos). Los AP de baldes cambian mucho con una foto. No son evidencia sólida.
- **`bucket:water` se confunde con `bucket:open`:** la mitad de las imágenes con agua del test salen como `open`. Es el error más costoso de la app (un balde con agua no marcado como riesgo). Pasa sobre todo con agua poco profunda y turbia, y con el mismo balde seco con hojas.
- **Falsos positivos:** los 3 negativos de test se detectaron como balde. El modelo todavía no distingue bien baldes de otros recipientes.
- **Poca variedad de baldes:** casi todas las fotos propias son tinas y palanganas anchas, y baldes en suelo de cemento. Poca presencia de cubetas altas con asa, baldes en exteriores, de noche o con otros fondos.
- **`water_tank` viene de vistas de dron (Silva):** un tanque fotografiado de cerca con el teléfono puede comportarse distinto. No se midió.
- Sin latencia ni consumo medidos en un teléfono. El APK no se ha probado en dispositivo.
- Licencia: el dataset de Silva es GPL-3.0; revisar su implicación antes de publicar el modelo.

## Uso previsto y privacidad

Apoyo a la inspección doméstica; no diagnostica dengue ni sustituye criterio humano ni a salud pública.
Inferencia 100 % en el dispositivo; sin INTERNET en el manifiesto.
