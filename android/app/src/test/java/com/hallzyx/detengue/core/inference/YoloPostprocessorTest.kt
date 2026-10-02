package com.hallzyx.detengue.core.inference

import com.hallzyx.detengue.core.model.NormalizedBox
import com.hallzyx.detengue.core.model.ObjectClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloPostprocessorTest {

    private val eps = 1e-3f
    private val classes = listOf(ObjectClass.BUCKET, ObjectClass.BOTTLE)
    private val identity = LetterboxTransform(320, 320, 320)

    /** Cada ancla: cx, cy, w, h, scoreBucket, scoreBottle. */
    private fun channelFirst(vararg anchors: FloatArray): FloatArray {
        val n = anchors.size
        val out = FloatArray(6 * n)
        for (a in 0 until n) for (c in 0 until 6) out[c * n + a] = anchors[a][c]
        return out
    }

    private fun channelLast(vararg anchors: FloatArray): FloatArray =
        anchors.flatMap { it.toList() }.toFloatArray()

    private fun anchor(cx: Float, cy: Float, w: Float, h: Float, bucket: Float, bottle: Float) =
        floatArrayOf(cx, cy, w, h, bucket, bottle)

    private val sample = arrayOf(
        anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.90f, 0.10f), // balde
        anchor(0.51f, 0.5f, 0.4f, 0.4f, 0.80f, 0.05f), // duplicado del balde
        anchor(0.2f, 0.2f, 0.1f, 0.1f, 0.05f, 0.70f), // botella
    )

    @Test
    fun decodesChannelFirstAndSuppressesDuplicates() {
        val pp = YoloPostprocessor(classes)
        val result = pp.process(channelFirst(*sample), intArrayOf(1, 6, 3), 320, identity)

        assertEquals(2, result.size)
        assertEquals(ObjectClass.BUCKET, result[0].objectClass)
        assertEquals(0.90f, result[0].confidence, eps)
        assertEquals(0.3f, result[0].box.x, eps)
        assertEquals(0.3f, result[0].box.y, eps)
        assertEquals(0.4f, result[0].box.width, eps)
        assertEquals(0.4f, result[0].box.height, eps)
        assertEquals(ObjectClass.BOTTLE, result[1].objectClass)
    }

    @Test
    fun channelLastLayoutGivesSameResult() {
        val pp = YoloPostprocessor(classes)
        val first = pp.process(channelFirst(*sample), intArrayOf(1, 6, 3), 320, identity)
        val last = pp.process(channelLast(*sample), intArrayOf(1, 3, 6), 320, identity)
        assertEquals(first.size, last.size)
        first.zip(last).forEach { (a, b) ->
            assertEquals(a.objectClass, b.objectClass)
            assertEquals(a.confidence, b.confidence, eps)
            assertEquals(a.box.x, b.box.x, eps)
            assertEquals(a.box.width, b.box.width, eps)
        }
    }

    @Test
    fun acceptsPixelCoordinates() {
        val pp = YoloPostprocessor(classes)
        val pixels = arrayOf(anchor(160f, 160f, 128f, 128f, 0.9f, 0.1f))
        val result = pp.process(channelFirst(*pixels), intArrayOf(1, 6, 1), 320, identity)
        assertEquals(1, result.size)
        assertEquals(0.3f, result[0].box.x, eps)
        assertEquals(0.4f, result[0].box.width, eps)
    }

    @Test
    fun dropsDetectionsBelowThreshold() {
        val pp = YoloPostprocessor(classes, detectionThreshold = 0.5f)
        val weak = arrayOf(anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.45f, 0.30f))
        val result = pp.process(channelFirst(*weak), intArrayOf(1, 6, 1), 320, identity)
        assertTrue(result.isEmpty())
    }

    @Test
    fun ignoresClassesMappedToNull() {
        val pp = YoloPostprocessor(listOf(ObjectClass.BUCKET, null))
        val onlyIgnored = arrayOf(anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.1f, 0.95f))
        val result = pp.process(channelFirst(*onlyIgnored), intArrayOf(1, 6, 1), 320, identity)
        assertTrue(result.isEmpty())
    }

    @Test
    fun undoesLetterboxForPortraitFrames() {
        val t = LetterboxTransform(480, 640, 320) // padX=40, escala 0.5
        val pp = YoloPostprocessor(classes)
        val a = arrayOf(anchor(160f, 160f, 80f, 160f, 0.9f, 0.1f)) // x:120..200, y:80..240 (píxeles)
        val result = pp.process(channelFirst(*a), intArrayOf(1, 6, 1), 320, t)
        assertEquals(1, result.size)
        assertEquals(1f / 3f, result[0].box.x, eps)
        assertEquals(1f / 3f, result[0].box.width, eps)
        assertEquals(0.25f, result[0].box.y, eps)
        assertEquals(0.5f, result[0].box.height, eps)
    }

    @Test
    fun nmsIsClassAware() {
        val pp = YoloPostprocessor(classes)
        val overlapping = arrayOf(
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.90f, 0.05f),
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.05f, 0.80f),
        )
        val result = pp.process(channelFirst(*overlapping), intArrayOf(1, 6, 2), 320, identity)
        assertEquals(2, result.size)
    }

    @Test
    fun respectsMaxDetections() {
        val pp = YoloPostprocessor(classes, maxDetections = 2)
        val many = Array(5) { i -> anchor(0.1f + i * 0.18f, 0.5f, 0.1f, 0.1f, 0.9f - i * 0.01f, 0.0f) }
        val result = pp.process(channelFirst(*many), intArrayOf(1, 6, 5), 320, identity)
        assertEquals(2, result.size)
    }

    @Test
    fun emptyWhenNothingDetected() {
        val pp = YoloPostprocessor(classes)
        val none = arrayOf(anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.01f, 0.02f))
        assertTrue(pp.process(channelFirst(*none), intArrayOf(1, 6, 1), 320, identity).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsShapeThatDoesNotMatchClassCount() {
        YoloPostprocessor(classes).process(FloatArray(100), intArrayOf(1, 10, 10), 320, identity)
    }

    @Test
    fun iouBehavesSanely() {
        val a = NormalizedBox(0f, 0f, 0.5f, 0.5f)
        assertEquals(1f, a.iou(a), eps)
        assertEquals(0f, a.iou(NormalizedBox(0.6f, 0.6f, 0.2f, 0.2f)), eps)
        assertEquals(1f / 3f, a.iou(NormalizedBox(0.25f, 0f, 0.5f, 0.5f)), eps)
    }
}
