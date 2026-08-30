/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingDisplayTransparencyPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("透明度设置") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("透明度设置") },
                ) {
                    item(
                        headlineContent = { Text("界面材质不透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.interfaceSurfaceOpacity,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(interfaceSurfaceOpacity = it))
                                    },
                                    valueRange = 0f..95f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.interfaceSurfaceOpacity.roundToInt()}%")
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("弹出界面不透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.popupSurfaceOpacity.coerceIn(60f, 100f),
                                    onValueChange = {
                                        updateDisplaySetting(
                                            displaySetting.copy(
                                                popupSurfaceOpacity = it.coerceIn(60f, 100f)
                                            )
                                        )
                                    },
                                    valueRange = 60f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${
                                        displaySetting.popupSurfaceOpacity.coerceIn(60f, 100f).roundToInt()
                                    }%"
                                )
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("侧边栏整体不透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.drawerSurfaceOpacity,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(drawerSurfaceOpacity = it))
                                    },
                                    valueRange = 60f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.drawerSurfaceOpacity.roundToInt()}%")
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("聊天气泡透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.chatBubbleTransparency,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(chatBubbleTransparency = it))
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.chatBubbleTransparency.roundToInt()}%")
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("思维链透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.thinkingChainTransparency,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(thinkingChainTransparency = it))
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.thinkingChainTransparency.roundToInt()}%")
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("侧边栏元素透明度") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.drawerItemAlpha,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(drawerItemAlpha = it))
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${(displaySetting.drawerItemAlpha * 100).roundToInt()}%")
                            }
                        }
                    )
                }
            }
        }
    }
}