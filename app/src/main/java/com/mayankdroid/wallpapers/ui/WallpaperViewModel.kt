package com.mayankdroid.wallpapers.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayankdroid.wallpapers.data.Wallpaper
import com.mayankdroid.wallpapers.data.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FeedState {
    data object Loading : FeedState
    data class Success(val items: List<Wallpaper>) : FeedState
    data class Error(val message: String) : FeedState
}

class WallpaperViewModel(
    private val repository: WallpaperRepository = WallpaperRepository()
) : ViewModel() {
    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { refresh(initial = true) }

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            if (!initial) _refreshing.value = true
            runCatching { repository.fetch() }
                .onSuccess { _state.value = FeedState.Success(it) }
                .onFailure { error ->
                    if (_state.value !is FeedState.Success) {
                        _state.value = FeedState.Error(error.message ?: "Unable to load wallpapers")
                    }
                }
            _refreshing.value = false
        }
    }
}
