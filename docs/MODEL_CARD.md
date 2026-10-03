# Model card — detengue-detector

**Estado: modelo MVP entrenado (experimental).** Enfocado en baldes (3 estados) y tanques de agua. Los tests de
baldes son muy pequeños (ver Limitaciones). Los campos *pendiente* no se han medido.

| Campo | Valor |
|---|---|
| Versión | `detengue-0.3.0` |
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
| train | 864 | 106 | 36 | 30 | 2052 |
| val | 128 | 10 | 5 | 2 | 337 |
| test | 93 | 16 | 9 | 3 | 164 |

(cajas por clase; incluye 25 imágenes negativas en total). Fuentes:
- **Fotos propias** (167 en train): baldes y tinas en el suelo de una casa y alrededores, fotografiados por el equipo con un teléfono,
  más 21 negativos (objetos que no son balde). Estado fijado por carpeta al fotografiar. Cajas propuestas por Grounding DINO y
  revisadas por el asistente (Claude), no por un experto: de 203 fotos se descartaron 20 por caja o estado erróneos.
  Split **por ráfaga** (marca de hora) para evitar fugas entre train y test.
- **Imágenes web** (Openverse, CC0/PDM/BY/BY-SA; atribuciones en `ml/external/web/ATTRIBUTION.csv`): 24 fotos de balde.
- **Silva** (Roboflow Universe, GPL-3.0): solo `water_tank` (vistas de dron y de Brasil).

## Métricas (split de test, `ml.evaluate`, modelo PyTorch `best.pt`)

| Métrica | Valor |
|---|---|
| Precision | 0.900 |
| Recall | 0.778 |
| F1 | 0.835 |
| mAP@0.5 | 0.882 |
| mAP@0.5:0.95 | 0.754 |

| Clase | Instancias test | Precision | Recall | F1 | AP@0.5 |
|---|---|---|---|---|---|
| bucket:open | 16 | 0.711 | 0.938 | 0.809 | 0.968 |
| bucket:water | 9 | 0.984 | 0.556 | 0.710 | 0.849 |
| bucket:inverted | 3 | 0.905 | 0.667 | 0.768 | 0.715 |
| water_tank | 164 | 1.000 | 0.952 | 0.975 | 0.995 |

Estas cifras son del modelo FP32. **No se evaluó el `.tflite` con `ml.evaluate`.** Comprobación aparte sobre el `.tflite`
INT8, a nivel de imagen (clase de mayor confianza, umbral 0,4, sin letterbox) con las fotos propias de test:

| Real | Predicción del `.tflite` |
|---|---|
| `bucket:inverted` (2 imágenes) | 2 × `bucket:inverted` |
| `bucket:water` (8 imágenes) | 4 × `bucket:water`, **4 × `bucket:open`** |
| `bucket:open` (16 imágenes) | 13 × `bucket:open`, 3 × sin detección |
| Negativos, no son balde (3 imágenes) | **3 de 3 detectadas como balde** (falso positivo) |

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
