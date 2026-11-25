package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdConfig
import com.example.ads.AdPlacement
import com.example.ui.theme.FredokaFontFamily

/**
 * Reusable Banner Ad slot placed on Menu, Game, and Leaderboard screens.
 * Renders zero height and zero ad UI while AdConfig.ADS_ENABLED is false (or user unlocked No Ads).
 * Ready to host a real Ad SDK Banner AndroidView when ADS_ENABLED is enabled in a future update.
 */
@Composable
fun BannerAdSlot(
  placement: AdPlacement,
  hasNoAds: Boolean,
  modifier: Modifier = Modifier
) {
  if (!AdConfig.ADS_ENABLED || hasNoAds) return

  Box(
    modifier = modifier
      .testTag("banner_ad_slot_${placement.name.lowercase()}")
      .fillMaxWidth()
      .height(52.dp)
      .background(Color(0xFF01579B).copy(alpha = 0.14f)),
    contentAlignment = Alignment.Center
  ) {
    // Future Ad SDK Banner AndroidView goes here when ADS_ENABLED = true
  }
}

/**
 * Clean 1-second auto-closing transition host for Interstitial and Rewarded ad slots.
 * While AdConfig.ADS_ENABLED is false, displays NO advertisements — just a clean 1-second
 * stage/reward transition that closes automatically.
 */
@Composable
fun AdSlotTransitionHost(
  isSlotActive: Boolean,
  activePlacement: AdPlacement?
) {
  AnimatedVisibility(
    visible = isSlotActive && !AdConfig.ADS_ENABLED,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .testTag("ad_slot_transition_host")
        .background(Color(0x66001A33))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { /* Prevent double-tap during 1s slot transition */ },
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .shadow(16.dp, RoundedCornerShape(24.dp))
          .background(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE1F5FE))),
            shape = RoundedCornerShape(24.dp)
          )
          .border(2.dp, Color(0xFF40C4FF), RoundedCornerShape(24.dp))
          .padding(horizontal = 28.dp, vertical = 20.dp)
      ) {
        CircularProgressIndicator(
          color = Color(0xFF0288D1),
          strokeWidth = 3.5.dp,
          modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = when (activePlacement) {
            AdPlacement.SHOP_REWARDED_COINS,
            AdPlacement.GAME_OVER_BONUS_REWARDED -> "Preparing Reward..."
            else -> "Loading Next Stage..."
          },
          fontFamily = FredokaFontFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFF01579B)
        )
      }
    }
  }
}
