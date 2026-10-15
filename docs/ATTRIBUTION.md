# Atribuciones de datos

Detengue se entrenó con datos de varias fuentes (detalle en [`MODEL_CARD.md`](MODEL_CARD.md)).

## Imágenes web (Openverse)

24 fotos de balde con licencia CC0, dominio público (PDM), CC BY o CC BY-SA, obtenidas con `ml/collect_openverse.py`.
Ese script escribe autor, título, licencia y URL de cada imagen en `ml/external/web/ATTRIBUTION.csv`, que **no se versiona**
(`ml/external/` está en `.gitignore`, igual que las imágenes). Para reproducir la lista: `python -m ml.collect_openverse` y revisar ese CSV.

> **Pendiente:** copiar aquí la tabla completa (autor · título · licencia · URL) desde el CSV generado en el equipo de entrenamiento.
> Las licencias CC BY y CC BY-SA exigen atribución.

## Dataset de tanques de agua

- **Silva** (Roboflow Universe), licencia **GPL-3.0**. Se usa solo la clase `water_tank`.

## Herramientas

- **Ultralytics YOLO11** (AGPL-3.0): entrenamiento y exportación.
- **Grounding DINO**: propuesta de cajas para el etiquetado asistido; las cajas se revisaron una a una.
- **TensorFlow Lite**: inferencia en el dispositivo.
- **Inter** (SIL OFL 1.1): tipografía del video.

## Imágenes propias y sintéticas

- Fotos propias tomadas con un teléfono.
- Imágenes sintéticas generadas con IA, usadas solo en entrenamiento.
