@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OverchargeAnalysis
import com.example.data.model.OverchargeCategory
import com.example.data.model.TransportType
import com.example.ui.components.FairFareCard
import com.example.ui.components.FairnessBadge
import com.example.ui.components.FareBreakdownCard
import com.example.ui.components.TransportIcon
import com.example.ui.theme.FairFareTheme
import com.example.ui.theme.Spacing
import com.example.ui.util.formatKm
import com.example.ui.util.formatRupeeRange
import com.example.ui.util.formatRupees
import com.example.ui.util.formatSignedPercent
import com.example.ui.viewmodel.FareViewModel
import com.example.ui.viewmodel.OverchargeState

/**
 * Overcharge audit.
 *
 * The verdict is the answer the user came for, so it sits directly under the
 * quote input. Share is disabled until there is something to share (it previously
 * exported a generic advert), and no verdict appears until a valid amount has
 * been entered.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OverchargeCheckerScreen(
    viewModel: FareViewModel,
    onBack: () -> Unit,
    onNavigateToReport: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val state by viewModel.overchargeState.collectAsStateWithLifecycle()
    val analysis = state.analysis

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fare audit",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = selectedCity.name,
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
                    val shareText = analysis?.let { buildShareText(selectedCity.name, state, it) }
                    IconButton(
                        onClick = {
                            if (shareText != null) {
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            },
                                            "Share fare audit"
                                        )
                                    )
                                }.onFailure {
                                    // No share target installed: the old code let
                                    // ActivityNotFoundException crash the app.
                                    onShowMessage("No app available to share this")
                                }
                            }
                        },
                        enabled = shareText != null
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share result"
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
        AuditContent(
            viewModel = viewModel,
            onNavigateToReport = onNavigateToReport,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun AuditContent(
    viewModel: FareViewModel,
    onNavigateToReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.overchargeState.collectAsStateWithLifecycle()
    val analysis = state.analysis

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = Spacing.card, bottom = Spacing.scrollBottom)
    ) {
        item(key = "inputs") {
            AuditInputCard(
                quote = state.driverQuoteText,
                distanceKm = state.distanceKm,
                isNightMode = state.isNightMode,
                selectedTransport = state.selectedTransport,
                onQuoteChange = viewModel::updateDriverQuoteText,
                onDistanceChange = viewModel::setOverchargeDistance,
                onNightModeChange = viewModel::setOverchargeNightMode,
                onTransportChange = viewModel::setOverchargeTransport,
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
        }

        if (analysis == null) {
            item(key = "awaiting_input") {
                Spacer(modifier = Modifier.height(Spacing.section))
                Text(
                    text = "Enter the fare you were quoted to see whether it matches the " +
                        "official rate for this distance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }
        } else {
            item(key = "verdict") {
                Spacer(modifier = Modifier.height(Spacing.section))
                VerdictCard(
                    analysis = analysis,
                    onReportIssue = onNavigateToReport,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }

            item(key = "advice") {
                Spacer(modifier = Modifier.height(Spacing.card))
                AdviceCard(
                    icon = Icons.Filled.RecordVoiceOver,
                    title = "What to say",
                    body = analysis.bargainingAdvice,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }

            item(key = "breakdown") {
                Spacer(modifier = Modifier.height(Spacing.card))
                FareBreakdownCard(
                    calculation = analysis.breakdown,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }

            item(key = "legal") {
                Spacer(modifier = Modifier.height(Spacing.card))
                AdviceCard(
                    icon = Icons.Filled.Gavel,
                    title = "Your rights",
                    body = analysis.legalNoticeText,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }
        }
    }
}

private fun buildShareText(
    cityName: String,
    state: OverchargeState,
    analysis: OverchargeAnalysis
): String = buildString {
    appendLine("FairFare audit — $cityName")
    appendLine("${state.selectedTransport.displayName}, ${formatKm(state.distanceKm)}")
    appendLine("Driver asked: ${formatRupees(analysis.askedFare)}")
    appendLine(
        "Fair range: ${formatRupeeRange(analysis.expectedFareMin, analysis.expectedFareMax)}"
    )
    append("Verdict: ${analysis.category.label}")
}

/** Modes an overcharge check makes sense for; metro and walking are fixed-price. */
private val AUDIT_TRANSPORT_MODES = listOf(
    TransportType.AUTO_RICKSHAW,
    TransportType.E_RICKSHAW,
    TransportType.BIKE_TAXI,
    TransportType.CAB_MINI,
    TransportType.CAB_SEDAN,
    TransportType.BUS
)

/**
 * Quote, mode, distance and night-rate inputs.
 *
 * The distance slider has a readable value label, and the quote field is numeric
 * only — entering letters is no longer possible, which is what used to produce a
 * silent ₹100 default.
 */
@Composable
private fun AuditInputCard(
    quote: String,
    distanceKm: Double,
    isNightMode: Boolean,
    selectedTransport: TransportType,
    onQuoteChange: (String) -> Unit,
    onDistanceChange: (Double) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onTransportChange: (TransportType) -> Unit,
    modifier: Modifier = Modifier
) {
    FairFareCard(modifier = modifier) {
        Text(
            text = "Trip details",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(Spacing.card))

        OutlinedTextField(
            value = quote,
            onValueChange = onQuoteChange,
            label = { Text("Fare the driver asked") },
            prefix = { Text("₹") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_driver_quote")
        )

        Spacer(modifier = Modifier.height(Spacing.cardPadding))

        Text(
            text = "Transport",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.tight))

        TransportModeSelector(
            selectedTransport = selectedTransport,
            onTransportChange = onTransportChange
        )

        Spacer(modifier = Modifier.height(Spacing.cardPadding))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Distance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatKm(distanceKm),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = distanceKm.toFloat().coerceIn(1f, 40f),
            onValueChange = { value -> onDistanceChange(Math.round(value * 10.0) / 10.0) },
            valueRange = 1f..40f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_audit_distance")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Night rate", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "11 pm – 5 am",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isNightMode,
                onCheckedChange = onNightModeChange,
                modifier = Modifier.testTag("switch_audit_night")
            )
        }
    }
}

@Composable
private fun TransportModeSelector(
    selectedTransport: TransportType,
    onTransportChange: (TransportType) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        items(
            items = AUDIT_TRANSPORT_MODES,
            key = { type -> type.name }
        ) { type ->
            val isSelected = type == selectedTransport
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = { onTransportChange(type) },
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = if (isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = Spacing.card,
                        vertical = Spacing.tight
                    )
                ) {
                    TransportIcon(
                        type = type,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * The verdict.
 *
 * Asked vs expected sit side by side with the delta between them, because the
 * number the user needs is "how much over", not two figures they have to subtract
 * themselves.
 */
@Composable
private fun VerdictCard(
    analysis: OverchargeAnalysis,
    onReportIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairFareTheme.colors

    FairFareCard(
        modifier = modifier.testTag("card_overcharge_verdict"),
        borderColor = colors.accentFor(analysis.category)
    ) {
        FairnessBadge(analysis.category)

        Spacer(modifier = Modifier.height(Spacing.card))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Driver asked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupees(analysis.askedFare),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fair range",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupeeRange(analysis.expectedFareMin, analysis.expectedFareMax),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (analysis.category != OverchargeCategory.FAIR && analysis.differenceAmount > 0) {
            Spacer(modifier = Modifier.height(Spacing.card))
            Surface(
                color = colors.containerFor(analysis.category),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${formatRupees(analysis.differenceAmount)} over the fair maximum " +
                        "(${formatSignedPercent(analysis.differencePercentage)})",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onContainerFor(analysis.category),
                    modifier = Modifier.padding(Spacing.card)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.card))

        Text(
            text = analysis.fairnessExplanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (analysis.category != OverchargeCategory.FAIR) {
            Spacer(modifier = Modifier.height(Spacing.card))
            OutlinedButton(
                onClick = onReportIssue,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.minTouchTarget)
                    .testTag("btn_report_overcharge")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Report this", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Advice / legal panel. Same shape for both so they read as a pair. */
@Composable
private fun AdviceCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    FairFareCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.tight))
            Text(text = title, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(Spacing.tight))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

