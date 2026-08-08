package me.rerere.rikkahub.data.ai.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodDetectorTest {
    @Test
    fun `strips whole tag and emits its event`() {
        val detector = MoodDetector()

        val result = detector.push("夜深了<mood>moonlight</mood>看星星")

        assertEquals("夜深了看星星", result.cleanedText)
        assertEquals(MoodMode.MOONLIGHT, result.moodEvent)
        assertEquals("", detector.endOfTurn())
    }

    @Test
    fun `does not leak a tag split immediately after mood name`() {
        val detector = MoodDetector()

        assertEquals("这是全屏皮肤测试。", detector.push("这是全屏皮肤测试。<mood").cleanedText)
        assertEquals("", detector.push(">moonlight").cleanedText)

        val resolved = detector.push("</mood>晚安")
        assertEquals("晚安", resolved.cleanedText)
        assertEquals(MoodMode.MOONLIGHT, resolved.moodEvent)
    }

    @Test
    fun `handles every character boundary of a mood tag`() {
        val tag = "<mood>rage</mood>"
        for (splitAt in 1 until tag.length) {
            val detector = MoodDetector()
            val first = detector.push("前缀" + tag.substring(0, splitAt))
            val second = detector.push(tag.substring(splitAt) + "后缀")

            assertEquals("split at $splitAt", "前缀", first.cleanedText)
            assertEquals("split at $splitAt", "后缀", second.cleanedText)
            assertEquals("split at $splitAt", MoodMode.RAGE, second.moodEvent)
        }
    }

    @Test
    fun `unknown tag is stripped without turning active skin off`() {
        val detector = MoodDetector()

        val result = detector.push("前<mood>not-a-mode</mood>后")

        assertEquals("前后", result.cleanedText)
        assertNull(result.moodEvent)
    }

    @Test
    fun `drops incomplete tag at end of turn`() {
        val detector = MoodDetector()

        assertEquals("可见文字", detector.push("可见文字<mood>moonlight").cleanedText)
        assertEquals("", detector.endOfTurn())
    }
}
