/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.ai.mood.MoodMode
import java.util.concurrent.atomic.AtomicInteger
import kotlin.uuid.Uuid

private const val TAG = "ConversationSession"
private const val IDLE_TIMEOUT_MS = 5_000L

class ConversationSession(
    val id: Uuid,
    initial: Conversation,
    private val scope: CoroutineScope,
    private val onIdle: (Uuid) -> Unit,
) {
    // 会话状态
    val state = MutableStateFlow(initial)

    // 原子引用计数
    private val refCount = AtomicInteger(0)

    // 处理状态（如 OCR 识别中）
    val processingStatus = MutableStateFlow<String?>(null)

    // Pelle d'Umore mood for this conversation
    val moodMode = MutableStateFlow(MoodMode.OFF)

    // 生成任务（内聚在 session 中）
    private val _generationJob = MutableStateFlow<Job?>(null)
    val generationJob: StateFlow<Job?> = _generationJob.asStateFlow()
    val isGenerating: Boolean get() = _generationJob.value?.isActive == true
    val isInUse: Boolean get() = refCount.get() > 0 || isGenerating

    /**
     * 保存互斥锁：保护"读取最新对话状态 -> 修改 messageNodes -> 落库"这一整段操作的原子性。
     *
     * 主消息生成(sendMessage)、语音通话挂断反馈(notifyVoiceCallDeclined)、标题生成(generateTitle)、
     * 建议生成(generateSuggestion)、重新生成(regenerateAtMessage)、工具审批(handleToolApproval)
     * 都可能对同一个 conversationId 并发触发保存 —— 它们各自都是"读旧对话 -> 追加/修改自己那部分 -> 整体存"，
     * 如果不加锁，谁后存谁就会把对方刚写入的消息覆盖掉。
     *
     * 这个锁只包裹"读-改-存"这个原子动作本身，不包裹耗时的 AI 生成请求过程，
     * 避免辅助任务（标题/建议/语音反馈）因为长时间持锁而互相卡住。
     */
    val saveMutex = Mutex()

    // 空闲检查任务
    private var idleCheckJob: Job? = null

    fun acquire(): Int = refCount.incrementAndGet().also {
        cancelIdleCheck()
        Log.d(TAG, "acquire $id (refs=$it)")
    }

    fun release(): Int = refCount.decrementAndGet().also {
        Log.d(TAG, "release $id (refs=$it)")
        if (it <= 0) scheduleIdleCheck()
    }

    // 作用域 API - 短请求（REST）
    inline fun <T> withRef(block: () -> T): T {
        acquire()
        try {
            return block()
        } finally {
            release()
        }
    }

    // 作用域 API - 长连接（SSE、挂起函数）
    suspend inline fun <T> withRefSuspend(block: () -> T): T {
        acquire()
        try {
            return block()
        } finally {
            release()
        }
    }

    fun setJob(job: Job?) {
        _generationJob.value?.cancel()
        _generationJob.value = job
        job?.invokeOnCompletion {
            _generationJob.value = null
            if (refCount.get() <= 0) {
                scheduleIdleCheck()
            }
        }
    }

    fun getJob(): Job? = _generationJob.value

    /**
     * 原子地尝试把 newJob 注册为当前会话的生成任务，仅当当前没有生成任务在进行时才成功。
     * 用于主动消息/激进模式等"低优先级"生成源：如果已有生成在跑（无论是正常聊天还是
     * 另一路主动消息），直接返回 false，调用方应放弃本次触发，而不是排队等待。
     *
     * 与 setJob() 的区别：setJob() 是"强制抢占"（会取消旧 job），用于用户主动发消息这种
     * 应该无条件打断旧生成的场景；tryClaimGeneration() 是"礼貌性尝试"，用于不应该打断
     * 已有生成的低优先级场景。
     *
     * 注意：调用方在拿不到 Job 引用（返回 false）时，应打印包含 conversationId 的 debug
     * 日志说明"因为会话正在生成中，跳过本次触发"，方便排查。
     */
    private val claimLock = Any()

    fun tryClaimGeneration(newJob: Job): Boolean = synchronized(claimLock) {
        if (isGenerating) return@synchronized false
        setJob(newJob)
        true
    }

    private fun scheduleIdleCheck() {
        idleCheckJob?.cancel()
        idleCheckJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            if (refCount.get() <= 0 && !isGenerating) {
                onIdle(id)
            }
        }
    }

    private fun cancelIdleCheck() {
        idleCheckJob?.cancel()
        idleCheckJob = null
    }

    fun cleanup() {
        _generationJob.value?.cancel()
        _generationJob.value = null
        idleCheckJob?.cancel()
        idleCheckJob = null
    }
}