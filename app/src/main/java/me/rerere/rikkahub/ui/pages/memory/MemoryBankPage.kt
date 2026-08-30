/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.memory

import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.DatabaseSync
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity
import me.rerere.rikkahub.data.service.MemoryBankService
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBankPage(
    onBack: () -> Unit,
) {
    val vm: MemoryBankVM = koinViewModel()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val todayPhaseSummaries by vm.todayPhaseSummaries.collectAsStateWithLifecycle()
    val dailySummaries by vm.dailySummaries.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedType by vm.selectedType.collectAsStateWithLifecycle()
    val selectedAssistantId by vm.selectedAssistantId.collectAsStateWithLifecycle()
    val assistantIds by vm.assistantIds.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showDeleteDialog by remember { mutableStateOf<MemoryBankEntity?>(null) }

    Scaffold(
        containerColor = settingsScaffoldContainerColor(),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text("记忆库")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = "返回")
                    }
                },
                actions = {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { vm.rebuildIndex() }) {
                        Icon(HugeIcons.Database02, contentDescription = "重建索引")
                    }
                    IconButton(onClick = { vm.processPendingVectors() }) {
                        Icon(HugeIcons.DatabaseSync, contentDescription = "处理向量化")
                    }
                    IconButton(onClick = { vm.loadMemories() }) {
                        Icon(HugeIcons.Refresh01, contentDescription = "刷新")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 统计卡片 - 消息只显示条数
            item {
                StatsRow(stats)
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 助手筛选
            if (assistantIds.isNotEmpty()) {
                item {
                    AssistantFilterRow(
                        selectedAssistantId = selectedAssistantId,
                        assistantIds = assistantIds,
                        onAssistantSelected = { vm.setSelectedAssistantId(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 类型筛选
            item {
                TypeFilterRow(
                    selectedType = selectedType,
                    onTypeSelected = { vm.setSelectedType(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 搜索栏 (only when type filter is active)
            if (selectedType.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索记忆...") },
                        leadingIcon = {
                            Icon(HugeIcons.Search01, contentDescription = null)
                        },
                        singleLine = true,
                    )
                }
            }

            // Default view: today's phase summaries + daily summaries
            if (selectedType.isEmpty()) {
                // 今日阶段总结
                if (todayPhaseSummaries.isNotEmpty()) {
                    item {
                        Text(
                            "今日阶段总结",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(todayPhaseSummaries, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onDelete = { showDeleteDialog = memory }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 每日总结（日记）
                if (dailySummaries.isNotEmpty()) {
                    item {
                        Text(
                            "日记",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    items(dailySummaries, key = { it.id }) { memory ->
                        DiaryCard(
                            memory = memory,
                            onDelete = { showDeleteDialog = memory }
                        )
                    }
                }

                if (todayPhaseSummaries.isEmpty() && dailySummaries.isEmpty() && !loading) {
                    item {
                        Text(
                            text = "暂无总结数据",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Type filter active: show filtered memories list
                items(memories, key = { it.id }) { memory ->
                    MemoryCard(
                        memory = memory,
                        onDelete = { showDeleteDialog = memory }
                    )
                }

                if (memories.isEmpty() && !loading) {
                    item {
                        Text(
                            text = "暂无数据",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { memory ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除记忆") },
            text = { Text("确定要删除这条记忆吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMemory(memory.id)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun StatsRow(stats: MemoryBankService.MemoryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("总计", stats.total, Modifier.weight(1f))
        StatCard("消息", stats.messageCount, Modifier.weight(1f))
        StatCard("总结", stats.summaryCount, Modifier.weight(1f))
        StatCard("手动", stats.manualCount, Modifier.weight(1f))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("已向量化", stats.vectorizedCount, Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
        StatCard("待处理", stats.pendingCount, Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer)
        StatCard("失败", stats.failedCount, Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer)
    }
}

@Composable
private fun StatCard(label: String, count: Int, modifier: Modifier = Modifier, containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantFilterRow(
    selectedAssistantId: String?,
    assistantIds: List<String>,
    onAssistantSelected: (String?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedAssistantId == null,
            onClick = { onAssistantSelected(null) },
            label = { Text("全部助手") },
        )
        assistantIds.forEach { id ->
            FilterChip(
                selected = selectedAssistantId == id,
                onClick = { onAssistantSelected(id) },
                label = { Text(id.take(16)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterRow(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
) {
    val types = listOf("" to "总结视图", "phase_summary" to "阶段总结", "daily_summary" to "日记", "message" to "消息", "manual" to "手动")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        types.forEach { (value, label) ->
            FilterChip(
                selected = selectedType == value,
                onClick = { onTypeSelected(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: MemoryBankEntity,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (memory.vectorStatus) {
                "done" -> MaterialTheme.colorScheme.surface
                "pending" -> MaterialTheme.colorScheme.tertiaryContainer
                "failed" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = when (memory.type) {
                            "message" -> MaterialTheme.colorScheme.primaryContainer
                            "phase_summary" -> MaterialTheme.colorScheme.secondaryContainer
                            "daily_summary" -> MaterialTheme.colorScheme.tertiaryContainer
                            "manual" -> MaterialTheme.colorScheme.inversePrimary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = when (memory.type) {
                                "message" -> "消息"
                                "phase_summary" -> "阶段总结"
                                "daily_summary" -> "日记"
                                "manual" -> "手动"
                                else -> memory.type
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    if (memory.vectorStatus == "done") {
                        Icon(
                            HugeIcons.Database02,
                            contentDescription = "已向量化",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    val timeStr = remember(memory.createdAt) {
                        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                            .format(Date(memory.createdAt))
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Show assistant ID if available
                    if (memory.assistantId != null) {
                        Text(
                            text = memory.assistantId.take(12),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    HugeIcons.Delete02,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DiaryCard(
    memory: MemoryBankEntity,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateStr = remember(memory.dateGroup) {
                        memory.dateGroup ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(memory.createdAt))
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (memory.vectorStatus == "done") {
                        Icon(
                            HugeIcons.Database02,
                            contentDescription = "已向量化",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = memory.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    HugeIcons.Delete02,
                    contentDescription = "删除",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}