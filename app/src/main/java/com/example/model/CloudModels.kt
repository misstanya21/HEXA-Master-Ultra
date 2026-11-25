package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlayerCloudSave(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Hexa Player",
  val currentLevel: Long = 1L,
  val highestLevel: Long = 1L,
  val coins: Long = 450L,
  val gems: Long = 30L,
  val totalScore: Long = 0L,
  val bestTimeAttackScore: Long = 0L,
  val bestEndlessScore: Long = 0L,
  val totalLevelsCompleted: Long = 0L,
  val totalMerges: Long = 0L,
  val totalStacksCleared: Long = 0L,
  val bestComboEver: Long = 1L,
  val totalObstaclesCleared: Long = 0L,
  val consecutiveDays: Long = 1L,
  val unlockedTrophies: Long = 0L,
  val hasNoAds: Boolean = false,
  val isColorblindMode: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun safeName(): String = displayName.trim().ifBlank { "Hexa Player" }.take(40)

  fun mergeWith(remote: PlayerCloudSave): PlayerCloudSave {
    return PlayerCloudSave(
      id = remote.id.ifBlank { id },
      userId = remote.userId.ifBlank { userId },
      displayName = if (displayName != "Hexa Player" && displayName.isNotBlank()) safeName() else remote.safeName(),
      currentLevel = maxOf(currentLevel, remote.currentLevel).coerceAtLeast(1L),
      highestLevel = maxOf(highestLevel, remote.highestLevel).coerceAtLeast(1L),
      coins = maxOf(coins, remote.coins).coerceAtLeast(0L),
      gems = maxOf(gems, remote.gems).coerceAtLeast(0L),
      totalScore = maxOf(totalScore, remote.totalScore).coerceAtLeast(0L),
      bestTimeAttackScore = maxOf(bestTimeAttackScore, remote.bestTimeAttackScore).coerceAtLeast(0L),
      bestEndlessScore = maxOf(bestEndlessScore, remote.bestEndlessScore).coerceAtLeast(0L),
      totalLevelsCompleted = maxOf(totalLevelsCompleted, remote.totalLevelsCompleted).coerceAtLeast(0L),
      totalMerges = maxOf(totalMerges, remote.totalMerges).coerceAtLeast(0L),
      totalStacksCleared = maxOf(totalStacksCleared, remote.totalStacksCleared).coerceAtLeast(0L),
      bestComboEver = maxOf(bestComboEver, remote.bestComboEver).coerceAtLeast(1L),
      totalObstaclesCleared = maxOf(totalObstaclesCleared, remote.totalObstaclesCleared).coerceAtLeast(0L),
      consecutiveDays = maxOf(consecutiveDays, remote.consecutiveDays).coerceAtLeast(1L),
      unlockedTrophies = maxOf(unlockedTrophies, remote.unlockedTrophies).coerceAtLeast(0L),
      hasNoAds = hasNoAds || remote.hasNoAds,
      isColorblindMode = isColorblindMode || remote.isColorblindMode,
      createdAt = remote.createdAt ?: createdAt,
      updatedAt = remote.updatedAt ?: updatedAt
    )
  }

  fun toCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "displayName" to safeName(),
    "currentLevel" to currentLevel.coerceIn(1L, 10000L),
    "highestLevel" to highestLevel.coerceIn(1L, 10000L),
    "coins" to coins.coerceIn(0L, 100000000L),
    "gems" to gems.coerceIn(0L, 10000000L),
    "totalScore" to totalScore.coerceIn(0L, 1000000000L),
    "bestTimeAttackScore" to bestTimeAttackScore.coerceIn(0L, 100000000L),
    "bestEndlessScore" to bestEndlessScore.coerceIn(0L, 100000000L),
    "totalLevelsCompleted" to totalLevelsCompleted.coerceIn(0L, 100000L),
    "totalMerges" to totalMerges.coerceIn(0L, 100000000L),
    "totalStacksCleared" to totalStacksCleared.coerceIn(0L, 10000000L),
    "bestComboEver" to bestComboEver.coerceIn(1L, 10000L),
    "totalObstaclesCleared" to totalObstaclesCleared.coerceIn(0L, 10000000L),
    "consecutiveDays" to consecutiveDays.coerceIn(1L, 10000L),
    "unlockedTrophies" to unlockedTrophies.coerceIn(0L, 100L),
    "hasNoAds" to hasNoAds,
    "isColorblindMode" to isColorblindMode,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  fun toUpdateMap(): Map<String, Any> = mapOf(
    "displayName" to safeName(),
    "currentLevel" to currentLevel.coerceIn(1L, 10000L),
    "highestLevel" to highestLevel.coerceIn(1L, 10000L),
    "coins" to coins.coerceIn(0L, 100000000L),
    "gems" to gems.coerceIn(0L, 10000000L),
    "totalScore" to totalScore.coerceIn(0L, 1000000000L),
    "bestTimeAttackScore" to bestTimeAttackScore.coerceIn(0L, 100000000L),
    "bestEndlessScore" to bestEndlessScore.coerceIn(0L, 100000000L),
    "totalLevelsCompleted" to totalLevelsCompleted.coerceIn(0L, 100000L),
    "totalMerges" to totalMerges.coerceIn(0L, 100000000L),
    "totalStacksCleared" to totalStacksCleared.coerceIn(0L, 10000000L),
    "bestComboEver" to bestComboEver.coerceIn(1L, 10000L),
    "totalObstaclesCleared" to totalObstaclesCleared.coerceIn(0L, 10000000L),
    "consecutiveDays" to consecutiveDays.coerceIn(1L, 10000L),
    "unlockedTrophies" to unlockedTrophies.coerceIn(0L, 100L),
    "hasNoAds" to hasNoAds,
    "isColorblindMode" to isColorblindMode,
    "updatedAt" to FieldValue.serverTimestamp()
  )
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val playerName: String = "Hexa Player",
  val rankTitle: String = "Hexa Rookie",
  val rankEmoji: String = "🌱",
  val totalScore: Long = 0L,
  val highestLevel: Long = 1L,
  val bestTimeAttackScore: Long = 0L,
  val bestEndlessScore: Long = 0L,
  val unlockedTrophies: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun safePlayerName(): String = playerName.trim().ifBlank { "Hexa Player" }.take(40)
  fun safeRankTitle(): String = rankTitle.trim().ifBlank { "Hexa Rookie" }.take(30)
  fun safeRankEmoji(): String = rankEmoji.trim().ifBlank { "🌱" }.take(12)

  fun toCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "playerName" to safePlayerName(),
    "rankTitle" to safeRankTitle(),
    "rankEmoji" to safeRankEmoji(),
    "totalScore" to totalScore.coerceIn(0L, 1000000000L),
    "highestLevel" to highestLevel.coerceIn(1L, 10000L),
    "bestTimeAttackScore" to bestTimeAttackScore.coerceIn(0L, 100000000L),
    "bestEndlessScore" to bestEndlessScore.coerceIn(0L, 100000000L),
    "unlockedTrophies" to unlockedTrophies.coerceIn(0L, 100L),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
  )

  fun toUpdateMap(): Map<String, Any> = mapOf(
    "playerName" to safePlayerName(),
    "rankTitle" to safeRankTitle(),
    "rankEmoji" to safeRankEmoji(),
    "totalScore" to totalScore.coerceIn(0L, 1000000000L),
    "highestLevel" to highestLevel.coerceIn(1L, 10000L),
    "bestTimeAttackScore" to bestTimeAttackScore.coerceIn(0L, 100000000L),
    "bestEndlessScore" to bestEndlessScore.coerceIn(0L, 100000000L),
    "unlockedTrophies" to unlockedTrophies.coerceIn(0L, 100L),
    "updatedAt" to FieldValue.serverTimestamp()
  )
}

enum class LeaderboardCategory(
  val label: String,
  val fieldName: String,
  val minQueryValue: Long,
  val emoji: String
) {
  TOTAL_SCORE("Career Score", "totalScore", 0L, "🏆"),
  HIGHEST_LEVEL("Top Level", "highestLevel", 1L, "🗺️"),
  TIME_ATTACK("90s Rush", "bestTimeAttackScore", 0L, "⏱️"),
  ENDLESS("Endless", "bestEndlessScore", 0L, "♾️");

  fun scoreFor(entry: LeaderboardEntry): Long = when (this) {
    TOTAL_SCORE -> entry.totalScore
    HIGHEST_LEVEL -> entry.highestLevel
    TIME_ATTACK -> entry.bestTimeAttackScore
    ENDLESS -> entry.bestEndlessScore
  }

  fun formattedValue(entry: LeaderboardEntry): String = when (this) {
    TOTAL_SCORE -> "${entry.totalScore} pts"
    HIGHEST_LEVEL -> "Lv. ${entry.highestLevel}"
    TIME_ATTACK -> "${entry.bestTimeAttackScore} pts"
    ENDLESS -> "${entry.bestEndlessScore} pts"
  }
}

enum class CloudSyncStatus {
  IDLE,
  SYNCING,
  SYNCED,
  ERROR
}
