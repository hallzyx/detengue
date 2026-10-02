package com.hallzyx.detengue.core.model

/**
 * Estado visual del objeto según lo que el modelo ve en la imagen.
 * Es evidencia visual, nunca un diagnóstico.
 */
enum class ObservedState(val code: String) {
    /** El modelo no distingue estado (etiqueta sin sufijo). */
    UNKNOWN(""),

    /** Abierto o sin tapar: podría acumular agua, pero no se ve agua. */
    OPEN("open"),

    /** Agua visible. */
    WATER("water"),

    /** Cerrado / con tapa. */
    CLOSED("closed"),

    /** Boca abajo o drenado. */
    INVERTED("inverted"),
    ;

    companion object {
        fun fromCode(code: String): ObservedState? = entries.firstOrNull { it.code == code && it != UNKNOWN }
    }
}

/**
 * Una clase de la salida del modelo = objeto + estado.
 *
 * Decisión de diseño: en vez de encadenar un segundo clasificador de estado (más latencia y RAM
 * en gama baja), el detector emite directamente clases como `bucket:water` o `bottle:closed`.
 * Una sola inferencia, y el score de la clase actúa como confianza del estado: si el modelo duda
 * entre `open` y `water`, el score se reparte y las reglas caen a REVIEW por sí solas.
 */
data class ModelClass(
    val objectClass: ObjectClass,
    val state: ObservedState = ObservedState.UNKNOWN,
) {
    companion object {
        /**
         * Interpreta una línea de `detengue_labels.txt`: `objeto` o `objeto:estado`.
         * Devuelve `null` si el objeto o el estado son desconocidos (la app ignora esa clase).
         */
        fun parse(line: String): ModelClass? {
            val parts = line.trim().split(':', limit = 2)
            val objectClass = ObjectClass.fromCode(parts[0]) ?: return null
            if (parts.size == 1) return ModelClass(objectClass)
            val state = ObservedState.fromCode(parts[1].trim()) ?: return null
            return ModelClass(objectClass, state)
        }
    }
}
