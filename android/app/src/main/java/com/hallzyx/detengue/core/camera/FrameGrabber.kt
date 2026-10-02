package com.hallzyx.detengue.core.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicReference

/**
 * Captura UN frame bajo demanda (PRD §14: preview fluido + botón "Analizar", sin inferencia continua).
 *
 * Privacidad (PRD §36, §67): el frame solo existe como [Bitmap] en RAM. No se codifica a JPEG,
 * no se escribe en disco y no se registra su contenido. Quien lo recibe es responsable de
 * llamar a [Bitmap.recycle] cuando termine la inferencia.
 *
 * Mientras no hay una captura pendiente, cada frame se cierra de inmediato, por lo que el
 * coste en CPU de tener el analizador enlazado es mínimo (importante en gama baja).
 */
class FrameGrabber : ImageAnalysis.Analyzer {

    private val pending = AtomicReference<CompletableDeferred<Bitmap>?>(null)

    /**
     * Espera el siguiente frame de la cámara y lo devuelve ya rotado en vertical.
     * Devuelve `null` si no llega ningún frame antes de [timeoutMs].
     */
    suspend fun grab(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Bitmap? {
        val request = CompletableDeferred<Bitmap>()
        pending.set(request)
        return try {
            withTimeoutOrNull(timeoutMs) { request.await() }
        } finally {
            pending.compareAndSet(request, null)
        }
    }

    override fun analyze(image: ImageProxy) {
        val request = pending.getAndSet(null)
        if (request == null) {
            image.close()
            return
        }
        try {
            request.complete(image.toUprightBitmap())
        } catch (t: Throwable) {
            request.completeExceptionally(t)
        } finally {
            image.close()
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 2_000L
    }
}

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val raw = toBitmap()
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return raw
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    if (rotated !== raw) raw.recycle()
    return rotated
}
