package com.example.locus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.locus.R
import com.example.locus.ui.theme.*

enum class NavDestination { HOME, ADD, COMPASS, EXPLORE, PROFILE }

@Composable
fun BottomNav(
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(50.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    iconRes = R.drawable.ic_home,
                    destination = NavDestination.HOME,
                    selected = selected,
                    onSelect = onSelect
                )
                NavItem(
                    iconRes = R.drawable.ic_add,
                    destination = NavDestination.ADD,
                    selected = selected,
                    onSelect = onSelect
                )

                // -- Compass center —-
                IconButton(
                    onClick = { onSelect(NavDestination.COMPASS) },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Compass",
                        tint = Color.Unspecified, // preserves gold + navy from SVG
                        modifier = Modifier.size(44.dp)
                    )
                }

                NavItem(
                    iconRes = R.drawable.ic_explore,
                    destination = NavDestination.EXPLORE,
                    selected = selected,
                    onSelect = onSelect
                )
                NavItem(
                    iconRes = R.drawable.ic_account,
                    destination = NavDestination.PROFILE,
                    selected = selected,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    iconRes: Int,
    destination: NavDestination,
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit
) {
    val isSelected = selected == destination

    IconButton(
        onClick = { onSelect(destination) },
        modifier = Modifier
//            .size(50.dp)
            .then(
                if (isSelected)
                    Modifier
                        .clip(CircleShape)
                        .background(NavyDark)
                else
                    Modifier
            )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = destination.name,
            tint = if (isSelected) White else NavyDark,
            modifier = Modifier.size(32.dp)
        )
    }
}