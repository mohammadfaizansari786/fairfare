package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class TransportType(val displayName: String, val defaultSpeedKmh: Float, val iconName: String) {
    BUS("Local Bus", 18f, "directions_bus"),
    E_RICKSHAW("E-Rickshaw", 15f, "electric_rickshaw"),
    AUTO_RICKSHAW("Auto-Rickshaw", 24f, "electric_rickshaw"),
    BIKE_TAXI("Bike Taxi", 32f, "two_wheeler"),
    CAB_MINI("Cab (Mini / Hatchback)", 28f, "local_taxi"),
    CAB_SEDAN("Cab (Sedan / Prime)", 28f, "directions_car"),
    METRO("Metro Transit", 35f, "subway"),
    WALK("Walking", 4.5f, "directions_walk"),
    MULTI_MODAL("Bus + Auto Combo", 22f, "alt_route")
}

enum class VerificationStatus {
    OFFICIAL,
    ESTIMATED,
    COMMUNITY_SUBMITTED
}

enum class OverchargeCategory(val label: String, val level: Int) {
    FAIR("Fair & Standard", 0),
    SLIGHTLY_HIGH("Slightly High", 1),
    HIGH("High Fare", 2),
    VERY_HIGH("Significantly High", 3)
}

enum class PlaceCategory {
    HOME, WORK, STATION, AIRPORT, COLLEGE, MARKET, HOSPITAL, RECENT, FAVORITE
}

@Entity(
    tableName = "tariffs",
    indices = [Index(value = ["city", "transportType"], unique = true)]
)
@JsonClass(generateAdapter = true)
data class TariffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val country: String = "India",
    val state: String,
    val city: String,
    val transportType: TransportType,
    val baseFare: Double,
    val baseDistanceKm: Double = 1.5,
    val perKmRate: Double,
    val waitingRatePerHour: Double = 30.0,
    val nightChargePercent: Double = 25.0,
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 5,
    val luggageRatePerItem: Double = 10.0,
    val minFare: Double = baseFare,
    val effectiveDate: String = "2024-01-01",
    val lastUpdatedDate: String = "2025-06-15",
    val officialSource: String,
    val sourceUrl: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.OFFICIAL,
    val notes: String = ""
)

@Entity(tableName = "trip_history")
@JsonClass(generateAdapter = true)
data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromLocation: String,
    val toLocation: String,
    val city: String,
    val transportType: TransportType,
    val distanceKm: Double,
    val durationMinutes: Int,
    val estimatedFareMin: Double,
    val estimatedFareMax: Double,
    val actualFarePaid: Double,
    val overchargeDifference: Double, // actual - avg estimated
    val timestamp: Long = System.currentTimeMillis(),
    val isNightTrip: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "saved_places")
@JsonClass(generateAdapter = true)
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory = PlaceCategory.RECENT,
    val visitCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_reports")
@JsonClass(generateAdapter = true)
data class CommunityReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val transportType: TransportType,
    val fromLocation: String,
    val toLocation: String,
    val distanceKm: Double,
    val askedFare: Double,
    val expectedFare: Double,
    val issueType: String, // "Overcharging", "Meter Refusal", "Outdated Tariff", "Bus Skipped"
    val description: String,
    val hasProofImage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 0
)

@Entity(
    tableName = "bus_routes",
    indices = [Index(value = ["city", "routeNumber", "routeName"], unique = true)]
)
@JsonClass(generateAdapter = true)
data class BusRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val city: String,
    val routeNumber: String,
    val routeName: String,
    val startStop: String,
    val endStop: String,
    val intermediateStopsCsv: String, // Comma-separated list of stops
    val baseFare: Double = 10.0,
    val perStageFare: Double = 5.0,
    val maxFare: Double = 35.0,
    val frequencyMinutes: Int = 15,
    val firstBusTime: String = "06:00 AM",
    val lastBusTime: String = "10:30 PM",
    val isAcBus: Boolean = false
)

data class FareCalculationResult(
    val transportType: TransportType,
    val estimatedFare: Double,
    val fareRangeMin: Double,
    val fareRangeMax: Double,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val baseFare: Double,
    val distanceCharge: Double,
    val waitingCharge: Double,
    val nightCharge: Double,
    val luggageCharge: Double,
    val extraCharges: Double,
    val isNightApplied: Boolean,
    val officialSource: String,
    val verificationStatus: VerificationStatus,
    val isCheapest: Boolean = false,
    val isFastest: Boolean = false,
    val isBestValue: Boolean = false,
    val savingsVsMax: Double = 0.0,
    val calculationNotes: String = ""
)

data class OverchargeAnalysis(
    val askedFare: Double,
    val expectedFareMin: Double,
    val expectedFareMax: Double,
    val expectedFareAvg: Double,
    val differenceAmount: Double,
    val differencePercentage: Double,
    val category: OverchargeCategory,
    val fairnessExplanation: String,
    val bargainingAdvice: String,
    val legalNoticeText: String,
    val breakdown: FareCalculationResult
)

data class CityInfo(
    val name: String,
    val state: String,
    val defaultLat: Double,
    val defaultLng: Double,
    val availableTransports: List<TransportType>,
    val popularLandmarks: List<LandmarkInfo>
)

data class LandmarkInfo(
    val name: String,
    val area: String,
    val lat: Double,
    val lng: Double,
    val category: PlaceCategory
)

data class PlaceSearchResult(
    val id: String,
    val name: String,
    val secondaryText: String,
    val category: PlaceCategory,
    val coordinates: GeoPoint
)


data class MultiModalRoute(
    val title: String,
    val totalFare: Double,
    val totalTimeMinutes: Int,
    val totalDistanceKm: Double,
    val steps: List<RouteStep>,
    val savingsComparedToCab: Double,
    val isRecommended: Boolean = false
)

data class RouteStep(
    val transportType: TransportType,
    val instructions: String,
    val fromPoint: String,
    val toPoint: String,
    val distanceKm: Double,
    val timeMinutes: Int,
    val fare: Double,
    val routeCode: String? = null // e.g. Bus "11A"
)

enum class TrafficLevel(val label: String, val delayFactor: Double, val speedKmh: Int) {
    FREE_FLOW("Clear & Free Flow", 1.0, 45),
    MODERATE("Moderate Traffic", 1.25, 28),
    HEAVY("Heavy Congestion", 1.6, 16),
    SEVERE("Severe Bottleneck", 2.1, 8)
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class TrafficSegment(
    val name: String,
    val lengthKm: Double,
    val trafficLevel: TrafficLevel,
    val startPercent: Float,
    val endPercent: Float
)

data class RouteWaypoint(
    val title: String,
    val subtitle: String,
    val distanceRatio: Float,
    val trafficLevel: TrafficLevel,
    val etaMinutes: Int,
    val isIncident: Boolean = false,
    val incidentDescription: String = "",
    val geoPoint: GeoPoint? = null
)

data class TrafficRouteOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String, // "Fastest Route", "Shortest Path", "Lowest Congestion", "Scenic Corridor"
    val isRecommended: Boolean,
    val distanceKm: Double,
    val baseDurationMinutes: Int,
    val trafficDelayMinutes: Int,
    val totalDurationMinutes: Int,
    val overallTraffic: TrafficLevel,
    val congestionPercentage: Int,
    val estimatedAutoFare: Double,
    val estimatedCabFare: Double,
    val estimatedBusFare: Double,
    val segments: List<TrafficSegment>,
    val waypoints: List<RouteWaypoint>,
    val tollCount: Int = 0,
    val roadConditions: String = "Paved 4-lane highway with flyover access",
    val geoPoints: List<GeoPoint> = emptyList(),
    val startPoint: GeoPoint? = null,
    val endPoint: GeoPoint? = null
)
