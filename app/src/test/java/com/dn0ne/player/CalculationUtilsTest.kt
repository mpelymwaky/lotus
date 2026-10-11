package com.dn0ne.player

import com.dn0ne.player.core.util.CalculationUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationUtilsTest {

    @Test
    fun testLerp() {
        assertEquals(0f, CalculationUtils.lerp(0f, 100f, 0f), 0.0001f)
        assertEquals(50f, CalculationUtils.lerp(0f, 100f, 0.5f), 0.0001f)
        assertEquals(100f, CalculationUtils.lerp(0f, 100f, 1f), 0.0001f)
        assertEquals(20f, CalculationUtils.lerp(10f, 30f, 0.5f), 0.0001f)
    }

    @Test
    fun testLerpInv() {
        assertEquals(0f, CalculationUtils.lerpInv(0f, 100f, 0f), 0.0001f)
        assertEquals(0.5f, CalculationUtils.lerpInv(0f, 100f, 50f), 0.0001f)
        assertEquals(1f, CalculationUtils.lerpInv(0f, 100f, 100f), 0.0001f)
        assertEquals(1.5f, CalculationUtils.lerpInv(0f, 100f, 150f), 0.0001f)
        assertEquals(-0.5f, CalculationUtils.lerpInv(0f, 100f, -50f), 0.0001f)
        assertEquals(0f, CalculationUtils.lerpInv(100f, 100f, 100f), 0.0001f)
    }

    @Test
    fun testLerpInvSat() {
        assertEquals(0f, CalculationUtils.lerpInvSat(0f, 100f, 0f), 0.0001f)
        assertEquals(0.5f, CalculationUtils.lerpInvSat(0f, 100f, 50f), 0.0001f)
        assertEquals(1f, CalculationUtils.lerpInvSat(0f, 100f, 100f), 0.0001f)
        assertEquals(1f, CalculationUtils.lerpInvSat(0f, 100f, 150f), 0.0001f)
        assertEquals(0f, CalculationUtils.lerpInvSat(0f, 100f, -50f), 0.0001f)
    }

    @Test
    fun testSetAlphaComponent() {
        val color = 0x00112233
        val result = CalculationUtils.setAlphaComponent(color, 0xFF)
        assertEquals(-15654349 /* 0xFF112233 */, result)

        val transparent = CalculationUtils.setAlphaComponent(0xFFFFFFFF.toInt(), 0)
        assertEquals(0x00FFFFFF, transparent)

        val alpha77 = CalculationUtils.setAlphaComponent(0xFF112233.toInt(), 77)
        assertEquals((77 shl 24) or 0x00112233, alpha77)
    }
}
