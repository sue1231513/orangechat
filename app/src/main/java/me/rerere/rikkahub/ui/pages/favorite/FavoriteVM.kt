/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.db.entity.FavoriteEntity
import me.rerere.rikkahub.data.favorite.NodeFavoriteAdapter
import me.rerere.rikkahub.data.model.FavoriteType
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.repository.FavoriteRepository
import kotlin.uuid.Uuid

data class NodeFavoriteListItem(
    val id: String,
    val refKey: String,
    val conversationId: Uuid,
    val nodeId: Uuid,
    val conversationTitle: String,
    val preview: String,
    val createdAt: Long,
    /**
     * 收藏指向的对话是否仍然存在。
     * false 表示原对话已被删除，这条收藏点开只会跳到空会话，属于失效项。
     */
    val conversationExists: Boolean = true,
)

class FavoriteVM(
    private val favoriteRepository: FavoriteRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {
    // 已确认不存在的 conversationId 集合，用于给列表标记失效收藏
    private val missingConversations = MutableStateFlow<Set<Uuid>>(emptySet())

    private val rawFavorites = favoriteRepository
        .listByType(FavoriteType.NODE)
        .map { favorites ->
            favorites.mapNotNull { entity ->
                val ref = NodeFavoriteAdapter.decodeRef(entity) ?: return@mapNotNull null
                val meta = NodeFavoriteAdapter.decodeMeta(entity)

                NodeFavoriteListItem(
                    id = entity.id,
                    refKey = entity.refKey,
                    conversationId = ref.conversationId,
                    nodeId = ref.nodeId,
                    conversationTitle = meta?.title.orEmpty(),
                    preview = meta?.previewText ?: "",
                    createdAt = entity.createdAt,
                )
            }
        }

    val nodeFavorites: StateFlow<List<NodeFavoriteListItem>> =
        rawFavorites
            .combine(missingConversations) { list, missing ->
                list.map { it.copy(conversationExists = it.conversationId !in missing) }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _cleaning = MutableStateFlow(false)
    val cleaning: StateFlow<Boolean> = _cleaning.asStateFlow()

    init {
        refreshValidity()
    }

    /**
     * 重新检查每条收藏指向的对话是否还在。
     *
     * 收藏只存 conversationId + nodeId 的指针，对话被删掉以后这条记录不会自动消失，
     * 所以进页面时统一核对一次，把失效项标出来。
     */
    fun refreshValidity() {
        viewModelScope.launch {
            val ids = nodeFavorites.value.map { it.conversationId }.distinct()
            if (ids.isEmpty()) return@launch
            val missing = ids.filterNot { conversationRepository.existsConversationById(it) }.toSet()
            missingConversations.value = missing
        }
    }

    fun removeFavorite(refKey: String) {
        viewModelScope.launch {
            favoriteRepository.deleteByRefKey(refKey)
        }
    }

    /**
     * 一键清理所有指向已删除对话的收藏。
     * 返回被清理的条数，供界面提示。
     */
    fun cleanInvalidFavorites(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _cleaning.value = true
            try {
                val invalid = nodeFavorites.value.filterNot { it.conversationExists }
                invalid.forEach { favoriteRepository.deleteByRefKey(it.refKey) }
                missingConversations.value = emptySet()
                onDone(invalid.size)
            } finally {
                _cleaning.value = false
            }
        }
    }

    suspend fun getEntityByRefKey(refKey: String): FavoriteEntity? {
        return favoriteRepository.getByRefKey(refKey)
    }

    fun restoreFavorite(entity: FavoriteEntity) {
        viewModelScope.launch {
            favoriteRepository.upsert(entity)
        }
    }
}
