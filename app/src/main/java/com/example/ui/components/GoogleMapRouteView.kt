package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.BuildConfig
import com.example.data.model.GeoPoint
import com.example.data.model.TrafficLevel
import com.example.data.model.TrafficRouteOption
import com.example.data.model.TrafficSegment
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Map tile API key.
 *
 * Supplied by the Secrets Gradle plugin from `.env`, so the value is never
 * committed. `.env.example` provides a `MY_…` placeholder for checkouts without a
 * `.env`; anything of that shape counts as unconfigured and the tile layers below
 * fall back to the keyless CDNs (Carto / OpenStreetMap), so the map still renders
 * without a key.
 */
private val TOMTOM_API_KEY: String = BuildConfig.TOMTOM_API_KEY

private val hasTomTomKey: Boolean
    get() = TOMTOM_API_KEY.isNotBlank() && !TOMTOM_API_KEY.startsWith("MY_")

/**
 * Base map styles.
 *
 * URLs are resolved through [tileUrlTemplate] rather than stored as enum
 * constructor arguments: the TomTom key is read at runtime from BuildConfig, and
 * enum entry initialisation can run before top-level properties in this file are
 * assigned, which would bake an empty key into the templates.
 */
enum class TomTomMapStyle(
    val title: String,
    val isDark: Boolean
) {
    GOOGLE_MAPS(title = "Google Maps", isDark = false),
    GOOGLE_DARK(title = "Google Dark", isDark = true),
    GOOGLE_HYBRID(title = "Google Hybrid", isDark = true),
    TOMTOM_NIGHT(title = "Dark Navigation", isDark = true),
    CARTO_VOYAGER(title = "Carto Voyager", isDark = false),
    OPEN_STREET(title = "OpenStreetMap", isDark = false)
}

private fun TomTomMapStyle.tileUrlTemplate(): String = when (this) {
    TomTomMapStyle.GOOGLE_MAPS ->
        "https://mt{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"

    TomTomMapStyle.GOOGLE_DARK ->
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"

    TomTomMapStyle.GOOGLE_HYBRID ->
        "https://mt{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}"

    TomTomMapStyle.TOMTOM_NIGHT -> if (hasTomTomKey) {
        "https://api.tomtom.com/map/1/tile/basic/night/{z}/{x}/{y}.png?key=$TOMTOM_API_KEY&tileSize=256"
    } else {
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
    }

    TomTomMapStyle.CARTO_VOYAGER ->
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png"

    TomTomMapStyle.OPEN_STREET ->
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
}

/**
 * Live traffic flow overlay. Empty when no key is configured, in which case the
 * overlay is simply not drawn instead of requesting a URL that returns 403.
 */
private fun trafficTileUrl(zoom: Int, x: Int, y: Int): String = if (hasTomTomKey) {
    "https://api.tomtom.com/traffic/map/4/tile/flow/relative0/$zoom/$x/$y.png" +
        "?key=$TOMTOM_API_KEY&tileSize=256"
} else {
    ""
}

/**
 * Resilient Real-World Geographic Map Engine for Jetpack Compose
 * 
 * Features:
 * 1. Web Mercator projection matching real GPS coordinates (Latitude/Longitude).
 * 2. Official Google Maps Demo Tile Display API + TomTom + Carto + OpenStreetMap.
 * 3. Official TomTom Real-Time Traffic Flow raster tile layer overlay with toggle.
 * 4. Dynamic procedural vector fallback cartography (streets, waterways, highways, compass rose)
 *    so the map is always visually rich, crisp, and responsive even during network latency or offline mode.
 * 5. Real-time traffic congestion glow, speed indicators, animated navigation puck with radar pulse, and interactive gestures (pan, pinch-to-zoom, tap-to-pin, re-center).
 */
@Composable
fun TomTomMapRouteView(
    currentRoute: TrafficRouteOption?,
    allRoutes: List<TrafficRouteOption>,
    selectedRouteId: String,
    onSelectRoute: (String) -> Unit,
    onMapCoordinateSelected: ((lat: Double, lng: Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var selectedMapStyle by remember { mutableStateOf(TomTomMapStyle.GOOGLE_MAPS) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var showLayersMenu by remember { mutableStateOf(false) }

    // Interactive Pan & Zoom State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (selectedMapStyle.isDark) Color(0xFF11131A) else Color(0xFFF3EFE9),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .testTag("tomtom_map_container")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val viewportW = with(density) { maxWidth.toPx() }.coerceAtLeast(200f)
            val viewportH = with(density) { maxHeight.toPx() }.coerceAtLeast(200f)

            // 1. Gather the coordinates that define the framing.
            val activeRouteForBounds = allRoutes.firstOrNull { it.id == selectedRouteId }
                ?: currentRoute
                ?: allRoutes.firstOrNull()

            val allPoints = remember(activeRouteForBounds) {
                buildList {
                    activeRouteForBounds?.let { route ->
                        route.startPoint?.let(::add)
                        addAll(route.geoPoints)
                        route.endPoint?.let(::add)
                    }
                    if (isEmpty()) {
                        add(GeoPoint(26.8900, 81.0500))
                        add(GeoPoint(26.8530, 80.9450))
                    }
                }
            }

            val minLat = allPoints.minOf { it.latitude }
            val maxLat = allPoints.maxOf { it.latitude }
            val minLng = allPoints.minOf { it.longitude }
            val maxLng = allPoints.maxOf { it.longitude }

            val centerLat = (minLat + maxLat) / 2.0
            val centerLng = (minLng + maxLng) / 2.0

            LaunchedEffect(activeRouteForBounds?.id, centerLat, centerLng) {
                zoomScale = 1.0f
                panOffsetX = 0f
                panOffsetY = 0f
            }

            // 2. Compute Base Web Mercator Zoom Level
            val baseZoom = remember(minLat, maxLat, minLng, maxLng, viewportW, viewportH) {
                calculateBaseZoom(minLat, maxLat, minLng, maxLng, viewportW, viewportH)
            }

            // Effective integer tile zoom and sub-tile scaling
            val targetZoomDouble = (baseZoom + (ln(zoomScale.toDouble()) / ln(2.0))).coerceIn(9.0, 18.0)
            val tileZoom = targetZoomDouble.roundToInt().coerceIn(9, 18)
            val tileScale = (2.0.pow(targetZoomDouble - tileZoom)).toFloat()

            val centerWorld = latLngToWorldPixel(centerLat, centerLng, tileZoom)
            val screenCenterX = viewportW / 2f + panOffsetX
            val screenCenterY = viewportH / 2f - 18f + panOffsetY

            // 3. Compute Visible Map Tiles
            val tileSize = 256f * tileScale
            val maxTileCoord = (1 shl tileZoom) - 1

            val calcMinTileX = (floor((centerWorld.x - screenCenterX / tileScale) / 256.0).toInt()).coerceIn(0, maxTileCoord)
            val calcMaxTileX = (ceil((centerWorld.x + (viewportW - screenCenterX) / tileScale) / 256.0).toInt()).coerceIn(0, maxTileCoord)
            val calcMinTileY = (floor((centerWorld.y - screenCenterY / tileScale) / 256.0).toInt()).coerceIn(0, maxTileCoord)
            val calcMaxTileY = (ceil((centerWorld.y + (viewportH - screenCenterY) / tileScale) / 256.0).toInt()).coerceIn(0, maxTileCoord)

            val minTileX = min(calcMinTileX, calcMaxTileX)
            val maxTileX = max(calcMinTileX, calcMaxTileX)
            val minTileY = min(calcMinTileY, calcMaxTileY)
            val maxTileY = max(calcMinTileY, calcMaxTileY)

            val visibleTiles = remember(tileZoom, minTileX, maxTileX, minTileY, maxTileY, selectedMapStyle) {
                val list = mutableListOf<TileInfo>()
                val subdomains = if (selectedMapStyle == TomTomMapStyle.GOOGLE_MAPS || selectedMapStyle == TomTomMapStyle.GOOGLE_HYBRID) {
                    listOf("0", "1", "2", "3")
                } else {
                    listOf("a", "b", "c", "d")
                }
                for (tx in minTileX..maxTileX) {
                    for (ty in minTileY..maxTileY) {
                        val s = subdomains[kotlin.math.abs(tx + ty) % subdomains.size]
                        val url = selectedMapStyle.tileUrlTemplate()
                            .replace("{s}", s)
                            .replace("{z}", tileZoom.toString())
                            .replace("{x}", tx.toString())
                            .replace("{y}", ty.toString())
                        list.add(TileInfo(tx, ty, tileZoom, url, trafficTileUrl(tileZoom, tx, ty)))
                    }
                }
                list
            }

            // Coordinate Projection Function (GPS -> Screen Offset)
            fun project(pt: GeoPoint): Offset {
                val world = latLngToWorldPixel(pt.latitude, pt.longitude, tileZoom)
                val screenX = screenCenterX + (world.x - centerWorld.x) * tileScale
                val screenY = screenCenterY + (world.y - centerWorld.y) * tileScale
                return Offset(screenX.toFloat(), screenY.toFloat())
            }

            // Reverse Projection Function (Screen Offset -> GPS Lat/Lng)
            fun unproject(screenOffset: Offset): GeoPoint {
                val worldX = (screenOffset.x - screenCenterX) / tileScale + centerWorld.x
                val worldY = (screenOffset.y - screenCenterY) / tileScale + centerWorld.y
                return worldPixelToLatLng(worldX, worldY, tileZoom)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.6f, 5.5f)
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                        }
                    }
                    .pointerInput(allRoutes, selectedRouteId) {
                        detectTapGestures(
                            onDoubleTap = {
                                zoomScale = (zoomScale * 1.45f).coerceAtMost(5.5f)
                            },
                            onLongPress = { offset ->
                                val geo = unproject(offset)
                                onMapCoordinateSelected?.invoke(geo.latitude, geo.longitude)
                            },
                            onTap = { offset ->
                                val thresholdPx = 36f * density.density
                                val hitRoutes = allRoutes.mapNotNull { route ->
                                    val points = buildList {
                                        route.startPoint?.let(::add)
                                        addAll(route.geoPoints)
                                        route.endPoint?.let(::add)
                                    }
                                    if (points.size < 2) return@mapNotNull null
                                    val screenPoints = points.map { project(it) }
                                    val distance = distanceToPolyline(offset, screenPoints)
                                    if (distance <= thresholdPx) {
                                        route to distance
                                    } else null
                                }

                                val closestRoute = hitRoutes.minByOrNull { it.second }?.first
                                if (closestRoute != null) {
                                    onSelectRoute(closestRoute.id)
                                }
                            }
                        )
                    }
            ) {
                // Layer 1: Real-World Web Mercator Map Tiles (Google / TomTom / Carto / OSM)
                MapTileLayerView(
                    visibleTiles = visibleTiles,
                    screenCenterX = screenCenterX,
                    screenCenterY = screenCenterY,
                    centerWorldX = centerWorld.x,
                    centerWorldY = centerWorld.y,
                    tileScale = tileScale,
                    tileSize = tileSize,
                    isTrafficEnabled = isTrafficEnabled,
                    density = density,
                    context = context
                )

                // Stroke widths scale with zoom but are clamped
                val zoomStrokeScale = zoomScale.coerceIn(0.85f, 1.6f)

                // Layer 2: Animated Navigation Route Overlays, Chevrons & Pins
                MapOverlayCanvas(
                    allRoutes = allRoutes,
                    activeRoute = activeRouteForBounds,
                    zoomStrokeScale = zoomStrokeScale,
                    isTrafficEnabled = isTrafficEnabled,
                    isDark = selectedMapStyle.isDark,
                    project = ::project
                )
            } // Close interactive map container

            // Top-Left: Minimalist Map Style & Traffic Indicator Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF10121A).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isTrafficEnabled) Color(0xFF00E676) else Color(0xFF757575),
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = selectedMapStyle.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Top-Right: Map Controls (Traffic Toggle, Re-Center, Map Theme Style Selector)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Traffic Toggle Button
                Surface(
                    shape = CircleShape,
                    color = if (isTrafficEnabled) Color(0xFFDF1B12) else Color(0xFF1B1D28).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { isTrafficEnabled = !isTrafficEnabled }
                        .testTag("btn_toggle_traffic")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Traffic,
                            contentDescription = "Toggle Live Traffic",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Re-Center / Target Reset Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1B1D28).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            zoomScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                        .testTag("btn_recenter_map")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Re-Center Map",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Map Style / Theme Selector (Dropdown anchored stably with fixed offset & width)
                Box(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1B1D28).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { showLayersMenu = true }
                            .testTag("btn_map_layers")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = "Map Style Layers",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLayersMenu,
                        onDismissRequest = { showLayersMenu = false },
                        offset = DpOffset(x = (-165).dp, y = 4.dp),
                        modifier = Modifier.width(200.dp)
                    ) {
                        TomTomMapStyle.entries.forEach { style ->
                            val isSelected = style == selectedMapStyle
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (style) {
                                            TomTomMapStyle.GOOGLE_MAPS -> Icons.Filled.Map
                                            TomTomMapStyle.GOOGLE_DARK -> Icons.Filled.Nightlight
                                            TomTomMapStyle.GOOGLE_HYBRID -> Icons.Filled.Satellite
                                            TomTomMapStyle.TOMTOM_NIGHT -> Icons.Filled.Nightlight
                                            TomTomMapStyle.CARTO_VOYAGER -> Icons.Filled.Navigation
                                            TomTomMapStyle.OPEN_STREET -> Icons.Filled.Public
                                        },
                                        contentDescription = null,
                                        tint = when (style) {
                                            TomTomMapStyle.GOOGLE_MAPS -> Color(0xFF4285F4)
                                            TomTomMapStyle.GOOGLE_DARK -> Color(0xFF7986CB)
                                            TomTomMapStyle.GOOGLE_HYBRID -> Color(0xFF00E676)
                                            TomTomMapStyle.TOMTOM_NIGHT -> Color(0xFF90CAF9)
                                            TomTomMapStyle.CARTO_VOYAGER -> Color(0xFFFFA000)
                                            TomTomMapStyle.OPEN_STREET -> Color(0xFF43A047)
                                        }
                                    )
                                },
                                text = {
                                    Text(
                                        text = style.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    selectedMapStyle = style
                                    showLayersMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Center-Right: Floating Zoom In/Out Controls (+ / -) sitting safely above the bottom docked card
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 66.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF141620).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { zoomScale = (zoomScale * 1.35f).coerceAtMost(5.5f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF141620).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { zoomScale = (zoomScale / 1.35f).coerceAtLeast(0.6f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Bottom: Clean, minimalistic docked Route Summary Card
            Surface(
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                color = Color(0xFF10121A).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentRoute?.title ?: "Fastest Transit Corridor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentRoute?.distanceKm ?: 0.0} km • ${currentRoute?.totalDurationMinutes ?: 25} mins" +
                                if ((currentRoute?.trafficDelayMinutes ?: 0) > 0) " (${currentRoute?.trafficDelayMinutes}m delay)" else "",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (currentRoute?.overallTraffic) {
                            TrafficLevel.FREE_FLOW -> Color(0xFF00C853).copy(alpha = 0.22f)
                            TrafficLevel.MODERATE -> Color(0xFFFF9100).copy(alpha = 0.22f)
                            TrafficLevel.HEAVY, TrafficLevel.SEVERE -> Color(0xFFFF1744).copy(alpha = 0.22f)
                            null -> Color(0xFF00C853).copy(alpha = 0.22f)
                        },
                        border = BorderStroke(
                            0.8.dp,
                            when (currentRoute?.overallTraffic) {
                                TrafficLevel.FREE_FLOW -> Color(0xFF00E676)
                                TrafficLevel.MODERATE -> Color(0xFFFFAB00)
                                TrafficLevel.HEAVY, TrafficLevel.SEVERE -> Color(0xFFFF1744)
                                null -> Color(0xFF00E676)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(
                                        color = when (currentRoute?.overallTraffic) {
                                            TrafficLevel.FREE_FLOW -> Color(0xFF00E676)
                                            TrafficLevel.MODERATE -> Color(0xFFFFAB00)
                                            TrafficLevel.HEAVY, TrafficLevel.SEVERE -> Color(0xFFFF1744)
                                            null -> Color(0xFF00E676)
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = currentRoute?.overallTraffic?.label ?: "Clear Flow",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders an authentic procedural vector cartographic background layer (streets, waterways, coordinates)
 * that is permanently visible regardless of network latency or tile loading state.
 */
private fun DrawScope.drawBaseProceduralCartography(
    isDark: Boolean,
    screenCenterX: Float,
    screenCenterY: Float,
    tileScale: Float,
    viewportW: Float,
    viewportH: Float
) {
    val bgFill = if (isDark) Color(0xFF1E212B) else Color(0xFFEBE8E1)
    val gridLine = if (isDark) Color(0xFF282D3B) else Color(0xFFDFDBD2)

    // 1. Clean background landmass
    drawRect(color = bgFill, size = Size(viewportW, viewportH))

    // 2. Subtle coordinate grid
    val blockStep = 64f * tileScale.coerceIn(0.7f, 1.5f)
    var gridX = (screenCenterX % blockStep)
    while (gridX < viewportW) {
        drawLine(
            color = gridLine,
            start = Offset(gridX, 0f),
            end = Offset(gridX, viewportH),
            strokeWidth = 1f
        )
        gridX += blockStep
    }
    var gridY = (screenCenterY % blockStep)
    while (gridY < viewportH) {
        drawLine(
            color = gridLine,
            start = Offset(0f, gridY),
            end = Offset(viewportW, gridY),
            strokeWidth = 1f
        )
        gridY += blockStep
    }
}

/**
 * Data Holder for Map Tiles
 */
private data class TileInfo(
    val x: Int,
    val y: Int,
    val zoom: Int,
    val url: String,
    val trafficUrl: String = ""
)

/**
 * Double-Precision Data Holder for World Pixel coordinates
 */
/**
 * Route line palette, matched to Google Maps' navigation styling.
 *
 * A single casing colour plus one fill, rather than the previous translucent-glow
 * stack. Traffic is expressed as segment overlays on top of the blue.
 */
private val RouteBlue = Color(0xFF1A73E8)
private val RouteCasingLight = Color(0xFF1557B0)
private val RouteCasingDark = Color(0xFF0B2C52)

private val AltRouteFillLight = Color(0xFF9AA0A6)
private val AltRouteFillDark = Color(0xFF7A8288)
private val AltRouteCasingLight = Color(0xFF6E7479)
private val AltRouteCasingDark = Color(0xFF3C4043)

/** Traffic flow colours for per-segment overlays. */
private val TrafficModerateAmber = Color(0xFFF9AB00)
private val TrafficHeavyRed = Color(0xFFE8483B)
private val TrafficSevereMaroon = Color(0xFF9C1F16)

/** Destination pin fill. */
private val DestinationPin = Color(0xFF202124)

/** Neutral intermediate waypoint dot. */
private val WaypointDot = Color(0xFF5F6368)

// Stroke widths in px at 1x zoom. Casing is ~1.6x the fill, which is the ratio
// Google uses and what makes the line read as a road with an outline.
private const val ROUTE_CASING_WIDTH = 15f
private const val ROUTE_FILL_WIDTH = 9.5f
private const val ALT_CASING_WIDTH = 11f
private const val ALT_FILL_WIDTH = 6.5f

/**
 * Calculates the shortest screen-pixel distance from a point to a polyline.
 */
private fun distanceToPolyline(point: Offset, polyline: List<Offset>): Float {
    if (polyline.size < 2) return Float.MAX_VALUE
    var minDistance = Float.MAX_VALUE
    for (i in 0 until polyline.size - 1) {
        val a = polyline[i]
        val b = polyline[i + 1]
        val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        val dist = if (l2 == 0f) {
            (point - a).getDistance()
        } else {
            val t = (((point.x - a.x) * (b.x - a.x) + (point.y - a.y) * (b.y - a.y)) / l2).coerceIn(0f, 1f)
            val projection = Offset(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
            (point - projection).getDistance()
        }
        if (dist < minDistance) {
            minDistance = dist
        }
    }
    return minDistance
}

/**
 * Builds a rounded path through the given points.
 *
 * Provider geometry has enough vertices to trace roads faithfully, so this only
 * needs to soften the joins. When a route falls back to the synthetic curve (few
 * vertices, wide spacing) the same smoothing prevents visible kinks.
 *
 * Quadratic segments through the midpoints give C1 continuity without adding
 * source points.
 */
private fun List<Offset>.toSmoothPath(): Path {
    val path = Path()
    if (isEmpty()) return path

    path.moveTo(first().x, first().y)
    if (size == 1) return path
    if (size == 2) {
        path.lineTo(this[1].x, this[1].y)
        return path
    }

    // Curve through the midpoint of each pair, using the shared vertex as the
    // quadratic control point.
    for (i in 1 until size - 1) {
        val current = this[i]
        val next = this[i + 1]
        val midX = (current.x + next.x) / 2f
        val midY = (current.y + next.y) / 2f
        path.quadraticTo(current.x, current.y, midX, midY)
    }

    // Final vertex is a control point for the run into the true endpoint, so the
    // line terminates exactly on the destination marker.
    val penultimate = this[size - 2]
    val last = last()
    path.quadraticTo(penultimate.x, penultimate.y, last.x, last.y)

    return path
}

/**
 * Overlays congested stretches on top of the base route line.
 *
 * [TrafficSegment.startPercent] and [TrafficSegment.endPercent] describe where
 * along the route each stretch sits, so slice the projected points to that span
 * and stroke it in the flow colour. Free-flowing segments are skipped — leaving
 * the blue visible is what makes the amber and red stretches stand out.
 */
private fun DrawScope.drawTrafficSegments(
    screenPoints: List<Offset>,
    segments: List<TrafficSegment>,
    strokeWidth: Float
) {
    if (screenPoints.size < 2) return

    segments.forEach { segment ->
        val color = when (segment.trafficLevel) {
            TrafficLevel.FREE_FLOW -> null
            TrafficLevel.MODERATE -> TrafficModerateAmber
            TrafficLevel.HEAVY -> TrafficHeavyRed
            TrafficLevel.SEVERE -> TrafficSevereMaroon
        } ?: return@forEach

        val slice = screenPoints.slice(
            startPercent = segment.startPercent,
            endPercent = segment.endPercent
        )
        if (slice.size < 2) return@forEach

        drawPath(
            path = slice.toSmoothPath(),
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Points covering a fractional span of the polyline, with interpolated endpoints so
 * a segment boundary that falls between two vertices does not snap to the nearest
 * one.
 */
private fun List<Offset>.slice(startPercent: Float, endPercent: Float): List<Offset> {
    if (size < 2) return this

    val from = startPercent.coerceIn(0f, 1f)
    val to = endPercent.coerceIn(0f, 1f)
    if (to <= from) return emptyList()

    val lastIndex = size - 1
    val startScaled = from * lastIndex
    val endScaled = to * lastIndex

    fun interpolate(scaled: Float): Offset {
        val index = scaled.toInt().coerceIn(0, lastIndex - 1)
        val t = scaled - index
        val a = this[index]
        val b = this[index + 1]
        return Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }

    return buildList {
        add(interpolate(startScaled))
        for (i in (kotlin.math.ceil(startScaled).toInt())..(kotlin.math.floor(endScaled).toInt())) {
            if (i in 0..lastIndex) add(this@slice[i])
        }
        add(interpolate(endScaled))
    }.dedupeConsecutiveOffsets()
}

/** Drops coincident points left behind by interpolation at a slice boundary. */
private fun List<Offset>.dedupeConsecutiveOffsets(): List<Offset> {
    if (size < 2) return this
    return filterIndexed { index, point ->
        index == 0 || (point - this[index - 1]).getDistance() > 0.5f
    }
}

private data class WorldPoint(
    val x: Double,
    val y: Double
)

/**
 * Collapses repeated coordinates.
 *
 * Prepending [TrafficRouteOption.startPoint] and appending
 * [TrafficRouteOption.endPoint] duplicates the first and last polyline vertices
 * whenever the generator already included them, which produced a zero-length
 * segment. Those break `atan2` heading maths for the animated vehicle and give
 * `StrokeCap.Round` a degenerate segment to render.
 */
private fun List<GeoPoint>.dedupeConsecutive(): List<GeoPoint> {
    if (size < 2) return this
    val epsilon = 1e-7
    return filterIndexed { index, point ->
        if (index == 0) return@filterIndexed true
        val previous = this[index - 1]
        kotlin.math.abs(point.latitude - previous.latitude) > epsilon ||
            kotlin.math.abs(point.longitude - previous.longitude) > epsilon
    }
}

/**
 * Point at a fractional position along the polyline, measured in vertex steps.
 *
 * Used to place waypoints that carry no explicit coordinate. Interpolating along
 * the line keeps them on the drawn corridor; the previous bounding-box lerp put
 * them out in open space whenever the route curved.
 */
private fun List<GeoPoint>.pointAtRatio(ratio: Float): GeoPoint {
    if (isEmpty()) return GeoPoint(0.0, 0.0)
    if (size == 1) return first()

    val clamped = ratio.coerceIn(0f, 1f)
    val scaled = clamped * (size - 1)
    val index = scaled.toInt().coerceIn(0, size - 2)
    val t = scaled - index

    val a = this[index]
    val b = this[index + 1]
    return GeoPoint(
        latitude = a.latitude + (b.latitude - a.latitude) * t,
        longitude = a.longitude + (b.longitude - a.longitude) * t
    )
}

/**
 * Web Mercator Projection: Converts GPS Latitude/Longitude into World Pixel coordinates at a given Zoom
 */
private fun latLngToWorldPixel(lat: Double, lng: Double, zoom: Int): WorldPoint {
    val mapSize = 256.0 * (1 shl zoom)
    val worldX = ((lng + 180.0) / 360.0) * mapSize

    val sinLat = sin(lat * PI / 180.0).coerceIn(-0.9999, 0.9999)
    val worldY = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * mapSize

    return WorldPoint(worldX, worldY)
}

/**
 * Reverse Web Mercator Projection: Converts World Pixel coordinates at a given Zoom back into GPS Latitude/Longitude
 */
private fun worldPixelToLatLng(worldX: Double, worldY: Double, zoom: Int): GeoPoint {
    val mapSize = 256.0 * (1 shl zoom)
    val lng = (worldX / mapSize) * 360.0 - 180.0

    val n = PI - (2.0 * PI * worldY) / mapSize
    val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))

    return GeoPoint(lat, lng)
}

/**
 * Optimal Mercator zoom to fit the journey bounding box.
 *
 * Padding accounts for the marker artwork, not just the frame edge: the origin
 * pin draws a pulsing halo out to ~34px, so a 60px inset let the pin visually
 * touch (or clip) the rounded map corners at the extremes of the route.
 */
private fun calculateBaseZoom(
    minLat: Double,
    maxLat: Double,
    minLng: Double,
    maxLng: Double,
    viewportW: Float,
    viewportH: Float
): Int {
    // Generous padding so the entire journey, polyline curves, and both pins are 100% visible inside the map frame
    val padX = 72.0
    val padY = 88.0
    val usableW = max(viewportW.toDouble() - padX * 2, 100.0)
    val usableH = max(viewportH.toDouble() - padY * 2, 100.0)

    val deltaLng = (maxLng - minLng).coerceAtLeast(0.005)
    val lngFraction = deltaLng / 360.0

    val sinMinLat = sin(minLat * PI / 180.0).coerceIn(-0.9999, 0.9999)
    val sinMaxLat = sin(maxLat * PI / 180.0).coerceIn(-0.9999, 0.9999)
    val yMin = 0.5 - ln((1.0 + sinMinLat) / (1.0 - sinMinLat)) / (4.0 * PI)
    val yMax = 0.5 - ln((1.0 + sinMaxLat) / (1.0 - sinMaxLat)) / (4.0 * PI)
    val latFraction = kotlin.math.abs(yMax - yMin).coerceAtLeast(0.00005)

    val latZoom = floor(ln(usableH / 256.0 / latFraction) / ln(2.0))
    val lngZoom = floor(ln(usableW / 256.0 / lngFraction) / ln(2.0))

    return min(latZoom, lngZoom).toInt().coerceIn(9, 16)
}

@Composable
private fun MapTileLayerView(
    visibleTiles: List<TileInfo>,
    screenCenterX: Float,
    screenCenterY: Float,
    centerWorldX: Double,
    centerWorldY: Double,
    tileScale: Float,
    tileSize: Float,
    isTrafficEnabled: Boolean,
    density: androidx.compose.ui.unit.Density,
    context: Context
) {
    val requestHeaders = remember {
        NetworkHeaders.Builder()
            .set("User-Agent", "FairFare-Transit/1.0 (Android; support@fairfare.app)")
            .set("Accept", "image/png,image/webp,image/*")
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        visibleTiles.forEach { tile ->
            key(tile.url) {
                MapTileItem(
                    tile = tile,
                    screenCenterX = screenCenterX,
                    screenCenterY = screenCenterY,
                    centerWorldX = centerWorldX,
                    centerWorldY = centerWorldY,
                    tileScale = tileScale,
                    tileSize = tileSize,
                    isTrafficEnabled = isTrafficEnabled,
                    requestHeaders = requestHeaders,
                    density = density,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun MapTileItem(
    tile: TileInfo,
    screenCenterX: Float,
    screenCenterY: Float,
    centerWorldX: Double,
    centerWorldY: Double,
    tileScale: Float,
    tileSize: Float,
    isTrafficEnabled: Boolean,
    requestHeaders: NetworkHeaders,
    density: androidx.compose.ui.unit.Density,
    context: Context
) {
    val tileScreenLeft = screenCenterX + ((tile.x * 256.0) - centerWorldX).toFloat() * tileScale
    val tileScreenTop = screenCenterY + ((tile.y * 256.0) - centerWorldY).toFloat() * tileScale

    val imgRequest = remember(tile.url) {
        ImageRequest.Builder(context)
            .data(tile.url)
            .httpHeaders(requestHeaders)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    AsyncImage(
        model = imgRequest,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset {
                IntOffset(tileScreenLeft.roundToInt(), tileScreenTop.roundToInt())
            }
            .size(
                width = with(density) { (tileSize + 0.5f).toDp() },
                height = with(density) { (tileSize + 0.5f).toDp() }
            )
    )

    if (isTrafficEnabled && tile.trafficUrl.isNotBlank()) {
        val trafficImgRequest = remember(tile.trafficUrl) {
            ImageRequest.Builder(context)
                .data(tile.trafficUrl)
                .httpHeaders(requestHeaders)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            model = trafficImgRequest,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset {
                    IntOffset(tileScreenLeft.roundToInt(), tileScreenTop.roundToInt())
                }
                .size(
                    width = with(density) { (tileSize + 0.5f).toDp() },
                    height = with(density) { (tileSize + 0.5f).toDp() }
                )
        )
    }
}

@Composable
private fun MapOverlayCanvas(
    allRoutes: List<TrafficRouteOption>,
    activeRoute: TrafficRouteOption?,
    zoomStrokeScale: Float,
    isTrafficEnabled: Boolean,
    isDark: Boolean,
    project: (GeoPoint) -> Offset
) {
    val infiniteTransition = rememberInfiniteTransition(label = "map_navigation_anim")
    val vehicleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vehicle_progress"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        fun routeStroke(width: Float) = Stroke(
            width = width * zoomStrokeScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // 1. Alternative (non-selected) corridors
        allRoutes.forEach { route ->
            if (route.id != activeRoute?.id && route.geoPoints.size >= 2) {
                val altPath = route.geoPoints.map { project(it) }.toSmoothPath()

                drawPath(
                    path = altPath,
                    color = if (isDark) AltRouteCasingDark else AltRouteCasingLight,
                    style = routeStroke(ALT_CASING_WIDTH)
                )
                drawPath(
                    path = altPath,
                    color = if (isDark) AltRouteFillDark else AltRouteFillLight,
                    style = routeStroke(ALT_FILL_WIDTH)
                )
            }
        }

        // 2. Draw Selected Primary Route with Traffic Congestion
        if (activeRoute != null && activeRoute.geoPoints.size >= 2) {
            val routeLine = buildList {
                activeRoute.startPoint?.let(::add)
                addAll(activeRoute.geoPoints)
                activeRoute.endPoint?.let(::add)
            }.dedupeConsecutive()

            val screenPts = routeLine.map { project(it) }
            val path = screenPts.toSmoothPath()
            val routeColor = RouteBlue

            drawPath(
                path = path,
                color = if (isDark) RouteCasingDark else RouteCasingLight,
                style = routeStroke(ROUTE_CASING_WIDTH)
            )

            drawPath(
                path = path,
                color = routeColor,
                style = routeStroke(ROUTE_FILL_WIDTH)
            )

            if (isTrafficEnabled) {
                drawTrafficSegments(
                    screenPoints = screenPts,
                    segments = activeRoute.segments,
                    strokeWidth = ROUTE_FILL_WIDTH * zoomStrokeScale
                )
            }

            // 3. Draw Intermediate Waypoints
            activeRoute.waypoints
                .filter { it.distanceRatio > 0.01f && it.distanceRatio < 0.99f }
                .forEach { wp ->
                    val wpPt = wp.geoPoint ?: routeLine.pointAtRatio(wp.distanceRatio)
                    val wpOffset = project(wpPt)

                    drawCircle(
                        color = Color.White,
                        radius = 5.5f * zoomStrokeScale,
                        center = wpOffset
                    )
                    drawCircle(
                        color = if (wp.isIncident) TrafficModerateAmber else WaypointDot,
                        radius = 3.5f * zoomStrokeScale,
                        center = wpOffset
                    )
                }

            // 4. Animated navigation chevron
            if (screenPts.size >= 2) {
                val totalSegments = screenPts.size - 1
                val segmentIndex = (vehicleProgress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
                val segmentT = (vehicleProgress * totalSegments) - segmentIndex
                val p1 = screenPts[segmentIndex]
                val p2 = screenPts[segmentIndex + 1]
                val vehiclePos = Offset(
                    p1.x + (p2.x - p1.x) * segmentT,
                    p1.y + (p2.y - p1.y) * segmentT
                )

                val angleRad = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())

                fun chevron(length: Float, halfWidth: Float): Path {
                    val tipX = vehiclePos.x + cos(angleRad).toFloat() * length
                    val tipY = vehiclePos.y + sin(angleRad).toFloat() * length
                    val leftX = vehiclePos.x + cos(angleRad + PI * 0.72).toFloat() * halfWidth
                    val leftY = vehiclePos.y + sin(angleRad + PI * 0.72).toFloat() * halfWidth
                    val rightX = vehiclePos.x + cos(angleRad - PI * 0.72).toFloat() * halfWidth
                    val rightY = vehiclePos.y + sin(angleRad - PI * 0.72).toFloat() * halfWidth
                    return Path().apply {
                        moveTo(tipX, tipY)
                        lineTo(leftX, leftY)
                        lineTo(vehiclePos.x, vehiclePos.y)
                        lineTo(rightX, rightY)
                        close()
                    }
                }

                drawPath(chevron(15f, 10f), color = Color.White)
                drawPath(chevron(11.5f, 7.5f), color = routeColor)
            }

            // 5. Origin marker
            val startOffset = project(activeRoute.startPoint ?: routeLine.first())
            drawCircle(
                color = routeColor.copy(alpha = pulseAlpha * 0.4f),
                radius = 20f * pulseScale,
                center = startOffset
            )
            drawCircle(color = Color.White, radius = 9f, center = startOffset)
            drawCircle(color = routeColor, radius = 6f, center = startOffset)

            // 6. Destination marker
            val endOffset = project(activeRoute.endPoint ?: routeLine.last())
            drawCircle(color = Color.White, radius = 10f, center = endOffset)
            drawCircle(color = DestinationPin, radius = 7f, center = endOffset)
            drawCircle(color = Color.White, radius = 2.5f, center = endOffset)
        }
    }
}


