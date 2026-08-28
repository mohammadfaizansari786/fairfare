package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.InitialData
import com.example.data.model.BusRouteEntity
import com.example.data.model.CityInfo
import com.example.data.model.CommunityReportEntity
import com.example.data.model.FareCalculationResult
import com.example.data.model.GeoPoint
import com.example.data.model.LandmarkInfo
import com.example.data.model.MultiModalRoute
import com.example.data.model.OverchargeAnalysis
import com.example.data.model.SavedPlaceEntity
import com.example.data.model.TariffEntity
import com.example.data.model.TrafficRouteOption
import com.example.data.model.TransportType
import com.example.data.model.TripHistoryEntity
import com.example.data.remote.RoutingRepository
import com.example.data.repository.FareRepository
import com.example.engine.FareCalculatorEngine
import com.example.engine.OverchargeEngine
import com.example.engine.RoadCorridorMapper
import com.example.engine.RouteMatrixEngine
import com.example.engine.TrafficRouteEngine
import com.example.ui.components.IncidentReportDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import com.example.data.model.PlaceCategory
import com.example.data.model.PlaceSearchResult

/** Journey inputs plus the derived comparison for the current route. */
data class RouteSearchState(
    val fromQuery: String = "",
    val toQuery: String = "",
    val fromCoordinates: GeoPoint? = null,
    val toCoordinates: GeoPoint? = null,
    val distanceKm: Double = 0.0,
    val isNightMode: Boolean = false,
    val luggageCount: Int = 0,
    val waitingMinutes: Int = 0,
    val comparisonList: List<FareCalculationResult> = emptyList(),
    val multiModalRoute: MultiModalRoute? = null,
    val isCalculating: Boolean = false,
    val errorMessage: String? = null
) {
    val hasResults: Boolean get() = comparisonList.isNotEmpty()
}

/**
 * Overcharge checker inputs.
 *
 * The quote is held as text because it is bound directly to a text field, but
 * [quoteValue] is the single place that converts it, so no screen has to repeat
 * the parse-or-default logic.
 */
data class OverchargeState(
    val driverQuoteText: String = "",
    val selectedTransport: TransportType = TransportType.AUTO_RICKSHAW,
    val distanceKm: Double = 5.0,
    val isNightMode: Boolean = false,
    val analysis: OverchargeAnalysis? = null,
    val isAnalyzing: Boolean = false
) {
    val quoteValue: Double? get() = driverQuoteText.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
    val isQuoteValid: Boolean get() = quoteValue != null
}

class FareViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = FareRepository(database)

    /**
     * Road geometry provider. Injectable so tests can supply a fake instead of
     * hitting the network.
     */
    private val routingRepository = RoutingRepository()

    private val _selectedCity = MutableStateFlow(InitialData.CITIES.first())
    val selectedCity: StateFlow<CityInfo> = _selectedCity.asStateFlow()

    private val _routeState = MutableStateFlow(RouteSearchState())
    val routeState: StateFlow<RouteSearchState> = _routeState.asStateFlow()

    private val _overchargeState = MutableStateFlow(OverchargeState())
    val overchargeState: StateFlow<OverchargeState> = _overchargeState.asStateFlow()

    private val _trafficRoutes = MutableStateFlow<List<TrafficRouteOption>>(emptyList())
    val trafficRoutes: StateFlow<List<TrafficRouteOption>> = _trafficRoutes.asStateFlow()

    private val _selectedTrafficRouteId = MutableStateFlow("")
    val selectedTrafficRouteId: StateFlow<String> = _selectedTrafficRouteId.asStateFlow()

    private val _isTrafficUpdating = MutableStateFlow(false)
    val isTrafficUpdating: StateFlow<Boolean> = _isTrafficUpdating.asStateFlow()

    /**
     * True when the corridors on screen are synthetic curves rather than real road
     * geometry, i.e. routing was unavailable. Surfaced so the UI can say so instead
     * of presenting an approximation as fact.
     */
    private val _isUsingApproximateRoutes = MutableStateFlow(false)
    val isUsingApproximateRoutes: StateFlow<Boolean> = _isUsingApproximateRoutes.asStateFlow()

    private val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.SYSTEM)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    /**
     * Only the most recent fare calculation matters. Without this, rapidly
     * changing inputs (dragging the distance slider, typing a destination) queued
     * a coroutine per keystroke and a stale one could finish last and overwrite
     * the correct result.
     */
    private var fareCalculationJob: Job? = null
    private var overchargeJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val cityTariffs: StateFlow<List<TariffEntity>> = _selectedCity
        .flatMapLatest { city -> repository.getTariffsForCity(city.name) }
        .map { tariffs ->
            if (tariffs.isEmpty()) {
                InitialData.tariffsForCityOrFallback(_selectedCity.value.name)
            } else {
                tariffs
            }
        }
        .catch { emit(InitialData.tariffsForCityOrFallback(_selectedCity.value.name)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InitialData.tariffsForCityOrFallback(InitialData.CITIES.first().name)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val busRoutes: StateFlow<List<BusRouteEntity>> = _selectedCity
        .flatMapLatest { city -> repository.getBusRoutesForCity(city.name) }
        .map { routes ->
            if (routes.isEmpty()) {
                InitialData.DEFAULT_BUS_ROUTES.filter { it.city.equals(_selectedCity.value.name, ignoreCase = true) }
            } else {
                routes
            }
        }
        .catch { emit(InitialData.DEFAULT_BUS_ROUTES.filter { it.city.equals(_selectedCity.value.name, ignoreCase = true) }) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InitialData.DEFAULT_BUS_ROUTES.filter { it.city.equals(InitialData.CITIES.first().name, ignoreCase = true) }
        )

    val allTrips: StateFlow<List<TripHistoryEntity>> = repository.getAllTrips()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = repository.getAllSavedPlaces()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val communityReports: StateFlow<List<CommunityReportEntity>> = _selectedCity
        .flatMapLatest { city -> repository.getReportsForCity(city.name) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Seed from the selected city's own landmarks rather than hardcoded
        // Lucknow strings, so the first screen is coherent for every city.
        applyCityDefaults(_selectedCity.value)
    }

    // ------------------------------------------------------------------
    // City & journey inputs
    // ------------------------------------------------------------------

    fun selectCity(city: CityInfo) {
        if (city.name == _selectedCity.value.name) return
        _selectedCity.value = city
        applyCityDefaults(city)
    }

    private fun applyCityDefaults(city: CityInfo) {
        val originLm = city.popularLandmarks.firstOrNull()
        val destinationLm = city.popularLandmarks.getOrNull(1)
        val origin = originLm?.name ?: city.name
        val destination = destinationLm?.name ?: "City centre"
        _routeState.value = _routeState.value.copy(
            fromQuery = origin,
            toQuery = destination,
            fromCoordinates = originLm?.let { GeoPoint(it.lat, it.lng) },
            toCoordinates = destinationLm?.let { GeoPoint(it.lat, it.lng) },
            errorMessage = null
        )
        calculateRouteFares()
    }

    fun updateFromQuery(query: String, coordinates: GeoPoint? = null) {
        _routeState.value = _routeState.value.copy(fromQuery = query, fromCoordinates = coordinates)
    }

    fun updateToQuery(query: String, coordinates: GeoPoint? = null) {
        _routeState.value = _routeState.value.copy(toQuery = query, toCoordinates = coordinates)
    }

    fun selectFromPlace(place: PlaceSearchResult) {
        viewModelScope.launch {
            var coords = place.coordinates
            if (coords.latitude == 0.0 && coords.longitude == 0.0) {
                coords = routingRepository.geocodePlaceId(place.id)
                    ?: routingRepository.geocodeQuery(
                        query = "${place.name}, ${place.secondaryText}",
                        cityLat = _selectedCity.value.defaultLat,
                        cityLng = _selectedCity.value.defaultLng
                    ) ?: coords
            }
            _routeState.value = _routeState.value.copy(
                fromQuery = place.name,
                fromCoordinates = if (coords.latitude != 0.0 || coords.longitude != 0.0) coords else null
            )
            calculateRouteFares()
        }
    }

    fun selectToPlace(place: PlaceSearchResult) {
        viewModelScope.launch {
            var coords = place.coordinates
            if (coords.latitude == 0.0 && coords.longitude == 0.0) {
                coords = routingRepository.geocodePlaceId(place.id)
                    ?: routingRepository.geocodeQuery(
                        query = "${place.name}, ${place.secondaryText}",
                        cityLat = _selectedCity.value.defaultLat,
                        cityLng = _selectedCity.value.defaultLng
                    ) ?: coords
            }
            _routeState.value = _routeState.value.copy(
                toQuery = place.name,
                toCoordinates = if (coords.latitude != 0.0 || coords.longitude != 0.0) coords else null
            )
            calculateRouteFares()
        }
    }

    fun swapLocations() {
        val current = _routeState.value
        _routeState.value = current.copy(
            fromQuery = current.toQuery,
            toQuery = current.fromQuery,
            fromCoordinates = current.toCoordinates,
            toCoordinates = current.fromCoordinates
        )
        calculateRouteFares()
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _appThemeMode.value = mode
    }

    fun setNightMode(enabled: Boolean) {
        if (enabled == _routeState.value.isNightMode) return
        _routeState.value = _routeState.value.copy(isNightMode = enabled)
        calculateRouteFares()
        // The audit screen shares the same tariff assumption, so keep it in step.
        setOverchargeNightMode(enabled)
    }

    fun setLuggage(count: Int) {
        val safeCount = count.coerceIn(0, 10)
        if (safeCount == _routeState.value.luggageCount) return
        _routeState.value = _routeState.value.copy(luggageCount = safeCount)
        calculateRouteFares()
    }

    fun setWaitingMinutes(minutes: Int) {
        val safeMinutes = minutes.coerceIn(0, 240)
        if (safeMinutes == _routeState.value.waitingMinutes) return
        _routeState.value = _routeState.value.copy(waitingMinutes = safeMinutes)
        calculateRouteFares()
    }

    fun setManualDistance(km: Double) {
        val safeKm = km.coerceIn(0.5, 200.0)
        _routeState.value = _routeState.value.copy(distanceKm = safeKm)
        calculateRouteFares()
    }

    fun selectPresetRoute(
        from: String,
        to: String,
        fromCoords: GeoPoint? = null,
        toCoords: GeoPoint? = null
    ) {
        _routeState.value = _routeState.value.copy(
            fromQuery = from.ifBlank { _routeState.value.fromQuery },
            toQuery = to.ifBlank { _routeState.value.toQuery },
            fromCoordinates = fromCoords ?: _routeState.value.fromCoordinates,
            toCoordinates = toCoords ?: _routeState.value.toCoordinates
        )
        calculateRouteFares()
    }

    /**
     * Instantly searches for place suggestions across curated landmarks, transit stops, and saved places.
     * Returns in 0 ms synchronously without blocking on network requests.
     */
    /**
     * Instantly searches for place suggestions across curated landmarks, transit stops, and saved places for the current city.
     * Uses strict relevance scoring: exact prefix > word prefix > area prefix > substring.
     * Returns in 0 ms synchronously without blocking on network requests or leaking other cities.
     */
    fun searchPlaceSuggestionsInstant(query: String): List<PlaceSearchResult> {
        val city = _selectedCity.value
        val trimmed = query.trim()

        if (trimmed.isEmpty()) return emptyList()

        val scoredList = mutableListOf<Pair<Int, PlaceSearchResult>>()

        // Match current city landmarks with fine-grained scoring
        city.popularLandmarks.forEachIndexed { idx, lm ->
            val name = lm.name
            val area = lm.area

            val nameExact = name.equals(trimmed, ignoreCase = true) || name.substringBefore("(").trim().equals(trimmed, ignoreCase = true)
            val nameStarts = name.startsWith(trimmed, ignoreCase = true) || name.substringBefore("(").trim().startsWith(trimmed, ignoreCase = true)
            val nameWordStarts = name.split(" ", "-", "(", ")", "/", "&", ",").any { it.startsWith(trimmed, ignoreCase = true) }
            val areaStarts = area.startsWith(trimmed, ignoreCase = true)
            val areaWordStarts = area.split(" ", "-", "(", ")", "/", "&", ",").any { it.startsWith(trimmed, ignoreCase = true) }
            val nameContains = name.contains(trimmed, ignoreCase = true)
            val areaContains = area.contains(trimmed, ignoreCase = true)

            val score = when {
                nameExact -> 100
                nameStarts -> 90
                nameWordStarts -> 75
                areaStarts -> 60
                areaWordStarts -> 45
                nameContains -> 30
                areaContains -> 15
                else -> 0
            }

            if (score > 0) {
                scoredList.add(
                    Pair(
                        score,
                        PlaceSearchResult(
                            id = "local_lm_${lm.name}_$idx",
                            name = lm.name,
                            secondaryText = "${lm.area}, ${city.name}",
                            category = lm.category,
                            coordinates = GeoPoint(lm.lat, lm.lng)
                        )
                    )
                )
            }
        }

        // Match saved places for current active city
        savedPlaces.value.filter {
            (it.city.isBlank() || it.city.equals(city.name, ignoreCase = true))
        }.forEachIndexed { idx, sp ->
            val name = sp.name
            val address = sp.address
            val nameExact = name.equals(trimmed, ignoreCase = true)
            val nameStarts = name.startsWith(trimmed, ignoreCase = true)
            val nameWordStarts = name.split(" ", "-", "(", ")", "/", "&", ",").any { it.startsWith(trimmed, ignoreCase = true) }
            val addrStarts = address.startsWith(trimmed, ignoreCase = true)
            val nameContains = name.contains(trimmed, ignoreCase = true)
            val addrContains = address.contains(trimmed, ignoreCase = true)

            val score = when {
                nameExact -> 98
                nameStarts -> 88
                nameWordStarts -> 72
                addrStarts -> 55
                nameContains -> 25
                addrContains -> 10
                else -> 0
            }

            if (score > 0) {
                scoredList.add(
                    Pair(
                        score,
                        PlaceSearchResult(
                            id = "saved_${sp.id}_$idx",
                            name = sp.name,
                            secondaryText = sp.address.ifBlank { city.name },
                            category = sp.category,
                            coordinates = GeoPoint(sp.latitude, sp.longitude)
                        )
                    )
                )
            }
        }

        // Match transit stops (Metro stations, bus stops) in the current city
        busRoutes.value.forEachIndexed { rIdx, route ->
            val stops = route.intermediateStopsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            stops.forEachIndexed { sIdx, stop ->
                val stopExact = stop.equals(trimmed, ignoreCase = true) || stop.substringBefore("(").trim().equals(trimmed, ignoreCase = true)
                val stopStarts = stop.startsWith(trimmed, ignoreCase = true)
                val stopWordStarts = stop.split(" ", "-", "(", ")", "/", "&", ",").any { it.startsWith(trimmed, ignoreCase = true) }
                val stopContains = stop.contains(trimmed, ignoreCase = true)

                val score = when {
                    stopExact -> 95
                    stopStarts -> 85
                    stopWordStarts -> 70
                    stopContains -> 25
                    else -> 0
                }

                if (score > 0) {
                    scoredList.add(
                        Pair(
                            score,
                            PlaceSearchResult(
                                id = "transit_stop_${rIdx}_${sIdx}_${stop.hashCode()}",
                                name = stop,
                                secondaryText = "${route.routeName}, ${city.name}",
                                category = PlaceCategory.STATION,
                                coordinates = GeoPoint(city.defaultLat, city.defaultLng)
                            )
                        )
                    )
                }
            }
        }

        return scoredList
            .sortedWith(
                compareByDescending<Pair<Int, PlaceSearchResult>> { it.first }
                    .thenBy { it.second.name.length }
            )
            .map { it.second }
            .distinctBy { "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}" }
            .take(6)
    }

    /**
     * Searches Google Maps Android Geocoding service directly on device for accurate real-time place names.
     */
    suspend fun searchGoogleMapsPlaces(query: String, city: CityInfo): List<PlaceSearchResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        kotlinx.coroutines.withTimeoutOrNull(600L) {
            runCatching {
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.ENGLISH)
                val fullQuery = if (trimmed.contains(city.name, ignoreCase = true)) trimmed else "$trimmed, ${city.name}"

                val addresses = geocoder.getFromLocationName(
                    fullQuery,
                    5,
                    city.defaultLat - 0.45,
                    city.defaultLng - 0.45,
                    city.defaultLat + 0.45,
                    city.defaultLng + 0.45
                ) ?: emptyList()

                addresses.mapIndexedNotNull { idx, addr ->
                    if (addr.latitude == 0.0 && addr.longitude == 0.0) return@mapIndexedNotNull null

                    val thoroughfare = addr.thoroughfare?.takeIf { it.isNotBlank() }
                    val subLocality = addr.subLocality?.takeIf { it.isNotBlank() }
                    val feature = addr.featureName?.takeIf {
                        it.isNotBlank() &&
                        !it.matches(Regex("^[0-9+\\-/]+$")) &&
                        it != thoroughfare &&
                        it != subLocality
                    }
                    val locality = addr.locality?.takeIf { it.isNotBlank() } ?: city.name

                    val primaryName = feature
                        ?: thoroughfare
                        ?: subLocality
                        ?: addr.getAddressLine(0)?.split(",")?.firstOrNull()?.trim()
                        ?: trimmed

                    val secondaryParts = listOfNotNull(
                        thoroughfare.takeIf { it != primaryName },
                        subLocality.takeIf { it != primaryName },
                        locality,
                        addr.adminArea
                    ).distinct()
                    val secondary = secondaryParts.joinToString(", ")

                    PlaceSearchResult(
                        id = "google_map_${idx}_${addr.latitude}_${addr.longitude}",
                        name = primaryName,
                        secondaryText = secondary.ifBlank { "${city.name}, India" },
                        category = inferCategoryFromName(primaryName, secondary),
                        coordinates = GeoPoint(addr.latitude, addr.longitude)
                    )
                }
            }.getOrDefault(emptyList())
        }.orEmpty()
    }

    /**
     * Searches for place suggestions across curated landmarks, saved places, transit stops, and live geocoding.
     */
    suspend fun searchPlaceSuggestions(query: String): List<PlaceSearchResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val city = _selectedCity.value
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val instantMatches = searchPlaceSuggestionsInstant(trimmed)
        if (trimmed.length < 2) return@withContext instantMatches.take(6)

        // Query Google geocoder and remote geocoding in PARALLEL
        val googleDeferred = async {
            searchGoogleMapsPlaces(trimmed, city)
        }
        val remoteDeferred = async {
            runCatching {
                routingRepository.searchPlaces(
                    query = trimmed,
                    cityLat = city.defaultLat,
                    cityLng = city.defaultLng
                )
            }.getOrDefault(emptyList())
        }

        val liveResults = mutableListOf<PlaceSearchResult>()
        liveResults.addAll(googleDeferred.await())
        liveResults.addAll(remoteDeferred.await())

        val strongInstant = instantMatches.filter {
            it.name.startsWith(trimmed, ignoreCase = true) ||
            it.name.substringBefore("(").trim().startsWith(trimmed, ignoreCase = true) ||
            it.name.split(" ", "-", "(", ")", "/", "&", ",").any { word -> word.startsWith(trimmed, ignoreCase = true) }
        }

        val combined = mutableListOf<PlaceSearchResult>()
        combined.addAll(strongInstant)
        combined.addAll(liveResults)
        combined.addAll(instantMatches)

        combined
            .distinctBy { "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}" }
            .take(8)
    }

    // ------------------------------------------------------------------
    // Fare calculation
    // ------------------------------------------------------------------

    fun calculateRouteFares() {
        fareCalculationJob?.cancel()
        fareCalculationJob = viewModelScope.launch {
            val city = _selectedCity.value
            val current = _routeState.value
            _routeState.value = current.copy(isCalculating = true, errorMessage = null)

            // Resolve Origin Coordinates
            var originCoords = current.fromCoordinates?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }
                ?: city.resolveLandmark(current.fromQuery)?.let { GeoPoint(it.lat, it.lng) }
            if (originCoords == null && current.fromQuery.isNotBlank() && current.fromQuery.length >= 2) {
                val searchQuery = if (current.fromQuery.contains(city.name, ignoreCase = true)) {
                    current.fromQuery
                } else {
                    "${current.fromQuery}, ${city.name}"
                }
                originCoords = routingRepository.geocodeQuery(
                    query = searchQuery,
                    cityLat = city.defaultLat,
                    cityLng = city.defaultLng
                ) ?: routingRepository.geocodeQuery(
                    query = current.fromQuery,
                    cityLat = city.defaultLat,
                    cityLng = city.defaultLng
                )
            }

            // Resolve Destination Coordinates
            var destCoords = current.toCoordinates?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }
                ?: city.resolveLandmark(current.toQuery)?.let { GeoPoint(it.lat, it.lng) }
            if (destCoords == null && current.toQuery.isNotBlank() && current.toQuery.length >= 2) {
                val searchQuery = if (current.toQuery.contains(city.name, ignoreCase = true)) {
                    current.toQuery
                } else {
                    "${current.toQuery}, ${city.name}"
                }
                destCoords = routingRepository.geocodeQuery(
                    query = searchQuery,
                    cityLat = city.defaultLat,
                    cityLng = city.defaultLng
                ) ?: routingRepository.geocodeQuery(
                    query = current.toQuery,
                    cityLat = city.defaultLat,
                    cityLng = city.defaultLng
                )
            }

            if (originCoords != current.fromCoordinates || destCoords != current.toCoordinates) {
                _routeState.value = _routeState.value.copy(
                    fromCoordinates = originCoords,
                    toCoordinates = destCoords
                )
            }

            // Straight-line estimate, used to seed routing and as fallback
            val estimatedDistance = when {
                originCoords != null && destCoords != null -> RouteMatrixEngine.calculateRoadDistanceKm(
                    originCoords.latitude, originCoords.longitude, destCoords.latitude, destCoords.longitude
                )
                current.distanceKm > 0.0 -> current.distanceKm
                else -> DEFAULT_DISTANCE_KM
            }

            val tariffs = runCatching { repository.getTariffsForCitySync(city.name) }
                .getOrElse { InitialData.tariffsForCityOrFallback(city.name) }

            if (tariffs.isEmpty()) {
                _routeState.value = _routeState.value.copy(
                    isCalculating = false,
                    comparisonList = emptyList(),
                    errorMessage = "No tariff data available for ${city.name} yet."
                )
                return@launch
            }

            // Fetch road geometry with resolved coordinates
            val roadDistance = regenerateTrafficRoutes(
                distanceKm = estimatedDistance,
                resolvedOrigin = originCoords,
                resolvedDestination = destCoords
            )
            val distance = roadDistance ?: estimatedDistance

            val comparison = FareCalculatorEngine.compareTransports(
                tariffs = tariffs,
                distanceKm = distance,
                waitingMinutes = current.waitingMinutes,
                luggageCount = current.luggageCount,
                forceNightMode = current.isNightMode
            )

            val multiModal = RouteMatrixEngine.buildMultiModalOption(
                fromName = current.fromQuery,
                toName = current.toQuery,
                totalDistanceKm = distance,
                city = city.name
            )

            _routeState.value = _routeState.value.copy(
                distanceKm = distance,
                comparisonList = comparison,
                multiModalRoute = multiModal,
                isCalculating = false,
                errorMessage = null
            )

            // Keep the audit screen aligned with the journey on screen.
            _overchargeState.value = _overchargeState.value.copy(distanceKm = distance)
            if (_overchargeState.value.isQuoteValid) {
                runOverchargeAnalysis()
            }
        }
    }

    /**
     * Rebuilds corridor options from real road geometry.
     *
     * Order of preference:
     *  1. A routing provider (TomTom with a key, else keyless OSRM) — returns
     *     geometry that follows actual roads, plus live congestion sections.
     *  2. The synthetic parabolic curve, only if routing fails. That geometry
     *     ignores the road network entirely, so it is a last resort to keep the map
     *     populated rather than something to show by choice.
     *
     * The user's selected corridor is preserved when it still exists.
     *
     * Returns the recommended corridor's true road distance when routing succeeded,
     * so the caller can recompute fares against the route actually being shown
     * instead of the straight-line estimate. Null when no road data was available.
     */
    private suspend fun regenerateTrafficRoutes(
        distanceKm: Double,
        resolvedOrigin: GeoPoint? = null,
        resolvedDestination: GeoPoint? = null
    ): Double? {
        val city = _selectedCity.value
        val current = _routeState.value
        val (startCoord, endCoord) = resolveJourneyCoordinates(
            city = city,
            fromQuery = current.fromQuery,
            toQuery = current.toQuery,
            roadDistanceKm = distanceKm,
            resolvedOrigin = resolvedOrigin,
            resolvedDestination = resolvedDestination
        )

        val tariffs = runCatching { repository.getTariffsForCitySync(city.name) }
            .getOrElse { InitialData.tariffsForCityOrFallback(city.name) }

        val roadRoutes = routingRepository.fetchRoutes(startCoord, endCoord)

        val routes = if (roadRoutes.isNotEmpty()) {
            RoadCorridorMapper.toCorridors(
                roadRoutes = roadRoutes,
                fromName = current.fromQuery,
                toName = current.toQuery,
                tariffs = tariffs,
                autoFarePerKm = tariffs.perKmRateFor(TransportType.AUTO_RICKSHAW, fallback = 11.0),
                cabFarePerKm = tariffs.perKmRateFor(TransportType.CAB_MINI, fallback = 16.0),
                busFlatFare = tariffs.baseFareFor(TransportType.BUS, fallback = 15.0)
            )
        } else {
            TrafficRouteEngine.generateTrafficRoutes(
                fromName = current.fromQuery,
                toName = current.toQuery,
                baseDistanceKm = distanceKm,
                city = city.name,
                startCoord = startCoord,
                endCoord = endCoord,
                tariffs = tariffs
            )
        }

        _trafficRoutes.value = routes
        _isUsingApproximateRoutes.value = roadRoutes.isEmpty()

        val previousId = _selectedTrafficRouteId.value
        if (routes.none { it.id == previousId }) {
            _selectedTrafficRouteId.value =
                routes.firstOrNull { it.isRecommended }?.id ?: routes.firstOrNull()?.id ?: ""
        }

        return routes
            .firstOrNull { it.isRecommended }
            ?.distanceKm
            ?.takeIf { roadRoutes.isNotEmpty() && it > 0.0 }
    }

    fun selectTrafficRoute(id: String) {
        if (_trafficRoutes.value.any { it.id == id }) {
            _selectedTrafficRouteId.value = id
        }
    }

    fun refreshTrafficConditions() {
        if (_isTrafficUpdating.value) return
        viewModelScope.launch {
            _isTrafficUpdating.value = true
            try {
                val distance = _routeState.value.distanceKm.takeIf { it > 0.0 } ?: DEFAULT_DISTANCE_KM
                // Re-query the routing provider: on a keyed setup this picks up live
                // congestion changes. The artificial delay that used to sit here only
                // simulated work.
                regenerateTrafficRoutes(distance)
            } finally {
                // Always clear the flag: a cancellation used to leave the refresh
                // control stuck in its loading state.
                _isTrafficUpdating.value = false
            }
        }
    }

    // ------------------------------------------------------------------
    // Overcharge audit
    // ------------------------------------------------------------------

    fun updateDriverQuoteText(text: String) {
        // Accept digits only. The old free-text field allowed "abc", which parsed
        // to a silent 100.0 default and produced a confident but wrong verdict.
        val sanitised = text.filter { it.isDigit() }.take(6)
        _overchargeState.value = _overchargeState.value.copy(driverQuoteText = sanitised)
        if (sanitised.isBlank()) {
            // Clear the stale verdict instead of leaving the previous result on
            // screen next to an empty input.
            _overchargeState.value = _overchargeState.value.copy(analysis = null)
        } else {
            runOverchargeAnalysis()
        }
    }

    fun setOverchargeTransport(type: TransportType) {
        if (type == _overchargeState.value.selectedTransport) return
        _overchargeState.value = _overchargeState.value.copy(selectedTransport = type)
        runOverchargeAnalysis()
    }

    fun setOverchargeDistance(distanceKm: Double) {
        val safeKm = distanceKm.coerceIn(0.5, 100.0)
        if (safeKm == _overchargeState.value.distanceKm) return
        _overchargeState.value = _overchargeState.value.copy(distanceKm = safeKm)
        runOverchargeAnalysis()
    }

    fun setOverchargeNightMode(isNight: Boolean) {
        if (isNight == _overchargeState.value.isNightMode) return
        _overchargeState.value = _overchargeState.value.copy(isNightMode = isNight)
        runOverchargeAnalysis()
    }

    /**
     * Pre-fills the audit from a comparison result so navigating from Compare or
     * the map lands on a populated screen rather than an empty form.
     */
    fun prepareOverchargeCheck(type: TransportType, fare: Double) {
        _overchargeState.value = _overchargeState.value.copy(
            selectedTransport = type,
            driverQuoteText = fare.coerceAtLeast(0.0).toInt().toString(),
            distanceKm = _routeState.value.distanceKm.takeIf { it > 0.0 }
                ?: _overchargeState.value.distanceKm,
            isNightMode = _routeState.value.isNightMode
        )
        runOverchargeAnalysis()
    }

    fun runOverchargeAnalysis() {
        val current = _overchargeState.value
        val quote = current.quoteValue
        if (quote == null) {
            _overchargeState.value = current.copy(analysis = null, isAnalyzing = false)
            return
        }

        overchargeJob?.cancel()
        overchargeJob = viewModelScope.launch {
            _overchargeState.value = _overchargeState.value.copy(isAnalyzing = true)

            val city = _selectedCity.value
            val tariffs = runCatching { repository.getTariffsForCitySync(city.name) }
                .getOrElse { InitialData.tariffsForCityOrFallback(city.name) }

            val tariff = tariffs.firstOrNull { it.transportType == current.selectedTransport }
                ?: tariffs.firstOrNull { it.transportType == TransportType.AUTO_RICKSHAW }
                ?: tariffs.firstOrNull()

            if (tariff == null) {
                _overchargeState.value = _overchargeState.value.copy(
                    analysis = null,
                    isAnalyzing = false
                )
                return@launch
            }

            val calculation = FareCalculatorEngine.calculateFare(
                tariff = tariff,
                distanceKm = current.distanceKm,
                forceNightMode = current.isNightMode
            )

            _overchargeState.value = _overchargeState.value.copy(
                analysis = OverchargeEngine.analyze(driverQuote = quote, calculation = calculation),
                isAnalyzing = false
            )
        }
    }

    // ------------------------------------------------------------------
    // Trip history & community reports
    // ------------------------------------------------------------------

    fun logTrip(
        from: String,
        to: String,
        transportType: TransportType,
        distanceKm: Double,
        durationMin: Int,
        estMin: Double,
        estMax: Double,
        actualPaid: Double,
        isNight: Boolean
    ) {
        viewModelScope.launch {
            val averageEstimate = (estMin + estMax) / 2.0
            runCatching {
                repository.saveTrip(
                    TripHistoryEntity(
                        fromLocation = from,
                        toLocation = to,
                        city = _selectedCity.value.name,
                        transportType = transportType,
                        distanceKm = distanceKm,
                        durationMinutes = durationMin,
                        estimatedFareMin = estMin,
                        estimatedFareMax = estMax,
                        actualFarePaid = actualPaid,
                        overchargeDifference = actualPaid - averageEstimate,
                        isNightTrip = isNight
                    )
                )
            }
        }
    }

    /** Saves the journey currently on screen, using the selected transport mode. */
    fun logCurrentTrip(result: FareCalculationResult, actualPaid: Double) {
        val current = _routeState.value
        logTrip(
            from = current.fromQuery,
            to = current.toQuery,
            transportType = result.transportType,
            distanceKm = current.distanceKm,
            durationMin = result.estimatedTimeMinutes,
            estMin = result.fareRangeMin,
            estMax = result.fareRangeMax,
            actualPaid = actualPaid,
            isNight = current.isNightMode
        )
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { runCatching { repository.deleteTrip(id) } }
    }

    fun clearAllTrips() {
        viewModelScope.launch { runCatching { repository.clearAllTrips() } }
    }

    /**
     * Persists a report using what the user actually entered. The expected fare
     * comes from the live tariff calculation rather than a hardcoded constant, so
     * the asked-vs-expected delta stored with the report is meaningful.
     */
    fun submitIncidentReport(draft: IncidentReportDraft) {
        viewModelScope.launch {
            val city = _selectedCity.value
            val current = _routeState.value
            val transport = _overchargeState.value.selectedTransport
            val distance = current.distanceKm.takeIf { it > 0.0 } ?: DEFAULT_DISTANCE_KM

            val tariffs = runCatching { repository.getTariffsForCitySync(city.name) }
                .getOrElse { InitialData.tariffsForCityOrFallback(city.name) }
            val tariff = tariffs.firstOrNull { it.transportType == transport } ?: tariffs.firstOrNull()

            val expectedFare = tariff?.let {
                FareCalculatorEngine.calculateFare(
                    tariff = it,
                    distanceKm = distance,
                    forceNightMode = current.isNightMode
                ).estimatedFare
            } ?: 0.0

            val askedFare = draft.askedFare ?: expectedFare

            val description = buildString {
                if (draft.vehicleNumber.isNotBlank()) {
                    append("[${draft.vehicleNumber}] ")
                }
                append(draft.description)
            }

            runCatching {
                repository.submitReport(
                    CommunityReportEntity(
                        city = city.name,
                        transportType = transport,
                        fromLocation = draft.location,
                        toLocation = current.toQuery.ifBlank { "Not specified" },
                        distanceKm = distance,
                        askedFare = askedFare,
                        expectedFare = expectedFare,
                        issueType = if (draft.askedFare != null && draft.askedFare > expectedFare) {
                            "Overcharging"
                        } else {
                            "Fare dispute"
                        },
                        description = description,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun upvoteReport(id: Long) {
        viewModelScope.launch { runCatching { repository.upvoteReport(id) } }
    }

    fun savePlace(place: SavedPlaceEntity) {
        viewModelScope.launch { runCatching { repository.savePlace(place) } }
    }

    private companion object {
        /** Median urban trip length; used only when no coordinates are available. */
        const val DEFAULT_DISTANCE_KM = 6.0
    }
}

/**
 * Endpoint coordinates for the journey currently on screen.
 *
 * The old code fell back to two fixed offsets from the city centre
 * (`+0.025/+0.045` and `-0.020/-0.030`) whenever a query did not match a
 * landmark. Those constants never changed, so the map drew the same pair of pins
 * for every unmatched journey and the on-map separation had no relationship to
 * the distance used for the fare — which is why the start and destination never
 * looked right.
 *
 * This resolves what it can and *derives* the rest from the real road distance:
 *  - both matched -> use both landmark positions
 *  - one matched  -> anchor on it and offset the other by the true distance
 *  - neither      -> straddle the city centre, separated by the true distance
 *
 * The bearing is derived from the place names, so a given journey always maps to
 * the same geometry instead of jumping around between recompositions.
 */
private fun resolveJourneyCoordinates(
    city: CityInfo,
    fromQuery: String,
    toQuery: String,
    roadDistanceKm: Double,
    resolvedOrigin: GeoPoint? = null,
    resolvedDestination: GeoPoint? = null
): Pair<GeoPoint, GeoPoint> {
    val origin = resolvedOrigin ?: city.resolveLandmark(fromQuery)?.let { GeoPoint(it.lat, it.lng) }
    val destination = resolvedDestination ?: city.resolveLandmark(toQuery)?.let { GeoPoint(it.lat, it.lng) }

    if (origin != null && destination != null) return origin to destination

    // Road distance includes the urban curvature multiplier applied in
    // RouteMatrixEngine; undo it to get the straight-line span the two pins
    // should actually sit apart on the map.
    val straightLineKm = (roadDistanceKm / URBAN_CURVATURE_FACTOR).coerceAtLeast(0.4)
    val bearingRad = stableBearingRadians(fromQuery, toQuery)

    return when {
        origin != null -> origin to origin.offsetBy(straightLineKm, bearingRad)

        // Project backwards from a known destination so the destination pin stays
        // exactly on its landmark.
        destination != null ->
            destination.offsetBy(straightLineKm, bearingRad + PI) to destination

        else -> {
            val centre = GeoPoint(city.defaultLat, city.defaultLng)
            centre.offsetBy(straightLineKm / 2.0, bearingRad + PI) to
                centre.offsetBy(straightLineKm / 2.0, bearingRad)
        }
    }
}

/**
 * Moves a point [distanceKm] along [bearingRad] using an equirectangular
 * approximation. Accurate to well under a metre at city scale and avoids pulling
 * in a geodesy dependency for what is a display-only offset.
 */
private fun GeoPoint.offsetBy(distanceKm: Double, bearingRad: Double): GeoPoint {
    val deltaLat = (distanceKm * cos(bearingRad)) / KM_PER_DEGREE_LATITUDE
    val lngScale = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
    val deltaLng = (distanceKm * sin(bearingRad)) / (KM_PER_DEGREE_LATITUDE * lngScale)
    return GeoPoint(
        latitude = (latitude + deltaLat).coerceIn(-85.0, 85.0),
        longitude = (longitude + deltaLng).coerceIn(-180.0, 180.0)
    )
}

/**
 * Deterministic bearing for a named journey, so the same origin/destination pair
 * always produces the same orientation on the map.
 */
private fun stableBearingRadians(fromQuery: String, toQuery: String): Double {
    val seed = "${fromQuery.trim().lowercase()}->${toQuery.trim().lowercase()}".hashCode()
    val bucket = ((seed % BEARING_BUCKETS) + BEARING_BUCKETS) % BEARING_BUCKETS
    return (2.0 * PI) * (bucket.toDouble() / BEARING_BUCKETS)
}

/** Matches the curvature multiplier RouteMatrixEngine applies to straight-line distance. */
private const val URBAN_CURVATURE_FACTOR = 1.30

/** One degree of latitude in kilometres. */
private const val KM_PER_DEGREE_LATITUDE = 111.32

private const val BEARING_BUCKETS = 16

/** Per-km rate for a transport mode, or [fallback] when the city has no such tariff. */
private fun List<TariffEntity>.perKmRateFor(type: TransportType, fallback: Double): Double =
    firstOrNull { it.transportType == type }?.perKmRate ?: fallback

/** Base fare for a transport mode, or [fallback] when the city has no such tariff. */
private fun List<TariffEntity>.baseFareFor(type: TransportType, fallback: Double): Double =
    firstOrNull { it.transportType == type }?.baseFare ?: fallback

/**
 * Matches a typed query against the city's landmarks with high specificity.
 * Matches exact names, names without parenthetical info, full area/name combinations, or strict prefix matches.
 * Avoids hijacking coordinates on loose partial substrings (e.g. generic words like "Hospital" or "Station").
 */
private fun CityInfo.resolveLandmark(query: String): LandmarkInfo? {
    val trimmed = query.trim()
    if (trimmed.length < 3) return null

    // 1. Exact full name match
    popularLandmarks.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }

    // 2. Exact match on base name before parentheses (e.g. "BBD University" for "BBD University (Babu Banarasi Das / BBDITM)")
    popularLandmarks.firstOrNull {
        val shortName = it.name.substringBefore("(").trim()
        shortName.equals(trimmed, ignoreCase = true)
    }?.let { return it }

    // 3. Exact combined name & area match
    popularLandmarks.firstOrNull {
        "${it.name}, ${it.area}".equals(trimmed, ignoreCase = true) ||
        "${it.area}, ${it.name}".equals(trimmed, ignoreCase = true)
    }?.let { return it }

    // 4. Strict prefix match on landmark name
    popularLandmarks.firstOrNull { landmark ->
        landmark.name.startsWith(trimmed, ignoreCase = true) ||
        landmark.name.substringBefore("(").trim().startsWith(trimmed, ignoreCase = true)
    }?.let { return it }

    return null
}

private fun inferCategoryFromName(name: String, secondary: String = ""): PlaceCategory {
    val text = "$name $secondary".lowercase()
    return when {
        text.contains("airport") || text.contains("terminal") || text.contains("aerodrome") -> PlaceCategory.AIRPORT
        text.contains("station") || text.contains("metro") || text.contains("railway") || text.contains("bus stand") || text.contains("isbt") || text.contains("junction") -> PlaceCategory.STATION
        text.contains("college") || text.contains("university") || text.contains("campus") || text.contains("school") || text.contains("institute") || text.contains("iit") || text.contains("iim") -> PlaceCategory.COLLEGE
        text.contains("hospital") || text.contains("clinic") || text.contains("medanta") || text.contains("aiims") || text.contains("health") || text.contains("medical") -> PlaceCategory.HOSPITAL
        text.contains("mall") || text.contains("market") || text.contains("bazaar") || text.contains("plaza") || text.contains("store") -> PlaceCategory.MARKET
        text.contains("tech park") || text.contains("cyber") || text.contains("tower") || text.contains("complex") || text.contains("office") || text.contains("sector") -> PlaceCategory.WORK
        else -> PlaceCategory.RECENT
    }
}


