@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TransferWithinAStation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BusRouteEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Spacing
import com.example.ui.util.formatRupeeRange
import com.example.ui.util.formatRupees
import com.example.ui.viewmodel.FareViewModel

enum class TransitFilterTab(val label: String) {
    ALL("All Transit"),
    METRO("🚆 Metro Rail"),
    BUS("🚌 City Bus")
}

/**
 * City Transit Network & Explorer.
 *
 * Detailed multi-modal transit viewer supporting Metro Lines, City Buses,
 * station-by-station timelines, and stage fare calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTransitScreen(
    viewModel: FareViewModel,
    onBack: () -> Unit,
    onSelectBusRoute: (String, String) -> Unit
) {
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val allRoutes by viewModel.busRoutes.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf(TransitFilterTab.ALL) }
    var expandedRouteKey by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredRoutes = remember(allRoutes, searchQuery, selectedTab) {
        val query = searchQuery.trim()
        allRoutes.filter { route ->
            val isMetro = route.routeNumber.contains("Metro", ignoreCase = true)

            val tabMatches = when (selectedTab) {
                TransitFilterTab.ALL -> true
                TransitFilterTab.METRO -> isMetro
                TransitFilterTab.BUS -> !isMetro
            }

            val queryMatches = query.isEmpty() ||
                route.routeNumber.contains(query, ignoreCase = true) ||
                route.routeName.contains(query, ignoreCase = true) ||
                route.startStop.contains(query, ignoreCase = true) ||
                route.endStop.contains(query, ignoreCase = true) ||
                route.intermediateStopsCsv.contains(query, ignoreCase = true)

            tabMatches && queryMatches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "City transit network",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${selectedCity.name} · Metro & Buses",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val distinctRoutes = remember(filteredRoutes) {
            filteredRoutes.distinctBy { "${it.city}_${it.routeNumber}_${it.routeName}" }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(top = Spacing.card, bottom = Spacing.scrollBottom)
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search route, metro line, station or stop") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.gutter)
                        .testTag("input_bus_search")
                )
            }

            item(key = "filter_tabs") {
                Spacer(modifier = Modifier.height(Spacing.card))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
                ) {
                    TransitFilterTab.values().forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (filteredRoutes.isEmpty()) {
                item(key = "empty") {
                    BusEmptyState(
                        hasAnyRoutes = allRoutes.isNotEmpty(),
                        cityName = selectedCity.name,
                        query = searchQuery.trim(),
                        onClearSearch = {
                            searchQuery = ""
                            selectedTab = TransitFilterTab.ALL
                        }
                    )
                }
            } else {
                item(key = "count") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(
                        title = if (searchQuery.isBlank()) {
                            "${filteredRoutes.size} corridors available"
                        } else {
                            "${filteredRoutes.size} matching routes"
                        }
                    )
                    Spacer(modifier = Modifier.height(Spacing.card))
                }

                items(
                    items = distinctRoutes,
                    key = { route -> "bus_${route.id}_${route.city}_${route.routeNumber}_${route.routeName}" }
                ) { route ->
                    val routeKey = "${route.city}_${route.routeNumber}_${route.routeName}"
                    TransitRouteCard(
                        route = route,
                        isExpanded = expandedRouteKey == routeKey,
                        onToggleExpand = {
                            expandedRouteKey = if (expandedRouteKey == routeKey) null else routeKey
                        },
                        onCompareFares = { onSelectBusRoute(route.startStop, route.endStop) },
                        modifier = Modifier.padding(
                            horizontal = Spacing.gutter,
                            vertical = Spacing.hairline + 2.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BusEmptyState(
    hasAnyRoutes: Boolean,
    cityName: String,
    query: String,
    onClearSearch: () -> Unit
) {
    EmptyState(
        icon = if (hasAnyRoutes) Icons.Filled.SearchOff else Icons.Filled.DirectionsBus,
        title = if (hasAnyRoutes) "No matching transit routes" else "No transit data for $cityName",
        message = if (hasAnyRoutes) {
            "No routes match \"$query\" under this category."
        } else {
            "Transit route and metro data for this city will be added shortly."
        },
        action = if (hasAnyRoutes) {
            { TextButton(onClick = onClearSearch) { Text("Reset filters") } }
        } else {
            null
        }
    )
}

/**
 * Enhanced Transit Route Card with Metro Line and Bus badge support.
 */
@Composable
private fun TransitRouteCard(
    route: BusRouteEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCompareFares: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMetro = route.routeNumber.contains("Metro", ignoreCase = true)
    val stops = remember(route.intermediateStopsCsv, route.startStop, route.endStop) {
        val list = mutableListOf<String>()
        route.intermediateStopsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { list.add(it) }
        if (list.isEmpty()) {
            list.add(route.startStop)
            list.add(route.endStop)
        }
        list
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Transit Mode Icon & Badge
                Surface(
                    color = if (isMetro) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.tight, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isMetro) Icons.Filled.Subway else Icons.Filled.DirectionsBus,
                            contentDescription = null,
                            tint = if (isMetro) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = route.routeNumber,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isMetro) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.card))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.routeName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${route.startStop} ➔ ${route.endStop}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Hide stations" else "Show stations"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.card))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatRupeeRange(route.baseFare, route.maxFare),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.hairline))
                        Text(
                            text = "${route.firstBusTime}–${route.lastBusTime} · every ${route.frequencyMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(onClick = onCompareFares) {
                    Text(text = "Plan Journey", style = MaterialTheme.typography.labelLarge)
                }
            }

            TransitStopsTimeline(
                stops = stops,
                isMetro = isMetro,
                baseFare = route.baseFare,
                perStageFare = route.perStageFare,
                maxFare = route.maxFare,
                isVisible = isExpanded
            )
        }
    }
}

/**
 * Interactive Timeline with Station Details & Fare Stage Breakdown.
 */
@Composable
private fun TransitStopsTimeline(
    stops: List<String>,
    isMetro: Boolean,
    baseFare: Double,
    perStageFare: Double,
    maxFare: Double,
    isVisible: Boolean
) {
    AnimatedVisibility(visible = isVisible) {
        Column(modifier = Modifier.padding(top = Spacing.card)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.card),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${stops.size} ${if (isMetro) "Stations" else "Stops"}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Base ₹${baseFare.toInt()} · Stage increment ₹${perStageFare.toInt()} · Max ₹${maxFare.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.card))

            stops.forEachIndexed { index, stop ->
                val isTerminal = index == 0 || index == stops.lastIndex
                val isInterchange = stop.contains("Interchange", ignoreCase = true) ||
                    stop.contains("Railway", ignoreCase = true) ||
                    stop.contains("Airport", ignoreCase = true)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Line indicator node
                    Box(
                        modifier = Modifier
                            .size(if (isTerminal) 10.dp else if (isInterchange) 9.dp else 6.dp)
                            .background(
                                color = when {
                                    isTerminal -> MaterialTheme.colorScheme.primary
                                    isInterchange -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(Spacing.card))

                    Text(
                        text = stop,
                        style = if (isTerminal || isInterchange) {
                            MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (isInterchange) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.TransferWithinAStation,
                            contentDescription = "Interchange",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

