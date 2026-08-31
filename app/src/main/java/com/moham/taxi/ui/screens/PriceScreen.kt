package com.moham.taxi.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.moham.taxi.data.model.SavedQuote
import com.moham.taxi.data.service.PdfGenerator
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.QuoteData
import com.moham.taxi.data.model.QuoteItem
import com.moham.taxi.data.model.Surcharge
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.data.service.GeocodingService
import com.moham.taxi.data.service.PlaceSuggestion
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.TariffViewModel
import com.moham.taxi.utils.PriceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

enum class PriceStep {
    SELECT_ACTION,
    SELECT_ADDRESSES,
    SELECT_TARIFF,
    SUMMARY
}

// Opciones de tarifa de Madrid solicitadas por el usuario
enum class MadridTariffOption(val displayName: String, val tariff: MadridTariff, val isWorkday: Boolean) {
    T1("Tarifa 1", MadridTariff.T1, true),
    T2("Tarifa 2", MadridTariff.T2, false),
    T3_AFTER_1("Tarifa 3 y después 1", MadridTariff.T3, true),
    T3_AFTER_2("Tarifa 3 y después 2", MadridTariff.T3, false),
    T4_FIXED("Tarifa 4 fija", MadridTariff.T4, true),
    T7_AFTER_1("Tarifa 7 después 1", MadridTariff.T7, true),
    T7_AFTER_2("Tarifa 7 después 2", MadridTariff.T7, false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceScreen(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val application = context.applicationContext as GestionTaxiApplication
    val tariffViewModel: TariffViewModel = viewModel(
        factory = TariffViewModel.Factory(application)
    )
    val currentCity by tariffViewModel.currentCity.collectAsState()
    val allTariffs by tariffViewModel.allTariffs.collectAsState()
    val allSurcharges by tariffViewModel.allSurcharges.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Control de paso/pantalla actual
    var currentStep by remember { mutableStateOf(PriceStep.SELECT_ACTION) }

    val destinationFocusRequester = remember { FocusRequester() }

    val latestBudgetsJson by application.getLatestBudgets().collectAsState(initial = "[]")
    val latestBudgets = remember(latestBudgetsJson) {
        try {
            val list = mutableListOf<SavedQuote>()
            val arr = org.json.JSONArray(latestBudgetsJson)
            for (i in 0 until arr.length()) {
                list.add(SavedQuote.fromJson(arr.getString(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    val historicalOrigins = remember(latestBudgets) {
        latestBudgets.map { it.origin }.distinct().take(2)
    }

    // Paso 1: Estados de búsqueda de direcciones
    var originQuery by remember { mutableStateOf("") }
    var destinationQuery by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf<PlaceSuggestion?>(null) }
    var selectedDestination by remember { mutableStateOf<PlaceSuggestion?>(null) }
    var originSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }
    var destinationSuggestions by remember { mutableStateOf<List<PlaceSuggestion>>(emptyList()) }

    // Estados de cálculo de ruta
    var isRoutingLoading by remember { mutableStateOf(false) }
    var calculatedDistanceKm by remember { mutableStateOf(0.0) }
    var calculatedDurationSec by remember { mutableStateOf(0.0) }

    // Paso 2: Estados de kilómetros y tarifas
    var kilometers by remember { mutableStateOf("") }
    var isTariffDropdownExpanded by remember { mutableStateOf(false) }
    var isTrafficDropdownExpanded by remember { mutableStateOf(false) }
    
    // Madrid
    var selectedMadridTariffOption by remember { mutableStateOf<MadridTariffOption?>(null) }
    var selectedTraffic by remember { mutableStateOf<TrafficLevel>(TrafficLevel.NONE) }

    // Otras ciudades
    var selectedNonMadridTariff by remember { mutableStateOf<Tariff?>(null) }
    val selectedSurcharges = remember { mutableStateMapOf<Long, Boolean>() }
    val surchargeQuantities = remember { mutableStateMapOf<Long, Int>() }

    // Paso 3: Precio final
    var price by remember { mutableStateOf<Double?>(null) }

    // BackHandler inteligente según el paso actual
    BackHandler {
        when (currentStep) {
            PriceStep.SELECT_ACTION -> {
                navController.navigate(AppScreens.Other.route) {
                    popUpTo(AppScreens.Other.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            PriceStep.SELECT_ADDRESSES -> {
                currentStep = PriceStep.SELECT_ACTION
            }
            PriceStep.SELECT_TARIFF -> {
                currentStep = PriceStep.SELECT_ADDRESSES
            }
            PriceStep.SUMMARY -> {
                currentStep = PriceStep.SELECT_TARIFF
            }
        }
    }

    // Debounce para sugerencias de origen
    LaunchedEffect(originQuery) {
        if (originQuery.trim().length >= 3 && originQuery != selectedOrigin?.displayName) {
            delay(600)
            originSuggestions = GeocodingService.getSuggestions(originQuery, currentCity)
        } else if (originQuery.isEmpty() || originQuery == selectedOrigin?.displayName) {
            originSuggestions = emptyList()
        }
    }

    // Debounce para sugerencias de destino
    LaunchedEffect(destinationQuery) {
        if (destinationQuery.trim().length >= 3 && destinationQuery != selectedDestination?.displayName) {
            delay(600)
            destinationSuggestions = GeocodingService.getSuggestions(destinationQuery, currentCity)
        } else if (destinationQuery.isEmpty() || destinationQuery == selectedDestination?.displayName) {
            destinationSuggestions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            PriceStep.SELECT_ACTION -> "Presupuestos"
                            PriceStep.SELECT_ADDRESSES -> "Presupuesto: Direcciones"
                            PriceStep.SELECT_TARIFF -> "Presupuesto: Tarifa"
                            PriceStep.SUMMARY -> "Presupuesto: Resumen"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentStep) {
                            PriceStep.SELECT_ACTION -> {
                                navController.navigate(AppScreens.Other.route) {
                                    popUpTo(AppScreens.Other.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            PriceStep.SELECT_ADDRESSES -> currentStep = PriceStep.SELECT_ACTION
                            PriceStep.SELECT_TARIFF -> currentStep = PriceStep.SELECT_ADDRESSES
                            PriceStep.SUMMARY -> currentStep = PriceStep.SELECT_TARIFF
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedItem = 3, // Seleccionamos "Otras"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    PriceStep.SELECT_ACTION -> {
                        // Card for "Nuevo Presupuesto"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentStep = PriceStep.SELECT_ADDRESSES
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nuevo presupuesto",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Calcular precio estimado usando direcciones o manual",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle "Últimos presupuestos"
                        Text(
                            text = "Últimos presupuestos",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (latestBudgets.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "No hay presupuestos recientes",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                latestBudgets.forEach { savedQuote ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                coroutineScope.launch {
                                                    val billingData = application.getBillingData().first()
                                                    val pdfUri = PdfGenerator.generateQuote(
                                                        context = context,
                                                        billingData = billingData,
                                                        quoteData = savedQuote.quoteData,
                                                        origin = savedQuote.origin,
                                                        destination = savedQuote.destination,
                                                        dateTime = savedQuote.dateTime
                                                    )
                                                    if (pdfUri != null) {
                                                        PdfGenerator.sharePdf(context, pdfUri)
                                                    } else {
                                                        Toast.makeText(context, "Error al abrir el presupuesto", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${savedQuote.origin} ➔ ${savedQuote.destination}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = savedQuote.dateTime,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = formatCurrency(savedQuote.quoteData.totalAmount),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            val billingData = application.getBillingData().first()
                                                            val pdfUri = PdfGenerator.generateQuote(
                                                                context = context,
                                                                billingData = billingData,
                                                                quoteData = savedQuote.quoteData,
                                                                origin = savedQuote.origin,
                                                                destination = savedQuote.destination,
                                                                dateTime = savedQuote.dateTime
                                                            )
                                                            if (pdfUri != null) {
                                                                PdfGenerator.sharePdf(context, pdfUri)
                                                            } else {
                                                                Toast.makeText(context, "Error al compartir el presupuesto", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Compartir",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PriceStep.SELECT_ADDRESSES -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Campo Origen
                                OutlinedTextField(
                                    value = originQuery,
                                    onValueChange = { originQuery = it },
                                    label = { Text("Buscar origen") },
                                    placeholder = { Text("Buscar origen") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    trailingIcon = {
                                        if (originQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                originQuery = ""
                                                selectedOrigin = null
                                                originSuggestions = emptyList()
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (originQuery.isEmpty() && historicalOrigins.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Ubicaciones recientes",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                            historicalOrigins.forEach { histOrigin ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                val suggestions = GeocodingService.getSuggestions(histOrigin, currentCity)
                                                                val bestSuggestion = if (suggestions.isNotEmpty()) {
                                                                    suggestions.first()
                                                                } else {
                                                                    PlaceSuggestion(histOrigin, 0.0, 0.0)
                                                                }
                                                                selectedOrigin = bestSuggestion
                                                                originQuery = bestSuggestion.displayName
                                                                originSuggestions = emptyList()
                                                                destinationFocusRequester.requestFocus()
                                                            }
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = histOrigin,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            }
                                        }
                                    }
                                }

                                if (originSuggestions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(originSuggestions) { suggestion ->
                                                Text(
                                                    text = suggestion.displayName,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedOrigin = suggestion
                                                            originQuery = suggestion.displayName
                                                            originSuggestions = emptyList()
                                                            destinationFocusRequester.requestFocus()
                                                        }
                                                        .padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Campo Destino
                                OutlinedTextField(
                                    value = destinationQuery,
                                    onValueChange = { destinationQuery = it },
                                    label = { Text("Buscar destino") },
                                    placeholder = { Text("Buscar destino") },
                                    leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                                    trailingIcon = {
                                        if (destinationQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                destinationQuery = ""
                                                selectedDestination = null
                                                destinationSuggestions = emptyList()
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().focusRequester(destinationFocusRequester),
                                    singleLine = true
                                )

                                if (destinationSuggestions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(destinationSuggestions) { suggestion ->
                                                Text(
                                                    text = suggestion.displayName,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedDestination = suggestion
                                                            destinationQuery = suggestion.displayName
                                                            destinationSuggestions = emptyList()
                                                            focusManager.clearFocus()
                                                        }
                                                        .padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botón Calcular Manual
                        OutlinedButton(
                            onClick = {
                                selectedOrigin = null
                                selectedDestination = null
                                originQuery = ""
                                destinationQuery = ""
                                kilometers = ""
                                calculatedDistanceKm = 0.0
                                calculatedDurationSec = 0.0
                                currentStep = PriceStep.SELECT_TARIFF
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Calcular manual", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botón Siguiente
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isRoutingLoading = true
                                    val start = selectedOrigin
                                    val end = selectedDestination
                                    if (start != null && end != null) {
                                        val result = GeocodingService.getRouteDistance(
                                            startLat = start.latitude, startLon = start.longitude,
                                            endLat = end.latitude, endLon = end.longitude
                                        )
                                        if (result != null) {
                                            val adjustedDistance = when {
                                                result.distanceKm < 10.0 -> result.distanceKm * 1.05
                                                result.distanceKm > 50.0 -> result.distanceKm * 1.01
                                                result.distanceKm > 20.0 -> result.distanceKm * 1.025
                                                else -> result.distanceKm
                                            }
                                            calculatedDistanceKm = adjustedDistance
                                            calculatedDurationSec = result.durationSeconds
                                            kilometers = String.format(Locale.US, "%.2f", adjustedDistance)
                                        } else {
                                            Toast.makeText(context, "No se pudo calcular la ruta automáticamente. Puede introducir los kilómetros manualmente.", Toast.LENGTH_LONG).show()
                                            kilometers = ""
                                            calculatedDistanceKm = 0.0
                                            calculatedDurationSec = 0.0
                                        }
                                    } else {
                                        // En caso de que hayan escrito manualmente y no seleccionaron del autocompletado
                                        kilometers = ""
                                        calculatedDistanceKm = 0.0
                                        calculatedDurationSec = 0.0
                                    }
                                    isRoutingLoading = false
                                    currentStep = PriceStep.SELECT_TARIFF
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = originQuery.isNotBlank() && destinationQuery.isNotBlank()
                        ) {
                            Text("Siguiente", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }

                    PriceStep.SELECT_TARIFF -> {
                        val showKmInput = if (currentCity == "Madrid") {
                            selectedMadridTariffOption != MadridTariffOption.T4_FIXED
                        } else {
                            selectedNonMadridTariff?.isFixed != true
                        }

                        if (calculatedDistanceKm > 0.0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Trayecto estimado:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Origen: ${selectedOrigin?.displayName ?: originQuery}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Destino: ${selectedDestination?.displayName ?: destinationQuery}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            if (!showKmInput) {
                                                Text("Distancia: ${String.format(Locale.US, "%.2f", calculatedDistanceKm)} km", fontWeight = FontWeight.Bold)
                                            }
                                            if (calculatedDurationSec > 0.0) {
                                                if (!showKmInput) Spacer(modifier = Modifier.height(2.dp))
                                                Text("Duración: ${formatDuration(calculatedDurationSec)}", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (showKmInput) {
                                            OutlinedTextField(
                                                value = kilometers,
                                                onValueChange = {
                                                    if (it.isEmpty() || it.toDoubleOrNull() != null) kilometers = it
                                                },
                                                label = { Text("Distancia (km)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.width(160.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showKmInput && calculatedDistanceKm <= 0.0) {
                            OutlinedTextField(
                                value = kilometers,
                                onValueChange = {
                                    if (it.isEmpty() || it.toDoubleOrNull() != null) kilometers = it
                                },
                                label = { Text("Kilómetros") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (currentCity == "Madrid") {
                            ExposedDropdownMenuBox(
                                expanded = isTariffDropdownExpanded,
                                onExpandedChange = { isTariffDropdownExpanded = !isTariffDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedMadridTariffOption?.displayName ?: "Selecciona tarifa",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tarifa") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTariffDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = isTariffDropdownExpanded,
                                    onDismissRequest = { isTariffDropdownExpanded = false }
                                ) {
                                    MadridTariffOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(option.displayName, fontWeight = FontWeight.Bold)
                                                    val desc = when (option) {
                                                        MadridTariffOption.T1 -> "Laborables de día (07:00 a 21:00)"
                                                        MadridTariffOption.T2 -> "Noches, fines de semana y festivos"
                                                        MadridTariffOption.T3_AFTER_1 -> "Carrera mínima de 22€ con 9km incl., luego Tarifa 1"
                                                        MadridTariffOption.T3_AFTER_2 -> "Carrera mínima de 22€ con 9km incl., luego Tarifa 2"
                                                        MadridTariffOption.T4_FIXED -> "Tarifa plana al aeropuerto (33.00€)"
                                                        MadridTariffOption.T7_AFTER_1 -> "Carrera mínima de 8€ con 1.45km incl., luego Tarifa 1"
                                                        MadridTariffOption.T7_AFTER_2 -> "Carrera mínima de 8€ con 1.45km incl., luego Tarifa 2"
                                                    }
                                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                                }
                                            },
                                            onClick = {
                                                selectedMadridTariffOption = option
                                                isTariffDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Modificador de Tráfico para Madrid
                            if (selectedMadridTariffOption != null && selectedMadridTariffOption != MadridTariffOption.T4_FIXED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                ExposedDropdownMenuBox(
                                    expanded = isTrafficDropdownExpanded,
                                    onExpandedChange = { isTrafficDropdownExpanded = !isTrafficDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = stringResource(selectedTraffic.labelResId),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Nivel de tráfico previsto") },
                                        leadingIcon = {
                                            Icon(
                                                selectedTraffic.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTrafficDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isTrafficDropdownExpanded,
                                        onDismissRequest = { isTrafficDropdownExpanded = false }
                                    ) {
                                        TrafficLevel.values().forEach { level ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(level.icon, contentDescription = null)
                                                },
                                                text = {
                                                    Text(stringResource(level.labelResId))
                                                },
                                                onClick = {
                                                    selectedTraffic = level
                                                    isTrafficDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Otras ciudades: listado de tarifas de la DB
                            if (allTariffs.isEmpty()) {
                                Text("No hay tarifas configuradas para esta ciudad en los Ajustes.", color = MaterialTheme.colorScheme.error)
                            } else {
                                ExposedDropdownMenuBox(
                                    expanded = isTariffDropdownExpanded,
                                    onExpandedChange = { isTariffDropdownExpanded = !isTariffDropdownExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = selectedNonMadridTariff?.let { PriceUtils.formatTariffNameForDisplay(it.name) } ?: "Selecciona tarifa",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tarifa") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTariffDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isTariffDropdownExpanded,
                                        onDismissRequest = { isTariffDropdownExpanded = false }
                                    ) {
                                        allTariffs.forEach { tariff ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(PriceUtils.formatTariffNameForDisplay(tariff.name), fontWeight = FontWeight.Bold)
                                                        if (tariff.isFixed) {
                                                            Text("Tarifa Fija: ${formatCurrency(tariff.fixedPrice ?: 0.0)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                                        } else {
                                                            Text("Base: ${formatCurrency(tariff.baseFare ?: 0.0)} + ${formatCurrency(tariff.pricePerKm ?: 0.0)}/km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedNonMadridTariff = tariff
                                                    isTariffDropdownExpanded = false
                                                }
                                            )
                                    }
                                }
                            }
                        }

                            // Cargar suplementos de otras ciudades
                            if (allSurcharges.isNotEmpty()) {
                                Text("Suplementos y paquetes:", fontWeight = FontWeight.Bold)
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
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón Calcular
                        val isCalculateEnabled = if (currentCity == "Madrid") {
                            selectedMadridTariffOption != null && (selectedMadridTariffOption == MadridTariffOption.T4_FIXED || kilometers.isNotEmpty())
                        } else {
                            selectedNonMadridTariff != null && (selectedNonMadridTariff!!.isFixed || kilometers.isNotEmpty())
                        }

                        Button(
                            onClick = {
                                val kmVal = kilometers.toDoubleOrNull() ?: 0.0
                                if (currentCity == "Madrid") {
                                    val option = selectedMadridTariffOption!!
                                    val calculated = calculatePrice(
                                        tariff = option.tariff,
                                        isWorkday = option.isWorkday,
                                        km = kmVal,
                                        traffic = selectedTraffic
                                    )
                                    if (calculated != null) {
                                        price = PriceUtils.roundToNearestFiveCents(calculated)
                                        coroutineScope.launch {
                                            tariffViewModel.incrementMadridCalculationsCount()
                                        }

                                        // Crear QuoteData
                                        val items = mutableListOf<QuoteItem>()
                                        when (option.tariff) {
                                            MadridTariff.T1 -> {
                                                items.add(QuoteItem(context.getString(R.string.quote_fare_start, "1"), null, 2.55, 2.55))
                                                items.add(QuoteItem(context.getString(R.string.quote_kilometers), kmVal, 1.40, kmVal * 1.40))
                                            }
                                            MadridTariff.T2 -> {
                                                items.add(QuoteItem(context.getString(R.string.quote_fare_start, "2"), null, 3.20, 3.20))
                                                items.add(QuoteItem(context.getString(R.string.quote_kilometers), kmVal, 1.60, kmVal * 1.60))
                                            }
                                            MadridTariff.T3 -> {
                                                items.add(QuoteItem(context.getString(R.string.quote_minimum_trip, "3"), null, 22.0, 22.0))
                                                val extraKm = (kmVal - 9.0).coerceAtLeast(0.0)
                                                if (extraKm > 0) {
                                                    val p = if (option.isWorkday) 1.40 else 1.60
                                                    items.add(QuoteItem(context.getString(R.string.quote_extra_km), extraKm, p, extraKm * p))
                                                }
                                            }
                                            MadridTariff.T4 -> {
                                                items.add(QuoteItem(context.getString(R.string.quote_flat_airport, "4"), null, 33.0, 33.0))
                                            }
                                            MadridTariff.T7 -> {
                                                items.add(QuoteItem(context.getString(R.string.quote_minimum_trip, "7"), null, 8.0, 8.0))
                                                val extraKm = (kmVal - 1.45).coerceAtLeast(0.0)
                                                if (extraKm > 0) {
                                                    val p = if (option.isWorkday) 1.40 else 1.60
                                                    items.add(QuoteItem(context.getString(R.string.quote_extra_km), extraKm, p, extraKm * p))
                                                }
                                            }
                                            else -> {}
                                        }

                                        if (selectedTraffic != TrafficLevel.NONE && option.tariff != MadridTariff.T4) {
                                            val surch = selectedTraffic.surcharge
                                            if (option.tariff == MadridTariff.T3 || option.tariff == MadridTariff.T7) {
                                                val extraKm = if (option.tariff == MadridTariff.T3) (kmVal - 9.0).coerceAtLeast(0.0) else (kmVal - 1.45).coerceAtLeast(0.0)
                                                if (extraKm > 0.0) {
                                                    items.add(QuoteItem(context.getString(R.string.quote_traffic_surcharge), null, surch, surch))
                                                } else if (selectedTraffic == TrafficLevel.MODERATE || selectedTraffic == TrafficLevel.HEAVY) {
                                                    items.add(QuoteItem(context.getString(R.string.quote_traffic_surcharge_base), null, surch, surch))
                                                }
                                            } else {
                                                items.add(QuoteItem(context.getString(R.string.quote_traffic_surcharge), null, surch, surch))
                                            }
                                        }

                                        val itemsSum = items.sumOf { it.total }
                                        val diff = price!! - itemsSum
                                        if (diff > 0.01) {
                                            items.add(QuoteItem(context.getString(R.string.quote_time_traffic_estimation), null, diff, diff))
                                        } else if (diff < -0.01) {
                                            items.add(QuoteItem(context.getString(R.string.quote_round_adjustment), null, diff, diff))
                                        }
                                        val quoteData = QuoteData(
                                            title = context.getString(R.string.quote_title_format, currentCity),
                                            items = items,
                                            totalAmount = price!!
                                        )
                                        application.currentQuote = quoteData
                                        
                                        // Guardar direcciones en la app para pre-rellenar
                                        val originText = selectedOrigin?.displayName ?: originQuery
                                        val destText = selectedDestination?.displayName ?: destinationQuery
                                        application.currentOrigin = originText
                                        application.currentDestination = destText
                                        
                                        val currentDateTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                        coroutineScope.launch {
                                            application.addQuoteToLatest(originText, destText, currentDateTime, quoteData)
                                        }
                                        
                                        currentStep = PriceStep.SUMMARY
                                    }
                                } else {
                                    // Cálculo no Madrid
                                    val t = selectedNonMadridTariff!!
                                    var total = 0.0
                                    if (t.isFixed) {
                                        total = t.fixedPrice ?: 0.0
                                    } else {
                                        val base = t.baseFare ?: 0.0
                                        val perKm = t.pricePerKm ?: 0.0
                                        val sur = t.surcharge ?: 0.0
                                        total = base + (kmVal * perKm) + sur
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
                                        items.add(QuoteItem(PriceUtils.formatTariffNameForDisplay(t.name), null, t.fixedPrice ?: 0.0, t.fixedPrice ?: 0.0))
                                    } else {
                                        items.add(QuoteItem(context.getString(R.string.quote_fare_start, PriceUtils.formatTariffNameForDisplay(t.name)), null, t.baseFare ?: 0.0, t.baseFare ?: 0.0))
                                        items.add(QuoteItem(context.getString(R.string.quote_kilometers), kmVal, t.pricePerKm ?: 0.0, kmVal * (t.pricePerKm ?: 0.0)))
                                        if ((t.surcharge ?: 0.0) > 0.0) {
                                            items.add(QuoteItem(context.getString(R.string.label_surcharge), null, t.surcharge!!, t.surcharge!!))
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
                                        items.add(QuoteItem(context.getString(R.string.quote_time_traffic_estimation), null, diff, diff))
                                    } else if (diff < -0.01) {
                                        items.add(QuoteItem(context.getString(R.string.quote_round_adjustment), null, diff, diff))
                                    }

                                    val quoteData = QuoteData(
                                        title = context.getString(R.string.quote_title_format, currentCity),
                                        items = items,
                                        totalAmount = price!!
                                    )
                                    application.currentQuote = quoteData

                                    // Guardar direcciones en la app para pre-rellenar
                                     val originText = selectedOrigin?.displayName ?: originQuery
                                     val destText = selectedDestination?.displayName ?: destinationQuery
                                     application.currentOrigin = originText
                                     application.currentDestination = destText

                                     val currentDateTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                     coroutineScope.launch {
                                         application.addQuoteToLatest(originText, destText, currentDateTime, quoteData)
                                     }

                                    currentStep = PriceStep.SUMMARY
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = isCalculateEnabled
                        ) {
                            Text("Calcular", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    PriceStep.SUMMARY -> {
                        // Tarjeta de precio principal
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Presupuesto estimado",
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                AutoSizeText(
                                    text = formatCurrency(price ?: 0.0),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxFontSize = 56.sp,
                                    minFontSize = 36.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Resumen de detalles del viaje
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Detalles del trayecto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                
                                Column {
                                    Text("Origen:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(selectedOrigin?.displayName ?: originQuery, style = MaterialTheme.typography.bodyMedium)
                                }
                                
                                Column {
                                    Text("Destino:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(selectedDestination?.displayName ?: destinationQuery, style = MaterialTheme.typography.bodyMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Distancia:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        val kmVal = kilometers.toDoubleOrNull() ?: 0.0
                                        Text("${String.format(Locale.US, "%.2f", kmVal)} km", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (calculatedDurationSec > 0.0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Duración estimada:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(formatDuration(calculatedDurationSec), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                Column {
                                    Text("Tarifa seleccionada:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    val tariffName = if (currentCity == "Madrid") {
                                        selectedMadridTariffOption?.displayName ?: ""
                                    } else {
                                        PriceUtils.formatTariffNameForDisplay(selectedNonMadridTariff?.name ?: "")
                                    }
                                    Text(tariffName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botones de acción
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val billingData = application.getBillingData().first()
                                    val isValid = billingData.name.isNotBlank() && billingData.nif.isNotBlank() && billingData.street.isNotBlank() && billingData.city.isNotBlank() && billingData.postalCode.isNotBlank()
                                    if (isValid) {
                                        navController.navigate(AppScreens.QuoteForm.route)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.billing_data_missing_toast), Toast.LENGTH_LONG).show()
                                        navController.navigate(AppScreens.BillingData.route)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Crear presupuesto (PDF)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Reiniciar estados
                                originQuery = ""
                                destinationQuery = ""
                                selectedOrigin = null
                                selectedDestination = null
                                originSuggestions = emptyList()
                                destinationSuggestions = emptyList()
                                kilometers = ""
                                price = null
                                selectedMadridTariffOption = null
                                selectedTraffic = TrafficLevel.NONE
                                selectedNonMadridTariff = null
                                selectedSurcharges.clear()
                                surchargeQuantities.clear()
                                calculatedDistanceKm = 0.0
                                calculatedDurationSec = 0.0
                                currentStep = PriceStep.SELECT_ADDRESSES
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Volver a empezar", fontSize = 16.sp)
                        }
                    }
                }
            }

            // Indicador de carga de cálculo de rutas
            if (isRoutingLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Calculando distancia y ruta...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Reutilizamos el formateador de tiempo
fun formatDuration(seconds: Double): String {
    val mins = (seconds / 60.0).roundToInt()
    if (mins < 60) {
        return "$mins min"
    }
    val hours = mins / 60
    val remainingMins = mins % 60
    return if (remainingMins == 0) "${hours}h" else "${hours}h ${remainingMins}min"
}

// Estructuras locales para mantener la lógica original de Madrid sin romper dependencias
sealed class MadridTariff(val displayName: String) {
    object T1 : MadridTariff("1")
    object T2 : MadridTariff("2")
    object T3 : MadridTariff("3")
    object T4 : MadridTariff("4")
    object T7 : MadridTariff("7")
}

enum class TrafficLevel(val labelResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val surcharge: Double) {
    NONE(R.string.traffic_none, Icons.Default.Traffic, 0.0),
    LIGHT(R.string.traffic_light, Icons.Default.Traffic, 2.5),
    MODERATE(R.string.traffic_moderate, Icons.Default.Traffic, 5.0),
    HEAVY(R.string.traffic_heavy, Icons.Default.Traffic, 10.0)
}

// Lógica de cálculo original de tarifas de Madrid
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
                    TrafficLevel.MODERATE, TrafficLevel.HEAVY -> traffic.surcharge ?: 0.0
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
                    TrafficLevel.MODERATE, TrafficLevel.HEAVY -> traffic.surcharge ?: 0.0
                    else -> 0.0
                }
            }
            basePrice + addTraffic
        }
    }
}

// Borde decorativo para selección
private object RowDefaults {
    val RowBorder @Composable get() = androidx.compose.foundation.BorderStroke(
        width = 2.dp,
        color = MaterialTheme.colorScheme.primary
    )
}
