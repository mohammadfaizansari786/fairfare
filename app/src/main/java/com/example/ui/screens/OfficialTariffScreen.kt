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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TariffEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.FairFareCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TopCityBar
import com.example.ui.components.TransportIcon
import com.example.ui.components.VerificationPill
import com.example.ui.theme.Spacing
import com.example.ui.util.formatKm
import com.example.ui.util.formatRupees
import com.example.ui.viewmodel.FareViewModel

/**
 * Official tariff reference.
 *
 * The city bar stays here because tariffs are city-specific and switching cities
 * is the main action on this screen. An empty-state now covers cities with no
 * seeded rate card instead of rendering a blank list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialTariffScreen(
    viewModel: FareViewModel,
    onBack: () -> Unit
) {
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val cityTariffs by viewModel.cityTariffs.collectAsStateWithLifecycle()
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()

    var expandedTariffKey by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Official tariffs",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Government rate cards",
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
        val distinctTariffs = remember(cityTariffs) { cityTariffs.distinctBy { it.transportType } }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.scrollBottom)
        ) {
            item(key = "city_bar") {
                TopCityBar(
                    currentCity = selectedCity,
                    onCitySelected = viewModel::selectCity,
                    isNightMode = routeState.isNightMode,
                    onNightModeToggle = viewModel::setNightMode
                )
            }

            item(key = "notice") {
                Spacer(modifier = Modifier.height(Spacing.gutter))
                FairFareCard(
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    borderColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Policy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.tight))
                        Text(
                            text = "Rates for ${selectedCity.name}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.tight))
                    Text(
                        text = "These are the published RTO rates. Drivers are required to " +
                            "charge by the meter or by these rates, whichever applies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (distinctTariffs.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "No published rates",
                        message = "Tariff data for ${selectedCity.name} has not been added yet."
                    )
                }
            } else {
                item(key = "list_header") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(title = "${distinctTariffs.size} transport modes")
                    Spacer(modifier = Modifier.height(Spacing.card))
                }

                items(
                    items = distinctTariffs,
                    key = { tariff -> "tariff_${tariff.id}_${tariff.city}_${tariff.transportType.name}" }
                ) { tariff ->
                    val tariffKey = "${tariff.city}_${tariff.transportType.name}"
                    TariffCard(
                        tariff = tariff,
                        isExpanded = expandedTariffKey == tariffKey,
                        onToggleExpand = {
                            expandedTariffKey = if (expandedTariffKey == tariffKey) {
                                null
                            } else {
                                tariffKey
                            }
                        },
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

/** One transport mode's rate card. */
@Composable
private fun TariffCard(
    tariff: TariffEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggleExpand,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
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
                    TransportIcon(tariff.transportType, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(Spacing.card))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tariff.transportType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    VerificationPill(tariff.verificationStatus)
                }

                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Filled.ExpandLess
                    } else {
                        Icons.Filled.ExpandMore
                    },
                    contentDescription = if (isExpanded) "Hide details" else "Show details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.card))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                TariffStat(
                    label = "Base",
                    value = formatRupees(tariff.baseFare),
                    caption = "first ${formatKm(tariff.baseDistanceKm)}",
                    modifier = Modifier.weight(1f)
                )
                TariffStat(
                    label = "Per km",
                    value = formatRupees(tariff.perKmRate),
                    caption = "after base",
                    modifier = Modifier.weight(1f)
                )
            }

            TariffDetails(tariff = tariff, isVisible = isExpanded)
        }
    }
}

@Composable
private fun TariffDetails(
    tariff: TariffEntity,
    isVisible: Boolean
) {
    AnimatedVisibility(visible = isVisible) {
        Column(modifier = Modifier.padding(top = Spacing.card)) {
            TariffDetailRow("Minimum fare", formatRupees(tariff.minFare))
            TariffDetailRow(
                label = "Waiting",
                value = "${formatRupees(tariff.waitingRatePerHour)} per hour"
            )
            TariffDetailRow(
                label = "Night surcharge",
                value = "+${tariff.nightChargePercent.toInt()}% " +
                    "(${tariff.nightStartHour}:00–${tariff.nightEndHour}:00)"
            )
            TariffDetailRow(
                label = "Extra luggage",
                value = "${formatRupees(tariff.luggageRatePerItem)} per bag"
            )

            if (tariff.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.tight))
                Text(
                    text = tariff.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.tight))
            Text(
                text = "Source: ${tariff.officialSource}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Updated ${tariff.lastUpdatedDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TariffStat(
    label: String,
    value: String,
    caption: String,
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
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TariffDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

