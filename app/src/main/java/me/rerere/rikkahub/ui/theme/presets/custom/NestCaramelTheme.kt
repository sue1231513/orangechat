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
 * Nest Caramel · 巢·焦糖
 *
 * 源色卡 nest-palettes-v2：
 *   light  bg #F5EFE6 / card #FEFCF6 / text #2C2013 / subtle #8A7A65
 *          accent #7D5A44 / accent-light #B89B82 / border #E3D8C8 / tint #ECDDC8
 *   dark   bg #191410 / card #261F18 / text #EFE5D6 / subtle #9A8970
 *          accent #A07358 / accent-light #614A38 / border #382D23 / tint #352A1E
 *
 * 同 NestRose：亮色 onSurfaceVariant 由 subtle 压深到 #6B5C48，原色转给 outline。
 */

val NestCaramelThemePreset by lazy {
    PresetTheme(
        id = "nestcaramel",
        name = {
            Text(stringResource(id = R.string.theme_name_nestcaramel))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF7D5A44),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECDDC8),
    onPrimaryContainer = Color(0xFF2C2013),
    secondary = Color(0xFF6B5C48),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3D8C8),
    onSecondaryContainer = Color(0xFF2C2013),
    tertiary = Color(0xFFB89B82),
    onTertiary = Color(0xFF2C2013),
    tertiaryContainer = Color(0xFFF0E4D5),
    onTertiaryContainer = Color(0xFF3A2A1C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF5EFE6),
    onBackground = Color(0xFF2C2013),
    surface = Color(0xFFF5EFE6),
    onSurface = Color(0xFF2C2013),
    surfaceVariant = Color(0xFFE3D8C8),
    onSurfaceVariant = Color(0xFF6B5C48),
    outline = Color(0xFF8A7A65),
    outlineVariant = Color(0xFFE3D8C8),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF322619),
    inverseOnSurface = Color(0xFFF5EFE6),
    inversePrimary = Color(0xFFA07358),
    surfaceDim = Color(0xFFDBD1BE),
    surfaceBright = Color(0xFFFEFCF6),
    surfaceContainerLowest = Color(0xFFFEFCF6),
    surfaceContainerLow = Color(0xFFFBF6EC),
    surfaceContainer = Color(0xFFF0E9DD),
    surfaceContainerHigh = Color(0xFFEAE2D3),
    surfaceContainerHighest = Color(0xFFE4DACA),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFA07358),
    onPrimary = Color(0xFF2C2013),
    primaryContainer = Color(0xFF614A38),
    onPrimaryContainer = Color(0xFFEFE5D6),
    secondary = Color(0xFF9A8970),
    onSecondary = Color(0xFF191410),
    secondaryContainer = Color(0xFF382D23),
    onSecondaryContainer = Color(0xFFEFE5D6),
    tertiary = Color(0xFFB89B82),
    onTertiary = Color(0xFF352A1E),
    tertiaryContainer = Color(0xFF352A1E),
    onTertiaryContainer = Color(0xFFECDDC8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191410),
    onBackground = Color(0xFFEFE5D6),
    surface = Color(0xFF191410),
    onSurface = Color(0xFFEFE5D6),
    surfaceVariant = Color(0xFF382D23),
    onSurfaceVariant = Color(0xFF9A8970),
    outline = Color(0xFF776650),
    outlineVariant = Color(0xFF382D23),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEFE5D6),
    inverseOnSurface = Color(0xFF322619),
    inversePrimary = Color(0xFF7D5A44),
    surfaceDim = Color(0xFF191410),
    surfaceBright = Color(0xFF3E342B),
    surfaceContainerLowest = Color(0xFF120E0B),
    surfaceContainerLow = Color(0xFF1E1814),
    surfaceContainer = Color(0xFF261F18),
    surfaceContainerHigh = Color(0xFF302720),
    surfaceContainerHighest = Color(0xFF3A3028),
)
