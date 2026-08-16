/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.db.dao.MemoryBankDAO
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private const val TAG = "MemoryBankService"

class MemoryBankService(
    private val memoryBankDAO: MemoryBankDAO,
    private val okHttpClient: OkHttpClient,
    private val context: Context,
) {
    data class MemoryStats(
        val total: Int = 0,
        val messageCount: Int = 0,
        val summaryCount: Int = 0,
        val manualCount: Int = 0,
        val vectorizedCount: Int = 0,
        val pendingCount: Int = 0,
        val failedCount: Int = 0,
    )

    val recallCount: Int = 3

    suspend fun getAssistantIds(): List<String> = withContext(Dispatchers.IO) {
        memoryBankDAO.getDistinctAssistantIds()
    }

    suspend fun getStats(assistantId: String? = null): MemoryStats = withContext(Dispatchers.IO) {
        val total = if (assistantId != null) {
            memoryBankDAO.getCountByAssistant(assistantId)
        } else {
            memoryBankDAO.getTotalCount()
        }
        val messageCount = if (assistantId != null) {
            memoryBankDAO.getCountByAssistantAndType(assistantId, "message")
        } else {
            memoryBankDAO.getCountByType("message")
        }
        val summaryCount = memoryBankDAO.getSummaryCount()
        val manualCount = if (assistantId != null) {
            memoryBankDAO.getCountByAssistantAndType(assistantId, "manual")
        } else {
            memoryBankDAO.getCountByType("manual")
        }
        val vectorizedCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("done")
        }
        val pendingCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("pending")
        }
        val failedCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("failed")
        }
        MemoryStats(
            total = total,
            messageCount = messageCount,
            summaryCount = summaryCount,
            manualCount = manualCount,
            vectorizedCount = vectorizedCount,
            pendingCount = pendingCount,
            failedCount = failedCount
        )
    }

    suspend fun getTodayPhaseSummaries(assistantId: String? = null): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantTypeAndDateGroup(assistantId, "phase_summary", today)
        } else {
            memoryBankDAO.getMemoriesByTypeAndDateGroup("phase_summary", today)
        }
    }

    suspend fun getDailySummaries(assistantId: String? = null): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantTypeAndDateGroup(assistantId, "daily_summary", today)
        } else {
            memoryBankDAO.getMemoriesByTypeAndDateGroup("daily_summary", today)
        }
    }

    suspend fun searchMemories(
        keyword: String = "",
        type: String = "",
        limit: Int = 100,
        assistantId: String? = null
    ): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        if (keyword.isNotBlank() && type.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeywordAndType(keyword, type, limit)
        } else if (keyword.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeyword(keyword, limit)
        } else if (type.isNotBlank() && assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantAndTypeLimit(assistantId, type, limit)
        } else if (type.isNotBlank()) {
            memoryBankDAO.getMemoriesByTypeLimit(type, limit)
        } else {
            memoryBankDAO.getRecentMemories(limit)
        }
    }

    suspend fun deleteMemory(id: Int) = withContext(Dispatchers.IO) {
        memoryBankDAO.deleteMemoryById(id)
    }

    suspend fun rebuildIndex() {
        // No-op: vector index removed
    }

    suspend fun processPendingVectors() {
        // No-op: vector processing removed
    }

    suspend fun saveManualMemory(content: String): MemoryBankEntity = withContext(Dispatchers.IO) {
        val entity = MemoryBankEntity(
            content = content,
            type = "manual"
        )
        val id = memoryBankDAO.insertMemory(entity).toInt()
        entity.copy(id = id)
    }

    suspend fun recallMemories(query: String, count: Int): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeyword(query, count)
        } else {
            memoryBankDAO.getRecentMemories(count)
        }
    }

    // ==================== Vector Recall ====================

    /**
     * 向量召回：从 Supabase 远端查 embedding 做余弦相似度
     * 本地不再存储 embedding，全部走 ExternalMemoryService
     */
    suspend fun vectorRecall(
        queryEmbedding: List<Float>,
        assistantId: String? = null,
        count: Int = recallCount
    ): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        // 本地 embedding 已清空，返回空列表，调用方应改用 ExternalMemoryService.vectorRecallSummaries
        emptyList()
    }

    /**
     * 保存自动总结到记忆库
     */
    suspend fun saveAutoSummary(
        content: String,
        assistantId: String?,
        conversationId: String? = null
    ): MemoryBankEntity? = withContext(Dispatchers.IO) {
        try {
            val entity = MemoryBankEntity(
                content = content,
                type = "auto_summary",
                assistantId = assistantId,
                conversationId = conversationId,
                vectorStatus = "skipped"
            )
            val id = memoryBankDAO.insertMemory(entity).toInt()
            entity.copy(id = id)
        } catch (e: Exception) {
            Log.e(TAG, "saveAutoSummary failed", e)
            null
        }
    }

    /**
     * 保存单条聊天记录到记忆库
     */
    suspend fun saveChatMessage(
        content: String,
        role: String,
        assistantId: String?,
        conversationId: String? = null
    ): MemoryBankEntity? = withContext(Dispatchers.IO) {
        try {
            val entity = MemoryBankEntity(
                content = content,
                type = "message",
                role = role,
                assistantId = assistantId,
                conversationId = conversationId,
                vectorStatus = "skipped"
            )
            val id = memoryBankDAO.insertMemory(entity).toInt()
            entity.copy(id = id)
        } catch (e: Exception) {
            Log.e(TAG, "saveChatMessage failed", e)
            null
        }
    }

    /**
     * 清空本地 embedding 数据并执行 VACUUM 回收磁盘空间
     * embedding 已迁移到 Supabase 远端存储，本地不再需要
     * @return 清空的 embedding 条数
     */
    suspend fun clearLocalEmbeddingsAndVacuum(): Int = withContext(Dispatchers.IO) {
        val beforeCount = memoryBankDAO.getCountByVectorStatus("done")
        // 清空所有 embedding 字段
        memoryBankDAO.clearAllEmbeddings()
        Log.i(TAG, "Cleared $beforeCount local embeddings")
        beforeCount
    }

    // ==================== Helper Methods ====================

    private fun parseEmbedding(embeddingJson: String?): List<Float>? {
        // 本地 embedding 已废弃，始终返回 null
        null
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
}
