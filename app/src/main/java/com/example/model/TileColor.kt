package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Professional 3D tile colors with high-fidelity faceted lighting and
 * complete high-contrast colorblind accessibility numerical symbols:
 * - topColor: main surface
 * - lightColor: specular highlight
 * - sideColor: front facet mid-tone
 * - darkSideColor: shadow facet
 * - lightSideColor: illuminated facet
 * - symbol: accessibility shape symbol (▲, ♥, ★, ◆, ⬡, ●, ■, ✚)
 * - colorId: 1-indexed numerical identifier for the color (1..8)
 * - numericalSymbol: combined high-contrast string (e.g. "▲1", "♥2", etc.)
 */
enum class TileColor(
  val displayName: String,
  val topColor: Color,
  val sideColor: Color,
  val lightColor: Color,
  val darkSideColor: Color,
  val lightSideColor: Color,
  val symbol: String,
  val colorId: Int,
  val textColor: Color = Color.White
) {
  GREEN(
    displayName = "Green",
    topColor = Color(0xFF00E676),
    sideColor = Color(0xFF00A844),
    lightColor = Color(0xFF69F0AE),
    darkSideColor = Color(0xFF007E33),
    lightSideColor = Color(0xFF43A047),
    symbol = "▲",
    colorId = 1
  ),
  MAGENTA(
    displayName = "Magenta",
    topColor = Color(0xFFFF2D87),
    sideColor = Color(0xFFC2185B),
    lightColor = Color(0xFFFF60B0),
    darkSideColor = Color(0xFF880E4F),
    lightSideColor = Color(0xFFD81B60),
    symbol = "♥",
    colorId = 2
  ),
  YELLOW(
    displayName = "Yellow",
    topColor = Color(0xFFFFD600),
    sideColor = Color(0xFFFF9100),
    lightColor = Color(0xFFFFF176),
    darkSideColor = Color(0xFFE65100),
    lightSideColor = Color(0xFFFFA000),
    symbol = "★",
    colorId = 3,
    textColor = Color(0xFF3E2723)
  ),
  BLUE(
    displayName = "Blue",
    topColor = Color(0xFF00A0FF),
    sideColor = Color(0xFF0066CC),
    lightColor = Color(0xFF40C4FF),
    darkSideColor = Color(0xFF004BA0),
    lightSideColor = Color(0xFF1976D2),
    symbol = "◆",
    colorId = 4
  ),
  PURPLE(
    displayName = "Purple",
    topColor = Color(0xFFAA00FF),
    sideColor = Color(0xFF6A0080),
    lightColor = Color(0xFFE040FB),
    darkSideColor = Color(0xFF4A148C),
    lightSideColor = Color(0xFF7B1FA2),
    symbol = "⬡",
    colorId = 5
  ),
  CYAN(
    displayName = "Cyan",
    topColor = Color(0xFF00E5FF),
    sideColor = Color(0xFF0097A7),
    lightColor = Color(0xFF84FFFF),
    darkSideColor = Color(0xFF006064),
    lightSideColor = Color(0xFF00ACC1),
    symbol = "●",
    colorId = 6,
    textColor = Color(0xFF004D40)
  ),
  ORANGE(
    displayName = "Orange",
    topColor = Color(0xFFFF6D00),
    sideColor = Color(0xFFBF360C),
    lightColor = Color(0xFFFFA726),
    darkSideColor = Color(0xFF870000),
    lightSideColor = Color(0xFFE64A19),
    symbol = "■",
    colorId = 7
  ),
  PEACH(
    displayName = "Peach",
    topColor = Color(0xFFFF80AB),
    sideColor = Color(0xFFC51162),
    lightColor = Color(0xFFFFB2DD),
    darkSideColor = Color(0xFF880E4F),
    lightSideColor = Color(0xFFE91E63),
    symbol = "✚",
    colorId = 8
  );

  val numericalSymbol: String
    get() = "$symbol$colorId"

  val fullBadgeText: String
    get() = "$symbol $colorId"

  companion object {
    fun fromColorId(id: Int): TileColor? = entries.find { it.colorId == id }

    fun randomEasy(): TileColor {
      val easyList = listOf(GREEN, MAGENTA, YELLOW, BLUE)
      return easyList.random()
    }

    fun randomMedium(): TileColor {
      val mediumList = listOf(GREEN, MAGENTA, YELLOW, BLUE, PURPLE, CYAN)
      return mediumList.random()
    }

    fun randomAll(): TileColor {
      return entries.random()
    }
  }
}
