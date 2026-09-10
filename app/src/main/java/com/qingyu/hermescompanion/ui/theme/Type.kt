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
    headlineSmall = appText(FontWeight.SemiBold, 18.sp, 26.sp),
    titleLarge = appText(FontWeight.SemiBold, 20.sp, 28.sp),
    titleMedium = appText(FontWeight.SemiBold, 16.sp, 23.sp),
    titleSmall = appText(FontWeight.Medium, 14.sp, 21.sp),
    bodyLarge = appText(FontWeight.Normal, 16.sp, 25.sp),
    bodyMedium = appText(FontWeight.Normal, 14.sp, 21.sp),
    bodySmall = appText(FontWeight.Normal, 13.sp, 20.sp),
    labelLarge = appText(FontWeight.Medium, 14.sp, 20.sp),
    labelMedium = appText(FontWeight.Medium, 12.sp, 18.sp),
    labelSmall = appText(FontWeight.Normal, 11.sp, 16.sp),
)
