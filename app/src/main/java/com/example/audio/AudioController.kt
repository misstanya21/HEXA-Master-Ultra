package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.R
import com.example.model.GameMode
import com.example.model.ScreenState

enum class BgmTrack(val rawResId: Int) {
  MENU(R.raw.bgm_menu),
  CLASSIC(R.raw.bgm_classic),
  TIME_ATTACK(R.raw.bgm_time_attack),
  ENDLESS(R.raw.bgm_endless)
}

/**
 * Multi-track Studio Stereo BGM engine backed by pre-mastered 44.1kHz PCM WAV loops
 * in res/raw/ with seamless looping, dynamic track switching per mode, and safe
 * JVM/Robolectric guards.
 */
class AudioController(private val context: Context) {

  private var isSoundEnabled = true
  private var isMusicEnabled = true
  private var isHapticsEnabled = true

  private var soundVolume = 1.0f
  private var musicVolume = 0.55f

  private var currentTrack: BgmTrack = BgmTrack.MENU
  private var mediaPlayer: MediaPlayer? = null
  var onTapPreview: (() -> Unit)? = null

  init {
    startBgm()
  }

  fun playTap() {
    if (isSoundEnabled) {
      onTapPreview?.invoke()
    }
  }

  fun setSoundEnabled(enabled: Boolean) {
    isSoundEnabled = enabled
  }

  fun setMusicEnabled(enabled: Boolean) {
    isMusicEnabled = enabled
    if (enabled) {
      startBgm()
    } else {
      stopBgm()
    }
  }

  fun setHapticsEnabled(enabled: Boolean) {
    isHapticsEnabled = enabled
  }

  fun setSoundVolume(volume: Float) {
    soundVolume = volume.coerceIn(0f, 1f)
  }

  fun setMusicVolume(volume: Float) {
    musicVolume = volume.coerceIn(0f, 1f)
    applyPlayerVolume()
  }

  fun isSoundOn() = isSoundEnabled
  fun isMusicOn() = isMusicEnabled
  fun isHapticsOn() = isHapticsEnabled
  fun getSoundVolume() = soundVolume
  fun getMusicVolume() = musicVolume

  fun updateTrackForContext(screen: ScreenState, mode: GameMode) {
    val target = when (screen) {
      ScreenState.GAME -> when (mode) {
        GameMode.CLASSIC -> BgmTrack.CLASSIC
        GameMode.TIME_ATTACK -> BgmTrack.TIME_ATTACK
        GameMode.ENDLESS -> BgmTrack.ENDLESS
      }
      else -> BgmTrack.MENU
    }
    setTrack(target)
  }

  fun setTrack(track: BgmTrack) {
    if (currentTrack == track && mediaPlayer != null) return
    currentTrack = track
    if (isMusicEnabled) {
      stopBgm()
      startBgm()
    }
  }

  @Synchronized
  fun startBgm() {
    if (!isMusicEnabled) return
    val existing = mediaPlayer
    if (existing != null) {
      try {
        if (!existing.isPlaying) {
          applyPlayerVolume()
          existing.start()
        }
        return
      } catch (_: Exception) {
        releasePlayerSafely()
      }
    }

    try {
      val mp = MediaPlayer.create(context, currentTrack.rawResId) ?: return
      mp.setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      )
      mp.isLooping = true
      val vol = (musicVolume * 0.65f).coerceIn(0f, 1f)
      mp.setVolume(vol, vol)
      mp.setOnErrorListener { _, _, _ ->
        releasePlayerSafely()
        true
      }
      mp.start()
      mediaPlayer = mp
    } catch (_: Throwable) {
      // Safe fallback under Robolectric / headless unit tests
      mediaPlayer = null
    }
  }

  private fun applyPlayerVolume() {
    try {
      val vol = (musicVolume * 0.65f).coerceIn(0f, 1f)
      mediaPlayer?.setVolume(vol, vol)
    } catch (_: Exception) {
    }
  }

  @Synchronized
  fun stopBgm() {
    releasePlayerSafely()
  }

  private fun releasePlayerSafely() {
    mediaPlayer?.let { mp ->
      try {
        if (mp.isPlaying) mp.stop()
      } catch (_: Exception) {
      }
      try {
        mp.release()
      } catch (_: Exception) {
      }
    }
    mediaPlayer = null
  }

  fun release() {
    stopBgm()
  }
}
