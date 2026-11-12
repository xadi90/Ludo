package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Self-contained low-latency synthesized PCM audio & haptic feedback engine.
 * Generates dice rattle, token hop, sword slash cut, power activation chime,
 * victory fanfare, defeat tone, button pop, notification ping, and soft ambient BGM
 * with independent volume and toggle controls.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile var musicEnabled: Boolean = true
    @Volatile var sfxEnabled: Boolean = true
    @Volatile var vibrationEnabled: Boolean = true
    @Volatile var musicVolume: Float = 0.45f
    @Volatile var sfxVolume: Float = 0.85f

    private var musicTrack: AudioTrack? = null
    private var musicLoopJob: Job? = null
    private var isPaused: Boolean = false

    private val sampleRate = 22050

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    fun applySettings(
        musicOn: Boolean,
        sfxOn: Boolean,
        vibeOn: Boolean,
        musicVol: Float,
        sfxVol: Float
    ) {
        musicEnabled = musicOn
        sfxEnabled = sfxOn
        vibrationEnabled = vibeOn
        musicVolume = musicVol.coerceIn(0f, 1f)
        sfxVolume = sfxVol.coerceIn(0f, 1f)
        updateMusicPlayback()
    }

    fun onResume() {
        isPaused = false
        updateMusicPlayback()
    }

    fun onPause() {
        isPaused = true
        stopMusicInternal()
    }

    fun release() {
        stopMusicInternal()
        scope.cancel()
    }

    private fun updateMusicPlayback() {
        if (musicEnabled && !isPaused && musicVolume > 0.01f) {
            startMusicIfNeeded()
            try {
                musicTrack?.setVolume(musicVolume * 0.28f)
            } catch (_: Exception) {
            }
        } else {
            stopMusicInternal()
        }
    }

    private fun startMusicIfNeeded() {
        if (musicLoopJob?.isActive == true) return
        musicLoopJob = scope.launch {
            try {
                val loopSamples = buildAmbientMusicBuffer()
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(loopSamples.size * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                musicTrack = track
                track.setVolume(musicVolume * 0.28f)
                track.play()

                while (isActive && musicEnabled && !isPaused) {
                    track.write(loopSamples, 0, loopSamples.size)
                }
            } catch (_: Exception) {
                // Gracefully degrade on headless/test devices without audio hardware
            }
        }
    }

    private fun stopMusicInternal() {
        musicLoopJob?.cancel()
        musicLoopJob = null
        try {
            musicTrack?.pause()
            musicTrack?.flush()
            musicTrack?.release()
        } catch (_: Exception) {
        }
        musicTrack = null
    }

    fun playButtonClick() {
        playToneBuffer(synthesizeBlip(freqHz = 680.0, durationMs = 45, decay = 22.0))
    }

    fun playDiceRoll() {
        vibrateMs(30)
        scope.launch {
            playBufferDirect(synthesizeDiceRattle())
        }
    }

    fun playTokenMove() {
        vibrateMs(15)
        playToneBuffer(synthesizeBlip(freqHz = 520.0, durationMs = 65, decay = 18.0))
    }

    fun playTokenCut() {
        vibrateMs(85)
        playToneBuffer(synthesizeSwordCut())
    }

    fun playPowerActivate() {
        vibrateMs(45)
        playToneBuffer(synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.5), noteMs = 55))
    }

    fun playNotification() {
        playToneBuffer(synthesizeArpeggio(listOf(880.0, 1174.66), noteMs = 65))
    }

    fun playWinFanfare() {
        vibrateMs(120)
        playToneBuffer(synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), noteMs = 95))
    }

    fun playLoseTone() {
        playToneBuffer(synthesizeArpeggio(listOf(392.0, 349.23, 293.66), noteMs = 130))
    }

    private fun vibrateMs(ms: Long) {
        if (!vibrationEnabled) return
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }

    private fun playToneBuffer(samples: ShortArray) {
        if (!sfxEnabled || sfxVolume <= 0.01f) return
        scope.launch {
            playBufferDirect(samples)
        }
    }

    private suspend fun playBufferDirect(samples: ShortArray) {
        if (!sfxEnabled || sfxVolume <= 0.01f) return
        var track: AudioTrack? = null
        try {
            val byteSize = (samples.size * 2).coerceAtLeast(
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.setVolume(sfxVolume)
            track.play()
            delay((samples.size * 1000L / sampleRate) + 40L)
        } catch (_: Exception) {
        } finally {
            try {
                track?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun synthesizeBlip(freqHz: Double, durationMs: Int, decay: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val env = exp(-decay * t)
            val wave = sin(2.0 * PI * freqHz * t)
            (wave * env * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun synthesizeDiceRattle(): ShortArray {
        val count = (sampleRate * 260) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val clickPulse = if ((i % 900) < 160) 1.0 else 0.2
            val noise = (Random.nextDouble() * 2.0 - 1.0) * clickPulse
            val thud = sin(2.0 * PI * 190.0 * t) * exp(-10.0 * t)
            out[i] = ((noise * 0.55 + thud * 0.45) * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthesizeSwordCut(): ShortArray {
        val count = (sampleRate * 280) / 1000
        return ShortArray(count) { i ->
            val t = i.toDouble() / sampleRate
            val sweepFreq = 1200.0 * exp(-6.0 * t) + 140.0
            val metallic = sin(2.0 * PI * sweepFreq * t) + 0.5 * sin(2.0 * PI * sweepFreq * 1.48 * t)
            val env = exp(-8.0 * t)
            (metallic * env * 14000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private fun synthesizeArpeggio(freqs: List<Double>, noteMs: Int): ShortArray {
        val noteSamples = (sampleRate * noteMs) / 1000
        val total = noteSamples * freqs.size
        val out = ShortArray(total)
        freqs.forEachIndexed { idx, freq ->
            for (j in 0 until noteSamples) {
                val t = j.toDouble() / sampleRate
                val env = exp(-9.0 * t)
                val wave = sin(2.0 * PI * freq * t)
                out[idx * noteSamples + j] = (wave * env * 16500.0).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun buildAmbientMusicBuffer(): ShortArray {
        // Gentle warm 4-chord ambient pad loop (~3.2 seconds)
        val chords = listOf(
            listOf(261.63, 329.63, 392.00), // C major
            listOf(220.00, 261.63, 329.63), // A minor
            listOf(174.61, 220.00, 261.63), // F major
            listOf(196.00, 246.94, 293.66)  // G major
        )
        val chordSamples = (sampleRate * 800) / 1000
        val out = ShortArray(chordSamples * chords.size)
        chords.forEachIndexed { cIdx, notes ->
            for (i in 0 until chordSamples) {
                val progress = i.toDouble() / chordSamples
                val smoothEnv = sin(PI * progress).coerceIn(0.0, 1.0)
                val t = i.toDouble() / sampleRate
                var mix = 0.0
                notes.forEach { f ->
                    mix += sin(2.0 * PI * f * t)
                }
                mix /= notes.size
                out[cIdx * chordSamples + i] =
                    (mix * smoothEnv * 9000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }
}
