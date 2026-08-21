package me.rerere.rikkahub.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/**
 * Internal：从 IB 的亮/暗层级提炼出的低饱和暖灰主题。
 * 不依赖壁纸、毛玻璃或气泡特效，只负责稳定的颜色层级。
 */
val InternalThemePreset by lazy {
    PresetTheme(
        id = "internal",
        name = {
            Text(stringResource(id = R.string.theme_name_internal))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFFD3B891),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBE3D7),
    onPrimaryContainer = Color(0xFF24211C),
    secondary = Color(0xFF8A847B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E1D8),
    onSecondaryContainer = Color(0xFF24211C),
    tertiary = Color(0xFFB59B72),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2EEE6),
    onTertiaryContainer = Color(0xFF24211C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF7F4EF),
    onBackground = Color(0xFF24211C),
    surface = Color(0xFFF7F4EF),
    onSurface = Color(0xFF24211C),
    surfaceVariant = Color(0xFFEBE3D7),
    onSurfaceVariant = Color(0xFF6B6459),
    outline = Color(0xFFA39C90),
    outlineVariant = Color(0xFFD8D1C4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF302C26),
    inverseOnSurface = Color(0xFFF7F4EF),
    inversePrimary = Color(0xFFB59B72),
    surfaceDim = Color(0xFFDDD7CA),
    surfaceBright = Color(0xFFF7F4EF),
    surfaceContainerLowest = Color(0xFFFBF8F1),
    surfaceContainerLow = Color(0xFFF2EEE6),
    surfaceContainer = Color(0xFFEBE3D7),
    surfaceContainerHigh = Color(0xFFE4DCCE),
    surfaceContainerHighest = Color(0xFFDDD4C4),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFD3B891),
    onPrimary = Color(0xFF2A2418),
    primaryContainer = Color(0xFF453A28),
    onPrimaryContainer = Color(0xFFF0E8D8),
    secondary = Color(0xFFC4BCAC),
    onSecondary = Color(0xFF2A2418),
    secondaryContainer = Color(0xFF3B372E),
    onSecondaryContainer = Color(0xFFF0E8D8),
    tertiary = Color(0xFFCFC0A4),
    onTertiary = Color(0xFF2A2418),
    tertiaryContainer = Color(0xFF4A4133),
    onTertiaryContainer = Color(0xFFF0E8D8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141414),
    onBackground = Color(0xFFF0E8D8),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF0E8D8),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFC5BEB0),
    outline = Color(0xFF938C80),
    outlineVariant = Color(0xFF2A2A2A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF0E8D8),
    inverseOnSurface = Color(0xFF2A2724),
    inversePrimary = Color(0xFF8A7A5C),
    surfaceDim = Color(0xFF141414),
    surfaceBright = Color(0xFF3A3A3A),
    surfaceContainerLowest = Color(0xFF0C0C0C),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF212121),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF333333),
)
