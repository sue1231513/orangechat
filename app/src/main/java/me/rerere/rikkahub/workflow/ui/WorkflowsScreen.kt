/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.workflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.workflow.model.TriggerSpec
import me.rerere.rikkahub.workflow.model.WorkflowDefinition
import me.rerere.rikkahub.workflow.model.WorkflowRunStatus
import me.rerere.rikkahub.workflow.repository.WorkflowRepository.Loaded
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
import java.time.Instant

@Composable
fun WorkflowsScreen(vm: WorkflowsViewModel = koinViewModel()) {
    val nav = LocalNavController.current
    val workflows by vm.workflows.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showHowItWorks by remember { mutableStateOf(false) }

    if (showHowItWorks) {
        AlertDialog(
            onDismissRequest = { showHowItWorks = false },
            title = { Text("工作流如何运作") },
            text = {
                Text(
                    "工作流由 AI 编写。描述触发条件和动作，例如「连上家里 WiFi 时静音」或" +
                        "「工作日早 8 点如果电量高于 50% 就检查邮件」。触发器触发且所有条件满足时，" +
                        "按顺序执行动作。可在助手设置里开启「工作流」工具后由 AI 创建。"
                )
            },
            confirmButton = {
                TextButton(onClick = { showHowItWorks = false }) { Text("知道了") }
            },
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("工作流") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        if (workflows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有工作流。告诉助手创建一个，例如「连上家里 WiFi 时静音」。",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(workflows, key = { it.entity.id }) { loaded ->
                    WorkflowRow(
                        loaded = loaded,
                        onToggle = { enabled -> vm.setEnabled(loaded.entity.id, enabled) },
                        onTap = { nav.navigate(Screen.WorkflowDetail(loaded.entity.id)) },
                    )
                }
                item {
                    TextButton(
                        onClick = { showHowItWorks = true },
                        modifier = Modifier.padding(8.dp),
                    ) { Text("工作流如何运作？") }
                }
            }
        }
    }
}

@Composable
private fun WorkflowRow(
    loaded: Loaded,
    onToggle: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    val nowMs by rememberTickingNowMs()
    val triggerSummary = remember(loaded.definition) {
        oneLineTriggerSummary(loaded.definition)
    }
    val statusLine: String = when {
        loaded.entity.lastRunAtMs == null -> "从未运行"
        else -> {
            val ago = formatAgo(loaded.entity.lastRunAtMs, nowMs)
            when (loaded.entity.lastRunStatus) {
                WorkflowRunStatus.SUCCESS.name -> "成功运行 · $ago"
                WorkflowRunStatus.FAILED.name -> "运行失败 · $ago"
                else -> "已跳过 · $ago"
            }
        }
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 8.dp),
        headlineContent = { Text(loaded.entity.name) },
        supportingContent = {
            Text(
                text = "当：$triggerSummary\n$statusLine",
                maxLines = 3,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Switch(checked = loaded.entity.enabled, onCheckedChange = onToggle)
        },
    )
    HorizontalDivider()
}

internal fun oneLineTriggerSummary(def: WorkflowDefinition): String = when (val t = def.trigger) {
    is TriggerSpec.TimeCron ->
        if (!t.timeOfDay.isNullOrBlank()) "每天 ${t.timeOfDay}" else "定时"
    is TriggerSpec.WifiConnected -> "WiFi 连接" + (t.ssid?.let { "($it)" }.orEmpty())
    is TriggerSpec.WifiDisconnected -> "WiFi 断开" + (t.ssid?.let { "($it)" }.orEmpty())
    is TriggerSpec.BluetoothDeviceConnected -> "蓝牙连接"
    is TriggerSpec.BluetoothDeviceDisconnected -> "蓝牙断开"
    is TriggerSpec.HeadphonesPlugged -> "耳机插入"
    is TriggerSpec.HeadphonesUnplugged -> "耳机拔出"
    is TriggerSpec.PowerConnected -> "接入电源"
    is TriggerSpec.PowerDisconnected -> "断开电源"
    is TriggerSpec.BatteryBelow -> "电量低于 ${t.thresholdPercent}%"
    is TriggerSpec.BatteryAbove -> "电量高于 ${t.thresholdPercent}%"
    is TriggerSpec.GeofenceEnter -> "到达 ${t.label ?: "某地点"}"
    is TriggerSpec.GeofenceExit -> "离开 ${t.label ?: "某地点"}"
    is TriggerSpec.AppLaunched -> "${t.packageName} 启动"
    is TriggerSpec.AppClosed -> "${t.packageName} 关闭"
    is TriggerSpec.AppForegroundDuration -> "${t.packageName} 持续前台 ${t.minutes} 分钟"
    is TriggerSpec.NotificationReceived -> "收到通知" + (t.packageName?.let { "($it)" } ?: "")
    is TriggerSpec.BootCompleted -> "设备开机"
    is TriggerSpec.ScreenOn -> "屏幕点亮"
    is TriggerSpec.ScreenOff -> "屏幕熄灭"
    is TriggerSpec.Manual -> "仅手动运行"
}

/**
 * A [State<Long>] of the current wall-clock millis, refreshed every 30s while the calling
 * Composable is in the composition, so "ran 2m ago" subtitles stay fresh.
 */
@Composable
internal fun rememberTickingNowMs(): State<Long> {
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            state.longValue = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    return state
}

/** Simple relative-time formatter ("刚刚" / "x 分钟前" / "x 小时前" / "x 天前"). */
internal fun formatAgo(timeMs: Long, nowMs: Long): String {
    val secs = Duration.between(Instant.ofEpochMilli(timeMs), Instant.ofEpochMilli(nowMs)).seconds
    return when {
        secs < 60 -> "刚刚"
        secs < 3600 -> "${secs / 60} 分钟前"
        secs < 86400 -> "${secs / 3600} 小时前"
        else -> "${secs / 86400} 天前"
    }
}
