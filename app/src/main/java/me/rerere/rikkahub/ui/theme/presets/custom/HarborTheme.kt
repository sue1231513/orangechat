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

val HarborThemePreset by lazy {
    PresetTheme(
        id = "harbor",
        name = {
            Text(stringResource(id = R.string.theme_name_harbor))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF4A5D6C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAE6E4),
    onPrimaryContainer = Color(0xFF36404B),
    secondary = Color(0xFF5E6B78),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E0E3),
    onSecondaryContainer = Color(0xFF36404B),
    tertiary = Color(0xFF7A6B8A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE8E4),
    onTertiaryContainer = Color(0xFF36404B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF4F2EF),
    onBackground = Color(0xFF36404B),
    surface = Color(0xFFF4F2EF),
    onSurface = Color(0xFF36404B),
    surfaceVariant = Color(0xFFEAE6E4),
    onSurfaceVariant = Color(0xFF5E6B78),
    outline = Color(0xFF9197A0),
    outlineVariant = Color(0xFFB5B8BA),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFF1F2F4),
    inversePrimary = Color(0xFF8A9DAD),
    surfaceDim = Color(0xFFD6D8D7),
    surfaceBright = Color(0xFFF4F2EF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFEBE8),
    surfaceContainer = Color(0xFFEAE6E4),
    surfaceContainerHigh = Color(0xFFE4E1DF),
    surfaceContainerHighest = Color(0xFFDEDBD9),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF8A9DAD),
    onPrimary = Color(0xFF202E3C),
    primaryContainer = Color(0xFF344D65),
    onPrimaryContainer = Color(0xFFD0D6DC),
    secondary = Color(0xFFB4BEC6),
    onSecondary = Color(0xFF202E3C),
    secondaryContainer = Color(0xFF434D56),
    onSecondaryContainer = Color(0xFFD0D6DC),
    tertiary = Color(0xFFC6BFD0),
    onTertiary = Color(0xFF202E3C),
    tertiaryContainer = Color(0xFF4C3A5F),
    onTertiaryContainer = Color(0xFFD0D6DC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121416),
    onBackground = Color(0xFFE3E6E8),
    surface = Color(0xFF121416),
    onSurface = Color(0xFFE3E6E8),
    surfaceVariant = Color(0xFF454D54),
    onSurfaceVariant = Color(0xFFC8CCD0),
    outline = Color(0xFF9199A1),
    outlineVariant = Color(0xFF454D54),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E6E8),
    inverseOnSurface = Color(0xFF2E3338),
    inversePrimary = Color(0xFF4A5D6C),
    surfaceDim = Color(0xFF121416),
    surfaceBright = Color(0xFF373D43),
    surfaceContainerLowest = Color(0xFF0A0F11),
    surfaceContainerLow = Color(0xFF191C1F),
    surfaceContainer = Color(0xFF1E2124),
    surfaceContainerHigh = Color(0xFF292E32),
    surfaceContainerHighest = Color(0xFF353B41),
)
