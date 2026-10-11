package com.dn0ne.player.core.util

import androidx.annotation.ColorInt
import androidx.annotation.IntRange

object CalculationUtils {
    /**
     * Linearly interpolates between [start] and [stop] by [amount].
     */
    fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    /**
     * Inverted linear interpolation: calculates the fractional position of [value] between [start] and [stop].
     */
    fun lerpInv(start: Float, stop: Float, value: Float): Float {
        return if (start != stop) (value - start) / (stop - start) else 0f
    }

    /**
     * Inverted linear interpolation saturated (clamped) to the range [0, 1].
     */
    fun lerpInvSat(start: Float, stop: Float, value: Float): Float {
        return lerpInv(start, stop, value).coerceIn(0f, 1f)
    }

    /**
     * Sets the alpha component of [color] to [alpha].
     */
    @ColorInt
    fun setAlphaComponent(
        @ColorInt color: Int,
        @IntRange(from = 0, to = 255) alpha: Int
    ): Int {
        require(alpha in 0..255) { "alpha must be between 0 and 255." }
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }
}
