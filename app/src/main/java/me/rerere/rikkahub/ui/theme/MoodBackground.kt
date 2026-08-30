/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * MoodBackground — Compose overlays for full-screen mood skins.
 *
 * Draws scanlines, noise, vignettes, and star fields on top of
 * the normal chat background when a mood is active.
 * Each overlay is opt-in based on MoodOverlay flags.
 */

package me.rerere.rikkahub.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.sin
import me.rerere.rikkahub.data.ai.mood.MoodMode
import java.util.Random

/**
 * Draws mood-driven overlays on top of the chat background.
 * Wrap your chat content in this composable for full-screen mood effects.
 *
 * Usage:
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     ChatContent()
 *     MoodOverlay(mode = currentMood)
 * }
 * ```
 */
@Composable
fun MoodSkinOverlay(
    mode: MoodMode,
    modifier: Modifier = Modifier,
) {
    if (mode == MoodMode.OFF) return

    val overlay = mode.toOverlay()

    Box(modifier = modifier.fillMaxSize()) {
        // Scanlines (rage, rage2)
        if (overlay.scanlines) {
            ScanlinesOverlay(color = if (mode == MoodMode.RAGE2) Color(0x22FF0000) else Color(0x22FF0000))
        }

        // Noise (rage, rage2)
        if (overlay.noise) {
            NoiseOverlay()
        }

        // Vignette (rage, rage2, desire)
        if (overlay.vignette) {
            VignetteOverlay()
        }

        // Stars (moonlight)
        if (overlay.stars) {
            StarsOverlay()
        }
    }
}

@Composable
private fun ScanlinesOverlay(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val lineSpacing = 4f
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f,
            )
            y += lineSpacing
        }
    }
}

@Composable
private fun NoiseOverlay() {
    val transition = rememberInfiniteTransition(label = "noise")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 256f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "noisePhase",
    )

    Canvas(Modifier.fillMaxSize()) {
        val cellSize = 4f
        val seed = phase.toInt()
        val rng = Random(seed.toLong())

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                val alpha = rng.nextFloat() * 0.08f
                if (alpha > 0.02f) {
                    drawRect(
                        color = Color(1f, 1f, 1f, alpha),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                    )
                }
                y += cellSize
            }
            x += cellSize
            rng.setSeed((seed + x.toInt()).toLong())
        }
    }
}

@Composable
private fun VignetteOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.6f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x00000000),
                    Color(0x44000000),
                    Color(0x88000000),
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius,
            ),
            radius = size.minDimension * 0.8f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}

@Composable
private fun StarsOverlay() {
    val density = LocalDensity.current.density
    val starData = remember {
        val rng = Random(42)
        List(60) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                size = 0.5f + rng.nextFloat() * 1.5f,
                alpha = 0.3f + rng.nextFloat() * 0.7f,
                twinkleSpeed = 0.5f + rng.nextFloat() * 2f,
                twinkleOffset = rng.nextFloat() * 6.28f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f * 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starTime",
    )

    Canvas(Modifier.fillMaxSize()) {
        starData.forEach { star ->
            val brightness = (sin(time * star.twinkleSpeed + star.twinkleOffset) + 1f) / 2f
            val alpha = star.alpha * (0.4f + 0.6f * brightness)
            drawCircle(
                color = Color(1f, 1f, 1f, alpha),
                radius = star.size * density,
                center = Offset(
                    star.x * size.width,
                    star.y * size.height,
                ),
            )
        }

        // Meteors
        val meteorT = (sin(time * 0.3f) + 1f) / 2f
        val mx = meteorT * size.width + size.width * 0.1f
        val my = meteorT * size.height * 0.3f
        val tailLen = 60f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0x00FFD700), Color(0x44FFD700), Color(0x88FFD700)),
                startX = mx - tailLen,
                endX = mx,
            ),
            start = Offset(mx - tailLen, my - tailLen * 0.3f),
            end = Offset(mx, my),
            strokeWidth = 1.5f,
        )
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float,
)