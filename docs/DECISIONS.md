# Decisiones de diseño (desviaciones del PRD)

1. **Un solo detector con clases objeto+estado** (`bucket:water`) en vez de detector + clasificador de estado: una inferencia, menos RAM/latencia. Etiqueta sin estado → REVIEW con pregunta humana.
2. **TFLite 2.16.1 directo** (sin Play Services) para operar totalmente offline. Riesgo: alineación de páginas de 16 KB en Android 15+; evaluar migrar a LiteRT.
3. **Captura bajo demanda** ("Analizar") en vez de análisis continuo: ahorra batería en gama baja.
4. **Fotograma congelado en RAM** mientras se ven resultados (matiz respecto a PRD §36).
5. **UI honesta**: sin modelo no hay detecciones simuladas; DEMO_MODE solo en debug.
6. **Reglas deterministas** con umbrales centralizados en `DetectionConfig`.
7. **Gamificación local derivada del historial** (racha semanal + 4 insignias): sin tablas ni red nuevas. Premia *atender* alertas, no encontrar riesgo. Limitación: responder "No" también cuenta como atendida; es válida para un demo, no como métrica de impacto.
8. **Barrido de zona de 5 s** además de "Analizar": reúne hallazgos en una galería en RAM. Sigue siendo bajo demanda, no inferencia continua.
9. **Export `w8a32` (pesos int8, activaciones float32)** en vez de INT8 estático: el INT8 estático topaba la confianza en ~0,50 y la app no podía dar veredicto (ver el incidente en la model card).
10. **v0.5.0 sobre v0.4.0**: encuentra más agua (8/11 frente a 5/11) a costa de más falsas alarmas. Se priorizó no pasar por alto agua, con la confirmación humana como red de seguridad.
11. **Datos sintéticos solo en train**: val y test son fotos reales, para no inflar las métricas.
