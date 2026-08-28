package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CityInfo
import com.example.data.model.GeoPoint
import com.example.data.model.PlaceCategory
import com.example.data.model.PlaceSearchResult
import com.example.ui.viewmodel.FareViewModel
import kotlinx.coroutines.delay

enum class SearchTarget {
    FROM_LOCATION,
    TO_LOCATION,
    EXPLORE_CORRIDOR
}

/**
 * High-Density Location Discovery Modal for sub-localities, colonies,
 * educational campuses, hospitals, and commercial sectors with categorized filters and GPS lock.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeepSearchModal(
    isOpen: Boolean,
    currentCity: CityInfo,
    searchTarget: SearchTarget,
    onDismiss: () -> Unit,
    onLocationSelected: (landmarkName: String, target: SearchTarget, coordinates: GeoPoint?) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    viewModel: FareViewModel? = null
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchTextFieldValue by remember(isOpen, initialQuery) {
        mutableStateOf(
            TextFieldValue(
                text = initialQuery,
                selection = TextRange(initialQuery.length)
            )
        )
    }
    val searchQuery = searchTextFieldValue.text
    var selectedCategoryFilter by remember { mutableStateOf<PlaceCategory?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Start with local city landmarks
    val defaultPlaces = remember(currentCity) {
        currentCity.popularLandmarks.mapIndexed { idx, lm ->
            PlaceSearchResult(
                id = "city_${lm.name}_$idx",
                name = lm.name,
                secondaryText = "${lm.area}, ${currentCity.name}",
                category = lm.category,
                coordinates = GeoPoint(lm.lat, lm.lng)
            )
        }
    }

    var searchResults by remember { mutableStateOf(defaultPlaces) }

    // Live search when query changes
    LaunchedEffect(searchQuery, currentCity) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            searchResults = defaultPlaces
            isSearching = false
            return@LaunchedEffect
        }

        // 1. Immediately show instant local matching landmarks / places
        val instant = viewModel?.searchPlaceSuggestionsInstant(trimmed).orEmpty()
        val local = defaultPlaces.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
            it.secondaryText.contains(trimmed, ignoreCase = true)
        }
        val immediate = (instant + local).distinctBy { "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}" }
        if (immediate.isNotEmpty()) {
            searchResults = immediate
        }

        // 2. Query live Google Maps & routing geocoding in background
        if (trimmed.length >= 2 && viewModel != null) {
            isSearching = true
            val results = viewModel.searchPlaceSuggestions(trimmed)
            if (results.isNotEmpty()) {
                searchResults = (immediate + results).distinctBy { "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}" }
            }
            isSearching = false
        } else {
            isSearching = false
        }
    }

    val filteredResults = remember(searchResults, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            searchResults
        } else {
            searchResults.filter { it.category == selectedCategoryFilter }
        }
    }

    val targetBadgeColor = when (searchTarget) {
        SearchTarget.FROM_LOCATION -> Color(0xFF10B981) // Emerald Green
        SearchTarget.TO_LOCATION -> Color(0xFFF43F5E)   // Coral Rose
        SearchTarget.EXPLORE_CORRIDOR -> MaterialTheme.colorScheme.primary
    }

    val targetTitle = when (searchTarget) {
        SearchTarget.FROM_LOCATION -> "Set Pickup Point"
        SearchTarget.TO_LOCATION -> "Where are you going?"
        SearchTarget.EXPLORE_CORRIDOR -> "Explore Corridor"
    }

    val targetSubtitle = when (searchTarget) {
        SearchTarget.FROM_LOCATION -> "Choose your starting location in ${currentCity.name}"
        SearchTarget.TO_LOCATION -> "Find your destination in ${currentCity.name}"
        SearchTarget.EXPLORE_CORRIDOR -> "Explore routes & fares in ${currentCity.name}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Modern Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(targetBadgeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = targetTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = targetSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Ultra-Sleek Search Input Field
            OutlinedTextField(
                value = searchTextFieldValue,
                onValueChange = { newVal ->
                    searchTextFieldValue = newVal
                    val trimmed = newVal.text.trim()
                    if (trimmed.isEmpty()) {
                        searchResults = defaultPlaces
                    } else {
                        val instant = viewModel?.searchPlaceSuggestionsInstant(trimmed).orEmpty()
                        val local = defaultPlaces.filter {
                            it.name.contains(trimmed, ignoreCase = true) ||
                            it.secondaryText.contains(trimmed, ignoreCase = true)
                        }
                        val immediate = (instant + local).distinctBy { "${it.name.trim().lowercase()}_${it.secondaryText.trim().lowercase()}" }
                        if (immediate.isNotEmpty()) {
                            searchResults = immediate
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = if (searchTarget == SearchTarget.FROM_LOCATION) "Search pickup e.g. Mantri Awas, Charbagh..." else "Search destination e.g. Barabanki, Airport...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Surface(
                            onClick = {
                                searchTextFieldValue = TextFieldValue("", selection = TextRange(0))
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("deepsearch_input_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sleek Horizontal Category Filters
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    CategoryPillChip(
                        label = "All",
                        icon = Icons.Filled.LocationOn,
                        isSelected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null }
                    )
                }
                item {
                    CategoryPillChip(
                        label = "Metro & Rail",
                        icon = Icons.Filled.Train,
                        isSelected = selectedCategoryFilter == PlaceCategory.STATION,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == PlaceCategory.STATION) null else PlaceCategory.STATION
                        }
                    )
                }
                item {
                    CategoryPillChip(
                        label = "Airports",
                        icon = Icons.Filled.Flight,
                        isSelected = selectedCategoryFilter == PlaceCategory.AIRPORT,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == PlaceCategory.AIRPORT) null else PlaceCategory.AIRPORT
                        }
                    )
                }
                item {
                    CategoryPillChip(
                        label = "Colleges",
                        icon = Icons.Filled.School,
                        isSelected = selectedCategoryFilter == PlaceCategory.COLLEGE,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == PlaceCategory.COLLEGE) null else PlaceCategory.COLLEGE
                        }
                    )
                }
                item {
                    CategoryPillChip(
                        label = "Markets & Malls",
                        icon = Icons.Filled.LocalMall,
                        isSelected = selectedCategoryFilter == PlaceCategory.MARKET,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == PlaceCategory.MARKET) null else PlaceCategory.MARKET
                        }
                    )
                }
                item {
                    CategoryPillChip(
                        label = "Hospitals",
                        icon = Icons.Filled.LocalHospital,
                        isSelected = selectedCategoryFilter == PlaceCategory.HOSPITAL,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == PlaceCategory.HOSPITAL) null else PlaceCategory.HOSPITAL
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current GPS Location Quick Action (Highlight Card)
            Surface(
                onClick = {
                    val originLm = currentCity.popularLandmarks.firstOrNull()
                    val gpsName = "${originLm?.name ?: currentCity.name} (Current GPS)"
                    val coords = originLm?.let { GeoPoint(it.lat, it.lng) }
                        ?: GeoPoint(currentCity.defaultLat, currentCity.defaultLng)
                    onLocationSelected(gpsName, searchTarget, coords)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_deepsearch_use_gps")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Current GPS Location",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "High-accuracy live pin in ${currentCity.name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.NearMe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom typed place option if query has no exact match
            if (searchQuery.isNotBlank() && filteredResults.none { it.name.equals(searchQuery, ignoreCase = true) }) {
                Surface(
                    onClick = {
                        onLocationSelected(searchQuery.trim(), searchTarget, null)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use \"$searchQuery\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Search custom query in ${currentCity.name}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Results Section Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "POPULAR IN ${currentCity.name.uppercase()}" else "SEARCH RESULTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${filteredResults.size} places",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fluid Results List
            if (filteredResults.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching places found",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try typing the locality, colony, or station name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredResults, key = { it.id }) { place ->
                        Surface(
                            onClick = {
                                onLocationSelected(place.name, searchTarget, place.coordinates)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("landmark_item_${place.name.filter { it.isLetterOrDigit() }}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            categoryColor(place.category).copy(alpha = 0.14f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(place.category),
                                        contentDescription = null,
                                        tint = categoryColor(place.category),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = place.secondaryText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Select",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPillChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun categoryIcon(cat: PlaceCategory): ImageVector {
    return when (cat) {
        PlaceCategory.STATION -> Icons.Filled.Train
        PlaceCategory.AIRPORT -> Icons.Filled.Flight
        PlaceCategory.MARKET -> Icons.Filled.LocalMall
        PlaceCategory.COLLEGE -> Icons.Filled.School
        PlaceCategory.HOSPITAL -> Icons.Filled.LocalHospital
        PlaceCategory.WORK -> Icons.Filled.Apartment
        PlaceCategory.HOME -> Icons.Filled.LocationOn
        PlaceCategory.RECENT, PlaceCategory.FAVORITE -> Icons.Filled.LocationOn
    }
}

@Composable
private fun categoryColor(cat: PlaceCategory): Color = when (cat) {
    PlaceCategory.STATION -> Color(0xFF3B82F6) // Blue
    PlaceCategory.AIRPORT -> Color(0xFF06B6D4) // Cyan
    PlaceCategory.MARKET -> Color(0xFFF59E0B)  // Amber
    PlaceCategory.HOME -> Color(0xFF10B981)    // Emerald
    PlaceCategory.COLLEGE -> Color(0xFF8B5CF6) // Purple
    PlaceCategory.WORK -> Color(0xFF6366F1)    // Indigo
    PlaceCategory.HOSPITAL -> Color(0xFFEF4444)// Red
    PlaceCategory.RECENT, PlaceCategory.FAVORITE -> MaterialTheme.colorScheme.primary
}
