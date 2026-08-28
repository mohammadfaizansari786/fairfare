@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GeoPoint
import com.example.data.model.TrafficRouteOption
import com.example.ui.components.DeepSearchModal
import com.example.ui.components.EmptyState
import com.example.ui.components.FairFareCard
import com.example.ui.components.FeatureBadge
import com.example.ui.components.JourneyEndpoints
import com.example.ui.components.SearchTarget
import com.example.ui.components.SectionHeader
import com.example.ui.components.TomTomMapRouteView
import com.example.ui.theme.FairFareTheme
import com.example.ui.theme.Spacing
import com.example.ui.util.formatCoordinates
import com.example.ui.util.formatDuration
import com.example.ui.util.formatKm
import com.example.ui.util.formatRupees
import com.example.ui.viewmodel.FareViewModel

/** Which field the location search sheet is editing. */
private enum class SearchTargetKey { ORIGIN, DESTINATION }

private fun SearchTargetKey.toComponentTarget(): SearchTarget = when (this) {
    SearchTargetKey.ORIGIN -> SearchTarget.FROM_LOCATION
    SearchTargetKey.DESTINATION -> SearchTarget.TO_LOCATION
}

/**
 * Routes and traffic.
 *
 * Now a proper screen with a real top app bar — it previously faked one with a
 * Surface inside the scrolling list, so the back button scrolled out of reach.
 * The map/engine toggle is gone: one renderer, no developer-facing switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteMapScreen(
    viewModel: FareViewModel,
    onBack: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onCheckOvercharge: (fare: Double) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val trafficRoutes by viewModel.trafficRoutes.collectAsStateWithLifecycle()
    val selectedRouteId by viewModel.selectedTrafficRouteId.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isTrafficUpdating.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()

    val currentRoute = remember(trafficRoutes, selectedRouteId) {
        trafficRoutes.firstOrNull { it.id == selectedRouteId } ?: trafficRoutes.firstOrNull()
    }

    var showSearchModal by rememberSaveable { mutableStateOf(false) }
    var searchTarget by rememberSaveable { mutableStateOf(SearchTargetKey.DESTINATION) }

    DeepSearchModal(
        isOpen = showSearchModal,
        currentCity = selectedCity,
        searchTarget = searchTarget.toComponentTarget(),
        onDismiss = { showSearchModal = false },
        viewModel = viewModel,
        onLocationSelected = { landmarkName, _, coords ->
            val hasCoords = coords != null && (coords.latitude != 0.0 || coords.longitude != 0.0)
            when (searchTarget) {
                SearchTargetKey.ORIGIN -> {
                    if (hasCoords) {
                        viewModel.updateFromQuery(landmarkName, coords)
                    } else {
                        viewModel.selectFromPlace(
                            com.example.data.model.PlaceSearchResult(
                                id = "selected_$landmarkName",
                                name = landmarkName,
                                secondaryText = selectedCity.name,
                                category = com.example.data.model.PlaceCategory.RECENT,
                                coordinates = coords ?: GeoPoint(0.0, 0.0)
                            )
                        )
                    }
                }
                SearchTargetKey.DESTINATION -> {
                    if (hasCoords) {
                        viewModel.updateToQuery(landmarkName, coords)
                    } else {
                        viewModel.selectToPlace(
                            com.example.data.model.PlaceSearchResult(
                                id = "selected_$landmarkName",
                                name = landmarkName,
                                secondaryText = selectedCity.name,
                                category = com.example.data.model.PlaceCategory.RECENT,
                                coordinates = coords ?: GeoPoint(0.0, 0.0)
                            )
                        )
                    }
                }
            }
            viewModel.calculateRouteFares()
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Routes & traffic",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${selectedCity.name} · ${trafficRoutes.size} corridors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshTrafficConditions,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("btn_refresh_traffic")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh traffic"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        RouteMapContent(
            viewModel = viewModel,
            currentRoute = currentRoute,
            trafficRoutes = trafficRoutes,
            selectedRouteId = selectedRouteId,
            fromQuery = routeState.fromQuery,
            toQuery = routeState.toQuery,
            onNavigateToCompare = onNavigateToCompare,
            onCheckOvercharge = onCheckOvercharge,
            onEditOrigin = {
                searchTarget = SearchTargetKey.ORIGIN
                showSearchModal = true
            },
            onEditDestination = {
                searchTarget = SearchTargetKey.DESTINATION
                showSearchModal = true
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun RouteMapContent(
    viewModel: FareViewModel,
    currentRoute: TrafficRouteOption?,
    trafficRoutes: List<TrafficRouteOption>,
    selectedRouteId: String,
    fromQuery: String,
    toQuery: String,
    onNavigateToCompare: () -> Unit,
    onCheckOvercharge: (Double) -> Unit,
    onEditOrigin: () -> Unit,
    onEditDestination: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = Spacing.card, bottom = Spacing.scrollBottom)
    ) {
        item(key = "journey") {
            FairFareCard(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JourneyEndpoints(
                        origin = fromQuery,
                        destination = toQuery,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = viewModel::swapLocations,
                        modifier = Modifier.testTag("btn_swap_route")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapVert,
                            contentDescription = "Swap origin and destination",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.card))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                    OutlinedButton(
                        onClick = onEditOrigin,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Change from", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onEditDestination,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Change to", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (currentRoute == null) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Route,
                    title = "No routes yet",
                    message = "Set an origin and destination to map the corridors between them."
                )
            }
            return@LazyColumn
        }

        item(key = "map") {
            Spacer(modifier = Modifier.height(Spacing.card))
            TomTomMapRouteView(
                currentRoute = currentRoute,
                allRoutes = trafficRoutes,
                selectedRouteId = selectedRouteId,
                onSelectRoute = viewModel::selectTrafficRoute,
                onMapCoordinateSelected = { lat, lng ->
                    viewModel.updateToQuery("Pinned location (${formatCoordinates(lat, lng)})", GeoPoint(lat, lng))
                    viewModel.calculateRouteFares()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter)
                    .height(420.dp)
            )
        }

        item(key = "routes_header") {
            Spacer(modifier = Modifier.height(Spacing.section))
            SectionHeader(
                title = "Alternative routes",
                subtitle = "Tap to preview on the map"
            )
            Spacer(modifier = Modifier.height(Spacing.card))
        }

        items(
            items = trafficRoutes,
            key = { route -> route.id }
        ) { route ->
            RouteOptionCard(
                route = route,
                isSelected = route.id == selectedRouteId,
                onSelect = { viewModel.selectTrafficRoute(route.id) },
                modifier = Modifier.padding(
                    horizontal = Spacing.gutter,
                    vertical = Spacing.hairline + 2.dp
                )
            )
        }

        item(key = "actions") {
            Spacer(modifier = Modifier.height(Spacing.section))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.card)
            ) {
                OutlinedButton(
                    onClick = { onCheckOvercharge(currentRoute.estimatedAutoFare) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.minTouchTarget)
                        .testTag("btn_audit_route_fare")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Audit fare", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onNavigateToCompare,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(Spacing.minTouchTarget)
                        .testTag("btn_view_fare_breakdown")
                ) {
                    Text(text = "Compare fares", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * One corridor option.
 *
 * Congestion is expressed with both a colour and a word — colour alone is not
 * accessible, and the previous version used a green/red pill with no label.
 */
@Composable
private fun RouteOptionCard(
    route: TrafficRouteOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors
    val trafficColor = colors.colorFor(route.overallTraffic)

    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_route_${route.id}")
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatKm(route.distanceKm)} · ${formatDuration(route.totalDurationMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.tight))

                if (route.isRecommended) {
                    FeatureBadge(
                        text = "Best",
                        backgroundColor = colors.cheapestContainer,
                        textColor = colors.onCheapestContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.card))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(trafficColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = route.overallTraffic.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.tight))
                Text(
                    text = "${route.congestionPercentage}% congestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (route.trafficDelayMinutes > 0) {
                Spacer(modifier = Modifier.height(Spacing.tight))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ReportProblem,
                        contentDescription = null,
                        tint = trafficColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.hairline))
                    Text(
                        text = "${formatDuration(route.trafficDelayMinutes)} of delay expected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RouteFareEstimates(route = route, isVisible = isSelected)
        }
    }
}

@Composable
private fun RouteFareEstimates(
    route: TrafficRouteOption,
    isVisible: Boolean
) {
    AnimatedVisibility(visible = isVisible) {
        Column {
            Spacer(modifier = Modifier.height(Spacing.card))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                FareEstimate(
                    label = "Auto",
                    fare = route.estimatedAutoFare,
                    modifier = Modifier.weight(1f)
                )
                FareEstimate(
                    label = "Cab",
                    fare = route.estimatedCabFare,
                    modifier = Modifier.weight(1f)
                )
                FareEstimate(
                    label = "Bus",
                    fare = route.estimatedBusFare,
                    modifier = Modifier.weight(1f)
                )
            }

            if (route.roadConditions.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.card))
                Text(
                    text = route.roadConditions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FareEstimate(
    label: String,
    fare: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(Spacing.card)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRupees(fare),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

