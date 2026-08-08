/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.repository

import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.dao.FavoriteDAO
import me.rerere.rikkahub.data.db.entity.FavoriteEntity
import me.rerere.rikkahub.data.favorite.NodeFavoriteAdapter
import me.rerere.rikkahub.data.model.FavoriteMeta
import me.rerere.rikkahub.data.model.FavoriteType
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.data.model.NodeFavoriteTarget
import kotlin.uuid.Uuid

class FavoriteRepository(
    private val dao: FavoriteDAO,
) {
    fun listAll(): Flow<List<FavoriteEntity>> = dao.listAll()

    fun listByType(type: FavoriteType): Flow<List<FavoriteEntity>> = dao.listByType(type.value)

    suspend fun getByRefKey(refKey: String): FavoriteEntity? = dao.getByRefKey(refKey)

    suspend fun existsByRefKey(refKey: String): Boolean = dao.existsByRefKey(refKey)

    suspend fun deleteByRefKey(refKey: String): Int = dao.deleteByRefKey(refKey)

    suspend fun deleteById(id: String): Int = dao.deleteById(id)

    suspend fun upsert(entity: FavoriteEntity) = dao.upsert(entity)

    suspend fun addNodeFavorite(target: NodeFavoriteTarget): FavoriteEntity {
        val refKey = NodeFavoriteAdapter.buildRefKey(target)
        val existing = dao.getByRefKey(refKey)
        val favorite = NodeFavoriteAdapter.buildFavoriteEntity(
            target = target,
            existing = existing,
        )
        dao.upsert(favorite)
        return favorite
    }

    suspend fun removeNodeFavorite(conversationId: Uuid, nodeId: Uuid): Int {
        return dao.deleteByRefKey(NodeFavoriteAdapter.buildRefKey(conversationId.toString(), nodeId.toString()))
    }

    suspend fun isNodeFavorited(conversationId: Uuid, nodeId: Uuid): Boolean {
        return dao.existsByRefKey(NodeFavoriteAdapter.buildRefKey(conversationId.toString(), nodeId.toString()))
    }

    private fun moodletRefKey(conversationId: Uuid, moodKey: String): String =
        "moodlet:$conversationId:$moodKey"

    suspend fun isMoodletFavorited(conversationId: Uuid, moodKey: String): Boolean {
        return dao.existsByRefKey(moodletRefKey(conversationId, moodKey))
    }

    suspend fun setMoodletFavorited(
        conversationId: Uuid,
        conversationTitle: String,
        moodKey: String,
        favorited: Boolean,
        label: String,
        reason: String,
    ) {
        val refKey = moodletRefKey(conversationId, moodKey)
        if (!favorited) {
            dao.deleteByRefKey(refKey)
            return
        }
        val existing = dao.getByRefKey(refKey)
        val now = System.currentTimeMillis()
        val meta = FavoriteMeta(
            title = label.ifBlank { "情绪徽章" },
            subtitle = conversationTitle.ifBlank { null },
            previewText = reason.ifBlank { label }.ifBlank { moodKey },
        )
        dao.upsert(
            FavoriteEntity(
                id = existing?.id ?: refKey,
                type = FavoriteType.MOODLET.value,
                refKey = refKey,
                refJson = """{"conversationId":"$conversationId","moodKey":${JsonInstant.encodeToString(moodKey)}}""",
                snapshotJson = "",
                metaJson = JsonInstant.encodeToString(meta),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

}
