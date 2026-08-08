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
import androidx.compose.material3.LargeFlexibleTopAppBar
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.rerere.rikkahub.ui.components.richtext.MoodletBadgePreviewPanel
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

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
        containerColor = CustomColors.topBarColors.containerColor
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
                        headlineContent = { Text("液态玻璃气泡") },
                        supportingContent = { Text("背景模糊、透明渐变与高光描边；关闭后恢复普通气泡") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.enableGlassBubbles,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(enableGlassBubbles = it))
                                }
                            )
                        }
                    )

                    item(
                        headlineContent = { Text("玻璃气泡预览") },
                        supportingContent = {
                            GlassBubblePreview(
                                enabled = displaySetting.enableGlassBubbles,
                                transparency = displaySetting.chatBubbleTransparency,
                            )
                        }
                    )
                    item(
                        headlineContent = { Text("情绪徽章预览") },
                        supportingContent = {
                            Column {
                                Text(
                                    text = "助手回复末尾输出 <silent mood=\"sleepy\" reason=\"...\"></silent> 时显示",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                MoodletBadgePreviewPanel()
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
                                    steps = 19,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.chatBubbleTransparency.toInt()}%")
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
                                    steps = 19,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${displaySetting.thinkingChainTransparency.toInt()}%")
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
                                    steps = 19,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${(displaySetting.drawerItemAlpha * 100).toInt()}%")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassBubblePreview(
    enabled: Boolean,
    transparency: Float,
) {
    val bubbleAlpha = 1f - transparency / 100f
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val userColor = MaterialTheme.colorScheme.primaryContainer
    val assistantColor = MaterialTheme.colorScheme.secondaryContainer
    val shape = RoundedCornerShape(18.dp)

    fun tintAlpha(): Float {
        val base = if (isDark) 0.26f else 0.20f
        return (base * (0.50f + bubbleAlpha * 0.55f)).coerceIn(0.12f, 0.40f)
    }

    val ta = if (enabled) tintAlpha() else bubbleAlpha.coerceIn(0.35f, 1f)
    val rimTop = if (isDark) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.78f)
    val rimBottom = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // assistant bubble (left)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .background(assistantColor.copy(alpha = ta), shape)
                    .then(
                        if (enabled) Modifier.border(
                            width = 1.15.dp,
                            brush = Brush.verticalGradient(listOf(rimTop, rimBottom)),
                            shape = shape,
                        ) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (enabled) "助手 · 液态玻璃" else "助手 · 普通气泡",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
            }
        }
        // user bubble (right)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .background(userColor.copy(alpha = ta), shape)
                    .then(
                        if (enabled) Modifier.border(
                            width = 1.15.dp,
                            brush = Brush.verticalGradient(listOf(rimTop, rimBottom)),
                            shape = shape,
                        ) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (enabled) "用户 · 液态玻璃" else "用户 · 普通气泡",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                )
            }
        }
        Text(
            text = if (enabled) {
                "深色/浅色都会加强描边与高光；可调上方透明度滑条看变化"
            } else {
                "开关打开后预览玻璃样式"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

