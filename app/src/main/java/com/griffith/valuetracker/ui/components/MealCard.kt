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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.griffith.valuetracker.ui.theme.AppCarbs
import com.griffith.valuetracker.ui.theme.AppFat
import com.griffith.valuetracker.ui.theme.AppProtein

@Composable
fun MealCard(
    title: String,
    calories: Int,
    portion: String = "",
    proteinGrams: Int? = null,
    fatGrams: Int? = null,
    carbsGrams: Int? = null,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    if (compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${calories} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (proteinGrams != null && fatGrams != null && carbsGrams != null) {
                        MacroInlineText(
                            label = "p:",
                            value = "${proteinGrams}g",
                            color = AppProtein,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        MacroInlineText(
                            label = "f:",
                            value = "${fatGrams}g",
                            color = AppFat,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        MacroInlineText(
                            label = "c:",
                            value = "${carbsGrams}g",
                            color = AppCarbs,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .testTag("food_add_button_$calories"),
            ) {
                if (trailingContent != null) {
                    trailingContent()
                } else {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = calories.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (portion.isNotBlank()) {
            Text(
                text = portion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (proteinGrams != null && fatGrams != null && carbsGrams != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MacroInlineText(
                    label = "p:",
                    value = "${proteinGrams}g",
                    color = AppProtein,
                    style = MaterialTheme.typography.bodySmall,
                )
                MacroInlineText(
                    label = "f:",
                    value = "${fatGrams}g",
                    color = AppFat,
                    style = MaterialTheme.typography.bodySmall,
                )
                MacroInlineText(
                    label = "c:",
                    value = "${carbsGrams}g",
                    color = AppCarbs,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun MacroInlineText(
    label: String,
    value: String,
    color: Color,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                append(label)
            }
            append(" ")
            withStyle(SpanStyle(color = valueColor)) {
                append(value)
            }
        },
        style = style,
    )
}
