package com.moham.taxi.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import com.moham.taxi.data.service.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteFormScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val coroutineScope = rememberCoroutineScope()

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("") }
    var serviceTime by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            serviceDate = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val minStr = if (minute < 10) "0$minute" else "$minute"
            val hourStr = if (hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
            serviceTime = "$hourStr:$minStr"
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.quote_data_title)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it },
                label = { Text(stringResource(R.string.quote_origin_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(stringResource(R.string.quote_destination_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (serviceDate.isEmpty()) stringResource(R.string.choose_date_label) else serviceDate)
                }
                OutlinedButton(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (serviceTime.isEmpty()) stringResource(R.string.choose_time_label) else serviceTime)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (origin.isBlank() || destination.isBlank() || serviceDate.isBlank() || serviceTime.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.fields_required_toast), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    coroutineScope.launch {
                        val quoteData = application.currentQuote
                        val billingData = application.getBillingData().first()
                        if (quoteData == null) {
                            Toast.makeText(context, context.getString(R.string.no_quote_data_error), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val dateTime = context.getString(R.string.quote_datetime_format, serviceDate, serviceTime)
                        val pdfUri = PdfGenerator.generateQuote(
                            context = context,
                            billingData = billingData,
                            quoteData = quoteData,
                            origin = origin,
                            destination = destination,
                            dateTime = dateTime
                        )

                        if (pdfUri != null) {
                            Toast.makeText(context, context.getString(R.string.quote_generated_success), Toast.LENGTH_SHORT).show()
                            PdfGenerator.sharePdf(context, pdfUri)
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, context.getString(R.string.pdf_generation_error_simple), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = origin.isNotBlank() && destination.isNotBlank() && serviceDate.isNotBlank() && serviceTime.isNotBlank()
            ) {
                Text(stringResource(R.string.generate_pdf_button))
            }
        }
    }
}
