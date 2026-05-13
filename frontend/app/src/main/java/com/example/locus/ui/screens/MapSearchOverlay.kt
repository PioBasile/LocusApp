package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locus.data.remote.LieuResponse
import com.example.locus.ui.theme.*
import com.example.locus.utils.parseLatLon
import com.example.locus.viewmodel.RoutePlanningViewModel

@Composable
fun MapSearchOverlay(
    modifier: Modifier = Modifier,
    viewModel: RoutePlanningViewModel,
    currentGps: String,
    onFlyTo: (lat: Double, lon: Double) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // From / To state
    var fromText by remember { mutableStateOf("") }
    var fromGps  by remember { mutableStateOf("") }
    var toText   by remember { mutableStateOf("") }
    var toGps    by remember { mutableStateOf("") }
    // Which field is currently active (receiving suggestions)
    var activeField by remember { mutableStateOf("to") }

    val suggestions   = viewModel.mapSearchResults
    val isSearching   = viewModel.isSearchingMap
    val isLoadingRoute = viewModel.isLoadingNavRoute

    val toFocusRequester = remember { FocusRequester() }

    if (!expanded) {
        // ── Collapsed pill ─────────────────────────────────────────────────
        Surface(
            onClick = { expanded = true },
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(50.dp),
            color = White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = NavyDark, modifier = Modifier.size(20.dp))
                Text("Search for a destination...", color = InputHint, fontSize = 15.sp)
            }
        }
    } else {
        // ── Expanded — fills the parent Box ───────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            // Dim scrim — tap to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        expanded = false
                        viewModel.clearMapSearch()
                    }
            )

            // White card anchored to top
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
                color = White,
                shadowElevation = 12.dp
            ) {
                Column {

                    // ── Header ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { expanded = false; viewModel.clearMapSearch() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = NavyDark)
                        }
                        Text("Directions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = NavyDark)
                    }

                    // ── From field ────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF42A5F5))
                        )
                        TextField(
                            value = fromText,
                            onValueChange = {
                                fromText = it
                                fromGps = ""
                                activeField = "from"
                                viewModel.searchMapPlaces(it)
                            },
                            placeholder = { Text("My location", color = InputHint, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) { activeField = "from"; if (fromText.isNotBlank()) viewModel.searchMapPlaces(fromText) } },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = NavyDark,
                                focusedTextColor   = NavyDark,
                                unfocusedTextColor = NavyDark
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        if (fromText.isNotEmpty()) {
                            IconButton(
                                onClick = { fromText = ""; fromGps = ""; viewModel.clearMapSearch() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = MediumGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // ── Swap / divider ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = InputBorder.copy(alpha = 0.5f))
                        IconButton(
                            onClick = {
                                val tmpT = fromText; fromText = toText; toText = tmpT
                                val tmpG = fromGps;  fromGps  = toGps;  toGps  = tmpG
                                viewModel.clearMapSearch()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.SwapVert, contentDescription = "Swap", tint = NavyDark, modifier = Modifier.size(20.dp))
                        }
                    }

                    // ── To field ──────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(14.dp)
                        )
                        TextField(
                            value = toText,
                            onValueChange = {
                                toText = it
                                toGps = ""
                                activeField = "to"
                                viewModel.searchMapPlaces(it)
                            },
                            placeholder = { Text("Destination", color = InputHint, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(toFocusRequester)
                                .onFocusChanged { if (it.isFocused) { activeField = "to"; if (toText.isNotBlank()) viewModel.searchMapPlaces(toText) } },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = NavyDark,
                                focusedTextColor   = NavyDark,
                                unfocusedTextColor = NavyDark
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                        if (toText.isNotEmpty()) {
                            IconButton(
                                onClick = { toText = ""; toGps = ""; viewModel.clearMapSearch() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = MediumGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ── Go button (shown once destination GPS is resolved) ─
                    val startReady = fromGps.isNotBlank() || currentGps.isNotBlank()
                    if (toGps.isNotBlank() && startReady) {
                        Button(
                            onClick = {
                                val startGps = fromGps.ifBlank { currentGps }
                                viewModel.fetchDirectRouteFromTo(startGps, toGps)
                                toGps.parseLatLon()?.let { (lat, lon) -> onFlyTo(lat, lon) }
                                expanded = false
                                viewModel.clearMapSearch()
                            },
                            enabled = !isLoadingRoute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White)
                        ) {
                            if (isLoadingRoute) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Loading…", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Go", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // Loading bar / divider
                    if (isSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NavyDark, trackColor = LightGray)
                    } else {
                        HorizontalDivider(color = LightGray)
                    }

                    // ── Suggestions ───────────────────────────────────────
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {

                        // "My position" — always shown when FROM is active
                        if (activeField == "from") {
                            item {
                                MyPositionRow {
                                    fromText = "My location"
                                    fromGps  = currentGps
                                    viewModel.clearMapSearch()
                                    activeField = "to"
                                }
                                if (suggestions.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = LightGray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        items(suggestions, key = { it.id }) { lieu ->
                            PlaceSuggestionRow(lieu = lieu) {
                                val gps = lieu.gps ?: return@PlaceSuggestionRow
                                if (activeField == "from") {
                                    fromText = lieu.nom; fromGps = gps
                                    viewModel.clearMapSearch()
                                    activeField = "to"
                                } else {
                                    toText = lieu.nom; toGps = gps
                                    viewModel.clearMapSearch()
                                }
                            }
                            if (lieu != suggestions.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = LightGray.copy(alpha = 0.4f)
                                )
                            }
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        // Auto-focus the destination field when opening
        LaunchedEffect(Unit) { toFocusRequester.requestFocus() }
    }
}

@Composable
private fun MyPositionRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1565C0).copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("My position", color = NavyDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Your current location", color = InputHint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlaceSuggestionRow(lieu: LieuResponse, onClick: () -> Unit) {
    val catColor = when (lieu.categorie) {
        "restaurant", "bar", "cafe" -> Color(0xFFFF7043)
        "musee", "monument"         -> Color(0xFFAB47BC)
        "parc"                      -> Color(0xFF66BB6A)
        "sport", "plage"            -> Color(0xFF42A5F5)
        "shopping"                  -> Color(0xFFFFCA28)
        else                        -> Color(0xFF78909C)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                lieu.nom,
                color = NavyDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(lieu.categorie.replaceFirstChar { it.uppercase() })
                    if (lieu.adresse.isNotBlank()) append(" · ${lieu.adresse}")
                },
                color = InputHint,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (lieu.distanceKm > 0) {
            Text("%.1fkm".format(lieu.distanceKm), color = InputHint, fontSize = 12.sp)
        }
    }
}
