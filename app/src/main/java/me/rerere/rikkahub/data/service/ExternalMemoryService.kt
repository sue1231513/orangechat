/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.rerere.rikkahub.data.model.ExternalMemory
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 外置记忆库服务
 * 基于 ExternalMemory 配置操作 Supabase 数据库
 */
class ExternalMemoryService(
    private val config: ExternalMemory
) {
    companion object {
        private const val TAG = "ExternalMemoryService"
    }

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /**
     * 保存聊天消息到外置记忆库
     */
    suspend fun saveMessage(
        assistantId: String,
        conversationId: String,
        role: String,
        content: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val endpoint = URL("$url/rest/v1/${config.tableName}")

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val jsonString = buildJsonObject {
                put("assistant_id", JsonPrimitive(assistantId))
                put("conversation_id", JsonPrimitive(conversationId))
                put("role", JsonPrimitive(role))
                put("content", JsonPrimitive(content))
                put("created_at", JsonPrimitive(sdf.format(java.util.Date())))
            }.toString()

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Prefer", "return=minimal")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "saveMessage HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            Log.d(TAG, "Saved message to ${config.tableName} for assistant $assistantId")
        }.map { }
    }

    /**
     * 查询最新 N 条消息
     */
    suspend fun queryLatestMessages(
        assistantId: String,
        limit: Int = 10,
    ): Result<List<ExternalMemoryMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val query = "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&order=created_at.desc&limit=$limit"
            val endpoint = URL("$url/rest/v1/${config.tableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "queryLatestMessages HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseMessages(responseText)
        }
    }

    /**
     * 关键词搜索消息
     */
    suspend fun searchMessages(
        assistantId: String,
        keyword: String,
        limit: Int = 10,
    ): Result<List<ExternalMemoryMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val encodedKeyword = URLEncoder.encode("%$keyword%", "UTF-8")
            val query = "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&content=ilike.$encodedKeyword&order=created_at.desc&limit=$limit"
            val endpoint = URL("$url/rest/v1/${config.tableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "searchMessages HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseMessages(responseText)
        }
    }

    /**
     * 保存日记摘要（可选带 embedding 向量）
     *
     * 若表缺少 embedding 列导致失败，自动降级为不带 embedding 重试，
     * 保证日记内容一定能写入表（向量检索能力需用户在 Supabase 补列后才有）。
     */
    suspend fun saveDiarySummary(
        assistantId: String,
        content: String,
        embedding: List<Float>? = null,
        /**
         * 这篇日记对应的日期（"yyyy-MM-dd"）。
         * 会作为 created_at 写入（设为该日 00:00:00），这样去重查询 querySummariesByDate
         * 才能按"日记对应日"命中，而不是按写入时刻（可能落在次日）导致每次都重复生成。
         * 为空时退回旧逻辑（用当前写入时刻）。
         */
        targetDate: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val createdAt = if (!targetDate.isNullOrBlank()) {
                "$targetDate 00:00:00"
            } else {
                sdf.format(java.util.Date())
            }

            // 首次尝试：若提供了 embedding，则带上 embedding 字段
            val firstJson = buildSummaryJson(assistantId, content, createdAt, embedding)
            try {
                postSummaries(firstJson)
                Log.i(
                    TAG,
                    "Saved diary summary to ${config.summariesTableName} for assistant $assistantId " +
                        "(with embedding=${embedding != null})"
                )
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                // 表缺少 embedding 列（PostgREST PGRST204 / 列名错误）时，降级为不带 embedding 重试
                val missingEmbeddingColumn = embedding != null &&
                    (
                        msg.contains("PGRST204", ignoreCase = true) ||
                            msg.contains("Could not find the", ignoreCase = true) ||
                            msg.contains("'embedding'", ignoreCase = true) ||
                            msg.contains("embedding", ignoreCase = true)
                        )
                if (missingEmbeddingColumn) {
                    Log.w(
                        TAG,
                        "Column 'embedding' not found in ${config.summariesTableName}, retrying without embedding",
                        e
                    )
                    val fallbackJson = buildSummaryJson(assistantId, content, createdAt, null)
                    postSummaries(fallbackJson)
                    Log.i(
                        TAG,
                        "Saved diary summary to ${config.summariesTableName} for assistant $assistantId " +
                            "(without embedding, fallback)"
                    )
                } else {
                    throw e
                }
            }
        }.map { }
    }

    /**
     * 构建日记摘要的 JSON body
     */
    private fun buildSummaryJson(
        assistantId: String,
        content: String,
        createdAt: String,
        embedding: List<Float>?
    ): String = buildJsonObject {
        put("assistant_id", JsonPrimitive(assistantId))
        put("content", JsonPrimitive(content))
        put("created_at", JsonPrimitive(createdAt))
        if (embedding != null) {
            // pgvector 接受 "[1.0,2.0]" 字符串形式
            put("embedding", JsonPrimitive(embedding.joinToString(",", "[", "]")))
        }
    }.toString()

    /**
     * 向 memory_summaries 表发送 POST 请求，失败抛出带详细错误体的异常
     */
    private fun postSummaries(jsonString: String) {
        val url = config.supabaseUrl.trimEnd('/')
        val endpoint = URL("$url/rest/v1/${config.summariesTableName}")

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", config.supabaseKey)
            setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
            setRequestProperty("Prefer", "return=minimal")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(jsonString)
            writer.flush()
        }

        val responseCode = connection.responseCode
        Log.d(TAG, "saveDiarySummary POST ${config.summariesTableName} responseCode=$responseCode")
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            Log.e(TAG, "saveDiarySummary HTTP $responseCode body=$errorBody")
            throw Exception("Supabase API error ($responseCode): $errorBody")
        }
    }

    /**
     * 按日期查询消息（用于日记总结）
     */
    suspend fun queryMessagesByDate(
        dateStr: String,
    ): Result<List<ExternalMemoryMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val startOfDay = "${dateStr} 00:00:00"
            val endOfDay = "${dateStr} 23:59:59"
            val query = "created_at=gte.${URLEncoder.encode(startOfDay, "UTF-8")}&created_at=lte.${URLEncoder.encode(endOfDay, "UTF-8")}&order=created_at.asc"
            val endpoint = URL("$url/rest/v1/${config.tableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "queryMessagesByDate HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseMessages(responseText)
        }
    }

    /**
     * 查询指定日期是否有日记摘要（用于去重）
     */
    suspend fun querySummariesByDate(
        assistantId: String,
        dateStr: String,
    ): Result<List<ExternalMemorySummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val startOfDay = "${dateStr} 00:00:00"
            val endOfDay = "${dateStr} 23:59:59"
            val query = "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&created_at=gte.${URLEncoder.encode(startOfDay, "UTF-8")}&created_at=lte.${URLEncoder.encode(endOfDay, "UTF-8")}&order=created_at.desc"
            val endpoint = URL("$url/rest/v1/${config.summariesTableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "querySummariesByDate HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseSummaries(responseText)
        }
    }

    /**
     * 查询最新日记摘要
     *
     * assistantId 可能对不上云端数据（本地助手 UUID vs 云端存的助手名），
     * 先按传入的 assistantId 过滤查询，查不到时回退为不带过滤查询最近 N 条，兜底保证能召回。
     */
    suspend fun queryLatestSummaries(
        assistantId: String,
        limit: Int = 5,
    ): Result<List<ExternalMemorySummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val filtered = querySummariesFiltered(assistantId, limit)
            if (filtered.isNotEmpty()) {
                return@runCatching filtered
            }
            // 兜底：不带 assistant_id 过滤
            querySummariesUnfiltered(limit)
        }
    }

    private fun querySummariesFiltered(
        assistantId: String,
        limit: Int,
    ): List<ExternalMemorySummary> {
        val url = config.supabaseUrl.trimEnd('/')
        val query = "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&order=created_at.desc&limit=$limit"
        val endpoint = URL("$url/rest/v1/${config.summariesTableName}?$query")

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", config.supabaseKey)
            setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            Log.e(TAG, "querySummariesFiltered HTTP $responseCode body=$errorBody")
            throw Exception("Supabase API error ($responseCode): $errorBody")
        }

        val responseText = connection.inputStream.bufferedReader().readText()
        return parseSummaries(responseText)
    }

    private fun querySummariesUnfiltered(
        limit: Int,
    ): List<ExternalMemorySummary> {
        val url = config.supabaseUrl.trimEnd('/')
        val query = "order=created_at.desc&limit=$limit"
        val endpoint = URL("$url/rest/v1/${config.summariesTableName}?$query")

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", config.supabaseKey)
            setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            Log.e(TAG, "querySummariesUnfiltered HTTP $responseCode body=$errorBody")
            throw Exception("Supabase API error ($responseCode): $errorBody")
        }

        val responseText = connection.inputStream.bufferedReader().readText()
        return parseSummaries(responseText)
    }

    /**
     * 关键词搜索消息（双路：先带 assistant_id，搜不到再全局搜）
     */
    suspend fun searchMessagesAnyAssistant(
        keyword: String,
        assistantId: String?,
        limit: Int = 10,
    ): Result<List<ExternalMemoryMessage>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val encodedKeyword = URLEncoder.encode("%$keyword%", "UTF-8")
            val idFilter = if (assistantId != null) {
                "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&"
            } else ""
            val query = "${idFilter}content=ilike.$encodedKeyword&order=created_at.desc&limit=$limit"
            val endpoint = URL("$url/rest/v1/${config.tableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "searchMessagesAnyAssistant HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseMessages(responseText)
        }
    }

    /**
     * 查询某助手的所有日记摘要（用于向量召回）
     */
    suspend fun queryAllSummaries(
        assistantId: String,
    ): Result<List<ExternalMemorySummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = config.supabaseUrl.trimEnd('/')
            val query = "assistant_id=eq.${URLEncoder.encode(assistantId, "UTF-8")}&order=created_at.desc"
            val endpoint = URL("$url/rest/v1/${config.summariesTableName}?$query")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", config.supabaseKey)
                setRequestProperty("Authorization", "Bearer ${config.supabaseKey}")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "queryAllSummaries HTTP $responseCode body=$errorBody")
                throw Exception("Supabase API error ($responseCode): $errorBody")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            parseSummaries(responseText)
        }
    }

    /**
     * 向量召回日记摘要（本地计算余弦相似度）
     */
    suspend fun vectorRecallSummaries(
        queryEmbedding: List<Float>,
        assistantId: String,
        count: Int = 5,
    ): Result<List<ExternalMemorySummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val allSummaries = queryAllSummaries(assistantId).getOrDefault(emptyList())
                .filter { it.embedding.isNotEmpty() }

            val scored = allSummaries.mapNotNull { summary ->
                val similarity = cosineSimilarity(queryEmbedding, summary.embedding)
                summary to similarity
            }

            scored.sortedByDescending { it.second }
                .take(count)
                .map { it.first }
        }
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f else dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    private fun parseMessages(jsonText: String): List<ExternalMemoryMessage> {
        val result = mutableListOf<ExternalMemoryMessage>()
        try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    ExternalMemoryMessage(
                        id = obj.optInt("id", 0),
                        assistantId = obj.optString("assistant_id", ""),
                        conversationId = obj.optString("conversation_id", ""),
                        role = obj.optString("role", ""),
                        content = obj.optString("content", ""),
                        createdAt = obj.optString("created_at", ""),
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse messages", e)
        }
        return result
    }

    private fun parseSummaries(jsonText: String): List<ExternalMemorySummary> {
        val result = mutableListOf<ExternalMemorySummary>()
        try {
            val array = JSONArray(jsonText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val embeddingStr = obj.optString("embedding", "")
                val embedding = if (embeddingStr.isNotBlank() && embeddingStr.startsWith("[")) {
                    embeddingStr.trim('[', ']').split(",").mapNotNull { it.trim().toFloatOrNull() }
                } else emptyList()
                result.add(
                    ExternalMemorySummary(
                        id = obj.optInt("id", 0),
                        assistantId = obj.optString("assistant_id", ""),
                        content = obj.optString("content", ""),
                        createdAt = obj.optString("created_at", ""),
                        embedding = embedding,
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse summaries", e)
        }
        return result
    }
}

data class ExternalMemoryMessage(
    val id: Int = 0,
    val assistantId: String = "",
    val conversationId: String = "",
    val role: String = "",
    val content: String = "",
    val createdAt: String = "",
)

data class ExternalMemorySummary(
    val id: Int = 0,
    val assistantId: String = "",
    val content: String = "",
    val createdAt: String = "",
    val embedding: List<Float> = emptyList(),
)