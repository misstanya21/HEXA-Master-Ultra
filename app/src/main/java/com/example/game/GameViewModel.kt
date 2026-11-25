package com.example.game

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdConfig
import com.example.ads.AdManager
import com.example.ads.AdPlacement
import com.example.audio.AudioController
import com.example.audio.AudioPlayer
import com.example.audio.SoundCategory
import com.example.audio.SoundEffect
import com.example.model.Achievement
import com.example.model.AchievementMetric
import com.example.model.ActiveSlide
import com.example.model.BoardSnapshot
import com.example.model.BoosterType
import com.example.model.CellObstacle
import com.example.model.DailyMission
import com.example.model.EngagementCatalog
import com.example.model.FloatingText
import com.example.model.GameMode
import com.example.model.HexCell
import com.example.model.HexCoord
import com.example.model.HexTile
import com.example.model.HintRecommendation
import com.example.model.MissionReward
import com.example.model.MissionType
import com.example.model.Particle
import com.example.model.ParticleShape
import com.example.model.PlayerCloudSave
import com.example.model.PlayerProfileStats
import com.example.model.ScreenState
import com.example.model.StagedStack
import com.example.model.TileColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.sin

class GameViewModel(application: Application) : AndroidViewModel(application) {

  val audio = AudioController(application.applicationContext)
  val audioPlayer = AudioPlayer(application.applicationContext).apply {
    attachSynthesizer(audio)
  }
  private val prefs = application.getSharedPreferences("hexa_master_ultra_prefs", Context.MODE_PRIVATE)

  // Current & Highest Level
  private val _currentLevel = MutableStateFlow(prefs.getInt("current_level", 1))
  val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

  private val _highestLevel = MutableStateFlow(
    maxOf(prefs.getInt("highest_level", 1), _currentLevel.value)
  )
  val highestLevel: StateFlow<Int> = _highestLevel.asStateFlow()

  private val _careerTotalScore = MutableStateFlow(prefs.getLong("career_total_score", 0L))
  val careerTotalScore: StateFlow<Long> = _careerTotalScore.asStateFlow()

  private val _cloudSyncTrigger = MutableStateFlow(0)
  val cloudSyncTrigger: StateFlow<Int> = _cloudSyncTrigger.asStateFlow()

  // Game Modes (Classic Levels, 90s Time Attack, Endless Survival)
  private val _selectedGameMode = MutableStateFlow(GameMode.CLASSIC)
  val selectedGameMode: StateFlow<GameMode> = _selectedGameMode.asStateFlow()

  private val _activeGameMode = MutableStateFlow(GameMode.CLASSIC)
  val activeGameMode: StateFlow<GameMode> = _activeGameMode.asStateFlow()

  private val _timeRemainingSec = MutableStateFlow(TIME_ATTACK_DURATION_SEC)
  val timeRemainingSec: StateFlow<Int> = _timeRemainingSec.asStateFlow()

  private val _isTimeUp = MutableStateFlow(false)
  val isTimeUp: StateFlow<Boolean> = _isTimeUp.asStateFlow()

  private val _timeAttackCoinsEarned = MutableStateFlow(0)
  val timeAttackCoinsEarned: StateFlow<Int> = _timeAttackCoinsEarned.asStateFlow()

  private val _isNewBestScore = MutableStateFlow(false)
  val isNewBestScore: StateFlow<Boolean> = _isNewBestScore.asStateFlow()

  private val _timeAttackBestScore = MutableStateFlow(prefs.getInt("best_time_attack", 0))
  val timeAttackBestScore: StateFlow<Int> = _timeAttackBestScore.asStateFlow()

  private val _endlessBestScore = MutableStateFlow(prefs.getInt("best_endless", 0))
  val endlessBestScore: StateFlow<Int> = _endlessBestScore.asStateFlow()

  private val _endlessWave = MutableStateFlow(1)
  val endlessWave: StateFlow<Int> = _endlessWave.asStateFlow()

  private var endlessClearedStacks = 0
  private var timeAttackTimerJob: Job? = null

  // Navigation State
  private val _currentScreen = MutableStateFlow(com.example.model.ScreenState.LOADING)
  val currentScreen: StateFlow<com.example.model.ScreenState> = _currentScreen.asStateFlow()

  fun navigateTo(screen: com.example.model.ScreenState) {
    _currentScreen.value = screen
    if (screen != ScreenState.GAME) {
      timeAttackTimerJob?.cancel()
      _isPaused.value = false
    }
    audio.updateTrackForContext(screen, _activeGameMode.value)
    audioPlayer.play(SoundEffect.TileTap)
  }

  fun selectGameMode(mode: GameMode) {
    _selectedGameMode.value = mode
    audioPlayer.play(SoundEffect.TileTap, pitch = 1.05f)
  }

  fun startSelectedMode() {
    when (_selectedGameMode.value) {
      GameMode.CLASSIC -> loadLevel(_currentLevel.value)
      GameMode.TIME_ATTACK -> startTimeAttack()
      GameMode.ENDLESS -> startEndlessMode()
    }
    _currentScreen.value = ScreenState.GAME
    audio.updateTrackForContext(ScreenState.GAME, _activeGameMode.value)
    audioPlayer.play(SoundEffect.TileTap)
  }

  fun finishLoading() {
    if (_currentScreen.value == com.example.model.ScreenState.LOADING) {
      _currentScreen.value = com.example.model.ScreenState.MENU
      audio.updateTrackForContext(ScreenState.MENU, _activeGameMode.value)
    }
  }

  private val _levelDef = MutableStateFlow(GameLevels.getLevel(_currentLevel.value))
  val levelDef: StateFlow<LevelDefinition> = _levelDef.asStateFlow()

  // Score, Coins & Gems
  private val _coins = MutableStateFlow(prefs.getInt("coins", 450))
  val coins: StateFlow<Int> = _coins.asStateFlow()

  private val _gems = MutableStateFlow(prefs.getInt("gems", 30))
  val gems: StateFlow<Int> = _gems.asStateFlow()

  private val _score = MutableStateFlow(0)
  val score: StateFlow<Int> = _score.asStateFlow()

  private val _combo = MutableStateFlow(1)
  val combo: StateFlow<Int> = _combo.asStateFlow()

  // Goals
  private val _remainingGoal = MutableStateFlow(35)
  val remainingGoal: StateFlow<Int> = _remainingGoal.asStateFlow()

  private val _secondaryGoalRemaining = MutableStateFlow(0)
  val secondaryGoalRemaining: StateFlow<Int> = _secondaryGoalRemaining.asStateFlow()

  // Board State
  private val _boardCells = MutableStateFlow<Map<HexCoord, HexCell>>(emptyMap())
  val boardCells: StateFlow<Map<HexCoord, HexCell>> = _boardCells.asStateFlow()

  // Staging Stacks (3 bottom pedestals)
  private val _stagedStacks = MutableStateFlow<List<StagedStack?>>(listOf(null, null, null))
  val stagedStacks: StateFlow<List<StagedStack?>> = _stagedStacks.asStateFlow()

  private val _selectedStagedIndex = MutableStateFlow<Int?>(null)
  val selectedStagedIndex: StateFlow<Int?> = _selectedStagedIndex.asStateFlow()

  // Drag & Drop State (<100ms latency)
  private val _draggedStack = MutableStateFlow<StagedStack?>(null)
  val draggedStack: StateFlow<StagedStack?> = _draggedStack.asStateFlow()

  private val _dragPosition = MutableStateFlow<Offset?>(null)
  val dragPosition: StateFlow<Offset?> = _dragPosition.asStateFlow()

  private val _hoveredCoord = MutableStateFlow<HexCoord?>(null)
  val hoveredCoord: StateFlow<HexCoord?> = _hoveredCoord.asStateFlow()

  private val _snapStrength = MutableStateFlow(0f)
  val snapStrength: StateFlow<Float> = _snapStrength.asStateFlow()

  private var hasDraggedOntoBoard = false
  private var dragVerticalOffsetPx = 220f

  // Boosters
  private val _activeBooster = MutableStateFlow<BoosterType?>(null)
  val activeBooster: StateFlow<BoosterType?> = _activeBooster.asStateFlow()

  private val _boosterCounts = MutableStateFlow<Map<BoosterType, Int>>(
    mapOf(
      BoosterType.UNDO to prefs.getInt("booster_undo", 3),
      BoosterType.SHUFFLE to prefs.getInt("booster_shuffle", 2),
      BoosterType.HINT to prefs.getInt("booster_hint", 3),
      BoosterType.HAMMER to prefs.getInt("booster_hammer", 3),
      BoosterType.PINWHEEL to prefs.getInt("booster_pinwheel", 5)
    )
  )
  val boosterCounts: StateFlow<Map<BoosterType, Int>> = _boosterCounts.asStateFlow()

  // Undo History Stack
  private val undoHistory = ArrayDeque<BoardSnapshot>()
  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  // Active Hint
  private val _activeHint = MutableStateFlow<HintRecommendation?>(null)
  val activeHint: StateFlow<HintRecommendation?> = _activeHint.asStateFlow()

  // Visual Effects
  private val _activeSlides = MutableStateFlow<List<ActiveSlide>>(emptyList())
  val activeSlides: StateFlow<List<ActiveSlide>> = _activeSlides.asStateFlow()

  private val _particles = MutableStateFlow<List<Particle>>(emptyList())
  val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

  private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
  val floatingTexts: StateFlow<List<FloatingText>> = _floatingTexts.asStateFlow()

  // Performance Mode (Low-end device optimization)
  private val _isLowPerformanceMode = MutableStateFlow(prefs.getBoolean("low_perf_mode", false))
  val isLowPerformanceMode: StateFlow<Boolean> = _isLowPerformanceMode.asStateFlow()

  // Daily Rewards System
  private val _consecutiveDays = MutableStateFlow(prefs.getInt("consecutive_days", 1))
  val consecutiveDays: StateFlow<Int> = _consecutiveDays.asStateFlow()

  private val _canClaimDailyReward = MutableStateFlow(checkDailyRewardAvailable())
  val canClaimDailyReward: StateFlow<Boolean> = _canClaimDailyReward.asStateFlow()

  // Daily Missions System
  private val _dailyMissions = MutableStateFlow<List<DailyMission>>(emptyList())
  val dailyMissions: StateFlow<List<DailyMission>> = _dailyMissions.asStateFlow()

  private val _isGrandChestClaimed = MutableStateFlow(false)
  val isGrandChestClaimed: StateFlow<Boolean> = _isGrandChestClaimed.asStateFlow()

  private val _unclaimedMissionCount = MutableStateFlow(0)
  val unclaimedMissionCount: StateFlow<Int> = _unclaimedMissionCount.asStateFlow()

  // Achievements & Player Profile Trophy System
  private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
  val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

  private val _unclaimedAchievementCount = MutableStateFlow(0)
  val unclaimedAchievementCount: StateFlow<Int> = _unclaimedAchievementCount.asStateFlow()

  private val _profileStats = MutableStateFlow(PlayerProfileStats())
  val profileStats: StateFlow<PlayerProfileStats> = _profileStats.asStateFlow()

  // Monetization / Ad Simulation
  private val _hasNoAds = MutableStateFlow(prefs.getBoolean("has_no_ads", false))
  val hasNoAds: StateFlow<Boolean> = _hasNoAds.asStateFlow()

  // Colorblind accessibility mode
  private val _isColorblindMode = MutableStateFlow(prefs.getBoolean("colorblind_mode", false))
  val isColorblindMode: StateFlow<Boolean> = _isColorblindMode.asStateFlow()

  // Screen shake and hammer strike VFX
  private val _screenShake = MutableStateFlow(Offset.Zero)
  val screenShake: StateFlow<Offset> = _screenShake.asStateFlow()

  private val _hammerStrikePos = MutableStateFlow<Offset?>(null)
  val hammerStrikePos: StateFlow<Offset?> = _hammerStrikePos.asStateFlow()

  private var levelsCompletedCounter = 0
  private var gameOverRetriesCounter = 0
  private val _showInterstitialAd = MutableStateFlow(false)
  val showInterstitialAd: StateFlow<Boolean> = _showInterstitialAd.asStateFlow()

  val isAdSlotActive: StateFlow<Boolean> = AdManager.isAdSlotActive
  val activeAdPlacement: StateFlow<AdPlacement?> = AdManager.activePlacement

  // Game Dialogs / State
  private val _isLevelComplete = MutableStateFlow(false)
  val isLevelComplete: StateFlow<Boolean> = _isLevelComplete.asStateFlow()

  private val _isGameOver = MutableStateFlow(false)
  val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

  private val _isPaused = MutableStateFlow(false)
  val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

  private var boardCenter = Offset(500f, 600f)
  private var hexRadius = 48f
  private var isCascading = false
  private var cascadeJob: Job? = null

  init {
    loadLevel(_currentLevel.value)
    initDailyMissions()
    initAchievements()
    // Apply saved sound preferences
    val sfxVol = prefs.getFloat("sound_volume", 1.0f)
    val musVol = prefs.getFloat("music_volume", 0.6f)
    val sndOn = prefs.getBoolean("sound_enabled", true)
    val musOn = prefs.getBoolean("music_enabled", true)
    val haptOn = prefs.getBoolean("haptics_enabled", true)

    audio.setSoundVolume(sfxVol)
    audio.setMusicVolume(musVol)
    audio.setSoundEnabled(sndOn)
    audio.setMusicEnabled(musOn)
    audio.setHapticsEnabled(haptOn)

    audioPlayer.setMasterVolume(sfxVol)
    audioPlayer.setMusicVolume(musVol)
    audioPlayer.setSfxEnabled(sndOn)
    audioPlayer.setMusicEnabled(musOn)
    audioPlayer.setHapticsEnabled(haptOn)
  }

  fun updateBoardGeometry(center: Offset, radius: Float, dragOffsetPx: Float = dragVerticalOffsetPx) {
    this.boardCenter = center
    this.hexRadius = radius
    this.dragVerticalOffsetPx = dragOffsetPx.coerceAtLeast(160f)
  }

  fun loadLevel(levelNum: Int) {
    cascadeJob?.cancel()
    timeAttackTimerJob?.cancel()
    isCascading = false
    _activeGameMode.value = GameMode.CLASSIC
    _selectedGameMode.value = GameMode.CLASSIC
    _currentLevel.value = levelNum
    prefs.edit().putInt("current_level", levelNum).apply()
    if (levelNum > _highestLevel.value) {
      _highestLevel.value = levelNum
      prefs.edit().putInt("highest_level", levelNum).apply()
    }

    val def = GameLevels.getLevel(levelNum)
    _levelDef.value = def
    _remainingGoal.value = def.targetGoalCount
    _secondaryGoalRemaining.value = def.secondaryGoalCount
    _boardCells.value = def.initialCells.toMutableMap()
    _selectedStagedIndex.value = null
    _activeBooster.value = null
    _activeHint.value = null
    _activeSlides.value = emptyList()
    _particles.value = emptyList()
    _floatingTexts.value = emptyList()
    _isLevelComplete.value = false
    _isGameOver.value = false
    _isTimeUp.value = false
    _isNewBestScore.value = false
    _isPaused.value = false
    _combo.value = 1
    undoHistory.clear()
    _canUndo.value = false

    replenishStagedStacks(force = true)
    if (_currentScreen.value == ScreenState.GAME) {
      audio.updateTrackForContext(ScreenState.GAME, GameMode.CLASSIC)
    }
  }

  fun startTimeAttack() {
    cascadeJob?.cancel()
    timeAttackTimerJob?.cancel()
    isCascading = false
    _activeGameMode.value = GameMode.TIME_ATTACK
    _selectedGameMode.value = GameMode.TIME_ATTACK

    val def = GameLevels.createTimeAttackBoard()
    _levelDef.value = def
    _score.value = 0
    _remainingGoal.value = 0
    _secondaryGoalRemaining.value = 0
    _timeRemainingSec.value = TIME_ATTACK_DURATION_SEC
    _timeAttackCoinsEarned.value = 0
    _isNewBestScore.value = false
    _boardCells.value = def.initialCells.toMutableMap()
    _selectedStagedIndex.value = null
    _activeBooster.value = null
    _activeHint.value = null
    _activeSlides.value = emptyList()
    _particles.value = emptyList()
    _floatingTexts.value = emptyList()
    _isLevelComplete.value = false
    _isGameOver.value = false
    _isTimeUp.value = false
    _isPaused.value = false
    _combo.value = 1
    undoHistory.clear()
    _canUndo.value = false

    replenishStagedStacks(force = true)
    startTimeAttackTimer()
    if (_currentScreen.value == ScreenState.GAME) {
      audio.updateTrackForContext(ScreenState.GAME, GameMode.TIME_ATTACK)
    }
  }

  fun startEndlessMode() {
    cascadeJob?.cancel()
    timeAttackTimerJob?.cancel()
    isCascading = false
    _activeGameMode.value = GameMode.ENDLESS
    _selectedGameMode.value = GameMode.ENDLESS

    val def = GameLevels.createEndlessBoard()
    _levelDef.value = def
    _score.value = 0
    _remainingGoal.value = 0
    _secondaryGoalRemaining.value = 0
    _endlessWave.value = 1
    endlessClearedStacks = 0
    _isNewBestScore.value = false
    _boardCells.value = def.initialCells.toMutableMap()
    _selectedStagedIndex.value = null
    _activeBooster.value = null
    _activeHint.value = null
    _activeSlides.value = emptyList()
    _particles.value = emptyList()
    _floatingTexts.value = emptyList()
    _isLevelComplete.value = false
    _isGameOver.value = false
    _isTimeUp.value = false
    _isPaused.value = false
    _combo.value = 1
    undoHistory.clear()
    _canUndo.value = false

    replenishStagedStacks(force = true)
    if (_currentScreen.value == ScreenState.GAME) {
      audio.updateTrackForContext(ScreenState.GAME, GameMode.ENDLESS)
    }
  }

  private fun startTimeAttackTimer() {
    timeAttackTimerJob?.cancel()
    timeAttackTimerJob = viewModelScope.launch {
      while (_activeGameMode.value == GameMode.TIME_ATTACK && !_isTimeUp.value) {
        delay(1000L)
        if (_currentScreen.value == ScreenState.GAME && !_isPaused.value && !_isGameOver.value && !_isTimeUp.value) {
          val next = (_timeRemainingSec.value - 1).coerceAtLeast(0)
          _timeRemainingSec.value = next
          if (next in 1..10) {
            audioPlayer.play(SoundEffect.TileTap, pitch = 1.35f)
          }
          if (next == 0) {
            completeTimeAttack()
            break
          }
        }
      }
    }
  }

  private fun completeTimeAttack() {
    _draggedStack.value = null
    _dragPosition.value = null
    _hoveredCoord.value = null
    _snapStrength.value = 0f
    hasDraggedOntoBoard = false

    val finalScore = _score.value
    val prevBest = _timeAttackBestScore.value
    if (finalScore > prevBest) {
      _timeAttackBestScore.value = finalScore
      prefs.edit().putInt("best_time_attack", finalScore).apply()
      _isNewBestScore.value = finalScore > 0
    } else {
      _isNewBestScore.value = false
    }

    val coinsEarned = if (finalScore > 0) maxOf(15, finalScore / 40) else 0
    _timeAttackCoinsEarned.value = coinsEarned
    if (coinsEarned > 0) {
      addCoins(coinsEarned)
    }

    _isTimeUp.value = true
    audioPlayer.play(SoundEffect.VictoryFanfare)
    if (!_isLowPerformanceMode.value) {
      launchComboFireworks(comboLevel = 5)
      triggerScreenShake(intensity = 0.85f)
    }
    requestCloudSync()
  }

  private fun recordModeScore(pointsGained: Int) {
    val newScore = _score.value + pointsGained
    _score.value = newScore
    if (pointsGained > 0) {
      val nextCareer = _careerTotalScore.value + pointsGained.toLong()
      _careerTotalScore.value = nextCareer
      prefs.edit().putLong("career_total_score", nextCareer).apply()
    }
    when (_activeGameMode.value) {
      GameMode.TIME_ATTACK -> {
        if (newScore > _timeAttackBestScore.value) {
          _timeAttackBestScore.value = newScore
          prefs.edit().putInt("best_time_attack", newScore).apply()
          _isNewBestScore.value = true
          refreshAchievementsFromStats(announceUnlocks = true)
        }
      }
      GameMode.ENDLESS -> {
        if (newScore > _endlessBestScore.value) {
          _endlessBestScore.value = newScore
          prefs.edit().putInt("best_endless", newScore).apply()
          _isNewBestScore.value = true
          refreshAchievementsFromStats(announceUnlocks = true)
        }
      }
      GameMode.CLASSIC -> {}
    }
  }

  fun restartCurrentLevel() {
    val wasGameOver = _isGameOver.value
    val executeRestart = {
      when (_activeGameMode.value) {
        GameMode.CLASSIC -> loadLevel(_currentLevel.value)
        GameMode.TIME_ATTACK -> startTimeAttack()
        GameMode.ENDLESS -> startEndlessMode()
      }
    }
    if (wasGameOver) {
      gameOverRetriesCounter++
      if (gameOverRetriesCounter % AdConfig.INTERSTITIAL_RETRY_INTERVAL == 0) {
        AdManager.showInterstitialSlot(
          scope = viewModelScope,
          placement = AdPlacement.GAME_OVER_RETRY_INTERSTITIAL,
          hasNoAds = _hasNoAds.value,
          onCompleted = executeRestart
        )
        return
      }
    }
    executeRestart()
  }

  fun nextLevel() {
    levelsCompletedCounter++
    val next = _currentLevel.value + 1
    if (levelsCompletedCounter % AdConfig.INTERSTITIAL_LEVEL_INTERVAL == 0) {
      AdManager.showInterstitialSlot(
        scope = viewModelScope,
        placement = AdPlacement.LEVEL_COMPLETE_INTERSTITIAL,
        hasNoAds = _hasNoAds.value
      ) {
        loadLevel(next)
      }
    } else {
      loadLevel(next)
    }
  }

  fun claimShopCoinReward(amount: Int) {
    AdManager.showRewardedSlot(
      scope = viewModelScope,
      placement = AdPlacement.SHOP_REWARDED_COINS,
      onRewardEarned = {
        addCoins(amount)
        requestCloudSync()
        audioPlayer.play(SoundEffect.CoinCollect)
        if (_currentScreen.value == ScreenState.GAME) {
          spawnCoinParticles(boardCenter.x, boardCenter.y)
        }
        showFloatingText("+$amount COINS! 🪙", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 22f)
      }
    )
  }

  fun claimGameOverBonusCoins() {
    AdManager.showRewardedSlot(
      scope = viewModelScope,
      placement = AdPlacement.GAME_OVER_BONUS_REWARDED,
      onRewardEarned = {
        addCoins(100)
        requestCloudSync()
        audioPlayer.play(SoundEffect.CoinCollect)
      }
    )
  }

  fun dismissInterstitialAd() {
    _showInterstitialAd.value = false
  }

  fun toggleLowPerformanceMode() {
    val newMode = !_isLowPerformanceMode.value
    _isLowPerformanceMode.value = newMode
    prefs.edit().putBoolean("low_perf_mode", newMode).apply()
  }

  // --- Drag and Drop Handlers (Offset Lift + Magnetic Snap-to-Grid + Auto-Release Lock) ---

  fun onDragStart(index: Int, touchPos: Offset) {
    if (isCascading || _isLevelComplete.value || _isGameOver.value || _isTimeUp.value || _isPaused.value) return
    val stack = _stagedStacks.value.getOrNull(index) ?: return
    hasDraggedOntoBoard = false
    _draggedStack.value = stack
    _selectedStagedIndex.value = index
    _activeHint.value = null
    val elevatedAnchor = Offset(touchPos.x, touchPos.y - dragVerticalOffsetPx)
    updateDragSnap(elevatedAnchor, playHoverSound = false)
    audioPlayer.play(SoundEffect.TileTap)
  }

  fun onDragMove(currentTouchPos: Offset) {
    if (_draggedStack.value == null) return
    val elevatedAnchor = Offset(currentTouchPos.x, currentTouchPos.y - dragVerticalOffsetPx)
    updateDragSnap(elevatedAnchor, playHoverSound = true)
  }

  private fun updateDragSnap(elevatedAnchor: Offset, playHoverSound: Boolean) {
    val prevHover = _hoveredCoord.value
    val snap = HexMath.findMagneticSnapTarget(
      dragAnchor = elevatedAnchor,
      boardCells = _boardCells.value,
      hexRadius = hexRadius,
      boardCenter = boardCenter
    )
    if (snap.isOverBoard) {
      hasDraggedOntoBoard = true
    }
    _dragPosition.value = snap.visualPosition
    _snapStrength.value = snap.snapStrength
    _hoveredCoord.value = snap.snappedCoord

    if (playHoverSound && snap.snappedCoord != null && snap.snappedCoord != prevHover) {
      audioPlayer.play(SoundEffect.TileHover, pitch = 1.0f + snap.snapStrength * 0.12f)
    }
  }

  fun onDragEnd() {
    val dragged = _draggedStack.value
    val targetCoord = _hoveredCoord.value
    val wasOverBoard = hasDraggedOntoBoard
    hasDraggedOntoBoard = false
    _draggedStack.value = null
    _dragPosition.value = null
    _hoveredCoord.value = null
    _snapStrength.value = 0f

    if (dragged != null) {
      if (targetCoord != null) {
        // Auto-release lock directly onto the snapped hex cell!
        placeStagedStack(dragged.pedestalIndex, targetCoord)
      } else if (wasOverBoard) {
        // Error cue only when released over an occupied/blocked board cell
        audioPlayer.play(SoundEffect.InvalidPlacement)
      }
    }
  }

  fun selectStagedStack(index: Int) {
    val stack = _stagedStacks.value.getOrNull(index) ?: return
    if (_activeBooster.value != null) {
      _activeBooster.value = null
    }
    if (_selectedStagedIndex.value == index) {
      _selectedStagedIndex.value = null
      audioPlayer.play(SoundEffect.TileTap, pitch = 0.85f)
    } else {
      _selectedStagedIndex.value = index
      audioPlayer.play(SoundEffect.TileTap)
    }
  }

  /**
   * Performs placement of a staged stack onto targetCoord.
   */
  private fun placeStagedStack(stagedIndex: Int, targetCoord: HexCoord) {
    val currentMap = _boardCells.value.toMutableMap()
    val cell = currentMap[targetCoord] ?: return
    if (!cell.isEmpty) {
      audioPlayer.play(SoundEffect.InvalidPlacement)
      return
    }

    val staged = _stagedStacks.value.getOrNull(stagedIndex) ?: return

    // Save snapshot for Undo
    saveBoardSnapshot()

    audioPlayer.play(SoundEffect.TileDrop)
    val updatedStaged = _stagedStacks.value.toMutableList()
    updatedStaged[stagedIndex] = null
    _stagedStacks.value = updatedStaged
    _selectedStagedIndex.value = null
    _activeHint.value = null

    currentMap[targetCoord] = cell.copy(stack = staged.tiles, popScale = 1.12f)
    _boardCells.value = currentMap

    if (updatedStaged.all { it == null }) {
      replenishStagedStacks(force = false)
    }

    startCascadeSequence(targetCoord)
  }

  /**
   * Called when player taps a board cell.
   */
  fun onCellTapped(coord: HexCoord) {
    if (_isLevelComplete.value || _isGameOver.value || _isTimeUp.value || _isPaused.value || isCascading) return

    val currentMap = _boardCells.value.toMutableMap()
    val cell = currentMap[coord] ?: return

    // 1. Hammer Booster active
    if (_activeBooster.value == BoosterType.HAMMER) {
      if (!cell.isEmpty) {
        saveBoardSnapshot()
        useBooster(BoosterType.HAMMER)
        _activeBooster.value = null
        val pos = HexMath.hexToPixel(coord, hexRadius, boardCenter)
        _hammerStrikePos.value = pos
        audioPlayer.play(SoundEffect.TileTap, pitch = 1.15f)
        viewModelScope.launch {
          // Wait until hammer head visually reaches impact point
          delay(130)
          audioPlayer.play(SoundEffect.HammerSmash)
          triggerScreenShake()
          spawnExplosion(pos.x, pos.y, cell.topTile?.color?.topColor ?: Color(0xFFFFD600), count = 35)
          spawnExplosion(pos.x, pos.y, Color(0xFFFF4081), count = 20)
          showFloatingText("SMASH! 💥", pos.x, pos.y, Color(0xFFFF2D87), 28f)
          if (cell.obstacle !is CellObstacle.None) {
            recordClearedObstacle(cell.obstacle)
          }
          val m = _boardCells.value.toMutableMap()
          m[coord] = cell.copy(stack = emptyList(), obstacle = CellObstacle.None)
          _boardCells.value = m
          delay(280)
          _hammerStrikePos.value = null
          checkPostActionStatus()
        }
      } else {
        audioPlayer.play(SoundEffect.InvalidPlacement)
      }
      return
    }

    // 2. Placing selected staged stack via tap
    val selectedIdx = _selectedStagedIndex.value ?: return
    if (!cell.isEmpty) {
      audioPlayer.play(SoundEffect.InvalidPlacement)
      return
    }

    placeStagedStack(selectedIdx, coord)
  }

  // --- Boosters Implementation ---

  fun activateBooster(booster: BoosterType) {
    val count = _boosterCounts.value[booster] ?: 0
    if (count <= 0) {
      if (_coins.value >= booster.cost) {
        addCoins(-booster.cost)
        val updated = _boosterCounts.value.toMutableMap()
        updated[booster] = (updated[booster] ?: 0) + 1
        _boosterCounts.value = updated
        saveBoosters()
        audioPlayer.play(SoundEffect.CoinCollect)
        if (booster == BoosterType.HAMMER && _isGameOver.value) {
          _isGameOver.value = false
          _activeBooster.value = BoosterType.HAMMER
          _selectedStagedIndex.value = null
          audioPlayer.play(SoundEffect.TileTap)
        }
      }
      return
    }

    when (booster) {
      BoosterType.UNDO -> {
        if (canUndo.value) {
          useBooster(BoosterType.UNDO)
          performUndo()
        }
      }
      BoosterType.SHUFFLE -> {
        useBooster(BoosterType.SHUFFLE)
        replenishStagedStacks(force = true)
        audioPlayer.play(SoundEffect.ShuffleDeck)
        showFloatingText("SHUFFLED! 🔁", boardCenter.x, boardCenter.y, Color(0xFF00E5FF))
      }
      BoosterType.HINT -> {
        useBooster(BoosterType.HINT)
        calculateBestHint()
      }
      BoosterType.HAMMER -> {
        if (_isGameOver.value) {
          _isGameOver.value = false
        }
        if (_activeBooster.value == BoosterType.HAMMER) {
          _activeBooster.value = null
          checkGameOverCondition()
        } else {
          _activeBooster.value = BoosterType.HAMMER
          _selectedStagedIndex.value = null
          audioPlayer.play(SoundEffect.TileTap)
        }
      }
      BoosterType.PINWHEEL -> {
        useBooster(BoosterType.PINWHEEL)
        saveBoardSnapshot()
        audioPlayer.play(SoundEffect.ShuffleDeck, pitch = 1.25f)
        triggerScreenShake()
        showFloatingText("WINDMILL STORM! 🌀", boardCenter.x, boardCenter.y, Color(0xFF00E5FF), 28f)
        viewModelScope.launch {
          val targets = _boardCells.value.filter { !it.value.isEmpty }.entries
            .sortedByDescending { it.value.totalTilesCount }
            .take(2)
          targets.forEachIndexed { idx, (coord, cell) ->
            if (idx > 0) delay(110)
            val pos = HexMath.hexToPixel(coord, hexRadius, boardCenter)
            audioPlayer.play(SoundEffect.StackFullClear, pitch = 1.0f + idx * 0.12f)
            spawnExplosion(pos.x, pos.y, Color(0xFFFF2D87), count = 25)
            spawnExplosion(pos.x, pos.y, Color(0xFF00E5FF), count = 25)
            if (cell.obstacle !is CellObstacle.None) {
              recordClearedObstacle(cell.obstacle)
            }
            val map = _boardCells.value.toMutableMap()
            map[coord] = cell.copy(stack = emptyList(), obstacle = CellObstacle.None)
            _boardCells.value = map
          }
          checkPostActionStatus()
        }
      }
    }
  }

  private fun useBooster(booster: BoosterType) {
    val updated = _boosterCounts.value.toMutableMap()
    val count = updated[booster] ?: 1
    updated[booster] = maxOf(0, count - 1)
    _boosterCounts.value = updated
    saveBoosters()
  }

  private fun saveBoosters() {
    val counts = _boosterCounts.value
    prefs.edit()
      .putInt("booster_undo", counts[BoosterType.UNDO] ?: 3)
      .putInt("booster_shuffle", counts[BoosterType.SHUFFLE] ?: 2)
      .putInt("booster_hint", counts[BoosterType.HINT] ?: 3)
      .putInt("booster_hammer", counts[BoosterType.HAMMER] ?: 3)
      .putInt("booster_pinwheel", counts[BoosterType.PINWHEEL] ?: 5)
      .apply()
  }

  // --- Undo History ---

  private fun saveBoardSnapshot() {
    val snapshot = BoardSnapshot(
      boardCells = _boardCells.value.toMap(),
      stagedStacks = _stagedStacks.value.toList(),
      score = _score.value,
      remainingGoal = _remainingGoal.value,
      secondaryGoalRemaining = _secondaryGoalRemaining.value
    )
    undoHistory.push(snapshot)
    if (undoHistory.size > 5) {
      undoHistory.removeLast()
    }
    _canUndo.value = true
  }

  private fun performUndo() {
    if (undoHistory.isEmpty()) return
    val snapshot = undoHistory.pop()
    _boardCells.value = snapshot.boardCells
    _stagedStacks.value = snapshot.stagedStacks
    _score.value = snapshot.score
    _remainingGoal.value = snapshot.remainingGoal
    _secondaryGoalRemaining.value = snapshot.secondaryGoalRemaining
    _canUndo.value = undoHistory.isNotEmpty()
    _activeHint.value = null
    _selectedStagedIndex.value = null
    audioPlayer.play(SoundEffect.UndoMove)
    showFloatingText("UNDO! ↩️", boardCenter.x, boardCenter.y, Color(0xFFFFD600))
  }

  // --- Intelligent Hint Solver ---

  private fun calculateBestHint() {
    val map = _boardCells.value
    val emptyCoords = map.filter { it.value.isEmpty }.keys
    val staged = _stagedStacks.value

    var bestRec: HintRecommendation? = null
    var maxMatches = 0

    staged.forEachIndexed { sIdx, stack ->
      if (stack != null && stack.topTile != null) {
        val topTile = stack.topTile!!
        for (coord in emptyCoords) {
          var matchingNeighbors = 0
          for (neighbor in coord.getNeighbors()) {
            val nCell = map[neighbor]
            if (nCell != null && nCell.topTile != null && nCell.topTile!!.matchesTile(topTile)) {
              matchingNeighbors += (nCell.topTile?.count ?: 1)
            }
          }
          if (matchingNeighbors > maxMatches) {
            maxMatches = matchingNeighbors
            bestRec = HintRecommendation(
              stagedIndex = sIdx,
              targetCoord = coord,
              matchingColor = topTile.color,
              estimatedMatches = matchingNeighbors
            )
          }
        }
      }
    }

    if (bestRec != null) {
      _activeHint.value = bestRec
      _selectedStagedIndex.value = bestRec!!.stagedIndex
      audioPlayer.play(SoundEffect.HintBeacon)
      val pos = HexMath.hexToPixel(bestRec!!.targetCoord, hexRadius, boardCenter)
      showFloatingText("HINT! 💡", pos.x, pos.y - 30f, Color(0xFFFFF176), 24f)
    } else {
      // Fallback hint to first empty cell
      val firstEmpty = emptyCoords.firstOrNull()
      val firstStaged = staged.indexOfFirst { it != null }
      if (firstEmpty != null && firstStaged != -1) {
        _activeHint.value = HintRecommendation(firstStaged, firstEmpty, staged[firstStaged]!!.topTile!!.color, 1)
        _selectedStagedIndex.value = firstStaged
        audioPlayer.play(SoundEffect.HintBeacon)
      }
    }
  }

  // --- Daily Reward Logic ---

  private fun checkDailyRewardAvailable(): Boolean {
    val lastClaim = prefs.getLong("last_daily_claim_ms", 0L)
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    return (now - lastClaim) >= oneDayMs
  }

  fun claimDailyReward() {
    audioPlayer.play(SoundEffect.DailyRewardJingle)
    val currentDay = _consecutiveDays.value
    val coinsAward = currentDay * 150
    addCoins(coinsAward)

    // Also award 1 free booster depending on day
    val boosterType = when (currentDay % 4) {
      1 -> BoosterType.UNDO
      2 -> BoosterType.SHUFFLE
      3 -> BoosterType.HINT
      else -> BoosterType.HAMMER
    }
    val updated = _boosterCounts.value.toMutableMap()
    updated[boosterType] = (updated[boosterType] ?: 0) + 1
    _boosterCounts.value = updated
    saveBoosters()

    val nextDay = if (currentDay >= 7) 1 else currentDay + 1
    _consecutiveDays.value = nextDay
    _canClaimDailyReward.value = false

    prefs.edit()
      .putLong("last_daily_claim_ms", System.currentTimeMillis())
      .putInt("consecutive_days", nextDay)
      .apply()

    if (_currentScreen.value == ScreenState.GAME) {
      spawnCoinParticles(boardCenter.x, boardCenter.y)
    }
    showFloatingText("+$coinsAward COINS & 1 $boosterType! 🎁", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 22f)
    requestCloudSync()
  }

  // --- Daily Mission System Management ---

  private fun getTodayDateString(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return sdf.format(java.util.Date())
  }

  private fun initDailyMissions() {
    val today = getTodayDateString()
    val savedDate = prefs.getString("missions_date", null)
    if (savedDate == today) {
      loadSavedMissions()
    } else {
      generateFreshDailyMissions(today)
    }
  }

  private fun generateFreshDailyMissions(today: String) {
    val freshMissions = EngagementCatalog.buildDailyMissionsForDate(today)
    _dailyMissions.value = freshMissions
    _isGrandChestClaimed.value = false
    saveMissionsToPrefs(today, freshMissions, false)
    recomputeUnclaimedCount()
  }

  private fun loadSavedMissions() {
    val count = prefs.getInt("missions_count", 0)
    if (count <= 0) {
      generateFreshDailyMissions(getTodayDateString())
      return
    }

    val list = mutableListOf<DailyMission>()
    for (i in 0 until count) {
      val id = prefs.getString("m_${i}_id", "m_$i") ?: "m_$i"
      val typeStr = prefs.getString("m_${i}_type", MissionType.STACK_CLEAR.name) ?: MissionType.STACK_CLEAR.name
      val type = try { MissionType.valueOf(typeStr) } catch (_: Exception) { MissionType.STACK_CLEAR }
      val title = prefs.getString("m_${i}_title", "Daily Mission") ?: "Daily Mission"
      val desc = prefs.getString("m_${i}_desc", "") ?: ""
      val icon = prefs.getString("m_${i}_icon", "🎯") ?: "🎯"
      val target = prefs.getInt("m_${i}_target", 1)
      val progress = prefs.getInt("m_${i}_progress", 0)
      val rewardType = prefs.getString("m_${i}_reward_type", "COINS") ?: "COINS"
      val rewardVal = prefs.getInt("m_${i}_reward_val", 300)
      val rewardVal2 = prefs.getInt("m_${i}_reward_val2", 10)
      val claimed = prefs.getBoolean("m_${i}_claimed", false)

      val reward = when (rewardType) {
        "GEMS" -> MissionReward.Gems(rewardVal)
        "COINS_GEMS" -> MissionReward.CoinsAndGems(rewardVal, rewardVal2)
        "BOOSTER_HAMMER" -> MissionReward.Booster(BoosterType.HAMMER, rewardVal)
        "BOOSTER_SHUFFLE" -> MissionReward.Booster(BoosterType.SHUFFLE, rewardVal)
        "BOOSTER_UNDO" -> MissionReward.Booster(BoosterType.UNDO, rewardVal)
        "BOOSTER_HINT" -> MissionReward.Booster(BoosterType.HINT, rewardVal)
        else -> MissionReward.Coins(rewardVal)
      }

      list.add(
        DailyMission(
          id = id,
          type = type,
          title = title,
          description = desc,
          iconEmoji = icon,
          targetValue = target,
          currentProgress = progress,
          reward = reward,
          isClaimed = claimed
        )
      )
    }

    _dailyMissions.value = list
    _isGrandChestClaimed.value = prefs.getBoolean("missions_grand_claimed", false)
    recomputeUnclaimedCount()
  }

  private fun saveMissionsToPrefs(date: String, missions: List<DailyMission>, grandClaimed: Boolean) {
    val editor = prefs.edit()
    editor.putString("missions_date", date)
    editor.putInt("missions_count", missions.size)
    editor.putBoolean("missions_grand_claimed", grandClaimed)

    missions.forEachIndexed { i, m ->
      editor.putString("m_${i}_id", m.id)
      editor.putString("m_${i}_type", m.type.name)
      editor.putString("m_${i}_title", m.title)
      editor.putString("m_${i}_desc", m.description)
      editor.putString("m_${i}_icon", m.iconEmoji)
      editor.putInt("m_${i}_target", m.targetValue)
      editor.putInt("m_${i}_progress", m.currentProgress)
      editor.putBoolean("m_${i}_claimed", m.isClaimed)
      when (val r = m.reward) {
        is MissionReward.Coins -> {
          editor.putString("m_${i}_reward_type", "COINS")
          editor.putInt("m_${i}_reward_val", r.amount)
        }
        is MissionReward.Gems -> {
          editor.putString("m_${i}_reward_type", "GEMS")
          editor.putInt("m_${i}_reward_val", r.amount)
        }
        is MissionReward.CoinsAndGems -> {
          editor.putString("m_${i}_reward_type", "COINS_GEMS")
          editor.putInt("m_${i}_reward_val", r.coins)
          editor.putInt("m_${i}_reward_val2", r.gems)
        }
        is MissionReward.Booster -> {
          editor.putString("m_${i}_reward_type", "BOOSTER_${r.boosterType.name}")
          editor.putInt("m_${i}_reward_val", r.count)
        }
        is MissionReward.GrandChest -> {
          editor.putString("m_${i}_reward_type", "GRAND_CHEST")
          editor.putInt("m_${i}_reward_val", r.coins)
          editor.putInt("m_${i}_reward_val2", r.gems)
        }
      }
    }
    editor.apply()
  }

  private fun recomputeUnclaimedCount() {
    var count = _dailyMissions.value.count { it.canClaim }
    val allCompleted = _dailyMissions.value.isNotEmpty() && _dailyMissions.value.all { it.isCompleted }
    if (allCompleted && !_isGrandChestClaimed.value) {
      count++
    }
    _unclaimedMissionCount.value = count
  }

  fun updateMissionProgress(type: MissionType, delta: Int) {
    // 1. Record permanent lifetime profile stats for Achievements
    when (type) {
      MissionType.TILE_MERGES -> incrementStat("stat_total_merges", delta)
      MissionType.STACK_CLEAR -> incrementStat("stat_total_stacks", delta)
      MissionType.LEVEL_COMPLETE -> incrementStat("stat_total_levels", delta)
      MissionType.OBSTACLE_CLEAR -> incrementStat("stat_total_obstacles", delta)
      else -> {}
    }

    // 2. Advance today's Daily Missions
    val current = _dailyMissions.value
    var changed = false
    val updated = current.map { mission ->
      if (mission.type == type && !mission.isCompleted) {
        val newProg = (mission.currentProgress + delta).coerceAtMost(mission.targetValue)
        if (newProg != mission.currentProgress) {
          changed = true
          if (newProg >= mission.targetValue) {
            audioPlayer.play(SoundEffect.MissionComplete)
            showFloatingText("MISSION READY! 🎯 ${mission.title}", boardCenter.x, boardCenter.y - 60f, Color(0xFF00E676), 22f)
          }
          mission.copy(currentProgress = newProg)
        } else mission
      } else mission
    }

    if (changed) {
      _dailyMissions.value = updated
      saveMissionsToPrefs(getTodayDateString(), updated, _isGrandChestClaimed.value)
      recomputeUnclaimedCount()
    }
    refreshAchievementsFromStats(announceUnlocks = true)
  }

  fun updateMissionMax(type: MissionType, value: Int) {
    if (type == MissionType.COMBO_STREAK) {
      val prevBest = prefs.getInt("stat_best_combo", 1)
      if (value > prevBest) {
        prefs.edit().putInt("stat_best_combo", value).apply()
      }
    }

    val current = _dailyMissions.value
    var changed = false
    val updated = current.map { mission ->
      if (mission.type == type && !mission.isCompleted) {
        if (value > mission.currentProgress) {
          changed = true
          val newProg = value.coerceAtMost(mission.targetValue)
          if (newProg >= mission.targetValue) {
            audioPlayer.play(SoundEffect.MissionComplete)
            showFloatingText("MISSION READY! 🎯 ${mission.title}", boardCenter.x, boardCenter.y - 60f, Color(0xFF00E676), 22f)
          }
          mission.copy(currentProgress = newProg)
        } else mission
      } else mission
    }

    if (changed) {
      _dailyMissions.value = updated
      saveMissionsToPrefs(getTodayDateString(), updated, _isGrandChestClaimed.value)
      recomputeUnclaimedCount()
    }
    refreshAchievementsFromStats(announceUnlocks = true)
  }

  fun claimMission(missionId: String) {
    val current = _dailyMissions.value
    val target = current.find { it.id == missionId } ?: return
    if (!target.canClaim) return

    when (val reward = target.reward) {
      is MissionReward.Coins -> {
        addCoins(reward.amount)
      }
      is MissionReward.Gems -> {
        addGems(reward.amount)
      }
      is MissionReward.CoinsAndGems -> {
        addCoins(reward.coins)
        addGems(reward.gems)
      }
      is MissionReward.Booster -> {
        val currentCounts = _boosterCounts.value.toMutableMap()
        currentCounts[reward.boosterType] = (currentCounts[reward.boosterType] ?: 0) + reward.count
        _boosterCounts.value = currentCounts
        saveBoosters()
      }
      is MissionReward.GrandChest -> {
        addCoins(reward.coins)
        addGems(reward.gems)
        val currentCounts = _boosterCounts.value.toMutableMap()
        currentCounts[reward.booster] = (currentCounts[reward.booster] ?: 0) + 1
        _boosterCounts.value = currentCounts
        saveBoosters()
      }
    }

    audioPlayer.play(SoundEffect.MissionClaim)
    if (_currentScreen.value == ScreenState.GAME) {
      spawnCoinParticles(boardCenter.x, boardCenter.y)
      triggerScreenShake(intensity = 0.45f)
    }
    showFloatingText("CLAIMED! ${target.reward.displayBadge}", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 24f)

    val updated = current.map { if (it.id == missionId) it.copy(isClaimed = true) else it }
    _dailyMissions.value = updated
    saveMissionsToPrefs(getTodayDateString(), updated, _isGrandChestClaimed.value)
    recomputeUnclaimedCount()
    requestCloudSync()
  }

  fun claimGrandChest() {
    if (_isGrandChestClaimed.value) return
    val allCompleted = _dailyMissions.value.isNotEmpty() && _dailyMissions.value.all { it.isCompleted }
    if (!allCompleted) return

    _isGrandChestClaimed.value = true
    addCoins(500)
    addGems(25)
    val currentCounts = _boosterCounts.value.toMutableMap()
    currentCounts[BoosterType.HAMMER] = (currentCounts[BoosterType.HAMMER] ?: 0) + 1
    currentCounts[BoosterType.SHUFFLE] = (currentCounts[BoosterType.SHUFFLE] ?: 0) + 1
    _boosterCounts.value = currentCounts
    saveBoosters()

    saveMissionsToPrefs(getTodayDateString(), _dailyMissions.value, true)
    audioPlayer.play(SoundEffect.VictoryFanfare)
    if (_currentScreen.value == ScreenState.GAME) {
      launchComboFireworks(comboLevel = 5)
      triggerScreenShake(intensity = 0.75f)
    }
    showFloatingText("GRAND CHEST! +500 🪙 +25 💎", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 22f)
    recomputeUnclaimedCount()
    requestCloudSync()
  }

  // --- Achievements & Player Profile Trophy System ---

  private fun incrementStat(key: String, delta: Int) {
    val next = prefs.getInt(key, 0) + delta
    prefs.edit().putInt(key, next).apply()
  }

  private fun initAchievements() {
    refreshAchievementsFromStats(announceUnlocks = false)
  }

  private fun refreshAchievementsFromStats(announceUnlocks: Boolean) {
    val totalMerges = prefs.getInt("stat_total_merges", 0)
    val totalStacks = prefs.getInt("stat_total_stacks", 0)
    val totalLevels = prefs.getInt("stat_total_levels", 0)
    val bestCombo = prefs.getInt("stat_best_combo", 1)
    val totalObstacles = prefs.getInt("stat_total_obstacles", 0)
    val bestTA = _timeAttackBestScore.value
    val bestEndless = _endlessBestScore.value
    val totalGems = _gems.value

    val previous = _achievements.value.associateBy { it.id }
    val updated = EngagementCatalog.baseAchievements().map { base ->
      val progress = when (base.metric) {
        AchievementMetric.TOTAL_LEVELS -> totalLevels
        AchievementMetric.TOTAL_MERGES -> totalMerges
        AchievementMetric.TOTAL_STACKS -> totalStacks
        AchievementMetric.BEST_COMBO -> bestCombo
        AchievementMetric.TOTAL_OBSTACLES -> totalObstacles
        AchievementMetric.BEST_TIME_ATTACK -> bestTA
        AchievementMetric.BEST_ENDLESS -> bestEndless
        AchievementMetric.TOTAL_GEMS -> totalGems
      }.coerceAtMost(base.targetValue)
      val isClaimed = prefs.getBoolean("ach_claimed_${base.id}", false)
      val updatedAch = base.copy(currentProgress = progress, isClaimed = isClaimed)

      if (announceUnlocks) {
        val wasUnlocked = previous[base.id]?.isUnlocked ?: false
        if (!wasUnlocked && updatedAch.isUnlocked) {
          audioPlayer.play(SoundEffect.MissionComplete, pitch = 1.15f)
          if (_currentScreen.value == ScreenState.GAME) {
            spawnStarParticles(boardCenter.x, boardCenter.y - 40f)
          }
          showFloatingText(
            "🏆 TROPHY UNLOCKED: ${updatedAch.title}!",
            boardCenter.x,
            boardCenter.y - 100f,
            Color(0xFF00E5FF),
            21f
          )
        }
      }
      updatedAch
    }

    _achievements.value = updated
    _unclaimedAchievementCount.value = updated.count { it.canClaim }
    _profileStats.value = PlayerProfileStats(
      totalMerges = totalMerges,
      totalStacksCleared = totalStacks,
      totalLevelsCompleted = totalLevels,
      bestComboEver = bestCombo,
      totalObstaclesCleared = totalObstacles,
      bestTimeAttackScore = bestTA,
      bestEndlessScore = bestEndless,
      unlockedTrophies = updated.count { it.isUnlocked },
      totalTrophies = updated.size
    )
  }

  fun claimAchievement(achievementId: String) {
    val target = _achievements.value.find { it.id == achievementId } ?: return
    if (!target.canClaim) return

    if (target.rewardCoins > 0) addCoins(target.rewardCoins)
    if (target.rewardGems > 0) addGems(target.rewardGems)
    prefs.edit().putBoolean("ach_claimed_${target.id}", true).apply()

    audioPlayer.play(SoundEffect.MissionClaim)
    if (_currentScreen.value == ScreenState.GAME) {
      spawnStarParticles(boardCenter.x, boardCenter.y)
      triggerScreenShake(intensity = 0.55f)
    }
    showFloatingText("🏆 TROPHY CLAIMED! ${target.rewardBadge}", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 23f)
    refreshAchievementsFromStats(announceUnlocks = false)
    requestCloudSync()
  }

  // --- Cascade Slide Sequence ---

  private fun startCascadeSequence(startCoord: HexCoord) {
    cascadeJob?.cancel()
    cascadeJob = viewModelScope.launch {
      isCascading = true

      // Let the placed stack settle for ~95ms so TileDrop sound and placement pop finish cleanly before cascade slides begin
      delay(55)
      _boardCells.value[startCoord]?.let { placed ->
        val settled = _boardCells.value.toMutableMap()
        settled[startCoord] = placed.copy(popScale = 1.0f)
        _boardCells.value = settled
      }
      delay(45)

      var hasMoreMerges = true
      var currentCombo = 1

      while (hasMoreMerges) {
        hasMoreMerges = false
        val map = _boardCells.value.toMutableMap()

        var matchFound: Pair<HexCoord, HexCoord>? = null
        for ((coord, cell) in map) {
          val topTile = cell.topTile ?: continue
          for (neighbor in coord.getNeighbors()) {
            val neighborCell = map[neighbor] ?: continue
            val neighborTop = neighborCell.topTile ?: continue
            if (neighborTop.matchesTile(topTile) && coord != neighbor) {
              matchFound = Pair(coord, neighbor)
              break
            }
          }
          if (matchFound != null) break
        }

        if (matchFound != null) {
          hasMoreMerges = true
          val (sourceCoord, targetCoord) = matchFound
          val sourceCell = map[sourceCoord]!!
          val targetCell = map[targetCoord]!!

          val sourceTop = sourceCell.topTile!!
          val targetTop = targetCell.topTile!!
          val matchingColor = if (sourceTop.matchesColor(targetTop.color)) {
            targetTop.color
          } else if (targetTop.splitColor != null && sourceTop.matchesColor(targetTop.splitColor!!)) {
            targetTop.splitColor!!
          } else {
            sourceTop.color
          }
          val countToSlide = sourceTop.count

          val newSourceStack = sourceCell.stack.dropLast(1)
          map[sourceCoord] = sourceCell.copy(stack = newSourceStack)
          _boardCells.value = map

          val startPos = HexMath.hexToPixel(sourceCoord, hexRadius, boardCenter)
          val endPos = HexMath.hexToPixel(targetCoord, hexRadius, boardCenter)

          // Exact synchronized visual & audio slide timing
          val timing = HexMath.computeSlideTiming(countToSlide)
          val slideStartMs = System.currentTimeMillis()
          val slide = ActiveSlide(
            sourceCoord = sourceCoord,
            targetCoord = targetCoord,
            startPos = startPos,
            endPos = endPos,
            color = matchingColor,
            count = countToSlide,
            startTimeMs = slideStartMs,
            durationMs = timing.durationMs
          )
          _activeSlides.value = _activeSlides.value + slide

          // Trigger each ascending slide wave note precisely as each fanning slice lifts off
          val noteStepMs = if (timing.visualSlices <= 1) {
            0L
          } else {
            (timing.durationMs * timing.delayPerSliceFrac).toLong().coerceAtLeast(24L)
          }
          for (i in 0 until timing.visualSlices) {
            val targetNoteTime = slideStartMs + i * noteStepMs
            val waitMs = (targetNoteTime - System.currentTimeMillis()).coerceAtLeast(0L)
            if (waitMs > 0L) delay(waitMs)
            audioPlayer.play(SoundEffect.SlideWaveNote(i), pitch = 1.0f + minOf(currentCombo - 1, 3) * 0.04f)
          }

          // Wait the exact remaining travel time until visual slices reach targetCoord (progress == 1.0)
          val remainingSlideMs = (slideStartMs + timing.durationMs - System.currentTimeMillis()).coerceAtLeast(0L)
          if (remainingSlideMs > 0L) {
            delay(remainingSlideMs)
          }

          // Exact landing frame: remove flying slide, stack lands, popScale expands, and StackMerge hits simultaneously
          _activeSlides.value = _activeSlides.value.filter { it.id != slide.id }

          val currentTarget = _boardCells.value[targetCoord] ?: targetCell
          val currentTargetTop = currentTarget.topTile
          val newTargetStack = if (currentTargetTop != null && currentTargetTop.matchesColor(matchingColor)) {
            val mergedTop = HexTile(matchingColor, currentTargetTop.count + countToSlide)
            currentTarget.stack.dropLast(1) + mergedTop
          } else {
            currentTarget.stack + HexTile(matchingColor, countToSlide)
          }

          val mergePitch = (1.0f + minOf(currentCombo - 1, 5) * 0.05f)
          audioPlayer.play(SoundEffect.StackMerge, pitch = mergePitch)
          updateMissionProgress(MissionType.TILE_MERGES, 1)

          // Bright sparks + mini hex shockwave on every successful merge
          val mergePos = HexMath.hexToPixel(targetCoord, hexRadius, boardCenter)
          val mergedTopCount = newTargetStack.lastOrNull()?.count ?: countToSlide
          spawnMergeSparks(mergePos.x, mergePos.y, matchingColor.topColor, mergedTopCount)
          if (mergedTopCount >= 6) {
            triggerScreenShake(intensity = 0.38f)
          }

          val updatedMap = _boardCells.value.toMutableMap()
          updatedMap[targetCoord] = currentTarget.copy(
            stack = newTargetStack,
            popScale = 1.18f
          )
          _boardCells.value = updatedMap

          // Smooth pop settle back to 1.0f
          delay(55)
          val midMap = _boardCells.value.toMutableMap()
          midMap[targetCoord]?.let {
            midMap[targetCoord] = it.copy(popScale = 1.07f)
            _boardCells.value = midMap
          }
          delay(50)
          val settledMap = _boardCells.value.toMutableMap()
          settledMap[targetCoord]?.let {
            settledMap[targetCoord] = it.copy(popScale = 1.0f)
            _boardCells.value = settledMap
          }

          val finalTop = _boardCells.value[targetCoord]?.topTile
          if (finalTop != null && finalTop.count >= 10) {
            currentCombo++
            _combo.value = currentCombo

            // Clear sound + particle fireworks + stack removal hit on exact same frame
            audioPlayer.play(SoundEffect.StackFullClear)
            updateMissionProgress(MissionType.STACK_CLEAR, 1)
            updateMissionMax(MissionType.COMBO_STREAK, currentCombo)

            val popPos = HexMath.hexToPixel(targetCoord, hexRadius, boardCenter)
            if (!_isLowPerformanceMode.value) {
              spawnFireworksBurst(popPos.x, popPos.y, finalTop.color.topColor, currentCombo)
              if (currentCombo >= 3) {
                launchComboFireworks(currentCombo)
              }
            }
            val shakeIntensity = when {
              currentCombo >= 10 -> 1.75f
              currentCombo >= 5 -> 1.35f
              currentCombo >= 2 -> 0.95f
              else -> 0.65f
            }
            triggerScreenShake(intensity = shakeIntensity)

            val comboText = when {
              currentCombo >= 10 -> "🎆 ULTRA COMBO X$currentCombo!"
              currentCombo >= 4 -> "🔥 MEGA COMBO X$currentCombo!"
              currentCombo > 1 -> "COMBO X$currentCombo!"
              else -> "PERFECT!"
            }
            showFloatingText(comboText, popPos.x, popPos.y - 40f, finalTop.color.topColor, if (currentCombo >= 5) 31f else 28f)

            val clearedCount = finalTop.count
            val pointsGained = clearedCount * 15 * currentCombo
            recordModeScore(pointsGained)
            addCoins(minOf(clearedCount * 2, 25))
            updateMissionProgress(MissionType.SCORE_TOTAL, pointsGained)

            when (_activeGameMode.value) {
              GameMode.CLASSIC -> {
                _remainingGoal.value = maxOf(0, _remainingGoal.value - clearedCount)
              }
              GameMode.TIME_ATTACK -> {
                if (!_isTimeUp.value) {
                  val bonusSec = if (currentCombo >= 3) 4 else 3
                  _timeRemainingSec.value = (_timeRemainingSec.value + bonusSec).coerceAtMost(120)
                  showFloatingText("⏱️ +${bonusSec}s!", popPos.x, popPos.y + 18f, Color(0xFF00E676), 22f)
                }
              }
              GameMode.ENDLESS -> {
                endlessClearedStacks++
                if (endlessClearedStacks % 5 == 0) {
                  _endlessWave.value += 1
                  addCoins(30)
                  if (!_isLowPerformanceMode.value) {
                    launchComboFireworks(comboLevel = 3)
                  }
                  showFloatingText("♾️ WAVE ${_endlessWave.value}! +30 🪙", boardCenter.x, boardCenter.y - 50f, Color(0xFFFFD600), 25f)
                }
              }
            }

            val afterClearMap = _boardCells.value.toMutableMap()
            val targetCellNow = afterClearMap[targetCoord]
            if (targetCellNow != null) {
              afterClearMap[targetCoord] = targetCellNow.copy(
                stack = targetCellNow.stack.dropLast(1),
                popScale = 1.0f
              )
              _boardCells.value = afterClearMap
            }

            // Stagger Combo chime right after clear burst so sounds layer cleanly instead of colliding
            if (currentCombo > 1) {
              delay(90)
              audioPlayer.play(SoundEffect.ComboStreak(currentCombo))
            }

            delay(75)
            checkAdjacentObstacles(targetCoord)
            delay(130)
          } else {
            delay(70)
          }
        }
      }

      isCascading = false
      _combo.value = 1

      checkPostActionStatus()
    }
  }

  private suspend fun checkPostActionStatus() {
    if (_activeGameMode.value == GameMode.CLASSIC &&
      !_isLevelComplete.value &&
      _remainingGoal.value <= 0 &&
      _secondaryGoalRemaining.value <= 0
    ) {
      delay(260)
      _isLevelComplete.value = true
      audioPlayer.play(SoundEffect.VictoryFanfare)
      if (!_isLowPerformanceMode.value) {
        launchComboFireworks(comboLevel = 6)
        triggerScreenShake(intensity = 0.85f)
      }
      addCoins(100)
      addGems(5)
      updateMissionProgress(MissionType.LEVEL_COMPLETE, 1)
      val nextUnlockedLevel = _currentLevel.value + 1
      if (nextUnlockedLevel > _highestLevel.value) {
        _highestLevel.value = nextUnlockedLevel
        prefs.edit().putInt("highest_level", nextUnlockedLevel).apply()
      }
      requestCloudSync()
    } else if (!_isTimeUp.value && !_isLevelComplete.value) {
      checkGameOverCondition()
    }
  }

  private fun recordClearedObstacle(obs: CellObstacle) {
    val secType = _levelDef.value.secondaryGoalType
    when (obs) {
      is CellObstacle.CoinReward -> {
        addCoins(obs.coins)
        if (secType == "Coins") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - obs.coins)
        }
      }
      is CellObstacle.RobotToy -> {
        if (secType == null || secType == "Robots") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.Honeycomb -> {
        if (secType == null || secType == "Honeycombs") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.Cake -> {
        if (secType == null || secType == "Cakes") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.Lightning -> {
        if (secType == null || secType == "Lightning") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.StarToken -> {
        if (secType == null || secType == "Stars") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.SleepyGhost -> {
        if (secType == null || secType == "Ghosts") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      is CellObstacle.Cloud -> {
        if (secType == null || secType == "Clouds") {
          _secondaryGoalRemaining.value = maxOf(0, _secondaryGoalRemaining.value - 1)
        }
      }
      CellObstacle.None -> {}
    }
    if (obs !is CellObstacle.None) {
      updateMissionProgress(MissionType.OBSTACLE_CLEAR, 1)
    }
  }

  private fun checkAdjacentObstacles(clearedCoord: HexCoord) {
    val map = _boardCells.value.toMutableMap()
    for (neighbor in clearedCoord.getNeighbors()) {
      val cell = map[neighbor] ?: continue
      when (val obs = cell.obstacle) {
        is CellObstacle.CoinReward -> {
          audioPlayer.play(SoundEffect.CoinCollect)
          recordClearedObstacle(obs)
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          showFloatingText("+${obs.coins} COINS", pos.x, pos.y, Color(0xFFFFD600), 24f)
          spawnCoinParticles(pos.x, pos.y)
          map[neighbor] = cell.copy(obstacle = CellObstacle.None)
        }
        is CellObstacle.SleepyGhost -> {
          val nextHits = obs.remainingHits - 1
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          if (nextHits <= 0) {
            audioPlayer.play(SoundEffect.StarPop)
            showFloatingText("WOKE UP! ⭐", pos.x, pos.y, Color(0xFF00E5FF), 24f)
            spawnStarParticles(pos.x, pos.y)
            recordClearedObstacle(obs)
            map[neighbor] = cell.copy(obstacle = CellObstacle.None)
          } else {
            map[neighbor] = cell.copy(obstacle = CellObstacle.SleepyGhost(nextHits))
          }
        }
        is CellObstacle.Honeycomb -> {
          val nextHits = obs.remainingHits - 1
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          if (nextHits <= 0) {
            audioPlayer.play(SoundEffect.StackMerge)
            showFloatingText("CRACKED! 🍯", pos.x, pos.y, Color(0xFFFF9100), 24f)
            spawnExplosion(pos.x, pos.y, Color(0xFFFFB300), count = 20)
            recordClearedObstacle(obs)
            map[neighbor] = cell.copy(obstacle = CellObstacle.None)
          } else {
            map[neighbor] = cell.copy(obstacle = CellObstacle.Honeycomb(nextHits))
          }
        }
        is CellObstacle.Cake -> {
          val nextHits = obs.remainingHits - 1
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          if (nextHits <= 0) {
            audioPlayer.play(SoundEffect.StackFullClear)
            showFloatingText("YUMMY! 🎂", pos.x, pos.y, Color(0xFFFF4081), 24f)
            spawnExplosion(pos.x, pos.y, Color(0xFFFF80AB), count = 22)
            recordClearedObstacle(obs)
            map[neighbor] = cell.copy(obstacle = CellObstacle.None)
          } else {
            map[neighbor] = cell.copy(obstacle = CellObstacle.Cake(nextHits))
          }
        }
        is CellObstacle.Cloud -> {
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          audioPlayer.play(SoundEffect.StackFullClear)
          showFloatingText("CLEARED! ☁️", pos.x, pos.y, Color(0xFF80D8FF), 24f)
          spawnExplosion(pos.x, pos.y, Color(0xFFB3E5FC), count = 20)
          recordClearedObstacle(obs)
          map[neighbor] = cell.copy(obstacle = CellObstacle.None)
        }
        is CellObstacle.StarToken -> {
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          audioPlayer.play(SoundEffect.StarPop)
          showFloatingText("STAR! ⭐", pos.x, pos.y, Color(0xFFFFD600), 24f)
          spawnStarParticles(pos.x, pos.y)
          recordClearedObstacle(obs)
          map[neighbor] = cell.copy(obstacle = CellObstacle.None)
        }
        is CellObstacle.Lightning -> {
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          audioPlayer.play(SoundEffect.HammerSmash)
          showFloatingText("LIGHTNING! ⚡", pos.x, pos.y, Color(0xFF00E5FF), 26f)
          spawnExplosion(pos.x, pos.y, Color(0xFF00E5FF), count = 30)
          recordClearedObstacle(obs)
          map[neighbor] = cell.copy(obstacle = CellObstacle.None)
        }
        is CellObstacle.RobotToy -> {
          val nextHits = obs.remainingHits - 1
          val pos = HexMath.hexToPixel(neighbor, hexRadius, boardCenter)
          if (nextHits <= 0) {
            audioPlayer.play(SoundEffect.StarPop)
            showFloatingText("SAVED ROBOT! 🤖", pos.x, pos.y, Color(0xFF00E5FF), 26f)
            spawnStarParticles(pos.x, pos.y)
            spawnExplosion(pos.x, pos.y, Color(0xFF0288D1), count = 25)
            recordClearedObstacle(obs)
            map[neighbor] = cell.copy(obstacle = CellObstacle.None)
          } else {
            map[neighbor] = cell.copy(obstacle = CellObstacle.RobotToy(nextHits))
          }
        }
        CellObstacle.None -> {}
      }
    }
    _boardCells.value = map
  }

  private fun checkGameOverCondition() {
    val map = _boardCells.value
    val hasEmptyPedestal = map.values.any { it.isEmpty }
    val hasStagedStacks = _stagedStacks.value.any { it != null }

    if (!hasEmptyPedestal && hasStagedStacks) {
      _isGameOver.value = true
      audioPlayer.play(SoundEffect.GameOverAlert)
    }
  }

  private fun replenishStagedStacks(force: Boolean) {
    val current = _stagedStacks.value.toMutableList()
    val colors = _levelDef.value.allowedColors
    val isFreshLevel1 = force && _currentLevel.value == 1 && _score.value == 0
    for (i in 0 until 3) {
      if (force || current[i] == null) {
        if (isFreshLevel1) {
          val tiles = when (i) {
            0 -> listOf(
              HexTile(TileColor.GREEN, 1),
              HexTile(TileColor.YELLOW, 1),
              HexTile(TileColor.BLUE, 1)
            )
            1 -> listOf(
              HexTile(TileColor.ORANGE, 1),
              HexTile(TileColor.YELLOW, 1),
              HexTile(TileColor.GREEN, 1)
            )
            else -> listOf(
              HexTile(TileColor.MAGENTA, 1),
              HexTile(TileColor.PURPLE, 2)
            )
          }
          current[i] = StagedStack(tiles = tiles, pedestalIndex = i)
        } else {
          val numLayers = if (Math.random() < 0.60) 2 else 1
          val tiles = mutableListOf<HexTile>()
          for (l in 0 until numLayers) {
            val prevColor = tiles.lastOrNull()?.color
            val nextColor = colors.filter { it != prevColor }.randomOrNull() ?: colors.random()
            tiles.add(HexTile(nextColor, (1..4).random()))
          }
          current[i] = StagedStack(tiles = tiles, pedestalIndex = i)
        }
      }
    }
    _stagedStacks.value = current
  }

  fun addCoins(amount: Int) {
    _coins.value += amount
    prefs.edit().putInt("coins", _coins.value).apply()
  }

  fun addGems(amount: Int) {
    _gems.value += amount
    prefs.edit().putInt("gems", _gems.value).apply()
  }

  fun redeemGemsForCoins(gemCost: Int = 20, coinsGain: Int = 400): Boolean {
    if (_gems.value < gemCost) return false
    _gems.value -= gemCost
    prefs.edit().putInt("gems", _gems.value).apply()
    addCoins(coinsGain)
    audioPlayer.play(SoundEffect.CoinCollect)
    if (_currentScreen.value == ScreenState.GAME) {
      spawnCoinParticles(boardCenter.x, boardCenter.y)
    }
    showFloatingText("+$coinsGain COINS! 🪙", boardCenter.x, boardCenter.y, Color(0xFFFFD600), 22f)
    requestCloudSync()
    return true
  }

  fun togglePause() {
    _isPaused.value = !_isPaused.value
    audioPlayer.play(SoundEffect.TileTap)
  }

  fun pauseForBackground() {
    if (_currentScreen.value == ScreenState.GAME &&
      !_isLevelComplete.value &&
      !_isGameOver.value &&
      !_isTimeUp.value
    ) {
      _isPaused.value = true
    }
  }

  fun showFloatingText(text: String, x: Float, y: Float, color: Color, sizeSp: Float = 24f) {
    _floatingTexts.value = _floatingTexts.value + FloatingText(
      text = text,
      x = x,
      y = y,
      color = color,
      fontSizeSp = sizeSp
    )
  }

  private fun appendParticlesCapped(newParticles: List<Particle>) {
    if (newParticles.isEmpty()) return
    val maxParticles = 180
    val combined = _particles.value + newParticles
    _particles.value = if (combined.size > maxParticles) {
      combined.takeLast(maxParticles)
    } else {
      combined
    }
  }

  private fun spawnMergeSparks(cx: Float, cy: Float, color: Color, mergedCount: Int) {
    if (_isLowPerformanceMode.value) return
    val sparkCount = (12 + minOf(mergedCount, 8)).coerceAtMost(20)
    val now = System.currentTimeMillis()
    val newParticles = mutableListOf<Particle>()

    // Crisp radial shockwave ring on merge
    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = color,
        size = 6f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 340L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * 1.45f
      )
    )

    for (i in 0 until sparkCount) {
      val angle = Math.toRadians((360.0 / sparkCount) * i + (Math.random() * 24.0 - 12.0))
      val speed = 220f + Math.random().toFloat() * 260f
      newParticles.add(
        Particle(
          x = cx,
          y = cy,
          vx = (cos(angle) * speed).toFloat(),
          vy = (sin(angle) * speed).toFloat() - 35f,
          color = if (i % 3 == 0) Color.White else color,
          size = 8f + Math.random().toFloat() * 7f,
          shape = if (i % 4 == 0) ParticleShape.HEXAGON else ParticleShape.SPARK,
          lifeMs = 460L,
          spawnTime = now,
          gravity = 240f,
          drag = 0.92f,
          spinRate = if (i % 2 == 0) 240f else -240f
        )
      )
    }
    appendParticlesCapped(newParticles)
  }

  fun spawnExplosion(cx: Float, cy: Float, color: Color, count: Int = 24) {
    if (_isLowPerformanceMode.value) return
    val now = System.currentTimeMillis()
    val newParticles = mutableListOf<Particle>()

    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = color,
        size = 8f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 420L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * 1.85f
      )
    )

    for (i in 0 until count) {
      val angle = Math.toRadians((360.0 / count) * i + (Math.random() * 20.0 - 10.0))
      val speed = (220f + Math.random().toFloat() * 340f)
      val shape = when {
        i % 4 == 0 -> ParticleShape.SPARK
        i % 3 == 0 -> ParticleShape.STAR
        i % 5 == 0 -> ParticleShape.HEXAGON
        else -> ParticleShape.CONFETTI
      }
      newParticles.add(
        Particle(
          x = cx,
          y = cy,
          vx = (cos(angle) * speed).toFloat(),
          vy = (sin(angle) * speed).toFloat() - 50f,
          color = if (i % 3 == 0) Color.White else color,
          size = (10f + Math.random().toFloat() * 12f),
          shape = shape,
          lifeMs = 680L,
          spawnTime = now,
          gravity = 320f,
          drag = 0.94f,
          spinRate = ((Math.random().toFloat() - 0.5f) * 540f)
        )
      )
    }
    appendParticlesCapped(newParticles)
  }

  private fun spawnFireworksBurst(cx: Float, cy: Float, accentColor: Color, comboLevel: Int) {
    if (_isLowPerformanceMode.value) return
    val now = System.currentTimeMillis()
    val palette = listOf(
      accentColor,
      Color(0xFFFFD600),
      Color(0xFFFF2D87),
      Color(0xFF00E5FF),
      Color(0xFF00E676),
      Color.White
    )
    val count = (30 + minOf(comboLevel * 3, 24)).coerceAtMost(54)
    val newParticles = mutableListOf<Particle>()

    // Glowing double shockwave pulse
    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = Color.White,
        size = 10f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 460L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * (1.8f + minOf(comboLevel, 8) * 0.12f)
      )
    )
    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = accentColor,
        size = 7f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 560L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * (2.3f + minOf(comboLevel, 8) * 0.14f)
      )
    )

    for (i in 0 until count) {
      val angle = Math.toRadians((360.0 / count) * i + (Math.random() * 18.0 - 9.0))
      val isOuterRing = (i % 2 == 0)
      val speed = if (isOuterRing) (320f + Math.random().toFloat() * 320f) else (160f + Math.random().toFloat() * 210f)
      val shape = when {
        isOuterRing && i % 4 == 0 -> ParticleShape.SPARK
        i % 5 == 0 -> ParticleShape.STAR
        i % 3 == 0 -> ParticleShape.CONFETTI
        else -> ParticleShape.SPARK
      }
      newParticles.add(
        Particle(
          x = cx,
          y = cy,
          vx = (cos(angle) * speed).toFloat(),
          vy = (sin(angle) * speed).toFloat() - 75f,
          color = palette[i % palette.size],
          size = if (shape == ParticleShape.SPARK) (9f + Math.random().toFloat() * 6f) else (11f + Math.random().toFloat() * 11f),
          shape = shape,
          lifeMs = if (isOuterRing) 780L else 640L,
          spawnTime = now,
          gravity = 360f,
          drag = 0.93f,
          spinRate = ((Math.random().toFloat() - 0.5f) * 620f)
        )
      )
    }
    appendParticlesCapped(newParticles)
  }

  private fun launchComboFireworks(comboLevel: Int) {
    if (_isLowPerformanceMode.value) return
    val burstCount = when {
      comboLevel >= 12 -> 5
      comboLevel >= 7 -> 4
      comboLevel >= 4 -> 3
      else -> 2
    }
    val fireworkColors = listOf(
      Color(0xFFFFD600),
      Color(0xFF00E5FF),
      Color(0xFFFF2D87),
      Color(0xFF00E676),
      Color(0xFFFF9100)
    )
    viewModelScope.launch {
      for (b in 0 until burstCount) {
        delay(85L)
        val spreadX = boardCenter.x + (Math.random().toFloat() - 0.5f) * hexRadius * 4.4f
        val spreadY = boardCenter.y + (Math.random().toFloat() - 0.65f) * hexRadius * 3.6f
        spawnFireworksBurst(spreadX, spreadY, fireworkColors[b % fireworkColors.size], comboLevel = 1)
      }
    }
  }

  private fun spawnCoinParticles(cx: Float, cy: Float) {
    if (_isLowPerformanceMode.value) return
    val now = System.currentTimeMillis()
    val newParticles = mutableListOf<Particle>()
    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = Color(0xFFFFD600),
        size = 6f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 380L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * 1.4f
      )
    )
    for (i in 0..17) {
      val angle = Math.random() * 2.0 * Math.PI
      val speed = 150f + Math.random().toFloat() * 230f
      val shape = when {
        i % 3 == 0 -> ParticleShape.GEM
        i % 2 == 0 -> ParticleShape.STAR
        else -> ParticleShape.SPARK
      }
      newParticles.add(
        Particle(
          x = cx,
          y = cy,
          vx = (cos(angle) * speed).toFloat(),
          vy = (sin(angle) * speed).toFloat() - 120f,
          color = if (i % 4 == 0) Color.White else if (i % 3 == 0) Color(0xFFFFF59D) else Color(0xFFFFD600),
          size = 13f + Math.random().toFloat() * 7f,
          shape = shape,
          lifeMs = 740L,
          spawnTime = now,
          gravity = 290f,
          drag = 0.94f,
          spinRate = ((Math.random().toFloat() - 0.5f) * 480f)
        )
      )
    }
    appendParticlesCapped(newParticles)
  }

  private fun spawnStarParticles(cx: Float, cy: Float) {
    if (_isLowPerformanceMode.value) return
    val now = System.currentTimeMillis()
    val newParticles = mutableListOf<Particle>()
    newParticles.add(
      Particle(
        x = cx,
        y = cy,
        vx = 0f,
        vy = 0f,
        color = Color(0xFF00E5FF),
        size = 6f,
        shape = ParticleShape.SHOCKWAVE,
        lifeMs = 420L,
        spawnTime = now,
        gravity = 0f,
        drag = 1f,
        maxRadius = hexRadius * 1.55f
      )
    )
    for (i in 0..19) {
      val angle = Math.random() * 2.0 * Math.PI
      val speed = 180f + Math.random().toFloat() * 250f
      val shape = when {
        i % 2 == 0 -> ParticleShape.GEM
        i % 3 == 0 -> ParticleShape.SPARK
        else -> ParticleShape.STAR
      }
      newParticles.add(
        Particle(
          x = cx,
          y = cy,
          vx = (cos(angle) * speed).toFloat(),
          vy = (sin(angle) * speed).toFloat() - 60f,
          color = if (i % 3 == 0) Color(0xFF00E5FF) else if (i % 2 == 0) Color(0xFFFFF176) else Color.White,
          size = 14f + Math.random().toFloat() * 8f,
          shape = shape,
          lifeMs = 800L,
          spawnTime = now,
          gravity = 240f,
          drag = 0.94f,
          spinRate = ((Math.random().toFloat() - 0.5f) * 420f)
        )
      )
    }
    appendParticlesCapped(newParticles)
  }

  fun tickParticles(deltaSec: Float) {
    if (_particles.value.isEmpty() && _floatingTexts.value.isEmpty()) return
    val now = System.currentTimeMillis()
    if (_particles.value.isNotEmpty()) {
      val dtFrames = (deltaSec * 60.0).coerceIn(0.2, 2.5)
      val drag92 = Math.pow(0.92, dtFrames).toFloat()
      val drag94 = Math.pow(0.94, dtFrames).toFloat()
      _particles.value = _particles.value.mapNotNull { p ->
        val age = now - p.spawnTime
        if (age >= p.lifeMs) {
          null
        } else {
          val progress = (age.toFloat() / p.lifeMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
          val damp = when {
            p.drag >= 0.999f -> 1f
            p.drag <= 0.925f -> drag92
            else -> drag94
          }
          val nextVx = p.vx * damp
          val nextVy = (p.vy + p.gravity * deltaSec) * damp
          p.copy(
            prevX = p.x,
            prevY = p.y,
            x = p.x + nextVx * deltaSec,
            y = p.y + nextVy * deltaSec,
            vx = nextVx,
            vy = nextVy,
            rotation = p.rotation + p.spinRate * deltaSec,
            alpha = (1f - progress * progress).coerceIn(0f, 1f)
          )
        }
      }
    }
    if (_floatingTexts.value.isNotEmpty()) {
      _floatingTexts.value = _floatingTexts.value.filter { !it.isDead(now) }
    }
  }

  fun updateSoundSettings(sfxVol: Float, musVol: Float, soundOn: Boolean, musicOn: Boolean, hapticsOn: Boolean) {
    audio.setSoundVolume(sfxVol)
    audio.setMusicVolume(musVol)
    audio.setSoundEnabled(soundOn)
    audio.setMusicEnabled(musicOn)
    audio.setHapticsEnabled(hapticsOn)

    audioPlayer.setMasterVolume(sfxVol)
    audioPlayer.setMusicVolume(musVol)
    audioPlayer.setSfxEnabled(soundOn)
    audioPlayer.setMusicEnabled(musicOn)
    audioPlayer.setHapticsEnabled(hapticsOn)

    prefs.edit()
      .putFloat("sound_volume", sfxVol)
      .putFloat("music_volume", musVol)
      .putBoolean("sound_enabled", soundOn)
      .putBoolean("music_enabled", musicOn)
      .putBoolean("haptics_enabled", hapticsOn)
      .apply()
  }

  private var shakeJob: Job? = null

  fun triggerScreenShake(intensity: Float = 1.0f) {
    if (_isLowPerformanceMode.value) return
    shakeJob?.cancel()
    val scale = intensity.coerceIn(0.25f, 2.2f)
    shakeJob = viewModelScope.launch {
      val basePattern = listOf(
        Offset(10f, -8f),
        Offset(-9f, 7f),
        Offset(7f, 6f),
        Offset(-6f, -5f),
        Offset(4f, -3f),
        Offset(-2f, 2f),
        Offset.Zero
      )
      for (off in basePattern) {
        _screenShake.value = Offset(off.x * scale, off.y * scale)
        delay(28)
      }
    }
  }

  fun toggleColorblindMode() {
    val newMode = !_isColorblindMode.value
    _isColorblindMode.value = newMode
    prefs.edit().putBoolean("colorblind_mode", newMode).apply()
  }

  fun buyNoAds() {
    _hasNoAds.value = true
    prefs.edit().putBoolean("has_no_ads", true).apply()
    audioPlayer.play(SoundEffect.CoinCollect)
    showFloatingText("NO ADS UNLOCKED! 🛡️", boardCenter.x, boardCenter.y, Color(0xFF00E676), 24f)
    requestCloudSync()
  }

  fun requestCloudSync() {
    _cloudSyncTrigger.value += 1
  }

  fun buildLocalCloudSave(displayName: String): PlayerCloudSave {
    val stats = _profileStats.value
    return PlayerCloudSave(
      displayName = displayName.trim().ifBlank { "Hexa Player" }.take(40),
      currentLevel = _currentLevel.value.toLong().coerceAtLeast(1L),
      highestLevel = maxOf(_highestLevel.value, _currentLevel.value).toLong().coerceAtLeast(1L),
      coins = _coins.value.toLong().coerceAtLeast(0L),
      gems = _gems.value.toLong().coerceAtLeast(0L),
      totalScore = _careerTotalScore.value.coerceAtLeast(0L),
      bestTimeAttackScore = _timeAttackBestScore.value.toLong().coerceAtLeast(0L),
      bestEndlessScore = _endlessBestScore.value.toLong().coerceAtLeast(0L),
      totalLevelsCompleted = stats.totalLevelsCompleted.toLong().coerceAtLeast(0L),
      totalMerges = stats.totalMerges.toLong().coerceAtLeast(0L),
      totalStacksCleared = stats.totalStacksCleared.toLong().coerceAtLeast(0L),
      bestComboEver = stats.bestComboEver.toLong().coerceAtLeast(1L),
      totalObstaclesCleared = stats.totalObstaclesCleared.toLong().coerceAtLeast(0L),
      consecutiveDays = _consecutiveDays.value.toLong().coerceAtLeast(1L),
      unlockedTrophies = stats.unlockedTrophies.toLong().coerceAtLeast(0L),
      hasNoAds = _hasNoAds.value,
      isColorblindMode = _isColorblindMode.value
    )
  }

  fun applyMergedCloudSave(merged: PlayerCloudSave) {
    val mergedCurrentLevel = maxOf(_currentLevel.value, merged.currentLevel.toInt()).coerceAtLeast(1)
    val mergedHighestLevel = maxOf(_highestLevel.value, merged.highestLevel.toInt(), mergedCurrentLevel).coerceAtLeast(1)
    val mergedCoins = maxOf(_coins.value, merged.coins.toInt())
    val mergedGems = maxOf(_gems.value, merged.gems.toInt())
    val mergedCareerScore = maxOf(_careerTotalScore.value, merged.totalScore)
    val mergedBestTa = maxOf(_timeAttackBestScore.value, merged.bestTimeAttackScore.toInt())
    val mergedBestEndless = maxOf(_endlessBestScore.value, merged.bestEndlessScore.toInt())
    val mergedConsecutive = maxOf(_consecutiveDays.value, merged.consecutiveDays.toInt()).coerceAtLeast(1)
    val mergedNoAds = _hasNoAds.value || merged.hasNoAds
    val mergedColorblind = _isColorblindMode.value || merged.isColorblindMode

    val mergedLevelsCompleted = maxOf(prefs.getInt("stat_total_levels", 0), merged.totalLevelsCompleted.toInt())
    val mergedMerges = maxOf(prefs.getInt("stat_total_merges", 0), merged.totalMerges.toInt())
    val mergedStacks = maxOf(prefs.getInt("stat_total_stacks", 0), merged.totalStacksCleared.toInt())
    val mergedBestCombo = maxOf(prefs.getInt("stat_best_combo", 1), merged.bestComboEver.toInt()).coerceAtLeast(1)
    val mergedObstacles = maxOf(prefs.getInt("stat_total_obstacles", 0), merged.totalObstaclesCleared.toInt())

    _currentLevel.value = mergedCurrentLevel
    _highestLevel.value = mergedHighestLevel
    _coins.value = mergedCoins
    _gems.value = mergedGems
    _careerTotalScore.value = mergedCareerScore
    _timeAttackBestScore.value = mergedBestTa
    _endlessBestScore.value = mergedBestEndless
    _consecutiveDays.value = mergedConsecutive
    _hasNoAds.value = mergedNoAds
    _isColorblindMode.value = mergedColorblind

    prefs.edit()
      .putInt("current_level", mergedCurrentLevel)
      .putInt("highest_level", mergedHighestLevel)
      .putInt("coins", mergedCoins)
      .putInt("gems", mergedGems)
      .putLong("career_total_score", mergedCareerScore)
      .putInt("best_time_attack", mergedBestTa)
      .putInt("best_endless", mergedBestEndless)
      .putInt("consecutive_days", mergedConsecutive)
      .putBoolean("has_no_ads", mergedNoAds)
      .putBoolean("colorblind_mode", mergedColorblind)
      .putInt("stat_total_levels", mergedLevelsCompleted)
      .putInt("stat_total_merges", mergedMerges)
      .putInt("stat_total_stacks", mergedStacks)
      .putInt("stat_best_combo", mergedBestCombo)
      .putInt("stat_total_obstacles", mergedObstacles)
      .apply()

    refreshAchievementsFromStats(announceUnlocks = false)
  }

  override fun onCleared() {
    super.onCleared()
    cascadeJob?.cancel()
    timeAttackTimerJob?.cancel()
    audioPlayer.release()
    audio.release()
  }

  companion object {
    const val TIME_ATTACK_DURATION_SEC = 90
  }
}
