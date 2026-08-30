/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.extensions.workspace

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.ComputerTerminal01
import me.rerere.hugeicons.stroke.File01
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.FileDownload
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.MoreVertical
import me.rerere.hugeicons.stroke.Refresh
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Share03
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import me.rerere.workspace.RootfsInstallStage
import me.rerere.workspace.WorkspaceFileEntry
import me.rerere.workspace.WorkspaceShellStatus
import me.rerere.workspace.WorkspaceStorageArea
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WorkspaceDetailPage(id: String) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val vm: WorkspaceDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val state by vm.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    var deleteTarget by remember { mutableStateOf<WorkspaceFileEntry?>(null) }
    var showInstallDialog by remember { mutableStateOf(false) }
    // 导出目标文件条目：非空时触发系统"另存为"选择器
    var exportTarget by remember { mutableStateOf<WorkspaceFileEntry?>(null) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            toaster.show(it)
            vm.consumeError()
        }
    }

    // 文件选择器：选择任意文件导入到当前正在浏览的工作区目录（FILES / LINUX 均支持）
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val fileName = context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) cursor.getString(nameIdx) else null
            } ?: "imported_file"
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            vm.importFile(fileName, inputStream)
        } else {
            toaster.show(context.getString(R.string.workspace_detail_import_failed))
        }
    }

    // 导出文件：用系统"另存为"选择器，以文件名作为建议名
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val target = exportTarget
        if (uri != null && target != null) {
            context.contentResolver.openOutputStream(uri)?.let { out ->
                vm.exportFile(target, out)
            } ?: toaster.show(context.getString(R.string.workspace_detail_export_failed))
        }
        exportTarget = null
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(state.workspace?.name ?: stringResource(R.string.workspace_page_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(onClick = { vm.refreshFiles() }) {
                        Icon(HugeIcons.Refresh, contentDescription = null)
                    }
                    if (state.rootfsReady) {
                        IconButton(
                            onClick = { navController.navigate(Screen.WorkspaceTerminal(id)) }
                        ) {
                            Icon(HugeIcons.ComputerTerminal01, contentDescription = null)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    label = { Text(stringResource(R.string.workspace_detail_tab_basic)) },
                    icon = { Icon(HugeIcons.Settings03, contentDescription = null) },
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    label = { Text(stringResource(R.string.workspace_detail_tab_files)) },
                    icon = { Icon(HugeIcons.File02, contentDescription = null) },
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                )
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == 1) {
                FloatingActionButton(onClick = { importFileLauncher.launch(arrayOf("*/*")) }) {
                    Icon(HugeIcons.FileImport, contentDescription = null)
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> WorkspaceBasicPage(
                    state = state,
                    approvals = vm.toolApprovals(state.workspace),
                    onInstallRootfs = { showInstallDialog = true },
                    onCancelInstall = { vm.cancelInstall() },
                    onToolApprovalChange = { name, needsApproval -> vm.setToolApproval(name, needsApproval) },
                )

                1 -> WorkspaceFilesPage(
                    state = state,
                    onSwitchArea = { vm.switchArea(it) },
                    onNavigateUp = { vm.navigateUp() },
                    onOpen = { vm.navigateTo(it.path) },
                    onDelete = { deleteTarget = it },
                    onExport = { entry ->
                        exportTarget = entry
                        exportFileLauncher.launch(entry.name)
                    },
                    onShare = { entry ->
                        vm.shareFile(entry, context.cacheDir) { file ->
                            val shareUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    },
                )
            }
        }
    }

    RikkaConfirmDialog(
        show = deleteTarget != null,
        title = stringResource(R.string.workspace_detail_delete_file_title),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            deleteTarget?.let { vm.deleteFile(it) }
            deleteTarget = null
        },
        onDismiss = { deleteTarget = null },
    ) {
        Text(stringResource(R.string.workspace_detail_delete_file_message, deleteTarget?.name ?: ""))
    }

    RootfsInstallUrlDialog(
        show = showInstallDialog,
        onDismiss = { showInstallDialog = false },
        onConfirm = { url ->
            showInstallDialog = false
            vm.installRootfs(url)
        },
    )
}

/**
 * 基本页：工作区信息 + Rootfs 安装/重装 + 工具审批开关
 */
@Composable
private fun WorkspaceBasicPage(
    state: WorkspaceDetailState,
    approvals: Map<String, Boolean>,
    onInstallRootfs: () -> Unit,
    onCancelInstall: () -> Unit,
    onToolApprovalChange: (String, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RootfsCard(
                status = state.shellStatus,
                installing = state.installing,
                progressStageText = installProgressText(state),
                onInstall = onInstallRootfs,
                onCancel = onCancelInstall,
            )
        }

        item {
            ToolApprovalCard(
                approvals = approvals,
                onChange = onToolApprovalChange,
            )
        }
    }
}

/**
 * 文件页：区域选择器 + 路径栏 + 文件列表
 */
@Composable
private fun WorkspaceFilesPage(
    state: WorkspaceDetailState,
    onSwitchArea: (WorkspaceStorageArea) -> Unit,
    onNavigateUp: () -> Unit,
    onOpen: (WorkspaceFileEntry) -> Unit,
    onDelete: (WorkspaceFileEntry) -> Unit,
    onExport: (WorkspaceFileEntry) -> Unit,
    onShare: (WorkspaceFileEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AreaSelector(
                area = state.area,
                onSwitch = onSwitchArea,
            )
        }

        item {
            PathBar(
                path = state.currentPath,
                canGoUp = state.currentPath.isNotBlank(),
                onGoUp = onNavigateUp,
            )
        }

        if (state.filesLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.files.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.workspace_detail_empty_dir),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            // Linux Rootfs 视图下，根目录(/)的系统文件夹禁止删除，避免误删导致 rootfs 损坏；
            // 工作区文件视图(FILES)以及 Rootfs 子目录不受此限制。
            val hideDelete = state.area == WorkspaceStorageArea.LINUX && state.currentPath.isBlank()
            items(state.files, key = { it.path }) { entry ->
                FileRow(
                    entry = entry,
                    onClick = {
                        if (entry.isDirectory) {
                            onOpen(entry)
                        }
                    },
                    onDelete = { onDelete(entry) },
                    onExport = { onExport(entry) },
                    onShare = { onShare(entry) },
                    showDelete = !hideDelete,
                )
            }
        }
    }
}

@Composable
private fun RootfsInstallUrlDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var url by remember(show) { mutableStateOf("https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.3-base-arm64.tar.gz") }
    var error by remember(show) { mutableStateOf(false) }

    RikkaConfirmDialog(
        show = show,
        title = stringResource(R.string.workspace_detail_install_rootfs),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            if (url.isBlank()) {
                error = true
            } else {
                onConfirm(url.trim())
            }
        },
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    error = false
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error,
                placeholder = {
                    Text(stringResource(R.string.workspace_detail_install_url_hint))
                },
                supportingText = {
                    if (error) {
                        Text(stringResource(R.string.workspace_detail_install_url_empty))
                    }
                },
            )
        }
    }
}

@Composable
private fun RootfsCard(
    status: WorkspaceShellStatus,
    installing: Boolean,
    progressStageText: String?,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.workspace_detail_rootfs_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(workspaceStatusLabel(status)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (installing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (progressStageText != null) {
                    Text(
                        text = progressStageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            } else if (status != WorkspaceShellStatus.READY) {
                Button(onClick = onInstall) {
                    Text(stringResource(R.string.workspace_detail_install_rootfs))
                }
            }
        }
    }
}

@Composable
private fun ToolApprovalCard(
    approvals: Map<String, Boolean>,
    onChange: (String, Boolean) -> Unit,
) {
    Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(R.string.workspace_detail_tool_approval_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp),
            )
            approvals.entries.forEachIndexed { index, (name, needsApproval) ->
                if (index > 0) HorizontalDivider()
                FormItem(
                    modifier = Modifier.padding(8.dp),
                    label = { Text(workspaceToolLabel(name)) },
                    tail = {
                        Switch(
                            checked = needsApproval,
                            onCheckedChange = { onChange(name, it) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AreaSelector(
    area: WorkspaceStorageArea,
    onSwitch: (WorkspaceStorageArea) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = area == WorkspaceStorageArea.FILES,
            onClick = { onSwitch(WorkspaceStorageArea.FILES) },
            label = { Text(stringResource(R.string.workspace_detail_area_files)) },
        )
        FilterChip(
            selected = area == WorkspaceStorageArea.LINUX,
            onClick = { onSwitch(WorkspaceStorageArea.LINUX) },
            label = { Text(stringResource(R.string.workspace_detail_area_linux)) },
        )
    }
}

@Composable
private fun PathBar(
    path: String,
    canGoUp: Boolean,
    onGoUp: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canGoUp) {
            IconButton(onClick = onGoUp) {
                Icon(HugeIcons.ArrowLeft01, contentDescription = null)
            }
        }
        Text(
            text = "/" + path,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileRow(
    entry: WorkspaceFileEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit = {},
    onShare: () -> Unit = {},
    showDelete: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CustomColors.cardColorsOnSurfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) HugeIcons.Folder01 else HugeIcons.File01,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (entry.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!entry.isDirectory) {
                    Text(
                        text = formatSize(entry.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // 非目录文件显示"更多"下拉菜单（导出/分享）；目录不显示
            if (!entry.isDirectory) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = HugeIcons.MoreVertical,
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        border = materialModeBorderStroke(),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_detail_export)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = HugeIcons.FileDownload,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workspace_detail_share)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = HugeIcons.Share03,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            },
                        )
                    }
                }
            }
            if (showDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = HugeIcons.Delete01,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / 1024 / 1024} MB"
}

/**
 * 工作区工具名 -> 显示用说明文字的字符串资源 id。
 * 用于工具审批开关上方的说明文字。
 */
@Composable
private fun workspaceToolLabel(name: String): String = when (name) {
    "workspace_read_file" -> stringResource(R.string.workspace_detail_tool_read_file)
    "workspace_write_file" -> stringResource(R.string.workspace_detail_tool_write_file)
    "workspace_edit_file" -> stringResource(R.string.workspace_detail_tool_edit_file)
    "workspace_shell" -> stringResource(R.string.workspace_detail_tool_shell)
    else -> name
}

@Composable
private fun installProgressText(state: WorkspaceDetailState): String? {
    val progress = state.installProgress ?: return null
    return when (progress.stage) {
        RootfsInstallStage.DOWNLOADING -> {
            val total = progress.totalBytes
            if (total != null && total > 0) {
                val percent = (progress.bytesRead * 100 / total)
                stringResource(R.string.workspace_detail_install_downloading_percent, percent)
            } else {
                stringResource(R.string.workspace_detail_install_downloading)
            }
        }

        RootfsInstallStage.EXTRACTING ->
            stringResource(R.string.workspace_detail_install_extracting, progress.entriesExtracted)

        RootfsInstallStage.INSTALLED ->
            stringResource(R.string.workspace_detail_install_finishing)
    }
}