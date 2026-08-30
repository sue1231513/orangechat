/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.plugin.ui

import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import me.rerere.rikkahub.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import me.rerere.ai.provider.ModelType
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.CheckmarkCircle01
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.plugin.model.PluginConfigField
import me.rerere.rikkahub.plugin.model.PluginFolder
import me.rerere.rikkahub.plugin.model.PluginInfo
import me.rerere.rikkahub.plugin.model.PluginToolDefinition
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/**
 * 插件详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailPage(
    pluginId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCustomPage: (String) -> Unit = {},
    onNavigateToWebView: (pluginId: String, entryPath: String) -> Unit = { _, _ -> },
    onNavigateToDeclarativeUI: (pluginId: String) -> Unit = {},
    viewModel: PluginViewModel = koinViewModel()
) {
    val plugin = viewModel.getPlugin(pluginId)

    if (plugin == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("插件不存在")
        }
        return
    }

    var configValues by remember(pluginId) {
        mutableStateOf(plugin.config.toMutableMap())
    }

    LaunchedEffect(plugin.config) {
        configValues = plugin.config.toMutableMap()
    }

    val folders by viewModel.folders.collectAsState()

    Scaffold(
        containerColor = settingsScaffoldContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text("插件详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = HugeIcons.ArrowLeft01,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PluginInfoSection(plugin)

            // Supabase 记忆库插件的数据库初始化说明
            if (plugin.manifest.id == "com.orangechat.plugin.supabase_memory") {
                Spacer(modifier = Modifier.height(16.dp))
                SupabaseSetupInstructions()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 文件夹归属选择器
            FolderSelector(
                currentFolderId = plugin.folderId,
                folders = folders,
                onSelect = { folderId ->
                    viewModel.movePluginToFolder(pluginId, folderId)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 自定义页面入口（声明式 UI 优先于 WebView）
            val uiDeclaration = plugin.manifest.ui
            if (uiDeclaration != null) {
                CustomPageEntry(
                    label = "管理页面",
                    description = "打开插件管理页面",
                    onClick = { onNavigateToDeclarativeUI(plugin.manifest.id) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else if (plugin.manifest.customPageWebView != null) {
                val webViewConfig = plugin.manifest.customPageWebView
                CustomPageEntry(
                    label = "管理页面",
                    description = "打开插件管理页面",
                    onClick = { onNavigateToWebView(plugin.manifest.id, webViewConfig.entry) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else if (plugin.manifest.customPage != null) {
                val customPage = plugin.manifest.customPage
                CustomPageEntry(
                    label = when (customPage) {
                        "memory_bank" -> "记忆库管理"
                        else -> "管理页面"
                    },
                    description = when (customPage) {
                        "memory_bank" -> "查看、搜索和管理记忆库中的记忆数据"
                        else -> "打开插件专属管理页面"
                    },
                    onClick = { onNavigateToCustomPage(customPage) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (plugin.manifest.tools.isNotEmpty()) {
                Text(text = "提供的工具", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                plugin.manifest.tools.forEach { tool -> ToolItem(tool) }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (plugin.manifest.config.isNotEmpty()) {
                Text(text = "插件配置", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                plugin.manifest.config.forEach { field ->
                    ConfigField(
                        field = field,
                        value = configValues[field.name],
                        onValueChange = { value ->
                            configValues = configValues.toMutableMap().apply {
                                if (value != null) put(field.name, value) else remove(field.name)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.updatePluginConfig(pluginId, configValues) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = HugeIcons.CheckmarkCircle01, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("保存配置")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(text = "插件路径", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = plugin.directory.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 文件夹归属选择器
 * 用于将当前插件移动到不同文件夹
 */
@Composable
private fun FolderSelector(
    currentFolderId: String?,
    folders: List<PluginFolder>,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentFolder = folders.find { it.id == currentFolderId }
    val displayText = currentFolder?.name ?: "未分组"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = { },
            readOnly = true,
            label = { Text("所属文件夹") },
            supportingText = { Text("点击选择文件夹，移动插件到其他分组") },
            leadingIcon = {
                Icon(
                    imageVector = HugeIcons.Folder01,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = HugeIcons.ArrowRight01,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            border = materialModeBorderStroke(),
        ) {
            DropdownMenuItem(
                text = { Text("未分组") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.name) },
                    onClick = {
                        onSelect(folder.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PluginInfoSection(plugin: PluginInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = plugin.manifest.icon,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(text = plugin.manifest.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "v${plugin.manifest.version} · ${plugin.manifest.author}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = plugin.manifest.description, style = MaterialTheme.typography.bodyLarge)
    if (plugin.loadError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "加载失败: ${plugin.loadError}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ToolItem(tool: PluginToolDefinition) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = tool.name, style = MaterialTheme.typography.bodyLarge)
        Text(text = tool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tool.parameters.isNotEmpty()) {
            Text(
                text = "参数: ${tool.parameters.joinToString { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CustomPageEntry(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(HugeIcons.Database02, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Icon(HugeIcons.ArrowRight01, contentDescription = "进入")
    }
}

@Composable
private fun ConfigField(
    field: PluginConfigField,
    value: JsonElement?,
    onValueChange: (JsonElement?) -> Unit
) {
    when (field.type) {
        "boolean" -> BooleanConfigField(field, value, onValueChange)
        "select" -> SelectConfigField(field, value, onValueChange)
        "password" -> StringConfigField(field, value, onValueChange, isPassword = true)
        "model" -> ModelConfigField(field, value, onValueChange)
        else -> StringConfigField(field, value, onValueChange)
    }
}

@Composable
private fun StringConfigField(
    field: PluginConfigField,
    value: JsonElement?,
    onValueChange: (JsonElement?) -> Unit,
    isPassword: Boolean = false
) {
    var textValue by remember(field.name) {
        val v = value as? JsonPrimitive
        val d = field.default as? JsonPrimitive
        mutableStateOf(v?.contentOrNull ?: d?.contentOrNull ?: "")
    }
    OutlinedTextField(
        value = textValue,
        onValueChange = {
            textValue = it
            onValueChange(if (it.isEmpty()) null else JsonPrimitive(it))
        },
        label = { Text(field.label) },
        placeholder = { Text(field.placeholder ?: "") },
        supportingText = field.description?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun BooleanConfigField(
    field: PluginConfigField,
    value: JsonElement?,
    onValueChange: (JsonElement?) -> Unit
) {
    var checked by remember(field.name) {
        val v = value as? JsonPrimitive
        val d = field.default as? JsonPrimitive
        mutableStateOf(v?.booleanOrNull ?: d?.booleanOrNull ?: false)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
            if (field.description != null) {
                Text(text = field.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = { checked = it; onValueChange(JsonPrimitive(it)) })
    }
}

@Composable
private fun SelectConfigField(
    field: PluginConfigField,
    value: JsonElement?,
    onValueChange: (JsonElement?) -> Unit
) {
    var selectedValue by remember(field.name) {
        val v = value as? JsonPrimitive
        val d = field.default as? JsonPrimitive
        mutableStateOf(v?.contentOrNull ?: d?.contentOrNull ?: field.options?.firstOrNull()?.value ?: "")
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
        if (field.description != null) {
            Text(text = field.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val optionsText = field.options?.joinToString(", ") { it.label } ?: ""
        OutlinedTextField(
            value = field.options?.find { it.value == selectedValue }?.label ?: selectedValue,
            onValueChange = { },
            readOnly = true,
            supportingText = { Text("可用选项: $optionsText") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModelConfigField(
    field: PluginConfigField,
    value: JsonElement?,
    onValueChange: (JsonElement?) -> Unit
) {
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val providers = settings.providers
    
    // 解析当前选中的模型 ID
    val currentModelId = remember(value) {
        val v = value as? JsonPrimitive
        val modelIdStr = v?.contentOrNull
        try {
            modelIdStr?.let { Uuid.parse(it) }
        } catch (_: Exception) {
            null
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
        if (field.description != null) {
            Text(text = field.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        ModelSelector(
            modelId = currentModelId,
            providers = providers,
            type = ModelType.CHAT,
            modifier = Modifier.fillMaxWidth(),
            onSelect = { model ->
                // 保存选中的模型 ID 为字符串
                onValueChange(JsonPrimitive(model.id.toString()))
            }
        )
    }
}

/**
 * Supabase 记忆库插件的数据库初始化说明
 */
@Composable
private fun SupabaseSetupInstructions() {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copiedIndex by remember { mutableStateOf(-1) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "数据库初始化",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "首次使用需要在 Supabase 数据库中创建表。复制以下 SQL 到 Supabase Dashboard 的 SQL Editor 中执行：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val sql = """create table chat_messages (
  id uuid default gen_random_uuid() primary key,
  assistant_id text not null,
  conversation_id text not null,
  role text not null,
  content text not null,
  created_at timestamp with time zone default now()
);

create index idx_chat_messages_conversation on chat_messages(conversation_id);
create index idx_chat_messages_created on chat_messages(created_at);"""
            
            CopyableCodeBlock(
                code = sql,
                index = 0,
                copiedIndex = copiedIndex,
                onCopy = { idx, text ->
                    clipboardManager.setText(AnnotatedString(text))
                    copiedIndex = idx
                }
            )
        }
    }
}

/**
 * 可复制的代码块
 */
@Composable
private fun CopyableCodeBlock(
    code: String,
    index: Int,
    copiedIndex: Int,
    onCopy: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SQL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(
                    onClick = { onCopy(index, code) },
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = if (copiedIndex == index) "已复制 ✓" else "复制",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}