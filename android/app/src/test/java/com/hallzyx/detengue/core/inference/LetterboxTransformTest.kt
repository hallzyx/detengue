package com.hallzyx.detengue.core.inference

import org.junit.Assert.assertEquals
import org.junit.Test

class LetterboxTransformTest {

    private val eps = 1e-4f

    @Test
    fun portraitFrameIsPaddedHorizontally() {
        val t = LetterboxTransform(srcWidth = 480, srcHeight = 640, dstSize = 320)
        assertEquals(0.5f, t.scale, eps)
        assertEquals(240, t.scaledWidth)
        assertEquals(320, t.scaledHeight)
        assertEquals(40f, t.padX, eps)
        assertEquals(0f, t.padY, eps)
    }

    @Test
    fun landscapeFrameIsPaddedVertically() {
        val t = LetterboxTransform(srcWidth = 640, srcHeight = 480, dstSize = 320)
        assertEquals(320, t.scaledWidth)
        assertEquals(240, t.scaledHeight)
        assertEquals(0f, t.padX, eps)
        assertEquals(40f, t.padY, eps)
    }

    @Test
    fun unmapMapsContentEdgesToZeroAndOne() {
        val t = LetterboxTransform(480, 640, 320)
        assertEquals(0f, t.unmapX(40f), eps)
        assertEquals(0.5f, t.unmapX(160f), eps)
        assertEquals(1f, t.unmapX(280f), eps)
        assertEquals(0.5f, t.unmapY(160f), eps)
    }

    @Test
    fun unmapClampsCoordinatesInsidePadding() {
        val t = LetterboxTransform(480, 640, 320)
        assertEquals(0f, t.unmapX(0f), eps)
        assertEquals(1f, t.unmapX(320f), eps)
    }

    @Test
    fun squareFrameHasNoPadding() {
        val t = LetterboxTransform(320, 320, 320)
        assertEquals(0f, t.padX, eps)
        assertEquals(0f, t.padY, eps)
        assertEquals(0.3f, t.unmapX(96f), eps)
    }

    @Test
    fun halfwayValuesRoundUp() {
        // 5 * 0.5 = 2.5 -> 3. Mismo caso que ml/tests/test_postprocess.py: Python y Kotlin deben coincidir.
        assertEquals(3, LetterboxTransform(srcWidth = 5, srcHeight = 10, dstSize = 5).scaledWidth)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        LetterboxTransform(0, 100, 320)
    }
}
