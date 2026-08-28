package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TomTom Routing API.
 *
 * `computeTravelTimeFor=all` and `sectionType=traffic` are what make the response
 * usable for corridor colouring: the former returns live vs free-flow travel time,
 * the latter splits the geometry into congestion sections.
 */
interface TomTomRoutingApi {

    @GET("routing/1/calculateRoute/{coordinates}/json")
    suspend fun calculateRoute(
        /** `startLat,startLng:endLat,endLng` */
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("key") apiKey: String,
        @Query("traffic") traffic: Boolean = true,
        @Query("computeTravelTimeFor") computeTravelTimeFor: String = "all",
        @Query("sectionType") sectionType: String = "traffic",
        @Query("routeType") routeType: String = "fastest",
        @Query("travelMode") travelMode: String = "car",
        /** Ask for alternatives so the UI can offer real corridor choices. */
        @Query("maxAlternatives") maxAlternatives: Int = 2,
        @Query("instructionsType") instructionsType: String = "text"
    ): TomTomRouteResponse
}

/**
 * OSRM demo server. Keyless fallback so road-following geometry works without any
 * API key configured.
 *
 * Note: the public demo host is rate-limited and carries no uptime guarantee. It is
 * a graceful-degradation path, not a production routing source.
 */
interface OsrmRoutingApi {

    @GET("route/v1/driving/{coordinates}")
    suspend fun route(
        /** `startLng,startLat;endLng,endLat` — OSRM is lng-first. */
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("annotations") annotations: String = "speed,distance",
        @Query("alternatives") alternatives: Boolean = true,
        @Query("steps") steps: Boolean = false
    ): OsrmRouteResponse
}
