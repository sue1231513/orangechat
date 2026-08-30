/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode

@Composable
fun materialModeBorderStroke(): BorderStroke? = if (LocalMaterialMode.current.hasMaterialBorder) {
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
    )
} else {
    null
}

@Composable
fun Modifier.materialTopAppBarDivider(): Modifier {
    if (!LocalMaterialMode.current.hasMaterialBorder) return this

    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    return drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2f
        drawLine(
            color = dividerColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
}

private val DisplayMaterialMode.hasMaterialBorder: Boolean
    get() = this == DisplayMaterialMode.TRANSLUCENT || this == DisplayMaterialMode.GLASS