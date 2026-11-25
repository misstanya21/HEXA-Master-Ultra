package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.model.Achievement
import com.example.model.AchievementTier
import com.example.model.DailyMission
import com.example.model.MissionReward
import com.example.model.MissionType
import com.example.model.PlayerProfileStats
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import com.example.ads.AdConfig
import com.example.audio.AudioController
import com.example.model.GameMode
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.RubikFontFamily
import com.example.ui.theme.TitanOneFontFamily
import kotlinx.coroutines.delay

@Composable
fun SettingsDialog(
  currentLevel: Int,
  totalLevels: Int,
  audio: AudioController,
  isLowPerfMode: Boolean,
  isColorblindMode: Boolean,
  gameMode: GameMode = GameMode.CLASSIC,
  onToggleLowPerfMode: () -> Unit,
  onToggleColorblindMode: () -> Unit,
  onSaveAudioSettings: (Float, Float, Boolean, Boolean, Boolean) -> Unit,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onSelectLevel: (Int) -> Unit,
  onMainMenu: () -> Unit
) {
  var sfxVol by remember { mutableFloatStateOf(audio.getSoundVolume()) }
  var musVol by remember { mutableFloatStateOf(audio.getMusicVolume()) }
  var soundOn by remember { mutableStateOf(audio.isSoundOn()) }
  var musicOn by remember { mutableStateOf(audio.isMusicOn()) }
  var hapticsOn by remember { mutableStateOf(audio.isHapticsOn()) }

  Dialog(onDismissRequest = {
    onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
    onResume()
  }) {
    Box(
      modifier = Modifier
        .testTag("settings_dialog")
        .fillMaxWidth()
        .shadow(20.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF))
          ),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.dp, Color(0xFF40C4FF), RoundedCornerShape(28.dp))
        .padding(20.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "SETTINGS",
              fontFamily = TitanOneFontFamily,
              fontSize = 24.sp,
              fontWeight = FontWeight.Normal,
              color = Color(0xFF01579B)
            )
            Text(
              text = if (gameMode == GameMode.CLASSIC) "Classic Level $currentLevel" else gameMode.title,
              fontFamily = RubikFontFamily,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0288D1)
            )
          }
          IconButton(onClick = {
            onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
            onResume()
          }) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sound FX Volume
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = null,
              tint = Color(0xFF0288D1),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sound FX", fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
          }
          Switch(
            checked = soundOn,
            onCheckedChange = {
              soundOn = it
              audio.setSoundEnabled(it)
              if (it) audio.playTap()
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
          )
        }
        Slider(
          value = sfxVol,
          onValueChange = {
            sfxVol = it
            audio.setSoundVolume(it)
          },
          colors = SliderDefaults.colors(thumbColor = Color(0xFF00B0FF), activeTrackColor = Color(0xFF00B0FF)),
          modifier = Modifier.fillMaxWidth()
        )

        // Music Volume
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (musicOn) Icons.Default.MusicNote else Icons.Default.MusicOff,
              contentDescription = null,
              tint = Color(0xFF0288D1),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("BGM Music", fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
          }
          Switch(
            checked = musicOn,
            onCheckedChange = {
              musicOn = it
              audio.setMusicEnabled(it)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
          )
        }
        Slider(
          value = musVol,
          onValueChange = {
            musVol = it
            audio.setMusicVolume(it)
          },
          colors = SliderDefaults.colors(thumbColor = Color(0xFF00B0FF), activeTrackColor = Color(0xFF00B0FF)),
          modifier = Modifier.fillMaxWidth()
        )

        // Haptics & Performance Mode
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("📳 Haptic Vibration", fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
          Switch(
            checked = hapticsOn,
            onCheckedChange = {
              hapticsOn = it
              audio.setHapticsEnabled(it)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("⚡ Low Effects Mode", fontWeight = FontWeight.Bold, color = Color(0xFF01579B))
          Switch(
            checked = isLowPerfMode,
            onCheckedChange = { onToggleLowPerfMode() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
          )
        }

        // High-Contrast Colorblind Mode Settings Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isColorblindMode) 4.dp else 1.dp, RoundedCornerShape(16.dp))
            .background(
              if (isColorblindMode) Color(0xFFF1F8E9) else Color(0xFFF5F5F5),
              RoundedCornerShape(16.dp)
            )
            .border(
              width = if (isColorblindMode) 2.dp else 1.dp,
              color = if (isColorblindMode) Color(0xFF2E7D32) else Color(0xFFCFD8DC),
              shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "👁️ Colorblind Mode",
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp,
                  color = if (isColorblindMode) Color(0xFF1B5E20) else Color(0xFF01579B)
                )
                if (isColorblindMode) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Box(
                    modifier = Modifier
                      .background(Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                      .padding(horizontal = 5.dp, vertical = 1.dp)
                  ) {
                    Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                  }
                }
              }
              Text(
                "Adds high-contrast numerical symbols (▲1, ♥2, ★3...) & sharp outlines to all hex tiles",
                fontSize = 11.sp,
                color = Color(0xFF546E7A),
                lineHeight = 14.sp
              )
            }
            Switch(
              checked = isColorblindMode,
              onCheckedChange = { onToggleColorblindMode() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2E7D32)
              ),
              modifier = Modifier.testTag("colorblind_mode_switch")
            )
          }

          // Visual Preview Legend of all 8 Numerical Symbols
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "NUMERICAL SYMBOLS KEY:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isColorblindMode) Color(0xFF2E7D32) else Color(0xFF78909C)
          )
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(com.example.model.TileColor.entries) { tileColor ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .shadow(2.dp, RoundedCornerShape(8.dp))
                  .background(tileColor.topColor, RoundedCornerShape(8.dp))
                  .border(
                    width = if (isColorblindMode) 1.5.dp else 1.dp,
                    color = if (isColorblindMode) Color.Black else Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                  )
                  .padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Box(
                  modifier = Modifier
                    .background(Color(0xFF111111), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = tileColor.fullBadgeText,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = tileColor.displayName,
                  color = tileColor.textColor,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp
                )
              }
            }
          }
        }

        if (gameMode == GameMode.CLASSIC) {
          Spacer(modifier = Modifier.height(14.dp))

          // Level Select Row
          Text(
            text = "LEVEL SELECT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF546E7A)
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items((1..totalLevels).toList()) { lvl ->
              val isCurrent = lvl == currentLevel
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .shadow(if (isCurrent) 6.dp else 2.dp, CircleShape)
                  .background(
                    if (isCurrent) Color(0xFFFF2D87) else Color(0xFFE1F5FE),
                    CircleShape
                  )
                  .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = if (isCurrent) Color(0xFFFFD600) else Color(0xFF81D4FA),
                    shape = CircleShape
                  )
                  .clickable {
                    onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
                    onSelectLevel(lvl)
                  },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "$lvl",
                  color = if (isCurrent) Color.White else Color(0xFF01579B),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Arcade3DButton(
            onClick = {
              onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
              onMainMenu()
            },
            colors = ArcadeButtonStyles.SkyBlue,
            height = 44.dp,
            cornerRadius = 15.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("menu_button")
              .weight(1f)
          ) {
            Icon(Icons.Rounded.Home, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Menu", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
          }

          Arcade3DButton(
            onClick = {
              onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
              onRestart()
            },
            colors = ArcadeButtonStyles.AmberGold,
            height = 44.dp,
            cornerRadius = 15.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("restart_button")
              .weight(1f)
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Restart", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Restart", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
          }

          Arcade3DButton(
            onClick = {
              onSaveAudioSettings(sfxVol, musVol, soundOn, musicOn, hapticsOn)
              onResume()
            },
            colors = ArcadeButtonStyles.Emerald,
            height = 44.dp,
            cornerRadius = 15.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("resume_button")
              .weight(1f)
          ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Play", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun DailyRewardDialog(
  consecutiveDays: Int,
  canClaim: Boolean,
  onClaim: () -> Unit,
  onClose: () -> Unit
) {
  Dialog(onDismissRequest = onClose) {
    Box(
      modifier = Modifier
        .testTag("daily_reward_dialog")
        .fillMaxWidth()
        .shadow(20.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF), Color(0xFFFFF9C4))
          ),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.5.dp, Color(0xFFFFD600), RoundedCornerShape(28.dp))
        .padding(22.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "DAILY REWARD",
            fontFamily = TitanOneFontFamily,
            fontSize = 23.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF01579B)
          )
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Text(
          text = "7-Day Login Streak Bonus!",
          fontFamily = RubikFontFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFFF8F00)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7 Days Cards
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items((1..7).toList()) { day ->
            val isCurrent = day == consecutiveDays
            val isClaimed = day < consecutiveDays

            Box(
              modifier = Modifier
                .width(62.dp)
                .height(86.dp)
                .shadow(if (isCurrent) 8.dp else 2.dp, RoundedCornerShape(14.dp))
                .background(
                  if (isCurrent) Color(0xFFFFD600) else if (isClaimed) Color(0xFFC8E6C9) else Color(0xFFE1F5FE),
                  RoundedCornerShape(14.dp)
                )
                .border(
                  width = if (isCurrent) 2.dp else 1.dp,
                  color = if (isCurrent) Color(0xFFFF6F00) else Color(0xFF81D4FA),
                  shape = RoundedCornerShape(14.dp)
                )
                .padding(6.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "Day $day",
                  fontFamily = RubikFontFamily,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF01579B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (day == 7) {
                  Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                } else if (day % 2 == 0) {
                  Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Color(0xFFD81B60), modifier = Modifier.size(22.dp))
                } else {
                  VectorCoinIcon(modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "+${day * 150}",
                  fontFamily = RubikFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color(0xFF00796B)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Arcade3DButton(
          onClick = {
            onClaim()
            onClose()
          },
          enabled = canClaim,
          colors = ArcadeButtonStyles.Emerald,
          height = 50.dp,
          cornerRadius = 18.dp,
          lipHeight = 5.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (canClaim) {
            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = if (canClaim) "CLAIM TODAY'S REWARD!" else "ALREADY CLAIMED TODAY",
            fontFamily = RubikFontFamily,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun LevelCompleteDialog(
  levelNumber: Int,
  score: Int,
  coinsEarned: Int,
  onNextLevel: () -> Unit
) {
  val star1Scale = remember { Animatable(0f) }
  val star2Scale = remember { Animatable(0f) }
  val star3Scale = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    star1Scale.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    star1Scale.animateTo(1.0f, tween(100))
    star2Scale.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    star2Scale.animateTo(1.15f, tween(100))
    star3Scale.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    star3Scale.animateTo(1.0f, tween(100))
  }

  Dialog(onDismissRequest = {}) {
    Box(
      modifier = Modifier
        .testTag("level_complete_dialog")
        .fillMaxWidth()
        .shadow(20.dp, RoundedCornerShape(32.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF), Color(0xFFFFF9C4))
          ),
          shape = RoundedCornerShape(32.dp)
        )
        .border(3.dp, Color(0xFFFFD600), RoundedCornerShape(32.dp))
        .padding(28.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD600),
            modifier = Modifier
              .size(46.dp)
              .scale(star1Scale.value)
          )
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD600),
            modifier = Modifier
              .size(62.dp)
              .scale(star2Scale.value)
          )
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD600),
            modifier = Modifier
              .size(46.dp)
              .scale(star3Scale.value)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "LEVEL $levelNumber",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF0277BD)
        )
        Text(
          text = "COMPLETED!",
          fontSize = 30.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF00C853)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE1F5FE), RoundedCornerShape(16.dp))
            .padding(14.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "SCORE: $score",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFF01579B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "+$coinsEarned Coins Awarded! 🪙",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFF8F00)
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Arcade3DButton(
          onClick = onNextLevel,
          colors = ArcadeButtonStyles.Emerald,
          height = 54.dp,
          cornerRadius = 20.dp,
          lipHeight = 5.dp,
          modifier = Modifier
            .testTag("next_level_button")
            .fillMaxWidth()
        ) {
          Text(
            text = "NEXT LEVEL",
            fontFamily = RubikFontFamily,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun GameOverDialog(
  onRestart: () -> Unit,
  onUseHammer: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Box(
      modifier = Modifier
        .testTag("game_over_dialog")
        .fillMaxWidth()
        .shadow(20.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFFFEBEE), Color(0xFFFFFFFF))
          ),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.5.dp, Color(0xFFFF5252), RoundedCornerShape(28.dp))
        .padding(24.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Rounded.Warning,
          contentDescription = "No More Moves",
          tint = Color(0xFFD50000),
          modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "NO MORE MOVES!",
          fontFamily = TitanOneFontFamily,
          fontSize = 24.sp,
          fontWeight = FontWeight.Normal,
          color = Color(0xFFC62828)
        )
        Text(
          text = "The board is full. Use a booster or retry!",
          fontSize = 14.sp,
          color = Color(0xFF546E7A),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Arcade3DButton(
          onClick = onUseHammer,
          colors = ArcadeButtonStyles.SkyBlue,
          height = 48.dp,
          cornerRadius = 16.dp,
          lipHeight = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Hardware, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Use Hammer to Clear", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Arcade3DButton(
          onClick = onRestart,
          colors = ArcadeButtonStyles.AmberGold,
          height = 48.dp,
          cornerRadius = 16.dp,
          lipHeight = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Try Again", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
        }
      }
    }
  }
}

@Composable
fun ShopDialog(
  coins: Int,
  gems: Int = 0,
  hasNoAds: Boolean,
  onClose: () -> Unit,
  onBuyCoins: (Int) -> Unit,
  onBuyNoAds: () -> Unit,
  onRedeemGems: () -> Unit = {}
) {
  Dialog(onDismissRequest = onClose) {
    Box(
      modifier = Modifier
        .testTag("shop_dialog")
        .fillMaxWidth()
        .shadow(16.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF))
          ),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.dp, Color(0xFF40C4FF), RoundedCornerShape(28.dp))
        .padding(20.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "COIN & GEM SHOP",
            fontFamily = TitanOneFontFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF01579B)
          )
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            VectorCoinIcon(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$coins",
              fontFamily = RubikFontFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFFF8F00)
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            VectorGemIcon(modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$gems",
              fontFamily = RubikFontFamily,
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF0091EA)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gem-to-Coin Exchange
        ShopItem(
          emoji = "💎",
          title = "Gem Vault (+400 Coins)",
          priceLabel = if (gems >= 20) "20 💎" else "NEED 20 💎",
          isEnabled = gems >= 20,
          onBuy = onRedeemGems
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (AdConfig.ADS_ENABLED) {
          ShopItem(
            emoji = "🛡️",
            title = "Remove All Ads Forever",
            priceLabel = if (hasNoAds) "ACTIVE" else "UNLOCK",
            isEnabled = !hasNoAds,
            onBuy = onBuyNoAds
          )

          Spacer(modifier = Modifier.height(10.dp))
        }

        ShopItem(
          emoji = "🪙",
          title = "Starter Pack (+250 Coins)",
          priceLabel = "FREE REWARD",
          onBuy = { onBuyCoins(250) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ShopItem(
          emoji = "💰",
          title = "Pro Vault (+1,000 Coins)",
          priceLabel = "FREE REWARD",
          onBuy = { onBuyCoins(1000) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ShopItem(
          emoji = "👑",
          title = "Mega Bundle (+5,000 Coins)",
          priceLabel = "FREE REWARD",
          onBuy = { onBuyCoins(5000) }
        )
      }
    }
  }
}

@Composable
fun ShopItem(
  emoji: String,
  title: String,
  priceLabel: String,
  isEnabled: Boolean = true,
  onBuy: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(4.dp, RoundedCornerShape(16.dp))
      .background(Color.White, RoundedCornerShape(16.dp))
      .border(1.dp, Color(0xFF81D4FA), RoundedCornerShape(16.dp))
      .padding(10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .weight(1f)
        .padding(end = 8.dp)
    ) {
      Text(emoji, fontSize = 26.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF01579B))
    }
    Arcade3DButton(
      onClick = onBuy,
      enabled = isEnabled,
      colors = ArcadeButtonStyles.Emerald,
      height = 38.dp,
      cornerRadius = 12.dp,
      lipHeight = 3.dp,
      modifier = Modifier.width(108.dp)
    ) {
      Text(
        text = priceLabel,
        fontFamily = RubikFontFamily,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        maxLines = 1
      )
    }
  }
}

/**
 * AdMob-ready interstitial overlay (appears smoothly every 3 levels with skip countdown).
 */
@Composable
fun InterstitialAdDialog(
  onDismiss: () -> Unit
) {
  var countdown by remember { mutableIntStateOf(3) }

  LaunchedEffect(Unit) {
    while (countdown > 0) {
      delay(1000)
      countdown--
    }
  }

  Dialog(onDismissRequest = {
    if (countdown == 0) onDismiss()
  }) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(24.dp, RoundedCornerShape(24.dp))
        .background(Color(0xFF263238), RoundedCornerShape(24.dp))
        .border(2.dp, Color(0xFF00B0FF), RoundedCornerShape(24.dp))
        .padding(24.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("📺 SPONSORED", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
              brush = Brush.verticalGradient(listOf(Color(0xFF00B0FF), Color(0xFF0091EA))),
              RoundedCornerShape(16.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("HEXA Master Ultra", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Play Offline & Online Anywhere!", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Arcade3DButton(
          onClick = onDismiss,
          enabled = countdown == 0,
          colors = ArcadeButtonStyles.Emerald,
          height = 48.dp,
          cornerRadius = 16.dp,
          lipHeight = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (countdown > 0) "SKIP IN $countdown..." else "CONTINUE GAME",
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun DailyMissionsDialog(
  missions: List<DailyMission>,
  isGrandChestClaimed: Boolean,
  achievements: List<Achievement> = emptyList(),
  profileStats: PlayerProfileStats = PlayerProfileStats(),
  coins: Int = 0,
  gems: Int = 0,
  initialTab: Int = 0,
  onClaimMission: (String) -> Unit,
  onClaimGrandChest: () -> Unit,
  onClaimAchievement: (String) -> Unit = {},
  onClose: () -> Unit
) {
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
  val completedCount = missions.count { it.isCompleted }
  val totalCount = missions.size
  val allDone = totalCount > 0 && completedCount == totalCount
  val unclaimedMissionsCount = missions.count { it.canClaim } + if (allDone && !isGrandChestClaimed) 1 else 0
  val unclaimedTrophiesCount = achievements.count { it.canClaim }

  Dialog(onDismissRequest = onClose) {
    Box(
      modifier = Modifier
        .testTag("daily_missions_dialog")
        .fillMaxWidth()
        .shadow(24.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xFFE1F5FE), Color(0xFFFFFFFF), Color(0xFFFFF9C4))
          ),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.5.dp, Color(0xFF00B0FF), RoundedCornerShape(28.dp))
        .padding(18.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (selectedTab == 0) "🎯" else "🏆", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (selectedTab == 0) "DAILY MISSIONS" else "PROFILE & TROPHIES",
                fontFamily = FredokaFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF01579B)
              )
              Text(
                text = if (selectedTab == 0) "New Daily Tasks • Coins & Gems" else "${profileStats.rankEmoji} ${profileStats.rankTitle}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0288D1)
              )
            }
          }
          IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF01579B))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Switcher: Daily Missions vs Profile Trophies
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFB3E5FC).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          EngagementTabPill(
            label = "🎯 MISSIONS",
            badgeCount = unclaimedMissionsCount,
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            testTag = "tab_missions",
            modifier = Modifier.weight(1f)
          )
          EngagementTabPill(
            label = "🏆 TROPHIES",
            badgeCount = unclaimedTrophiesCount,
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            testTag = "tab_achievements",
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
          // Progress Summary Card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(4.dp, RoundedCornerShape(16.dp))
              .background(
                brush = Brush.horizontalGradient(
                  listOf(Color(0xFF0091EA), Color(0xFF00B0FF))
                ),
                RoundedCornerShape(16.dp)
              )
              .padding(14.dp)
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Today's Challenges",
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontSize = 13.sp
                )
                Text(
                  text = "$completedCount / $totalCount Done",
                  fontWeight = FontWeight.Black,
                  color = Color(0xFFFFD600),
                  fontSize = 14.sp
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              // Progress Track
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
              ) {
                val ratio = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
                Box(
                  modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(10.dp)
                    .background(
                      brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD600), Color(0xFF00E676))
                      ),
                      RoundedCornerShape(5.dp)
                    )
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Missions List
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(290.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(missions) { mission ->
              MissionCardItem(
                mission = mission,
                onClaim = { onClaimMission(mission.id) }
              )
            }

            // Grand Chest Item at bottom of list
            item {
              GrandChestItem(
                allDone = allDone,
                isClaimed = isGrandChestClaimed,
                onClaim = onClaimGrandChest
              )
            }
          }
        } else {
          // Profile Summary & Trophy Case
          ProfileSummaryBanner(
            profileStats = profileStats,
            coins = coins,
            gems = gems
          )

          Spacer(modifier = Modifier.height(10.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(265.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(achievements) { ach ->
              AchievementTrophyCard(
                achievement = ach,
                onClaim = { onClaimAchievement(ach.id) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EngagementTabPill(
  label: String,
  badgeCount: Int,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .testTag(testTag)
      .shadow(if (selected) 4.dp else 0.dp, RoundedCornerShape(12.dp))
      .background(
        if (selected) Color.White else Color.Transparent,
        RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(vertical = 8.dp, horizontal = 6.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = FontWeight.Black,
      color = if (selected) Color(0xFF01579B) else Color(0xFF0277BD).copy(alpha = 0.8f)
    )
    if (badgeCount > 0) {
      Spacer(modifier = Modifier.width(5.dp))
      Box(
        modifier = Modifier
          .background(Color(0xFFFF2D87), CircleShape)
          .padding(horizontal = 6.dp, vertical = 1.dp)
      ) {
        Text(
          text = "$badgeCount",
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Black
        )
      }
    }
  }
}

@Composable
private fun ProfileSummaryBanner(
  profileStats: PlayerProfileStats,
  coins: Int,
  gems: Int
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(4.dp, RoundedCornerShape(18.dp))
      .background(
        brush = Brush.horizontalGradient(listOf(Color(0xFF5E35B1), Color(0xFF1E88E5))),
        shape = RoundedCornerShape(18.dp)
      )
      .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .background(Color.White.copy(alpha = 0.22f), CircleShape)
            .border(1.5.dp, Color(0xFFFFD600), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(profileStats.rankEmoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = profileStats.rankTitle,
            fontFamily = FredokaFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
          Text(
            text = "🏆 ${profileStats.unlockedTrophies}/${profileStats.totalTrophies} Trophies Unlocked",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFEE58)
          )
        }
      }
      Column(horizontalAlignment = Alignment.End) {
        Text("🪙 $coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("💎 $gems", color = Color(0xFF80D8FF), fontWeight = FontWeight.Black, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      ProfileStatChip("Levels", "${profileStats.totalLevelsCompleted}", Modifier.weight(1f))
      ProfileStatChip("Merges", "${profileStats.totalMerges}", Modifier.weight(1f))
      ProfileStatChip("Best Combo", "${profileStats.bestComboEver}x", Modifier.weight(1f))
      ProfileStatChip("Stacks", "${profileStats.totalStacksCleared}", Modifier.weight(1f))
    }
  }
}

@Composable
private fun ProfileStatChip(label: String, value: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
      .padding(vertical = 5.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    Text(label, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
  }
}

@Composable
private fun AchievementTrophyCard(
  achievement: Achievement,
  onClaim: () -> Unit
) {
  val tierColor = when (achievement.tier) {
    AchievementTier.BRONZE -> Color(0xFFCD7F32)
    AchievementTier.SILVER -> Color(0xFF78909C)
    AchievementTier.GOLD -> Color(0xFFFFB300)
    AchievementTier.DIAMOND -> Color(0xFF00B0FF)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(if (achievement.canClaim) 6.dp else 2.dp, RoundedCornerShape(18.dp))
      .background(
        when {
          achievement.canClaim -> Color(0xFFFFFDE7)
          achievement.isClaimed -> Color(0xFFE8F5E9)
          else -> Color.White
        },
        RoundedCornerShape(18.dp)
      )
      .border(
        width = if (achievement.canClaim) 2.dp else 1.dp,
        color = when {
          achievement.canClaim -> Color(0xFFFFB300)
          achievement.isClaimed -> Color(0xFF66BB6A)
          else -> Color(0xFFB3E5FC)
        },
        shape = RoundedCornerShape(18.dp)
      )
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Trophy Badge Icon
      Box(
        modifier = Modifier
          .size(46.dp)
          .shadow(if (achievement.isUnlocked) 4.dp else 0.dp, RoundedCornerShape(14.dp))
          .background(
            if (achievement.isUnlocked) tierColor.copy(alpha = 0.22f) else Color(0xFFECEFF1),
            RoundedCornerShape(14.dp)
          )
          .border(1.5.dp, if (achievement.isUnlocked) tierColor else Color(0xFFCFD8DC), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (achievement.isUnlocked) achievement.iconEmoji else "🔒",
          fontSize = 22.sp
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Title, Tier, Progress
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = achievement.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF01579B)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .background(tierColor, RoundedCornerShape(6.dp))
              .padding(horizontal = 5.dp, vertical = 1.dp)
          ) {
            Text(
              text = achievement.tier.label,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }
        Text(
          text = achievement.description,
          fontSize = 11.sp,
          color = Color(0xFF546E7A)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .background(Color(0xFFCFD8DC), RoundedCornerShape(3.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(achievement.progressPercent)
                .height(6.dp)
                .background(
                  if (achievement.isUnlocked) Color(0xFF00E676) else tierColor,
                  RoundedCornerShape(3.dp)
                )
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${achievement.currentProgress}/${achievement.targetValue}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (achievement.isUnlocked) Color(0xFF2E7D32) else Color(0xFF546E7A)
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Reward & Action Button
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = achievement.rewardBadge,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color(0xFF0288D1)
        )
        Spacer(modifier = Modifier.height(4.dp))
        when {
          achievement.isClaimed -> {
            Box(
              modifier = Modifier
                .background(Color(0xFFC8E6C9), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text(
                text = "EARNED ✓",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2E7D32)
              )
            }
          }
          achievement.canClaim -> {
            Arcade3DButton(
              onClick = onClaim,
              colors = ArcadeButtonStyles.AmberGold,
              height = 34.dp,
              cornerRadius = 10.dp,
              lipHeight = 3.dp,
              modifier = Modifier.width(86.dp)
            ) {
              Text(
                text = "CLAIM",
                fontFamily = RubikFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
            }
          }
          else -> {
            Box(
              modifier = Modifier
                .background(Color(0xFFECEFF1), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "LOCKED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun MissionCardItem(
  mission: DailyMission,
  onClaim: () -> Unit
) {
  val isDone = mission.isCompleted
  val isClaimed = mission.isClaimed
  val canClaim = mission.canClaim

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(if (canClaim) 6.dp else 2.dp, RoundedCornerShape(18.dp))
      .background(
        if (canClaim) Color(0xFFE8F5E9) else if (isClaimed) Color(0xFFF5F5F5) else Color.White,
        RoundedCornerShape(18.dp)
      )
      .border(
        width = if (canClaim) 2.dp else 1.dp,
        color = if (canClaim) Color(0xFF00E676) else if (isClaimed) Color(0xFFB0BEC5) else Color(0xFF81D4FA),
        shape = RoundedCornerShape(18.dp)
      )
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left Icon Badge
      Box(
        modifier = Modifier
          .size(44.dp)
          .shadow(2.dp, RoundedCornerShape(12.dp))
          .background(
            if (canClaim) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFE1F5FE),
            RoundedCornerShape(12.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(mission.iconEmoji, fontSize = 22.sp)
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Middle Description & Progress
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = mission.title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF01579B)
        )
        Text(
          text = mission.description,
          fontSize = 11.sp,
          color = Color(0xFF546E7A)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar + count
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(6.dp)
              .background(Color(0xFFCFD8DC), RoundedCornerShape(3.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(mission.progressPercent)
                .height(6.dp)
                .background(
                  if (canClaim) Color(0xFF00E676) else Color(0xFF00B0FF),
                  RoundedCornerShape(3.dp)
                )
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${mission.currentProgress}/${mission.targetValue}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDone) Color(0xFF2E7D32) else Color(0xFF455A64)
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Right Action Button & Reward Badge
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = mission.reward.displayBadge,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color(0xFFFF8F00)
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (isClaimed) {
          Box(
            modifier = Modifier
              .background(Color(0xFFCFD8DC), RoundedCornerShape(12.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "CLAIMED ✓",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF78909C)
            )
          }
        } else if (canClaim) {
          Arcade3DButton(
            onClick = onClaim,
            colors = ArcadeButtonStyles.Emerald,
            height = 34.dp,
            cornerRadius = 11.dp,
            lipHeight = 3.dp,
            modifier = Modifier.width(86.dp)
          ) {
            Text(
              text = "CLAIM!",
              fontFamily = RubikFontFamily,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        } else {
          Box(
            modifier = Modifier
              .background(Color(0xFFE1F5FE), RoundedCornerShape(12.dp))
              .border(1.dp, Color(0xFF81D4FA), RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "IN PROGRESS",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0288D1)
            )
          }
        }
      }
    }
  }
}

@Composable
fun GrandChestItem(
  allDone: Boolean,
  isClaimed: Boolean,
  onClaim: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(if (allDone && !isClaimed) 8.dp else 2.dp, RoundedCornerShape(18.dp))
      .background(
        brush = Brush.horizontalGradient(
          if (allDone && !isClaimed) listOf(Color(0xFFFFD600), Color(0xFFFF9100))
          else if (isClaimed) listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))
          else listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC))
        ),
        RoundedCornerShape(18.dp)
      )
      .border(
        width = 1.5.dp,
        color = if (allDone && !isClaimed) Color(0xFFFF6F00) else Color(0xFF90CAF9),
        shape = RoundedCornerShape(18.dp)
      )
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Text(if (allDone && !isClaimed) "🎁" else if (isClaimed) "✨" else "🔒", fontSize = 28.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "GRAND BONUS CHEST",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (allDone && !isClaimed) Color(0xFF3E2723) else Color(0xFF01579B)
          )
          Text(
            text = if (isClaimed) "Claimed! Resets tomorrow"
            else if (allDone) "+500 Coins, +25 Gems & Boosters!"
            else "Complete all 4 missions to unlock!",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (allDone && !isClaimed) Color(0xFF4E342E) else Color(0xFF455A64)
          )
        }
      }

      if (allDone && !isClaimed) {
        Arcade3DButton(
          onClick = onClaim,
          colors = ArcadeButtonStyles.Crimson,
          height = 38.dp,
          cornerRadius = 12.dp,
          lipHeight = 3.dp,
          modifier = Modifier.width(92.dp)
        ) {
          Text(
            text = "OPEN",
            fontFamily = RubikFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }
      }
    }
  }
}
