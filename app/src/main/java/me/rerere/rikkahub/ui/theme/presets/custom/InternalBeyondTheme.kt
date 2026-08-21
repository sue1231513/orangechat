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
 * 内景 Internal Beyond
 * 直接搬 IB 单文件里那两套 CSS 变量：body（亮）与 body.theme-infernal（暗），
 * 在橘瓣这边合成一个主题的两态，跟随系统/手动的深浅切换。
 *
 * 源变量：
 *   light  bg #DFE9F6 / tx #0A1E42 / tx2 #3D5788 / tx3 #7D92B5
 *          acc #2A6BB0 / think #51678F / gold #C9A86A / danger #C05555
 *          line rgba(140,170,220,.32)
 *   dark   bg #141A2E / tx #EAF0FA / tx2 #CBD7EB / tx3 #A9BBD8
 *          acc #72A8D8 / think #8BA0C4 / gold #D0A44E / danger #E08A8A
 *          line rgba(165,188,230,.22)
 *
 * IB 的三档文字（tx / tx2 / tx3）对应 onSurface / onSurfaceVariant / outline，
 * think 那档蓝灰接 secondary，gold 接 tertiary。line 是半透明描边，
 * 这里按各自底色压成实色给 outlineVariant。
 * IB 靠 --glass / --panel 半透明层拉开层级，Compose 这边换成 surfaceContainer 五档实色。
 */

val InternalBeyondThemePreset by lazy {
    PresetTheme(
        id = "ibbeyond",
        name = {
            Text(stringResource(id = R.string.theme_name_ibbeyond))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF2A6BB0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E4F1),
    onPrimaryContainer = Color(0xFF1A4579),
    secondary = Color(0xFF51678F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EFF9),
    onSecondaryContainer = Color(0xFF0A1E42),
    tertiary = Color(0xFFC9A86A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0E6D4),
    onTertiaryContainer = Color(0xFF4A3A1E),
    error = Color(0xFFC05555),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFDFE9F6),
    onBackground = Color(0xFF0A1E42),
    surface = Color(0xFFDFE9F6),
    onSurface = Color(0xFF0A1E42),
    surfaceVariant = Color(0xFFD2DFF2),
    onSurfaceVariant = Color(0xFF3D5788),
    outline = Color(0xFF7D92B5),
    outlineVariant = Color(0xFFC4D5EE),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1E2842),
    inverseOnSurface = Color(0xFFEAF0FA),
    inversePrimary = Color(0xFF72A8D8),
    surfaceDim = Color(0xFFD2DFF2),
    surfaceBright = Color(0xFFF8FAFD),
    surfaceContainerLowest = Color(0xFFF8FAFD),
    surfaceContainerLow = Color(0xFFF1F5FB),
    surfaceContainer = Color(0xFFE8EFF9),
    surfaceContainerHigh = Color(0xFFE0E9F6),
    surfaceContainerHighest = Color(0xFFD2DFF2),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF72A8D8),
    onPrimary = Color(0xFF101728),
    primaryContainer = Color(0xFF24344F),
    onPrimaryContainer = Color(0xFFC7DDF3),
    secondary = Color(0xFF8BA0C4),
    onSecondary = Color(0xFF101728),
    secondaryContainer = Color(0xFF242F50),
    onSecondaryContainer = Color(0xFFEAF0FA),
    tertiary = Color(0xFFD0A44E),
    onTertiary = Color(0xFF2A2010),
    tertiaryContainer = Color(0xFF453519),
    onTertiaryContainer = Color(0xFFF0DDB0),
    error = Color(0xFFE08A8A),
    onError = Color(0xFF4A1416),
    errorContainer = Color(0xFF6B2426),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141A2E),
    onBackground = Color(0xFFEAF0FA),
    surface = Color(0xFF141A2E),
    onSurface = Color(0xFFEAF0FA),
    surfaceVariant = Color(0xFF2C3A62),
    onSurfaceVariant = Color(0xFFCBD7EB),
    outline = Color(0xFFA9BBD8),
    outlineVariant = Color(0xFF343E56),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEAF0FA),
    inverseOnSurface = Color(0xFF0A1E42),
    inversePrimary = Color(0xFF2A6BB0),
    surfaceDim = Color(0xFF141A2E),
    surfaceBright = Color(0xFF2C3A62),
    surfaceContainerLowest = Color(0xFF0D111E),
    surfaceContainerLow = Color(0xFF192138),
    surfaceContainer = Color(0xFF1E2842),
    surfaceContainerHigh = Color(0xFF242F50),
    surfaceContainerHighest = Color(0xFF2C3A62),
)
