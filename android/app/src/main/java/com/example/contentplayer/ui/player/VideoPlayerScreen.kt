package com.example.contentplayer.ui.player

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDrag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.contentplayer.ui.theme.ContentPlayerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    title: String,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val error by viewModel.error.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(videoUrl) {
        viewModel.initializePlayer(context)
        viewModel.loadVideo(videoUrl)
    }

    ContentPlayerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(showControls, isControlsLocked) {
                    if (!isControlsLocked) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width / 3) {
                                    viewModel.skipBackward(10)
                                } else if (offset.x > width * 2 / 3) {
                                    viewModel.skipForward(10)
                                }
                            }
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDrag { change, dragAmount ->
                        if (!isControlsLocked && showControls) {
                            viewModel.skipForward((dragAmount / 100).toInt())
                        }
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = false
                        controllerShowTimeoutMs = 0
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = error!!,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
            }

            if (!isControlsLocked) {
                TopBar(
                    title = title,
                    isVisible = showControls,
                    onNavigateBack = onNavigateBack
                )

                BottomControls(
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    currentPosition = viewModel.getCurrentPosition(),
                    duration = viewModel.getDuration(),
                    isVisible = showControls,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onSkipForward = { viewModel.skipForward(10) },
                    onSkipBackward = { viewModel.skipBackward(10) },
                    currentSpeed = playbackSpeed,
                    onSpeedChange = { speed ->
                        playbackSpeed = speed
                        viewModel.setPlaybackSpeed(speed)
                        showSpeedMenu = false
                    },
                    onToggleLock = { isControlsLocked = true },
                    onToggleSpeedMenu = { showSpeedMenu = !showSpeedMenu }
                )

                if (showSpeedMenu) {
                    SpeedMenu(
                        currentSpeed = playbackSpeed,
                        availableSpeeds = viewModel.availableSpeeds,
                        onSpeedSelect = { speed ->
                            playbackSpeed = speed
                            viewModel.setPlaybackSpeed(speed)
                            showSpeedMenu = false
                        },
                        onDismiss = { showSpeedMenu = false }
                    )
                }
            } else {
                LockIndicator(onClick = { isControlsLocked = false })
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    isVisible: Boolean,
    onNavigateBack: () -> Unit
) {
    val alpha = if (isVisible) 1f else 0f
    TopAppBar(
        title = {
            Text(
                title,
                maxLines = 1,
                color = Color.White,
                fontSize = 14.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.5f),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
    )
}

@Composable
private fun BottomControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPosition: Long,
    duration: Long,
    isVisible: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onToggleSpeedMenu: () -> Unit
) {
    val alpha = if (isVisible) 1f else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .alpha(alpha)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onSkipBackward) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "后退 10 秒",
                    tint = Color.White
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onSkipForward) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "前进 10 秒",
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 12.sp
            )
            
            Slider(
                value = currentPosition.toFloat(),
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                onValueChange = { onSeek(it.toLong()) },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentSpeed}x",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onToggleSpeedMenu() })
                    }
            )
            
            IconButton(onClick = onToggleLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "锁定",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(
    currentSpeed: Float,
    availableSpeeds: List<Float>,
    onSpeedSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .width(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "播放速度",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                availableSpeeds.forEach { speed ->
                    Button(
                        onClick = { onSpeedSelect(speed) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (speed == currentSpeed)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent
                        )
                    ) {
                        Text(
                            text = "${speed}x",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockIndicator(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onClick() })
            }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "解锁",
                tint = Color.White
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
