/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.hooks.rememberSharedPreferenceBoolean
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayGeneralPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var sendSoundPath by remember(settings) { mutableStateOf(settings.displaySetting.sendSoundPath) }
    fun updateSendSoundPath(value: String) {
        sendSoundPath = value
        vm.updateSettings(settings.copy(displaySetting = settings.displaySetting.copy(sendSoundPath = value)))
    }

    var createNewConversationOnStart by rememberSharedPreferenceBoolean(
        "create_new_conversation_on_start",
        true
    )

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("通用设置") },
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
                    title = { Text(stringResource(R.string.setting_page_general_settings)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_create_new_conversation_on_start_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_create_new_conversation_on_start_desc)) },
                        trailingContent = {
                            Switch(
                                checked = createNewConversationOnStart,
                                onCheckedChange = { createNewConversationOnStart = it }
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("\u53D1\u9001\u97F3\u6548") },
                        supportingContent = { Text(if (sendSoundPath.isNotBlank()) "\u5DF2\u8BBE\u7F6E\u81EA\u5B9A\u4E49\u97F3\u6548" else "\u672A\u8BBE\u7F6E") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val soundPickerLauncher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.GetContent()
                                ) { uri ->
                                    uri?.let {
                                        val context = LocalContext.current
                                        val path = getPathFromUri(context, it)
                                        if (path != null) updateSendSoundPath(path)
                                    }
                                }
                                if (sendSoundPath.isNotBlank()) {
                                    Text(sendSoundPath, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    TextButton(onClick = { updateSendSoundPath("") }) { Text("\u79FB\u9664", color = MaterialTheme.colorScheme.error) }
                                }
                                TextButton(onClick = { soundPickerLauncher.launch("audio/mpeg") }) { Text(if (sendSoundPath.isNotBlank()) "\u66F4\u6362" else "\u5BFC\u5165 MP3") }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_show_updates_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_show_updates_desc)) },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.showUpdates,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(showUpdates = it))
                                }
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun getPathFromUri(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
            if (idx >= 0 && it.moveToFirst()) {
                it.getString(idx)
            } else {
                // For newer Android versions, copy to app private storage
                val input = context.contentResolver.openInputStream(uri) ?: return null
                val file = java.io.File(context.filesDir, "send_sound_${System.currentTimeMillis()}.mp3")
                input.use { inputStream ->
                    java.io.FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                file.absolutePath
            }
        }
    } catch (e: Exception) {
        null
    }
}
