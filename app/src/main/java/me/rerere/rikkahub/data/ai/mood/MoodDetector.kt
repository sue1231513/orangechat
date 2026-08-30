/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * MoodDetector — buffers the streaming token stream to detect <mood>…</mood>
 * tags that may arrive fragmented across chunk boundaries.
 */

package me.rerere.rikkahub.data.ai.mood

/**
 * Removes hidden mood tags from a streaming reply without ever emitting a
 * partially received tag into the chat bubble.
 *
 * A provider is free to split `<mood>moonlight</mood>` at any byte boundary.
 * Until a complete tag is available, its prefix and body stay in [buffer].
 * The returned text is therefore always new, user-visible text only.
 */
class MoodDetector {
    private val buffer = StringBuilder()

    /**
     * Adds one text delta and returns the safe visible text plus the most recent
     * valid mood event resolved by this delta.
     */
    fun push(chunk: String): Result {
        buffer.append(chunk)
        val visible = StringBuilder()
        var moodEvent: MoodMode? = null

        while (buffer.isNotEmpty()) {
            val tagStart = buffer.toString().indexOf(OPEN_PREFIX, ignoreCase = true)
            if (tagStart < 0) {
                val protectedSuffix = partialOpenTagSuffix(buffer.toString())
                val flushLength = buffer.length - protectedSuffix.length
                if (flushLength > 0) {
                    visible.append(buffer.substring(0, flushLength))
                    buffer.delete(0, flushLength)
                }
                break
            }

            // Normal prose before a possible tag can be emitted immediately.
            if (tagStart > 0) {
                visible.append(buffer.substring(0, tagStart))
                buffer.delete(0, tagStart)
                continue
            }

            // `<mood` is not yet enough to decide whether this is a tag.
            if (buffer.length < OPEN_TAG.length) break

            // `<moodful>` and similar prose/HTML are not Pelle tags. Release
            // the leading '<' and let the next loop flush the remaining text.
            if (!buffer.substring(0, OPEN_TAG.length).equals(OPEN_TAG, ignoreCase = true)) {
                visible.append(buffer[0])
                buffer.deleteCharAt(0)
                continue
            }

            val completeTag = completeTagRegex.find(buffer)
            if (completeTag == null) {
                // A real opening tag is present, but the closing tag has not
                // arrived yet. Keep the whole tag body hidden until it does.
                break
            }

            val value = completeTag.groupValues[1].trim().lowercase()
            val resolved = MoodMode.fromTag(value)
            if (resolved != MoodMode.OFF || value == MoodMode.OFF.tag) {
                moodEvent = resolved
            }
            // Unknown values are intentionally stripped but do not change the
            // currently active skin.
            buffer.delete(0, completeTag.range.last + 1)
        }

        return Result(cleanedText = visible.toString(), moodEvent = moodEvent)
    }

    /**
     * Drops an unfinished tag at the end of generation. [push] never leaves
     * ordinary visible text in the buffer, so no text is lost here.
     */
    fun endOfTurn(): String {
        val remaining = buffer.toString()
        buffer.clear()
        return if (remaining.isPotentialMoodFragment()) "" else remaining
    }

    data class Result(
        val cleanedText: String,
        val moodEvent: MoodMode?,
    )

    private fun partialOpenTagSuffix(text: String): String {
        return OPEN_PREFIXES.firstOrNull { text.endsWith(it, ignoreCase = true) }.orEmpty()
    }

    private fun String.isPotentialMoodFragment(): Boolean {
        return startsWith(OPEN_PREFIX, ignoreCase = true) ||
            OPEN_PREFIXES.any { equals(it, ignoreCase = true) }
    }

    private companion object {
        const val OPEN_PREFIX = "<mood"
        const val OPEN_TAG = "<mood>"
        val OPEN_PREFIXES = listOf("<mood", "<moo", "<mo", "<m", "<")
        val completeTagRegex = Regex(
            "^<mood>\\s*([^<\\s]+)\\s*</mood>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
    }
}
