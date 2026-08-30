/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Camera01
import me.rerere.hugeicons.stroke.Location01
import me.rerere.hugeicons.stroke.Notification02
import me.rerere.hugeicons.stroke.SmartPhone01
import me.rerere.hugeicons.stroke.BatteryFull
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.MusicNote02
import me.rerere.hugeicons.stroke.Watch01
import me.rerere.hugeicons.stroke.Pulse01
import me.rerere.hugeicons.stroke.Flashlight
import me.rerere.hugeicons.stroke.Megaphone01
import me.rerere.hugeicons.stroke.Sun02
import me.rerere.hugeicons.stroke.Speaker01
import me.rerere.hugeicons.stroke.SmartphoneWifi
import me.rerere.hugeicons.stroke.SmartPhone01
import me.rerere.hugeicons.stroke.Share05
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.FullScreen
import me.rerere.hugeicons.stroke.Scan
import me.rerere.hugeicons.stroke.Notification02
import me.rerere.hugeicons.stroke.HardDrive
import me.rerere.hugeicons.stroke.SlidersHorizontal
import me.rerere.hugeicons.stroke.FingerPrint
import me.rerere.hugeicons.stroke.Search01
import me.rerere.rikkahub.data.ai.tools.SystemTools
import me.rerere.rikkahub.service.KeepAliveService
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SystemToolsSetting
import me.rerere.rikkahub.data.gadgetbridge.GadgetbridgeReader
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.RiskConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.components.ui.permission.PermissionAccessBackgroundLocation
import me.rerere.rikkahub.ui.components.ui.permission.PermissionAccessCoarseLocation
import me.rerere.rikkahub.ui.components.ui.permission.PermissionAccessFineLocation
import me.rerere.rikkahub.ui.components.ui.permission.PermissionCamera
import me.rerere.rikkahub.ui.components.ui.permission.PermissionManager
import me.rerere.rikkahub.ui.components.ui.permission.PermissionInfo
import me.rerere.rikkahub.ui.components.ui.permission.PermissionPostNotifications
import me.rerere.rikkahub.ui.components.ui.permission.PermissionReadSms
import me.rerere.rikkahub.ui.components.ui.permission.PermissionReadPhoneState
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.Screen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingSystemToolsPage(vm: SettingVM = koinViewModel()) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var systemToolsSetting by remember(settings) {
        mutableStateOf(settings.systemToolsSetting)
    }
    LaunchedEffect(settings) {
        systemToolsSetting = settings.systemToolsSetting
    }

    fun updateSystemToolsSetting(setting: SystemToolsSetting) {
        systemToolsSetting = setting
        vm.updateSettings(settings.copy(systemToolsSetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val locationPermissions = buildSet {
        add(PermissionAccessFineLocation)
        add(PermissionAccessCoarseLocation)
        add(PermissionAccessBackgroundLocation)
    }
    val locationPermissionState = rememberPermissionState(permissions = locationPermissions)

    val notificationPermissionState = rememberPermissionState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setOf(PermissionPostNotifications)
        } else emptySet<PermissionInfo>()
    )

    // 保活开关状态
    var keepAliveEnabled by remember(settings) {
        mutableStateOf(settings.keepAliveEnabled)
    }
    LaunchedEffect(settings) {
        keepAliveEnabled = settings.keepAliveEnabled
    }

    val cameraPermissionState = rememberPermissionState(permissions = setOf(PermissionCamera))

    val smsPermissionState = rememberPermissionState(permissions = setOf(PermissionReadSms))

    val phoneStatePermissionState = rememberPermissionState(permissions = setOf(PermissionReadPhoneState))

    // 搜索栏状态

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("系统工具") },
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


            // 安全提示
            item {
                CardGroup(
                    title = { Text(stringResource(R.string.system_tools_security_warning_title)) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    item(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.system_tools_security_warning_message),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.system_tools_security_warning_action),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            navController.navigate(
                                Screen.Legal(
                                    titleRes = R.string.system_tools_security_warning_title,
                                    contentRes = R.string.system_tools_security_warning_message
                                )
                            )
                        }
                    )
                }
            }

            // 后台保活
            item {
            CardGroup(
                title = { Text("后台保活") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Pulse01, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.setting_system_tools_keep_alive)) },
                    supportingContent = { Text(stringResource(R.string.setting_system_tools_keep_alive_desc)) },
                    trailingContent = {
                        Switch(
                            checked = keepAliveEnabled,
                            onCheckedChange = { enabled ->
                                keepAliveEnabled = enabled
                                vm.updateSettings(settings.copy(keepAliveEnabled = enabled))
                                if (enabled) {
                                    // Android 13+ 需要先请求 POST_NOTIFICATIONS 权限
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                        && !notificationPermissionState.allPermissionsGranted
                                    ) {
                                        notificationPermissionState.requestPermissions()
                                    } else {
                                        KeepAliveService.start(context)
                                    }
                                } else {
                                    KeepAliveService.stop(context)
                                }
                            }
                        )
                    }
                )
                if (keepAliveEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && !notificationPermissionState.allPermissionsGranted
                ) {
                    item(
                        headlineContent = { Text("⚠ 通知权限未授予") },
                        supportingContent = { Text("开启保活需要通知权限以显示常驻通知") },
                        trailingContent = {
                            FilledTonalButton(onClick = { notificationPermissionState.requestPermissions() }) {
                                Text("授权")
                            }
                        }
                    )
                }
            }
            }


            // 位置服务
            item {
            CardGroup(
                title = { Text("位置服务") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = {
                        Icon(imageVector = HugeIcons.Location01, contentDescription = null)
                    },
                    headlineContent = { Text("启用位置工具") },
                    supportingContent = { Text("允许AI获取您的当前位置，并使用高德API转换为地址") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.locationAccess,
                            onCheckedChange = { enabled ->
                                if (enabled && !locationPermissionState.allPermissionsGranted) {
                                    locationPermissionState.requestPermissions()
                                }
                                updateSystemToolsSetting(systemToolsSetting.copy(locationAccess = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.locationAccess && !locationPermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 位置权限未授予") },
                        supportingContent = { Text("点击授权按钮授予位置权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { locationPermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
                if (systemToolsSetting.locationAccess) {
                    item(
                        headlineContent = { Text("高德API Key") },
                        supportingContent = {
                            TextField(
                                value = systemToolsSetting.amapApiKey,
                                onValueChange = { key -> updateSystemToolsSetting(systemToolsSetting.copy(amapApiKey = key)) },
                                placeholder = { Text("请输入高德Web服务API Key") },
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                            )
                        }
                    )
                }
            }
            }


            // 通知服务
            item {
            CardGroup(
                title = { Text("通知服务") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Notification02, contentDescription = null) },
                    headlineContent = { Text("启用通知工具") },
                    supportingContent = { Text("允许AI读取今日通知，了解您的消息动态") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.notificationAccess,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(notificationAccess = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.notificationAccess) {
                    item(
                        headlineContent = { Text("通知访问权限") },
                        supportingContent = {
                            val cn = android.content.ComponentName(context, me.rerere.rikkahub.data.service.RikkaNotificationListenerService::class.java)
                            val enabled = try {
                                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(cn.flattenToString()) == true
                            } catch (_: Exception) { false }
                            if (enabled) Text("✓ 已授予通知访问权限") else Text("⚠ 需要在系统设置中授予通知访问权限")
                        },
                        trailingContent = {
                            val cn = android.content.ComponentName(context, me.rerere.rikkahub.data.service.RikkaNotificationListenerService::class.java)
                            val enabled = try {
                                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(cn.flattenToString()) == true
                            } catch (_: Exception) { false }
                            if (!enabled) {
                                FilledTonalButton(onClick = {
                                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) {}
                                }) { Text("去设置") }
                            }
                        }
                    )
                }
            }
            }


            // App使用统计
            item {
            CardGroup(
                title = { Text("应用使用统计") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用应用使用工具") },
                    supportingContent = { Text("允许AI查看您的应用使用情况和轨迹，需要使用情况访问权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.appUsageAccess,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(appUsageAccess = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.appUsageAccess) {
                    item(
                        headlineContent = { Text("使用情况访问权限") },
                        supportingContent = {
                            if (SystemTools.hasAppUsagePermission(context)) Text("✓ 已授予使用情况访问权限") else Text("⚠ 需要在系统设置中授予使用情况访问权限")
                        },
                        trailingContent = {
                            if (!SystemTools.hasAppUsagePermission(context)) {
                                FilledTonalButton(onClick = {
                                    try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
                                }) { Text("去设置") }
                            }
                        }
                    )
                }
            }
            }


            // 探索周边
            item {
            CardGroup(
                title = { Text("探索周边") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Location01, contentDescription = null) },
                    headlineContent = { Text("启用周边探索") },
                    supportingContent = { Text("允许AI使用高德API搜索周边POI，如餐厅、商店、景点等") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.locationExploreEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !locationPermissionState.allPermissionsGranted) locationPermissionState.requestPermissions()
                                updateSystemToolsSetting(systemToolsSetting.copy(locationExploreEnabled = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.locationExploreEnabled) {
                    item(
                        headlineContent = { Text("搜索半径 (米)") },
                        supportingContent = {
                            OutlinedTextField(
                                value = systemToolsSetting.locationExploreRadius.toString(),
                                onValueChange = { value ->
                                    val radius = value.toIntOrNull()
                                    if (radius != null && radius > 0) updateSystemToolsSetting(systemToolsSetting.copy(locationExploreRadius = radius))
                                },
                                placeholder = { Text("1000") },
                                singleLine = true,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    )
                }
            }
            }


            // Supabase 数据同步
            item {
            CardGroup(
                title = { Text("Supabase 数据同步") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用 Supabase 同步") },
                    supportingContent = { Text("开启后立即同步一次，之后每15分钟自动同步") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.supabaseEnabled,
                            onCheckedChange = { enabled ->
                                val newSetting = systemToolsSetting.copy(supabaseEnabled = enabled)
                                updateSystemToolsSetting(newSetting)
                                if (enabled) me.rerere.rikkahub.data.service.SupabaseSyncService.triggerNow(context)
                                else me.rerere.rikkahub.data.service.SupabaseSyncService.cancel(context)
                            }
                        )
                    }
                )
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("开机/亮屏/黑屏事件推送") },
                    supportingContent = {
                        Text("开启后会在设备开机、亮屏、黑屏时立即推送一条事件记录到同一张数据表。需要保持一个常驻通知以实时监听亮屏/黑屏状态，会有持续小幅耗电；如遇频繁失效，请到系统设置中把本App加入电池优化白名单。")
                    },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.deviceEventTrackingEnabled,
                            onCheckedChange = { enabled ->
                                val newSetting = systemToolsSetting.copy(deviceEventTrackingEnabled = enabled)
                                updateSystemToolsSetting(newSetting)
                                if (enabled) {
                                    if (newSetting.supabaseEnabled &&
                                        newSetting.supabaseUrl.isNotBlank() &&
                                        newSetting.supabaseApiKey.isNotBlank()
                                    ) {
                                        try {
                                            context.startForegroundService(
                                                Intent(context, me.rerere.rikkahub.data.service.DeviceEventTrackingService::class.java)
                                            )
                                        } catch (e: Exception) {
                                            android.util.Log.e("SettingSystemToolsPage", "启动 DeviceEventTrackingService 失败", e)
                                        }
                                    } else {
                                        android.util.Log.w("SettingSystemToolsPage", "Supabase 未配置完整，跳过启动 DeviceEventTrackingService")
                                    }
                                } else {
                                    context.stopService(
                                        Intent(context, me.rerere.rikkahub.data.service.DeviceEventTrackingService::class.java)
                                    )
                                }
                            }
                        )
                    }
                )
                var supabaseNextTime by remember { mutableStateOf(me.rerere.rikkahub.data.service.SupabaseSyncService.getNextTriggerTime(context)) }
                var supabaseSyncing by remember { mutableStateOf(me.rerere.rikkahub.data.service.SupabaseSyncService.isSyncing(context)) }
                LaunchedEffect(systemToolsSetting) {
                    supabaseNextTime = me.rerere.rikkahub.data.service.SupabaseSyncService.getNextTriggerTime(context)
                    supabaseSyncing = me.rerere.rikkahub.data.service.SupabaseSyncService.isSyncing(context)
                    while (true) {
                        kotlinx.coroutines.delay(2_000L)
                        supabaseNextTime = me.rerere.rikkahub.data.service.SupabaseSyncService.getNextTriggerTime(context)
                        supabaseSyncing = me.rerere.rikkahub.data.service.SupabaseSyncService.isSyncing(context)
                    }
                }
                item(
                    headlineContent = { Text("同步状态") },
                    supportingContent = {
                        if (!systemToolsSetting.supabaseEnabled) {
                            Text("未启用")
                        } else if (supabaseSyncing) {
                            Text("🔄 同步中...")
                        } else {
                            val currentTime = System.currentTimeMillis()
                            val triggerTime = supabaseNextTime
                            if (triggerTime != null && triggerTime > currentTime) {
                                val remaining = triggerTime - currentTime
                                val remainMinutes = remaining / 60_000
                                val remainSeconds = (remaining % 60_000) / 1000
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                Text("🕐 下次: ${sdf.format(java.util.Date(triggerTime))}（剩余 ${remainMinutes}分${remainSeconds}秒）")
                            } else {
                                Text("✅ 已开启，等待调度...")
                            }
                        }
                    }
                )
                item(
                    headlineContent = { Text("Supabase URL") },
                    supportingContent = {
                        TextField(
                            value = systemToolsSetting.supabaseUrl,
                            onValueChange = { url -> updateSystemToolsSetting(systemToolsSetting.copy(supabaseUrl = url)) },
                            placeholder = { Text("https://xxxx.supabase.co") },
                            modifier = Modifier.fillMaxSize(), singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                )
                item(
                    headlineContent = { Text("Supabase API Key") },
                    supportingContent = {
                        TextField(
                            value = systemToolsSetting.supabaseApiKey,
                            onValueChange = { key -> updateSystemToolsSetting(systemToolsSetting.copy(supabaseApiKey = key)) },
                            placeholder = { Text("eyJhbGciOiJIUzI1NiIs...") },
                            modifier = Modifier.fillMaxSize(), singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                )
                item(
                    headlineContent = { Text("设备数据表名") },
                    supportingContent = {
                        TextField(
                            value = systemToolsSetting.supabaseTableName,
                            onValueChange = { name -> updateSystemToolsSetting(systemToolsSetting.copy(supabaseTableName = name)) },
                            placeholder = { Text("device_data") },
                            modifier = Modifier.fillMaxSize(), singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                        )
                    }
                )
                item(
                    headlineContent = { Text("说明") },
                    supportingContent = {
                        Text("需要先在 Supabase SQL Editor 中创建设备数据表。\n\n设备数据表：\ntimestamp (text), foreground_app (text), location_latitude (float), location_longitude (float), location_address (text), location_city (text), location_district (text), location_street (text), app_usage (text/jsonb), notifications (text/jsonb), device_event (text), health_data (jsonb)")
                    }
                )
            }
            }


            // 相机/拍照服务
            item {
            CardGroup(
                title = { Text("相机/拍照服务") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Camera01, contentDescription = null) },
                    headlineContent = { Text("启用拍照工具") },
                    supportingContent = { Text("允许AI在后台拍照并识别图像内容（物体、场景、文字等），照片会发送给AI进行视觉分析") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.cameraAccess,
                            onCheckedChange = { enabled ->
                                if (enabled && !cameraPermissionState.allPermissionsGranted) cameraPermissionState.requestPermissions()
                                updateSystemToolsSetting(systemToolsSetting.copy(cameraAccess = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.cameraAccess && !cameraPermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 相机权限未授予") },
                        supportingContent = { Text("点击授权按钮授予相机权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { cameraPermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
            }
            }


            // Gadgetbridge 健康数据
            item {
            CardGroup(
                title = { Text("Gadgetbridge 健康数据") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Watch01, contentDescription = null) },
                    headlineContent = { Text("启用 Gadgetbridge 工具") },
                    supportingContent = { Text("允许AI读取手表的健康数据（步数、心率、睡眠等），需要存储权限和 Gadgetbridge 开启自动导出") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.gadgetbridgeEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(gadgetbridgeEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.gadgetbridgeEnabled) {
                    item(
                        headlineContent = { Text("数据库文件路径") },
                        supportingContent = {
                            TextField(
                                value = systemToolsSetting.gadgetbridgeDbPath,
                                onValueChange = { path -> updateSystemToolsSetting(systemToolsSetting.copy(gadgetbridgeDbPath = path)) },
                                placeholder = { Text("/sdcard/Download/手环/Gadgetbridge.db（留空使用默认路径）") },
                                modifier = Modifier.fillMaxSize(), singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                            )
                        }
                    )
                    val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        android.content.pm.PackageManager.PERMISSION_GRANTED == androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    if (!hasStoragePermission) {
                        item(
                            headlineContent = { Text("⚠ 存储权限未授予") },
                            supportingContent = { Text("需要存储权限才能读取 Gadgetbridge 导出的数据库文件") },
                            trailingContent = {
                                FilledTonalButton(onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                            intent.data = android.net.Uri.parse("package:${context.packageName}")
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            try { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) } catch (_: Exception) {}
                                        }
                                    }
                                }) { Text("授权") }
                            }
                        )
                    }
                    val dbExists = if (hasStoragePermission) GadgetbridgeReader.dbFileExists(systemToolsSetting.gadgetbridgeDbPath) else false
                    if (hasStoragePermission && !dbExists) {
                        item(
                            headlineContent = { Text("⚠ 数据库文件未找到") },
                            supportingContent = { Text("请在 Gadgetbridge 设置中开启\"自动导出\"功能。默认路径: /sdcard/Download/手环/Gadgetbridge.db") }
                        )
                    }
                    val gbInstalled = try { context.packageManager.getPackageInfo("nodomain.freeyourgadget.gadgetbridge", 0) != null } catch (_: Exception) { false }
                    if (!gbInstalled) {
                        item(headlineContent = { Text("⚠ Gadgetbridge 未安装") }, supportingContent = { Text("请先安装 Gadgetbridge 应用并配对您的穿戴设备") })
                    } else if (dbExists && hasStoragePermission) {
                        item(headlineContent = { Text("✓ 数据读取正常") }, supportingContent = { Text("已找到数据库文件并拥有存储权限，AI可以读取健康数据") })
                    }
                }
            }
            }


            // 设置闹钟
            item {
            CardGroup(
                title = { Text("设置闹钟") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Watch01, contentDescription = null) },
                    headlineContent = { Text("启用闹钟工具") },
                    supportingContent = { Text("允许AI通过系统时钟应用为您设置设备闹钟") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.alarmEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(alarmEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.alarmEnabled) {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可以创建新闹钟，并可设置小时、分钟和闹钟标签。闹钟通过系统时钟应用设置。") }
                    )
                }
            }
            }

            // 设定定时器
            item {
            CardGroup(
                title = { Text("设定定时器") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Watch01, contentDescription = null) },
                    headlineContent = { Text("启用定时器工具") },
                    supportingContent = { Text("允许AI通过系统时钟应用为您设置倒计时定时器") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.timerEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(timerEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.timerEnabled) {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可以设置倒计时定时器，可指定秒数和标签。定时器通过系统时钟应用设置。") }
                    )
                }
            }
            }


            // 电量信息
            item {
            CardGroup(
                title = { Text("电量信息") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.BatteryFull, contentDescription = null) },
                    headlineContent = { Text("启用电量工具") },
                    supportingContent = { Text("允许AI读取设备电量、充电状态、温度和健康信息，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.batteryEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(batteryEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.batteryEnabled) {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可以读取当前电量百分比、充电状态（充电中/未充电）、充电方式（USB/AC/无线）、电池温度和电池健康状态。此功能无需任何权限。") }
                    )
                }
            }
            }


            // 音乐控制
            item {
            CardGroup(
                title = { Text("音乐控制") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.MusicNote03, contentDescription = null) },
                    headlineContent = { Text("启用音乐控制工具") },
                    supportingContent = { Text("允许AI查看当前播放的音乐信息，并控制播放（播放、暂停、上一首、下一首、跳转），需要通知监听权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.musicEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(musicEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.musicEnabled) {
                    item(
                        headlineContent = { Text("通知监听权限") },
                        supportingContent = {
                            val cn = android.content.ComponentName(context, me.rerere.rikkahub.data.service.RikkaNotificationListenerService::class.java)
                            val enabled = try {
                                android.provider.Settings.Secure.getString(
                                    context.contentResolver,
                                    "enabled_notification_listeners"
                                )?.contains(cn.flattenToString()) == true
                            } catch (_: Exception) { false }
                            if (enabled) Text("✓ 通知监听权限已开启") else Text("⚠ 需要在系统设置中开启通知监听权限")
                        },
                        trailingContent = {
                            val cn = android.content.ComponentName(context, me.rerere.rikkahub.data.service.RikkaNotificationListenerService::class.java)
                            val enabled = try {
                                android.provider.Settings.Secure.getString(
                                    context.contentResolver,
                                    "enabled_notification_listeners"
                                )?.contains(cn.flattenToString()) == true
                            } catch (_: Exception) { false }
                            if (!enabled) {
                                FilledTonalButton(onClick = {
                                    try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) {}
                                }) { Text("去设置") }
                            }
                        }
                    )
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可以获取当前正在播放的音乐信息（标题、艺术家、专辑、播放状态），控制播放（播放、暂停、上一首、下一首、跳转进度），以及通过搜索播放音乐。需要开启通知监听权限才能使用。") }
                    )
                }
            }
            }


            // 短信读取
            item {
            CardGroup(
                title = { Text("短信读取") },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用短信读取工具") },
                    supportingContent = { Text("允许AI读取设备短信收件箱，支持按发件人、关键词和时间筛选") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.smsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !smsPermissionState.allPermissionsGranted) {
                                    smsPermissionState.requestPermissions()
                                }
                                updateSystemToolsSetting(systemToolsSetting.copy(smsEnabled = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.smsEnabled && !smsPermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 短信读取权限未授予") },
                        supportingContent = { Text("点击授权按钮授予短信读取权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { smsPermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
                if (systemToolsSetting.smsEnabled) {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可以读取短信收件箱中的消息，支持按条数、发件人、关键词和最近天数筛选。返回内容包括发件人、短信正文、时间和已读状态。") }
                    )
                }
            }
            }


            // 手电筒
            item {
            CardGroup(title = { Text("手电筒") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Flashlight, contentDescription = null) },
                    headlineContent = { Text("启用手电筒工具") },
                    supportingContent = { Text("允许AI开关设备手电筒，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.torchEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(torchEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // Toast提示
            item {
            CardGroup(title = { Text("Toast提示") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Megaphone01, contentDescription = null) },
                    headlineContent = { Text("启用Toast工具") },
                    supportingContent = { Text("允许AI弹出简短Toast提示，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.toastEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(toastEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 震动
            item {
            CardGroup(title = { Text("震动") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用震动工具") },
                    supportingContent = { Text("允许AI控制设备震动，支持单次或波形震动，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.vibrateEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(vibrateEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 屏幕亮度
            item {
            CardGroup(title = { Text("屏幕亮度") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Sun02, contentDescription = null) },
                    headlineContent = { Text("启用亮度工具") },
                    supportingContent = { Text("允许AI读取和设置屏幕亮度。写入亮度需要WRITE_SETTINGS特殊权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.brightnessEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(brightnessEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.brightnessEnabled) {
                    item(
                        headlineContent = { Text("修改系统设置权限") },
                        supportingContent = {
                            if (android.provider.Settings.System.canWrite(context)) Text("✓ 已授予修改系统设置权限") else Text("⚠ 设置亮度需要在系统设置中授予修改系统设置权限")
                        },
                        trailingContent = {
                            if (!android.provider.Settings.System.canWrite(context)) {
                                FilledTonalButton(onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                        intent.data = android.net.Uri.parse("package:${context.packageName}")
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }) { Text("去设置") }
                            }
                        }
                    )
                }
            }
            }


            // 音量控制
            item {
            CardGroup(title = { Text("音量控制") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Speaker01, contentDescription = null) },
                    headlineContent = { Text("启用音量工具") },
                    supportingContent = { Text("允许AI读取和设置系统音量。修改铃声/通知音量需要勿扰权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.volumeEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(volumeEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.volumeEnabled) {
                    item(
                        headlineContent = { Text("勿扰访问权限") },
                        supportingContent = {
                            val nm = context.getSystemService(android.app.NotificationManager::class.java)
                            val granted = try { nm?.isNotificationPolicyAccessGranted == true } catch (_: Exception) { false }
                            if (granted) Text("✓ 勿扰访问权限已授予") else Text("⚠ 修改铃声/通知音量需要勿扰访问权限")
                        },
                        trailingContent = {
                            val nm = context.getSystemService(android.app.NotificationManager::class.java)
                            val granted = try { nm?.isNotificationPolicyAccessGranted == true } catch (_: Exception) { false }
                            if (!granted) {
                                FilledTonalButton(onClick = {
                                    try { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) } catch (_: Exception) {}
                                }) { Text("去设置") }
                            }
                        }
                    )
                }
            }
            }


            // WiFi信息
            item {
            CardGroup(title = { Text("WiFi信息") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartphoneWifi, contentDescription = null) },
                    headlineContent = { Text("启用WiFi信息工具") },
                    supportingContent = { Text("允许AI读取WiFi连接信息（SSID/信号强度/IP等），需要位置权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.wifiInfoEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !locationPermissionState.allPermissionsGranted) locationPermissionState.requestPermissions()
                                updateSystemToolsSetting(systemToolsSetting.copy(wifiInfoEnabled = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.wifiInfoEnabled && !locationPermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 位置权限未授予") },
                        supportingContent = { Text("读取WiFi信息需要位置权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { locationPermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
            }
            }


            // 电话信息
            item {
            CardGroup(title = { Text("电话信息") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用电话信息工具") },
                    supportingContent = { Text("允许AI读取SIM卡/运营商/网络类型信息，需要READ_PHONE_STATE权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.telephonyInfoEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !phoneStatePermissionState.allPermissionsGranted) phoneStatePermissionState.requestPermissions()
                                updateSystemToolsSetting(systemToolsSetting.copy(telephonyInfoEnabled = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.telephonyInfoEnabled && !phoneStatePermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 电话状态权限未授予") },
                        supportingContent = { Text("点击授权按钮授予电话状态读取权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { phoneStatePermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
            }
            }


            // 分享
            item {
            CardGroup(title = { Text("分享") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Share05, contentDescription = null) },
                    headlineContent = { Text("启用分享工具") },
                    supportingContent = { Text("允许AI通过系统分享面板分享文字/URL，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.shareEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(shareEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 设置壁纸
            item {
            CardGroup(title = { Text("设置壁纸") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Image02, contentDescription = null) },
                    headlineContent = { Text("启用壁纸工具") },
                    supportingContent = { Text("允许AI从本地图片文件设置壁纸（主屏/锁屏/两者），无需特殊权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.setWallpaperEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(setWallpaperEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 唤醒屏幕
            item {
            CardGroup(title = { Text("唤醒屏幕") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.FullScreen, contentDescription = null) },
                    headlineContent = { Text("启用唤醒屏幕工具") },
                    supportingContent = { Text("允许AI唤醒黑屏设备，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.wakeScreenEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(wakeScreenEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 媒体扫描
            item {
            CardGroup(title = { Text("媒体扫描") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Scan, contentDescription = null) },
                    headlineContent = { Text("启用媒体扫描工具") },
                    supportingContent = { Text("允许AI通知媒体库扫描指定文件路径，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.scanMediaEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(scanMediaEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 发送通知
            item {
            CardGroup(title = { Text("发送通知") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.Notification02, contentDescription = null) },
                    headlineContent = { Text("启用发送通知工具") },
                    supportingContent = { Text("允许AI主动发送系统通知，需要通知权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.postNotificationEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !notificationPermissionState.allPermissionsGranted) notificationPermissionState.requestPermissions()
                                updateSystemToolsSetting(systemToolsSetting.copy(postNotificationEnabled = enabled))
                            }
                        )
                    }
                )
                if (systemToolsSetting.postNotificationEnabled && !notificationPermissionState.allPermissionsGranted) {
                    item(
                        headlineContent = { Text("⚠ 通知权限未授予") },
                        supportingContent = { Text("发送通知需要通知权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = { notificationPermissionState.requestPermissions() }) { Text("授权") }
                        }
                    )
                }
            }
            }


            // 存储信息
            item {
            CardGroup(title = { Text("存储信息") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.HardDrive, contentDescription = null) },
                    headlineContent = { Text("启用存储信息工具") },
                    supportingContent = { Text("允许AI读取内部/外部存储空间使用情况，无需额外权限") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.storageInfoEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(storageInfoEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // 应用切换
            item {
            CardGroup(title = { Text("应用切换") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用应用切换工具") },
                    supportingContent = { Text("允许 AI 启动其他应用、把橘瓣拉到前台、或用浏览器打开 URL") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.appSwitchEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(appSwitchEnabled = enabled)) }
                        )
                    }
                )
            }
            }


            // App 锁定
            item {
            CardGroup(title = { Text("App 锁定") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.SmartPhone01, contentDescription = null) },
                    headlineContent = { Text("启用 App 锁定工具") },
                    supportingContent = { Text("允许 AI 锁定指定 App；之后打开该 App 会被拦截，需输入密码验证才能继续使用。依赖无障碍服务检测前台应用，不是系统级强制限制，关闭无障碍权限即可绕过") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.appLockEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(appLockEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.appLockEnabled &&
                    !me.rerere.rikkahub.data.ai.tools.local.AccessibilityServiceHandle.isEnabledInSettings(context)
                ) {
                    item(
                        headlineContent = { Text("⚠ 无障碍服务未启用") },
                        supportingContent = { Text("App 锁定依赖无障碍服务检测前台应用切换，请在系统设置中开启橘瓣的无障碍权限") },
                        trailingContent = {
                            FilledTonalButton(onClick = {
                                try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
                            }) { Text("去设置") }
                        }
                    )
                }
            }
            }


            // 指纹验证
            item {
            CardGroup(title = { Text("指纹验证") }, modifier = Modifier.padding(horizontal = 8.dp)) {
                item(
                    leadingContent = { Icon(imageVector = HugeIcons.FingerPrint, contentDescription = null) },
                    headlineContent = { Text("启用指纹验证工具") },
                    supportingContent = { Text("允许 AI 弹出系统指纹/人脸验证框验证用户身份（如执行敏感操作前确认是本人）。需设备已录入指纹或人脸") },
                    trailingContent = {
                        Switch(
                            checked = systemToolsSetting.fingerprintEnabled,
                            onCheckedChange = { enabled -> updateSystemToolsSetting(systemToolsSetting.copy(fingerprintEnabled = enabled)) }
                        )
                    }
                )
                if (systemToolsSetting.fingerprintEnabled) {
                    item(
                        headlineContent = { Text("说明") },
                        supportingContent = { Text("AI 可在需要时调用 verify_fingerprint 工具弹出系统生物识别提示框。开启 allow_device_credential 后允许在无指纹或验证失败时回退到设备 PIN/密码。验证结果会返回给 AI（成功/取消/超时等）。") }
                    )
                }
            }
            }


        }

        PermissionManager(permissionState = locationPermissionState)
        PermissionManager(permissionState = notificationPermissionState)
        PermissionManager(permissionState = cameraPermissionState)
        PermissionManager(permissionState = smsPermissionState)
        PermissionManager(permissionState = phoneStatePermissionState)
    }
}
