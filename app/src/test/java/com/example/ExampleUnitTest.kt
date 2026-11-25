package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testHexNeighbors() {
    val origin = com.example.model.HexCoord(0, 0)
    val neighbors = origin.getNeighbors()
    assertEquals(6, neighbors.size)
    assertTrue(neighbors.contains(com.example.model.HexCoord(1, 0)))
    assertTrue(neighbors.contains(com.example.model.HexCoord(-1, 0)))
  }

  @Test
  fun testLevelsExist() {
    for (i in 1..8) {
      val lvl = com.example.game.GameLevels.getLevel(i)
      assertTrue(lvl.coords.isNotEmpty())
      assertTrue(lvl.targetGoalCount > 0)
    }
  }

  @Test
  fun testCategorizedSoundEffects() {
    val placementSounds = listOf(
      com.example.audio.SoundEffect.TileTap,
      com.example.audio.SoundEffect.TileDrop,
      com.example.audio.SoundEffect.TileHover,
      com.example.audio.SoundEffect.InvalidPlacement
    )
    placementSounds.forEach {
      assertEquals(com.example.audio.SoundCategory.PLACEMENT, it.category)
    }

    val mergeSounds = listOf(
      com.example.audio.SoundEffect.SlideWaveNote(0),
      com.example.audio.SoundEffect.StackMerge,
      com.example.audio.SoundEffect.StackFullClear,
      com.example.audio.SoundEffect.ComboStreak(2)
    )
    mergeSounds.forEach {
      assertEquals(com.example.audio.SoundCategory.MERGE, it.category)
    }

    val boosterSounds = listOf(
      com.example.audio.SoundEffect.HammerSmash,
      com.example.audio.SoundEffect.ShuffleDeck,
      com.example.audio.SoundEffect.HintBeacon,
      com.example.audio.SoundEffect.UndoMove
    )
    boosterSounds.forEach {
      assertEquals(com.example.audio.SoundCategory.BOOSTER, it.category)
    }

    val uiSounds = listOf(
      com.example.audio.SoundEffect.CoinCollect,
      com.example.audio.SoundEffect.StarPop,
      com.example.audio.SoundEffect.DailyRewardJingle,
      com.example.audio.SoundEffect.VictoryFanfare,
      com.example.audio.SoundEffect.GameOverAlert,
      com.example.audio.SoundEffect.MissionComplete,
      com.example.audio.SoundEffect.MissionClaim
    )
    uiSounds.forEach {
      assertEquals(com.example.audio.SoundCategory.UI_REWARD, it.category)
    }
  }

  @Test
  fun testDailyMissionProgressAndClaimStates() {
    val mission = com.example.model.DailyMission(
      id = "test_mission",
      type = com.example.model.MissionType.STACK_CLEAR,
      title = "Clear 5 Stacks",
      description = "Clear 5 complete stacks",
      iconEmoji = "💥",
      targetValue = 5,
      currentProgress = 3,
      reward = com.example.model.MissionReward.Coins(100),
      isClaimed = false
    )

    assertEquals(0.6f, mission.progressPercent, 0.001f)
    assertFalse(mission.isCompleted)
    assertFalse(mission.canClaim)

    // Complete the mission
    val completedMission = mission.copy(currentProgress = 5)
    assertEquals(1.0f, completedMission.progressPercent, 0.001f)
    assertTrue(completedMission.isCompleted)
    assertTrue(completedMission.canClaim)

    // Claim the mission
    val claimedMission = completedMission.copy(isClaimed = true)
    assertTrue(claimedMission.isCompleted)
    assertFalse(claimedMission.canClaim)
  }

  @Test
  fun testMissionRewardTypes() {
    val coinReward = com.example.model.MissionReward.Coins(150)
    assertEquals(150, coinReward.amount)

    val boosterReward = com.example.model.MissionReward.Booster(com.example.model.BoosterType.HAMMER, 2)
    assertEquals(com.example.model.BoosterType.HAMMER, boosterReward.boosterType)
    assertEquals(2, boosterReward.count)

    val grandReward = com.example.model.MissionReward.GrandChest(
      coins = 500,
      gems = 25,
      booster = com.example.model.BoosterType.SHUFFLE
    )
    assertEquals(500, grandReward.coins)
    assertEquals(25, grandReward.gems)
    assertEquals(com.example.model.BoosterType.SHUFFLE, grandReward.booster)
  }

  @Test
  fun testColorblindNumericalSymbols() {
    val entries = com.example.model.TileColor.entries
    assertEquals(8, entries.size)

    // Verify all 8 colors have unique numerical identifiers 1..8
    val ids = entries.map { it.colorId }.toSet()
    assertEquals(8, ids.size)
    for (i in 1..8) {
      assertTrue(ids.contains(i))
      assertNotNull(com.example.model.TileColor.fromColorId(i))
    }

    // Verify each color has its proper symbol and numericalSymbol
    assertEquals("▲", com.example.model.TileColor.GREEN.symbol)
    assertEquals(1, com.example.model.TileColor.GREEN.colorId)
    assertEquals("▲1", com.example.model.TileColor.GREEN.numericalSymbol)
    assertEquals("▲ 1", com.example.model.TileColor.GREEN.fullBadgeText)

    assertEquals("♥", com.example.model.TileColor.MAGENTA.symbol)
    assertEquals(2, com.example.model.TileColor.MAGENTA.colorId)
    assertEquals("♥2", com.example.model.TileColor.MAGENTA.numericalSymbol)
    assertEquals("♥ 2", com.example.model.TileColor.MAGENTA.fullBadgeText)

    assertEquals("★", com.example.model.TileColor.YELLOW.symbol)
    assertEquals(3, com.example.model.TileColor.YELLOW.colorId)
    assertEquals("★3", com.example.model.TileColor.YELLOW.numericalSymbol)
    assertEquals("★ 3", com.example.model.TileColor.YELLOW.fullBadgeText)

    assertEquals("◆", com.example.model.TileColor.BLUE.symbol)
    assertEquals(4, com.example.model.TileColor.BLUE.colorId)
    assertEquals("◆4", com.example.model.TileColor.BLUE.numericalSymbol)

    assertEquals("⬡", com.example.model.TileColor.PURPLE.symbol)
    assertEquals(5, com.example.model.TileColor.PURPLE.colorId)
    assertEquals("⬡5", com.example.model.TileColor.PURPLE.numericalSymbol)

    assertEquals("●", com.example.model.TileColor.CYAN.symbol)
    assertEquals(6, com.example.model.TileColor.CYAN.colorId)
    assertEquals("●6", com.example.model.TileColor.CYAN.numericalSymbol)

    assertEquals("■", com.example.model.TileColor.ORANGE.symbol)
    assertEquals(7, com.example.model.TileColor.ORANGE.colorId)
    assertEquals("■7", com.example.model.TileColor.ORANGE.numericalSymbol)

    assertEquals("✚", com.example.model.TileColor.PEACH.symbol)
    assertEquals(8, com.example.model.TileColor.PEACH.colorId)
    assertEquals("✚8", com.example.model.TileColor.PEACH.numericalSymbol)
  }

  @Test
  fun testSplitColorTileAndRobotObstacle() {
    val splitTile = com.example.model.HexTile(
      color = com.example.model.TileColor.MAGENTA,
      count = 3,
      splitColor = com.example.model.TileColor.GREEN
    )
    assertTrue(splitTile.isSplit)
    assertTrue(splitTile.matchesColor(com.example.model.TileColor.MAGENTA))
    assertTrue(splitTile.matchesColor(com.example.model.TileColor.GREEN))
    assertFalse(splitTile.matchesColor(com.example.model.TileColor.BLUE))

    val greenTile = com.example.model.HexTile(com.example.model.TileColor.GREEN, 2)
    assertFalse(greenTile.isSplit)
    assertTrue(splitTile.matchesTile(greenTile))
    assertTrue(greenTile.matchesTile(splitTile))

    val level3 = com.example.game.GameLevels.getLevel(3)
    assertEquals("Robots", level3.secondaryGoalType)
    assertEquals(3, level3.secondaryGoalCount)
    val robotCount = level3.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.RobotToy }
    assertEquals(3, robotCount)

    assertEquals(5, com.example.model.BoosterType.entries.size)
    assertTrue(com.example.model.BoosterType.entries.contains(com.example.model.BoosterType.PINWHEEL))
  }

  @Test
  fun testSynchronizedSlideTimingAndSliceArrival() {
    val start = androidx.compose.ui.geometry.Offset(100f, 200f)
    val end = androidx.compose.ui.geometry.Offset(320f, 140f)

    for (count in 1..10) {
      val timing = com.example.game.HexMath.computeSlideTiming(count)
      assertTrue(timing.durationMs in 280L..480L)
      assertEquals(minOf(count, 6), timing.visualSlices)

      for (sliceIdx in 0 until count) {
        val atStart = com.example.game.HexMath.calculateSliceArc(
          start = start,
          end = end,
          progress = 0f,
          sliceIndex = sliceIdx,
          totalSlices = count
        )
        assertEquals(start.x, atStart.position.x, 0.01f)
        assertEquals(start.y, atStart.position.y, 0.01f)

        val atEnd = com.example.game.HexMath.calculateSliceArc(
          start = start,
          end = end,
          progress = 1f,
          sliceIndex = sliceIdx,
          totalSlices = count
        )
        assertEquals(end.x, atEnd.position.x, 0.01f)
        assertEquals(end.y, atEnd.position.y, 0.01f)
        assertEquals(0f, atEnd.rotationDegrees, 0.01f)
        assertEquals(1f, atEnd.scale, 0.01f)
      }
    }
  }

  @Test
  fun testLoadingScreenStateExists() {
    val states = com.example.model.ScreenState.entries
    assertEquals(com.example.model.ScreenState.LOADING, states.first())
    assertTrue(states.contains(com.example.model.ScreenState.MENU))
    assertTrue(states.contains(com.example.model.ScreenState.GAME))
    assertTrue(states.contains(com.example.model.ScreenState.LEVEL_SELECT))
  }

  @Test
  fun testMagneticSnapToGridAndAutoLock() {
    val center = androidx.compose.ui.geometry.Offset(500f, 500f)
    val radius = 80f
    val cOrigin = com.example.model.HexCoord(0, 0)
    val cOccupied = com.example.model.HexCoord(1, 0)
    val cEmptyRight = com.example.model.HexCoord(0, 1)

    val board = mapOf(
      cOrigin to com.example.model.HexCell(cOrigin),
      cOccupied to com.example.model.HexCell(
        cOccupied,
        stack = listOf(com.example.model.HexTile(com.example.model.TileColor.BLUE, 2))
      ),
      cEmptyRight to com.example.model.HexCell(cEmptyRight)
    )

    val originPixel = com.example.game.HexMath.hexToPixel(cOrigin, radius, center)
    val occupiedPixel = com.example.game.HexMath.hexToPixel(cOccupied, radius, center)

    // 1. Inside inner lock core -> full 1.0f magnetic snap onto cell center
    val nearOrigin = androidx.compose.ui.geometry.Offset(originPixel.x + 25f, originPixel.y - 15f)
    val lockSnap = com.example.game.HexMath.findMagneticSnapTarget(nearOrigin, board, radius, center)
    assertEquals(cOrigin, lockSnap.snappedCoord)
    assertEquals(1f, lockSnap.snapStrength, 0.001f)
    assertEquals(originPixel.x, lockSnap.visualPosition.x, 0.01f)
    assertEquals(originPixel.y, lockSnap.visualPosition.y, 0.01f)

    // 2. Within outer magnetic capture ring -> smoothly pulled toward empty cell
    val outerCapture = androidx.compose.ui.geometry.Offset(originPixel.x - radius * 0.9f, originPixel.y)
    val pullSnap = com.example.game.HexMath.findMagneticSnapTarget(outerCapture, board, radius, center)
    assertEquals(cOrigin, pullSnap.snappedCoord)
    assertTrue(pullSnap.snapStrength > 0f && pullSnap.snapStrength < 1f)
    assertTrue(pullSnap.visualPosition.x > outerCapture.x) // Pulled rightward toward originPixel.x

    // 3. Directly over an occupied cell (far from any empty cell) -> does NOT snap onto occupied cell
    val onOccupiedOuter = androidx.compose.ui.geometry.Offset(occupiedPixel.x + radius * 0.8f, occupiedPixel.y - radius * 0.6f)
    val noSnap = com.example.game.HexMath.findMagneticSnapTarget(onOccupiedOuter, board, radius, center)
    assertNull(noSnap.snappedCoord)
    assertEquals(0f, noSnap.snapStrength, 0.001f)
  }

  @Test
  fun testParticleShapesAndLifecycle() {
    val shapes = com.example.model.ParticleShape.entries
    assertTrue(shapes.contains(com.example.model.ParticleShape.SPARK))
    assertTrue(shapes.contains(com.example.model.ParticleShape.GEM))
    assertTrue(shapes.contains(com.example.model.ParticleShape.SHOCKWAVE))
    assertTrue(shapes.contains(com.example.model.ParticleShape.STAR))
    assertTrue(shapes.contains(com.example.model.ParticleShape.HEXAGON))
    assertTrue(shapes.contains(com.example.model.ParticleShape.CONFETTI))

    val t0 = 10_000L
    val spark = com.example.model.Particle(
      x = 120f,
      y = 240f,
      vx = 180f,
      vy = -220f,
      color = androidx.compose.ui.graphics.Color(0xFFFFD600),
      size = 10f,
      shape = com.example.model.ParticleShape.SPARK,
      lifeMs = 500L,
      spawnTime = t0
    )
    assertFalse(spark.isDead(t0 + 250L))
    assertTrue(spark.isDead(t0 + 500L))
    assertEquals(120f, spark.prevX, 0.001f)
    assertEquals(240f, spark.prevY, 0.001f)
  }

  @Test
  fun testGameModesAndArenas() {
    val modes = com.example.model.GameMode.entries
    assertEquals(3, modes.size)
    assertTrue(modes.contains(com.example.model.GameMode.CLASSIC))
    assertTrue(modes.contains(com.example.model.GameMode.TIME_ATTACK))
    assertTrue(modes.contains(com.example.model.GameMode.ENDLESS))

    val timeAttack = com.example.game.GameLevels.createTimeAttackBoard()
    assertEquals(19, timeAttack.coords.size)
    assertEquals(19, timeAttack.initialCells.size)
    assertTrue(timeAttack.initialCells.values.any { !it.isEmpty })

    val endless = com.example.game.GameLevels.createEndlessBoard()
    assertEquals(19, endless.coords.size)
    assertEquals(19, endless.initialCells.size)
    assertEquals(com.example.model.TileColor.entries.size, endless.allowedColors.size)
  }

  @Test
  fun testGemsRewardsRotatingMissionsAndAchievements() {
    val gemReward = com.example.model.MissionReward.Gems(25)
    assertEquals(25, gemReward.amount)
    assertEquals("💎 +25", gemReward.displayBadge)

    val comboReward = com.example.model.MissionReward.CoinsAndGems(coins = 180, gems = 10)
    assertEquals(180, comboReward.coins)
    assertEquals(10, comboReward.gems)
    assertEquals("🪙+180 💎+10", comboReward.displayBadge)

    val todayMissions = com.example.model.EngagementCatalog.buildDailyMissionsForDate("2026-11-13")
    assertEquals(4, todayMissions.size)
    assertEquals(4, todayMissions.map { it.type }.toSet().size)

    val baseTrophies = com.example.model.EngagementCatalog.baseAchievements()
    assertEquals(8, baseTrophies.size)

    val locked = baseTrophies.first().copy(currentProgress = 0, isClaimed = false)
    assertFalse(locked.isUnlocked)
    assertFalse(locked.canClaim)

    val unlockedUnclaimed = locked.copy(currentProgress = locked.targetValue)
    assertTrue(unlockedUnclaimed.isUnlocked)
    assertTrue(unlockedUnclaimed.canClaim)

    val claimed = unlockedUnclaimed.copy(isClaimed = true)
    assertTrue(claimed.isUnlocked)
    assertFalse(claimed.canClaim)

    val rookie = com.example.model.PlayerProfileStats(unlockedTrophies = 0)
    assertEquals("Hexa Rookie", rookie.rankTitle)

    val master = com.example.model.PlayerProfileStats(unlockedTrophies = 4)
    assertEquals("Puzzle Master", master.rankTitle)

    val legend = com.example.model.PlayerProfileStats(unlockedTrophies = 8)
    assertEquals("Hexa Legend", legend.rankTitle)
  }

  @Test
  fun testCloudSaveMergeAndLeaderboardCategories() {
    assertTrue(com.example.model.ScreenState.entries.contains(com.example.model.ScreenState.LEADERBOARD))

    val local = com.example.model.PlayerCloudSave(
      displayName = "Tasnim",
      currentLevel = 4L,
      highestLevel = 6L,
      coins = 820L,
      gems = 45L,
      totalScore = 4200L,
      bestTimeAttackScore = 1100L,
      bestEndlessScore = 900L,
      unlockedTrophies = 3L,
      hasNoAds = false
    )
    val remote = com.example.model.PlayerCloudSave(
      id = "uid_1",
      userId = "uid_1",
      displayName = "Hexa Player",
      currentLevel = 2L,
      highestLevel = 8L,
      coins = 600L,
      gems = 80L,
      totalScore = 5100L,
      bestTimeAttackScore = 950L,
      bestEndlessScore = 2400L,
      unlockedTrophies = 5L,
      hasNoAds = true
    )

    val merged = local.mergeWith(remote)
    assertEquals("Tasnim", merged.displayName)
    assertEquals(4L, merged.currentLevel)
    assertEquals(8L, merged.highestLevel)
    assertEquals(820L, merged.coins)
    assertEquals(80L, merged.gems)
    assertEquals(5100L, merged.totalScore)
    assertEquals(1100L, merged.bestTimeAttackScore)
    assertEquals(2400L, merged.bestEndlessScore)
    assertEquals(5L, merged.unlockedTrophies)
    assertTrue(merged.hasNoAds)

    val entry = com.example.model.LeaderboardEntry(
      userId = "uid_1",
      playerName = "Tasnim",
      totalScore = 5100L,
      highestLevel = 8L,
      bestTimeAttackScore = 1100L,
      bestEndlessScore = 2400L,
      unlockedTrophies = 5L
    )
    assertEquals("5100 pts", com.example.model.LeaderboardCategory.TOTAL_SCORE.formattedValue(entry))
    assertEquals("Lv. 8", com.example.model.LeaderboardCategory.HIGHEST_LEVEL.formattedValue(entry))
    assertEquals("1100 pts", com.example.model.LeaderboardCategory.TIME_ATTACK.formattedValue(entry))
    assertEquals("2400 pts", com.example.model.LeaderboardCategory.ENDLESS.formattedValue(entry))
    assertEquals(1L, com.example.model.LeaderboardCategory.HIGHEST_LEVEL.minQueryValue)
  }

  @Test
  fun testAllLevelsAreCompletableWithoutCollisions() {
    for (levelNum in 1..com.example.game.GameLevels.totalLevels) {
      val lvl = com.example.game.GameLevels.getLevel(levelNum)
      assertTrue("Level $levelNum must have empty cells for placement", lvl.initialCells.values.any { it.isEmpty })

      // Verify no cell ever has both a tile stack and an obstacle simultaneously
      lvl.initialCells.values.forEach { cell ->
        if (cell.obstacle !is com.example.model.CellObstacle.None) {
          assertTrue(
            "Level $levelNum cell ${cell.coord} has both stack and obstacle",
            cell.stack.isEmpty()
          )
        }
      }

      // Verify secondary goals strictly match board obstacles
      when (lvl.secondaryGoalType) {
        "Coins" -> {
          val totalCoins = lvl.initialCells.values
            .mapNotNull { it.obstacle as? com.example.model.CellObstacle.CoinReward }
            .sumOf { it.coins }
          assertTrue("Level $levelNum CoinRewards ($totalCoins) < goal (${lvl.secondaryGoalCount})", totalCoins >= lvl.secondaryGoalCount)
        }
        "Robots" -> {
          val count = lvl.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.RobotToy }
          assertTrue("Level $levelNum RobotToy ($count) < goal (${lvl.secondaryGoalCount})", count >= lvl.secondaryGoalCount)
        }
        "Honeycombs" -> {
          val count = lvl.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.Honeycomb }
          assertTrue("Level $levelNum Honeycomb ($count) < goal (${lvl.secondaryGoalCount})", count >= lvl.secondaryGoalCount)
        }
        "Lightning" -> {
          val count = lvl.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.Lightning }
          assertTrue("Level $levelNum Lightning ($count) < goal (${lvl.secondaryGoalCount})", count >= lvl.secondaryGoalCount)
        }
        "Stars" -> {
          val count = lvl.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.StarToken }
          assertTrue("Level $levelNum StarToken ($count) < goal (${lvl.secondaryGoalCount})", count >= lvl.secondaryGoalCount)
        }
        "Cakes" -> {
          val count = lvl.initialCells.values.count { it.obstacle is com.example.model.CellObstacle.Cake }
          assertTrue("Level $levelNum Cake ($count) < goal (${lvl.secondaryGoalCount})", count >= lvl.secondaryGoalCount)
        }
      }
    }
  }

  @Test
  fun testAdPlacementInfrastructureDefaults() {
    assertFalse(com.example.ads.AdConfig.ADS_ENABLED)
    assertEquals(1000L, com.example.ads.AdConfig.AUTO_DISMISS_DELAY_MS)
    assertEquals(7, com.example.ads.AdPlacement.entries.size)
  }
}
