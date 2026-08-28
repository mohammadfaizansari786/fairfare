package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.GeoPoint
import com.example.data.model.TrafficLevel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** A road-following route returned by a routing provider. */
data class RoadRoute(
    /** Ordered coordinates that trace actual roads. */
    val geometry: List<GeoPoint>,
    val distanceKm: Double,
    val durationMinutes: Int,
    /** Free-flow duration; equals [durationMinutes] when the provider has no traffic data. */
    val freeFlowMinutes: Int,
    /** Congestion spans as fractions of the route, for per-segment colouring. */
    val congestion: List<CongestionSpan> = emptyList()
) {
    val delayMinutes: Int get() = (durationMinutes - freeFlowMinutes).coerceAtLeast(0)
}

/** A stretch of a route sharing one congestion level. */
data class CongestionSpan(
    val startFraction: Float,
    val endFraction: Float,
    val level: TrafficLevel
)

/**
 * Fetches real road geometry.
 *
 * The corridors were previously synthesised as parabolic curves between two
 * points — mathematically smooth but crossing buildings, rivers and blocks, i.e.
 * "lines in the air". This calls a routing engine so the polyline follows the road
 * network.
 *
 * Provider order is deliberate: TomTom when a key exists (it returns live traffic
 * sections), OSRM otherwise (keyless, still real roads). If both fail the caller
 * keeps its synthetic curve as a last resort, so the map is never blank.
 */
class RoutingRepository(
    private val tomTomApi: TomTomRoutingApi = defaultTomTomApi(),
    private val osrmApi: OsrmRoutingApi = defaultOsrmApi(),
    private val photonApi: PhotonGeocodingApi = defaultPhotonApi(),
    private val tomTomSearchApi: TomTomSearchApi = defaultTomTomSearchApi(),
    private val nominatimApi: NominatimSearchApi = defaultNominatimApi(),
    private val googlePlacesApi: GooglePlacesApi = defaultGooglePlacesApi(),
    private val apiKey: String = BuildConfig.TOMTOM_API_KEY,
    private val googleApiKey: String = BuildConfig.MAPS_API_KEY
) {

    private val hasKey: Boolean
        get() = apiKey.isNotBlank() && !apiKey.startsWith("MY_")

    private val hasGoogleKey: Boolean
        get() = googleApiKey.isNotBlank() && !googleApiKey.startsWith("MY_")
    
    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, List<com.example.data.model.PlaceSearchResult>>()

    /**
     * Searches for real-world places matching [query], biased near ([cityLat], [cityLng]).
     */
    /**
     * Searches for real-world places matching [query], biased near ([cityLat], [cityLng]).
     */
    suspend fun searchPlaces(
        query: String,
        cityLat: Double? = null,
        cityLng: Double? = null
    ): List<com.example.data.model.PlaceSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val cacheKey = "${trimmed.lowercase()}_${cityLat?.let { String.format("%.2f", it) }}_${cityLng?.let { String.format("%.2f", it) }}"
        searchCache[cacheKey]?.let { return@withContext it }

        val results = mutableListOf<com.example.data.model.PlaceSearchResult>()

        // 1. Local Landmark Places Index biased to active city
        val relevantCities = if (cityLat != null && cityLng != null) {
            com.example.data.local.InitialData.CITIES.filter { city ->
                kotlin.math.abs(city.defaultLat - cityLat) < 1.0 && kotlin.math.abs(city.defaultLng - cityLng) < 1.0
            }.ifEmpty {
                // Find single closest city
                com.example.data.local.InitialData.CITIES.minByOrNull {
                    val dLat = it.defaultLat - cityLat
                    val dLng = it.defaultLng - cityLng
                    dLat * dLat + dLng * dLng
                }?.let { listOf(it) } ?: com.example.data.local.InitialData.CITIES.take(1)
            }
        } else {
            com.example.data.local.InitialData.CITIES
        }

        val localDemoPlaces = relevantCities.flatMap { city ->
            city.popularLandmarks.mapIndexed { idx, lm ->
                com.example.data.model.PlaceSearchResult(
                    id = "demo_${city.name}_${lm.name}_$idx",
                    name = lm.name,
                    secondaryText = "${lm.area}, ${city.name}",
                    category = lm.category,
                    coordinates = GeoPoint(lm.lat, lm.lng)
                )
            }
        }.filter {
            it.name.startsWith(trimmed, ignoreCase = true) ||
            it.name.split(" ", "-", "(", ")", "/", "&", ",").any { word -> word.startsWith(trimmed, ignoreCase = true) }
        }
        results.addAll(localDemoPlaces)

        // 2. Fetch remote geocoding in parallel with fast timeouts
        coroutineScope {
            val googleDeferred = if (hasGoogleKey) {
                async {
                    runCatching {
                        withTimeoutOrNull(2000L) {
                            val locParam = if (cityLat != null && cityLng != null) "$cityLat,$cityLng" else null
                            googlePlacesApi.autocomplete(
                                input = trimmed,
                                apiKey = googleApiKey,
                                location = locParam,
                                radiusMeters = 50000
                            ).predictions.mapIndexedNotNull { index, item -> item.toPlaceSearchResult(index) }
                        }
                    }.getOrNull()
                }
            } else null

            val tomtomDeferred = if (hasKey) {
                async {
                    runCatching {
                        withTimeoutOrNull(2000L) {
                            tomTomSearchApi.search(
                                query = trimmed,
                                apiKey = apiKey,
                                lat = cityLat,
                                lon = cityLng,
                                radiusMeters = 50000,
                                limit = 15
                            ).results.mapIndexedNotNull { index, item -> item.toPlaceSearchResult(index) }
                        }
                    }.getOrNull()
                }
            } else null

            val photonDeferred = async {
                runCatching {
                    withTimeoutOrNull(2000L) {
                        photonApi.search(
                            query = trimmed,
                            lat = cityLat,
                            lon = cityLng,
                            limit = 10
                        ).features.mapIndexedNotNull { index, feature -> feature.toPlaceSearchResult(index) }
                    }
                }.getOrNull()
            }

            googleDeferred?.await()?.let { results.addAll(0, it) }
            tomtomDeferred?.await()?.let { results.addAll(0, it) }
            val photonResults = photonDeferred.await()
            if (!photonResults.isNullOrEmpty()) {
                results.addAll(photonResults)
            } else {
                // If Photon had no results, fallback to Nominatim
                val nominatimRes = runCatching {
                    withTimeoutOrNull(2000L) {
                        nominatimApi.search(
                            query = trimmed,
                            limit = 8
                        ).mapIndexedNotNull { index, item -> item.toPlaceSearchResult(index) }
                    }
                }.getOrNull()
                if (!nominatimRes.isNullOrEmpty()) {
                    results.addAll(nominatimRes)
                }
            }
        }

        val distinctResults = results.distinctBy {
            "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}"
        }.take(12)

        if (distinctResults.isNotEmpty()) {
            searchCache[cacheKey] = distinctResults
        }
        distinctResults
    }

    /**
     * Resolves the primary GPS coordinates for a freeform location query.
     */
    suspend fun geocodeQuery(
        query: String,
        cityLat: Double? = null,
        cityLng: Double? = null
    ): GeoPoint? = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext null

        if (hasGoogleKey) {
            val googleGeocoded = runCatching {
                val response = googlePlacesApi.geocodeByAddress(
                    address = trimmed,
                    apiKey = googleApiKey
                )
                response.results.firstOrNull()?.geometry?.location?.let {
                    GeoPoint(it.lat, it.lng)
                }
            }.getOrNull()

            if (googleGeocoded != null && (googleGeocoded.latitude != 0.0 || googleGeocoded.longitude != 0.0)) {
                return@withContext googleGeocoded
            }
        }

        // Try Photon direct coordinate resolution with city lat/lon bias
        val photonCoords = runCatching {
            withTimeoutOrNull(3500L) {
                val res = photonApi.search(
                    query = trimmed,
                    lat = cityLat,
                    lon = cityLng,
                    limit = 5
                )
                res.features.firstOrNull()?.geometry?.coordinates?.let { c ->
                    if (c.size >= 2) GeoPoint(latitude = c[1], longitude = c[0]) else null
                }
            }
        }.getOrNull()

        if (photonCoords != null && (photonCoords.latitude != 0.0 || photonCoords.longitude != 0.0)) {
            return@withContext photonCoords
        }

        // Try Nominatim
        val nominatimCoords = runCatching {
            withTimeoutOrNull(3500L) {
                val res = nominatimApi.search(
                    query = trimmed,
                    limit = 5
                )
                res.firstOrNull()?.let { item ->
                    val lat = item.lat?.toDoubleOrNull()
                    val lon = item.lon?.toDoubleOrNull()
                    if (lat != null && lon != null) GeoPoint(lat, lon) else null
                }
            }
        }.getOrNull()

        if (nominatimCoords != null && (nominatimCoords.latitude != 0.0 || nominatimCoords.longitude != 0.0)) {
            return@withContext nominatimCoords
        }

        val results = searchPlaces(trimmed, cityLat, cityLng)
        results.firstOrNull { it.coordinates.latitude != 0.0 && it.coordinates.longitude != 0.0 }?.coordinates
    }

    /**
     * Resolves GPS coordinates for a Google Place ID.
     */
    suspend fun geocodePlaceId(placeId: String): GeoPoint? = withContext(Dispatchers.IO) {
        if (!hasGoogleKey) return@withContext null
        runCatching {
            val response = googlePlacesApi.geocodeByPlaceId(
                placeId = placeId,
                apiKey = googleApiKey
            )
            response.results.firstOrNull()?.geometry?.location?.let {
                GeoPoint(it.lat, it.lng)
            }
        }.getOrNull()
    }


    /**
     * Road routes between two points, best provider first.
     *
     * Returns an empty list rather than throwing: a routing outage should degrade
     * the map, not break the fare flow.
     */
    suspend fun fetchRoutes(origin: GeoPoint, destination: GeoPoint): List<RoadRoute> =
        withContext(Dispatchers.IO) {
            if (hasKey) {
                runCatching { fetchFromTomTom(origin, destination) }
                    .getOrNull()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { return@withContext it }
            }

            runCatching { fetchFromOsrm(origin, destination) }
                .getOrDefault(emptyList())
        }

    private suspend fun fetchFromTomTom(
        origin: GeoPoint,
        destination: GeoPoint
    ): List<RoadRoute> {
        val coordinates = "${origin.latitude},${origin.longitude}:" +
            "${destination.latitude},${destination.longitude}"

        val response = tomTomApi.calculateRoute(coordinates = coordinates, apiKey = apiKey)

        return response.routes.mapNotNull { route ->
            // Legs are consecutive, so flattening gives the full ordered geometry
            // that section indices refer into.
            val points = route.legs
                .flatMap { it.points }
                .map { GeoPoint(it.latitude, it.longitude) }
            if (points.size < 2) return@mapNotNull null

            val summary = route.summary
            val liveSeconds = summary?.travelTimeInSeconds ?: 0
            val freeFlowSeconds = summary?.noTrafficTravelTimeInSeconds?.takeIf { it > 0 }
                ?: liveSeconds

            RoadRoute(
                geometry = points,
                distanceKm = (summary?.lengthInMeters ?: 0) / 1000.0,
                durationMinutes = secondsToMinutes(liveSeconds),
                freeFlowMinutes = secondsToMinutes(freeFlowSeconds),
                congestion = route.sections.toCongestionSpans(points.size)
            )
        }
    }

    private suspend fun fetchFromOsrm(
        origin: GeoPoint,
        destination: GeoPoint
    ): List<RoadRoute> {
        // OSRM takes lng,lat — the reverse of TomTom.
        val coordinates = "${origin.longitude},${origin.latitude};" +
            "${destination.longitude},${destination.latitude}"

        val response = osrmApi.route(coordinates = coordinates)
        if (response.code != null && response.code != "Ok") return emptyList()

        return response.routes.mapNotNull { route ->
            val points = route.geometry?.let(PolylineCodec::decode).orEmpty()
            if (points.size < 2) return@mapNotNull null

            val minutes = secondsToMinutes(route.duration.toInt())

            RoadRoute(
                geometry = points,
                distanceKm = route.distance / 1000.0,
                durationMinutes = minutes,
                // OSRM has no live traffic; report free-flow honestly rather than
                // inventing a delay.
                freeFlowMinutes = minutes,
                congestion = route.legs
                    .firstOrNull()
                    ?.annotation
                    ?.toCongestionSpans(points.size)
                    .orEmpty()
            )
        }
    }

    private fun secondsToMinutes(seconds: Int): Int =
        Math.round(seconds / 60.0).toInt().coerceAtLeast(1)

    private companion object {
        fun httpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        fun moshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        fun defaultTomTomApi(): TomTomRoutingApi = Retrofit.Builder()
            .baseUrl("https://api.tomtom.com/")
            .client(httpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(TomTomRoutingApi::class.java)

        fun defaultOsrmApi(): OsrmRoutingApi = Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(httpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(OsrmRoutingApi::class.java)

        fun defaultPhotonApi(): PhotonGeocodingApi = Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .client(httpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(PhotonGeocodingApi::class.java)

        fun defaultTomTomSearchApi(): TomTomSearchApi = Retrofit.Builder()
            .baseUrl("https://api.tomtom.com/")
            .client(httpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(TomTomSearchApi::class.java)

        fun defaultNominatimApi(): NominatimSearchApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "FairFare/1.0 (Android-Transport-Planner)")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi()))
                .build()
                .create(NominatimSearchApi::class.java)
        }

        fun defaultGooglePlacesApi(): GooglePlacesApi = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(httpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(GooglePlacesApi::class.java)
    }
}
