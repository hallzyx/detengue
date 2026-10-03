# Detengue

> *10 minutos contra el dengue, asistidos por IA.*

App Android de prevención domiciliaria del dengue. Usa visión artificial **en el teléfono**
para ayudarte a saber qué objetos revisar en tu entorno (recipientes que podrían acumular agua),
**sin Internet**, con teléfonos económicos y sin enviar tus fotos a ningún servidor.

Detengue **no diagnostica dengue** ni reemplaza una inspección sanitaria.

El documento fuente de verdad es [`docs/PRD.md`](docs/PRD.md).

## Estado

| Fase | Contenido | Estado |
|------|-----------|--------|
| 1 | Esqueleto: onboarding, home, permiso de cámara, CameraX, captura de frame | ✅ |
| 2 | Inferencia local (TFLite), boxes alineadas. modelo MVP en Fase 5 | ✅ |
| 3 | Reglas, resultados, recomendaciones, reescaneo, comparación antes/después | ✅ |
| 4 | Room, historial, privacidad, diagnostics, guardas anti-red en CI | ✅ |
| 5 | Pipeline ML, model card, docs; modelo MVP entrenado | ✅ |

## Compilar

Requisitos: JDK 17+ y Android SDK (compileSdk 35).

```bash
cd android
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # tests unitarios
```

El workflow [`Android CI`](.github/workflows/android.yml) compila, ejecuta tests y lint en cada push
y publica el APK de debug como artefacto.

## Principios

- **Offline-first**: el manifiesto **no declara el permiso `INTERNET`**; la app no puede abrir conexiones.
- **Privacy-first**: el frame de la cámara vive solo en RAM durante el análisis; no se guarda.
- **Low-end-first**: `minSdk 26`, análisis bajo demanda (botón *Analizar*), sin inferencia a 30 FPS.
- **Human-in-the-loop**: todo resultado ambiguo termina en *Revisar manualmente*.

## Estructura

```
android/   App Android (Kotlin, Jetpack Compose, CameraX)
ml/        Dataset, entrenamiento, evaluación y export del detector (Fase 5)
docs/      PRD, model card, privacidad, guion de demo
```

## Limitaciones honestas
- Hay un modelo MVP (`detengue-0.3.0`, INT8, 3 MB) enfocado en baldes (`open`/`water`/`inverted`) y tanques de agua, entrenado con
  fotos propias, web y el dataset público de Silva. Con pocos baldes distintos: confunde a menudo balde con agua y balde abierto, y da
  falsos positivos con otros recipientes (ver `docs/MODEL_CARD.md`).
- El APK compila y pasa tests y lint en GitHub Actions, pero **no se ha probado en un teléfono**: latencia y precisión reales sin medir.
- Ver `docs/DECISIONS.md`, `docs/MODEL_CARD.md`, `docs/PRIVACY.md`, `docs/DEMO.md`.
