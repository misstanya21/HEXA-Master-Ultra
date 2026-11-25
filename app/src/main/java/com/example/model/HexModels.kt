package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Axial coordinates for flat-topped hexagonal grid:
 * q: column axis, r: row axis
 */
data class HexCoord(val q: Int, val r: Int) {

  /**
   * 6 adjacent neighbors for flat-topped hexagons:
   * (+1, 0), (+1, -1), (0, -1), (-1, 0), (-1, +1), (0, +1)
   */
  fun getNeighbors(): List<HexCoord> {
    return listOf(
      HexCoord(q + 1, r),
      HexCoord(q + 1, r - 1),
      HexCoord(q, r - 1),
      HexCoord(q - 1, r),
      HexCoord(q - 1, r + 1),
      HexCoord(q, r + 1)
    )
  }

  fun distanceTo(other: HexCoord): Int {
    val dq = this.q - other.q
    val dr = this.r - other.r
    val ds = (-this.q - this.r) - (-other.q - other.r)
    return (kotlin.math.abs(dq) + kotlin.math.abs(dr) + kotlin.math.abs(ds)) / 2
  }
}

/**
 * Represents a single color layer within a stack.
 * [count] is the number of chips/wafers in this layer (1..15).
 * [splitColor] enables dual-sector / split-color tiles (as shown in Screenshot_20261023_230226.jpg).
 */
data class HexTile(
  val color: TileColor,
  val count: Int,
  val splitColor: TileColor? = null
) {
  val isSplit: Boolean get() = splitColor != null && splitColor != color

  fun matchesColor(other: TileColor): Boolean {
    return color == other || (splitColor != null && splitColor == other)
  }

  fun matchesTile(other: HexTile): Boolean {
    return matchesColor(other.color) || (other.splitColor != null && matchesColor(other.splitColor))
  }
}

/**
 * Obstacles and special interactive entities on the hex pillars.
 */
sealed class CellObstacle {
  data object None : CellObstacle()
  data class CoinReward(val coins: Int = 25) : CellObstacle()
  data class SleepyGhost(val remainingHits: Int = 1) : CellObstacle()
  data class StarToken(val remainingHits: Int = 1) : CellObstacle()
  data class Honeycomb(val remainingHits: Int = 2) : CellObstacle()
  data class Cloud(val remainingHits: Int = 1) : CellObstacle()
  data class Cake(val remainingHits: Int = 2) : CellObstacle()
  data class RobotToy(val remainingHits: Int = 1) : CellObstacle()
  data object Lightning : CellObstacle()
}

/**
 * Represents a pedestal / hexagonal column on the board.
 */
data class HexCell(
  val coord: HexCoord,
  val stack: List<HexTile> = emptyList(),
  val obstacle: CellObstacle = CellObstacle.None,
  val isLocked: Boolean = false,
  val popScale: Float = 1f,
  val glowAlpha: Float = 0f
) {
  val topTile: HexTile? get() = stack.lastOrNull()
  val isEmpty: Boolean get() = stack.isEmpty() && obstacle is CellObstacle.None
  val hasObstacle: Boolean get() = obstacle !is CellObstacle.None
  val totalTilesCount: Int get() = stack.sumOf { it.count }
}

/**
 * A stack staged at the bottom ready for the player to place onto the board.
 */
data class StagedStack(
  val id: String = UUID.randomUUID().toString(),
  val tiles: List<HexTile>,
  val pedestalIndex: Int
) {
  val topTile: HexTile? get() = tiles.lastOrNull()
  val totalCount: Int get() = tiles.sumOf { it.count }
}

/**
 * Boosters available to the player in HEXA Master Ultra.
 * Undo, Shuffle, Hint, Hammer, and Pinwheel (as shown in Screenshot_20261023_230226.jpg).
 */
enum class BoosterType(
  val title: String,
  val description: String,
  val cost: Int,
  val iconEmoji: String
) {
  UNDO("Undo", "Take back your last move!", 30, "↩️"),
  SHUFFLE("Shuffle", "Get 3 brand new staged stacks!", 50, "🔁"),
  HINT("Hint", "Highlight the best matching placement!", 35, "💡"),
  HAMMER("Hammer", "Smash and clear any stack or obstacle!", 100, "🔨"),
  PINWHEEL("Pinwheel", "Blow away a whole column of tiles!", 75, "🌀")
}

/**
 * Snapshot for state history and 1-move Undo.
 */
data class BoardSnapshot(
  val boardCells: Map<HexCoord, HexCell>,
  val stagedStacks: List<StagedStack?>,
  val score: Int,
  val remainingGoal: Int,
  val secondaryGoalRemaining: Int
)

/**
 * Hint recommendation containing which staged stack to place where.
 */
data class HintRecommendation(
  val stagedIndex: Int,
  val targetCoord: HexCoord,
  val matchingColor: TileColor,
  val estimatedMatches: Int
)

/**
 * Fanning card-deck slide animation between two adjacent hex cells.
 */
data class ActiveSlide(
  val id: String = UUID.randomUUID().toString(),
  val sourceCoord: HexCoord,
  val targetCoord: HexCoord,
  val startPos: Offset,
  val endPos: Offset,
  val color: TileColor,
  val count: Int,
  val startTimeMs: Long,
  val durationMs: Long = 450L
) {
  fun progress(now: Long): Float {
    val elapsed = now - startTimeMs
    return (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
  }

  val isFinished: Boolean get() = progress(System.currentTimeMillis()) >= 1f
}

/**
 * High-performance particle for sparks, fireworks, gems, coins, and shockwave rings.
 */
data class Particle(
  val x: Float,
  val y: Float,
  val vx: Float,
  val vy: Float,
  val color: Color,
  val size: Float,
  val alpha: Float = 1f,
  val shape: ParticleShape = ParticleShape.CIRCLE,
  val lifeMs: Long = 600L,
  val spawnTime: Long = System.currentTimeMillis(),
  val prevX: Float = x,
  val prevY: Float = y,
  val gravity: Float = 260f,
  val drag: Float = 0.96f,
  val rotation: Float = 0f,
  val spinRate: Float = 0f,
  val maxRadius: Float = 110f
) {
  fun isDead(now: Long): Boolean = (now - spawnTime) >= lifeMs
}

enum class ParticleShape {
  CIRCLE, STAR, SQUARE, HEXAGON, CONFETTI, SPARK, GEM, SHOCKWAVE
}

/**
 * Floating text (e.g., "COMBO X2!", "+25 COINS", "PERFECT!").
 */
data class FloatingText(
  val text: String,
  val x: Float,
  val y: Float,
  val color: Color,
  val fontSizeSp: Float = 22f,
  val spawnTime: Long = System.currentTimeMillis(),
  val durationMs: Long = 900L
) {
  fun isDead(now: Long): Boolean = (now - spawnTime) >= durationMs
  fun progress(now: Long): Float = ((now - spawnTime).toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Screen state navigation: LOADING, MENU, GAME, LEVEL_SELECT
 */
enum class ScreenState {
  LOADING,
  MENU,
  GAME,
  LEVEL_SELECT,
  LEADERBOARD
}

/**
 * Playable game modes selectable from the Main Menu.
 */
enum class GameMode(
  val title: String,
  val shortLabel: String,
  val badgeEmoji: String
) {
  CLASSIC("Classic Levels", "CLASSIC", "🎯"),
  TIME_ATTACK("Time Attack", "90s RUSH", "⏱️"),
  ENDLESS("Endless Mode", "ENDLESS", "♾️")
}
