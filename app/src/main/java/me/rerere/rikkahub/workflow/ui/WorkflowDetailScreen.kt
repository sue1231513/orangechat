/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.workflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.workflow.model.WorkflowAction
import me.rerere.rikkahub.workflow.model.WorkflowRun
import me.rerere.rikkahub.workflow.repository.WorkflowRepository.Loaded
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkflowDetailScreen(
    workflowId: String,
    vm: WorkflowsViewModel = koinViewModel(),
) {
    val nav = LocalNavController.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var loaded by remember { mutableStateOf<Loaded?>(null) }
    var history by remember { mutableStateOf<List<WorkflowRun>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(workflowId) {
        loaded = vm.get(workflowId)
        history = vm.history(workflowId)
    }

    val currentLoaded = loaded
    if (currentLoaded == null) {
        Scaffold(
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text("工作流") },
                    navigationIcon = { BackButton() },
                    colors = CustomColors.topBarColors,
                )
            },
            containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = "找不到该工作流",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除工作流") },
            text = { Text("确定删除「${currentLoaded.entity.name}」及其全部运行历史？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete(currentLoaded.entity.id) { nav.popBackStack() }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(currentLoaded.entity.name) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        // Sticky bottom bar - keeps Run-now / Edit / Delete reachable as history grows.
        bottomBar = {
            androidx.compose.material3.Surface(
                tonalElevation = 3.dp,
                color = CustomColors.topBarColors.containerColor,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Button(onClick = {
                        scope.launch {
                            val outcome = vm.runNow(currentLoaded.entity.id)
                            history = vm.history(currentLoaded.entity.id)
                            loaded = vm.get(currentLoaded.entity.id)
                            snackbarHostState.showSnackbar("运行结束：${outcome.status.name}")
                        }
                    }) { Text("立即运行") }
                    TextButton(onClick = {
                        nav.navigate(
                            Screen.Chat(
                                id = kotlin.uuid.Uuid.random().toString(),
                                text = "帮我修改工作流「${currentLoaded.entity.name}」".base64Encode(),
                            )
                        )
                    }) { Text("编辑") }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Description
            currentLoaded.definition.description?.takeIf { it.isNotBlank() }?.let { desc ->
                item {
                    SectionHeader("描述")
                    Text(desc, style = MaterialTheme.typography.bodyMedium)
                }
            }
            // Trigger
            item {
                SectionHeader("触发器")
                Text(oneLineTriggerSummary(currentLoaded.definition))
            }
            // Conditions
            item {
                SectionHeader("条件")
                if (currentLoaded.definition.conditions.isEmpty()) {
                    Text("无（始终满足）")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in currentLoaded.definition.conditions) {
                            Text("• ${conditionLine(c)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            // Actions
            item {
                SectionHeader("动作")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((idx, a) in currentLoaded.definition.actions.withIndex()) {
                        ActionRow(idx + 1, a)
                    }
                }
            }
            // Stats
            item {
                SectionHeader("统计")
                StatsBlock(currentLoaded)
            }
            // History
            item {
                SectionHeader("运行历史")
                if (history.isEmpty()) {
                    Text("暂无运行记录")
                } else {
                    val nowMs by rememberTickingNowMs()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in history) {
                            val ago = formatAgo(r.firedAtMs, nowMs)
                            val line = "$ago - ${r.status.name}" +
                                (r.errorMessage?.let { " - ${it.take(60)}" } ?: "")
                            Text(line, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun ActionRow(index: Int, action: WorkflowAction) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp),
            )
            .padding(8.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("$index. ", style = MaterialTheme.typography.bodyMedium)
            Text(
                action.tool,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            Text(" (${action.timeoutSeconds}s)", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp))
        }
        if (action.args.isNotEmpty()) {
            TextButton(onClick = { expanded = !expanded },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(if (expanded) "收起参数" else "展开参数",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (expanded) {
                Text(
                    action.args.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsBlock(loaded: Loaded) {
    val nowMs by rememberTickingNowMs()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val lastRunText = loaded.entity.lastRunAtMs?.let {
            "上次运行：${formatAgo(it, nowMs)}"
        } ?: "上次运行：从未"
        Text(lastRunText, style = MaterialTheme.typography.bodySmall)
        Text("今日运行：${loaded.entity.runsTodayCount} 次", style = MaterialTheme.typography.bodySmall)
        Text(
            if (loaded.definition.cooldownSeconds == 0) "冷却：无"
            else "冷却：${loaded.definition.cooldownSeconds} 秒",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            loaded.definition.maxRunsPerDay?.let { "每日上限：$it 次" } ?: "每日上限：不限",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun conditionLine(c: me.rerere.rikkahub.workflow.model.ConditionSpec): String {
    val base = when (c) {
        is me.rerere.rikkahub.workflow.model.ConditionSpec.TimeBetween -> "${c.start} 到 ${c.end} 之间"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.TimeAfterSunset -> "日落后" +
            if (c.offsetMinutes != 0) " (偏移 ${c.offsetMinutes} 分钟)" else ""
        is me.rerere.rikkahub.workflow.model.ConditionSpec.TimeBeforeSunrise -> "日出前" +
            if (c.offsetMinutes != 0) " (偏移 ${c.offsetMinutes} 分钟)" else ""
        is me.rerere.rikkahub.workflow.model.ConditionSpec.DayOfWeekIn -> "星期 ${c.days.joinToString(",")}"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.WifiSsidIs -> "WiFi 为 ${c.ssid}"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.WifiSsidIn -> "WiFi 在 ${c.ssids.joinToString(",")}"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.BatteryAbove -> "电量 > ${c.percent}%"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.BatteryBelow -> "电量 < ${c.percent}%"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.IsCharging -> "正在充电"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.IsNotCharging -> "未在充电"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.ForegroundAppIs -> "前台应用 = ${c.packageName}"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.ForegroundAppIn -> "前台应用在 ${c.packageNames.size} 个应用中"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.ScreenIsOn -> "屏幕点亮"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.ScreenIsOff -> "屏幕熄灭"
        is me.rerere.rikkahub.workflow.model.ConditionSpec.LastChatAgo -> "距上次聊天 ≥ ${c.minutes} 分钟"
    }
    return if (c.invert) "非（$base）" else base
}
