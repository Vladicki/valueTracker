package com.griffith.valuetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun DateSwitcher(
    dateText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

@Composable
fun DaySelector(
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    onPreviousWeekClick: () -> Unit,
    onNextWeekClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous week",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onPreviousWeekClick),
        )
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            weekDays(selectedDate).forEach { date ->
                val isSelected = date == selectedDate
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onDaySelected(date) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (onNextWeekClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next week",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onNextWeekClick),
            )
        } else {
            Box(modifier = Modifier.size(20.dp))
        }
    }
}

fun weekStart(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekDays(selectedDate: LocalDate): List<LocalDate> {
    val start = weekStart(selectedDate)
    return (0..6).map { offset -> start.plusDays(offset.toLong()) }
}

fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}

fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
