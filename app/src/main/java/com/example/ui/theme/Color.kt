package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * FairFare Design System — "Quiet Premium"
 *
 * A restrained jade + graphite palette. Jade signals trust and verification
 * (the core promise of the app); graphite neutrals keep surfaces calm so that
 * fares and verdicts are the only things competing for attention.
 *
 * Every colour used in a composable must come from either
 * MaterialTheme.colorScheme or one of the semantic tokens exposed through
 * LocalFairFareColors. Avoid raw literals in screen code.
 */

// ---------------------------------------------------------------------------
// Light scheme
// ---------------------------------------------------------------------------
val JadePrimaryLight = Color(0xFF0E5C4F)
val JadeOnPrimaryLight = Color(0xFFFFFFFF)
val JadePrimaryContainerLight = Color(0xFFC8EFE2)
val JadeOnPrimaryContainerLight = Color(0xFF00281F)

val SlateSecondaryLight = Color(0xFF4B5B63)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFDBE5EA)
val SlateOnSecondaryContainerLight = Color(0xFF0C1D24)

val BronzeTertiaryLight = Color(0xFF7A5A2E)
val BronzeOnTertiaryLight = Color(0xFFFFFFFF)
val BronzeTertiaryContainerLight = Color(0xFFFBE3BE)
val BronzeOnTertiaryContainerLight = Color(0xFF2A1A00)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val BackgroundLight = Color(0xFFF9FAF9)
val OnBackgroundLight = Color(0xFF14181A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF14181A)
val SurfaceVariantLight = Color(0xFFE6EBE8)
val OnSurfaceVariantLight = Color(0xFF414A47)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F9F8)
val SurfaceContainerLight = Color(0xFFF2F5F3)
val SurfaceContainerHighLight = Color(0xFFECF0EE)
val SurfaceContainerHighestLight = Color(0xFFE6EBE8)

val OutlineLight = Color(0xFF717976)
val OutlineVariantLight = Color(0xFFC4CCC8)
val InverseSurfaceLight = Color(0xFF292E2C)
val InverseOnSurfaceLight = Color(0xFFEFF2F0)
val InversePrimaryLight = Color(0xFF7CD6C0)

// ---------------------------------------------------------------------------
// Dark scheme
// ---------------------------------------------------------------------------
val JadePrimaryDark = Color(0xFF7CD6C0)
val JadeOnPrimaryDark = Color(0xFF003C31)
val JadePrimaryContainerDark = Color(0xFF005747)
val JadeOnPrimaryContainerDark = Color(0xFF98F3DC)

val SlateSecondaryDark = Color(0xFFB4C7CE)
val SlateOnSecondaryDark = Color(0xFF1E3238)
val SlateSecondaryContainerDark = Color(0xFF34484F)
val SlateOnSecondaryContainerDark = Color(0xFFD0E3EA)

val BronzeTertiaryDark = Color(0xFFE5C28E)
val BronzeOnTertiaryDark = Color(0xFF422C05)
val BronzeTertiaryContainerDark = Color(0xFF5D421A)
val BronzeOnTertiaryContainerDark = Color(0xFFFFDEA8)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

val BackgroundDark = Color(0xFF101413)
val OnBackgroundDark = Color(0xFFDFE4E1)
val SurfaceDark = Color(0xFF101413)
val OnSurfaceDark = Color(0xFFDFE4E1)
val SurfaceVariantDark = Color(0xFF3F4945)
val OnSurfaceVariantDark = Color(0xFFBEC9C4)

val SurfaceContainerLowestDark = Color(0xFF0B0F0E)
val SurfaceContainerLowDark = Color(0xFF181D1B)
val SurfaceContainerDark = Color(0xFF1C2120)
val SurfaceContainerHighDark = Color(0xFF262B29)
val SurfaceContainerHighestDark = Color(0xFF313634)

val OutlineDark = Color(0xFF899390)
val OutlineVariantDark = Color(0xFF3F4945)
val InverseSurfaceDark = Color(0xFFDFE4E1)
val InverseOnSurfaceDark = Color(0xFF2D3230)
val InversePrimaryDark = Color(0xFF0E5C4F)

// ---------------------------------------------------------------------------
// Semantic fairness tokens
//
// These encode the overcharge verdict scale. Exposed through [FairFareColors]
// so dark mode gets legible variants instead of light-mode pastels.
// ---------------------------------------------------------------------------
val FairGreen = Color(0xFF0F7A44)
val FairGreenLight = Color(0xFFD3F2DF)
val FairGreenText = Color(0xFF04321B)

val SlightlyHighAmber = Color(0xFFA1650B)
val SlightlyHighAmberLight = Color(0xFFFBEBCC)
val SlightlyHighAmberText = Color(0xFF3F2703)

val HighOrange = Color(0xFFB4501A)
val HighOrangeLight = Color(0xFFFCE3D4)
val HighOrangeText = Color(0xFF451D05)

val VeryHighRed = Color(0xFFB3261E)
val VeryHighRedLight = Color(0xFFFADCD9)
val VeryHighRedText = Color(0xFF410E0B)

val FairGreenDark = Color(0xFF74DBA0)
val FairGreenContainerDark = Color(0xFF10402A)
val FairGreenOnContainerDark = Color(0xFFB9F2CE)

val SlightlyHighAmberDark = Color(0xFFEEC073)
val SlightlyHighAmberContainerDark = Color(0xFF463108)
val SlightlyHighAmberOnContainerDark = Color(0xFFFBE0B0)

val HighOrangeDark = Color(0xFFF0A97F)
val HighOrangeContainerDark = Color(0xFF4A2410)
val HighOrangeOnContainerDark = Color(0xFFFBD6BF)

val VeryHighRedDark = Color(0xFFF2B8B5)
val VeryHighRedContainerDark = Color(0xFF4A1613)
val VeryHighRedOnContainerDark = Color(0xFFF9DEDC)

// Journey markers: origin reads as "go", destination as "stop".
val OriginMarkerLight = Color(0xFF0F7A44)
val OriginMarkerDark = Color(0xFF74DBA0)
val DestinationMarkerLight = Color(0xFFB3261E)
val DestinationMarkerDark = Color(0xFFF2B8B5)

// Comparison badges
val BadgeCheapestBg = FairGreenLight
val BadgeCheapestText = FairGreenText
val BadgeFastestBg = Color(0xFFD6E4F7)
val BadgeFastestText = Color(0xFF0B2C52)
val BadgeBestValueBg = Color(0xFFE6E0F3)
val BadgeBestValueText = Color(0xFF241B3D)

val BadgeCheapestBgDark = FairGreenContainerDark
val BadgeCheapestTextDark = FairGreenOnContainerDark
val BadgeFastestBgDark = Color(0xFF17324F)
val BadgeFastestTextDark = Color(0xFFCFE1F8)
val BadgeBestValueBgDark = Color(0xFF2B2545)
val BadgeBestValueTextDark = Color(0xFFE1DAF5)

// Traffic flow scale, shared by the map canvas and corridor cards.
val TrafficFreeFlow = Color(0xFF1B9C5B)
val TrafficModerate = Color(0xFFD9A21B)
val TrafficHeavy = Color(0xFFDD7A2B)
val TrafficSevere = Color(0xFFC6362D)

// ---------------------------------------------------------------------------
// Monochrome & AMOLED Black & White Palettes
// ---------------------------------------------------------------------------
val MonoBlack = Color(0xFF000000)
val MonoPitchDark = Color(0xFF000000)
val MonoSurfaceDark = Color(0xFF0A0A0A)
val MonoSurfaceContainerLowDark = Color(0xFF121212)
val MonoSurfaceContainerDark = Color(0xFF181818)
val MonoSurfaceContainerHighDark = Color(0xFF222222)
val MonoSurfaceContainerHighestDark = Color(0xFF2E2E2E)

val MonoWhite = Color(0xFFFFFFFF)
val MonoPureWhite = Color(0xFFFFFFFF)
val MonoSurfaceLight = Color(0xFFFFFFFF)
val MonoSurfaceContainerLowLight = Color(0xFFF7F7F7)
val MonoSurfaceContainerLight = Color(0xFFF0F0F0)
val MonoSurfaceContainerHighLight = Color(0xFFE8E8E8)
val MonoSurfaceContainerHighestLight = Color(0xFFDFDFDF)

val MonoGrey100 = Color(0xFFF5F5F5)
val MonoGrey200 = Color(0xFFEEEEEE)
val MonoGrey300 = Color(0xFFE0E0E0)
val MonoGrey400 = Color(0xFFBDBDBD)
val MonoGrey500 = Color(0xFF9E9E9E)
val MonoGrey600 = Color(0xFF757575)
val MonoGrey700 = Color(0xFF616161)
val MonoGrey800 = Color(0xFF424242)
val MonoGrey900 = Color(0xFF212121)

val MonoBorderDark = Color(0xFF2C2C2E)
val MonoBorderLight = Color(0xFFD1D1D6)



