package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSearchOverlay(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }

    if (!expanded) {
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
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            color = White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { expanded = false; from = ""; to = "" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NavyDark)
                    }
                    Text("Itinerary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = NavyDark)
                }

                // From row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF42A5F5))
                    )
                    OutlinedTextField(
                        value = from,
                        onValueChange = { from = it },
                        placeholder = { Text("From: My location", color = InputHint, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDark,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = NavyDark
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                }

                // Divider + swap button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = InputBorder.copy(alpha = 0.6f))
                    IconButton(
                        onClick = { val tmp = from; from = to; to = tmp },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.SwapVert, contentDescription = "Swap", tint = NavyDark, modifier = Modifier.size(20.dp))
                    }
                }

                // To row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                    OutlinedTextField(
                        value = to,
                        onValueChange = { to = it },
                        placeholder = { Text("To: destination", color = InputHint, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDark,
                            unfocusedBorderColor = InputBorder,
                            cursorColor = NavyDark
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )
                }

                // Get directions button — appears once destination is filled
                if (to.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { /* TODO: Mapbox Directions API */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get directions", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
