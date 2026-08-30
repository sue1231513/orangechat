/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.plugin.ui
 
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import me.rerere.rikkahub.ui.theme.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Delete02
import me.rerere.hugeicons.stroke.Search01
import me.rerere.rikkahub.plugin.data.PluginDataStore
import me.rerere.rikkahub.plugin.manager.PluginManager
import me.rerere.rikkahub.plugin.model.PluginUIAction
import me.rerere.rikkahub.plugin.model.PluginUIComponent
import me.rerere.rikkahub.plugin.model.PluginUIDeclaration
import me.rerere.rikkahub.plugin.model.PluginUIQuery
import me.rerere.document.PdfParser
import org.json.JSONObject
import java.io.File
import java.util.Base64
 
private const val TAG = "PluginUIDeclarative"
 
private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }
 
/**
 * 插件声明式 UI 页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginUIDeclarativePage(
    pluginId: String,
    pluginManager: PluginManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = remember(pluginId) { PluginDataStore(context, pluginId) }
    val loadedPlugin = remember(pluginId) { pluginManager.getPlugin(pluginId) }
    val uiDeclaration = remember(pluginId) { loadedPlugin?.manifest?.ui } ?: run {
        Scaffold(
            containerColor = settingsScaffoldContainerColor(),
            topBar = {
                TopAppBar(
                    title = { Text("Error") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(HugeIcons.ArrowLeft01, contentDescription = "Back")
                        }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
                Text("No UI declaration found for this plugin")
            }
        }
        return
    }
 
    var queryResults by remember(pluginId) { mutableStateOf<Map<String, List<JsonObject>>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterValue by remember { mutableStateOf("all") }
    var openDialogForm by remember { mutableStateOf<PluginUIComponent?>(null) }
    var formValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var confirmAction by remember { mutableStateOf<Pair<PluginUIAction, Map<String, String>>?>(null) }
 
    // ── 图片选择器 ──────────────────────────────────────────────
    var imagePickerTarget by remember { mutableStateOf<String?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    inputStream?.use { stream ->
                        val bytes = stream.readBytes()
                        val fileName = "img_${System.currentTimeMillis()}.png"
                        val file = File(dataStore.getDataDir(), fileName)
                        file.writeBytes(bytes)
                        imagePickerTarget?.let { target ->
                            formValues = formValues.toMutableMap().apply {
                                put(target, file.absolutePath)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read image: uri=$it", e)
                }
            }
            imagePickerTarget = null
        }
    )
 
    // ── 文件选择器（支持 TXT 和 PDF）─────────────────────────
    var filePickerTarget by remember { mutableStateOf<String?>(null) }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    // 获取文件名
                    val displayName = context.contentResolver
                        .query(it, null, null, null, null)
                        ?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIdx >= 0) cursor.getString(nameIdx) else null
                        } ?: "已选择文件"

                    val content: String = if (displayName.endsWith(".pdf", ignoreCase = true)) {
                        // PDF 文件：保存到临时文件后用 PdfParser 提取文本
                        val tempFile = File(context.cacheDir, "plugin_pick_${System.currentTimeMillis()}.pdf")
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            tempFile.outputStream().use { out -> stream.copyTo(out) }
                        }
                        val text = try {
                            PdfParser.parserPdf(tempFile)
                        } finally {
                            tempFile.delete()
                        }
                        text
                    } else {
                        // 普通文本文件
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        } ?: ""
                    }

                    filePickerTarget?.let { target ->
                        formValues = formValues.toMutableMap().apply {
                            put(target, content)
                            put("${target}__filename", displayName)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read file: uri=$it", e)
                }
            }
            filePickerTarget = null
        }
    )
 
    // ── 查询执行 ───────────────────────────────────────────────
    suspend fun executeQuery(name: String, query: PluginUIQuery): List<JsonObject> {
        return when (query.type) {
            "dataStore_list" -> {
                val prefix = query.params["prefix"]?.jsonPrimitive?.contentOrNull ?: ""
                dataStore.listData()
                    .filter { it.startsWith(prefix) }
                    .mapNotNull { key ->
                        val value = dataStore.getData(key)
                        value?.let {
                            try {
                                val obj = JSONObject(it)
                                val jsonMap = mutableMapOf<String, JsonElement>()
                                jsonMap["_key"] = JsonPrimitive(key)
                                obj.keys().forEach { k ->
                                    val v = obj.get(k)
                                    jsonMap[k] = when (v) {
                                        is Int -> JsonPrimitive(v)
                                        is Long -> JsonPrimitive(v)
                                        is Double -> JsonPrimitive(v)
                                        is Boolean -> JsonPrimitive(v)
                                        else -> JsonPrimitive(v.toString())
                                    }
                                }
                                JsonObject(jsonMap)
                            } catch (e: Exception) {
                                JsonObject(mapOf("_key" to JsonPrimitive(key), "value" to JsonPrimitive(it)))
                            }
                        }
                    }
            }
            "dataStore_search" -> {
                val prefix = query.params["prefix"]?.jsonPrimitive?.contentOrNull ?: ""
                val keyword = searchQuery.lowercase()
                val searchFields = query.params["searchFields"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                dataStore.listData()
                    .filter { it.startsWith(prefix) }
                    .mapNotNull { key ->
                        val value = dataStore.getData(key)
                        value?.let {
                            try {
                                val obj = JSONObject(it)
                                val jsonMap = mutableMapOf<String, JsonElement>()
                                jsonMap["_key"] = JsonPrimitive(key)
                                obj.keys().forEach { k ->
                                    val v = obj.get(k)
                                    jsonMap[k] = when (v) {
                                        is Int -> JsonPrimitive(v)
                                        is Long -> JsonPrimitive(v)
                                        is Double -> JsonPrimitive(v)
                                        is Boolean -> JsonPrimitive(v)
                                        else -> JsonPrimitive(v.toString())
                                    }
                                }
                                JsonObject(jsonMap)
                            } catch (e: Exception) {
                                JsonObject(mapOf("_key" to JsonPrimitive(key), "value" to JsonPrimitive(it)))
                            }
                        }
                    }
                    .filter { item ->
                        if (keyword.isBlank()) true
                        else searchFields.any { field ->
                            item[field]?.jsonPrimitive?.contentOrNull?.lowercase()?.contains(keyword) == true
                        }
                    }
            }
            else -> emptyList()
        }
    }
 
    suspend fun refreshAllQueries() {
        val results = mutableMapOf<String, List<JsonObject>>()
        uiDeclaration.queries.forEach { (name, query) ->
            results[name] = executeQuery(name, query)
        }
        queryResults = results.toMap()
    }
 
    // ── Action 执行 ────────────────────────────────────────────
    suspend fun executeAction(action: PluginUIAction, fieldValues: Map<String, String> = emptyMap()) {
        when (action.type) {
            "dataStore_set" -> {
                val keyTemplate = action.params["key"]?.jsonPrimitive?.contentOrNull ?: ""
                val valueTemplate = action.params["value"]?.jsonPrimitive?.contentOrNull ?: ""
                val key = resolveTemplate(keyTemplate, fieldValues)
                val value = resolveTemplate(valueTemplate, fieldValues)
                dataStore.setData(key, value)
            }
            "dataStore_delete" -> {
                val keyTemplate = action.params["key"]?.jsonPrimitive?.contentOrNull ?: ""
                val key = resolveTemplate(keyTemplate, fieldValues)
                val data = dataStore.getData(key)
                dataStore.deleteData(key)
                try {
                    data?.let {
                        val obj = JSONObject(it)
                        if (obj.has("imageFile")) {
                            val fileName = obj.getString("imageFile")
                            File(dataStore.getDataDir(), fileName).delete()
                        }
                    }
                } catch (_: Exception) {}
            }
            "file_delete" -> {
                val fileNameTemplate = action.params["fileName"]?.jsonPrimitive?.contentOrNull ?: ""
                val fileName = resolveTemplate(fileNameTemplate, fieldValues)
                File(dataStore.getDataDir(), fileName).delete()
            }
            "call_js_function" -> {
                // 调用插件导出的 JS 函数，params 默认为整个表单 JSON (${form})
                val functionName = action.params["function"]?.jsonPrimitive?.contentOrNull
                if (functionName.isNullOrBlank()) {
                    Log.e(TAG, "call_js_function: missing 'function' param, action.params=${action.params}")
                    return
                }
                val paramsTemplate = action.params["params"]?.jsonPrimitive?.contentOrNull ?: "\${form}"
                val resolvedParams = resolveTemplate(paramsTemplate, fieldValues)
                try {
                    val paramsJson = lenientJson.parseToJsonElement(resolvedParams)
                    val result = pluginManager.callTool(pluginId, functionName, paramsJson)
                    result.onSuccess { ret ->
                        Log.d(TAG, "call_js_function ok: function=$functionName, result=$ret")
                    }
                    result.onFailure { e ->
                        Log.e(TAG, "call_js_function failed: function=$functionName, error=${e.message}", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "call_js_function exception: function=$functionName, resolvedParams=$resolvedParams", e)
                }
            }
        }
 
        when (action.onSuccess) {
            "refresh" -> refreshAllQueries()
            "refresh_queries" -> {
                val results: MutableMap<String, List<JsonObject>> = queryResults.toMutableMap()
                action.refreshQueries.forEach { queryName ->
                    uiDeclaration.queries[queryName]?.let { query ->
                        results[queryName] = executeQuery(queryName, query)
                    }
                }
                queryResults = results.toMap()
            }
            "close_dialog" -> {
                openDialogForm = null
                refreshAllQueries()
            }
            "navigate_back" -> onNavigateBack()
            "none" -> {}
            else -> refreshAllQueries()
        }
    }
 
    LaunchedEffect(pluginId) { refreshAllQueries() }
 
    Scaffold(
        containerColor = settingsScaffoldContainerColor(),
        topBar = {
            TopAppBar(
                title = { Text(uiDeclaration.title.ifEmpty { "管理" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(HugeIcons.ArrowLeft01, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            uiDeclaration.components.forEach { component ->
                RenderComponent(
                    component = component,
                    queryResults = queryResults,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    filterValue = filterValue,
                    onFilterChange = { filterValue = it },
                    onAction = { action, fv ->
                        if (action.confirmDialog != null) {
                            confirmAction = action to fv
                        } else {
                            scope.launch { executeAction(action, fv) }
                        }
                    },
                    onOpenForm = { formComponent ->
                        openDialogForm = formComponent
                        formValues = emptyMap()
                    },
                    dataStore = dataStore,
                    onPickImage = { fieldName ->
                        imagePickerTarget = fieldName
                        imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onPickFile = { fieldName ->
                        filePickerTarget = fieldName
                        fileLauncher.launch("text/*,application/pdf")
                    }
                )
            }
        }
    }
 
    // ── Dialog Form ────────────────────────────────────────────
    openDialogForm?.let { formComponent ->
        DialogFormSheet(
            component = formComponent,
            formValues = formValues,
            onFormValueChange = { name, value ->
                formValues = formValues.toMutableMap().apply { put(name, value) }
            },
            onDismiss = { openDialogForm = null },
            onSubmit = { fields ->
                val actionName = formComponent.props["submitAction"]?.jsonPrimitive?.contentOrNull
                    ?: return@DialogFormSheet
                uiDeclaration.actions[actionName]?.let { action ->
                    scope.launch {
                        executeAction(action, fields)
                        if (action.onSuccess == "refresh" || action.onSuccess.isEmpty()) {
                            openDialogForm = null
                        }
                    }
                }
            },
            dataStore = dataStore,
            onPickImage = { fieldName ->
                imagePickerTarget = fieldName
                imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickFile = { fieldName ->
                filePickerTarget = fieldName
                fileLauncher.launch("text/*,application/pdf")
            }
        )
    }
 
    // ── Confirm Dialog ─────────────────────────────────────────
    confirmAction?.let { (action, fv) ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(action.confirmDialog?.title ?: "确认") },
            text = { Text(action.confirmDialog?.message ?: "确定要执行此操作吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    scope.launch { executeAction(action, fv) }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("取消") }
            }
        )
    }
}
 
/**
 * 解析模板字符串
 *  - \${field.xxx} → fieldValues["xxx"]
 *  - \${form}      → 所有 fieldValues 的 JSON（不含内部 __filename 辅助字段）
 */
private fun resolveTemplate(template: String, fieldValues: Map<String, String>): String {
    if (!template.contains("\${")) return template
    var result = template
 
    fieldValues.forEach { (key, value) ->
        result = result.replace("\${field.$key}", value)
    }
 
    if (result.contains("\${form}")) {
        // 过滤掉以 "__filename" 结尾的辅助字段，不暴露给插件 JS
        val filtered = fieldValues.filterKeys { !it.endsWith("__filename") }
        val formParts = StringBuilder("{")
        var first = true
        filtered.forEach { (k, v) ->
            if (!first) formParts.append(",")
            first = false
            // JsonPrimitive.toString() 会正确转义引号、换行符等
            formParts.append(JsonPrimitive(k).toString())
            formParts.append(":")
            formParts.append(JsonPrimitive(v).toString())
        }
        formParts.append("}")
        result = result.replace("\${form}", formParts.toString())
    }
 
    return result
}
 
private fun JsonObject.stringField(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.intField(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
 
// ── 组件渲染入口 ───────────────────────────────────────────────
 
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderComponent(
    component: PluginUIComponent,
    queryResults: Map<String, List<JsonObject>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterValue: String,
    onFilterChange: (String) -> Unit,
    onAction: (PluginUIAction, Map<String, String>) -> Unit,
    onOpenForm: (PluginUIComponent) -> Unit,
    dataStore: PluginDataStore,
    onPickImage: (String) -> Unit,
    onPickFile: (String) -> Unit
) {
    when (component.type) {
        "stats" -> RenderStats(component, queryResults)
        "search_bar" -> RenderSearchBar(component, searchQuery, onSearchChange)
        "filter_bar" -> RenderFilterBar(component, queryResults, filterValue, onFilterChange)
        "card_grid" -> RenderCardGrid(component, queryResults, filterValue, onAction, onOpenForm)
        "card_list" -> RenderCardList(component, queryResults, filterValue, onAction, onOpenForm)
        "button_row" -> RenderButtonRow(component, onAction, onOpenForm)
        "dialog_form" -> RenderDialogFormTrigger(component, onOpenForm)
        "text" -> RenderText(component)
        "empty_state" -> RenderEmptyState(component, queryResults)
    }
}
 
@Composable
private fun RenderStats(component: PluginUIComponent, queryResults: Map<String, List<JsonObject>>) {
    val items = component.props["items"]?.jsonArray ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val obj = item.jsonObject
            val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: ""
            val queryName = obj["queryName"]?.jsonPrimitive?.contentOrNull
            val field = obj["field"]?.jsonPrimitive?.contentOrNull
            val staticValue = obj["value"]?.jsonPrimitive?.contentOrNull
            val displayValue = when {
                staticValue != null -> staticValue
                queryName != null && field != null -> {
                    val result = queryResults[queryName]
                    when (field) {
                        "total" -> (result?.size ?: 0).toString()
                        "unique" -> (result?.mapNotNull { it["category"]?.jsonPrimitive?.contentOrNull }?.distinct()?.size ?: 0).toString()
                        else -> result?.size?.toString() ?: "0"
                    }
                }
                else -> "0"
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = displayValue, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
 
@Composable
private fun RenderSearchBar(component: PluginUIComponent, searchQuery: String, onSearchChange: (String) -> Unit) {
    val placeholder = component.props.stringField("placeholder") ?: "搜索..."
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(HugeIcons.Search01, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}
 
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderFilterBar(
    component: PluginUIComponent,
    queryResults: Map<String, List<JsonObject>>,
    filterValue: String,
    onFilterChange: (String) -> Unit
) {
    val queryName = component.props.stringField("queryName") ?: return
    val filterField = component.props.stringField("filterField") ?: "category"
    val allLabel = component.props.stringField("allLabel") ?: "全部"
    val items = queryResults[queryName] ?: emptyList()
    val categories = items.mapNotNull { it[filterField]?.jsonPrimitive?.contentOrNull }.distinct()
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(selected = filterValue == "all", onClick = { onFilterChange("all") }, label = { Text(allLabel) })
        categories.forEach { cat ->
            FilterChip(selected = filterValue == cat, onClick = { onFilterChange(cat) }, label = { Text(cat) })
        }
    }
}
 
@Composable
private fun RenderCardGrid(
    component: PluginUIComponent,
    queryResults: Map<String, List<JsonObject>>,
    filterValue: String,
    onAction: (PluginUIAction, Map<String, String>) -> Unit,
    onOpenForm: (PluginUIComponent) -> Unit
) {
    val queryName = component.props.stringField("queryName") ?: return
    val columns = component.props.intField("columns") ?: 2
    val imageField = component.props.stringField("imageField")
    val titleField = component.props.stringField("titleField") ?: "name"
    val subtitleField = component.props.stringField("subtitleField") ?: "description"
    val tagField = component.props.stringField("tagField")
    val filterField = component.props.stringField("filterField")
    val deleteActionName = component.props.stringField("deleteAction")
    val deleteKeyField = component.props.stringField("deleteKeyField") ?: "_key"
 
    val allItems = queryResults[queryName] ?: emptyList()
    val filteredItems = if (filterValue != "all" && filterField != null) {
        allItems.filter { it[filterField]?.jsonPrimitive?.contentOrNull == filterValue }
    } else allItems
 
    if (filteredItems.isEmpty()) return
 
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth().height((((filteredItems.size / columns) + 1) * 220).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(filteredItems) { item ->
                CardGridItem(
                    item = item,
                    imageField = imageField,
                    titleField = titleField,
                    subtitleField = subtitleField,
                    tagField = tagField,
                    onDelete = if (deleteActionName != null) { { key ->
                        onAction(
                            PluginUIAction(type = "dataStore_delete", params = JsonObject(mapOf("key" to JsonPrimitive(key)))),
                            mapOf(deleteKeyField to key)
                        )
                    } } else null,
                    deleteKeyField = deleteKeyField
                )
            }
        }
    }
}
 
@Composable
private fun CardGridItem(
    item: JsonObject,
    imageField: String?,
    titleField: String,
    subtitleField: String,
    tagField: String?,
    onDelete: ((String) -> Unit)?,
    deleteKeyField: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Box {
            Column {
                val imageUrl = imageField?.let { item[it]?.jsonPrimitive?.contentOrNull }
                val imageFile = item["imageFile"]?.jsonPrimitive?.contentOrNull
                when {
                    imageUrl != null && imageUrl.isNotBlank() -> AsyncImage(
                        model = imageUrl, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop
                    )
                    imageFile != null -> AsyncImage(
                        model = File(imageFile), contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Crop
                    )
                    else -> {
                        val initial = item[titleField]?.jsonPrimitive?.contentOrNull?.firstOrNull() ?: "?"
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = initial.toString(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = item[titleField]?.jsonPrimitive?.contentOrNull ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = item[subtitleField]?.jsonPrimitive?.contentOrNull
                    if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val tag = tagField?.let { item[it]?.jsonPrimitive?.contentOrNull }
                    if (tag != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
            if (onDelete != null) {
                val key = item[deleteKeyField]?.jsonPrimitive?.contentOrNull ?: ""
                IconButton(
                    onClick = { onDelete(key) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(HugeIcons.Delete02, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
 
@Composable
private fun RenderCardList(
    component: PluginUIComponent,
    queryResults: Map<String, List<JsonObject>>,
    filterValue: String,
    onAction: (PluginUIAction, Map<String, String>) -> Unit,
    onOpenForm: (PluginUIComponent) -> Unit
) {
    val queryName = component.props.stringField("queryName") ?: return
    val titleField = component.props.stringField("titleField") ?: "name"
    val subtitleField = component.props.stringField("subtitleField") ?: "description"
    val tagField = component.props.stringField("tagField")
    val filterField = component.props.stringField("filterField")
    val deleteActionName = component.props.stringField("deleteAction")
    val deleteKeyField = component.props.stringField("deleteKeyField") ?: "_key"
 
    val allItems = queryResults[queryName] ?: emptyList()
    val filteredItems = if (filterValue != "all" && filterField != null) {
        allItems.filter { it[filterField]?.jsonPrimitive?.contentOrNull == filterValue }
    } else allItems
 
    if (filteredItems.isEmpty()) return
 
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        filteredItems.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item[titleField]?.jsonPrimitive?.contentOrNull ?: "", style = MaterialTheme.typography.bodyLarge)
                        val subtitle = item[subtitleField]?.jsonPrimitive?.contentOrNull
                        if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val tag = tagField?.let { item[it]?.jsonPrimitive?.contentOrNull }
                        if (tag != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (deleteActionName != null) {
                        val key = item[deleteKeyField]?.jsonPrimitive?.contentOrNull ?: ""
                        IconButton(onClick = {
                            onAction(
                                PluginUIAction(
                                    type = deleteActionName,
                                    params = JsonObject(mapOf("name" to JsonPrimitive(item["name"]?.jsonPrimitive?.contentOrNull ?: key))),
                                    confirmDialog = me.rerere.rikkahub.plugin.model.PluginUIConfirmDialog(title = "删除确认", message = "确定删除？")
                                ),
                                mapOf(deleteKeyField to key, "name" to (item["name"]?.jsonPrimitive?.contentOrNull ?: key))
                            )
                        }) {
                            Icon(HugeIcons.Delete02, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
 
@Composable
private fun RenderButtonRow(
    component: PluginUIComponent,
    onAction: (PluginUIAction, Map<String, String>) -> Unit,
    onOpenForm: (PluginUIComponent) -> Unit
) {
    val buttons = component.props["buttons"]?.jsonArray ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { btn ->
            val obj = btn.jsonObject
            val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: ""
            val icon = obj["icon"]?.jsonPrimitive?.contentOrNull
            val variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "secondary"
            val isForm = obj["isForm"]?.jsonPrimitive?.booleanOrNull ?: false
            when (variant) {
                "primary" -> Button(onClick = { if (isForm) onOpenForm(component) }, modifier = Modifier.weight(1f)) {
                    if (icon != null) Text(icon); Spacer(Modifier.width(4.dp)); Text(label)
                }
                "text" -> TextButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    if (icon != null) Text(icon); Spacer(Modifier.width(4.dp)); Text(label)
                }
                else -> OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    if (icon != null) Text(icon); Spacer(Modifier.width(4.dp)); Text(label)
                }
            }
        }
    }
}
 
@Composable
private fun RenderDialogFormTrigger(component: PluginUIComponent, onOpenForm: (PluginUIComponent) -> Unit) {
    val triggerLabel = component.props.stringField("triggerLabel") ?: "添加"
    val triggerVariant = component.props.stringField("triggerVariant") ?: "primary"
    when (triggerVariant) {
        "primary" -> Button(onClick = { onOpenForm(component) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { Text(triggerLabel) }
        "secondary" -> OutlinedButton(onClick = { onOpenForm(component) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { Text(triggerLabel) }
        else -> FilledTonalButton(onClick = { onOpenForm(component) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { Text(triggerLabel) }
    }
}
 
@Composable
private fun RenderText(component: PluginUIComponent) {
    val content = component.props.stringField("content") ?: return
    val style = component.props.stringField("style") ?: "body"
    Text(
        text = content,
        style = when (style) {
            "headline" -> MaterialTheme.typography.headlineSmall
            "title" -> MaterialTheme.typography.titleMedium
            "caption" -> MaterialTheme.typography.bodySmall
            else -> MaterialTheme.typography.bodyLarge
        },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}
 
@Composable
private fun RenderEmptyState(component: PluginUIComponent, queryResults: Map<String, List<JsonObject>>) {
    val queryName = component.props.stringField("queryName")
    val message = component.props.stringField("message") ?: "暂无数据"
    val items = queryName?.let { queryResults[it] } ?: emptyList()
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
 
// ── 表单弹窗 ───────────────────────────────────────────────────
 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogFormSheet(
    component: PluginUIComponent,
    formValues: Map<String, String>,
    onFormValueChange: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit,
    dataStore: PluginDataStore,
    onPickImage: (String) -> Unit,
    onPickFile: (String) -> Unit
) {
    val title = component.props.stringField("title") ?: "添加"
    val fields = component.props["fields"]?.jsonArray ?: return
    val submitLabel = component.props.stringField("submitLabel") ?: "保存"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
 
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
 
            fields.forEach { fieldElement ->
                val field = fieldElement.jsonObject
                val name = field["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val label = field["label"]?.jsonPrimitive?.contentOrNull ?: name
                val type = field["type"]?.jsonPrimitive?.contentOrNull ?: "string"
                val required = field["required"]?.jsonPrimitive?.booleanOrNull ?: false
                val placeholder = field["placeholder"]?.jsonPrimitive?.contentOrNull
                val default = field["default"]?.jsonPrimitive?.contentOrNull
                val multiline = field["multiline"]?.jsonPrimitive?.booleanOrNull ?: false
                val currentValue = formValues[name] ?: default ?: ""
 
                when (type) {
                    "boolean" -> {
                        var checked by remember(name) { mutableStateOf(currentValue == "true") }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (required) "$label *" else label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = checked, onCheckedChange = { checked = it; onFormValueChange(name, it.toString()) })
                        }
                    }
                    "select" -> {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { onFormValueChange(name, it) },
                            label = { Text(if (required) "$label *" else label) },
                            placeholder = placeholder?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            readOnly = true, singleLine = true
                        )
                    }
                    "image" -> {
                        OutlinedButton(onClick = { onPickImage(name) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(if (formValues.containsKey(name)) "✓ 已选择图片" else "选择图片")
                        }
                    }
                    "file" -> {
                        // 文件选择器（支持 TXT / PDF）
                        val displayName = formValues["${name}__filename"]
                        val hasFile = formValues.containsKey(name) && formValues[name]?.isNotBlank() == true
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = if (required) "$label *" else label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedButton(
                                onClick = { onPickFile(name) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (hasFile) {
                                    Text("✓ ${displayName ?: "已选择文件"}")
                                } else {
                                    Text("📄 选择文件（TXT/PDF）")
                                }
                            }
                            if (hasFile) {
                                val charCount = formValues[name]?.length ?: 0
                                Text(
                                    text = "文件大小：约 ${charCount / 1000} K 字符",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    "integer" -> {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { onFormValueChange(name, it) },
                            label = { Text(if (required) "$label *" else label) },
                            placeholder = placeholder?.let { { Text(it) } },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true
                        )
                    }
                    else -> {
                        // string / multiline
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { onFormValueChange(name, it) },
                            label = { Text(if (required) "$label *" else label) },
                            placeholder = placeholder?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            minLines = if (multiline) 3 else 1,
                            maxLines = if (multiline) 6 else 1,
                            singleLine = !multiline
                        )
                    }
                }
            }
 
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                OutlinedButton(onClick = onDismiss) { Text("取消") }
                Button(onClick = { onSubmit(formValues) }) { Text(submitLabel) }
            }
        }
    }
}