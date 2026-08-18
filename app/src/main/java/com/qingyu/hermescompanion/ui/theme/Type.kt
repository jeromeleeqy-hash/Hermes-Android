package com.qingyu.hermescompanion.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppSans = FontFamily.SansSerif

private fun appText(weight: FontWeight, size: androidx.compose.ui.unit.TextUnit, line: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = AppSans,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
)

val HermesTypography = Typography(
    headlineLarge = appText(FontWeight.SemiBold, 25.sp, 32.sp),
    headlineMedium = appText(FontWeight.SemiBold, 20.sp, 27.sp),
    headlineSmall = appText(FontWeight.SemiBold, 17.sp, 23.sp),
    titleLarge = appText(FontWeight.SemiBold, 17.sp, 23.sp),
    titleMedium = appText(FontWeight.SemiBold, 14.5.sp, 20.sp),
    titleSmall = appText(FontWeight.Medium, 13.sp, 18.sp),
    bodyLarge = appText(FontWeight.Normal, 14.5.sp, 21.sp),
    bodyMedium = appText(FontWeight.Normal, 13.sp, 19.sp),
    bodySmall = appText(FontWeight.Normal, 12.sp, 17.sp),
    labelLarge = appText(FontWeight.Medium, 12.sp, 17.sp),
    labelMedium = appText(FontWeight.Medium, 11.5.sp, 16.sp),
    labelSmall = appText(FontWeight.Normal, 10.5.sp, 15.sp),
)
