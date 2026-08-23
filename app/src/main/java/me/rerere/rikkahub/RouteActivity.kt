/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.rerere.highlight.Highlighter
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.db.DatabaseMigrationTracker
import me.rerere.rikkahub.data.db.MigrationState
import me.rerere.rikkahub.data.event.AppEvent
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.rikkahub.ui.activity.SafeModeActivity
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.ui.context.LocalASRState
import me.rerere.rikkahub.ui.context.LocalCurrentAssistant
import me.rerere.rikkahub.ui.context.LocalCurrentChatModel
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.context.LocalMcpServers
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalProviders
import me.rerere.rikkahub.ui.context.LocalQuickMessages
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalSharedTransitionScope
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.rikkahub.ui.hooks.readBooleanPreference
import me.rerere.rikkahub.ui.hooks.readStringPreference
import me.rerere.rikkahub.ui.hooks.rememberCustomAsrState
import me.rerere.rikkahub.ui.hooks.rememberCustomTtsState
import me.rerere.rikkahub.ui.pages.assistant.AssistantPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantBasicPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantExtensionsPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantLocalToolPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantMcpPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantMemoryPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantPromptPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantRequestPage
import me.rerere.rikkahub.ui.pages.backup.BackupPage
import me.rerere.rikkahub.ui.pages.chat.ChatPage
import me.rerere.rikkahub.ui.pages.debug.DebugPage
import me.rerere.rikkahub.ui.pages.developer.DeveloperPage
import me.rerere.rikkahub.ui.pages.disclaimer.DisclaimerPage
import me.rerere.rikkahub.ui.pages.disclaimer.LegalPage
import me.rerere.rikkahub.ui.pages.extensions.ExtensionsPage
import me.rerere.rikkahub.ui.pages.extensions.ExternalMemoriesPage
import me.rerere.rikkahub.ui.pages.extensions.PromptPage
import me.rerere.rikkahub.ui.pages.extensions.QuickMessagesPage
import me.rerere.rikkahub.ui.pages.extensions.SkillDetailPage
import me.rerere.rikkahub.ui.pages.extensions.SkillsPage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceDetailPage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspacePage
import me.rerere.rikkahub.ui.pages.extensions.workspace.WorkspaceTerminalPage
import me.rerere.rikkahub.ui.pages.favorite.FavoritePage
import me.rerere.rikkahub.ui.pages.health.HealthPage
import me.rerere.rikkahub.ui.pages.history.HistoryPage
import me.rerere.rikkahub.ui.pages.imggen.ImageGenPage
import me.rerere.rikkahub.ui.pages.log.LogPage
import me.rerere.rikkahub.ui.pages.security.SecurityAuditPage
import me.rerere.rikkahub.ui.pages.miniapp.MiniAppEditPage
import me.rerere.rikkahub.ui.pages.miniapp.MiniAppManagerPage
import me.rerere.rikkahub.ui.pages.miniapp.MiniAppPage
import me.rerere.rikkahub.ui.pages.search.SearchPage
import me.rerere.rikkahub.ui.pages.setting.SettingAboutPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayCodeInteractionPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayColorPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayGeneralPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayIllustrationPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayMessagePage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayNotificationPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayThemePage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayTransparencyPage
import me.rerere.rikkahub.ui.pages.setting.SettingThemePage
import me.rerere.rikkahub.ui.pages.setting.SettingDonatePage
import me.rerere.rikkahub.ui.pages.setting.SettingFilesPage
import me.rerere.rikkahub.ui.pages.setting.SettingMcpPage
import me.rerere.rikkahub.ui.pages.setting.SettingModelPage
import me.rerere.rikkahub.ui.pages.setting.SettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderDetailPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchDetailPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchPage
import me.rerere.rikkahub.ui.pages.setting.SettingSpeechPage
import me.rerere.rikkahub.ui.pages.setting.SettingWebPage
import me.rerere.rikkahub.ui.pages.setting.SettingSystemToolsPage
import me.rerere.rikkahub.ui.pages.setting.SecuritySettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingProactiveMessagePage
import me.rerere.rikkahub.ui.pages.setting.SettingWeixinBotPage
import me.rerere.rikkahub.ui.pages.setting.SettingQqBotPage
import me.rerere.rikkahub.ui.pages.setting.components.SettingsBackground
import me.rerere.rikkahub.plugin.webview.PluginWebViewPage
import me.rerere.rikkahub.ui.pages.memory.MemoryBankPage
import me.rerere.rikkahub.ui.components.ui.EmojiPickerPage
import me.rerere.rikkahub.ui.pages.share.handler.ShareHandlerPage
import me.rerere.rikkahub.ui.pages.stats.StatsPage
import me.rerere.rikkahub.ui.pages.translator.TranslatorPage
import me.rerere.rikkahub.ui.pages.voice.IncomingCallPage
import me.rerere.rikkahub.ui.pages.voice.VoiceCallPage
import me.rerere.rikkahub.service.VoiceCallService
import me.rerere.rikkahub.ui.pages.webview.WebViewPage
import me.rerere.rikkahub.ui.theme.THEME_BACKGROUND_SCRIM
import me.rerere.rikkahub.ui.theme.ScrimColors
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import me.rerere.rikkahub.utils.CrashHandler
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.service.KeepAliveService
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

private const val TAG = "RouteActivity"

class RouteActivity : ComponentActivity() {
    private val highlighter by inject<Highlighter>()
    private val okHttpClient by inject<OkHttpClient>()
    private val settingsStore by inject<SettingsStore>()
    private var navStack: MutableList<NavKey>? = null

    // Volume key listener registry — last registered handler wins
    internal val volumeKeyListeners = mutableListOf<(isVolumeUp: Boolean) -> Boolean>()

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isVolumeUp = when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> true
                KeyEvent.KEYCODE_VOLUME_DOWN -> false
                else -> return super.dispatchKeyEvent(event)
            }
            if (volumeKeyListeners.lastOrNull()?.invoke(isVolumeUp) == true) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        disableNavigationBarContrast()
        super.onCreate(savedInstanceState)

        if (CrashHandler.hasCrashed(this)) {
            startActivity(Intent(this, SafeModeActivity::class.java))
            finish()
            return
        }

        // 根据设置自动启动保活服务
        try {
            if (settingsStore.settingsFlow.value.keepAliveEnabled) {
                KeepAliveService.start(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "自动启动保活服务失败", e)
        }

        setContent {
            RikkahubTheme {
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .components {
                            add(OkHttpNetworkFetcherFactory(
                                callFactory = { okHttpClient },
                                cacheStrategy = { CacheControlCacheStrategy() },
                            ))
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                add(AnimatedImageDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                            add(SvgDecoder.Factory(scaleToDensity = true))
                        }
                        .build()
                }
                AppRoutes()
            }
        }
    }

    private fun disableNavigationBarContrast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    @Composable
    private fun ShareHandler(backStack: MutableList<NavKey>) {
        val shareIntent = remember {
            Intent().apply {
                action = intent?.action
                putExtra(Intent.EXTRA_TEXT, intent?.getStringExtra(Intent.EXTRA_TEXT))
                putExtra(Intent.EXTRA_STREAM, intent?.getStringExtra(Intent.EXTRA_STREAM))
                putExtra(Intent.EXTRA_PROCESS_TEXT, intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
            }
        }

        LaunchedEffect(backStack) {
            when (shareIntent.action) {
                Intent.ACTION_SEND -> {
                    val text = shareIntent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    val imageUri = shareIntent.getStringExtra(Intent.EXTRA_STREAM)
                    backStack.add(Screen.ShareHandler(text, imageUri))
                }

                Intent.ACTION_PROCESS_TEXT -> {
                    val text = shareIntent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
                    backStack.add(Screen.ShareHandler(text, null))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Navigate to the chat screen if a conversation ID is provided
        intent.getStringExtra("conversationId")?.let { text ->
            val autoStartVoice = intent.getBooleanExtra("autoStartVoice", false)
            navStack?.let { stack ->
                Snapshot.withMutableSnapshot {
                    // 检查是否已经在同一个对话页面
                    val existingIndex = stack.indexOfLast {
                        it is Screen.Chat && it.id == text
                    }
                    if (existingIndex >= 0) {
                        // 已经在同一个对话，移动到栈顶
                        val existing = stack.removeAt(existingIndex) as Screen.Chat
                        if (autoStartVoice) {
                            stack.add(existing.copy(autoStartVoice = true))
                        } else {
                            stack.add(existing)
                        }
                    } else {
                        // 添加新对话页面
                        stack.add(Screen.Chat(text, autoStartVoice = autoStartVoice))
                    }
                }
            }
        }

        // 新增逻辑: 从语音通话通知点击, 回到通话页面
        intent.getStringExtra("openVoiceCallConversationId")?.let { convId ->
            navStack?.let { stack ->
                Snapshot.withMutableSnapshot {
                    val alreadyOnVoiceCall = stack.lastOrNull().let {
                        it is Screen.VoiceCall && it.conversationId == convId
                    }
                    if (!alreadyOnVoiceCall) {
                        stack.add(Screen.VoiceCall(convId))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @Composable
    fun AppRoutes() {
        val toastState = rememberToasterState()
        val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val tts = rememberCustomTtsState()

        // 首次启动：未同意免责声明时强制展示
        if (!settings.disclaimerAccepted) {
            DisclaimerPage(
                onAccept = {
                    scope.launch {
                        settingsStore.update {
                            it.copy(
                                disclaimerAccepted = true,
                                disclaimerAcceptedAt = (System.currentTimeMillis() / 1000).toInt()
                            )
                        }
                    }
                },
                onDecline = { finish() }
            )
            return
        }
        val asr = rememberCustomAsrState()
        val eventBus = koinInject<AppEventBus>()
        val migrationState by DatabaseMigrationTracker.state.collectAsStateWithLifecycle()

        val startScreen = Screen.Chat(
            id = if (readBooleanPreference("create_new_conversation_on_start", true)) {
                Uuid.random().toString()
            } else {
                readStringPreference(
                    "lastConversationId",
                    Uuid.random().toString()
                ) ?: Uuid.random().toString()
            }
        )

        val backStack = rememberNavBackStack(startScreen)
        SideEffect { this@RouteActivity.navStack = backStack }

        ShareHandler(backStack)

        // 监听 App 事件: TTS 朗读 / AI 主动发起语音通话
        LaunchedEffect(tts) {
            eventBus.events.collect { event ->
                when (event) {
                    is AppEvent.Speak -> tts.speak(event.text)
                    is AppEvent.EmojiSelected -> { /* handled in UIAvatar */ }
                    is AppEvent.McpOAuthCallback -> Unit // 由 McpManager 消费
                    is AppEvent.RequestVoiceCall -> {
                        val convId = event.conversationId
                        // 单通话守卫: 已有通话进行中就不重复弹
                        if (VoiceCallService.activeConversationId.value != null) return@collect
                        // 防止重复叠加来电页
                        val alreadyIncoming = backStack.lastOrNull() is Screen.IncomingCall
                        if (!alreadyIncoming) {
                            backStack.add(Screen.IncomingCall(convId))
                        }
                    }
                }
            }
        }

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavController provides Navigator(backStack),
                LocalSharedTransitionScope provides this,
                LocalSettings provides settings,
                LocalDisplaySettings provides settings.displaySetting,
                LocalProviders provides settings.providers,
                LocalMcpServers provides settings.mcpServers,
                LocalQuickMessages provides settings.quickMessages,
                LocalCurrentAssistant provides settings.getCurrentAssistant(),
                LocalCurrentChatModel provides settings.getCurrentChatModel(),
                LocalHighlighter provides highlighter,
                LocalToaster provides toastState,
                LocalTTSState provides tts,
                LocalASRState provides asr,
            ) {
                Toaster(
                    state = toastState,
                    darkTheme = LocalDarkMode.current,
                    richColors = true,
                    alignment = Alignment.TopCenter,
                    showCloseButton = true,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .let { base ->
                            when (settings.themeId) {
                                "pearltide" -> {
                                    val scrim = THEME_BACKGROUND_SCRIM.getValue("pearltide")
                                    base
                                        .paint(
                                            painter = painterResource(id = R.drawable.pearltide_chat_bg),
                                            contentScale = ContentScale.Crop,
                                        )
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(scrim.top.toLong()),
                                                    Color(scrim.accent.toLong()).copy(alpha = 0.15f),
                                                    Color(scrim.bottom.toLong()).copy(alpha = 0.08f),
                                                    Color(scrim.bottom.toLong()),
                                                )
                                            )
                                        )
                                }

                                "harbor" -> {
                                    val scrim = THEME_BACKGROUND_SCRIM.getValue("harbor")
                                    val painted = base
                                        .paint(
                                            painter = painterResource(id = R.drawable.harbor_chat_bg),
                                            contentScale = ContentScale.Crop,
                                        )
                                    if (LocalDarkMode.current) {
                                        painted
                                            .background(Color(0xFF141A22).copy(alpha = 0.66f))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF1E2A36).copy(alpha = 0.38f),
                                                        Color(0xFF111820).copy(alpha = 0.72f),
                                                    )
                                                )
                                            )
                                    } else {
                                        painted.background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(scrim.top.toLong()),
                                                    Color(scrim.accent.toLong()).copy(alpha = 0.12f),
                                                    Color(scrim.bottom.toLong()).copy(alpha = 0.10f),
                                                    Color(scrim.bottom.toLong()),
                                                )
                                            )
                                        )
                                    }
                                }

                                "creamrose" -> {
                                    // 奶油玫瑰不复用珍珠潮汐蓝图。两态各自从底色起画：
                                    // 浅色是奶油纸感与低饱和玫瑰雾，深色是酒红褐的暗面，
                                    // 切深色时不靠把浅图压黑，避免蓝色从玻璃层底下透出来。
                                    if (LocalDarkMode.current) {
                                        base
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF2A1718),
                                                        Color(0xFF1C1412),
                                                        Color(0xFF241615),
                                                        Color(0xFF1C1412),
                                                    )
                                                )
                                            )
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF8B5558).copy(alpha = 0.20f),
                                                        Color(0xFF3A2428).copy(alpha = 0.10f),
                                                        Color.Transparent,
                                                    ),
                                                    radius = Float.POSITIVE_INFINITY,
                                                )
                                            )
                                    } else {
                                        base
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFFCF8),
                                                        Color(0xFFF6F1EB),
                                                        Color(0xFFF2E5DF),
                                                        Color(0xFFF6F1EB),
                                                    )
                                                )
                                            )
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFFD4A0A3).copy(alpha = 0.24f),
                                                        Color(0xFFF2D9D0).copy(alpha = 0.12f),
                                                        Color.Transparent,
                                                    ),
                                                    radius = Float.POSITIVE_INFINITY,
                                                )
                                            )
                                    }
                                }

                                else -> base
                            }
                        }
                ) {
                    SettingsBackground {
                        NavDisplay(
                            backStack = backStack,
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            modifier = Modifier.fillMaxSize(),
                            onBack = { backStack.removeLastOrNull() },
                            transitionSpec = {
                                if (backStack.size == 1) fadeIn() togetherWith fadeOut()
                                else {
                                    slideInHorizontally { it } togetherWith
                                        slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                                }
                            },
                            popTransitionSpec = {
                                slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                    slideOutHorizontally { it }
                            },
                            predictivePopTransitionSpec = {
                                slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                                    slideOutHorizontally { it }
                            },
                            entryProvider = entryProvider {
                            entry<Screen.Chat>(
                                metadata = NavDisplay.transitionSpec { fadeIn() togetherWith fadeOut() }
                                        + NavDisplay.popTransitionSpec { fadeIn() togetherWith fadeOut() }
                            ) { key ->
                                ChatPage(
                                    id = Uuid.parse(key.id),
                                    text = key.text,
                                    files = key.files.map { it.toUri() },
                                    nodeId = key.nodeId?.let { Uuid.parse(it) },
                                    autoStartVoice = key.autoStartVoice,
                                )
                            }

                            entry<Screen.ShareHandler> { key ->
                                ShareHandlerPage(
                                    text = key.text,
                                    image = key.streamUri
                                )
                            }

                            entry<Screen.History> {
                                HistoryPage()
                            }

                            entry<Screen.Favorite> {
                                FavoritePage()
                            }

                            entry<Screen.Assistant> {
                                AssistantPage()
                            }

                            entry<Screen.AssistantDetail> { key ->
                                AssistantDetailPage(key.id)
                            }

                            entry<Screen.AssistantBasic> { key ->
                                AssistantBasicPage(key.id)
                            }

                            entry<Screen.AssistantPrompt> { key ->
                                AssistantPromptPage(key.id)
                            }

                            entry<Screen.AssistantMemory> { key ->
                                AssistantMemoryPage(key.id)
                            }

                            entry<Screen.AssistantRequest> { key ->
                                AssistantRequestPage(key.id)
                            }

                            entry<Screen.AssistantMcp> { key ->
                                AssistantMcpPage(key.id)
                            }

                            entry<Screen.AssistantLocalTool> { key ->
                                AssistantLocalToolPage(key.id)
                            }

                            entry<Screen.AssistantInjections> { key ->
                                AssistantExtensionsPage(key.id)
                            }

                            entry<Screen.Translator> {
                                TranslatorPage()
                            }

                            entry<Screen.Setting> {
                                SettingPage()
                            }

                            entry<Screen.Backup> {
                                BackupPage()
                            }

                            entry<Screen.ImageGen> {
                                ImageGenPage()
                            }

                            entry<Screen.WebView> { key ->
                                WebViewPage(key.url, key.content)
                            }

                            entry<Screen.SettingDisplay> {
                                SettingDisplayPage()
                            }

                            entry<Screen.SettingTheme> {
                                SettingThemePage()
                            }

                            entry<Screen.SettingDisplayTheme> {
                                SettingDisplayThemePage()
                            }
                            entry<Screen.SettingDisplayColor> {
                                SettingDisplayColorPage()
                            }
                            entry<Screen.SettingDisplayTransparency> {
                                SettingDisplayTransparencyPage()
                            }
                            entry<Screen.SettingDisplayIllustration> {
                                SettingDisplayIllustrationPage()
                            }
                            entry<Screen.SettingDisplayMessage> {
                                SettingDisplayMessagePage()
                            }
                            entry<Screen.SettingDisplayCodeInteraction> {
                                SettingDisplayCodeInteractionPage()
                            }
                            entry<Screen.SettingDisplayGeneral> {
                                SettingDisplayGeneralPage()
                            }
                            entry<Screen.SettingDisplayNotification> {
                                SettingDisplayNotificationPage()
                            }

                            entry<Screen.SettingProvider> {
                                SettingProviderPage()
                            }

                            entry<Screen.SettingProviderDetail> { key ->
                                val id = Uuid.parse(key.providerId)
                                SettingProviderDetailPage(id = id)
                            }

                            entry<Screen.SettingModels> {
                                SettingModelPage()
                            }

                            entry<Screen.SettingAbout> {
                                SettingAboutPage()
                            }

                            entry<Screen.SettingSearch> {
                                SettingSearchPage()
                            }

                            entry<Screen.SettingSearchDetail> { key ->
                                val id = Uuid.parse(key.serviceId)
                                SettingSearchDetailPage(id)
                            }

                            entry<Screen.SettingSpeech> {
                                SettingSpeechPage()
                            }

                            entry<Screen.SettingMcp> {
                                SettingMcpPage()
                            }

                            entry<Screen.SettingDonate> {
                                SettingDonatePage()
                            }

                            entry<Screen.SettingFiles> {
                                SettingFilesPage()
                            }

                            entry<Screen.SettingWeb> {
                                SettingWebPage()
                            }

                            entry<Screen.Developer> {
                                DeveloperPage()
                            }

                            entry<Screen.Debug> {
                                DebugPage()
                            }

                            entry<Screen.Log> {
                                LogPage()
                            }

                            entry<Screen.SecurityAudit> {
                                SecurityAuditPage()
                            }

                            entry<Screen.Extensions> {
                                ExtensionsPage()
                            }

                            entry<Screen.QuickMessages> {
                                QuickMessagesPage()
                            }

                            entry<Screen.ExternalMemories> {
                                ExternalMemoriesPage()
                            }

                            entry<Screen.Prompts> {
                                PromptPage()
                            }

                            entry<Screen.Skills> {
                                SkillsPage()
                            }

                            entry<Screen.SkillDetail> { key ->
                                SkillDetailPage(skillName = key.skillName)
                            }

                            entry<Screen.Workspaces> {
                                WorkspacePage()
                            }

                            entry<Screen.WorkspaceDetail> { key ->
                                WorkspaceDetailPage(key.id)
                            }

                            entry<Screen.WorkspaceTerminal> { key ->
                                WorkspaceTerminalPage(key.id)
                            }

                            entry<Screen.MessageSearch> {
                                SearchPage()
                            }

                            entry<Screen.Health> {
                                HealthPage()
                            }

                            entry<Screen.Stats> {
                                StatsPage()
                            }

                            entry<Screen.SettingSystemTools> {
                                SettingSystemToolsPage()
                            }

                            entry<Screen.SettingSecurity> {
                                SecuritySettingPage()
                            }

                            entry<Screen.SettingProactiveMessage> {
                                SettingProactiveMessagePage()
                            }

                            entry<Screen.SettingWeixinBot> {
                                SettingWeixinBotPage()
                            }

                            entry<Screen.SettingQqBot> {
                                SettingQqBotPage()
                            }

                            entry<Screen.Workflows> {
                                me.rerere.rikkahub.workflow.ui.WorkflowsScreen()
                            }

                            entry<Screen.WorkflowDetail> { key ->
                                me.rerere.rikkahub.workflow.ui.WorkflowDetailScreen(key.id)
                            }

                            entry<Screen.SettingPlugins> {
                                val nav = LocalNavController.current
                                me.rerere.rikkahub.plugin.ui.PluginManagePage(
                                    onNavigateToFolder = { folderId ->
                                        nav.navigate(Screen.PluginFolder(folderId))
                                    },
                                    onNavigateToDetail = { pluginId ->
                                        nav.navigate(Screen.PluginDetail(pluginId))
                                    }
                                )
                            }

                            entry<Screen.PluginFolder> { key ->
                                val nav = LocalNavController.current
                                me.rerere.rikkahub.plugin.ui.PluginFolderPage(
                                    folderId = key.folderId,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                    onNavigateToDetail = { pluginId ->
                                        nav.navigate(Screen.PluginDetail(pluginId))
                                    }
                                )
                            }

                            entry<Screen.PluginDetail> { key ->
                                val nav = LocalNavController.current
                                me.rerere.rikkahub.plugin.ui.PluginDetailPage(
                                    pluginId = key.pluginId,
                                    onNavigateBack = { backStack.removeLastOrNull() },
                                    onNavigateToCustomPage = { customPage ->
                                        when (customPage) {
                                            "memory_bank" -> nav.navigate(Screen.MemoryBank)
                                        }
                                    },
                                    onNavigateToWebView = { pluginId, entryPath ->
                                        nav.navigate(Screen.PluginWebView(pluginId, entryPath))
                                    },
                                    onNavigateToDeclarativeUI = { pluginId ->
                                        nav.navigate(Screen.PluginDeclarativeUI(pluginId))
                                    }
                                )
                            }

                            entry<Screen.PluginDeclarativeUI> { key ->
                                val pluginManager = koinInject<me.rerere.rikkahub.plugin.manager.PluginManager>()
                                me.rerere.rikkahub.plugin.ui.PluginUIDeclarativePage(
                                    pluginId = key.pluginId,
                                    pluginManager = pluginManager,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }

                            entry<Screen.PluginWebView> { key ->
                                val pluginManager = koinInject<me.rerere.rikkahub.plugin.manager.PluginManager>()
                                PluginWebViewPage(
                                    pluginId = key.pluginId,
                                    htmlEntryPath = key.entryPath,
                                    pluginManager = pluginManager,
                                    onNavigateBack = { backStack.removeLastOrNull() }
                                )
                            }

                            entry<Screen.MemoryBank> {
                                MemoryBankPage(
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }

                            entry<Screen.EmojiPicker> {
                                EmojiPickerPage(
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }

                            entry<Screen.VoiceCall> { key ->
                                VoiceCallPage(
                                    conversationId = Uuid.parse(key.conversationId),
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            }

                            entry<Screen.IncomingCall> { key ->
                                // 来电界面: 接听 -> 启动通话并跳转到通话页; 拒接 -> 返回
                                // 通过 conversationId 查会话 -> assistantId -> 助手名
                                val conversationRepo = koinInject<ConversationRepository>()
                                val chatService = koinInject<ChatService>()
                                var assistantName by remember {
                                    mutableStateOf("")
                                }
                                LaunchedEffect(key.conversationId) {
                                    assistantName = runCatching {
                                        conversationRepo.getConversationById(Uuid.parse(key.conversationId))
                                            ?.assistantId
                                            ?.let { settings.getAssistantById(it)?.name }
                                    }.getOrNull().orEmpty()
                                }
                                IncomingCallPage(
                                    conversationId = key.conversationId,
                                    assistantName = assistantName,
                                    onStartCall = {
                                        val convId = key.conversationId
                                        // 先停掉聊天侧的 TTS, 避免音频焦点抢占导致 ASR 识别质量下降
                                        // (AI 刚说完话触发主动来电时, 聊天页的 TTS 可能还在播放)
                                        runCatching { tts.stop() }
                                        if (VoiceCallService.activeConversationId.value == null) {
                                            VoiceCallService.start(
                                                this@RouteActivity,
                                                convId
                                            )
                                        }
                                        // 替换来电页为通话页
                                        backStack.removeLastOrNull()
                                        backStack.add(Screen.VoiceCall(convId))
                                    },
                                    onDecline = {
                                        backStack.removeLastOrNull()
                                        try {
                                            chatService.notifyVoiceCallDeclined(Uuid.parse(key.conversationId))
                                        } catch (e: Exception) {
                                            Log.e(TAG, "onDecline 触发 notifyVoiceCallDeclined 失败, conversationId=${key.conversationId}", e)
                                        }
                                    }
                                )
                            }

                            entry<Screen.MiniAppManager> {
                                MiniAppManagerPage()
                            }

                            entry<Screen.MiniAppEdit> { key ->
                                MiniAppEditPage(id = key.id)
                            }

                            entry<Screen.MiniApp> { key ->
                                MiniAppPage(
                                    url = key.url,
                                    title = key.title,
                                )
                            }

                            entry<Screen.Legal> { key ->
                                LegalPage(
                                    titleRes = key.titleRes,
                                    contentRes = key.contentRes,
                                )
                            }

                            }
                        )
                        AnimatedVisibility(
                            visible = migrationState is MigrationState.Migrating,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val state = migrationState as? MigrationState.Migrating
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = stringResource(R.string.db_migrating),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (state != null) {
                                        Text(
                                            text = "v${state.from} → v${state.to}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface Screen : NavKey {
    @Serializable
    data class Chat(
        val id: String,
        val text: String? = null,
        val files: List<String> = emptyList(),
        val nodeId: String? = null,
        val autoStartVoice: Boolean = false,
    ) : Screen

    @Serializable
    data class ShareHandler(val text: String, val streamUri: String? = null) : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data object Favorite : Screen

    @Serializable
    data object Assistant : Screen

    @Serializable
    data class AssistantDetail(val id: String) : Screen

    @Serializable
    data class AssistantBasic(val id: String) : Screen

    @Serializable
    data class AssistantPrompt(val id: String) : Screen

    @Serializable
    data class AssistantMemory(val id: String) : Screen

    @Serializable
    data class AssistantRequest(val id: String) : Screen

    @Serializable
    data class AssistantMcp(val id: String) : Screen

    @Serializable
    data class AssistantLocalTool(val id: String) : Screen

    @Serializable
    data class AssistantInjections(val id: String) : Screen

    @Serializable
    data object Translator : Screen

    @Serializable
    data object Setting : Screen

    @Serializable
    data object Backup : Screen

    @Serializable
    data object ImageGen : Screen

    @Serializable
    data class WebView(val url: String = "", val content: String = "") : Screen

    @Serializable
    data object SettingDisplay : Screen

    @Serializable
    data object SettingTheme : Screen

    @Serializable
    data object SettingDisplayTheme : Screen

    @Serializable
    data object SettingDisplayColor : Screen

    @Serializable
    data object SettingDisplayTransparency : Screen

    @Serializable
    data object SettingDisplayIllustration : Screen

    @Serializable
    data object SettingDisplayMessage : Screen

    @Serializable
    data object SettingDisplayCodeInteraction : Screen

    @Serializable
    data object SettingDisplayGeneral : Screen

    @Serializable
    data object SettingDisplayNotification : Screen

    @Serializable
    data object SettingProvider : Screen

    @Serializable
    data class SettingProviderDetail(val providerId: String) : Screen

    @Serializable
    data object SettingModels : Screen

    @Serializable
    data object SettingAbout : Screen

    @Serializable
    data object SettingSearch : Screen

    @Serializable
    data class SettingSearchDetail(val serviceId: String) : Screen

    @Serializable
    data object SettingSpeech : Screen

    @Serializable
    data object SettingMcp : Screen

    @Serializable
    data object SettingDonate : Screen

    @Serializable
    data object SettingFiles : Screen

    @Serializable
    data object SettingWeb : Screen

    @Serializable
    data object Developer : Screen

    @Serializable
    data object Debug : Screen

    @Serializable
    data object Log : Screen

    @Serializable
    data object SecurityAudit : Screen

    @Serializable
    data object Extensions : Screen

    @Serializable
    data object QuickMessages : Screen

    @Serializable
    data object Prompts : Screen

    @Serializable
    data object Skills : Screen

    @Serializable
    data class SkillDetail(val skillName: String) : Screen

    @Serializable
    data object Workspaces : Screen

    @Serializable
    data class WorkspaceDetail(val id: String) : Screen

    @Serializable
    data class WorkspaceTerminal(val id: String) : Screen

    @Serializable
    data object MessageSearch : Screen

    @Serializable
    data object Stats : Screen

    @Serializable
    data object SettingSystemTools : Screen

    @Serializable
    data object SettingSecurity : Screen

    @Serializable
    data object SettingProactiveMessage : Screen

    @Serializable
    data object SettingWeixinBot : Screen

    @Serializable
    data object SettingQqBot : Screen

    @Serializable
    data object Health : Screen

    @Serializable
    data object SettingPlugins : Screen

    @Serializable
    data object Workflows : Screen

    @Serializable
    data class WorkflowDetail(val id: String) : Screen

    @Serializable
    data class PluginDetail(val pluginId: String) : Screen

    @Serializable
    data class PluginFolder(val folderId: String) : Screen

    @Serializable
    data class PluginWebView(val pluginId: String, val entryPath: String) : Screen

    @Serializable
    data class PluginDeclarativeUI(val pluginId: String) : Screen

    @Serializable
    data object MemoryBank : Screen

    @Serializable
    data object EmojiPicker : Screen

    @Serializable
    data object ExternalMemories : Screen

    @Serializable
    data class VoiceCall(val conversationId: String) : Screen

    /**
     * AI 主动发起语音通话时弹出的来电界面.
     * conversationId 用于接听后跳转到对应会话的通话页.
     */
    @Serializable
    data class IncomingCall(val conversationId: String) : Screen

    @Serializable
    data object MiniAppManager : Screen

    @Serializable
    data class MiniAppEdit(val id: String?) : Screen

    @Serializable
    data class MiniApp(val url: String, val title: String?) : Screen

    @Serializable
    data class Legal(val titleRes: Int, val contentRes: Int) : Screen
}
