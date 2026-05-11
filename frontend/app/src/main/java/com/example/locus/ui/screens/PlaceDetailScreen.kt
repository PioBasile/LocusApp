package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.PlaceDetailViewModel

private fun categoryColor(cat: String): Color = when (cat) {
    "restaurant", "bar", "cafe" -> Color(0xFFFF7043)
    "musee"                     -> Color(0xFFAB47BC)
    "monument", "parc", "autre" -> Color(0xFF42A5F5)
    else                        -> Color(0xFF26A69A)
}

private fun categoryIcon(cat: String): ImageVector = when (cat) {
    "restaurant"              -> Icons.Filled.Restaurant
    "bar", "cafe"             -> Icons.Filled.LocalCafe
    "musee"                   -> Icons.Filled.Museum
    "monument"                -> Icons.Filled.AccountBalance
    "parc"                    -> Icons.Filled.Park
    "shopping"                -> Icons.Filled.ShoppingBag
    "sport"                   -> Icons.Filled.FitnessCenter
    "hotel"                   -> Icons.Filled.Hotel
    "plage"                   -> Icons.Filled.BeachAccess
    else                      -> Icons.Filled.Place
}

private fun categoryLabel(cat: String): String = when (cat) {
    "restaurant" -> "Restaurant"
    "bar"        -> "Bar"
    "cafe"       -> "Café"
    "musee"      -> "Museum"
    "monument"   -> "Monument"
    "parc"       -> "Park"
    "shopping"   -> "Shopping"
    "sport"      -> "Sport"
    "hotel"      -> "Hotel"
    "plage"      -> "Beach"
    else         -> cat.replaceFirstChar { it.uppercase() }
}

@Composable
fun PlaceDetailScreen(
    lieuId: Int,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit = {},
    onAddToFavorites: (String) -> Unit = {},
    viewModel: PlaceDetailViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(lieuId) { viewModel.load(lieuId) }

    val lieu = viewModel.lieu
    val posts = viewModel.posts
    val isLoading = viewModel.isLoading

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var favAdded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        if (isLoading && lieu == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyDark, strokeWidth = 2.5.dp)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                        .padding(top = statusBarTop)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                    lieu?.let { l ->
                        Text(
                            l.nom,
                            color = NavyDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 52.dp)
                        )
                        IconButton(
                            onClick = {
                                if (!favAdded) {
                                    onAddToFavorites(l.nom)
                                    favAdded = true
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                if (favAdded) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Add to favorites",
                                tint = if (favAdded) GoldPrimary else NavyDark
                            )
                        }
                    }
                }

                // ── Scrollable body ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Hero (photos pager or fallback) ──────────────────────
                    lieu?.let { l ->
                        val photos = l.photos.sortedBy { it.ordre }
                        val catColor = categoryColor(l.categorie)

                        if (photos.isNotEmpty()) {
                            val pagerState = rememberPagerState { photos.size }
                            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(photos[page].url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = photos[page].legende.ifEmpty { l.nom },
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                // Gradient at bottom
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
                                )
                                // Dot indicators
                                if (photos.size > 1) {
                                    Row(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        repeat(photos.size) { i ->
                                            Box(
                                                modifier = Modifier
                                                    .size(if (i == pagerState.currentPage) 7.dp else 5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (i == pagerState.currentPage) White else White.copy(alpha = 0.5f))
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (!l.urlImage.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(l.urlImage).crossfade(true).build(),
                                contentDescription = l.nom,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(260.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp).background(catColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(categoryIcon(l.categorie), contentDescription = null, tint = catColor.copy(alpha = 0.4f), modifier = Modifier.size(72.dp))
                            }
                        }
                    }

                    // ── Info card ────────────────────────────────────────────
                    lieu?.let { l ->
                        val catColor = categoryColor(l.categorie)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .offset(y = (-16).dp)
                                .padding(horizontal = 20.dp)
                                .padding(top = 24.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {

                            // Category + rating
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(catColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(categoryLabel(l.categorie), color = catColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                if (l.note > 0f) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("%.1f".format(l.note), color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (l.nbAvis > 0) {
                                        Text(" (${l.nbAvis})", color = InputHint, fontSize = 12.sp)
                                    }
                                }
                            }

                            // Name
                            Text(l.nom, color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)

                            // Address
                            if (l.adresse.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Place, contentDescription = null, tint = InputHint, modifier = Modifier.size(15.dp).padding(top = 1.dp))
                                    Text(l.adresse, color = InputHint, fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }

                            HorizontalDivider(color = InputBorder)

                            // Metrics chips
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (l.prixMoyen > 0) PlaceChip("~${l.prixMoyen}€", Icons.Filled.AttachMoney)
                                if (!l.horaires.isNullOrBlank()) PlaceChip(l.horaires, Icons.Filled.Schedule)
                                if (!l.telephone.isNullOrBlank()) PlaceChip(l.telephone, Icons.Filled.Phone)
                                if (!l.siteWeb.isNullOrBlank()) PlaceChip("Website", Icons.Filled.Language)
                            }

                            // Description
                            if (l.description.isNotBlank()) {
                                HorizontalDivider(color = InputBorder)
                                Text(l.description, color = DarkGray, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                    }

                    // ── Posts from this place ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(White)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = InputBorder)
                        Text("Photos from this place", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        if (posts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No photos yet", color = InputHint, fontSize = 13.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                posts.forEach { post ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(InputBorder)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { onPostClick(post.id) }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(post.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceChip(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(OffWhite)
            .border(1.dp, InputBorder, RoundedCornerShape(50.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NavyDark.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        Text(text, color = NavyDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
