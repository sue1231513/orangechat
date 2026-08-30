/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.rerere.ai.core.MessageRole

/**
 * 无头像模式下的左侧时间线。
 *
 * 头像关闭后消息整体左移、行首失去对齐锚点，读起来比有头像更乱。
 * 这里用一条贯穿细线加每轮一个节点补回纵向锚点。
 *
 * 实现上不新增布局层级：直接在内容区左侧内边距里 drawBehind，
 * 这样线的高度天然等于整条消息的实际高度，不需要额外测量。
 */
@Composable
fun MessageTimelineRailIfNeeded(
    enabled: Boolean,
    role: MessageRole,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = when (role) {
        MessageRole.USER -> MaterialTheme.colorScheme.primary
        MessageRole.ASSISTANT -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val centerX = TIMELINE_RAIL_WIDTH.toPx() / 2f
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = TIMELINE_LINE_WIDTH.toPx(),
                )
                drawCircle(
                    color = nodeColor,
                    radius = TIMELINE_NODE_RADIUS.toPx(),
                    center = Offset(centerX, TIMELINE_NODE_TOP.toPx()),
                )
            }
            .padding(start = TIMELINE_RAIL_WIDTH + TIMELINE_CONTENT_GAP)
    ) {
        content()
    }
}

private val TIMELINE_RAIL_WIDTH = 14.dp
private val TIMELINE_CONTENT_GAP = 6.dp
private val TIMELINE_LINE_WIDTH = 1.dp
private val TIMELINE_NODE_RADIUS = 3.dp
private val TIMELINE_NODE_TOP = 14.dp
