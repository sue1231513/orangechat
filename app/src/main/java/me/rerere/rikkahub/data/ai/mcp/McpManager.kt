package me.rerere.rikkahub.data.ai.mcp

import android.util.Log
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.StringValues
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.RequestOptions
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.core.InputSchema
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.ai.mcp.transport.SseClientTransport
import me.rerere.rikkahub.data.ai.mcp.transport.StreamableHttpClientTransport
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.files.saveUploadFromBytes
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.checkDifferent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "McpManager"
private const val MAX_RECONNECT_ATTEMPTS = 5
private const val BASE_RECONNECT_DELAY_MS = 1000L
private const val MAX_RECONNECT_DELAY_MS = 30000L

class McpManager(
    private val settingsStore: SettingsStore,
    private val appScope: AppScope,
    private val filesManager: FilesManager,
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followSslRedirects(true)
        .followRedirects(true)
        .build()

    private val client = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(SSE)
    }

    private val clients: MutableMap<Uuid, Pair<McpServerConfig, Client>> = mutableMapOf()
    private val reconnectJobs: MutableMap<Uuid, Job> = mutableMapOf()
    private val reconnectAttempts: MutableMap<Uuid, Int> = mutableMapOf()
    val syncingStatus = MutableStateFlow<Map<Uuid, McpStatus>>(mapOf())

    init {
        appScope.launch {
            settingsStore.settingsFlow
                .map { settings -> settings.mcpServers }
                .collect { mcpServerConfigs ->
                    runCatching {
                        Log.i(TAG, "update configs: $mcpServerConfigs")
                        val newConfigs = mcpServerConfigs.filter { it.commonOptions.enable }
                        val currentConfigs = clients.values.map { it.first }.toList()
                        val (toAdd, toRemove) = currentConfigs.checkDifferent(
                            other = newConfigs,
                            eq = { a, b ->
                                a.id == b.id &&
                                a.getUrl() == b.getUrl() &&
                                a.commonOptions.headers == b.commonOptions.headers
                            }
                        )
                        Log.i(TAG, "to_add: $toAdd")
                        Log.i(TAG, "to_remove: $toRemove")
                        toAdd.forEach { cfg ->
                            appScope.launch {
                                runCatching { addClient(cfg) }
                                    .onFailure { it.printStackTrace() }
                            }
                        }
                        toRemove.forEach { cfg ->
                            appScope.launch { removeClient(cfg) }
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
        }
    }

    fun getClient(config: McpServerConfig): Client? {
        return clients[config.id]?.second
    }

    fun getAllAvailableTools(): List<Pair<Uuid, McpTool>> {
        val settings = settingsStore.settingsFlow.value
        val assistant = settings.getCurrentAssistant()
        return settings.mcpServers
            .filter {
                it.commonOptions.enable && it.id in assistant.mcpServers
            }
            .flatMap { server ->
                server.commonOptions.tools
                    .filter { tool -> tool.enable }
                    .map { tool -> server.id to tool }
            }
    }

    suspend fun callTool(serverId: Uuid, toolName: String, args: JsonObject): List<UIMessagePart> {
        val pair = clients[serverId]
        val client = pair?.second
            ?: return listOf(UIMessagePart.Text("Failed to execute tool, because no such mcp client for the tool"))
        val config = pair.first
        Log.i(TAG, "callTool: $toolName / $args (server: ${config.commonOptions.name})")

        if (client.transport == null) client.connect(getTransport(config))
        val result = client.callTool(
            request = CallToolRequest(
                params = CallToolRequestParams(
                    name = toolName,
                    arguments = args,
                ),
            ),
            options = RequestOptions(timeout = 120.seconds),
        )
        return result.content.map {
            when (it) {
                is TextContent -> convertTextContentToAudioPart(it.text) ?: UIMessagePart.Text(it.text)
                is ImageContent -> convertImageContentToFilePart(it)
                else -> UIMessagePart.Text(JsonInstant.encodeToString(it))
            }
        }
    }

    private suspend fun convertTextContentToAudioPart(text: String): UIMessagePart.Audio? {
        val payload = runCatching { JsonInstant.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val nestedData = payload["data"]?.jsonObjectOrNull()
        val declaredMimeType = payload.string("mimeType")
            ?: payload.string("mime")
            ?: payload.string("mediaType")
            ?: payload.string("contentType")
            ?: nestedData?.let {
                it.string("mimeType") ?: it.string("mime") ?: it.string("mediaType") ?: it.string("contentType")
            }
        val mimeType = declaredMimeType ?: "audio/mpeg"
        val declaredType = payload.string("type")
            ?: payload.string("kind")
            ?: nestedData?.let { it.string("type") ?: it.string("kind") }
        val looksLikeAudio = declaredType?.contains("audio", ignoreCase = true) == true ||
            declaredType?.contains("voice", ignoreCase = true) == true ||
            declaredMimeType?.startsWith("audio/", ignoreCase = true) == true

        val url = payload.string("url")
            ?: payload.string("audioUrl")
            ?: payload.string("audio_url")
            ?: payload.string("fileUrl")
            ?: nestedData?.let {
                it.string("url") ?: it.string("audioUrl") ?: it.string("audio_url") ?: it.string("fileUrl")
            }
        if (url != null && (looksLikeAudio || url.isAudioUrl())) {
            return UIMessagePart.Audio(url = url)
        }

        val encodedAudio = payload.string("audio")
            ?: payload.string("base64")
            ?: payload.string("data")
            ?: payload.string("content")
            ?: nestedData?.let {
                it.string("audio") ?: it.string("base64") ?: it.string("data") ?: it.string("content")
            }
            ?: return null

        if (!looksLikeAudio && !encodedAudio.startsWith("data:audio/", ignoreCase = true)) {
            return null
        }

        val encoding = payload.string("encoding")
            ?: payload.string("output_format")
            ?: payload.string("format")
            ?: nestedData?.let {
                it.string("encoding") ?: it.string("output_format") ?: it.string("format")
            }
        val bytes = decodeAudioPayload(encodedAudio, encoding) ?: return null
        val entity = filesManager.saveUploadFromBytes(
            bytes = bytes,
            displayName = "mcp_audio.${audioExtension(mimeType)}",
            mimeType = mimeType,
        )
        val uri = filesManager.getFile(entity).toUri()
        Log.i(TAG, "convertTextContentToAudioPart: saved mcp audio to $uri")
        return UIMessagePart.Audio(url = uri.toString())
    }

    private fun decodeAudioPayload(value: String, encoding: String?): ByteArray? {
        val trimmed = value.trim()
        val dataPayload = if (trimmed.startsWith("data:audio/", ignoreCase = true)) {
            trimmed.substringAfter("base64,", missingDelimiterValue = "")
        } else {
            trimmed
        }
        if (dataPayload.isBlank()) return null

        return runCatching {
            when {
                encoding.equals("hex", ignoreCase = true) || dataPayload.isHexAudioPayload() ->
                    dataPayload.hexToBytes()
                else -> Base64.decode(dataPayload)
            }
        }.getOrNull()
    }

    private fun String.isHexAudioPayload(): Boolean {
        val compact = replace("\\s+".toRegex(), "")
        return compact.length > 16 &&
            compact.length % 2 == 0 &&
            compact.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun String.hexToBytes(): ByteArray {
        val compact = replace("\\s+".toRegex(), "")
        return ByteArray(compact.length / 2) { index ->
            compact.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun String.isAudioUrl(): Boolean {
        val clean = substringBefore('?').lowercase()
        return clean.startsWith("file:") ||
            clean.startsWith("content:") ||
            clean.endsWith(".mp3") ||
            clean.endsWith(".wav") ||
            clean.endsWith(".m4a") ||
            clean.endsWith(".aac") ||
            clean.endsWith(".ogg") ||
            clean.endsWith(".opus")
    }

    private fun audioExtension(mimeType: String): String {
        return android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: when (mimeType.lowercase()) {
                "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
                "audio/aac" -> "aac"
                "audio/ogg" -> "ogg"
                "audio/opus" -> "opus"
                else -> "mp3"
            }
    }
    private suspend fun convertImageContentToFilePart(image: ImageContent): UIMessagePart.Image {
        val bytes = Base64.decode(image.data)
        val ext = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(image.mimeType) ?: "bin"
        val entity = filesManager.saveUploadFromBytes(
            bytes = bytes,
            displayName = "mcp_image.$ext",
            mimeType = image.mimeType,
        )
        val uri = filesManager.getFile(entity).toUri()
        Log.i(TAG, "convertImageContentToFilePart: saved mcp image to $uri")
        return UIMessagePart.Image(url = uri.toString())
    }

    private fun getTransport(config: McpServerConfig): AbstractTransport = when (config) {
        is McpServerConfig.SseTransportServer -> {
            SseClientTransport(
                urlString = config.url,
                client = client,
                requestBuilder = {
                    headers.appendAll(StringValues.build {
                        config.commonOptions.headers.forEach {
                            append(it.first, it.second)
                        }
                    })
                },
            )
        }

        is McpServerConfig.StreamableHTTPServer -> {
            StreamableHttpClientTransport(
                url = config.url,
                client = client,
                requestBuilder = {
                    headers.appendAll(StringValues.build {
                        config.commonOptions.headers.forEach {
                            append(it.first, it.second)
                        }
                    })
                }
            )
        }
    }

    private fun McpServerConfig.getUrl(): String = when (this) {
        is McpServerConfig.SseTransportServer -> url
        is McpServerConfig.StreamableHTTPServer -> url
    }

    suspend fun addClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        removeClient(config) // Remove first
        cancelReconnect(config.id)
        reconnectAttempts[config.id] = 0

        val transport = getTransport(config)
        val client = Client(
            clientInfo = Implementation(
                name = config.commonOptions.name,
                version = "1.0",
            )
        )

        // 注册 transport 回调以支持自动重连
        transport.onClose {
            Log.i(TAG, "Transport closed for ${config.commonOptions.name}")
            val currentStatus = syncingStatus.value[config.id]
            // 只有在已连接状态下才触发重连，避免正常关闭时重连
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        transport.onError { error ->
            Log.e(TAG, "Transport error for ${config.commonOptions.name}: ${error.message}")
            val currentStatus = syncingStatus.value[config.id]
            // 只有在已连接状态下才触发重连
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        clients[config.id] = Pair(config, client)
        runCatching {
            setStatus(config = config, status = McpStatus.Connecting)
            client.connect(transport)
            sync(config)
            setStatus(config = config, status = McpStatus.Connected)
            reconnectAttempts[config.id] = 0 // 重置重连计数
            Log.i(TAG, "addClient: connected ${config.commonOptions.name}")
        }.onFailure {
            it.printStackTrace()
            setStatus(config = config, status = McpStatus.Error(it.message ?: it.javaClass.name))
        }
    }

    private suspend fun sync(config: McpServerConfig) {
        val client = clients[config.id]?.second ?: return

        setStatus(config = config, status = McpStatus.Connecting)

        // Update tools
        if (client.transport == null) {
            client.connect(getTransport(config))
        }
        val serverTools = client.listTools()?.tools ?: emptyList()
        Log.i(TAG, "sync: tools: $serverTools")

        // 在 lambda 外构建新的 tools 列表
        val common = config.commonOptions
        val tools = common.tools.toMutableList()

        // 基于server对比
        serverTools.forEach { serverTool ->
            val tool = tools.find { it.name == serverTool.name }
            if (tool == null) {
                tools.add(
                    McpTool(
                        name = serverTool.name,
                        description = serverTool.description,
                        enable = true,
                        inputSchema = serverTool.inputSchema.toSchema()
                    )
                )
            } else {
                val index = tools.indexOf(tool)
                tools[index] = tool.copy(
                    description = serverTool.description,
                    inputSchema = serverTool.inputSchema.toSchema()
                )
            }
        }

        // 删除不在server内的
        tools.removeIf { tool -> serverTools.none { it.name == tool.name } }

        // 构造更新后的 config
        val updatedConfig = config.clone(
            commonOptions = common.copy(
                tools = tools
            )
        )

        // 单次原子覆盖写，消除 remove+put 的空档期
        clients[config.id] = Pair(updatedConfig, client)

        // 纯数据持久化：只把匹配 id 的 serverConfig 换成 updatedConfig
        settingsStore.update { old ->
            old.copy(
                mcpServers = old.mcpServers.map { serverConfig ->
                    if (serverConfig.id == config.id) updatedConfig else serverConfig
                }
            )
        }

        setStatus(config = config, status = McpStatus.Connected)
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        clients.values.map { it.first }.toList().forEach { config ->
            runCatching {
                sync(config)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    suspend fun removeClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        cancelReconnect(config.id)
        val entry = clients.remove(config.id)
        if (entry != null) {
            runCatching {
                entry.second.close()
            }.onFailure {
                it.printStackTrace()
            }
            syncingStatus.emit(syncingStatus.value.toMutableMap().apply { remove(config.id) })
            Log.i(TAG, "removeClient: ${entry.first} / ${entry.first.commonOptions.name}")
        }
        reconnectAttempts.remove(config.id)
    }

    private fun scheduleReconnect(config: McpServerConfig) {
        val configId = config.id
        val currentAttempt = (reconnectAttempts[configId] ?: 0) + 1

        if (currentAttempt > MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached for ${config.commonOptions.name}")
            appScope.launch {
                setStatus(config, McpStatus.Error("连接断开，已达最大重连次数"))
            }
            return
        }

        reconnectAttempts[configId] = currentAttempt

        // 取消之前的重连任务
        reconnectJobs[configId]?.cancel()

        // 计算指数退避延迟
        val delayMs = calculateBackoffDelay(currentAttempt)
        Log.i(TAG, "Scheduling reconnect for ${config.commonOptions.name}, attempt $currentAttempt/$MAX_RECONNECT_ATTEMPTS, delay ${delayMs}ms")

        reconnectJobs[configId] = appScope.launch {
            try {
                setStatus(config, McpStatus.Reconnecting(currentAttempt, MAX_RECONNECT_ATTEMPTS))
                delay(delayMs)

                // 检查配置是否仍然启用
                val currentConfig = settingsStore.settingsFlow.value.mcpServers
                    .find { it.id == configId && it.commonOptions.enable }

                if (currentConfig == null) {
                    Log.i(TAG, "Config disabled or removed, cancelling reconnect for ${config.commonOptions.name}")
                    return@launch
                }

                Log.i(TAG, "Attempting reconnect for ${config.commonOptions.name}")
                reconnectClient(currentConfig)
            } catch (e: CancellationException) {
                Log.i(TAG, "Reconnect cancelled for ${config.commonOptions.name}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Reconnect failed for ${config.commonOptions.name}", e)
                // 继续尝试重连
                scheduleReconnect(config)
            }
        }
    }

    private fun cancelReconnect(configId: Uuid) {
        reconnectJobs[configId]?.cancel()
        reconnectJobs.remove(configId)
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        // 指数退避: baseDelay * 2^(attempt-1)，最大不超过 maxDelay
        val exponentialDelay = BASE_RECONNECT_DELAY_MS * (1L shl (attempt - 1).coerceAtMost(10))
        return exponentialDelay.coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    private suspend fun reconnectClient(config: McpServerConfig) = withContext(Dispatchers.IO) {
        // 先关闭旧客户端
        val oldEntry = clients[config.id]
        if (oldEntry != null) {
            runCatching { oldEntry.second.close() }.onFailure { it.printStackTrace() }
            clients.remove(config.id)
        }

        val transport = getTransport(config)
        val client = Client(
            clientInfo = Implementation(
                name = config.commonOptions.name,
                version = "1.0",
            )
        )

        // 注册回调
        transport.onClose {
            Log.i(TAG, "Transport closed for ${config.commonOptions.name}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        transport.onError { error ->
            Log.e(TAG, "Transport error for ${config.commonOptions.name}: ${error.message}")
            val currentStatus = syncingStatus.value[config.id]
            if (currentStatus == McpStatus.Connected) {
                scheduleReconnect(config)
            }
        }

        clients[config.id] = Pair(config, client)
        setStatus(config, McpStatus.Connecting)
        client.connect(transport)
        sync(config)
        setStatus(config, McpStatus.Connected)
        reconnectAttempts[config.id] = 0 // 重置重连计数
        Log.i(TAG, "Reconnected successfully: ${config.commonOptions.name}")
    }

    private suspend fun setStatus(config: McpServerConfig, status: McpStatus) {
        syncingStatus.emit(syncingStatus.value.toMutableMap().apply {
            put(config.id, status)
        })
    }

    fun getStatus(config: McpServerConfig): Flow<McpStatus> {
        return syncingStatus.map { it[config.id] ?: McpStatus.Idle }
    }
}

internal val McpJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminatorMode = ClassDiscriminatorMode.NONE
        explicitNulls = false
    }
}

private fun ToolSchema.toSchema(): InputSchema {
    return InputSchema.Obj(properties = this.properties ?: JsonObject(emptyMap()), required = this.required)
}
