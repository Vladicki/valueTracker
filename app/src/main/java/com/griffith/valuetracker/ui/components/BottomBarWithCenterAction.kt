package com.griffith.valuetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.FoodBank
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun BottomBarWithCenterAction(
    selectedIndex: Int,
    onItemClick: (Int) -> Unit,
    onScanFoodClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFoodDatabaseClick: () -> Unit,
    items: List<BottomBarItem>,
    modifier: Modifier = Modifier,
) {
    require(items.size == 4) {
        "BottomBarWithCenterAction requires exactly 4 items to match the reference shell."
    }

    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(32.dp),
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavigationItem(
                    item = items[0],
                    selected = selectedIndex == 0,
                    onClick = { if (!showMenu) onItemClick(0) },
                    modifier = Modifier.fillMaxWidth().testTag(items[0].testTag()),
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavigationItem(
                    item = items[1],
                    selected = selectedIndex == 1,
                    onClick = { if (!showMenu) onItemClick(1) },
                    modifier = Modifier.fillMaxWidth().testTag(items[1].testTag()),
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = { showMenu = true })
                        .testTag("center_action_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick actions",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavigationItem(
                    item = items[3],
                    selected = selectedIndex == 3,
                    onClick = { if (!showMenu) onItemClick(3) },
                    modifier = Modifier.fillMaxWidth().testTag(items[3].testTag()),
                )
            }
        }

        if (showMenu) {
            PlusActionPopup(
                onDismissRequest = { showMenu = false },
                onScanFoodClick = {
                    showMenu = false
                    onScanFoodClick()
                },
                onGalleryClick = {
                    showMenu = false
                    onGalleryClick()
                },
                onFoodDatabaseClick = {
                    showMenu = false
                    onFoodDatabaseClick()
                }
            )
        }
    }
}

@Composable
private fun PlusActionPopup(
    onDismissRequest: () -> Unit,
    onScanFoodClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFoodDatabaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val popupWidth = LocalConfiguration.current.screenWidthDp.dp * 0.5f

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -188),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Column(
            modifier = modifier
                .width(popupWidth)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("quick_actions_popup"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlusActionItem(
                icon = Icons.Default.CameraAlt,
                label = "Scan Food",
                onClick = onScanFoodClick
            )
            PlusActionItem(
                icon = Icons.Default.Collections,
                label = "Gallery",
                onClick = onGalleryClick
            )
            PlusActionItem(
                icon = Icons.Default.FoodBank,
                label = "Food Database",
                onClick = onFoodDatabaseClick
            )
        }
    }
}

@Composable
private fun PlusActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun NavigationItem(
    item: BottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.selectable(
            selected = selected,
            onClick = onClick,
            role = Role.Tab,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

private fun BottomBarItem.testTag(): String = "bottom_nav_${label.lowercase().replace(' ', '_')}"

data class BottomBarItem(
    val icon: ImageVector,
    val label: String,
)
