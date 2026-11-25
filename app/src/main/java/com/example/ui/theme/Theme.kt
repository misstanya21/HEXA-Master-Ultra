package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArcadeDarkColorScheme = darkColorScheme(
  primary = HexaElectricCyan,
  onPrimary = HexaDeepNavy,
  secondary = HexaMagentaGlow,
  onSecondary = Color.White,
  tertiary = HexaGold,
  onTertiary = HexaDeepNavy,
  background = HexaMidnightBg,
  onBackground = Color.White,
  surface = HexaSurfaceCard,
  onSurface = Color.White
)

private val ArcadeLightColorScheme = lightColorScheme(
  primary = HexaRoyalBlue,
  onPrimary = Color.White,
  secondary = HexaMagentaGlow,
  onSecondary = Color.White,
  tertiary = HexaAmber,
  onTertiary = Color.White,
  background = HexaFrostWhite,
  onBackground = HexaDeepNavy,
  surface = Color.White,
  onSurface = HexaDeepNavy
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ArcadeDarkColorScheme else ArcadeLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
