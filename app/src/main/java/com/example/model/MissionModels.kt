package com.example.model

/**
 * Types of in-game puzzle objectives for daily missions in HEXA Master Ultra.
 */
enum class MissionType {
  STACK_CLEAR,     // Clear 10+ tile stacks
  SCORE_TOTAL,     // Reach specific score thresholds
  LEVEL_COMPLETE,  // Clear levels
  COMBO_STREAK,    // Hit combo multipliers (e.g. 2x, 3x)
  OBSTACLE_CLEAR,  // Clear obstacles (ghosts, cakes, clouds, honeycombs, stars, lightnings, robots)
  TILE_MERGES      // Perform color slide merges
}

/**
 * Rewards earned upon completing a mission: Coins, Gems, Coins+Gems, Booster items, or Grand Chest.
 */
sealed class MissionReward {
  data class Coins(val amount: Int) : MissionReward()
  data class Gems(val amount: Int) : MissionReward()
  data class CoinsAndGems(val coins: Int, val gems: Int) : MissionReward()
  data class Booster(val boosterType: BoosterType, val count: Int = 1) : MissionReward()
  data class GrandChest(
    val coins: Int = 500,
    val gems: Int = 25,
    val booster: BoosterType = BoosterType.HAMMER
  ) : MissionReward()

  val displayText: String
    get() = when (this) {
      is Coins -> "+$amount Coins"
      is Gems -> "+$amount Gems"
      is CoinsAndGems -> "+$coins Coins & +$gems Gems"
      is Booster -> "+$count ${boosterType.title}"
      is GrandChest -> "+$coins Coins, +$gems Gems & Boosters"
    }

  val displayBadge: String
    get() = when (this) {
      is Coins -> "🪙 +$amount"
      is Gems -> "💎 +$amount"
      is CoinsAndGems -> "🪙+$coins 💎+$gems"
      is Booster -> "${boosterType.iconEmoji} +$count"
      is GrandChest -> "🎁 Grand Prize"
    }
}

/**
 * Daily mission objective item with progress tracking and claim status.
 */
data class DailyMission(
  val id: String,
  val type: MissionType,
  val title: String,
  val description: String,
  val iconEmoji: String,
  val targetValue: Int,
  val currentProgress: Int = 0,
  val reward: MissionReward,
  val isClaimed: Boolean = false
) {
  val isCompleted: Boolean get() = currentProgress >= targetValue
  val canClaim: Boolean get() = isCompleted && !isClaimed
  val progressPercent: Float get() = if (targetValue <= 0) 1f else (currentProgress.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
}

/**
 * Badge rarity tiers for Profile Trophies & Achievements.
 */
enum class AchievementTier(val label: String) {
  BRONZE("BRONZE"),
  SILVER("SILVER"),
  GOLD("GOLD"),
  DIAMOND("DIAMOND")
}

/**
 * Lifetime player metrics tracked for unlocking Profile Trophies.
 */
enum class AchievementMetric {
  TOTAL_LEVELS,
  TOTAL_MERGES,
  TOTAL_STACKS,
  BEST_COMBO,
  TOTAL_OBSTACLES,
  BEST_TIME_ATTACK,
  BEST_ENDLESS,
  TOTAL_GEMS
}

/**
 * Milestone trophy / badge unlocked on the player's profile.
 */
data class Achievement(
  val id: String,
  val title: String,
  val description: String,
  val iconEmoji: String,
  val tier: AchievementTier,
  val metric: AchievementMetric,
  val targetValue: Int,
  val currentProgress: Int = 0,
  val rewardCoins: Int = 0,
  val rewardGems: Int = 10,
  val isClaimed: Boolean = false
) {
  val isUnlocked: Boolean get() = currentProgress >= targetValue
  val canClaim: Boolean get() = isUnlocked && !isClaimed
  val progressPercent: Float get() = if (targetValue <= 0) 1f else (currentProgress.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)

  val rewardBadge: String
    get() = when {
      rewardCoins > 0 && rewardGems > 0 -> "🪙+$rewardCoins 💎+$rewardGems"
      rewardGems > 0 -> "💎 +$rewardGems"
      else -> "🪙 +$rewardCoins"
    }
}

/**
 * Aggregated lifetime profile stats displayed in the Profile & Trophies hub.
 */
data class PlayerProfileStats(
  val totalMerges: Int = 0,
  val totalStacksCleared: Int = 0,
  val totalLevelsCompleted: Int = 0,
  val bestComboEver: Int = 1,
  val totalObstaclesCleared: Int = 0,
  val bestTimeAttackScore: Int = 0,
  val bestEndlessScore: Int = 0,
  val unlockedTrophies: Int = 0,
  val totalTrophies: Int = 8
) {
  val rankTitle: String
    get() = when {
      unlockedTrophies >= 7 -> "Hexa Legend"
      unlockedTrophies >= 5 -> "Grand Tactician"
      unlockedTrophies >= 3 -> "Puzzle Master"
      unlockedTrophies >= 1 -> "Rising Strategist"
      else -> "Hexa Rookie"
    }

  val rankEmoji: String
    get() = when {
      unlockedTrophies >= 7 -> "👑"
      unlockedTrophies >= 5 -> "💎"
      unlockedTrophies >= 3 -> "🔥"
      unlockedTrophies >= 1 -> "⚡"
      else -> "🌱"
    }
}

/**
 * Catalogs for rotating Daily Missions and permanent Milestone Achievements.
 */
object EngagementCatalog {

  fun buildDailyMissionsForDate(dateString: String): List<DailyMission> {
    val pool = listOf(
      DailyMission(
        id = "dm_merge_10",
        type = MissionType.TILE_MERGES,
        title = "Quick Merger",
        description = "Merge 10 hexagonal tiles",
        iconEmoji = "✨",
        targetValue = 10,
        reward = MissionReward.CoinsAndGems(coins = 180, gems = 10)
      ),
      DailyMission(
        id = "dm_level_1",
        type = MissionType.LEVEL_COMPLETE,
        title = "Stage Conqueror",
        description = "Complete 1 puzzle level",
        iconEmoji = "🏁",
        targetValue = 1,
        reward = MissionReward.CoinsAndGems(coins = 220, gems = 12)
      ),
      DailyMission(
        id = "dm_stack_clear_3",
        type = MissionType.STACK_CLEAR,
        title = "Stack Destroyer",
        description = "Clear 3 full stacks of 10+ tiles",
        iconEmoji = "💥",
        targetValue = 3,
        reward = MissionReward.Coins(350)
      ),
      DailyMission(
        id = "dm_score_2500",
        type = MissionType.SCORE_TOTAL,
        title = "Point Master",
        description = "Score 2,500 total points in puzzles",
        iconEmoji = "⭐",
        targetValue = 2500,
        reward = MissionReward.Booster(BoosterType.HAMMER, 1)
      ),
      DailyMission(
        id = "dm_combo_3",
        type = MissionType.COMBO_STREAK,
        title = "Chain Reaction",
        description = "Reach a 3x Combo Streak",
        iconEmoji = "🔥",
        targetValue = 3,
        reward = MissionReward.Gems(18)
      ),
      DailyMission(
        id = "dm_obstacle_2",
        type = MissionType.OBSTACLE_CLEAR,
        title = "Obstacle Buster",
        description = "Clear 2 board obstacles or robots",
        iconEmoji = "🤖",
        targetValue = 2,
        reward = MissionReward.CoinsAndGems(coins = 250, gems = 15)
      ),
      DailyMission(
        id = "dm_merges_20",
        type = MissionType.TILE_MERGES,
        title = "Slide Virtuoso",
        description = "Perform 20 hexagonal tile merges",
        iconEmoji = "🎯",
        targetValue = 20,
        reward = MissionReward.Gems(20)
      ),
      DailyMission(
        id = "dm_level_2",
        type = MissionType.LEVEL_COMPLETE,
        title = "Level Climber",
        description = "Complete 2 puzzle stages",
        iconEmoji = "🏆",
        targetValue = 2,
        reward = MissionReward.Booster(BoosterType.SHUFFLE, 1)
      )
    )

    val seed = kotlin.math.abs(dateString.hashCode())
    val rotated = pool.indices.map { i -> pool[(seed + i) % pool.size] }

    // Select 4 challenges covering distinct mission types for variety each day
    val chosen = mutableListOf<DailyMission>()
    val usedTypes = mutableSetOf<MissionType>()
    for (mission in rotated) {
      if (mission.type !in usedTypes) {
        chosen.add(mission)
        usedTypes.add(mission.type)
        if (chosen.size == 4) break
      }
    }
    return chosen
  }

  fun baseAchievements(): List<Achievement> = listOf(
    Achievement(
      id = "ach_first_win",
      title = "First Victory",
      description = "Complete your first puzzle level",
      iconEmoji = "🌱",
      tier = AchievementTier.BRONZE,
      metric = AchievementMetric.TOTAL_LEVELS,
      targetValue = 1,
      rewardCoins = 150,
      rewardGems = 10
    ),
    Achievement(
      id = "ach_merge_25",
      title = "Tile Alchemist",
      description = "Merge 25 hexagonal tile stacks",
      iconEmoji = "⚡",
      tier = AchievementTier.BRONZE,
      metric = AchievementMetric.TOTAL_MERGES,
      targetValue = 25,
      rewardCoins = 200,
      rewardGems = 15
    ),
    Achievement(
      id = "ach_stack_15",
      title = "Stack Crusher",
      description = "Clear 15 full 10+ tile stacks",
      iconEmoji = "💥",
      tier = AchievementTier.SILVER,
      metric = AchievementMetric.TOTAL_STACKS,
      targetValue = 15,
      rewardCoins = 250,
      rewardGems = 20
    ),
    Achievement(
      id = "ach_combo_4",
      title = "Combo Maestro",
      description = "Achieve a 4x or higher Combo Streak",
      iconEmoji = "🔥",
      tier = AchievementTier.SILVER,
      metric = AchievementMetric.BEST_COMBO,
      targetValue = 4,
      rewardCoins = 0,
      rewardGems = 25
    ),
    Achievement(
      id = "ach_obstacle_10",
      title = "Rescue Hero",
      description = "Clear 10 obstacles, coins, or robots",
      iconEmoji = "🤖",
      tier = AchievementTier.SILVER,
      metric = AchievementMetric.TOTAL_OBSTACLES,
      targetValue = 10,
      rewardCoins = 300,
      rewardGems = 20
    ),
    Achievement(
      id = "ach_time_attack_1000",
      title = "Speed Demon",
      description = "Score 1,000+ in Time Attack Mode",
      iconEmoji = "⏱️",
      tier = AchievementTier.GOLD,
      metric = AchievementMetric.BEST_TIME_ATTACK,
      targetValue = 1000,
      rewardCoins = 350,
      rewardGems = 30
    ),
    Achievement(
      id = "ach_endless_1500",
      title = "Endless Legend",
      description = "Score 1,500+ in Endless Mode",
      iconEmoji = "♾️",
      tier = AchievementTier.GOLD,
      metric = AchievementMetric.BEST_ENDLESS,
      targetValue = 1500,
      rewardCoins = 400,
      rewardGems = 35
    ),
    Achievement(
      id = "ach_level_5",
      title = "Hexa Grandmaster",
      description = "Complete 5 puzzle levels overall",
      iconEmoji = "👑",
      tier = AchievementTier.DIAMOND,
      metric = AchievementMetric.TOTAL_LEVELS,
      targetValue = 5,
      rewardCoins = 500,
      rewardGems = 50
    )
  )
}
