package com.example.locus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.data.model.Post
import com.example.locus.ui.theme.*

@Composable
fun Postcard(post: Post) {
    // Key on post.id so state is stable across recompositions
    key(post.id) {
        PostCardContent(post = post)
    }
}

@Composable
private fun PostCardContent(post: Post) {
    // Variables par défaut pour l'UI en attendant que ton backend fournisse ces infos
    val authorName = "User ${post.userId}"
    val authorAvatarUrl = "https://picsum.photos/seed/${post.userId}/100/100" // Image random basée sur l'ID
    val locationName = if (post.idLoc != null) "Loc ${post.idLoc}" else "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {

            // -- Header --------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = authorAvatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        color = White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = post.date, // Remplacement de timeAgo par date
                        color = White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = White.copy(alpha = 0.7f)
                    )
                }
            }

            // -- Image unique (Pager retiré car une seule imageUrl) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = post.imageUrl, // Remplacement de images par imageUrl
                    contentDescription = "Post image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // -- Actions row ---------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostActionItem(
                    icon = {
                        Icon(
                            Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    count = 0 // Forcé à 0 pour le moment
                )
                Spacer(modifier = Modifier.width(14.dp))
                PostActionItem(
                    icon = {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    count = 0 // Forcé à 0 pour le moment
                )
                Spacer(modifier = Modifier.width(14.dp))
                PostActionItem(
                    icon = {
                        Icon(
                            Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    count = 0 // Forcé à 0 pour le moment
                )

                Spacer(modifier = Modifier.weight(1f))

                // Location chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = locationName, // Utilise l'ID de localisation temporairement
                        color = White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = ">",
                        color = White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            // -- Caption -------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "$authorName : ",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = post.description, // Remplacement de caption par description
                    color = White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PostActionItem(icon: @Composable () -> Unit, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = White,
            fontSize = 12.sp
        )
    }
}