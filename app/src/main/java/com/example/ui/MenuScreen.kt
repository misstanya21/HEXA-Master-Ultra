package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ads.AdPlacement
import com.example.game.GameViewModel
import com.example.model.CloudSyncStatus
import com.example.model.GameMode
import com.example.model.ScreenState
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.RubikFontFamily
import com.example.ui.theme.TitanOneFontFamily

@Composable
fun MenuScreen(
  viewModel: GameViewModel,
  onOpenSettings: () -> Unit,
  onOpenShop: () -> Unit,
  onOpenDaily: () -> Unit,
  onOpenMissions: () -> Unit,
  onOpenProfile: () -> Unit = {},
  onOpenLeaderboard: () -> Unit = { viewModel.navigateTo(ScreenState.LEADERBOARD) },
  cloudPlayerName: String? = null,
  cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.IDLE,
  modifier: Modifier = Modifier
) {
  val currentLevel by viewModel.currentLevel.collectAsState()
  val coins by viewModel.coins.collectAsState()
  val gems by viewModel.gems.collectAsState()
  val canClaimDaily by viewModel.canClaimDailyReward.collectAsState()
  val unclaimedMissions by viewModel.unclaimedMissionCount.collectAsState()
  val unclaimedAchievements by viewModel.unclaimedAchievementCount.collectAsState()
  val profileStats by viewModel.profileStats.collectAsState()
  val selectedMode by viewModel.selectedGameMode.collectAsState()
  val timeAttackBest by viewModel.timeAttackBestScore.collectAsState()
  val endlessBest by viewModel.endlessBestScore.collectAsState()

  val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
  val playPulse by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.035f,
    animationSpec = infiniteRepeatable(
      animation = tween(900),
      repeatMode = RepeatMode.Reverse
    ),
    label = "play_pulse"
  )

  val floatBob by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "float_bob"
  )

  val haloPulse by infiniteTransition.animateFloat(
    initialValue = 0.78f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600),
      repeatMode = RepeatMode.Reverse
    ),
    label = "halo_pulse"
  )

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // 1. Illustrated Royal Twilight Cosmic Sky Backdrop
    Image(
      painter = painterResource(id = R.drawable.img_bg_menu),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    // Atmospheric vignette scrim so foreground cards & typography have razor-sharp contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xAA041430),
              Color(0x66082452),
              Color(0x88041536),
              Color(0xCC030F28)
            )
          )
        )
    )

    // Animated drifting cosmic starlight & warm motes
    AmbientStarlightOverlay(
      moteCount = 26,
      primaryGlow = Color(0xFF80D8FF),
      secondaryGlow = Color(0xFFFFD54F)
    )

    // 2. Main Foreground Column
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Top Bar: High-Contrast Sapphire Glass Currency Pills, Cloud Sync Pill & Settings
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Coins & Gems Pills
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .shadow(8.dp, RoundedCornerShape(22.dp))
              .background(
                brush = Brush.verticalGradient(listOf(Color(0xE6103C78), Color(0xEE082147))),
                shape = RoundedCornerShape(22.dp)
              )
              .border(1.5.dp, Color(0xFF64D8FF), RoundedCornerShape(22.dp))
              .clickable { onOpenShop() }
              .padding(start = 7.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            VectorCoinIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "$coins +",
              color = Color(0xFFFFEE58),
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 13.5.sp
            )
          }

          Row(
            modifier = Modifier
              .testTag("menu_gems_pill")
              .minimumInteractiveComponentSize()
              .shadow(8.dp, RoundedCornerShape(22.dp))
              .background(
                brush = Brush.verticalGradient(listOf(Color(0xE60B4F6C), Color(0xEE052A3D))),
                shape = RoundedCornerShape(22.dp)
              )
              .border(1.5.dp, Color(0xFF40E8FF), RoundedCornerShape(22.dp))
              .clickable { onOpenShop() }
              .padding(start = 9.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            VectorGemIcon(modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "$gems",
              color = Color(0xFFB2FEFF),
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 13.5.sp
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Cloud Status Pill
          val isSignedIn = !cloudPlayerName.isNullOrBlank()
          Row(
            modifier = Modifier
              .testTag("menu_cloud_pill")
              .minimumInteractiveComponentSize()
              .shadow(8.dp, RoundedCornerShape(18.dp))
              .background(
                brush = Brush.verticalGradient(
                  when {
                    !isSignedIn -> listOf(Color(0xFFFFAB00), Color(0xFFE65100))
                    cloudSyncStatus == CloudSyncStatus.ERROR -> listOf(Color(0xFFFF5252), Color(0xFFB71C1C))
                    else -> listOf(Color(0xFF00E676), Color(0xFF00A152))
                  }
                ),
                shape = RoundedCornerShape(18.dp)
              )
              .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
              .clickable { onOpenLeaderboard() }
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = when {
                !isSignedIn -> Icons.Default.CloudOff
                cloudSyncStatus == CloudSyncStatus.SYNCING -> Icons.Default.CloudSync
                cloudSyncStatus == CloudSyncStatus.ERROR -> Icons.Default.CloudOff
                else -> Icons.Default.CloudDone
              },
              contentDescription = "Cloud Save Status",
              tint = Color.White,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = when {
                !isSignedIn -> "Sign In"
                cloudSyncStatus == CloudSyncStatus.SYNCING -> "Sync..."
                cloudSyncStatus == CloudSyncStatus.ERROR -> "Retry"
                else -> "Synced"
              },
              color = Color.White,
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp
            )
          }

          // Settings Squircle Button
          Box(
            modifier = Modifier
              .size(48.dp)
              .shadow(8.dp, RoundedCornerShape(15.dp))
              .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD))),
                shape = RoundedCornerShape(15.dp)
              )
              .border(1.6.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(15.dp))
              .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
          }
        }
      }

      // Scrollable Center Content
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 3D Glossy Hero Emblem inside animated golden-cyan luminous halo
          Box(
            modifier = Modifier
              .offset(y = floatBob.dp)
              .size(106.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size((102 * haloPulse).dp)
                .background(
                  brush = Brush.radialGradient(
                    listOf(
                      Color(0x8800E5FF),
                      Color(0x44FFD600),
                      Color.Transparent
                    )
                  ),
                  shape = CircleShape
                )
            )
            Image(
              painter = painterResource(id = R.drawable.img_hero_emblem),
              contentDescription = "HEXA Master Ultra Emblem",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(88.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color(0xFF00E5FF))
                .clip(CircleShape)
                .border(2.5.dp, Color(0xFFFFEE58), CircleShape)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Bold Display Arcade Title
          ArcadeOutlinedText(
            text = "HEXA MASTER",
            fontSizeSp = 31f,
            fillColor = Color.White,
            strokeColor = Color(0xFF061B3E),
            strokeWidthPx = 9f,
            fontFamily = TitanOneFontFamily,
            fontWeight = FontWeight.Normal,
            letterSpacingSp = 1.1f
          )

          Spacer(modifier = Modifier.height(3.dp))

          // Magenta-Gold Ribbon Badge for ULTRA
          Box(
            modifier = Modifier
              .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = Color(0xFFFF2D87))
              .background(
                brush = Brush.horizontalGradient(
                  listOf(Color(0xFFFF2D87), Color(0xFFFF6D00), Color(0xFFFF2D87))
                ),
                shape = RoundedCornerShape(14.dp)
              )
              .border(1.5.dp, Color(0xFFFFF59D), RoundedCornerShape(14.dp))
              .padding(horizontal = 20.dp, vertical = 3.dp)
          ) {
            Text(
              text = "ULTRA EDITION",
              fontFamily = RubikFontFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 3.sp
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Game Mode Selection Header
          Text(
            text = "CHOOSE GAME MODE",
            fontFamily = RubikFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFB3E5FC),
            letterSpacing = 1.6.sp
          )

          Spacer(modifier = Modifier.height(8.dp))

          // 3 Interactive Glossy 3D Game Mode Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
          ) {
            GameModeCard(
              mode = GameMode.CLASSIC,
              icon = Icons.Rounded.GridView,
              subtitle = "Level $currentLevel",
              selected = selectedMode == GameMode.CLASSIC,
              gradientColors = listOf(Color(0xFF00E676), Color(0xFF00897B)),
              testTag = "mode_classic_card",
              onClick = { viewModel.selectGameMode(GameMode.CLASSIC) },
              modifier = Modifier.weight(1f)
            )
            GameModeCard(
              mode = GameMode.TIME_ATTACK,
              icon = Icons.Rounded.Timer,
              subtitle = if (timeAttackBest > 0) "Best $timeAttackBest" else "90s Rush",
              selected = selectedMode == GameMode.TIME_ATTACK,
              gradientColors = listOf(Color(0xFFFF7043), Color(0xFFD5004F)),
              testTag = "mode_time_attack_card",
              onClick = { viewModel.selectGameMode(GameMode.TIME_ATTACK) },
              modifier = Modifier.weight(1f)
            )
            GameModeCard(
              mode = GameMode.ENDLESS,
              icon = Icons.Rounded.AllInclusive,
              subtitle = if (endlessBest > 0) "Best $endlessBest" else "No Limits",
              selected = selectedMode == GameMode.ENDLESS,
              gradientColors = listOf(Color(0xFF9C4DFF), Color(0xFF2979FF)),
              testTag = "mode_endless_card",
              onClick = { viewModel.selectGameMode(GameMode.ENDLESS) },
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Frosted Sapphire Glass Mode Description Banner
          val modeSummary = when (selectedMode) {
            GameMode.CLASSIC -> "Clear tile goals & rescue obstacles stage by stage."
            GameMode.TIME_ATTACK -> "Score max points in 90s! Every clear adds +3s bonus."
            GameMode.ENDLESS -> "No timer or level limits — stack & clear continuously!"
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(Color(0xBB0A2348), RoundedCornerShape(14.dp))
              .border(1.dp, Color(0xFF64D8FF).copy(alpha = 0.55f), RoundedCornerShape(14.dp))
              .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = when (selectedMode) {
                GameMode.CLASSIC -> Icons.Rounded.Flag
                GameMode.TIME_ATTACK -> Icons.Rounded.Timer
                GameMode.ENDLESS -> Icons.Rounded.AllInclusive
              },
              contentDescription = null,
              tint = Color(0xFFFFEE58),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = modeSummary,
              fontFamily = NunitoFontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFE3F2FD),
              textAlign = TextAlign.Center
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Primary 3D Arcade Play Button matching selected mode
          val playLabel = when (selectedMode) {
            GameMode.CLASSIC -> "PLAY LEVEL $currentLevel"
            GameMode.TIME_ATTACK -> "START TIME ATTACK"
            GameMode.ENDLESS -> "PLAY ENDLESS MODE"
          }
          val playColors = when (selectedMode) {
            GameMode.CLASSIC -> ArcadeButtonStyles.Emerald
            GameMode.TIME_ATTACK -> ArcadeButtonStyles.Crimson
            GameMode.ENDLESS -> ArcadeButtonStyles.Violet
          }

          Arcade3DButton(
            onClick = { viewModel.startSelectedMode() },
            colors = playColors,
            height = 60.dp,
            cornerRadius = 24.dp,
            lipHeight = 6.dp,
            modifier = Modifier
              .testTag("play_button")
              .fillMaxWidth(0.94f)
              .scale(playPulse)
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = playLabel,
              fontFamily = RubikFontFamily,
              fontSize = 18.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 0.6.sp
            )
          }

          Spacer(modifier = Modifier.height(11.dp))

          // Select Level 3D Button
          Arcade3DButton(
            onClick = { viewModel.navigateTo(ScreenState.LEVEL_SELECT) },
            colors = ArcadeButtonStyles.SkyBlue,
            height = 48.dp,
            cornerRadius = 18.dp,
            lipHeight = 4.dp,
            modifier = Modifier.fillMaxWidth(0.94f)
          ) {
            Icon(
              imageVector = Icons.Rounded.Map,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "SELECT LEVEL",
              fontFamily = RubikFontFamily,
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
          }

          Spacer(modifier = Modifier.height(9.dp))

          // Daily Missions 3D Button with Unclaimed Badge
          Arcade3DButton(
            onClick = onOpenMissions,
            colors = if (unclaimedMissions > 0) ArcadeButtonStyles.Magenta else ArcadeButtonStyles.SkyBlue,
            height = 48.dp,
            cornerRadius = 18.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("menu_missions_button")
              .fillMaxWidth(0.94f)
          ) {
            Icon(
              imageVector = Icons.Rounded.Flag,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DAILY MISSIONS",
              fontFamily = RubikFontFamily,
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            if (unclaimedMissions > 0) {
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .background(Color(0xFFFFD600), RoundedCornerShape(10.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "$unclaimedMissions CLAIM",
                  color = Color(0xFFB71C1C),
                  fontFamily = RubikFontFamily,
                  fontWeight = FontWeight.Black,
                  fontSize = 10.5.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(9.dp))

          // Profile & Trophies 3D Button with Unlocked Count & Claim Badge
          Arcade3DButton(
            onClick = onOpenProfile,
            colors = ArcadeButtonStyles.Violet,
            height = 48.dp,
            cornerRadius = 18.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("menu_profile_button")
              .fillMaxWidth(0.94f)
          ) {
            Icon(
              imageVector = Icons.Rounded.EmojiEvents,
              contentDescription = null,
              tint = Color(0xFFFFEE58),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = "PROFILE & TROPHIES (${profileStats.unlockedTrophies}/${profileStats.totalTrophies})",
              fontFamily = RubikFontFamily,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (unclaimedAchievements > 0) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .background(Color(0xFFFFD600), RoundedCornerShape(10.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "$unclaimedAchievements CLAIM",
                  color = Color(0xFFB71C1C),
                  fontFamily = RubikFontFamily,
                  fontWeight = FontWeight.Black,
                  fontSize = 10.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(9.dp))

          // Global Leaderboard & Cloud Save 3D Button
          Arcade3DButton(
            onClick = onOpenLeaderboard,
            colors = ArcadeButtonStyles.TealCyan,
            height = 48.dp,
            cornerRadius = 18.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("menu_leaderboard_button")
              .fillMaxWidth(0.94f)
          ) {
            Icon(
              imageVector = Icons.Rounded.Public,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = if (cloudPlayerName != null) "GLOBAL LEADERBOARD • SYNCED" else "GLOBAL LEADERBOARD & CLOUD",
              fontFamily = RubikFontFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Bottom Row: Daily Reward & Shop 3D Buttons
          Row(
            modifier = Modifier.fillMaxWidth(0.94f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Arcade3DButton(
              onClick = onOpenDaily,
              colors = ArcadeButtonStyles.AmberGold,
              height = 46.dp,
              cornerRadius = 17.dp,
              lipHeight = 4.dp,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Rounded.CardGiftcard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (canClaimDaily) "REWARD!" else "DAILY",
                fontFamily = RubikFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }

            Arcade3DButton(
              onClick = onOpenShop,
              colors = ArcadeButtonStyles.Magenta,
              height = 46.dp,
              cornerRadius = 17.dp,
              lipHeight = 4.dp,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Rounded.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "SHOP",
                fontFamily = RubikFontFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      BannerAdSlot(
        placement = AdPlacement.MENU_BOTTOM_BANNER,
        hasNoAds = viewModel.hasNoAds.collectAsState().value
      )
    }
  }
}

@Composable
private fun GameModeCard(
  mode: GameMode,
  icon: ImageVector,
  subtitle: String,
  selected: Boolean,
  gradientColors: List<Color>,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val cardScale by animateFloatAsState(
    targetValue = if (selected) 1.04f else 0.96f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
    label = "mode_card_scale"
  )

  Box(
    modifier = modifier
      .testTag(testTag)
      .minimumInteractiveComponentSize()
      .scale(cardScale)
      .shadow(
        elevation = if (selected) 14.dp else 4.dp,
        shape = RoundedCornerShape(18.dp),
        ambientColor = gradientColors.first().copy(alpha = 0.55f)
      )
      .clip(RoundedCornerShape(18.dp))
      .background(
        brush = Brush.verticalGradient(
          if (selected) gradientColors else gradientColors.map { it.copy(alpha = 0.58f) }
        )
      )
      .border(
        width = if (selected) 2.5.dp else 1.2.dp,
        color = if (selected) Color(0xFFFFEE58) else Color.White.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp)
      )
      .clickable { onClick() }
  ) {
    // Top specular glass highlight band
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth(0.88f)
        .padding(top = 3.dp)
        .height(22.dp)
        .background(
          brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (selected) 0.34f else 0.16f), Color.Transparent)
          ),
          shape = RoundedCornerShape(14.dp)
        )
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = mode.title,
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = mode.shortLabel,
        fontFamily = FredokaFontFamily,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(3.dp))
      Box(
        modifier = Modifier
          .background(Color.Black.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
          .padding(horizontal = 7.dp, vertical = 2.dp)
      ) {
        Text(
          text = subtitle,
          fontFamily = RubikFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (selected) Color(0xFFFFF59D) else Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
