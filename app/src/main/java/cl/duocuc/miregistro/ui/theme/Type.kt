package cl.duocuc.miregistro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Tipografia = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle( //text para los botones
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.3.sp
    )
)

val TipografiaGrande = Tipografia.copy(
    titleLarge = Tipografia.titleLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
    titleMedium = Tipografia.titleMedium.copy(fontSize = 21.sp, lineHeight = 27.sp),
    bodyMedium = Tipografia.bodyMedium.copy(fontSize = 19.sp, lineHeight = 26.sp),
    bodyLarge = Tipografia.bodyLarge.copy(fontSize = 20.sp, lineHeight = 27.sp),
    labelLarge = Tipografia.labelLarge.copy(fontSize = 18.sp)
)