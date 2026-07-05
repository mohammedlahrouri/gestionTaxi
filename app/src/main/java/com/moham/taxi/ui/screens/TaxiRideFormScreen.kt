package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import com.moham.taxi.ui.viewmodel.ServicePlatformViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.utils.DateUtils
import com.moham.taxi.utils.ImageUtils
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Paleta de colores oficial para la pantalla de Nueva Carrera
 */
object CarreraColors {
    val Background = Color(0xFF1A1A1D)
    val Surface = Color(0xFF26262A)
    val SurfacePressed = Color(0xFF303036)
    val GreenPrimary = Color(0xFF2E9E4F)
    val GreenPressed = Color(0xFF268043)
    val OnBackground = Color(0xFFFAFAFA)
    val TextSecondary = Color(0xFFA8A8AD)
    val BorderSubtle = Color(0x1EFFFFFF) // rgba(255,255,255,0.12)
    val DividerSubtle = Color(0x14FFFFFF) // rgba(255,255,255,0.08)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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

    // Estados de datos
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }
    var existingTip by remember { mutableStateOf<Double?>(null) }
    var existingRealDate by remember { mutableStateOf<Date?>(null) }
    var rideTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var selectedServiceType by remember { mutableStateOf(TaxiRide.SERVICE_TYPE_METER) }
    var selectedServicePlatform by remember { mutableStateOf(context.getString(R.string.platform_direct)) }
    var priceInputMode by remember { mutableStateOf(context.getString(R.string.label_net).uppercase()) }
    var ticketPhotoPath by remember { mutableStateOf<String?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    // Estados de error y carga
    var priceError by remember { mutableStateOf(false) }
    var paymentMethodError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Estados de acordeones expandibles
    var isRouteExpanded by remember { mutableStateOf(false) }
    var isTipExpanded by remember { mutableStateOf(false) }

    // Lanzador de cámara para foto
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                val uri = android.net.Uri.fromFile(file)
                val relativePath = ImageUtils.compressAndSaveTicketPhoto(context, uri)
                if (relativePath != null) {
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

    // Datos del Viewmodel
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

    // Carga de datos de carrera para edición
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
                    existingRealDate = it.realDate
                    
                    // Si hay datos de ruta, expandir el acordeón
                    if (it.origin.isNotEmpty() || it.destination.isNotEmpty()) {
                        isRouteExpanded = true
                    }
                    // Si hay propina, expandir el acordeón de propina
                    if (it.tip != null && it.tip > 0.0) {
                        isTipExpanded = true
                    }
                }
            }
        }
    }

    // Inicialización y actualización de métodos de pago cuando cambia la plataforma
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

    // Guardado de carrera
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
                    val tipValue = tip.toDoubleOrNull()?.takeIf { it > 0.0 }
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
                        ticketPhotoPath = ticketPhotoPath,
                        realDate = existingRealDate ?: Date()
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
        containerColor = CarreraColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CarreraColors.Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 96.dp) // Deja espacio para los botones anclados abajo
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. TOPBAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (rideId > 0) stringResource(R.string.edit_ride_title) else stringResource(R.string.new_ride_title),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarreraColors.OnBackground
                        )
                    )
                    IconButton(
                        onClick = {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = CarreraColors.OnBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Aviso si la fecha es distinta a la actual
                if (!isToday) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFB300).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.warning),
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.warning_date_mismatch, formattedDate),
                            color = CarreraColors.OnBackground,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 2. PRECIO GIGANTE (BasicTextField grande + símbolo €)
                PriceField(
                    value = price,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            price = newValue
                            priceError = false
                        }
                    },
                    placeholder = "0.00"
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Precio de la carrera",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarreraColors.TextSecondary
                    )
                )

                if (priceError) {
                    Text(
                        text = stringResource(R.string.error_valid_price),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Selector de Bruto/Neto si la plataforma tiene comisión
                if (hasPlatformCommission) {
                    Text(
                        text = stringResource(R.string.label_amount_with_commission),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CarreraColors.TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ChoiceChip(
                            label = stringResource(R.string.label_gross),
                            selected = priceInputMode == context.getString(R.string.label_gross).uppercase(),
                            onClick = { priceInputMode = context.getString(R.string.label_gross).uppercase() },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceChip(
                            label = stringResource(R.string.label_net),
                            selected = priceInputMode == context.getString(R.string.label_net).uppercase(),
                            onClick = { priceInputMode = context.getString(R.string.label_net).uppercase() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 3. CAMPO DE HORA
                LabeledTextField(
                    label = "Hora",
                    value = rideTime,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.length <= 5 && newValue.matches(Regex("^\\d{0,2}:?\\d{0,2}$")))) {
                            rideTime = newValue
                        }
                    },
                    placeholder = "HH:mm",
                    icon = Icons.Filled.AccessTime,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. ACORDEÓN DE RUTA
                val routeSummary = remember(origin, destination) {
                    when {
                        origin.isNotEmpty() && destination.isNotEmpty() -> "$origin → $destination"
                        origin.isNotEmpty() -> origin
                        destination.isNotEmpty() -> destination
                        else -> ""
                    }
                }

                ExpandableSection(
                    title = "Ruta",
                    icon = Icons.Default.Place,
                    summary = routeSummary,
                    isExpanded = isRouteExpanded,
                    onToggle = { isRouteExpanded = !isRouteExpanded }
                ) {
                    LabeledTextField(
                        label = "",
                        value = origin,
                        onValueChange = { origin = it },
                        placeholder = "Dirección de origen",
                        icon = Icons.Default.MyLocation,
                        cornerRadius = 12
                    )
                    LabeledTextField(
                        label = "",
                        value = destination,
                        onValueChange = { destination = it },
                        placeholder = "Dirección de destino",
                        icon = Icons.Default.Flag,
                        cornerRadius = 12
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. SECCIÓN PLATAFORMA (Chips limitados a 3 con opción 'Otros')
                Text(
                    text = "Plataforma",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarreraColors.TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                val firstThreePlatforms = servicePlatformOptions.take(3)
                val isSelectedPlatformInFirstThree = firstThreePlatforms.any { it.equals(selectedServicePlatform, ignoreCase = true) }
                val autoExpandPlatforms = remember(selectedServicePlatform, servicePlatformOptions) {
                    !isSelectedPlatformInFirstThree && servicePlatformOptions.size > 3
                }
                var userToggledPlatforms by remember { mutableStateOf(false) }
                val showAllP = autoExpandPlatforms || userToggledPlatforms || servicePlatformOptions.size <= 3

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val platformsToRender = if (showAllP) servicePlatformOptions else firstThreePlatforms
                    platformsToRender.forEach { platformName ->
                        ChoiceChip(
                            label = platformName,
                            selected = selectedServicePlatform.equals(platformName, ignoreCase = true),
                            onClick = { selectedServicePlatform = platformName }
                        )
                    }
                    if (!showAllP && servicePlatformOptions.size > 3) {
                        ChoiceChip(
                            label = "Otros",
                            selected = false,
                            onClick = { userToggledPlatforms = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 6. SECCIÓN MÉTODO DE PAGO (Chips limitados a 3 con opción 'Otros')
                Text(
                    text = "Método de pago",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarreraColors.TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                val firstThreePayments = paymentMethodOptionsDisplay.take(3)
                val isSelectedPaymentInFirstThree = firstThreePayments.any { it.equals(toPaymentMethodDisplayName(selectedPaymentMethod), ignoreCase = true) }
                val autoExpandPayments = remember(selectedPaymentMethod, paymentMethodOptionsDisplay) {
                    !isSelectedPaymentInFirstThree && paymentMethodOptionsDisplay.size > 3
                }
                var userToggledPayments by remember { mutableStateOf(false) }
                val showAllPM = autoExpandPayments || userToggledPayments || paymentMethodOptionsDisplay.size <= 3

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val paymentsToRender = if (showAllPM) paymentMethodOptionsDisplay else firstThreePayments
                    paymentsToRender.forEach { paymentName ->
                        val storedName = paymentMethodDisplayToStored[paymentName] ?: paymentName
                        ChoiceChip(
                            label = paymentName,
                            selected = selectedPaymentMethod.equals(storedName, ignoreCase = true),
                            onClick = {
                                selectedPaymentMethod = storedName
                                paymentMethodError = false
                            }
                        )
                    }
                    if (!showAllPM && paymentMethodOptionsDisplay.size > 3) {
                        ChoiceChip(
                            label = "Otros",
                            selected = false,
                            onClick = { userToggledPayments = true }
                        )
                    }
                }

                if (paymentMethodError) {
                    Text(
                        text = stringResource(R.string.error_select_payment),
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 7. SECCIÓN TIPO DE SERVICIO (Dos chips grandes)
                if (serviceTypeOptions.size > 1) {
                    Text(
                        text = "Servicio",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CarreraColors.TextSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    val selectedServiceTypeLabel = when (selectedServiceType) {
                        TaxiRide.SERVICE_TYPE_FIXED -> stringResource(R.string.option_fixed_price)
                        else -> stringResource(R.string.option_taximeter)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        serviceTypeOptions.forEach { optionLabel ->
                            val optionServiceType = when (optionLabel) {
                                stringResource(R.string.option_fixed_price) -> TaxiRide.SERVICE_TYPE_FIXED
                                else -> TaxiRide.SERVICE_TYPE_METER
                            }
                            ChoiceChip(
                                label = optionLabel,
                                selected = selectedServiceType == optionServiceType,
                                onClick = { selectedServiceType = optionServiceType },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 8. ACORDEÓN DE PROPINA
                val tipSummary = remember(tip) {
                    if (tip.isNotEmpty() && tip.toDoubleOrNull() != null) "+$tip €" else ""
                }

                ExpandableSection(
                    title = "Propina",
                    icon = Icons.Default.Add,
                    summary = tipSummary,
                    isExpanded = isTipExpanded,
                    onToggle = { isTipExpanded = !isTipExpanded }
                ) {
                    LabeledTextField(
                        label = "",
                        value = tip,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                tip = newValue
                            }
                        },
                        placeholder = "0.00",
                        icon = Icons.Default.Add,
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                        cornerRadius = 12
                    )
                }

                // Total de carrera + propina
                val totalText = remember(price, tip) {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val t = tip.toDoubleOrNull() ?: 0.0
                    if (p > 0.0 && t > 0.0) {
                        String.format(Locale.US, "%.2f €", p + t)
                    } else {
                        ""
                    }
                }
                
                if (totalText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarreraColors.Surface, RoundedCornerShape(16.dp))
                            .border(1.dp, CarreraColors.BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total (carrera + propina)",
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = CarreraColors.TextSecondary
                            )
                        )
                        Text(
                            text = totalText,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarreraColors.OnBackground
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 9. BOTÓN GUARDAR (Anclado en la parte inferior)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CarreraColors.Background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Guardar principal
                    Button(
                        onClick = {
                            if (!isSubmitting) {
                                saveTaxiRide()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarreraColors.GreenPrimary,
                            contentColor = CarreraColors.Background
                        ),
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = stringResource(R.string.save),
                                tint = CarreraColors.Background,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSubmitting) {
                                    stringResource(R.string.saving)
                                } else if (rideId > 0) {
                                    stringResource(R.string.update_upper)
                                } else {
                                    stringResource(R.string.save_upper)
                                },
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Botón de Cámara secundario
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CarreraColors.Surface)
                            .border(1.dp, CarreraColors.BorderSubtle, RoundedCornerShape(16.dp))
                            .clickable {
                                val file = ImageUtils.createTempImageFile(context)
                                tempPhotoFile = file
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${application.packageName}.provider",
                                    file
                                )
                                cameraLauncher.launch(uri)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (ticketPhotoPath != null) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                            contentDescription = "Cámara",
                            tint = if (ticketPhotoPath != null) CarreraColors.GreenPrimary else CarreraColors.OnBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Componente para secciones expandibles y animadas (Ruta y Propina)
 */
@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    summary: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CarreraColors.Surface)
                .border(1.dp, CarreraColors.BorderSubtle, RoundedCornerShape(16.dp))
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CarreraColors.OnBackground,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarreraColors.OnBackground
                ),
                modifier = Modifier.weight(1f)
            )
            if (!isExpanded && summary.isNotEmpty()) {
                Text(
                    text = summary,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CarreraColors.TextSecondary
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Contraer" else "Expandir",
                tint = CarreraColors.TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = rotationState)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Chip de selección única personalizado con la paleta de colores indicada
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) CarreraColors.GreenPrimary else CarreraColors.Surface
    val contentColor = if (selected) CarreraColors.Background else CarreraColors.OnBackground
    val borderModifier = if (selected) {
        Modifier
    } else {
        Modifier.border(1.dp, CarreraColors.BorderSubtle, RoundedCornerShape(16.dp))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        )
    }
}

/**
 * Campo de texto personalizado con borde, fondo y diseño adaptativo
 */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    cornerRadius: Int = 16
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CarreraColors.TextSecondary
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = CarreraColors.OnBackground
            ),
            cursorBrush = SolidColor(CarreraColors.GreenPrimary),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarreraColors.Surface, RoundedCornerShape(cornerRadius.dp))
                        .border(
                            width = 1.dp,
                            color = if (isError) Color.Red else CarreraColors.BorderSubtle,
                            shape = RoundedCornerShape(cornerRadius.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = CarreraColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CarreraColors.TextSecondary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Campo gigante de precio protagonista
 */
@Composable
fun PriceField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "0.00",
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        textStyle = TextStyle(
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = CarreraColors.OnBackground,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(CarreraColors.GreenPrimary),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarreraColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "€",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarreraColors.OnBackground
                    ),
                    modifier = Modifier.alignBy(FirstBaseline)
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
