/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.search

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Refresh01
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.db.fts.MessageSearchResult
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.utils.navigateToChatPage
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.uuid.Uuid

@Composable
fun SearchPage(vm: SearchVM = koinViewModel()) {
    val navController = LocalNavController.current
    val focusRequester = remember { FocusRequester() }
    var showRebuildDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val materialMode = LocalMaterialMode.current
    val surfaceOpacity =
        (LocalDisplaySettings.current.interfaceSurfaceOpacity / 100f).coerceIn(0f, 1f)
    val searchFieldShape: Shape = RoundedCornerShape(50)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    if (showRebuildDialog) {
        AlertDialog(
            onDismissRequest = { showRebuildDialog = false },
            title = { Text(stringResource(R.string.search_page_rebuild_index)) },
            text = { Text(stringResource(R.string.search_page_rebuild_index_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebuildDialog = false
                        vm.rebuildIndex()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { BackButton() },
                title = { Text(stringResource(R.string.search_page_title)) },
                actions = {
                    IconButton(
                        onClick = { showRebuildDialog = true },
                        enabled = !vm.isRebuilding,
                    ) {
                        Icon(
                            HugeIcons.Refresh01,
                            contentDescription = stringResource(R.string.search_page_rebuild_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.onQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .clip(searchFieldShape)
                    .searchContainerMaterial(materialMode, searchFieldShape),
                placeholder = { Text(stringResource(R.string.search_page_placeholder)) },
                colors = OutlinedTextFieldDefaults.colors(
                    // 保留原始容器色 RGB（surface），仅将背景 alpha 设为 interfaceSurfaceOpacity
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = surfaceOpacity),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = surfaceOpacity),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = surfaceOpacity),
                    // 材质描边由 searchContainerMaterial 统一绘制，关闭 M3 默认边框避免双边框
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                shape = searchFieldShape,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { vm.search() }
                ),
            )

            Box(modifier = Modifier.weight(1f)) {
                if (vm.isLoading || vm.isRebuilding) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when {
                    vm.isRebuilding -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val (current, total) = vm.rebuildProgress
                            Text(
                                text = if (total > 0) stringResource(
                                    R.string.search_page_rebuilding,
                                    current,
                                    total
                                ) else stringResource(R.string.search_page_rebuilding_simple),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    vm.searchQuery.isBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_page_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    vm.results.isEmpty() && !vm.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_page_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(vm.results) { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = {
                                        navigateToChatPage(
                                            navController,
                                            chatId = Uuid.parse(result.conversationId),
                                            nodeId = Uuid.parse(result.nodeId),
                                        )
                                    }
                                )
                            }

                            // 云端搜索结果
                            if (vm.cloudResults.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "云端记录",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(vm.cloudResults) { result ->
                                    CloudSearchResultItem(
                                        result = result,
                                        query = vm.searchQuery,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: MessageSearchResult,
    onClick: () -> Unit,
) {
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val untitled = stringResource(R.string.search_page_untitled)
    val snippetText = buildAnnotatedString {
        val snippet = result.snippet
        var index = 0
        while (index < snippet.length) {
            val start = snippet.indexOf('[', index)
            if (start == -1) {
                append(snippet.substring(index))
                break
            }
            if (start > index) {
                append(snippet.substring(index, start))
            }
            val end = snippet.indexOf(']', start + 1)
            if (end == -1) {
                append(snippet.substring(start))
                break
            }
            val matched = snippet.substring(start + 1, end)
            withStyle(SpanStyle(background = highlightColor)) {
                append(matched)
            }
            index = end + 1
        }
    }
    val formattedTime = remember(result.updateAt) {
        result.updateAt.toLocalDateTime()
    }

    val materialMode = LocalMaterialMode.current
    val surfaceOpacity =
        (LocalDisplaySettings.current.interfaceSurfaceOpacity / 100f).coerceIn(0f, 1f)
    val shape = MaterialTheme.shapes.large
    val baseColor = CustomColors.listItemColors.containerColor
    // 背景 alpha 直接使用 interfaceSurfaceOpacity，不与其他透明度相乘
    val backgroundColor = baseColor.copy(alpha = surfaceOpacity)

    Box(modifier = Modifier.clip(shape)) {
        // 材质装饰层（matchParentSize，不参与测量），位于半透明底色之上、文字之下
        Box(
            modifier = Modifier
                .matchParentSize()
                .searchContainerMaterial(materialMode, shape)
        )
        Surface(
            onClick = onClick,
            color = backgroundColor,
            shape = shape,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = result.title.ifBlank { untitled },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = snippetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloudSearchResultItem(
    result: CloudSearchResult,
    query: String,
) {
    val highlightColor = MaterialTheme.colorScheme.tertiaryContainer
    val snippetText = buildAnnotatedString {
        val text = result.content.take(300)
        var index = 0
        while (index < text.length) {
            val matchIndex = text.indexOf(query, index, ignoreCase = true)
            if (matchIndex == -1) {
                append(text.substring(index))
                break
            }
            if (matchIndex > index) {
                append(text.substring(index, matchIndex))
            }
            withStyle(SpanStyle(background = highlightColor)) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }
            index = matchIndex + query.length
        }
    }

    val materialMode = LocalMaterialMode.current
    val surfaceOpacity = (LocalDisplaySettings.current.interfaceSurfaceOpacity / 100f).coerceIn(0f, 1f)
    val shape = MaterialTheme.shapes.large
    val backgroundColor = CustomColors.listItemColors.containerColor.copy(alpha = surfaceOpacity)

    Box(modifier = Modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .searchContainerMaterial(materialMode, shape)
        )
        Surface(
            color = backgroundColor,
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = result.role.ifBlank { "unknown" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = snippetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${result.source} · ${result.createdAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 搜索容器统一材质模板（FLAT / TRANSLUCENT / GLASS）。
 *
 * - 底板透明度由调用方通过容器颜色 alpha 设置（interfaceSurfaceOpacity），本函数不参与。
 * - FLAT：不附加任何层。
 * - TRANSLUCENT：仅 1dp 主题色描边（onSurface 0.18），无内部渐变。
 * - GLASS：白色内部渐变（0.18 → 0.08 → Transparent）+ 从左到右衰减的 1dp 渐变描边
 *   （0.55 → 0.20 → Transparent），描边受原 shape 裁剪。
 */
@Composable
private fun Modifier.searchContainerMaterial(
    materialMode: DisplayMaterialMode,
    shape: Shape,
): Modifier {
    val onSurface = MaterialTheme.colorScheme.onSurface
    return when (materialMode) {
        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> this

        DisplayMaterialMode.TRANSLUCENT -> this.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = (outline as? Outline.Generic)?.path
            val borderBrush = Brush.linearGradient(
                listOf(
                    onSurface.copy(alpha = 0.18f),
                    onSurface.copy(alpha = 0.18f),
                    onSurface.copy(alpha = 0.18f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            onDrawBehind {
                if (path != null) {
                    drawPath(path, brush = borderBrush, style = Stroke(width = 1.dp.toPx()))
                }
            }
        }

        DisplayMaterialMode.GLASS -> this.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = (outline as? Outline.Generic)?.path
            val glassFillBrush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val glassBorderBrush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            onDrawBehind {
                drawRect(brush = glassFillBrush)
                if (path != null) {
                    drawPath(path, brush = glassBorderBrush, style = Stroke(width = 1.dp.toPx()))
                }
            }
        }
    }
}