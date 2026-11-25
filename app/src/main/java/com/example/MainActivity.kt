package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cloud.CloudProgressRepository
import com.example.cloud.LeaderboardViewModel
import com.example.cloud.attemptAutoSignIn
import com.example.cloud.authStateFlow
import com.example.game.GameLevels
import com.example.game.GameViewModel
import com.example.model.CloudSyncStatus
import com.example.model.ScreenState
import com.example.ui.AdSlotTransitionHost
import com.example.ui.DailyMissionsDialog
import com.example.ui.DailyRewardDialog
import com.example.ui.GameScreen
import com.example.ui.LeaderboardScreen
import com.example.ui.LevelSelectScreen
import com.example.ui.LoadingScreen
import com.example.ui.MenuScreen
import com.example.ui.SettingsDialog
import com.example.ui.ShopDialog
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val context = LocalContext.current
          val coroutineScope = rememberCoroutineScope()
          val credentialManager = remember(context) { CredentialManager.create(context) }

          val gameViewModel: GameViewModel = viewModel()
          val lifecycleOwner = LocalLifecycleOwner.current
          DisposableEffect(lifecycleOwner, gameViewModel) {
            val observer = LifecycleEventObserver { _, event ->
              when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                  gameViewModel.audioPlayer.stopBgm()
                  gameViewModel.pauseForBackground()
                }
                Lifecycle.Event.ON_RESUME -> {
                  if (gameViewModel.audioPlayer.isMusicOn()) {
                    gameViewModel.audioPlayer.startBgm()
                  }
                }
                else -> {}
              }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
              lifecycleOwner.lifecycle.removeObserver(observer)
            }
          }

          val currentUser by Firebase.auth.authStateFlow()
            .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

          LaunchedEffect(Unit) {
            attemptAutoSignIn(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = { gameViewModel.requestCloudSync() },
              onUnauthenticated = {},
              scope = coroutineScope
            )
          }

          val leaderboardViewModel: LeaderboardViewModel? = if (currentUser != null) {
            val uid = currentUser!!.uid
            key(uid) {
              viewModel(
                key = "leaderboard_vm_$uid",
                factory = viewModelFactory {
                  initializer {
                    val app = context.applicationContext
                    val dbId = app.getString(R.string.firestore_database_id)
                    val firestore = FirebaseFirestore.getInstance(dbId)
                    LeaderboardViewModel(
                      repository = CloudProgressRepository(firestore, Firebase.auth),
                      currentUserId = uid
                    )
                  }
                }
              )
            }
          } else {
            null
          }

          val cloudSyncTrigger by gameViewModel.cloudSyncTrigger.collectAsState()
          if (currentUser != null && leaderboardViewModel != null) {
            val uid = currentUser!!.uid
            val playerDisplayName = currentUser!!.displayName?.takeIf { it.isNotBlank() } ?: "Hexa Player"
            LaunchedEffect(uid, cloudSyncTrigger) {
              leaderboardViewModel.syncCloudProgress(
                localSave = gameViewModel.buildLocalCloudSave(playerDisplayName),
                stats = gameViewModel.profileStats.value,
                onMerged = { merged ->
                  gameViewModel.applyMergedCloudSave(merged)
                }
              )
            }
          }

          val cloudSyncStatus = if (leaderboardViewModel != null) {
            leaderboardViewModel.syncStatus.collectAsStateWithLifecycle().value
          } else {
            CloudSyncStatus.IDLE
          }

          val currentScreen by gameViewModel.currentScreen.collectAsState()
          val currentLevel by gameViewModel.currentLevel.collectAsState()
          val coins by gameViewModel.coins.collectAsState()
          val gems by gameViewModel.gems.collectAsState()
          val consecutiveDays by gameViewModel.consecutiveDays.collectAsState()
          val canClaimDaily by gameViewModel.canClaimDailyReward.collectAsState()
          val isLowPerfMode by gameViewModel.isLowPerformanceMode.collectAsState()
          val isColorblindMode by gameViewModel.isColorblindMode.collectAsState()
          val hasNoAds by gameViewModel.hasNoAds.collectAsState()
          val dailyMissions by gameViewModel.dailyMissions.collectAsState()
          val isGrandChestClaimed by gameViewModel.isGrandChestClaimed.collectAsState()
          val achievements by gameViewModel.achievements.collectAsState()
          val profileStats by gameViewModel.profileStats.collectAsState()
          val selectedGameMode by gameViewModel.selectedGameMode.collectAsState()
          val isAdSlotActive by gameViewModel.isAdSlotActive.collectAsState()
          val activeAdPlacement by gameViewModel.activeAdPlacement.collectAsState()

          var showMenuSettings by remember { mutableStateOf(false) }
          var showMenuShop by remember { mutableStateOf(false) }
          var showMenuDaily by remember { mutableStateOf(false) }
          var showMenuMissions by remember { mutableStateOf(false) }
          var missionsInitialTab by remember { mutableIntStateOf(0) }

          Crossfade(
            targetState = currentScreen,
            animationSpec = tween(durationMillis = 360),
            label = "screen_crossfade"
          ) { screen ->
            when (screen) {
              ScreenState.LOADING -> {
                LoadingScreen(
                  onLoadingComplete = { gameViewModel.finishLoading() },
                  onSupportTap = { gameViewModel.audioPlayer.playTap() }
                )
              }

              ScreenState.MENU -> {
                MenuScreen(
                  viewModel = gameViewModel,
                  onOpenSettings = { showMenuSettings = true },
                  onOpenShop = { showMenuShop = true },
                  onOpenDaily = { showMenuDaily = true },
                  onOpenMissions = {
                    missionsInitialTab = 0
                    showMenuMissions = true
                  },
                  onOpenProfile = {
                    missionsInitialTab = 1
                    showMenuMissions = true
                  },
                  onOpenLeaderboard = {
                    gameViewModel.navigateTo(ScreenState.LEADERBOARD)
                  },
                  cloudPlayerName = currentUser?.let {
                    it.displayName?.takeIf { name -> name.isNotBlank() } ?: "Hexa Player"
                  },
                  cloudSyncStatus = cloudSyncStatus
                )

                if (showMenuSettings) {
                  SettingsDialog(
                    currentLevel = currentLevel,
                    totalLevels = GameLevels.totalLevels,
                    audio = gameViewModel.audio,
                    isLowPerfMode = isLowPerfMode,
                    isColorblindMode = isColorblindMode,
                    gameMode = selectedGameMode,
                    onToggleLowPerfMode = { gameViewModel.toggleLowPerformanceMode() },
                    onToggleColorblindMode = { gameViewModel.toggleColorblindMode() },
                    onSaveAudioSettings = { sfx, mus, sOn, mOn, hOn ->
                      gameViewModel.updateSoundSettings(sfx, mus, sOn, mOn, hOn)
                    },
                    onResume = { showMenuSettings = false },
                    onRestart = {
                      showMenuSettings = false
                      gameViewModel.startSelectedMode()
                    },
                    onSelectLevel = { lvl ->
                      showMenuSettings = false
                      gameViewModel.loadLevel(lvl)
                      gameViewModel.navigateTo(ScreenState.GAME)
                    },
                    onMainMenu = { showMenuSettings = false }
                  )
                }

                if (showMenuShop) {
                  ShopDialog(
                    coins = coins,
                    gems = gems,
                    hasNoAds = hasNoAds,
                    onClose = { showMenuShop = false },
                    onBuyCoins = { amount ->
                      showMenuShop = false
                      gameViewModel.claimShopCoinReward(amount)
                    },
                    onBuyNoAds = {
                      gameViewModel.buyNoAds()
                      showMenuShop = false
                    },
                    onRedeemGems = {
                      gameViewModel.redeemGemsForCoins()
                    }
                  )
                }

                if (showMenuDaily) {
                  DailyRewardDialog(
                    consecutiveDays = consecutiveDays,
                    canClaim = canClaimDaily,
                    onClaim = { gameViewModel.claimDailyReward() },
                    onClose = { showMenuDaily = false }
                  )
                }

                if (showMenuMissions) {
                  DailyMissionsDialog(
                    missions = dailyMissions,
                    isGrandChestClaimed = isGrandChestClaimed,
                    achievements = achievements,
                    profileStats = profileStats,
                    coins = coins,
                    gems = gems,
                    initialTab = missionsInitialTab,
                    onClaimMission = { missionId ->
                      gameViewModel.claimMission(missionId)
                    },
                    onClaimGrandChest = {
                      gameViewModel.claimGrandChest()
                    },
                    onClaimAchievement = { achievementId ->
                      gameViewModel.claimAchievement(achievementId)
                    },
                    onClose = { showMenuMissions = false }
                  )
                }
              }

              ScreenState.GAME -> {
                GameScreen(viewModel = gameViewModel)
              }

              ScreenState.LEVEL_SELECT -> {
                LevelSelectScreen(viewModel = gameViewModel)
              }

              ScreenState.LEADERBOARD -> {
                LeaderboardScreen(
                  gameViewModel = gameViewModel,
                  currentUser = currentUser,
                  leaderboardViewModel = leaderboardViewModel,
                  onSyncRequested = { gameViewModel.requestCloudSync() }
                )
              }
            }
          }

          AdSlotTransitionHost(
            isSlotActive = isAdSlotActive,
            activePlacement = activeAdPlacement
          )
        }
      }
    }
  }
}
