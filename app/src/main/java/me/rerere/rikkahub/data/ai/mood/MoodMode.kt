/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 */

package me.rerere.rikkahub.data.ai.mood

/**
 * Mood skin modes.
 * Maps to the `<mood>` tag values the model emits.
 */
enum class MoodMode(val tag: String) {
    OFF("off"),
    RAGE("rage"),
    RAGE2("rage2"),
    DESIRE("desire"),
    VUOTO("vuoto"),
    MOONLIGHT("moonlight");

    companion object {
        private val map = entries.associateBy { it.tag }
        fun fromTag(tag: String): MoodMode = map[tag] ?: OFF
    }
}