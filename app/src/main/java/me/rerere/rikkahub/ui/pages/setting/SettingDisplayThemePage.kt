/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import me.rerere.rikkahub.ui.pages.setting.components.PresetThemeButtonGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingDisplayThemePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    var amoledDarkMode by rememberAmoledDarkMode()
    val navController = LocalNavController.current

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("主题外观") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
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
                    Text(
                        text = stringResource(R.string.setting_page_theme_setting),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                    )
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            ),
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
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        headlineContent = { Text("材质模式") },
                        supportingContent = {
                            Text(
                                when (displaySetting.materialMode) {
                                    DisplayMaterialMode.FOLLOW_THEME -> "跟随主题"
                                    DisplayMaterialMode.FLAT -> "平面"
                                    DisplayMaterialMode.TRANSLUCENT -> "轻透"
                                    DisplayMaterialMode.GLASS -> "玻璃"
                                }
                            )
                        },
                        trailingContent = {
                            Select(
                                options = DisplayMaterialMode.entries,
                                selectedOption = displaySetting.materialMode,
                                onOptionSelected = {
                                    updateDisplaySetting(displaySetting.copy(materialMode = it))
                                },
                                optionToString = {
                                    when (it) {
                                        DisplayMaterialMode.FOLLOW_THEME -> "跟随主题"
                                        DisplayMaterialMode.FLAT -> "平面"
                                        DisplayMaterialMode.TRANSLUCENT -> "轻透"
                                        DisplayMaterialMode.GLASS -> "玻璃"
                                    }
                                },
                                modifier = Modifier.width(150.dp),
                            )
                        },
                        colors = CustomColors.listItemColors,
                    )
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        headlineContent = { Text("界面实时渲染") },
                        supportingContent = { Text("在支持的设备上为玻璃界面启用实时背景渲染") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.interfaceRealtimeRendering,
                                onCheckedChange = {
                                    updateDisplaySetting(
                                        displaySetting.copy(interfaceRealtimeRendering = it)
                                    )
                                },
                            )
                        },
                        colors = CustomColors.listItemColors,
                    )
                    if (displaySetting.interfaceRealtimeRendering) {
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            headlineContent = { Text("聊天气泡实时模糊") },
                            supportingContent = { Text("为普通聊天气泡实时渲染背景模糊") },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.chatBubbleRealtimeBlur,
                                    onCheckedChange = {
                                        updateDisplaySetting(
                                            displaySetting.copy(chatBubbleRealtimeBlur = it)
                                        )
                                    },
                                )
                            },
                            colors = CustomColors.listItemColors,
                        )
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            headlineContent = { Text("液态玻璃气泡") },
                            supportingContent = { Text("iOS Liquid Glass 风格：实时模糊 + 边缘高光 + 顶部折射反光") },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.liquidGlassBubbles,
                                    onCheckedChange = {
                                        updateDisplaySetting(
                                            displaySetting.copy(liquidGlassBubbles = it)
                                        )
                                    },
                                )
                            },
                            colors = CustomColors.listItemColors,
                        )
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            headlineContent = { Text("模糊强度") },
                            supportingContent = {
                                Column {
                                    Slider(
                                        value = displaySetting.interfaceBlurRadius.coerceIn(3f, 20f),
                                        onValueChange = {
                                            updateDisplaySetting(
                                                displaySetting.copy(interfaceBlurRadius = it)
                                            )
                                        },
                                        valueRange = 3f..20f,
                                    )
                                    Text(
                                        text = "${displaySetting.interfaceBlurRadius.coerceIn(3f, 20f).roundToInt()} dp",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            },
                            colors = CustomColors.listItemColors,
                        )
                    }
                    // Custom theme management entry
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { navController.navigate(Screen.SettingTheme) },
                        headlineContent = { Text("自定义主题管理") },
                        supportingContent = { Text("HCT 色彩算法自定义主题") },
                        colors = CustomColors.listItemColors,
                    )
                    if (!settings.dynamicColor) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        ) {
                            PresetThemeButtonGroup(
                                themeId = settings.themeId,
                                modifier = Modifier.fillMaxWidth(),
                                onChangeTheme = { vm.updateSettings(settings.copy(themeId = it)) }
                            )
                        }
                    }
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 20.dp,
                                    bottomEnd = 20.dp
                                )
                            ),
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
}