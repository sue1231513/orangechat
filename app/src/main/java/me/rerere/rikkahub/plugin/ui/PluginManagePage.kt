/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.plugin.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import me.rerere.rikkahub.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.PlusSign
import me.rerere.hugeicons.stroke.Reload
import me.rerere.rikkahub.R
import me.rerere.rikkahub.plugin.model.PluginFolder
import me.rerere.rikkahub.plugin.model.PluginInfo
import me.rerere.rikkahub.ui.components.ui.RiskConfirmDialog
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import org.koin.androidx.compose.koinViewModel

private const val TAG = "PluginManagePage"

/**
 * 插件管理页面（文件夹列表页）
 * 显示文件夹列表和未分组插件，点进文件夹才看到插件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagePage(
    onNavigateToFolder: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: PluginViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val plugins by viewModel.plugins.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    val pendingImport by viewModel.pendingImport.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val hasStoragePermission = remember { mutableStateOf(checkStoragePermission()) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf<PluginFolder?>(null) }
    var showDeleteFolderConfirm by remember { mutableStateOf<PluginFolder?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission.value = checkStoragePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.previewPlugin(it) }
    }

    LaunchedEffect(importState) {
        when (importState) {
            is PluginViewModel.ImportState.Success -> {
                snackbarHostState.showSnackbar(message = "插件导入成功")
                viewModel.resetImportState()
            }
            is PluginViewModel.ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "导入失败: ${(importState as PluginViewModel.ImportState.Error).message}"
                )
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(operationState) {
        when (operationState) {
            is PluginViewModel.OperationState.Success -> {
                snackbarHostState.showSnackbar(
                    message = (operationState as PluginViewModel.OperationState.Success).message
                )
                viewModel.resetOperationState()
            }
            is PluginViewModel.OperationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (operationState as PluginViewModel.OperationState.Error).message
                )
                viewModel.resetOperationState()
            }
            else -> {}
        }
    }

    LaunchedEffect(hasStoragePermission.value) {
        if (hasStoragePermission.value) {
            viewModel.refreshPlugins()
        }
    }

    pendingImport?.let { pending ->
        val manifest = pending.manifest
        AlertDialog(
            onDismissRequest = { viewModel.cancelImportPreview() },
            title = { Text("确认安装插件") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("插件名称: ${manifest.name}")
                    Text("版本: ${manifest.version}")
                    Text("作者: ${manifest.author}")
                    if (manifest.description.isNotBlank()) {
                        Text("描述: ${manifest.description}")
                    }
                    if (manifest.tools.isNotEmpty()) {
                        Text("")
                        Text("声明的工具:", style = MaterialTheme.typography.labelLarge)
                        manifest.tools.forEach { tool ->
                            Text("• ${tool.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (manifest.permissions.isNotEmpty()) {
                        Text("")
                        Text("申请的权限:", style = MaterialTheme.typography.labelLarge)
                        manifest.permissions.forEach { perm ->
                            Text("• $perm", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (manifest.allowedHosts.isNotEmpty()) {
                        Text("")
                        Text("允许访问的网络域名:", style = MaterialTheme.typography.labelLarge)
                        manifest.allowedHosts.forEach { host ->
                            Text("• $host", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("")
                        Text("• 不允许任何网络访问", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text("确认安装")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImportPreview() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件管理") },
                actions = {
                    if (hasStoragePermission.value) {
                        IconButton(onClick = { viewModel.refreshPlugins() }) {
                            Icon(imageVector = HugeIcons.Reload, contentDescription = "刷新")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasStoragePermission.value) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    icon = { Icon(HugeIcons.PlusSign, null) },
                    text = { Text("新建文件夹") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = settingsScaffoldContainerColor(),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasStoragePermission.value) {
                StoragePermissionGate(
                    onGrantClick = { openStoragePermissionSettings(context) }
                )
            } else if (isLoading && plugins.isEmpty() && folders.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PluginFolderContent(
                    folders = folders,
                    plugins = plugins,
                    onFolderClick = { folder -> onNavigateToFolder(folder.id) },
                    onFolderLongClick = { folder -> showRenameFolderDialog = folder },
                    onPluginClick = { plugin -> onNavigateToDetail(plugin.manifest.id) },
                    onTogglePlugin = { plugin, enabled ->
                        viewModel.togglePlugin(plugin.manifest.id, enabled)
                    },
                    onDeletePlugin = { plugin -> viewModel.deletePlugin(plugin.manifest.id) },
                    onImportPlugin = { filePickerLauncher.launch("application/zip") }
                )
            }

            AnimatedVisibility(
                visible = importState is PluginViewModel.ImportState.Loading,
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    showRenameFolderDialog?.let { folder ->
        RenameFolderDialog(
            folder = folder,
            onDismiss = { showRenameFolderDialog = null },
            onConfirm = { newName ->
                viewModel.renameFolder(folder.id, newName)
                showRenameFolderDialog = null
            },
            onDelete = {
                showRenameFolderDialog = null
                showDeleteFolderConfirm = folder
            }
        )
    }

    showDeleteFolderConfirm?.let { folder ->
        val count = plugins.count { it.folderId == folder.id }
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirm = null },
            title = { Text("删除文件夹") },
            text = {
                Text(
                    if (count > 0) {
                        "确定要删除文件夹「${folder.name}」吗？该文件夹下有 $count 个插件，将自动移到未分组。"
                    } else {
                        "确定要删除空文件夹「${folder.name}」吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder.id)
                    showDeleteFolderConfirm = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderConfirm = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PluginFolderContent(
    folders: List<PluginFolder>,
    plugins: List<PluginInfo>,
    onFolderClick: (PluginFolder) -> Unit,
    onFolderLongClick: (PluginFolder) -> Unit,
    onPluginClick: (PluginInfo) -> Unit,
    onTogglePlugin: (PluginInfo, Boolean) -> Unit,
    onDeletePlugin: (PluginInfo) -> Unit,
    onImportPlugin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.plugin_import_security_warning_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.plugin_import_security_warning_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.plugin_import_security_warning_action),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (folders.isNotEmpty()) {
            item {
                Text(
                    text = "文件夹",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(items = folders, key = { it.id }) { folder ->
                val count = plugins.count { it.folderId == folder.id }
                FolderCard(
                    folder = folder,
                    pluginCount = count,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            }
        }

        val ungroupedPlugins = plugins.filter { it.folderId == null }
        if (ungroupedPlugins.isNotEmpty() || folders.isEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "未分组插件",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (folders.isEmpty() && ungroupedPlugins.isEmpty()) {
                        Text(
                            text = "点击右下角创建文件夹",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (ungroupedPlugins.isEmpty() && folders.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("暂无未分组插件")
                    }
                }
            }
        }

        items(items = ungroupedPlugins, key = { it.manifest.id }) { plugin ->
            PluginCard(
                plugin = plugin,
                onClick = { onPluginClick(plugin) },
                onToggle = { enabled -> onTogglePlugin(plugin, enabled) },
                onDelete = { onDeletePlugin(plugin) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    folder: PluginFolder,
    pluginCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = HugeIcons.Folder01,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$pluginCount 个插件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun PluginCard(
    plugin: PluginInfo,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = plugin.manifest.icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.manifest.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "v${plugin.manifest.version} · ${plugin.manifest.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (plugin.loadError != null) {
                    Text(
                        text = "加载失败: ${plugin.loadError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = HugeIcons.Delete02,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除插件 \"${plugin.manifest.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("文件夹名称") },
                placeholder = { Text("输入文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun RenameFolderDialog(
    folder: PluginFolder,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit
) {
    var folderName by remember(folder.id) { mutableStateOf(folder.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑文件夹") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank() && folderName != folder.name
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

private fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}

private fun openStoragePermissionSettings(context: android.content.Context) {
    try {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "openStoragePermissionSettings: precise intent failed, pkg=${context.packageName}", e)
        try {
            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } catch (ex: Exception) {
            Log.e(TAG, "openStoragePermissionSettings: fallback intent also failed", ex)
        }
    }
}

@Composable
private fun StoragePermissionGate(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "需要存储权限加载插件",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "插件存放在外部存储目录，需要授予「所有文件访问权限」才能加载和管理插件",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGrantClick) {
            Text("授予权限")
        }
    }
}