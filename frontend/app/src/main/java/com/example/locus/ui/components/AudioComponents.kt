package com.example.locus.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.locus.ui.theme.*
import java.io.File

// -- Audio player for a remote URL -------------------------------------------
@Composable
fun AudioPlayerBar(
    audioUrl: String,
    modifier: Modifier = Modifier,
    tintColor: Color = GoldPrimary,
    bgColor: Color = White.copy(alpha = 0.08f)
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    DisposableEffect(audioUrl) {
        onDispose {
            player.release()
        }
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Voice note",
            color = tintColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    if (isPrepared) {
                        player.start()
                        isPlaying = true
                    } else {
                        try {
                            player.setDataSource(audioUrl)
                            player.setOnPreparedListener {
                                isPrepared = true
                                player.start()
                                isPlaying = true
                            }
                            player.setOnCompletionListener {
                                isPlaying = false
                            }
                            player.prepareAsync()
                        } catch (e: Exception) {
                            android.util.Log.e("AudioPlayer", "Error: ${e.message}")
                        }
                    }
                }
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -- Inline mic button for recording -----------------------------------------
// Returns the recorded file via onRecordingDone; onCleared clears it.
@Composable
fun AudioRecorderButton(
    recordedFile: File?,
    onRecordingDone: (File) -> Unit,
    onCleared: () -> Unit,
    tintColor: Color = NavyDark,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentFile by remember { mutableStateOf<File?>(null) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File.createTempFile("audio_", ".m4a", context.cacheDir)
            currentFile = file
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            isRecording = true
        }
    }

    if (recordedFile != null) {
        // Show recorded indicator + delete option
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = {
                    onCleared()
                    recordedFile.delete()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove audio",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else if (isRecording) {
        // Recording in progress — tap to stop
        IconButton(
            onClick = {
                recorder?.apply { stop(); release() }
                recorder = null
                isRecording = false
                currentFile?.let { onRecordingDone(it) }
                currentFile = null
            },
            modifier = modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop recording",
                    tint = White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        // Idle — tap to start recording
        IconButton(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    val file = File.createTempFile("audio_", ".m4a", context.cacheDir)
                    currentFile = file
                    val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    rec.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    recorder = rec
                    isRecording = true
                } else {
                    permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Record audio",
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
