/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.RiskConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.data.service.ProactiveMessageService
import me.rerere.rikkahub.data.service.ProactiveMessageWorker
import org.koin.compose.koinInject
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingProactiveMessagePage(vm: SettingVM = koinInject()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var showProactiveRiskDialog by remember { mutableStateOf(false) }
    var showAggressiveRiskDialog by remember { mutableStateOf(false) }

    if (showProactiveRiskDialog) {
        RiskConfirmDialog(
            title = stringResource(R.string.risk_proactive_message_title),
            message = stringResource(R.string.risk_proactive_message_message),
            onConfirm = {
                showProactiveRiskDialog = false
                val newSetting = settings.proactiveMessageSetting.copy(enabled = true, aggressiveModeEnabled = false)
                vm.updateSettings(settings.copy(proactiveMessageSetting = newSetting))
                me.rerere.rikkahub.data.service.DeviceEventAiTriggerService.stop(context)
                ProactiveMessageService.triggerNow(context, newSetting)
            },
            onDismiss = { showProactiveRiskDialog = false }
        )
    }

    if (showAggressiveRiskDialog) {
        RiskConfirmDialog(
            title = stringResource(R.string.risk_proactive_message_title),
            message = stringResource(R.string.risk_proactive_message_message),
            onConfirm = {
                showAggressiveRiskDialog = false
                val newSetting = settings.proactiveMessageSetting.copy(aggressiveModeEnabled = true, enabled = false)
                vm.updateSettings(settings.copy(proactiveMessageSetting = newSetting))
                ProactiveMessageService.cancel(context)
                try {
                    val intent = android.content.Intent(context, me.rerere.rikkahub.data.service.DeviceEventAiTriggerService::class.java)
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    android.util.Log.e("SettingProactiveMessage", "Failed to start aggressive mode service", e)
                }
            },
            onDismiss = { showAggressiveRiskDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("主动消息") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                CardGroup {
                    item(
                        headlineContent = { Text("启用主动消息") },
                        supportingContent = { Text("开启后AI立即主动发一条消息，之后按设定间隔循环") },
                        trailingContent = {
                            Switch(
                                checked = settings.proactiveMessageSetting.enabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showProactiveRiskDialog = true
                                    } else {
                                        val newSetting = settings.proactiveMessageSetting.copy(enabled = false)
                                        vm.updateSettings(settings.copy(proactiveMessageSetting = newSetting))
                                        ProactiveMessageService.cancel(context)
                                    }
                                }
                            )
                        }
                    )
                    if (settings.proactiveMessageSetting.enabled) {
                        var nextTime by remember { mutableStateOf(ProactiveMessageService.getNextTriggerTime(context)) }
                        LaunchedEffect(settings.proactiveMessageSetting) {
                            nextTime = ProactiveMessageService.getNextTriggerTime(context)
                            while (true) {
                                kotlinx.coroutines.delay(10_000L)
                                nextTime = ProactiveMessageService.getNextTriggerTime(context)
                            }
                        }
                        item(
                            headlineContent = { Text("下次触发时间") },
                            supportingContent = {
                                val currentTime = System.currentTimeMillis()
                                val triggerTime = nextTime
                                if (triggerTime != null && triggerTime > currentTime) {
                                    val remaining = triggerTime - currentTime
                                    val remainMinutes = remaining / 60_000
                                    val remainSeconds = (remaining % 60_000) / 1000
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    Text("🕐 ${sdf.format(java.util.Date(triggerTime))}（剩余 ${remainMinutes}分${remainSeconds}秒）")
                                } else {
                                    Text("等待调度中...")
                                }
                            }
                        )
                    }
                    // 悬浮球开关（仅在主动消息启用时显示）
                    if (settings.proactiveMessageSetting.enabled) {
                        val overlayPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { }
                        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else true

                        item(
                            headlineContent = { Text("悬浮球提醒") },
                            supportingContent = {
                                Text(
                                    if (hasOverlayPermission) {
                                        "主动消息到达时以悬浮球形式提醒，点击直接进入聊天页"
                                    } else {
                                        "需要先授予「显示在其他应用上层」权限才能显示悬浮球"
                                    }
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.proactiveMessageSetting.floatingBubbleEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                            // 无 overlay 权限，引导用户去系统设置授权
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            overlayPermissionLauncher.launch(intent)
                                            return@Switch
                                        }
                                        vm.updateSettings(
                                            settings.copy(
                                                proactiveMessageSetting = settings.proactiveMessageSetting.copy(
                                                    floatingBubbleEnabled = enabled
                                                )
                                            )
                                        )
                                        if (!enabled) {
                                            me.rerere.rikkahub.data.service.FloatingBubbleService.dismiss(context)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
            item {
                CardGroup {
                    item(
                        headlineContent = { Text("最小间隔 (分钟)") },
                        supportingContent = {
                            OutlinedTextField(
                                value = settings.proactiveMessageSetting.minIntervalMinutes.toString(),
                                onValueChange = { value ->
                                    val minutes = value.toIntOrNull()
                                    if (minutes != null && minutes > 0) {
                                        vm.updateSettings(
                                            settings.copy(
                                                proactiveMessageSetting = settings.proactiveMessageSetting.copy(
                                                    minIntervalMinutes = minutes
                                                )
                                            )
                                        )
                                    }
                                },
                                placeholder = { Text("30") },
                                singleLine = true,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("最大间隔 (分钟)") },
                        supportingContent = {
                            OutlinedTextField(
                                value = settings.proactiveMessageSetting.maxIntervalMinutes.toString(),
                                onValueChange = { value ->
                                    val minutes = value.toIntOrNull()
                                    if (minutes != null && minutes >= settings.proactiveMessageSetting.minIntervalMinutes) {
                                        vm.updateSettings(
                                            settings.copy(
                                                proactiveMessageSetting = settings.proactiveMessageSetting.copy(
                                                    maxIntervalMinutes = minutes
                                                )
                                            )
                                        )
                                    }
                                },
                                placeholder = { Text("90") },
                                singleLine = true,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        },
                    )
                }
            }
            // 激进模式开关（与主动消息互斥）
            item {
                CardGroup {
                    item(
                        headlineContent = { Text("激进模式") },
                        supportingContent = {
                            Text("开启后，每次手机切换应用、开屏锁屏、回到桌面都会触发 AI 思考。AI 会根据用户的手机动向自主决定是否主动发消息或切屏。\n\n可以独立开启，不需要同时开启主动消息。\n\n这是一个常驻前台服务，会持续小幅耗电。需要开启使用情况访问权限。\n\nAI 大多数时候会选择 [PASS] 跳过，只在觉得有话要说时才会发消息。")
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.proactiveMessageSetting.aggressiveModeEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showAggressiveRiskDialog = true
                                    } else {
                                        val newSetting = settings.proactiveMessageSetting.copy(aggressiveModeEnabled = false)
                                        vm.updateSettings(settings.copy(proactiveMessageSetting = newSetting))
                                        me.rerere.rikkahub.data.service.DeviceEventAiTriggerService.stop(context)
                                    }
                                }
                            )
                        }
                    )
                    // 最小间隔设置（仅当激进模式开启时显示）
                    if (settings.proactiveMessageSetting.aggressiveModeEnabled) {
                        item(
                            headlineContent = { Text("激进模式最小间隔 (秒)") },
                            supportingContent = {
                                OutlinedTextField(
                                    value = settings.proactiveMessageSetting.aggressiveMinIntervalSeconds.toString(),
                                    onValueChange = { value ->
                                        val seconds = value.toIntOrNull()
                                        if (seconds != null && seconds >= 10) {
                                            vm.updateSettings(
                                                settings.copy(
                                                    proactiveMessageSetting = settings.proactiveMessageSetting.copy(
                                                        aggressiveMinIntervalSeconds = seconds
                                                    )
                                                )
                                            )
                                        }
                                    },
                                    placeholder = { Text("60") },
                                    singleLine = true,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text("两次 AI 思考之间的最小间隔（秒）。防抖+限流，避免频繁触发浪费 token。最小10秒。")
                            },
                        )
                        // 防抖等待时间设置（仅当激进模式开启时显示）
                        item(
                            headlineContent = { Text("激进模式防抖等待 (秒)") },
                            supportingContent = {
                                OutlinedTextField(
                                    value = settings.proactiveMessageSetting.aggressiveDebounceSeconds.toString(),
                                    onValueChange = { value ->
                                        val seconds = value.toIntOrNull()
                                        if (seconds != null && seconds >= 3) {
                                            vm.updateSettings(
                                                settings.copy(
                                                    proactiveMessageSetting = settings.proactiveMessageSetting.copy(
                                                        aggressiveDebounceSeconds = seconds
                                                    )
                                                )
                                            )
                                        }
                                    },
                                    placeholder = { Text("30") },
                                    singleLine = true,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text("检测到切换应用/开关屏/回桌面等操作后，等待多少秒再让 AI 思考（期间的新操作会重新计时）。设多少就是精确多少秒，不是随机值。最小3秒。")
                            },
                        )
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val hasExactAlarm = ProactiveMessageWorker.canScheduleExactAlarms(context)
                    CardGroup {
                        item(
                            headlineContent = { Text("精确闹钟权限") },
                            supportingContent = {
                                if (hasExactAlarm) {
                                    Text("已授予精确闹钟权限，定时触发将更准确")
                                } else {
                                    Text("未授予精确闹钟权限，触发时间可能不精确。已自动使用 WorkManager 作为备用方案。")
                                }
                            },
                            onClick = if (!hasExactAlarm) {
                                {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
            item {
                val isIgnoring = ProactiveMessageWorker.isIgnoringBatteryOptimizations(context)
                CardGroup {
                    item(
                        headlineContent = { Text("电池优化") },
                        supportingContent = {
                            if (isIgnoring) {
                                Text("已忽略电池优化，后台触发更稳定")
                            } else {
                                Text("未忽略电池优化，系统可能限制后台活动导致消息无法准时触发。建议关闭电池优化。")
                            }
                        },
                        onClick = if (!isIgnoring) {
                            {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        } else null
                    )
                }
            }
            item {
                CardGroup {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = {
                            Text("启用后，AI 会在设定的最小和最大间隔之间随机一个时间点主动给你发消息。你回复后计时器重置，重新开始随机计时；不回复则继续循环发消息。AI 可以自己思考选择要不要回复，如果觉得没什么好说的可以跳过。\n\n提示：同时使用 AlarmManager + WorkManager 双重调度，确保消息能准时触发。\n\n强制跳转：开启后 AI 会自行判断是否需要拉起屏幕。")
                        },
                    )
                }
            }
        }
    }
}
