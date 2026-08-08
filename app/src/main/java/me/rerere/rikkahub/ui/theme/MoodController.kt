/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * MoodController — Compose-side mood state and theme overrides.
 *
 * Usage:
 *   val mood = MoodController.current()
 *   // Read in composables:
 *   val bgColor = mood.backgroundColor
 *   // Set when mood event arrives:
 *   mood.set(MoodMode.RAGE)
 *
 * MoodController overrides MaterialTheme.colorScheme tokens when
 * a mood is active. Setting to OFF restores the user's own theme.
 */

package me.rerere.rikkahub.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.ai.mood.MoodMode

/**
 * Observable mood state for the chat UI.
 * Instantiate at ChatPage/Conversation level, pass down as needed.
 */
class MoodController(scope: CoroutineScope) {
    private val _mode = MutableStateFlow(MoodMode.OFF)
    val mode: StateFlow<MoodMode> = _mode.asStateFlow()

    /** Whether the chat wall/ambient layer should show mood effects */
    val isActive: Boolean get() = _mode.value != MoodMode.OFF

    fun set(newMode: MoodMode) {
        _mode.value = newMode
    }

    /** Reset to normal */
    fun clear() {
        _mode.value = MoodMode.OFF
    }

    companion object {
        /**
         * Create a MoodController in a remember block.
         */
        @Composable
        fun remember(): MoodController {
            val scope = rememberCoroutineScope()
            return remember(scope) { MoodController(scope) }
        }
    }
}

/**
 * Applies mood skin color overrides to a ColorScheme.
 * Returns the original scheme if mode is OFF.
 */
fun ColorScheme.applyMood(mode: MoodMode): ColorScheme {
    if (mode == MoodMode.OFF) return this

    return when (mode) {
        MoodMode.RAGE -> this.copy(
            background = Color(0xFF1A0000),
            surface = Color(0xFF2A0A0A),
            surfaceVariant = Color(0xFF3A1515),
            onBackground = Color(0xFFFF6666),
            onSurface = Color(0xFFFF8888),
            primary = Color(0xFFFF4444),
            error = Color(0xFFFF0000),
        )

        MoodMode.RAGE2 -> this.copy(
            background = Color(0xFFFFCCCC),
            surface = Color(0xFFFFAAAA),
            surfaceVariant = Color(0xFFFF8888),
            onBackground = Color(0xFF330000),
            onSurface = Color(0xFF220000),
            primary = Color(0xFFCC0000),
            error = Color(0xFFFF2222),
        )

        MoodMode.DESIRE -> this.copy(
            background = Color(0xFF1A0A0A),
            surface = Color(0xFF2A1515),
            surfaceVariant = Color(0xFF3A2020),
            onBackground = Color(0xFFFFCCCC),
            onSurface = Color(0xFFFFDDDD),
            primary = Color(0xFFFF6688),
        )

        MoodMode.VUOTO -> this.copy(
            background = Color(0xFF1A1A1A),
            surface = Color(0xFF2A2A2A),
            surfaceVariant = Color(0xFF3A3A3A),
            onBackground = Color(0xFF888888),
            onSurface = Color(0xFF999999),
            primary = Color(0xFF666666),
        )

        MoodMode.MOONLIGHT -> this.copy(
            background = Color(0xFF0A0A2A),
            surface = Color(0xFF151540),
            surfaceVariant = Color(0xFF202055),
            onBackground = Color(0xFFCCCCFF),
            onSurface = Color(0xFFDDDDFF),
            primary = Color(0xFFFFD700),
        )

        else -> this
    }
}

/**
 * Mood-driven background overlay draw specs.
 */
data class MoodOverlay(
    val scanlines: Boolean = false,
    val noise: Boolean = false,
    val stars: Boolean = false,
    val vignette: Boolean = false,
)

fun MoodMode.toOverlay(): MoodOverlay = when (this) {
    MoodMode.RAGE -> MoodOverlay(scanlines = true, noise = true, vignette = true)
    MoodMode.RAGE2 -> MoodOverlay(scanlines = true, noise = true, vignette = true)
    MoodMode.DESIRE -> MoodOverlay(vignette = true)
    MoodMode.VUOTO -> MoodOverlay()
    MoodMode.MOONLIGHT -> MoodOverlay(stars = true)
    MoodMode.OFF -> MoodOverlay()
}