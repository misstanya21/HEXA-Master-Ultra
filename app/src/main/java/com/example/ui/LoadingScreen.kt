package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.NunitoFontFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val HexPillShape = GenericShape { size, _ ->
  val w = size.width
  val h = size.height
  val tip = (h * 0.36f).coerceAtMost(w * 0.18f)
  moveTo(tip, 0f)
  lineTo(w - tip, 0f)
  lineTo(w, h / 2f)
  lineTo(w - tip, h)
  lineTo(tip, h)
  lineTo(0f, h / 2f)
  close()
}

@Composable
fun LoadingScreen(
  onLoadingComplete: () -> Unit,
  onSupportTap: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val displayTypeface = remember(context) {
    ResourcesCompat.getFont(context, R.font.fredoka) ?: Typeface.DEFAULT_BOLD
  }

  var showSupportDialog by remember { mutableStateOf(false) }
  var progressFinished by remember { mutableStateOf(false) }

  val progressAnim = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    progressAnim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 2300, easing = FastOutSlowInEasing)
    )
    progressFinished = true
  }

  LaunchedEffect(progressFinished, showSupportDialog) {
    if (progressFinished && !showSupportDialog) {
      delay(160)
      onLoadingComplete()
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "loading_fx")
  val fanCycle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "fan_cycle"
  )
  val sparklePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "sparkle_phase"
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .testTag("loading_screen")
  ) {
    // 1. Full-screen HD Vector Canvas: Sky, Pastel Clouds, Sparkles, 3D Title, 3D Pillars & Fanning Arc
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Clean vibrant sky-blue vertical gradient (no blurriness)
      drawRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF109EFF),
            Color(0xFF2CB9FF),
            Color(0xFF5CD6FF),
            Color(0xFFA2ECFF),
            Color(0xFFDDF9FF)
          )
        )
      )

      // Smooth vector pastel clouds in background
      drawSoftVectorClouds(w, h)

      // Soft halo glow behind centerpiece
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.Transparent
          ),
          center = Offset(w * 0.50f, h * 0.56f),
          radius = w * 0.70f
        )
      )

      // 3D Extruded Title ("HEXA" / "MASTER") matching bold reference silhouette
      draw3DLoadingTitle(
        canvasWidth = w,
        topY = h * 0.24f,
        typeface = displayTypeface
      )

      // Floating crisp white diamond & square sparkles
      drawLoadingSparkles(w, h, sparklePhase)

      // 3D Hexagonal Pillar Cluster + Colored Stacks + Animated Magenta Fanning Arc
      drawLoadingHexShowcase(
        w = w,
        h = h,
        fanCycle = fanCycle
      )
    }

    // 2. Foreground Controls: Top-Right "SUPPORT" Hexagonal Pill Button & Bottom Progress Bar
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 10.dp, end = 16.dp)
          .minimumInteractiveComponentSize()
          .testTag("support_button")
          .shadow(8.dp, HexPillShape, ambientColor = Color(0xFF01579B))
          .clip(HexPillShape)
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(
                Color(0xFF5CC8FF).copy(alpha = 0.88f),
                Color(0xFF289DF5).copy(alpha = 0.88f)
              )
            )
          )
          .border(1.8.dp, Color.White, HexPillShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true)
          ) {
            onSupportTap()
            showSupportDialog = true
          }
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        VectorHeadsetIcon(modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.support_button_label),
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Black,
          fontSize = 13.sp,
          letterSpacing = 0.7.sp,
          color = Color.White
        )
      }

      // 3. Bottom Loading Text + Glossy White-on-Blue Capsule Progress Bar
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .padding(horizontal = 32.dp)
          .padding(bottom = 34.dp)
      ) {
        OutlinedScoreText(
          text = stringResource(R.string.loading_label),
          fontSizeSp = 21f,
          strokeColor = Color(0xFF0A4D9C)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rounded Progress Capsule Track
        val pct = progressAnim.value.coerceIn(0f, 1f)
        Box(
          modifier = Modifier
            .testTag("loading_progress_bar")
            .fillMaxWidth()
            .height(18.dp)
            .shadow(10.dp, RoundedCornerShape(50), ambientColor = Color(0xFF01579B))
            .clip(RoundedCornerShape(50))
            .background(
              brush = Brush.verticalGradient(
                listOf(Color(0xFF1D7AE0), Color(0xFF399BFF))
              )
            )
            .border(2.dp, Color(0xFF0B4E9E), RoundedCornerShape(50))
            .padding(2.5.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(pct.coerceAtLeast(0.05f))
              .fillMaxHeight()
              .clip(RoundedCornerShape(50))
              .background(
                brush = Brush.verticalGradient(
                  listOf(Color.White, Color(0xFFE3F5FF))
                )
              )
          )
        }
      }
    }

    if (showSupportDialog) {
      LoadingSupportDialog(
        onDismiss = { showSupportDialog = false }
      )
    }
  }
}

@Composable
private fun VectorHeadsetIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    // Top headband arc
    drawArc(
      color = Color.White,
      startAngle = 195f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(w * 0.16f, h * 0.12f),
      size = Size(w * 0.68f, h * 0.68f),
      style = Stroke(width = w * 0.13f, cap = StrokeCap.Round)
    )
    // Left & Right earcups
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(w * 0.08f, h * 0.42f),
      size = Size(w * 0.22f, h * 0.34f),
      cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(w * 0.70f, h * 0.42f),
      size = Size(w * 0.22f, h * 0.34f),
      cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
    )
    // Mic boom curved toward chin
    drawArc(
      color = Color.White,
      startAngle = 15f,
      sweepAngle = 82f,
      useCenter = false,
      topLeft = Offset(w * 0.36f, h * 0.56f),
      size = Size(w * 0.48f, h * 0.34f),
      style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
    )
  }
}

@Composable
private fun LoadingSupportDialog(
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(20.dp, RoundedCornerShape(28.dp))
        .background(
          brush = Brush.verticalGradient(listOf(Color(0xFFE1F5FE), Color.White)),
          shape = RoundedCornerShape(28.dp)
        )
        .border(2.5.dp, Color(0xFF29B6F6), RoundedCornerShape(28.dp))
        .padding(22.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = stringResource(R.string.support_dialog_title),
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Black,
          fontSize = 24.sp,
          color = Color(0xFF01579B)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "• Drag and drop hex stacks onto empty pillars.\n" +
            "• Matching adjacent colors slide and merge together.\n" +
            "• Stack 10 or more matching tiles to clear and score combos!\n" +
            "• Need Accessibility? Enable Colorblind Symbols anytime in Settings.",
          fontFamily = NunitoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          color = Color(0xFF025082)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .testTag("support_dialog_close")
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Text(
            text = stringResource(R.string.support_dialog_continue),
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
          )
        }
      }
    }
  }
}

private fun DrawScope.drawSoftVectorClouds(w: Float, h: Float) {
  val topCloud = Color(0xFF2AB9FF).copy(alpha = 0.55f)
  drawCircle(topCloud, radius = w * 0.28f, center = Offset(w * 0.14f, h * 0.16f))
  drawCircle(topCloud, radius = w * 0.22f, center = Offset(w * 0.36f, h * 0.21f))
  drawCircle(topCloud, radius = w * 0.34f, center = Offset(w * 0.88f, h * 0.24f))

  val lowerCloud = Color(0xFF8BE5FF).copy(alpha = 0.55f)
  drawCircle(lowerCloud, radius = w * 0.30f, center = Offset(w * 0.10f, h * 0.44f))
  drawCircle(lowerCloud, radius = w * 0.26f, center = Offset(w * 0.38f, h * 0.46f))
  drawCircle(lowerCloud, radius = w * 0.32f, center = Offset(w * 0.82f, h * 0.43f))
}

private fun DrawScope.drawLoadingSparkles(w: Float, h: Float, phase: Float) {
  val sparkles = listOf(
    Triple(Offset(w * 0.15f, h * 0.43f), 9f, true),
    Triple(Offset(w * 0.36f, h * 0.37f), 7f, false),
    Triple(Offset(w * 0.85f, h * 0.45f), 10f, true),
    Triple(Offset(w * 0.72f, h * 0.35f), 6f, false),
    Triple(Offset(w * 0.22f, h * 0.52f), 6f, true)
  )
  sparkles.forEachIndexed { idx, (pos, sizePx, isDiamond) ->
    val pulse = (0.45f + 0.55f * ((sin(phase + idx * 1.3f) + 1f) / 2f)).coerceIn(0.25f, 1f)
    val c = Color.White.copy(alpha = pulse)
    if (isDiamond) {
      val path = Path().apply {
        moveTo(pos.x, pos.y - sizePx * 1.3f)
        lineTo(pos.x + sizePx * 0.9f, pos.y)
        lineTo(pos.x, pos.y + sizePx * 1.3f)
        lineTo(pos.x - sizePx * 0.9f, pos.y)
        close()
      }
      drawPath(path, color = c)
    } else {
      rotate(18f + idx * 12f, pivot = pos) {
        drawRoundRect(
          color = c,
          topLeft = Offset(pos.x - sizePx, pos.y - sizePx),
          size = Size(sizePx * 2f, sizePx * 2f),
          cornerRadius = CornerRadius(3f, 3f)
        )
      }
    }
  }
}

private fun DrawScope.draw3DLoadingTitle(
  canvasWidth: Float,
  topY: Float,
  typeface: Typeface
) {
  val sizeMain = (canvasWidth * 0.165f).coerceIn(92f, 185f)
  val sizeSub = (canvasWidth * 0.145f).coerceIn(82f, 165f)
  val cx = canvasWidth / 2f
  val line2Y = topY + sizeMain * 0.86f

  fun draw3DWord(text: String, y: Float, textSizePx: Float) {
    // Deep extruded 3D shadow depth stack
    for (step in 16 downTo 1) {
      val dy = step * 1.35f
      val depthPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = if (step > 8) Color(0xFF0D59B5).toArgb() else Color(0xFF1E88E5).toArgb()
        textSize = textSizePx
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
        this.typeface = typeface
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 10f
        strokeJoin = Paint.Join.ROUND
      }
      drawContext.canvas.nativeCanvas.drawText(text, cx, y + dy, depthPaint)
    }

    // Crisp outer rim stroke
    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
      color = Color(0xFF3CB8FF).toArgb()
      textSize = textSizePx
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
      this.typeface = typeface
      style = Paint.Style.STROKE
      strokeWidth = 8f
      strokeJoin = Paint.Join.ROUND
    }
    drawContext.canvas.nativeCanvas.drawText(text, cx, y, rimPaint)

    // Glossy pure-white front letter face
    val facePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
      color = Color.White.toArgb()
      textSize = textSizePx
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
      this.typeface = typeface
    }
    drawContext.canvas.nativeCanvas.drawText(text, cx, y, facePaint)
  }

  draw3DWord("HEXA", topY, sizeMain)
  draw3DWord("MASTER", line2Y, sizeSub)

  // Compact "ULTRA" pill badge beneath title
  val pillW = sizeSub * 1.32f
  val pillH = sizeSub * 0.34f
  val pillY = line2Y + sizeSub * 0.24f
  drawRoundRect(
    brush = Brush.horizontalGradient(listOf(Color(0xFFFF4081), Color(0xFFD500F9))),
    topLeft = Offset(cx - pillW / 2f, pillY),
    size = Size(pillW, pillH),
    cornerRadius = CornerRadius(pillH / 2f, pillH / 2f)
  )
  drawRoundRect(
    color = Color.White,
    topLeft = Offset(cx - pillW / 2f, pillY),
    size = Size(pillW, pillH),
    cornerRadius = CornerRadius(pillH / 2f, pillH / 2f),
    style = Stroke(width = 3.2f)
  )
  val ultraPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    color = Color.White.toArgb()
    textSize = pillH * 0.62f
    isFakeBoldText = true
    textAlign = Paint.Align.CENTER
    letterSpacing = 0.14f
    this.typeface = typeface
  }
  val fm = ultraPaint.fontMetrics
  val ultraTextY = pillY + pillH / 2f - (fm.ascent + fm.descent) / 2f
  drawContext.canvas.nativeCanvas.drawText("ULTRA", cx, ultraTextY, ultraPaint)
}

private fun DrawScope.drawLoadingHexShowcase(
  w: Float,
  h: Float,
  fanCycle: Float
) {
  val hexR = (w * 0.155f).coerceIn(66f, 152f)
  val ySquash = 0.72f
  val sqrt3 = sqrt(3.0).toFloat()
  val clusterCenter = Offset(w * 0.50f, h * 0.65f)

  fun hexPos(q: Int, r: Int, zLift: Float = 0f): Offset {
    val x = clusterCenter.x + hexR * 1.54f * q
    val y = clusterCenter.y + hexR * sqrt3 * ySquash * (r + q / 2f) - zLift
    return Offset(x, y)
  }

  // Painter's order from back-to-front for true 3D occlusion
  val backPillars = listOf(
    Pair(hexPos(-1, -1, 18f), h * 0.50f),
    Pair(hexPos(0, -1, 24f), h * 0.50f),
    Pair(hexPos(1, -2, 14f), h * 0.50f),
    Pair(hexPos(2, -1, 6f), h * 0.50f)
  )
  for ((pos, depth) in backPillars) {
    drawShowcaseWhitePillar(pos, hexR, ySquash, depth)
  }

  // Mid row pillars: Blue stack pillar (-1, 0) and Magenta stack pillar (1, -1)
  val bluePillarPos = hexPos(-1, 0, 8f)
  val magentaPillarPos = hexPos(1, -1, 10f)
  val leftEdgePillar = hexPos(-2, 1, -6f)
  val rightEdgePillar = hexPos(2, 0, -8f)

  drawShowcaseWhitePillar(leftEdgePillar, hexR, ySquash, h * 0.52f)
  drawShowcaseWhitePillar(bluePillarPos, hexR, ySquash, h * 0.52f)
  drawShowcaseWhitePillar(magentaPillarPos, hexR, ySquash, h * 0.52f)
  drawShowcaseWhitePillar(rightEdgePillar, hexR, ySquash, h * 0.52f)

  // Draw Tall Blue Stack on left-center pillar
  val tileR = hexR * 0.88f
  val discThick = tileR * 0.165f
  val blueTopCenter = drawShowcaseColorStack(
    baseCenter = Offset(bluePillarPos.x, bluePillarPos.y - discThick * 0.3f),
    radius = tileR,
    ySquash = ySquash,
    layerCount = 8,
    layerThick = discThick,
    topColor = Color(0xFF2196F3),
    lightColor = Color(0xFF64B5F6),
    sideColor = Color(0xFF1565C0),
    darkSideColor = Color(0xFF0D47A1)
  )

  // Draw Medium Magenta Stack on right-center pillar
  val magentaTopCenter = drawShowcaseColorStack(
    baseCenter = Offset(magentaPillarPos.x, magentaPillarPos.y - discThick * 0.3f),
    radius = tileR,
    ySquash = ySquash,
    layerCount = 4,
    layerThick = discThick,
    topColor = Color(0xFFEC40FF),
    lightColor = Color(0xFFF68AFF),
    sideColor = Color(0xFFBD19D4),
    darkSideColor = Color(0xFF880E9E)
  )

  // Animated 7-Slice Magenta Fanning Rainbow Arc cascading from Blue Stack -> Magenta Stack
  drawAnimatedMagentaFanArc(
    start = Offset(blueTopCenter.x + tileR * 0.12f, blueTopCenter.y - discThick * 0.4f),
    end = Offset(magentaTopCenter.x, magentaTopCenter.y - discThick * 0.9f),
    tileRadius = tileR * 0.94f,
    ySquash = ySquash,
    discThick = discThick * 1.08f,
    fanCycle = fanCycle
  )

  // Front-center & Foreground White Pillars (overlap stacks cleanly from below!)
  val frontYellowPillarPos = hexPos(0, 0, -14f)
  val frontLeftPillarPos = hexPos(-1, 1, -24f)
  val frontRightPillarPos = hexPos(1, 0, -24f)
  val bottomCenterPillar = hexPos(0, 1, -36f)

  drawShowcaseWhitePillar(frontYellowPillarPos, hexR, ySquash, h * 0.50f)

  // Short Golden-Yellow Stack on Front-Center Pillar
  drawShowcaseColorStack(
    baseCenter = Offset(frontYellowPillarPos.x, frontYellowPillarPos.y - discThick * 0.25f),
    radius = tileR,
    ySquash = ySquash,
    layerCount = 3,
    layerThick = discThick,
    topColor = Color(0xFFFFD600),
    lightColor = Color(0xFFFFF176),
    sideColor = Color(0xFFFFA000),
    darkSideColor = Color(0xFFE65100)
  )

  drawShowcaseWhitePillar(frontLeftPillarPos, hexR, ySquash, h * 0.50f)
  drawShowcaseWhitePillar(frontRightPillarPos, hexR, ySquash, h * 0.50f)
  drawShowcaseWhitePillar(bottomCenterPillar, hexR, ySquash, h * 0.50f)
}

private fun DrawScope.drawShowcaseWhitePillar(
  center: Offset,
  radius: Float,
  ySquash: Float,
  pillarDepth: Float
) {
  val r = radius * 0.95f
  val v = perspectiveHexVertices(center, r, ySquash)

  // Left Facet (cool sky tint)
  val leftFacet = Path().apply {
    moveTo(v[3].x, v[3].y)
    lineTo(v[2].x, v[2].y)
    lineTo(v[2].x, v[2].y + pillarDepth)
    lineTo(v[3].x, v[3].y + pillarDepth)
    close()
  }
  drawPath(
    leftFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFB3E1FA), Color(0xFF91D0F4)),
      startY = v[3].y,
      endY = v[3].y + pillarDepth
    )
  )

  // Center Facet (soft frosty mid-tone)
  val centerFacet = Path().apply {
    moveTo(v[2].x, v[2].y)
    lineTo(v[1].x, v[1].y)
    lineTo(v[1].x, v[1].y + pillarDepth)
    lineTo(v[2].x, v[2].y + pillarDepth)
    close()
  }
  drawPath(
    centerFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFD5EFFC), Color(0xFFB9E3F9)),
      startY = v[2].y,
      endY = v[2].y + pillarDepth
    )
  )

  // Right Facet (bright crisp white-blue)
  val rightFacet = Path().apply {
    moveTo(v[1].x, v[1].y)
    lineTo(v[0].x, v[0].y)
    lineTo(v[0].x, v[0].y + pillarDepth)
    lineTo(v[1].x, v[1].y + pillarDepth)
    close()
  }
  drawPath(
    rightFacet,
    brush = Brush.verticalGradient(
      colors = listOf(Color(0xFFF0FAFF), Color(0xFFD8F1FD)),
      startY = v[1].y,
      endY = v[1].y + pillarDepth
    )
  )

  // Vertical crisp bevel edge highlights
  drawLine(
    color = Color.White.copy(alpha = 0.6f),
    start = v[2],
    end = Offset(v[2].x, v[2].y + pillarDepth * 0.85f),
    strokeWidth = 1.8f
  )
  drawLine(
    color = Color.White.copy(alpha = 0.85f),
    start = v[1],
    end = Offset(v[1].x, v[1].y + pillarDepth * 0.85f),
    strokeWidth = 2.0f
  )

  // Top Rounded Perspective Hexagon Cap
  val capPath = createPerspectiveRoundedHexPath(center, r, ySquash)
  drawPath(
    capPath,
    brush = Brush.linearGradient(
      colors = listOf(Color.White, Color(0xFFEEF9FF), Color(0xFFD5EEFC)),
      start = Offset(center.x - r, center.y - r * ySquash),
      end = Offset(center.x + r, center.y + r * ySquash)
    )
  )
  drawPath(capPath, color = Color(0xFF9AD3F5).copy(alpha = 0.75f), style = Stroke(width = 2.0f))
}

private fun DrawScope.drawShowcaseColorStack(
  baseCenter: Offset,
  radius: Float,
  ySquash: Float,
  layerCount: Int,
  layerThick: Float,
  topColor: Color,
  lightColor: Color,
  sideColor: Color,
  darkSideColor: Color
): Offset {
  // Subtle ambient shadow on pillar cap
  val shadowPath = createPerspectiveRoundedHexPath(
    Offset(baseCenter.x, baseCenter.y + 6f),
    radius * 1.03f,
    ySquash
  )
  drawPath(shadowPath, color = Color(0xFF0A4785).copy(alpha = 0.22f))

  var curY = baseCenter.y
  for (i in 0 until layerCount) {
    val c = Offset(baseCenter.x, curY)
    drawShowcaseHexDisc(
      center = c,
      radius = radius,
      ySquash = ySquash,
      height = layerThick,
      topColor = topColor,
      lightColor = lightColor,
      sideColor = sideColor,
      darkSideColor = darkSideColor,
      isTop = (i == layerCount - 1)
    )
    curY -= layerThick * 0.96f
  }
  return Offset(baseCenter.x, curY + layerThick * 0.96f)
}

private fun DrawScope.drawShowcaseHexDisc(
  center: Offset,
  radius: Float,
  ySquash: Float,
  height: Float,
  topColor: Color,
  lightColor: Color,
  sideColor: Color,
  darkSideColor: Color,
  isTop: Boolean
) {
  val topCenter = Offset(center.x, center.y - height)
  val vTop = perspectiveHexVertices(topCenter, radius, ySquash)
  val vBot = perspectiveHexVertices(center, radius, ySquash)

  val bottomPath = createPerspectiveRoundedHexPath(center, radius, ySquash)
  drawPath(bottomPath, color = darkSideColor)

  // Left extruded wall
  val fLeft = Path().apply {
    moveTo(vTop[3].x, vTop[3].y)
    lineTo(vTop[2].x, vTop[2].y)
    lineTo(vBot[2].x, vBot[2].y)
    lineTo(vBot[3].x, vBot[3].y)
    close()
  }
  drawPath(fLeft, color = darkSideColor)

  // Center extruded wall
  val fCenter = Path().apply {
    moveTo(vTop[2].x, vTop[2].y)
    lineTo(vTop[1].x, vTop[1].y)
    lineTo(vBot[1].x, vBot[1].y)
    lineTo(vBot[2].x, vBot[2].y)
    close()
  }
  drawPath(fCenter, color = sideColor)

  // Right extruded wall
  val fRight = Path().apply {
    moveTo(vTop[1].x, vTop[1].y)
    lineTo(vTop[0].x, vTop[0].y)
    lineTo(vBot[0].x, vBot[0].y)
    lineTo(vBot[1].x, vBot[1].y)
    close()
  }
  drawPath(fRight, color = lightColor)

  // Subtle seam between stacked chips
  drawPath(bottomPath, color = Color.Black.copy(alpha = 0.22f), style = Stroke(width = 1.4f))

  // Top surface cap
  val topPath = createPerspectiveRoundedHexPath(topCenter, radius, ySquash)
  drawPath(
    topPath,
    brush = Brush.linearGradient(
      colors = listOf(lightColor, topColor),
      start = Offset(topCenter.x - radius * 0.5f, topCenter.y - radius * ySquash),
      end = Offset(topCenter.x + radius * 0.5f, topCenter.y + radius * ySquash)
    )
  )
  drawPath(
    topPath,
    color = Color.White.copy(alpha = if (isTop) 0.72f else 0.26f),
    style = Stroke(width = if (isTop) 2.2f else 1.2f)
  )
}

private fun DrawScope.drawAnimatedMagentaFanArc(
  start: Offset,
  end: Offset,
  tileRadius: Float,
  ySquash: Float,
  discThick: Float,
  fanCycle: Float
) {
  val sliceCount = 7
  val peakLift = tileRadius * 2.35f
  val wavePhase = sin(fanCycle * 2.0 * PI).toFloat()

  // Draw from backmost slice toward frontmost cascading slice
  for (idx in 0 until sliceCount) {
    // Base fanned positions spread cleanly across the arch + gentle breathing cascade wave
    val baseT = (idx + 0.6f) / (sliceCount + 0.2f)
    val rippleOffset = sin(fanCycle * 2.0 * PI - idx * 0.55).toFloat() * 0.045f
    val t = (baseT + rippleOffset).coerceIn(0.06f, 0.96f)

    // Parabolic rainbow trajectory from Blue stack top to Magenta stack top
    val curX = start.x + (end.x - start.x) * t
    val arcY = -4f * peakLift * t * (1f - t)
    val curY = start.y + (end.y - start.y) * t + arcY

    // Fanning tilt angle: lifts up on left, flips upright at peak, leans down on right
    val tiltDeg = -58f + t * 116f + wavePhase * 3.5f

    rotate(tiltDeg, pivot = Offset(curX, curY)) {
      drawShowcaseHexDisc(
        center = Offset(curX, curY),
        radius = tileRadius,
        ySquash = ySquash * (0.85f + 0.15f * kotlin.math.abs(t - 0.5f) * 2f),
        height = discThick,
        topColor = Color(0xFFEC40FF),
        lightColor = Color(0xFFF89CFF),
        sideColor = Color(0xFFBD19D4),
        darkSideColor = Color(0xFF880E9E),
        isTop = true
      )
    }
  }
}

private fun perspectiveHexVertices(center: Offset, radius: Float, ySquash: Float): List<Offset> {
  return (0 until 6).map { i ->
    val angleRad = Math.toRadians(60.0 * i)
    Offset(
      center.x + radius * cos(angleRad).toFloat(),
      center.y + radius * ySquash * sin(angleRad).toFloat()
    )
  }
}

private fun createPerspectiveRoundedHexPath(
  center: Offset,
  radius: Float,
  ySquash: Float,
  cornerFraction: Float = 0.16f
): Path {
  val vertices = perspectiveHexVertices(center, radius, ySquash)
  val path = Path()
  val f = cornerFraction.coerceIn(0.02f, 0.35f)
  for (i in 0 until 6) {
    val prev = vertices[(i + 5) % 6]
    val curr = vertices[i]
    val next = vertices[(i + 1) % 6]

    val pIn = Offset(curr.x + (prev.x - curr.x) * f, curr.y + (prev.y - curr.y) * f)
    val pOut = Offset(curr.x + (next.x - curr.x) * f, curr.y + (next.y - curr.y) * f)

    if (i == 0) path.moveTo(pIn.x, pIn.y) else path.lineTo(pIn.x, pIn.y)
    path.quadraticTo(curr.x, curr.y, pOut.x, pOut.y)
  }
  path.close()
  return path
}
