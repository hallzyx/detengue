# Model card — detengue-detector

**Estado: modelo MVP entrenado (experimental).** Enfocado en baldes (3 estados) y tanques de agua. Los tests de
baldes son muy pequeños y la tasa de falsas alarmas es alta (ver Limitaciones). Los campos *pendiente* no se han medido.

| Campo | Valor |
|---|---|
| Versión | `detengue-0.5.0` |
| Arquitectura | YOLO11n (Ultralytics), un solo detector, ~2,6 M parámetros |
| Entrenamiento | 30 épocas, batch 16, imgsz 320, GPU RTX 3050 (WSL2); partida desde pesos preentrenados |
| Exportación | litert-torch (Ultralytics 8.4.172), **pesos int8 + activaciones float32** (`w8a32`), 3,0 MB; entrada permutada a NHWC (`ml/export_tflite.py`). El archivo se llama `..._int8.tflite` solo por compatibilidad con la app |
| Entrada | 320×320 RGB, NHWC `[1,320,320,3]`, float32 0..1, letterbox gris 114 |
| Salida | `[1,8,2100]` = 4 coordenadas (normalizadas 0..1) + 4 clases, sin NMS embebido |
| Clases | `bucket:open`, `bucket:water`, `bucket:inverted`, `water_tank` |
| Runtime | TensorFlow Lite 2.16.1, CPU + XNNPACK, sin Play Services. Probado a mano en un teléfono (ver Limitaciones) |
| Verificación del contrato | `ml.verify_tflite`: OK (NHWC, una salida, 4+4 canales, < 15 MB) |
| Latencia en gama baja | *pendiente* (`ml/benchmark/benchmark_tflite.py` + pantalla de diagnóstico; no medida) |

## Incidente: el INT8 estático recortaba la confianza (versiones 0.2.0–0.4.0)

Con INT8 estático (pesos + activaciones int8) los scores de clase del `.tflite` quedaban **topados en ~0,50**
(máx. medido 0,59 en v0.3 y 0,4998 en v0.4), mientras que el mismo modelo en FP32 daba 0,53–0,95 (mediana 0,82). La app muestra
"no determinado" por debajo de 0,50 y "riesgo/seguro" desde 0,70, así que **no podía dar un veredicto**: lo notó una persona probando el APK.
Con v0.5.0 el export es `w8a32` (activaciones float32) y la confianza mediana es 0,84. Las métricas de `ml.evaluate` (FP32) no detectan este
problema; por eso existe `ml.simulate_app`, que reproduce letterbox, umbrales y veredicto de la app sobre el `.tflite`.

## Datos

| Split | Imágenes | `bucket:open` | `bucket:water` | `bucket:inverted` | `water_tank` |
|---|---|---|---|---|---|
| train | 1126 | 154 | 126 | 78 | 2052 |
| val | 128 | 10 | 5 | 2 | 337 |
| test | 93 | 16 | 9 | 3 | 164 |

(cajas por clase; train incluye 94 negativas, val 4 y test 3). Fuentes:
- **Fotos propias reales** (167 en train): baldes y tinas en el suelo de una casa y alrededores, con un teléfono, más 21 negativos.
  Estado fijado por carpeta al fotografiar. Cajas propuestas por Grounding DINO y revisadas por el asistente (Claude), no por un experto
  (de 203 fotos se descartaron 20). Split por ráfaga. **Todo val y todo test son fotos reales.**
- **Imágenes SINTÉTICAS (generadas con IA, ChatGPT), solo en train:** 93 con caja (24 abiertas, 24 invertidas, 45 con agua) y 38 negativos
  (cestos, ollas, sartenes, tazones, macetas, jarras, bacinillas), revisadas una a una. Se descartaron las tinas anchas del lote de negativos
  porque contradecían las fotos reales (tinas = balde). Son "de catálogo" (objeto centrado y completo) y algunas de agua tienen el nivel muy
  tenue, así que pueden ser etiquetas ruidosas. Prompts y hashes en la carpeta de origen; no se versionan aquí.
- **Copias degradadas de las sintéticas, solo en train** (131, `ml/degrade.py`): desenfoque, ruido, JPEG fuerte, poca luz y contraluz
  aplicados por código, con las mismas cajas. Simulan una cámara de baja calidad.
- **Imágenes web** (Openverse, CC0/PDM/BY/BY-SA; atribuciones en `ml/external/web/ATTRIBUTION.csv`): 24 fotos de balde.
- **Silva** (Roboflow Universe, GPL-3.0): solo `water_tank` (vistas de dron y de Brasil).

Clases retiradas desde v0.2: `tire` (poco relevante en una casa) y `bottle` (las 568 cajas de Silva miden 4–15 px, vista de dron).

## Métricas (split de test real, `ml.evaluate`, modelo PyTorch FP32)

| Métrica | v0.5.0 | v0.4.0 (referencia) |
|---|---|---|
| Precision | 0.917 | 0.841 |
| Recall | 0.853 | 0.810 |
| F1 | 0.884 | 0.825 |
| mAP@0.5 | 0.907 | 0.919 |
| mAP@0.5:0.95 | 0.754 | 0.779 |

| Clase | Instancias test | Precision | Recall | F1 | AP@0.5 (v0.4.0) |
|---|---|---|---|---|---|
| bucket:open | 16 | 0.868 | 1.000 | 0.929 | 0.985 (0.946) |
| bucket:water | 9 | 1.000 | 0.775 | 0.873 | 0.926 (0.871) |
| bucket:inverted | 3 | 0.807 | 0.667 | 0.730 | 0.731 (0.863) |
| water_tank | 164 | 0.994 | 0.970 | 0.981 | 0.986 (0.994) |

## Comportamiento de la app con el `.tflite` (`ml.simulate_app`, 47 fotos reales de val y test)

Letterbox y umbrales de la app (detección 0,40; incierto 0,50; riesgo/seguro 0,70). Esto es lo que vería una persona.

| Foto real | v0.4.0 INT8 estático (APK anterior) | v0.4.0 `w8a32` | **v0.5.0 `w8a32`** |
|---|---|---|---|
| `water` (11) | 10 no determinado, 1 sin detección | 5 RIESGO, 4 revisar, 1 sin det., 1 no det. | **8 RIESGO**, 3 revisar |
| `open` (25) | 25 no determinado | 25 revisar | 21 revisar, **2 RIESGO falso**, 2 no det. |
| `inverted` (4) | 4 no determinado | 4 SEGURO | 4 SEGURO |
| No son balde (7) | 3 no det., 4 sin detección | 1 RIESGO falso, 2 revisar, 4 sin det. | 4 **RIESGO falso**, 3 no det. |

**Por qué v0.5.0 y no v0.4.0:** encuentra más agua (8 de 11 frente a 5), a costa de más falsas alarmas (6 de 32 baldes secos y no-baldes
frente a 1). Se eligió priorizar no pasar por alto agua (error más costoso en prevención), con "revisar" humano como red de seguridad.
Es una decisión de MVP, reversible. Con 47 fotos, diferencias de 1–3 fotos no son concluyentes.

## Experimentos descartados (no están en la app)

Mismas fotos reales (47) y reglas de la app (`ml.simulate_app`). Solo cambia lo indicado respecto a v0.5.0.

| Variante | `water` → RIESGO (11) | Seco o no-balde → RIESGO falso (32) | Decisión |
|---|---|---|---|
| v0.4.0 `w8a32` (sin copias degradadas) | 5 | 1 | descartada: encuentra poca agua |
| **v0.5.0 `w8a32` (en la app)** | **8** | 6 | elegida |
| v0.6 = v0.5 + 133 sintéticas nuevas (V3: cámara barata, negativos con agua/tierra) | 6 | 6 | descartada: pierde agua y no reduce las falsas alarmas |

V3 mejora el test sintético (todo 10/10 y menos falsos positivos) pero no el real: **los datos sintéticos parecen haber tocado techo**.
Subir el umbral de riesgo tampoco ayuda: en v0.5.0 las falsas alarmas (confianza 0,71–0,89) se solapan con los aciertos (0,77–0,94).
Lo que falta son fotos reales de agua poco profunda y de objetos que no son balde.

## Limitaciones conocidas

- **Falsas alarmas:** 4 de 7 objetos que no son balde (cestos de ropa, ollas) se marcan como RIESGO con confianza alta, y 2 baldes secos también.
- **Agua poco profunda:** 3 de 11 fotos con agua siguen sin alerta de riesgo.
- **Test muy pequeño:** `bucket:inverted` tiene 3 instancias y `bucket:water` 9, de pocas ráfagas (pocos baldes distintos). Sus AP no son evidencia sólida.
- **Poca variedad de baldes:** casi todas las fotos propias son tinas y palanganas anchas, y baldes en suelo de cemento. Poca presencia de cubetas
  altas con asa en exteriores reales, de noche o con otros fondos. Gran parte de lo demás es sintético.
- **`water_tank` viene de vistas de dron (Silva):** un tanque fotografiado de cerca puede comportarse distinto. No se midió.
- **Prueba en dispositivo:** una persona instaló la APK v0.5.0 en su teléfono (modelo no indicado), el modelo cargó y analizó, y dio veredictos
  de revisión y de peligro "dentro de lo que cabe". Es una prueba informal: **sin tabla de aciertos, sin latencia ni consumo medidos**.
- Licencia: el dataset de Silva es GPL-3.0; revisar su implicación antes de publicar el modelo.

## Uso previsto y privacidad

Apoyo a la inspección doméstica; no diagnostica dengue ni sustituye criterio humano ni a salud pública.
Inferencia 100 % en el dispositivo; sin INTERNET en el manifiesto.
