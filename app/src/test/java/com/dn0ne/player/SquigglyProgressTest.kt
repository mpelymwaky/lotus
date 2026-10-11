package com.dn0ne.player

import com.dn0ne.player.app.presentation.components.playback.SquigglyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SquigglyProgressTest {

    @Test
    fun testSquigglyProgressInitialState() {
        val progress = SquigglyProgress()
        assertEquals(0f, progress.waveLength, 0.0001f)
        assertEquals(0f, progress.lineAmplitude, 0.0001f)
        assertEquals(0f, progress.phaseSpeed, 0.0001f)
        assertEquals(0f, progress.strokeWidth, 0.0001f)
        assertTrue(progress.transitionEnabled)
        assertFalse(progress.animate)
    }

    @Test
    fun testPropertiesSetAndGet() {
        val progress = SquigglyProgress()
        progress.waveLength = 40f
        progress.lineAmplitude = 6f
        progress.phaseSpeed = 30f
        progress.strokeWidth = 4f
        progress.transitionEnabled = false

        assertEquals(40f, progress.waveLength, 0.0001f)
        assertEquals(6f, progress.lineAmplitude, 0.0001f)
        assertEquals(30f, progress.phaseSpeed, 0.0001f)
        assertEquals(4f, progress.strokeWidth, 0.0001f)
        assertFalse(progress.transitionEnabled)
    }

    @Test
    fun testDisabledAlphaConstant() {
        assertEquals(77, SquigglyProgress.DISABLED_ALPHA)
    }
}
