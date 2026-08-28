@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlaceCategory
import com.example.data.model.PlaceSearchResult
import com.example.data.model.TripHistoryEntity
import kotlinx.coroutines.delay
import com.example.ui.components.DeepSearchModal
import com.example.ui.components.FairFareCard
import com.example.ui.components.SearchTarget
import com.example.ui.components.SectionHeader
import com.example.ui.components.TopCityBar
import com.example.ui.components.TransportIcon
import com.example.ui.theme.FairFareTheme
import com.example.ui.theme.Spacing
import com.example.ui.util.formatDuration
import com.example.ui.util.formatKm
import com.example.ui.util.formatRelativeTime
import com.example.ui.util.formatRupees
import com.example.ui.viewmodel.FareViewModel

/**
 * Home.
 *
 * Restructured around one job: enter a journey, get a fare. The route form is now
 * the first thing on screen — the decorative hero banner and the "TomTom SDK vs
 * Live Traffic" engine switcher were removed because they were developer-facing
 * and pushed the primary action below the fold.
 */
@Composable
fun HomeScreen(
    viewModel: FareViewModel,
    onNavigateToResults: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToOvercharge: () -> Unit,
    onNavigateToTransit: () -> Unit,
    onNavigateToTariffs: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val currentCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val trafficRoutes by viewModel.trafficRoutes.collectAsStateWithLifecycle()
    val selectedRouteId by viewModel.selectedTrafficRouteId.collectAsStateWithLifecycle()
    val recentTrips by viewModel.allTrips.collectAsStateWithLifecycle()

    val currentRoute = remember(trafficRoutes, selectedRouteId) {
        trafficRoutes.firstOrNull { it.id == selectedRouteId } ?: trafficRoutes.firstOrNull()
    }

    var quickQuote by rememberSaveable { mutableStateOf("") }
    var showSearchModal by rememberSaveable { mutableStateOf(false) }
    var searchTarget by rememberSaveable { mutableStateOf(SearchTarget.TO_LOCATION) }
    var initialSearchQuery by rememberSaveable { mutableStateOf("") }

    DeepSearchModal(
        isOpen = showSearchModal,
        initialQuery = initialSearchQuery,
        currentCity = currentCity,
        searchTarget = searchTarget,
        onDismiss = { showSearchModal = false },
        viewModel = viewModel,
        onLocationSelected = { landmarkName, target, coords ->
            val hasCoords = coords != null && (coords.latitude != 0.0 || coords.longitude != 0.0)
            when (target) {
                SearchTarget.FROM_LOCATION -> {
                    if (hasCoords) {
                        viewModel.updateFromQuery(landmarkName, coords)
                    } else {
                        viewModel.selectFromPlace(
                            PlaceSearchResult(
                                id = "selected_$landmarkName",
                                name = landmarkName,
                                secondaryText = currentCity.name,
                                category = PlaceCategory.RECENT,
                                coordinates = coords ?: com.example.data.model.GeoPoint(0.0, 0.0)
                            )
                        )
                    }
                }
                SearchTarget.TO_LOCATION, SearchTarget.EXPLORE_CORRIDOR -> {
                    if (hasCoords) {
                        viewModel.updateToQuery(landmarkName, coords)
                    } else {
                        viewModel.selectToPlace(
                            PlaceSearchResult(
                                id = "selected_$landmarkName",
                                name = landmarkName,
                                secondaryText = currentCity.name,
                                category = PlaceCategory.RECENT,
                                coordinates = coords ?: com.example.data.model.GeoPoint(0.0, 0.0)
                            )
                        )
                    }
                }
            }
            viewModel.calculateRouteFares()
            showSearchModal = false
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.scrollBottom)
    ) {
        item(key = "city_bar") {
            TopCityBar(
                currentCity = currentCity,
                onCitySelected = viewModel::selectCity,
                isNightMode = routeState.isNightMode,
                onNightModeToggle = viewModel::setNightMode,
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = viewModel::setThemeMode,
                // Home has no TopAppBar, so it owns the status bar inset itself.
                modifier = Modifier.statusBarsPadding()
            )
        }

        item(key = "route_form") {
            Spacer(modifier = Modifier.height(Spacing.gutter))
            RouteInputCard(
                fromQuery = routeState.fromQuery,
                toQuery = routeState.toQuery,
                distanceKm = routeState.distanceKm,
                isCalculating = routeState.isCalculating,
                onFromClear = { viewModel.updateFromQuery("") },
                onToClear = { viewModel.updateToQuery("") },
                onSwap = viewModel::swapLocations,
                onSearchOrigin = {
                    initialSearchQuery = ""
                    searchTarget = SearchTarget.FROM_LOCATION
                    showSearchModal = true
                },
                onSearchDestination = {
                    initialSearchQuery = ""
                    searchTarget = SearchTarget.TO_LOCATION
                    showSearchModal = true
                },
                onCompare = {
                    viewModel.calculateRouteFares()
                    onNavigateToResults()
                },
                onViewMap = {
                    viewModel.calculateRouteFares()
                    onNavigateToMap()
                },
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
        }

        routeState.errorMessage?.let { message ->
            item(key = "route_error") {
                Spacer(modifier = Modifier.height(Spacing.card))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.gutter)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.card)
                    )
                }
            }
        }

        if (currentCity.popularLandmarks.isNotEmpty()) {
            item(key = "landmark_chips") {
                Spacer(modifier = Modifier.height(Spacing.card))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    items(
                        items = currentCity.popularLandmarks.take(6),
                        key = { it.name }
                    ) { landmark ->
                        Surface(
                            onClick = {
                                viewModel.updateToQuery(landmark.name)
                                viewModel.calculateRouteFares()
                            },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = landmark.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.card,
                                    vertical = Spacing.tight
                                )
                            )
                        }
                    }
                }
            }
        }

        // The live map lives on the Routes screen only. Home previously embedded a
        // 220dp map here and Compare embedded another — three renderers of the same
        // tiles across the app, each re-fetching and re-animating. This is a compact
        // summary that links there instead.
        if (currentRoute != null) {
            item(key = "corridor_summary") {
                Spacer(modifier = Modifier.height(Spacing.section))
                CorridorSummaryCard(
                    routeTitle = currentRoute.title,
                    corridorCount = trafficRoutes.size,
                    trafficLabel = currentRoute.overallTraffic.label,
                    trafficColor = FairFareTheme.colors.colorFor(currentRoute.overallTraffic),
                    durationMinutes = currentRoute.totalDurationMinutes,
                    onClick = onNavigateToMap,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }
        }

        item(key = "quick_check") {
            Spacer(modifier = Modifier.height(Spacing.section))
            QuickFareCheckCard(
                quote = quickQuote,
                onQuoteChange = { input -> quickQuote = input.filter { it.isDigit() }.take(6) },
                onVerify = {
                    val parsed = quickQuote.toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        // Previously this silently substituted ₹120 and showed a
                        // verdict for a number the user never entered.
                        onShowMessage("Enter the fare the driver asked for")
                    } else {
                        viewModel.updateDriverQuoteText(quickQuote)
                        onNavigateToOvercharge()
                    }
                },
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
        }

        item(key = "shortcuts") {
            Spacer(modifier = Modifier.height(Spacing.section))
            SectionHeader(title = "Reference")
            Spacer(modifier = Modifier.height(Spacing.card))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.card)
            ) {
                ShortcutCard(
                    icon = Icons.Filled.DirectionsTransit,
                    title = "Metro & Bus transit",
                    subtitle = "Lines, stations & stage fares",
                    onClick = onNavigateToTransit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_bus_transit")
                )
                ShortcutCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Official tariffs",
                    subtitle = "Government rate cards",
                    onClick = onNavigateToTariffs,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_official_tariffs")
                )
            }
        }

        if (recentTrips.isNotEmpty()) {
            item(key = "history_header") {
                Spacer(modifier = Modifier.height(Spacing.section))
                SectionHeader(title = "Recent trips", icon = Icons.Filled.History)
                Spacer(modifier = Modifier.height(Spacing.card))
            }

            items(
                items = recentTrips.take(3),
                key = { trip -> trip.id }
            ) { trip ->
                RecentTripRow(
                    trip = trip,
                    onClick = {
                        viewModel.selectPresetRoute(trip.fromLocation, trip.toLocation)
                        onNavigateToResults()
                    },
                    modifier = Modifier.padding(
                        horizontal = Spacing.gutter,
                        vertical = Spacing.hairline
                    )
                )
            }
        }
    }
}

/**
 * Origin/destination entry.
 *
 * The two fields share one visual rail with a swap control between them, and the
 * computed distance is shown inline so the user can sanity-check the journey
 * before comparing fares.
 */
@Composable
private fun RouteInputCard(
    fromQuery: String,
    toQuery: String,
    distanceKm: Double,
    isCalculating: Boolean,
    onFromClear: () -> Unit,
    onToClear: () -> Unit,
    onSwap: () -> Unit,
    onSearchOrigin: () -> Unit,
    onSearchDestination: () -> Unit,
    onCompare: () -> Unit,
    onViewMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    FairFareCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Plan a journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (distanceKm > 0.0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${formatKm(distanceKm)} route",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Connected Journey Card Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Vertical Path Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF10B981), CircleShape) // Emerald Origin
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(36.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF10B981), Color(0xFFF43F5E))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFF43F5E), CircleShape) // Coral Destination
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Input Fields
                Column(modifier = Modifier.weight(1f)) {
                    // Pickup Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSearchOrigin)
                            .padding(vertical = 4.dp)
                            .testTag("input_from_location"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PICKUP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = fromQuery.ifBlank { "Choose pickup point..." },
                                fontSize = 13.5.sp,
                                fontWeight = if (fromQuery.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (fromQuery.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (fromQuery.isNotBlank()) {
                            IconButton(
                                onClick = onFromClear,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear origin",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Divider between Pickup and Destination
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(vertical = 2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    )

                    // Destination Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSearchDestination)
                            .padding(vertical = 4.dp)
                            .testTag("input_to_location"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DESTINATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = toQuery.ifBlank { "Where to? (e.g. Mantri Awas, Airport)" },
                                fontSize = 13.5.sp,
                                fontWeight = if (toQuery.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (toQuery.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (toQuery.isNotBlank()) {
                            IconButton(
                                onClick = onToClear,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear destination",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Swap Button Floating on the Right
                Surface(
                    onClick = onSwap,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = "Swap locations",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.card)
        ) {
            OutlinedButton(
                onClick = onViewMap,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("btn_view_traffic_map")
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Routes", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onCompare,
                enabled = !isCalculating,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.4f)
                    .height(46.dp)
                    .testTag("btn_compare_fares")
            ) {
                Text(
                    text = if (isCalculating) "Calculating..." else "Compare fares",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun categoryIcon(cat: PlaceCategory): ImageVector = when (cat) {
    PlaceCategory.STATION -> Icons.Filled.Train
    PlaceCategory.AIRPORT -> Icons.Filled.Flight
    PlaceCategory.MARKET -> Icons.Filled.LocalMall
    PlaceCategory.COLLEGE -> Icons.Filled.School
    PlaceCategory.HOSPITAL -> Icons.Filled.LocalHospital
    PlaceCategory.WORK -> Icons.Filled.Apartment
    PlaceCategory.HOME -> Icons.Filled.LocationOn
    PlaceCategory.RECENT, PlaceCategory.FAVORITE -> Icons.Filled.LocationOn
}


/** Enter a driver's quote and jump straight to the verdict. */
@Composable
private fun QuickFareCheckCard(
    quote: String,
    onQuoteChange: (String) -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier
) {
    FairFareCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        borderColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.tight))
            Column {
                Text(
                    text = "Check a quote",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Compare against the official rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.card))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.card)
        ) {
            OutlinedTextField(
                value = quote,
                onValueChange = onQuoteChange,
                label = { Text("Fare asked") },
                prefix = { Text("₹") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_driver_quote_quick")
            )

            Button(
                onClick = onVerify,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .height(56.dp)
                    .testTag("btn_check_quote_quick")
            ) {
                Text(text = "Check", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Compact stand-in for the live map.
 *
 * Carries the three facts the map was actually communicating at a glance — which
 * corridor is active, how congested it is, and how long it takes — and routes to
 * the single real map on the Routes screen.
 */
@Composable
private fun CorridorSummaryCard(
    routeTitle: String,
    corridorCount: Int,
    trafficLabel: String,
    trafficColor: Color,
    durationMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.card))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(trafficColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$trafficLabel · ${formatDuration(durationMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.tight))

            Text(
                text = if (corridorCount > 1) "$corridorCount routes" else "View map",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Tile linking to a reference screen. */
@Composable
private fun ShortcutCard(

    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.card))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One saved trip. Tapping it reloads that journey into the calculator. */
@Composable
private fun RecentTripRow(
    trip: TripHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TransportIcon(trip.transportType, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.card))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${trip.fromLocation} → ${trip.toLocation}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatKm(trip.distanceKm)} · ${formatRelativeTime(trip.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(Spacing.tight))
            Text(
                text = formatRupees(trip.actualFarePaid),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

