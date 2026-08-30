/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import me.rerere.rikkahub.data.db.entity.MessageNodeEntity

@Dao
interface MessageNodeDAO {
    @Query("SELECT * FROM message_node WHERE conversation_id = :conversationId ORDER BY node_index ASC")
    suspend fun getNodesOfConversation(conversationId: String): List<MessageNodeEntity>

    /**
     * 只查 id，不查 messages 列，用于计算"哪些node已经不存在了需要删除"，
     * 不会因为某一行 messages 内容过大触发 SQLiteBlobTooBigException。
     */
    @Query("SELECT id FROM message_node WHERE conversation_id = :conversationId ORDER BY node_index ASC")
    suspend fun getNodeIdsOfConversation(conversationId: String): List<String>

    /**
     * 按 id 列表批量查询完整内容，用于对"本轮涉及改动的node"做内容比较（是否需要更新）。
     * 只对本轮实际涉及的少量 id 查询，比对整个对话全表查询更不容易命中超大行。
     */
    @Query("SELECT * FROM message_node WHERE id IN (:ids)")
    suspend fun getNodesByIds(ids: List<String>): List<MessageNodeEntity>

    /**
     * 查某个对话下 node 的总行数。loadMessageNodes 用它提前知道总行数，
     * 这样在逐行重试跳过超大blob行时，能精确控制循环范围，
     * 不会因为"空结果"（可能是被跳过的超大行，也可能是真的到末尾）而误判提前退出。
     */
    @Query("SELECT COUNT(*) FROM message_node WHERE conversation_id = :conversationId")
    suspend fun getNodeCountOfConversation(conversationId: String): Int

    @Query(
        "SELECT * FROM message_node WHERE conversation_id = :conversationId " +
            "ORDER BY node_index ASC LIMIT :limit OFFSET :offset"
    )
    suspend fun getNodesOfConversationPaged(
        conversationId: String,
        limit: Int,
        offset: Int
    ): List<MessageNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<MessageNodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: MessageNodeEntity)

    @Update
    suspend fun update(node: MessageNodeEntity)

    /**
     * 清理超旧对话的消息节点（保留 N 天内）
     * 返回清理掉的对话数
     */
    @Query("DELETE FROM message_node WHERE conversation_id IN (SELECT id FROM conversationentity WHERE update_at < :cutoffMillis)")
    suspend fun deleteOldConversationNodes(cutoffMillis: Long): Int

    /**
     * 清理超旧对话的记录本身（保留 N 天内）
     */
    @Query("DELETE FROM conversationentity WHERE update_at < :cutoffMillis")
    suspend fun deleteOldConversations(cutoffMillis: Long): Int

    /**
     * 统计超旧对话的 node 行数（用于展示可清理量）
     */
    @Query("SELECT COUNT(*) FROM message_node WHERE conversation_id IN (SELECT id FROM conversationentity WHERE update_at < :cutoffMillis)")
    suspend fun countOldConversationNodes(cutoffMillis: Long): Int

    @Query("DELETE FROM message_node WHERE id = :nodeId")
    suspend fun deleteById(nodeId: String)

    /**
     * 按 id 批量删除节点。用于 ConversationRepository.updateConversation 的增量 diff 更新：
     * 只删除本轮真正消失的 node，而不是像以前那样把整个对话的 node 全部删光重建。
     */
    @Query("DELETE FROM message_node WHERE id IN (:nodeIds)")
    suspend fun deleteByIds(nodeIds: List<String>)

    // 使用 @RawQuery 绕过 Room 编译期校验，以便使用 json_each() 虚拟表
    @RawQuery
    suspend fun getTokenStatsRaw(query: SupportSQLiteQuery): MessageTokenStats

    @RawQuery
    suspend fun getMessageCountPerDayRaw(query: SupportSQLiteQuery): List<MessageDayCount>
}

data class MessageTokenStats(
    val totalMessages: Int = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cachedTokens: Long = 0,
)

data class MessageDayCount(val day: String, val count: Int)

// SQLite json_each() 展开 messages JSON 数组，json_extract() 提取 Token 字段并聚合
private val TOKEN_STATS_SQL = SimpleSQLiteQuery(
    "SELECT COUNT(*) AS totalMessages, " +
        "COALESCE(SUM(CAST(json_extract(j.value, '$.usage.promptTokens') AS INTEGER)), 0) AS promptTokens, " +
        "COALESCE(SUM(CAST(json_extract(j.value, '$.usage.completionTokens') AS INTEGER)), 0) AS completionTokens, " +
        "COALESCE(SUM(CAST(json_extract(j.value, '$.usage.cachedTokens') AS INTEGER)), 0) AS cachedTokens " +
        "FROM message_node mn, json_each(mn.messages) j"
)

suspend fun MessageNodeDAO.getTokenStats(): MessageTokenStats = getTokenStatsRaw(TOKEN_STATS_SQL)

// 按用户消息的 createdAt 字段（LocalDateTime ISO 字符串前10位即日期）统计每日消息数
suspend fun MessageNodeDAO.getMessageCountPerDay(startDate: String): List<MessageDayCount> =
    getMessageCountPerDayRaw(
        SimpleSQLiteQuery(
            "SELECT substr(json_extract(j.value, '$.createdAt'), 1, 10) AS day, " +
                "COUNT(*) AS count " +
                "FROM message_node mn, json_each(mn.messages) j " +
                "WHERE json_extract(j.value, '$.role') = 'user' " +
                "AND json_extract(j.value, '$.createdAt') >= ? " +
                "GROUP BY day",
            arrayOf(startDate)
        )
    )