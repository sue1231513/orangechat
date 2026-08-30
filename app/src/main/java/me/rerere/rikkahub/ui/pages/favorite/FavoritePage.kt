/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.favorite

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Delete01
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.utils.navigateToChatPage
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

@Composable
fun FavoritePage(vm: FavoriteVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val favorites = vm.nodeFavorites.collectAsStateWithLifecycle().value
    val favoriteRemovedText = stringResource(R.string.favorite_page_removed)
    val undoText = stringResource(R.string.history_page_undo)
    val invalidBlockedText = stringResource(R.string.favorite_page_invalid_open_blocked)
    val noInvalidText = stringResource(R.string.favorite_page_no_invalid)
    val cleanedTextTemplate = stringResource(R.string.favorite_page_cleaned)
    val invalidCount = favorites.count { !it.conversationExists }

    // 每次回到页面重新核对一次有效性：对话可能在别处被删掉了
    LaunchedEffect(favorites.size) {
        vm.refreshValidity()
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    BackButton()
                },
                title = {
                    Text(stringResource(R.string.favorite_page_title))
                },
                actions = {
                    // 失效收藏批量清理：只在真有失效项时出现
                    if (invalidCount > 0) {
                        IconButton(
                            onClick = {
                                vm.cleanInvalidFavorites { removed ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (removed > 0) cleanedTextTemplate.format(removed) else noInvalidText
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = HugeIcons.Delete01,
                                contentDescription = stringResource(R.string.favorite_page_clean_invalid),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.favorite_page_no_favorites),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(favorites, key = { it.id }) { item ->
                SwipeableFavoriteCard(
                    item = item,
                    onClick = {
                        // 原对话已删除时不跳转，否则会落到一个空会话里
                        if (item.conversationExists) {
                            navigateToChatPage(navController, item.conversationId, nodeId = item.nodeId)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(invalidBlockedText) }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            val entity = vm.getEntityByRefKey(item.refKey) ?: return@launch
                            vm.removeFavorite(item.refKey)
                            val result = snackbarHostState.showSnackbar(
                                message = favoriteRemovedText,
                                actionLabel = undoText,
                                withDismissAction = true,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                vm.restoreFavorite(entity)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SwipeableFavoriteCard(
    item: NodeFavoriteListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
    )

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
            }

            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // 删除操作底层：只绘制右侧实际揭露区域，静止时不透出
            FavoriteDeleteBackground(
                revealPx = runCatching { dismissState.requireOffset() }.getOrDefault(0f),
                progress = dismissState.progress,
                shape = RoundedCornerShape(12.dp),
            )
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier,
    ) {
        FavoriteCard(
            item = item,
            onClick = onClick,
            onDelete = onDelete,
        )
    }
}

/**
 * 删除操作底层。
 *
 * 根据当前横向滑动偏移计算右侧揭露宽度，删除渐变只在揭露区域内绘制；
 * 垃圾桶 alpha 随滑动进度变化，静止时严格为 0。
 *
 * 背景 Row 已由 SwipeToDismissBox 以 matchParentSize 固定尺寸，
 * 因此这里使用 fillMaxSize 填满已有约束，不会影响父级测量。
 */
@Composable
private fun FavoriteDeleteBackground(
    revealPx: Float,
    progress: Float,
    shape: Shape,
) {
    val errorContainer = MaterialTheme.colorScheme.errorContainer

    // 删除渐变层：只绘制右侧实际揭露区域，静止时完全不透出
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .drawWithCache {
                // Brush 在 drawWithCache 中创建，绘制阶段只复用
                val revealWidth = (-revealPx).coerceIn(0f, size.width)
                val revealFactor = (revealWidth / size.width).coerceIn(0f, 1f)
                val revealLeft = size.width - revealWidth
                // 右到左衰减的危险色渐变（相对揭露区局部坐标）：
                // 左缘 0.16、中部 0.55、右缘 0.90
                val localGradient = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to errorContainer.copy(alpha = 0.16f * revealFactor),
                        0.5f to errorContainer.copy(alpha = 0.55f * revealFactor),
                        1f to errorContainer.copy(alpha = 0.9f * revealFactor),
                    )
                )
                onDrawBehind {
                    if (revealWidth > FAVORITE_TRASH_MIN_REVEAL_PX) {
                        // 矩形已限定在揭露区域内，无需额外裁剪
                        drawRect(
                            brush = localGradient,
                            topLeft = Offset(revealLeft, 0f),
                            size = Size(revealWidth, size.height),
                        )
                    }
                }
            }
    )

    // 垃圾桶图标层：右侧操作区居中，alpha 随滑动进度，静止时严格为 0
    val trashAlpha = if (revealPx < 0f) {
        ((progress - FAVORITE_TRASH_FADE_IN_START) / (1f - FAVORITE_TRASH_FADE_IN_START)).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = HugeIcons.Delete01,
            contentDescription = stringResource(R.string.assistant_page_remove),
            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = trashAlpha),
        )
    }
}

@Composable
private fun FavoriteCard(
    item: NodeFavoriteListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val materialMode = LocalMaterialMode.current
    val shape = RoundedCornerShape(12.dp)
    val baseColor = CustomColors.cardColorsOnSurfaceContainer.containerColor

    // 前景固定材质透明度，不读取/乘入普通界面透明度 Slider
    val backgroundColor = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT -> baseColor.copy(alpha = FAVORITE_CARD_TRANSLUCENT_ALPHA)
        DisplayMaterialMode.GLASS -> baseColor.copy(alpha = FAVORITE_CARD_GLASS_ALPHA)
        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> baseColor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
    ) {
        // 材质装饰层（matchParentSize，不参与测量）
        FavoriteCardMaterialLayers(
            materialMode = materialMode,
            shape = shape,
        )

        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.conversationTitle.ifBlank { stringResource(R.string.favorite_page_untitled_conversation) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val dateText = Instant.ofEpochMilli(item.createdAt).toLocalDateTime()
                    Text(
                        text = item.preview,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = dateText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        // 原对话已删除的收藏在这里明确标出，避免用户反复点击一个打不开的条目
                        if (!item.conversationExists) {
                            Text(
                                text = stringResource(R.string.favorite_page_invalid),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // 显式删除按钮：左滑手势在部分设备/布局下不易触发，这里提供确定入口
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = HugeIcons.Delete01,
                    contentDescription = stringResource(R.string.assistant_page_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.FavoriteCardMaterialLayers(
    materialMode: DisplayMaterialMode,
    shape: Shape,
) {
    val baseColor = CustomColors.cardColorsOnSurfaceContainer.containerColor
    val onSurface = MaterialTheme.colorScheme.onSurface

    when (materialMode) {
        DisplayMaterialMode.GLASS -> {
            // 静态玻璃高光/边框（沿用项目现有玻璃体系）
            val primary = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                onSurface.copy(alpha = 0.1f),
                                primary.copy(alpha = 0.07f),
                                Color.Transparent,
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                onSurface.copy(alpha = 0.13f),
                                onSurface.copy(alpha = 0.035f),
                                Color.Transparent,
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val outlinePath = (outline as? Outline.Generic)?.path
                        // 垂直衰减边框：顶部强、底部弱
                        val borderBrush = Brush.verticalGradient(
                            colors = listOf(
                                onSurface.copy(alpha = 0.2f),
                                onSurface.copy(alpha = 0.045f),
                                Color.Transparent,
                            )
                        )
                        onDrawBehind {
                            if (outlinePath != null) {
                                drawPath(
                                    path = outlinePath,
                                    brush = borderBrush,
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            }
                        }
                    }
            )
            // 右下角向左上衰减的材质遮罩：右下半区较明确，用于稳定覆盖删除底层；左半尽量透明
            // 不使用 Haze/PixelCopy/ImageBitmap，纯静态 Brush
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val coverBrush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.55f to Color.Transparent,
                                0.75f to baseColor.copy(alpha = FAVORITE_GLASS_COVER_MID),
                                1f to baseColor.copy(alpha = FAVORITE_GLASS_COVER_BOTTOM_RIGHT),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        )
                        onDrawBehind {
                            drawRoundRect(
                                brush = coverBrush,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.1f),
                            )
                        }
                    }
            )
        }

        DisplayMaterialMode.TRANSLUCENT -> {
            // 主题色方向性描边：顶部与左上强、右下弱，单条渐变描边，不形成完整双边框
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val outlinePath = (outline as? Outline.Generic)?.path
                        val borderBrush = Brush.linearGradient(
                            colors = listOf(
                                onSurface.copy(alpha = 0.18f),
                                onSurface.copy(alpha = 0.09f),
                                onSurface.copy(alpha = 0.03f),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        )
                        onDrawBehind {
                            if (outlinePath != null) {
                                drawPath(
                                    path = outlinePath,
                                    brush = borderBrush,
                                    style = Stroke(width = 1.dp.toPx()),
                                )
                            }
                        }
                    }
            )
        }

        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> Unit
    }
}

// 收藏前景固定透明度（与项目静态玻璃默认值一致：TRANSLUCENT 0.78 / GLASS 0.56）
private const val FAVORITE_CARD_TRANSLUCENT_ALPHA = 0.78f
private const val FAVORITE_CARD_GLASS_ALPHA = 0.56f

// GLASS 收藏项右下衰减遮罩
private const val FAVORITE_GLASS_COVER_BOTTOM_RIGHT = 0.45f
private const val FAVORITE_GLASS_COVER_MID = 0.22f

// 删除底层垃圾桶
private const val FAVORITE_TRASH_FADE_IN_START = 0.12f
private const val FAVORITE_TRASH_MIN_REVEAL_PX = 0.5f