package com.hallzyx.detengue.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelClassTest {
    @Test
    fun bareObjectHasUnknownState() {
        assertEquals(ModelClass(ObjectClass.BUCKET, ObservedState.UNKNOWN), ModelClass.parse("bucket"))
    }

    @Test
    fun parsesObjectAndState() {
        assertEquals(ModelClass(ObjectClass.BUCKET, ObservedState.WATER), ModelClass.parse("bucket:water"))
        assertEquals(ModelClass(ObjectClass.WATER_TANK, ObservedState.CLOSED), ModelClass.parse("water_tank:closed"))
        assertEquals(ModelClass(ObjectClass.BOTTLE, ObservedState.CLOSED), ModelClass.parse("  bottle : closed  "))
    }

    @Test
    fun unknownObjectOrStateIsIgnored() {
        assertNull(ModelClass.parse("banana"))
        assertNull(ModelClass.parse("bucket:flying"))
        assertNull(ModelClass.parse("bucket:"))
        assertNull(ModelClass.parse(""))
    }
}
