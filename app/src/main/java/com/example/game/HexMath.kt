package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.example.model.HexCell
import com.example.model.HexCoord
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object HexMath {
  private val SQRT_3 = sqrt(3.0).toFloat()

  /**
   * Converts axial HexCoord (q, r) to 2D screen Offset for flat-topped hexagons.
   */
  fun hexToPixel(coord: HexCoord, radius: Float, center: Offset): Offset {
    val x = center.x + radius * 1.5f * coord.q
    val y = center.y + radius * SQRT_3 * (coord.r + coord.q / 2f)
    return Offset(x, y)
  }

  /**
   * Converts a 2D screen point to the nearest axial HexCoord.
   */
  fun pixelToHex(point: Offset, radius: Float, center: Offset): HexCoord {
    val ptX = point.x - center.x
    val ptY = point.y - center.y

    val qFract = (2f / 3f * ptX) / radius
    val rFract = (-1f / 3f * ptX + SQRT_3 / 3f * ptY) / radius
    val sFract = -qFract - rFract

    var q = qFract.roundToInt()
    var r = rFract.roundToInt()
    val s = sFract.roundToInt()

    val qDiff = kotlin.math.abs(q - qFract)
    val rDiff = kotlin.math.abs(r - rFract)
    val sDiff = kotlin.math.abs(s - sFract)

    if (qDiff > rDiff && qDiff > sDiff) {
      q = -r - s
    } else if (rDiff > sDiff) {
      r = -q - s
    }
    return HexCoord(q, r)
  }

  /**
   * Evaluates magnetic Snap-to-Grid attraction around the elevated drag anchor.
   * Pulls the floating tile cleanly toward the nearest empty cell within capture radius
   * and locks onto the cell center when inside the inner snap core.
   */
  fun findMagneticSnapTarget(
    dragAnchor: Offset,
    boardCells: Map<HexCoord, HexCell>,
    hexRadius: Float,
    boardCenter: Offset
  ): SnapResult {
    if (boardCells.isEmpty() || hexRadius <= 0f) {
      return SnapResult(null, dragAnchor, 0f, false)
    }

    val directCoord = pixelToHex(dragAnchor, hexRadius, boardCenter)
    val directCell = boardCells[directCoord]

    var nearestEmptyCoord: HexCoord? = null
    var nearestEmptyCenter = Offset.Zero
    var minEmptyDist = Float.MAX_VALUE
    var minAnyDist = Float.MAX_VALUE

    for ((coord, cell) in boardCells) {
      val cellCenter = hexToPixel(coord, hexRadius, boardCenter)
      val dist = hypot(dragAnchor.x - cellCenter.x, dragAnchor.y - cellCenter.y)
      if (dist < minAnyDist) {
        minAnyDist = dist
      }
      if (cell.isEmpty && dist < minEmptyDist) {
        minEmptyDist = dist
        nearestEmptyCoord = coord
        nearestEmptyCenter = cellCenter
      }
    }

    val isOverBoard = (directCell != null) || (minAnyDist <= hexRadius * 1.45f)
    val captureRadius = hexRadius * 1.28f
    val lockCoreRadius = hexRadius * 0.58f

    if (nearestEmptyCoord != null && minEmptyDist <= captureRadius) {
      if (minEmptyDist <= lockCoreRadius) {
        return SnapResult(
          snappedCoord = nearestEmptyCoord,
          visualPosition = nearestEmptyCenter,
          snapStrength = 1f,
          isOverBoard = true
        )
      }
      val raw = 1f - ((minEmptyDist - lockCoreRadius) / (captureRadius - lockCoreRadius)).coerceIn(0f, 1f)
      val easedPull = raw * (2f - raw)
      val magnetBlend = 0.38f + 0.62f * easedPull
      val snappedPos = Offset(
        x = dragAnchor.x + (nearestEmptyCenter.x - dragAnchor.x) * magnetBlend,
        y = dragAnchor.y + (nearestEmptyCenter.y - dragAnchor.y) * magnetBlend
      )
      return SnapResult(
        snappedCoord = nearestEmptyCoord,
        visualPosition = snappedPos,
        snapStrength = easedPull,
        isOverBoard = true
      )
    }

    return SnapResult(
      snappedCoord = null,
      visualPosition = dragAnchor,
      snapStrength = 0f,
      isOverBoard = isOverBoard
    )
  }

  /**
   * Builds a Path for a flat-topped hexagon.
   */
  fun createHexagonPath(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
      val angleDeg = 60.0 * i
      val angleRad = Math.toRadians(angleDeg)
      val x = center.x + radius * cos(angleRad).toFloat()
      val y = center.y + radius * sin(angleRad).toFloat()
      if (i == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }
    path.close()
    return path
  }

  /**
   * Builds a crisp vector Path for a flat-topped hexagon with smooth rounded corners.
   */
  fun createRoundedHexagonPath(
    center: Offset,
    radius: Float,
    cornerFraction: Float = 0.16f
  ): Path {
    val vertices = (0 until 6).map { i ->
      val angleRad = Math.toRadians(60.0 * i)
      Offset(
        center.x + radius * cos(angleRad).toFloat(),
        center.y + radius * sin(angleRad).toFloat()
      )
    }
    val path = Path()
    val f = cornerFraction.coerceIn(0.02f, 0.35f)
    for (i in 0 until 6) {
      val prev = vertices[(i + 5) % 6]
      val curr = vertices[i]
      val next = vertices[(i + 1) % 6]

      val pIn = Offset(
        curr.x + (prev.x - curr.x) * f,
        curr.y + (prev.y - curr.y) * f
      )
      val pOut = Offset(
        curr.x + (next.x - curr.x) * f,
        curr.y + (next.y - curr.y) * f
      )

      if (i == 0) {
        path.moveTo(pIn.x, pIn.y)
      } else {
        path.lineTo(pIn.x, pIn.y)
      }
      path.quadraticTo(curr.x, curr.y, pOut.x, pOut.y)
    }
    path.close()
    return path
  }

  /**
   * Computes exact stagger delay fraction and per-slice travel duration so that every slice
   * starts smoothly fanned out and reaches progress = 1.0f at the exact end of durationMs.
   */
  fun computeSlideTiming(totalSlices: Int): SlideTiming {
    val count = totalSlices.coerceAtLeast(1)
    val visualSlices = minOf(count, 6)
    val delayPerSliceFrac = if (visualSlices <= 1) {
      0f
    } else {
      (0.32f / (visualSlices - 1)).coerceAtMost(0.06f)
    }
    val durationMs = (240L + visualSlices * 38L).coerceIn(280L, 480L)
    return SlideTiming(
      durationMs = durationMs,
      delayPerSliceFrac = delayPerSliceFrac,
      visualSlices = visualSlices
    )
  }

  /**
   * Computes position and tilt angle of an individual fanning slice during slide animation.
   * Guarantees every slice finishes at sliceProgress == 1f when progress == 1f.
   */
  fun calculateSliceArc(
    start: Offset,
    end: Offset,
    progress: Float,
    sliceIndex: Int,
    totalSlices: Int,
    arcPeakHeight: Float = 70f
  ): SliceTransform {
    val timing = computeSlideTiming(totalSlices)
    val effectiveIdx = minOf(sliceIndex, timing.visualSlices - 1)
    val startOffset = effectiveIdx * timing.delayPerSliceFrac
    val travelSpan = (1f - (timing.visualSlices - 1) * timing.delayPerSliceFrac).coerceAtLeast(0.45f)
    val sliceProgress = ((progress - startOffset) / travelSpan).coerceIn(0f, 1f)

    // Smooth ease in-out
    val eased = if (sliceProgress < 0.5f) {
      2f * sliceProgress * sliceProgress
    } else {
      1f - (-2f * sliceProgress + 2f) * (-2f * sliceProgress + 2f) / 2f
    }

    val curX = start.x + (end.x - start.x) * eased
    val arcY = -4f * arcPeakHeight * eased * (1f - eased)
    val curY = start.y + (end.y - start.y) * eased + arcY

    val fanSpread = (sliceIndex - (totalSlices - 1) / 2f) * 6f
    val tiltAngle = fanSpread * sin(eased * PI).toFloat()

    val scale = 1f + 0.14f * sin(eased * PI).toFloat()

    return SliceTransform(
      position = Offset(curX, curY),
      rotationDegrees = tiltAngle,
      scale = scale,
      elevation = -arcY
    )
  }
}

data class SnapResult(
  val snappedCoord: HexCoord?,
  val visualPosition: Offset,
  val snapStrength: Float,
  val isOverBoard: Boolean
)

data class SlideTiming(
  val durationMs: Long,
  val delayPerSliceFrac: Float,
  val visualSlices: Int
)

data class SliceTransform(
  val position: Offset,
  val rotationDegrees: Float,
  val scale: Float,
  val elevation: Float
)
