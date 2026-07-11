package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.utils.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private enum class ExportPeriod {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    RANGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var exportInProgress by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(0) } // 0: PDF, 1: CSV, 2: Tickets
    var period by remember { mutableStateOf(ExportPeriod.DAY) }

    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var rangeStartDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var rangeEndDate by remember { mutableStateOf(LocalDate.now()) }

    var showSingleDatePicker by remember { mutableStateOf(false) }
    var showRangeStartPicker by remember { mutableStateOf(false) }
    var showRangeEndPicker by remember { mutableStateOf(false) }
    var showFormatPicker by remember { mutableStateOf(false) }

    fun exportAndShare(runExport: suspend () -> com.moham.taxi.data.service.ExportResult) {
        exportInProgress = true
        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val exportResult = runExport()
                when (exportResult) {
                    is com.moham.taxi.data.service.ExportResult.Success -> {
                        snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                        val mime = when (exportFormat) {
                            0 -> "application/pdf"
                            1 -> "text/csv"
                            else -> "application/zip"
                        }
                        val shareResult = application.exportService.shareFile(exportResult.uri, exportResult.fileName, mime)
                        if (shareResult is com.moham.taxi.data.service.ShareResult.Error) {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.export_share_error, shareResult.message)
                            )
                        }
                    }
                    is com.moham.taxi.data.service.ExportResult.Error -> {
                        snackbarHostState.showSnackbar(context.getString(R.string.export_error, exportResult.message))
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.error_unexpected,
                        e.localizedMessage ?: e.message.orEmpty()
                    )
                )
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 10000) {
                    delay(10000 - elapsed)
                }
                exportInProgress = false
            }
        }
    }

    LaunchedEffect(rangeStartDate) {
        if (rangeEndDate.isBefore(rangeStartDate)) {
            rangeEndDate = rangeStartDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_export_data_card_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (exportInProgress) {
                // Pantalla de carga: solo barra centrada, nada más visible
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.dialog_exporting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Contenido normal de exportación
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (period == ExportPeriod.RANGE) {
                        Text(stringResource(R.string.dialog_export_range), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showRangeStartPicker = true }
                                    .padding(8.dp)
                            ) {
                                Text(stringResource(R.string.dialog_export_select_start_date), fontWeight = FontWeight.Medium)
                                Text(rangeStartDate.format(dateFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showRangeEndPicker = true }
                                    .padding(8.dp)
                            ) {
                                Text(stringResource(R.string.dialog_export_select_end_date), fontWeight = FontWeight.Medium)
                                Text(rangeEndDate.format(dateFormatter), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSingleDatePicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedDate.format(dateFormatter), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.dialog_date_change_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text(stringResource(R.string.dialog_export_format_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFormatPicker = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (exportFormat) {
                                    0 -> stringResource(R.string.dialog_export_format_pdf)
                                    1 -> stringResource(R.string.dialog_export_format_csv)
                                    else -> "Tickets"
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.dialog_date_change_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { period = ExportPeriod.DAY }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = period == ExportPeriod.DAY, onClick = { period = ExportPeriod.DAY })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_day), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { period = ExportPeriod.WEEK }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = period == ExportPeriod.WEEK, onClick = { period = ExportPeriod.WEEK })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_week), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { period = ExportPeriod.MONTH }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = period == ExportPeriod.MONTH, onClick = { period = ExportPeriod.MONTH })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_month), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { period = ExportPeriod.YEAR }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = period == ExportPeriod.YEAR, onClick = { period = ExportPeriod.YEAR })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_year), fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { period = ExportPeriod.RANGE }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = period == ExportPeriod.RANGE, onClick = { period = ExportPeriod.RANGE })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_range), fontWeight = FontWeight.Medium)
                        }
                    }

                    Button(
                        onClick = {
                            when (period) {
                                ExportPeriod.DAY -> {
                                    val dateToExport = Date.from(selectedDate.atStartOfDay(zone).toInstant())
                                    exportAndShare {
                                        when (exportFormat) {
                                            0 -> application.exportService.exportDayData(dateToExport)
                                            1 -> application.exportService.exportDayDataCsv(dateToExport)
                                            else -> application.exportService.exportDayDataTickets(dateToExport)
                                        }
                                    }
                                }
                                ExportPeriod.WEEK -> {
                                    val dateToExport = Date.from(selectedDate.atStartOfDay(zone).toInstant())
                                    exportAndShare {
                                        val firstDayOfWeek = application.getFirstDayOfWeek().first()
                                        when (exportFormat) {
                                            0 -> application.exportService.exportWeekData(dateToExport, firstDayOfWeek)
                                            1 -> application.exportService.exportWeekDataCsv(dateToExport, firstDayOfWeek)
                                            else -> application.exportService.exportWeekDataTickets(dateToExport, firstDayOfWeek)
                                        }
                                    }
                                }
                                ExportPeriod.MONTH -> {
                                    val dateToExport = Date.from(selectedDate.atStartOfDay(zone).toInstant())
                                    exportAndShare {
                                        when (exportFormat) {
                                            0 -> application.exportService.exportMonthData(dateToExport)
                                            1 -> application.exportService.exportMonthDataCsv(dateToExport)
                                            else -> application.exportService.exportMonthDataTickets(dateToExport)
                                        }
                                    }
                                }
                                ExportPeriod.YEAR -> {
                                    val dateToExport = Date.from(selectedDate.atStartOfDay(zone).toInstant())
                                    exportAndShare {
                                        when (exportFormat) {
                                            0 -> application.exportService.exportYearData(dateToExport)
                                            1 -> application.exportService.exportYearDataCsv(dateToExport)
                                            else -> application.exportService.exportYearDataTickets(dateToExport)
                                        }
                                    }
                                }
                                ExportPeriod.RANGE -> {
                                    val start = Date.from(rangeStartDate.atStartOfDay(zone).toInstant())
                                    val end = Date.from(rangeEndDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant())
                                    exportAndShare {
                                        when (exportFormat) {
                                            0 -> application.exportService.exportRangeData(start, end)
                                            1 -> application.exportService.exportRangeDataCsv(start, end)
                                            else -> application.exportService.exportRangeDataTickets(start, end)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dialog_button_export))
                    }
                }
            }
        }
    }

    if (showSingleDatePicker) {
        val datePickerState = remember(selectedDate) {
            DatePickerState(
                locale = Locale.getDefault(),
                initialSelectedDateMillis = DateUtils.localDateToUtcStartOfDayMillis(selectedDate)
            )
        }
        DatePickerDialog(
            onDismissRequest = { showSingleDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = DateUtils.utcMillisToLocalDate(millis)
                        }
                        showSingleDatePicker = false
                    }
                ) { Text(stringResource(R.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showSingleDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showRangeStartPicker) {
        val datePickerState = remember(rangeStartDate) {
            DatePickerState(
                locale = Locale.getDefault(),
                initialSelectedDateMillis = DateUtils.localDateToUtcStartOfDayMillis(rangeStartDate)
            )
        }
        DatePickerDialog(
            onDismissRequest = { showRangeStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = DateUtils.utcMillisToLocalDate(millis)
                            rangeStartDate = newDate
                            if (rangeEndDate.isBefore(newDate)) {
                                rangeEndDate = newDate
                            }
                        }
                        showRangeStartPicker = false
                    }
                ) { Text(stringResource(R.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangeStartPicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }

    if (showRangeEndPicker) {
        val datePickerState = remember(rangeEndDate) {
            DatePickerState(
                locale = Locale.getDefault(),
                initialSelectedDateMillis = DateUtils.localDateToUtcStartOfDayMillis(rangeEndDate)
            )
        }
        DatePickerDialog(
            onDismissRequest = { showRangeEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = DateUtils.utcMillisToLocalDate(millis)
                            rangeEndDate = newDate
                            if (rangeStartDate.isAfter(newDate)) {
                                rangeStartDate = newDate
                            }
                        }
                        showRangeEndPicker = false
                    }
                ) { Text(stringResource(R.string.action_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showRangeEndPicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
    
    if (showFormatPicker) {
        AlertDialog(
            onDismissRequest = { showFormatPicker = false },
            title = { Text(stringResource(R.string.dialog_export_format_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportFormat == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        onClick = {
                            exportFormat = 0
                            showFormatPicker = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportFormat == 0,
                                onClick = {
                                    exportFormat = 0
                                    showFormatPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_format_pdf), fontWeight = FontWeight.Medium)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportFormat == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        onClick = {
                            exportFormat = 1
                            showFormatPicker = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportFormat == 1,
                                onClick = {
                                    exportFormat = 1
                                    showFormatPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.dialog_export_format_csv), fontWeight = FontWeight.Medium)
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportFormat == 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        onClick = {
                            exportFormat = 2
                            showFormatPicker = false
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = exportFormat == 2,
                                onClick = {
                                    exportFormat = 2
                                    showFormatPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tickets", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatPicker = false }) {
                    Text(stringResource(R.string.action_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormatPicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
