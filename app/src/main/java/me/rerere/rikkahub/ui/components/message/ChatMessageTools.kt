/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message

import android.util.Log
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.ui.ToolApprovalState
import me.rerere.ai.ui.UIMessagePart
import me.rerere.common.http.jsonObjectOrNull
import me.rerere.highlight.HighlightText
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.BubbleChatQuestion
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Clipboard
import me.rerere.hugeicons.stroke.ComputerTerminal01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Eraser
import me.rerere.hugeicons.stroke.FileAdd
import me.rerere.hugeicons.stroke.FileEdit
import me.rerere.hugeicons.stroke.FileView
import me.rerere.hugeicons.stroke.GlobalSearch
import me.rerere.hugeicons.stroke.MagicWand01
import me.rerere.hugeicons.stroke.QuillWrite01
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.Time02
import me.rerere.hugeicons.stroke.FileDownload
import me.rerere.hugeicons.stroke.Pause
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.Tools
import me.rerere.hugeicons.stroke.VolumeHigh
import me.rerere.hugeicons.stroke.Zip02
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.event.AppEvent
import me.rerere.rikkahub.data.event.AppEventBus
import me.rerere.rikkahub.data.ai.tools.ToolNaming
import me.rerere.rikkahub.data.ai.tools.WriteFilesCache
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.ui.components.richtext.DiffAddedColor
import me.rerere.rikkahub.ui.components.richtext.DiffRemovedColor
import me.rerere.rikkahub.ui.components.richtext.DiffView
import me.rerere.rikkahub.ui.components.richtext.HighlightCodeBlock
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.richtext.ZoomableAsyncImage
import me.rerere.rikkahub.ui.components.richtext.parseDiffStats
import me.rerere.rikkahub.ui.components.ui.ChainOfThoughtScope
import me.rerere.rikkahub.ui.components.ui.DotLoading
import me.rerere.rikkahub.ui.components.ui.Favicon
import me.rerere.rikkahub.ui.components.ui.FaviconRow
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.hooks.CustomTtsState
import me.rerere.rikkahub.ui.modifier.shimmer
import me.rerere.rikkahub.ui.theme.popupContainerColor
import me.rerere.tts.model.PlaybackStatus
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull
import me.rerere.rikkahub.utils.openUrl
import org.koin.compose.koinInject

private object ToolNames {
    const val MEMORY = "memory_tool"
    const val SEARCH_WEB = "search_web"
    const val SCRAPE_WEB = "scrape_web"
    const val GET_TIME_INFO = "get_time_info"
    const val CLIPBOARD = "clipboard_tool"
    const val TTS = "text_to_speech"
    const val ASK_USER = "ask_user"
    const val USE_SKILL = "use_skill"
    const val WRITE_FILES = "write_files"
    const val ZIP_FILES = "zip_files"  // backward compat
    // 工作区 (rootfs) 工具: 走 WorkspaceRepository, 与上面的 WRITE_FILES/ZIP_FILES 完全独立
    const val WORKSPACE_READ_FILE = "workspace_read_file"
    const val WORKSPACE_WRITE_FILE = "workspace_write_file"
    const val WORKSPACE_EDIT_FILE = "workspace_edit_file"
    const val WORKSPACE_SHELL = "workspace_shell"
}

private object MemoryActions {
    const val CREATE = "create"
    const val EDIT = "edit"
    const val DELETE = "delete"
}

private object ClipboardActions {
    const val READ = "read"
    const val WRITE = "write"
}

private fun getToolIcon(toolName: String, action: String?) = when (toolName) {
    ToolNames.MEMORY -> when (action) {
        MemoryActions.CREATE, MemoryActions.EDIT -> HugeIcons.QuillWrite01
        MemoryActions.DELETE -> HugeIcons.Eraser
        else -> HugeIcons.QuillWrite01
    }

    ToolNames.SEARCH_WEB -> HugeIcons.Search01
    ToolNames.SCRAPE_WEB -> HugeIcons.GlobalSearch
    ToolNames.GET_TIME_INFO -> HugeIcons.Time02
    ToolNames.CLIPBOARD -> HugeIcons.Clipboard
    ToolNames.TTS -> HugeIcons.VolumeHigh
    ToolNames.ASK_USER -> HugeIcons.BubbleChatQuestion
    ToolNames.USE_SKILL -> HugeIcons.MagicWand01
    ToolNames.ZIP_FILES, ToolNames.WRITE_FILES -> HugeIcons.Zip02
    ToolNames.WORKSPACE_READ_FILE -> HugeIcons.FileView
    ToolNames.WORKSPACE_WRITE_FILE -> HugeIcons.FileAdd
    ToolNames.WORKSPACE_EDIT_FILE -> HugeIcons.FileEdit
    ToolNames.WORKSPACE_SHELL -> HugeIcons.ComputerTerminal01
    else -> HugeIcons.Tools
}

private fun JsonElement?.getStringContent(key: String): String? =
    this?.jsonObjectOrNull?.get(key)?.jsonPrimitiveOrNull?.contentOrNull

@Composable
fun ChainOfThoughtScope.ChatMessageToolStep(
    tool: UIMessagePart.Tool,
    loading: Boolean = false,
    allParts: List<UIMessagePart> = emptyList(),
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
) {
    val isAskUser = tool.toolName == ToolNames.ASK_USER

    if (isAskUser) {
        AskUserToolStep(tool = tool, loading = loading, onToolAnswer = onToolAnswer)
        return
    }
    var showResult by remember { mutableStateOf(false) }
    var showDenyDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(true) }
    val eventBus: AppEventBus = koinInject()
    val scope = rememberCoroutineScope()
    val isPending = tool.approvalState is ToolApprovalState.Pending
    val isDenied = tool.approvalState is ToolApprovalState.Denied
    val arguments = tool.inputAsJson()
    val memoryAction = arguments.getStringContent("action")
    val content = if (tool.isExecuted) {
        runCatching {
            JsonInstant.parseToJsonElement(
                tool.output.filterIsInstance<UIMessagePart.Text>().joinToString("\n") { it.text }
            )
        }.getOrElse { JsonObject(emptyMap()) }
    } else {
        null
    }
    val images = tool.output.filterIsInstance<UIMessagePart.Image>()
    val audios = tool.output.filterIsInstance<UIMessagePart.Audio>()

    // 工具调用三态：进行中 / 完成 / 失败。
    // 失败按「只降不藏」处理——用错误色标出并把原文留在行里，不隐藏、不折叠掉。
    // 约定见 GenerationHandler：执行失败与用户拒绝都写成 {"error": "..."}。
    val errorMessage = remember(content) {
        (content as? JsonObject)?.get("error")?.let { node ->
            runCatching { node.jsonPrimitive.content }.getOrNull()
        }?.takeIf { it.isNotBlank() }
    }
    val isFailed = errorMessage != null
    // 失败原文可能带整段堆栈，行内只保留第一行，完整内容留在展开的结果里
    val errorFirstLine = remember(errorMessage) {
        errorMessage?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    val title = when (tool.toolName) {
        ToolNames.MEMORY -> when (memoryAction) {
            MemoryActions.CREATE -> stringResource(R.string.chat_message_tool_create_memory)
            MemoryActions.EDIT -> stringResource(R.string.chat_message_tool_edit_memory)
            MemoryActions.DELETE -> stringResource(R.string.chat_message_tool_delete_memory)
            else -> stringResource(R.string.chat_message_tool_call_generic, ToolNaming.toDisplayName(tool.toolName))
        }

        ToolNames.SEARCH_WEB -> stringResource(
            R.string.chat_message_tool_search_web,
            arguments.getStringContent("query") ?: ""
        )

        ToolNames.SCRAPE_WEB -> stringResource(R.string.chat_message_tool_scrape_web)
        ToolNames.GET_TIME_INFO -> stringResource(R.string.chat_message_tool_get_time)
        ToolNames.CLIPBOARD -> when (memoryAction) {
            ClipboardActions.READ -> stringResource(R.string.chat_message_tool_clipboard_read)
            ClipboardActions.WRITE -> stringResource(R.string.chat_message_tool_clipboard_write)
            else -> stringResource(R.string.chat_message_tool_call_generic, ToolNaming.toDisplayName(tool.toolName))
        }

        ToolNames.TTS -> {
            val preview = arguments.getStringContent("text")?.let { text ->
                if (text.length > 24) text.take(24) + "…" else text
            } ?: ""
            "Speaking: $preview"
        }

        ToolNames.USE_SKILL -> {
            val skillName = arguments.getStringContent("name") ?: ""
            val path = arguments.getStringContent("path")
            if (path != null) "Skill: $skillName / $path" else "Skill: $skillName"
        }

        ToolNames.ZIP_FILES, ToolNames.WRITE_FILES -> {
            val zipName = arguments.getStringContent("zip_name") ?: ""
            val fileCount = arguments.jsonObject.get("files")?.jsonArray?.size ?: 0
            val editCount = arguments.jsonObject.get("edits")?.jsonArray?.size ?: 0
            val isEdit = arguments.getStringContent("base_files") == "previous" && editCount > 0
            when {
                isEdit && zipName.isNotBlank() -> "✏️ $zipName ($editCount edits)"
                zipName.isNotBlank() -> "📦 $zipName"
                else -> "💾 ${fileCount} files"
            }
        }

        ToolNames.WORKSPACE_READ_FILE -> {
            val path = arguments.getStringContent("path")
            if (path != null) stringResource(R.string.tool_ui_read_file, path)
            else stringResource(R.string.tool_ui_read_file_default)
        }

        ToolNames.WORKSPACE_WRITE_FILE -> {
            val path = arguments.getStringContent("path")
            if (path != null) stringResource(R.string.tool_ui_write_file, path)
            else stringResource(R.string.tool_ui_write_file_default)
        }

        ToolNames.WORKSPACE_EDIT_FILE -> {
            val path = arguments.getStringContent("path")
            if (path != null) stringResource(R.string.tool_ui_edit_file, path)
            else stringResource(R.string.tool_ui_edit_file_default)
        }

        ToolNames.WORKSPACE_SHELL -> {
            val command = arguments.getStringContent("command")
            if (command.isNullOrBlank()) {
                stringResource(R.string.tool_ui_shell_default)
            } else {
                val preview = command.replace("\n", " ").trim()
                val truncated = if (preview.length > 40) preview.take(40) + "…" else preview
                stringResource(R.string.tool_ui_shell, truncated)
            }
        }

        else -> stringResource(R.string.chat_message_tool_call_generic, ToolNaming.toDisplayName(tool.toolName))
    }

    // 判断是否有额外内容需要显示
    val hasExtraContent = when (tool.toolName) {
        ToolNames.MEMORY -> memoryAction in listOf(MemoryActions.CREATE, MemoryActions.EDIT) &&
            content.getStringContent("content") != null

        ToolNames.SEARCH_WEB -> content.getStringContent("answer") != null ||
            (content?.jsonObject?.get("items")?.jsonArray?.isNotEmpty() == true)

        ToolNames.SCRAPE_WEB -> arguments.getStringContent("url") != null
        ToolNames.TTS -> arguments.getStringContent("text") != null
        ToolNames.ZIP_FILES, ToolNames.WRITE_FILES -> tool.isExecuted && content != null
        ToolNames.WORKSPACE_READ_FILE -> content.getStringContent("text") != null
        ToolNames.WORKSPACE_WRITE_FILE -> arguments.getStringContent("text") != null
        ToolNames.WORKSPACE_EDIT_FILE -> workspaceEditDiffOf(tool) != null
        ToolNames.WORKSPACE_SHELL -> content != null
        else -> false
    } || isDenied || isFailed || images.isNotEmpty() || audios.isNotEmpty()

    ControlledChainOfThoughtStep(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        icon = {
            when {
                loading -> DotLoading(size = 10.dp)
                // 失败：错误色图标，跟完成态在一眼之内可区分
                isFailed -> Icon(
                    imageVector = HugeIcons.Cancel01,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )

                else -> Icon(
                    imageVector = getToolIcon(tool.toolName, memoryAction),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        },
        label = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.shimmer(isLoading = loading),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // 失败原文留在行里（只降不藏），完整堆栈在展开的结果里
                if (errorFirstLine != null) {
                    Text(
                        text = errorFirstLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        extra = if (isPending && onToolApproval != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalIconButton(
                        onClick = { showDenyDialog = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Cancel01,
                            contentDescription = stringResource(R.string.chat_message_tool_deny),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { onToolApproval(tool.toolCallId, true, "") },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Tick01,
                            contentDescription = stringResource(R.string.chat_message_tool_approve),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            null
        },
        onClick = if (content != null || isPending || images.isNotEmpty() || audios.isNotEmpty() ||
            tool.toolName == ToolNames.WORKSPACE_EDIT_FILE ||
            tool.toolName == ToolNames.WORKSPACE_WRITE_FILE ||
            tool.toolName == ToolNames.WORKSPACE_READ_FILE
        ) {
            { showResult = true }
        } else {
            null
        },
        content = if (hasExtraContent) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tool.toolName == ToolNames.MEMORY &&
                        memoryAction in listOf(MemoryActions.CREATE, MemoryActions.EDIT)
                    ) {
                        content.getStringContent("content")?.let { memoryContent ->
                            Text(
                                text = memoryContent,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.shimmer(isLoading = loading),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (tool.toolName == ToolNames.SEARCH_WEB) {
                        content.getStringContent("answer")?.let { answer ->
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.shimmer(isLoading = loading),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val items = content?.jsonObject?.get("items")?.jsonArray ?: emptyList()
                        if (items.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                FaviconRow(
                                    urls = items.mapNotNull { it.getStringContent("url") },
                                    size = 18.dp,
                                )
                                Text(
                                    text = stringResource(R.string.chat_message_tool_search_results_count, items.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                    if (tool.toolName == ToolNames.SCRAPE_WEB) {
                        val url = arguments.getStringContent("url") ?: ""
                        Text(
                            text = url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                    if (tool.toolName == ToolNames.TTS) {
                        val text = arguments.getStringContent("text") ?: ""
                        TtsVoiceBar(
                            text = text,
                            onReplay = { scope.launch { eventBus.emit(AppEvent.Speak(text)) } }
                        )
                    }
                    if ((tool.toolName == ToolNames.ZIP_FILES || tool.toolName == ToolNames.WRITE_FILES) && tool.isExecuted && content != null) {
                        val context = LocalContext.current
                        val zipName = content.getStringContent("zip_name") ?: "files.zip"
                        val totalFiles = content.jsonObject.get("total_files")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                            ?: content.jsonObject.get("files")?.jsonArray?.size ?: 0

                        // 从工具执行结果中读取确切文件内容（不再猜数据源）
                        // write_files 工具现在在输出 JSON 中包含 files_content 字段
                        val fileContents = remember(content) {
                            val filesContentObj = content.jsonObject["files_content"]?.jsonObject
                            if (filesContentObj != null) {
                                filesContentObj.entries.mapNotNull { (name, element) ->
                                    runCatching {
                                        name to (element as kotlinx.serialization.json.JsonPrimitive).content
                                    }.getOrNull()
                                }
                            } else {
                                // 向后兼容: 如果工具结果中没有 files_content, 尝试从 arguments 提取 (full write 模式)
                                arguments.jsonObject.get("files")?.jsonArray?.mapNotNull { fileElement ->
                                    runCatching {
                                        val obj = fileElement.jsonObject
                                        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                                        val fileContent = obj["content"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                                        name to fileContent
                                    }.getOrNull()
                                } ?: emptyList()
                            }
                        }

                        val scope = rememberCoroutineScope()
                        val createZipLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/zip")
                        ) { uri ->
                            uri?.let { destination ->
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        context.contentResolver.openOutputStream(destination)?.use { stream ->
                                            val zipOut = java.util.zip.ZipOutputStream(stream)
                                            fileContents.forEach { (name, content) ->
                                                zipOut.putNextEntry(java.util.zip.ZipEntry(name))
                                                zipOut.write(content.toByteArray(Charsets.UTF_8))
                                                zipOut.closeEntry()
                                            }
                                            zipOut.finish()
                                        }
                                    }.onFailure {
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Export failed: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "$totalFiles files",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(
                                onClick = { createZipLauncher.launch(zipName) },
                                enabled = fileContents.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = HugeIcons.FileDownload,
                                    contentDescription = "Download ZIP",
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Download",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    if (tool.toolName == ToolNames.WORKSPACE_READ_FILE) {
                        val text = content.getStringContent("text")
                        if (text != null) {
                            WorkspaceFileContentSummary(
                                text = text,
                                path = arguments.getStringContent("path"),
                                loading = loading,
                            )
                        }
                    }
                    if (tool.toolName == ToolNames.WORKSPACE_WRITE_FILE) {
                        val text = arguments.getStringContent("text")
                        if (text != null) {
                            WorkspaceFileContentSummary(
                                text = text,
                                path = arguments.getStringContent("path"),
                                loading = loading,
                            )
                        }
                    }
                    if (tool.toolName == ToolNames.WORKSPACE_EDIT_FILE) {
                        val diff = workspaceEditDiffOf(tool)
                        if (diff != null) {
                            val stats = remember(diff) { parseDiffStats(diff) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "+${stats.additions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiffAddedColor,
                                )
                                Text(
                                    text = "-${stats.deletions}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiffRemovedColor,
                                )
                            }
                            DiffView(
                                diff = diff,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 10,
                                showFileHeader = false,
                            )
                        }
                    }
                    if (tool.toolName == ToolNames.WORKSPACE_SHELL) {
                        WorkspaceShellSummary(
                            arguments = arguments,
                            content = content,
                            loading = loading,
                        )
                    }
                    if (images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.wrapContentWidth(),
                        ) {
                            items(images) { image ->
                                ZoomableAsyncImage(
                                    model = image.url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(64.dp)
                                        .wrapContentWidth(),
                                )
                            }
                        }
                    }
                    if (audios.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            audios.fastForEach { audio ->
                                AudioPlayerBubble(url = audio.url)
                            }
                        }
                    }
                    if (isDenied) {
                        val reason = (tool.approvalState as ToolApprovalState.Denied).reason
                        Text(
                            text = stringResource(R.string.chat_message_tool_denied) +
                                if (reason.isNotBlank()) ": $reason" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        } else {
            null
        },
    )

    if (showDenyDialog && onToolApproval != null) {
        ToolDenyReasonDialog(
            onDismiss = { showDenyDialog = false },
            onConfirm = { reason ->
                showDenyDialog = false
                onToolApproval(tool.toolCallId, false, reason)
            }
        )
    }

    if (showResult) {
        ToolCallPreviewSheet(
            toolName = ToolNaming.toDisplayName(tool.toolName),
            arguments = arguments,
            content = content,
            output = tool.output,
            onDismissRequest = { showResult = false }
        )
    }
}

@Composable
private fun ToolCallPreviewSheet(
    toolName: String,
    arguments: JsonElement,
    content: JsonElement?,
    output: List<UIMessagePart>,
    onDismissRequest: () -> Unit = {}
) {
    val memoryRepo: MemoryRepository = koinInject()
    val scope = rememberCoroutineScope()

    val memoryAction = arguments.getStringContent("action")
    val isMemoryOperation = toolName == ToolNames.MEMORY &&
        memoryAction in listOf(MemoryActions.CREATE, MemoryActions.EDIT)
    val memoryId = (content as? JsonObject)?.get("id")?.jsonPrimitiveOrNull?.intOrNull

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismissRequest,
        containerColor = popupContainerColor(BottomSheetDefaults.ContainerColor),
        content = {
            when {
                // workspace_write_file / workspace_edit_file 即使未执行 (content == null)
                // 也能基于入参预览内容/diff, 先于 null 兜底分支处理
                toolName == ToolNames.WORKSPACE_WRITE_FILE -> WorkspaceFileContentPreview(
                    path = arguments.getStringContent("path"),
                    code = arguments.getStringContent("text"),
                )

                toolName == ToolNames.WORKSPACE_EDIT_FILE -> WorkspaceEditFilePreview(
                    arguments = arguments,
                    toolName = toolName,
                    output = output,
                    memoryRepo = memoryRepo,
                    scope = scope,
                    onDismissRequest = onDismissRequest,
                )

                content == null -> GenericToolPreview(
                    toolName = toolName,
                    arguments = arguments,
                    output = emptyList(),
                    isMemoryOperation = false,
                    memoryId = null,
                    memoryRepo = memoryRepo,
                    scope = scope,
                    onDismissRequest = onDismissRequest
                )

                toolName == ToolNames.SEARCH_WEB -> SearchWebPreview(
                    arguments = arguments,
                    content = content,
                )

                toolName == ToolNames.SCRAPE_WEB -> ScrapeWebPreview(content = content)

                toolName == ToolNames.WORKSPACE_READ_FILE -> WorkspaceFileContentPreview(
                    path = arguments.getStringContent("path"),
                    code = content.getStringContent("text"),
                )

                toolName == ToolNames.WORKSPACE_SHELL -> WorkspaceShellPreview(
                    arguments = arguments,
                    content = content,
                )

                else -> GenericToolPreview(
                    toolName = toolName,
                    arguments = arguments,
                    output = output,
                    isMemoryOperation = isMemoryOperation,
                    memoryId = memoryId,
                    memoryRepo = memoryRepo,
                    scope = scope,
                    onDismissRequest = onDismissRequest
                )
            }
        },
    )
}

@Composable
private fun SearchWebPreview(
    arguments: JsonElement,
    content: JsonElement,
) {
    val context = LocalContext.current
    val items = content.jsonObject["items"]?.jsonArray ?: emptyList()
    val answer = content.getStringContent("answer")
    val query = arguments.getStringContent("query") ?: ""

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(stringResource(R.string.chat_message_tool_search_prefix, query))
        }

        if (answer != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    MarkdownBlock(
                        content = answer,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (items.isNotEmpty()) {
            items(items) { item ->
                val url = item.getStringContent("url") ?: return@items
                val title = item.getStringContent("title") ?: return@items
                val text = item.getStringContent("text") ?: return@items

                Card(
                    onClick = { context.openUrl(url) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Favicon(
                            url = url,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(text = title, maxLines = 1)
                            Text(
                                text = text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = url,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                HighlightText(
                    code = JsonInstantPretty.encodeToString(content),
                    language = "json",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ScrapeWebPreview(content: JsonElement) {
    val urls = content.jsonObject["urls"]?.jsonArray ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(
                    R.string.chat_message_tool_scrape_prefix,
                    urls.joinToString(", ") { it.getStringContent("url") ?: "" }
                )
            )
        }

        items(urls) { url ->
            val urlObject = url.jsonObject
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = urlObject["url"]?.jsonPrimitive?.content ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                Card {
                    MarkdownBlock(
                        content = urlObject["content"]?.jsonPrimitive?.content ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericToolPreview(
    toolName: String,
    arguments: JsonElement,
    output: List<UIMessagePart>,
    isMemoryOperation: Boolean,
    memoryId: Int?,
    memoryRepo: MemoryRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismissRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_message_tool_call_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            if (isMemoryOperation && memoryId != null) {
                IconButton(
                    onClick = {
                        scope.launch {
                            memoryRepo.deleteMemory(memoryId)
                            onDismissRequest()
                        }
                    }
                ) {
                    Icon(
                        imageVector = HugeIcons.Delete01,
                        contentDescription = "Delete memory"
                    )
                }
            }
        }
        FormItem(
            label = {
                Text(stringResource(R.string.chat_message_tool_call_label, toolName))
            }
        ) {
            HighlightCodeBlock(
                code = JsonInstantPretty.encodeToString(arguments),
                language = "json",
                style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)
            )
        }
        if (output.isNotEmpty()) {
            FormItem(
                label = {
                    Text(stringResource(R.string.chat_message_tool_call_result))
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    output.fastForEach { part ->
                        when (part) {
                            is UIMessagePart.Text -> HighlightCodeBlock(
                                code = runCatching {
                                    JsonInstantPretty.encodeToString(
                                        JsonInstant.parseToJsonElement(part.text)
                                    )
                                }.getOrElse { part.text },
                                language = "json",
                                style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)
                            )

                            is UIMessagePart.Image -> ZoomableAsyncImage(
                                model = part.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            is UIMessagePart.Audio -> AudioPlayerBubble(url = part.url)

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChainOfThoughtScope.AskUserToolStep(
    tool: UIMessagePart.Tool,
    loading: Boolean,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)?,
) {
    val isPending = tool.approvalState is ToolApprovalState.Pending
    val isAnswered = tool.approvalState is ToolApprovalState.Answered
    val arguments = tool.inputAsJson()

    // Parse questions from arguments
    val questions = remember(arguments) {
        runCatching {
            arguments.jsonObject["questions"]?.jsonArray?.map { q ->
                val obj = q.jsonObject
                AskUserQuestion(
                    id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    question = obj["question"]?.jsonPrimitive?.contentOrNull ?: "",
                    options = obj["options"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                    selectionType = obj["selection_type"]?.jsonPrimitive?.contentOrNull ?: "text"
                )
            } ?: emptyList()
        }.getOrElse { emptyList() }
    }

    // Track answers for text/single questions
    val answers = remember { mutableStateMapOf<String, String>() }
    // Track selected options for multi questions
    val multiAnswers = remember { mutableStateMapOf<String, Set<String>>() }

    val firstQuestion = questions.firstOrNull()?.question ?: "..."

    var expanded by remember { mutableStateOf(true) }

    ControlledChainOfThoughtStep(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        icon = {
            if (loading) {
                DotLoading(size = 10.dp)
            } else {
                Icon(
                    imageVector = HugeIcons.BubbleChatQuestion,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                )
            }
        },
        label = {
            Text(
                text = if (questions.size <= 1) firstQuestion else stringResource(
                    R.string.chat_message_tool_ask_questions,
                    questions.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.shimmer(isLoading = loading),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                questions.forEach { q ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (isPending && onToolAnswer != null) {
                            when (q.selectionType) {
                                "single" -> {
                                    // Single select: chips only, no text input
                                    if (q.options.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            q.options.forEach { option ->
                                                FilterChip(
                                                    selected = answers[q.id] == option,
                                                    onClick = { answers[q.id] = option },
                                                    label = {
                                                        Text(
                                                            text = option,
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                                "multi" -> {
                                    // Multi select: chips only, multiple can be selected
                                    if (q.options.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            q.options.forEach { option ->
                                                val selectedSet = multiAnswers[q.id] ?: emptySet()
                                                FilterChip(
                                                    selected = selectedSet.contains(option),
                                                    onClick = {
                                                        val current = selectedSet.toMutableSet()
                                                        if (current.contains(option)) current.remove(option)
                                                        else current.add(option)
                                                        multiAnswers[q.id] = current
                                                    },
                                                    label = {
                                                        Text(
                                                            text = option,
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Text (default): optional option chips + free text input
                                    if (q.options.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            q.options.forEach { option ->
                                                FilterChip(
                                                    selected = answers[q.id] == option,
                                                    onClick = { answers[q.id] = option },
                                                    label = {
                                                        Text(
                                                            text = option,
                                                            style = MaterialTheme.typography.labelSmall,
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    // Free text input
                                    OutlinedTextField(
                                        value = answers[q.id] ?: "",
                                        onValueChange = { answers[q.id] = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        singleLine = false,
                                        minLines = 1,
                                        maxLines = 3,
                                    )
                                }
                            }
                        } else if (isAnswered) {
                            // Show the user's answer
                            val answeredState = tool.approvalState as ToolApprovalState.Answered
                            val answerJson = runCatching {
                                JsonInstant.parseToJsonElement(answeredState.answer)
                            }.getOrNull()
                            val answerText = answerJson?.jsonObject?.get("answers")
                                ?.jsonObject?.get(q.id)?.jsonPrimitive?.contentOrNull
                                ?: answeredState.answer
                            Text(
                                text = answerText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Submit button
                if (isPending && onToolAnswer != null) {
                    FilledTonalButton(
                        onClick = {
                            val answerPayload = buildJsonObject {
                                put("answers", buildJsonObject {
                                    questions.forEach { q ->
                                        when (q.selectionType) {
                                            "multi" -> put(q.id, JsonPrimitive(multiAnswers[q.id]?.joinToString(", ") ?: ""))
                                            else -> put(q.id, JsonPrimitive(answers[q.id] ?: ""))
                                        }
                                    }
                                })
                            }
                            onToolAnswer(tool.toolCallId, answerPayload.toString())
                        },
                        enabled = questions.all { q ->
                            when (q.selectionType) {
                                "multi" -> !multiAnswers[q.id].isNullOrEmpty()
                                else -> !answers[q.id].isNullOrBlank()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Tick01,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.chat_message_tool_submit),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        },
    )
}

private data class AskUserQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val selectionType: String = "text", // "text" | "single" | "multi"
)

@Composable
private fun TtsVoiceBar(
    text: String,
    onReplay: () -> Unit
) {
    val ttsState = LocalTTSState.current
    val playbackState by ttsState.playbackState.collectAsState()
    val isSpeaking by ttsState.isSpeaking.collectAsState()

    val progress = if (playbackState.durationMs > 0) {
        playbackState.positionMs.toFloat() / playbackState.durationMs
    } else 0f

    val waveformBars = remember(text) {
        val rnd = java.util.Random(text.hashCode().toLong())
        List(24) { 0.2f + rnd.nextFloat() * 0.8f }
    }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 3.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    when (playbackState.status) {
                        PlaybackStatus.Playing -> ttsState.pause()
                        else -> if (isSpeaking) ttsState.resume() else onReplay()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playbackState.status == PlaybackStatus.Playing) HugeIcons.Pause else HugeIcons.Play,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Waveform
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            val barCount = waveformBars.size
            val totalWidth = size.width
            val barWidth = 2f
            val gap = (totalWidth - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
            val playedBarCount = (progress * barCount).toInt()

            waveformBars.forEachIndexed { index, barRatio ->
                val barHeight = size.height * barRatio.coerceIn(0.2f, 1f)
                val x = index * (barWidth + gap)
                val y = (size.height - barHeight) / 2f
                drawRoundRect(
                    color = if (index < playedBarCount) activeColor else inactiveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Duration
        val remainingSec = if (isSpeaking && playbackState.durationMs > 0) {
            ((playbackState.durationMs - playbackState.positionMs) / 1000).toInt().coerceAtLeast(0)
        } else {
            text.length / 5
        }
        Text(
            text = String.format("%d:%02d", remainingSec / 60, remainingSec % 60),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 10.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ToolDenyReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.chat_message_tool_deny_dialog_title))
        },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.chat_message_tool_deny_dialog_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }) {
                Text(stringResource(R.string.chat_message_tool_deny))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// ==================== 工作区 (workspace_*) 工具 UI ====================
// 以下组件服务 workspace_read_file / workspace_write_file / workspace_edit_file /
// workspace_shell 四个走 WorkspaceRepository (rootfs) 的工具, 与上面的 write_files/
// zip_files (ZipFilesTool) 完全独立, 两套并存互不影响。

private const val WORKSPACE_FILE_SUMMARY_MAX_LINES = 10
private const val WORKSPACE_SHELL_SUMMARY_MAX_LINES = 8

/**
 * 从 workspace_edit_file 输出部件的 metadata 读取全文件 diff。
 * workspace_edit_file 执行后会把 unified diff 放在输出 Text part 的
 * metadata["diff"] 里 (见 WorkspaceTools.kt), 不会随工具结果发给 API。
 */
private fun workspaceEditDiffOf(tool: UIMessagePart.Tool): String? =
    tool.output.firstOrNull()?.metadata?.get("diff")?.jsonPrimitiveOrNull?.contentOrNull

/** 由文件扩展名推断语法高亮语言 */
private fun languageOf(path: String?): String = when (
    path?.substringAfterLast('.', "")?.lowercase().orEmpty()
) {
    "kt", "kts" -> "kotlin"
    "java" -> "java"
    "js", "mjs", "cjs" -> "javascript"
    "ts" -> "typescript"
    "tsx" -> "tsx"
    "jsx" -> "jsx"
    "py" -> "python"
    "rb" -> "ruby"
    "go" -> "go"
    "rs" -> "rust"
    "c", "h" -> "c"
    "cpp", "cc", "cxx", "hpp", "hxx" -> "cpp"
    "cs" -> "csharp"
    "swift" -> "swift"
    "php" -> "php"
    "sh", "bash", "zsh" -> "bash"
    "json" -> "json"
    "xml" -> "xml"
    "html", "htm" -> "html"
    "css" -> "css"
    "scss" -> "scss"
    "yaml", "yml" -> "yaml"
    "toml" -> "toml"
    "md", "markdown" -> "markdown"
    "sql" -> "sql"
    "gradle" -> "groovy"
    else -> "plaintext"
}

/** 内联摘要: 按扩展名语法高亮展示文件内容首部若干行 */
@Composable
private fun WorkspaceFileContentSummary(text: String, path: String?, loading: Boolean) {
    val preview = remember(text) {
        text.lineSequence().take(WORKSPACE_FILE_SUMMARY_MAX_LINES).joinToString("\n")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shimmer(isLoading = loading),
    ) {
        HighlightText(
            code = preview,
            language = languageOf(path),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = WORKSPACE_FILE_SUMMARY_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** BottomSheet 详情: 文件路径 + 按扩展名语法高亮的完整内容 */
@Composable
private fun WorkspaceFileContentPreview(path: String?, code: String?) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = path ?: stringResource(R.string.tool_ui_file),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (code != null) {
            HighlightCodeBlock(
                code = code,
                language = languageOf(path),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** workspace_edit_file 详情: 路径 + 增删统计 + 完整 diff view; 无 diff 时回退到通用预览 */
@Composable
private fun WorkspaceEditFilePreview(
    arguments: JsonElement,
    toolName: String,
    output: List<UIMessagePart>,
    memoryRepo: MemoryRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismissRequest: () -> Unit,
) {
    val diff = output.firstOrNull()?.metadata?.get("diff")?.jsonPrimitiveOrNull?.contentOrNull
    if (diff == null) {
        GenericToolPreview(
            toolName = toolName,
            arguments = arguments,
            output = output,
            isMemoryOperation = false,
            memoryId = null,
            memoryRepo = memoryRepo,
            scope = scope,
            onDismissRequest = onDismissRequest,
        )
        return
    }
    val stats = remember(diff) { parseDiffStats(diff) }
    val path = arguments.getStringContent("path")
    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = path ?: toolName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+${stats.additions}",
                style = MaterialTheme.typography.labelMedium,
                color = DiffAddedColor,
            )
            Text(
                text = "-${stats.deletions}",
                style = MaterialTheme.typography.labelMedium,
                color = DiffRemovedColor,
            )
        }
        DiffView(
            diff = diff,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** workspace_shell 内联摘要: 退出状态 + stdout/stderr 首部若干行 */
@Composable
private fun WorkspaceShellSummary(
    arguments: JsonElement,
    content: JsonElement?,
    loading: Boolean,
) {
    if (content == null) return
    val combined = remember(content) {
        listOf(content.getStringContent("stdout"), content.getStringContent("stderr"))
            .filterNot { it.isNullOrBlank() }
            .joinToString("\n")
            .trim()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WorkspaceShellExitStatus(content, MaterialTheme.typography.labelSmall)
        if (combined.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .shimmer(isLoading = loading),
            ) {
                Text(
                    text = combined.lineSequence().take(WORKSPACE_SHELL_SUMMARY_MAX_LINES).joinToString("\n"),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = WORKSPACE_SHELL_SUMMARY_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** workspace_shell BottomSheet 详情: 命令 + cwd + stdout/stderr */
@Composable
private fun WorkspaceShellPreview(
    arguments: JsonElement,
    content: JsonElement?,
) {
    if (content == null) return
    val command = arguments.getStringContent("command").orEmpty()
    val cwd = arguments.getStringContent("cwd")
    val stdout = content.getStringContent("stdout").orEmpty()
    val stderr = content.getStringContent("stderr").orEmpty()
    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.tool_ui_shell_default),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            WorkspaceShellExitStatus(content, MaterialTheme.typography.labelMedium)
        }
        HighlightCodeBlock(
            code = if (cwd.isNullOrBlank()) command else "# cwd: $cwd\n$command",
            language = "bash",
            modifier = Modifier.fillMaxWidth(),
        )
        if (stdout.isNotEmpty()) {
            Text(text = "stdout", style = MaterialTheme.typography.labelMedium)
            HighlightCodeBlock(
                code = stdout,
                language = "plaintext",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (stderr.isNotEmpty()) {
            Text(
                text = "stderr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
            HighlightCodeBlock(
                code = stderr,
                language = "plaintext",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Shell 退出状态文本: exit code 为 0 显示绿色, 超时或非零显示错误色 */
@Composable
private fun WorkspaceShellExitStatus(content: JsonElement, style: TextStyle) {
    val exitCode = content.jsonObjectOrNull?.get("exitCode")?.jsonPrimitiveOrNull?.intOrNull
    val timedOut = content.jsonObjectOrNull?.get("timedOut")?.jsonPrimitiveOrNull?.booleanOrNull ?: false
    val ok = !timedOut && exitCode == 0
    Text(
        text = when {
            timedOut -> stringResource(R.string.tool_ui_shell_timeout)
            else -> stringResource(R.string.tool_ui_shell_exit, exitCode?.toString() ?: "?")
        },
        style = style,
        color = if (ok) DiffAddedColor else MaterialTheme.colorScheme.error,
    )
}
