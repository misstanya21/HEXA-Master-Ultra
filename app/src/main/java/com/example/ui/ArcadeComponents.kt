package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.RubikFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ArcadeButtonColors(
  val topColor: Color,
  val bottomColor: Color,
  val lipColor: Color,
  val borderColor: Color = Color.White.copy(alpha = 0.88f)
)

object ArcadeButtonStyles {
  val Emerald = ArcadeButtonColors(
    topColor = Color(0xFF2EFF8B),
    bottomColor = Color(0xFF00B853),
    lipColor = Color(0xFF007B36),
    borderColor = Color(0xFFB9F6CA)
  )
  val SkyBlue = ArcadeButtonColors(
    topColor = Color(0xFF3FD0FF),
    bottomColor = Color(0xFF0082E6),
    lipColor = Color(0xFF005199),
    borderColor = Color(0xFFB3E5FC)
  )
  val Magenta = ArcadeButtonColors(
    topColor = Color(0xFFFF5CA8),
    bottomColor = Color(0xFFE0116F),
    lipColor = Color(0xFF960047),
    borderColor = Color(0xFFFFCCE4)
  )
  val Violet = ArcadeButtonColors(
    topColor = Color(0xFF9F75FF),
    bottomColor = Color(0xFF5D2AE8),
    lipColor = Color(0xFF3914A3),
    borderColor = Color(0xFFD1C4E9)
  )
  val AmberGold = ArcadeButtonColors(
    topColor = Color(0xFFFFCF33),
    bottomColor = Color(0xFFFF7B00),
    lipColor = Color(0xFFB84E00),
    borderColor = Color(0xFFFFF59D)
  )
  val Crimson = ArcadeButtonColors(
    topColor = Color(0xFFFF6E6E),
    bottomColor = Color(0xFFE5193B),
    lipColor = Color(0xFF9B001D),
    borderColor = Color(0xFFFFCDD2)
  )
  val TealCyan = ArcadeButtonColors(
    topColor = Color(0xFF26E8E8),
    bottomColor = Color(0xFF008996),
    lipColor = Color(0xFF005661),
    borderColor = Color(0xFFB2EBF2)
  )
  val Slate = ArcadeButtonColors(
    topColor = Color(0xFF78909C),
    bottomColor = Color(0xFF455A64),
    lipColor = Color(0xFF263238),
    borderColor = Color(0xFFCFD8DC)
  )
}

/**
 * Tactile AAA 3D Arcade Button with bottom extruded bevel lip, top specular glass sheen,
 * spring press-down bounce, and minimum 48dp accessible touch target.
 */
@Composable
fun Arcade3DButton(
  onClick: () -> Unit,
  colors: ArcadeButtonColors,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  height: Dp = 54.dp,
  cornerRadius: Dp = 20.dp,
  lipHeight: Dp = 5.dp,
  content: @Composable RowScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val pressScale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.96f else 1f,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
    label = "arcade_btn_scale"
  )
  val pressOffsetDp by animateFloatAsState(
    targetValue = if (isPressed && enabled) lipHeight.value * 0.72f else 0f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = 800f),
    label = "arcade_btn_offset"
  )

  val activeColors = if (enabled) colors else ArcadeButtonStyles.Slate
  val shape = RoundedCornerShape(cornerRadius)

  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .height(height + lipHeight)
      .scale(pressScale)
      .shadow(
        elevation = if (enabled) 10.dp else 2.dp,
        shape = shape,
        ambientColor = activeColors.bottomColor.copy(alpha = 0.45f),
        spotColor = activeColors.lipColor.copy(alpha = 0.7f)
      )
      .clip(shape)
      .background(activeColors.lipColor, shape)
      .clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = ripple(color = Color.White.copy(alpha = 0.35f)),
        onClick = onClick
      )
  ) {
    // Raised glossy face
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .offset(y = pressOffsetDp.dp)
        .background(
          brush = Brush.verticalGradient(listOf(activeColors.topColor, activeColors.bottomColor)),
          shape = shape
        )
        .border(
          width = 1.6.dp,
          color = if (enabled) activeColors.borderColor else Color.White.copy(alpha = 0.45f),
          shape = shape
        ),
      contentAlignment = Alignment.Center
    ) {
      // Top glossy specular highlight band
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth(0.90f)
          .padding(top = 3.dp)
          .height(height * 0.34f)
          .background(
            brush = Brush.verticalGradient(
              listOf(
                Color.White.copy(alpha = if (enabled) 0.38f else 0.14f),
                Color.Transparent
              )
            ),
            shape = RoundedCornerShape(cornerRadius)
          )
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
      )
    }
  }
}

/**
 * Crisp arcade display text with dark outer stroke and warm luminance so titles pop
 * cleanly over cosmic, neon, or sunlit arena art.
 */
@Composable
fun ArcadeOutlinedText(
  text: String,
  fontSizeSp: Float,
  modifier: Modifier = Modifier,
  fillColor: Color = Color.White,
  strokeColor: Color = Color(0xFF071E42),
  strokeWidthPx: Float = 7f,
  fontFamily: FontFamily = FredokaFontFamily,
  fontWeight: FontWeight = FontWeight.Black,
  letterSpacingSp: Float = 0.5f,
  maxLines: Int = 1
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Text(
      text = text,
      fontFamily = fontFamily,
      fontWeight = fontWeight,
      fontSize = fontSizeSp.sp,
      letterSpacing = letterSpacingSp.sp,
      color = strokeColor,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      style = TextStyle(
        drawStyle = Stroke(
          width = strokeWidthPx,
          join = StrokeJoin.Round,
          cap = StrokeCap.Round
        )
      )
    )
    Text(
      text = text,
      fontFamily = fontFamily,
      fontWeight = fontWeight,
      fontSize = fontSizeSp.sp,
      letterSpacing = letterSpacingSp.sp,
      color = fillColor,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
fun VectorGemIcon(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val outer = Path().apply {
      moveTo(w * 0.28f, h * 0.14f)
      lineTo(w * 0.72f, h * 0.14f)
      lineTo(w * 0.94f, h * 0.40f)
      lineTo(w * 0.50f, h * 0.90f)
      lineTo(w * 0.06f, h * 0.40f)
      close()
    }
    drawPath(
      outer,
      brush = Brush.linearGradient(
        listOf(Color(0xFFB2FEFF), Color(0xFF00E5FF), Color(0xFF0083B0)),
        start = Offset(0f, 0f),
        end = Offset(w, h)
      )
    )
    val facetTop = Path().apply {
      moveTo(w * 0.28f, h * 0.14f)
      lineTo(w * 0.72f, h * 0.14f)
      lineTo(w * 0.64f, h * 0.40f)
      lineTo(w * 0.36f, h * 0.40f)
      close()
    }
    drawPath(facetTop, color = Color.White.copy(alpha = 0.55f))
    drawLine(Color.White.copy(alpha = 0.8f), Offset(w * 0.06f, h * 0.40f), Offset(w * 0.94f, h * 0.40f), strokeWidth = 1.6f)
    drawPath(outer, color = Color.White, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
  }
}

/**
 * Lightweight animated starlight & luminous bokeh motes backdrop overlay for AAA polish.
 */
@Composable
fun AmbientStarlightOverlay(
  modifier: Modifier = Modifier,
  moteCount: Int = 24,
  primaryGlow: Color = Color(0xFF80D8FF),
  secondaryGlow: Color = Color(0xFFFFE082)
) {
  val infiniteTransition = rememberInfiniteTransition(label = "starlight")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "starlight_phase"
  )

  Canvas(modifier = modifier.fillMaxSize()) {
    for (i in 0 until moteCount) {
      val seedA = ((i * 73 + 19) % 100) / 100f
      val seedB = ((i * 137 + 41) % 100) / 100f
      val baseX = size.width * seedA
      val baseY = size.height * seedB
      val driftX = sin(phase + i * 0.9f) * 18f
      val driftY = cos(phase * 0.8f + i * 1.3f) * 22f
      val twinkle = (0.35f + 0.65f * ((sin(phase * 2f + i * 1.7f) + 1f) * 0.5f))
      val radius = 2.2f + (i % 4) * 1.4f
      val c = if (i % 3 == 0) secondaryGlow else primaryGlow
      val pos = Offset(baseX + driftX, baseY + driftY)
      drawCircle(color = c.copy(alpha = 0.16f * twinkle), radius = radius * 3.0f, center = pos)
      drawCircle(color = Color.White.copy(alpha = 0.55f * twinkle), radius = radius * 0.85f, center = pos)
    }
  }
}

/**
 * Compact 3D Arcade Badge pill text font helper.
 */
val ArcadeLabelFont = RubikFontFamily
