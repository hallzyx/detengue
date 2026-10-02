package com.hallzyx.detengue.core.inference

import com.hallzyx.detengue.core.model.ModelClass
import com.hallzyx.detengue.core.model.ObjectClass
import com.hallzyx.detengue.core.model.ObservedState
import org.junit.Assert.assertEquals
import org.junit.Test

/** El detector emite clases objeto+estado (p. ej. bucket:water); el estado debe llegar intacto. */
class StateAwareDetectionTest {

    private val classes = listOf(
        ModelClass(ObjectClass.BUCKET, ObservedState.OPEN),
        ModelClass(ObjectClass.BUCKET, ObservedState.WATER),
        ModelClass(ObjectClass.BOTTLE, ObservedState.CLOSED),
    )
    private val identity = LetterboxTransform(320, 320, 320)
    private val channels = 4 + classes.size

    private fun anchor(cx: Float, cy: Float, w: Float, h: Float, vararg scores: Float) =
        floatArrayOf(cx, cy, w, h, *scores)

    private fun channelFirst(vararg anchors: FloatArray): FloatArray {
        val n = anchors.size
        val out = FloatArray(channels * n)
        for (a in 0 until n) for (c in 0 until channels) out[c * n + a] = anchors[a][c]
        return out
    }

    private fun run(vararg anchors: FloatArray) = YoloPostprocessor(classes)
        .process(channelFirst(*anchors), intArrayOf(1, channels, anchors.size), 320, identity)

    @Test
    fun stateIsPropagatedToTheDetection() {
        val result = run(anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.10f, 0.85f, 0.00f))
        assertEquals(1, result.size)
        assertEquals(ObjectClass.BUCKET, result[0].objectClass)
        assertEquals(ObservedState.WATER, result[0].state)
        assertEquals(0.85f, result[0].confidence, 1e-4f)
    }

    @Test
    fun differentStatesOfTheSameObjectAreMergedByNms() {
        // Mismo balde visto como "abierto" (0.6) y "con agua" (0.8): es UN objeto, gana el de mayor confianza.
        val result = run(
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.60f, 0.10f, 0.00f),
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.10f, 0.80f, 0.00f),
        )
        assertEquals(1, result.size)
        assertEquals(ObservedState.WATER, result[0].state)
    }

    @Test
    fun differentObjectsAtTheSamePlaceAreKept() {
        val result = run(
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.10f, 0.80f, 0.00f),
            anchor(0.5f, 0.5f, 0.4f, 0.4f, 0.00f, 0.00f, 0.90f),
        )
        assertEquals(2, result.size)
        assertEquals(ObjectClass.BOTTLE, result[0].objectClass)
        assertEquals(ObservedState.CLOSED, result[0].state)
    }
}
