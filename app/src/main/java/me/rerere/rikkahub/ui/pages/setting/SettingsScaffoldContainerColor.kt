/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.rerere.rikkahub.ui.context.LocalSettingsBackgroundActive

@Composable
fun settingsScaffoldContainerColor(
    fallback: Color = MaterialTheme.colorScheme.background,
): Color = if (LocalSettingsBackgroundActive.current) Color.Transparent else fallback