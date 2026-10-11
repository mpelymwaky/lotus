/*
 *     Copyright (C) 2022 The Android Open Source Project
 *                   2024 Akane Foundation
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.dn0ne.player.app.presentation.components.playback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.animation.PathInterpolator
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withClip
import com.dn0ne.player.core.util.CalculationUtils.lerp
import com.dn0ne.player.core.util.CalculationUtils.lerpInv
import com.dn0ne.player.core.util.CalculationUtils.lerpInvSat
import com.dn0ne.player.core.util.CalculationUtils.setAlphaComponent
import kotlin.math.abs
import kotlin.math.cos

class SquigglyProgress : Drawable() {

    companion object {
        private const val TWO_PI = (Math.PI * 2f).toFloat()

        @VisibleForTesting
        internal const val DISABLED_ALPHA = 77
    }

    private val wavePaint = Paint()
    private val linePaint = Paint()
    private val path = Path()
    private var heightFraction = 0f
    private var heightAnimator: ValueAnimator? = null
    private var phaseOffset = 0f
    private var lastFrameTime = -1L

    /* distance over which amplitude drops to zero, measured in wavelengths */
    private val transitionPeriods = 1.5f

    /* wave endpoint as percentage of bar when play position is zero */
    private val minWaveEndpoint = 0f

    /* wave endpoint as percentage of bar when play position matches wave endpoint */
    private val matchedWaveEndpoint = 1f

    // Horizontal length of the sine wave
    var waveLength = 0f

    // Height of each peak of the sine wave
    var lineAmplitude = 0f

    // Line speed in px per second
    var phaseSpeed = 0f

    // Progress stroke width, both for wave and solid line
    var strokeWidth = 0f
        set(value) {
            if (field == value) {
                return
            }
            field = value
            wavePaint.strokeWidth = value
            linePaint.strokeWidth = value
        }

    // Enables a transition region where the amplitude
    // of the wave is reduced linearly across it.
    var transitionEnabled = true
        set(value) {
            field = value
            invalidateSelf()
        }

    init {
        wavePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.style = Paint.Style.STROKE
        wavePaint.style = Paint.Style.STROKE
        linePaint.alpha = DISABLED_ALPHA
    }

    var animate: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (field) {
                lastFrameTime = SystemClock.uptimeMillis()
            }
            heightAnimator?.cancel()
            heightAnimator =
                ValueAnimator.ofFloat(heightFraction, if (animate) 1f else 0f).apply {
                    if (animate) {
                        startDelay = 60
                        duration = 800
                        interpolator = PathInterpolator(
                            0.05f, 0.7f, 0.1f, 1f
                        )
                    } else {
                        duration = 550
                        interpolator = PathInterpolator(
                            0f, 0f, 0f, 1f
                        )
                    }
                    addUpdateListener {
                        heightFraction = it.animatedValue as Float
                        invalidateSelf()
                    }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                heightAnimator = null
                            }
                        }
                    )
                    start()
                }
        }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty || waveLength <= 0f) {
            return
        }

        if (animate) {
            invalidateSelf()
            val now = SystemClock.uptimeMillis()
            if (lastFrameTime < 0L) {
                lastFrameTime = now
            }
            phaseOffset += (now - lastFrameTime) / 1000f * phaseSpeed
            phaseOffset %= waveLength
            lastFrameTime = now
        }

        val progress = level / 10_000f
        val totalWidth = bounds.width().toFloat()
        val totalProgressPx = totalWidth * progress
        val waveProgressPx =
            totalWidth *
                    (if (!transitionEnabled || progress > matchedWaveEndpoint) progress
                    else
                        lerp(
                            minWaveEndpoint,
                            matchedWaveEndpoint,
                            lerpInv(0f, matchedWaveEndpoint, progress)
                        ))

        // Build Wiggly Path
        val waveStart = -phaseOffset - waveLength / 2f
        val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

        // helper function, computes amplitude for wave segment
        val computeAmplitude: (Float, Float) -> Float = { x, sign ->
            if (transitionEnabled) {
                val length = transitionPeriods * waveLength
                val coeff =
                    lerpInvSat(waveProgressPx + length / 2f, waveProgressPx - length / 2f, x)
                sign * heightFraction * lineAmplitude * coeff
            } else {
                sign * heightFraction * lineAmplitude
            }
        }

        // Reset path object to the start
        path.rewind()
        path.moveTo(waveStart, 0f)

        // Build the wave, incrementing by half the wavelength each time
        var currentX = waveStart
        var waveSign = 1f
        var currentAmp = computeAmplitude(currentX, waveSign)
        val dist = waveLength / 2f
        while (currentX < waveEnd) {
            waveSign = -waveSign
            val nextX = currentX + dist
            val midX = currentX + dist / 2
            val nextAmp = computeAmplitude(nextX, waveSign)
            path.cubicTo(midX, currentAmp, midX, nextAmp, nextX, nextAmp)
            currentAmp = nextAmp
            currentX = nextX
        }

        // translate to the start position of the progress bar for all draw commands
        val clipTop = lineAmplitude + strokeWidth
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.centerY().toFloat())

        // Draw path up to progress position
        canvas.save()
        canvas.clipRect(0f, -1f * clipTop, totalProgressPx, clipTop)
        canvas.drawPath(path, wavePaint)
        canvas.restore()

        if (transitionEnabled) {
            // If there's a smooth transition, we draw the rest of the
            // path in a different color (using different clip params)
            canvas.withClip(totalProgressPx, -1f * clipTop, totalWidth, clipTop) {
                drawPath(path, linePaint)
            }
        } else {
            // No transition, just draw a flat line to the end of the region.
            // The discontinuity is hidden by the progress bar thumb shape.
            canvas.drawLine(totalProgressPx, 0f, totalWidth, 0f, linePaint)
        }

        // Draw round line cap at the beginning of the wave
        val startAmp = cos(abs(waveStart) / waveLength * TWO_PI)
        canvas.drawPoint(0f, startAmp * lineAmplitude * heightFraction, wavePaint)

        canvas.restore()
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        wavePaint.colorFilter = colorFilter
        linePaint.colorFilter = colorFilter
    }

    override fun setAlpha(alpha: Int) {
        updateColors(wavePaint.color, alpha)
    }

    override fun getAlpha(): Int {
        return wavePaint.alpha
    }

    override fun setTint(tintColor: Int) {
        updateColors(tintColor, alpha)
    }

    override fun onLevelChange(level: Int): Boolean {
        return animate
    }

    override fun setTintList(tint: ColorStateList?) {
        if (tint == null) {
            return
        }
        updateColors(tint.defaultColor, alpha)
    }

    fun setColors(waveColor: Int, lineColor: Int) {
        wavePaint.color = waveColor
        linePaint.color = lineColor
        invalidateSelf()
    }

    private fun updateColors(tintColor: Int, alpha: Int) {
        wavePaint.color = setAlphaComponent(tintColor, alpha)
        linePaint.color =
            setAlphaComponent(tintColor, (DISABLED_ALPHA * (alpha / 255f)).toInt())
    }
}

/**
 * Composable wrapper for [SquigglyProgress] to render Android 13/Gramophone's
 * squiggly progress line inside Jetpack Compose.
 */
@Composable
fun SquigglyProgress(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    strokeWidth: Dp = 3.dp,
    waveLength: Dp = 42.dp,
    lineAmplitude: Dp = 6.dp,
    phaseSpeed: Dp = 40.dp,
    transitionEnabled: Boolean = true,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val waveLengthPx = with(density) { waveLength.toPx() }
    val lineAmplitudePx = with(density) { lineAmplitude.toPx() }
    val phaseSpeedPx = with(density) { phaseSpeed.toPx() }

    val drawable = remember { SquigglyProgress() }

    drawable.strokeWidth = strokeWidthPx
    drawable.waveLength = waveLengthPx
    drawable.lineAmplitude = lineAmplitudePx
    drawable.phaseSpeed = phaseSpeedPx
    drawable.transitionEnabled = transitionEnabled
    drawable.animate = isPlaying
    drawable.setColors(waveColor.toArgb(), trackColor.toArgb())

    val layoutDirection = LocalLayoutDirection.current
    val isLtr = layoutDirection == LayoutDirection.Ltr

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                withFrameMillis { time ->
                    frameTick = time
                }
            }
        } else {
            val startTime = SystemClock.uptimeMillis()
            while (SystemClock.uptimeMillis() - startTime < 650) {
                withFrameMillis { time ->
                    frameTick = time
                }
            }
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            if (!isLtr) {
                scaleX = -1f
            }
        }
    ) {
        val tick = frameTick
        val newLevel = (progress.coerceIn(0f, 1f) * 10_000).toInt()
        drawable.level = newLevel
        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }
}
