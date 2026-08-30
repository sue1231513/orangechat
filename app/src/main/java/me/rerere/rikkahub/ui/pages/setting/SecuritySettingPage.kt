/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Alert01
import me.rerere.hugeicons.stroke.Shield02
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("安全设置") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding + PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CardGroup {
                item(
                    leadingContent = { Icon(HugeIcons.Alert01, null) },
                    headlineContent = { Text("强制确认工具调用") },
                    supportingContent = {
                        Text(
                            "开启后，每次 AI 调用任何工具前都需要你手动确认",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.forceConfirmToolCalls && !settings.autoApproveAllTools,
                            onCheckedChange = { enabled ->
                                vm.updateSettings(settings.copy(forceConfirmToolCalls = enabled, autoApproveAllTools = false))
                            }
                        )
                    }
                )
                item(
                    leadingContent = { Icon(HugeIcons.Alert01, null) },
                    headlineContent = { Text("自动批准所有工具调用") },
                    supportingContent = {
                        Text(
                            "懒人模式：AI 调用任何工具时自动允许，不再弹窗确认（⚠️ 降低安全性）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.autoApproveAllTools,
                            onCheckedChange = { enabled ->
                                vm.updateSettings(settings.copy(autoApproveAllTools = enabled, forceConfirmToolCalls = !enabled))
                            }
                        )
                    }
                )
                item(
                    leadingContent = { Icon(HugeIcons.Alert01, null) },
                    headlineContent = { Text("后台工作流拦截敏感工具") },
                    supportingContent = {
                        Text(
                            "开启后，由定时器/地理围栏等后台触发的工作流将禁止执行需要用户确认的工具（如短信、定位、截图等）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.workflowHeadlessBlockSensitive && !settings.autoApproveAllTools,
                            onCheckedChange = { enabled ->
                                vm.updateSettings(settings.copy(workflowHeadlessBlockSensitive = enabled, autoApproveAllTools = false))
                            }
                        )
                    }
                )
            }
        }
    }
}
