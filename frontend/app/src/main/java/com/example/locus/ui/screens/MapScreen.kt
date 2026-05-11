package com.example.locus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.theme.NavyDark
import com.example.locus.ui.theme.White
import com.example.locus.utils.SessionManager
import com.example.locus.viewmodel.RoutePlanningViewModel
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.locationcomponent.location

@Composable
fun MapScreen(
    onNavigate: (NavDestination) -> Unit = {},
    onPostClick: (Int) -> Unit = {},
    planningVm: RoutePlanningViewModel = viewModel()
) {
    val context = LocalContext.current
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

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
        // Inject auth token for itinerary save operations
        planningVm.token = SessionManager(context).token ?: ""
    }

    // Update GPS in VM whenever location permission is granted
    LaunchedEffect(locationGranted) {
        if (locationGranted) {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation
                .addOnSuccessListener { loc ->
                    loc?.let { planningVm.gps = "${it.latitude},${it.longitude}" }
                }
        }
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions(
            cameraOptions {
                center(Point.fromLngLat(2.3522, 48.8566))
                zoom(12.0)
            }
        )
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
            MapEffect(locationGranted) { mapView ->
                    try {
                        mapView.location.updateSettings {
                            enabled = locationGranted
                            pulsingEnabled = locationGranted
                        }
                    } catch (_: Exception) { }
                }
        }

        // Search bar overlay — top of map
        MapSearchOverlay(modifier = Modifier.align(Alignment.TopCenter))

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

        // Route planning overlay (sheets + detail screen)
        RoutePlanningFlow(viewModel = planningVm, onPostClick = onPostClick)
    }
}
