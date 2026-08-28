package com.example.ui.util

import java.util.Locale

/*
 * Centralised formatting.
 *
 * Two reasons this exists rather than inlining String.format at call sites:
 *  1. String.format without an explicit Locale uses the device default, which
 *     produces "16,4 km" on comma-decimal locales and can break parsing.
 *     Everything here pins Locale.US for numeric output.
 *  2. Currency and distance rendering was previously duplicated across every
 *     screen with slightly different rounding, so the same fare could read as
 *     ₹119 in one card and ₹120 in another.
 */

private val NUMERIC_LOCALE: Locale = Locale.US

/** Rupee amount, no decimals — fares are never quoted in paise. */
fun formatRupees(amount: Double): String =
    "₹" + String.format(NUMERIC_LOCALE, "%,d", amount.toSafeRoundedInt())

/** Inclusive fare range, e.g. "₹95 – ₹110". Collapses when both ends match. */
fun formatRupeeRange(min: Double, max: Double): String {
    val low = min.toSafeRoundedInt()
    val high = max.toSafeRoundedInt()
    return if (low == high) {
        formatRupees(min)
    } else {
        "${formatRupees(min)} – ${formatRupees(max)}"
    }
}

/** Distance with a single decimal, e.g. "16.4 km". */
fun formatKm(distanceKm: Double): String =
    String.format(NUMERIC_LOCALE, "%.1f km", distanceKm.orZeroIfNotFinite())

/** Coordinate pair for debug / tap-to-pin labels. */
fun formatCoordinates(latitude: Double, longitude: Double): String =
    String.format(NUMERIC_LOCALE, "%.4f, %.4f", latitude, longitude)

/** Signed percentage, e.g. "+34%" or "-12%". */
fun formatSignedPercent(percent: Double): String {
    val rounded = percent.toSafeRoundedInt()
    return if (rounded > 0) "+$rounded%" else "$rounded%"
}

/**
 * Duration in a compact human form: "8 min", "1 h 05 min".
 * Journeys are never displayed in seconds, so minutes is the smallest unit.
 */
fun formatDuration(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceAtLeast(0)
    return if (safeMinutes < 60) {
        "$safeMinutes min"
    } else {
        val hours = safeMinutes / 60
        val minutes = safeMinutes % 60
        if (minutes == 0) {
            "$hours h"
        } else {
            String.format(NUMERIC_LOCALE, "%d h %02d min", hours, minutes)
        }
    }
}

/** Relative timestamp for trip history and community reports. */
fun formatRelativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val deltaMinutes = ((nowMillis - timestampMillis).coerceAtLeast(0L)) / 60_000L
    return when {
        deltaMinutes < 1L -> "Just now"
        deltaMinutes < 60L -> "$deltaMinutes min ago"
        deltaMinutes < 1_440L -> "${deltaMinutes / 60L} h ago"
        deltaMinutes < 10_080L -> "${deltaMinutes / 1_440L} d ago"
        else -> "${deltaMinutes / 10_080L} w ago"
    }
}

/**
 * NaN and infinity used to reach the UI as "NaN km" when a route had no valid
 * coordinates. Clamp defensively at the formatting boundary.
 */
private fun Double.orZeroIfNotFinite(): Double = if (isFinite()) this else 0.0

private fun Double.toSafeRoundedInt(): Int =
    if (isFinite()) Math.round(this).coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt() else 0
