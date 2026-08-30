/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Mic01
import me.rerere.hugeicons.stroke.MicOff01
import me.rerere.rikkahub.service.VoiceCallService
import me.rerere.rikkahub.ui.components.ui.permission.PermissionRecordAudio
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import kotlin.uuid.Uuid

private const val TAG = "VoiceCallPage"

// 暖色色板 (用户指定, 原值不动) - 不同状态对应不同主色
private val ColorIdle = Color(0xFF9D9A55)   // 暗卡其 - 准备就绪
private val ColorListening = Color(0xFFC6BD56) // 金黄 - 聆听
private val ColorProcessing = Color(0xFFFDAE4F) // 琥珀 - 思考
private val ColorSpeaking = Color(0xFFF58232)   // 橙 - 传达

// 暖色深底 (提亮一档, 不再死黑, 让暖色光晕透得出来)
private val ColorBgWarm = Color(0xFF241A0B)

/**
 * 状态 -> 主色
 */
private fun statusAccentColor(status: VoiceCallStatus): Color = when (status) {
    VoiceCallStatus.Idle -> ColorIdle
    VoiceCallStatus.Listening -> ColorListening
    VoiceCallStatus.Processing -> ColorProcessing
    VoiceCallStatus.Speaking -> ColorSpeaking
    VoiceCallStatus.Error -> Color(0xFFE5484D)
}

/**
 * 语音通话页面 (ChatGPT 独立语音模式风格)
 *
 * - 暖色深色背景 + 随状态微妙变色的径向光晕
 * - 流动光球 (颜色随状态变化)
 * - 多行流式字幕 (聆听/思考显示, 传达/就绪隐藏)
 * - 底部只有两个按钮: 静音 / 挂断
 * - 返回键 = 切后台继续通话 (不挂断)
 * - 业务逻辑全部跑在 VoiceCallService 里, 页面只负责 bind + 显示 uiState
 */
@Composable
fun VoiceCallPage(
    conversationId: Uuid,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var boundService by remember { mutableStateOf<VoiceCallService?>(null) }

    // 录音权限
    val asrPermission = rememberPermissionState(PermissionRecordAudio)

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                boundService = (binder as? VoiceCallService.LocalBinder)?.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
            }
        }
    }

    // bind/unbind Service. 关键: onDispose 只解绑, 绝不调用 endCall/stopService
    DisposableEffect(conversationId) {
        // 如果 Service 还没在跑这个对话的通话, 先 start 再 bind
        // 如果已经在跑 (用户是从通知点回来的), 只 bind, 不重复 start
        if (VoiceCallService.activeConversationId.value != conversationId.toString()) {
            // 权限检查: 没权限先请求, 拿到权限后再 start (见下方 LaunchedEffect)
            if (asrPermission.allRequiredPermissionsGranted) {
                VoiceCallService.start(context, conversationId.toString())
            }
        }
        val intent = Intent(context, VoiceCallService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                Log.e(TAG, "unbindService 失败", e)
            }
        }
    }

    // 权限授予后启动 Service (如果还没启动)
    LaunchedEffect(asrPermission.allRequiredPermissionsGranted) {
        if (asrPermission.allRequiredPermissionsGranted &&
            VoiceCallService.activeConversationId.value == null
        ) {
            VoiceCallService.start(context, conversationId.toString())
        }
    }

    // 进入页面时, 如果还没权限, 请求权限
    LaunchedEffect(Unit) {
        if (!asrPermission.allRequiredPermissionsGranted) {
            asrPermission.requestPermissions()
        }
    }

    // boundService 为 null (绑定还没完成) 时, 显示默认空状态
    val uiState by (boundService?.uiState
        ?: MutableStateFlow(VoiceCallUiState()).asStateFlow())
        .collectAsStateWithLifecycle(initialValue = VoiceCallUiState())

    // 返回键 = 切后台继续通话, 不挂断. 这是这次改动最核心的行为变化.
    BackHandler {
        onBack()
    }

    // 随状态平滑过渡的主色 (光球 / 标签 / 背景光晕共用)
    val accentColor by animateColorAsState(
        targetValue = statusAccentColor(uiState.status),
        animationSpec = tween(durationMillis = 800),
        label = "accentColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgWarm)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.45f),
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width / 2f,
                            size.height * 0.4f
                        ),
                        radius = size.maxDimension * 0.75f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部: 状态标签 + 毛玻璃浮层
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 60.dp)
            ) {
                Text(
                    text = statusText(uiState.status),
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // 中部: 流动光球 (颜色随状态变化)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.size(40.dp))

                VoiceOrb(
                    amplitudes = uiState.amplitudes,
                    status = uiState.status,
                    baseColor = accentColor,
                    size = 200.dp
                )

                // 绑定还没完成时, 显示一个小的加载指示器
                if (boundService == null) {
                    Spacer(modifier = Modifier.size(24.dp))
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.5f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 字幕区: 按状态切换显示谁的字幕 + 毛玻璃容器
            val subtitleText = when (uiState.status) {
                VoiceCallStatus.Listening,
                VoiceCallStatus.Processing -> uiState.userTranscript
                VoiceCallStatus.Speaking,
                VoiceCallStatus.Idle -> uiState.assistantText
                VoiceCallStatus.Error -> ""
            }
            if (subtitleText.isNotBlank()) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    StreamingSubtitle(
                        text = subtitleText,
                        accentColor = accentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 错误信息 (保留, 方便调试)
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            // 底部按钮区: 毛玻璃底板
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                // 静音按钮
                val canControl = boundService != null
                ControlButton(
                    icon = if (uiState.isMuted) HugeIcons.MicOff01 else HugeIcons.Mic01,
                    contentDescription = "静音",
                    onClick = {
                        boundService?.toggleMute()
                    },
                    backgroundColor = if (uiState.isMuted) {
                        Color.White.copy(alpha = 0.3f)
                    } else {
                        Color.White.copy(alpha = 0.15f)
                    },
                    iconTint = Color.White,
                    enabled = canControl
                )

                // 挂断按钮
                ControlButton(
                    icon = HugeIcons.Cancel01,
                    contentDescription = "挂断",
                    onClick = {
                        VoiceCallService.stop(context)
                        onBack()
                    },
                    backgroundColor = MaterialTheme.colorScheme.error,
                    iconTint = Color.White,
                    enabled = true
                )
                }
            }
        }
    }
}

/**
 * 多行流式字幕
 *
 * - 自动换行, 超出容器高度向上滚动, 始终显示最新文字
 * - Listening/Processing 状态使用; Speaking/Idle 由上层隐藏
 */
@Composable
private fun StreamingSubtitle(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    // 文本增长时滚到最底部, 让最新内容始终可见
    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 36.dp, vertical = 16.dp)
            .heightIn(max = 140.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text.ifBlank { " " },
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 控制按钮 (圆形)
 */
@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconTint: Color,
    size: Dp = 64.dp,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.3f),
        modifier = Modifier.size(size),
        enabled = enabled
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) iconTint else iconTint.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}

private fun statusText(status: VoiceCallStatus): String = when (status) {
    VoiceCallStatus.Idle -> "准备就绪"
    VoiceCallStatus.Listening -> "正在聆听"
    VoiceCallStatus.Processing -> "正在思考"
    VoiceCallStatus.Speaking -> "正在传达"
    VoiceCallStatus.Error -> "出错了"
}
