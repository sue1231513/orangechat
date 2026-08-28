/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Clean
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.db.entity.ManagedFileEntity
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.files.FileFolders
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.service.MemoryBankService
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.compose.koinInject
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingFilesPage(
    filesManager: FilesManager = koinInject(),
    appDatabase: AppDatabase = koinInject(),
    memoryBankService: MemoryBankService = koinInject(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val folders = remember { listOf(FileFolders.UPLOAD) }

    // 预先获取字符串资源
    val deletedToast = stringResource(R.string.setting_files_page_deleted_toast)
    val deleteFailedToast = stringResource(R.string.setting_files_page_delete_failed_toast)

    var selectedFolder by remember { mutableStateOf(FileFolders.UPLOAD) }
    var pendingDelete by remember { mutableStateOf<ManagedFileEntity?>(null) }
    var showFileCleanSheet by remember { mutableStateOf(false) }
    var selectedFileCleanRange by remember { mutableStateOf(FileCleanRange.DAYS_7) }
    val files by filesManager.observe(selectedFolder).collectAsState(initial = emptyList())

        // 数据库清理
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showOldConversationsDialog by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }
    var cleanupResult by remember { mutableStateOf<String?>(null) }
    var daysToKeep by remember { mutableIntStateOf(90) }
    var cleanableNodes by remember { mutableIntStateOf(-1) }  // -1 = 未加载
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showFileCleanSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFileCleanSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FileCleanSheet(
                selectedRange = selectedFileCleanRange,
                onRangeSelected = { selectedFileCleanRange = it },
                onClean = {
                    showFileCleanSheet = false
                    scope.launch {
                        val days = selectedFileCleanRange.days
                        val ok = if (days == null) {
                            filesManager.deleteAll(selectedFolder)
                        } else {
                            filesManager.deleteOlderThan(
                                folder = selectedFolder,
                                cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()),
                            )
                        }
                        toaster.show(if (ok) "附件清理完成" else "部分附件清理失败")
                    }
                },
            )
        }
    }

    if (pendingDelete != null) {
        val target = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.setting_files_page_delete_file_title)) },
            text = { Text(target.displayName) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = filesManager.delete(target.id, deleteFromDisk = true)
                            if (ok) {
                                toaster.show(deletedToast)
                            } else {
                                toaster.show(deleteFailedToast)
                            }
                            pendingDelete = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.setting_files_page_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.setting_files_page_cancel_action))
                }
            }
        )
    }

    // 旧对话清理对话框
    if (showOldConversationsDialog) {
        // 打开时懒加载一次可清理数量
        if (cleanableNodes == -1) {
            LaunchedEffect(Unit) {
                cleanableNodes = runCatching {
                    val cutoff = System.currentTimeMillis() - daysToKeep.toLong() * 86_400_000L
                    appDatabase.openHelper.writableDatabase.query(
                        "SELECT COUNT(*) FROM message_node WHERE conversation_id IN (SELECT id FROM conversationentity WHERE update_at < $cutoff)"
                    ).use { cur -> if (cur.moveToFirst()) cur.getInt(0) else 0 }
                }.getOrDefault(0)
            }
        }
        AlertDialog(
            onDismissRequest = { if (!isCleaning) { showOldConversationsDialog = false; cleanableNodes = -1 } },
            title = { Text("清理旧对话") },
            text = {
                Column {
                    Text("本地对话记录会越来越大。清理后旧对话只保留在云端（Supabase），需要时可通过搜索找回。")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(30, 60, 90, 180).forEach { days ->
                            FilterChip(
                                selected = daysToKeep == days,
                                onClick = {
                                    daysToKeep = days
                                    cleanableNodes = -1
                                },
                                label = { Text("${days}天") }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (cleanableNodes >= 0) {
                            "将清理 ${cleanableNodes} 条本地消息记录（保留最近 $daysToKeep 天）"
                        } else {
                            "计算可清理量…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isCleaning = true
                            try {
                                val cutoff = System.currentTimeMillis() - daysToKeep.toLong() * 86_400_000L
                                val db = appDatabase.openHelper.writableDatabase
                                db.query("DELETE FROM message_node WHERE conversation_id IN (SELECT id FROM conversationentity WHERE update_at < $cutoff)")
                                    .use { it.moveToFirst() }
                                db.query("DELETE FROM conversationentity WHERE update_at < $cutoff")
                                    .use { it.moveToFirst() }
                                // FTS 索引同步清理
                                runCatching { db.query("DELETE FROM message_fts WHERE conversation_id NOT IN (SELECT id FROM conversationentity)").use { it.moveToFirst() } }
                                try { db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() } } catch (_: Exception) {}
                                try { db.query("VACUUM").use { it.moveToFirst() } } catch (_: Exception) {}
                                cleanupResult = "已清理 $daysToKeep 天前的本地记录并压缩数据库"
                                showOldConversationsDialog = false
                                cleanableNodes = -1
                                cleanupResult?.let { toaster.show(it) }
                            } catch (e: Exception) {
                                cleanupResult = "清理失败: ${e.message}"
                                cleanupResult?.let { toaster.show(it) }
                            }
                            isCleaning = false
                        }
                    },
                    enabled = !isCleaning && cleanableNodes != -1
                ) {
                    Text(if (isCleaning) "清理中…" else "清理")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isCleaning) { showOldConversationsDialog = false; cleanableNodes = -1 } }
                ) {
                    Text(stringResource(R.string.setting_files_page_cancel_action))
                }
            }
        )
    }

    // 数据库清理对话框
    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCleaning) showCleanupDialog = false },
            title = { Text("清理数据库") },
            text = {
                if (isCleaning) {
                    Text("正在清理并压缩数据库，请稍候…")
                } else {
                    Text("将清空本地 Embedding 向量数据（已迁移至云端）并执行 VACUUM 压缩数据库以回收磁盘空间。\n\n此操作不可撤销，但不会影响对话记录和云端数据。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isCleaning = true
                            try {
                                // 1. 清空本地 embedding
                                val cleared = memoryBankService.clearLocalEmbeddingsAndVacuum()
                                // 2. WAL checkpoint + VACUUM
                                // RequerySQLiteOpenHelper 不支持 execSQL(PRAGMA/VACUUM)，
                                // 必须用 rawQuery 执行（VACUUM 虽然不返回行，但 rawQuery 可以执行无结果集的语句）
                                val db = appDatabase.openHelper.writableDatabase
                                try { db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() } } catch (_: Exception) {}
                                try { db.query("VACUUM").use { it.moveToFirst() } } catch (_: Exception) {}
                                cleanupResult = "已清空 $cleared 条 Embedding，数据库已压缩"
                            } catch (e: Exception) {
                                cleanupResult = "清理失败: ${e.message}"
                            }
                            isCleaning = false
                            showCleanupDialog = false
                            cleanupResult?.let { toaster.show(it) }
                        }
                    },
                    enabled = !isCleaning
                ) {
                    Text("清理")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCleanupDialog = false },
                    enabled = !isCleaning
                ) {
                    Text(stringResource(R.string.setting_files_page_cancel_action))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.setting_files_page_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { showFileCleanSheet = true },
                        enabled = files.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Clean,
                            contentDescription = "清理附件",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FolderRow(
                folders = folders,
                selectedFolder = selectedFolder,
                onFolderSelected = { selectedFolder = it }
            )

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.setting_files_page_no_files))
                }
            } else {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2)
                ) {
                    items(files, key = { it.id }) { file ->
                        FileItem(
                            file = file,
                            fileOnDisk = filesManager.getFile(file),
                            onDelete = { pendingDelete = file }
                        )
                    }
                }
            }

            // 数据库清理入口
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = CustomColors.listItemColors.containerColor),
                onClick = { showCleanupDialog = true }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "数据库清理",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "清空本地 Embedding 向量并压缩数据库，回收磁盘空间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 旧对话清理入口
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CustomColors.listItemColors.containerColor),
                onClick = {
                    daysToKeep = 90
                    cleanableNodes = -1
                    showOldConversationsDialog = true
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "清理旧对话",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "删除 N 天前的本地对话记录（云端保留），显著减小应用体积",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private enum class FileCleanRange(val days: Int?) {
    DAYS_7(7),
    DAYS_14(14),
    DAYS_30(30),
    ALL(null),
}

@Composable
private fun FileCleanSheet(
    selectedRange: FileCleanRange,
    onRangeSelected: (FileCleanRange) -> Unit,
    onClean: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text("清理附件", style = MaterialTheme.typography.headlineSmall)
        Text(
            "按上传时间清理当前文件夹中的附件；不会删除聊天消息。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
        FileCleanRange.entries.forEach { range ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedRange == range,
                    onClick = { onRangeSelected(range) },
                )
                Text(
                    range.days?.let { "$it 天前的附件" } ?: "全部附件",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        TextButton(
            onClick = onClean,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("清理")
        }
    }
}

@Composable
private fun FolderRow(
    folders: List<String>,
    selectedFolder: String,
    onFolderSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        folders.forEach { folder ->
            FilterChip(
                selected = selectedFolder == folder,
                onClick = { onFolderSelected(folder) },
                label = { Text(folderDisplayName(folder)) }
            )
        }
    }
}

@Composable
private fun folderDisplayName(folder: String): String = when (folder) {
    FileFolders.UPLOAD -> stringResource(R.string.setting_files_page_folder_upload)
    else -> folder
}

@Composable
private fun FileItem(
    file: ManagedFileEntity,
    fileOnDisk: File,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CustomColors.listItemColors.containerColor)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (file.mimeType.startsWith("image/")) {
                    AsyncImage(
                        model = fileOnDisk,
                        contentDescription = file.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HugeIcons.Image02,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        HugeIcons.Delete01,
                        contentDescription = stringResource(R.string.setting_files_page_delete_content_description)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.mimeType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatBytes(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1fKB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1fMB", mb)
    val gb = mb / 1024.0
    return String.format("%.1fGB", gb)
}
