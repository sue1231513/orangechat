/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * Nest Rose · 巢·玫瑰
 *
 * 源色卡 nest-palettes-v2：
 *   light  bg #F6F1EB / card #FFFCF8 / text #2E2118 / subtle #8C7B6B
 *          accent #B45A5F / accent-light #D4A0A3 / border #E8DFD4 / tint #F2D9D0
 *   dark   bg #1C1412 / card #2A201B / text #F0E6DA / subtle #9C8B7D
 *          accent #C97478 / accent-light #8B5558 / border #3D2F28 / tint #3A2428
 *
 * 色卡 subtle 对亮色底只有约 3:1，直接当 onSurfaceVariant 正文会糊，
 * 所以压深到 #6B5C4E，原色转给 outline。surfaceContainer 那几档按明度等距插值。
 */

val NestRoseThemePreset by lazy {
    PresetTheme(
        id = "nestrose",
        name = {
            Text(stringResource(id = R.string.theme_name_nestrose))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFFB45A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2D9D0),
    onPrimaryContainer = Color(0xFF4A1F22),
    secondary = Color(0xFF6B5C4E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DFD4),
    onSecondaryContainer = Color(0xFF2E2118),
    tertiary = Color(0xFFD4A0A3),
    onTertiary = Color(0xFF3A1D1F),
    tertiaryContainer = Color(0xFFF6E3E2),
    onTertiaryContainer = Color(0xFF4A1F22),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF6F1EB),
    onBackground = Color(0xFF2E2118),
    surface = Color(0xFFF6F1EB),
    onSurface = Color(0xFF2E2118),
    surfaceVariant = Color(0xFFE8DFD4),
    onSurfaceVariant = Color(0xFF6B5C4E),
    outline = Color(0xFF8C7B6B),
    outlineVariant = Color(0xFFE8DFD4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF33261F),
    inverseOnSurface = Color(0xFFF6F1EB),
    inversePrimary = Color(0xFFC97478),
    surfaceDim = Color(0xFFDED4C8),
    surfaceBright = Color(0xFFFFFCF8),
    surfaceContainerLowest = Color(0xFFFFFCF8),
    surfaceContainerLow = Color(0xFFFDF8F2),
    surfaceContainer = Color(0xFFF1EAE1),
    surfaceContainerHigh = Color(0xFFEBE3D8),
    surfaceContainerHighest = Color(0xFFE5DCD0),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFC97478),
    onPrimary = Color(0xFF3A1D1F),
    primaryContainer = Color(0xFF8B5558),
    onPrimaryContainer = Color(0xFFF2D9D0),
    secondary = Color(0xFF9C8B7D),
    onSecondary = Color(0xFF2A201B),
    secondaryContainer = Color(0xFF3D2F28),
    onSecondaryContainer = Color(0xFFF0E6DA),
    tertiary = Color(0xFFC9A0A0),
    onTertiary = Color(0xFF3A2428),
    tertiaryContainer = Color(0xFF3A2428),
    onTertiaryContainer = Color(0xFFF2D9D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1412),
    onBackground = Color(0xFFF0E6DA),
    surface = Color(0xFF1C1412),
    onSurface = Color(0xFFF0E6DA),
    surfaceVariant = Color(0xFF3D2F28),
    onSurfaceVariant = Color(0xFF9C8B7D),
    outline = Color(0xFF7A6A5C),
    outlineVariant = Color(0xFF3D2F28),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF0E6DA),
    inverseOnSurface = Color(0xFF33261F),
    inversePrimary = Color(0xFFB45A5F),
    surfaceDim = Color(0xFF1C1412),
    surfaceBright = Color(0xFF433530),
    surfaceContainerLowest = Color(0xFF150F0D),
    surfaceContainerLow = Color(0xFF211815),
    surfaceContainer = Color(0xFF2A201B),
    surfaceContainerHigh = Color(0xFF342822),
    surfaceContainerHighest = Color(0xFF3F312A),
)
