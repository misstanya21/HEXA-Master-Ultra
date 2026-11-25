package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TitanOneFontFamily = FontFamily(
  Font(R.font.titan_one, FontWeight.Normal),
  Font(R.font.titan_one, FontWeight.Bold),
  Font(R.font.titan_one, FontWeight.Black)
)

val RubikFontFamily = FontFamily(
  Font(R.font.rubik, FontWeight.Normal),
  Font(R.font.rubik, FontWeight.Medium),
  Font(R.font.rubik, FontWeight.SemiBold),
  Font(R.font.rubik, FontWeight.Bold),
  Font(R.font.rubik, FontWeight.ExtraBold),
  Font(R.font.rubik, FontWeight.Black)
)

val FredokaFontFamily = FontFamily(
  Font(R.font.fredoka, FontWeight.Normal),
  Font(R.font.fredoka, FontWeight.Medium),
  Font(R.font.fredoka, FontWeight.SemiBold),
  Font(R.font.fredoka, FontWeight.Bold),
  Font(R.font.fredoka, FontWeight.ExtraBold),
  Font(R.font.fredoka, FontWeight.Black)
)

val NunitoFontFamily = FontFamily(
  Font(R.font.nunito, FontWeight.Normal),
  Font(R.font.nunito, FontWeight.Medium),
  Font(R.font.nunito, FontWeight.SemiBold),
  Font(R.font.nunito, FontWeight.Bold),
  Font(R.font.nunito, FontWeight.ExtraBold),
  Font(R.font.nunito, FontWeight.Black)
)

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = TitanOneFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = 0.5.sp
  ),
  headlineMedium = TextStyle(
    fontFamily = RubikFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.sp
  ),
  titleLarge = TextStyle(
    fontFamily = FredokaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
  ),
  titleMedium = TextStyle(
    fontFamily = RubikFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.15.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = NunitoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.25.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = NunitoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.2.sp
  ),
  labelLarge = TextStyle(
    fontFamily = RubikFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.3.sp
  ),
  labelSmall = TextStyle(
    fontFamily = NunitoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.3.sp
  )
)
