package com.hallzyx.detengue.core.model

/**
 * Clases objetivo del detector (PRD §15).
 *
 * El ORDEN en que el modelo emite sus clases lo define `android/models/detengue_labels.txt`
 * (una clase por línea, por [code]). El entrenamiento en `ml/` debe respetar ese archivo.
 */
enum class ObjectClass(val code: String) {
    BUCKET("bucket"),
    CONTAINER("container"),
    PLANT_POT("plant_pot"),
    BOTTLE("bottle"),
    TIRE("tire"),
    PET_BOWL("pet_bowl"),
    DRUM("drum"),
    WATER_TANK("water_tank"),
    ;

    companion object {
        fun fromCode(code: String): ObjectClass? = entries.firstOrNull { it.code == code.trim() }
    }
}
