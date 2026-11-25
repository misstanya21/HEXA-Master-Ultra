package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameMode
import com.example.ui.theme.NunitoFontFamily
import com.example.ui.theme.RubikFontFamily
import com.example.ui.theme.TitanOneFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity full-screen animated overlay for Level Complete celebration.
 */
@Composable
fun AnimatedLevelCompleteOverlay(
  visible: Boolean,
  levelNumber: Int,
  score: Int,
  coinsEarned: Int = 100,
  onNextLevel: () -> Unit,
  onReplay: () -> Unit,
  onHome: () -> Unit,
  onPlaySound: ((String) -> Unit)? = null
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(350)),
    exit = fadeOut(animationSpec = tween(300))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .testTag("level_complete_overlay")
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xCC01579B),
              Color(0xEE002171),
              Color(0xF5000A12)
            ),
            radius = 1200f
          )
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { /* Consume taps to prevent click-through */ },
      contentAlignment = Alignment.Center
    ) {
      CelebrationRayCanvas()
      FestiveConfettiCanvas()

      val cardScale = remember { Animatable(0.7f) }
      val star1Scale = remember { Animatable(0f) }
      val star2Scale = remember { Animatable(0f) }
      val star3Scale = remember { Animatable(0f) }

      var targetScore by remember { mutableIntStateOf(0) }
      val animatedScore by animateIntAsState(
        targetValue = targetScore,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "score_count"
      )

      LaunchedEffect(visible) {
        if (visible) {
          cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
          delay(120)
          onPlaySound?.invoke("star1")
          star1Scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
          star1Scale.animateTo(1.0f, tween(80))

          delay(140)
          onPlaySound?.invoke("star2")
          star2Scale.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
          star2Scale.animateTo(1.18f, tween(80))

          delay(140)
          onPlaySound?.invoke("star3")
          star3Scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
          star3Scale.animateTo(1.0f, tween(80))

          targetScore = score
        } else {
          cardScale.snapTo(0.7f)
          star1Scale.snapTo(0f)
          star2Scale.snapTo(0f)
          star3Scale.snapTo(0f)
          targetScore = 0
        }
      }

      val infiniteTransition = rememberInfiniteTransition(label = "pulse_border")
      val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
          animation = tween(850, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "button_pulse"
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .widthIn(max = 440.dp)
          .scale(cardScale.value)
          .shadow(
            elevation = 32.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color(0xFFFFD600).copy(alpha = 0.4f),
            spotColor = Color(0xFFFF8F00).copy(alpha = 0.6f)
          )
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF0F9FF),
                Color(0xFFE1F5FE)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .border(
            width = 3.dp,
            brush = Brush.sweepGradient(
              listOf(
                Color(0xFFFFD600),
                Color(0xFFFF80AB),
                Color(0xFF40C4FF),
                Color(0xFF00E676),
                Color(0xFFFFD600)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 26.dp, vertical = 28.dp)
      ) {
        // Floating Top Ribbon
        Box(
          modifier = Modifier
            .offset(y = (-6).dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .background(
              brush = Brush.horizontalGradient(
                listOf(Color(0xFFFF2D87), Color(0xFFFF6D00), Color(0xFFFF2D87))
              ),
              shape = RoundedCornerShape(20.dp)
            )
            .border(2.dp, Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "LEVEL $levelNumber",
            fontFamily = RubikFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Animated Stars Row
        Row(
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalAlignment = Alignment.Bottom,
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          Decorative3DStar(scale = star1Scale.value, size = 54.dp, tiltDegrees = -12f)
          Decorative3DStar(scale = star2Scale.value, size = 76.dp, tiltDegrees = 0f)
          Decorative3DStar(scale = star3Scale.value, size = 54.dp, tiltDegrees = 12f)
        }

        Spacer(modifier = Modifier.height(12.dp))

        ArcadeOutlinedText(
          text = "VICTORY!",
          fontSizeSp = 34f,
          fillColor = Color(0xFF00C853),
          strokeColor = Color(0xFF004D25),
          strokeWidthPx = 7f,
          fontFamily = TitanOneFontFamily,
          fontWeight = FontWeight.Normal,
          letterSpacingSp = 1.0f
        )

        Text(
          text = "Outstanding Play! Stage Cleared",
          fontFamily = NunitoFontFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF0277BD),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Score & Coin Reward Stats Card
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .background(
              brush = Brush.verticalGradient(
                listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC).copy(alpha = 0.7f))
              ),
              shape = RoundedCornerShape(22.dp)
            )
            .border(1.5.dp, Color.White, RoundedCornerShape(22.dp))
            .padding(vertical = 16.dp, horizontal = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "TOTAL SCORE",
            fontFamily = RubikFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0277BD),
            letterSpacing = 1.5.sp
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "$animatedScore",
            fontFamily = TitanOneFontFamily,
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF01579B),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .shadow(4.dp, RoundedCornerShape(16.dp))
              .background(
                brush = Brush.horizontalGradient(
                  listOf(Color(0xFFFFD600), Color(0xFFFF9100))
                ),
                shape = RoundedCornerShape(16.dp)
              )
              .border(1.2.dp, Color.White, RoundedCornerShape(16.dp))
              .padding(horizontal = 14.dp, vertical = 5.dp)
          ) {
            VectorCoinIcon(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "+$coinsEarned Coins Awarded!",
              fontFamily = RubikFontFamily,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFF3E2723)
            )
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Primary "Next Level" 3D Arcade Button
        Arcade3DButton(
          onClick = onNextLevel,
          colors = ArcadeButtonStyles.Emerald,
          height = 56.dp,
          cornerRadius = 22.dp,
          lipHeight = 6.dp,
          modifier = Modifier
            .testTag("next_level_button")
            .fillMaxWidth()
            .scale(buttonPulse)
        ) {
          Text(
            text = "NEXT LEVEL",
            fontFamily = RubikFontFamily,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary 3D Buttons: Replay & Menu
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Arcade3DButton(
            onClick = onReplay,
            colors = ArcadeButtonStyles.SkyBlue,
            height = 46.dp,
            cornerRadius = 17.dp,
            lipHeight = 4.dp,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = "Replay", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("REPLAY", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.5.sp)
          }

          Arcade3DButton(
            onClick = onHome,
            colors = ArcadeButtonStyles.Slate,
            height = 46.dp,
            cornerRadius = 17.dp,
            lipHeight = 4.dp,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Rounded.Home, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MENU", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.5.sp)
          }
        }
      }
    }
  }
}

/**
 * High-fidelity full-screen animated overlay for Game Over / Out of Moves.
 */
@Composable
fun AnimatedGameOverOverlay(
  visible: Boolean,
  score: Int,
  levelNumber: Int,
  gameMode: GameMode = GameMode.CLASSIC,
  bestScore: Int = 0,
  hasHammerBooster: Boolean = true,
  onRetry: () -> Unit,
  onUseHammer: () -> Unit,
  onBonusCoins: (() -> Unit)? = null,
  onHome: () -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(350)),
    exit = fadeOut(animationSpec = tween(300))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .testTag("game_over_overlay")
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xDD3E2723),
              Color(0xF0212121),
              Color(0xF8000000)
            ),
            radius = 1200f
          )
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { /* Consume taps */ },
      contentAlignment = Alignment.Center
    ) {
      val cardScale = remember { Animatable(0.75f) }
      val badgeShake = remember { Animatable(0f) }

      LaunchedEffect(visible) {
        if (visible) {
          cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
          repeat(3) {
            badgeShake.animateTo(-12f, tween(50))
            badgeShake.animateTo(12f, tween(50))
          }
          badgeShake.animateTo(0f, tween(50))
        } else {
          cardScale.snapTo(0.75f)
        }
      }

      val infiniteTransition = rememberInfiniteTransition(label = "pulse_retry")
      val retryPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
          animation = tween(850, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "retry_pulse"
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .widthIn(max = 440.dp)
          .scale(cardScale.value)
          .shadow(
            elevation = 32.dp,
            shape = RoundedCornerShape(32.dp),
            ambientColor = Color(0xFFFF5252).copy(alpha = 0.5f),
            spotColor = Color(0xFFD50000).copy(alpha = 0.7f)
          )
          .background(
            brush = Brush.verticalGradient(
              listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFEBEE),
                Color(0xFFFFCDD2)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .border(
            width = 3.dp,
            brush = Brush.sweepGradient(
              listOf(
                Color(0xFFFF1744),
                Color(0xFFFF8A80),
                Color(0xFFFF5252),
                Color(0xFFFF1744)
              )
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 26.dp, vertical = 28.dp)
      ) {
        Box(
          modifier = Modifier
            .rotate(badgeShake.value)
            .size(72.dp)
            .shadow(12.dp, CircleShape)
            .background(
              brush = Brush.radialGradient(
                listOf(Color(0xFFFF5252), Color(0xFFD50000))
              ),
              shape = CircleShape
            )
            .border(3.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = "No More Moves",
            tint = Color(0xFFFFEE58),
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        ArcadeOutlinedText(
          text = "NO MORE MOVES!",
          fontSizeSp = 26f,
          fillColor = Color(0xFFD50000),
          strokeColor = Color(0xFF4E0000),
          strokeWidthPx = 6f,
          fontFamily = TitanOneFontFamily,
          fontWeight = FontWeight.Normal,
          letterSpacingSp = 0.6f
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "The hexagon pedestals are completely filled.\nTry another strategy or smash with a booster!",
          fontFamily = NunitoFontFamily,
          fontSize = 13.sp,
          color = Color(0xFF5D4037),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Score Summary Card
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.5.dp, Color(0xFFFFCDD2), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            val modeLabel = when (gameMode) {
              GameMode.CLASSIC -> "LEVEL $levelNumber"
              GameMode.TIME_ATTACK -> "TIME ATTACK"
              GameMode.ENDLESS -> "ENDLESS MODE"
            }
            Text(
              text = modeLabel,
              fontFamily = RubikFontFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = Color(0xFFC62828)
            )
            Text(
              text = if (gameMode != GameMode.CLASSIC && bestScore > 0) "BEST: $bestScore" else "SCORE REACHED",
              fontFamily = NunitoFontFamily,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF757575)
            )
          }

          Text(
            text = "$score",
            fontFamily = TitanOneFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFB71C1C)
          )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Primary 3D Action: Retry Button
        Arcade3DButton(
          onClick = onRetry,
          colors = ArcadeButtonStyles.AmberGold,
          height = 56.dp,
          cornerRadius = 22.dp,
          lipHeight = 6.dp,
          modifier = Modifier
            .testTag("retry_button")
            .fillMaxWidth()
            .scale(retryPulse)
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (gameMode == GameMode.CLASSIC) "RETRY LEVEL" else "PLAY AGAIN",
            fontFamily = RubikFontFamily,
            fontSize = 18.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.8.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary 3D Booster Option: Hammer Recovery
        Arcade3DButton(
          onClick = onUseHammer,
          enabled = hasHammerBooster,
          colors = ArcadeButtonStyles.SkyBlue,
          height = 48.dp,
          cornerRadius = 18.dp,
          lipHeight = 4.dp,
          modifier = Modifier
            .testTag("hammer_recover_button")
            .fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Hardware, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (hasHammerBooster) "Smash Stack with Hammer" else "Need Hammer (100 Coins)",
            fontFamily = RubikFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        if (!hasHammerBooster && onBonusCoins != null) {
          Spacer(modifier = Modifier.height(10.dp))

          Arcade3DButton(
            onClick = onBonusCoins,
            colors = ArcadeButtonStyles.AmberGold,
            height = 46.dp,
            cornerRadius = 17.dp,
            lipHeight = 4.dp,
            modifier = Modifier
              .testTag("bonus_coins_recover_button")
              .fillMaxWidth()
          ) {
            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Get +100 Bonus Coins",
              fontFamily = RubikFontFamily,
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Arcade3DButton(
          onClick = onHome,
          colors = ArcadeButtonStyles.Slate,
          height = 42.dp,
          cornerRadius = 16.dp,
          lipHeight = 4.dp,
          modifier = Modifier.fillMaxWidth(0.74f)
        ) {
          Icon(Icons.Rounded.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("MAIN MENU", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
        }
      }
    }
  }
}

/**
 * Celebratory Time's Up results overlay for Time Attack Mode.
 */
@Composable
fun AnimatedTimeAttackResultOverlay(
  visible: Boolean,
  score: Int,
  bestScore: Int,
  isNewRecord: Boolean,
  coinsEarned: Int,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(350)),
    exit = fadeOut(animationSpec = tween(250))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .testTag("time_attack_result_overlay")
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xCCFF6D00),
              Color(0xEE0D47A1),
              Color(0xF5001028)
            ),
            radius = 1200f
          )
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { /* Consume taps */ },
      contentAlignment = Alignment.Center
    ) {
      CelebrationRayCanvas()
      FestiveConfettiCanvas()

      val cardScale = remember { Animatable(0.75f) }
      LaunchedEffect(visible) {
        if (visible) {
          cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        } else {
          cardScale.snapTo(0.75f)
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth(0.92f)
          .widthIn(max = 440.dp)
          .scale(cardScale.value)
          .shadow(28.dp, RoundedCornerShape(32.dp))
          .background(
            brush = Brush.verticalGradient(
              listOf(Color(0xFFFFFFFF), Color(0xFFFFF8E1), Color(0xFFFFECB3))
            ),
            shape = RoundedCornerShape(32.dp)
          )
          .border(3.dp, Color(0xFFFF9100), RoundedCornerShape(32.dp))
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 26.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .shadow(12.dp, CircleShape)
            .background(
              brush = Brush.radialGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF6D00))),
              shape = CircleShape
            )
            .border(3.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Rounded.Timer,
            contentDescription = "Time's Up",
            tint = Color.White,
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ArcadeOutlinedText(
          text = "TIME'S UP!",
          fontSizeSp = 30f,
          fillColor = Color(0xFFFF6D00),
          strokeColor = Color(0xFF4E2300),
          strokeWidthPx = 6.5f,
          fontFamily = TitanOneFontFamily,
          fontWeight = FontWeight.Normal,
          letterSpacingSp = 0.8f
        )

        if (isNewRecord) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .background(Color(0xFFFF2D87), RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.EmojiEvents,
              contentDescription = null,
              tint = Color(0xFFFFEE58),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "NEW HIGH SCORE!",
              fontFamily = RubikFontFamily,
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0xFFFFCC80), RoundedCornerShape(20.dp))
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "FINAL SCORE",
                fontFamily = RubikFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C)
              )
              Text(
                text = "$score",
                fontFamily = TitanOneFontFamily,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF01579B)
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "BEST: $bestScore",
                fontFamily = RubikFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE65100)
              )
              if (coinsEarned > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  VectorCoinIcon(modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "+$coinsEarned Coins",
                    fontFamily = RubikFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Arcade3DButton(
          onClick = onPlayAgain,
          colors = ArcadeButtonStyles.Emerald,
          height = 56.dp,
          cornerRadius = 22.dp,
          lipHeight = 5.dp,
          modifier = Modifier
            .testTag("play_again_button")
            .fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PLAY AGAIN",
            fontFamily = RubikFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Arcade3DButton(
          onClick = onHome,
          colors = ArcadeButtonStyles.SkyBlue,
          height = 46.dp,
          cornerRadius = 17.dp,
          lipHeight = 4.dp,
          modifier = Modifier.fillMaxWidth(0.75f)
        ) {
          Icon(Icons.Rounded.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("MAIN MENU", fontFamily = RubikFontFamily, fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.5.sp)
        }
      }
    }
  }
}

@Composable
private fun Decorative3DStar(
  scale: Float,
  size: androidx.compose.ui.unit.Dp,
  tiltDegrees: Float
) {
  Box(
    modifier = Modifier
      .scale(scale)
      .rotate(tiltDegrees)
      .size(size)
      .shadow(12.dp, CircleShape)
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFFF9C4),
            Color(0xFFFFD600),
            Color(0xFFFF8F00)
          )
        ),
        shape = CircleShape
      )
      .border(2.5.dp, Color.White, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.Star,
      contentDescription = null,
      tint = Color.White.copy(alpha = 0.95f),
      modifier = Modifier.size(size * 0.72f)
    )
  }
}

@Composable
private fun CelebrationRayCanvas() {
  val infiniteTransition = rememberInfiniteTransition(label = "rays")
  val rayAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "ray_rotation"
  )

  Canvas(modifier = Modifier.fillMaxSize()) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val rayCount = 16
    val maxRadius = kotlin.math.max(size.width, size.height)

    rotate(rayAngle, pivot = center) {
      for (i in 0 until rayCount) {
        val angleDeg = i * (360f / rayCount)
        val angleRad = angleDeg * PI.toFloat() / 180f
        val rayEnd = Offset(
          center.x + maxRadius * cos(angleRad),
          center.y + maxRadius * sin(angleRad)
        )
        drawLine(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFD600).copy(alpha = 0.28f),
              Color(0xFFFF80AB).copy(alpha = 0.12f),
              Color.Transparent
            ),
            center = center,
            radius = maxRadius * 0.7f
          ),
          start = center,
          end = rayEnd,
          strokeWidth = 36f
        )
      }
    }
  }
}

@Composable
private fun FestiveConfettiCanvas() {
  val infiniteTransition = rememberInfiniteTransition(label = "confetti")
  val animProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "confetti_fall"
  )

  val confettiColors = remember {
    listOf(
      Color(0xFFFF2D87),
      Color(0xFFFFD600),
      Color(0xFF00E676),
      Color(0xFF00A0FF),
      Color(0xFFAA00FF),
      Color(0xFFFFFFFF)
    )
  }

  Canvas(modifier = Modifier.fillMaxSize()) {
    val count = 35
    for (i in 0 until count) {
      val seed = (i * 97) % 1000 / 1000f
      val color = confettiColors[i % confettiColors.size]
      val x = size.width * ((seed * 1.3f + i * 0.03f) % 1f)
      val initialY = -50f
      val travelDistance = size.height + 100f
      val y = initialY + ((animProgress + seed) % 1f) * travelDistance

      val sizePx = 8f + (i % 6) * 2f
      val rotation = (animProgress * 360f * (if (i % 2 == 0) 1 else -1) + seed * 180f)

      rotate(rotation, pivot = Offset(x, y)) {
        if (i % 3 == 0) {
          drawCircle(color = color.copy(alpha = 0.75f), radius = sizePx / 2f, center = Offset(x, y))
        } else {
          drawRect(
            color = color.copy(alpha = 0.75f),
            topLeft = Offset(x - sizePx / 2f, y - sizePx / 3f),
            size = androidx.compose.ui.geometry.Size(sizePx, sizePx * 0.65f)
          )
        }
      }
    }
  }
}
