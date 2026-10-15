# PRD — Detengue

> Documento original de requisitos. El estado real y las desviaciones están en el [README](../README.md) y en [`DECISIONS.md`](DECISIONS.md).

**Versión:** 1.0  
**Fecha:** 02 de octubre de 2026  
**Producto:** Detengue  
**Tagline de trabajo:** *“10 minutos contra el dengue, asistidos por IA.”*  
**Contexto:** Proyecto para #AIforImpact2026  
**Plataforma MVP:** Android  
**Prioridad:** Demo funcional, offline-first, low-end-first, privacy-first

---

# 0. Instrucciones para agentes de código

Este documento es la fuente de verdad del MVP.

Al implementar:

1. **No conviertan Detengue en un chatbot.**
2. **No hagan depender el escaneo de Internet ni de una API externa.**
3. **No suban imágenes a la nube por defecto.**
4. **No diagnostiquen dengue ni afirmen detectar mosquitos.**
5. **No inventen un “riesgo epidemiológico” clínico.**
6. **Prioricen el loop de escaneo → acción → reescaneo antes que login, dashboard, mapas o backend.**
7. **Si una feature pone en riesgo la demo principal, se posterga.**
8. La app debe poder demostrar su función principal con el teléfono en **modo avión**.
9. La UI debe estar pensada para personas no técnicas y para Android de gama baja.
10. Todo resultado ambiguo debe poder terminar en **“Revisar manualmente”** en vez de forzar una conclusión.

La implementación debe optimizar primero:

**confiabilidad de demo > funcionamiento offline > claridad UX > rendimiento > features adicionales.**

---

# 1. Resumen ejecutivo

Detengue es una aplicación Android de prevención domiciliaria del dengue que utiliza visión artificial ejecutada localmente en el teléfono para ayudar a una persona a identificar objetos y condiciones visuales que **podrían convertirse en criaderos del mosquito transmisor del dengue**.

La aplicación no intenta diagnosticar dengue, identificar personas infectadas ni reemplazar a una brigada sanitaria.

Su función es mucho más concreta:

> **Ayudar al ciudadano a saber qué revisar dentro de su entorno, incluso sin Internet y usando un teléfono económico.**

La experiencia principal consiste en:

1. El usuario abre la cámara.
2. Detengue analiza la escena en el dispositivo.
3. La IA localiza recipientes u objetos relevantes.
4. La aplicación señala cuáles requieren revisión.
5. El usuario corrige físicamente el problema.
6. Realiza un segundo escaneo.
7. Detengue verifica visualmente que la condición haya cambiado.
8. La inspección se registra localmente.

El valor diferencial no es solamente “usar IA contra el dengue”.

El diferencial es:

> **hacer que la autoinspección domiciliaria sea asistida por visión artificial, accesible, privada y funcional sin conectividad permanente.**

---

# 2. Problema

La prevención domiciliaria del dengue depende de que las personas reconozcan y eliminen condiciones donde puede acumularse agua.

El problema práctico es que una instrucción como:

> “Revisa tu vivienda y elimina posibles criaderos”

requiere que el ciudadano:

- sepa qué objetos revisar;
- recuerde todos los tipos de recipientes relevantes;
- interprete correctamente el estado de cada objeto;
- no pase por alto elementos cotidianos;
- tenga acceso a material educativo;
- y, en muchas soluciones digitales, disponga de conectividad.

Detengue plantea una capa de asistencia visual:

> **el usuario no tiene que saber de antemano todo lo que debe buscar; la cámara lo ayuda a encontrarlo.**

---

# 3. Hipótesis principal

Si un teléfono económico puede detectar localmente recipientes y condiciones visuales relevantes para una autoinspección domiciliaria, entonces podemos reducir la fricción de la prevención sin exigir:

- conexión permanente;
- un teléfono de gama alta;
- pago por inferencia;
- conocimiento técnico;
- o envío de imágenes privadas a servidores.

---

# 4. Propuesta de valor

## Para ciudadanos

“Apunta tu cámara y Detengue te ayuda a saber qué revisar.”

Beneficios:

- funciona sin Internet después de la instalación;
- no requiere pagar por análisis;
- da instrucciones concretas;
- no necesita conocimiento previo;
- procesa la imagen localmente;
- permite verificar una corrección en el momento.

## Para brigadas o instituciones — fase posterior

Detengue podría permitir:

- estandarizar autoinspecciones;
- registrar inspecciones agregadas;
- sincronizar datos cuando exista conectividad;
- identificar zonas con alta cantidad de elementos reportados;
- complementar campañas existentes.

**Esto NO forma parte del núcleo obligatorio del MVP.**

---

# 5. Principios del producto

## 5.1 Offline-first

El escaneo, detección, clasificación básica, recomendaciones y registro local deben funcionar sin Internet.

## 5.2 Low-end-first

El producto debe diseñarse pensando primero en teléfonos Android modestos, no en dispositivos premium.

## 5.3 Privacy-first

Las imágenes de la vivienda se procesan en el dispositivo.

Por defecto:

- no se suben;
- no se almacenan permanentemente;
- no se usan para entrenamiento.

Guardar o compartir imágenes deberá requerir consentimiento explícito en una fase futura.

## 5.4 Human-in-the-loop

La IA ayuda a encontrar.

No debe fingir certeza cuando no la tiene.

Estados válidos:

- seguro visualmente;
- revisar;
- condición visual de riesgo;
- incierto.

## 5.5 IA como herramienta de acción

El resultado no debe terminar en “detectamos un balde”.

Debe terminar en una acción:

> “Revisa si contiene agua. Vacíalo, cepíllalo o mantenlo cubierto.”

---

# 6. Alcance del MVP

El MVP debe demostrar solamente este ciclo:

```text
Abrir app
   ↓
Escanear escena
   ↓
Detectar elementos relevantes
   ↓
Mostrar qué revisar
   ↓
Usuario corrige físicamente
   ↓
Reescanear
   ↓
Mostrar mejora
   ↓
Guardar resumen local
```

Si este flujo funciona bien, el MVP es exitoso.

Todo lo demás es secundario.

---

# 7. No objetivos del MVP

No construir todavía:

- diagnóstico médico;
- detección de infección por dengue;
- reconocimiento de mosquitos;
- mapas epidemiológicos nacionales;
- predicción de brotes;
- sistema completo de brigadas;
- login obligatorio;
- pagos;
- chatbot generativo;
- LLM remoto para cada escaneo;
- reconocimiento de personas;
- red social;
- notificaciones complejas;
- gamificación avanzada;
- panel gubernamental completo;
- multi-país;
- iOS.

---

# 8. Usuario principal

## Persona A — Ciudadano

Características:

- Android;
- puede tener conectividad limitada;
- no tiene conocimientos de epidemiología;
- necesita instrucciones simples;
- puede compartir el teléfono con su familia;
- no quiere configurar nada complejo.

Objetivo:

> revisar rápidamente una zona de su vivienda y saber qué objetos requieren atención.

---

# 9. Usuario secundario

## Persona B — Brigadista / agente comunitario

No es prioridad del MVP.

En una fase posterior podría usar el mismo motor para:

- inspecciones guiadas;
- evidencia estructurada;
- conteo de elementos;
- sincronización;
- resumen por zona.

---

# 10. Jobs to be Done

### JTBD-01

Cuando estoy revisando mi vivienda, quiero que el celular me indique qué objetos podrían necesitar revisión para no depender únicamente de recordar una lista.

### JTBD-02

Cuando corrijo un objeto, quiero volver a escanearlo y verificar visualmente que ya no esté marcado como prioridad.

### JTBD-03

Cuando no tengo Internet, quiero que la aplicación siga funcionando.

### JTBD-04

Cuando la IA no esté segura, quiero que lo indique en lugar de inventar una conclusión.

---

# 11. Experiencia principal

## Pantalla 1 — Inicio

Contenido:

- logo Detengue;
- tagline;
- CTA grande: **“Iniciar revisión”**;
- acceso a historial;
- indicador pequeño:

```text
IA disponible sin conexión ✓
```

No mostrar terminología técnica como “modelo INT8”, “TFLite”, “NPU”, etc.

---

# 12. Onboarding

Máximo 3 pantallas la primera vez.

## Onboarding 1

**Título:** Revisa tu entorno con ayuda de IA

Texto:

“Detengue identifica objetos que podrían necesitar revisión para evitar acumulaciones de agua.”

## Onboarding 2

**Título:** Funciona sin Internet

Texto:

“El análisis principal ocurre directamente en tu teléfono.”

## Onboarding 3

**Título:** Es una herramienta preventiva

Texto:

“Detengue no diagnostica dengue ni reemplaza una inspección sanitaria.”

CTA:

**Empezar**

---

# 13. Permisos

Requerido:

- cámara.

Opcional / NO requerido para MVP:

- ubicación;
- almacenamiento;
- micrófono;
- contactos.

La app debe poder utilizarse sin conceder ubicación.

---

# 14. Flujo de escaneo

## 14.1 UX recomendada para MVP

No intentar 30 FPS de inferencia.

Usar:

**modo cámara + botón “Analizar”**

Proceso:

1. cámara muestra preview fluido;
2. usuario encuadra;
3. pulsa “Analizar”;
4. se captura un frame;
5. inferencia local;
6. se dibujan bounding boxes;
7. resultado aparece en menos de ~1 segundo idealmente.

Esto reduce consumo, mejora estabilidad y hace viable el funcionamiento en teléfonos modestos.

---

# 15. Objetos objetivo

Primera versión del detector:

1. balde;
2. recipiente / tina;
3. maceta o plato de maceta;
4. botella / envase;
5. llanta;
6. bebedero de mascota;
7. cilindro / depósito;
8. tanque o contenedor de agua.

Para el primer modelo funcional se permite empezar con solo:

- balde;
- recipiente;
- maceta;
- botella.

La demo puede usar un subconjunto si el modelo aún no tiene calidad suficiente en las ocho clases.

---

# 16. Estados visuales

Detengue debe separar:

**“qué objeto es”**

de:

**“qué tan prioritario es revisarlo”.**

Estados:

```text
SAFE
REVIEW
RISK
UNCERTAIN
```

## SAFE

Ejemplo:

- recipiente correctamente invertido;
- tanque visualmente cerrado;
- botella cerrada.

UI:

```text
✓ Sin riesgo visible
```

## REVIEW

El objeto puede acumular agua, pero la imagen no permite confirmar una condición problemática.

UI:

```text
⚠ Revisar este recipiente
```

## RISK

Solo utilizar cuando exista evidencia visual suficientemente clara.

Ejemplo:

- recipiente abierto con agua visible;
- acumulación claramente visible.

UI:

```text
! Posible acumulación de agua
```

## UNCERTAIN

La IA no tiene confianza suficiente.

UI:

```text
? No pude determinarlo
Revisa manualmente
```

---

# 17. Regla crítica de seguridad

Nunca mostrar:

- “Este objeto tiene dengue.”
- “Hay dengue aquí.”
- “Este es un criadero confirmado.”
- “Tu casa tiene 80% de riesgo de dengue.”
- “No hay riesgo de dengue.”

Usar:

- “posible acumulación de agua”;
- “objeto a revisar”;
- “condición visual”;
- “prioridad de revisión”;
- “no se observa una condición de riesgo en esta imagen”.

---

# 18. Detección de agua

Detectar agua visible puede ser técnicamente difícil por:

- reflejos;
- transparencia;
- iluminación;
- suciedad;
- recipientes opacos.

Por eso el MVP debe aceptar esta estrategia:

```text
Detector de objeto
      ↓
Clasificador de estado
      ↓
¿Alta confianza?
  ├── sí → RISK / SAFE
  └── no → REVIEW / UNCERTAIN
```

Para un recipiente detectado sin evidencia suficiente:

> “Revisa si contiene agua.”

Esto es preferible a una falsa certeza.

---

# 19. Interacción humana opcional

Para aumentar confiabilidad:

Cuando un objeto está en REVIEW:

```text
Balde detectado

¿Tiene agua acumulada?

[ Sí ] [ No ] [ No estoy seguro ]
```

Si el usuario selecciona “Sí”:

```text
Acción sugerida:
Vaciar, limpiar y mantener cubierto o invertido.
```

Esto convierte Detengue en un sistema IA + humano, no en un clasificador que pretende tener visión perfecta.

---

# 20. Acciones recomendadas

Las recomendaciones deben ser determinísticas y locales.

No usar LLM.

Ejemplos:

## Recipiente abierto

“Vacía el agua y guarda el recipiente boca abajo o cubierto.”

## Plato de maceta

“Elimina el agua acumulada y revisa periódicamente.”

## Bebedero

“Renueva el agua y limpia el recipiente.”

## Tanque / depósito

“Mantenlo correctamente cubierto.”

## Llanta

“Evita que acumule agua y mantenla protegida de la lluvia.”

---

# 21. Reescaneo

Después de mostrar recomendaciones:

CTA:

**“Corregí los elementos — volver a revisar”**

La app vuelve al scanner.

Después del segundo análisis se compara:

```text
ANTES
3 elementos por revisar

AHORA
1 elemento por revisar
```

Si todos desaparecen:

```text
✓ Revisión completada

No se observan elementos pendientes
en este escaneo.
```

No afirmar:

“Tu casa está libre de dengue”.

---

# 22. Wow moment de la demo

La demo oficial debe poder hacerse así:

## Escena

Mesa o ambiente controlado con:

- un balde abierto;
- un recipiente con agua visible;
- una botella cerrada;
- una maceta;
- un recipiente invertido.

## Secuencia

1. Activar modo avión.
2. Abrir Detengue.
3. Mostrar:

```text
Sin conexión
IA local disponible ✓
```

4. Apuntar a la escena.
5. Pulsar **Analizar**.
6. Bounding boxes aparecen.
7. Detengue marca 2–3 elementos a revisar.
8. Se vacía o invierte uno físicamente.
9. Pulsar **Volver a revisar**.
10. El número de condiciones detectadas disminuye.

Objetivo:

**el wow moment debe ocurrir en menos de 30–40 segundos.**

---

# 23. Arquitectura técnica recomendada

## Cliente

**Android nativo**

- Kotlin
- Jetpack Compose
- CameraX
- Room
- LiteRT / TensorFlow Lite
- Coroutines
- StateFlow
- Material 3

Razón:

máximo control sobre:

- cámara;
- memoria;
- CPU;
- cuantización;
- inferencia offline;
- rendimiento en low-end.

---

# 24. Arquitectura de alto nivel

```text
┌─────────────────────────┐
│       Android UI        │
│    Jetpack Compose      │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│      Scan Controller    │
│        CameraX          │
└────────────┬────────────┘
             │ Bitmap / YUV
             ▼
┌─────────────────────────┐
│   Preprocessing Layer   │
│  resize / normalize     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│     On-device Model     │
│       INT8 TFLite       │
└────────────┬────────────┘
             │ detections
             ▼
┌─────────────────────────┐
│     Risk Rule Engine    │
│ deterministic / local   │
└────────────┬────────────┘
             │
       ┌─────┴─────┐
       ▼           ▼
     UI          Room DB
```

Ninguna de estas operaciones requiere Internet.

---

# 25. Modelo de IA

## Primera opción

Detector nano entrenado específicamente para Detengue.

Posibles familias:

- YOLO nano;
- MobileNet SSD;
- EfficientDet Lite.

La decisión final debe tomarse por benchmark real, no por popularidad.

### Requisito de exportación

Debe terminar como:

```text
detengue_detector_int8.tflite
```

Preferentemente:

- INT8;
- input 320×320;
- batch 1.

---

# 26. Pipeline de IA

```text
Frame de cámara
     ↓
Resize 320x320
     ↓
Modelo detector
     ↓
NMS
     ↓
Bounding boxes
     ↓
Confianza
     ↓
Clasificación de condición
     ↓
Risk Rule Engine
     ↓
UI
```

Si el clasificador de estado todavía no está listo:

```text
Detector
   ↓
Objeto potencial
   ↓
REVIEW
   ↓
Pregunta breve al usuario
```

Esto permite lanzar un MVP útil antes de tener una visión perfecta de “agua/no agua”.

---

# 27. Targets de rendimiento

Estos son **objetivos**, no números que deban fingirse si no se alcanzan.

## Dispositivo objetivo

Android modesto:

- 3–4 GB RAM;
- CPU ARM de gama de entrada/media;
- sin GPU dedicada;
- sin NPU obligatoria.

## Objetivos

- modelo: idealmente < 15 MB;
- app total: idealmente < 60 MB;
- peak RAM durante análisis: < 250 MB;
- inferencia objetivo p50: < 700 ms;
- p95: < 1.5 s;
- cero llamadas de red durante análisis;
- funcionamiento completo en modo avión.

Registrar métricas reales en el dispositivo.

---

# 28. Benchmark obligatorio

Crear una pantalla/debug overlay:

```text
Detengue Diagnostics

Model: detengue_int8_v1
Input: 320x320
Inference: 482 ms
RAM: 176 MB
Network: OFF
Device: XXXXX
```

Esta pantalla no es para usuario final.

Sirve para:

- benchmark;
- demo técnica;
- evidencia de viabilidad.

---

# 29. Almacenamiento local

Room.

Entidades mínimas:

## Inspection

```kotlin
id: UUID
startedAt: Instant
completedAt: Instant?
initialReviewCount: Int
finalReviewCount: Int?
status: IN_PROGRESS | COMPLETED
modelVersion: String
```

## Detection

```kotlin
id: UUID
inspectionId: UUID
objectClass: String
confidence: Float
condition: SAFE | REVIEW | RISK | UNCERTAIN
actionCode: String?
createdAt: Instant
```

No almacenar la imagen original por defecto.

---

# 30. Contrato interno de detección

```json
{
  "class": "bucket",
  "confidence": 0.91,
  "bbox": {
    "x": 0.21,
    "y": 0.35,
    "width": 0.34,
    "height": 0.42
  },
  "condition": "REVIEW",
  "conditionConfidence": 0.68,
  "actionCode": "CHECK_STANDING_WATER"
}
```

Las coordenadas deben estar normalizadas 0..1.

---

# 31. Motor de reglas

El motor de reglas no debe depender de un LLM.

Ejemplo:

```text
bucket + water_visible high confidence
→ RISK
→ EMPTY_AND_INVERT

bucket + state unknown
→ REVIEW
→ CHECK_STANDING_WATER

bottle + closed
→ SAFE

water_tank + uncovered
→ RISK

low confidence
→ UNCERTAIN
```

Implementar reglas en una estructura testeable y separada de UI.

---

# 32. Diseño visual de detecciones

Color sugerido por semántica, no por marca:

- verde → SAFE;
- ámbar → REVIEW;
- rojo → RISK;
- gris → UNCERTAIN.

Cada bounding box debe mostrar:

```text
Balde
Revisar
```

No mostrar porcentajes de confianza al ciudadano.

---

# 33. Pantalla de resultados

Ejemplo:

```text
Encontramos 3 elementos para revisar

1. Balde abierto
   Revisar si contiene agua
   [Ver recomendación]

2. Plato de maceta
   Posible acumulación
   [Ver recomendación]

3. Botella cerrada
   ✓ Sin riesgo visible
```

CTA:

**Corregir y volver a revisar**

---

# 34. Resultado comparativo

Después del segundo scan:

```text
Tu revisión

Antes      Ahora
  3    →     1

✓ 2 elementos corregidos
```

Mensaje:

“Buen trabajo. Revisa manualmente cualquier elemento que la cámara no haya podido identificar.”

---

# 35. Historial

Pantalla simple:

```text
Mis revisiones

Hoy, 16:24
3 → 1 pendientes

Hoy, 10:10
2 → 0 pendientes
```

Todo local.

No cloud obligatorio.

---

# 36. Privacidad

Pantalla de privacidad:

> “Las imágenes utilizadas durante el análisis se procesan en tu dispositivo. Detengue no necesita enviar tus fotografías a Internet para identificar elementos.”

Requisito:

- eliminar frame temporal al finalizar inferencia;
- no persistir JPEG;
- no loggear bytes de imágenes;
- no enviar analytics con fotografías.

---

# 37. Conectividad

El MVP debe funcionar con:

```text
WiFi OFF
Mobile Data OFF
Airplane Mode ON
```

Internet solamente puede usarse en fase posterior para:

- actualizar modelo;
- sincronizar métricas anónimas;
- panel institucional.

La pérdida de conexión no puede romper el scanner.

---

# 38. Backend futuro — NO bloqueante

Si se implementa después:

**FastAPI + PostgreSQL**

Endpoints conceptuales:

```text
POST /v1/inspections/sync
GET  /v1/model/manifest
GET  /v1/app/config
```

Nunca enviar imágenes por defecto.

Payload de sincronización:

```json
{
  "inspectionId": "uuid",
  "timestamp": "2026-10-02T16:00:00-05:00",
  "initialReviewCount": 3,
  "finalReviewCount": 1,
  "modelVersion": "detengue-0.1.0"
}
```

Ubicación únicamente con consentimiento explícito.

---

# 39. Dashboard futuro

NO construir antes de terminar el loop principal.

Eventualmente:

```text
Inspecciones sincronizadas: 428
Elementos detectados: 973
Correcciones reportadas: 615

Zonas:
██████ alta cantidad de revisiones
████   media
██     baja
```

No presentar esto como mapa epidemiológico.

Es un mapa de actividad / elementos reportados.

---

# 40. Dataset

Crear carpeta:

```text
/ml
  /dataset
  /training
  /evaluation
  /export
```

Estructura:

```text
dataset/
  images/
    train/
    val/
    test/
  labels/
    train/
    val/
    test/
```

---

# 41. Estrategia inicial de datos

Para el hackathon:

1. crear escenas reales con objetos cotidianos;
2. variar iluminación;
3. variar fondos;
4. variar ángulo;
5. variar distancia;
6. incluir objetos parcialmente ocultos;
7. incluir negativos difíciles.

El test set debe contener imágenes que no hayan sido utilizadas durante entrenamiento.

---

# 42. Datos negativos

Muy importantes.

Ejemplos:

- tazas;
- ollas;
- mochilas;
- cajas;
- mesas;
- sillas;
- recipientes cerrados;
- objetos cilíndricos no relevantes;
- sombras;
- reflejos.

El modelo no debe marcar cualquier objeto redondo como recipiente de riesgo.

---

# 43. Evaluación del modelo

Reportar:

- precision;
- recall;
- F1;
- mAP cuando aplique;
- matriz de confusión;
- latencia.

No ocultar resultados malos.

Para la demo es preferible reducir las clases y tener un detector confiable antes que soportar 20 categorías mediocremente.

---

# 44. Confidence thresholds

Configurables desde código:

```text
DETECTION_THRESHOLD
RISK_THRESHOLD
UNCERTAIN_THRESHOLD
NMS_THRESHOLD
```

No hardcodear valores por toda la app.

Centralizar configuración.

---

# 45. Modo demo

Puede existir un modo:

```text
BuildConfig.DEMO_MODE
```

Pero nunca debe falsificar detecciones.

Puede:

- mostrar métricas;
- bloquear orientación;
- ocultar pantallas no relevantes;
- precargar textos;
- activar debug overlay.

No puede:

- insertar bounding boxes falsas;
- sustituir el modelo por resultados pregrabados sin indicarlo;
- afirmar análisis real cuando sea mock.

---

# 46. Estados de error

## Cámara no disponible

“Detengue necesita acceso a la cámara para realizar la revisión.”

## Modelo no cargó

“No pudimos iniciar la IA local. Reinicia la aplicación.”

## Ninguna detección

“No identificamos elementos en esta imagen. Revisa manualmente el entorno o intenta otro ángulo.”

## Baja confianza

“No pude determinar este objeto con suficiente confianza.”

---

# 47. Accesibilidad

MVP:

- textos grandes;
- contraste alto;
- botones grandes;
- lenguaje simple;
- evitar párrafos largos;
- iconografía acompañada de texto;
- soporte TalkBack donde sea viable.

Futuro:

- instrucciones por voz;
- Quechua;
- otros idiomas locales.

---

# 48. Diseño de marca

Nombre:

# Detengue

Concepto:

**DE + TENGUE / DETÉN + DENGUE**

No hace falta explicar el juego de palabras dentro de la app.

Tono:

- preventivo;
- claro;
- amable;
- no alarmista.

Evitar estética de emergencia médica.

---

# 49. Copy principal

## Home

**Detengue**

“Revisa tu entorno con ayuda de IA.”

CTA:

**Iniciar revisión**

Secondary:

“Funciona sin conexión.”

---

# 50. Copy durante análisis

```text
Analizando en tu dispositivo…
```

No:

```text
Enviando a la IA…
```

---

# 51. Copy después de detección

```text
Encontramos elementos que vale la pena revisar.
```

No:

```text
Encontramos criaderos de dengue.
```

---

# 52. Copy offline

```text
Sin conexión
La IA de Detengue sigue disponible.
```

Este mensaje es importante para la demo.

---

# 53. Métrica principal de producto

Para el MVP:

> **Porcentaje de inspecciones en las que el usuario logra pasar de al menos un elemento marcado como REVIEW/RISK a menos elementos después de corregir la escena.**

No confundir esto con reducción real de casos de dengue.

---

# 54. Métricas técnicas

Registrar localmente:

- tiempo de carga del modelo;
- tiempo de preprocessing;
- tiempo de inferencia;
- tiempo de postprocessing;
- memoria aproximada;
- modelo;
- versión;
- cantidad de detecciones.

---

# 55. Criterios de aceptación — P0

El MVP NO está terminado si alguno falla.

### AC-01

La app instala en Android físico.

### AC-02

La cámara funciona.

### AC-03

El modelo está empaquetado localmente.

### AC-04

Modo avión no rompe el scanner.

### AC-05

Una escena preparada produce al menos una detección real.

### AC-06

La UI muestra bounding boxes correctamente alineadas.

### AC-07

El usuario recibe una acción concreta.

### AC-08

Puede realizar un segundo escaneo.

### AC-09

El resumen compara primer y segundo escaneo.

### AC-10

No se realizan requests de red durante inferencia.

### AC-11

Las imágenes no quedan almacenadas por defecto.

### AC-12

Existe estado UNCERTAIN.

---

# 56. Criterios de aceptación — P1

Deseables:

- historial local;
- debug metrics;
- múltiples clases;
- pregunta de confirmación;
- animación comparación antes/después;
- modo oscuro;
- modelo descargable/updateable.

---

# 57. Criterios de aceptación — P2

Después de la competencia:

- backend;
- sync;
- dashboard;
- geolocalización opt-in;
- modo brigadista;
- actualización remota segura de modelos;
- analítica territorial;
- estudios de campo.

---

# 58. Estructura sugerida del proyecto

```text
detengue/
│
├── android/
│   ├── app/
│   │   └── src/main/java/.../
│   │       ├── core/
│   │       │   ├── model/
│   │       │   ├── camera/
│   │       │   ├── inference/
│   │       │   ├── rules/
│   │       │   └── database/
│   │       ├── feature/
│   │       │   ├── onboarding/
│   │       │   ├── home/
│   │       │   ├── scan/
│   │       │   ├── results/
│   │       │   ├── recheck/
│   │       │   ├── history/
│   │       │   └── diagnostics/
│   │       └── ui/
│   │
│   └── models/
│       └── detengue_detector_int8.tflite
│
├── ml/
│   ├── dataset/
│   ├── train.py
│   ├── evaluate.py
│   ├── export_tflite.py
│   └── benchmark/
│
├── backend/                 # fase futura
│
├── docs/
│   ├── PRD.md
│   ├── MODEL_CARD.md
│   ├── PRIVACY.md
│   └── DEMO.md
│
└── README.md
```

---

# 59. Separación de responsabilidades

## Android Agent

Responsable de:

- Compose;
- CameraX;
- overlay;
- Room;
- inferencia TFLite;
- flujo UX;
- benchmark local.

## ML Agent

Responsable de:

- dataset;
- entrenamiento;
- evaluación;
- cuantización;
- export;
- model card.

## QA Agent

Responsable de:

- pruebas offline;
- low-memory;
- rotación;
- permisos;
- cold start;
- modelos corruptos;
- regresiones.

## Product/UI Agent

Responsable de:

- copy;
- accesibilidad;
- flujos;
- estados de error;
- demo.

---

# 60. Orden de implementación obligatorio

## Fase 1 — Esqueleto

1. crear proyecto Android;
2. home;
3. permisos;
4. CameraX;
5. captura de frame.

## Fase 2 — Inferencia

6. integrar modelo placeholder TFLite;
7. preprocessing;
8. inference;
9. postprocessing;
10. bounding boxes.

## Fase 3 — Producto

11. reglas;
12. resultados;
13. recomendaciones;
14. reescaneo;
15. comparación.

## Fase 4 — Offline

16. verificar cero dependencia de red;
17. modo avión;
18. Room;
19. historial.

## Fase 5 — ML real

20. entrenar detector;
21. export INT8;
22. benchmark;
23. ajustar thresholds.

## Fase 6 — Demo

24. preparar escena;
25. diagnostics overlay;
26. ensayo completo;
27. medir tiempo;
28. grabar backup de la demo.

No comenzar backend antes del paso 18.

---

# 61. Plan de implementación sugerido

## Día 1–2

- Android skeleton;
- CameraX;
- UI básica;
- frame capture.

## Día 3–4

- modelo inicial;
- inference;
- boxes;
- benchmark.

## Día 5–6

- datos propios;
- entrenamiento;
- export INT8.

## Día 7

- reglas;
- recomendaciones;
- recheck.

## Día 8

- historial;
- privacidad;
- modo avión.

## Día 9

- mejorar dataset;
- falsos positivos;
- thresholds.

## Día 10

- pruebas en teléfono modesto.

## Día 11

- polish visual;
- diagnostics;
- demo.

## Día 12+

- buffer;
- pitch;
- video;
- opcionales.

---

# 62. Pruebas obligatorias

## Funcionales

- abrir scanner;
- aceptar permiso;
- rechazar permiso;
- escanear;
- volver a escanear;
- historial;
- cerrar/reabrir app.

## Offline

- instalar;
- activar modo avión;
- reiniciar app;
- escanear;
- completar inspección.

## Performance

- cold start;
- primera inferencia;
- inferencias repetidas;
- memoria después de 10 scans.

## IA

- objeto frontal;
- lateral;
- parcialmente oculto;
- luz baja;
- luz fuerte;
- fondo complejo;
- sin objetos relevantes.

---

# 63. Device matrix

Mínimo:

## Device A

Teléfono principal de desarrollo.

## Device B

Android físico de gama baja/media con 3–4 GB RAM.

## Device C

Otro chipset si se puede conseguir.

Un emulador NO sustituye el benchmark del Device B.

---

# 64. Riesgos del proyecto

## Riesgo 1 — modelo demasiado lento

Mitigación:

- 320×320;
- INT8;
- scan bajo demanda;
- XNNPACK;
- reducir clases.

## Riesgo 2 — detectar agua es inconsistente

Mitigación:

- REVIEW;
- human confirmation;
- no basar todo el MVP en agua visible.

## Riesgo 3 — dataset insuficiente

Mitigación:

- limitar categorías;
- recolectar escenas específicas;
- data augmentation moderado.

## Riesgo 4 — falsos positivos

Mitigación:

- threshold alto;
- UNCERTAIN;
- negativos difíciles.

## Riesgo 5 — demo falla

Mitigación:

- escena conocida pero real;
- iluminación controlada;
- warm-up del modelo;
- ensayo;
- backup video.

---

# 65. Model Card obligatoria

Crear:

`docs/MODEL_CARD.md`

Debe incluir:

- arquitectura;
- clases;
- dataset;
- número de imágenes;
- split;
- métricas;
- tamaño;
- cuantización;
- hardware probado;
- latencia;
- limitaciones;
- casos donde NO usar.

---

# 66. Limitaciones explícitas

Detengue puede fallar ante:

- mala iluminación;
- objetos ocultos;
- recipientes transparentes;
- agua no visible;
- reflejos;
- ángulos extremos;
- clases no entrenadas.

La interfaz debe reconocer estas limitaciones.

---

# 67. Privacidad técnica

Durante scan:

```text
Camera frame
   ↓
RAM
   ↓
Inference
   ↓
Detection metadata
   ↓
Frame discarded
```

Persistir:

- clase;
- confidence;
- estado;
- timestamp.

No persistir:

- fotografía completa.

---

# 68. Futuro institucional

Arquitectura conceptual:

```text
Ciudadano
   ↓
Detengue Offline
   ↓
metadata local
   ↓ cuando exista internet
sync opcional
   ↓
Backend
   ↓
Dashboard
```

La inferencia debe seguir ocurriendo en el dispositivo.

---

# 69. Modelo económico conceptual

No implementar pagos.

Hipótesis:

## Ciudadano

Gratis.

## Institución

En el futuro podría pagar por:

- dashboard;
- administración;
- sincronización;
- soporte;
- deployment;
- analítica;
- gestión de campañas.

La inferencia local mantiene bajo el costo marginal por inspección.

---

# 70. Diferenciación

Detengue NO se posiciona como:

“otra plataforma para registrar casos”.

Se posiciona como:

> **una capa de visión artificial accesible para asistir la autoinspección del entorno físico.**

Diferenciales:

- teléfono económico;
- offline;
- inferencia local;
- privacidad;
- costo marginal cercano a cero;
- acción inmediata;
- antes/después verificable.

---

# 71. Mensaje de impacto

La historia del producto:

```text
Hoy:
“Busca posibles criaderos.”

Detengue:
“Yo te ayudo a encontrarlos.”
```

---

# 72. Mensaje tecnológico

No:

> “Usamos inteligencia artificial de última generación.”

Sí:

> “El modelo corre en el teléfono porque la prevención no debería depender de conexión permanente ni de pagar por cada análisis.”

---

# 73. Demo técnica objetivo

Debe poder mostrarse:

```text
Airplane mode: ON
Internet: OFF

Model: Local
Inference: < 1 s

Camera
  ↓
detección real
  ↓
acción física
  ↓
segundo scan
  ↓
mejora visible
```

---

# 74. Definition of Done del hackathon

Detengue está listo para entregar cuando:

- existe APK instalable;
- la inferencia es real;
- funciona offline;
- corre en al menos un Android modesto físico;
- hay un modelo cuantizado local;
- el escaneo tarda un tiempo razonable;
- la demo antes/después funciona;
- no hay dependencia crítica de cloud;
- la privacidad está explicada;
- existe documentación técnica;
- existe model card;
- tenemos métricas reales;
- podemos ejecutar el wow moment en menos de 40 segundos.

---

# 75. North Star

Cada decisión técnica debe responder:

> **¿Esto ayuda a que una persona con un Android económico pueda revisar su entorno de manera simple, privada y sin Internet?**

Si la respuesta es no, probablemente no pertenece al MVP.

---

# 76. Frase final del producto

> **Detengue convierte la cámara de un celular común en un asistente de prevención que ayuda a identificar qué revisar, incluso sin Internet.**
