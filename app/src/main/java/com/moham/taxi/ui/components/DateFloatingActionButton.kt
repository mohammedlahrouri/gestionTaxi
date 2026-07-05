package com.moham.taxi.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.moham.taxi.ui.theme.CalendarAccent
import com.moham.taxi.ui.theme.CalendarBackground
import com.moham.taxi.ui.theme.CalendarText
import com.moham.taxi.utils.DateUtils
import java.util.Date

/**
 * Botón flotante para selección de fecha que replica la funcionalidad del calendario de HomeScreen
 * 
 * @param currentDate La fecha actualmente seleccionada
 * @param onDateSelected Callback que se ejecuta cuando se selecciona una nueva fecha
 * @param modifier Modificador para personalizar el botón
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFloatingActionButton(
    currentDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Botón flotante
    FloatingActionButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showDatePicker = true
        },
        modifier = modifier,
        shape = CircleShape,
        containerColor = CalendarAccent.copy(alpha = 0.9f),
        contentColor = CalendarText
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = "Seleccionar fecha",
            modifier = Modifier.size(24.dp)
        )
    }
    
    // Diálogo de selección de fecha (idéntico al de HomeScreen)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToUtcStartOfDayMillis(currentDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(DateUtils.utcStartOfDayMillisToLocalDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CalendarBackground, // Fondo azul gris oscuro
                    titleContentColor = CalendarText, // Texto blanco
                    headlineContentColor = CalendarText, // Texto blanco
                    weekdayContentColor = CalendarText, // Texto blanco
                    subheadContentColor = CalendarText, // Texto blanco
                    yearContentColor = CalendarText, // Texto blanco
                    currentYearContentColor = CalendarAccent, // Azul acero
                    selectedYearContainerColor = CalendarAccent, // Azul acero
                    selectedYearContentColor = CalendarText, // Texto blanco
                    selectedDayContainerColor = CalendarAccent, // Azul acero
                    selectedDayContentColor = CalendarText, // Texto blanco
                    todayContentColor = CalendarAccent, // Azul acero
                    todayDateBorderColor = CalendarAccent, // Azul acero
                    dayContentColor = CalendarText.copy(alpha = 0.8f), // Texto gris claro
                    disabledDayContentColor = CalendarText.copy(alpha = 0.4f), // Texto gris muy claro
                    disabledSelectedDayContainerColor = CalendarAccent.copy(alpha = 0.4f), // Azul acero deshabilitado
                    disabledSelectedDayContentColor = CalendarText.copy(alpha = 0.4f), // Texto deshabilitado
                    dayInSelectionRangeContainerColor = CalendarAccent.copy(alpha = 0.3f), // Rango de selección
                    dayInSelectionRangeContentColor = CalendarText, // Texto en rango
                    navigationContentColor = CalendarText, // Navegación
                    disabledYearContentColor = CalendarText.copy(alpha = 0.4f) // Años deshabilitados
                )
            )
        }
    }
}
