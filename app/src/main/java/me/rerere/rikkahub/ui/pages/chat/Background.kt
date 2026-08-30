/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.ui.components.ui.toComposeColor

/**
 * 聊天图片背景的共享视觉参数。
 *
 * AssistantBackground 与气泡实时背景片段共用同一套数值，确保两者逐像素一致：
 * - baseColor：页面基础底色（chatBackgroundColor 优先，否则取当前主题 background）；
 * - imageAlpha：背景纸不透明度（assistant.backgroundOpacity，coerceIn 0..1）；
 * - gradientTopAlpha / gradientBottomAlpha：图片上方垂直渐变遮罩的两个端点 alpha。
 */
data class ChatBackgroundVisuals(
    val baseColor: Color,
    val imageAlpha: Float,
    val gradientTopAlpha: Float = 0.2f,
    val gradientBottomAlpha: Float = 0.5f,
) {
    companion object {
        val Unspecified = ChatBackgroundVisuals(
            baseColor = Color.Unspecified,
            imageAlpha = 1f,
        )
    }
}

/**
 * 仅当聊天背景为图片时返回可共享的 Painter（Coil 缓存，不会重复发起请求）。
 * 无图片/纯色背景返回 null。
 */
@Composable
fun rememberChatBackgroundPainter(setting: Settings): Painter? {
    val assistant = setting.getCurrentAssistant()
    return if (assistant.background != null) {
        rememberAsyncImagePainter(model = assistant.background)
    } else {
        null
    }
}

/**
 * 计算与 AssistantBackground 完全一致的背景视觉参数（主题实时值）。
 */
@Composable
fun rememberChatBackgroundVisuals(setting: Settings): ChatBackgroundVisuals {
    val assistant = setting.getCurrentAssistant()
    val baseColor = setting.displaySetting.chatBackgroundColor?.let { it.toComposeColor() }
        ?: MaterialTheme.colorScheme.background
    return ChatBackgroundVisuals(
        baseColor = baseColor,
        imageAlpha = assistant.backgroundOpacity.coerceIn(0f, 1f),
    )
}

@Composable
fun AssistantBackground(
    setting: Settings,
    backgroundPainter: Painter? = null,
) {
    val assistant = setting.getCurrentAssistant()
    val sharedPainter = backgroundPainter ?: rememberChatBackgroundPainter(setting)
    val visuals = rememberChatBackgroundVisuals(setting)
    val chatBackgroundColor = setting.displaySetting.chatBackgroundColor?.let { it.toComposeColor() }

    when {
        assistant.background != null && sharedPainter != null -> {
            // 用户手动为助手设置的背景图，优先级最高
            Box {
                // 与原有 AsyncImage(ContentScale.Crop, Alignment.Center) 等价的绘制：
                // paint 默认 size = Size.Infinite（填满约束）、alignment = Center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(visuals.imageAlpha)
                        .paint(
                            painter = sharedPainter,
                            contentScale = ContentScale.Crop,
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    visuals.baseColor.copy(alpha = visuals.gradientTopAlpha),
                                    visuals.baseColor.copy(alpha = visuals.gradientBottomAlpha)
                                )
                            )
                        )
                )
            }
        }

        chatBackgroundColor != null -> {
            // 用户设置了自定义纯色背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(chatBackgroundColor)
            )
        }
    }
}
