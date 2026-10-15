# Guía de demo

El modelo ya viene en el repo (`android/models/detengue_detector_int8.tflite`, v0.5.0).

1. Instala el APK: descárgalo del artefacto `detengue-debug-apk` de [Android CI](https://github.com/hallzyx/detengue/actions/workflows/android.yml)
   o compílalo con `cd android && ./gradlew installDebug`.
2. **Activa el modo avión.** La app debe funcionar igual; en el inicio se lee "Sin conexión · La IA de Detengue sigue disponible".
3. Pulsa **Iniciar revisión** y apunta a un balde con agua → **Analizar**: aparece *Balde · Posible acumulación* (RIESGO).
4. Responde la pregunta de confirmación ("¿tiene agua acumulada?") y lee la recomendación (vaciar y guardar boca abajo o cubierto).
5. Vacía o voltea el balde → **Corregí los elementos — volver a revisar** → *Sin riesgo visible* (SEGURO).
6. **Terminar revisión**: se ve el antes/después, la **racha** y las **insignias** nuevas ("Primera inspección", "Primera alerta atendida").
7. Abre **Privacidad** y **Diagnóstico**: modelo `detengue-0.5.0`, latencia, y `Network: OFF (sin permiso INTERNET)`.

Consejos: usa buena luz y mantén fuera de cuadro cestos y ollas (el modelo todavía los confunde con baldes; ver la model card).
Si quieres que la insignia salte en cámara, borra los datos de la app antes de grabar. La opción **Barrer zona (5 s)** reúne los hallazgos en una galería.

El video completo del demo está enlazado en el [README](../README.md).
