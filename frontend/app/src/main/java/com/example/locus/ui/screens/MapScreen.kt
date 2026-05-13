package com.example.locus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.data.remote.SearchPostResult
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.theme.GoldPrimary
import com.example.locus.ui.theme.MediumGray
import com.example.locus.ui.theme.OffWhite
import com.example.locus.utils.formatTimeAgo
import com.example.locus.utils.parseLatLon
import com.example.locus.ui.theme.NavyDark
import com.example.locus.ui.theme.White
import com.example.locus.utils.SessionManager
import com.example.locus.viewmodel.RoutePlanningViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.locationcomponent.location

private fun weatherEmoji(iconCode: String, isRain: Boolean, isSnow: Boolean): String = when {
    isSnow -> "❄️"
    isRain -> "🌧️"
    iconCode.startsWith("01") -> "☀️"
    iconCode.startsWith("02") -> "🌤️"
    iconCode.startsWith("03") -> "⛅"
    iconCode.startsWith("04") -> "☁️"
    iconCode.startsWith("09") -> "🌧️"
    iconCode.startsWith("10") -> "🌦️"
    iconCode.startsWith("11") -> "⛈️"
    iconCode.startsWith("13") -> "❄️"
    iconCode.startsWith("50") -> "🌫️"
    else -> "🌡️"
}

private fun makePlaceBitmap(letter: String, bgColor: Int): Bitmap {
    val size = 80
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = bgColor
    cv.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 5f
    cv.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)
    paint.style = Paint.Style.FILL
    paint.textSize = size * 0.40f
    paint.textAlign = Paint.Align.CENTER
    cv.drawText(letter, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2, paint)
    return bmp
}

private fun makeStepBitmap(number: Int): Bitmap {
    val size = 72
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    // white border
    p.color = android.graphics.Color.WHITE
    cv.drawCircle(size / 2f, size / 2f, size / 2f, p)
    // blue fill
    p.color = android.graphics.Color.parseColor("#1565C0")
    cv.drawCircle(size / 2f, size / 2f, size / 2f - 5, p)
    // number text
    p.color = android.graphics.Color.WHITE
    p.textSize = size * 0.40f
    p.textAlign = Paint.Align.CENTER
    cv.drawText("$number", size / 2f, size / 2f - (p.descent() + p.ascent()) / 2, p)
    return bmp
}

// Montpellier bounding box
private val MONTPELLIER_SW = Point.fromLngLat(3.77, 43.54)
private val MONTPELLIER_NE = Point.fromLngLat(4.00, 43.69)
private val MONTPELLIER_CENTER = Point.fromLngLat(3.8767, 43.6108)

@Composable
fun MapScreen(
    onNavigate: (NavDestination) -> Unit = {},
    onPostClick: (Int) -> Unit = {},
    focusGps: String? = null,
    planningVm: RoutePlanningViewModel = viewModel()
) {
    val context = LocalContext.current
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var myGps by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!locationGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        planningVm.token = SessionManager(context).token ?: ""
        planningVm.mapboxToken = context.getString(com.example.locus.R.string.mapbox_access_token)
    }

    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val gpsStr = "${loc.latitude},${loc.longitude}"
                    myGps = gpsStr
                    planningVm.gps = gpsStr
                    planningVm.loadWeather(loc.latitude, loc.longitude)
                    planningVm.loadNearbyPostMarkers()
                    planningVm.loadNearbyPlaces()
                } else {
                    // lastLocation is null (device hasn't cached a fix yet) — request a fresh one
                    fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { currentLoc ->
                            currentLoc?.let {
                                val gpsStr = "${it.latitude},${it.longitude}"
                                myGps = gpsStr
                                planningVm.gps = gpsStr
                                planningVm.loadWeather(it.latitude, it.longitude)
                                planningVm.loadNearbyPostMarkers()
                                planningVm.loadNearbyPlaces()
                            }
                        }
                }
            }
        }
    }

    // Pre-build category bitmaps once — passed directly to IconImage so Mapbox
    // Compose handles registration internally, no style-load race condition.
    val categoryBitmaps = remember {
        mapOf(
            "restaurant" to makePlaceBitmap("R", android.graphics.Color.parseColor("#E64A19")),
            "bar"        to makePlaceBitmap("B", android.graphics.Color.parseColor("#FF8F00")),
            "cafe"       to makePlaceBitmap("C", android.graphics.Color.parseColor("#795548")),
            "musee"      to makePlaceBitmap("M", android.graphics.Color.parseColor("#7B1FA2")),
            "monument"   to makePlaceBitmap("T", android.graphics.Color.parseColor("#37474F")),
            "parc"       to makePlaceBitmap("V", android.graphics.Color.parseColor("#388E3C")),
            "hotel"      to makePlaceBitmap("H", android.graphics.Color.parseColor("#1565C0")),
            "sport"      to makePlaceBitmap("S", android.graphics.Color.parseColor("#00838F")),
            "shopping"   to makePlaceBitmap("G", android.graphics.Color.parseColor("#AD1457")),
            "plage"      to makePlaceBitmap("P", android.graphics.Color.parseColor("#0288D1")),
            "autre"      to makePlaceBitmap("+", android.graphics.Color.parseColor("#455A64")),
        )
    }

    val stepBitmaps = remember { (1..20).associateWith { makeStepBitmap(it) } }

    val mapViewportState = rememberMapViewportState {
        val initial = focusGps.parseLatLon()
        setCameraOptions(
            cameraOptions {
                if (initial != null) {
                    center(Point.fromLngLat(initial.second, initial.first))
                    zoom(16.0)
                } else {
                    center(MONTPELLIER_CENTER)
                    zoom(13.0)
                }
            }
        )
    }

    val nearbyPosts      = planningVm.nearbyPosts
    val nearbyPlaces     = planningVm.places
    val focusedPlaceGps  = planningVm.focusedPlaceGps
    val selectedRoute    = planningVm.selectedRoute
    val stepGpsMap       = planningVm.stepGpsMap
    val navigationPoints = planningVm.navigationPoints

    // Fly to destination when user taps a route step detail
    LaunchedEffect(focusedPlaceGps) {
        val (lat, lon) = focusedPlaceGps.parseLatLon() ?: return@LaunchedEffect
        mapViewportState.flyTo(
            CameraOptions.Builder().center(Point.fromLngLat(lon, lat)).zoom(16.0).build(),
            MapAnimationOptions.mapAnimationOptions { duration(900) }
        )
        planningVm.clearFocusedPlaceGps()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.MAPBOX_STREETS) },
            scaleBar = {},
            logo = {},
            attribution = {},
            compass = {}
        ) {
            // Restrict camera to Montpellier area
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.setBounds(
                    com.mapbox.maps.CameraBoundsOptions.Builder()
                        .bounds(CoordinateBounds(MONTPELLIER_SW, MONTPELLIER_NE, false))
                        .minZoom(11.0)
                        .build()
                )
            }

            MapEffect(locationGranted) { mapView ->
                try {
                    mapView.location.updateSettings {
                        enabled = locationGranted
                        pulsingEnabled = locationGranted
                    }
                } catch (_: Exception) { }
            }

            // Navigation route — real road geometry from Directions API (Google Maps style)
            if (navigationPoints.size >= 2) {
                PolylineAnnotation(points = navigationPoints) {
                    lineColor = Color.White
                    lineWidth = 10.0
                    lineOpacity = 1.0
                }
                PolylineAnnotation(points = navigationPoints) {
                    lineColor = Color(0xFF1976D2)
                    lineWidth = 6.0
                    lineOpacity = 1.0
                }
            }

            // Straight-line step connectors shown while no nav route is loaded
            selectedRoute?.let { route ->
                if (navigationPoints.isEmpty()) {
                    val routePoints = route.steps.mapNotNull { step ->
                        val gps = stepGpsMap[step.lieuId] ?: step.gps.ifBlank { null } ?: return@mapNotNull null
                        val (lat, lon) = gps.parseLatLon() ?: return@mapNotNull null
                        Point.fromLngLat(lon, lat)
                    }
                    if (routePoints.size >= 2) {
                        PolylineAnnotation(points = routePoints) {
                            lineColor = Color.White
                            lineWidth = 9.0
                            lineOpacity = 1.0
                        }
                        PolylineAnnotation(points = routePoints) {
                            lineColor = Color(0xFF4285F4)
                            lineWidth = 5.5
                            lineOpacity = 0.75
                        }
                    }
                }

                // Numbered step markers
                route.steps.forEachIndexed { index, step ->
                    val gps = stepGpsMap[step.lieuId] ?: step.gps.ifBlank { null } ?: return@forEachIndexed
                    val (lat, lon) = gps.parseLatLon() ?: return@forEachIndexed
                    val bmp = stepBitmaps[index + 1] ?: stepBitmaps[1]!!
                    key("route_step_$index") {
                        PointAnnotation(point = Point.fromLngLat(lon, lat)) {
                            iconImage = IconImage(bmp)
                            iconSize = 0.55
                        }
                    }
                }
            }

            // Nearby place markers — IconImage(bitmap) avoids style-load race condition
            nearbyPlaces.forEach { lieu ->
                val (lat, lon) = lieu.gps.parseLatLon() ?: return@forEach
                val bmp = categoryBitmaps[lieu.categorie] ?: categoryBitmaps["autre"]!!
                key("place_${lieu.id}") {
                    PointAnnotation(point = Point.fromLngLat(lon, lat)) {
                        iconImage = IconImage(bmp)
                        iconSize = 0.55
                        interactionsState.onClicked { planningVm.selectPlaceFromList(lieu.id); true }
                    }
                }
            }

            // Highlight the focused destination from route step detail
            focusedPlaceGps.parseLatLon()?.let { (lat, lon) ->
                key("focused_place") {
                    CircleAnnotation(point = Point.fromLngLat(lon, lat)) {
                        circleRadius = 20.0
                        circleColor = Color(0xFFC8A032)
                        circleOpacity = 0.30
                        circleStrokeWidth = 3.0
                        circleStrokeColor = Color(0xFFC8A032)
                    }
                }
            }

            // Nearby post markers (gold dots)
            nearbyPosts.forEach { post ->
                val (lat, lon) = post.locGps.parseLatLon() ?: return@forEach
                val isSelected = planningVm.selectedNearbyPost?.id == post.id
                key("post_${post.id}") {
                    CircleAnnotation(point = Point.fromLngLat(lon, lat)) {
                        circleRadius = if (isSelected) 14.0 else 10.0
                        circleColor = Color(0xFFC8A032)
                        circleStrokeWidth = if (isSelected) 3.5 else 2.5
                        circleStrokeColor = Color.White
                        interactionsState.onClicked {
                            planningVm.selectNearbyPost(post)
                            mapViewportState.flyTo(
                                CameraOptions.Builder().center(Point.fromLngLat(lon, lat)).zoom(15.0).build(),
                                MapAnimationOptions.mapAnimationOptions { duration(600) }
                            )
                            true
                        }
                    }
                }
            }
        }

        // Search bar overlay — top of map
        MapSearchOverlay(
            modifier = Modifier.align(Alignment.TopCenter),
            viewModel = planningVm,
            currentGps = myGps,
            onFlyTo = { lat, lon ->
                mapViewportState.flyTo(
                    CameraOptions.Builder().center(Point.fromLngLat(lon, lat)).zoom(16.0).build(),
                    MapAnimationOptions.mapAnimationOptions { duration(800) }
                )
            }
        )

        // Weather pill — bottom-start, above Plan route FAB
        planningVm.weather?.let { w ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 175.dp),
                shape = RoundedCornerShape(50.dp),
                color = White,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(weatherEmoji(w.iconCode, w.isRain, w.isSnow), fontSize = 15.sp)
                    Text(
                        "${w.tempC.toInt()}°C",
                        color = NavyDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }


        // Plan route FAB — bottom-start, above navbar
        ExtendedFloatingActionButton(
            onClick = { planningVm.open() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 110.dp),
            containerColor = NavyDark,
            contentColor = White,
            icon = { Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null) },
            text = { Text("Plan route", fontWeight = FontWeight.SemiBold) }
        )

        // My Routes button — bottom-end, above navbar
        Surface(
            onClick = { planningVm.openSavedRoutes() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 222.dp)
                .size(48.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            color = White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "My Routes",
                    tint = NavyDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Places button — bottom-end, above navbar
        Surface(
            onClick = { planningVm.openPlaces() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 166.dp)
                .size(48.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            color = White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Nearby Places",
                    tint = NavyDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Re-center button — bottom-end, above navbar
        Surface(
            onClick = { mapViewportState.transitionToFollowPuckState() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 110.dp)
                .size(48.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            color = White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Ma position",
                    tint = NavyDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        BottomNav(
            selected = NavDestination.COMPASS,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Post popup — shown when a gold dot marker is tapped
        planningVm.selectedNearbyPost?.let { post ->
            PostMapPopup(
                post = post,
                authorName = planningVm.postAuthorNames[post.user_id],
                onDismiss = { planningVm.selectNearbyPost(null) },
                onDetails = {
                    planningVm.selectNearbyPost(null)
                    onPostClick(post.id)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 82.dp)
            )
        }

        // Route planning overlay (sheets + detail screen)
        RoutePlanningFlow(viewModel = planningVm, onPostClick = onPostClick)
    }
}

@Composable
private fun PostMapPopup(
    post: SearchPostResult,
    authorName: String?,
    onDismiss: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = White,
        shadowElevation = 12.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.20f))
                )
                val locationLabel = post.description
                    .substringAfter("\n---loc:", "")
                    .substringBefore("\n---tags:")
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: post.locNom?.takeIf { it.isNotBlank() }
                if (locationLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(locationLabel, color = White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = White, modifier = Modifier.size(9.dp))
                }
            }

            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = authorName ?: "User ${post.user_id}",
                        color = NavyDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatTimeAgo(post.date),
                        color = MediumGray,
                        fontSize = 11.sp
                    )
                }

                val caption = post.description
                    .substringBefore("\n---loc:")
                    .substringBefore("\n---tags:")
                    .trim()
                if (caption.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = caption,
                        color = NavyDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onDetails,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("View Details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
