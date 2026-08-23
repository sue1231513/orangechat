/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.hooks

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getSelectedTTSProvider
import me.rerere.rikkahub.utils.stripMarkdown
import me.rerere.tts.model.PlaybackState
import me.rerere.tts.provider.TTSManager
import me.rerere.tts.provider.TTSProviderSetting
import me.rerere.tts.controller.TtsController
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "TTS"
private const val VOICE_MESSAGES_DIR = "voice_messages"

/**
 * Composable function to remember and manage custom TTS state.
 * Uses user-configured TTS providers instead of system TTS.
 */
@Composable
fun rememberCustomTtsState(): CustomTtsState {
    val context = LocalContext.current
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()

    val ttsState = remember {
        CustomTtsStateImpl(
            context = context.applicationContext,
            settingsStore = settingsStore
        )
    }

    DisposableEffect(settings.selectedTTSProviderId, settings.ttsProviders) {
        ttsState.updateProvider(settings.getSelectedTTSProvider())
        onDispose { }
    }

    DisposableEffect(ttsState) {
        onDispose {
            ttsState.cleanup()
        }
    }

    return ttsState
}

/**
 * 非 Compose 版本的工厂函数，供 Service 等非 UI 场景创建独立的 TTS 实例。
 *
 * suspend 函数，会真正挂起等待 provider 设置完成后才返回实例，
 * 消除之前 launch{} "发射后不管" 导致的 controller 为 null 竞态。
 */
suspend fun createCustomTtsState(
    context: Context,
    settingsStore: SettingsStore,
): CustomTtsState {
    val ttsState = CustomTtsStateImpl(context.applicationContext, settingsStore)
    try {
        val settings = settingsStore.settingsFlow.first()
        ttsState.updateProvider(settings.getSelectedTTSProvider())
    } catch (e: Exception) {
        Log.e("CreateCustomTtsState", "初始化 TTS provider 失败", e)
    }
    return ttsState
}

interface CustomTtsState {
    val isAvailable: StateFlow<Boolean>
    val isSpeaking: StateFlow<Boolean>
    val error: StateFlow<String?>
    val currentChunk: StateFlow<Int>
    val totalChunks: StateFlow<Int>
    val playbackState: StateFlow<PlaybackState>

    fun speak(text: String, flushCalled: Boolean = true)
    fun stop()
    fun pause()
    fun resume()
    fun skipNext()
    fun fastForward(ms: Long = 5_000)
    fun setSpeed(speed: Float)
    fun cleanup()

    /**
     * 把一条文本合成为可持久化的语音消息。
     * 与 speak() 分开：不进入实时播放队列，完整收完音频后写入 files/voice_messages，
     * 重听直接播本地文件，不再请求 provider 或消耗字数额度。
     */
    suspend fun createVoiceMessage(text: String): UIMessagePart.VoiceMessage?

    /**
     * 流式朗读: 追加一段文本到 TTS 队列, 不清空当前播放。
     * 用于语音通话中边生成边朗读的场景。
     */
    fun enqueueText(text: String)
}

internal class CustomTtsStateImpl(
    private val context: Context,
    private val settingsStore: SettingsStore
) : CustomTtsState, KoinComponent {

    private val ttsManager by inject<TTSManager>()

    private val controller by lazy {
        TtsController(context, ttsManager)
    }

    override val isAvailable: StateFlow<Boolean> get() = controller.isAvailable
    override val isSpeaking: StateFlow<Boolean> get() = controller.isSpeaking
    override val error: StateFlow<String?> get() = controller.error
    override val currentChunk: StateFlow<Int> get() = controller.currentChunk
    override val totalChunks: StateFlow<Int> get() = controller.totalChunks
    override val playbackState: StateFlow<PlaybackState> get() = controller.playbackState

    fun updateProvider(provider: TTSProviderSetting?) {
        controller.setProvider(provider)
    }

    override fun speak(text: String, flushCalled: Boolean) {
        val processed = text.stripMarkdown()
        controller.speak(processed, flushCalled)
    }

    override fun stop() {
        controller.stop()
    }

    override fun pause() {
        controller.pause()
        Log.d("CustomTtsState", "TTS paused")
    }

    override fun resume() {
        controller.resume()
        Log.d("CustomTtsState", "TTS resumed")
    }

    override fun skipNext() {
        controller.skipNext()
    }

    override fun fastForward(ms: Long) {
        controller.fastForward(ms)
    }

    override fun setSpeed(speed: Float) {
        controller.setSpeed(speed)
    }

    override fun enqueueText(text: String) {
        if (text.isBlank()) return
        val processed = text.stripMarkdown()
        if (processed.isBlank()) return
        controller.speak(processed, flush = false)
    }

    override suspend fun createVoiceMessage(text: String): UIMessagePart.VoiceMessage? =
        withContext(Dispatchers.IO) {
            val processed = text.stripMarkdown()
            if (processed.isBlank()) return@withContext null

            val provider = settingsStore.settingsFlow.first().getSelectedTTSProvider()
                ?: run {
                    Log.w(TAG, "createVoiceMessage: no TTS provider selected")
                    return@withContext null
                }

            val voiceDir = File(context.filesDir, VOICE_MESSAGES_DIR).apply { mkdirs() }
            val tempFile = File(voiceDir, ".${UUID.randomUUID()}.part")
            var extension = "mp3"
            try {
                FileOutputStream(tempFile).use { output ->
                    ttsManager.generateSpeech(provider, me.rerere.tts.model.TTSRequest(processed))
                        .collect { chunk ->
                            extension = when (chunk.format) {
                                me.rerere.tts.model.AudioFormat.MP3 -> "mp3"
                                me.rerere.tts.model.AudioFormat.WAV -> "wav"
                                me.rerere.tts.model.AudioFormat.OGG -> "ogg"
                                me.rerere.tts.model.AudioFormat.AAC -> "aac"
                                me.rerere.tts.model.AudioFormat.OPUS -> "opus"
                                me.rerere.tts.model.AudioFormat.PCM -> "pcm"
                            }
                            if (chunk.data.isNotEmpty()) output.write(chunk.data)
                        }
                }
                if (tempFile.length() == 0L) {
                    tempFile.delete()
                    return@withContext null
                }

                val voiceFile = File(voiceDir, "voice-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension")
                if (!tempFile.renameTo(voiceFile)) {
                    tempFile.copyTo(voiceFile, overwrite = true)
                    tempFile.delete()
                }
                val durationMs = runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, Uri.fromFile(voiceFile))
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                    } finally {
                        retriever.release()
                    }
                }.getOrDefault(0L)

                UIMessagePart.VoiceMessage(
                    url = Uri.fromFile(voiceFile).toString(),
                    duration = durationMs,
                    transcript = processed,
                )
            } catch (e: Exception) {
                Log.e(TAG, "createVoiceMessage failed", e)
                tempFile.delete()
                null
            }
        }

    override fun cleanup() {
        controller.dispose()
    }
}
