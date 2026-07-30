package com.example.contentplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentplayer.BuildConfig
import com.example.contentplayer.data.ApiProvider
import com.example.contentplayer.data.ContentApi
import com.example.contentplayer.data.ContentItem
import com.example.contentplayer.data.FavoriteRequest
import com.example.contentplayer.data.LoginRequest
import com.example.contentplayer.data.ProgressRequest
import com.example.contentplayer.data.ResolveRequest
import com.example.contentplayer.data.ResolvedMedia
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ListState(
    val title: String = "内容",
    val path: String = "/",
    val items: List<ContentItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class MainViewModel(
    private val api: ContentApi = ApiProvider.api,
) : ViewModel() {
    private val _browser = MutableStateFlow(ListState())
    val browser: StateFlow<ListState> = _browser.asStateFlow()

    private val _search = MutableStateFlow(ListState(title = "搜索"))
    val search: StateFlow<ListState> = _search.asStateFlow()

    private val _library = MutableStateFlow(ListState(title = "收藏"))
    val library: StateFlow<ListState> = _library.asStateFlow()

    private val _selected = MutableStateFlow<ContentItem?>(null)
    val selected: StateFlow<ContentItem?> = _selected.asStateFlow()

    private val _authenticated = MutableStateFlow(false)
    val authenticated: StateFlow<Boolean> = _authenticated.asStateFlow()

    fun login(username: String, password: String) = viewModelScope.launch {
        runCatching { api.login(LoginRequest(username.trim(), password)) }
            .onSuccess { _authenticated.value = true; browse("/") }
    }

    fun browse(path: String) = viewModelScope.launch {
        _browser.value = _browser.value.copy(path = path, loading = true, error = null)
        runCatching { api.content(path) }
            .onSuccess { items ->
                _browser.value = ListState(
                    title = if (path == "/") "内容" else path.substringAfterLast('/'),
                    path = path,
                    items = items,
                )
            }
            .onFailure { _browser.value = _browser.value.copy(loading = false, error = message(it)) }
    }

    fun search(query: String) = viewModelScope.launch {
        if (query.isBlank()) {
            _search.value = ListState(title = "搜索")
            return@launch
        }
        _search.value = _search.value.copy(loading = true, error = null)
        runCatching { api.content("/", query.trim()) }
            .onSuccess { _search.value = ListState(title = "搜索", items = it) }
            .onFailure { _search.value = _search.value.copy(loading = false, error = message(it)) }
    }

    fun loadLibrary(favorites: Boolean) = viewModelScope.launch {
        val title = if (favorites) "收藏" else "历史"
        _library.value = ListState(title = title, loading = true)
        runCatching { if (favorites) api.favorites() else api.history() }
            .onSuccess { saved ->
                val items = saved.map {
                    ContentItem(
                        path = it.path,
                        name = it.name ?: it.path.substringAfterLast('/'),
                        serverId = it.serverId,
                    )
                }
                _library.value = ListState(title = title, items = items)
            }
            .onFailure { _library.value = ListState(title = title, error = message(it)) }
    }

    fun select(item: ContentItem) {
        _selected.value = item
    }

    suspend fun resolve(item: ContentItem): ResolvedMedia {
        val resolved = api.resolve(ResolveRequest(item.serverId, item.path))
        val mediaUrl = resolved.url ?: resolved.proxyUrl?.let {
            BuildConfig.BASE_URL.toHttpUrl().resolve(it)?.toString()
        }
        require(!mediaUrl.isNullOrBlank()) { "服务端未返回播放地址" }
        return resolved.copy(url = mediaUrl)
    }

    fun reportProgress(item: ContentItem, positionMs: Long, durationMs: Long) {
        if (positionMs <= 0 || durationMs <= 0) return
        viewModelScope.launch {
            runCatching {
                api.progress(
                    ProgressRequest(
                        item.serverId,
                        item.path,
                        positionMs / 1000.0,
                        durationMs / 1000.0,
                    ),
                )
            }
        }
    }

    fun addFavorite(item: ContentItem) = viewModelScope.launch {
        runCatching { api.favorite(FavoriteRequest(item.serverId, item.path, item.name)) }
    }

    private fun message(error: Throwable): String = error.message?.take(120) ?: "请求失败"
}
