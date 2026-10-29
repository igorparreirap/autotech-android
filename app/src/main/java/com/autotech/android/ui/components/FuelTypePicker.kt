package com.autotech.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autotech.android.domain.FuelType
import com.autotech.android.ui.theme.AtColors

/** RF-03: escolha do combustível em grade 2 x 2 (Flex, Gasolina, Diesel, Híbrido). */
@Composable
fun FuelTypePicker(selected: FuelType?, onSelect: (FuelType) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FuelType.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    val on = type == selected
                    val bg by animateColorAsState(if (on) AtColors.Primary else AtColors.Field, tween(220), label = "fuelBg")
                    val fg by animateColorAsState(if (on) Color.White else AtColors.Ink, tween(220), label = "fuelFg")
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .clickable { onSelect(type) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(type.label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
                        Text(
                            type.hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (on) Color.White.copy(alpha = 0.85f) else AtColors.Muted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
