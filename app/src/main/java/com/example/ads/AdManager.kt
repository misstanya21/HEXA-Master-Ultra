package com.example.ads

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central Ad configuration.
 *
 * Keep ADS_ENABLED = false for ad-free third-party store releases (e.g. APKPure).
 * In a future monetized update, set ADS_ENABLED = true, fill in your Ad Network IDs
 * (e.g. Start.io / Appodeal / Mintegral / AdMob), and call AdManager.registerAdapter(...).
 */
object AdConfig {
  const val ADS_ENABLED = false

  // Brief auto-close transition delay when an ad slot is triggered while ads are disabled.
  const val AUTO_DISMISS_DELAY_MS = 1000L

  // Frequency caps for Interstitial ad slots
  const val INTERSTITIAL_LEVEL_INTERVAL = 3
  const val INTERSTITIAL_RETRY_INTERVAL = 2

  // Placeholder IDs — replace with real IDs from your chosen Ad Network before setting ADS_ENABLED = true
  const val AD_NETWORK_APP_ID = "YOUR_AD_NETWORK_APP_ID"
  const val BANNER_AD_UNIT_ID = "YOUR_BANNER_AD_UNIT_ID"
  const val INTERSTITIAL_AD_UNIT_ID = "YOUR_INTERSTITIAL_AD_UNIT_ID"
  const val REWARDED_AD_UNIT_ID = "YOUR_REWARDED_AD_UNIT_ID"
}

/**
 * All ad placement hooks wired across HEXA Master Ultra.
 */
enum class AdPlacement(val label: String) {
  MENU_BOTTOM_BANNER("Menu Bottom Banner"),
  GAME_BOTTOM_BANNER("Game Bottom Banner"),
  LEADERBOARD_BOTTOM_BANNER("Leaderboard Bottom Banner"),
  LEVEL_COMPLETE_INTERSTITIAL("Level Transition Interstitial"),
  GAME_OVER_RETRY_INTERSTITIAL("Game Over Retry Interstitial"),
  SHOP_REWARDED_COINS("Shop Rewarded Video"),
  GAME_OVER_BONUS_REWARDED("Game Over Bonus Coins")
}

/**
 * Adapter contract for plugging in any real Ad SDK (Start.io, Appodeal, Unity Ads, AdMob, etc.)
 * without changing any gameplay or UI logic.
 */
interface AdSdkAdapter {
  fun initialize(context: Context, appId: String)
  fun showInterstitial(placement: AdPlacement, onAdClosed: () -> Unit)
  fun showRewarded(placement: AdPlacement, onRewardEarned: () -> Unit, onAdClosed: () -> Unit)
}

/**
 * Coordinates all Banner, Interstitial, and Rewarded ad slots.
 * While AdConfig.ADS_ENABLED is false, NO ads are shown; triggered interstitial/rewarded slots
 * automatically bypass and close cleanly after AdConfig.AUTO_DISMISS_DELAY_MS (~1 second).
 */
object AdManager {
  private var sdkAdapter: AdSdkAdapter? = null
  private var activeSlotJob: Job? = null

  private val _isAdSlotActive = MutableStateFlow(false)
  val isAdSlotActive: StateFlow<Boolean> = _isAdSlotActive.asStateFlow()

  private val _activePlacement = MutableStateFlow<AdPlacement?>(null)
  val activePlacement: StateFlow<AdPlacement?> = _activePlacement.asStateFlow()

  fun registerAdapter(adapter: AdSdkAdapter, context: Context) {
    sdkAdapter = adapter
    if (AdConfig.ADS_ENABLED) {
      adapter.initialize(context.applicationContext, AdConfig.AD_NETWORK_APP_ID)
    }
  }

  fun showInterstitialSlot(
    scope: CoroutineScope,
    placement: AdPlacement,
    hasNoAds: Boolean,
    onCompleted: () -> Unit
  ) {
    if (hasNoAds) {
      onCompleted()
      return
    }

    val adapter = sdkAdapter
    if (AdConfig.ADS_ENABLED && adapter != null) {
      _activePlacement.value = placement
      _isAdSlotActive.value = true
      adapter.showInterstitial(placement) {
        _isAdSlotActive.value = false
        _activePlacement.value = null
        onCompleted()
      }
      return
    }

    // Ad-free mode: show zero ads and auto-close cleanly after 1 second
    activeSlotJob?.cancel()
    activeSlotJob = scope.launch {
      _activePlacement.value = placement
      _isAdSlotActive.value = true
      delay(AdConfig.AUTO_DISMISS_DELAY_MS)
      _isAdSlotActive.value = false
      _activePlacement.value = null
      onCompleted()
    }
  }

  fun showRewardedSlot(
    scope: CoroutineScope,
    placement: AdPlacement,
    onRewardEarned: () -> Unit,
    onCompleted: () -> Unit = {}
  ) {
    val adapter = sdkAdapter
    if (AdConfig.ADS_ENABLED && adapter != null) {
      _activePlacement.value = placement
      _isAdSlotActive.value = true
      adapter.showRewarded(
        placement = placement,
        onRewardEarned = onRewardEarned,
        onAdClosed = {
          _isAdSlotActive.value = false
          _activePlacement.value = null
          onCompleted()
        }
      )
      return
    }

    // Ad-free mode: show zero ads, auto-close after 1 second, and grant reward cleanly
    activeSlotJob?.cancel()
    activeSlotJob = scope.launch {
      _activePlacement.value = placement
      _isAdSlotActive.value = true
      delay(AdConfig.AUTO_DISMISS_DELAY_MS)
      _isAdSlotActive.value = false
      _activePlacement.value = null
      onRewardEarned()
      onCompleted()
    }
  }
}
