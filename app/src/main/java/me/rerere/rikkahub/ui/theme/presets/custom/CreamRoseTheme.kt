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
 * 奶油玫瑰 Cream Rose
 * 一个主题两态，跟随橘瓣的深浅切换（ColorMode.SYSTEM/LIGHT/DARK）。
 *
 * 源色卡：
 *   light  bg #F6F1EB / card #FFFCF8 / text #2E2118 / subtle #8C7B6B
 *          accent #B45A5F / accent-light #D4A0A3 / border #E8DFD4 / tint #F2D9D0
 *   dark   bg #1C1412 / card #2A201B / text #F0E6DA / subtle #9C8B7D
 *          accent #C97478 / accent-light #8B5558 / border #3D2F28 / tint #3A2428
 *
 * 层级关系照 IB 的做法拆：bg 是最底，card 是浮起的面，border 是最高一档容器，
 * 中间几档 surfaceContainer 按明度等距插值补齐，这样卡片叠卡片不会糊成一片。
 * subtle 直接当正文次级色对亮底只有约 3:1，压深到 #584A3D，原色转给 outline。
 */

val CreamRoseThemePreset by lazy {
    PresetTheme(
        id = "creamrose",
        name = {
            Text(stringResource(id = R.string.theme_name_creamrose))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFFB45A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2D9D0),
    onPrimaryContainer = Color(0xFF6A3B38),
    secondary = Color(0xFF8C7B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DFD4),
    onSecondaryContainer = Color(0xFF2E2118),
    tertiary = Color(0xFFD4A0A3),
    onTertiary = Color(0xFF3A1D1F),
    tertiaryContainer = Color(0xFFF3E2E0),
    onTertiaryContainer = Color(0xFF6A3B38),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF6F1EB),
    onBackground = Color(0xFF2E2118),
    surface = Color(0xFFF6F1EB),
    onSurface = Color(0xFF2E2118),
    surfaceVariant = Color(0xFFE8DFD4),
    onSurfaceVariant = Color(0xFF584A3D),
    outline = Color(0xFF8C7B6B),
    outlineVariant = Color(0xFFE8DFD4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2A201B),
    inverseOnSurface = Color(0xFFF0E6DA),
    inversePrimary = Color(0xFFC97478),
    surfaceDim = Color(0xFFE2D9CD),
    surfaceBright = Color(0xFFFFFCF8),
    surfaceContainerLowest = Color(0xFFFFFCF8),
    surfaceContainerLow = Color(0xFFF9F5EF),
    surfaceContainer = Color(0xFFF4EEE6),
    surfaceContainerHigh = Color(0xFFEEE6DD),
    surfaceContainerHighest = Color(0xFFE8DFD4),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFC97478),
    onPrimary = Color(0xFF201815),
    primaryContainer = Color(0xFF3A2428),
    onPrimaryContainer = Color(0xFFE0B8B3),
    secondary = Color(0xFF9C8B7D),
    onSecondary = Color(0xFF201815),
    secondaryContainer = Color(0xFF3D2F28),
    onSecondaryContainer = Color(0xFFF0E6DA),
    tertiary = Color(0xFF8B5558),
    onTertiary = Color(0xFFF0E6DA),
    tertiaryContainer = Color(0xFF4A2E30),
    onTertiaryContainer = Color(0xFFE0B8B3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1412),
    onBackground = Color(0xFFF0E6DA),
    surface = Color(0xFF1C1412),
    onSurface = Color(0xFFF0E6DA),
    surfaceVariant = Color(0xFF3D2F28),
    onSurfaceVariant = Color(0xFFCABDB0),
    outline = Color(0xFF9C8B7D),
    outlineVariant = Color(0xFF3D2F28),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF0E6DA),
    inverseOnSurface = Color(0xFF2E2118),
    inversePrimary = Color(0xFFB45A5F),
    surfaceDim = Color(0xFF1C1412),
    surfaceBright = Color(0xFF4E4037),
    surfaceContainerLowest = Color(0xFF150F0D),
    surfaceContainerLow = Color(0xFF221916),
    surfaceContainer = Color(0xFF2A201B),
    surfaceContainerHigh = Color(0xFF342822),
    surfaceContainerHighest = Color(0xFF3D2F28),
)
