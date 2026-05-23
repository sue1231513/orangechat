package me.rerere.rikkahub.data.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.app.NotificationCompat
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.data.datastore.ProactiveMessageSetting
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.toMessageNode
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.utils.sendNotification
import java.time.Instant
import kotlin.uuid.Uuid
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ProactiveMessageService : KoinComponent {
    private val settingsStore: SettingsStore by inject()
    private val conversationRepository: ConversationRepository by inject()

    companion object {
        const val TAG = "ProactiveMessageService"
        const val ACTION_PROACTIVE_MESSAGE = "me.rerere.rikkahub.PROACTIVE_MESSAGE"
        private const val REQUEST_CODE = 10001

        private const val PREFS_NAME = "proactive_message_prefs"
        private const val KEY_NEXT_TRIGGER_TIME = "next_trigger_time"

        fun scheduleNext(context: Context, setting: ProactiveMessageSetting) {
            if (!setting.enabled) {
                cancel(context)
                return
            }

            val minMinutes = setting.minIntervalMinutes.coerceAtLeast(1)
            val maxMinutes = setting.maxIntervalMinutes.coerceAtLeast(minMinutes)
            val delayMinutes = Random.nextInt(minMinutes, maxMinutes + 1)
            val triggerTime = java.lang.System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes.toLong())

            // 保存下次触发时间到SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_NEXT_TRIGGER_TIME, triggerTime)
                .apply()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProactiveMessageReceiver::class.java).apply {
                action = ACTION_PROACTIVE_MESSAGE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            Log.d(TAG, "Scheduled proactive message in $delayMinutes minutes, trigger at ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(triggerTime))}")
        }

        fun getNextTriggerTime(context: Context): Long? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val triggerTime = prefs.getLong(KEY_NEXT_TRIGGER_TIME, 0L)
            return if (triggerTime > 0) triggerTime else null
        }

        fun cancel(context: Context) {
            // 清除保存的触发时间
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_NEXT_TRIGGER_TIME)
                .apply()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ProactiveMessageReceiver::class.java).apply {
                action = ACTION_PROACTIVE_MESSAGE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                Log.d(TAG, "Cancelled proactive message alarm")
            }
        }

        fun resetTimer(context: Context, setting: ProactiveMessageSetting) {
            scheduleNext(context, setting)
        }

        fun triggerNow(context: Context, setting: ProactiveMessageSetting) {
            // 先安排下一次（写入SP让UI立即显示），再立即触发
            scheduleNext(context, setting)
            // 立即触发：直接启动TriggerService
            val serviceIntent = Intent(context, ProactiveMessageTriggerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }

    suspend fun buildProactiveContext(context: Context, settings: Settings): String {
        val sb = StringBuilder()
        sb.appendLine("[主动消息上下文]")

        // Time since last chat
        try {
            val lastMessageTime = getLastMessageTime()
            if (lastMessageTime != null) {
                val nowMs = java.lang.System.currentTimeMillis()
                val lastMs = lastMessageTime.toEpochMilliseconds()
                val diffMs = nowMs - lastMs
                val duration = diffMs.milliseconds
                val minutesAgo = duration.inWholeMinutes
                val hoursAgo = duration.inWholeHours
                when {
                    hoursAgo > 24 -> sb.appendLine("距离上次聊天: ${hoursAgo / 24}天${hoursAgo % 24}小时")
                    hoursAgo > 0 -> sb.appendLine("距离上次聊天: ${hoursAgo}小时${minutesAgo % 60}分钟")
                    else -> sb.appendLine("距离上次聊天: ${minutesAgo}分钟")
                }
            } else {
                sb.appendLine("距离上次聊天: 很久没有聊天了")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last message time", e)
        }

        // Current time
        val currentTime = java.lang.System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        sb.appendLine("当前时间: ${sdf.format(java.util.Date(currentTime))}")

        // Location context
        try {
            val amapApiKey = settings.systemToolsSetting.amapApiKey
            if (amapApiKey.isNotBlank()) {
                val amapService = AmapService(amapApiKey)
                val locationService = LocationService(context, amapService)
                val locationResult = locationService.getCurrentLocation(amapApiKey)
                if (locationResult.isSuccess) {
                    val location = locationResult.getOrThrow()
                    if (location.address.isNotBlank()) {
                        sb.appendLine("当前位置: ${location.address}")
                    } else {
                        sb.appendLine("当前坐标: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get location context", e)
        }

        // App usage
        try {
            val appUsageService = AppUsageService(context)
            val usageResult = appUsageService.getTodayUsageStats()
            if (usageResult.isSuccess) {
                val usageStats = usageResult.getOrThrow()
                if (usageStats.isNotEmpty()) {
                    sb.appendLine("今日应用使用:")
                    usageStats.take(5).forEach { stat ->
                        val minutes = stat.totalTimeInForeground / 60000
                        if (minutes > 0) {
                            sb.appendLine("  - ${stat.appName}: ${minutes}分钟")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get app usage context", e)
        }

        // Foreground app
        try {
            val appUsageService = AppUsageService(context)
            val foregroundResult = appUsageService.getForegroundApp()
            if (foregroundResult.isSuccess) {
                val foregroundApp = foregroundResult.getOrThrow()
                if (foregroundApp.isNotBlank()) {
                    sb.appendLine("当前前台应用: $foregroundApp")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get foreground app", e)
        }

        sb.appendLine()
        sb.appendLine("请根据以上上下文，以自然、关心、有趣的方式主动给用户发一条消息。")
        sb.appendLine()
        sb.appendLine("重要规则：")
        sb.appendLine("- 不要提及你是在定时发消息，要像自然想起对方一样")
        sb.appendLine("- 绝对不要提及任何数据来源、工具使用、传感器数据、位置服务、应用使用统计等技术细节")
        sb.appendLine("- 不要说\"根据xxx\"、\"我注意到xxx数据\"之类暴露信息来源的话")
        sb.appendLine("- 直接以朋友聊天的语气开口，就像你突然想到了什么想跟对方说")
        sb.appendLine("- 不要使用任何XML标签、思考标记或特殊格式，只输出纯文本的消息内容")
        return sb.toString()
    }

    private suspend fun getLastMessageTime(): kotlinx.datetime.Instant? {
        return try {
            val settings = settingsStore.settingsFlow.first()
            val assistantId = settings.assistantId
            val recentConversations = conversationRepository.getRecentConversations(assistantId, limit = 1)
            if (recentConversations.isNotEmpty()) {
                val conv = recentConversations.first()
                val fullConv = conversationRepository.getConversationById(conv.id)
                val localDateTime: LocalDateTime? = fullConv?.messageNodes?.lastOrNull()?.messages?.lastOrNull()?.createdAt
                localDateTime?.toInstant(TimeZone.currentSystemDefault())
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last message time", e)
            null
        }
    }
}

class ProactiveMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ProactiveMessageService.ACTION_PROACTIVE_MESSAGE -> {
                Log.d(ProactiveMessageService.TAG, "Proactive message alarm triggered")
                val serviceIntent = Intent(context, ProactiveMessageTriggerService::class.java)
                context.startForegroundService(serviceIntent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(ProactiveMessageService.TAG, "Boot completed, rescheduling proactive message")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settingsStore = org.koin.core.context.GlobalContext.get().get<SettingsStore>()
                        val settings = settingsStore.settingsFlow.first()
                        val proactiveSetting = settings.proactiveMessageSetting
                        if (proactiveSetting.enabled) {
                            ProactiveMessageService.scheduleNext(context, proactiveSetting)
                        }
                    } catch (e: Exception) {
                        Log.e(ProactiveMessageService.TAG, "Failed to reschedule after boot", e)
                    }
                }
            }
        }
    }
}

class ProactiveMessageTriggerService : android.app.Service(), KoinComponent {
    private val settingsStore: SettingsStore by inject()
    private val conversationRepository: ConversationRepository by inject()
    private val providerManager: ProviderManager by inject()
    private val proactiveMessageService = ProactiveMessageService()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("正在思考...")
            .setSmallIcon(me.rerere.rikkahub.R.drawable.small_icon)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(20001, notification)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsStore.settingsFlow.first()
                val proactiveSetting = settings.proactiveMessageSetting

                if (!proactiveSetting.enabled) {
                    stopSelf()
                    return@launch
                }

                // 构建上下文
                val contextStr = proactiveMessageService.buildProactiveContext(
                    this@ProactiveMessageTriggerService, settings
                )

                // 获取助手
                val assistant = settings.assistants.find { it.id.toString() == proactiveSetting.assistantId }
                    ?: settings.getCurrentAssistant()
                val assistantUuid = assistant.id
                val model = settings.findModelById(assistant.chatModelId ?: settings.chatModelId)

                if (model == null) {
                    Log.e(ProactiveMessageService.TAG, "No model found for proactive message")
                    ProactiveMessageService.scheduleNext(this@ProactiveMessageTriggerService, proactiveSetting)
                    stopSelf()
                    return@launch
                }

                // 直接调用 AI API 生成消息
                val providerSetting = model.findProvider(settings.providers)
                if (providerSetting == null) {
                    Log.e(ProactiveMessageService.TAG, "No provider found for proactive message")
                    ProactiveMessageService.scheduleNext(this@ProactiveMessageTriggerService, proactiveSetting)
                    stopSelf()
                    return@launch
                }

                val providerImpl = providerManager.getProviderByType(providerSetting)
                val systemPrompt = assistant.systemPrompt.ifBlank { "你是一个友好的AI助手。" }
                val messages = listOf(
                    UIMessage(
                        role = MessageRole.SYSTEM,
                        parts = listOf(UIMessagePart.Text(systemPrompt))
                    ),
                    UIMessage(
                        role = MessageRole.USER,
                        parts = listOf(UIMessagePart.Text(
                            contextStr + "\n\n如果你觉得现在没什么好说的，或者没什么有趣的话题，请只回复 [PASS] 即可，不要强行找话题。"
                        ))
                    )
                )
                val params = TextGenerationParams(
                    model = model,
                    temperature = 0.8f
                )

                val result = providerImpl.generateText(providerSetting, messages, params)
                val replyText = result.choices.firstOrNull()?.message?.parts
                    ?.filterIsInstance<UIMessagePart.Text>()
                    ?.joinToString("") { it.text }
                    ?.trim() ?: ""

                if (replyText.isBlank() || replyText.contains("[PASS]")) {
                    // AI 选择跳过，不发通知
                    Log.d(ProactiveMessageService.TAG, "AI chose to skip proactive message")
                } else {
                    // 保存消息到对话并弹窗通知
                    val conversationId = saveProactiveMessage(settings, assistant, contextStr, replyText)
                    showProactiveNotification(conversationId, assistant.name.ifBlank { "AI" }, replyText)
                }

                ProactiveMessageService.scheduleNext(this@ProactiveMessageTriggerService, proactiveSetting)
            } catch (e: Exception) {
                Log.e(ProactiveMessageService.TAG, "Failed to trigger proactive message", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun saveProactiveMessage(
        settings: Settings,
        assistant: Assistant,
        contextStr: String,
        replyText: String
    ): kotlin.uuid.Uuid {
        val assistantUuid = assistant.id
        // 找到最近的对话，或创建新对话
        val recentConversations = conversationRepository.getRecentConversations(assistantUuid, limit = 1)
        val conversation = if (recentConversations.isNotEmpty()) {
            conversationRepository.getConversationById(recentConversations.first().id)
        } else null

        val conversationId = conversation?.id ?: kotlin.uuid.Uuid.random()
        if (conversation == null) {
            // 创建新对话
            val newConversation = Conversation(
                id = conversationId,
                assistantId = assistantUuid,
                title = "",
                messageNodes = emptyList()
            )
            conversationRepository.insertConversation(newConversation)
        }

        // 添加消息（只保存AI的主动消息，不保存上下文信息到对话历史）
        val existingConversation = conversationRepository.getConversationById(conversationId)
        val updatedConversation = (existingConversation ?: Conversation(
            id = conversationId,
            assistantId = assistantUuid,
            title = "",
            messageNodes = emptyList()
        )).let { conv ->
            conv.copy(
                messageNodes = conv.messageNodes + listOf(
                    UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = listOf(UIMessagePart.Text(replyText))
                    ).toMessageNode()
                ),
                updateAt = java.time.Instant.now()
            )
        }
        conversationRepository.updateConversation(updatedConversation)
        return conversationId
    }

    private fun showProactiveNotification(
        conversationId: kotlin.uuid.Uuid,
        senderName: String,
        message: String
    ) {
        val intent = Intent(this, RouteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId.toString())
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        sendNotification(
            channelId = CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID,
            notificationId = 20002
        ) {
            title = senderName
            content = message.take(100)
            autoCancel = true
            useDefaults = true
            category = NotificationCompat.CATEGORY_MESSAGE
            contentIntent = pendingIntent
            useBigTextStyle = true
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
