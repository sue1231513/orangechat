/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.mcp

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabsIntent
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private const val TAG = "McpManager"
private const val MAX_RECONNECT_ATTEMPTS = 5
private const val BASE_RECONNECT_DELAY_MS = 1000L
private const val MAX_RECONNECT_DELAY_MS = 30000L

// OAuth 相关常量
private fun launchOAuthAuthorization(context: Context, authorizationUrl: String) {
    val intent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
    intent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.launchUrl(context, authorizationUrl.toUri())
}
private const val MCP_OAUTH_CALLBACK_PORT = 52_134
private const val MCP_OAUTH_CALLBACK_PATH = "/oauth/callback"
private val OAUTH_CALLBACK_TIMEOUT = 5.minutes

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

    private val oauthClient = McpOAuthClient(okHttpClient)
    private val oauthLoopbackCallbackServer = McpOAuthLoopbackCallbackServer(
        port = MCP_OAUTH_CALLBACK_PORT,
        path = MCP_OAUTH_CALLBACK_PATH,
    )

    private val clients: MutableMap<Uuid, Pair<McpServerConfig, Client>> = mutableMapOf()
    private val reconnectJobs: MutableMap<Uuid, Job> = mutableMapOf()
    private val reconnectAttempts: MutableMap<Uuid, Int> = mutableMapOf()
    private val authorizationJobs: MutableMap<Uuid, Job> = mutableMapOf()
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
            when(it) {
                is TextContent -> UIMessagePart.Text(it.text)
                is ImageContent -> convertImageContentToFilePart(it)
                else -> UIMessagePart.Text(JsonInstant.encodeToString(it))
            }
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
                        config.resolveHeaders().forEach {
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
                        config.resolveHeaders().forEach {
                            append(it.first, it.second)
                        }
                    })
                }
            )
        }
    }

    /** 合并用户自定义请求头与 OAuth Bearer 令牌。 */
    private fun McpServerConfig.resolveHeaders(): List<Pair<String, String>> {
        val base = commonOptions.headers
        val token = commonOptions.oauth?.takeIf { it.enabled }?.accessToken
        val hasAuthHeader = base.any { it.first.equals("Authorization", ignoreCase = true) }
        return if (!token.isNullOrBlank() && !hasAuthHeader) {
            base + ("Authorization" to "Bearer $token")
        } else {
            base
        }
    }

    private fun McpServerConfig.getUrl(): String = when (this) {
        is McpServerConfig.SseTransportServer -> url
        is McpServerConfig.StreamableHTTPServer -> url
    }

    suspend fun addClient(configInput: McpServerConfig) = withContext(Dispatchers.IO) {
        val config = ensureFreshToken(configInput)
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
            if (needsAuthorization(config, it)) {
                setStatus(config = config, status = McpStatus.NeedsAuthorization)
            } else {
                setStatus(config = config, status = McpStatus.Error(it.message ?: it.javaClass.name))
            }
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

    private suspend fun reconnectClient(configInput: McpServerConfig) = withContext(Dispatchers.IO) {
        val config = ensureFreshToken(configInput)
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
        runCatching {
            client.connect(transport)
            sync(config)
        }.onSuccess {
            setStatus(config, McpStatus.Connected)
            reconnectAttempts[config.id] = 0 // 重置重连计数
            Log.i(TAG, "Reconnected successfully: ${config.commonOptions.name}")
        }.onFailure { e ->
            // 令牌失效/需要授权时停止重连，引导用户重新授权
            if (needsAuthorization(config, e)) {
                cancelReconnect(config.id)
                setStatus(config, McpStatus.NeedsAuthorization)
            } else {
                throw e
            }
        }
    }

    private suspend fun setStatus(config: McpServerConfig, status: McpStatus) {
        syncingStatus.emit(syncingStatus.value.toMutableMap().apply {
            put(config.id, status)
        })
    }

    fun getStatus(config: McpServerConfig): Flow<McpStatus> {
        return syncingStatus.map { it[config.id] ?: McpStatus.Idle }
    }

    // =====================================================================
    // OAuth 2.1 授权 (MCP 规范 2025-11-25)
    // =====================================================================

    /**
     * 发起 OAuth 授权流程：发现元数据 -> 动态注册 -> 浏览器授权 -> 交换令牌 -> 重新连接。
     * 通过 [Context] 打开 Custom Tab，用户完成后经 deep link 回调继续。
     */
    fun startAuthorization(config: McpServerConfig, context: Context) {
        // 若已有进行中的授权，先取消，避免并发的挂起协程互相覆盖状态
        authorizationJobs.remove(config.id)?.cancel()
        val job = appScope.launch {
            setStatus(config, McpStatus.Authorizing)
            runCatching { authorizeInternal(config, context.applicationContext) }
                .onFailure {
                    // 用户主动取消：状态由 cancelAuthorization 负责回退，这里不覆盖
                    if (it is CancellationException) return@onFailure
                    it.printStackTrace()
                    setStatus(config, McpStatus.Error(it.message ?: "OAuth authorization failed"))
                }
        }
        authorizationJobs[config.id] = job
        job.invokeOnCompletion { authorizationJobs.remove(config.id, job) }
    }

    /** 取消进行中的 OAuth 授权（用户中止），并回退到需要授权状态。 */
    fun cancelAuthorization(config: McpServerConfig) {
        authorizationJobs.remove(config.id)?.cancel()
        appScope.launch { setStatus(config, McpStatus.NeedsAuthorization) }
    }

    private suspend fun authorizeInternal(config: McpServerConfig, context: Context) =
        withContext(Dispatchers.IO) {
            val serverUrl = config.serverUrl
            require(serverUrl.isNotBlank()) { "Server URL 为空，无法授权" }

            val prm = oauthClient.discoverProtectedResource(serverUrl)
            val issuer = prm.authorizationServers.firstOrNull()
                ?: error("受保护资源未声明授权服务器")
            val asMeta = oauthClient.discoverAuthorizationServer(issuer)
            val authEndpoint = asMeta.authorizationEndpoint
                ?: error("授权服务器缺少 authorization_endpoint")
            val tokenEndpoint = asMeta.tokenEndpoint
                ?: error("授权服务器缺少 token_endpoint")
            val scope = config.commonOptions.oauth?.scope
                ?: prm.scopesSupported?.joinToString(" ")
                ?: asMeta.scopesSupported?.joinToString(" ")

            val pkce = oauthClient.generatePkce()
            val state = oauthClient.generateState()
            val resource = McpOAuthClient.canonicalResource(serverUrl)
            val callbackSession = oauthLoopbackCallbackServer.openSession(state)
            val redirectUri = oauthLoopbackCallbackServer.redirectUri()
            McpOAuthCallbackService.start(context)
            try {
                val existing = config.commonOptions.oauth
                // The old custom-scheme registration cannot be reused: OAuth servers bind client_id to redirect_uri.
                val reuseClient = existing?.redirectUri == redirectUri && !existing.clientId.isNullOrBlank()
                var clientId = existing?.clientId.takeIf { reuseClient }
                var clientSecret = existing?.clientSecret.takeIf { reuseClient }
                if (clientId.isNullOrBlank()) {
                    val regEndpoint = asMeta.registrationEndpoint
                        ?: error("授权服务器不支持动态注册，且未预配置 client_id")
                    val reg = oauthClient.registerClient(
                        registrationEndpoint = regEndpoint,
                        clientName = config.commonOptions.name,
                        redirectUri = redirectUri,
                        scope = scope,
                    )
                    clientId = reg.clientId
                    clientSecret = reg.clientSecret
                }

                persistOAuthState(
                    config.id,
                    (existing ?: McpOAuthState()).copy(
                        enabled = true,
                        clientId = clientId,
                        clientSecret = clientSecret,
                        authorizationEndpoint = authEndpoint,
                        tokenEndpoint = tokenEndpoint,
                        registrationEndpoint = asMeta.registrationEndpoint,
                        redirectUri = redirectUri,
                        scope = scope,
                    )
                )

                val authUrl = oauthClient.buildAuthorizationUrl(
                    authorizationEndpoint = authEndpoint,
                    clientId = clientId,
                    redirectUri = redirectUri,
                    pkce = pkce,
                    state = state,
                    scope = scope,
                    resource = resource,
                )
                withContext(Dispatchers.Main) { launchOAuthAuthorization(context, authUrl) }

                val callback = callbackSession.await(OAUTH_CALLBACK_TIMEOUT)
                    ?: error("OAuth 授权超时")
                callback.error?.let { error(
                    if (callback.errorDescription.isNullOrBlank()) "授权失败: $it"
                    else "授权失败: $it (${callback.errorDescription})"
                ) }
                val code = callback.code ?: error("授权失败: 未返回授权码")

                val token = oauthClient.exchangeCode(
                    tokenEndpoint = tokenEndpoint,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    code = code,
                    codeVerifier = pkce.verifier,
                    redirectUri = redirectUri,
                    resource = resource,
                )
                persistOAuthState(
                    config.id,
                    McpOAuthState(
                        enabled = true,
                        clientId = clientId,
                        clientSecret = clientSecret,
                        authorizationEndpoint = authEndpoint,
                        tokenEndpoint = tokenEndpoint,
                        registrationEndpoint = asMeta.registrationEndpoint,
                        redirectUri = redirectUri,
                        scope = token.scope ?: scope,
                        accessToken = token.accessToken,
                        refreshToken = token.refreshToken,
                        expiresAt = computeExpiry(token.expiresIn),
                    )
                )

                val freshConfig = settingsStore.settingsFlow.value.mcpServers.find { it.id == config.id }
                    ?: config
                addClient(freshConfig)
            } finally {
                callbackSession.close()
                McpOAuthCallbackService.stop(context)
            }
        }

    /** 清除某个 Server 的 OAuth 授权状态（登出）。 */
    suspend fun clearAuthorization(config: McpServerConfig) {
        persistOAuthState(config.id, null)
    }

    /** 若令牌即将过期且存在 refresh_token，则提前刷新并持久化，返回更新后的配置。 */
    private suspend fun ensureFreshToken(config: McpServerConfig): McpServerConfig {
        val oauth = config.commonOptions.oauth ?: return config
        if (!oauth.enabled || oauth.refreshToken.isNullOrBlank()) return config
        val expired = oauth.expiresAt > 0 &&
            System.currentTimeMillis() >= oauth.expiresAt - TOKEN_REFRESH_LEEWAY_MS
        val needsRefresh = oauth.accessToken.isNullOrBlank() || expired
        if (!needsRefresh) return config

        val tokenEndpoint = oauth.tokenEndpoint ?: return config
        val clientId = oauth.clientId ?: return config
        return runCatching {
            val token = oauthClient.refreshToken(
                tokenEndpoint = tokenEndpoint,
                clientId = clientId,
                clientSecret = oauth.clientSecret,
                refreshToken = oauth.refreshToken,
                resource = McpOAuthClient.canonicalResource(config.serverUrl),
                scope = oauth.scope,
            )
            val updated = oauth.copy(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken ?: oauth.refreshToken,
                expiresAt = computeExpiry(token.expiresIn),
                scope = token.scope ?: oauth.scope,
            )
            persistOAuthState(config.id, updated)
            config.clone(commonOptions = config.commonOptions.copy(oauth = updated))
        }.getOrElse {
            Log.w(TAG, "Token refresh failed for ${config.commonOptions.name}: ${it.message}")
            config // 刷新失败仍用旧令牌尝试，失败会转为 NeedsAuthorization
        }
    }

    private suspend fun persistOAuthState(configId: Uuid, oauth: McpOAuthState?) {
        settingsStore.update { old ->
            old.copy(
                mcpServers = old.mcpServers.map { server ->
                    if (server.id != configId) server
                    else server.clone(commonOptions = server.commonOptions.copy(oauth = oauth))
                }
            )
        }
    }

    private fun computeExpiry(expiresIn: Long?): Long =
        if (expiresIn != null && expiresIn > 0) {
            System.currentTimeMillis() + expiresIn * 1000
        } else {
            0L
        }

    /**
     * 判断某次连接/同步失败是否应引导用户进行 OAuth 授权。
     *
     * 仅靠错误文本匹配 401/invalid_token 并不可靠：很多 MCP server 依赖用户手动填写
     * Authorization header，缺失时同样返回 401。因此在文本预筛之上进一步区分：
     * - 已开启 OAuth（此前授权过、令牌失效）→ 直接引导重新授权
     * - 用户手动配置了 Authorization header → 视为普通错误，尊重手动登录模式
     * - 其余情况 → 主动探测该 server 是否发布受保护资源元数据 (RFC 9728)，
     *   能发现才认为其支持 OAuth、需要授权
     */
    private suspend fun needsAuthorization(config: McpServerConfig, error: Throwable): Boolean {
        if (!looksUnauthorized(error)) return false
        // 已开启 OAuth：令牌失效，直接引导重新授权
        if (config.commonOptions.oauth?.enabled == true) return true
        // 用户手动配置了 Authorization header：属于手动登录模式，header 无效是用户配置问题
        val hasManualAuth = config.commonOptions.headers.any {
            it.first.equals("Authorization", ignoreCase = true)
        }
        if (hasManualAuth) return false
        // 主动探测：仅当 server 发布了受保护资源元数据 (protected resource metadata) 时才支持 OAuth
        return runCatching { oauthClient.discoverProtectedResource(config.serverUrl) }
            .onFailure { Log.i(TAG, "OAuth probe failed for ${config.commonOptions.name}: ${it.message}") }
            .isSuccess
    }

    /** 错误文本是否疑似未授权（HTTP 401 或 RFC 6750 定义的 OAuth token 错误）。 */
    private fun looksUnauthorized(error: Throwable): Boolean {
        val message = generateSequence(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return message.contains("401") ||
            message.contains("unauthorized") ||
            message.contains("invalid_token") ||
            message.contains("invalid access token") ||
            message.contains("missing or invalid")
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
