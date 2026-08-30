/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.ChatFontFamily
import me.rerere.rikkahub.data.datastore.ChatBubbleStyle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayMessagePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    val context = LocalContext.current

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val fontDir = remember { File(context.filesDir, "custom_fonts").apply { mkdirs() } }
    val fontPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val destFile = File(fontDir, "custom_font_${System.currentTimeMillis()}.ttf")
            context.contentResolver.openInputStream(it)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            updateDisplaySetting(
                displaySetting.copy(
                    chatFontFamily = ChatFontFamily.CUSTOM,
                    customFontPath = destFile.absolutePath
                )
            )
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("消息显示") },
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
            // 消息显示设置
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.setting_page_message_display_settings)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_top_bar_avatar_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_top_bar_avatar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showTopBarAvatar,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showTopBarAvatar = it))
                                }
                            )
                        },
                    )
                    // 顶栏头像接管后，逐条头像不再显示，这两个开关点了没有效果，直接隐藏
                    if (!displaySetting.showTopBarAvatar) {
                        item(
                            headlineContent = { Text(stringResource(R.string.setting_display_page_show_user_avatar_title)) },
                            supportingContent = { Text(stringResource(R.string.setting_display_page_show_user_avatar_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.showUserAvatar,
                                    onCheckedChange = {
                                        updateDisplaySetting(displaySetting.copy(showUserAvatar = it))
                                    }
                                )
                            },
                        )
                        item(
                            headlineContent = { Text(stringResource(R.string.setting_display_page_show_assistant_avatar_title)) },
                            supportingContent = { Text(stringResource(R.string.setting_display_page_show_assistant_avatar_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.showAssistantAvatar,
                                    onCheckedChange = {
                                        updateDisplaySetting(displaySetting.copy(showAssistantAvatar = it))
                                    }
                                )
                            },
                        )
                    }
                    // 列表里没有头像可作锚点时才有意义（含顶栏接管的情况）
                    if (!displaySetting.effectiveShowUserAvatar && !displaySetting.effectiveShowAssistantAvatar) {
                        item(
                            headlineContent = { Text(stringResource(R.string.setting_display_page_timeline_rail_title)) },
                            supportingContent = { Text(stringResource(R.string.setting_display_page_timeline_rail_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.useTimelineWhenNoAvatar,
                                    onCheckedChange = {
                                        updateDisplaySetting(displaySetting.copy(useTimelineWhenNoAvatar = it))
                                    }
                                )
                            },
                        )
                    }
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_user_bubble_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_user_bubble_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showUserBubble,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showUserBubble = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_assistant_bubble_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_assistant_bubble_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showAssistantBubble,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showAssistantBubble = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("消息气泡样式") },
                        supportingContent = {
                            Select(
                                options = ChatBubbleStyle.entries,
                                selectedOption = displaySetting.chatBubbleStyle,
                                onOptionSelected = { style ->
                                    updateDisplaySetting(displaySetting.copy(chatBubbleStyle = style))
                                },
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth(),
                                optionToString = { style ->
                                    when (style) {
                                        ChatBubbleStyle.RIKKA -> "Rikka 原生"
                                        ChatBubbleStyle.TELEGRAM -> "Telegram（时间与双勾）"
                                    }
                                }
                            )
                        }
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_model_name_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_model_name_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showModelName,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showModelName = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_date_below_name_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_date_below_name_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showDateBelowName,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showDateBelowName = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_token_usage_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_token_usage_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showTokenUsage,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showTokenUsage = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_thinking_content_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_thinking_content_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showThinkingContent,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showThinkingContent = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_auto_collapse_thinking_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_auto_collapse_thinking_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.autoCloseThinking,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(autoCloseThinking = it))
                                }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_enable_latex_rendering_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_enable_latex_rendering_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.enableLatexRendering,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(enableLatexRendering = it))
                                }
                            )
                        },
                    )
                    val chatFontFamilyOptions = listOf(
                        ChatFontFamily.DEFAULT to stringResource(R.string.setting_display_page_chat_font_family_default),
                        ChatFontFamily.SERIF to stringResource(R.string.setting_display_page_chat_font_family_serif),
                        ChatFontFamily.MONOSPACE to stringResource(R.string.setting_display_page_chat_font_family_monospace),
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_chat_font_family_title)) },
                        supportingContent = {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                chatFontFamilyOptions.forEachIndexed { index, (family, label) ->
                                    SegmentedButton(
                                        selected = displaySetting.chatFontFamily == family,
                                        onClick = { updateDisplaySetting(displaySetting.copy(chatFontFamily = family)) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index,
                                            chatFontFamilyOptions.size
                                        ),
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = when (family) {
                                                ChatFontFamily.DEFAULT -> FontFamily.Default
                                                ChatFontFamily.SERIF -> FontFamily.Serif
                                                ChatFontFamily.MONOSPACE -> FontFamily.Monospace
                                                ChatFontFamily.CUSTOM -> FontFamily.Default
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_font_size_title)) },
                        supportingContent = {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Slider(
                                        value = displaySetting.fontSizeRatio,
                                        onValueChange = {
                                            updateDisplaySetting(displaySetting.copy(fontSizeRatio = it))
                                        },
                                        valueRange = 0.5f..2f,
                                        steps = 11,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(text = "${(displaySetting.fontSizeRatio * 100).toInt()}%")
                                }
                                MarkdownBlock(
                                    content = stringResource(R.string.setting_display_page_font_size_preview),
                                    style = LocalTextStyle.current.copy(
                                        fontSize = LocalTextStyle.current.fontSize * displaySetting.fontSizeRatio,
                                        lineHeight = LocalTextStyle.current.lineHeight * displaySetting.fontSizeRatio,
                                        fontFamily = when (displaySetting.chatFontFamily) {
                                            ChatFontFamily.DEFAULT -> FontFamily.Default
                                            ChatFontFamily.SERIF -> FontFamily.Serif
                                            ChatFontFamily.MONOSPACE -> FontFamily.Monospace
                                            ChatFontFamily.CUSTOM -> FontFamily.Default
                                        }
                                    )
                                )
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("思维链字体大小") },
                        supportingContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Slider(
                                    value = displaySetting.thinkingFontSizeRatio,
                                    onValueChange = {
                                        updateDisplaySetting(displaySetting.copy(thinkingFontSizeRatio = it))
                                    },
                                    valueRange = 0.5f..2.0f,
                                    steps = 5,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(text = "${(displaySetting.thinkingFontSizeRatio * 100).toInt()}%")
                            }
                        }
                    )
                }
            }

            // 自定义字体
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("自定义字体") },
                ) {
                    item(
                        headlineContent = { Text("导入自定义字体") },
                        supportingContent = {
                            Text(
                                if (displaySetting.customFontPath.isNotBlank() && File(displaySetting.customFontPath).exists())
                                    "当前字体: ${File(displaySetting.customFontPath).name}"
                                else "支持 .ttf / .otf 字体文件"
                            )
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (displaySetting.customFontPath.isNotBlank()) {
                                    TextButton(onClick = {
                                        File(displaySetting.customFontPath).delete()
                                        updateDisplaySetting(
                                            displaySetting.copy(
                                                customFontPath = "",
                                                chatFontFamily = ChatFontFamily.DEFAULT
                                            )
                                        )
                                    }) { Text("清除") }
                                }
                                TextButton(onClick = {
                                    fontPickerLauncher.launch(arrayOf("*/*"))
                                }) { Text("选择字体") }
                            }
                        },
                    )
                    if (displaySetting.customFontPath.isNotBlank() && File(displaySetting.customFontPath).exists()) {
                        item(
                            headlineContent = { Text("字体预览") },
                            supportingContent = {
                                val customFont = remember(displaySetting.customFontPath) {
                                    runCatching { FontFamily(Font(File(displaySetting.customFontPath))) }
                                        .getOrDefault(FontFamily.Default)
                                }
                                Text(
                                    text = "The quick brown fox jumps over the lazy dog. 你好世界！1234567890",
                                    fontFamily = customFont,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}