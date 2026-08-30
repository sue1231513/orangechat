/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LocalMaterialMode

private val CardGroupCorner = 20.dp
private val CardGroupItemSpacing = 2.dp
private val CardGroupInnerCorner = 4.dp

data class CardGroupItem(
    val onClick: (() -> Unit)?,
    val modifier: Modifier,
    val overlineContent: (@Composable () -> Unit)?,
    val headlineContent: @Composable () -> Unit,
    val supportingContent: (@Composable () -> Unit)?,
    val leadingContent: (@Composable () -> Unit)?,
    val trailingContent: (@Composable () -> Unit)?,
    val colors: ListItemColors?,
)

class CardGroupScope {
    internal val items = mutableListOf<CardGroupItem>()

    fun item(
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        overlineContent: (@Composable () -> Unit)? = null,
        supportingContent: (@Composable () -> Unit)? = null,
        leadingContent: (@Composable () -> Unit)? = null,
        trailingContent: (@Composable () -> Unit)? = null,
        colors: ListItemColors? = null,
        headlineContent: @Composable () -> Unit,
    ) {
        items.add(
            CardGroupItem(
                onClick = onClick,
                modifier = modifier,
                overlineContent = overlineContent,
                headlineContent = headlineContent,
                supportingContent = supportingContent,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                colors = colors,
            )
        )
    }
}

@Composable
private fun CardGroupListItem(
    item: CardGroupItem,
    count: Int,
    index: Int,
) {
    val isFirst = index == 0
    val isLast = index == count - 1
    val interfaceSurfaceAlpha =
        (LocalDisplaySettings.current.interfaceSurfaceOpacity / 100f).coerceIn(0f, 0.95f)
    val materialMode = LocalMaterialMode.current
    val isGlass = materialMode == DisplayMaterialMode.GLASS
    val showBorder = materialMode == DisplayMaterialMode.TRANSLUCENT ||
        isGlass

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val topCorner by animateDpAsState(
        targetValue = if (isPressed || count == 1 || isFirst) CardGroupCorner else CardGroupInnerCorner,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val bottomCorner by animateDpAsState(
        targetValue = if (isPressed || count == 1 || isLast) CardGroupCorner else CardGroupInnerCorner,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    )
    val dynamicShape = RoundedCornerShape(
        topStart = topCorner,
        topEnd = topCorner,
        bottomStart = bottomCorner,
        bottomEnd = bottomCorner,
    )
    val baseColors = item.colors ?: CustomColors.listItemColors
    val containerColor = baseColors.containerColor.copy(alpha = interfaceSurfaceAlpha)
    val colors = baseColors.copy(
        containerColor = if (isGlass) Color.Transparent else containerColor,
    )
    val glassGradient = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.18f),
        0.45f to Color.White.copy(alpha = 0.08f),
        1f to Color.Transparent,
    )
    val glassBorder = Brush.linearGradient(
        0f to Color.White.copy(alpha = 0.42f),
        0.5f to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
        1f to Color.White.copy(alpha = 0.09f),
    )

    ListItem(
        headlineContent = item.headlineContent,
        modifier = item.modifier
            .fillMaxWidth()
            .clip(dynamicShape)
            .then(
                if (isGlass) {
                    Modifier
                        .background(color = containerColor, shape = dynamicShape)
                        .background(brush = glassGradient, shape = dynamicShape)
                } else {
                    Modifier
                }
            )
            .then(
                if (isGlass) {
                    Modifier.border(
                        border = BorderStroke(
                            width = 1.dp,
                            brush = glassBorder,
                        ),
                        shape = dynamicShape,
                    )
                } else if (showBorder) {
                    Modifier.border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                        ),
                        shape = dynamicShape,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (item.onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = item.onClick,
                    )
                } else Modifier
            ),
        overlineContent = item.overlineContent,
        supportingContent = item.supportingContent,
        leadingContent = item.leadingContent,
        trailingContent = item.trailingContent,
        colors = colors,
    )
}

@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    /**
     * 可折叠分组。传 null（默认）时保持原行为：标题不可点、内容始终展开。
     * 传入初始展开状态后，标题变为可点的折叠头，右侧显示项数与箭头。
     */
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable CardGroupScope.() -> Unit,
) {
    val scope = CardGroupScope()
    scope.content()

    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val showItems = !collapsible || expanded

    Column(modifier = modifier) {
        if (title != null) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                ProvideTextStyle(MaterialTheme.typography.titleSmallEmphasized) {
                    if (collapsible) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expanded = !expanded }
                                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) { title() }
                            Text(
                                text = scope.items.size.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(18.dp),
                            )
                        }
                    } else {
                        Box(modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)) {
                            title()
                        }
                    }
                }
            }
        }
        if (showItems) {
            val count = scope.items.size
            scope.items.fastForEachIndexed { index, item ->
                CardGroupListItem(item = item, count = count, index = index)
                if (index != count - 1) {
                    Spacer(modifier = Modifier.height(CardGroupItemSpacing))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CardGroupPreview() {
    CompositionLocalProvider(
        LocalDisplaySettings provides DisplaySetting(),
    ) {
        Scaffold(
            topBar = {
                LargeFlexibleTopAppBar(
                    title = {
                        Text("Card Group")
                    },
                    colors = CustomColors.topBarColors,
                )
            },
            containerColor = CustomColors.topBarColors.containerColor,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = { Text("About") },
                ) {
                    item(
                        headlineContent = { Text("第一项") },
                    )
                    item(
                        headlineContent = { Text("第二项") },
                        supportingContent = { Text("支持文本") },
                    )
                    item(
                        onClick = {},
                        headlineContent = { Text("第三项") },
                        trailingContent = { Text("→") },
                    )
                }
            }
        }
    }
}
