/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.utils
 
/**
 * 移除字符串中的Markdown格式
 * @return 移除Markdown格式后的纯文本
 */
/**
 * 剥掉只供客户端解析的内部协议，不触碰正常正文。
 * 兼容 moodlet 的完整标签、自闭合标签，以及模型流式输出留下的末尾残缺标签。
 * 必须在「只朗读引号内容」的提取之前调用：reason 属性本身带引号，
 * 先提取会只留下 reason，之后再无法辨认它属于 silent 标签。
 */
fun String.stripTtsInternalMarkup(): String = this
    // 完整 moodlet：<silent ...>...</silent>
    .replace(Regex("(?is)<silent\\b[^>]*>.*?</silent\\s*>"), "")
    // 自闭合 moodlet：<silent ... />
    .replace(Regex("(?is)<silent\\b[^>]*/\\s*>"), "")
    // 连闭合尖括号都没来得及输出的流式残片；moodlet 约定只在回复末尾出现。
    .replace(Regex("(?is)\\s*<silent\\b.*$"), "")
    .replace(Regex("(?m)^\\s*\\[JUMP]\\s*$"), "")

fun String.stripMarkdown(): String {
    return this
        // 兜底：电话、工具等路径即使没预处理，也不能把内部协议念出去。
        .stripTtsInternalMarkup()
        // 移除代码块 (```...``` 和 `...`)
        .replace(Regex("```[\\s\\S]*?```|`[^`]*?`"), "")
        // 移除图片和链接，但保留其文本内容
        .replace(Regex("!?\\[([^\\]]+)\\]\\([^\\)]*\\)"), "$1")
        // 移除加粗和斜体 (先处理两个星号的)
        .replace(Regex("\\*\\*([^*]+?)\\*\\*"), "$1")
        .replace(Regex("\\*([^*]+?)\\*"), "$1")
        // 移除下划线
        .replace(Regex("__([^_]+?)__"), "$1")
        .replace(Regex("_([^_]+?)_"), "$1")
        // 移除删除线
        .replace(Regex("~~([^~]+?)~~"), "$1")
        // 移除标题标记 (多行模式)
        .replace(Regex("(?m)^#+\\s*"), "")
        // 移除列表标记 (多行模式)
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
        // 移除引用标记 (多行模式)
        .replace(Regex("(?m)^>\\s*"), "")
        // 移除水平分割线
        .replace(Regex("(?m)^(\\s*[-*_]){3,}\\s*$"), "")
        // 将多个换行符压缩，以保留段落
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
 
fun String.extractThinkingTitle(): String? {
    // 按行分割文本
    val lines = this.lines()
 
    // 从后往前查找最后一个符合条件的加粗文本行
    for (i in lines.indices.reversed()) {
        val line = lines[i].trim()
 
        // 检查是否为加粗格式且独占一整行
        val boldPattern = Regex("^\\*\\*(.+?)\\*\\*$")
        val match = boldPattern.find(line)
 
        if (match != null) {
            // 返回加粗标记内的文本内容
            return match.groupValues[1].trim().takeUnless { it.isBlank() }
        }
    }
 
    return null
}
 
// ============================================================
// 分句气泡: 按模型自己写的换行 (\n) 把一条助手回复拆成多个气泡片段
// ============================================================
 
/**
 * 围栏代码块 (```...```)，内部的换行必须原样保留，不能被当作气泡边界。
 * 复用与 MarkdownNew.kt / stripMarkdown 相同的匹配方式，保持项目内一致的约定。
 */
private val FENCED_CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```")
 
/**
 * 找出文本中「不可在其内部拆分气泡」的字符区间：
 * 1. 围栏代码块 (```...```) —— 内部换行是代码的一部分，拆开代码就碎了
 * 2. Markdown 表格 —— 连续 2 行及以上、每行都形如 "|...|" 的区块，拆开表格就散架了
 *
 * 除了这两种情况，其余所有换行完全由模型自己写、自己控制，不做任何额外的"智能分段"。
 *
 * 注意：这里假设换行符为单个 "\n"（LLM 输出的常见情况），不特殊处理 "\r\n"。
 */
private fun findProtectedRanges(text: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
 
    // 1. 围栏代码块
    FENCED_CODE_BLOCK_REGEX.findAll(text).forEach { ranges.add(it.range) }
 
    // 2. Markdown 表格块
    fun isTableRow(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.length > 1 && trimmed.startsWith("|") && trimmed.endsWith("|")
    }
 
    val lines = text.lines()
    var lineStartOffset = 0
    var tableStartLineIndex = -1
    var tableStartOffset = 0
 
    lines.forEachIndexed { index, line ->
        if (isTableRow(line)) {
            if (tableStartLineIndex == -1) {
                tableStartLineIndex = index
                tableStartOffset = lineStartOffset
            }
        } else {
            if (tableStartLineIndex != -1 && index - tableStartLineIndex >= 2) {
                // 表格至少需要 2 行 (表头 + 分隔行)，才当作一张完整表格保护起来
                ranges.add(tableStartOffset until (lineStartOffset - 1).coerceAtLeast(tableStartOffset))
            }
            tableStartLineIndex = -1
        }
        lineStartOffset += line.length + 1 // +1 计入被 lines() 消费掉的那个 '\n'
    }
    // 文本结尾仍处于表格中 (没有后续非表格行来"关闭"它)
    if (tableStartLineIndex != -1 && lines.size - tableStartLineIndex >= 2) {
        ranges.add(tableStartOffset until text.length)
    }
 
    return ranges
}
 
/**
 * 按模型自己写的换行符 (\n) 把一条助手回复拆分成多个"气泡"片段。
 *
 * - 拆分边界 100% 由模型输出的换行决定，客户端不做任何"按句子/按段落"的猜测式加工。
 * - 唯一的保护：围栏代码块与 Markdown 表格内部的换行不会被当作气泡边界（见 [findProtectedRanges]），
 *   因为这两种结构一旦被从中间拆开，视觉上是彻底损坏、不可读的，这是纯技术保护，不是风格判断。
 * - 拆分后每个片段会 trim 首尾空白；纯空白片段会被丢弃。
 * - 如果拆分结果为空 (例如整段文本都在保护区间内)，回退为把原文整体作为唯一一个片段返回，
 *   保证调用方任何时候都能拿到至少一个片段。
 */
fun String.splitIntoBubbleSegments(): List<String> {
    if (isBlank()) return listOf(this)
 
    val protectedRanges = findProtectedRanges(this)
    fun isProtectedIndex(index: Int) = protectedRanges.any { index in it }
 
    val segments = mutableListOf<String>()
    val current = StringBuilder()
    for (index in indices) {
        val ch = this[index]
        if (ch == '\n' && !isProtectedIndex(index)) {
            segments.add(current.toString())
            current.clear()
        } else {
            current.append(ch)
        }
    }
    segments.add(current.toString())
 
    val result = segments
        .map { it.trim('\r').trim() }
        .filter { it.isNotBlank() }
 
    return result.ifEmpty { listOf(this.trim()) }
}
 