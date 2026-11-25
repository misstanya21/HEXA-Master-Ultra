package com.example.game

import com.example.model.CellObstacle
import com.example.model.HexCell
import com.example.model.HexCoord
import com.example.model.HexTile
import com.example.model.TileColor

data class LevelDefinition(
  val levelNumber: Int,
  val name: String,
  val targetGoalCount: Int,
  val targetColor: TileColor? = null,
  val secondaryGoalType: String? = null,
  val secondaryGoalCount: Int = 0,
  val coords: List<HexCoord>,
  val initialCells: Map<HexCoord, HexCell>,
  val allowedColors: List<TileColor>
)

object GameLevels {

  fun getLevel(levelNumber: Int): LevelDefinition {
    return when (levelNumber) {
      1 -> createLevel1()
      2 -> createLevel2()
      3 -> createLevel3()
      4 -> createLevel4()
      5 -> createLevel5()
      6 -> createLevel6()
      7 -> createLevel7()
      else -> createLevel8(levelNumber)
    }
  }

  val totalLevels = 15

  // Level 1: Warm introduction to Color Slide
  private fun createLevel1(): LevelDefinition {
    val coords = listOf(
      HexCoord(0, 0),
      HexCoord(1, 0), HexCoord(1, -1), HexCoord(0, -1),
      HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(0, 1),
      HexCoord(0, -2), HexCoord(0, 2)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val stack = when (coord) {
        HexCoord(0, 0) -> listOf(HexTile(TileColor.GREEN, 4))
        HexCoord(1, 0) -> listOf(HexTile(TileColor.MAGENTA, 3))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.YELLOW, 4))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.GREEN, 3))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.MAGENTA, 2))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack)
    }

    return LevelDefinition(
      levelNumber = 1,
      name = "First Slides",
      targetGoalCount = 35,
      coords = coords,
      initialCells = initialCells,
      allowedColors = listOf(TileColor.GREEN, TileColor.MAGENTA, TileColor.YELLOW, TileColor.BLUE)
    )
  }

  // Level 2: Coin Hunt (featuring +25 Coin Pedestals)
  private fun createLevel2(): LevelDefinition {
    val coords = listOf(
      HexCoord(0, 0),
      HexCoord(1, 0), HexCoord(1, -1), HexCoord(0, -1),
      HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(0, 1),
      HexCoord(2, -1), HexCoord(2, -2), HexCoord(-2, 1), HexCoord(-2, 2),
      HexCoord(1, 1), HexCoord(-1, -1)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(2, -1) -> CellObstacle.CoinReward(25)
        HexCoord(-2, 2) -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(0, 0) -> listOf(HexTile(TileColor.BLUE, 2), HexTile(TileColor.MAGENTA, 3))
        HexCoord(1, 0) -> listOf(HexTile(TileColor.MAGENTA, 4))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.GREEN, 5))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.YELLOW, 3))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.BLUE, 4))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 2,
      name = "Coin Rush",
      targetGoalCount = 60,
      secondaryGoalType = "Coins",
      secondaryGoalCount = 50,
      coords = coords,
      initialCells = initialCells,
      allowedColors = listOf(TileColor.GREEN, TileColor.MAGENTA, TileColor.YELLOW, TileColor.BLUE, TileColor.PURPLE)
    )
  }

  // Level 3: Robot Rampage (Exact match to Screenshot_20261023_230226.jpg!)
  private fun createLevel3(): LevelDefinition {
    val coords = listOf(
      HexCoord(0, -2), HexCoord(0, -1), HexCoord(0, 0), HexCoord(0, 1), HexCoord(0, 2),
      HexCoord(-1, -1), HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(-1, 2),
      HexCoord(1, -1), HexCoord(1, 0), HexCoord(1, 1),
      HexCoord(-2, 0), HexCoord(-2, 1),
      HexCoord(2, -1), HexCoord(2, 0)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(-1, -1) -> CellObstacle.RobotToy(1)
        HexCoord(2, -1) -> CellObstacle.RobotToy(1)
        HexCoord(0, 2) -> CellObstacle.RobotToy(1)
        HexCoord(-2, 0) -> CellObstacle.CoinReward(25)
        HexCoord(-1, 2) -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(0, -2) -> listOf(HexTile(TileColor.PEACH, 3, TileColor.MAGENTA))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.MAGENTA, 3, TileColor.GREEN))
        HexCoord(0, 0) -> listOf(HexTile(TileColor.CYAN, 3, TileColor.BLUE))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.MAGENTA, 3, TileColor.BLUE))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.GREEN, 6))
        HexCoord(-2, 1) -> listOf(HexTile(TileColor.YELLOW, 3, TileColor.BLUE))
        HexCoord(1, 0) -> listOf(HexTile(TileColor.YELLOW, 6))
        HexCoord(1, 1) -> listOf(HexTile(TileColor.PEACH, 3, TileColor.CYAN))
        HexCoord(2, 0) -> listOf(HexTile(TileColor.PURPLE, 3, TileColor.GREEN))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 3,
      name = "Robot Rampage",
      targetGoalCount = 231,
      secondaryGoalType = "Robots",
      secondaryGoalCount = 3,
      coords = coords,
      initialCells = initialCells,
      allowedColors = listOf(TileColor.YELLOW, TileColor.GREEN, TileColor.BLUE, TileColor.MAGENTA, TileColor.CYAN, TileColor.PEACH, TileColor.PURPLE)
    )
  }

  // Level 4: Sweet Honeycomb & Cakes
  private fun createLevel4(): LevelDefinition {
    val coords = listOf(
      HexCoord(0, 0), HexCoord(0, -1), HexCoord(0, 1),
      HexCoord(1, -1), HexCoord(1, 0), HexCoord(1, 1),
      HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(-1, -1),
      HexCoord(2, -1), HexCoord(2, 0), HexCoord(-2, 1), HexCoord(-2, 0),
      HexCoord(0, -2), HexCoord(0, 2), HexCoord(1, -2), HexCoord(-1, 2)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(1, -2) -> CellObstacle.Honeycomb(2)
        HexCoord(-1, 2) -> CellObstacle.Honeycomb(2)
        HexCoord(2, 0) -> CellObstacle.Cake(2)
        HexCoord(-2, 0) -> CellObstacle.Cloud(1)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(0, 0) -> listOf(HexTile(TileColor.ORANGE, 2), HexTile(TileColor.YELLOW, 4))
        HexCoord(1, 0) -> listOf(HexTile(TileColor.YELLOW, 3))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.PEACH, 4))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.GREEN, 5))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.ORANGE, 3))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 4,
      name = "Honey & Cakes",
      targetGoalCount = 90,
      secondaryGoalType = "Honeycombs",
      secondaryGoalCount = 2,
      coords = coords,
      initialCells = initialCells,
      allowedColors = listOf(TileColor.ORANGE, TileColor.YELLOW, TileColor.GREEN, TileColor.PEACH, TileColor.BLUE)
    )
  }

  // Level 5: Electric Storm
  private fun createLevel5(): LevelDefinition {
    val coords = listOf(
      HexCoord(0, 0), HexCoord(0, -1), HexCoord(0, 1),
      HexCoord(1, -1), HexCoord(1, 0), HexCoord(1, 1),
      HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(-1, -1),
      HexCoord(2, -2), HexCoord(2, -1), HexCoord(-2, 1), HexCoord(-2, 2),
      HexCoord(0, -2), HexCoord(0, 2), HexCoord(2, 0), HexCoord(-2, 0)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(2, -2) -> CellObstacle.Lightning
        HexCoord(-2, 2) -> CellObstacle.Lightning
        HexCoord(0, 0) -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(1, 0) -> listOf(HexTile(TileColor.BLUE, 3), HexTile(TileColor.MAGENTA, 4))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.MAGENTA, 3))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.CYAN, 4))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.YELLOW, 5))
        HexCoord(2, -1) -> listOf(HexTile(TileColor.BLUE, 4))
        HexCoord(-2, 1) -> listOf(HexTile(TileColor.GREEN, 3))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 5,
      name = "Thunder Surge",
      targetGoalCount = 110,
      secondaryGoalType = "Lightning",
      secondaryGoalCount = 2,
      coords = coords,
      initialCells = initialCells,
      allowedColors = listOf(TileColor.BLUE, TileColor.MAGENTA, TileColor.CYAN, TileColor.YELLOW, TileColor.GREEN)
    )
  }

  // Level 6: The Golden Ring
  private fun createLevel6(): LevelDefinition {
    val coords = listOf(
      HexCoord(1, 0), HexCoord(1, -1), HexCoord(0, -1),
      HexCoord(-1, 0), HexCoord(-1, 1), HexCoord(0, 1),
      HexCoord(2, -1), HexCoord(2, -2), HexCoord(1, -2),
      HexCoord(-1, -1), HexCoord(-2, 0), HexCoord(-2, 1),
      HexCoord(-2, 2), HexCoord(-1, 2), HexCoord(0, 2),
      HexCoord(1, 1), HexCoord(2, 0), HexCoord(0, 0)
    )

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(0, 0) -> CellObstacle.CoinReward(50)
        HexCoord(2, -2) -> CellObstacle.StarToken(1)
        HexCoord(-2, 2) -> CellObstacle.StarToken(1)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(1, 0) -> listOf(HexTile(TileColor.PURPLE, 4))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.PURPLE, 2))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.GREEN, 5))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.ORANGE, 4))
        HexCoord(2, 0) -> listOf(HexTile(TileColor.YELLOW, 3))
        HexCoord(-2, 0) -> listOf(HexTile(TileColor.BLUE, 3))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 6,
      name = "The Hex Ring",
      targetGoalCount = 125,
      secondaryGoalType = "Stars",
      secondaryGoalCount = 2,
      coords = coords,
      initialCells = initialCells,
      allowedColors = TileColor.entries
    )
  }

  // Level 7: High Rise Mega Puzzle
  private fun createLevel7(): LevelDefinition {
    val coords = mutableListOf<HexCoord>()
    for (q in -2..2) {
      val r1 = maxOf(-2, -q - 2)
      val r2 = minOf(2, -q + 2)
      for (r in r1..r2) {
        coords.add(HexCoord(q, r))
      }
    }

    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(0, 0) -> CellObstacle.Cake(2)
        HexCoord(2, 0) -> CellObstacle.CoinReward(25)
        HexCoord(-2, 0) -> CellObstacle.CoinReward(25)
        HexCoord(0, -2) -> CellObstacle.SleepyGhost(1)
        HexCoord(0, 2) -> CellObstacle.Cloud(1)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(1, 0) -> listOf(HexTile(TileColor.MAGENTA, 3), HexTile(TileColor.GREEN, 4))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.CYAN, 2), HexTile(TileColor.GREEN, 3))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.YELLOW, 4))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.BLUE, 3))
        HexCoord(1, -1) -> listOf(HexTile(TileColor.PURPLE, 4))
        HexCoord(-1, 1) -> listOf(HexTile(TileColor.PEACH, 3))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 7,
      name = "High Rise Mega",
      targetGoalCount = 150,
      secondaryGoalType = "Cakes",
      secondaryGoalCount = 1,
      coords = coords,
      initialCells = initialCells,
      allowedColors = TileColor.entries
    )
  }

  // Level 8+: Endless Master Challenges
  private fun createLevel8(levelNumber: Int): LevelDefinition {
    val coords = createHoneycombCoords(2)
    val starCoords = setOf(HexCoord(2, -2), HexCoord(-2, 2), HexCoord(0, -2))
    val coinCoord = HexCoord(0, 2)
    val initialCells = mutableMapOf<HexCoord, HexCell>()
    val colors = TileColor.entries

    coords.forEachIndexed { index, coord ->
      val obstacle = when (coord) {
        in starCoords -> CellObstacle.StarToken(1)
        coinCoord -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }

      val stack = if (obstacle == CellObstacle.None && index % 3 == 0) {
        val c1 = colors[(index + levelNumber) % colors.size]
        val c2 = colors[(index + levelNumber + 2) % colors.size]
        listOf(
          HexTile(c1, 2 + ((index + levelNumber) % 3)),
          HexTile(c2, 3 + ((index + levelNumber * 2) % 3))
        )
      } else emptyList()

      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = levelNumber,
      name = "Master Challenge $levelNumber",
      targetGoalCount = 100 + levelNumber * 20,
      secondaryGoalType = "Stars",
      secondaryGoalCount = starCoords.size,
      coords = coords,
      initialCells = initialCells,
      allowedColors = TileColor.entries
    )
  }

  private fun createHoneycombCoords(gridRadius: Int = 2): List<HexCoord> {
    val coords = mutableListOf<HexCoord>()
    for (q in -gridRadius..gridRadius) {
      val r1 = maxOf(-gridRadius, -q - gridRadius)
      val r2 = minOf(gridRadius, -q + gridRadius)
      for (r in r1..r2) {
        coords.add(HexCoord(q, r))
      }
    }
    return coords
  }

  /**
   * 90-second Time Attack Arena: fast 6-color palette for high-speed combos & coin rewards.
   */
  fun createTimeAttackBoard(): LevelDefinition {
    val coords = createHoneycombCoords(2)
    val fastColors = listOf(
      TileColor.GREEN,
      TileColor.MAGENTA,
      TileColor.YELLOW,
      TileColor.BLUE,
      TileColor.CYAN,
      TileColor.ORANGE
    )
    val initialCells = mutableMapOf<HexCoord, HexCell>()
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(2, -1), HexCoord(-2, 1) -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(0, 0) -> listOf(HexTile(TileColor.BLUE, 3), HexTile(TileColor.MAGENTA, 4))
        HexCoord(1, 0) -> listOf(HexTile(TileColor.YELLOW, 4))
        HexCoord(-1, 0) -> listOf(HexTile(TileColor.GREEN, 5))
        HexCoord(0, -1) -> listOf(HexTile(TileColor.CYAN, 4))
        HexCoord(0, 1) -> listOf(HexTile(TileColor.ORANGE, 3))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 0,
      name = "Time Attack",
      targetGoalCount = 999,
      coords = coords,
      initialCells = initialCells,
      allowedColors = fastColors
    )
  }

  /**
   * Endless Survival Arena: no timer, no lives limit, infinite waves and high-score chase.
   */
  fun createEndlessBoard(): LevelDefinition {
    val coords = createHoneycombCoords(2)
    val initialCells = mutableMapOf<HexCoord, HexCell>()
    val palette = TileColor.entries
    coords.forEach { coord ->
      val obstacle = when (coord) {
        HexCoord(2, -2), HexCoord(-2, 2) -> CellObstacle.CoinReward(25)
        else -> CellObstacle.None
      }
      val stack = when (coord) {
        HexCoord(0, 0) -> listOf(HexTile(palette[0], 3), HexTile(palette[1], 4))
        HexCoord(1, -1) -> listOf(HexTile(palette[2], 3))
        HexCoord(-1, 1) -> listOf(HexTile(palette[3], 4))
        HexCoord(0, 1) -> listOf(HexTile(palette[4], 3))
        HexCoord(0, -1) -> listOf(HexTile(palette[5], 4))
        else -> emptyList()
      }
      initialCells[coord] = HexCell(coord = coord, stack = stack, obstacle = obstacle)
    }

    return LevelDefinition(
      levelNumber = 0,
      name = "Endless Mode",
      targetGoalCount = 999,
      coords = coords,
      initialCells = initialCells,
      allowedColors = TileColor.entries
    )
  }
}
