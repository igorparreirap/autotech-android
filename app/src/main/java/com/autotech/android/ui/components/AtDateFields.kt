package com.autotech.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autotech.android.ui.theme.AtColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

// O DatePicker trabalha com a meia-noite em UTC; o app grava a data local (meio-dia).

private fun localToPickerUtc(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun pickerUtcToLocal(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val today = Calendar.getInstance()
    val sameAsToday = utc.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        utc.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    if (sameAsToday) return System.currentTimeMillis()
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0)
    }.timeInMillis
}

/** Compara apenas o dia (ignora a hora). */
fun sameLocalDay(a: Long, b: Long): Boolean = localToPickerUtc(a) == localToPickerUtc(b)

fun dayIsBeforeOrEqual(a: Long, b: Long): Boolean = localToPickerUtc(a) <= localToPickerUtc(b)

/**
 * Campo de data sem digitação: abre o calendário do sistema ao toque.
 * Só permite datas entre [minMillis] e [maxMillis] (por padrão, até hoje).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtDateField(
    label: String,
    millis: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxMillis: Long = System.currentTimeMillis(),
    minMillis: Long? = null
) {
    var open by remember { mutableStateOf(false) }
    val df = remember { SimpleDateFormat("dd/MM/yyyy", PT_BR) }

    Box(modifier.fillMaxWidth()) {
        AtField(
            value = df.format(Date(millis)),
            onValueChange = {},
            label = label,
            readOnly = true,
            leadingIcon = Icons.Rounded.Event,
            trailing = { Icon(Icons.Rounded.CalendarMonth, "Escolher data") }
        )
        // camada transparente por cima: o toque abre o calendário em vez do teclado
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
                .clickable { open = true }
        )
    }

    if (open) {
        val maxUtc = localToPickerUtc(maxMillis)
        val minUtc = minMillis?.let { localToPickerUtc(it) }
        val maxYear = Calendar.getInstance().apply { timeInMillis = maxMillis }.get(Calendar.YEAR)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = localToPickerUtc(millis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= maxUtc && (minUtc == null || utcTimeMillis >= minUtc)

                override fun isSelectableYear(year: Int): Boolean = year <= maxYear
            }
        )
        val colors = DatePickerDefaults.colors(
            containerColor = AtColors.Card,
            selectedDayContainerColor = AtColors.Primary,
            selectedDayContentColor = Color.White,
            todayDateBorderColor = AtColors.Primary,
            todayContentColor = AtColors.Primary,
            selectedYearContainerColor = AtColors.Primary,
            selectedYearContentColor = Color.White
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(pickerUtcToLocal(it)) }
                    open = false
                }) { Text("OK", color = AtColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancelar", color = AtColors.Muted) }
            },
            colors = colors
        ) {
            // sem o botão de alternar para digitação: a data é escolhida só no calendário
            DatePicker(state = state, showModeToggle = false, colors = colors)
        }
    }
}

/**
 * RF-04 / RF-05: data do evento (serviço ou abastecimento) e data do registro.
 * Marcado "mesmo dia": uma única data preenche as duas.
 * Desmarcado: o registro vem com a data de hoje e o evento é escolhido no calendário.
 */
@Composable
fun AtEventDates(
    eventLabel: String,
    sameDay: Boolean,
    onSameDayChange: (Boolean) -> Unit,
    eventMillis: Long,
    onEventChange: (Long) -> Unit,
    registeredMillis: Long,
    onRegisteredChange: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onSameDayChange(!sameDay) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = sameDay,
                onCheckedChange = { onSameDayChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = AtColors.Primary,
                    uncheckedColor = AtColors.Muted,
                    checkmarkColor = Color.White
                )
            )
            Text(
                "Mesma data para o ${eventLabel.lowercase(PT_BR)} e o registro",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedContent(
            targetState = sameDay,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "eventDates"
        ) { same ->
            if (same) {
                AtDateField(
                    label = "Data do ${eventLabel.lowercase(PT_BR)} e do registro",
                    millis = eventMillis,
                    onChange = { onEventChange(it); onRegisteredChange(it) }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AtDateField(
                        label = "Data do ${eventLabel.lowercase(PT_BR)}",
                        millis = eventMillis,
                        onChange = onEventChange,
                        maxMillis = registeredMillis
                    )
                    AtDateField(
                        label = "Data do registro",
                        millis = registeredMillis,
                        onChange = onRegisteredChange,
                        minMillis = eventMillis
                    )
                }
            }
        }
    }
}
