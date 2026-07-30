package com.example.contentplayer.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    
    private var exoPlayer: ExoPlayer? = null
    val player: Player? get() = exoPlayer

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentVideoUrl: String = ""

    fun initializePlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                _isLoading.value = true
                            }
                            Player.STATE_READY -> {
                                _isLoading.value = false
                                _error.value = null
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                            }
                            Player.STATE_IDLE -> {
                                _isLoading.value = false
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                })
            }
        }
    }

    fun loadVideo(url: String) {
        currentVideoUrl = url
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                exoPlayer?.apply {
                    clearMediaItems()
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = true
                }
            } catch (e: Exception) {
                _error.value = "播放失败：${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            exoPlayer?.pause()
        } else {
            exoPlayer?.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceIn(0L, (exoPlayer?.duration ?: 0L)))
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    fun skipForward(seconds: Int = 10) {
        seekTo(getCurrentPosition() + seconds * 1000L)
    }

    fun skipBackward(seconds: Int = 10) {
        seekTo(getCurrentPosition() - seconds * 1000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }

    val availableSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
