/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * FxStyles — Compose SpanStyle implementations for Pelle d'Umore
 * inline text effects.
 *
 * Each effect maps to a SpanStyle (or SpanStyle + an animation signal).
 * For complex effects (shake, glitch, blur) the Compose-only approach
 * is limited — these are marked where a full port would need a custom
 * Text composable or Modifier. In practice the glow/big/whisper/red
 * effects cover 90%+ of real usage.
 */

package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import me.rerere.rikkahub.data.ai.mood.FxTag

/**
 * Appends ordinary text while restoring any Pelle FX placeholders as spans.
 * This runs after Markdown tokenization, so Markdown remains responsible for
 * its own syntax and only the protected effect body receives the FX style.
 */
fun androidx.compose.ui.text.AnnotatedString.Builder.appendFxText(
    text: String,
    tags: List<FxTag>,
    accent: Color,
    danger: Color,
    textColor: Color,
    textDim: Color,
) {
    if (tags.isEmpty()) {
        append(text)
        return
    }

    var cursor = 0
    val placeholder = Regex("\\uE010(\\d+)\\uE011")
    placeholder.findAll(text).forEach { match ->
        append(text, cursor, match.range.first)
        val tag = match.groupValues[1].toIntOrNull()?.let(tags::getOrNull)
        if (tag == null) {
            append(match.value)
        } else {
            withStyle(
                spanStyleForFxTag(
                    tag = tag,
                    accent = accent,
                    danger = danger,
                    textColor = textColor,
                    textDim = textDim,
                )
            ) {
                append(tag.inner)
            }
        }
        cursor = match.range.last + 1
    }
    append(text, cursor, text.length)
}

/**
 * Build a SpanStyle for an FX tag.
 * Falls back to neutral span for unknown tags.
 */
fun spanStyleForFxTag(
    tag: FxTag,
    accent: Color = Color(0xFF7C5CFC),
    danger: Color = Color(0xFFFF4444),
    textColor: Color = Color.White,
    textDim: Color = Color(0x99FFFFFF),
): SpanStyle {
    return when (tag.name) {
        "glow" -> SpanStyle(
            shadow = Shadow(
                color = accent.copy(alpha = 0.6f),
                blurRadius = 12f,
            ),
            color = accent,
            fontWeight = FontWeight.Medium,
        )

        "big" -> SpanStyle(
            fontSize = 22.sp
        )

        "huge" -> SpanStyle(
            fontSize = 30.sp
        )

        "whisper" -> SpanStyle(
            fontSize = 13.sp,
            color = textDim,
        )

        "red" -> SpanStyle(
            color = danger,
            fontWeight = FontWeight.SemiBold,
        )

        // shake / blur / glitch need animation or interaction support
        // beyond what SpanStyle alone can provide. For now they get a
        // visual approximation; a full port would add them as custom
        // InlineTextContent renderers.
        "shake" -> SpanStyle(
            color = textColor.copy(alpha = 0.9f),
            letterSpacing = 0.5.sp,
        )

        "blur" -> SpanStyle(
            color = textColor.copy(alpha = 0.3f),
        )

        "glitch" -> SpanStyle(
            color = textColor,
            letterSpacing = (-0.5).sp,
        )

        else -> SpanStyle()
    }
}

/**
 * Returns the inline content placeholder height for a given base font size.
 * Used for FX effects rendered as InlineTextContent (shake/blur/glitch)
 * when we need a custom composable.
 */
fun fxPlaceholder(fontSize: TextUnit) = InlineTextContent(
    placeholder = Placeholder(
        width = fontSize * 0.8f,
        height = fontSize,
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
    ),
    children = {
        // Custom composable renderer for complex FX
        // Extend this when adding shake/blur/glitch implementations
    }
)

/**
 * Debug: preview all FX styles rendered as text.
 */
@Composable
fun rememberFxExample(): String = remember {
    buildString {
        appendLine("[glow]This glows[/glow]")
        appendLine("[big]Big text[/big]")
        appendLine("[huge]HUGE text[/huge]")
        appendLine("[whisper]a whisper[/whisper]")
        appendLine("[red]warning[/red]")
        appendLine("[shake]trembling[/shake]")
        appendLine("[blur]hidden[/blur]")
        appendLine("[glitch]corrupted[/glitch]")
    }
}