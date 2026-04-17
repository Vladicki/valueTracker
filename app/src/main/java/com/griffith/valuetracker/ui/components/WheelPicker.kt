package com.griffith.valuetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .height(196.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "No items", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val clampedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = clampedIndex)

    LaunchedEffect(clampedIndex) {
        if (listState.firstVisibleItemIndex != clampedIndex) {
            listState.animateScrollToItem(clampedIndex)
        }
    }

    LaunchedEffect(listState, items.size) {
        // Snap fling keeps one row aligned at the top, so the first visible item becomes the selection source of truth.
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index -> index.coerceIn(0, items.lastIndex) }
            .distinctUntilChanged()
            .collect { index -> onSelectedIndexChange(index) }
    }

    Box(
        modifier = modifier
            .height(196.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items.size) { index ->
                val isSelected = index == listState.firstVisibleItemIndex
                Text(
                    text = items[index],
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
            }
        }
    }
}
