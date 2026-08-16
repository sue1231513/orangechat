/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.db.fts.MessageSearchResult
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.repository.ConversationRepository

class SearchVM(
    private val conversationRepo: ConversationRepository,
    private val settingsStore: me.rerere.rikkahub.data.datastore.SettingsStore,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")

    var searchQuery by mutableStateOf("")
        private set
    var results by mutableStateOf<List<MessageSearchResult>>(emptyList())
        private set
    var cloudResults by mutableStateOf<List<CloudSearchResult>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRebuilding by mutableStateOf(false)
        private set
    var rebuildProgress by mutableStateOf(0 to 0)
        private set

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collectLatest { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        searchQuery = query
        _searchQuery.value = query
    }

    fun search() {
        viewModelScope.launch {
            performSearch(searchQuery)
        }
    }

    fun rebuildIndex() {
        viewModelScope.launch {
            isRebuilding = true
            rebuildProgress = 0 to 0
            try {
                conversationRepo.rebuildAllIndexes { current, total ->
                    rebuildProgress = current to total
                }
            } finally {
                isRebuilding = false
            }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            results = emptyList()
            cloudResults = emptyList()
            return
        }
        isLoading = true
        try {
            results = conversationRepo.searchMessages(query)
            // 同时搜 Supabase 云端
            searchCloud(query)
        } finally {
            isLoading = false
        }
    }

    private suspend fun searchCloud(query: String) {
        try {
            val settings = settingsStore.settingsFlow.value
            val externalConfigs = settings.externalMemories.filter { it.enabled }
            val allCloudResults = mutableListOf<CloudSearchResult>()
            externalConfigs.forEach { config ->
                val service = me.rerere.rikkahub.data.service.ExternalMemoryService(config)
                val cloudMsgs = service.searchMessages(
                    assistantId = settings.getCurrentAssistant().id.toString(),
                    keyword = query,
                    limit = 20,
                ).getOrDefault(emptyList())
                cloudMsgs.forEach { msg ->
                    allCloudResults.add(CloudSearchResult(
                        content = msg.content,
                        role = msg.role,
                        createdAt = msg.createdAt,
                        source = config.name,
                    ))
                }
            }
            cloudResults = allCloudResults
        } catch (e: Exception) {
            cloudResults = emptyList()
        }
    }
}

data class CloudSearchResult(
        val content: String,
        val role: String,
        val createdAt: String,
        val source: String,
)
