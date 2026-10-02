# Decisiones de diseño (desviaciones del PRD)

1. **Un solo detector con clases objeto+estado** (`bucket:water`) en vez de detector + clasificador de estado: una inferencia, menos RAM/latencia. Etiqueta sin estado → REVIEW con pregunta humana.
2. **TFLite 2.16.1 directo** (sin Play Services) para operar totalmente offline. Riesgo: alineación de páginas de 16 KB en Android 15+; evaluar migrar a LiteRT.
3. **Captura bajo demanda** ("Analizar") en vez de análisis continuo: ahorra batería en gama baja.
4. **Fotograma congelado en RAM** mientras se ven resultados (matiz respecto a PRD §36).
5. **UI honesta**: sin modelo no hay detecciones simuladas; DEMO_MODE solo en debug.
6. **Reglas deterministas** con umbrales centralizados en `DetectionConfig`.
