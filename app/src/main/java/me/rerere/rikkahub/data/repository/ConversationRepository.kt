/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.repository

import android.database.sqlite.SQLiteBlobTooBigException
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.db.fts.MessageFtsManager
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.dao.FavoriteDAO
import me.rerere.rikkahub.data.db.dao.MessageNodeDAO
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.db.entity.MessageNodeEntity
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.utils.JsonInstant
import java.time.Instant
import kotlin.uuid.Uuid

private const val TAG = "ConversationRepository"

class ConversationRepository(
    private val conversationDAO: ConversationDAO,
    private val messageNodeDAO: MessageNodeDAO,
    private val favoriteDAO: FavoriteDAO,
    private val database: AppDatabase,
    private val filesManager: FilesManager,
    private val messageFtsManager: MessageFtsManager,
) {
    companion object {
        private const val PAGE_SIZE = 20
        private const val INITIAL_LOAD_SIZE = 40
    }

    suspend fun getRecentConversations(assistantId: Uuid, limit: Int = 10): List<Conversation> {
        return conversationDAO.getRecentConversationsOfAssistant(
            assistantId = assistantId.toString(),
            limit = limit
        ).map { entity ->
            val nodes = loadMessageNodes(entity.id)
            conversationEntityToConversation(entity, nodes)
        }
    }

    fun getConversationsOfAssistant(assistantId: Uuid): Flow<List<Conversation>> {
        return conversationDAO
            .getConversationsOfAssistant(assistantId.toString())
            .map { flow ->
                flow.map { entity ->
                    // 列表视图不需要完整的 nodes，使用空列表
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun getConversationsOfAssistantPaging(assistantId: Uuid): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { conversationDAO.getConversationsOfAssistantPaging(assistantId.toString()) }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    fun getUnfiledConversationsOfAssistantPaging(assistantId: Uuid): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { conversationDAO.getUnfiledConversationsOfAssistantPaging(assistantId.toString()) }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    fun getConversationsOfFolderPaging(folderId: Uuid): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { conversationDAO.getConversationsOfFolderPaging(folderId.toString()) }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    suspend fun getConversationsOfAssistantPage(
        assistantId: Uuid,
        offset: Int,
        limit: Int,
    ): ConversationPageResult {
        val pagingSource = conversationDAO.getConversationsOfAssistantPaging(assistantId.toString())
        return try {
            when (
                val result = pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = if (offset == 0) null else offset,
                        loadSize = limit,
                        placeholdersEnabled = false
                    )
                )
            ) {
                is PagingSource.LoadResult.Page -> ConversationPageResult(
                    items = result.data.map { entity ->
                        conversationSummaryToConversation(entity)
                    },
                    nextOffset = result.nextKey
                )

                is PagingSource.LoadResult.Error -> throw result.throwable
                is PagingSource.LoadResult.Invalid -> ConversationPageResult(emptyList(), null)
            }
        } finally {
            pagingSource.invalidate()
        }
    }

    suspend fun searchConversationsOfAssistantPage(
        assistantId: Uuid,
        titleKeyword: String,
        offset: Int,
        limit: Int,
    ): ConversationPageResult {
        val pagingSource = conversationDAO.searchConversationsOfAssistantPaging(
            assistantId = assistantId.toString(),
            searchText = titleKeyword
        )
        return try {
            when (
                val result = pagingSource.load(
                    PagingSource.LoadParams.Refresh(
                        key = if (offset == 0) null else offset,
                        loadSize = limit,
                        placeholdersEnabled = false
                    )
                )
            ) {
                is PagingSource.LoadResult.Page -> ConversationPageResult(
                    items = result.data.map { entity ->
                        conversationSummaryToConversation(entity)
                    },
                    nextOffset = result.nextKey
                )

                is PagingSource.LoadResult.Error -> throw result.throwable
                is PagingSource.LoadResult.Invalid -> ConversationPageResult(emptyList(), null)
            }
        } finally {
            pagingSource.invalidate()
        }
    }

    fun searchConversations(titleKeyword: String): Flow<List<Conversation>> {
        return conversationDAO
            .searchConversations(titleKeyword)
            .map { flow ->
                flow.map { entity ->
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun searchConversationsPaging(titleKeyword: String): Flow<PagingData<Conversation>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { conversationDAO.searchConversationsPaging(titleKeyword) }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            conversationSummaryToConversation(entity)
        }
    }

    fun searchConversationsOfAssistant(assistantId: Uuid, titleKeyword: String): Flow<List<Conversation>> {
        return conversationDAO
            .searchConversationsOfAssistant(assistantId.toString(), titleKeyword)
            .map { flow ->
                flow.map { entity ->
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    fun searchConversationsOfAssistantPaging(assistantId: Uuid, titleKeyword: String): Flow<PagingData<Conversation>> =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                conversationDAO.searchConversationsOfAssistantPaging(
                    assistantId.toString(),
                    titleKeyword
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                conversationSummaryToConversation(entity)
            }
        }

    suspend fun getConversationById(uuid: Uuid): Conversation? {
        val entity = conversationDAO.getConversationById(uuid.toString())
        return if (entity != null) {
            val nodes = loadMessageNodes(entity.id)
            conversationEntityToConversation(entity, nodes)
        } else null
    }

    suspend fun existsConversationById(uuid: Uuid): Boolean {
        return conversationDAO.existsById(uuid.toString())
    }

    suspend fun insertConversation(conversation: Conversation) {
        try {
            database.withTransaction {
                conversationDAO.insert(
                    conversationToConversationEntity(conversation)
                )
                saveMessageNodes(conversation.id.toString(), conversation.messageNodes)
            }
            messageFtsManager.indexConversation(conversation)
        } catch (e: Exception) {
            Log.e(TAG, "insertConversation failed, conversationId=${conversation.id}", e)
            throw e
        }
    }

    /**
     * 更新对话。
     *
     * 之前的实现是"删掉这个对话下全部消息节点，再把全部节点重新插入"，不管这一轮实际改了几条消息。
     * 对话历史越长，这个开销越大，而且每次保存都要付出跟历史总长度成正比的代价。
     *
     * 现在改成按 MessageNode.id 做精确 diff：
     * - 新出现的 id -> 插入
     * - 消失的 id（比如重新生成时截断历史）-> 删除
     * - 两边都有但内容不同（用 MessageNodeEntity 的 data class 结构相等判断）-> 更新
     * - 完全没变的 -> 跳过，不碰数据库
     *
     * FTS 索引也同步降级为只处理这批发生变化的 node，而不是整个对话重建。
     */
    suspend fun updateConversation(conversation: Conversation) {
        try {
            database.withTransaction {
                conversationDAO.update(
                    conversationToConversationEntity(conversation)
                )

                // 只查 id，不碰 messages 列，避免因个别行 blob 过大导致这里直接抛异常
                val existingIds = messageNodeDAO.getNodeIdsOfConversation(conversation.id.toString()).toSet()

                val newEntities = conversation.messageNodes.mapIndexed { index, node ->
                    MessageNodeEntity(
                        id = node.id.toString(),
                        conversationId = conversation.id.toString(),
                        nodeIndex = index,
                        messages = JsonInstant.encodeToString(node.messages),
                        selectIndex = node.selectIndex
                    )
                }
                val newById = newEntities.associateBy { it.id }

                // 只对本轮涉及、且之前已存在的 id 去查内容做比较，缩小命中超大行的范围
                val idsNeedCompare = newEntities.map { it.id }.filter { it in existingIds }
                val existingById = if (idsNeedCompare.isEmpty()) {
                    emptyMap()
                } else {
                    try {
                        messageNodeDAO.getNodesByIds(idsNeedCompare).associateBy { it.id }
                    } catch (e: SQLiteBlobTooBigException) {
                        Log.w(
                            TAG,
                            "updateConversation: blob too big while comparing existing nodes, " +
                                "conversationId=${conversation.id}, will treat all as changed and overwrite",
                            e
                        )
                        // 读不出旧内容就没法比较，保守地当作全部有变化，直接覆盖写入（REPLACE，安全）
                        emptyMap()
                    }
                }

                // 只处理真正发生变化的 node：内容/顺序(nodeIndex)/selectIndex 有任何不同才写库；
                // 读不到旧内容的（existingById 里没有对应 id）也会被判定为"变化"，直接覆盖写入
                val toUpsert = newEntities.filter { newEntity ->
                    existingById[newEntity.id] != newEntity
                }
                val toDeleteIds = existingIds - newById.keys

                if (toDeleteIds.isNotEmpty()) {
                    messageNodeDAO.deleteByIds(toDeleteIds.toList())
                }
                if (toUpsert.isNotEmpty()) {
                    messageNodeDAO.insertAll(toUpsert)
                }

                val changedNodeIds = toUpsert.map { it.id }.toSet() + toDeleteIds
                if (changedNodeIds.isNotEmpty()) {
                    messageFtsManager.reindexNodes(
                        conversationId = conversation.id.toString(),
                        conversationTitle = conversation.title,
                        updateAt = conversation.updateAt,
                        changedNodeIds = changedNodeIds,
                        currentNodes = conversation.messageNodes,
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateConversation failed, conversationId=${conversation.id}", e)
            throw e
        }
    }

    suspend fun deleteConversation(conversation: Conversation) {
        try {
            // 获取完整的 Conversation（包含 messageNodes）以正确清理文件
            val fullConversation = if (conversation.messageNodes.isEmpty()) {
                getConversationById(conversation.id) ?: conversation
            } else {
                conversation
            }
            messageFtsManager.deleteConversation(conversation.id.toString())
            database.withTransaction {
                // message_node 会通过 CASCADE 自动删除
                conversationDAO.delete(
                    conversationToConversationEntity(conversation)
                )
                // favorites 表不是 conversation 的外键子表，需要显式清理，
                // 否则会留下打不开也难删除的孤儿收藏
                favoriteDAO.deleteNodeFavoritesOfConversation(conversation.id.toString())
            }
            filesManager.deleteChatFiles(fullConversation.files)
        } catch (e: Exception) {
            Log.e(TAG, "deleteConversation failed, conversationId=${conversation.id}", e)
            throw e
        }
    }

    suspend fun searchMessages(keyword: String) = messageFtsManager.search(keyword)

    suspend fun rebuildAllIndexes(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }) {
        try {
            messageFtsManager.deleteAll()
            val allIds = conversationDAO.getAllIds()
            val total = allIds.size
            allIds.forEachIndexed { index, id ->
                val entity = conversationDAO.getConversationById(id) ?: return@forEachIndexed
                val nodes = loadMessageNodes(entity.id)
                val conversation = conversationEntityToConversation(entity, nodes)
                messageFtsManager.indexConversation(conversation)
                onProgress(index + 1, total)
            }
        } catch (e: Exception) {
            Log.e(TAG, "rebuildAllIndexes failed", e)
            throw e
        }
    }

    suspend fun deleteConversationOfAssistant(assistantId: Uuid) {
        getConversationsOfAssistant(assistantId).first().forEach { conversation ->
            deleteConversation(conversation)
        }
    }

    fun conversationToConversationEntity(conversation: Conversation): ConversationEntity {
        require(conversation.messageNodes.none { it.messages.any { message -> message.hasBase64Part() } })
        return ConversationEntity(
            id = conversation.id.toString(),
            title = conversation.title,
            nodes = "[]",  // nodes 现在存储在单独的表中
            createAt = conversation.createAt.toEpochMilli(),
            updateAt = conversation.updateAt.toEpochMilli(),
            assistantId = conversation.assistantId.toString(),
            chatSuggestions = JsonInstant.encodeToString(conversation.chatSuggestions),
            isPinned = conversation.isPinned,
            customSystemPrompt = conversation.customSystemPrompt ?: "",
            folderId = conversation.folderId?.toString() ?: "",
        )
    }

    fun conversationEntityToConversation(
        conversationEntity: ConversationEntity,
        messageNodes: List<MessageNode>
    ): Conversation {
        return Conversation(
            id = Uuid.parse(conversationEntity.id),
            title = conversationEntity.title,
            messageNodes = messageNodes.filter { it.messages.isNotEmpty() },
            createAt = Instant.ofEpochMilli(conversationEntity.createAt),
            updateAt = Instant.ofEpochMilli(conversationEntity.updateAt),
            assistantId = Uuid.parse(conversationEntity.assistantId),
            chatSuggestions = JsonInstant.decodeFromString(conversationEntity.chatSuggestions),
            isPinned = conversationEntity.isPinned,
            customSystemPrompt = conversationEntity.customSystemPrompt.ifEmpty { null },
            folderId = conversationEntity.folderId.ifEmpty { null }?.let { Uuid.parse(it) },
        )
    }

    fun getPinnedConversations(): Flow<List<Conversation>> {
        return conversationDAO
            .getPinnedConversations()
            .map { flow ->
                flow.map { entity ->
                    conversationEntityToConversation(entity, emptyList())
                }
            }
    }

    suspend fun togglePinStatus(conversationId: Uuid) {
        conversationDAO.updatePinStatus(
            id = conversationId.toString(),
            isPinned = !(getConversationById(conversationId)?.isPinned ?: false)
        )
    }

    /**
     * 单列更新会话的文件夹归属，folderId 为 null 表示移出文件夹（未归类）。
     */
    suspend fun updateConversationFolderId(conversationId: Uuid, folderId: Uuid?) {
        conversationDAO.updateFolderId(
            id = conversationId.toString(),
            folderId = folderId?.toString() ?: ""
        )
    }

    private fun conversationSummaryToConversation(entity: LightConversationEntity): Conversation {
        return Conversation(
            id = Uuid.parse(entity.id),
            assistantId = Uuid.parse(entity.assistantId),
            title = entity.title,
            isPinned = entity.isPinned,
            createAt = Instant.ofEpochMilli(entity.createAt),
            updateAt = Instant.ofEpochMilli(entity.updateAt),
            messageNodes = emptyList(),
            folderId = entity.folderId.ifEmpty { null }?.let { Uuid.parse(it) },
        )
    }

    private suspend fun loadMessageNodes(conversationId: String): List<MessageNode> {
        val favoriteNodeIds = favoriteDAO
            .getFavoriteNodeIdsOfConversation(conversationId)
            .mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }
            .toSet()

        return database.withTransaction {
            val nodes = mutableListOf<MessageNode>()

            // 提前查出总行数，这样命中超大blob逐行重试时能精确控制范围，
            // 不再依赖"空结果"来判断是否读到末尾——空结果既可能是被跳过的超大行，
            // 也可能是真的没数据，两者无法区分，是之前会误判提前退出、丢消息的根因。
            val totalCount = messageNodeDAO.getNodeCountOfConversation(conversationId)
            if (totalCount == 0) return@withTransaction nodes

            val pageSize = 64
            var offset = 0
            while (offset < totalCount) {
                val page = try {
                    messageNodeDAO.getNodesOfConversationPaged(conversationId, pageSize, offset)
                } catch (e: SQLiteBlobTooBigException) {
                    Log.w(
                        TAG,
                        "loadMessageNodes: blob too big in page, conversationId=$conversationId, " +
                            "offset=$offset, retrying row by row to isolate the bad row",
                        e
                    )
                    // 整页命中超大blob时，逐行重试，精确定位并只跳过那一行，
                    // 避免同一页里其他正常的消息被连坐丢弃
                    // （这是之前 UI 显示数 < 数据库实际数的根因）
                    val recovered = mutableListOf<MessageNodeEntity>()
                    val pageEnd = minOf(offset + pageSize, totalCount)
                    for (rowOffset in offset until pageEnd) {
                        try {
                            val row = messageNodeDAO.getNodesOfConversationPaged(conversationId, 1, rowOffset)
                            recovered.addAll(row)
                        } catch (rowError: SQLiteBlobTooBigException) {
                            Log.e(
                                TAG,
                                "loadMessageNodes: skipping single oversized row, " +
                                    "conversationId=$conversationId, offset=$rowOffset",
                                rowError
                            )
                        }
                    }
                    recovered
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "loadMessageNodes failed, conversationId=$conversationId, offset=$offset", e)
                    offset += pageSize
                    continue
                }

                // 注意：不能用 page.isEmpty() 来判断是否读完——逐行重试时如果整页全是超大行，
                // recovered 会是空的，但后面可能还有正常数据。循环终止完全由 offset < totalCount 控制。
                page.forEach { entity ->
                    val messages = JsonInstant.decodeFromString<List<UIMessage>>(entity.messages)
                    val nodeId = Uuid.parse(entity.id)
                    nodes.add(
                        MessageNode(
                            id = nodeId,
                            messages = messages,
                            selectIndex = entity.selectIndex,
                            isFavorite = favoriteNodeIds.contains(nodeId)
                        )
                    )
                }
                offset += pageSize
            }
            nodes
        }
    }

    private suspend fun saveMessageNodes(conversationId: String, nodes: List<MessageNode>) {
        val entities = nodes.mapIndexed { index, node ->
            MessageNodeEntity(
                id = node.id.toString(),
                conversationId = conversationId,
                nodeIndex = index,
                messages = JsonInstant.encodeToString(node.messages),
                selectIndex = node.selectIndex
            )
        }
        messageNodeDAO.insertAll(entities)
    }

    /**
     * 获取指定日期范围内的所有聊天记录（用于日记总结）
     * 从 message_node 表读取，不依赖 memory_bank
     */
    suspend fun getMessagesByDateRange(startDate: String, endDate: String): List<DatedMessage> = withContext(Dispatchers.IO) {
        val allConversationIds = conversationDAO.getAllIds()
        val result = mutableListOf<DatedMessage>()

        for (conversationId in allConversationIds) {
            val nodes = try {
                loadMessageNodes(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "getMessagesByDateRange: failed to load nodes, conversationId=$conversationId", e)
                continue
            }

            for (node in nodes) {
                // 只取选中的消息（selectIndex 对应的）
                val selectedMessage = node.messages.getOrNull(node.selectIndex) ?: continue
                val dateStr = selectedMessage.createdAt.toString().take(10) // yyyy-MM-dd

                if (dateStr >= startDate && dateStr <= endDate) {
                    val text = selectedMessage.parts.filterIsInstance<UIMessagePart.Text>()
                        .joinToString("\n") { it.text }
                        .trim()
                    if (text.isNotBlank()) {
                        result.add(
                            DatedMessage(
                                date = dateStr,
                                role = selectedMessage.role.name.lowercase(),
                                content = text,
                                assistantId = null, // 无法直接从消息获取
                                conversationId = conversationId
                            )
                        )
                    }
                }
            }
        }

        result.sortedBy { it.date }
    }
}

/**
 * 带日期的聊天记录条目（用于日记总结）
 */
data class DatedMessage(
    val date: String,
    val role: String,
    val content: String,
    val assistantId: String?,
    val conversationId: String
)

/**
 * 轻量级的会话查询结果，不包含 nodes 和 suggestions 字段
 */
data class LightConversationEntity(
    val id: String,
    val assistantId: String,
    val title: String,
    val isPinned: Boolean,
    val createAt: Long,
    val updateAt: Long,
    val folderId: String = "",
)

data class ConversationPageResult(
    val items: List<Conversation>,
    val nextOffset: Int?,
)