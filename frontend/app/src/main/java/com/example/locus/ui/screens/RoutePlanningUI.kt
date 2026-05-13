package com.example.locus.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locus.BuildConfig
import com.example.locus.data.model.*
import com.example.locus.data.remote.CreateLieuRequest
import com.example.locus.data.remote.LieuResponse
import com.example.locus.data.remote.SavedItinResponse
import com.example.locus.data.remote.WeatherResponse
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.PlanningState
import com.example.locus.viewmodel.RoutePlanningViewModel

// --- Color / icon helpers ----------------------------------------------------

private fun ActivityCategory.color() = when (this) {
    ActivityCategory.DINING -> Color(0xFFFF7043)
    ActivityCategory.LEISURE -> Color(0xFF26A69A)
    ActivityCategory.DISCOVERY -> Color(0xFF42A5F5)
    ActivityCategory.CULTURE -> Color(0xFFAB47BC)
}

private fun ActivityCategory.icon(): ImageVector = when (this) {
    ActivityCategory.DINING -> Icons.Filled.Restaurant
    ActivityCategory.LEISURE -> Icons.Filled.Spa
    ActivityCategory.DISCOVERY -> Icons.Filled.Explore
    ActivityCategory.CULTURE -> Icons.Filled.Museum
}

private fun RouteType.color() = when (this) {
    RouteType.ECONOMIC -> Color(0xFF66BB6A)
    RouteType.BALANCED -> GoldPrimary
    RouteType.COMFORT -> Color(0xFF7E57C2)
}

private fun TimeSlot.color() = when (this) {
    TimeSlot.MORNING -> Color(0xFFFFA726)
    TimeSlot.AFTERNOON -> Color(0xFF42A5F5)
    TimeSlot.EVENING -> Color(0xFF7E57C2)
}

private fun Int.toHourMin(): String {
    val h = this / 60
    val m = this % 60
    return if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
}

// --- Entry point -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningFlow(
    viewModel: RoutePlanningViewModel,
    onPostClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current

    when (viewModel.state) {
        PlanningState.IDLE -> Unit
        PlanningState.PLANNING -> PreferencesSheet(
            prefs = viewModel.preferences,
            weather = viewModel.weather,
            onGenerate = { viewModel.generate(it) },
            onDismiss = { viewModel.close() }
        )
        PlanningState.GENERATING -> GeneratingOverlay()
        PlanningState.OPTIONS -> RouteOptionsSheet(
            routes = viewModel.routes,
            error = viewModel.error,
            likedIds = viewModel.likedIds,
            dislikedIds = viewModel.dislikedIds,
            onLike = { viewModel.like(it) },
            onDislike = { viewModel.dislike(it) },
            onSelect = { viewModel.select(it) },
            onRegenerate = { viewModel.regenerate() },
            onDismiss = { viewModel.close() }
        )
        PlanningState.DETAIL -> viewModel.selectedRoute?.let { route ->
            RouteDetailScreen(
                route = route,
                isSaved = viewModel.isSaved,
                savedItineraireId = viewModel.savedItineraireId,
                isItinLiked = viewModel.isCurrentItinLiked,
                onSave = { viewModel.toggleSave() },
                onRegenerate = if (viewModel.canRegenerate) { { viewModel.regenerate() } } else null,
                onLikeItineraire = { viewModel.toggleItineraireLike() },
                onShare = {
                    val id = viewModel.savedItineraireId
                    if (id > 0) {
                        viewModel.shareItineraire { url ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, url)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    } else {
                        val text = "Check out my ${route.type.label.lowercase()} route: " +
                            route.steps.joinToString(" → ") { it.name }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                onExportPdf = {
                    val id = viewModel.savedItineraireId
                    if (id > 0) {
                        val uri = Uri.parse("${BuildConfig.API_BASE_URL}travelPath/itineraires/pdf?id=$id")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } else {
                        Toast.makeText(context, "Save the route first to export PDF", Toast.LENGTH_SHORT).show()
                    }
                },
                navError = viewModel.navRouteError,
                onBack = { viewModel.backToOptions() },
                onClose = { viewModel.close() },
                onStart = { viewModel.startNavigation() },
                onStepClick = { lieuId, gps -> viewModel.selectPlace(lieuId, gps) }
            )
        }
        PlanningState.PLACE_DETAIL -> PlaceDetailScreen(
            lieuId = viewModel.selectedLieuId,
            token = viewModel.token,
            onBack = { viewModel.backFromPlace() },
            onPostClick = onPostClick,
            onAddToFavorites = { name -> viewModel.addToFavorites(name) }
        )
        PlanningState.SAVED_ROUTES -> SavedRoutesSheet(
            savedItineraries = viewModel.savedItineraries,
            onSelectRoute = { viewModel.selectSavedRoute(it) },
            onExplore = { viewModel.openExploreItins() },
            onDismiss = { viewModel.close() }
        )
        PlanningState.EXPLORE_ITINS -> ExploreItinsSheet(
            results = viewModel.itinSearchResults,
            isSearching = viewModel.isSearchingItins,
            hasToken = viewModel.token.isNotBlank(),
            onSearch = { q -> viewModel.searchItinsByQuery(q) },
            onSelectRoute = { viewModel.selectSavedRoute(it) },
            onBack = { viewModel.openSavedRoutes() },
            onDismiss = { viewModel.close() }
        )
        PlanningState.PLACES -> PlacesSheet(
            places = viewModel.places,
            isLoading = viewModel.isLoadingPlaces,
            hasToken = viewModel.token.isNotBlank(),
            defaultLat = viewModel.currentLat,
            defaultLon = viewModel.currentLon,
            onSelectPlace = { viewModel.selectPlaceFromList(it) },
            onCreatePlace = { viewModel.createPlace(it) },
            onDismiss = { viewModel.close() }
        )
    }
}

// --- Preferences sheet -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesSheet(
    prefs: RoutePlanPreferences,
    weather: WeatherResponse? = null,
    onGenerate: (RoutePlanPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    var activities by remember { mutableStateOf(prefs.activities) }
    var places by remember { mutableStateOf(prefs.favoritePlaces) }
    var newPlace by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf(prefs.maxBudget.toFloat()) }
    var durationHours by remember { mutableStateOf(prefs.durationHours) }
    var effort by remember { mutableStateOf(prefs.effort) }
    var anyWeather by remember { mutableStateOf(prefs.anyWeather) }

    val durations = listOf(2 to "2h", 4 to "4h", 6 to "6h", 8 to "Half day", 12 to "Full day")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))

            Text("Plan my route", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)

            // Activities
            SectionLabel("ACTIVITIES")
            val cats = ActivityCategory.entries
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.take(2).forEach { cat ->
                        ActivityCategoryChip(cat, cat in activities, Modifier.weight(1f)) {
                            activities = if (cat in activities) activities - cat else activities + cat
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.drop(2).forEach { cat ->
                        ActivityCategoryChip(cat, cat in activities, Modifier.weight(1f)) {
                            activities = if (cat in activities) activities - cat else activities + cat
                        }
                    }
                }
            }

            // Favorite places
            SectionLabel("FAVORITE PLACES")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPlace,
                    onValueChange = { newPlace = it },
                    placeholder = { Text("Add a place or address", color = InputHint, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = White, focusedContainerColor = White,
                        unfocusedBorderColor = InputBorder, focusedBorderColor = NavyDark,
                        unfocusedTextColor = NavyDark, focusedTextColor = NavyDark
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (newPlace.isNotBlank()) { places = places + newPlace.trim(); newPlace = "" } },
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NavyDark)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add", tint = White)
                }
            }
            if (places.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    places.forEach { place ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(OffWhite).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(place, color = NavyDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { places = places - place }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = InputHint, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Budget
            SectionLabel("MAX BUDGET")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = budget,
                        onValueChange = { budget = it },
                        valueRange = 0f..500f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = NavyDark, activeTrackColor = NavyDark, inactiveTrackColor = InputBorder)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("${budget.toInt()}€", fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp, modifier = Modifier.width(48.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("€0", color = InputHint, fontSize = 11.sp)
                    Text("€500", color = InputHint, fontSize = 11.sp)
                }
            }

            // Duration
            SectionLabel("DURATION")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { (h, label) ->
                    SelectChip(label, durationHours == h) { durationHours = h }
                }
            }

            // Effort
            SectionLabel("EFFORT")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EffortLevel.entries.forEach { level ->
                    SelectChip(level.label, effort == level, Modifier.weight(1f)) { effort = level }
                }
            }

            // Weather
            SectionLabel("WEATHER")
            weather?.let { WeatherWidget(it) }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(White).border(1.dp, InputBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Any weather", fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 14.sp)
                    Text("Include all activities regardless of weather", color = InputHint, fontSize = 11.sp)
                }
                Switch(
                    checked = anyWeather,
                    onCheckedChange = { anyWeather = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = GoldPrimary, uncheckedThumbColor = InputHint, uncheckedTrackColor = Color.LightGray)
                )
            }

            // CTA
            Button(
                onClick = { onGenerate(RoutePlanPreferences(activities, places, budget.toInt(), durationHours, effort, anyWeather)) },
                enabled = activities.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White, disabledContainerColor = NavyDark.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate my routes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

// --- Generating overlay ------------------------------------------------------

@Composable
private fun GeneratingOverlay() {
    Box(Modifier.fillMaxSize().background(White.copy(alpha = 0.97f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = NavyDark, modifier = Modifier.size(52.dp), strokeWidth = 3.dp)
            Text("Calculating your routes...", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Weather · Opening hours · Budget", color = InputHint, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// --- Route options sheet -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteOptionsSheet(
    routes: List<RouteOption>,
    error: String?,
    likedIds: Set<Int>,
    dislikedIds: Set<Int>,
    onLike: (Int) -> Unit,
    onDislike: (Int) -> Unit,
    onSelect: (RouteOption) -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))
            Text(
                if (routes.isEmpty()) "No routes found" else "${routes.size} routes for you",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark
            )

            if (routes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = InputHint, modifier = Modifier.size(48.dp))
                        Text(
                            error ?: "Not enough places near you.\nTry adjusting your preferences.",
                            color = InputHint, fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                routes.forEach { route ->
                    RouteOptionCard(
                        route = route,
                        isLiked = route.id in likedIds,
                        isDisliked = route.id in dislikedIds,
                        onLike = { onLike(route.id) },
                        onDislike = { onDislike(route.id) },
                        onSelect = { onSelect(route) }
                    )
                }
            }

            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.5.dp, NavyDark)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = NavyDark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Regenerate routes", color = NavyDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RouteOptionCard(
    route: RouteOption,
    isLiked: Boolean,
    isDisliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onSelect: () -> Unit
) {
    val typeColor = route.type.color()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50.dp)).background(typeColor.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(route.type.label, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (route.weatherSensitive) Icons.Filled.WbSunny else Icons.Filled.Cloud,
                    contentDescription = null,
                    tint = if (route.weatherSensitive) Color(0xFFFFA726) else InputHint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (route.weatherSensitive) "Good weather only" else "Any weather",
                    color = if (route.weatherSensitive) Color(0xFFFFA726) else InputHint,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPill("${route.totalBudget}€", Icons.Filled.AttachMoney)
                SmallPill(route.totalDurationMinutes.toHourMin(), Icons.Filled.Schedule)
                SmallPill("Effort ${route.effortScore}/5", Icons.Filled.FitnessCenter)
            }

            HorizontalDivider(color = InputBorder, thickness = 1.dp)

            route.steps.take(3).forEach { step ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(step.category.color()))
                    Text(step.name, fontSize = 13.sp, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(step.timeSlot.label, fontSize = 10.sp, color = step.timeSlot.color())
                }
            }
            if (route.steps.size > 3) {
                Text("+ ${route.steps.size - 3} more step(s)", color = InputHint, fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFE53935) else InputHint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDislike, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) NavyDark else InputHint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("View details", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// --- Route detail screen -----------------------------------------------------

@Composable
fun RouteDetailScreen(
    route: RouteOption,
    isSaved: Boolean,
    savedItineraireId: Int = -1,
    isItinLiked: Boolean = false,
    onSave: () -> Unit,
    onLikeItineraire: () -> Unit = {},
    onShare: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onRegenerate: (() -> Unit)? = null,
    navError: String? = null,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onStart: () -> Unit = {},
    onStepClick: (Int, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val typeColor = route.type.color()

    Box(modifier = Modifier.fillMaxSize().background(OffWhite).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 8.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                }
                Box(
                    modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(50.dp)).background(typeColor.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(route.type.label, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = NavyDark)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    route.nom.ifEmpty { "Your ${route.type.label.lowercase()} route" },
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark
                )
                if (route.resume.isNotEmpty()) {
                    Text(route.resume, color = InputHint, fontSize = 13.sp, lineHeight = 18.sp)
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricChip("${route.totalBudget}€", Icons.Filled.AttachMoney, Color(0xFF66BB6A))
                    MetricChip(route.totalDurationMinutes.toHourMin(), Icons.Filled.Schedule, Color(0xFF42A5F5))
                    MetricChip("Effort ${route.effortScore}/5", Icons.Filled.FitnessCenter, Color(0xFF7E57C2))
                    MetricChip(
                        if (route.weatherSensitive) "Good weather" else "Any weather",
                        if (route.weatherSensitive) Icons.Filled.WbSunny else Icons.Filled.Cloud,
                        if (route.weatherSensitive) Color(0xFFFFA726) else InputHint
                    )
                }

                // Offline toggle
                var offlineEnabled by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(White).border(1.dp, InputBorder, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Offline mode", fontWeight = FontWeight.SemiBold, color = NavyDark, fontSize = 14.sp)
                        Text("Save route for offline access", color = InputHint, fontSize = 11.sp)
                    }
                    Switch(
                        checked = offlineEnabled,
                        onCheckedChange = { offlineEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = GoldPrimary, uncheckedThumbColor = InputHint, uncheckedTrackColor = Color.LightGray)
                    )
                }

                Text("Route steps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)

                route.steps.forEachIndexed { index, step ->
                    StepCard(step = step, number = index + 1, onStepClick = { onStepClick(step.lieuId, step.gps) })
                }

                Spacer(Modifier.height(80.dp))
            }
        }

        // Bottom action bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 14.dp)) {
                // Start navigation button
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                if (navError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(navError, color = Color(0xFFE53935), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                ActionIconBtn(
                    icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    label = if (isSaved) "Saved" else "Save",
                    tint = if (isSaved) GoldPrimary else NavyDark,
                    onClick = onSave
                )
                ActionIconBtn(
                    icon = if (isItinLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                    label = if (isItinLiked) "Liked" else "Like",
                    tint = when {
                        isItinLiked -> Color(0xFFE53935)
                        savedItineraireId > 0 -> NavyDark
                        else -> InputHint
                    },
                    onClick = { if (savedItineraireId > 0) onLikeItineraire() }
                )
                ActionIconBtn(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    tint = NavyDark,
                    onClick = onShare
                )
                ActionIconBtn(
                    icon = Icons.Filled.PictureAsPdf,
                    label = "PDF",
                    tint = if (savedItineraireId > 0) NavyDark else InputHint,
                    onClick = onExportPdf
                )
                if (onRegenerate != null) {
                    ActionIconBtn(
                        icon = Icons.Filled.Refresh,
                        label = "Regenerate",
                        tint = NavyDark,
                        onClick = onRegenerate
                    )
                }
                }  // Row (icon buttons)
            }  // Column (start + icons)
        }
    }
}

// --- Navigation start sheet ---------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationStartSheet(
    currentGps: String,
    suggestions: List<com.example.locus.data.remote.LieuResponse>,
    isLoading: Boolean,
    error: String?,
    onUseMyPosition: () -> Unit,
    onSearchQuery: (String) -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(InputBorder).align(Alignment.CenterHorizontally))

            Text("Where do you start?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyDark)

            // Use current position — triggers immediately
            Button(
                onClick = { if (!isLoading) onUseMyPosition() },
                enabled = currentGps.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading route…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use my current position", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = InputBorder)
                Text("or start from", color = InputHint, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = InputBorder)
            }

            // Search field — tapping a result triggers immediately
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearchQuery(it) },
                    placeholder = { Text("Search an address or place…", color = InputHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = InputHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = InputBorder,
                        focusedBorderColor = Color(0xFF1565C0)
                    )
                )
                if (suggestions.isNotEmpty()) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .border(1.dp, InputBorder, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(White)
                    ) {
                        suggestions.forEach { lieu ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) {
                                        val g = lieu.gps ?: return@clickable
                                        query = lieu.nom
                                        onSelectSuggestion(g)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Place, contentDescription = null,
                                    tint = InputHint, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(lieu.nom, fontSize = 14.sp, color = NavyDark, fontWeight = FontWeight.Medium)
                                    Text(lieu.adresse, fontSize = 12.sp, color = InputHint, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            HorizontalDivider(color = InputBorder.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            if (error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE53935).copy(alpha = 0.09f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null,
                        tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                    Text(error, color = Color(0xFFE53935), fontSize = 13.sp)
                }
            }
        }
    }
}

// --- Saved routes sheet -------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedRoutesSheet(
    savedItineraries: List<SavedItinResponse>,
    onSelectRoute: (SavedItinResponse) -> Unit,
    onExplore: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My Routes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))
                TextButton(onClick = onExplore) {
                    Text("Explore others", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                }
            }

            if (savedItineraries.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = InputHint, modifier = Modifier.size(48.dp))
                        Text("No saved routes yet", color = NavyDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("Plan a route and save it to see it here.", color = InputHint, fontSize = 13.sp)
                    }
                }
            } else {
                savedItineraries.forEach { saved ->
                    SavedItinCard(saved = saved, onClick = { onSelectRoute(saved) })
                }
            }
        }
    }
}

@Composable
private fun SavedItinCard(saved: SavedItinResponse, onClick: () -> Unit) {
    val typeColor = saved.type.toRouteType().color()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50.dp)).background(typeColor.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(saved.type.toRouteType().label, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(saved.createdAt.take(10), color = InputHint, fontSize = 11.sp)
            }
            Text(
                saved.nom.ifEmpty { "Route #${saved.id}" },
                fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallPill("${saved.budget}€", Icons.Filled.AttachMoney)
                SmallPill(saved.dureeMinutes.toHourMin(), Icons.Filled.Schedule)
                SmallPill("Effort ${saved.effortScore}/5", Icons.Filled.FitnessCenter)
            }
        }
    }
}

private fun String.toRouteType(): RouteType = when (this) {
    "economique" -> RouteType.ECONOMIC
    "equilibre"  -> RouteType.BALANCED
    else         -> RouteType.COMFORT
}

// --- Explore itineraries sheet ------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreItinsSheet(
    results: List<SavedItinResponse>,
    isSearching: Boolean,
    hasToken: Boolean,
    onSearch: (String) -> Unit,
    onSelectRoute: (SavedItinResponse) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Explore Routes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)
            }

            if (!hasToken) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = InputHint, modifier = Modifier.size(40.dp))
                        Text("Log in to explore saved routes", color = InputHint, fontSize = 13.sp)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search routes...", color = InputHint, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = White, focusedContainerColor = White,
                            unfocusedBorderColor = InputBorder, focusedBorderColor = NavyDark,
                            unfocusedTextColor = NavyDark, focusedTextColor = NavyDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onSearch(query) },
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NavyDark)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = White)
                    }
                }

                if (isSearching) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NavyDark, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                    }
                } else if (results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Search for routes by name or type.", color = InputHint, fontSize = 13.sp)
                    }
                } else {
                    results.forEach { saved ->
                        SavedItinCard(saved = saved, onClick = { onSelectRoute(saved) })
                    }
                }
            }
        }
    }
}

// --- Places sheet -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlacesSheet(
    places: List<LieuResponse>,
    isLoading: Boolean,
    hasToken: Boolean,
    defaultLat: Double,
    defaultLon: Double,
    onSelectPlace: (Int) -> Unit,
    onCreatePlace: (CreateLieuRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateForm by remember { mutableStateOf(false) }
    val categories = listOf("restaurant", "bar", "cafe", "musee", "monument", "parc", "shopping", "sport", "hotel", "plage", "autre")
    var filterCat by remember { mutableStateOf<String?>(null) }

    val filtered = if (filterCat == null) places else places.filter { it.categorie == filterCat }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        if (showCreateForm) {
            CreatePlaceForm(
                defaultLat = defaultLat,
                defaultLon = defaultLon,
                categories = categories,
                onSubmit = { req -> onCreatePlace(req); showCreateForm = false },
                onCancel = { showCreateForm = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nearby Places", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark, modifier = Modifier.weight(1f))
                        if (hasToken) {
                            IconButton(
                                onClick = { showCreateForm = true },
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(NavyDark)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add place", tint = White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaceFilterChip("All", filterCat == null) { filterCat = null }
                    categories.forEach { cat ->
                        PlaceFilterChip(cat.replaceFirstChar { it.uppercase() }, filterCat == cat) { filterCat = cat }
                    }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NavyDark, modifier = Modifier.size(36.dp), strokeWidth = 2.5.dp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("No places found nearby.", color = InputHint, fontSize = 13.sp)
                            }
                        } else {
                            filtered.forEach { lieu ->
                                PlaceListCard(lieu = lieu, onClick = { onSelectPlace(lieu.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceListCard(lieu: LieuResponse, onClick: () -> Unit) {
    val catColor = when (lieu.categorie) {
        "restaurant", "bar", "cafe" -> Color(0xFFFF7043)
        "musee"                     -> Color(0xFFAB47BC)
        "monument", "parc", "autre" -> Color(0xFF42A5F5)
        else                        -> Color(0xFF26A69A)
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth().clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (!lieu.urlImage.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(lieu.urlImage).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = catColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(lieu.nom, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(50.dp)).background(catColor.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                        Text(lieu.categorie.replaceFirstChar { it.uppercase() }, color = catColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (lieu.note > 0f) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(11.dp))
                        Text("%.1f".format(lieu.note), color = NavyDark, fontSize = 11.sp)
                    }
                    if (lieu.distanceKm > 0.0) {
                        Text("·", color = InputHint, fontSize = 11.sp)
                        Text("%.1f km".format(lieu.distanceKm), color = InputHint, fontSize = 11.sp)
                    }
                }
                if (lieu.adresse.isNotBlank()) {
                    Text(lieu.adresse, color = InputHint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = InputHint, modifier = Modifier.size(16.dp))
        }
    }
}

// --- Create place form --------------------------------------------------------

@Composable
private fun CreatePlaceForm(
    defaultLat: Double,
    defaultLon: Double,
    categories: List<String>,
    onSubmit: (CreateLieuRequest) -> Unit,
    onCancel: () -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf("") }
    var horaires by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("0") }
    var siteWeb by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(InputBorder).align(Alignment.CenterHorizontally))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Add a Place", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        }

        FormField("Name *", nom, "e.g. Le Café du Port") { nom = it }
        FormField("Address", adresse, "Street, city") { adresse = it }
        FormField("Description", description, "A short description", minLines = 3) { description = it }

        SectionLabel("CATEGORY *")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { cat ->
                        PlaceFilterChip(
                            label = cat.replaceFirstChar { it.uppercase() },
                            selected = categorie == cat,
                            modifier = Modifier.weight(1f),
                            onClick = { categorie = cat }
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { FormField("Opening hours", horaires, "e.g. 9h–18h") { horaires = it } }
            Column(Modifier.weight(1f)) { FormField("Avg price (€)", prix, "0") { prix = it } }
        }
        FormField("Website", siteWeb, "https://...") { siteWeb = it }
        FormField("Phone", telephone, "+33...") { telephone = it }

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(OffWhite).padding(12.dp)) {
            Text(
                "Location: %.4f, %.4f (your current position)".format(defaultLat, defaultLon),
                color = InputHint, fontSize = 12.sp
            )
        }

        Button(
            onClick = {
                if (nom.isNotBlank() && categorie.isNotBlank()) {
                    onSubmit(
                        CreateLieuRequest(
                            nom = nom.trim(),
                            description = description.trim(),
                            adresse = adresse.trim(),
                            categorie = categorie,
                            lat = defaultLat,
                            lon = defaultLon,
                            horaires = horaires.trim(),
                            prix_moyen = prix.toIntOrNull() ?: 0,
                            site_web = siteWeb.trim(),
                            telephone = telephone.trim()
                        )
                    )
                }
            },
            enabled = nom.isNotBlank() && categorie.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White, disabledContainerColor = NavyDark.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add this place", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FormField(label: String, value: String, placeholder: String, minLines: Int = 1, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = InputHint, fontSize = 13.sp) },
            singleLine = minLines == 1,
            minLines = minLines,
            maxLines = if (minLines == 1) 1 else 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = White, focusedContainerColor = White,
                unfocusedBorderColor = InputBorder, focusedBorderColor = NavyDark,
                unfocusedTextColor = NavyDark, focusedTextColor = NavyDark
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlaceFilterChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) NavyDark else White)
            .border(1.dp, if (selected) NavyDark else InputBorder, RoundedCornerShape(50.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) White else NavyDark, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun StepCard(step: RouteStep, number: Int, onStepClick: () -> Unit = {}) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(step.category.color()),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.weight(1f).clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onStepClick
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SlotBadge(step.timeSlot)
                    CatBadge(step.category)
                }

                Text(step.name, fontWeight = FontWeight.Bold, color = NavyDark, fontSize = 15.sp)
                Text(step.address, color = InputHint, fontSize = 12.sp)

                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(10.dp)).background(step.category.color().copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (step.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(step.imageUrl).crossfade(true).build(),
                            contentDescription = step.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(step.category.icon(), contentDescription = null, tint = step.category.color().copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
                    }
                }

                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip(if (step.price > 0) "${step.price}€" else "Free", Icons.Filled.AttachMoney)
                    InfoChip("${step.durationMinutes} min", Icons.Filled.Schedule)
                    if (step.distanceFromPrev != "Start") InfoChip(step.distanceFromPrev, Icons.Filled.LocationOn)
                    if (step.rating > 0f) InfoChip("%.1f ★".format(step.rating), Icons.Filled.Star)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = InputHint, modifier = Modifier.size(11.dp))
                    Text(step.openingHours, color = InputHint, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("See details →", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- Small helpers ------------------------------------------------------------

@Composable
private fun WeatherWidget(weather: WeatherResponse) {
    val icon = when {
        weather.isSnow -> Icons.Filled.AcUnit
        weather.isRain -> Icons.Filled.Umbrella
        weather.cloudsPct > 70 -> Icons.Filled.Cloud
        else -> Icons.Filled.WbSunny
    }
    val iconColor = when {
        weather.isSnow -> Color(0xFF90CAF9)
        weather.isRain -> Color(0xFF42A5F5)
        weather.cloudsPct > 70 -> InputHint
        else -> Color(0xFFFFA726)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OffWhite)
            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${weather.tempC.toInt()}°C · ${weather.description.replaceFirstChar { it.uppercase() }}",
                color = NavyDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
            Text(
                if (weather.isGoodForOutdoor) "Good for outdoor activities" else "Not ideal for outdoors",
                color = if (weather.isGoodForOutdoor) Color(0xFF66BB6A) else Color(0xFFEF5350),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MetricChip(value: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(color.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
private fun SmallPill(value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(OffWhite).border(1.dp, InputBorder, RoundedCornerShape(50.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NavyDark, modifier = Modifier.size(11.dp))
        Text(value, color = NavyDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SlotBadge(slot: TimeSlot) {
    Box(Modifier.clip(RoundedCornerShape(50.dp)).background(slot.color().copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(slot.label, color = slot.color(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CatBadge(category: ActivityCategory) {
    Box(Modifier.clip(RoundedCornerShape(50.dp)).background(category.color().copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(category.label, color = category.color(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoChip(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(OffWhite).border(1.dp, InputBorder, RoundedCornerShape(50.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NavyDark.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
        Text(text, color = NavyDark, fontSize = 11.sp)
    }
}

@Composable
private fun ActionIconBtn(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(label, color = NavyDark.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

@Composable
private fun SelectChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) NavyDark else White)
            .border(1.dp, if (selected) NavyDark else InputBorder, RoundedCornerShape(50.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) White else NavyDark, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable
private fun ActivityCategoryChip(category: ActivityCategory, selected: Boolean, modifier: Modifier = Modifier, onToggle: (ActivityCategory) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) NavyDark else White)
            .border(1.dp, if (selected) NavyDark else InputBorder, RoundedCornerShape(12.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToggle(category) }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(category.icon(), contentDescription = null, tint = if (selected) White else category.color(), modifier = Modifier.size(22.dp))
            Text(category.label, color = if (selected) White else NavyDark, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}
