package com.moham.taxi.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.QuoteData
import com.moham.taxi.data.model.QuoteItem
import com.moham.taxi.data.service.PdfGenerator
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.utils.PriceUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

data class ManualSurchargeItem(
    val name: String,
    val price: Double,
    val quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualQuoteScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val coroutineScope = rememberCoroutineScope()

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var serviceDate by remember { mutableStateOf("") }
    var serviceTime by remember { mutableStateOf("") }
    var totalKm by remember { mutableStateOf("") }
    var tariffName by remember { mutableStateOf("") }
    var tariffStartPrice by remember { mutableStateOf("") }
    var tariffPricePerKm by remember { mutableStateOf("") }

    // Surcharges list
    val surcharges = remember { mutableStateListOf<ManualSurchargeItem>() }

    // Surcharge inputs
    var tempSurchargeName by remember { mutableStateOf("") }
    var tempSurchargePrice by remember { mutableStateOf("") }
    var tempSurchargeQuantity by remember { mutableStateOf("1") }

    // User total price
    var userTotalPrice by remember { mutableStateOf("") }

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

    // Calculated price helper
    val kmVal = totalKm.toDoubleOrNull() ?: 0.0
    val startVal = tariffStartPrice.toDoubleOrNull() ?: 0.0
    val perKmVal = tariffPricePerKm.toDoubleOrNull() ?: 0.0
    val surchargesSum = surcharges.sumOf { it.price * it.quantity }
    val calculatedPrice = (kmVal * perKmVal) + startVal + surchargesSum

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.create_manual_quote_title)) })
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (serviceDate.isEmpty()) stringResource(R.string.choose_date_label) else serviceDate)
                }
                OutlinedButton(
                    onClick = { timePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (serviceTime.isEmpty()) stringResource(R.string.choose_time_label) else serviceTime)
                }
            }

            OutlinedTextField(
                value = totalKm,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) totalKm = it },
                label = { Text(stringResource(R.string.label_kilometers)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = tariffName,
                onValueChange = { tariffName = it },
                label = { Text(stringResource(R.string.label_manual_tariff_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tariffStartPrice,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) tariffStartPrice = it },
                    label = { Text(stringResource(R.string.label_tariff_start_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tariffPricePerKm,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) tariffPricePerKm = it },
                    label = { Text(stringResource(R.string.label_tariff_price_per_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Supplements add Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.btn_add_surcharge), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = tempSurchargeName,
                        onValueChange = { tempSurchargeName = it },
                        label = { Text(stringResource(R.string.label_surcharge_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tempSurchargePrice,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) tempSurchargePrice = it },
                            label = { Text(stringResource(R.string.label_surcharge_price)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempSurchargeQuantity,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*$"))) tempSurchargeQuantity = it },
                            label = { Text(stringResource(R.string.label_surcharge_quantity)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Button(
                        onClick = {
                            val priceVal = tempSurchargePrice.toDoubleOrNull()
                            val qtyVal = tempSurchargeQuantity.toIntOrNull()
                            if (tempSurchargeName.isNotBlank() && priceVal != null && qtyVal != null) {
                                surcharges.add(ManualSurchargeItem(tempSurchargeName, priceVal, qtyVal))
                                tempSurchargeName = ""
                                tempSurchargePrice = ""
                                tempSurchargeQuantity = "1"
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_fill_surcharge_fields), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_add_surcharge))
                    }
                }
            }

            // Supplements List
            if (surcharges.isNotEmpty()) {
                Text(stringResource(R.string.surcharges_added_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                surcharges.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(
                                    "${item.quantity} x ${formatCurrency(item.price)} = ${formatCurrency(item.price * item.quantity)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { surcharges.remove(item) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.empty_surcharges),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // User price input and comparison
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = userTotalPrice,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) userTotalPrice = it },
                    label = { Text(stringResource(R.string.label_user_total_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.3f),
                    singleLine = true
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.label_calculated_price, ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(calculatedPrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { userTotalPrice = String.format(Locale.US, "%.2f", calculatedPrice) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(stringResource(R.string.btn_copy_calculated))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalUserPrice = userTotalPrice.toDoubleOrNull()
                    if (origin.isBlank() || destination.isBlank() || serviceDate.isBlank() || serviceTime.isBlank() ||
                        tariffName.isBlank() || tariffStartPrice.isBlank() || tariffPricePerKm.isBlank() || finalUserPrice == null) {
                        Toast.makeText(context, context.getString(R.string.fields_required_toast), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    coroutineScope.launch {
                        val billingData = application.getBillingData().first()
                        val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                        if (!isValid) {
                            Toast.makeText(context, context.getString(R.string.billing_data_missing_toast), Toast.LENGTH_LONG).show()
                            navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route)
                            return@launch
                        }

                        // Build manual quote items
                        val items = mutableListOf<QuoteItem>()
                        // 1. Start price
                        items.add(QuoteItem(context.getString(R.string.quote_fare_start, PriceUtils.formatTariffNameForDisplay(tariffName)), null, startVal, startVal))
                        // 2. Kilometers
                        items.add(QuoteItem(context.getString(R.string.quote_kilometers), kmVal, perKmVal, kmVal * perKmVal))
                        // 3. Surcharges
                        surcharges.forEach { s ->
                            items.add(QuoteItem(s.name, s.quantity.toDouble(), s.price, s.price * s.quantity))
                        }
                        
                        // Compare and add adjustment if there is a difference
                        val itemsSum = items.sumOf { it.total }
                        val diff = finalUserPrice - itemsSum
                        if (diff > 0.01 || diff < -0.01) {
                            items.add(QuoteItem(context.getString(R.string.quote_adjustment_item), null, diff, diff))
                        }

                        val quoteData = QuoteData(
                            title = context.getString(R.string.manual_quote_header),
                            items = items,
                            totalAmount = finalUserPrice
                        )

                        // Save quoteData so it matches existing generation system if needed, then generate and share
                        application.currentQuote = quoteData

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
                            application.addQuoteToLatest(origin, destination, dateTime, quoteData)
                            Toast.makeText(context, context.getString(R.string.quote_generated_success), Toast.LENGTH_SHORT).show()
                            PdfGenerator.sharePdf(context, pdfUri)
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, context.getString(R.string.pdf_generation_error_simple), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = origin.isNotBlank() && destination.isNotBlank() && serviceDate.isNotBlank() && serviceTime.isNotBlank() &&
                        tariffName.isNotBlank() && tariffStartPrice.isNotBlank() && tariffPricePerKm.isNotBlank() && userTotalPrice.isNotBlank()
            ) {
                Text(stringResource(R.string.create_manual_quote_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
