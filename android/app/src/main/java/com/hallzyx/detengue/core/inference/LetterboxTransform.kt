package com.hallzyx.detengue.core.inference

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Relación entre el frame original y la entrada cuadrada del modelo, conservando proporciones
 * (letterbox: la imagen se escala y se centra; el resto es relleno).
 *
 * Es matemática pura para poder probarla sin Android.
 */
class LetterboxTransform(
    val srcWidth: Int,
    val srcHeight: Int,
    val dstSize: Int,
) {
    init {
        require(srcWidth > 0 && srcHeight > 0 && dstSize > 0) { "Dimensiones inválidas" }
    }

    val scale: Float = min(dstSize / srcWidth.toFloat(), dstSize / srcHeight.toFloat())
    val scaledWidth: Int = (srcWidth * scale).roundToInt().coerceIn(1, dstSize)
    val scaledHeight: Int = (srcHeight * scale).roundToInt().coerceIn(1, dstSize)
    val padX: Float = (dstSize - scaledWidth) / 2f
    val padY: Float = (dstSize - scaledHeight) / 2f

    /** X en píxeles de la entrada del modelo → 0..1 sobre el frame original. */
    fun unmapX(xInput: Float): Float = ((xInput - padX) / scaledWidth).coerceIn(0f, 1f)

    /** Y en píxeles de la entrada del modelo → 0..1 sobre el frame original. */
    fun unmapY(yInput: Float): Float = ((yInput - padY) / scaledHeight).coerceIn(0f, 1f)
}
