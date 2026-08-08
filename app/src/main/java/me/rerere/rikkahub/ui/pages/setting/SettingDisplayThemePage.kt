/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.rikkahub.data.datastore.CustomThemeColors
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.ColorPickerDialog
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import me.rerere.rikkahub.ui.pages.setting.components.PresetThemeButtonGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayThemePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    var amoledDarkMode by rememberAmoledDarkMode()

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    var showCustomDialog by remember { mutableStateOf(false) }
    var isNightCustom by remember { mutableStateOf(false) }
    var editingColors by remember { mutableStateOf(CustomThemeColors()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf("") }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("\u4E3B\u9898\u5916\u89C2") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
                        headlineContent = { Text(stringResource(R.string.setting_page_dynamic_color)) },
                        supportingContent = { Text(stringResource(R.string.setting_page_dynamic_color_desc)) },
                        trailingContent = {
                            Switch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { vm.updateSettings(settings.copy(dynamicColor = it)) },
                            )
                        },
                        colors = CustomColors.listItemColors,
                    )

                    if (!settings.dynamicColor) {
                        // ── 日间主题 ──
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
                            Text("\u65E5\u95F4\u4E3B\u9898", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            if (settings.dayCustomColors?.hasAny() == true) {
                                TextButton(onClick = { vm.updateSettings(settings.copy(dayCustomColors = null)) }) {
                                    Text("\u91CD\u7F6E", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PresetThemeButtonGroup(
                                    themeId = settings.themeId,
                                    modifier = Modifier.weight(1f),
                                    onChangeTheme = { vm.updateSettings(settings.copy(themeId = it)) }
                                )
                                IconButton(onClick = {
                                    editingColors = settings.dayCustomColors ?: CustomThemeColors()
                                    isNightCustom = false
                                    showCustomDialog = true
                                }) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (settings.dayCustomColors?.hasAny() == true)
                                                    settings.dayCustomColors!!.primaryColorArgb?.toComposeColor() ?: MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                            .then(
                                                if (settings.dayCustomColors?.hasAny() == true)
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Icon(
                                            HugeIcons.Add01,
                                            contentDescription = "\u81EA\u5B9A\u4E49\u989C\u8272",
                                            tint = if (settings.dayCustomColors?.hasAny() == true) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 夜间主题 ──
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)) {
                            Text("\u591C\u95F4\u4E3B\u9898", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            if (settings.darkThemeId != null || settings.nightCustomColors?.hasAny() == true) {
                                TextButton(onClick = { vm.updateSettings(settings.copy(darkThemeId = null, nightCustomColors = null)) }) {
                                    Text("\u91CD\u7F6E\uFF08\u8DDF\u968F\u65E5\u95F4\uFF09")
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PresetThemeButtonGroup(
                                    themeId = settings.darkThemeId ?: settings.themeId,
                                    modifier = Modifier.weight(1f),
                                    onChangeTheme = { vm.updateSettings(settings.copy(darkThemeId = it)) }
                                )
                                IconButton(onClick = {
                                    editingColors = settings.nightCustomColors ?: CustomThemeColors()
                                    isNightCustom = true
                                    showCustomDialog = true
                                }) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (settings.nightCustomColors?.hasAny() == true)
                                                    settings.nightCustomColors!!.primaryColorArgb?.toComposeColor() ?: MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                            .then(
                                                if (settings.nightCustomColors?.hasAny() == true)
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Icon(
                                            HugeIcons.Add01,
                                            contentDescription = "\u81EA\u5B9A\u4E49\u989C\u8272",
                                            tint = if (settings.nightCustomColors?.hasAny() == true) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)),
                        headlineContent = { Text(stringResource(R.string.setting_display_page_amoled_dark_mode_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_amoled_dark_mode_desc)) },
                        trailingContent = {
                            Switch(
                                checked = amoledDarkMode,
                                onCheckedChange = { amoledDarkMode = it }
                            )
                        },
                        colors = CustomColors.listItemColors,
                    )
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(if (isNightCustom) "\u591C\u95F4\u81EA\u5B9A\u4E49 HCT \u989C\u8272" else "\u65E5\u95F4\u81EA\u5B9A\u4E49 HCT \u989C\u8272") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\u4E3B\u8272", Modifier.weight(1f))
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(editingColors.primaryColorArgb?.toComposeColor() ?: MaterialTheme.colorScheme.primary).clickable { colorPickerTarget = "primary"; showColorPicker = true })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\u4E8C\u7EA7\u8272", Modifier.weight(1f))
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(editingColors.secondaryColorArgb?.toComposeColor() ?: Color.Gray.copy(alpha = 0.3f)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { colorPickerTarget = "secondary"; showColorPicker = true })
                        if (editingColors.secondaryColorArgb != null) {
                            TextButton(onClick = { editingColors = editingColors.copy(secondaryColorArgb = null) }) { Text("\u91CD\u7F6E") }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\u4E09\u7EA7\u8272", Modifier.weight(1f))
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(editingColors.tertiaryColorArgb?.toComposeColor() ?: Color.Gray.copy(alpha = 0.3f)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { colorPickerTarget = "tertiary"; showColorPicker = true })
                        if (editingColors.tertiaryColorArgb != null) {
                            TextButton(onClick = { editingColors = editingColors.copy(tertiaryColorArgb = null) }) { Text("\u91CD\u7F6E") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                val saved = editingColors.let { if (it.hasAny()) it else null }
                if (isNightCustom) vm.updateSettings(settings.copy(nightCustomColors = saved))
                else vm.updateSettings(settings.copy(dayCustomColors = saved))
                showCustomDialog = false
            }) { Text("\u4FDD\u5B58") } },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("\u53D6\u6D88") } },
        )
    }

    if (showColorPicker) {
        val defaultColor = when (colorPickerTarget) {
            "primary" -> MaterialTheme.colorScheme.primary
            "secondary" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        }
        val initialColor = when (colorPickerTarget) {
            "primary" -> editingColors.primaryColorArgb
            "secondary" -> editingColors.secondaryColorArgb
            else -> editingColors.tertiaryColorArgb
        }
        ColorPickerDialog(
            initialColor = initialColor,
            defaultColor = defaultColor,
            onConfirm = { color ->
                editingColors = when (colorPickerTarget) {
                    "primary" -> editingColors.copy(primaryColorArgb = color ?: 0xFF6750A4L)
                    "secondary" -> editingColors.copy(secondaryColorArgb = color)
                    else -> editingColors.copy(tertiaryColorArgb = color)
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}
