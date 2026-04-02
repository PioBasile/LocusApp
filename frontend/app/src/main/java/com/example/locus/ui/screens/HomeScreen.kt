package com.example.locus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locus.R
import com.example.locus.data.model.Post
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Postcard
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*

private val dummyPosts = listOf(
    Post(
        id = 1,
        userId = 42, // Remplace l'ancien "authorName" / "username"
        groupe = 1,
        description = "Small pic I took", // Remplace "caption"
        imageUrl = "https://picsum.photos/seed/paris1/600/400", // Remplace la liste "images"
        date = "15 minutes ago", // Remplace "timeAgo"
        idLoc = 34 // ID temporaire remplaçant "Montpellier, France"
    ),
    Post(
        id = 2,
        userId = 42,
        groupe = 2,
        description = "Beautiful morning walk along the Seine",
        imageUrl = "https://picsum.photos/seed/paris2/600/400",
        date = "1 hour ago",
        idLoc = 75 
    ),
    Post(
        id = 3,
        userId = 89,
        groupe = 1,
        description = "Sunset vibes by the beach",
        imageUrl = "https://picsum.photos/seed/beach1/600/400",
        date = "3 hours ago",
        idLoc = null // La localisation peut être null selon ton backend (*int)
    )
)

@Composable
fun HomeScreen(
    isGuest: Boolean = false,
    onNavigate: (NavDestination) -> Unit = {}
) {
    var selectedNav by remember { mutableStateOf(NavDestination.HOME) }
    var selectedGroup by remember { mutableStateOf("Group XYZ") }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // -- Scrollable feed ---------------------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 130.dp,
                    bottom = 110.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            dummyPosts.forEach { post ->
                Postcard(post = post)
            }
        }

        // -- Top bar -----------------------------------------------
        Topbar(
            modifier = Modifier.align(Alignment.TopCenter),
            showGroupSelector = true,
            selectedGroup = selectedGroup,
            groups = listOf("Group XYZ", "Group ABC", "All Groups"),
            onGroupChange = { selectedGroup = it }
        )

        // -- Floating nav ------------------------------------------
        BottomNav(
            selected = selectedNav,
            onSelect = {
                selectedNav = it
                onNavigate(it)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GroupSelector(
    groupName: String,
    onGroupChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val groups = listOf("Group XYZ", "Group ABC", "All Groups")

    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = groupName,
                color = NavyDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Select group",
                tint = NavyDark
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(White)
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = group,
                            color = NavyDark,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onGroupChange(group)
                        expanded = false
                    }
                )
            }
        }
    }
}