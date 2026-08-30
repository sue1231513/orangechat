/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.ColorPickerDialog
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayColorPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showChatTextColorPicker by remember { mutableStateOf(false) }
    var showGlobalTextColorPicker by remember { mutableStateOf(false) }
    var showUserBubbleColorPicker by remember { mutableStateOf(false) }
    var showAssistantBubbleColorPicker by remember { mutableStateOf(false) }
    var showThinkingBubbleColorPicker by remember { mutableStateOf(false) }
    var showChatBackgroundColorPicker by remember { mutableStateOf(false) }
    var showPrimaryColorPicker by remember { mutableStateOf(false) }
    var showInputFieldColorPicker by remember { mutableStateOf(false) }

    if (showChatTextColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.chatTextColor,
            defaultColor = MaterialTheme.colorScheme.onSurface,
            onConfirm = { updateDisplaySetting(displaySetting.copy(chatTextColor = it)) },
            onDismiss = { showChatTextColorPicker = false }
        )
    }
    if (showGlobalTextColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.globalTextColor,
            defaultColor = MaterialTheme.colorScheme.background,
            onConfirm = { updateDisplaySetting(displaySetting.copy(globalTextColor = it)) },
            onDismiss = { showGlobalTextColorPicker = false }
        )
    }
    if (showUserBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.userBubbleColor,
            defaultColor = MaterialTheme.colorScheme.secondaryContainer,
            onConfirm = { updateDisplaySetting(displaySetting.copy(userBubbleColor = it)) },
            onDismiss = { showUserBubbleColorPicker = false }
        )
    }
    if (showAssistantBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.assistantBubbleColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onConfirm = { updateDisplaySetting(displaySetting.copy(assistantBubbleColor = it)) },
            onDismiss = { showAssistantBubbleColorPicker = false }
        )
    }
    if (showThinkingBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.thinkingBubbleColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onConfirm = { updateDisplaySetting(displaySetting.copy(thinkingBubbleColor = it)) },
            onDismiss = { showThinkingBubbleColorPicker = false }
        )
    }
    if (showChatBackgroundColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.chatBackgroundColor,
            defaultColor = MaterialTheme.colorScheme.background,
            onConfirm = { updateDisplaySetting(displaySetting.copy(chatBackgroundColor = it)) },
            onDismiss = { showChatBackgroundColorPicker = false }
        )
    }
    if (showPrimaryColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.primaryColor,
            defaultColor = MaterialTheme.colorScheme.primary,
            onConfirm = { updateDisplaySetting(displaySetting.copy(primaryColor = it)) },
            onDismiss = { showPrimaryColorPicker = false }
        )
    }
    if (showInputFieldColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.inputFieldColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onConfirm = { updateDisplaySetting(displaySetting.copy(inputFieldColor = it)) },
            onDismiss = { showInputFieldColorPicker = false }
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("颜色自定义") },
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
                    title = { Text("颜色自定义") },
                ) {
                    item(
                        headlineContent = { Text("聊天正文颜色") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.chatTextColor?.let { it.toComposeColor() } ?: Color.Gray,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showChatTextColorPicker = true }) { Text("自定义") }
                                if (displaySetting.chatTextColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(chatTextColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("全局字体颜色") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.globalTextColor?.let { it.toComposeColor() } ?: Color.Gray,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showGlobalTextColorPicker = true }) { Text("自定义") }
                                if (displaySetting.globalTextColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(globalTextColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("用户气泡颜色") },
                        supportingContent = { Text("自定义用户消息气泡背景色") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.userBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.secondaryContainer,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showUserBubbleColorPicker = true }) { Text("自定义") }
                                if (displaySetting.userBubbleColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(userBubbleColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("AI气泡颜色") },
                        supportingContent = { Text("自定义AI消息气泡背景色") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.assistantBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showAssistantBubbleColorPicker = true }) { Text("自定义") }
                                if (displaySetting.assistantBubbleColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(assistantBubbleColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("思维链气泡颜色") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.thinkingBubbleColor?.let { it.toComposeColor() } ?: Color.Gray,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showThinkingBubbleColorPicker = true }) { Text("自定义") }
                                if (displaySetting.thinkingBubbleColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(thinkingBubbleColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("聊天背景色") },
                        supportingContent = { Text("有背景图时图片优先") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.chatBackgroundColor?.let { it.toComposeColor() } ?: Color.Gray,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showChatBackgroundColorPicker = true }) { Text("自定义") }
                                if (displaySetting.chatBackgroundColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(chatBackgroundColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("主色调（按钮/链接）") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.primaryColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showPrimaryColorPicker = true }) { Text("自定义") }
                                if (displaySetting.primaryColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(primaryColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text("输入框背景颜色") },
                        supportingContent = { Text("有背景图时图片优先") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            displaySetting.inputFieldColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.surfaceContainerLowest,
                                            CircleShape
                                        )
                                )
                                TextButton(onClick = { showInputFieldColorPicker = true }) { Text("自定义") }
                                if (displaySetting.inputFieldColor != null) {
                                    TextButton(onClick = { updateDisplaySetting(displaySetting.copy(inputFieldColor = null)) }) { Text("重置") }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}