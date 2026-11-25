package com.example.cloud

import android.content.Context
import com.example.R
import com.example.model.LeaderboardCategory
import com.example.model.LeaderboardEntry
import com.example.model.PlayerCloudSave
import com.example.model.PlayerProfileStats
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudProgressRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Cloud Save.")
  }

  suspend fun fetchCloudSave(): Result<PlayerCloudSave?> {
    var docPath = USERS_COLLECTION
    return try {
      val uid = requireUserId()
      val docRef = db.collection(USERS_COLLECTION).document(uid)
      docPath = docRef.path
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        Result.success(null)
      } else {
        val data = snapshot.toObject(
          PlayerCloudSave::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        Result.success(data)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docPath)
      Result.failure(e)
    }
  }

  suspend fun syncCloudProgress(
    localSave: PlayerCloudSave,
    stats: PlayerProfileStats
  ): Result<PlayerCloudSave> {
    var writePath = USERS_COLLECTION
    return try {
      val uid = requireUserId()
      val userDocRef = db.collection(USERS_COLLECTION).document(uid)
      val leaderboardDocRef = db.collection(LEADERBOARD_COLLECTION).document(uid)
      writePath = userDocRef.path
      val existingSnapshot = userDocRef.get().await()
      val existingSave = if (existingSnapshot.exists()) {
        existingSnapshot.toObject(
          PlayerCloudSave::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      } else {
        null
      }

      val merged = if (existingSave != null) {
        localSave.copy(userId = uid).mergeWith(existingSave)
      } else {
        localSave.copy(userId = uid)
      }

      if (existingSnapshot.exists()) {
        userDocRef.update(merged.toUpdateMap()).await()
      } else {
        userDocRef.set(merged.toCreateMap(uid)).await()
      }

      // Compute rank based on merged unlocked trophy count
      val mergedStats = stats.copy(
        unlockedTrophies = merged.unlockedTrophies.toInt()
      )
      val lbEntry = LeaderboardEntry(
        userId = uid,
        playerName = merged.safeName(),
        rankTitle = mergedStats.rankTitle,
        rankEmoji = mergedStats.rankEmoji,
        totalScore = merged.totalScore,
        highestLevel = merged.highestLevel,
        bestTimeAttackScore = merged.bestTimeAttackScore,
        bestEndlessScore = merged.bestEndlessScore,
        unlockedTrophies = merged.unlockedTrophies
      )

      val lbSnapshot = leaderboardDocRef.get().await()
      if (lbSnapshot.exists()) {
        leaderboardDocRef.update(lbEntry.toUpdateMap()).await()
      } else {
        leaderboardDocRef.set(lbEntry.toCreateMap(uid)).await()
      }

      Result.success(merged)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, writePath)
      Result.failure(e)
    }
  }

  fun observeLeaderboard(
    category: LeaderboardCategory,
    limit: Long = 50L
  ): Flow<List<LeaderboardEntry>> = flow {
    requireUserId()
    emitAll(
      db.collection(LEADERBOARD_COLLECTION)
        .whereGreaterThanOrEqualTo(category.fieldName, category.minQueryValue)
        .orderBy(category.fieldName, Query.Direction.DESCENDING)
        .limit(limit)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            LeaderboardEntry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
    )
  }

  suspend fun fetchLeaderboardOnce(
    category: LeaderboardCategory,
    limit: Long = 50L
  ): Result<List<LeaderboardEntry>> {
    val queryPath = LEADERBOARD_COLLECTION
    return try {
      requireUserId()
      val snapshot = db.collection(LEADERBOARD_COLLECTION)
        .whereGreaterThanOrEqualTo(category.fieldName, category.minQueryValue)
        .orderBy(category.fieldName, Query.Direction.DESCENDING)
        .limit(limit)
        .get()
        .await()
      val entries = snapshot.toObjects(
        LeaderboardEntry::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      )
      Result.success(entries)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, queryPath)
      Result.failure(e)
    }
  }

  companion object {
    const val USERS_COLLECTION = "users"
    const val LEADERBOARD_COLLECTION = "leaderboard"
  }
}
