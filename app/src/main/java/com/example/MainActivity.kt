package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.TransportType
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppDestination
import com.example.ui.components.ReportIncidentDialog
import com.example.ui.screens.BusTransitScreen
import com.example.ui.screens.FareResultsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OfficialTariffScreen
import com.example.ui.screens.OverchargeCheckerScreen
import com.example.ui.screens.RouteMapScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FareViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val fareViewModel: FareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by fareViewModel.appThemeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                FairFareApp(viewModel = fareViewModel)
            }
        }
    }
}

/**
 * Root shell.
 *
 * Navigation state is saveable, so rotating the device or returning from the
 * background no longer throws the user back to Home. A back stack keeps Back
 * predictable: it unwinds where the user came from instead of always jumping
 * to Home from any depth.
 */
@Composable
fun FairFareApp(viewModel: FareViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    // A SnapshotStateList is not saveable by default, so persist it as a list of
    // destination names. Without this the back stack was lost on rotation and Back
    // always jumped to Home.
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<AppDestination>, String>(
            save = { stack -> stack.map(AppDestination::name) },
            restore = { names ->
                mutableStateListOf<AppDestination>().apply {
                    names.forEach { name ->
                        runCatching { AppDestination.valueOf(name) }.getOrNull()?.let(::add)
                    }
                }
            }
        )
    ) { mutableStateListOf<AppDestination>() }

    var showReportDialog by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun navigateTo(destination: AppDestination) {
        if (destination == currentDestination) return
        backStack.add(currentDestination)
        currentDestination = destination
    }

    fun navigateBack() {
        currentDestination = if (backStack.isEmpty()) {
            AppDestination.HOME
        } else {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            // Replace rather than queue: toasts used to pile up when the user
            // tapped several actions in quick succession.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    BackHandler(enabled = currentDestination != AppDestination.HOME || backStack.isNotEmpty()) {
        navigateBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        // Zero insets here: each destination owns its own top-bar/status-bar
        // handling. Letting this Scaffold apply them too double-padded every
        // screen that has its own TopAppBar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomNavBar(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    // A tab tap is a lateral move: reset the stack to
                    // Home -> destination instead of growing it without bound.
                    if (destination != currentDestination) {
                        backStack.clear()
                        if (destination != AppDestination.HOME) {
                            backStack.add(AppDestination.HOME)
                        }
                        currentDestination = destination
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(140))
                },
                label = "destination"
            ) { destination ->
                DestinationContent(
                    destination = destination,
                    viewModel = viewModel,
                    onNavigateTo = ::navigateTo,
                    onNavigateBack = ::navigateBack,
                    onShowMessage = ::showMessage,
                    onRequestReport = { showReportDialog = true }
                )
            }

            if (showReportDialog) {
                ReportIncidentDialog(
                    onDismiss = { showReportDialog = false },
                    onSubmit = { draft ->
                        viewModel.submitIncidentReport(draft)
                        showReportDialog = false
                        showMessage("Report submitted. Thanks for helping other passengers.")
                    }
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    viewModel: FareViewModel,
    onNavigateTo: (AppDestination) -> Unit,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onRequestReport: () -> Unit
) {
    when (destination) {
        AppDestination.HOME -> HomeScreen(
            viewModel = viewModel,
            onNavigateToResults = { onNavigateTo(AppDestination.COMPARE) },
            onNavigateToMap = { onNavigateTo(AppDestination.MAP) },
            onNavigateToOvercharge = { onNavigateTo(AppDestination.AUDIT) },
            onNavigateToTransit = { onNavigateTo(AppDestination.TRANSIT) },
            onNavigateToTariffs = { onNavigateTo(AppDestination.TARIFFS) },
            onShowMessage = onShowMessage
        )

        AppDestination.COMPARE -> FareResultsScreen(
            viewModel = viewModel,
            onBack = onNavigateBack,
            onCheckOverchargeForTransport = { type, fare ->
                viewModel.prepareOverchargeCheck(type, fare)
                onNavigateTo(AppDestination.AUDIT)
            },
            onNavigateToTrafficMap = { onNavigateTo(AppDestination.MAP) }
        )

        AppDestination.MAP -> RouteMapScreen(
            viewModel = viewModel,
            onBack = onNavigateBack,
            onNavigateToCompare = { onNavigateTo(AppDestination.COMPARE) },
            onCheckOvercharge = { fare ->
                viewModel.prepareOverchargeCheck(TransportType.AUTO_RICKSHAW, fare)
                onNavigateTo(AppDestination.AUDIT)
            },
            onShowMessage = onShowMessage
        )

        AppDestination.AUDIT -> OverchargeCheckerScreen(
            viewModel = viewModel,
            onBack = onNavigateBack,
            onNavigateToReport = onRequestReport,
            onShowMessage = onShowMessage
        )

        AppDestination.TRANSIT -> BusTransitScreen(
            viewModel = viewModel,
            onBack = onNavigateBack,
            onSelectBusRoute = { from, to ->
                viewModel.selectPresetRoute(from, to)
                onNavigateTo(AppDestination.COMPARE)
            }
        )

        AppDestination.TARIFFS -> OfficialTariffScreen(
            viewModel = viewModel,
            onBack = onNavigateBack
        )
    }
}


