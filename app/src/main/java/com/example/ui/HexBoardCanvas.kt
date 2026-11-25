package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.game.GameViewModel
import com.example.game.HexMath
import com.example.model.ActiveSlide
import com.example.model.BoosterType
import com.example.model.CellObstacle
import com.example.model.FloatingText
import com.example.model.GameMode
import com.example.model.HexCell
import com.example.model.HexCoord
import com.example.model.HexTile
import com.example.model.HintRecommendation
import com.example.model.Particle
import com.example.model.ParticleShape
import com.example.model.StagedStack
import com.example.model.TileColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexBoardCanvas(
  viewModel: GameViewModel,
  boardCells: Map<HexCoord, HexCell>,
  activeSlides: List<ActiveSlide>,
  particles: List<Particle>,
  floatingTexts: List<FloatingText>,
  activeBooster: BoosterType?,
  activeHint: HintRecommendation?,
  hoveredCoord: HexCoord?,
  draggedStack: StagedStack?,
  dragPosition: Offset?,
  snapStrength: Float = 0f,
  isLowPerformanceMode: Boolean,
  isColorblindMode: Boolean,
  screenShake: Offset,
  hammerStrikePos: Offset?,
  gameMode: GameMode = GameMode.CLASSIC,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val dragOffsetPx = remember(density) { with(density) { 88.dp.toPx() } }
  val displayTypeface = remember(context) {
    ResourcesCompat.getFont(context, R.font.fredoka) ?: Typeface.DEFAULT_BOLD
  }
  val comboTypeface = remember(context) {
    ResourcesCompat.getFont(context, R.font.titan_one) ?: displayTypeface
  }

  val animatedDragPos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
  var trackedDragId by remember { mutableLongStateOf(0L) }
  val sharedParticlePath = remember { Path() }
  val sortedCells = remember(boardCells) {
    boardCells.entries.sortedWith(compareBy({ it.key.r }, { it.key.q }))
  }

  LaunchedEffect(draggedStack?.id) {
    val target = dragPosition
    if (draggedStack != null && target != null) {
      trackedDragId = System.identityHashCode(draggedStack.id).toLong()
      animatedDragPos.snapTo(target)
    }
  }

  LaunchedEffect(dragPosition, draggedStack?.id) {
    val target = dragPosition
    if (draggedStack != null && target != null) {
      if (snapStrength > 0.05f) {
        animatedDragPos.animateTo(
          targetValue = target,
          animationSpec = spring(dampingRatio = 0.82f, stiffness = 1750f)
        )
      } else {
        animatedDragPos.snapTo(target)
      }
    }
  }

  var animTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var lastTickTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

  // 60 FPS animation frame loop for smooth particle & slide rendering
  LaunchedEffect(isLowPerformanceMode) {
    while (true) {
      withInfiniteAnimationFrameMillis { _ ->
        val now = System.currentTimeMillis()
        val delta = ((now - lastTickTime).coerceAtMost(50L)) / 1000f
        lastTickTime = now
        animTimeMs = now
        viewModel.tickParticles(delta)
      }
    }
  }

  val bgDrawable = when (gameMode) {
    GameMode.CLASSIC -> R.drawable.img_bg_classic
    GameMode.TIME_ATTACK -> R.drawable.img_bg_time_attack
    GameMode.ENDLESS -> R.drawable.img_bg_endless
  }

  val matchingNeighborCoords = remember(hoveredCoord, draggedStack, boardCells) {
    val dragTop = draggedStack?.topTile
    if (hoveredCoord == null || dragTop == null) {
      emptySet()
    } else {
      hoveredCoord.getNeighbors().filter { nCoord ->
        val nTop = boardCells[nCoord]?.topTile
        nTop != null && nTop.matchesTile(dragTop)
      }.toSet()
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Image(
      painter = painterResource(id = bgDrawable),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(activeBooster, boardCells.keys) {
          detectTapGestures { tapOffset ->
            val cellWidth = size.width.toFloat()
            val cellHeight = size.height.toFloat()
            val center = Offset(cellWidth / 2f, cellHeight * 0.42f)
            val radius = calculateBoardRadius(cellWidth, cellHeight, boardCells.keys)

            val adjustedTap = Offset(tapOffset.x - screenShake.x, tapOffset.y - screenShake.y)
            val tappedCoord = HexMath.pixelToHex(adjustedTap, radius, center)
            if (boardCells.containsKey(tappedCoord)) {
              viewModel.onCellTapped(tappedCoord)
            }
          }
        }
    ) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val boardCenter = Offset(canvasWidth / 2f, canvasHeight * 0.42f)
      val hexRadius = calculateBoardRadius(canvasWidth, canvasHeight, boardCells.keys)

      viewModel.updateBoardGeometry(boardCenter, hexRadius, dragOffsetPx)

      // Atmospheric depth scrim & arena center spotlight so board tiles pop crisply
      val spotlightColor = when (gameMode) {
        GameMode.CLASSIC -> Color(0xFF00B0FF).copy(alpha = 0.22f)
        GameMode.TIME_ATTACK -> Color(0xFFFF4081).copy(alpha = 0.20f)
        GameMode.ENDLESS -> Color(0xFF00E5FF).copy(alpha = 0.18f)
      }
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF041028).copy(alpha = 0.34f),
            Color.Transparent,
            Color(0xFF041028).copy(alpha = 0.42f)
          )
        )
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(spotlightColor, Color.Transparent),
          center = boardCenter,
          radius = canvasWidth * 0.72f
        ),
        radius = canvasWidth * 0.72f,
        center = boardCenter
      )

      // Ambient drifting starlight motes behind pillars
      if (!isLowPerformanceMode) {
        val tSec = (animTimeMs % 60000L) / 1000f
        for (i in 0 until 18) {
          val sx = ((i * 97 + 23) % 100) / 100f * canvasWidth + sin(tSec * 0.7f + i) * 16f
          val sy = ((i * 151 + 47) % 100) / 100f * canvasHeight + cos(tSec * 0.55f + i) * 20f
          val tw = 0.35f + 0.65f * ((sin(tSec * 2.1f + i * 1.4f) + 1f) * 0.5f)
          val r = 2.5f + (i % 3) * 1.5f
          drawCircle(
            color = Color.White.copy(alpha = 0.42f * tw),
            radius = r,
            center = Offset(sx, sy)
          )
        }
      }

      // Apply Screen Shake offset
      translate(screenShake.x, screenShake.y) {
        // 2. Draw 3D Extruded Hexagonal Pillars with mode-matched crystal bevels
        for ((coord, _) in sortedCells) {
          val center = HexMath.hexToPixel(coord, hexRadius, boardCenter)
          drawFaceted3DPillar(center, hexRadius, gameMode, isLowPerformanceMode)
        }

        // 3. Draw Board Cells content (Stacks, Obstacles, & Hover Match Connections)
        for ((coord, cell) in sortedCells) {
          val center = HexMath.hexToPixel(coord, hexRadius, boardCenter)
          val isHovered = coord == hoveredCoord
          val isHinted = activeHint?.targetCoord == coord
          val isMatchNeighbor = matchingNeighborCoords.contains(coord)
          drawCellContent(
            center = center,
            radius = hexRadius,
            cell = cell,
            timeMs = animTimeMs,
            activeBooster = activeBooster,
            isHovered = isHovered,
            snapStrength = if (isHovered) snapStrength else 0f,
            isHinted = isHinted,
            isMatchNeighbor = isMatchNeighbor,
            isColorblindMode = isColorblindMode,
            typeface = displayTypeface
          )
        }

        // Draw electric merge preview links between hovered cell & matching neighbors
        if (hoveredCoord != null && matchingNeighborCoords.isNotEmpty() && snapStrength > 0.25f) {
          val hCenter = HexMath.hexToPixel(hoveredCoord, hexRadius, boardCenter)
          val linkPulse = 0.6f + 0.4f * sin(animTimeMs * 0.015f)
          for (nCoord in matchingNeighborCoords) {
            val nCenter = HexMath.hexToPixel(nCoord, hexRadius, boardCenter)
            drawLine(
              color = Color(0xFFFFD600).copy(alpha = 0.65f * linkPulse * snapStrength),
              start = hCenter,
              end = nCenter,
              strokeWidth = 7f,
              cap = StrokeCap.Round
            )
            drawLine(
              color = Color.White.copy(alpha = 0.9f * linkPulse * snapStrength),
              start = hCenter,
              end = nCenter,
              strokeWidth = 2.6f,
              cap = StrokeCap.Round
            )
          }
        }

        // 4. Draw Active Fanning Card Slides
        for (slide in activeSlides) {
          drawFanningSlide(slide, hexRadius, animTimeMs, isColorblindMode, displayTypeface)
        }

        // 5. Draw Active Drag Preview (Elevated above finger & magnetically snapped to target cell)
        if (draggedStack != null && dragPosition != null) {
          val drawPos = if (trackedDragId != 0L) animatedDragPos.value else dragPosition
          drawDraggedStack(drawPos, hexRadius, draggedStack, snapStrength, isColorblindMode, displayTypeface, animTimeMs)
        }

        // 6. Draw 3D Hammer Strike Visual
        if (hammerStrikePos != null) {
          drawHammerStrikeVFX(hammerStrikePos, hexRadius, animTimeMs)
        }

        // 7. Draw Particles (Zero-allocation reusable Path for lag-free 60 FPS sparks & fireworks)
        if (!isLowPerformanceMode && particles.isNotEmpty()) {
          for (p in particles) {
            drawParticle(p, animTimeMs, sharedParticlePath)
          }
        }

        // 8. Draw Crisp Floating Texts
        for (ft in floatingTexts) {
          drawFloatingText(ft, animTimeMs, comboTypeface)
        }
      }
    }
  }
}

/**
 * Renders true 3D extruded hexagonal pillar with 3 crisp faceted lighting walls and a rounded-hex cap.
 */
private fun DrawScope.drawFaceted3DPillar(
  center: Offset,
  radius: Float,
  gameMode: GameMode,
  isLowPerformanceMode: Boolean
) {
  val pillarRadius = radius * 0.94f
  val pillarHeight = radius * 4.6f
  val v = getHexagonVertices(center, pillarRadius)

  val (leftTop, leftMid, centerTop, centerMid, rightTop, rightMid, capColors, rimColor, socketColor) = when (gameMode) {
    GameMode.TIME_ATTACK -> PillarTheme(
      leftTop = Color(0xFF28124D),
      leftMid = Color(0xFF180A33),
      centerTop = Color(0xFF3E1C72),
      centerMid = Color(0xFF240E4B),
      rightTop = Color(0xFF5A2A9C),
      rightMid = Color(0xFF331466),
      capColors = listOf(Color(0xFF472580), Color(0xFF30165C), Color(0xFF220E45)),
      rimColor = Color(0xFFFF4081),
      socketColor = Color(0xFF1A0938)
    )
    GameMode.ENDLESS -> PillarTheme(
      leftTop = Color(0xFF12305A),
      leftMid = Color(0xFF0A1B38),
      centerTop = Color(0xFF1C4880),
      centerMid = Color(0xFF112C54),
      rightTop = Color(0xFF2966A8),
      rightMid = Color(0xFF173A6B),
      capColors = listOf(Color(0xFF285C9B), Color(0xFF193C6E), Color(0xFF122C52)),
      rimColor = Color(0xFF00E5FF),
      socketColor = Color(0xFF0D203D)
    )
    GameMode.CLASSIC -> PillarTheme(
      leftTop = Color(0xFF428CD1),
      leftMid = Color(0xFF2364A6),
      centerTop = Color(0xFF70B6EF),
      centerMid = Color(0xFF3C88CE),
      rightTop = Color(0xFFAAD8FA),
      rightMid = Color(0xFF5FA7E4),
      capColors = listOf(Color(0xFFFFFFFF), Color(0xFFE0F3FF), Color(0xFFB7DFFB)),
      rimColor = Color(0xFF64B5F6),
      socketColor = Color(0xFFCDE9FC)
    )
  }

  val shadowCap = HexMath.createRoundedHexagonPath(Offset(center.x, center.y + 6f), pillarRadius * 1.03f, 0.16f)
  drawPath(shadowCap, color = Color(0xFF04142E).copy(alpha = 0.24f))

  val leftFacet = Path().apply {
    moveTo(v[3].x, v[3].y)
    lineTo(v[2].x, v[2].y)
    lineTo(v[2].x, v[2].y + pillarHeight)
    lineTo(v[3].x, v[3].y + pillarHeight)
    close()
  }
  drawPath(
    leftFacet,
    brush = Brush.verticalGradient(
      colors = listOf(leftTop.copy(alpha = 0.94f), leftMid.copy(alpha = 0.55f), Color.Transparent),
      startY = v[3].y,
      endY = v[3].y + pillarHeight
    )
  )

  val centerFacet = Path().apply {
    moveTo(v[2].x, v[2].y)
    lineTo(v[1].x, v[1].y)
    lineTo(v[1].x, v[1].y + pillarHeight)
    lineTo(v[2].x, v[2].y + pillarHeight)
    close()
  }
  drawPath(
    centerFacet,
    brush = Brush.verticalGradient(
      colors = listOf(centerTop.copy(alpha = 0.96f), centerMid.copy(alpha = 0.58f), Color.Transparent),
      startY = v[2].y,
      endY = v[2].y + pillarHeight
    )
  )

  val rightFacet = Path().apply {
    moveTo(v[1].x, v[1].y)
    lineTo(v[0].x, v[0].y)
    lineTo(v[0].x, v[0].y + pillarHeight)
    lineTo(v[1].x, v[1].y + pillarHeight)
    close()
  }
  drawPath(
    rightFacet,
    brush = Brush.verticalGradient(
      colors = listOf(rightTop.copy(alpha = 0.98f), rightMid.copy(alpha = 0.60f), Color.Transparent),
      startY = v[1].y,
      endY = v[1].y + pillarHeight
    )
  )

  if (!isLowPerformanceMode) {
    drawLine(
      color = Color.White.copy(alpha = 0.55f),
      start = v[2],
      end = Offset(v[2].x, v[2].y + pillarHeight * 0.72f),
      strokeWidth = 1.6f
    )
    drawLine(
      color = rimColor.copy(alpha = 0.80f),
      start = v[1],
      end = Offset(v[1].x, v[1].y + pillarHeight * 0.72f),
      strokeWidth = 2.0f
    )
  }

  val topHexPath = HexMath.createRoundedHexagonPath(center, pillarRadius, 0.16f)
  drawPath(
    path = topHexPath,
    brush = Brush.linearGradient(
      colors = capColors,
      start = Offset(center.x - pillarRadius * 0.6f, center.y - pillarRadius),
      end = Offset(center.x + pillarRadius * 0.6f, center.y + pillarRadius)
    )
  )
  drawPath(path = topHexPath, color = rimColor.copy(alpha = 0.85f), style = Stroke(width = 2.2f))

  // Recessed inner hex socket rim
  val innerHex = HexMath.createRoundedHexagonPath(center, pillarRadius * 0.86f, 0.16f)
  drawPath(path = innerHex, color = socketColor.copy(alpha = 0.55f))
  drawPath(path = innerHex, color = Color.White.copy(alpha = 0.50f), style = Stroke(width = 1.2f))
}

private data class PillarTheme(
  val leftTop: Color,
  val leftMid: Color,
  val centerTop: Color,
  val centerMid: Color,
  val rightTop: Color,
  val rightMid: Color,
  val capColors: List<Color>,
  val rimColor: Color,
  val socketColor: Color
)

/**
 * Dynamically calculates the optimal board radius so hexagons scale crisply to screen width.
 */
fun calculateBoardRadius(canvasWidth: Float, canvasHeight: Float, coords: Set<HexCoord>): Float {
  if (coords.isEmpty()) return (canvasWidth / 7.2f).coerceIn(52f, 135f)
  val minQ = coords.minOf { it.q }
  val maxQ = coords.maxOf { it.q }
  val minR = coords.minOf { it.r + it.q / 2f }
  val maxR = coords.maxOf { it.r + it.q / 2f }

  val qSpan = (maxQ - minQ).coerceAtLeast(3).toFloat()
  val rSpan = (maxR - minR).coerceAtLeast(3f)

  val availW = canvasWidth * 0.92f
  val availH = canvasHeight * 0.48f

  val byW = availW / (qSpan * 1.5f + 2f)
  val byH = availH / (rSpan * 1.732f + 2f)
  return minOf(byW, byH).coerceIn(52f, 135f)
}

/**
 * Returns the 6 vertices of a flat-topped hexagon in clockwise order:
 * 0: Right, 1: Lower-Right, 2: Lower-Left, 3: Left, 4: Upper-Left, 5: Upper-Right
 */
private fun getHexagonVertices(center: Offset, radius: Float): List<Offset> {
  return (0 until 6).map { i ->
    val angleRad = Math.toRadians((60.0 * i))
    val x = center.x + radius * cos(angleRad).toFloat()
    val y = center.y + radius * sin(angleRad).toFloat()
    Offset(x, y)
  }
}

private fun DrawScope.drawCellContent(
  center: Offset,
  radius: Float,
  cell: HexCell,
  timeMs: Long,
  activeBooster: BoosterType?,
  isHovered: Boolean,
  snapStrength: Float,
  isHinted: Boolean,
  isMatchNeighbor: Boolean,
  isColorblindMode: Boolean,
  typeface: Typeface
) {
  when (val obs = cell.obstacle) {
    is CellObstacle.CoinReward -> drawCoinObstacle(center, radius, obs.coins, timeMs, typeface)
    is CellObstacle.RobotToy -> drawRobotObstacle(center, radius, obs.remainingHits, timeMs)
    is CellObstacle.SleepyGhost -> drawGhostObstacle(center, radius, timeMs, typeface)
    is CellObstacle.StarToken -> drawStarObstacle(center, radius, timeMs)
    is CellObstacle.Honeycomb -> drawHoneycombObstacle(center, radius, obs.remainingHits, typeface)
    is CellObstacle.Cake -> drawCakeObstacle(center, radius, obs.remainingHits)
    is CellObstacle.Cloud -> drawCloudObstacle(center, radius, timeMs)
    is CellObstacle.Lightning -> drawLightningObstacle(center, radius, timeMs)
    CellObstacle.None -> {}
  }

  if (cell.stack.isNotEmpty()) {
    draw3DLayeredStack(center, radius, cell.stack, cell.popScale, isColorblindMode, typeface, timeMs)
  }

  if (isMatchNeighbor) {
    val mPulse = 1f + 0.06f * sin(timeMs * 0.014f)
    val matchHex = HexMath.createRoundedHexagonPath(center, radius * 1.07f * mPulse, 0.16f)
    drawPath(matchHex, color = Color(0xFFFFD600).copy(alpha = 0.85f), style = Stroke(width = 4.2f))
  }

  if (isHovered && cell.isEmpty) {
    val pulse = 1f + 0.04f * sin(timeMs * 0.012f) * snapStrength
    val hoverHex = HexMath.createRoundedHexagonPath(center, radius * 1.04f * pulse, 0.16f)
    drawPath(hoverHex, color = Color(0xFF00E676).copy(alpha = 0.28f + 0.22f * snapStrength))
    drawPath(hoverHex, color = Color(0xFF00E676), style = Stroke(width = 3.8f + 2.5f * snapStrength))
    if (snapStrength > 0.4f) {
      val innerSnapHex = HexMath.createRoundedHexagonPath(center, radius * 0.90f, 0.16f)
      drawPath(innerSnapHex, color = Color.White.copy(alpha = 0.85f * snapStrength), style = Stroke(width = 2.6f))
    }
  }

  if (isHinted) {
    val pulse = 1f + 0.08f * sin(timeMs * 0.008f)
    val hintHex = HexMath.createRoundedHexagonPath(center, radius * 1.06f * pulse, 0.16f)
    drawPath(hintHex, color = Color(0xFFFFD600).copy(alpha = 0.32f))
    drawPath(hintHex, color = Color(0xFFFFD600), style = Stroke(width = 4.5f))
  }

  if (activeBooster == BoosterType.HAMMER && !cell.isEmpty) {
    drawHammerReticle(center, radius, timeMs)
  }
}

private fun DrawScope.drawDraggedStack(
  floatPos: Offset,
  radius: Float,
  stack: StagedStack,
  snapStrength: Float,
  isColorblindMode: Boolean,
  typeface: Typeface,
  timeMs: Long
) {
  val shadowDropY = 24f - snapStrength * 16f
  val shadowPath = HexMath.createRoundedHexagonPath(
    Offset(floatPos.x, floatPos.y + shadowDropY),
    radius * (0.95f - snapStrength * 0.04f),
    0.16f
  )
  drawPath(shadowPath, color = Color(0xFF04142E).copy(alpha = 0.28f + snapStrength * 0.12f))

  val dragScale = 1.07f - snapStrength * 0.05f
  draw3DLayeredStack(
    center = floatPos,
    radius = radius * dragScale,
    stack = stack.tiles,
    popScale = 1.03f,
    isColorblindMode = isColorblindMode,
    typeface = typeface,
    timeMs = timeMs
  )
}

/**
 * Renders a crisp HD 3D stack of rounded-hexagonal tiles with soft drop shadow,
 * vibrant gradients, extruded 3D side walls, and anti-aliased Fredoka typography.
 */
fun DrawScope.draw3DLayeredStack(
  center: Offset,
  radius: Float,
  stack: List<HexTile>,
  popScale: Float,
  isColorblindMode: Boolean,
  typeface: Typeface,
  timeMs: Long = 0L
) {
  if (stack.isEmpty()) return
  val tileRadius = radius * 0.89f * popScale
  var currentY = center.y

  // Soft ambient drop shadow beneath the entire stack on the pillar
  val baseShadowPath = HexMath.createRoundedHexagonPath(
    center = Offset(center.x, center.y + 7f),
    radius = tileRadius * 1.03f,
    cornerFraction = 0.16f
  )
  drawPath(baseShadowPath, color = Color(0xFF04142E).copy(alpha = 0.28f))

  val layerCount = stack.size
  for (i in 0 until layerCount - 1) {
    val tile = stack[i]
    val layerHeight = 8.5f + minOf(tile.count, 5) * 2.5f
    val layerCenter = Offset(center.x, currentY)

    draw3DFacetedHexDisc(
      center = layerCenter,
      radius = tileRadius,
      height = layerHeight,
      color = tile.color,
      splitColor = tile.splitColor,
      isTopLayer = false,
      isColorblindMode = isColorblindMode
    )
    currentY -= layerHeight
  }

  // Topmost tile disc
  val topTile = stack.last()
  val topHeight = 11.5f + minOf(topTile.count, 6) * 2.6f
  val topCenter = Offset(center.x, currentY)

  // Near-clear 8+ golden/neon shimmer halo or subtle glow around top tile
  val isNearClear = topTile.count >= 8
  val haloPulse = if (isNearClear && timeMs != 0L) (0.55f + 0.45f * sin(timeMs * 0.011f)) else 1f
  val glowHex = HexMath.createRoundedHexagonPath(
    center = Offset(topCenter.x, topCenter.y - topHeight),
    radius = tileRadius * (if (isNearClear) 1.09f else 1.05f),
    cornerFraction = 0.16f
  )
  drawPath(
    glowHex,
    color = if (isNearClear) Color(0xFFFFEE58).copy(alpha = 0.70f * haloPulse) else topTile.color.lightColor.copy(alpha = 0.30f),
    style = Stroke(width = if (isNearClear) 5.5f else 4f)
  )

  draw3DFacetedHexDisc(
    center = topCenter,
    radius = tileRadius,
    height = topHeight,
    color = topTile.color,
    splitColor = topTile.splitColor,
    isTopLayer = true,
    isColorblindMode = isColorblindMode
  )

  // Crisp anti-aliased Fredoka Number
  val numberText = topTile.count.toString()
  val numberSize = if (isColorblindMode) tileRadius * 0.62f else tileRadius * 0.76f
  val textCenterY = if (isColorblindMode) {
    topCenter.y - topHeight - tileRadius * 0.08f
  } else {
    topCenter.y - topHeight
  }

  val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = topTile.color.darkSideColor.copy(alpha = 0.92f).toArgb()
    textSize = numberSize
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
    style = Paint.Style.STROKE
    strokeWidth = if (isColorblindMode) 6.5f else 6f
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
  }

  val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = if (topTile.color == TileColor.YELLOW || topTile.color == TileColor.CYAN) {
      Color.White.toArgb()
    } else {
      topTile.color.textColor.toArgb()
    }
    textSize = numberSize
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
    setShadowLayer(5f, 0f, 3f, Color(0xFF002147).copy(alpha = 0.35f).toArgb())
  }

  val fm = fillPaint.fontMetrics
  val baselineY = textCenterY - (fm.ascent + fm.descent) / 2f

  drawContext.canvas.nativeCanvas.drawText(numberText, topCenter.x, baselineY, strokePaint)
  drawContext.canvas.nativeCanvas.drawText(numberText, topCenter.x, baselineY, fillPaint)

  // High-Contrast Accessibility Numerical Symbol Badge in Colorblind Mode
  if (isColorblindMode) {
    val badgeWidth = tileRadius * 1.06f
    val badgeHeight = tileRadius * 0.44f
    val badgeCenterY = topCenter.y - topHeight + (tileRadius * 0.45f)
    val badgeRect = android.graphics.RectF(
      topCenter.x - badgeWidth / 2f,
      badgeCenterY - badgeHeight / 2f,
      topCenter.x + badgeWidth / 2f,
      badgeCenterY + badgeHeight / 2f
    )

    val shadowPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.Black.copy(alpha = 0.55f).toArgb()
      style = Paint.Style.FILL
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
      badgeRect.left, badgeRect.top + 2f, badgeRect.right, badgeRect.bottom + 2f,
      badgeHeight / 2f, badgeHeight / 2f, shadowPillPaint
    )

    val bgPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color(0xFF121212).toArgb()
      style = Paint.Style.FILL
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
      badgeRect, badgeHeight / 2f, badgeHeight / 2f, bgPillPaint
    )

    val borderPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.White.toArgb()
      style = Paint.Style.STROKE
      strokeWidth = 2.5f
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
      badgeRect, badgeHeight / 2f, badgeHeight / 2f, borderPillPaint
    )

    val symPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
      color = Color.White.toArgb()
      textSize = tileRadius * 0.28f
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
      this.typeface = typeface
    }
    val badgeLabel = topTile.color.fullBadgeText
    val symFm = symPaint.fontMetrics
    val symY = badgeCenterY - (symFm.ascent + symFm.descent) / 2f
    drawContext.canvas.nativeCanvas.drawText(badgeLabel, topCenter.x, symY, symPaint)
  }
}

/**
 * Renders an individual crisp HD rounded-hexagonal 3D disc with smooth gradients,
 * 3-facet extruded side walls, optional split-color dual sectors, and specular bevels.
 */
fun DrawScope.draw3DFacetedHexDisc(
  center: Offset,
  radius: Float,
  height: Float,
  color: TileColor,
  splitColor: TileColor? = null,
  isTopLayer: Boolean,
  isColorblindMode: Boolean = false
) {
  val topCenter = Offset(center.x, center.y - height)
  val vTop = getHexagonVertices(topCenter, radius)
  val vBottom = getHexagonVertices(center, radius)

  // Bottom skirt silhouette using rounded hexagon so side corners appear smooth & vector-crisp
  val bottomHexPath = HexMath.createRoundedHexagonPath(center, radius, 0.16f)
  drawPath(bottomHexPath, color = color.darkSideColor)

  // 3D Extruded Facets (Left, Center, Right)
  val fLeft = Path().apply {
    moveTo(vTop[3].x, vTop[3].y)
    lineTo(vTop[2].x, vTop[2].y)
    lineTo(vBottom[2].x, vBottom[2].y)
    lineTo(vBottom[3].x, vBottom[3].y)
    close()
  }
  drawPath(
    fLeft,
    brush = Brush.verticalGradient(
      colors = listOf(color.sideColor, color.darkSideColor),
      startY = vTop[3].y,
      endY = vBottom[3].y
    )
  )

  val cCenterSide = splitColor?.sideColor ?: color.sideColor
  val cCenterDark = splitColor?.darkSideColor ?: color.darkSideColor
  val fCenter = Path().apply {
    moveTo(vTop[2].x, vTop[2].y)
    lineTo(vTop[1].x, vTop[1].y)
    lineTo(vBottom[1].x, vBottom[1].y)
    lineTo(vBottom[2].x, vBottom[2].y)
    close()
  }
  drawPath(
    fCenter,
    brush = Brush.verticalGradient(
      colors = listOf(cCenterSide, cCenterDark),
      startY = vTop[2].y,
      endY = vBottom[2].y
    )
  )

  val cRightLight = splitColor?.lightSideColor ?: color.lightSideColor
  val cRightSide = splitColor?.sideColor ?: color.sideColor
  val fRight = Path().apply {
    moveTo(vTop[1].x, vTop[1].y)
    lineTo(vTop[0].x, vTop[0].y)
    lineTo(vBottom[0].x, vBottom[0].y)
    lineTo(vBottom[1].x, vBottom[1].y)
    close()
  }
  drawPath(
    fRight,
    brush = Brush.verticalGradient(
      colors = listOf(cRightLight, cRightSide),
      startY = vTop[1].y,
      endY = vBottom[1].y
    )
  )

  // Crisp subtle seam lines & AO between stacked wafer discs
  drawPath(
    path = bottomHexPath,
    color = Color.Black.copy(alpha = if (isColorblindMode) 0.55f else 0.26f),
    style = Stroke(width = 1.8f)
  )

  // Top Rounded Hexagon Surface Plate
  val topHexPath = HexMath.createRoundedHexagonPath(topCenter, radius, 0.16f)

  if (splitColor != null) {
    val leftClip = Path().apply {
      moveTo(topCenter.x, topCenter.y - radius * 1.5f)
      lineTo(topCenter.x - radius * 1.8f, topCenter.y - radius * 1.5f)
      lineTo(topCenter.x - radius * 1.8f, topCenter.y + radius * 1.5f)
      lineTo(topCenter.x, topCenter.y + radius * 1.5f)
      close()
    }
    val rightClip = Path().apply {
      moveTo(topCenter.x, topCenter.y - radius * 1.5f)
      lineTo(topCenter.x + radius * 1.8f, topCenter.y - radius * 1.5f)
      lineTo(topCenter.x + radius * 1.8f, topCenter.y + radius * 1.5f)
      lineTo(topCenter.x, topCenter.y + radius * 1.5f)
      close()
    }

    val leftPath = Path().apply { op(topHexPath, leftClip, PathOperation.Intersect) }
    drawPath(
      path = leftPath,
      brush = Brush.linearGradient(
        colors = listOf(color.lightColor, color.topColor),
        start = Offset(topCenter.x - radius * 0.8f, topCenter.y - radius * 0.8f),
        end = Offset(topCenter.x, topCenter.y + radius * 0.8f)
      )
    )

    val rightPath = Path().apply { op(topHexPath, rightClip, PathOperation.Intersect) }
    drawPath(
      path = rightPath,
      brush = Brush.linearGradient(
        colors = listOf(splitColor.lightColor, splitColor.topColor),
        start = Offset(topCenter.x, topCenter.y - radius * 0.8f),
        end = Offset(topCenter.x + radius * 0.8f, topCenter.y + radius * 0.8f)
      )
    )

    drawLine(
      color = Color.White.copy(alpha = 0.88f),
      start = Offset(topCenter.x, topCenter.y - radius * 0.86f),
      end = Offset(topCenter.x, topCenter.y + radius * 0.86f),
      strokeWidth = 2.4f
    )
  } else {
    drawPath(
      path = topHexPath,
      brush = Brush.linearGradient(
        colors = listOf(color.lightColor, color.topColor, color.sideColor.copy(alpha = 0.9f)),
        start = Offset(topCenter.x - radius * 0.55f, topCenter.y - radius * 0.85f),
        end = Offset(topCenter.x + radius * 0.55f, topCenter.y + radius * 0.85f)
      )
    )
  }

  // Glossy Candy Glass Specular Sheen on upper-left facet of top disc
  if (isTopLayer) {
    val sheenPath = HexMath.createRoundedHexagonPath(
      Offset(topCenter.x - radius * 0.06f, topCenter.y - radius * 0.14f),
      radius * 0.72f,
      0.16f
    )
    val upperMask = Path().apply {
      moveTo(topCenter.x - radius, topCenter.y - radius)
      lineTo(topCenter.x + radius, topCenter.y - radius)
      lineTo(topCenter.x + radius, topCenter.y - radius * 0.05f)
      quadraticTo(topCenter.x, topCenter.y - radius * 0.34f, topCenter.x - radius, topCenter.y + radius * 0.10f)
      close()
    }
    val glossOverlay = Path().apply { op(sheenPath, upperMask, PathOperation.Intersect) }
    drawPath(
      path = glossOverlay,
      brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.05f)),
        startY = topCenter.y - radius * 0.85f,
        endY = topCenter.y
      )
    )
  }

  // Crisp bevel stroke & Colorblind accessibility border
  if (isColorblindMode) {
    drawPath(
      path = topHexPath,
      color = Color.Black,
      style = Stroke(width = if (isTopLayer) 4.2f else 2.2f)
    )
    drawPath(
      path = topHexPath,
      color = Color.White,
      style = Stroke(width = if (isTopLayer) 2.2f else 1.2f)
    )
  } else {
    drawPath(
      path = topHexPath,
      color = Color.White.copy(alpha = if (isTopLayer) 0.82f else 0.32f),
      style = Stroke(width = if (isTopLayer) 2.6f else 1.2f)
    )
    if (isTopLayer) {
      val innerRim = HexMath.createRoundedHexagonPath(topCenter, radius * 0.90f, 0.16f)
      drawPath(
        path = innerRim,
        color = Color.White.copy(alpha = 0.25f),
        style = Stroke(width = 1.2f)
      )
    }
  }
}

private fun DrawScope.drawFanningSlide(
  slide: ActiveSlide,
  hexRadius: Float,
  now: Long,
  isColorblindMode: Boolean,
  typeface: Typeface
) {
  val progress = slide.progress(now)
  val count = slide.count
  val sliceRadius = hexRadius * 0.86f

  for (i in 0 until count) {
    val transform = HexMath.calculateSliceArc(
      start = slide.startPos,
      end = slide.endPos,
      progress = progress,
      sliceIndex = i,
      totalSlices = count,
      arcPeakHeight = hexRadius * 2.2f
    )

    rotate(transform.rotationDegrees, pivot = transform.position) {
      scale(transform.scale, pivot = transform.position) {
        val shadowCenter = transform.position.copy(y = transform.position.y + transform.elevation * 0.35f + 7f)
        val shadowPath = HexMath.createRoundedHexagonPath(shadowCenter, sliceRadius * 0.94f, 0.16f)
        drawPath(shadowPath, color = Color(0xFF0B3C73).copy(alpha = 0.20f))

        draw3DFacetedHexDisc(
          center = transform.position,
          radius = sliceRadius,
          height = 6.5f,
          color = slide.color,
          isTopLayer = (i == count - 1),
          isColorblindMode = isColorblindMode
        )

        if (i == count - 1 && isColorblindMode) {
          val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.White.toArgb()
            textSize = sliceRadius * 0.30f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            setShadowLayer(5f, 0f, 2f, Color.Black.toArgb())
          }
          drawContext.canvas.nativeCanvas.drawText(
            slide.color.fullBadgeText,
            transform.position.x,
            transform.position.y - 6f + sliceRadius * 0.12f,
            badgePaint
          )
        }
      }
    }
  }
}

private fun DrawScope.drawHammerStrikeVFX(strikePos: Offset, radius: Float, timeMs: Long) {
  for (i in 0 until 12) {
    val angleRad = (i * 30.0 + timeMs * 0.05) * PI / 180.0
    val rayLen = radius * 3.2f
    val endX = strikePos.x + rayLen * cos(angleRad).toFloat()
    val endY = strikePos.y + rayLen * sin(angleRad).toFloat()
    drawLine(
      brush = Brush.radialGradient(listOf(Color(0xFFFFF59D), Color.Transparent), center = strikePos, radius = rayLen),
      start = strikePos,
      end = Offset(endX, endY),
      strokeWidth = 6f
    )
  }

  drawCircle(
    color = Color(0xFFFFD600).copy(alpha = 0.85f),
    radius = radius * 1.8f,
    center = strikePos,
    style = Stroke(width = 5f)
  )

  val hammerCenter = Offset(strikePos.x + 20f, strikePos.y - 45f)
  rotate(-35f, pivot = hammerCenter) {
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFFA726), Color(0xFFE65100))),
      topLeft = Offset(hammerCenter.x - 10f, hammerCenter.y),
      size = Size(20f, 65f),
      cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
      brush = Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFD50000)), center = hammerCenter, radius = 35f),
      topLeft = Offset(hammerCenter.x - 36f, hammerCenter.y - 30f),
      size = Size(72f, 40f),
      cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
      color = Color(0xFFFFD600),
      topLeft = Offset(hammerCenter.x - 42f, hammerCenter.y - 34f),
      size = Size(13f, 48f),
      cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
      color = Color(0xFFFFD600),
      topLeft = Offset(hammerCenter.x + 29f, hammerCenter.y - 34f),
      size = Size(13f, 48f),
      cornerRadius = CornerRadius(6f, 6f)
    )
  }
}

// --- Crisp HD Vector Obstacles ---

private fun DrawScope.drawCoinObstacle(
  center: Offset,
  radius: Float,
  amount: Int,
  timeMs: Long,
  typeface: Typeface
) {
  val plateCenter = Offset(center.x, center.y - 3f)
  val platePath = HexMath.createRoundedHexagonPath(plateCenter, radius * 0.90f, 0.16f)

  // Soft outer glow & frosted glass plate
  drawPath(
    path = platePath,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.92f), Color(0xFFD6F0FE).copy(alpha = 0.88f))
    )
  )
  drawPath(path = platePath, color = Color.White, style = Stroke(width = 3.0f))

  val wobble = sin(timeMs * 0.004f) * 2.5f
  val coinCenter = Offset(plateCenter.x, plateCenter.y - radius * 0.14f + wobble)
  val coinR = radius * 0.34f

  // Soft shadow beneath coin
  drawCircle(
    color = Color(0xFF01579B).copy(alpha = 0.18f),
    radius = coinR,
    center = Offset(coinCenter.x, coinCenter.y + 3.5f)
  )

  // Outer Gold Rim
  drawCircle(
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFFFFF176), Color(0xFFFFC107), Color(0xFFFF8F00)),
      start = Offset(coinCenter.x - coinR, coinCenter.y - coinR),
      end = Offset(coinCenter.x + coinR, coinCenter.y + coinR)
    ),
    radius = coinR,
    center = coinCenter
  )
  drawCircle(color = Color(0xFFE65100).copy(alpha = 0.65f), radius = coinR, center = coinCenter, style = Stroke(width = 2.0f))

  // Inner embossed gold disc
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFF59D), Color(0xFFFFCA28)),
      center = Offset(coinCenter.x - coinR * 0.25f, coinCenter.y - coinR * 0.25f),
      radius = coinR * 0.8f
    ),
    radius = coinR * 0.72f,
    center = coinCenter
  )

  // Inner Hexagon Coin Emblem
  val innerHex = HexMath.createRoundedHexagonPath(coinCenter, coinR * 0.42f, 0.18f)
  drawPath(innerHex, color = Color(0xFFFFB300))
  drawPath(innerHex, color = Color(0xFFFFF9C4), style = Stroke(width = 1.6f))

  // Crisp Anti-Aliased "25" Label
  val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = Color(0xFF1976D2).toArgb()
    textSize = radius * 0.36f
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
    style = Paint.Style.STROKE
    strokeWidth = 4.5f
    strokeJoin = Paint.Join.ROUND
  }
  val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = Color.White.toArgb()
    textSize = radius * 0.36f
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
  }
  val labelY = plateCenter.y + radius * 0.56f
  drawContext.canvas.nativeCanvas.drawText("$amount", plateCenter.x, labelY, strokePaint)
  drawContext.canvas.nativeCanvas.drawText("$amount", plateCenter.x, labelY, fillPaint)
}

/**
 * Renders the 3D Blue Robot Toy figurine (from reference Screenshot_20261023_230226.jpg)
 * with smooth idle bobbing, cyan monitor visor, orange eyes, clamp arms, and red antenna ball.
 */
private fun DrawScope.drawRobotObstacle(
  center: Offset,
  radius: Float,
  remainingHits: Int,
  timeMs: Long
) {
  val bob = sin(timeMs * 0.005f) * 3.5f
  val rCenter = Offset(center.x, center.y - radius * 0.08f + bob)
  val s = radius * 0.52f

  // Ground shadow on hex cap
  drawOval(
    color = Color(0xFF0D47A1).copy(alpha = 0.25f),
    topLeft = Offset(center.x - s * 0.72f, center.y + s * 0.46f),
    size = Size(s * 1.44f, s * 0.36f)
  )

  // Orange Feet
  drawRoundRect(
    color = Color(0xFFFF6D00),
    topLeft = Offset(rCenter.x - s * 0.46f, rCenter.y + s * 0.38f),
    size = Size(s * 0.36f, s * 0.20f),
    cornerRadius = CornerRadius(6f, 6f)
  )
  drawRoundRect(
    color = Color(0xFFFF6D00),
    topLeft = Offset(rCenter.x + s * 0.10f, rCenter.y + s * 0.38f),
    size = Size(s * 0.36f, s * 0.20f),
    cornerRadius = CornerRadius(6f, 6f)
  )

  // Clamp Arms (Left & Right)
  drawRoundRect(
    color = Color(0xFF0288D1),
    topLeft = Offset(rCenter.x - s * 0.72f, rCenter.y + s * 0.02f),
    size = Size(s * 0.24f, s * 0.30f),
    cornerRadius = CornerRadius(8f, 8f)
  )
  drawRoundRect(
    color = Color(0xFF0288D1),
    topLeft = Offset(rCenter.x + s * 0.48f, rCenter.y + s * 0.02f),
    size = Size(s * 0.24f, s * 0.30f),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Blue Torso
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD))),
    topLeft = Offset(rCenter.x - s * 0.48f, rCenter.y - s * 0.02f),
    size = Size(s * 0.96f, s * 0.48f),
    cornerRadius = CornerRadius(10f, 10f)
  )
  drawRoundRect(
    color = Color(0xFF01579B),
    topLeft = Offset(rCenter.x - s * 0.48f, rCenter.y - s * 0.02f),
    size = Size(s * 0.96f, s * 0.48f),
    cornerRadius = CornerRadius(10f, 10f),
    style = Stroke(width = 2.0f)
  )

  // Cyan Chest Core Light
  drawCircle(
    color = Color(0xFF00E5FF),
    radius = s * 0.13f,
    center = Offset(rCenter.x, rCenter.y + s * 0.22f)
  )

  // Antenna Stem & Red Ball
  drawLine(
    color = Color(0xFF01579B),
    start = Offset(rCenter.x, rCenter.y - s * 0.62f),
    end = Offset(rCenter.x, rCenter.y - s * 0.84f),
    strokeWidth = 3.5f
  )
  drawCircle(
    brush = Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFFF1744))),
    radius = s * 0.14f,
    center = Offset(rCenter.x, rCenter.y - s * 0.88f)
  )

  // Robot Head
  drawRoundRect(
    brush = Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1))),
    topLeft = Offset(rCenter.x - s * 0.56f, rCenter.y - s * 0.66f),
    size = Size(s * 1.12f, s * 0.68f),
    cornerRadius = CornerRadius(14f, 14f)
  )
  drawRoundRect(
    color = Color.White.copy(alpha = 0.7f),
    topLeft = Offset(rCenter.x - s * 0.56f, rCenter.y - s * 0.66f),
    size = Size(s * 1.12f, s * 0.68f),
    cornerRadius = CornerRadius(14f, 14f),
    style = Stroke(width = 2.0f)
  )

  // Cyan Visor Faceplate
  drawRoundRect(
    color = Color(0xFF004D61),
    topLeft = Offset(rCenter.x - s * 0.42f, rCenter.y - s * 0.54f),
    size = Size(s * 0.84f, s * 0.42f),
    cornerRadius = CornerRadius(8f, 8f)
  )

  // Glowing Orange Eyes
  drawCircle(color = Color(0xFFFFAB00), radius = s * 0.10f, center = Offset(rCenter.x - s * 0.19f, rCenter.y - s * 0.35f))
  drawCircle(color = Color(0xFFFFF59D), radius = s * 0.04f, center = Offset(rCenter.x - s * 0.21f, rCenter.y - s * 0.37f))

  drawCircle(color = Color(0xFFFFAB00), radius = s * 0.10f, center = Offset(rCenter.x + s * 0.19f, rCenter.y - s * 0.35f))
  drawCircle(color = Color(0xFFFFF59D), radius = s * 0.04f, center = Offset(rCenter.x + s * 0.17f, rCenter.y - s * 0.37f))

  // Cute Cyan Smile
  drawLine(
    color = Color(0xFF00E5FF),
    start = Offset(rCenter.x - s * 0.14f, rCenter.y - s * 0.20f),
    end = Offset(rCenter.x + s * 0.14f, rCenter.y - s * 0.20f),
    strokeWidth = 2.5f
  )

  if (remainingHits > 1) {
    drawCircle(color = Color(0xFFFF1744), radius = s * 0.18f, center = Offset(rCenter.x + s * 0.48f, rCenter.y - s * 0.60f))
  }
}

private fun DrawScope.drawGhostObstacle(center: Offset, radius: Float, timeMs: Long, typeface: Typeface) {
  val bob = sin(timeMs * 0.004f) * 4f
  val gCenter = Offset(center.x, center.y - 6f + bob)

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFE1F5FE), Color(0xFF81D4FA), Color(0xFF29B6F6)),
      center = gCenter,
      radius = radius * 0.52f
    ),
    radius = radius * 0.52f,
    center = gCenter
  )
  drawCircle(color = Color.White, radius = radius * 0.52f, center = gCenter, style = Stroke(width = 2.0f))

  val eyeY = gCenter.y - 3f
  drawLine(color = Color(0xFF01579B), start = Offset(gCenter.x - 12f, eyeY), end = Offset(gCenter.x - 4f, eyeY), strokeWidth = 3f)
  drawLine(color = Color(0xFF01579B), start = Offset(gCenter.x + 4f, eyeY), end = Offset(gCenter.x + 12f, eyeY), strokeWidth = 3f)

  drawCircle(Color(0xFFFF80AB).copy(alpha = 0.65f), radius = 4f, center = Offset(gCenter.x - 14f, eyeY + 7f))
  drawCircle(Color(0xFFFF80AB).copy(alpha = 0.65f), radius = 4f, center = Offset(gCenter.x + 14f, eyeY + 7f))

  val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color(0xFF0288D1).toArgb()
    textSize = radius * 0.32f
    isFakeBoldText = true
    this.typeface = typeface
  }
  drawContext.canvas.nativeCanvas.drawText("Zzz", gCenter.x + radius * 0.40f, gCenter.y - radius * 0.34f, paint)
}

private fun DrawScope.drawStarObstacle(center: Offset, radius: Float, timeMs: Long) {
  val pulse = 1f + 0.08f * sin(timeMs * 0.006f)
  val sCenter = Offset(center.x, center.y - 6f)

  scale(pulse, pivot = sCenter) {
    val starPath = Path()
    val outerR = radius * 0.54f
    val innerR = outerR * 0.45f
    for (i in 0 until 10) {
      val r = if (i % 2 == 0) outerR else innerR
      val angle = Math.toRadians((i * 36 - 90).toDouble())
      val x = sCenter.x + r * cos(angle).toFloat()
      val y = sCenter.y + r * sin(angle).toFloat()
      if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()

    drawPath(
      path = starPath,
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD600), Color(0xFFFF8F00)),
        center = sCenter,
        radius = outerR
      )
    )
    drawPath(path = starPath, color = Color(0xFFFF6F00), style = Stroke(width = 2.5f))

    drawCircle(Color(0xFF3E2723), radius = 3.5f, center = Offset(sCenter.x - 7f, sCenter.y - 2f))
    drawCircle(Color(0xFF3E2723), radius = 3.5f, center = Offset(sCenter.x + 7f, sCenter.y - 2f))
  }
}

private fun DrawScope.drawHoneycombObstacle(center: Offset, radius: Float, hitsLeft: Int, typeface: Typeface) {
  val hCenter = Offset(center.x, center.y - 4f)
  val hexPath = HexMath.createRoundedHexagonPath(hCenter, radius * 0.64f, 0.16f)
  drawPath(
    path = hexPath,
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFF8F00)),
      center = hCenter,
      radius = radius * 0.64f
    )
  )
  drawPath(hexPath, color = Color(0xFFFF6F00), style = Stroke(width = 3.2f))

  val innerPath = HexMath.createRoundedHexagonPath(hCenter, radius * 0.36f, 0.16f)
  drawPath(innerPath, color = Color(0xFFFFF8E1).copy(alpha = 0.7f), style = Stroke(width = 2f))

  if (hitsLeft > 1) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color(0xFF3E2723).toArgb()
      textSize = radius * 0.42f
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
      this.typeface = typeface
    }
    drawContext.canvas.nativeCanvas.drawText("$hitsLeft", hCenter.x, hCenter.y + 8f, paint)
  }
}

private fun DrawScope.drawCakeObstacle(center: Offset, radius: Float, hitsLeft: Int) {
  val cCenter = Offset(center.x, center.y - 6f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0xFFFFF0F5), Color(0xFFFF80AB), Color(0xFFC2185B)),
      center = cCenter,
      radius = radius * 0.56f
    ),
    radius = radius * 0.56f,
    center = cCenter
  )
  drawCircle(Color(0xFF880E4F), radius = radius * 0.56f, center = cCenter, style = Stroke(width = 2.8f))

  for (i in 0 until 4) {
    val angle = (i * 90.0) * PI / 180.0
    val sx = cCenter.x + radius * 0.30f * cos(angle).toFloat()
    val sy = cCenter.y + radius * 0.30f * sin(angle).toFloat()
    drawCircle(Color(0xFFFF1744), radius = 5f, center = Offset(sx, sy))
  }

  drawRoundRect(color = Color(0xFFFFF59D), topLeft = Offset(cCenter.x - 3f, cCenter.y - 16f), size = Size(6f, 16f), cornerRadius = CornerRadius(3f, 3f))
  drawCircle(Color(0xFFFF9100), radius = 4f, center = Offset(cCenter.x, cCenter.y - 20f))
}

private fun DrawScope.drawCloudObstacle(center: Offset, radius: Float, timeMs: Long) {
  val bob = sin(timeMs * 0.005f) * 3.5f
  val cCenter = Offset(center.x, center.y - 8f + bob)

  drawCircle(Color(0xFFE1F5FE), radius = radius * 0.34f, center = Offset(cCenter.x - 14f, cCenter.y))
  drawCircle(Color(0xFFFFFFFF), radius = radius * 0.44f, center = Offset(cCenter.x, cCenter.y - 4f))
  drawCircle(Color(0xFFE1F5FE), radius = radius * 0.34f, center = Offset(cCenter.x + 14f, cCenter.y))

  drawCircle(Color(0xFF0288D1), radius = 3f, center = Offset(cCenter.x - 6f, cCenter.y - 2f))
  drawCircle(Color(0xFF0288D1), radius = 3f, center = Offset(cCenter.x + 6f, cCenter.y - 2f))
}

private fun DrawScope.drawLightningObstacle(center: Offset, radius: Float, timeMs: Long) {
  val lCenter = Offset(center.x, center.y - 6f)
  val pulse = 1f + 0.08f * sin(timeMs * 0.012f)

  scale(pulse, pivot = lCenter) {
    val path = Path().apply {
      moveTo(lCenter.x - 2f, lCenter.y - radius * 0.54f)
      lineTo(lCenter.x + 11f, lCenter.y - 2f)
      lineTo(lCenter.x + 1f, lCenter.y)
      lineTo(lCenter.x + 8f, lCenter.y + radius * 0.54f)
      lineTo(lCenter.x - 11f, lCenter.y + 2f)
      lineTo(lCenter.x - 1f, lCenter.y)
      close()
    }
    drawPath(
      path = path,
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFE0F7FA), Color(0xFF00E5FF), Color(0xFF0091EA)),
        center = lCenter,
        radius = radius * 0.54f
      )
    )
    drawPath(path, color = Color.White, style = Stroke(width = 2.5f))
  }
}

private fun DrawScope.drawHammerReticle(center: Offset, radius: Float, timeMs: Long) {
  val pulse = 1f + 0.12f * sin(timeMs * 0.008f)
  val reticleRadius = radius * 1.10f * pulse
  val hex = HexMath.createRoundedHexagonPath(center, reticleRadius, 0.16f)
  drawPath(hex, color = Color(0xFFFF1744).copy(alpha = 0.8f), style = Stroke(width = 4.0f))
  val inner = HexMath.createRoundedHexagonPath(center, reticleRadius * 0.85f, 0.16f)
  drawPath(inner, color = Color(0xFFFFD600).copy(alpha = 0.7f), style = Stroke(width = 2.2f))
}

// --- Particles & Crisp Text ---

private fun DrawScope.drawParticle(p: Particle, nowMs: Long, reusablePath: Path) {
  if (p.alpha <= 0.01f) return

  when (p.shape) {
    ParticleShape.SHOCKWAVE -> {
      val t = ((nowMs - p.spawnTime).toFloat() / p.lifeMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
      val eased = 1f - (1f - t) * (1f - t)
      val radius = 12f + (p.maxRadius - 12f) * eased
      val ringAlpha = (1f - t).coerceIn(0f, 1f)
      drawCircle(
        color = p.color.copy(alpha = 0.36f * ringAlpha),
        radius = radius,
        center = Offset(p.x, p.y),
        style = Stroke(width = (p.size * 1.9f) * (1f - t * 0.45f))
      )
      drawCircle(
        color = Color.White.copy(alpha = 0.85f * ringAlpha),
        radius = radius * 0.94f,
        center = Offset(p.x, p.y),
        style = Stroke(width = (p.size * 0.7f) * (1f - t * 0.5f))
      )
    }

    ParticleShape.SPARK -> {
      val dx = p.x - p.prevX
      val dy = p.y - p.prevY
      val tailX: Float
      val tailY: Float
      if (dx * dx + dy * dy > 1.5f) {
        tailX = p.x - dx * 2.2f
        tailY = p.y - dy * 2.2f
      } else {
        tailX = p.x - p.vx * 0.032f
        tailY = p.y - p.vy * 0.032f
      }
      drawLine(
        color = p.color.copy(alpha = 0.85f * p.alpha),
        start = Offset(tailX, tailY),
        end = Offset(p.x, p.y),
        strokeWidth = p.size * 0.58f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = Color.White.copy(alpha = 0.95f * p.alpha),
        start = Offset((tailX + p.x) * 0.5f, (tailY + p.y) * 0.5f),
        end = Offset(p.x, p.y),
        strokeWidth = p.size * 0.28f,
        cap = StrokeCap.Round
      )
      drawCircle(
        color = Color.White.copy(alpha = p.alpha),
        radius = p.size * 0.26f,
        center = Offset(p.x, p.y)
      )
    }

    ParticleShape.GEM -> {
      val r = p.size * 0.62f
      reusablePath.reset()
      reusablePath.moveTo(p.x, p.y - r)
      reusablePath.lineTo(p.x + r * 0.72f, p.y)
      reusablePath.lineTo(p.x, p.y + r)
      reusablePath.lineTo(p.x - r * 0.72f, p.y)
      reusablePath.close()
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawPath(reusablePath, color = p.color.copy(alpha = p.alpha))
        drawPath(
          reusablePath,
          color = Color.White.copy(alpha = 0.85f * p.alpha),
          style = Stroke(width = 1.6f, join = StrokeJoin.Round)
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.95f * p.alpha),
          radius = r * 0.22f,
          center = Offset(p.x - r * 0.14f, p.y - r * 0.22f)
        )
      }
    }

    ParticleShape.HEXAGON -> {
      val r = p.size * 0.55f
      reusablePath.reset()
      for (i in 0 until 6) {
        val angle = (i * 60.0 - 30.0) * (PI / 180.0)
        val hx = p.x + r * cos(angle).toFloat()
        val hy = p.y + r * sin(angle).toFloat()
        if (i == 0) reusablePath.moveTo(hx, hy) else reusablePath.lineTo(hx, hy)
      }
      reusablePath.close()
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawPath(reusablePath, color = p.color.copy(alpha = p.alpha))
        drawPath(
          reusablePath,
          color = Color.White.copy(alpha = 0.75f * p.alpha),
          style = Stroke(width = 1.4f, join = StrokeJoin.Round)
        )
      }
    }

    ParticleShape.STAR -> {
      val r = p.size * 0.56f
      reusablePath.reset()
      for (i in 0 until 8) {
        val rad = if (i % 2 == 0) r else r * 0.38f
        val a = i * (PI / 4.0)
        val sx = p.x + rad * cos(a).toFloat()
        val sy = p.y + rad * sin(a).toFloat()
        if (i == 0) reusablePath.moveTo(sx, sy) else reusablePath.lineTo(sx, sy)
      }
      reusablePath.close()
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawPath(reusablePath, color = p.color.copy(alpha = p.alpha))
        drawCircle(
          color = Color.White.copy(alpha = 0.9f * p.alpha),
          radius = r * 0.22f,
          center = Offset(p.x, p.y)
        )
      }
    }

    ParticleShape.CONFETTI -> {
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawRoundRect(
          color = p.color.copy(alpha = p.alpha),
          topLeft = Offset(p.x - p.size / 2f, p.y - p.size / 4f),
          size = Size(p.size, p.size / 2f),
          cornerRadius = CornerRadius(3f, 3f)
        )
      }
    }

    ParticleShape.SQUARE -> {
      rotate(p.rotation, pivot = Offset(p.x, p.y)) {
        drawRoundRect(
          color = p.color.copy(alpha = p.alpha),
          topLeft = Offset(p.x - p.size * 0.42f, p.y - p.size * 0.42f),
          size = Size(p.size * 0.84f, p.size * 0.84f),
          cornerRadius = CornerRadius(2.5f, 2.5f)
        )
      }
    }

    ParticleShape.CIRCLE -> {
      drawCircle(
        color = p.color.copy(alpha = p.alpha * 0.35f),
        radius = p.size * 0.72f,
        center = Offset(p.x, p.y)
      )
      drawCircle(
        color = p.color.copy(alpha = p.alpha),
        radius = p.size * 0.45f,
        center = Offset(p.x, p.y)
      )
    }
  }
}

private fun DrawScope.drawFloatingText(ft: FloatingText, now: Long, typeface: Typeface) {
  val progress = ft.progress(now)
  val alpha = (1f - progress).coerceIn(0f, 1f)
  val floatY = ft.y - (progress * 70f)
  val scale = 1f + sin(progress * PI.toFloat()) * 0.25f

  val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = Color(0xFF002147).copy(alpha = 0.85f * alpha).toArgb()
    textSize = ft.fontSizeSp * 2.1f * scale
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
    style = Paint.Style.STROKE
    strokeWidth = 6f
    strokeJoin = Paint.Join.ROUND
  }

  val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = ft.color.copy(alpha = alpha).toArgb()
    textSize = ft.fontSizeSp * 2.1f * scale
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    this.typeface = typeface
    setShadowLayer(8f, 0f, 4f, Color.Black.copy(alpha = 0.45f * alpha).toArgb())
  }

  drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, floatY, strokePaint)
  drawContext.canvas.nativeCanvas.drawText(ft.text, ft.x, floatY, fillPaint)
}
