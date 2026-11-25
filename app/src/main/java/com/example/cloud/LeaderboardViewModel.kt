package com.example.cloud

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CloudSyncStatus
import com.example.model.LeaderboardCategory
import com.example.model.LeaderboardEntry
import com.example.model.PlayerCloudSave
import com.example.model.PlayerProfileStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface LeaderboardUiState {
  data object Loading : LeaderboardUiState
  data class Success(val entries: List<LeaderboardEntry>) : LeaderboardUiState
  data class Error(val message: String) : LeaderboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModel(
  private val repository: CloudProgressRepository,
  val currentUserId: String
) : ViewModel() {

  private val _selectedCategory = MutableStateFlow(LeaderboardCategory.TOTAL_SCORE)
  val selectedCategory: StateFlow<LeaderboardCategory> = _selectedCategory.asStateFlow()

  private val _syncStatus = MutableStateFlow(CloudSyncStatus.IDLE)
  val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

  private val _lastSyncLabel = MutableStateFlow<String?>(null)
  val lastSyncLabel: StateFlow<String?> = _lastSyncLabel.asStateFlow()

  private val _syncError = MutableStateFlow<String?>(null)
  val syncError: StateFlow<String?> = _syncError.asStateFlow()

  val leaderboardState: StateFlow<LeaderboardUiState> = _selectedCategory
    .flatMapLatest { category ->
      repository.observeLeaderboard(category, limit = 50L)
        .map<List<LeaderboardEntry>, LeaderboardUiState> { entries ->
          LeaderboardUiState.Success(entries)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val rootError = firestoreError ?: error
          Log.w(TAG, "Error observing leaderboard", rootError)
          emit(LeaderboardUiState.Error(rootError.localizedMessage ?: "Failed to load global rankings"))
        }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = LeaderboardUiState.Loading
    )

  fun selectCategory(category: LeaderboardCategory) {
    _selectedCategory.value = category
  }

  fun syncCloudProgress(
    localSave: PlayerCloudSave,
    stats: PlayerProfileStats,
    onMerged: (PlayerCloudSave) -> Unit
  ) {
    if (_syncStatus.value == CloudSyncStatus.SYNCING) return
    _syncStatus.value = CloudSyncStatus.SYNCING
    _syncError.value = null

    viewModelScope.launch {
      val result = repository.syncCloudProgress(localSave, stats)
      result.onSuccess { merged ->
        _syncStatus.value = CloudSyncStatus.SYNCED
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        _lastSyncLabel.value = "Synced $timeFmt"
        onMerged(merged)
      }.onFailure { err ->
        _syncStatus.value = CloudSyncStatus.ERROR
        _syncError.value = err.localizedMessage ?: "Cloud sync failed"
      }
    }
  }

  private companion object {
    const val TAG = "LeaderboardVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
