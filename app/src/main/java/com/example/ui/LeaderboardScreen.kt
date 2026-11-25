package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.AdPlacement
import com.example.cloud.GoogleSignInButton
import com.example.cloud.LeaderboardUiState
import com.example.cloud.LeaderboardViewModel
import com.example.cloud.signOutFromGoogle
import com.example.game.GameViewModel
import com.example.model.CloudSyncStatus
import com.example.model.LeaderboardCategory
import com.example.model.LeaderboardEntry
import com.example.model.ScreenState
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.RubikFontFamily
import com.example.ui.theme.TitanOneFontFamily
import com.google.firebase.auth.FirebaseUser

@Composable
fun LeaderboardScreen(
  gameViewModel: GameViewModel,
  currentUser: FirebaseUser?,
  leaderboardViewModel: LeaderboardViewModel?,
  onSyncRequested: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler {
    gameViewModel.navigateTo(ScreenState.MENU)
  }

  val coins by gameViewModel.coins.collectAsState()
  val gems by gameViewModel.gems.collectAsState()
  val highestLevel by gameViewModel.highestLevel.collectAsState()
  val careerScore by gameViewModel.careerTotalScore.collectAsState()

  var authError by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = modifier
      .testTag("leaderboard_screen")
      .fillMaxSize()
  ) {
    // Deep Aurora Cosmic Nebula Backdrop
    Image(
      painter = painterResource(id = R.drawable.img_bg_endless),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xAA050C24),
              Color(0x88081B45),
              Color(0xCC040A1E)
            )
          )
        )
    )

    AmbientStarlightOverlay(
      moteCount = 24,
      primaryGlow = Color(0xFF40E8FF),
      secondaryGlow = Color(0xFFFFD54F)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .widthIn(max = 600.dp)
      ) {
        // Top Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = { gameViewModel.navigateTo(ScreenState.MENU) },
            modifier = Modifier
              .testTag("leaderboard_back_button")
              .size(48.dp)
              .shadow(8.dp, RoundedCornerShape(14.dp))
              .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD))),
                shape = RoundedCornerShape(14.dp)
              )
              .border(1.5.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Menu",
              tint = Color.White
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeOutlinedText(
              text = "GLOBAL LEADERBOARD",
              fontSizeSp = 19f,
              fillColor = Color.White,
              strokeColor = Color(0xFF041430),
              strokeWidthPx = 6f,
              fontFamily = TitanOneFontFamily,
              fontWeight = FontWeight.Normal,
              letterSpacingSp = 0.5f
            )
            Text(
              text = "Cloud Save & World Rankings",
              fontFamily = RubikFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF80D8FF)
            )
          }

          if (currentUser != null && leaderboardViewModel != null) {
            IconButton(
              onClick = onSyncRequested,
              modifier = Modifier
                .testTag("header_sync_button")
                .size(48.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(
                  brush = Brush.verticalGradient(listOf(Color(0xFF2EFF8B), Color(0xFF00A152))),
                  shape = RoundedCornerShape(14.dp)
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sync Cloud Save",
                tint = Color.White
              )
            }
          } else {
            Spacer(modifier = Modifier.size(48.dp))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentUser == null || leaderboardViewModel == null) {
          CloudAuthGateCard(
            highestLevel = highestLevel,
            coins = coins,
            gems = gems,
            careerScore = careerScore,
            authError = authError,
            onAuthSuccess = {
              authError = null
              gameViewModel.requestCloudSync()
            },
            onAuthError = { err ->
              authError = err
            }
          )
        } else {
          AuthenticatedLeaderboardContent(
            currentUser = currentUser,
            gameViewModel = gameViewModel,
            leaderboardViewModel = leaderboardViewModel,
            onSyncRequested = onSyncRequested
          )
        }
      }

      BannerAdSlot(
        placement = AdPlacement.LEADERBOARD_BOTTOM_BANNER,
        hasNoAds = gameViewModel.hasNoAds.collectAsState().value
      )
    }
  }
}

@Composable
private fun CloudAuthGateCard(
  highestLevel: Int,
  coins: Int,
  gems: Int,
  careerScore: Long,
  authError: String?,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(18.dp, RoundedCornerShape(26.dp))
      .background(
        brush = Brush.verticalGradient(listOf(Color(0xEE102B58), Color(0xF5071734))),
        shape = RoundedCornerShape(26.dp)
      )
      .border(2.dp, Color(0xFF40C4FF), RoundedCornerShape(26.dp))
      .verticalScroll(rememberScrollState())
      .padding(22.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .shadow(10.dp, CircleShape)
        .background(
          brush = Brush.radialGradient(listOf(Color(0xFF40C4FF), Color(0xFF0288D1))),
          shape = CircleShape
        )
        .border(2.5.dp, Color.White, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.CloudOff,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(34.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "PROTECT YOUR PROGRESS",
      fontFamily = RubikFontFamily,
      fontSize = 20.sp,
      fontWeight = FontWeight.Black,
      color = Color.White,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Sign in with Google to back up your score, coins, gems & levels to the Cloud across any device — and climb the Global Leaderboard!",
      fontFamily = NunitoFontFamily,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      color = Color(0xFFCCE7FF),
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        .border(1.dp, Color(0xFF64D8FF).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        .padding(10.dp),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      LocalStatPreview("Level", "Lv.$highestLevel")
      LocalStatPreview("Coins", "$coins")
      LocalStatPreview("Gems", "$gems")
      LocalStatPreview("Score", "$careerScore")
    }

    Spacer(modifier = Modifier.height(18.dp))

    GoogleSignInButton(
      label = "Sign in with Google",
      onAuthSuccess = onAuthSuccess,
      onAuthError = onAuthError,
      modifier = Modifier.fillMaxWidth()
    )

    if (!authError.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = authError,
        color = Color(0xFFFF8A80),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun LocalStatPreview(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontFamily = RubikFontFamily,
      fontWeight = FontWeight.Black,
      fontSize = 13.sp,
      color = Color(0xFFFFEE58)
    )
    Text(
      text = label,
      fontFamily = NunitoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp,
      color = Color(0xFFB3E5FC)
    )
  }
}

@Composable
private fun AuthenticatedLeaderboardContent(
  currentUser: FirebaseUser,
  gameViewModel: GameViewModel,
  leaderboardViewModel: LeaderboardViewModel,
  onSyncRequested: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val selectedCategory by leaderboardViewModel.selectedCategory.collectAsStateWithLifecycle()
  val leaderboardState by leaderboardViewModel.leaderboardState.collectAsStateWithLifecycle()
  val syncStatus by leaderboardViewModel.syncStatus.collectAsStateWithLifecycle()
  val lastSyncLabel by leaderboardViewModel.lastSyncLabel.collectAsStateWithLifecycle()
  val syncError by leaderboardViewModel.syncError.collectAsStateWithLifecycle()

  val coins by gameViewModel.coins.collectAsState()
  val gems by gameViewModel.gems.collectAsState()
  val highestLevel by gameViewModel.highestLevel.collectAsState()
  val careerScore by gameViewModel.careerTotalScore.collectAsState()

  LaunchedEffect(currentUser.uid) {
    onSyncRequested()
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // 1. Cloud Sync Status Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(22.dp))
        .background(
          brush = Brush.verticalGradient(listOf(Color(0xEE103A75), Color(0xF5071E42))),
          shape = RoundedCornerShape(22.dp)
        )
        .border(1.6.dp, Color(0xFF64D8FF), RoundedCornerShape(22.dp))
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(Color.White.copy(alpha = 0.16f), CircleShape)
              .border(1.5.dp, Color(0xFF69F0AE), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (syncStatus) {
                CloudSyncStatus.SYNCING -> Icons.Default.CloudSync
                CloudSyncStatus.SYNCED -> Icons.Default.CloudDone
                CloudSyncStatus.ERROR -> Icons.Default.CloudOff
                CloudSyncStatus.IDLE -> Icons.Default.CloudDone
              },
              contentDescription = "Cloud Sync Status",
              tint = when (syncStatus) {
                CloudSyncStatus.ERROR -> Color(0xFFFF8A80)
                else -> Color(0xFF69F0AE)
              },
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Hexa Player",
              fontFamily = RubikFontFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = when (syncStatus) {
                CloudSyncStatus.SYNCING -> "Syncing Cloud Save..."
                CloudSyncStatus.SYNCED -> lastSyncLabel ?: "Cloud Backup Active"
                CloudSyncStatus.ERROR -> syncError ?: "Sync error — tap Sync Now"
                CloudSyncStatus.IDLE -> "Cloud Backup Ready"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (syncStatus == CloudSyncStatus.ERROR) Color(0xFFFFCDD2) else Color(0xFFB9F6CA)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Arcade3DButton(
            onClick = onSyncRequested,
            enabled = syncStatus != CloudSyncStatus.SYNCING,
            colors = ArcadeButtonStyles.Emerald,
            height = 38.dp,
            cornerRadius = 12.dp,
            lipHeight = 3.dp,
            modifier = Modifier
              .testTag("sync_now_button")
              .width(108.dp)
          ) {
            Text(
              text = if (syncStatus == CloudSyncStatus.SYNCING) "SYNCING" else "SYNC NOW",
              fontFamily = RubikFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              maxLines = 1
            )
          }

          IconButton(
            onClick = {
              signOutFromGoogle(
                credentialManager = credentialManager,
                scope = coroutineScope
              )
            },
            modifier = Modifier
              .testTag("sign_out_button")
              .size(48.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign out of Google",
              tint = Color.White.copy(alpha = 0.9f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SyncedStatPill("Level", "Lv.$highestLevel", Modifier.weight(1f))
        SyncedStatPill("Coins", "$coins", Modifier.weight(1f))
        SyncedStatPill("Gems", "$gems", Modifier.weight(1f))
        SyncedStatPill("Career", "$careerScore", Modifier.weight(1f))
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Category Filter Tabs (dark sapphire glass dock + vector icons)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xCC092046), RoundedCornerShape(16.dp))
        .border(1.2.dp, Color(0xFF64D8FF).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      LeaderboardCategory.entries.forEach { category ->
        val isSelected = selectedCategory == category
        val tabIcon = when (category) {
          LeaderboardCategory.TOTAL_SCORE -> Icons.Rounded.TrendingUp
          LeaderboardCategory.HIGHEST_LEVEL -> Icons.Rounded.Star
          LeaderboardCategory.TIME_ATTACK -> Icons.Rounded.Timer
          LeaderboardCategory.ENDLESS -> Icons.Rounded.AllInclusive
        }
        Column(
          modifier = Modifier
            .testTag("lb_tab_${category.name}")
            .weight(1f)
            .minimumInteractiveComponentSize()
            .shadow(if (isSelected) 6.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(
              brush = if (isSelected) {
                Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD)))
              } else {
                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
              },
              shape = RoundedCornerShape(12.dp)
            )
            .clickable { leaderboardViewModel.selectCategory(category) }
            .padding(vertical = 6.dp, horizontal = 2.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = tabIcon,
            contentDescription = category.label,
            tint = if (isSelected) Color(0xFFFFEE58) else Color(0xFFB3E5FC),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = category.label,
            fontFamily = RubikFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) Color.White else Color(0xFFB3E5FC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Rankings List
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      when (val state = leaderboardState) {
        is LeaderboardUiState.Loading -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(color = Color(0xFF40E8FF), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Loading Global Rankings...",
              fontFamily = RubikFontFamily,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }

        is LeaderboardUiState.Error -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xEE0D2246), RoundedCornerShape(20.dp))
              .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Rounded.Warning,
              contentDescription = null,
              tint = Color(0xFFFF8A80),
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Unable to Load Leaderboard",
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFFCDD2)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = state.message,
              fontFamily = NunitoFontFamily,
              fontSize = 12.sp,
              color = Color(0xFFB3E5FC),
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Arcade3DButton(
              onClick = onSyncRequested,
              colors = ArcadeButtonStyles.SkyBlue,
              height = 44.dp,
              cornerRadius = 16.dp,
              modifier = Modifier.width(180.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "RETRY SYNC",
                fontFamily = RubikFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color.White
              )
            }
          }
        }

        is LeaderboardUiState.Success -> {
          val entries = state.entries
          if (entries.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xEE0D2246), RoundedCornerShape(22.dp))
                .border(1.5.dp, Color(0xFF64D8FF), RoundedCornerShape(22.dp))
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFEE58),
                modifier = Modifier.size(44.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Be the First on the Board!",
                fontFamily = RubikFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text(
                text = "Tap SYNC NOW to publish your score and claim #1!",
                fontFamily = NunitoFontFamily,
                fontSize = 12.sp,
                color = Color(0xFFB3E5FC),
                textAlign = TextAlign.Center
              )
            }
          } else {
            val myRankIndex = entries.indexOfFirst { it.userId == currentUser.uid }

            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(bottom = 16.dp)
            ) {
              if (myRankIndex >= 0) {
                item(key = "my_rank_summary") {
                  MyRankBanner(
                    rank = myRankIndex + 1,
                    entry = entries[myRankIndex],
                    category = selectedCategory
                  )
                }
              }

              itemsIndexed(entries, key = { _, e -> e.userId.ifBlank { e.id } }) { index, entry ->
                LeaderboardRowCard(
                  rank = index + 1,
                  entry = entry,
                  category = selectedCategory,
                  isCurrentUser = entry.userId == currentUser.uid
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SyncedStatPill(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
      .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
      .padding(vertical = 5.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = value,
      fontFamily = RubikFontFamily,
      color = Color(0xFFFFEE58),
      fontWeight = FontWeight.Black,
      fontSize = 12.sp,
      maxLines = 1
    )
    Text(
      text = label,
      fontFamily = NunitoFontFamily,
      color = Color(0xFFB3E5FC),
      fontWeight = FontWeight.Bold,
      fontSize = 9.5.sp
    )
  }
}

@Composable
private fun MyRankBanner(
  rank: Int,
  entry: LeaderboardEntry,
  category: LeaderboardCategory
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(8.dp, RoundedCornerShape(18.dp))
      .background(
        brush = Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF6F00))),
        shape = RoundedCornerShape(18.dp)
      )
      .border(2.dp, Color(0xFFFFF59D), RoundedCornerShape(18.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Rounded.MilitaryTech,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "YOUR GLOBAL RANK: #$rank",
        fontFamily = RubikFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        color = Color.White
      )
    }
    Text(
      text = category.formattedValue(entry),
      fontFamily = RubikFontFamily,
      fontWeight = FontWeight.Black,
      fontSize = 15.sp,
      color = Color.White
    )
  }
}

@Composable
private fun LeaderboardRowCard(
  rank: Int,
  entry: LeaderboardEntry,
  category: LeaderboardCategory,
  isCurrentUser: Boolean
) {
  val rankBadgeColor = when (rank) {
    1 -> Color(0xFFFFD600)
    2 -> Color(0xFFE0E0E0)
    3 -> Color(0xFFFF8A65)
    else -> Color(0xFF40C4FF)
  }
  val rankLabel = "#$rank"

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(if (isCurrentUser || rank <= 3) 8.dp else 3.dp, RoundedCornerShape(18.dp))
      .background(
        brush = Brush.verticalGradient(
          if (isCurrentUser) listOf(Color(0xEE18427D), Color(0xEE0D2954))
          else listOf(Color(0xDD0D244B), Color(0xDD081836))
        ),
        shape = RoundedCornerShape(18.dp)
      )
      .border(
        width = if (isCurrentUser) 2.dp else 1.2.dp,
        color = if (isCurrentUser) Color(0xFFFFEE58) else Color(0xFF64D8FF).copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp)
      )
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .background(rankBadgeColor.copy(alpha = if (rank <= 3) 0.25f else 0.14f), CircleShape)
          .border(1.5.dp, rankBadgeColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = rankLabel,
          fontFamily = RubikFontFamily,
          fontWeight = FontWeight.Black,
          fontSize = if (rank <= 3) 13.sp else 12.sp,
          color = rankBadgeColor
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = entry.safePlayerName(),
            fontFamily = RubikFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (isCurrentUser) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .background(Color(0xFF00E676), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
              Text(
                text = "YOU",
                color = Color(0xFF003314),
                fontFamily = RubikFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp
              )
            }
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${entry.safeRankTitle()} • ${entry.unlockedTrophies} Trophies",
            fontFamily = NunitoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB3E5FC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Box(
      modifier = Modifier
        .background(Color(0xFF0A2E5C), RoundedCornerShape(12.dp))
        .border(1.dp, Color(0xFF40C4FF), RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Text(
        text = category.formattedValue(entry),
        fontFamily = RubikFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 13.sp,
        color = Color(0xFFFFEE58)
      )
    }
  }
}
