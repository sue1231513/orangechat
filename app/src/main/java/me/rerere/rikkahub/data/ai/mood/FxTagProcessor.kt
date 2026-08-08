/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * FxTagProcessor — extracts inline effect tags [glow]…[/glow] etc.
 * from raw AI text BEFORE markdown parsing.
 */

package me.rerere.rikkahub.data.ai.mood

/** An extracted inline effect tag. */
data class FxTag(
    val name: String,
    val inner: String,
)

/** Result of preprocessing: Markdown-safe text plus the effect metadata. */
data class FxExtractionResult(
    val text: String,
    val tags: List<FxTag>,
)

/**
 * Converts Pelle d'Umore inline tags to private-use placeholders before the
 * Markdown parser sees them. The placeholders are restored as styled Compose
 * spans by the native renderer after Markdown has been parsed.
 *
 * Code spans and fenced code blocks are deliberately left untouched: examples
 * such as ` [glow]example[/glow] ` must remain examples, not effects.
 */
object FxTagProcessor {
    private const val PH_OPEN = '\uE010'
    private const val PH_CLOSE = '\uE011'

    private val fxTagRegex = Regex(
        """\[(glow|big|huge|whisper|red|shake|blur|glitch)](.*?)\[/\1]""",
        RegexOption.DOT_MATCHES_ALL,
    )

    private val protectedMarkdownRegex = Regex(
        """```[\s\S]*?(?:```|$)|`[^`\n]*(?:`|$)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    /**
     * Extract all supported tags without allowing Markdown to interpret their
     * square brackets. This operation is intentionally idempotent: text that
     * already contains placeholders is left alone.
     */
    fun extract(text: String): FxExtractionResult {
        if (text.isBlank() || !text.contains('[')) return FxExtractionResult(text, emptyList())

        val tags = mutableListOf<FxTag>()
        val result = buildString(text.length) {
            var cursor = 0
            protectedMarkdownRegex.findAll(text).forEach { protected ->
                appendEffects(text.substring(cursor, protected.range.first), tags)
                append(protected.value)
                cursor = protected.range.last + 1
            }
            appendEffects(text.substring(cursor), tags)
        }
        return FxExtractionResult(result, tags)
    }

    /** Build the literal placeholder stored in the Markdown source. */
    fun placeholderFor(index: Int): String = "$PH_OPEN$index$PH_CLOSE"

    /**
     * Restores placeholders to inert custom HTML for the native HTML Markdown
     * path. The tag body is escaped because Pelle FX content is rendered as
     * literal inline text rather than parsed as another Markdown document.
     */
    fun restoreAsHtml(text: String, tags: List<FxTag>): String {
        if (tags.isEmpty()) return text
        return placeholderRegex.replace(text) { match ->
            val index = match.groupValues[1].toIntOrNull()
            val tag = index?.let(tags::getOrNull) ?: return@replace match.value
            "<pelle-fx name=\"${tag.name}\">${tag.inner.escapeHtml()}</pelle-fx>"
        }
    }

    /** Resolves a placeholder index, or null when [text] is not a placeholder. */
    fun tagIndexOf(text: String): Int? = placeholderRegex.matchEntire(text)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()

    private fun StringBuilder.appendEffects(segment: String, tags: MutableList<FxTag>) {
        var cursor = 0
        fxTagRegex.findAll(segment).forEach { match ->
            append(segment, cursor, match.range.first)
            val index = tags.size
            tags += FxTag(name = match.groupValues[1], inner = match.groupValues[2])
            append(placeholderFor(index))
            cursor = match.range.last + 1
        }
        append(segment, cursor, segment.length)
    }

    private fun String.escapeHtml(): String = buildString(length) {
        forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private val placeholderRegex = Regex("$PH_OPEN(\\d+)$PH_CLOSE")
}
