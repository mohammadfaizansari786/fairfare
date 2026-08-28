package com.example.engine

import com.example.data.model.GeoPoint
import com.example.data.model.RouteWaypoint
import com.example.data.model.TrafficLevel
import com.example.data.model.TrafficRouteOption
import com.example.data.model.TrafficSegment
import com.example.data.remote.RoadRoute
import kotlin.math.roundToInt

/**
 * Converts routing-provider results into the app's corridor model.
 *
 * [TrafficRouteEngine]'s synthetic-curve generator stays as the offline fallback,
 * but when real geometry is available this is what the map draws — so the line
 * follows roads instead of arcing over them.
 */
object RoadCorridorMapper {

    /**
     * Maps provider routes to corridor options.
     *
     * Ranking is derived from the data rather than assumed: the fastest becomes the
     * recommended option, and each alternative is labelled by how it actually
     * differs (shorter, or calmer) instead of asserting a fixed set of three named
     * corridors that may not match what the router returned.
     */
    fun toCorridors(
        roadRoutes: List<RoadRoute>,
        fromName: String,
        toName: String,
        tariffs: List<com.example.data.model.TariffEntity> = emptyList(),
        autoFarePerKm: Double = 11.0,
        cabFarePerKm: Double = 16.0,
        busFlatFare: Double = 15.0
    ): List<TrafficRouteOption> {
        if (roadRoutes.isEmpty()) return emptyList()

        val fastest = roadRoutes.minByOrNull { it.durationMinutes }
        val shortest = roadRoutes.minByOrNull { it.distanceKm }
        val calmest = roadRoutes.minByOrNull { it.congestionPercent() }

        val autoTariff = tariffs.firstOrNull { it.transportType == com.example.data.model.TransportType.AUTO_RICKSHAW }
        val cabTariff = tariffs.firstOrNull { it.transportType == com.example.data.model.TransportType.CAB_MINI }
            ?: tariffs.firstOrNull { it.transportType == com.example.data.model.TransportType.CAB_SEDAN }
        val busTariff = tariffs.firstOrNull { it.transportType == com.example.data.model.TransportType.BUS }

        return roadRoutes.mapIndexed { index, route ->
            val isFastest = route === fastest
            val isShortest = route === shortest && !isFastest
            val isCalmest = route === calmest && !isFastest && !isShortest

            val autoFare = if (autoTariff != null) {
                FareCalculatorEngine.calculateFare(autoTariff, route.distanceKm).estimatedFare
            } else {
                round1(25.0 + kotlin.math.max(0.0, route.distanceKm - 1.5) * autoFarePerKm)
            }

            val cabFare = if (cabTariff != null) {
                FareCalculatorEngine.calculateFare(cabTariff, route.distanceKm).estimatedFare
            } else {
                round1(50.0 + kotlin.math.max(0.0, route.distanceKm - 2.0) * cabFarePerKm)
            }

            val busFare = if (busTariff != null) {
                FareCalculatorEngine.calculateFare(busTariff, route.distanceKm).estimatedFare
            } else {
                FareCalculatorEngine.calculateBusFare(route.distanceKm, "Lucknow")
            }

            TrafficRouteOption(
                id = "road_route_$index",
                title = route.describe(fromName, toName),
                subtitle = route.subtitle(),
                tag = when {
                    isFastest -> "Fastest route"
                    isShortest -> "Shortest route"
                    isCalmest -> "Lowest congestion"
                    else -> "Alternative route"
                },
                isRecommended = isFastest,
                distanceKm = round1(route.distanceKm),
                baseDurationMinutes = route.freeFlowMinutes,
                trafficDelayMinutes = route.delayMinutes,
                totalDurationMinutes = route.durationMinutes,
                overallTraffic = route.overallLevel(),
                congestionPercentage = route.congestionPercent(),
                estimatedAutoFare = autoFare,
                estimatedCabFare = cabFare,
                estimatedBusFare = busFare,
                segments = route.toSegments(),
                waypoints = route.toWaypoints(fromName, toName),
                roadConditions = route.roadConditions(),
                geoPoints = route.geometry,
                startPoint = route.geometry.first(),
                endPoint = route.geometry.last()
            )
        }
    }

    private fun round1(value: Double): Double = Math.round(value * 10.0) / 10.0

    private fun RoadRoute.describe(fromName: String, toName: String): String {
        val via = congestion
            .maxByOrNull { it.endFraction - it.startFraction }
            ?.let { span ->
                when (span.level) {
                    TrafficLevel.SEVERE, TrafficLevel.HEAVY -> "busy stretch"
                    TrafficLevel.MODERATE -> "steady traffic"
                    TrafficLevel.FREE_FLOW -> null
                }
            }
        return if (via != null) "$fromName → $toName · $via" else "$fromName → $toName"
    }

    private fun RoadRoute.subtitle(): String = when {
        delayMinutes >= 10 -> "Heavy delays right now"
        delayMinutes >= 4 -> "Some delay on this route"
        else -> "Clear run expected"
    }

    /** Share of the route length sitting in non-free-flowing traffic. */
    private fun RoadRoute.congestionPercent(): Int {
        val congested = congestion
            .filter { it.level != TrafficLevel.FREE_FLOW }
            .sumOf { (it.endFraction - it.startFraction).toDouble() }
        return (congested * 100).roundToInt().coerceIn(0, 100)
    }

    /**
     * Worst level covering a meaningful share of the route.
     *
     * A 50 m jam should not label an entire 20 km corridor as severe, so spans under
     * 8% of the route are ignored for the headline figure.
     */
    private fun RoadRoute.overallLevel(): TrafficLevel {
        val significant = congestion.filter { (it.endFraction - it.startFraction) >= 0.08f }
        return significant.maxByOrNull { it.level.ordinal }?.level ?: TrafficLevel.FREE_FLOW
    }

    private fun RoadRoute.toSegments(): List<TrafficSegment> =
        congestion.map { span ->
            TrafficSegment(
                name = span.level.label,
                lengthKm = round1(distanceKm * (span.endFraction - span.startFraction)),
                trafficLevel = span.level,
                startPercent = span.startFraction,
                endPercent = span.endFraction
            )
        }

    private fun RoadRoute.roadConditions(): String = when (overallLevel()) {
        TrafficLevel.FREE_FLOW -> "Free-flowing along the whole route"
        TrafficLevel.MODERATE -> "Moderate traffic on parts of this route"
        TrafficLevel.HEAVY -> "Heavy traffic on a significant stretch"
        TrafficLevel.SEVERE -> "Severe congestion; expect stop-start driving"
    }

    /**
     * Waypoints at the origin, the worst congestion point, and the destination.
     *
     * Positioned from real geometry, so each marker sits on the road rather than at
     * an arbitrary fraction of a bounding box.
     */
    private fun RoadRoute.toWaypoints(fromName: String, toName: String): List<RouteWaypoint> {
        val result = mutableListOf(
            RouteWaypoint(
                title = fromName,
                subtitle = "Start",
                distanceRatio = 0f,
                trafficLevel = TrafficLevel.FREE_FLOW,
                etaMinutes = 0,
                geoPoint = geometry.first()
            )
        )

        congestion
            .filter { it.level == TrafficLevel.HEAVY || it.level == TrafficLevel.SEVERE }
            .maxByOrNull { it.endFraction - it.startFraction }
            ?.let { worst ->
                val midpoint = (worst.startFraction + worst.endFraction) / 2f
                result.add(
                    RouteWaypoint(
                        title = "Congestion",
                        subtitle = worst.level.label,
                        distanceRatio = midpoint,
                        trafficLevel = worst.level,
                        etaMinutes = (durationMinutes * midpoint).roundToInt(),
                        isIncident = true,
                        incidentDescription = "Slow moving traffic",
                        geoPoint = geometry.pointAt(midpoint)
                    )
                )
            }

        result.add(
            RouteWaypoint(
                title = toName,
                subtitle = "Destination",
                distanceRatio = 1f,
                trafficLevel = TrafficLevel.FREE_FLOW,
                etaMinutes = durationMinutes,
                geoPoint = geometry.last()
            )
        )

        return result
    }

    private fun List<GeoPoint>.pointAt(fraction: Float): GeoPoint {
        if (isEmpty()) return GeoPoint(0.0, 0.0)
        if (size == 1) return first()
        val scaled = fraction.coerceIn(0f, 1f) * (size - 1)
        val index = scaled.toInt().coerceIn(0, size - 2)
        val t = scaled - index
        val a = this[index]
        val b = this[index + 1]
        return GeoPoint(
            latitude = a.latitude + (b.latitude - a.latitude) * t,
            longitude = a.longitude + (b.longitude - a.longitude) * t
        )
    }
}
