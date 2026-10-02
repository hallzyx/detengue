package com.hallzyx.detengue.core.inference

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Convierte un [Bitmap] en el tensor de entrada del modelo (letterbox + normalización 0..1).
 *
 * Reutiliza todos sus buffers entre análisis para no generar presión de memoria en gama baja.
 * Acepta modelos con entrada float32 o cuantizada (int8/uint8), usando los parámetros de
 * cuantización del propio tensor, de modo que sirve tanto para exports FP32 como INT8.
 *
 * No es seguro entre hilos: [TfliteObjectDetector] lo serializa con un mutex.
 */
class Preprocessor(
    private val inputSize: Int,
    private val dataType: DataType,
    private val quantScale: Float,
    private val quantZeroPoint: Int,
) {
    private val canvasBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(canvasBitmap)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val pixels = IntArray(inputSize * inputSize)

    val buffer: ByteBuffer = ByteBuffer
        .allocateDirect(inputSize * inputSize * 3 * bytesPerValue(dataType))
        .order(ByteOrder.nativeOrder())

    fun prepare(frame: Bitmap, transform: LetterboxTransform): ByteBuffer {
        canvas.drawColor(Color.rgb(PAD_GRAY, PAD_GRAY, PAD_GRAY))
        val dst = RectF(
            transform.padX,
            transform.padY,
            transform.padX + transform.scaledWidth,
            transform.padY + transform.scaledHeight,
        )
        canvas.drawBitmap(frame, null, dst, paint)
        canvasBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        buffer.rewind()
        when (dataType) {
            DataType.FLOAT32 -> for (p in pixels) {
                buffer.putFloat(((p shr 16) and 0xFF) / 255f)
                buffer.putFloat(((p shr 8) and 0xFF) / 255f)
                buffer.putFloat((p and 0xFF) / 255f)
            }
            DataType.INT8 -> for (p in pixels) {
                buffer.put(quantize(((p shr 16) and 0xFF) / 255f, -128, 127).toByte())
                buffer.put(quantize(((p shr 8) and 0xFF) / 255f, -128, 127).toByte())
                buffer.put(quantize((p and 0xFF) / 255f, -128, 127).toByte())
            }
            DataType.UINT8 -> for (p in pixels) {
                buffer.put(quantize(((p shr 16) and 0xFF) / 255f, 0, 255).toByte())
                buffer.put(quantize(((p shr 8) and 0xFF) / 255f, 0, 255).toByte())
                buffer.put(quantize((p and 0xFF) / 255f, 0, 255).toByte())
            }
            else -> error("Tipo de entrada no soportado: $dataType")
        }
        buffer.rewind()
        return buffer
    }

    fun release() {
        canvasBitmap.recycle()
    }

    private fun quantize(value: Float, min: Int, max: Int): Int =
        ((value / quantScale).roundToInt() + quantZeroPoint).coerceIn(min, max)

    private companion object {
        const val PAD_GRAY = 114 // mismo relleno que usa Ultralytics en letterbox

        fun bytesPerValue(type: DataType) = when (type) {
            DataType.FLOAT32 -> 4
            DataType.INT8, DataType.UINT8 -> 1
            else -> error("Tipo de entrada no soportado: $type")
        }
    }
}
