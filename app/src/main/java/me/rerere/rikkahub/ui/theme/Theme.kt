/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.hooks.rememberUserSettingsState

private val ExtendLightColors = lightExtendColors()
private val ExtendDarkColors = darkExtendColors()
val LocalExtendColors = compositionLocalOf { ExtendLightColors }

val LocalDarkMode = compositionLocalOf { false }
val LocalMaterialMode = compositionLocalOf { DisplayMaterialMode.FLAT }
val LocalPopupSurfaceOpacity = compositionLocalOf { 90f }

@Composable
@ReadOnlyComposable
fun popupContainerColor(baseContainerColor: Color): Color {
    val popupAlpha =
        (LocalPopupSurfaceOpacity.current / 100f).coerceIn(0.6f, 1f)
    return baseContainerColor.copy(alpha = popupAlpha)
}

internal val GLASS_BACKGROUND_THEMES = setOf("pearltide", "harbor", "amberdock", "nestrose", "nestcaramel")

internal val THEME_BACKGROUND_SCRIM = mapOf(
    // Pair(topColor, bottomColor) — 顶部主题色浓覆盖, 底部反白托起输入框区域
    "pearltide"   to ScrimColors(0x80C8D8E4u, 0x60F7FAFCu, 0x38C8D8E4u),
    "harbor"      to ScrimColors(0x80A8B4C0u, 0x60F4F2EFu, 0x38A8B4C0u),
    "amberdock"   to ScrimColors(0x80D3B891u, 0x60F7F4EFu, 0x38D3B891u),
    "nestrose"    to ScrimColors(0x80D4A0A3u, 0x60F6F1EBu, 0x38D4A0A3u),
    "nestcaramel" to ScrimColors(0x80B89B82u, 0x60F5EFE6u, 0x38B89B82u),
)

data class ScrimColors(
    val top: UInt,        // 顶部主题色覆盖
    val bottom: UInt,     // 底部底色反白
    val accent: UInt,     // 强调色（用于中间过渡）
)

private val AMOLED_DARK_BACKGROUND = Color(0xFF000000)

@Serializable
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun RikkahubTheme(
    content: @Composable () -> Unit
) {
    val settings by rememberUserSettingsState()

    val colorMode by rememberColorMode()
    val darkTheme = when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }
    val amoledDarkMode by rememberAmoledDarkMode()

    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val theme = findThemeById(settings.themeId, settings.customThemes)
                ?: findPresetTheme(settings.themeId)
            theme.getColorScheme(dark = darkTheme)
        }
    }
    val colorSchemeConverted = remember(darkTheme, amoledDarkMode, colorScheme) {
        if (darkTheme && amoledDarkMode) {
            colorScheme.copy(
                background = AMOLED_DARK_BACKGROUND,
                surface = AMOLED_DARK_BACKGROUND,
            )
        } else {
            colorScheme
        }
    }
    val extendColors = if (darkTheme) ExtendDarkColors else ExtendLightColors

    // 颜色自定义覆盖
    val finalColorScheme = remember(
        colorSchemeConverted,
        settings.displaySetting.primaryColor,
        settings.displaySetting.globalTextColor,
        settings.displaySetting.interfaceSurfaceOpacity,
        settings.themeId,
    ) {
        var scheme = colorSchemeConverted
        settings.displaySetting.primaryColor?.let { pc ->
            val primaryColor = pc.toComposeColor()
            val luminance = 0.299f * primaryColor.red + 0.587f * primaryColor.green + 0.114f * primaryColor.blue
            val onPrimary = if (luminance > 0.5f) Color.Black else Color.White
            scheme = scheme.copy(
                primary = primaryColor,
                onPrimary = onPrimary,
            )
        }
        settings.displaySetting.globalTextColor?.let { gtc ->
            val textColor = gtc.toComposeColor()
            scheme = scheme.copy(
                onBackground = textColor,
                onSurface = textColor,
                onSurfaceVariant = textColor,
            )
        }
        val interfaceSurfaceAlpha =
            (settings.displaySetting.interfaceSurfaceOpacity / 100f).coerceIn(0f, 0.95f)
        // 直接覆盖容器 token 的 alpha，保留 RGB，避免与主题或组件已有 alpha 相乘。
        // background 保持不透明，使聊天页外层 Surface 能完整隔离导航根部的设置背景。
        scheme = scheme.copy(
            surface = scheme.surface.copy(alpha = interfaceSurfaceAlpha),
            surfaceBright = scheme.surfaceBright.copy(alpha = interfaceSurfaceAlpha),
            surfaceDim = scheme.surfaceDim.copy(alpha = interfaceSurfaceAlpha),
            surfaceContainerLowest = scheme.surfaceContainerLowest.copy(alpha = interfaceSurfaceAlpha),
            surfaceContainerLow = scheme.surfaceContainerLow.copy(alpha = interfaceSurfaceAlpha),
            surfaceContainer = scheme.surfaceContainer.copy(alpha = interfaceSurfaceAlpha),
            surfaceContainerHigh = scheme.surfaceContainerHigh.copy(alpha = interfaceSurfaceAlpha),
            surfaceContainerHighest = scheme.surfaceContainerHighest.copy(alpha = interfaceSurfaceAlpha),
            surfaceVariant = scheme.surfaceVariant.copy(alpha = interfaceSurfaceAlpha),
        )
        if (settings.themeId in GLASS_BACKGROUND_THEMES) {
            // 这些主题的根背景由 RouteActivity 绘制底图，允许背景透出形成玻璃质感。
            scheme = scheme.copy(background = scheme.background.copy(alpha = 0f))
        }
        scheme
    }

    val resolvedMaterialMode = when (settings.displaySetting.materialMode) {
        DisplayMaterialMode.FOLLOW_THEME -> {
            if (settings.themeId == "liquid_glass") DisplayMaterialMode.GLASS else DisplayMaterialMode.FLAT
        }
        else -> settings.displaySetting.materialMode
    }

    // 更新状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalMaterialMode provides resolvedMaterialMode,
        LocalPopupSurfaceOpacity provides settings.displaySetting.popupSurfaceOpacity,
        LocalExtendColors provides extendColors,
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}

val MaterialTheme.extendColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendColors.current