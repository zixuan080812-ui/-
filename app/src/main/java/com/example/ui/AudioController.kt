package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay

object AudioHelper {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPath: String? = null
    private var onCompletionListener: (() -> Unit)? = null

    // For recorder
    private var activeRecordFile: File? = null

    fun startRecording(context: Context): File? {
        stopRecording()
        stopPlaying()

        try {
            val cacheDir = context.cacheDir
            val file = File.createTempFile("audio_rec_", ".m4a", cacheDir)
            activeRecordFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            return file
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to start recording", e)
            activeRecordFile = null
            mediaRecorder = null
            return null
        }
    }

    fun stopRecording(): File? {
        val file = activeRecordFile
        try {
            mediaRecorder?.let {
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to stop recording", e)
        } finally {
            mediaRecorder = null
            activeRecordFile = null
        }
        return file
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null
    }

    fun getAmplitude(): Float {
        return try {
            mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun playAudio(context: Context, filePath: String, onFinished: () -> Unit) {
        stopPlaying()

        try {
            val player = MediaPlayer()
            
            val file = if (filePath.startsWith("/")) {
                File(filePath)
            } else {
                File(context.cacheDir, filePath)
            }

            if (!file.exists()) {
                Log.e("AudioHelper", "Audio file does not exist: $filePath")
                onFinished()
                return
            }

            player.setDataSource(file.absolutePath)
            player.prepare()
            
            mediaPlayer = player
            currentPlayingPath = filePath
            onCompletionListener = onFinished

            player.setOnCompletionListener {
                stopPlaying()
            }

            player.start()
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to play audio", e)
            onFinished()
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioHelper", "Failed to stop media player", e)
        } finally {
            mediaPlayer = null
            currentPlayingPath = null
            onCompletionListener?.invoke()
            onCompletionListener = null
        }
    }

    fun isPlaying(filePath: String): Boolean {
        return currentPlayingPath == filePath && mediaPlayer?.isPlaying == true
    }
}

@Composable
fun AudioPlayBubble(
    audioContent: String, // format: "audio:filename.m4a|durationSecs"
    isSelf: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    bubbleTextColor: Color
) {
    // Parse content
    val parts = audioContent.substringAfter("audio:").split("|")
    val durationText = parts.getOrNull(1)?.let { "$it\"" } ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable { onTogglePlay() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Play / Pause Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelf) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPlaying) { "⏸" } else { "▶" },
                fontSize = 12.sp,
                color = if (isSelf) MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Minimalist wave visualization
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(72.dp)
        ) {
            val waveHeights = listOf(10, 16, 12, 18, 14, 8, 15, 11)
            waveHeights.forEachIndexed { idx, height ->
                val finalHeight = if (isPlaying) {
                    val transition = rememberInfiniteTransition(label = "")
                    val pulse by transition.animateFloat(
                        initialValue = 4f,
                        targetValue = height.toFloat(),
                        animationSpec = infiniteRepeatable(
                            animation = tween(300 + idx * 50, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = ""
                    )
                    pulse.dp
                } else {
                    (height / 2 + 3).dp
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(finalHeight)
                        .clip(RoundedCornerShape(1.dp))
                        .background(bubbleTextColor.copy(alpha = if (isPlaying) 0.8f else 0.4f))
                )
            }
        }

        // Duration Label
        Text(
            text = durationText,
            color = bubbleTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceRecorderPanel(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopAndSend: () -> Unit,
    onKeyboardMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var durationSecs by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            durationSecs = 0
            while (true) {
                delay(1000)
                durationSecs += 1
            }
        }
    }

    val m = if (durationSecs / 60 < 10) { "0${durationSecs / 60}" } else { "${durationSecs / 60}" }
    val s = if (durationSecs % 60 < 10) { "0${durationSecs % 60}" } else { "${durationSecs % 60}" }
    val formattedTime = "$m:$s"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isRecording) {
                // Not recording: Show keyboard back button and "Tap to start"
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onKeyboardMode,
                        modifier = Modifier.testTag("recorder_keyboard_button")
                    ) {
                        Text("⌨️", fontSize = 18.sp)
                    }
                    Text(
                        text = "切回键盘 · 语音留言",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("start_record_button")
                ) {
                    Text("🎙️ 开始录音", fontSize = 13.sp, color = Color.White)
                }
            } else {
                // Recording active! Show cancel, wave/timer, and stop&send
                IconButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.testTag("cancel_record_button")
                ) {
                    Text("🗑️", fontSize = 18.sp)
                }

                // Dynamic pulsing red dot and timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val transition = rememberInfiniteTransition(label = "")
                    val alphaScale by transition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = alphaScale))
                    )

                    Text(
                        text = "正在录音 $formattedTime",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onStopAndSend,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.testTag("send_record_button")
                ) {
                    Text("DONE 📣", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
