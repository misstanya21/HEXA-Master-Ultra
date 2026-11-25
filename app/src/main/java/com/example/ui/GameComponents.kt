package com.example.ui

import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.HexMath
import com.example.model.BoosterType
import com.example.model.GameMode
import com.example.model.StagedStack
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.RubikFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TopHeaderBar(
  coins: Int,
  remainingGoal: Int,
  secondaryGoalType: String?,
  secondaryGoalCount: Int,
  levelNumber: Int,
  gameMode: GameMode = GameMode.CLASSIC,
  score: Int = 0,
  timeRemainingSec: Int = 90,
  endlessWave: Int = 1,
  bestScore: Int = 0,
  onPauseClick: () -> Unit,
  onCoinsClick: () -> Unit,
  onDailyClick: () -> Unit,
  canClaimDaily: Boolean,
  onMissionsClick: () -> Unit = {},
  unclaimedMissions: Int = 0,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. High-Contrast Sapphire Glass Coins Counter Pill with Crisp 3D Vector Coin
    Row(
      modifier = Modifier
        .testTag("coins_display")
        .minimumInteractiveComponentSize()
        .shadow(10.dp, RoundedCornerShape(22.dp))
        .background(
          brush = Brush.verticalGradient(
            listOf(Color(0xE6103C78), Color(0xEE082147))
          ),
          shape = RoundedCornerShape(22.dp)
        )
        .border(1.5.dp, Color(0xFF64D8FF), RoundedCornerShape(22.dp))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true)
        ) { onCoinsClick() }
        .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      VectorCoinIcon(modifier = Modifier.size(26.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "$coins +",
        color = Color(0xFFFFEE58),
        fontFamily = RubikFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 14.5.sp,
        letterSpacing = 0.3.sp
      )
    }

    // 2. Center Mode-Aware Score / Goal / Timer Hub
    when (gameMode) {
      GameMode.CLASSIC -> {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Primary Rainbow Hexagon Goal Hub
          Box(
            modifier = Modifier
              .testTag("primary_goal_badge")
              .size(width = 58.dp, height = 56.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val center = Offset(size.width / 2f, size.height * 0.46f)
              val outerR = size.minDimension * 0.46f
              drawFrostedHexBadgeFrame(center, outerR)
              drawRainbowHexagonCore(center, outerR * 0.72f)
            }
            OutlinedScoreText(
              text = "$remainingGoal",
              fontSizeSp = 17f,
              strokeColor = Color(0xFF1565C0),
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 1.dp)
            )
          }

          // Secondary Objective Hex Hub (Robots, Coins, Stars, etc.)
          if (secondaryGoalType != null && secondaryGoalCount > 0) {
            Box(
              modifier = Modifier
                .testTag("secondary_goal_badge")
                .size(width = 54.dp, height = 56.dp),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.46f)
                val outerR = size.minDimension * 0.45f
                drawFrostedHexBadgeFrame(center, outerR)
                when (secondaryGoalType) {
                  "Robots" -> drawMiniRobotIcon(center, outerR * 0.56f)
                  "Coins" -> drawVectorCoin(center, outerR * 0.52f)
                  "Stars" -> drawMiniStarIcon(center, outerR * 0.54f)
                  "Honeycombs" -> drawMiniHoneyIcon(center, outerR * 0.52f)
                  "Lightning" -> drawMiniBoltIcon(center, outerR * 0.52f)
                  else -> drawMiniStarIcon(center, outerR * 0.54f)
                }
              }
              OutlinedScoreText(
                text = "$secondaryGoalCount",
                fontSizeSp = 15f,
                strokeColor = Color(0xFF0D47A1),
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .offset(y = 1.dp)
              )
            }
          }
        }
      }

      GameMode.TIME_ATTACK -> {
        val mins = timeRemainingSec / 60
        val secs = timeRemainingSec % 60
        val timerText = String.format("%d:%02d", mins, secs)
        val isUrgent = timeRemainingSec <= 15
        val timerColors = if (isUrgent) {
          listOf(Color(0xFFFF5252), Color(0xFFD50000))
        } else {
          listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .testTag("time_attack_timer")
              .shadow(10.dp, RoundedCornerShape(18.dp))
              .background(Brush.verticalGradient(timerColors), RoundedCornerShape(18.dp))
              .border(1.6.dp, if (isUrgent) Color(0xFFFFEE58) else Color.White.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.Timer,
              contentDescription = "Timer",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = timerText,
              color = Color.White,
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }

          Column(
            modifier = Modifier
              .testTag("mode_score_badge")
              .shadow(8.dp, RoundedCornerShape(16.dp))
              .background(
                Brush.verticalGradient(listOf(Color(0xE6103C78), Color(0xEE082147))),
                RoundedCornerShape(16.dp)
              )
              .border(1.5.dp, Color(0xFF64D8FF), RoundedCornerShape(16.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "$score",
              color = Color.White,
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 14.sp
            )
            if (bestScore > 0) {
              Text(
                text = "BEST $bestScore",
                color = Color(0xFFFFEE58),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
              )
            }
          }
        }
      }

      GameMode.ENDLESS -> {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .testTag("mode_score_badge")
              .shadow(10.dp, RoundedCornerShape(18.dp))
              .background(
                Brush.verticalGradient(listOf(Color(0xFF9C4DFF), Color(0xFF512DA8))),
                RoundedCornerShape(18.dp)
              )
              .border(1.6.dp, Color(0xFFE1BEE7), RoundedCornerShape(18.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Rounded.AllInclusive,
              contentDescription = "Endless Score",
              tint = Color(0xFFFFEE58),
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "$score",
              color = Color.White,
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }

          Column(
            modifier = Modifier
              .testTag("endless_best_badge")
              .shadow(8.dp, RoundedCornerShape(16.dp))
              .background(
                Brush.verticalGradient(listOf(Color(0xE60B4F6C), Color(0xEE052A3D))),
                RoundedCornerShape(16.dp)
              )
              .border(1.5.dp, Color(0xFF40E8FF), RoundedCornerShape(16.dp))
              .padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "WAVE $endlessWave",
              color = Color.White,
              fontFamily = RubikFontFamily,
              fontWeight = FontWeight.Black,
              fontSize = 11.sp
            )
            if (bestScore > 0) {
              Text(
                text = "BEST $bestScore",
                color = Color(0xFFFFEE58),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
              )
            }
          }
        }
      }
    }

    // 3. Right Header Controls: Missions, Daily Gift, Pause
    Row(
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Missions Target Button
      HeaderSquircleButton(
        testTag = "daily_missions_button",
        highlightActive = unclaimedMissions > 0,
        activeColors = listOf(Color(0xFFFF5C93), Color(0xFFD81B60)),
        badgeCount = unclaimedMissions,
        onClick = onMissionsClick
      ) {
        VectorTargetIcon(modifier = Modifier.size(22.dp))
      }

      // Daily Reward Gift Button
      HeaderSquircleButton(
        testTag = "daily_reward_button",
        highlightActive = canClaimDaily,
        activeColors = listOf(Color(0xFFFFB74D), Color(0xFFF57C00)),
        badgeCount = 0,
        onClick = onDailyClick
      ) {
        VectorGiftIcon(modifier = Modifier.size(21.dp))
      }

      // Sleek Pause / Settings Button (matching reference high-contrast Pause squircle)
      HeaderSquircleButton(
        testTag = "pause_button",
        highlightActive = false,
        activeColors = listOf(Color(0xFF3E9BF0), Color(0xFF196FC2)),
        badgeCount = 0,
        onClick = onPauseClick
      ) {
        Icon(
          imageVector = Icons.Default.Pause,
          contentDescription = "Pause and Settings",
          tint = Color.White,
          modifier = Modifier.size(22.dp)
        )
      }
    }
  }
}

@Composable
private fun HeaderSquircleButton(
  testTag: String,
  highlightActive: Boolean,
  activeColors: List<Color>,
  badgeCount: Int,
  onClick: () -> Unit,
  content: @Composable () -> Unit
) {
  val bgColors = if (highlightActive) {
    activeColors
  } else {
    listOf(Color(0xFF29B6F6), Color(0xFF0277BD))
  }
  Box(
    modifier = Modifier
      .testTag(testTag)
      .minimumInteractiveComponentSize()
      .size(42.dp)
      .shadow(8.dp, RoundedCornerShape(14.dp))
      .clip(RoundedCornerShape(14.dp))
      .background(
        brush = Brush.verticalGradient(bgColors),
        shape = RoundedCornerShape(14.dp)
      )
      .border(
        width = if (highlightActive) 2.dp else 1.5.dp,
        color = if (highlightActive) Color(0xFFFFEE58) else Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(14.dp)
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true)
      ) { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth(0.84f)
        .padding(top = 2.dp)
        .height(14.dp)
        .background(
          brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.36f), Color.Transparent)),
          shape = RoundedCornerShape(10.dp)
        )
    )
    content()
    if (badgeCount > 0) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = 4.dp, y = (-4).dp)
          .size(16.dp)
          .background(Color(0xFFFF1744), CircleShape)
          .border(1.2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "$badgeCount",
          color = Color.White,
          fontFamily = RubikFontFamily,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun OutlinedScoreText(
  text: String,
  fontSizeSp: Float,
  strokeColor: Color,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Text(
      text = text,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Black,
      fontSize = fontSizeSp.sp,
      color = strokeColor,
      style = TextStyle(
        drawStyle = Stroke(
          width = 5.5f,
          join = StrokeJoin.Round,
          cap = StrokeCap.Round
        )
      )
    )
    Text(
      text = text,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.Black,
      fontSize = fontSizeSp.sp,
      color = Color.White
    )
  }
}

/**
 * Bottom Staging Pedestals featuring crisp 3D multi-layered stacked hexagonal number blocks
 * sitting atop frosted 3D hexagonal columns.
 */
@Composable
fun StagingArea(
  stagedStacks: List<StagedStack?>,
  selectedIndex: Int?,
  draggedPedestalIndex: Int? = null,
  onStackSelected: (Int) -> Unit,
  onDragStart: (Int, Offset) -> Unit,
  onDragMove: (Offset) -> Unit,
  onDragEnd: () -> Unit,
  isColorblindMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.Bottom
  ) {
    for (i in 0 until 3) {
      val stack = stagedStacks.getOrNull(i)
      val isSelected = selectedIndex == i
      val isBeingDragged = draggedPedestalIndex == i

      StagingPedestal(
        stack = stack,
        isSelected = isSelected,
        isBeingDragged = isBeingDragged,
        onClick = { onStackSelected(i) },
        onDragStart = { offset -> onDragStart(i, offset) },
        onDragMove = onDragMove,
        onDragEnd = onDragEnd,
        slotIndex = i,
        isColorblindMode = isColorblindMode
      )
    }
  }
}

@Composable
fun StagingPedestal(
  stack: StagedStack?,
  isSelected: Boolean,
  isBeingDragged: Boolean = false,
  onClick: () -> Unit,
  onDragStart: (Offset) -> Unit,
  onDragMove: (Offset) -> Unit,
  onDragEnd: () -> Unit,
  slotIndex: Int,
  isColorblindMode: Boolean = false
) {
  val context = LocalContext.current
  val displayTypeface = remember(context) {
    ResourcesCompat.getFont(context, R.font.fredoka) ?: Typeface.DEFAULT_BOLD
  }

  var globalOrigin by remember { mutableStateOf(Offset.Zero) }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isSelected) 1.07f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(550),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier
      .testTag("staging_pedestal_$slotIndex")
      .size(width = 96.dp, height = 108.dp)
      .onGloballyPositioned { coordinates ->
        globalOrigin = coordinates.positionInRoot()
      }
      .pointerInput(stack) {
        if (stack != null) {
          detectDragGestures(
            onDragStart = { localOffset ->
              onDragStart(Offset(globalOrigin.x + localOffset.x, globalOrigin.y + localOffset.y))
            },
            onDrag = { change, _ ->
              change.consume()
              onDragMove(Offset(globalOrigin.x + change.position.x, globalOrigin.y + change.position.y))
            },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() }
          )
        }
      }
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onClick() }
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .scale(if (isSelected) pulseScale else 1f)
    ) {
      val cx = size.width / 2f
      val baseY = size.height * 0.62f
      val hexR = size.width * 0.42f
      val liftY = if (isSelected) -10.dp.toPx() else 0f

      // 1. Tall Frosted 3D Pedestal Column below the staging spot
      drawStagingPillar(Offset(cx, baseY), hexR * 0.96f, size.height - baseY + 12f)

      // 2. Vibrant Multi-Layer 3D Stacked Hexagonal Tiles (hidden while actively lifted under drag)
      if (!isBeingDragged && stack != null && stack.tiles.isNotEmpty()) {
        if (isSelected) {
          val selHex = HexMath.createRoundedHexagonPath(Offset(cx, baseY + liftY - 8f), hexR * 1.06f, 0.16f)
          drawPath(selHex, color = Color(0xFFFFD600).copy(alpha = 0.85f), style = Stroke(width = 4.5f))
        }
        draw3DLayeredStack(
          center = Offset(cx, baseY + liftY),
          radius = hexR * 1.02f,
          stack = stack.tiles,
          popScale = 1f,
          isColorblindMode = isColorblindMode,
          typeface = displayTypeface
        )
      }
    }
  }
}

private fun DrawScope.drawStagingPillar(center: Offset, radius: Float, height: Float) {
  val vertices = (0 until 6).map { i ->
    val angleRad = Math.toRadians((60.0 * i))
    Offset(center.x + radius * cos(angleRad).toFloat(), center.y + radius * sin(angleRad).toFloat())
  }

  val leftFacet = Path().apply {
    moveTo(vertices[3].x, vertices[3].y)
    lineTo(vertices[2].x, vertices[2].y)
    lineTo(vertices[2].x, vertices[2].y + height)
    lineTo(vertices[3].x, vertices[3].y + height)
    close()
  }
  drawPath(
    leftFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF428CD1).copy(alpha = 0.88f), Color.Transparent),
      startY = vertices[3].y,
      endY = vertices[3].y + height
    )
  )

  val centerFacet = Path().apply {
    moveTo(vertices[2].x, vertices[2].y)
    lineTo(vertices[1].x, vertices[1].y)
    lineTo(vertices[1].x, vertices[1].y + height)
    lineTo(vertices[2].x, vertices[2].y + height)
    close()
  }
  drawPath(
    centerFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFF70B6EF).copy(alpha = 0.92f), Color.Transparent),
      startY = vertices[2].y,
      endY = vertices[2].y + height
    )
  )

  val rightFacet = Path().apply {
    moveTo(vertices[1].x, vertices[1].y)
    lineTo(vertices[0].x, vertices[0].y)
    lineTo(vertices[0].x, vertices[0].y + height)
    lineTo(vertices[1].x, vertices[1].y + height)
    close()
  }
  drawPath(
    rightFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFAAD8FA).copy(alpha = 0.94f), Color.Transparent),
      startY = vertices[1].y,
      endY = vertices[1].y + height
    )
  )

  val topCap = HexMath.createRoundedHexagonPath(center, radius, 0.16f)
  drawPath(
    topCap,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFFF3FAFF), Color(0xFFD5EEFF), Color(0xFF9ACDF7)),
      start = Offset(center.x - radius, center.y - radius),
      end = Offset(center.x + radius, center.y + radius)
    )
  )
  drawPath(topCap, color = Color.White.copy(alpha = 0.94f), style = Stroke(width = 2.2f))
  val innerSocket = HexMath.createRoundedHexagonPath(center, radius * 0.72f, 0.16f)
  drawPath(innerSocket, color = Color(0xFF2874C2).copy(alpha = 0.18f), style = Stroke(width = 1.6f))
}

/**
 * Sleek Rounded Bottom Control Dock (Undo, Shuffle, Hint, Hammer, Pinwheel)
 * with crisp custom vector icons and high-contrast badge counts.
 */
@Composable
fun BoostersBar(
  activeBooster: BoosterType?,
  boosterCounts: Map<BoosterType, Int>,
  canUndo: Boolean,
  coins: Int,
  onBoosterClick: (BoosterType) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 4.dp)
      .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF00B0FF).copy(alpha = 0.45f))
      .background(
        brush = Brush.verticalGradient(
          listOf(Color(0xDD0E2F5C), Color(0xEE071936))
        ),
        shape = RoundedCornerShape(26.dp)
      )
      .border(1.8.dp, Color(0xFF64D8FF).copy(alpha = 0.85f), RoundedCornerShape(26.dp))
      .padding(horizontal = 8.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      BoosterButton(
        type = BoosterType.UNDO,
        count = boosterCounts[BoosterType.UNDO] ?: 0,
        isActive = false,
        isEnabled = canUndo,
        cost = BoosterType.UNDO.cost,
        onClick = { onBoosterClick(BoosterType.UNDO) }
      )

      BoosterButton(
        type = BoosterType.SHUFFLE,
        count = boosterCounts[BoosterType.SHUFFLE] ?: 0,
        isActive = false,
        cost = BoosterType.SHUFFLE.cost,
        onClick = { onBoosterClick(BoosterType.SHUFFLE) }
      )

      BoosterButton(
        type = BoosterType.HINT,
        count = boosterCounts[BoosterType.HINT] ?: 0,
        isActive = false,
        cost = BoosterType.HINT.cost,
        onClick = { onBoosterClick(BoosterType.HINT) }
      )

      BoosterButton(
        type = BoosterType.HAMMER,
        count = boosterCounts[BoosterType.HAMMER] ?: 0,
        isActive = activeBooster == BoosterType.HAMMER,
        cost = BoosterType.HAMMER.cost,
        onClick = { onBoosterClick(BoosterType.HAMMER) }
      )

      BoosterButton(
        type = BoosterType.PINWHEEL,
        count = boosterCounts[BoosterType.PINWHEEL] ?: 0,
        isActive = false,
        cost = BoosterType.PINWHEEL.cost,
        onClick = { onBoosterClick(BoosterType.PINWHEEL) }
      )
    }
  }
}

@Composable
fun BoosterButton(
  type: BoosterType,
  count: Int,
  isActive: Boolean,
  isEnabled: Boolean = true,
  cost: Int,
  onClick: () -> Unit
) {
  val lipColor = when {
    !isEnabled -> Color(0xFF37474F)
    isActive -> Color(0xFF9B001D)
    else -> Color(0xFF004C99)
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .testTag("booster_${type.name.lowercase()}")
      .minimumInteractiveComponentSize()
      .clickable(
        enabled = isEnabled,
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = false, radius = 30.dp)
      ) { onClick() }
      .padding(horizontal = 2.dp)
  ) {
    // Tactile 3D beveled booster cap
    Box(
      modifier = Modifier
        .size(width = 50.dp, height = 52.dp)
        .shadow(
          elevation = if (isActive) 12.dp else 6.dp,
          shape = RoundedCornerShape(16.dp)
        )
        .clip(RoundedCornerShape(16.dp))
        .background(lipColor, RoundedCornerShape(16.dp))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .background(
            brush = when {
              !isEnabled -> Brush.verticalGradient(listOf(Color(0xFF78909C), Color(0xFF546E7A)))
              isActive -> Brush.verticalGradient(listOf(Color(0xFFFF6E6E), Color(0xFFE5193B)))
              else -> Brush.verticalGradient(listOf(Color(0xFF42C5FF), Color(0xFF0077E6)))
            },
            shape = RoundedCornerShape(16.dp)
          )
          .border(
            width = if (isActive) 2.4.dp else 1.5.dp,
            color = if (isActive) Color(0xFFFFEE58) else Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(16.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        // Top specular glass highlight
        Box(
          modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(0.86f)
            .padding(top = 2.5.dp)
            .height(16.dp)
            .background(
              brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = if (isEnabled) 0.40f else 0.14f), Color.Transparent)
              ),
              shape = RoundedCornerShape(12.dp)
            )
        )
        when (type) {
          BoosterType.UNDO -> VectorUndoIcon(modifier = Modifier.size(26.dp))
          BoosterType.SHUFFLE -> VectorShuffleIcon(modifier = Modifier.size(28.dp))
          BoosterType.HINT -> VectorHintIcon(modifier = Modifier.size(26.dp))
          BoosterType.HAMMER -> VectorHammerIcon(modifier = Modifier.size(28.dp))
          BoosterType.PINWHEEL -> VectorPinwheelIcon(modifier = Modifier.size(28.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // High-contrast count / cost pill badge
    Box(
      modifier = Modifier
        .shadow(3.dp, RoundedCornerShape(9.dp))
        .background(
          brush = when {
            !isEnabled -> Brush.verticalGradient(listOf(Color(0xFF78909C), Color(0xFF546E7A)))
            count > 0 -> Brush.verticalGradient(listOf(Color(0xFF2EFF8B), Color(0xFF00A152)))
            else -> Brush.verticalGradient(listOf(Color(0xFFFFCA28), Color(0xFFFF6F00)))
          },
          shape = RoundedCornerShape(9.dp)
        )
        .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(9.dp))
        .padding(horizontal = 8.dp, vertical = 1.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (count > 0) "$count" else "$cost",
        color = Color.White,
        fontFamily = RubikFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp
      )
    }
  }
}

// --- High-Resolution Vector Canvas Icons (No Low-Res Emojis!) ---

@Composable
fun VectorCoinIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawVectorCoin(center, size.minDimension * 0.48f)
  }
}

private fun DrawScope.drawVectorCoin(center: Offset, radius: Float) {
  drawCircle(
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFFFFF176), Color(0xFFFFCA28), Color(0xFFFF8F00)),
      start = Offset(center.x - radius, center.y - radius),
      end = Offset(center.x + radius, center.y + radius)
    ),
    radius = radius,
    center = center
  )
  drawCircle(color = Color(0xFFE65100).copy(alpha = 0.65f), radius = radius, center = center, style = Stroke(width = 1.8f))
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFCA28)),
      center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
      radius = radius * 0.8f
    ),
    radius = radius * 0.72f,
    center = center
  )
  val hex = HexMath.createRoundedHexagonPath(center, radius * 0.42f, 0.18f)
  drawPath(hex, color = Color(0xFFFFB300))
  drawPath(hex, color = Color(0xFFFFFDE7), style = Stroke(width = 1.4f))
}

private fun DrawScope.drawFrostedHexBadgeFrame(center: Offset, radius: Float) {
  val shadow = HexMath.createRoundedHexagonPath(Offset(center.x, center.y + 3f), radius * 1.02f, 0.20f)
  drawPath(shadow, color = Color(0xFF01579B).copy(alpha = 0.22f))

  val outer = HexMath.createRoundedHexagonPath(center, radius, 0.20f)
  drawPath(
    outer,
    brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3F4FF)))
  )
  drawPath(outer, color = Color(0xFF90CAF9), style = Stroke(width = 2.0f))
}

private fun DrawScope.drawRainbowHexagonCore(center: Offset, radius: Float) {
  val wedgeColors = listOf(
    Color(0xFFFF5252), // Red-Orange
    Color(0xFFFF4081), // Pink
    Color(0xFFAB47BC), // Purple
    Color(0xFF29B6F6), // Blue
    Color(0xFF00E676), // Green
    Color(0xFFFFD600)  // Yellow
  )
  val vertices = (0 until 6).map { i ->
    val rad = Math.toRadians(60.0 * i)
    Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
  }
  for (i in 0 until 6) {
    val next = (i + 1) % 6
    val wedge = Path().apply {
      moveTo(center.x, center.y)
      lineTo(vertices[i].x, vertices[i].y)
      lineTo(vertices[next].x, vertices[next].y)
      close()
    }
    drawPath(wedge, color = wedgeColors[i])
  }
  val border = HexMath.createRoundedHexagonPath(center, radius, 0.16f)
  drawPath(border, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 1.8f))
}

private fun DrawScope.drawMiniRobotIcon(center: Offset, r: Float) {
  drawRoundRect(
    color = Color(0xFF1E88E5),
    topLeft = Offset(center.x - r * 0.72f, center.y - r * 0.60f),
    size = Size(r * 1.44f, r * 1.05f),
    cornerRadius = CornerRadius(r * 0.3f, r * 0.3f)
  )
  drawRoundRect(
    color = Color(0xFF004D61),
    topLeft = Offset(center.x - r * 0.50f, center.y - r * 0.40f),
    size = Size(r * 1.0f, r * 0.56f),
    cornerRadius = CornerRadius(r * 0.16f, r * 0.16f)
  )
  drawCircle(Color(0xFFFFAB00), radius = r * 0.14f, center = Offset(center.x - r * 0.22f, center.y - r * 0.12f))
  drawCircle(Color(0xFFFFAB00), radius = r * 0.14f, center = Offset(center.x + r * 0.22f, center.y - r * 0.12f))
  drawCircle(Color(0xFFFF1744), radius = r * 0.16f, center = Offset(center.x, center.y - r * 0.80f))
}

private fun DrawScope.drawMiniStarIcon(center: Offset, r: Float) {
  val path = Path()
  for (i in 0 until 10) {
    val rad = if (i % 2 == 0) r else r * 0.45f
    val a = Math.toRadians((i * 36 - 90).toDouble())
    val x = center.x + rad * cos(a).toFloat()
    val y = center.y + rad * sin(a).toFloat()
    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
  }
  path.close()
  drawPath(path, brush = Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFF9800))))
  drawPath(path, color = Color(0xFFE65100), style = Stroke(width = 1.8f))
}

private fun DrawScope.drawMiniHoneyIcon(center: Offset, r: Float) {
  val hex = HexMath.createRoundedHexagonPath(center, r, 0.16f)
  drawPath(hex, brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000))))
  drawPath(hex, color = Color(0xFFE65100), style = Stroke(width = 1.8f))
}

private fun DrawScope.drawMiniBoltIcon(center: Offset, r: Float) {
  val path = Path().apply {
    moveTo(center.x + r * 0.1f, center.y - r)
    lineTo(center.x - r * 0.55f, center.y + r * 0.1f)
    lineTo(center.x, center.y + r * 0.1f)
    lineTo(center.x - r * 0.15f, center.y + r)
    lineTo(center.x + r * 0.55f, center.y - r * 0.1f)
    lineTo(center.x, center.y - r * 0.1f)
    close()
  }
  drawPath(path, color = Color(0xFF00E5FF))
  drawPath(path, color = Color(0xFF01579B), style = Stroke(width = 1.5f))
}

@Composable
private fun VectorTargetIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.44f
    drawCircle(Color.White, radius = r, center = c, style = Stroke(width = 3.2f))
    drawCircle(Color(0xFFFFEB3B), radius = r * 0.56f, center = c, style = Stroke(width = 2.8f))
    drawCircle(Color.White, radius = r * 0.22f, center = c)
  }
}

@Composable
private fun VectorGiftIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(w * 0.16f, h * 0.36f),
      size = Size(w * 0.68f, h * 0.50f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
      color = Color(0xFFFFEB3B),
      topLeft = Offset(w * 0.12f, h * 0.24f),
      size = Size(w * 0.76f, h * 0.18f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    drawRect(
      color = Color(0xFFFF5252),
      topLeft = Offset(w * 0.44f, h * 0.24f),
      size = Size(w * 0.12f, h * 0.62f)
    )
    drawCircle(Color(0xFFFFEB3B), radius = w * 0.10f, center = Offset(w * 0.38f, h * 0.18f), style = Stroke(width = 2.6f))
    drawCircle(Color(0xFFFFEB3B), radius = w * 0.10f, center = Offset(w * 0.62f, h * 0.18f), style = Stroke(width = 2.6f))
  }
}

@Composable
private fun VectorUndoIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val arrowPath = Path().apply {
      moveTo(w * 0.24f, h * 0.40f)
      lineTo(w * 0.62f, h * 0.40f)
      cubicTo(w * 0.85f, h * 0.40f, w * 0.85f, h * 0.78f, w * 0.60f, h * 0.78f)
      lineTo(w * 0.34f, h * 0.78f)
    }
    drawPath(arrowPath, color = Color.White, style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val head = Path().apply {
      moveTo(w * 0.38f, h * 0.22f)
      lineTo(w * 0.16f, h * 0.40f)
      lineTo(w * 0.38f, h * 0.58f)
      close()
    }
    drawPath(head, color = Color(0xFFFFEB3B))
  }
}

@Composable
private fun VectorShuffleIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.32f
    val hex = HexMath.createRoundedHexagonPath(Offset(c.x - r * 0.18f, c.y + r * 0.15f), r * 0.85f, 0.18f)
    drawPath(hex, color = Color(0xFF90CAF9))
    drawPath(hex, color = Color.White, style = Stroke(width = 2.2f))

    drawArc(
      color = Color(0xFFFFD600),
      startAngle = -140f,
      sweepAngle = 210f,
      useCenter = false,
      topLeft = Offset(c.x - r, c.y - r),
      size = Size(r * 2f, r * 2f),
      style = Stroke(width = 4.8f, cap = StrokeCap.Round)
    )
    val tip = Path().apply {
      moveTo(c.x + r * 0.95f, c.y - r * 0.25f)
      lineTo(c.x + r * 1.25f, c.y + r * 0.20f)
      lineTo(c.x + r * 0.60f, c.y + r * 0.15f)
      close()
    }
    drawPath(tip, color = Color(0xFFFFD600))
  }
}

@Composable
private fun VectorHintIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val c = Offset(size.width / 2f, size.height * 0.42f)
    val r = size.minDimension * 0.28f
    drawCircle(
      brush = Brush.radialGradient(listOf(Color(0xFFFFFF8D), Color(0xFFFFD600))),
      radius = r,
      center = c
    )
    drawCircle(color = Color.White, radius = r, center = c, style = Stroke(width = 2.0f))
    drawRoundRect(
      color = Color(0xFFECEFF1),
      topLeft = Offset(c.x - r * 0.45f, c.y + r * 0.82f),
      size = Size(r * 0.90f, r * 0.48f),
      cornerRadius = CornerRadius(4f, 4f)
    )
    for (angle in listOf(-135f, -90f, -45f)) {
      val rad = Math.toRadians(angle.toDouble())
      drawLine(
        color = Color(0xFFFFF59D),
        start = Offset(c.x + (r * 1.25f) * cos(rad).toFloat(), c.y + (r * 1.25f) * sin(rad).toFloat()),
        end = Offset(c.x + (r * 1.65f) * cos(rad).toFloat(), c.y + (r * 1.65f) * sin(rad).toFloat()),
        strokeWidth = 2.6f,
        cap = StrokeCap.Round
      )
    }
  }
}

@Composable
private fun VectorHammerIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val c = Offset(size.width / 2f, size.height / 2f)
    rotate(-35f, pivot = c) {
      drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFE65100))),
        topLeft = Offset(c.x - 4.5f, c.y - 2f),
        size = Size(9f, size.height * 0.44f),
        cornerRadius = CornerRadius(4f, 4f)
      )
      drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFD84315))),
        topLeft = Offset(c.x - size.width * 0.28f, c.y - size.height * 0.26f),
        size = Size(size.width * 0.56f, size.height * 0.28f),
        cornerRadius = CornerRadius(6f, 6f)
      )
      drawRoundRect(
        color = Color(0xFFFFE082),
        topLeft = Offset(c.x - size.width * 0.32f, c.y - size.height * 0.29f),
        size = Size(size.width * 0.14f, size.height * 0.34f),
        cornerRadius = CornerRadius(4f, 4f)
      )
      drawRoundRect(
        color = Color(0xFFFFE082),
        topLeft = Offset(c.x + size.width * 0.18f, c.y - size.height * 0.29f),
        size = Size(size.width * 0.14f, size.height * 0.34f),
        cornerRadius = CornerRadius(4f, 4f)
      )
    }
  }
}

@Composable
private fun VectorPinwheelIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.42f
    val bladeColors = listOf(
      Color(0xFFFF4081),
      Color(0xFFFFD600),
      Color(0xFF00E676),
      Color(0xFF29B6F6)
    )
    for (i in 0 until 4) {
      val baseAngle = i * 90f
      rotate(baseAngle, pivot = c) {
        val blade = Path().apply {
          moveTo(c.x, c.y)
          quadraticTo(c.x + r * 0.35f, c.y - r * 0.95f, c.x + r, c.y - r * 0.35f)
          quadraticTo(c.x + r * 0.65f, c.y, c.x, c.y)
          close()
        }
        drawPath(blade, color = bladeColors[i])
        drawPath(blade, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.4f))
      }
    }
    drawCircle(Color.White, radius = r * 0.18f, center = c)
    drawCircle(Color(0xFF1976D2), radius = r * 0.10f, center = c)
  }
}
