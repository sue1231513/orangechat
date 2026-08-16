/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity


@Dao
interface MemoryBankDAO {
    // ===== MemoryBank CRUD =====

    @Insert
    suspend fun insertMemory(memory: MemoryBankEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryBankEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryBankEntity)

    @Query("DELETE FROM memory_bank WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)

    @Query("SELECT * FROM memory_bank WHERE id = :id")
    suspend fun getMemoryById(id: Int): MemoryBankEntity?

    @Query("SELECT * FROM memory_bank ORDER BY created_at DESC")
    suspend fun getAllMemories(): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type ORDER BY created_at DESC")
    suspend fun getMemoriesByType(type: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun getMemoriesByTypeLimit(type: String, limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId ORDER BY created_at DESC")
    suspend fun getMemoriesByAssistant(assistantId: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun getMemoriesByAssistantAndTypeLimit(assistantId: String, type: String, limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND type = :type AND date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByAssistantTypeAndDateGroup(assistantId: String, type: String, dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type AND date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByTypeAndDateGroup(type: String, dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT DISTINCT assistant_id FROM memory_bank WHERE assistant_id IS NOT NULL")
    suspend fun getDistinctAssistantIds(): List<String>

    @Query("SELECT * FROM memory_bank WHERE date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByDateGroup(dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE date_group = :dateGroup AND type = :type ORDER BY created_at DESC")
    suspend fun getMemoriesByDateGroupAndType(dateGroup: String, type: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE vector_status = :status")
    suspend fun getMemoriesByVectorStatus(status: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE vector_status = 'pending' AND vector_retry_count < :maxRetry ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingVectorMemories(maxRetry: Int, limit: Int = 50): List<MemoryBankEntity>

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'message' AND created_at > :sinceTimestamp")
    suspend fun getMessageCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'message'")
    suspend fun getTotalMessageCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = :type")
    suspend fun getCountByType(type: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'phase_summary' OR type = 'daily_summary'")
    suspend fun getSummaryCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE vector_status = :status")
    suspend fun getCountByVectorStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE assistant_id = :assistantId")
    suspend fun getCountByAssistant(assistantId: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE assistant_id = :assistantId AND type = :type")
    suspend fun getCountByAssistantAndType(assistantId: String, type: String): Int

    @Query("SELECT * FROM memory_bank ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE content LIKE '%' || :keyword || '%' ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchMemoriesByKeyword(keyword: String, limit: Int = 20): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE content LIKE '%' || :keyword || '%' AND type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchMemoriesByKeywordAndType(keyword: String, type: String, limit: Int = 20): List<MemoryBankEntity>

    @Query("SELECT DISTINCT date_group FROM memory_bank WHERE date_group IS NOT NULL ORDER BY date_group DESC LIMIT :limit")
    suspend fun getRecentDateGroups(limit: Int): List<String>

    @Query("UPDATE memory_bank SET vector_status = :status, vector_retry_count = :retryCount WHERE id = :id")
    suspend fun updateVectorStatus(id: Int, status: String, retryCount: Int)

    // ===== Vector recall queries =====

    /** 获取所有已向量化且 embedding 非空的记忆 */
    @Query("SELECT * FROM memory_bank WHERE vector_status = 'done' AND embedding IS NOT NULL AND embedding != '' ORDER BY created_at DESC")
    suspend fun getAllVectorizedMemories(): List<MemoryBankEntity>

    /** 获取指定助手已向量化且 embedding 非空的记忆 */
    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND vector_status = 'done' AND embedding IS NOT NULL AND embedding != '' ORDER BY created_at DESC")
    suspend fun getVectorizedMemoriesByAssistant(assistantId: String): List<MemoryBankEntity>

    /** 更新指定记录的 embedding 和 vector_status */
    @Query("UPDATE memory_bank SET embedding = NULL WHERE embedding IS NOT NULL")
    suspend fun clearAllEmbeddings()

    @Query("UPDATE memory_bank SET embedding = :embedding, vector_status = :status WHERE id = :id")
    suspend fun updateEmbedding(id: Int, embedding: String, status: String)
}
