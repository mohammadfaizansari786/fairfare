package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Type scale.
 *
 * Three jobs only:
 *   display/headline -> fare amounts and screen titles
 *   title            -> section and card headers
 *   body/label       -> supporting copy, metadata, buttons, badges
 *
 * Screens reference MaterialTheme.typography rather than hard-coding fontSize and
 * fontWeight, so density stays consistent and text scales with the user's font
 * size preference.
 */

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp
)

val Typography = Typography(
    displayLarge = style(44, 52, FontWeight.ExtraBold, -1.0),
    displayMedium = style(36, 44, FontWeight.ExtraBold, -0.8),
    displaySmall = style(30, 38, FontWeight.Bold, -0.5),

    headlineLarge = style(28, 36, FontWeight.Bold, -0.4),
    headlineMedium = style(24, 32, FontWeight.Bold, -0.2),
    headlineSmall = style(21, 28, FontWeight.SemiBold, 0.0),

    titleLarge = style(19, 26, FontWeight.SemiBold, 0.0),
    titleMedium = style(16, 22, FontWeight.SemiBold, 0.1),
    titleSmall = style(14, 20, FontWeight.SemiBold, 0.1),

    bodyLarge = style(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = style(14, 20, FontWeight.Normal, 0.2),
    bodySmall = style(12, 17, FontWeight.Normal, 0.3),

    labelLarge = style(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = style(11, 15, FontWeight.Medium, 0.5)
)
