package com.hallzyx.detengue.core.inference

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.hallzyx.detengue.core.config.DetectionConfig
import com.hallzyx.detengue.core.model.DetectionResult
import com.hallzyx.detengue.core.model.InferenceTimings
import com.hallzyx.detengue.core.model.ModelClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** El modelo no está empaquetado, está corrupto o no tiene el formato esperado. */
class ModelUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Detector de objetos que corre 100 % en el dispositivo con TFLite (CPU + XNNPACK, sin red).
 *
 * Formato de modelo esperado (export de Ultralytics a TFLite):
 *  - entrada NHWC `[1, S, S, 3]`, float32 o cuantizada
 *  - una sola salida `[1, 4 + nc, N]` o `[1, N, 4 + nc]`
 * `nc` debe coincidir con las líneas de `detengue_labels.txt`.
 */
class TfliteObjectDetector private constructor(
    private val interpreter: Interpreter,
    private val classes: List<ModelClass?>,
    val modelName: String,
) {
    val inputSize: Int
    private val preprocessor: Preprocessor
    private val postprocessor = YoloPostprocessor(classes)

    private val outputShape: IntArray
    private val outputType: DataType
    private val outputScale: Float
    private val outputZeroPoint: Int
    private val outputBuffer: ByteBuffer
    private val outputValues: FloatArray

    private val mutex = Mutex()

    init {
        val input = interpreter.getInputTensor(0)
        val inShape = input.shape()
        if (inShape.size != 4 || inShape[3] != 3 || inShape[1] != inShape[2]) {
            throw ModelUnavailableException("Entrada no soportada: ${inShape.joinToString("x")}")
        }
        inputSize = inShape[1]
        val inQuant = input.quantizationParams()
        preprocessor = Preprocessor(inputSize, input.dataType(), inQuant.scale, inQuant.zeroPoint)

        if (interpreter.outputTensorCount != 1) {
            throw ModelUnavailableException("Se esperaba una sola salida, hay ${interpreter.outputTensorCount}")
        }
        val output = interpreter.getOutputTensor(0)
        outputShape = output.shape()
        outputType = output.dataType()
        outputScale = output.quantizationParams().scale
        outputZeroPoint = output.quantizationParams().zeroPoint
        outputBuffer = ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder())
        outputValues = FloatArray(output.numElements())

        val channels = 4 + classes.size
        if (outputShape.takeLast(2).none { it == channels }) {
            throw ModelUnavailableException(
                "La salida ${outputShape.joinToString("x")} no coincide con ${classes.size} clases",
            )
        }
    }

    /** Ejecuta una inferencia vacía para que la primera real no pague la inicialización. */
    suspend fun warmUp() = withContext(Dispatchers.Default) {
        mutex.withLock {
            preprocessor.buffer.rewind()
            outputBuffer.rewind()
            interpreter.run(preprocessor.buffer, outputBuffer)
        }
    }

    /** Analiza un frame. El [Bitmap] NO se modifica ni se conserva aquí. */
    suspend fun detect(frame: Bitmap): DetectionResult = withContext(Dispatchers.Default) {
        mutex.withLock {
            val transform = LetterboxTransform(frame.width, frame.height, inputSize)

            val t0 = SystemClock.elapsedRealtimeNanos()
            val input = preprocessor.prepare(frame, transform)
            val t1 = SystemClock.elapsedRealtimeNanos()

            outputBuffer.rewind()
            interpreter.run(input, outputBuffer)
            readOutput()
            val t2 = SystemClock.elapsedRealtimeNanos()

            val detections = postprocessor.process(outputValues, outputShape, inputSize, transform)
            val t3 = SystemClock.elapsedRealtimeNanos()

            DetectionResult(
                detections = detections,
                timings = InferenceTimings(
                    preprocessMs = (t1 - t0) / NANOS_PER_MS,
                    inferenceMs = (t2 - t1) / NANOS_PER_MS,
                    postprocessMs = (t3 - t2) / NANOS_PER_MS,
                ),
                frameWidth = frame.width,
                frameHeight = frame.height,
            )
        }
    }

    private fun readOutput() {
        outputBuffer.rewind()
        when (outputType) {
            DataType.FLOAT32 -> outputBuffer.asFloatBuffer().get(outputValues)
            DataType.INT8 -> for (i in outputValues.indices) {
                outputValues[i] = (outputBuffer.get().toInt() - outputZeroPoint) * outputScale
            }
            DataType.UINT8 -> for (i in outputValues.indices) {
                outputValues[i] = ((outputBuffer.get().toInt() and 0xFF) - outputZeroPoint) * outputScale
            }
            else -> throw ModelUnavailableException("Tipo de salida no soportado: $outputType")
        }
    }

    fun close() {
        interpreter.close()
        preprocessor.release()
    }

    companion object {
        private const val NANOS_PER_MS = 1_000_000L

        /** Carga el modelo y las etiquetas desde los assets. Lanza [ModelUnavailableException] si no se puede. */
        fun load(context: Context): TfliteObjectDetector {
            val classes = loadClasses(context)
            val model = mapModel(context)
            val options = Interpreter.Options().setNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
            val interpreter = try {
                Interpreter(model, options)
            } catch (t: Throwable) {
                throw ModelUnavailableException("No se pudo inicializar el intérprete", t)
            }
            return try {
                TfliteObjectDetector(interpreter, classes, DetectionConfig.MODEL_ASSET.removeSuffix(".tflite"))
            } catch (e: ModelUnavailableException) {
                interpreter.close()
                throw e
            }
        }

        private fun loadClasses(context: Context): List<ModelClass?> = try {
            context.assets.open(DetectionConfig.LABELS_ASSET).bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { ModelClass.parse(it) }
                    .toList()
            }
        } catch (e: FileNotFoundException) {
            throw ModelUnavailableException("Falta ${DetectionConfig.LABELS_ASSET}", e)
        }

        private fun mapModel(context: Context): ByteBuffer = try {
            context.assets.openFd(DetectionConfig.MODEL_ASSET).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        } catch (e: FileNotFoundException) {
            throw ModelUnavailableException("Falta ${DetectionConfig.MODEL_ASSET} en los assets", e)
        }
    }
}
