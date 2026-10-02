# Guía de demo

1. Coloca un modelo real en `android/models/detengue_detector_int8.tflite` (ver `ml/README.md`).
2. `cd android && ./gradlew installDebug`.
3. Activa modo avión: la app debe funcionar igual.
4. Escanea un balde con agua → RIESGO; vacíalo/vuélcalo y reescanea → SEGURO.
5. Sin modelo, la app muestra el error del PRD y deshabilita "Analizar" (no simula detecciones).
