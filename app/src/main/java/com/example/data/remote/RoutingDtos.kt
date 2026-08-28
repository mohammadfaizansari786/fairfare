package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * Routing API response models.
 *
 * Two providers are modelled because they serve different needs:
 *  - TomTom returns traffic-aware geometry plus per-section congestion, which is
 *    what the corridor colouring needs. Requires a key.
 *  - OSRM is keyless and still returns true road geometry, so the map shows roads
 *    rather than straight lines even with no key configured.
 */

// ---------------------------------------------------------------------------
// TomTom Routing API v1 — calculateRoute
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class TomTomRouteResponse(
    val routes: List<TomTomRoute> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TomTomRoute(
    val summary: TomTomSummary? = null,
    val legs: List<TomTomLeg> = emptyList(),
    val sections: List<TomTomSection> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TomTomSummary(
    val lengthInMeters: Int = 0,
    val travelTimeInSeconds: Int = 0,
    /** Seconds of delay attributable to live traffic; 0 when free-flowing. */
    val trafficDelayInSeconds: Int = 0,
    val noTrafficTravelTimeInSeconds: Int = 0
)

@JsonClass(generateAdapter = true)
data class TomTomLeg(
    val points: List<TomTomPoint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TomTomPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/**
 * A stretch of the route with a uniform property.
 *
 * `sectionType=TRAFFIC` responses carry [magnitudeOfDelay] (0..4), which maps
 * directly onto the congestion scale the UI already models. Indices refer to
 * positions in the flattened point list.
 */
@JsonClass(generateAdapter = true)
data class TomTomSection(
    val startPointIndex: Int = 0,
    val endPointIndex: Int = 0,
    val sectionType: String? = null,
    val simpleCategory: String? = null,
    val magnitudeOfDelay: Int? = null,
    val delayInSeconds: Int? = null
)

// ---------------------------------------------------------------------------
// OSRM — /route/v1/driving
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    val code: String? = null,
    val routes: List<OsrmRoute> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    /** Encoded polyline (precision 5) when `geometries=polyline`. */
    val geometry: String? = null,
    /** Metres. */
    val distance: Double = 0.0,
    /** Seconds, free-flow (OSRM has no live traffic). */
    val duration: Double = 0.0,
    val legs: List<OsrmLeg> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList(),
    val annotation: OsrmAnnotation? = null
)

@JsonClass(generateAdapter = true)
data class OsrmStep(
    val name: String? = null,
    val distance: Double = 0.0,
    val duration: Double = 0.0
)

/**
 * Per-vertex speed samples. OSRM reports free-flow speeds only, but the variation
 * between road classes still lets the UI distinguish an arterial stretch from a
 * congested lane when no traffic provider is available.
 */
@JsonClass(generateAdapter = true)
data class OsrmAnnotation(
    @Json(name = "speed") val speed: List<Double> = emptyList(),
    @Json(name = "distance") val distance: List<Double> = emptyList()
)
