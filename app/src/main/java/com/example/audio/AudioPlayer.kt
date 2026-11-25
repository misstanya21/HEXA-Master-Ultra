package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

enum class SoundCategory {
  PLACEMENT,
  MERGE,
  BOOSTER,
  UI_REWARD
}

sealed class SoundEffect(val category: SoundCategory) {
  // 1. Placement Category
  data object TileTap : SoundEffect(SoundCategory.PLACEMENT)
  data object TileDrop : SoundEffect(SoundCategory.PLACEMENT)
  data object TileHover : SoundEffect(SoundCategory.PLACEMENT)
  data object InvalidPlacement : SoundEffect(SoundCategory.PLACEMENT)

  // 2. Merge & Cascade Category
  data class SlideWaveNote(val noteIndex: Int) : SoundEffect(SoundCategory.MERGE)
  data object StackMerge : SoundEffect(SoundCategory.MERGE)
  data object StackFullClear : SoundEffect(SoundCategory.MERGE)
  data class ComboStreak(val comboLevel: Int) : SoundEffect(SoundCategory.MERGE)

  // 3. Booster Category
  data object HammerSmash : SoundEffect(SoundCategory.BOOSTER)
  data object ShuffleDeck : SoundEffect(SoundCategory.BOOSTER)
  data object HintBeacon : SoundEffect(SoundCategory.BOOSTER)
  data object UndoMove : SoundEffect(SoundCategory.BOOSTER)

  // 4. UI & Reward Category
  data object CoinCollect : SoundEffect(SoundCategory.UI_REWARD)
  data object StarPop : SoundEffect(SoundCategory.UI_REWARD)
  data object DailyRewardJingle : SoundEffect(SoundCategory.UI_REWARD)
  data object VictoryFanfare : SoundEffect(SoundCategory.UI_REWARD)
  data object GameOverAlert : SoundEffect(SoundCategory.UI_REWARD)
  data object MissionComplete : SoundEffect(SoundCategory.UI_REWARD)
  data object MissionClaim : SoundEffect(SoundCategory.UI_REWARD)
}

/**
 * Studio Stereo SoundPool AudioPlayer backed by pre-mastered 44.1kHz stereo WAV
 * resources in res/raw/ for instant zero-latency playback with spatial stereo depth.
 */
class AudioPlayer(private val context: Context) {

  private val scope = CoroutineScope(Dispatchers.IO)

  private var masterVolume = 1.0f
  private var sfxEnabled = true
  private var musicEnabled = true
  private var hapticsEnabled = true

  private val categoryVolumes = mutableMapOf(
    SoundCategory.PLACEMENT to 1.0f,
    SoundCategory.MERGE to 1.0f,
    SoundCategory.BOOSTER to 1.0f,
    SoundCategory.UI_REWARD to 1.0f
  )

  private val vibrator: Vibrator? by lazy {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
          context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (_: Exception) {
      null
    }
  }

  private val loadedSoundIds = Collections.synchronizedSet(HashSet<Int>())
  private val lastPlayedElapsedMs = ConcurrentHashMap<String, Long>()

  private val soundPool: SoundPool? = try {
    SoundPool.Builder()
      .setMaxStreams(20)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .build().apply {
        setOnLoadCompleteListener { _, sampleId, status ->
          if (status == 0) {
            loadedSoundIds.add(sampleId)
          }
        }
      }
  } catch (_: Throwable) {
    null
  }

  private var soundTap = 0
  private var soundDrop = 0
  private var soundHover = 0
  private var soundInvalid = 0
  private var soundMerge = 0
  private var soundClear = 0
  private var soundHammer = 0
  private var soundShuffle = 0
  private var soundHint = 0
  private var soundUndo = 0
  private var soundCoin = 0
  private var soundStar = 0
  private var soundDaily = 0
  private var soundGameOver = 0
  private var soundMissionComplete = 0
  private var soundMissionClaim = 0
  private val soundSlideNotes = IntArray(8)
  private val soundCombos = IntArray(5)

  private var victoryMediaPlayer: MediaPlayer? = null
  private var bgmController: AudioController? = null

  init {
    preloadAllStudioSfx()
  }

  fun attachSynthesizer(controller: AudioController) {
    this.bgmController = controller
    controller.onTapPreview = { play(SoundEffect.TileTap) }
  }

  private fun loadRawSfx(resId: Int): Int {
    return try {
      soundPool?.load(context, resId, 1) ?: 0
    } catch (_: Throwable) {
      0
    }
  }

  private fun preloadAllStudioSfx() {
    scope.launch {
      try {
        soundTap = loadRawSfx(R.raw.sfx_tap)
        soundHover = loadRawSfx(R.raw.sfx_hover)
        soundDrop = loadRawSfx(R.raw.sfx_drop)
        soundInvalid = loadRawSfx(R.raw.sfx_invalid)
        soundMerge = loadRawSfx(R.raw.sfx_merge)
        soundClear = loadRawSfx(R.raw.sfx_clear)

        val slideRes = intArrayOf(
          R.raw.sfx_slide_0, R.raw.sfx_slide_1, R.raw.sfx_slide_2, R.raw.sfx_slide_3,
          R.raw.sfx_slide_4, R.raw.sfx_slide_5, R.raw.sfx_slide_6, R.raw.sfx_slide_7
        )
        for (i in slideRes.indices) {
          soundSlideNotes[i] = loadRawSfx(slideRes[i])
        }

        val comboRes = intArrayOf(
          R.raw.sfx_combo_1, R.raw.sfx_combo_2, R.raw.sfx_combo_3,
          R.raw.sfx_combo_4, R.raw.sfx_combo_5
        )
        for (i in comboRes.indices) {
          soundCombos[i] = loadRawSfx(comboRes[i])
        }

        soundHammer = loadRawSfx(R.raw.sfx_hammer)
        soundShuffle = loadRawSfx(R.raw.sfx_shuffle)
        soundHint = loadRawSfx(R.raw.sfx_hint)
        soundUndo = loadRawSfx(R.raw.sfx_undo)

        soundCoin = loadRawSfx(R.raw.sfx_coin)
        soundStar = loadRawSfx(R.raw.sfx_star)
        soundDaily = loadRawSfx(R.raw.sfx_daily)
        soundGameOver = loadRawSfx(R.raw.sfx_gameover)
        soundMissionComplete = loadRawSfx(R.raw.sfx_mission_comp)
        soundMissionClaim = loadRawSfx(R.raw.sfx_mission_claim)
      } catch (_: Throwable) {
      }
    }
  }

  private fun canTriggerWithCooldown(key: String, minIntervalMs: Long): Boolean {
    if (minIntervalMs <= 0L) return true
    val now = try { SystemClock.elapsedRealtime() } catch (_: Throwable) { return true }
    val prev = lastPlayedElapsedMs[key] ?: 0L
    if (now - prev < minIntervalMs) return false
    lastPlayedElapsedMs[key] = now
    return true
  }

  fun play(effect: SoundEffect, pitch: Float = 1.0f) {
    if (!sfxEnabled || masterVolume <= 0f) return

    val catVol = categoryVolumes[effect.category] ?: 1.0f
    val finalVolume = (masterVolume * catVol).coerceIn(0f, 1f)
    if (finalVolume <= 0f) return

    when (effect) {
      SoundEffect.TileTap -> {
        if (!canTriggerWithCooldown("tap", 28L)) return
        playSfx(soundTap, finalVolume, pitch)
        vibrate(10, 48)
      }
      SoundEffect.TileDrop -> {
        if (!canTriggerWithCooldown("drop", 35L)) return
        playSfx(soundDrop, finalVolume, pitch)
        vibrate(22, 92)
      }
      SoundEffect.TileHover -> {
        if (!canTriggerWithCooldown("hover", 42L)) return
        playSfx(soundHover, finalVolume * 0.78f, pitch)
        vibrate(6, 32)
      }
      SoundEffect.InvalidPlacement -> {
        if (!canTriggerWithCooldown("invalid", 90L)) return
        playSfx(soundInvalid, finalVolume, pitch)
        vibrate(32, 115)
      }
      is SoundEffect.SlideWaveNote -> {
        val idx = effect.noteIndex.coerceIn(0, 7)
        val soundId = soundSlideNotes.getOrNull(idx) ?: return
        playSfx(soundId, finalVolume * 0.94f, pitch)
        vibrate(7, 45)
      }
      SoundEffect.StackMerge -> {
        if (!canTriggerWithCooldown("merge", 45L)) return
        playSfx(soundMerge, finalVolume, pitch)
        vibrate(24, 105)
      }
      SoundEffect.StackFullClear -> {
        if (!canTriggerWithCooldown("clear", 60L)) return
        playSfx(soundClear, finalVolume, pitch)
        vibrate(45, 185)
      }
      is SoundEffect.ComboStreak -> {
        val idx = (effect.comboLevel - 1).coerceIn(0, 4)
        playSfx(soundCombos[idx], finalVolume, pitch)
        vibrate(35, 155)
      }
      SoundEffect.HammerSmash -> {
        playSfx(soundHammer, finalVolume, pitch)
        vibrate(70, 255)
      }
      SoundEffect.ShuffleDeck -> {
        playSfx(soundShuffle, finalVolume, pitch)
        vibrate(16, 78)
      }
      SoundEffect.HintBeacon -> {
        playSfx(soundHint, finalVolume, pitch)
        vibrate(20, 90)
      }
      SoundEffect.UndoMove -> {
        playSfx(soundUndo, finalVolume, pitch)
        vibrate(16, 72)
      }
      SoundEffect.CoinCollect -> {
        playSfx(soundCoin, finalVolume, pitch)
        vibrate(14, 65)
      }
      SoundEffect.StarPop -> {
        playSfx(soundStar, finalVolume, pitch)
        vibrate(18, 72)
      }
      SoundEffect.DailyRewardJingle -> {
        playSfx(soundDaily, finalVolume, pitch)
        vibrate(50, 150)
      }
      SoundEffect.VictoryFanfare -> {
        playVictoryFanfare(finalVolume)
        vibrate(80, 200)
      }
      SoundEffect.GameOverAlert -> {
        playSfx(soundGameOver, finalVolume, pitch)
        vibrate(60, 160)
      }
      SoundEffect.MissionComplete -> {
        playSfx(soundMissionComplete, finalVolume, pitch)
        vibrate(45, 140)
      }
      SoundEffect.MissionClaim -> {
        playSfx(soundMissionClaim, finalVolume, pitch)
        vibrate(55, 170)
      }
    }
  }

  private fun playSfx(soundId: Int, volume: Float, pitch: Float = 1.0f) {
    if (soundId != 0 && sfxEnabled && loadedSoundIds.contains(soundId)) {
      try {
        soundPool?.play(soundId, volume, volume, 1, 0, pitch.coerceIn(0.5f, 2.0f))
      } catch (_: Throwable) {
      }
    }
  }

  private fun playVictoryFanfare(volume: Float) {
    scope.launch {
      try {
        victoryMediaPlayer?.let { prev ->
          try {
            if (prev.isPlaying) prev.stop()
            prev.release()
          } catch (_: Exception) {}
        }
        val mp = MediaPlayer.create(context, R.raw.sfx_victory)
        if (mp != null) {
          mp.setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          mp.setVolume(volume, volume)
          mp.setOnCompletionListener { completed ->
            try {
              completed.release()
            } catch (_: Exception) {}
            if (victoryMediaPlayer === completed) {
              victoryMediaPlayer = null
            }
          }
          mp.start()
          victoryMediaPlayer = mp
        } else {
          playSfx(soundClear, volume, 1.0f)
        }
      } catch (_: Throwable) {
        playSfx(soundClear, volume, 1.0f)
      }
    }
  }

  fun setMasterVolume(vol: Float) {
    masterVolume = vol.coerceIn(0f, 1f)
  }

  fun setCategoryVolume(category: SoundCategory, vol: Float) {
    categoryVolumes[category] = vol.coerceIn(0f, 1f)
  }

  fun setSfxEnabled(enabled: Boolean) {
    sfxEnabled = enabled
  }

  fun setMusicEnabled(enabled: Boolean) {
    musicEnabled = enabled
    bgmController?.setMusicEnabled(enabled)
  }

  fun setMusicVolume(vol: Float) {
    bgmController?.setMusicVolume(vol)
  }

  fun setHapticsEnabled(enabled: Boolean) {
    hapticsEnabled = enabled
  }

  fun startBgm() {
    if (musicEnabled) {
      bgmController?.startBgm()
    }
  }

  fun stopBgm() {
    bgmController?.stopBgm()
  }

  fun playTap() = play(SoundEffect.TileTap)
  fun playDrop() = play(SoundEffect.TileDrop)
  fun playHover() = play(SoundEffect.TileHover)
  fun playInvalid() = play(SoundEffect.InvalidPlacement)
  fun playSlideNote(noteIndex: Int) = play(SoundEffect.SlideWaveNote(noteIndex))
  fun playMerge() = play(SoundEffect.StackMerge)
  fun playClear() = play(SoundEffect.StackFullClear)
  fun playCombo(comboLevel: Int) = play(SoundEffect.ComboStreak(comboLevel))
  fun playHammer() = play(SoundEffect.HammerSmash)
  fun playShuffle() = play(SoundEffect.ShuffleDeck)
  fun playHint() = play(SoundEffect.HintBeacon)
  fun playUndo() = play(SoundEffect.UndoMove)
  fun playCoin() = play(SoundEffect.CoinCollect)
  fun playStar() = play(SoundEffect.StarPop)
  fun playDailyReward() = play(SoundEffect.DailyRewardJingle)
  fun playVictory() = play(SoundEffect.VictoryFanfare)
  fun playGameOver() = play(SoundEffect.GameOverAlert)
  fun playMissionComplete() = play(SoundEffect.MissionComplete)
  fun playMissionClaim() = play(SoundEffect.MissionClaim)

  fun isSfxOn() = sfxEnabled
  fun isMusicOn() = musicEnabled
  fun isHapticsOn() = hapticsEnabled
  fun getMasterVolume() = masterVolume
  fun getCategoryVolume(cat: SoundCategory) = categoryVolumes[cat] ?: 1.0f

  fun release() {
    try {
      stopBgm()
      victoryMediaPlayer?.stop()
      victoryMediaPlayer?.release()
      victoryMediaPlayer = null
      soundPool?.release()
    } catch (_: Throwable) {}
  }

  private fun vibrate(durationMs: Long, amplitude: Int = 100) {
    if (!hapticsEnabled) return
    try {
      vibrator?.let { v ->
        if (v.hasVibrator()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(
              VibrationEffect.createOneShot(
                durationMs,
                amplitude.coerceIn(1, 255)
              )
            )
          } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
          }
        }
      }
    } catch (_: Exception) {
    }
  }
}
