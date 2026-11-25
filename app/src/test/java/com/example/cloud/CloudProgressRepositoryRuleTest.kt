package com.example.cloud

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.LeaderboardCategory
import com.example.model.PlayerCloudSave
import com.example.model.PlayerProfileStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class CloudProgressRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun uniqueEmail(prefix: String): String =
    "${prefix}_${UUID.randomUUID().toString().take(8)}@test.com"

  @Test
  fun syncAndFetchCloudProgress_authenticatedOwner_persistsAndMerges() = runBlocking {
    val uid = signInTestUser(uniqueEmail("alice"))
    val repo = CloudProgressRepository(firestore, auth)

    val initialSave = PlayerCloudSave(
      displayName = "Alice Hexa",
      currentLevel = 5L,
      highestLevel = 5L,
      coins = 820L,
      gems = 55L,
      totalScore = 4400L,
      bestTimeAttackScore = 1350L,
      bestEndlessScore = 2100L,
      totalLevelsCompleted = 4L,
      totalMerges = 60L,
      totalStacksCleared = 18L,
      bestComboEver = 6L,
      totalObstaclesCleared = 11L,
      consecutiveDays = 3L,
      unlockedTrophies = 4L
    )
    val stats = PlayerProfileStats(
      totalMerges = 60,
      totalStacksCleared = 18,
      totalLevelsCompleted = 4,
      bestComboEver = 6,
      totalObstaclesCleared = 11,
      bestTimeAttackScore = 1350,
      bestEndlessScore = 2100,
      unlockedTrophies = 4
    )

    val synced = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudProgress(initialSave, stats).getOrThrow()
    }
    assertEquals(uid, synced.userId)
    assertEquals(820L, synced.coins)

    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.fetchCloudSave().getOrThrow()
    }
    assertNotNull(fetched)
    assertEquals(5L, fetched!!.highestLevel)
    assertEquals(4400L, fetched.totalScore)

    // Merging a lower local state on a new device must preserve the higher cloud progress
    val newDeviceLocal = PlayerCloudSave(
      displayName = "Alice Hexa",
      currentLevel = 1L,
      highestLevel = 1L,
      coins = 450L,
      gems = 30L,
      totalScore = 0L
    )
    val merged = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudProgress(newDeviceLocal, stats).getOrThrow()
    }
    assertEquals(5L, merged.highestLevel)
    assertEquals(820L, merged.coins)
    assertEquals(4400L, merged.totalScore)
  }

  @Test
  fun observeLeaderboard_authenticated_emitsPublishedPlayersSorted() = runBlocking {
    val aliceUid = signInTestUser(uniqueEmail("leader"))
    val repo = CloudProgressRepository(firestore, auth)

    val aliceSave = PlayerCloudSave(
      displayName = "Alice",
      currentLevel = 7L,
      highestLevel = 7L,
      coins = 1000L,
      gems = 70L,
      totalScore = 7600L,
      bestTimeAttackScore = 1900L,
      bestEndlessScore = 2800L,
      unlockedTrophies = 5L
    )
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.syncCloudProgress(aliceSave, PlayerProfileStats(unlockedTrophies = 5)).getOrThrow()
    }

    val entries = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeLeaderboard(LeaderboardCategory.TOTAL_SCORE).first { list ->
        list.any { it.userId == aliceUid }
      }
    }
    assertTrue(entries.any { it.userId == aliceUid && it.totalScore == 7600L })

    val taEntries = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.fetchLeaderboardOnce(LeaderboardCategory.TIME_ATTACK).getOrThrow()
    }
    assertTrue(taEntries.any { it.userId == aliceUid && it.bestTimeAttackScore == 1900L })
  }

  @Test
  fun crossUserCloudSaveAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(uniqueEmail("owner"))
    val aliceRepo = CloudProgressRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.syncCloudProgress(
        PlayerCloudSave(displayName = "Alice", coins = 700L),
        PlayerProfileStats()
      ).getOrThrow()
    }

    signInTestUser(uniqueEmail("intruder"))
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection(CloudProgressRepository.USERS_COLLECTION).document(aliceUid).get().await()
      }
      fail("Expected PERMISSION_DENIED when another user reads Alice's private cloud save")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  @Test
  fun unauthenticatedLeaderboardQuery_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore.collection(CloudProgressRepository.LEADERBOARD_COLLECTION)
          .whereGreaterThanOrEqualTo("totalScore", 0L)
          .get()
          .await()
      }
      fail("Expected PERMISSION_DENIED for unauthenticated leaderboard read")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
