package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moham.taxi.R
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.screens.BottomNavBar
import kotlinx.coroutines.launch
import com.moham.taxi.GestionTaxiApplication
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.core.content.FileProvider
import com.moham.taxi.utils.PdfGenerator
import com.moham.taxi.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import android.app.TimePickerDialog
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()
    val billingDataFlow = application.getBillingData().collectAsState(initial = com.moham.taxi.BillingData("", "", "", "", "", ""))
    val lastInvoiceNumber by application.getLastInvoiceNumber().collectAsState(initial = 0)

    // Configurar el manejo del botón Atrás para volver a la pantalla Otras opciones
    BackHandler {
        navController.navigate(AppScreens.Other.route) {
            popUpTo(AppScreens.Other.route) {
                inclusive = false
            }
            launchSingleTop = true
            // Las animaciones se manejan en AppNavigation.kt
        }
    }

    // Comprobar si los datos de facturación están completos
    val billingData = billingDataFlow.value
    val billingComplete = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.license.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()

    // Datos de la factura
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientNif by remember { mutableStateOf("") }
    var clientAddress by remember { mutableStateOf("") }
    var showSummary by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var invoiceNumberInput by remember { mutableStateOf("") }
    val invoiceNumber by remember { derivedStateOf { invoiceNumberInput.toIntOrNull() ?: 0 } }

    // Formateadores de fecha y hora
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val initialCalendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(initialCalendar.time) }
    var selectedHour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    val formattedTime = remember(selectedHour, selectedMinute) { String.format("%02d:%02d", selectedHour, selectedMinute) }

    LaunchedEffect(lastInvoiceNumber) {
        if (invoiceNumberInput.isEmpty()) {
            invoiceNumberInput = (lastInvoiceNumber + 1).toString()
        }
    }

    // Cálculo del IVA
    val totalAmount = amount.toDoubleOrNull() ?: 0.0
    val baseAmount = totalAmount / 1.10  // Base imponible = Total / 1.10
    val ivaAmount = totalAmount - baseAmount   // IVA = Total - Base

    fun generateAndSharePdf() {
        scope.launch {
            try {
                isGeneratingPdf = true
                errorMessage = null
                val pdfGenerator = PdfGenerator(context)
                val file = withContext(Dispatchers.IO) {
                    pdfGenerator.generateInvoice(
                        billingData = billingData,
                        clientName = clientName,
                        clientNif = clientNif,
                        clientAddress = clientAddress,
                        origin = origin,
                        destination = destination,
                        invoiceNumber = invoiceNumber,
                        totalAmount = totalAmount,
                        baseAmount = baseAmount,
                        ivaAmount = ivaAmount,
                        date = formattedDate,
                        time = formattedTime
                    )
                }
                generatedPdfFile = file
                application.saveLastInvoiceNumber(invoiceNumber)
                invoiceNumberInput = (invoiceNumber + 1).toString()
                isGeneratingPdf = false
                Toast.makeText(context, context.getString(R.string.pdf_generated_success), Toast.LENGTH_SHORT).show()
                showShareDialog = true
            } catch (e: Exception) {
                isGeneratingPdf = false
                errorMessage = context.getString(R.string.pdf_generation_error, e.message)
                Toast.makeText(context, context.getString(R.string.pdf_generation_error, e.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun sharePdf() {
        try {
            generatedPdfFile?.let { file ->
                if (!file.exists()) {
                    Toast.makeText(context, context.getString(R.string.pdf_not_found), Toast.LENGTH_SHORT).show()
                    return
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_invoice_title)))
            } ?: run {
                Toast.makeText(context, context.getString(R.string.pdf_no_file), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.pdf_share_error_msg, e.message), Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.invoice_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Reutilizamos el BottomNavBar del HomeScreen
            BottomNavBar(
                selectedItem = 3, // Seleccionamos "Otras"
                onItemSelected = { index ->
                    when (index) {
                        0 -> navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                        1 -> navController.navigate(AppScreens.TaxiRideList.createRouteWithDate(System.currentTimeMillis()))
                        2 -> navController.navigate(AppScreens.Statistics.route)
                        3 -> navController.navigate(AppScreens.Other.route)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!billingComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.invoice_billing_data_missing),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.go_to_billing_data))
                        }
                    }
                }
            } else if (!showSummary) {
                OutlinedTextField(
                    value = invoiceNumberInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            invoiceNumberInput = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.invoice_number_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.date)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                        }
                    }
                )
                OutlinedTextField(
                    value = formattedTime,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.time)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedHour = hour
                                        selectedMinute = minute
                                        val calendar = Calendar.getInstance().apply {
                                            time = selectedDate
                                            set(Calendar.HOUR_OF_DAY, hour)
                                            set(Calendar.MINUTE, minute)
                                        }
                                        selectedDate = calendar.time
                                    },
                                    selectedHour,
                                    selectedMinute,
                                    true
                                ).show()
                            }
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null)
                        }
                    }
                )
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text(stringResource(R.string.ride_origin)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text(stringResource(R.string.ride_destination)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Permitir entrada de números decimales
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                        }
                    },
                    label = { Text(stringResource(R.string.amount)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text(stringResource(R.string.client_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientNif,
                    onValueChange = { clientNif = it },
                    label = { Text(stringResource(R.string.client_nif_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientAddress,
                    onValueChange = { clientAddress = it },
                    label = { Text(stringResource(R.string.client_address_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { showSummary = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = origin.isNotBlank() && destination.isNotBlank() && amount.isNotBlank() && clientName.isNotBlank() && clientNif.isNotBlank() && clientAddress.isNotBlank() && invoiceNumber > 0
                ) {
                    Text(stringResource(R.string.generate_invoice_button))
                }
            } else {
                // Resumen de la factura
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.invoice_title), style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.invoice_number_display, invoiceNumber))
                        Text("${stringResource(R.string.date)}: $formattedDate")
                        Text("${stringResource(R.string.time)}: $formattedTime")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.owner_label, billingData.name))
                        Text(stringResource(R.string.nif_display, billingData.nif))
                        Text(stringResource(R.string.license_label, billingData.license))
                        Text(
                            stringResource(
                                R.string.address_label,
                                "${billingData.street}, ${billingData.city}, ${stringResource(R.string.postal_code_short, billingData.postalCode)}"
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                        )
                        Text(stringResource(R.string.client_label, clientName))
                        Text(stringResource(R.string.nif_cif_display, clientNif))
                        Text(stringResource(R.string.address_display, clientAddress))
                        Text(stringResource(R.string.origin_display, origin))
                        Text(stringResource(R.string.destination_display, destination))
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                        )
                        Text(stringResource(R.string.base_amount_label, String.format("%.2f", baseAmount)))
                        Text(stringResource(R.string.vat_amount_label, String.format("%.2f", ivaAmount)))
                        Text(
                            stringResource(R.string.total_amount_label, String.format("%.2f", totalAmount)),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                // Mostrar mensaje de error si existe
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { generateAndSharePdf() },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingPdf
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.generating_pdf))
                        } else {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.generate_pdf_button))
                        }
                    }
                    Button(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = generatedPdfFile != null && !isGeneratingPdf
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_button))
                    }
                }
                Button(
                    onClick = { showSummary = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.edit))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToUtcStartOfDayMillis(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance().apply {
                                time = DateUtils.utcStartOfDayMillisToLocalDate(millis)
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                            }
                            selectedDate = calendar.time
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
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

    if (showShareDialog && generatedPdfFile != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(R.string.share_invoice_title)) },
            text = { Text(stringResource(R.string.share_invoice_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        sharePdf()
                        showShareDialog = false
                    }
                ) {
                    Text(stringResource(R.string.share_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
