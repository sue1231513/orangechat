/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.extensions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.provider.ModelType
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Brain01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Edit01
import me.rerere.hugeicons.stroke.MoreVertical
import me.rerere.rikkahub.data.model.ExternalMemory
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExternalMemoriesPage(vm: ExternalMemoriesVM = koinViewModel()) {
    val settings = vm.settings.collectAsStateWithLifecycle().value
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ExternalMemory?>(null) }
    var deleteTarget by remember { mutableStateOf<ExternalMemory?>(null) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("进阶记忆") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(HugeIcons.Add01, contentDescription = null)
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.externalMemories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Brain01,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "暂无进阶记忆",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(settings.externalMemories, key = { it.id }) { memory ->
                ExternalMemoryItem(
                    memory = memory,
                    onEdit = { editTarget = memory },
                    onDelete = { deleteTarget = memory },
                )
            }
        }
    }

    if (showAddDialog) {
        ExternalMemoryEditDialog(
            memory = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { memory ->
                vm.addExternalMemory(
                    name = memory.name,
                    supabaseUrl = memory.supabaseUrl,
                    supabaseKey = memory.supabaseKey,
                    tableName = memory.tableName,
                    summariesTableName = memory.summariesTableName,
                    autoSaveMessages = memory.autoSaveMessages,
                    autoSaveDiarySummary = memory.autoSaveDiarySummary,
                    recallCount = memory.recallCount,
                    embeddingModelId = memory.embeddingModelId,
                )
                showAddDialog = false
            }
        )
    }

    editTarget?.let { target ->
        ExternalMemoryEditDialog(
            memory = target,
            onDismiss = { editTarget = null },
            onConfirm = { updated ->
                vm.updateExternalMemory(updated.copy(id = target.id))
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        RikkaConfirmDialog(
            show = true,
            title = "删除进阶记忆",
            confirmText = "删除",
            dismissText = "取消",
            onConfirm = {
                vm.deleteExternalMemory(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            text = {
                Text("确定要删除 \"${target.name}\" 吗？关联该记忆库的助手将自动解除绑定。")
            }
        )
    }
}

@Composable
private fun ExternalMemoryItem(
    memory: ExternalMemory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CustomColors.cardColorsOnSurfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = memory.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = memory.supabaseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = "表: ${memory.tableName} / 摘要: ${memory.summariesTableName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (memory.autoSaveMessages) {
                        FeatureChip("自动保存")
                    }
                    if (memory.autoSaveDiarySummary) {
                        FeatureChip("日记摘要")
                    }
                    FeatureChip("召回 ${memory.recallCount} 条")
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(HugeIcons.MoreVertical, contentDescription = null)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    border = materialModeBorderStroke(),
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(HugeIcons.Edit01, null) },
                        onClick = {
                            expanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(HugeIcons.Delete01, null) },
                        onClick = {
                            expanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun ExternalMemoryEditDialog(
    memory: ExternalMemory?,
    onDismiss: () -> Unit,
    onConfirm: (ExternalMemory) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(memory?.name ?: "") }
    var supabaseUrl by rememberSaveable { mutableStateOf(memory?.supabaseUrl ?: "") }
    var supabaseKey by rememberSaveable { mutableStateOf(memory?.supabaseKey ?: "") }
    var tableName by rememberSaveable { mutableStateOf(memory?.tableName ?: "chat_messages") }
    var summariesTableName by rememberSaveable { mutableStateOf(memory?.summariesTableName ?: "memory_summaries") }
    var autoSaveMessages by rememberSaveable { mutableStateOf(memory?.autoSaveMessages ?: true) }
    var autoSaveDiarySummary by rememberSaveable { mutableStateOf(memory?.autoSaveDiarySummary ?: false) }
    var recallCount by rememberSaveable { mutableStateOf((memory?.recallCount ?: 5).toString()) }
    var embeddingModelId by rememberSaveable { mutableStateOf(memory?.embeddingModelId) }

    val vm: ExternalMemoriesVM = koinViewModel()
    val settings = vm.settings.collectAsStateWithLifecycle().value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (memory == null) "新建进阶记忆" else "编辑进阶记忆") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = { supabaseKey = it },
                        label = { Text("Supabase Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                        label = { Text("消息表名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = summariesTableName,
                        onValueChange = { summariesTableName = it },
                        label = { Text("摘要表名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("自动保存消息")
                        Switch(
                            checked = autoSaveMessages,
                            onCheckedChange = { autoSaveMessages = it }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Supabase 端生成日记摘要")
                        Switch(
                            checked = autoSaveDiarySummary,
                            onCheckedChange = { autoSaveDiarySummary = it }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = recallCount,
                        onValueChange = { recallCount = it.filter { c -> c.isDigit() } },
                        label = { Text("召回条数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        text = "向量模型",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    ModelSelector(
                        modelId = embeddingModelId,
                        providers = settings.providers,
                        type = ModelType.EMBEDDING,
                        allowClear = true,
                        onSelect = { model ->
                            embeddingModelId = model.id
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ExternalMemory(
                            name = name,
                            supabaseUrl = supabaseUrl,
                            supabaseKey = supabaseKey,
                            tableName = tableName.ifBlank { "chat_messages" },
                            summariesTableName = summariesTableName.ifBlank { "memory_summaries" },
                            autoSaveMessages = autoSaveMessages,
                            autoSaveDiarySummary = autoSaveDiarySummary,
                            recallCount = recallCount.toIntOrNull() ?: 5,
                            embeddingModelId = embeddingModelId,
                        )
                    )
                },
                enabled = name.isNotBlank() && supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
