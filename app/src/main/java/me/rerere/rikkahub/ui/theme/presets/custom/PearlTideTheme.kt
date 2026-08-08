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

val PearlTideThemePreset by lazy {
    PresetTheme(
        id = "pearltide",
        name = {
            Text(stringResource(id = R.string.theme_name_pearltide))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val primaryLight = Color(0xFF4C6378)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFDEE6ED)
private val onPrimaryContainerLight = Color(0xFF213445)
private val secondaryLight = Color(0xFF586674)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFDFE5EE)
private val onSecondaryContainerLight = Color(0xFF253447)
private val tertiaryLight = Color(0xFF6B5A7C)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFE5DEED)
private val onTertiaryContainerLight = Color(0xFF382D43)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFF7FAFC)
private val onBackgroundLight = Color(0xFF253447)
private val surfaceLight = Color(0xFFF7FAFC)
private val onSurfaceLight = Color(0xFF253447)
private val surfaceVariantLight = Color(0xFFECEEF3)
private val onSurfaceVariantLight = Color(0xFF5E7080)
private val outlineLight = Color(0xFF7E93A4)
private val outlineVariantLight = Color(0xFF788EA5)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF2F3337)
private val inverseOnSurfaceLight = Color(0xFFF1F2F4)
private val inversePrimaryLight = Color(0xFFB5C7D9)
private val surfaceDimLight = Color(0xFFDBDEE1)
private val surfaceBrightLight = Color(0xFFF7FAFC)
private val surfaceContainerLowestLight = Color(0xFFF4F8FA)
private val surfaceContainerLowLight = Color(0xFFEEF4FA)
private val surfaceContainerLight = Color(0xFFEEF0F1)
private val surfaceContainerHighLight = Color(0xFFE9EBEC)
private val surfaceContainerHighestLight = Color(0xFFE3E6E8)

private val primaryDark = Color(0xFFB6C7D8)
private val onPrimaryDark = Color(0xFF202E3C)
private val primaryContainerDark = Color(0xFF344D65)
private val onPrimaryContainerDark = Color(0xFFDFE6EC)
private val secondaryDark = Color(0xFFBFC7CF)
private val onSecondaryDark = Color(0xFF2C333A)
private val secondaryContainerDark = Color(0xFF434D56)
private val onSecondaryContainerDark = Color(0xFFE2E6E9)
private val tertiaryDark = Color(0xFFCCC1D7)
private val onTertiaryDark = Color(0xFF33283E)
private val tertiaryContainerDark = Color(0xFF4C3A5F)
private val onTertiaryContainerDark = Color(0xFFE5E0EB)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF121416)
private val onBackgroundDark = Color(0xFFE3E6E8)
private val surfaceDark = Color(0xFF121416)
private val onSurfaceDark = Color(0xFFE3E6E8)
private val surfaceVariantDark = Color(0xFF454D54)
private val onSurfaceVariantDark = Color(0xFFC8CCD0)
private val outlineDark = Color(0xFF9199A1)
private val outlineVariantDark = Color(0xFF454D54)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFE3E6E8)
private val inverseOnSurfaceDark = Color(0xFF2E3338)
private val inversePrimaryDark = Color(0xFF476785)
private val surfaceDimDark = Color(0xFF121416)
private val surfaceBrightDark = Color(0xFF373D43)
private val surfaceContainerLowestDark = Color(0xFF090A0B)
private val surfaceContainerLowDark = Color(0xFF191C1F)
private val surfaceContainerDark = Color(0xFF1E2124)
private val surfaceContainerHighDark = Color(0xFF292E32)
private val surfaceContainerHighestDark = Color(0xFF353B41)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)
