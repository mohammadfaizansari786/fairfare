@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.InitialData
import com.example.data.model.CityInfo
import com.example.data.model.FareCalculationResult
import com.example.data.model.OverchargeCategory
import com.example.data.model.TransportType
import com.example.data.model.VerificationStatus
import com.example.ui.theme.FairFareTheme
import com.example.ui.theme.Spacing
import com.example.ui.util.formatRupeeRange
import com.example.ui.util.formatRupees

/**
 * Icon for a transport mode. Always carries a content description so screen
 * readers announce the mode rather than skipping it.
 */
@Composable
fun TransportIcon(
    type: TransportType,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val icon: ImageVector = when (type) {
        TransportType.BUS -> Icons.Filled.DirectionsBus
        TransportType.E_RICKSHAW, TransportType.AUTO_RICKSHAW -> Icons.Filled.ElectricRickshaw
        TransportType.BIKE_TAXI -> Icons.Filled.TwoWheeler
        TransportType.CAB_MINI, TransportType.CAB_SEDAN -> Icons.Filled.LocalTaxi
        TransportType.METRO -> Icons.Filled.Subway
        TransportType.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
        TransportType.MULTI_MODAL -> Icons.AutoMirrored.Filled.AltRoute
    }
    Icon(
        imageVector = icon,
        contentDescription = type.displayName,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Section heading used at the top of every content group. Consolidates what used
 * to be a dozen slightly different inline Text + Icon rows.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.tight))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * The single card surface used across the app. One elevation story (flat surface
 * + hairline outline) instead of a mix of Card, ElevatedCard and Surface with
 * different borders on every screen.
 */
@Composable
fun FairFareCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: Dp = Spacing.cardPadding,
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

/**
 * Verdict chip for an overcharge analysis. Colour, icon and wording move
 * together so the meaning cannot drift out of sync.
 */
@Composable
fun FairnessBadge(
    category: OverchargeCategory,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors
    val container = colors.containerFor(category)
    val onContainer = colors.onContainerFor(category)
    val icon = when (category) {
        OverchargeCategory.FAIR -> Icons.Filled.CheckCircle
        OverchargeCategory.SLIGHTLY_HIGH -> Icons.Filled.Info
        OverchargeCategory.HIGH, OverchargeCategory.VERY_HIGH -> Icons.Filled.Warning
    }

    Surface(
        color = container,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.hairline))
            Text(
                text = category.label,
                color = onContainer,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** Small tag used for "Cheapest", "Fastest", "Best value" and savings callouts. */
@Composable
fun FeatureBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.extraSmall,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.tight, vertical = 3.dp)
        )
    }
}

/**
 * Provenance indicator. This is the app's trust signal, so official rates read
 * differently from estimates and community submissions.
 */
@Composable
fun VerificationPill(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors
    val (label, tint) = when (status) {
        VerificationStatus.OFFICIAL -> "Official tariff" to colors.fair
        VerificationStatus.ESTIMATED -> "Estimated" to MaterialTheme.colorScheme.onSurfaceVariant
        VerificationStatus.COMMUNITY_SUBMITTED -> "Community rate" to MaterialTheme.colorScheme.tertiary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clearAndSetSemantics { contentDescription = "Source: $label" }
    ) {
        if (status == VerificationStatus.OFFICIAL) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(tint, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.hairline))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/**
 * City selector + day/night tariff toggle.
 *
 * Both controls are real buttons with proper semantics and >=40dp targets;
 * previously they were bare `clickable` rows that screen readers announced as
 * plain text and that were awkward to hit.
 */
@Composable
fun TopCityBar(
    currentCity: CityInfo,
    onCitySelected: (CityInfo) -> Unit,
    isNightMode: Boolean,
    onNightModeToggle: (Boolean) -> Unit,
    currentThemeMode: com.example.ui.theme.AppThemeMode = com.example.ui.theme.AppThemeMode.SYSTEM,
    onThemeModeSelected: ((com.example.ui.theme.AppThemeMode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var themeDropdownExpanded by remember { mutableStateOf(false) }

    val nightContainer by animateColorAsState(
        targetValue = if (isNightMode) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "night_toggle_container"
    )
    val nightContent = if (isNightMode) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.card),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
            ) {
                CitySelector(
                    currentCity = currentCity,
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    onCitySelected = onCitySelected,
                    modifier = Modifier.weight(1f)
                )

                if (onThemeModeSelected != null) {
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        Surface(
                            onClick = { themeDropdownExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(40.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Theme style: ${currentThemeMode.displayName}"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Change Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = themeDropdownExpanded,
                            onDismissRequest = { themeDropdownExpanded = false },
                            offset = DpOffset(x = (-120).dp, y = 4.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            com.example.ui.theme.AppThemeMode.values().forEach { mode ->
                                val isCurrent = mode == currentThemeMode
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.displayName,
                                            style = if (isCurrent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    trailingIcon = if (isCurrent) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        onThemeModeSelected(mode)
                                        themeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = { onNightModeToggle(!isNightMode) },
                    shape = MaterialTheme.shapes.small,
                    color = nightContainer,
                    modifier = Modifier
                        .height(40.dp)
                        .width(112.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.Switch
                            contentDescription = if (isNightMode) {
                                "Night tariff rate on (23:00-05:00 surcharge). Tap for day tariff rate."
                            } else {
                                "Day standard tariff rate on. Tap for night tariff rate."
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Filled.NightlightRound else Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = nightContent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isNightMode) "Night Fare" else "Day Fare",
                            style = MaterialTheme.typography.labelLarge,
                            color = nightContent,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CitySelector(
    currentCity: CityInfo,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCitySelected: (CityInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            onClick = { onExpandedChange(true) },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.card, vertical = Spacing.tight)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.tight))
                Text(
                    text = currentCity.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(Spacing.hairline))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Change city",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            InitialData.CITIES.forEach { city ->
                val isCurrent = city.name == currentCity.name
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = city.name,
                                style = if (isCurrent) {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                }
                            )
                            Text(
                                text = city.state,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = if (isCurrent) {
                        {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onCitySelected(city)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

/**
 * Expandable "how was this calculated" panel.
 *
 * Transparency is the product, so the breakdown always lists every component
 * that contributed to the total, and the disclosure control is a labelled button
 * rather than a tap anywhere on the card.
 */
@Composable
fun FareBreakdownCard(
    calculation: FareCalculationResult,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    FairFareCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fare breakdown",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(Spacing.hairline))
                VerificationPill(calculation.verificationStatus)
            }

            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "Hide" else "Details",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(Spacing.hairline))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = Spacing.card)) {
                BreakdownRow(
                    label = "Base fare",
                    value = formatRupees(calculation.baseFare)
                )
                if (calculation.distanceCharge > 0) {
                    BreakdownRow("Distance", formatRupees(calculation.distanceCharge))
                }
                if (calculation.waitingCharge > 0) {
                    BreakdownRow("Waiting", formatRupees(calculation.waitingCharge))
                }
                if (calculation.isNightApplied && calculation.nightCharge > 0) {
                    BreakdownRow("Night surcharge", "+" + formatRupees(calculation.nightCharge))
                }
                if (calculation.luggageCharge > 0) {
                    BreakdownRow("Luggage", "+" + formatRupees(calculation.luggageCharge))
                }
                if (calculation.extraCharges > 0) {
                    BreakdownRow("Tolls & parking", "+" + formatRupees(calculation.extraCharges))
                }

                Spacer(modifier = Modifier.height(Spacing.tight))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(Spacing.tight))

                BreakdownRow(
                    label = "Estimated total",
                    value = formatRupees(calculation.estimatedFare),
                    emphasise = true
                )
                BreakdownRow(
                    label = "Fair range",
                    value = formatRupeeRange(calculation.fareRangeMin, calculation.fareRangeMax)
                )

                if (calculation.officialSource.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.card))
                    Text(
                        text = "Source: ${calculation.officialSource}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    emphasise: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasise) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (emphasise) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(Spacing.tight))
        Text(
            text = value,
            style = if (emphasise) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (emphasise) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * Empty state placeholder. Previously lists simply rendered nothing when a
 * filter matched zero rows, which read as a broken screen.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.section),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.card))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.hairline))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(Spacing.card))
            action()
        }
    }
}

/**
 * Skeleton placeholder shown while fares recalculate. A shimmering block reads
 * as "working" far better than a spinner dropped in the middle of a list.
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 84.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
    )
}

/**
 * Origin/destination pair with the connecting rail. Used on results, map and
 * history screens so a journey always looks the same wherever it appears.
 */
@Composable
fun JourneyEndpoints(
    origin: String,
    destination: String,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "From $origin to $destination"
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.origin, CircleShape)
            )
            Spacer(modifier = Modifier.width(Spacing.card))
            Text(
                text = origin,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(2.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.destination, CircleShape)
            )
            Spacer(modifier = Modifier.width(Spacing.card))
            Text(
                text = destination,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}





