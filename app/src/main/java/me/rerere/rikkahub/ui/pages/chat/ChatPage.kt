/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.chat

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.dokar.sonner.ToastType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessagePart
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.LeftToRightListBullet
import me.rerere.hugeicons.stroke.Menu03
import me.rerere.hugeicons.stroke.MessageAdd01
import me.rerere.hugeicons.stroke.Voice
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.service.ChatError
import me.rerere.rikkahub.service.VoiceCallService
import me.rerere.rikkahub.ui.components.ai.ChatInput
import me.rerere.rikkahub.ui.components.message.AvatarFrameOverlay
import me.rerere.rikkahub.ui.components.message.LiveBubbleBlurContext
import me.rerere.rikkahub.ui.components.message.LocalLiveBubbleBlur
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.rikkahub.ui.hooks.ChatInputState
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.utils.base64Decode
import me.rerere.rikkahub.utils.navigateToChatPage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

/**
 * 按 Coil model（assistant.background 的 String）的 scheme 分流加载聊天背景位图。
 *
 * 背景 model 的实际形式（与 Coil 显示完全一致）：
 * - 图库选择：FilesManager.createChatFilesByContents 拷贝到 filesDir 后返回 file:// URI；
 * - 手动输入：http/https URL；
 * - 其余（content://、android.resource://、普通绝对路径）一并兼容。
 *
 * 所有磁盘/流解码都在 Dispatchers.IO 中执行；网络等复杂 model 走项目现有
 * Coil 3.4 SingletonImageLoader（不在本函数内做网络阻塞）。
 */
private suspend fun decodeChatBackgroundBitmap(
    context: Context,
    model: String,
): ImageBitmap? = withContext(Dispatchers.IO) {
    val uri = runCatching { Uri.parse(model) }.getOrNull()
    val bitmap = when {
        model.startsWith("/") -> {
            BitmapFactory.decodeFile(model)
        }

        uri?.scheme == ContentResolver.SCHEME_CONTENT ||
            uri?.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE -> {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }

        uri?.scheme == ContentResolver.SCHEME_FILE -> {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: uri.path?.let(BitmapFactory::decodeFile)
        }

        uri?.scheme == "http" || uri?.scheme == "https" -> {
            loadChatBackgroundViaCoil(context, model)
        }

        else -> {
            BitmapFactory.decodeFile(model)
                ?: uri?.let {
                    runCatching {
                        context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream)
                    }.getOrNull()
                }
        }
    }
    bitmap?.asImageBitmap()
}

/**
 * 使用项目现有 Coil 3 ImageLoader 加载同一 model（与 Coil 显示背景使用同一套 fetcher/缓存）。
 * 调用方已处于 Dispatchers.IO，Coil 内部网络/解码不会阻塞主线程。
 */
private suspend fun loadChatBackgroundViaCoil(context: Context, model: String): Bitmap? {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context)
        .data(model)
        // 硬件位图无法被 asImageBitmap/RenderEffect 采样，关闭硬件加速以保证可读
        .allowHardware(false)
        .build()
    val result = loader.execute(request)
    return (result.image as? BitmapImage)?.bitmap
}

@Composable
fun ChatPage(id: Uuid, text: String?, files: List<Uri>, nodeId: Uuid? = null, autoStartVoice: Boolean = false) {
    val vm: ChatVM = koinViewModel(
        parameters = {
            parametersOf(id.toString())
        }
    )
    val filesManager: FilesManager = koinInject()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val setting by vm.settings.collectAsStateWithLifecycle()
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val loadingJob by vm.conversationJob.collectAsStateWithLifecycle()
    val processingStatus by vm.processingStatus.collectAsStateWithLifecycle()
    val currentChatModel by vm.currentChatModel.collectAsStateWithLifecycle()
    val enableWebSearch by vm.enableWebSearch.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hazeState = rememberHazeState()
    val chatPageGraphicsLayer = rememberGraphicsLayer()
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    // Handle back press when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    // Hide keyboard when drawer is open
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            softwareKeyboardController?.hide()
        }
    }

    val windowAdaptiveInfo = currentWindowDpSize()
    val isBigScreen =
        windowAdaptiveInfo.width > windowAdaptiveInfo.height && windowAdaptiveInfo.width >= 1100.dp

    val inputState = vm.inputState

    // 初始化输入状态（处理传入的 files 和 text 参数）
    LaunchedEffect(files, text) {
        if (files.isNotEmpty()) {
            val localFiles = filesManager.createChatFilesByContents(files)
            val contentTypes = files.mapNotNull { file ->
                filesManager.getFileMimeType(file)
            }
            val parts = buildList {
                localFiles.forEachIndexed { index, file ->
                    val type = contentTypes.getOrNull(index)
                    if (type?.startsWith("image/") == true) {
                        add(UIMessagePart.Image(url = file.toString()))
                    } else if (type?.startsWith("video/") == true) {
                        add(UIMessagePart.Video(url = file.toString()))
                    } else if (type?.startsWith("audio/") == true) {
                        add(UIMessagePart.Audio(url = file.toString()))
                    }
                }
            }
            inputState.messageContent = parts
        }
        text?.base64Decode()?.let { decodedText ->
            if (decodedText.isNotEmpty()) {
                inputState.setMessageText(decodedText)
            }
        }
    }

    val chatListState = rememberLazyListState()
    LaunchedEffect(nodeId, conversation.messageNodes.size) {
        if (!vm.chatListInitialized && conversation.messageNodes.isNotEmpty()) {
            if (nodeId != null) {
                val index = conversation.messageNodes.indexOfFirst { it.id == nodeId }
                if (index >= 0) {
                    chatListState.scrollToItem(index)
                }
            } else {
                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
            }
            vm.chatListInitialized = true
        }
    }

    when {
        isBigScreen -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    ChatDrawerContent(
                        navController = navController,
                        current = conversation,
                        vm = vm,
                        settings = setting,
                    )
                }
            ) {
                ChatPageContent(
                    sourceLayer = chatPageGraphicsLayer,
                    inputState = inputState,
                    loadingJob = loadingJob,
                    processingStatus = processingStatus,
                    setting = setting,
                    conversation = conversation,
                    drawerState = drawerState,
                    navController = navController,
                    vm = vm,
                    chatListState = chatListState,
                    enableWebSearch = enableWebSearch,
                    currentChatModel = currentChatModel,
                    bigScreen = true,
                    autoStartVoice = autoStartVoice,
                    hazeState = hazeState,
                    errors = errors,
                    onDismissError = { vm.dismissError(it) },
                    onClearAllErrors = { vm.clearAllErrors() },
                )
            }
        }

        else -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ChatDrawerContent(
                        navController = navController,
                        current = conversation,
                        vm = vm,
                        settings = setting,
                    )
                }
            ) {
                ChatPageContent(
                    sourceLayer = chatPageGraphicsLayer,
                    inputState = inputState,
                    loadingJob = loadingJob,
                    processingStatus = processingStatus,
                    setting = setting,
                    conversation = conversation,
                    drawerState = drawerState,
                    navController = navController,
                    vm = vm,
                    chatListState = chatListState,
                    enableWebSearch = enableWebSearch,
                    currentChatModel = currentChatModel,
                    bigScreen = false,
                    autoStartVoice = autoStartVoice,
                    hazeState = hazeState,
                    errors = errors,
                    onDismissError = { vm.dismissError(it) },
                    onClearAllErrors = { vm.clearAllErrors() },
                )
            }
            BackHandler(drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
        }
    }
}

@Composable
private fun ChatPageContent(
    modifier: Modifier = Modifier,
    sourceLayer: GraphicsLayer,
    inputState: ChatInputState,
    loadingJob: Job?,
    processingStatus: String? = null,
    setting: Settings,
    bigScreen: Boolean,
    conversation: Conversation,
    drawerState: DrawerState,
    navController: Navigator,
    vm: ChatVM,
    chatListState: LazyListState,
    enableWebSearch: Boolean,
    currentChatModel: Model?,
    autoStartVoice: Boolean = false,
    hazeState: HazeState,
    errors: List<ChatError>,
    onDismissError: (Uuid) -> Unit,
    onClearAllErrors: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var previewMode by rememberSaveable { mutableStateOf(false) }

    TTSAutoPlay(vm = vm, setting = setting, conversation = conversation)

    // 原生实时模糊：GLASS + 界面实时渲染 + API 31+
    val actualMaterialMode = LocalMaterialMode.current
    val useLiveDrawerBlur =
        setting.displaySetting.interfaceRealtimeRendering &&
            actualMaterialMode == DisplayMaterialMode.GLASS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val density = LocalDensity.current

    // ===== 普通聊天气泡实时背景模糊 =====
    // 由设置页"聊天气泡实时模糊"开关控制；需要界面实时渲染 + API 31+
    // 材质模式：GLASS = 通透玻璃气泡，FLAT = iOS 磨砂气泡，两者都依赖实时背景模糊
    val bubbleBlurCapableMode =
        actualMaterialMode == DisplayMaterialMode.GLASS ||
            actualMaterialMode == DisplayMaterialMode.FLAT
    val enableLiveBubbleBlur =
        setting.displaySetting.interfaceRealtimeRendering &&
            setting.displaySetting.chatBubbleRealtimeBlur &&
            bubbleBlurCapableMode &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // 共享聊天背景 Painter（仅图片背景时非空；与 AssistantBackground 共用同一实例，不重复加载）
    val chatBackgroundPainter = rememberChatBackgroundPainter(setting)
    // 共享背景视觉参数（基础底色、背景纸 alpha、渐变遮罩），与 AssistantBackground 完全一致
    val chatBackgroundVisuals = rememberChatBackgroundVisuals(setting)
    // 共享聊天背景 ImageBitmap：仅当图片背景存在时在 LaunchedEffect 中异步解码一次，
    // 供每个气泡背景片段层复用（不创建全屏 Bitmap、不逐气泡复制）。
    // key 仅跟随背景 model（assistant.background）：修改模糊强度 Slider 等其他设置不会重新解码；
    // 背景变为 null 时清空共享位图，背景更换后重新加载。
    val bubbleBlurBackgroundModel = setting.getCurrentAssistant().background
    val bubbleBlurContext = LocalContext.current.applicationContext
    var bubbleBlurBackgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(bubbleBlurBackgroundModel) {
        if (bubbleBlurBackgroundModel != null) {
            bubbleBlurBackgroundBitmap = runCatching {
                decodeChatBackgroundBitmap(bubbleBlurContext, bubbleBlurBackgroundModel)
            }.getOrNull()
        } else {
            bubbleBlurBackgroundBitmap = null
        }
    }
    // 背景容器窗口原点与像素尺寸（现有内容 Box 同时是 AssistantBackground 的容器）
    var bubbleBlurBackgroundOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var bubbleBlurBackgroundSize by remember { mutableStateOf(Size.Unspecified) }
    // 侧边栏抽屉模糊强度：继续由设置项控制，范围 3..20 dp（与设置页 Slider 一致）
    val blurRadiusPx = with(density) {
        setting.displaySetting.interfaceBlurRadius
            .coerceIn(3f, 20f)
            .dp
            .toPx()
    }
    // 聊天气泡实时模糊半径：固定 5 dp（不随侧边栏 Slider 改变）
    val bubbleBlurRadiusPx = with(density) { 5.dp.toPx() }
    val drawerBlurEffect = remember(blurRadiusPx, useLiveDrawerBlur) {
        if (useLiveDrawerBlur) {
            AndroidRenderEffect.createBlurEffect(
                blurRadiusPx,
                blurRadiusPx,
                Shader.TileMode.CLAMP,
            ).asComposeRenderEffect()
        } else {
            null
        }
    }
    // 抽屉面板位置：material3 Modal 中 offset 值域 [-drawerWidth, 0]，open=0（贴左缘），closed=-width（左屏外）
    val rawDrawerOffset = drawerState.currentOffset
    val drawerWidthPx = with(density) { 300.dp.toPx() }
    // 安全处理：NaN/未初始化按完全关闭（offset = -width）；越界值收敛到 [-width, 0]
    val drawerOffsetPx = if (rawDrawerOffset.isNaN()) {
        -drawerWidthPx
    } else {
        rawDrawerOffset.coerceIn(-drawerWidthPx, 0f)
    }
    // 抽屉当前可见宽度（拖动/动画中实时变化，closed=0，open=width）
    val drawerVisibleWidthPx = (drawerWidthPx + drawerOffsetPx).coerceIn(0f, drawerWidthPx)
    // 抽屉打开进度：0=完全关闭，1=完全打开（drawerOffsetPx 已收敛到 [-width, 0]，故天然在 [0,1]）
    val drawerOpenFraction = if (drawerWidthPx > 0f) drawerVisibleWidthPx / drawerWidthPx else 0f
    // overlay 所在 Box 与源 layer 同宿主（同一窗口坐标），把抽屉窗口左缘换成局部 x
    var overlayOriginInWindow by remember { mutableStateOf(Offset.Zero) }

    // 独立中间模糊层：sourceLayer（清晰捕获）→ blurLayer（录制 + 原生 RenderEffect）→ overlay（绘制）。
    // 与 sourceLayer 是两个不同对象；blurLayer 只在 overlay DrawScope 中按抽屉可见宽度录制，不被 sourceLayer 捕获。
    val drawerBlurLayer = rememberGraphicsLayer()

    // 被录制子树：全部正常聊天原始内容（AssistantBackground + Scaffold + ChatList + ChatInput）。
    // 正常聊天页面由 drawContent() 直接上屏，不再经过 sourceLayer；
    // sourceLayer 仅作为额外录制副本，唯一消费位置是 drawerBlurLayer.record 内。
    // 仅抽屉可见时录制，避免空 layer；capture modifier 内绝不 drawLayer(sourceLayer)。
    val contentCaptureModifier = Modifier
        .fillMaxSize()
        .drawWithContent {
            // 正常聊天页面直接上屏
            drawContent()

            // 本轮停用（注释）：sourceLayer.record 重放路径经外部专家会诊确认本机无输出，
            // 注释掉录制以省掉双绘制成本；drawContent() 直绘上屏保留。
            // record 代码本体保留，清理留到实机验证成功之后单独进行。
            // 仅抽屉可见时额外录制一份，专供模糊 overlay
            // if (drawerVisibleWidthPx > 0f) {
            //     sourceLayer.record(
            //         density = this,
            //         layoutDirection = layoutDirection,
            //         size = IntSize(
            //             size.width.toInt().coerceAtLeast(1),
            //             size.height.toInt().coerceAtLeast(1),
            //         ),
            //     ) {
            //         this@drawWithContent.drawContent()
            //     }
            // }
        }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(contentCaptureModifier)
                .onGloballyPositioned { coordinates ->
                    overlayOriginInWindow = coordinates.positionInWindow()
                    // 气泡真实背景模糊：记录背景容器（内容 Box）窗口原点与像素尺寸
                    val pos = coordinates.positionInWindow()
                    if (pos.isFinite && coordinates.size.width > 0 && coordinates.size.height > 0) {
                        bubbleBlurBackgroundOrigin = pos
                        bubbleBlurBackgroundSize = coordinates.size.toSize()
                    }
                }
                // 新方案：抽屉可见时仅抽屉区域原生模糊（同一子树直挂 renderEffect 合成链，不涉及 layer 录制/重放）。
                // 满足条件才追加 graphicsLayer（lambda 版本，block 内读取进度值，半径随进度渐进）；
                // 不满足时不追加任何 modifier（抽屉关闭时零成本）。
                .then(
                    if (useLiveDrawerBlur && !bigScreen && drawerOpenFraction > 0.01f) {
                        Modifier.graphicsLayer {
                            // 保护：可见宽度 < 1f 或半径 <= 0f 时不构造链、不设置 renderEffect
                            // （外层 0.01f 门已保证 drawerOpenFraction > 0，这里再兜底防御）
                            if (drawerVisibleWidthPx >= 1f && blurRadiusPx > 0f) {
                                val r = blurRadiusPx * drawerOpenFraction
                                val blur = AndroidRenderEffect.createBlurEffect(
                                    r, r, Shader.TileMode.CLAMP
                                )
                                // 左侧矩形 mask：x < drawerVisibleWidthPx 不透明白，右侧透明，1px 硬边过渡。
                                // 坐标基于内容 Box 局部坐标系（Box 左缘即 x=0）；drawerVisibleWidthPx 由
                                // drawerOffsetPx 安全收敛派生，随抽屉拖动逐帧更新（graphicsLayer lambda 每次读最新值）。
                                val maskShader = LinearGradient(
                                    drawerVisibleWidthPx - 1f, 0f,
                                    drawerVisibleWidthPx, 0f,
                                    intArrayOf(Color.White.toArgb(), Color.Transparent.toArgb()),
                                    floatArrayOf(0f, 1f),
                                    Shader.TileMode.CLAMP,
                                )
                                val mask = AndroidRenderEffect.createShaderEffect(maskShader)
                                // SRC_IN：只保留抽屉可见区的模糊像素
                                val maskedBlur = AndroidRenderEffect.createBlendModeEffect(
                                    mask, blur, BlendMode.SRC_IN
                                )
                                // SRC_OVER 叠回原始内容：该 API 的 dst 参数不接受 null，
                                // 用恒等 ColorMatrixColorFilter（默认 ColorMatrix 即单位矩阵）构造
                                // "原样复制"的 RenderEffect 作为 dst，等价于节点原始内容（不涉及录制/重放）。
                                val identityColorFilter = ColorMatrixColorFilter(ColorMatrix())
                                val originalContent = AndroidRenderEffect.createColorFilterEffect(identityColorFilter)
                                val final = AndroidRenderEffect.createBlendModeEffect(
                                    originalContent, maskedBlur, BlendMode.SRC_OVER
                                )
                                renderEffect = final.asComposeRenderEffect()
                            }
                        }
                    } else {
                        Modifier
                    }
                ),
        ) {
        CompositionLocalProvider(
            LocalLiveBubbleBlur provides LiveBubbleBlurContext(
                imageBitmap = if (enableLiveBubbleBlur) bubbleBlurBackgroundBitmap else null,
                backgroundOriginInWindow = bubbleBlurBackgroundOrigin,
                backgroundSizePx = bubbleBlurBackgroundSize,
                baseColor = chatBackgroundVisuals.baseColor,
                imageAlpha = chatBackgroundVisuals.imageAlpha,
                gradientTopAlpha = chatBackgroundVisuals.gradientTopAlpha,
                gradientBottomAlpha = chatBackgroundVisuals.gradientBottomAlpha,
                enabled = enableLiveBubbleBlur && !bigScreen,
                radiusPx = bubbleBlurRadiusPx,
            )
        ) {
        AssistantBackground(setting = setting, backgroundPainter = chatBackgroundPainter)
        Scaffold(
            topBar = {
                TopBar(
                    settings = setting,
                    conversation = conversation,
                    bigScreen = bigScreen,
                    drawerState = drawerState,
                    previewMode = previewMode,
                    onNewChat = {
                        navigateToChatPage(navController)
                    },
                    onClickMenu = {
                        previewMode = !previewMode
                    },
                    onUpdateTitle = {
                        vm.updateTitle(it)
                    },
                    onVoiceCall = {
                        val activeId = VoiceCallService.activeConversationId.value
                        when {
                            activeId == null -> navController.navigate(
                                Screen.VoiceCall(conversation.id.toString())
                            )
                            activeId == conversation.id.toString() -> navController.navigate(
                                Screen.VoiceCall(conversation.id.toString())
                            )
                            else -> {
                                toaster.show("当前有通话进行中，请先挂断", type = ToastType.Warning)
                            }
                        }
                    },
                )
            },
            bottomBar = {
                ChatInput(
                    state = inputState,
                    loading = loadingJob != null,
                    settings = setting,
                    conversation = conversation,
                    mcpManager = vm.mcpManager,
                    hazeState = hazeState,
                    autoStartVoice = autoStartVoice,
                    onCancelClick = {
                        vm.stopGeneration()
                    },
                    enableSearch = enableWebSearch,
                    onToggleSearch = {
                        vm.updateSettings(setting.copy(enableWebSearch = !enableWebSearch))
                    },
                    onSendClick = {
                        if (currentChatModel == null) {
                            toaster.show("请先选择模型", type = ToastType.Error)
                            return@ChatInput
                        }
                        if (inputState.isEditing()) {
                            vm.handleMessageEdit(
                                parts = inputState.getContents(),
                                messageId = inputState.editingMessage!!,
                            )
                        } else {
                            vm.handleMessageSend(inputState.getContents())
                            scope.launch {
                                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                            }
                            val sendSound = setting.displaySetting.sendSoundPath
                            if (sendSound.isNotBlank() && java.io.File(sendSound).exists()) {
                                runCatching {
                                    android.media.MediaPlayer().apply {
                                        setDataSource(sendSound)
                                        setOnPreparedListener { it.start() }
                                        setOnCompletionListener { it.release() }
                                        setOnErrorListener { mp, _, _ -> mp.release(); true }
                                        prepareAsync()
                                    }
                                }
                            }
                        }
                        inputState.clearInput()
                    },
                    onVoiceMessage = { url, duration, transcript ->
                        if (currentChatModel == null) {
                            toaster.show("请先选择模型", type = ToastType.Error)
                            return@ChatInput
                        }
                        vm.handleMessageSend(
                            listOf(
                                UIMessagePart.VoiceMessage(
                                    url = url,
                                    duration = duration,
                                    transcript = transcript,
                                )
                            )
                        )
                        scope.launch {
                            chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                        }
                    },
                    onLongSendClick = {
                        if (inputState.isEditing()) {
                            vm.handleMessageEdit(
                                parts = inputState.getContents(),
                                messageId = inputState.editingMessage!!,
                            )
                        } else {
                            vm.handleMessageSend(content = inputState.getContents(), answer = false)
                            scope.launch {
                                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                            }
                        }
                        inputState.clearInput()
                    },
                    onUpdateChatModel = {
                        vm.setChatModel(assistant = setting.getCurrentAssistant(), model = it)
                    },
                    onUpdateAssistant = {
                        vm.updateSettings(
                            setting.copy(
                                assistants = setting.assistants.map { assistant ->
                                    if (assistant.id == it.id) {
                                        it
                                    } else {
                                        assistant
                                    }
                                }
                            )
                        )
                    },
                    onUpdateSearchService = { index ->
                        vm.updateSettings(
                            setting.copy(
                                searchServiceSelected = index
                            )
                        )
                    },
                    onCompressContext = { additionalPrompt, targetTokens, keepRecentMessages ->
                        vm.handleCompressContext(additionalPrompt, targetTokens, keepRecentMessages)
                    },
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            ChatList(
                innerPadding = innerPadding,
                conversation = conversation,
                state = chatListState,
                loading = loadingJob != null,
                processingStatus = processingStatus,
                previewMode = previewMode,
                settings = setting,
                hazeState = hazeState,
                errors = errors,
                onDismissError = onDismissError,
                onClearAllErrors = onClearAllErrors,
                onRegenerate = {
                    vm.regenerateAtMessage(it)
                },
                onEdit = {
                    inputState.editingMessage = it.id
                    inputState.setContents(it.parts)
                },
                onForkMessage = {
                    scope.launch {
                        val fork = vm.forkMessage(message = it)
                        navigateToChatPage(navController, chatId = fork.id)
                    }
                },
                onDelete = {
                    if (loadingJob != null) {
                        vm.showDeleteBlockedWhileGeneratingError()
                    } else {
                        vm.deleteMessage(it)
                    }
                },
                onUpdateMessage = { newNode ->
                    vm.updateConversation(
                        conversation.copy(
                            messageNodes = conversation.messageNodes.map { node ->
                                if (node.id == newNode.id) {
                                    newNode
                                } else {
                                    node
                                }
                            }
                        ))
                    vm.saveConversationAsync()
                },
                onClickSuggestion = { suggestion ->
                    inputState.editingMessage = null
                    inputState.setMessageText(suggestion)
                },
                onTranslate = { message, locale ->
                    vm.translateMessage(message, locale)
                },
                onClearTranslation = { message ->
                    vm.clearTranslationField(message.id)
                },
                onJumpToMessage = { index ->
                    previewMode = false
                    scope.launch {
                        chatListState.animateScrollToItem(index)
                    }
                },
                onToolApproval = { toolCallId, approved, reason ->
                    vm.handleToolApproval(toolCallId, approved, reason)
                },
                onToolAnswer = { toolCallId, answer ->
                    vm.handleToolAnswer(toolCallId, answer)
                },
                onToggleFavorite = { node ->
                    vm.toggleMessageFavorite(node)
                },
                onConversationSystemPromptChange = { newPrompt ->
                    vm.updateConversation(conversation.copy(customSystemPrompt = newPrompt))
                    vm.saveConversationAsync()
                },
            )
        }
        }

        }

        // 原生实时模糊覆盖层：录制子树（contentCaptureModifier Box）的后绘制 sibling，绝不被 sourceLayer 录制，
        // 位于清晰聊天页上方、Drawer 内容下方（Drawer 由 ModalNavigationDrawer 绘制在更上层，因此不被模糊）。
        // 仅适用于小屏 Modal（大屏 Permanent 不使用 drawerState offset，避免误绘）
        // 本轮停用：外部专家会诊确认重放侧无输出，overlay 渲染路径弃用，绘制条件改为恒 false；
        // 代码本体保留（含 tint、clipRect、drawLayer），清理留到实机验证之后单独进行。
        // 原条件：useLiveDrawerBlur && !bigScreen && drawerBlurEffect != null && drawerVisibleWidthPx > 0f
        if (false && useLiveDrawerBlur && !bigScreen && drawerBlurEffect != null && drawerVisibleWidthPx > 0f) {
            // 抽屉实际可见窗口范围恒为 [0, drawerVisibleWidthPx]（窗口坐标，左缘贴屏幕左缘）；
            // 不再用 drawerOffsetPx 作为左边界；转换为 overlay 局部坐标时减去 overlay 原点。
            val overlayLeftPx = -overlayOriginInWindow.x
            val overlayRightPx = drawerVisibleWidthPx - overlayOriginInWindow.x
            // 较轻的主题 tint：叠加在模糊副本上，不遮死高斯模糊
            val drawerTintColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.12f)
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                // blurLayer 录制尺寸只覆盖抽屉当前可见宽度和 overlay 高度（取整后必须 > 0，避免空 layer）
                val blurWidthPx = drawerVisibleWidthPx.roundToInt().coerceAtLeast(1)
                val blurHeightPx = size.height.toInt().coerceAtLeast(1)
                drawerBlurLayer.record(
                    density = this,
                    layoutDirection = layoutDirection,
                    size = IntSize(blurWidthPx, blurHeightPx),
                ) {
                    // blurLayer 原点 = 抽屉可见区域的窗口左上角；sourceLayer 原点 = 内容 Box 左上角（窗口坐标 overlayOriginInWindow）。
                    // 按窗口原点差平移后 drawLayer(sourceLayer)，使录制内容正好是抽屉背后的聊天区域。
                    translate(left = -overlayOriginInWindow.x, top = 0f) {
                        drawLayer(sourceLayer)
                    }
                }
                // 原生模糊只施加在独立 blurLayer 上；overlay Canvas 与 sourceLayer 均不挂 RenderEffect。
                drawerBlurLayer.renderEffect = drawerBlurEffect
                clipRect(
                    left = overlayLeftPx,
                    top = 0f,
                    right = overlayRightPx,
                    bottom = size.height.toFloat(),
                ) {
                    drawLayer(drawerBlurLayer)
                    drawRect(color = drawerTintColor)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    settings: Settings,
    conversation: Conversation,
    drawerState: DrawerState,
    bigScreen: Boolean,
    previewMode: Boolean,
    onClickMenu: () -> Unit,
    onNewChat: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onVoiceCall: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val titleState = useEditState<String> {
        onUpdateTitle(it)
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            if (!bigScreen) {
                IconButton(
                    onClick = {
                        scope.launch { drawerState.open() }
                    }
                ) {
                    Icon(HugeIcons.Menu03, "Messages")
                }
            }
        },
        title = {
            val editTitleWarning = stringResource(R.string.chat_page_edit_title_warning)
            Surface(
                onClick = {
                    if (conversation.messageNodes.isNotEmpty()) {
                        titleState.open(conversation.title)
                    } else {
                        toaster.show(editTitleWarning, type = ToastType.Warning)
                    }
                },
                color = Color.Transparent,
            ) {
                val assistant = settings.getCurrentAssistant()
                val model = settings.getCurrentChatModel()
                val provider = model?.findProvider(providers = settings.providers, checkOverwrite = false)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 顶栏头像：整屏只此一处，与逐条消息头像互斥。
                    // 开启后同时显示助手与用户头像（助手在前、略微交叠），参考 kimi-room 的页首双头像。
                    if (settings.displaySetting.showTopBarAvatar) {
                        Box {
                            Box(
                                modifier = Modifier.padding(end = TOP_BAR_AVATAR_OVERLAP),
                                contentAlignment = Alignment.Center,
                            ) {
                                UIAvatar(
                                    name = assistant.name,
                                    value = assistant.avatar,
                                    modifier = Modifier.size(TOP_BAR_AVATAR_SIZE),
                                )
                                AvatarFrameOverlay(
                                    framePath = settings.displaySetting.aiAvatarFramePath,
                                    offsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                                    offsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                                    scale = settings.displaySetting.aiAvatarFrameScale,
                                    baseSize = TOP_BAR_AVATAR_SIZE.value,
                                )
                            }
                            UIAvatar(
                                name = settings.displaySetting.userNickname.ifBlank { "Me" },
                                value = settings.displaySetting.userAvatar,
                                modifier = Modifier
                                    .padding(start = TOP_BAR_AVATAR_SIZE - TOP_BAR_AVATAR_OVERLAP)
                                    .size(TOP_BAR_AVATAR_SIZE),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = conversation.title.ifBlank { stringResource(R.string.chat_page_new_chat) },
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (model != null && provider != null) {
                            Text(
                                text = "${assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) }} / ${model.displayName} (${provider.name})",
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = LocalContentColor.current.copy(0.65f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                )
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    onVoiceCall()
                }
            ) {
                Icon(HugeIcons.Voice, "Voice Call")
            }

            IconButton(
                onClick = {
                    onClickMenu()
                }
            ) {
                Icon(if (previewMode) HugeIcons.Cancel01 else HugeIcons.LeftToRightListBullet, "Chat Options")
            }

            IconButton(
                onClick = {
                    onNewChat()
                }
            ) {
                Icon(HugeIcons.MessageAdd01, "New Message")
            }
        },
    )
    titleState.EditStateContent { title, onUpdate ->
        AlertDialog(
            onDismissRequest = {
                titleState.dismiss()
            },
            title = {
                Text(stringResource(R.string.chat_page_edit_title))
            },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        titleState.confirm()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        titleState.dismiss()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }
}
// 顶栏双头像尺寸与交叠量（助手在前、用户在后略微压住）
private val TOP_BAR_AVATAR_SIZE = 30.dp
private val TOP_BAR_AVATAR_OVERLAP = 10.dp
