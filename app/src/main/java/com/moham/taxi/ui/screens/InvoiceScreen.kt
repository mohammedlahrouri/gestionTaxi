package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.moham.taxi.GestionTaxiApplication
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.core.content.FileProvider
import com.moham.taxi.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()
    val billingDataFlow = application.getBillingData().collectAsState(initial = com.moham.taxi.BillingData("", "", "", "", "", ""))

    // Comprobar si los datos de facturación están completos
    val billingData = billingDataFlow.value
    val billingComplete = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.license.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()

    // Datos de la factura
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientNif by remember { mutableStateOf("") }
    var showSummary by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    // Formateadores de fecha y hora
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentDate = remember { dateFormat.format(Date()) }
    val currentTime = remember { timeFormat.format(Date()) }

    // Cálculo del IVA
    val totalAmount = amount.toDoubleOrNull() ?: 0.0
    val baseAmount = totalAmount * 0.9  // 90% del total
    val ivaAmount = totalAmount * 0.1   // 10% del total

    fun generateAndSharePdf() {
        scope.launch {
            try {
                val pdfGenerator = PdfGenerator(context)
                val file = withContext(Dispatchers.IO) {
                    pdfGenerator.generateInvoice(
                        billingData = billingData,
                        clientName = clientName,
                        clientNif = clientNif,
                        origin = origin,
                        destination = destination,
                        totalAmount = totalAmount,
                        baseAmount = baseAmount,
                        ivaAmount = ivaAmount,
                        date = currentDate,
                        time = currentTime
                    )
                }
                generatedPdfFile = file
                showShareDialog = true
            } catch (e: Exception) {
                // Manejar el error
            }
        }
    }

    fun sharePdf() {
        generatedPdfFile?.let { file ->
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
            context.startActivity(Intent.createChooser(intent, "Compartir factura"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Factura") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                        Text("Debes rellenar primero los datos de facturación en ajustes para poder crear facturas.", color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ir a datos de facturación")
                        }
                    }
                }
            } else if (!showSummary) {
                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origen de la carrera") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destino de la carrera") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importe") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre del cliente o razón social") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = clientNif,
                    onValueChange = { clientNif = it },
                    label = { Text("NIF/CIF del cliente") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { showSummary = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = origin.isNotBlank() && destination.isNotBlank() && amount.isNotBlank() && clientName.isNotBlank() && clientNif.isNotBlank()
                ) {
                    Text("Generar factura")
                }
            } else {
                // Resumen de la factura
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Factura", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fecha: $currentDate")
                        Text("Hora: $currentTime")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Propietario: ${billingData.name}")
                        Text("NIF: ${billingData.nif}")
                        Text("Licencia: ${billingData.license}")
                        Text("Dirección: ${billingData.street}, ${billingData.city}, CP: ${billingData.postalCode}")
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                        )
                        Text("Cliente: $clientName")
                        Text("NIF/CIF: $clientNif")
                        Text("Origen: $origin")
                        Text("Destino: $destination")
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                        )
                        Text("Base imponible: ${String.format("%.2f", baseAmount)} €")
                        Text("IVA (10%): ${String.format("%.2f", ivaAmount)} €")
                        Text("Total: ${String.format("%.2f", totalAmount)} €", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { generateAndSharePdf() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar PDF")
                    }
                    Button(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = generatedPdfFile != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compartir")
                    }
                }
                Button(
                    onClick = { showSummary = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar")
                }
            }
        }
    }

    if (showShareDialog && generatedPdfFile != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Compartir factura") },
            text = { Text("¿Deseas compartir la factura generada?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sharePdf()
                        showShareDialog = false
                    }
                ) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
} 
