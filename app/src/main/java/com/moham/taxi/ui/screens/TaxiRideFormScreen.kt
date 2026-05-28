package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.IconButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.moham.taxi.utils.ImageUtils
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.ui.components.TaxiDropdown
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.BlueAccent
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.DarkCard
import com.moham.taxi.ui.theme.GreenAccent
import com.moham.taxi.ui.theme.Warning
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import com.moham.taxi.ui.viewmodel.ServicePlatformViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiRideFormScreen(
    navController: NavHostController,
    rideId: Long = -1L,
    selectedDate: Long = -1L
) {
    BackHandler {
        navController.navigate(AppScreens.Home.route) {
            popUpTo(AppScreens.Home.route) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val originDestinationEnabled by application.isRideOriginDestinationEnabled().collectAsState(initial = true)
    val tipsEnabled by application.isTipsEnabled().collectAsState(initial = false)
    val ticketPhotosEnabled by application.isTicketPhotosEnabled().collectAsState(initial = false)

    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )

    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.PaymentMethodViewModelFactory(
            repository = application.paymentMethodRepository
        )
    )

    val servicePlatformViewModel: ServicePlatformViewModel = viewModel(
        factory = ServicePlatformViewModel.ServicePlatformViewModelFactory(
            repository = application.servicePlatformRepository
        )
    )

    val useDate = remember(selectedDate) {
        if (selectedDate > 0) Date(selectedDate) else Date()
    }

    val isToday = remember(useDate) {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { time = useDate }
        today.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
            today.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(useDate) { dateFormat.format(useDate) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }
    var existingTip by remember { mutableStateOf<Double?>(null) }
    var rideTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var selectedServiceType by remember { mutableStateOf(TaxiRide.SERVICE_TYPE_METER) }
    var selectedServicePlatform by remember { mutableStateOf(context.getString(R.string.platform_direct)) }
    var priceInputMode by remember { mutableStateOf(context.getString(R.string.label_net).uppercase()) }
    var ticketPhotoPath by remember { mutableStateOf<String?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    var priceError by remember { mutableStateOf(false) }
    var paymentMethodError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                val uri = android.net.Uri.fromFile(file)
                val relativePath = ImageUtils.compressAndSaveTicketPhoto(context, uri)
                if (relativePath != null) {
                    // Delete old photo if replacing
                    ImageUtils.deleteTicketPhoto(context, ticketPhotoPath)
                    ticketPhotoPath = relativePath
                }
                file.delete()
            }
        } else {
            tempPhotoFile?.delete()
        }
        tempPhotoFile = null
    }

    val servicePlatforms by servicePlatformViewModel.allServicePlatforms.collectAsState(initial = emptyList())
    val directLabel = context.getString(R.string.platform_direct)
    val directStoredName = "Directo"
    fun isDirectPlatformName(name: String): Boolean {
        return name.equals(directLabel, ignoreCase = true) ||
            name.equals(directStoredName, ignoreCase = true) ||
            name.equals("Direct", ignoreCase = true)
    }
    fun toPlatformDisplayName(storedName: String): String {
        return if (isDirectPlatformName(storedName)) directLabel else storedName
    }
    fun toPlatformStoredName(displayName: String): String {
        return if (displayName.equals(directLabel, ignoreCase = true)) directStoredName else displayName
    }

    val servicePlatformOptions = run {
        val fromDb = servicePlatforms.map { toPlatformDisplayName(it.name) }
        val base = fromDb.distinctBy { it.lowercase() }
        if (base.any { it.equals(directLabel, ignoreCase = true) }) base else listOf(directLabel) + base
    }

    val selectedPlatformStoredName = remember(selectedServicePlatform, directLabel) { toPlatformStoredName(selectedServicePlatform) }
    val selectedPlatform = servicePlatforms.firstOrNull { it.name.equals(selectedPlatformStoredName, ignoreCase = true) }
    val selectedPlatformId = selectedPlatform?.id
    val platformServiceMode = selectedPlatform?.serviceMode ?: com.moham.taxi.data.model.ServiceMode.BOTH
    val serviceTypeOptions = when (platformServiceMode) {
        com.moham.taxi.data.model.ServiceMode.BOTH -> listOf(stringResource(R.string.option_taximeter), stringResource(R.string.option_fixed_price))
        com.moham.taxi.data.model.ServiceMode.METER_ONLY -> listOf(stringResource(R.string.option_taximeter))
        com.moham.taxi.data.model.ServiceMode.FIXED_ONLY -> listOf(stringResource(R.string.option_fixed_price))
    }

    val cashLabel = context.getString(R.string.payment_cash)
    val cardLabel = context.getString(R.string.payment_card)
    val appLabel = context.getString(R.string.payment_via_app)
    fun canonicalizePaymentMethodStoredName(value: String): String {
        val v = value.trim()
        val lower = v.lowercase()
        return when {
            lower == "efectivo" || lower == "cash" -> "Efectivo"
            lower == "tarjeta" || lower == "card" -> "Tarjeta"
            lower.replace(" ", "") == "viaapp" || lower == "via app" -> "Via App"
            else -> v
        }
    }
    fun toPaymentMethodDisplayName(storedName: String): String {
        val canonical = canonicalizePaymentMethodStoredName(storedName)
        return when {
            canonical.equals("Efectivo", ignoreCase = true) -> cashLabel
            canonical.equals("Tarjeta", ignoreCase = true) -> cardLabel
            canonical.equals("Via App", ignoreCase = true) -> appLabel
            else -> storedName
        }
    }

    val platformPaymentMethods by remember(selectedPlatformId) {
        if (selectedPlatformId != null) {
            paymentMethodViewModel.paymentMethodsForPlatform(selectedPlatformId)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val paymentMethodOptionsStored = run {
        if (platformPaymentMethods.isNotEmpty()) {
            platformPaymentMethods.map { canonicalizePaymentMethodStoredName(it.name) }
        } else {
            emptyList()
        }
    }.distinctBy { it.lowercase() }
    val paymentMethodOptionsDisplay = paymentMethodOptionsStored
        .map { toPaymentMethodDisplayName(it) }
        .distinctBy { it.lowercase() }
    val paymentMethodDisplayToStored = remember(paymentMethodOptionsStored, paymentMethodOptionsDisplay) {
        paymentMethodOptionsDisplay.zip(paymentMethodOptionsStored).toMap()
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedPlatformCommission = selectedPlatform?.commissionPercentage
    val selectedPlatformVat = selectedPlatform?.commissionVat
    val hasPlatformCommission = selectedPlatformCommission != null
    val totalText = run {
        val priceValue = price.toDoubleOrNull()
        val tipValue = tip.toDoubleOrNull()
        if (priceValue == null && tipValue == null) "" else String.format(java.util.Locale.US, "%.2f", (priceValue ?: 0.0) + (tipValue ?: 0.0))
    }

    LaunchedEffect(rideId) {
        if (rideId > 0) {
            scope.launch {
                val ride = taxiRideViewModel.getTaxiRideById(rideId)
                ride?.let {
                    origin = it.origin
                    destination = it.destination
                    price = it.price.toString()
                    existingTip = it.tip
                    tip = it.tip?.toString()?.takeIf { v -> v != "0.0" && v != "0" } ?: ""
                    selectedPaymentMethod = canonicalizePaymentMethodStoredName(it.paymentMethod)
                    selectedServiceType = it.serviceType ?: TaxiRide.SERVICE_TYPE_METER
                    val storedPlatform = it.servicePlatform?.ifBlank { directStoredName } ?: directStoredName
                    selectedServicePlatform = toPlatformDisplayName(storedPlatform)
                    rideTime = it.rideTime
                    ticketPhotoPath = it.ticketPhotoPath
                }
            }
        }
    }

    LaunchedEffect(paymentMethodOptionsStored, servicePlatformOptions, selectedServicePlatform, rideId) {
        if (rideId > 0) return@LaunchedEffect

        if (selectedServicePlatform.isBlank()) {
            selectedServicePlatform = if (servicePlatformOptions.isEmpty()) context.getString(R.string.platform_direct) else servicePlatformOptions.first()
        }

        if (paymentMethodOptionsStored.isEmpty()) {
            selectedPaymentMethod = ""
            return@LaunchedEffect
        }

        val currentIsValid = paymentMethodOptionsStored.any { it.equals(selectedPaymentMethod, ignoreCase = true) }
        if (selectedPaymentMethod.isBlank() || !currentIsValid || paymentMethodOptionsStored.size == 1) {
            selectedPaymentMethod = paymentMethodOptionsStored.first()
        }
    }

    LaunchedEffect(platformServiceMode, selectedServicePlatform) {
        when (platformServiceMode) {
            com.moham.taxi.data.model.ServiceMode.METER_ONLY -> selectedServiceType = TaxiRide.SERVICE_TYPE_METER
            com.moham.taxi.data.model.ServiceMode.FIXED_ONLY -> selectedServiceType = TaxiRide.SERVICE_TYPE_FIXED
            else -> {}
        }
    }

    LaunchedEffect(selectedServicePlatform, servicePlatforms) {
        if (!hasPlatformCommission) {
            priceInputMode = context.getString(R.string.label_gross).uppercase()
        }
    }

    fun saveTaxiRide() {
        var isValid = true

        if (price.isBlank() || price.toDoubleOrNull() == null || price.toDouble() <= 0) {
            priceError = true
            isValid = false
        }

        if (selectedPaymentMethod.isBlank()) {
            paymentMethodError = true
            isValid = false
        }

        if (isValid && !isSubmitting) {
            isSubmitting = true

            scope.launch {
                try {
                    val inputPrice = price.toDouble()
                    val tipValue = if (tipsEnabled) tip.toDoubleOrNull()?.takeIf { it > 0.0 } else existingTip
                    val finalCommission = selectedPlatformCommission
                    val finalVat = selectedPlatformVat
                    val commissionRate = (finalCommission ?: 0.0) / 100.0
                    val vatRate = (finalVat ?: 0.0) / 100.0
                    val deductionFactor = commissionRate * (1 + vatRate)
                    val alternativeMath = selectedPlatform?.useAlternativeMath == true
                    val finalPrice = if (hasPlatformCommission && priceInputMode == context.getString(R.string.label_net).uppercase()) {
                        if (alternativeMath) {
                            inputPrice * (1 + deductionFactor)
                        } else {
                            inputPrice / (1 - deductionFactor)
                        }
                    } else {
                        inputPrice
                    }
                    val finalNetPrice = if (hasPlatformCommission) {
                        if (priceInputMode == context.getString(R.string.label_net).uppercase()) {
                            inputPrice
                        } else {
                            inputPrice * (1 - deductionFactor)
                        }
                    } else {
                        inputPrice
                    }
                    val finalDate = DateUtils.assignProperDate(useDate)
                    val finalRideTime = if (rideTime.matches(Regex("^\\d{2}:\\d{2}$"))) {
                        rideTime
                    } else {
                        timeFormat.format(Date())
                    }

                    val taxiRide = TaxiRide(
                        id = if (rideId > 0) rideId else 0,
                        origin = origin,
                        destination = destination,
                        price = finalPrice,
                        tip = tipValue,
                        netPrice = finalNetPrice,
                        commissionPercentAtTime = if (hasPlatformCommission) finalCommission else null,
                        commissionVatAtTime = if (hasPlatformCommission) finalVat else null,
                        paymentMethod = selectedPaymentMethod,
                        date = finalDate,
                        rideTime = finalRideTime,
                        serviceType = selectedServiceType,
                        servicePlatform = toPlatformStoredName(selectedServicePlatform).ifBlank { directStoredName },
                        ticketPhotoPath = ticketPhotoPath
                    )

                    if (rideId > 0) {
                        taxiRideViewModel.update(taxiRide)
                    } else {
                        taxiRideViewModel.insert(taxiRide)
                    }

                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_data", true)
                    navController.popBackStack()

                    val message = if (rideId > 0) context.getString(R.string.ride_updated_success) else context.getString(R.string.ride_saved_success)
                    snackbarHostState.showSnackbar(message)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("${context.getString(R.string.error)}: ${e.message}")
                    isSubmitting = false
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState())
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (rideId > 0) stringResource(R.string.edit_ride_title) else stringResource(R.string.new_ride_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (!isToday) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Warning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.warning),
                        tint = Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.warning_date_mismatch, formattedDate),
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InputField(
                            label = stringResource(R.string.time),
                            value = rideTime,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || (newValue.length <= 5 && newValue.matches(Regex("^\\d{0,2}:?\\d{0,2}$")))) {
                                    rideTime = newValue
                                }
                            },
                            icon = Icons.Filled.AccessTime,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f)
                        )
                        InputField(
                            label = stringResource(R.string.price_symbol),
                            value = price,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    price = newValue
                                    priceError = false
                                }
                            },
                            icon = Icons.Filled.AttachMoney,
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                            modifier = Modifier.weight(1f),
                            placeholder = "0.00",
                            isError = priceError
                        )
                    }

                    if (priceError) {
                        Text(
                            text = stringResource(R.string.error_valid_price),
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (tipsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InputField(
                                label = stringResource(R.string.tip),
                                value = tip,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        tip = newValue
                                    }
                                },
                                icon = Icons.Filled.AttachMoney,
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                modifier = Modifier.weight(1f),
                                placeholder = "0.00"
                            )
                            InputField(
                                label = stringResource(R.string.total),
                                value = totalText,
                                onValueChange = {},
                                icon = Icons.Filled.AttachMoney,
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                modifier = Modifier.weight(1f),
                                placeholder = "0.00",
                                enabled = false,
                                readOnly = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (originDestinationEnabled || rideId > 0) {
                        InputField(
                            label = stringResource(R.string.label_origin),
                            value = origin,
                            onValueChange = { origin = it },
                            icon = Icons.Filled.LocationOn,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            placeholder = stringResource(R.string.placeholder_origin)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        InputField(
                            label = stringResource(R.string.label_destination),
                            value = destination,
                            onValueChange = { destination = it },
                            icon = Icons.Filled.Flag,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            placeholder = stringResource(R.string.placeholder_destination)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    SelectField(
                        label = stringResource(R.string.label_platform),
                        options = servicePlatformOptions,
                        selectedOption = selectedServicePlatform,
                        onOptionSelected = { selectedServicePlatform = it },
                        icon = Icons.Filled.Smartphone,
                        modifier = Modifier.fillMaxWidth(),
                        emptyMessage = stringResource(R.string.platform_empty_msg)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (hasPlatformCommission) {
                        Text(
                            text = stringResource(R.string.label_amount_with_commission),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioOption(
                                selected = priceInputMode == context.getString(R.string.label_gross).uppercase(),
                                onClick = { priceInputMode = context.getString(R.string.label_gross).uppercase() },
                                label = stringResource(R.string.label_gross),
                                modifier = Modifier.weight(1f)
                            )
                            RadioOption(
                                selected = priceInputMode == context.getString(R.string.label_net).uppercase(),
                                onClick = { priceInputMode = context.getString(R.string.label_net).uppercase() },
                                label = stringResource(R.string.label_net),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (serviceTypeOptions.size > 1) {
                        val selectedServiceTypeLabel = when (selectedServiceType) {
                            TaxiRide.SERVICE_TYPE_FIXED -> stringResource(R.string.option_fixed_price)
                            else -> stringResource(R.string.option_taximeter)
                        }
                        SelectField(
                            label = stringResource(R.string.label_service_type),
                            options = serviceTypeOptions,
                            selectedOption = selectedServiceTypeLabel,
                            onOptionSelected = {
                                selectedServiceType = when (it) {
                                    context.getString(R.string.option_fixed_price) -> TaxiRide.SERVICE_TYPE_FIXED
                                    else -> TaxiRide.SERVICE_TYPE_METER
                                }
                            },
                            icon = Icons.Filled.DirectionsCar,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    SelectField(
                        label = stringResource(R.string.label_payment_method),
                        options = paymentMethodOptionsDisplay,
                        selectedOption = toPaymentMethodDisplayName(selectedPaymentMethod),
                        onOptionSelected = {
                            selectedPaymentMethod = paymentMethodDisplayToStored[it] ?: it
                            paymentMethodError = false
                        },
                        isError = paymentMethodError,
                        errorMessage = stringResource(R.string.error_select_payment),
                        enabled = paymentMethodOptionsDisplay.size > 1,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.CreditCard
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (!isSubmitting) {
                                    saveTaxiRide()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenAccent,
                                disabledContainerColor = GreenAccent.copy(alpha = 0.5f)
                            ),
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = stringResource(R.string.save),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSubmitting) stringResource(R.string.saving) else if (rideId > 0) stringResource(R.string.update_upper) else stringResource(R.string.save_upper),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (ticketPhotosEnabled) {
                            Button(
                                onClick = {
                                    val file = ImageUtils.createTempImageFile(context)
                                    tempPhotoFile = file
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${application.packageName}.provider",
                                        file
                                    )
                                    cameraLauncher.launch(uri)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (ticketPhotoPath != null) BlueAccent else DarkBackground.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (ticketPhotoPath != null) Icons.Filled.CheckCircle else Icons.Filled.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Button(
                            onClick = { 
                                if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                    navController.popBackStack() 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.6f)),
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            placeholder = { if (placeholder != null) Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            },
            isError = isError,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = BlueAccent.copy(alpha = 0.6f),
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = BlueAccent,
                cursorColor = BlueAccent,
                errorIndicatorColor = Color.Red,
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            )
        )
    }
}

@Composable
private fun SelectField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyMessage: String? = null,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    TaxiDropdown(
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = onOptionSelected,
        label = label,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        },
        enabled = enabled,
        emptyMessage = emptyMessage,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier
    )
}

@Composable
private fun RadioOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) BlueAccent else Color.White.copy(alpha = 0.3f)
    val backgroundColor = if (selected) BlueAccent.copy(alpha = 0.15f) else DarkBackground.copy(alpha = 0.5f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(2.dp, borderColor, CircleShape)
                .padding(2.dp)
                .background(if (selected) borderColor else Color.Transparent, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
