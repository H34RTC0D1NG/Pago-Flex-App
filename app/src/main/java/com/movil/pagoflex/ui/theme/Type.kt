package com.movil.pagoflex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.3.sp)
)

val TypographyGrande = Typography.copy(
    titleLarge = Typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
    titleMedium = Typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 27.sp),
    bodyMedium = Typography.bodyMedium.copy(fontSize = 19.sp, lineHeight = 26.sp),
    bodyLarge = Typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 27.sp),
    labelLarge = Typography.labelLarge.copy(fontSize = 18.sp)
)
