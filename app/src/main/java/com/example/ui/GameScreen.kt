package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import com.example.game.GameLevels
import com.example.ads.AdPlacement
import com.example.game.GameViewModel
import com.example.model.BoosterType
import com.example.model.GameMode
import com.example.model.ScreenState
import com.example.ui.theme.FredokaFontFamily

@Composable
fun GameScreen(
  viewModel: GameViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(ScreenState.MENU)
  }

  val levelDef by viewModel.levelDef.collectAsState()
  val coins by viewModel.coins.collectAsState()
  val gems by viewModel.gems.collectAsState()
  val score by viewModel.score.collectAsState()
  val remainingGoal by viewModel.remainingGoal.collectAsState()
  val secondaryGoalRemaining by viewModel.secondaryGoalRemaining.collectAsState()
  val boardCells by viewModel.boardCells.collectAsState()
  val stagedStacks by viewModel.stagedStacks.collectAsState()
  val selectedStagedIndex by viewModel.selectedStagedIndex.collectAsState()
  val activeBooster by viewModel.activeBooster.collectAsState()
  val boosterCounts by viewModel.boosterCounts.collectAsState()
  val activeSlides by viewModel.activeSlides.collectAsState()
  val particles by viewModel.particles.collectAsState()
  val floatingTexts by viewModel.floatingTexts.collectAsState()
  val isPaused by viewModel.isPaused.collectAsState()
  val isLevelComplete by viewModel.isLevelComplete.collectAsState()
  val isGameOver by viewModel.isGameOver.collectAsState()
  val activeGameMode by viewModel.activeGameMode.collectAsState()
  val timeRemainingSec by viewModel.timeRemainingSec.collectAsState()
  val isTimeUp by viewModel.isTimeUp.collectAsState()
  val timeAttackCoinsEarned by viewModel.timeAttackCoinsEarned.collectAsState()
  val isNewBestScore by viewModel.isNewBestScore.collectAsState()
  val timeAttackBest by viewModel.timeAttackBestScore.collectAsState()
  val endlessBest by viewModel.endlessBestScore.collectAsState()
  val endlessWave by viewModel.endlessWave.collectAsState()

  // New states
  val canUndo by viewModel.canUndo.collectAsState()
  val activeHint by viewModel.activeHint.collectAsState()
  val draggedStack by viewModel.draggedStack.collectAsState()
  val dragPosition by viewModel.dragPosition.collectAsState()
  val hoveredCoord by viewModel.hoveredCoord.collectAsState()
  val snapStrength by viewModel.snapStrength.collectAsState()
  val isLowPerfMode by viewModel.isLowPerformanceMode.collectAsState()
  val consecutiveDays by viewModel.consecutiveDays.collectAsState()
  val canClaimDaily by viewModel.canClaimDailyReward.collectAsState()
  val hasNoAds by viewModel.hasNoAds.collectAsState()

  val isColorblindMode by viewModel.isColorblindMode.collectAsState()
  val screenShake by viewModel.screenShake.collectAsState()
  val hammerStrikePos by viewModel.hammerStrikePos.collectAsState()

  val dailyMissions by viewModel.dailyMissions.collectAsState()
  val isGrandChestClaimed by viewModel.isGrandChestClaimed.collectAsState()
  val unclaimedMissions by viewModel.unclaimedMissionCount.collectAsState()
  val achievements by viewModel.achievements.collectAsState()
  val unclaimedAchievements by viewModel.unclaimedAchievementCount.collectAsState()
  val profileStats by viewModel.profileStats.collectAsState()

  var showShop by remember { mutableStateOf(false) }
  var showDailyReward by remember { mutableStateOf(false) }
  var showDailyMissions by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("game_screen")
  ) {
    // 1. The 3D Canvas Board with Drag Preview & Hover Highlights
    HexBoardCanvas(
      viewModel = viewModel,
      boardCells = boardCells,
      activeSlides = activeSlides,
      particles = particles,
      floatingTexts = floatingTexts,
      activeBooster = activeBooster,
      activeHint = activeHint,
      hoveredCoord = hoveredCoord,
      draggedStack = draggedStack,
      dragPosition = dragPosition,
      snapStrength = snapStrength,
      isLowPerformanceMode = isLowPerfMode,
      isColorblindMode = isColorblindMode,
      screenShake = screenShake,
      hammerStrikePos = hammerStrikePos,
      gameMode = activeGameMode,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Foreground UI Controls
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Top Header
      val currentModeBest = when (activeGameMode) {
        GameMode.TIME_ATTACK -> timeAttackBest
        GameMode.ENDLESS -> endlessBest
        GameMode.CLASSIC -> 0
      }
      TopHeaderBar(
        coins = coins,
        remainingGoal = remainingGoal,
        secondaryGoalType = levelDef.secondaryGoalType,
        secondaryGoalCount = secondaryGoalRemaining,
        levelNumber = levelDef.levelNumber,
        gameMode = activeGameMode,
        score = score,
        timeRemainingSec = timeRemainingSec,
        endlessWave = endlessWave,
        bestScore = currentModeBest,
        onPauseClick = { viewModel.togglePause() },
        onCoinsClick = { showShop = true },
        onDailyClick = { showDailyReward = true },
        canClaimDaily = canClaimDaily,
        onMissionsClick = { showDailyMissions = true },
        unclaimedMissions = unclaimedMissions + unclaimedAchievements
      )

      // Active Booster Notice Banner
      AnimatedVisibility(
        visible = activeBooster != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            modifier = Modifier
              .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFFD50000))
              .background(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFF3D57), Color(0xFFD50032))),
                shape = RoundedCornerShape(18.dp)
              )
              .border(1.8.dp, Color(0xFFFFE57F), RoundedCornerShape(18.dp))
              .clickable { viewModel.activateBooster(BoosterType.HAMMER) }
              .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.Hardware,
              contentDescription = "Hammer Active",
              tint = Color(0xFFFFE57F),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "HAMMER ACTIVE: Tap any stack to smash!",
              color = Color.White,
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Cancel Hammer",
              tint = Color.White.copy(alpha = 0.9f),
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Staging Area with Drag & Drop (<100ms) and Tap
      StagingArea(
        stagedStacks = stagedStacks,
        selectedIndex = selectedStagedIndex,
        draggedPedestalIndex = draggedStack?.pedestalIndex,
        onStackSelected = { viewModel.selectStagedStack(it) },
        onDragStart = { idx, pos -> viewModel.onDragStart(idx, pos) },
        onDragMove = { pos -> viewModel.onDragMove(pos) },
        onDragEnd = { viewModel.onDragEnd() },
        isColorblindMode = isColorblindMode
      )

      // Boosters Dock (Undo, Shuffle, Hint, Hammer)
      BoostersBar(
        activeBooster = activeBooster,
        boosterCounts = boosterCounts,
        canUndo = canUndo,
        coins = coins,
        onBoosterClick = { viewModel.activateBooster(it) }
      )

      Spacer(modifier = Modifier.height(8.dp))

      BannerAdSlot(
        placement = AdPlacement.GAME_BOTTOM_BANNER,
        hasNoAds = hasNoAds
      )
    }

    // 3. Dialog Overlays
    if (isPaused) {
      SettingsDialog(
        currentLevel = levelDef.levelNumber,
        totalLevels = GameLevels.totalLevels,
        audio = viewModel.audio,
        isLowPerfMode = isLowPerfMode,
        isColorblindMode = isColorblindMode,
        gameMode = activeGameMode,
        onToggleLowPerfMode = { viewModel.toggleLowPerformanceMode() },
        onToggleColorblindMode = { viewModel.toggleColorblindMode() },
        onSaveAudioSettings = { sfx, mus, sOn, mOn, hOn ->
          viewModel.updateSoundSettings(sfx, mus, sOn, mOn, hOn)
        },
        onResume = { viewModel.togglePause() },
        onRestart = {
          viewModel.togglePause()
          viewModel.restartCurrentLevel()
        },
        onSelectLevel = { lvl ->
          viewModel.togglePause()
          viewModel.loadLevel(lvl)
        },
        onMainMenu = {
          viewModel.togglePause()
          viewModel.navigateTo(ScreenState.MENU)
        }
      )
    }

    if (showDailyReward) {
      DailyRewardDialog(
        consecutiveDays = consecutiveDays,
        canClaim = canClaimDaily,
        onClaim = { viewModel.claimDailyReward() },
        onClose = { showDailyReward = false }
      )
    }

    if (showDailyMissions) {
      DailyMissionsDialog(
        missions = dailyMissions,
        isGrandChestClaimed = isGrandChestClaimed,
        achievements = achievements,
        profileStats = profileStats,
        coins = coins,
        gems = gems,
        onClaimMission = { missionId ->
          viewModel.claimMission(missionId)
        },
        onClaimGrandChest = {
          viewModel.claimGrandChest()
        },
        onClaimAchievement = { achievementId ->
          viewModel.claimAchievement(achievementId)
        },
        onClose = { showDailyMissions = false }
      )
    }

    // 3. High-Fidelity Animated Compose Overlays with Transitions
    AnimatedLevelCompleteOverlay(
      visible = isLevelComplete,
      levelNumber = levelDef.levelNumber,
      score = score,
      coinsEarned = 100,
      onNextLevel = { viewModel.nextLevel() },
      onReplay = { viewModel.restartCurrentLevel() },
      onHome = { viewModel.navigateTo(ScreenState.MENU) },
      onPlaySound = { sound ->
        when (sound) {
          "star1" -> viewModel.audioPlayer.play(com.example.audio.SoundEffect.StarPop, pitch = 1.0f)
          "star2" -> viewModel.audioPlayer.play(com.example.audio.SoundEffect.StarPop, pitch = 1.12f)
          "star3" -> viewModel.audioPlayer.play(com.example.audio.SoundEffect.StarPop, pitch = 1.25f)
          else -> viewModel.audioPlayer.play(com.example.audio.SoundEffect.SlideWaveNote(3))
        }
      }
    )

    AnimatedGameOverOverlay(
      visible = isGameOver,
      score = score,
      levelNumber = levelDef.levelNumber,
      gameMode = activeGameMode,
      bestScore = when (activeGameMode) {
        GameMode.TIME_ATTACK -> timeAttackBest
        GameMode.ENDLESS -> endlessBest
        GameMode.CLASSIC -> 0
      },
      hasHammerBooster = (boosterCounts[BoosterType.HAMMER] ?: 0) > 0 || coins >= BoosterType.HAMMER.cost,
      onRetry = { viewModel.restartCurrentLevel() },
      onUseHammer = {
        viewModel.activateBooster(BoosterType.HAMMER)
      },
      onBonusCoins = { viewModel.claimGameOverBonusCoins() },
      onHome = { viewModel.navigateTo(ScreenState.MENU) }
    )

    AnimatedTimeAttackResultOverlay(
      visible = isTimeUp,
      score = score,
      bestScore = timeAttackBest,
      isNewRecord = isNewBestScore,
      coinsEarned = timeAttackCoinsEarned,
      onPlayAgain = { viewModel.startTimeAttack() },
      onHome = { viewModel.navigateTo(ScreenState.MENU) }
    )

    if (showShop) {
      ShopDialog(
        coins = coins,
        gems = gems,
        hasNoAds = hasNoAds,
        onClose = { showShop = false },
        onBuyCoins = { amount ->
          showShop = false
          viewModel.claimShopCoinReward(amount)
        },
        onBuyNoAds = {
          viewModel.buyNoAds()
          showShop = false
        },
        onRedeemGems = {
          viewModel.redeemGemsForCoins()
        }
      )
    }
  }
}
