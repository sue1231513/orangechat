/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.miniapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.HapticFeedbackConstants
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import me.rerere.rikkahub.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import kotlinx.serialization.json.Json
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LocalDarkMode

private const val TAG = "MiniAppPage"

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppPage(
    url: String,
    title: String?,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isDarkMode = LocalDarkMode.current
    val colorScheme = MaterialTheme.colorScheme

    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val webView by webViewRef
    var pageTitle by remember { mutableStateOf(title ?: "") }
    var isLoading by remember { mutableStateOf(true) }
    var mainButtonVisible by remember { mutableStateOf(false) }
    var mainButtonText by remember { mutableStateOf("CONTINUE") }
    var mainButtonEnabled by remember { mutableStateOf(true) }
    var mainButtonProgress by remember { mutableStateOf(false) }
    var backButtonVisible by remember { mutableStateOf(false) }
    var popupData by remember { mutableStateOf<PopupData?>(null) }
    var confirmData by remember { mutableStateOf<ConfirmData?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }

    val bridgeScript = remember(colorScheme, isDarkMode) {
        buildTelegramBridgeScript(colorScheme, isDarkMode)
    }

    val miniAppWebViewClient = remember {
        MiniAppWebViewClient(
            bridgeScript = bridgeScript,
            onPageTitle = { pageTitle = it ?: title ?: "" },
            onLoadingChange = { isLoading = it },
            onMainButton = { visible, text, enabled, progress ->
                mainButtonVisible = visible
                text?.let { mainButtonText = it }
                mainButtonEnabled = enabled
                mainButtonProgress = progress
            },
            onBackButton = { visible ->
                backButtonVisible = visible
            },
            onPopup = { popupData = it },
            onConfirm = { confirmData = it },
            onAlert = { alertMessage = it },
            onOpenLink = { link, tryInstantView ->
                openExternalLink(context, link, tryInstantView)
            },
            onOpenTelegramLink = { link ->
                openTelegramLink(context, link)
            },
            onClose = { navController.popBackStack() },
            onHaptic = { style ->
                webView?.performHapticFeedback(parseHapticStyle(style))
            },
        )
    }

    BackHandler(enabled = true) {
        if (backButtonVisible) {
            webView?.evaluateJavascript("window.__bridgeFireEvent('backButtonClicked');", null)
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                navigationIcon = {
                    if (backButtonVisible) {
                        IconButton(onClick = {
                            webView?.evaluateJavascript(
                                "window.__bridgeFireEvent('backButtonClicked');",
                                null
                            )
                        }) {
                            Icon(HugeIcons.ArrowLeft01, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(HugeIcons.Cancel01, contentDescription = "关闭")
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                @SuppressLint("SetJavaScriptEnabled")
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            webViewClient = miniAppWebViewClient
                            webChromeClient = WebChromeClient()
                            loadUrl(url)
                            webViewRef.value = this
                        }
                    },
                    update = {
                        webViewRef.value = it
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (mainButtonVisible) {
                Button(
                    onClick = {
                        webView?.evaluateJavascript(
                            "window.__bridgeFireEvent('mainButtonClicked');",
                            null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = mainButtonEnabled && !mainButtonProgress,
                ) {
                    if (mainButtonProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(mainButtonText)
                }
            }
        }
    }

    popupData?.let { data ->
        AlertDialog(
            onDismissRequest = { popupData = null },
            title = { data.title?.let { Text(it) } },
            text = { Text(data.message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        popupData = null
                        val buttonId = data.buttons.firstOrNull()?.id ?: ""
                        webView?.evaluateJavascript(
                            "window.__bridgeResult('${data.callbackId}', {buttonId:'$buttonId'});",
                            null
                        )
                    }
                ) {
                    Text(data.buttons.firstOrNull()?.text ?: "确定")
                }
            },
            dismissButton = if (data.buttons.size > 1) {
                {
                    TextButton(
                        onClick = {
                            popupData = null
                            val buttonId = data.buttons.getOrNull(1)?.id ?: ""
                            webView?.evaluateJavascript(
                                "window.__bridgeResult('${data.callbackId}', {buttonId:'$buttonId'});",
                                null
                            )
                        }
                    ) {
                        Text(data.buttons.getOrNull(1)?.text ?: "取消")
                    }
                }
            } else null
        )
    }

    confirmData?.let { data ->
        RikkaConfirmDialog(
            show = true,
            title = "",
            confirmText = "确定",
            dismissText = "取消",
            onConfirm = {
                webView?.evaluateJavascript(
                    "window.__bridgeResult('${data.callbackId}', true);",
                    null
                )
                confirmData = null
            },
            onDismiss = {
                webView?.evaluateJavascript(
                    "window.__bridgeResult('${data.callbackId}', false);",
                    null
                )
                confirmData = null
            }
        ) {
            Text(data.message)
        }
    }

    alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) {
                    Text("确定")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}

private class MiniAppWebViewClient(
    private val bridgeScript: String,
    private val onPageTitle: (String?) -> Unit,
    private val onLoadingChange: (Boolean) -> Unit,
    private val onMainButton: (visible: Boolean, text: String?, enabled: Boolean, progress: Boolean) -> Unit,
    private val onBackButton: (visible: Boolean) -> Unit,
    private val onPopup: (PopupData) -> Unit,
    private val onConfirm: (ConfirmData) -> Unit,
    private val onAlert: (String) -> Unit,
    private val onOpenLink: (String, Boolean) -> Unit,
    private val onOpenTelegramLink: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onHaptic: (String) -> Unit,
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onLoadingChange(true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLoadingChange(false)
        onPageTitle(view?.title)
        view?.evaluateJavascript(bridgeScript, null)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("bridge://")) {
            handleBridgeCall(view!!, url)
            return true
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            // 保持内部 WebView 加载
            return false
        }
        return false
    }

    private fun handleBridgeCall(webView: WebView, url: String) {
        val uri = Uri.parse(url)
        val method = uri.host ?: return
        val params = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) ?: "" }
        val callbackId = params["callbackId"] ?: ""

        when (method) {
            "ready" -> {
                Log.i(TAG, "MiniApp ready")
            }
            "expand" -> {
                // 全屏展开：当前实现由页面自身占满屏幕即可
            }
            "close" -> {
                onClose()
            }
            "openLink" -> {
                val link = params["url"] ?: ""
                val tryInstantView = params["try_instant_view"] == "true"
                if (link.isNotBlank()) onOpenLink(link, tryInstantView)
            }
            "openTelegramLink" -> {
                val link = params["url"] ?: ""
                if (link.isNotBlank()) onOpenTelegramLink(link)
            }
            "sendData" -> {
                // 发送给 bot 的数据，当前仅记录
                Log.i(TAG, "sendData: ${params["data"]}")
            }
            "showPopup" -> {
                val title = params["title"]
                val message = params["message"] ?: ""
                val buttons = try {
                    json.decodeFromString<List<PopupButton>>(params["buttons"] ?: "[]")
                } catch (e: Exception) {
                    emptyList()
                }
                onPopup(PopupData(callbackId, title, message, buttons))
            }
            "showConfirm" -> {
                onConfirm(ConfirmData(callbackId, params["message"] ?: ""))
            }
            "showAlert" -> {
                onAlert(params["message"] ?: "")
            }
            "haptic.impact" -> {
                onHaptic(params["style"] ?: "medium")
            }
            "haptic.notification" -> {
                onHaptic("notification_${params["type"] ?: "success"}")
            }
            "haptic.selection" -> {
                onHaptic("selection")
            }
            "mainButton.setText" -> {
                onMainButton(true, params["text"], true, false)
            }
            "mainButton.show" -> {
                onMainButton(true, null, true, false)
            }
            "mainButton.hide" -> {
                onMainButton(false, null, true, false)
            }
            "mainButton.enable" -> {
                onMainButton(true, null, true, false)
            }
            "mainButton.disable" -> {
                onMainButton(true, null, false, false)
            }
            "mainButton.showProgress" -> {
                onMainButton(true, null, true, true)
            }
            "mainButton.hideProgress" -> {
                onMainButton(true, null, true, false)
            }
            "mainButton.setParams" -> {
                val text = params["text"]
                val enabled = params["is_active"] != "false"
                onMainButton(true, text, enabled, false)
            }
            "backButton.show" -> {
                onBackButton(true)
            }
            "backButton.hide" -> {
                onBackButton(false)
            }
            else -> {
                Log.w(TAG, "Unknown bridge method: $method")
            }
        }
    }
}

private data class PopupData(
    val callbackId: String,
    val title: String?,
    val message: String,
    val buttons: List<PopupButton>,
)

@kotlinx.serialization.Serializable
private data class PopupButton(
    val id: String = "",
    val text: String = "",
    val type: String = "",
)

private data class ConfirmData(
    val callbackId: String,
    val message: String,
)

private fun openExternalLink(context: android.content.Context, url: String, tryInstantView: Boolean) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(context, intent, null)
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No browser found for $url")
    }
}

private fun openTelegramLink(context: android.content.Context, url: String) {
    try {
        val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage("org.telegram.messenger")
        startActivity(context, telegramIntent, null)
    } catch (e: ActivityNotFoundException) {
        openExternalLink(context, url, false)
    }
}

private fun parseHapticStyle(style: String): Int {
    return when (style.lowercase()) {
        "light" -> HapticFeedbackConstants.LONG_PRESS
        "medium" -> HapticFeedbackConstants.LONG_PRESS
        "heavy" -> HapticFeedbackConstants.CONFIRM
        "rigid" -> HapticFeedbackConstants.CONFIRM
        "soft" -> HapticFeedbackConstants.LONG_PRESS
        "selection" -> HapticFeedbackConstants.CLOCK_TICK
        "notification_success" -> HapticFeedbackConstants.CONFIRM
        "notification_warning" -> HapticFeedbackConstants.LONG_PRESS
        "notification_error" -> HapticFeedbackConstants.REJECT
        else -> HapticFeedbackConstants.LONG_PRESS
    }
}

private fun buildTelegramBridgeScript(
    colorScheme: androidx.compose.material3.ColorScheme,
    isDarkMode: Boolean,
): String {
    val colorSchemeName = if (isDarkMode) "dark" else "light"
    val themeParams = buildString {
        append("{")
        append("bg_color: '${colorScheme.background.toRgbHex()}', ")
        append("text_color: '${colorScheme.onBackground.toRgbHex()}', ")
        append("hint_color: '${colorScheme.onSurfaceVariant.toRgbHex()}', ")
        append("link_color: '${colorScheme.primary.toRgbHex()}', ")
        append("button_color: '${colorScheme.primary.toRgbHex()}', ")
        append("button_text_color: '${colorScheme.onPrimary.toRgbHex()}', ")
        append("secondary_bg_color: '${colorScheme.surfaceVariant.toRgbHex()}'")
        append("}")
    }

    return TELEGRAM_BRIDGE_JAVASCRIPT
        .replace("%COLOR_SCHEME%", colorSchemeName)
        .replace("%THEME_PARAMS%", themeParams)
}

private fun androidx.compose.ui.graphics.Color.toRgbHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

private val TELEGRAM_BRIDGE_JAVASCRIPT = """
(function() {
    if (window.Telegram && window.Telegram.WebApp) return;
    window.__tgCallbackId = 0;
    window.__tgCallbacks = {};
    window.__tgEventListeners = {};

    function bridgeCall(method, params) {
        return new Promise(function(resolve, reject) {
            var cbId = 'tg_' + (++window.__tgCallbackId);
            window.__tgCallbacks[cbId] = { resolve: resolve, reject: reject };
            var url = 'bridge://' + method + '?callbackId=' + encodeURIComponent(cbId);
            if (params) {
                for (var key in params) {
                    if (params[key] !== undefined && params[key] !== null) {
                        url += '&' + encodeURIComponent(key) + '=' + encodeURIComponent(String(params[key]));
                    }
                }
            }
            var iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = url;
            document.body.appendChild(iframe);
            setTimeout(function() { document.body.removeChild(iframe); }, 100);
        });
    }

    window.__bridgeResult = function(callbackId, result) {
        var cb = window.__tgCallbacks[callbackId];
        if (cb) {
            cb.resolve(result);
            delete window.__tgCallbacks[callbackId];
        }
    };

    window.__bridgeFireEvent = function(eventType) {
        var listeners = window.__tgEventListeners[eventType] || [];
        listeners.forEach(function(listener) {
            try { listener(); } catch (e) { console.error('[TMA] event error', e); }
        });
    };

    var MainButton = {
        text: 'CONTINUE',
        color: null,
        textColor: '#ffffff',
        isVisible: false,
        isActive: true,
        isProgressVisible: false,
        setText: function(text) { this.text = text; bridgeCall('mainButton.setText', { text: text }); return this; },
        show: function() { this.isVisible = true; bridgeCall('mainButton.show', {}); return this; },
        hide: function() { this.isVisible = false; bridgeCall('mainButton.hide', {}); return this; },
        enable: function() { this.isActive = true; bridgeCall('mainButton.enable', {}); return this; },
        disable: function() { this.isActive = false; bridgeCall('mainButton.disable', {}); return this; },
        showProgress: function(leaveActive) { this.isProgressVisible = true; bridgeCall('mainButton.showProgress', {}); return this; },
        hideProgress: function() { this.isProgressVisible = false; bridgeCall('mainButton.hideProgress', {}); return this; },
        setParams: function(params) { 
            if (params.text) this.text = params.text; 
            if (params.color) this.color = params.color;
            if (params.text_color) this.textColor = params.text_color;
            if (params.is_active !== undefined) this.isActive = params.is_active;
            bridgeCall('mainButton.setParams', params); 
            return this; 
        },
        onClick: function(callback) { 
            window.__tgEventListeners['mainButtonClicked'] = [callback]; 
            return this; 
        }
    };

    var BackButton = {
        isVisible: false,
        show: function() { this.isVisible = true; bridgeCall('backButton.show', {}); return this; },
        hide: function() { this.isVisible = false; bridgeCall('backButton.hide', {}); return this; },
        onClick: function(callback) { 
            window.__tgEventListeners['backButtonClicked'] = [callback]; 
            return this; 
        }
    };

    var HapticFeedback = {
        impactOccurred: function(style) { bridgeCall('haptic.impact', { style: style }); },
        notificationOccurred: function(type) { bridgeCall('haptic.notification', { type: type }); },
        selectionChanged: function() { bridgeCall('haptic.selection', {}); }
    };

    window.Telegram = {
        WebApp: {
            initData: '',
            initDataUnsafe: {},
            version: '8.0',
            platform: 'android',
            colorScheme: '%COLOR_SCHEME%',
            themeParams: %THEME_PARAMS%,
            isExpanded: true,
            viewportHeight: window.innerHeight,
            viewportStableHeight: window.innerHeight,
            ready: function() { bridgeCall('ready', {}); },
            expand: function() { bridgeCall('expand', {}); },
            close: function() { bridgeCall('close', {}); },
            openLink: function(url, options) { 
                bridgeCall('openLink', { url: url, try_instant_view: options && options.try_instant_view ? 'true' : 'false' }); 
            },
            openTelegramLink: function(url) { bridgeCall('openTelegramLink', { url: url }); },
            sendData: function(data) { bridgeCall('sendData', { data: data }); },
            showPopup: function(params) { 
                return bridgeCall('showPopup', { 
                    title: params.title, 
                    message: params.message, 
                    buttons: JSON.stringify(params.buttons || []) 
                }); 
            },
            showConfirm: function(params) { 
                return bridgeCall('showConfirm', { message: params.message }); 
            },
            showAlert: function(message) { return bridgeCall('showAlert', { message: message }); },
            showScanQrPopup: function(params) { return bridgeCall('showScanQrPopup', { text: params.text }); },
            closeScanQrPopup: function() { bridgeCall('closeScanQrPopup', {}); },
            readTextFromClipboard: function() { return bridgeCall('readTextFromClipboard', {}); },
            HapticFeedback: HapticFeedback,
            MainButton: MainButton,
            BackButton: BackButton,
            onEvent: function(eventType, callback) {
                if (!window.__tgEventListeners[eventType]) window.__tgEventListeners[eventType] = [];
                window.__tgEventListeners[eventType].push(callback);
            },
            offEvent: function(eventType, callback) {
                var listeners = window.__tgEventListeners[eventType] || [];
                window.__tgEventListeners[eventType] = listeners.filter(function(l) { return l !== callback; });
            }
        }
    };
    window.TelegramGameProxy = window.Telegram.WebApp;
})();
""".trimIndent()
