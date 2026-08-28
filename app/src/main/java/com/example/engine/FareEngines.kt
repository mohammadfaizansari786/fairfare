package com.example.engine

import com.example.data.model.FareCalculationResult
import com.example.data.model.MultiModalRoute
import com.example.data.model.OverchargeAnalysis
import com.example.data.model.OverchargeCategory
import com.example.data.model.RouteStep
import com.example.data.model.TariffEntity
import com.example.data.model.TransportType
import com.example.data.model.VerificationStatus
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

object FareCalculatorEngine {

    fun isNightTime(nightStartHour: Int = 23, nightEndHour: Int = 5): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (nightStartHour > nightEndHour) {
            currentHour >= nightStartHour || currentHour < nightEndHour
        } else {
            currentHour in nightStartHour until nightEndHour
        }
    }

    fun calculateFare(
        tariff: TariffEntity,
        distanceKm: Double,
        waitingMinutes: Int = 0,
        luggageCount: Int = 0,
        forceNightMode: Boolean? = null,
        extraTollsOrParking: Double = 0.0
    ): FareCalculationResult {
        val safeDistance = max(0.5, distanceKm)
        val isNight = forceNightMode ?: isNightTime(tariff.nightStartHour, tariff.nightEndHour)

        // Specialised Metro Calculation with official statutory distance/station slabs
        if (tariff.transportType == TransportType.METRO) {
            val metroFare = calculateMetroFare(safeDistance, tariff.city)
            val speedKmh = 38.0
            val travelMinutes = max(3, round((safeDistance / speedKmh) * 60).toInt())
            val notes = "Official Metro distance slab: ₹${metroFare.toInt()} for ${String.format("%.1f", safeDistance)} km. Air-conditioned coaches, fixed timetable, zero traffic congestion."

            return FareCalculationResult(
                transportType = TransportType.METRO,
                estimatedFare = metroFare,
                fareRangeMin = metroFare,
                fareRangeMax = metroFare,
                distanceKm = safeDistance,
                estimatedTimeMinutes = travelMinutes,
                baseFare = min(10.0, metroFare),
                distanceCharge = max(0.0, metroFare - 10.0),
                waitingCharge = 0.0,
                nightCharge = 0.0,
                luggageCharge = 0.0,
                extraCharges = 0.0,
                isNightApplied = false,
                officialSource = tariff.officialSource,
                verificationStatus = tariff.verificationStatus,
                calculationNotes = notes
            )
        }

        // Specialised Municipal Bus Stage Slab Calculation
        if (tariff.transportType == TransportType.BUS) {
            val busFare = calculateBusFare(safeDistance, tariff.city)
            val speedKmh = 22.0
            val travelMinutes = max(6, round((safeDistance / speedKmh) * 60).toInt())
            val notes = "Official municipal stage bus fare: ₹${busFare.toInt()} for ${String.format("%.1f", safeDistance)} km."

            return FareCalculationResult(
                transportType = TransportType.BUS,
                estimatedFare = busFare,
                fareRangeMin = busFare,
                fareRangeMax = busFare,
                distanceKm = safeDistance,
                estimatedTimeMinutes = travelMinutes,
                baseFare = min(10.0, busFare),
                distanceCharge = max(0.0, busFare - 10.0),
                waitingCharge = 0.0,
                nightCharge = 0.0,
                luggageCharge = 0.0,
                extraCharges = 0.0,
                isNightApplied = false,
                officialSource = tariff.officialSource,
                verificationStatus = tariff.verificationStatus,
                calculationNotes = notes
            )
        }

        // 1. Base fare & Distance charge
        val baseFare = tariff.baseFare
        val extraDistance = max(0.0, safeDistance - tariff.baseDistanceKm)
        val distanceCharge = extraDistance * tariff.perKmRate

        // 2. Waiting charge
        val waitingCharge = (waitingMinutes.toDouble() / 60.0) * tariff.waitingRatePerHour

        // 3. Subtotal before night charge
        val subtotal = baseFare + distanceCharge + waitingCharge

        // 4. Night surcharge
        val nightCharge = if (isNight) {
            subtotal * (tariff.nightChargePercent / 100.0)
        } else {
            0.0
        }

        // 5. Luggage charge
        val luggageCharge = max(0, luggageCount - 1) * tariff.luggageRatePerItem // 1 piece free

        // 6. Total
        val totalFareRaw = subtotal + nightCharge + luggageCharge + extraTollsOrParking
        val estimatedFare = roundToRupee(totalFareRaw)

        // Reasonable range based on traffic density & meter tolerance
        val minRange = roundToRupee(max(tariff.minFare, estimatedFare * 0.95))
        val maxRange = roundToRupee(estimatedFare * 1.10)

        // Speed & ETA
        val speedKmh = tariff.transportType.defaultSpeedKmh
        val travelMinutes = max(4, round((safeDistance / speedKmh) * 60).toInt() + (waitingMinutes / 2))

        val notes = buildString {
            append("Base: ₹${tariff.baseFare.toInt()} for first ${tariff.baseDistanceKm} km. ")
            if (extraDistance > 0) {
                append("Distance: ₹${roundToRupee(distanceCharge).toInt()} (@₹${tariff.perKmRate}/km). ")
            }
            if (waitingCharge > 0) {
                append("Waiting: ₹${roundToRupee(waitingCharge).toInt()}. ")
            }
            if (isNight) {
                append("Night Surcharge (+${tariff.nightChargePercent.toInt()}%): ₹${roundToRupee(nightCharge).toInt()}. ")
            }
            if (luggageCharge > 0) {
                append("Luggage: ₹${luggageCharge.toInt()}. ")
            }
        }

        return FareCalculationResult(
            transportType = tariff.transportType,
            estimatedFare = estimatedFare,
            fareRangeMin = minRange,
            fareRangeMax = maxRange,
            distanceKm = safeDistance,
            estimatedTimeMinutes = travelMinutes,
            baseFare = baseFare,
            distanceCharge = distanceCharge,
            waitingCharge = waitingCharge,
            nightCharge = nightCharge,
            luggageCharge = luggageCharge,
            extraCharges = extraTollsOrParking,
            isNightApplied = isNight,
            officialSource = tariff.officialSource,
            verificationStatus = tariff.verificationStatus,
            calculationNotes = notes
        )
    }

    fun compareTransports(
        tariffs: List<TariffEntity>,
        distanceKm: Double,
        waitingMinutes: Int = 0,
        luggageCount: Int = 0,
        forceNightMode: Boolean? = null
    ): List<FareCalculationResult> {
        val results = tariffs.map { tariff ->
            calculateFare(
                tariff = tariff,
                distanceKm = distanceKm,
                waitingMinutes = waitingMinutes,
                luggageCount = luggageCount,
                forceNightMode = forceNightMode
            )
        }.sortedBy { it.estimatedFare }

        if (results.isEmpty()) return emptyList()

        val cheapest = results.minByOrNull { it.estimatedFare }
        val fastest = results.minByOrNull { it.estimatedTimeMinutes }
        val maxFare = results.maxOfOrNull { it.estimatedFare } ?: 0.0

        // Best value: balance time and cost
        val bestValue = results.filter { it.transportType == TransportType.AUTO_RICKSHAW || it.transportType == TransportType.BIKE_TAXI }
            .minByOrNull { it.estimatedFare } ?: results.first()

        return results.map { item ->
            item.copy(
                isCheapest = item.transportType == cheapest?.transportType,
                isFastest = item.transportType == fastest?.transportType,
                isBestValue = item.transportType == bestValue.transportType && item.transportType != cheapest?.transportType,
                savingsVsMax = max(0.0, maxFare - item.estimatedFare)
            )
        }
    }

    fun calculateMetroFare(distanceKm: Double, city: String): Double {
        val safeDistance = max(0.5, distanceKm)
        val lowerCity = city.lowercase()
        return when {
            lowerCity.contains("lucknow") -> when {
                safeDistance <= 1.5 -> 10.0 // 1 station
                safeDistance <= 3.0 -> 15.0 // 2 stations
                safeDistance <= 7.0 -> 20.0 // 3-6 stations
                safeDistance <= 11.0 -> 30.0 // 7-9 stations
                safeDistance <= 15.0 -> 40.0 // 10-13 stations
                safeDistance <= 19.0 -> 50.0 // 14-17 stations
                else -> 60.0 // 18-21 stations
            }
            lowerCity.contains("delhi") -> when {
                safeDistance <= 2.0 -> 10.0
                safeDistance <= 5.0 -> 20.0
                safeDistance <= 12.0 -> 30.0
                safeDistance <= 21.0 -> 40.0
                safeDistance <= 32.0 -> 50.0
                else -> 60.0
            }
            lowerCity.contains("kolkata") -> when {
                safeDistance <= 2.0 -> 5.0
                safeDistance <= 5.0 -> 10.0
                safeDistance <= 10.0 -> 15.0
                safeDistance <= 20.0 -> 20.0
                else -> 25.0
            }
            lowerCity.contains("mumbai") -> when {
                safeDistance <= 3.0 -> 10.0
                safeDistance <= 12.0 -> 20.0
                safeDistance <= 18.0 -> 30.0
                safeDistance <= 24.0 -> 40.0
                safeDistance <= 30.0 -> 50.0
                else -> 60.0
            }
            else -> when {
                safeDistance <= 2.5 -> 10.0
                safeDistance <= 6.0 -> 20.0
                safeDistance <= 12.0 -> 30.0
                safeDistance <= 18.0 -> 40.0
                safeDistance <= 25.0 -> 50.0
                else -> 60.0
            }
        }
    }

    fun calculateBusFare(distanceKm: Double, city: String): Double {
        val safeDistance = max(0.5, distanceKm)
        val lowerCity = city.lowercase()
        return when {
            lowerCity.contains("lucknow") -> when {
                safeDistance <= 3.0 -> 10.0
                safeDistance <= 6.0 -> 15.0
                safeDistance <= 10.0 -> 20.0
                safeDistance <= 15.0 -> 25.0
                safeDistance <= 20.0 -> 30.0
                else -> 35.0
            }
            lowerCity.contains("delhi") -> when {
                safeDistance <= 4.0 -> 5.0
                safeDistance <= 10.0 -> 10.0
                else -> 15.0
            }
            lowerCity.contains("mumbai") -> when {
                safeDistance <= 5.0 -> 5.0
                safeDistance <= 10.0 -> 10.0
                safeDistance <= 15.0 -> 15.0
                safeDistance <= 20.0 -> 20.0
                else -> 25.0
            }
            lowerCity.contains("bengaluru") || lowerCity.contains("bangalore") -> when {
                safeDistance <= 2.0 -> 5.0
                safeDistance <= 4.0 -> 10.0
                safeDistance <= 6.0 -> 15.0
                safeDistance <= 8.0 -> 18.0
                safeDistance <= 10.0 -> 20.0
                safeDistance <= 14.0 -> 25.0
                safeDistance <= 18.0 -> 28.0
                else -> 30.0
            }
            lowerCity.contains("kolkata") -> when {
                safeDistance <= 4.0 -> 10.0
                safeDistance <= 8.0 -> 12.0
                safeDistance <= 12.0 -> 14.0
                safeDistance <= 16.0 -> 16.0
                else -> 20.0
            }
            lowerCity.contains("hyderabad") -> when {
                safeDistance <= 2.0 -> 10.0
                safeDistance <= 4.0 -> 15.0
                safeDistance <= 6.0 -> 20.0
                safeDistance <= 9.0 -> 25.0
                safeDistance <= 13.0 -> 30.0
                safeDistance <= 18.0 -> 35.0
                else -> 40.0
            }
            else -> when {
                safeDistance <= 3.0 -> 10.0
                safeDistance <= 6.0 -> 15.0
                safeDistance <= 10.0 -> 20.0
                safeDistance <= 15.0 -> 25.0
                else -> 30.0
            }
        }
    }

    private fun roundToRupee(value: Double): Double {
        return round(value)
    }
}

object OverchargeEngine {

    fun analyze(
        driverQuote: Double,
        calculation: FareCalculationResult
    ): OverchargeAnalysis {
        val expectedAvg = (calculation.fareRangeMin + calculation.fareRangeMax) / 2.0
        val diffAmount = driverQuote - calculation.fareRangeMax
        val diffPercent = if (expectedAvg > 0) ((driverQuote - expectedAvg) / expectedAvg) * 100.0 else 0.0

        val category = when {
            driverQuote <= calculation.fareRangeMax * 1.05 -> OverchargeCategory.FAIR
            driverQuote <= calculation.fareRangeMax * 1.25 -> OverchargeCategory.SLIGHTLY_HIGH
            driverQuote <= calculation.fareRangeMax * 1.50 -> OverchargeCategory.HIGH
            else -> OverchargeCategory.VERY_HIGH
        }

        val explanation = when (category) {
            OverchargeCategory.FAIR ->
                "The quoted fare of ₹${driverQuote.toInt()} is within the expected fair range (₹${calculation.fareRangeMin.toInt()}–₹${calculation.fareRangeMax.toInt()}). This matches official standard meter rates."

            OverchargeCategory.SLIGHTLY_HIGH ->
                "The quoted fare is ₹${diffAmount.toInt()} above the highest expected fare (+${diffPercent.toInt()}%). While slightly elevated, it may reflect minor peak traffic or mild surge."

            OverchargeCategory.HIGH ->
                "The driver is asking ₹${diffAmount.toInt()} higher than standard tariffs (+${diffPercent.toInt()}%). The fair government regulated rate for this ${String.format("%.1f", calculation.distanceKm)} km distance is ₹${calculation.fareRangeMin.toInt()}–₹${calculation.fareRangeMax.toInt()}."

            OverchargeCategory.VERY_HIGH ->
                "The driver quote of ₹${driverQuote.toInt()} is significantly higher (+₹${diffAmount.toInt()} / +${diffPercent.toInt()}%) than the expected tariff of ₹${calculation.fareRangeMin.toInt()}–₹${calculation.fareRangeMax.toInt()}. This is an excessive quote."
        }

        val bargainingAdvice = when (category) {
            OverchargeCategory.FAIR ->
                "Safe to accept without negotiation. Fares are aligned with government meter charts."

            OverchargeCategory.SLIGHTLY_HIGH ->
                "Politely offer ₹${calculation.fareRangeMax.toInt()}: 'Bhaiya, meter se chalenge ya ₹${calculation.fareRangeMax.toInt()} me chalenge?'"

            OverchargeCategory.HIGH ->
                "Politely counter-offer: 'Official meter rate for ${String.format("%.1f", calculation.distanceKm)} km is ₹${calculation.fareRangeMin.toInt()}–₹${calculation.fareRangeMax.toInt()}. I can pay ₹${calculation.fareRangeMax.toInt()} maximum.'"

            OverchargeCategory.VERY_HIGH ->
                "Do not accept ₹${driverQuote.toInt()}. Counter with ₹${calculation.fareRangeMax.toInt()} or check a nearby prepaid booth / local bus / e-rickshaw."
        }

        val legalNotice = if (calculation.verificationStatus == VerificationStatus.OFFICIAL) {
            "Verified against official transport notification: ${calculation.officialSource}. Motor Vehicles Act requires compliance with statutory meter fares."
        } else {
            "Based on standard regional benchmark calculations: ${calculation.officialSource}."
        }

        return OverchargeAnalysis(
            askedFare = driverQuote,
            expectedFareMin = calculation.fareRangeMin,
            expectedFareMax = calculation.fareRangeMax,
            expectedFareAvg = expectedAvg,
            differenceAmount = max(0.0, diffAmount),
            differencePercentage = diffPercent,
            category = category,
            fairnessExplanation = explanation,
            bargainingAdvice = bargainingAdvice,
            legalNoticeText = legalNotice,
            breakdown = calculation
        )
    }
}

object RouteMatrixEngine {

    // Haversine distance with urban street curvature multiplier (~1.3x)
    fun calculateRoadDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val straightLineKm = r * c
        // Road routing is typically 25% - 35% longer than straight line in Indian cities
        val roadDistance = straightLineKm * 1.30
        return max(0.8, round(roadDistance * 10.0) / 10.0)
    }

    fun buildMultiModalOption(
        fromName: String,
        toName: String,
        totalDistanceKm: Double,
        city: String
    ): MultiModalRoute {
        return if (totalDistanceKm > 4.0) {
            val firstMileDist = 1.2
            val transitDist = totalDistanceKm - 2.0
            val lastMileDist = 0.8
            val busFare = 15.0
            val eRickshawFare = 15.0
            val walkFare = 0.0
            val totalFare = busFare + eRickshawFare + walkFare
            val totalTime = 10 + 20 + 8

            MultiModalRoute(
                title = "Smart Combo (Bus + E-Rickshaw)",
                totalFare = totalFare,
                totalTimeMinutes = totalTime,
                totalDistanceKm = totalDistanceKm,
                steps = listOf(
                    RouteStep(
                        transportType = TransportType.WALK,
                        instructions = "Walk 250m to nearest Bus Stop",
                        fromPoint = fromName,
                        toPoint = "Nearest Bus Stop",
                        distanceKm = 0.3,
                        timeMinutes = 4,
                        fare = 0.0
                    ),
                    RouteStep(
                        transportType = TransportType.BUS,
                        instructions = "Take Local Bus towards central transit junction",
                        fromPoint = "Bus Stop",
                        toPoint = "Transit Junction",
                        distanceKm = transitDist,
                        timeMinutes = 20,
                        fare = busFare,
                        routeCode = "Route 11A / 23"
                    ),
                    RouteStep(
                        transportType = TransportType.E_RICKSHAW,
                        instructions = "Take shared E-Rickshaw for final leg",
                        fromPoint = "Transit Junction",
                        toPoint = toName,
                        distanceKm = lastMileDist,
                        timeMinutes = 8,
                        fare = eRickshawFare
                    )
                ),
                savingsComparedToCab = max(20.0, (totalDistanceKm * 16.0 + 50.0) - totalFare),
                isRecommended = true
            )
        } else {
            MultiModalRoute(
                title = "Direct E-Rickshaw / Auto Hop",
                totalFare = 25.0,
                totalTimeMinutes = 12,
                totalDistanceKm = totalDistanceKm,
                steps = listOf(
                    RouteStep(
                        transportType = TransportType.E_RICKSHAW,
                        instructions = "Take shared or direct E-Rickshaw",
                        fromPoint = fromName,
                        toPoint = toName,
                        distanceKm = totalDistanceKm,
                        timeMinutes = 12,
                        fare = 25.0
                    )
                ),
                savingsComparedToCab = 45.0,
                isRecommended = true
            )
        }
    }
}

object TrafficRouteEngine {

    fun generateTrafficRoutes(
        fromName: String,
        toName: String,
        baseDistanceKm: Double,
        city: String,
        startCoord: com.example.data.model.GeoPoint? = null,
        endCoord: com.example.data.model.GeoPoint? = null,
        tariffs: List<TariffEntity> = emptyList()
    ): List<com.example.data.model.TrafficRouteOption> {
        val dist = max(1.2, baseDistanceKm)

        val autoTariff = tariffs.firstOrNull { it.transportType == TransportType.AUTO_RICKSHAW }
        val cabTariff = tariffs.firstOrNull { it.transportType == TransportType.CAB_MINI }
            ?: tariffs.firstOrNull { it.transportType == TransportType.CAB_SEDAN }
        val busTariff = tariffs.firstOrNull { it.transportType == TransportType.BUS }

        fun calcAuto(d: Double): Double = if (autoTariff != null) {
            FareCalculatorEngine.calculateFare(autoTariff, d).estimatedFare
        } else {
            (25.0 + max(0.0, d - 1.5) * 11.50).coerceAtLeast(25.0)
        }

        fun calcCab(d: Double): Double = if (cabTariff != null) {
            FareCalculatorEngine.calculateFare(cabTariff, d).estimatedFare
        } else {
            (50.0 + max(0.0, d - 2.0) * 15.0).coerceAtLeast(50.0)
        }

        fun calcBus(d: Double): Double = if (busTariff != null) {
            FareCalculatorEngine.calculateFare(busTariff, d).estimatedFare
        } else {
            FareCalculatorEngine.calculateBusFare(d, city)
        }

        // Coordinate resolution.
        val requestedStart = startCoord ?: com.example.data.model.GeoPoint(26.8900, 81.0500)
        val requestedEnd = endCoord ?: com.example.data.model.GeoPoint(26.8530, 80.9450)

        val (start, end) = ensureSeparated(requestedStart, requestedEnd, dist)

        val dLat = end.latitude - start.latitude
        val dLng = end.longitude - start.longitude
        // Perpendicular vector for curved corridors
        val perpLat = -dLng * 0.25
        val perpLng = dLat * 0.25

        // Route 1: Fastest via Flyover / Express Link
        val r1Dist = roundToDecimal(dist * 1.08, 1)
        val r1BaseMin = (r1Dist / 38.0 * 60).toInt() + 3
        val r1Delay = 4
        val r1TotalMin = r1BaseMin + r1Delay

        val r1Segments = listOf(
            com.example.data.model.TrafficSegment("Entry Corridor", r1Dist * 0.2, com.example.data.model.TrafficLevel.FREE_FLOW, 0.0f, 0.2f),
            com.example.data.model.TrafficSegment("Elevated Express Flyover", r1Dist * 0.5, com.example.data.model.TrafficLevel.FREE_FLOW, 0.2f, 0.7f),
            com.example.data.model.TrafficSegment("Junction Exit & Terminal", r1Dist * 0.3, com.example.data.model.TrafficLevel.MODERATE, 0.7f, 1.0f)
        )

        val wp1Flyover = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.35 + perpLat * 1.2,
            start.longitude + dLng * 0.35 + perpLng * 1.2
        )
        val wp1Interchange = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.75 + perpLat * 0.6,
            start.longitude + dLng * 0.75 + perpLng * 0.6
        )

        val r1Waypoints = listOf(
            com.example.data.model.RouteWaypoint(fromName, "Origin Point", 0.0f, com.example.data.model.TrafficLevel.FREE_FLOW, 0, geoPoint = start),
            com.example.data.model.RouteWaypoint("Elevated Expressway Bypass", "Free Flow Speed ~50 km/h", 0.35f, com.example.data.model.TrafficLevel.FREE_FLOW, r1BaseMin / 3, geoPoint = wp1Flyover),
            com.example.data.model.RouteWaypoint("Express Interchange Circle", "Signal Delay ~2 mins", 0.75f, com.example.data.model.TrafficLevel.MODERATE, (r1BaseMin * 2) / 3, geoPoint = wp1Interchange),
            com.example.data.model.RouteWaypoint(toName, "Destination Point", 1.0f, com.example.data.model.TrafficLevel.FREE_FLOW, r1TotalMin, geoPoint = end)
        )

        val r1Polyline = generateCurvedPath(start, end, curveFactor = 0.65, numPoints = 18)

        val route1 = com.example.data.model.TrafficRouteOption(
            id = "route_fastest",
            title = "Via Elevated Bypass & Flyover",
            subtitle = "Recommended • Best speed and minimal intersection delay",
            tag = "Fastest Route",
            isRecommended = true,
            distanceKm = r1Dist,
            baseDurationMinutes = r1BaseMin,
            trafficDelayMinutes = r1Delay,
            totalDurationMinutes = r1TotalMin,
            overallTraffic = com.example.data.model.TrafficLevel.FREE_FLOW,
            congestionPercentage = 18,
            estimatedAutoFare = calcAuto(r1Dist),
            estimatedCabFare = calcCab(r1Dist),
            estimatedBusFare = calcBus(r1Dist),
            segments = r1Segments,
            waypoints = r1Waypoints,
            roadConditions = "Paved 4-lane expressway with flyover access",
            geoPoints = r1Polyline,
            startPoint = start,
            endPoint = end
        )

        // Route 2: Shortest Distance via Main Market / Central Road
        val r2Dist = roundToDecimal(dist * 0.95, 1)
        val r2BaseMin = (r2Dist / 22.0 * 60).toInt() + 2
        val r2Delay = 14
        val r2TotalMin = r2BaseMin + r2Delay

        val r2Segments = listOf(
            com.example.data.model.TrafficSegment("City Link Rd", r2Dist * 0.3, com.example.data.model.TrafficLevel.MODERATE, 0.0f, 0.3f),
            com.example.data.model.TrafficSegment("Market Bazaar Bottleneck", r2Dist * 0.4, com.example.data.model.TrafficLevel.HEAVY, 0.3f, 0.7f),
            com.example.data.model.TrafficSegment("Terminal Approach", r2Dist * 0.3, com.example.data.model.TrafficLevel.MODERATE, 0.7f, 1.0f)
        )

        val wp2Market = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.45,
            start.longitude + dLng * 0.45
        )
        val wp2Station = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.80,
            start.longitude + dLng * 0.80
        )

        val r2Waypoints = listOf(
            com.example.data.model.RouteWaypoint(fromName, "Origin Point", 0.0f, com.example.data.model.TrafficLevel.FREE_FLOW, 0, geoPoint = start),
            com.example.data.model.RouteWaypoint("Central Market Chowk", "High Pedestrian Density & Signal", 0.45f, com.example.data.model.TrafficLevel.HEAVY, r2BaseMin / 2, true, "Heavy market rush", geoPoint = wp2Market),
            com.example.data.model.RouteWaypoint("Station Circle", "Moving slow", 0.8f, com.example.data.model.TrafficLevel.MODERATE, (r2BaseMin * 3) / 4, geoPoint = wp2Station),
            com.example.data.model.RouteWaypoint(toName, "Destination Point", 1.0f, com.example.data.model.TrafficLevel.FREE_FLOW, r2TotalMin, geoPoint = end)
        )

        val r2Polyline = generateCurvedPath(start, end, curveFactor = 0.0, numPoints = 16)

        val route2 = com.example.data.model.TrafficRouteOption(
            id = "route_shortest",
            title = "Via Central Arterial & Market Rd",
            subtitle = "Shortest Distance • High traffic density at central crossings",
            tag = "Shortest Path",
            isRecommended = false,
            distanceKm = r2Dist,
            baseDurationMinutes = r2BaseMin,
            trafficDelayMinutes = r2Delay,
            totalDurationMinutes = r2TotalMin,
            overallTraffic = com.example.data.model.TrafficLevel.HEAVY,
            congestionPercentage = 74,
            estimatedAutoFare = calcAuto(r2Dist),
            estimatedCabFare = calcCab(r2Dist),
            estimatedBusFare = calcBus(r2Dist),
            segments = r2Segments,
            waypoints = r2Waypoints,
            roadConditions = "2-lane arterial with multiple signalized intersections",
            geoPoints = r2Polyline,
            startPoint = start,
            endPoint = end
        )

        // Route 3: Balanced Metro / Ring Road Corridor
        val r3Dist = roundToDecimal(dist * 1.02, 1)
        val r3BaseMin = (r3Dist / 30.0 * 60).toInt() + 2
        val r3Delay = 6
        val r3TotalMin = r3BaseMin + r3Delay

        val r3Segments = listOf(
            com.example.data.model.TrafficSegment("Outer Link", r3Dist * 0.35, com.example.data.model.TrafficLevel.FREE_FLOW, 0.0f, 0.35f),
            com.example.data.model.TrafficSegment("Metro Line Boulevard", r3Dist * 0.45, com.example.data.model.TrafficLevel.MODERATE, 0.35f, 0.8f),
            com.example.data.model.TrafficSegment("Approach Road", r3Dist * 0.2, com.example.data.model.TrafficLevel.FREE_FLOW, 0.8f, 1.0f)
        )

        val wp3Metro = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.40 - perpLat * 0.9,
            start.longitude + dLng * 0.40 - perpLng * 0.9
        )
        val wp3Ring = com.example.data.model.GeoPoint(
            start.latitude + dLat * 0.75 - perpLat * 0.6,
            start.longitude + dLng * 0.75 - perpLng * 0.6
        )

        val r3Waypoints = listOf(
            com.example.data.model.RouteWaypoint(fromName, "Origin Point", 0.0f, com.example.data.model.TrafficLevel.FREE_FLOW, 0, geoPoint = start),
            com.example.data.model.RouteWaypoint("Metro Pillar Corridor", "Dedicated Transit lane", 0.4f, com.example.data.model.TrafficLevel.FREE_FLOW, r3BaseMin / 3, geoPoint = wp3Metro),
            com.example.data.model.RouteWaypoint("Ring Road Junction", "Moderate flow ~35 km/h", 0.75f, com.example.data.model.TrafficLevel.MODERATE, (r3BaseMin * 2) / 3, geoPoint = wp3Ring),
            com.example.data.model.RouteWaypoint(toName, "Destination Point", 1.0f, com.example.data.model.TrafficLevel.FREE_FLOW, r3TotalMin, geoPoint = end)
        )

        val r3Polyline = generateCurvedPath(start, end, curveFactor = -0.55, numPoints = 18)

        val route3 = com.example.data.model.TrafficRouteOption(
            id = "route_corridor",
            title = "Via Ring Road & Metro Line",
            subtitle = "Steady Flow • Consistent transit speeds and wide lanes",
            tag = "Low Congestion",
            isRecommended = false,
            distanceKm = r3Dist,
            baseDurationMinutes = r3BaseMin,
            trafficDelayMinutes = r3Delay,
            totalDurationMinutes = r3TotalMin,
            overallTraffic = com.example.data.model.TrafficLevel.MODERATE,
            congestionPercentage = 38,
            estimatedAutoFare = calcAuto(r3Dist),
            estimatedCabFare = calcCab(r3Dist),
            estimatedBusFare = calcBus(r3Dist),
            segments = r3Segments,
            waypoints = r3Waypoints,
            roadConditions = "Divided 6-lane avenue along Metro Line",
            geoPoints = r3Polyline,
            startPoint = start,
            endPoint = end
        )

        return listOf(route1, route2, route3)
    }

    /**
     * Guarantees the two endpoints are far enough apart to define a corridor.
     *
     * When the caller resolves both ends to the same coordinate the geometry below
     * degenerates: every polyline becomes a single repeated point, the bounding box
     * has zero span, and the map shows one marker instead of two. Nudge the
     * destination along a fixed easterly bearing by the journey's own distance so
     * the route still reads correctly.
     */
    private fun ensureSeparated(
        start: com.example.data.model.GeoPoint,
        end: com.example.data.model.GeoPoint,
        distanceKm: Double
    ): Pair<com.example.data.model.GeoPoint, com.example.data.model.GeoPoint> {
        val minimumDegrees = 0.0008 // ~90 m; below this the corridor is not drawable.
        val latGap = kotlin.math.abs(end.latitude - start.latitude)
        val lngGap = kotlin.math.abs(end.longitude - start.longitude)
        if (latGap > minimumDegrees || lngGap > minimumDegrees) return start to end

        val straightLineKm = (distanceKm / 1.30).coerceAtLeast(0.4)
        val lngScale = cos(Math.toRadians(start.latitude)).coerceAtLeast(0.01)
        val deltaLng = straightLineKm / (111.32 * lngScale)

        return start to com.example.data.model.GeoPoint(
            latitude = start.latitude,
            longitude = (start.longitude + deltaLng).coerceIn(-180.0, 180.0)
        )
    }

    private fun generateCurvedPath(
        start: com.example.data.model.GeoPoint,
        end: com.example.data.model.GeoPoint,
        curveFactor: Double,
        numPoints: Int
    ): List<com.example.data.model.GeoPoint> {
        val points = mutableListOf<com.example.data.model.GeoPoint>()
        val dLat = end.latitude - start.latitude
        val dLng = end.longitude - start.longitude
        val perpLat = -dLng * curveFactor * 0.3
        val perpLng = dLat * curveFactor * 0.3

        for (i in 0..numPoints) {
            val t = i.toDouble() / numPoints.toDouble()
            // Parabolic curve: 4 * t * (1 - t) has peak at t = 0.5
            val parabola = 4.0 * t * (1.0 - t)
            val lat = start.latitude + dLat * t + perpLat * parabola
            val lng = start.longitude + dLng * t + perpLng * parabola
            points.add(com.example.data.model.GeoPoint(lat, lng))
        }
        return points
    }

    private fun roundToDecimal(value: Double, decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(value * multiplier) / multiplier
    }
}
