package com.example.data.remote

import com.example.data.model.TrafficLevel

/*
 * Provider-specific congestion mapping.
 *
 * Kept separate from RoutingRepository so the wire formats stay isolated from the
 * app's own TrafficLevel scale.
 */

/**
 * TomTom traffic sections -> fractional congestion spans.
 *
 * `magnitudeOfDelay` is TomTom's 0..4 severity scale. Indices are positions in the
 * flattened point list, which is why [totalPoints] is needed to express them as
 * fractions the renderer can slice with.
 */
internal fun List<TomTomSection>.toCongestionSpans(totalPoints: Int): List<CongestionSpan> {
    if (totalPoints < 2) return emptyList()
    val lastIndex = (totalPoints - 1).toFloat()

    return mapNotNull { section ->
        // Only traffic sections carry a delay magnitude; ignore the rest
        // (country crossings, toll roads, carpool lanes and so on).
        if (!section.sectionType.equals("TRAFFIC", ignoreCase = true)) return@mapNotNull null

        val level = when (section.magnitudeOfDelay) {
            0 -> TrafficLevel.FREE_FLOW
            1 -> TrafficLevel.MODERATE
            2 -> TrafficLevel.HEAVY
            3, 4 -> TrafficLevel.SEVERE
            else -> return@mapNotNull null
        }

        val start = (section.startPointIndex / lastIndex).coerceIn(0f, 1f)
        val end = (section.endPointIndex / lastIndex).coerceIn(0f, 1f)
        if (end <= start) return@mapNotNull null

        CongestionSpan(startFraction = start, endFraction = end, level = level)
    }
}

/**
 * OSRM speed annotations -> fractional congestion spans.
 *
 * OSRM reports free-flow speeds only, so this is not live traffic. What it does
 * capture is road class: a 12 km/h residential lane genuinely is slower going than
 * a 60 km/h arterial, and showing that is more honest than colouring the whole
 * route one shade. Thresholds are chosen for Indian urban driving.
 *
 * Adjacent vertices at the same level are merged, otherwise a 200-vertex route
 * would produce 200 one-vertex spans and the renderer would stroke each separately.
 */
internal fun OsrmAnnotation.toCongestionSpans(totalPoints: Int): List<CongestionSpan> {
    if (speed.isEmpty() || totalPoints < 2) return emptyList()

    val lastIndex = (totalPoints - 1).toFloat()
    val levels = speed.map { metresPerSecond ->
        when (val kmh = metresPerSecond * 3.6) {
            in 0.0..12.0 -> TrafficLevel.SEVERE
            in 12.0..22.0 -> TrafficLevel.HEAVY
            in 22.0..34.0 -> TrafficLevel.MODERATE
            else -> if (kmh.isNaN()) TrafficLevel.MODERATE else TrafficLevel.FREE_FLOW
        }
    }

    val spans = mutableListOf<CongestionSpan>()
    var runStart = 0
    var runLevel = levels.first()

    fun closeRun(endExclusive: Int) {
        if (runLevel == TrafficLevel.FREE_FLOW) return
        val start = (runStart / lastIndex).coerceIn(0f, 1f)
        val end = (endExclusive / lastIndex).coerceIn(0f, 1f)
        if (end > start) {
            spans.add(CongestionSpan(start, end, runLevel))
        }
    }

    for (index in 1 until levels.size) {
        if (levels[index] != runLevel) {
            closeRun(index)
            runStart = index
            runLevel = levels[index]
        }
    }
    closeRun(levels.size)

    return spans
}
