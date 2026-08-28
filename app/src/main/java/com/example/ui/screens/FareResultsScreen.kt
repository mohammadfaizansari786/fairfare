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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
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
import com.example.data.model.FareCalculationResult
import com.example.data.model.RouteStep
import com.example.data.model.TransportType
import com.example.ui.components.EmptyState
import com.example.ui.components.FairFareCard
import com.example.ui.components.FareBreakdownCard
import com.example.ui.components.FeatureBadge
import com.example.ui.components.JourneyEndpoints
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeletonCard
import com.example.ui.components.TransportIcon
import com.example.ui.components.VerificationPill
import com.example.ui.theme.FairFareTheme
import com.example.ui.theme.Spacing
import com.example.ui.util.formatDuration
import com.example.ui.util.formatKm
import com.example.ui.util.formatRupeeRange
import com.example.ui.util.formatRupees
import com.example.ui.viewmodel.FareViewModel

/**
 * Fare comparison.
 *
 * The list is the point of this screen, so it leads with the results. The map is a
 * compact preview beneath the journey header rather than a 220dp block above the
 * fold, and selecting a mode expands its breakdown inline instead of appending a
 * detached card at the bottom of the scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FareResultsScreen(
    viewModel: FareViewModel,
    onBack: () -> Unit,
    onCheckOverchargeForTransport: (TransportType, Double) -> Unit,
    onNavigateToTrafficMap: () -> Unit
) {
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val trafficRoutes by viewModel.trafficRoutes.collectAsStateWithLifecycle()
    val selectedRouteId by viewModel.selectedTrafficRouteId.collectAsStateWithLifecycle()

    val currentRoute = remember(trafficRoutes, selectedRouteId) {
        trafficRoutes.firstOrNull { it.id == selectedRouteId } ?: trafficRoutes.firstOrNull()
    }

    // Persist the expanded mode across configuration changes.
    var expandedTransport by rememberSaveable { mutableStateOf<TransportType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fare comparison",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${selectedCity.name} · ${formatKm(routeState.distanceKm)}",
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
                    IconButton(onClick = onNavigateToTrafficMap) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AltRoute,
                            contentDescription = "Traffic corridors"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val distinctResults = remember(routeState.comparisonList) { routeState.comparisonList.distinctBy { it.transportType } }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                top = Spacing.card,
                bottom = Spacing.scrollBottom
            )
        ) {
            item(key = "journey") {
                FairFareCard(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JourneyEndpoints(
                            origin = routeState.fromQuery,
                            destination = routeState.toQuery,
                            modifier = Modifier.weight(1f)
                        )
                        if (routeState.isNightMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "Night rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.tight,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (routeState.isCalculating && !routeState.hasResults) {
                items(count = 4, key = { index -> "skeleton_$index" }) {
                    SkeletonCard(
                        modifier = Modifier.padding(
                            horizontal = Spacing.gutter,
                            vertical = Spacing.hairline + 2.dp
                        )
                    )
                }
            }

            if (!routeState.isCalculating && !routeState.hasResults) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = "No fares to compare",
                        message = routeState.errorMessage
                            ?: "Enter an origin and destination to see fare estimates.",
                        action = {
                            Button(onClick = onBack) { Text("Edit journey") }
                        }
                    )
                }
            }

            if (routeState.hasResults) {
                item(key = "results_header") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(
                        title = "Transport options",
                        subtitle = "${routeState.comparisonList.size} modes, cheapest first"
                    )
                    Spacer(modifier = Modifier.height(Spacing.card))
                }

                items(
                    items = distinctResults,
                    key = { result -> "fare_res_${result.transportType.name}" }
                ) { result ->
                    TransportComparisonCard(
                        result = result,
                        isExpanded = expandedTransport == result.transportType,
                        onToggle = {
                            expandedTransport = if (expandedTransport == result.transportType) {
                                null
                            } else {
                                result.transportType
                            }
                        },
                        onCheckOvercharge = {
                            onCheckOverchargeForTransport(result.transportType, result.estimatedFare)
                        },
                        modifier = Modifier.padding(
                            horizontal = Spacing.gutter,
                            vertical = Spacing.hairline + 2.dp
                        )
                    )
                }
            }

            routeState.multiModalRoute?.let { combo ->
                item(key = "multi_modal") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(title = "Cheaper with a change")
                    Spacer(modifier = Modifier.height(Spacing.card))
                    MultiModalCard(
                        title = combo.title,
                        totalFare = combo.totalFare,
                        totalMinutes = combo.totalTimeMinutes,
                        savings = combo.savingsComparedToCab,
                        steps = combo.steps,
                        modifier = Modifier.padding(horizontal = Spacing.gutter)
                    )
                }
            }

            // No embedded map here: the Routes screen owns the single map instance.
            // Compare previously rendered its own 200dp copy, which meant two live
            // tile renderers competing while the user scrolled a list of fares.
            if (currentRoute != null) {
                item(key = "route_link") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    Surface(
                        onClick = onNavigateToTrafficMap,
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.gutter)
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.cardPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.card))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "View on map",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = currentRoute.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One transport mode.
 *
 * The whole card is a single clickable target that expands the breakdown, and the
 * fare is the largest element — the previous layout gave equal weight to the mode
 * name, the fare, the range, the ETA and three badges, so nothing stood out.
 */
@Composable
private fun TransportComparisonCard(
    result: FareCalculationResult,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCheckOvercharge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors

    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.large,
        color = if (isExpanded) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (isExpanded) 2.dp else 1.dp,
            color = if (isExpanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_transport_${result.transportType.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    TransportIcon(result.transportType, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(Spacing.card))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.transportType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.hairline))
                        Text(
                            text = "${formatDuration(result.estimatedTimeMinutes)} · ${formatKm(result.distanceKm)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.tight))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupees(result.estimatedFare),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatRupeeRange(result.fareRangeMin, result.fareRangeMax),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (result.isCheapest || result.isFastest || result.isBestValue) {
                Spacer(modifier = Modifier.height(Spacing.card))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
                    if (result.isCheapest) {
                        FeatureBadge(
                            text = "Cheapest",
                            backgroundColor = colors.cheapestContainer,
                            textColor = colors.onCheapestContainer
                        )
                    }
                    if (result.isFastest) {
                        FeatureBadge(
                            text = "Fastest",
                            backgroundColor = colors.fastestContainer,
                            textColor = colors.onFastestContainer
                        )
                    }
                    if (result.isBestValue) {
                        FeatureBadge(
                            text = "Best value",
                            backgroundColor = colors.bestValueContainer,
                            textColor = colors.onBestValueContainer
                        )
                    }
                }
            }

            ExpandedFareDetails(
                result = result,
                isExpanded = isExpanded,
                onCheckOvercharge = onCheckOvercharge
            )
        }
    }
}

@Composable
private fun ExpandedFareDetails(
    result: FareCalculationResult,
    isExpanded: Boolean,
    onCheckOvercharge: () -> Unit
) {
    AnimatedVisibility(visible = isExpanded) {
        Column {
            Spacer(modifier = Modifier.height(Spacing.card))
            FareBreakdownCard(calculation = result, initiallyExpanded = true)
            Spacer(modifier = Modifier.height(Spacing.card))
            OutlinedButton(
                onClick = onCheckOvercharge,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Check a driver's quote",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (!isExpanded) {
        Spacer(modifier = Modifier.height(Spacing.tight))
        VerificationPill(result.verificationStatus)
    }
}

/** Multi-leg alternative (bus + last mile). */
@Composable
private fun MultiModalCard(
    title: String,
    totalFare: Double,
    totalMinutes: Int,
    savings: Double,
    steps: List<RouteStep>,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors

    FairFareCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.tight))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (savings > 0) {
                FeatureBadge(
                    text = "Saves ${formatRupees(savings)}",
                    backgroundColor = colors.cheapestContainer,
                    textColor = colors.onCheapestContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.card))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatRupees(totalFare),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatDuration(totalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.card))

        steps.forEach { step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = Spacing.hairline)
            ) {
                TransportIcon(step.transportType, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(Spacing.tight))
                Text(
                    text = step.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.tight))
                Text(
                    text = if (step.fare > 0) formatRupees(step.fare) else "Free",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

