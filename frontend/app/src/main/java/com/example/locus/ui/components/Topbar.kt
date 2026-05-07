package com.example.locus.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.ui.theme.*

@Composable
fun Topbar(
    modifier: Modifier = Modifier,
    showGroupSelector: Boolean = false,
    selectedGroup: MyGroupResponse? = null,
    groups: List<MyGroupResponse> = emptyList(),
    onGroupChange: (MyGroupResponse) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()          
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // -- Locus wordmark ------------------------------------
            Image(
                painter = painterResource(id = R.drawable.ic_name),
                contentDescription = "Locus",
                modifier = Modifier
                    .width(90.dp)
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // -- Group selector (optional) -------------------------
            if (showGroupSelector) {
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                selectedGroup == null || selectedGroup.id == 0 -> "Public Posts"
                                else -> selectedGroup.name
                            },
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
                        // Public Posts is always the first option
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Public Posts",
                                    color = if (selectedGroup == null || selectedGroup.id == 0) GoldPrimary else NavyDark,
                                    fontWeight = if (selectedGroup == null || selectedGroup.id == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                onGroupChange(MyGroupResponse(id = 0, name = "Public Posts", isPrivate = false, description = "", imageUrl = null))
                                expanded = false
                            }
                        )
                        groups.filter { it.id != 0 }.forEach { group ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = group.name,
                                        color = if (selectedGroup?.id == group.id) GoldPrimary else NavyDark,
                                        fontWeight = if (selectedGroup?.id == group.id) FontWeight.SemiBold else FontWeight.Normal,
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
        }

        HorizontalDivider(color = LightGray, thickness = 0.5.dp)

    }
}