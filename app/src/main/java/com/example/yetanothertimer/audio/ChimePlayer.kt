package com.example.yetanothertimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ChimePlayer {
    // Try to play a provided asset named "chime" with common extensions. Fallback to synthesized chime if asset not found.
    suspend fun playCustomOrFallback(context: Context, baseName: String = "chime") {
        val exts = listOf(".mp3", ".wav", ".ogg")
        var played = false
        for (ext in exts) {
            val fileName = baseName + ext
            try {
                context.assets.openFd(fileName).use { afd ->
                    withContext(Dispatchers.Default) {
                        val mp = MediaPlayer()
                        try {
                            mp.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            mp.setVolume(1.0f, 1.0f)
                            mp.prepare()
                            mp.start()
                            val dur = mp.duration.takeIf { it > 0 } ?: 350
                            delay(dur.toLong() + 50)
                        } finally {
                            kotlin.runCatching { mp.stop() }
                            mp.release()
                        }
                    }
                    played = true
                }
                break
            } catch (_: Exception) {
                // try next extension
            }
        }
        if (!played) {
            playSynth()
        }
    }

    // Generate and play a short crisp chime (<= 0.5s) using a simple dual-tone with envelope
    // Runs off the main thread and waits for playback to complete before releasing the track
    private suspend fun playSynth(durationMs: Int = 350, sampleRate: Int = 44100) = withContext(Dispatchers.Default) {
        val dur = durationMs.coerceIn(50, 500)
        val totalSamples = (sampleRate * (dur / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        // Dual tone: A4 (440 Hz) + A5 (880 Hz) with a quick fade-in/out envelope
        val f1 = 440.0
        val f2 = 880.0
        val twoPi = 2.0 * Math.PI
        val attackSamples = (sampleRate * 0.01).toInt().coerceAtLeast(1) // 10ms
        val releaseSamples = (sampleRate * 0.05).toInt().coerceAtLeast(1) // 50ms

        for (i in 0 until totalSamples) {
            val t = i / sampleRate.toDouble()
            val raw = Math.sin(twoPi * f1 * t) + 0.6 * Math.sin(twoPi * f2 * t)

            // Envelope
            val env = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > totalSamples - releaseSamples ->
                    ((totalSamples - i).toDouble() / releaseSamples).coerceAtLeast(0.0)
                else -> 1.0
            }
            val sample = (raw * 0.35 * env)
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        // Use MODE_STATIC so the whole buffer is preloaded and then played reliably
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val bufferSizeBytes = buffer.size * 2
        val audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSizeBytes,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Wait for playback to complete before stopping/releasing
            delay(dur.toLong() + 50)
        } finally {
            kotlin.runCatching { audioTrack.stop() }
            audioTrack.release()
        }
    }
}