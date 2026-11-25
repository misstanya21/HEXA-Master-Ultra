package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameLevels
import com.example.game.GameViewModel
import com.example.model.ScreenState
import com.example.ui.theme.RubikFontFamily
import com.example.ui.theme.TitanOneFontFamily

@Composable
fun LevelSelectScreen(
  viewModel: GameViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.navigateTo(ScreenState.MENU)
  }

  val currentLevel by viewModel.currentLevel.collectAsState()
  val highestLevel by viewModel.highestLevel.collectAsState()
  val coins by viewModel.coins.collectAsState()
  val maxUnlocked = maxOf(highestLevel, currentLevel)
  val totalLevelsToShow = maxOf(GameLevels.totalLevels, maxUnlocked)

  Box(modifier = modifier.fillMaxSize()) {
    // Illustrated Royal Cosmic Backdrop
    Image(
      painter = painterResource(id = R.drawable.img_bg_menu),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize()
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xAA041430),
              Color(0x77082452),
              Color(0xCC030F28)
            )
          )
        )
    )

    AmbientStarlightOverlay(
      moteCount = 22,
      primaryGlow = Color(0xFF80D8FF),
      secondaryGlow = Color(0xFFFFD54F)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(ScreenState.MENU) },
          modifier = Modifier
            .size(48.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .background(
              brush = Brush.verticalGradient(listOf(Color(0xFF29B6F6), Color(0xFF0277BD))),
              shape = RoundedCornerShape(14.dp)
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        ArcadeOutlinedText(
          text = "SELECT LEVEL",
          fontSizeSp = 23f,
          fillColor = Color.White,
          strokeColor = Color(0xFF061B3E),
          strokeWidthPx = 7f,
          fontFamily = TitanOneFontFamily,
          fontWeight = FontWeight.Normal,
          letterSpacingSp = 0.8f
        )

        Row(
          modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .background(
              brush = Brush.verticalGradient(listOf(Color(0xE6103C78), Color(0xEE082147))),
              shape = RoundedCornerShape(20.dp)
            )
            .border(1.5.dp, Color(0xFF64D8FF), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          VectorCoinIcon(modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "$coins",
            color = Color(0xFFFFEE58),
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 13.5.sp
          )
        }
      }

      // Levels Grid
      LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items((1..totalLevelsToShow).toList()) { lvl ->
          val isUnlocked = lvl <= maxUnlocked
          val isCurrent = lvl == currentLevel
          val isCompleted = lvl < maxUnlocked

          LevelCard(
            level = lvl,
            isUnlocked = isUnlocked,
            isCurrent = isCurrent,
            isCompleted = isCompleted,
            onClick = {
              if (isUnlocked) {
                viewModel.loadLevel(lvl)
                viewModel.navigateTo(ScreenState.GAME)
              }
            }
          )
        }
      }
    }
  }
}

@Composable
fun LevelCard(
  level: Int,
  isUnlocked: Boolean,
  isCurrent: Boolean,
  isCompleted: Boolean,
  onClick: () -> Unit
) {
  val topColors = when {
    isCurrent -> listOf(Color(0xFFFF66B2), Color(0xFFD81B60))
    isCompleted -> listOf(Color(0xFF2EFF8B), Color(0xFF00B853))
    isUnlocked -> listOf(Color(0xFF3FD0FF), Color(0xFF0082E6))
    else -> listOf(Color(0xFF607D8B), Color(0xFF37474F))
  }
  val lipColor = when {
    isCurrent -> Color(0xFF880E4F)
    isCompleted -> Color(0xFF007B36)
    isUnlocked -> Color(0xFF005199)
    else -> Color(0xFF263238)
  }

  val shape = RoundedCornerShape(22.dp)
  Box(
    modifier = Modifier
      .testTag("level_card_$level")
      .size(width = 96.dp, height = 98.dp)
      .shadow(if (isCurrent) 14.dp else 6.dp, shape, ambientColor = topColors.first().copy(alpha = 0.5f))
      .clip(shape)
      .background(lipColor, shape)
      .clickable(enabled = isUnlocked) { onClick() }
  ) {
    // Raised glossy card face
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(92.dp)
        .background(
          brush = Brush.verticalGradient(topColors),
          shape = shape
        )
        .border(
          width = if (isCurrent) 2.5.dp else 1.5.dp,
          color = if (isCurrent) Color(0xFFFFEE58) else Color.White.copy(alpha = if (isUnlocked) 0.88f else 0.4f),
          shape = shape
        ),
      contentAlignment = Alignment.Center
    ) {
      // Top specular glass highlight band
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth(0.86f)
          .padding(top = 4.dp)
          .height(26.dp)
          .background(
            brush = Brush.verticalGradient(
              listOf(Color.White.copy(alpha = if (isUnlocked) 0.36f else 0.14f), Color.Transparent)
            ),
            shape = RoundedCornerShape(16.dp)
          )
      )

      if (!isUnlocked) {
        Icon(
          Icons.Default.Lock,
          contentDescription = "Locked",
          tint = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.size(30.dp)
        )
      } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          ArcadeOutlinedText(
            text = "$level",
            fontSizeSp = 26f,
            fillColor = Color.White,
            strokeColor = lipColor,
            strokeWidthPx = 6f,
            fontFamily = TitanOneFontFamily,
            fontWeight = FontWeight.Normal
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val starsCount = if (isCompleted) 3 else if (isCurrent) 1 else 0
            for (s in 1..3) {
              Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (s <= starsCount) Color(0xFFFFEE58) else Color.Black.copy(alpha = 0.25f),
                modifier = Modifier.size(15.dp)
              )
            }
          }
        }
      }
    }
  }
}
