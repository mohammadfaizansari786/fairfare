package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Spacing

/**
 * Top-level destinations.
 *
 * Only five appear in the bar — six tabs at 10sp labels was cramped and the
 * labels truncated on small screens. Tariffs is reachable from Home and from
 * the Audit screen, which is where users actually look for it.
 */
enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val inBottomBar: Boolean = true
) {
    HOME("Home", Icons.Filled.Home),
    COMPARE("Compare", Icons.Filled.Tune),
    MAP("Routes", Icons.AutoMirrored.Filled.AltRoute),
    AUDIT("Audit", Icons.Filled.Shield),
    TRANSIT("Transit", Icons.Filled.DirectionsBus),
    TARIFFS("Tariffs", Icons.AutoMirrored.Filled.MenuBook, inBottomBar = false);

    companion object {
        val bottomBarDestinations: List<AppDestination> = entries.filter { it.inBottomBar }
    }
}

/**
 * Bottom navigation.
 *
 * Built with `selectable` + `Role.Tab` so TalkBack announces "tab, 2 of 5,
 * selected" instead of reading an unlabelled clickable row, and every target is
 * a full-height touch area rather than a 32dp icon.
 */
@Composable
fun AppBottomNavBar(
    currentDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.tight, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppDestination.bottomBarDestinations.forEach { destination ->
                    NavBarItem(
                        destination = destination,
                        isSelected = currentDestination == destination,
                        onClick = { onDestinationSelected(destination) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    destination: AppDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "nav_indicator"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav_content"
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 40.dp,
        label = "nav_indicator_width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(Spacing.minTouchTarget + 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag("nav_tab_${destination.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

