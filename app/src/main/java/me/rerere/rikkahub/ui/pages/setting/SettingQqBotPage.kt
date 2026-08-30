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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Message01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.QqBotSetting
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.service.QqBotService
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.RiskConfirmDialog
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

/**
 * QQ Bot 设置页.
 *
 * 比微信页简单: 不用扫码, 直接填 AppID + AppSecret (在 q.qq.com 注册机器人后获得).
 * 固定用当前助手.
 */
@Composable
fun SettingQqBotPage(vm: SettingVM = koinViewModel()) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var botSetting by remember(settings) { mutableStateOf(settings.qqBotSetting) }
    LaunchedEffect(settings) { botSetting = settings.qqBotSetting }

    fun update(newSetting: QqBotSetting) {
        botSetting = newSetting
        vm.updateSettings(settings.copy(qqBotSetting = newSetting))
    }

    var showEnableRiskDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showEnableRiskDialog) {
        RiskConfirmDialog(
            title = stringResource(R.string.risk_qq_bot_title),
            message = stringResource(R.string.risk_qq_bot_message),
            onConfirm = {
                showEnableRiskDialog = false
                update(botSetting.copy(enabled = true))
                QqBotService.start(context)
            },
            onDismiss = { showEnableRiskDialog = false }
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("QQ Bot [FIX1]") },
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
            // 说明
            item {
                CardGroup(
                    title = { Text("说明") },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    item(
                        leadingContent = { Icon(imageVector = HugeIcons.Message01, contentDescription = null) },
                        headlineContent = { Text("QQ Bot 是什么") },
                        supportingContent = { Text("把你的 QQ 机器人变成 AI 入口: 别人私聊你的 bot, 会由当前助手回复. 只处理私聊消息.") }
                    )
                    item(
                        headlineContent = { Text("怎么获取 AppID 和 Secret") },
                        supportingContent = { Text("1. 去 q.qq.com 注册开发者并创建机器人\n2. 在机器人管理页面找到 AppID 和 AppSecret\n3. 复制填到下面") }
                    )
                    item(
                        headlineContent = { Text("关联助手") },
                        supportingContent = { Text("固定使用当前助手: ${settings.getCurrentAssistant().name.ifBlank { "未命名" }}") }
                    )
                }
            }

            // 凭证
            item {
                CardGroup(
                    title = { Text("机器人凭证") },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    item(
                        headlineContent = { Text("AppID") },
                        supportingContent = {
                            OutlinedTextField(
                                value = botSetting.appId,
                                onValueChange = { update(botSetting.copy(appId = it.trim())) },
                                placeholder = { Text("如 102345678") },
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                )
                            )
                        }
                    )
                    item(
                        headlineContent = { Text("AppSecret") },
                        supportingContent = {
                            OutlinedTextField(
                                value = botSetting.appSecret,
                                onValueChange = { update(botSetting.copy(appSecret = it.trim())) },
                                placeholder = { Text("机器人密钥") },
                                modifier = Modifier.fillMaxSize(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = MaterialTheme.shapes.small,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                )
                            )
                        }
                    )
                }
            }

            // 开关
            item {
                CardGroup(
                    title = { Text("运行") },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    item(
                        headlineContent = { Text("启用 QQ Bot") },
                        supportingContent = { Text("开启后建立 WebSocket 连接监听私聊消息. 需先填 AppID 和 Secret.") },
                        trailingContent = {
                            Switch(
                                checked = botSetting.enabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        showEnableRiskDialog = true
                                    } else {
                                        update(botSetting.copy(enabled = false))
                                        QqBotService.stop(context)
                                    }
                                }
                            )
                        }
                    )
                    if (botSetting.enabled && (botSetting.appId.isBlank() || botSetting.appSecret.isBlank())) {
                        item(
                            headlineContent = { Text("⚠ 凭证未填写") },
                            supportingContent = { Text("请先填写 AppID 和 AppSecret, 再开启") }
                        )
                    }
                    if (botSetting.enabled) {
                        item(
                            headlineContent = { Text("运行提示") },
                            supportingContent = { Text("连接状态和错误请看 logcat (tag: QqBotService). token 会自动刷新. 被动回复需在收到消息 5 分钟内发出.") }
                        )
                    }
                }
            }
        }
    }
}
