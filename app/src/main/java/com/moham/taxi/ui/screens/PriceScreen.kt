package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.NumberFormat
import java.util.*
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.screens.BottomNavBar
import androidx.compose.ui.platform.LocalContext
import com.moham.taxi.utils.PriceUtils
import com.moham.taxi.GestionTaxiApplication
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.data.model.Surcharge
import com.moham.taxi.data.model.QuoteData
import com.moham.taxi.data.model.QuoteItem
import com.moham.taxi.ui.viewmodel.TariffViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val tariffViewModel: TariffViewModel = viewModel(
        factory = TariffViewModel.Factory(application)
    )
    val currentCity by tariffViewModel.currentCity.collectAsState()

    if (currentCity == "Madrid") {
        MadridPriceScreen(navController, currentCity)
    } else {
        NonMadridPriceScreen(navController, currentCity, tariffViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MadridPriceScreen(navController: NavController, currentCity: String) {

    var selectedTariff by remember { mutableStateOf<MadridTariff?>(null) }
    var isWorkday by remember { mutableStateOf(true) }
    var kilometers by remember { mutableStateOf("") }
    var selectedTraffic by remember { mutableStateOf<TrafficLevel?>(null) }
    var price by remember { mutableStateOf<Double?>(null) }
    var showSummary by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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

    val tariffs = listOf(
        MadridTariff.T1, MadridTariff.T2, MadridTariff.T3, MadridTariff.T4, MadridTariff.T7
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_calculate_price)) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSummary) {
                // Summary view - bigger!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tarifa seleccionada",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedTariff?.displayName ?: "",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        if (selectedTraffic != null && selectedTariff != MadridTariff.T4) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Nivel de tráfico",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(selectedTraffic!!.labelResId),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_estimated_price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            AutoSizeText(
                                text = formatCurrency(price!!),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                maxFontSize = 56.sp,
                                minFontSize = 36.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val app = navController.context.applicationContext as GestionTaxiApplication
                            val billingData = app.getBillingData().first()
                            val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                            if (isValid) {
                                navController.navigate(com.moham.taxi.ui.navigation.AppScreens.QuoteForm.route)
                            } else {
                                Toast.makeText(navController.context, "Por favor, completa tus datos de facturación primero", Toast.LENGTH_LONG).show()
                                navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Crear presupuesto", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { showSummary = false },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Volver a ajustes", fontSize = 18.sp)
                }
                
            } else {
                // Información sobre disponibilidad para Madrid
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = stringResource(R.string.card_madrid_rates_title),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.card_madrid_rates_title, currentCity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.configurar_otras_ciudades_msg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Selector de tarifa
                Text(stringResource(R.string.label_select_tariff), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tariffs.forEach { tariff ->
                        val tariffIcon = when (tariff) {
                            MadridTariff.T1, MadridTariff.T2 -> Icons.Filled.DirectionsCar
                            MadridTariff.T3, MadridTariff.T4 -> Icons.Filled.Flight
                            MadridTariff.T7 -> Icons.Filled.DirectionsBus
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTariff = tariff },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTariff == tariff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(tariffIcon, contentDescription = null)
                                Text(stringResource(R.string.tariff_prefix, tariff.displayName), fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                // Selector laborable/no laborable si aplica
                if (selectedTariff == MadridTariff.T3 || selectedTariff == MadridTariff.T7) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { isWorkday = true },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isWorkday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Brightness7, contentDescription = stringResource(R.string.cd_workday))
                                Text(stringResource(R.string.label_workday), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable { isWorkday = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isWorkday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NightsStay, contentDescription = stringResource(R.string.cd_holiday))
                                Text(stringResource(R.string.label_holiday), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
                // Campo de kilómetros
                OutlinedTextField(
                    value = kilometers,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) kilometers = it
                    },
                    label = { Text(stringResource(R.string.label_kilometers)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Selector de tráfico
                val trafficEnabled = selectedTariff != MadridTariff.T4 && selectedTariff != null
                Text(stringResource(R.string.label_traffic_level), fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrafficLevel.values().forEach { level ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clickable(enabled = trafficEnabled) { selectedTraffic = level },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTraffic == level && trafficEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(level.icon, contentDescription = stringResource(level.labelResId))
                                Text(stringResource(level.labelResId), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                // Botón calcular
                Button(
                    onClick = {
                        val calculatedPrice = calculatePrice(
                            tariff = selectedTariff,
                            isWorkday = isWorkday,
                            km = kilometers.toDoubleOrNull() ?: 0.0,
                            traffic = selectedTraffic
                        )
                        if (calculatedPrice != null) {
                            price = PriceUtils.roundToNearestFiveCents(calculatedPrice)
                            
                            // Generar el QuoteData para Madrid
                            val items = mutableListOf<QuoteItem>()
                            val kmVal = kilometers.toDoubleOrNull() ?: 0.0
                            
                            when (selectedTariff) {
                                MadridTariff.T1 -> {
                                    items.add(QuoteItem("Bajada de bandera (T1)", null, 2.55, 2.55))
                                    items.add(QuoteItem("Kilómetros", kmVal, 1.40, kmVal * 1.40))
                                }
                                MadridTariff.T2 -> {
                                    items.add(QuoteItem("Bajada de bandera (T2)", null, 3.20, 3.20))
                                    items.add(QuoteItem("Kilómetros", kmVal, 1.60, kmVal * 1.60))
                                }
                                MadridTariff.T3 -> {
                                    items.add(QuoteItem("Viaje mínimo (T3)", null, 22.0, 22.0))
                                    val extraKm = (kmVal - 9.0).coerceAtLeast(0.0)
                                    if (extraKm > 0) {
                                        val p = if (isWorkday) 1.40 else 1.60
                                        items.add(QuoteItem("Kilómetros extra", extraKm, p, extraKm * p))
                                    }
                                }
                                MadridTariff.T4 -> {
                                    items.add(QuoteItem("Tarifa plana Aeropuerto (T4)", null, 33.0, 33.0))
                                }
                                MadridTariff.T7 -> {
                                    items.add(QuoteItem("Viaje mínimo (T7)", null, 8.0, 8.0))
                                    val extraKm = (kmVal - 1.45).coerceAtLeast(0.0)
                                    if (extraKm > 0) {
                                        val p = if (isWorkday) 1.40 else 1.60
                                        items.add(QuoteItem("Kilómetros extra", extraKm, p, extraKm * p))
                                    }
                                }
                                else -> {}
                            }
                            
                            if (selectedTraffic != null && selectedTraffic != TrafficLevel.NONE) {
                                val surch = selectedTraffic!!.surcharge
                                if (selectedTariff == MadridTariff.T3 || selectedTariff == MadridTariff.T7) {
                                    val extraKm = if (selectedTariff == MadridTariff.T3) (kmVal - 9.0).coerceAtLeast(0.0) else (kmVal - 1.45).coerceAtLeast(0.0)
                                    if (extraKm > 0.0) {
                                        items.add(QuoteItem("Suplemento tráfico", null, surch, surch))
                                    } else if (selectedTraffic == TrafficLevel.MODERATE || selectedTraffic == TrafficLevel.HEAVY) {
                                        items.add(QuoteItem("Suplemento tráfico (Base)", null, surch, surch))
                                    }
                                } else if (selectedTariff != MadridTariff.T4) {
                                    items.add(QuoteItem("Suplemento tráfico", null, surch, surch))
                                }
                            }

                            val itemsSum = items.sumOf { it.total }
                            val diff = price!! - itemsSum
                            if (diff > 0.01) {
                                items.add(QuoteItem("Estimación de tiempo/tráfico", null, diff, diff))
                            } else if (diff < -0.01) {
                                items.add(QuoteItem("Ajuste de redondeo", null, diff, diff))
                            }

                            val app = navController.context.applicationContext as GestionTaxiApplication
                            app.currentQuote = QuoteData(
                                title = "Presupuesto de viaje en Taxi - $currentCity",
                                items = items,
                                totalAmount = price!!
                            )
                            showSummary = true
                        }
                    },
                    enabled = selectedTariff != null && kilometers.isNotEmpty() && (selectedTariff == MadridTariff.T4 || selectedTraffic != null)
                ) {
                    Text(stringResource(R.string.action_calculate_price))
                }
                // Resultado
                if (price != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(R.string.label_estimated_price),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        AutoSizeText(
                            text = formatCurrency(price!!),
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            maxFontSize = 36.sp,
                            minFontSize = 22.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val app = navController.context.applicationContext as GestionTaxiApplication
                                val billingData = app.getBillingData().first()
                                val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                                if (isValid) {
                                    navController.navigate(com.moham.taxi.ui.navigation.AppScreens.QuoteForm.route)
                                } else {
                                    Toast.makeText(navController.context, "Por favor, completa tus datos de facturación primero", Toast.LENGTH_LONG).show()
                                    navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Crear presupuesto")
                    }
                }
            }
        }
    }
}

// Tarifas y lógica
sealed class MadridTariff(val displayName: String) {
    object T1 : MadridTariff("1")
    object T2 : MadridTariff("2")
    object T3 : MadridTariff("3")
    object T4 : MadridTariff("4")
    object T7 : MadridTariff("7")
}

enum class TrafficLevel(val labelResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val surcharge: Double) {
    NONE(R.string.traffic_none, Icons.Default.Traffic, 1.0),
    LIGHT(R.string.traffic_light, Icons.Default.Traffic, 2.5),
    MODERATE(R.string.traffic_moderate, Icons.Default.Traffic, 5.0),
    HEAVY(R.string.traffic_heavy, Icons.Default.Traffic, 10.0)
}

fun calculatePrice(
    tariff: MadridTariff?,
    isWorkday: Boolean,
    km: Double,
    traffic: TrafficLevel?
): Double? {
    if (tariff == null || km <= 0) return null
    return when (tariff) {
        MadridTariff.T1 -> 2.55 + km * 1.40 + (traffic?.surcharge ?: 0.0)
        MadridTariff.T2 -> 3.20 + km * 1.60 + (traffic?.surcharge ?: 0.0)
        MadridTariff.T3 -> {
            val included = 9.0
            val base = 22.0
            val extraKm = (km - included).coerceAtLeast(0.0)
            val perKm = if (isWorkday) 1.40 else 1.60
            val basePrice = base + extraKm * perKm
            val addTraffic = if (extraKm > 0.0) {
                (traffic?.surcharge ?: 0.0)
            } else {
                when (traffic) {
                    TrafficLevel.MODERATE, TrafficLevel.HEAVY -> traffic.surcharge
                    else -> 0.0
                }
            }
            basePrice + addTraffic
        }
        MadridTariff.T4 -> 33.0
        MadridTariff.T7 -> {
            val included = 1.45
            val base = 8.0
            val extraKm = (km - included).coerceAtLeast(0.0)
            val perKm = if (isWorkday) 1.40 else 1.60
            val basePrice = base + extraKm * perKm
            val addTraffic = if (extraKm > 0.0) {
                (traffic?.surcharge ?: 0.0)
            } else {
                when (traffic) {
                    TrafficLevel.MODERATE, TrafficLevel.HEAVY -> traffic.surcharge
                    else -> 0.0
                }
            }
            basePrice + addTraffic
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NonMadridPriceScreen(navController: NavController, currentCity: String, tariffViewModel: TariffViewModel) {
    val allTariffs by tariffViewModel.allTariffs.collectAsState()
    val allSurcharges by tariffViewModel.allSurcharges.collectAsState()

    var selectedTariff by remember { mutableStateOf<Tariff?>(null) }
    var kilometers by remember { mutableStateOf("") }
    
    // Estado para los suplementos
    val selectedSurcharges = remember { mutableStateMapOf<Long, Boolean>() }
    val surchargeQuantities = remember { mutableStateMapOf<Long, Int>() }
    var price by remember { mutableStateOf<Double?>(null) }
    var showSummary by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        navController.navigate(AppScreens.Other.route) {
            popUpTo(AppScreens.Other.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.title_calculate_price)) })
        },
        bottomBar = {
            BottomNavBar(
                selectedItem = 3,
                onItemSelected = { index ->
                    when (index) {
                        0 -> navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) { inclusive = false }
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
            if (showSummary) {
                // Summary view - bigger!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tarifa seleccionada",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedTariff?.name ?: "",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_estimated_price),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            AutoSizeText(
                                text = formatCurrency(price!!),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                maxFontSize = 56.sp,
                                minFontSize = 36.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val app = navController.context.applicationContext as GestionTaxiApplication
                            val billingData = app.getBillingData().first()
                            val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                            if (isValid) {
                                navController.navigate(com.moham.taxi.ui.navigation.AppScreens.QuoteForm.route)
                            } else {
                                Toast.makeText(navController.context, "Por favor, completa tus datos de facturación primero", Toast.LENGTH_LONG).show()
                                navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Crear presupuesto", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { showSummary = false },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Volver a ajustes", fontSize = 18.sp)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text(text = "Tarifas de $currentCity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "Añade las tarifas y suplementos desde Ajustes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Text("Selecciona la tarifa", fontWeight = FontWeight.Bold)
                if (allTariffs.isEmpty()) {
                    Text("No hay tarifas configuradas", color = MaterialTheme.colorScheme.error)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allTariffs.forEach { tariff ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { selectedTariff = tariff },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTariff == tariff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(tariff.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (selectedTariff != null && !selectedTariff!!.isFixed) {
                    OutlinedTextField(
                        value = kilometers,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) kilometers = it },
                        label = { Text(stringResource(R.string.label_kilometers)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (allSurcharges.isNotEmpty()) {
                    Text("Suplementos y Bultos", fontWeight = FontWeight.Bold)
                    allSurcharges.forEach { surcharge ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (surcharge.isPerItem) {
                                val quantity = surchargeQuantities[surcharge.id] ?: 0
                                Text(surcharge.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (quantity > 0) surchargeQuantities[surcharge.id] = quantity - 1 }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$quantity", modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { surchargeQuantities[surcharge.id] = quantity + 1 }) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val checked = selectedSurcharges[surcharge.id] ?: false
                                Text(surcharge.name, modifier = Modifier.weight(1f))
                                Switch(checked = checked, onCheckedChange = { selectedSurcharges[surcharge.id] = it })
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val t = selectedTariff
                        if (t != null) {
                            var total = 0.0
                            if (t.isFixed) {
                                total = t.fixedPrice ?: 0.0
                            } else {
                                val km = kilometers.toDoubleOrNull() ?: 0.0
                                val base = t.baseFare ?: 0.0
                                val perKm = t.pricePerKm ?: 0.0
                                val sur = t.surcharge ?: 0.0
                                total = base + (km * perKm) + sur
                            }
                            
                            allSurcharges.forEach { s ->
                                if (s.isPerItem) {
                                    val q = surchargeQuantities[s.id] ?: 0
                                    total += s.price * q
                                } else {
                                    if (selectedSurcharges[s.id] == true) {
                                        total += s.price
                                    }
                                }
                            }
                            price = PriceUtils.roundToNearestFiveCents(total)

                            val items = mutableListOf<QuoteItem>()
                            if (t.isFixed) {
                                items.add(QuoteItem(t.name, null, t.fixedPrice ?: 0.0, t.fixedPrice ?: 0.0))
                            } else {
                                val km = kilometers.toDoubleOrNull() ?: 0.0
                                items.add(QuoteItem("Bajada de bandera (${t.name})", null, t.baseFare ?: 0.0, t.baseFare ?: 0.0))
                                items.add(QuoteItem("Kilómetros", km, t.pricePerKm ?: 0.0, km * (t.pricePerKm ?: 0.0)))
                                if ((t.surcharge ?: 0.0) > 0.0) {
                                    items.add(QuoteItem("Suplemento tarifa", null, t.surcharge!!, t.surcharge!!))
                                }
                            }
                            
                            allSurcharges.forEach { s ->
                                if (s.isPerItem) {
                                    val q = surchargeQuantities[s.id] ?: 0
                                    if (q > 0) {
                                        items.add(QuoteItem(s.name, q.toDouble(), s.price, s.price * q))
                                    }
                                } else {
                                    if (selectedSurcharges[s.id] == true) {
                                        items.add(QuoteItem(s.name, null, s.price, s.price))
                                    }
                                }
                            }

                            val itemsSum = items.sumOf { it.total }
                            val diff = price!! - itemsSum
                            if (diff > 0.01) {
                                items.add(QuoteItem("Estimación de tiempo/tráfico", null, diff, diff))
                            } else if (diff < -0.01) {
                                items.add(QuoteItem("Ajuste de redondeo", null, diff, diff))
                            }

                            val app = navController.context.applicationContext as GestionTaxiApplication
                            app.currentQuote = QuoteData(
                                title = "Presupuesto de viaje en Taxi - $currentCity",
                                items = items,
                                totalAmount = price!!
                            )
                            showSummary = true
                        }
                    },
                    enabled = selectedTariff != null && (selectedTariff!!.isFixed || kilometers.isNotEmpty()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_calculate_price))
                }

                if (price != null) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text(text = stringResource(R.string.label_estimated_price), modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = TextAlign.Center, color = Color.White, fontWeight = FontWeight.Bold)
                        AutoSizeText(
                            text = formatCurrency(price!!),
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            maxFontSize = 36.sp,
                            minFontSize = 22.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val app = navController.context.applicationContext as GestionTaxiApplication
                                val billingData = app.getBillingData().first()
                                val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                                if (isValid) {
                                    navController.navigate(com.moham.taxi.ui.navigation.AppScreens.QuoteForm.route)
                                } else {
                                    Toast.makeText(navController.context, "Por favor, completa tus datos de facturación primero", Toast.LENGTH_LONG).show()
                                    navController.navigate(com.moham.taxi.ui.navigation.AppScreens.BillingData.route)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Crear presupuesto")
                    }
                }
            }
        }
    }
}
