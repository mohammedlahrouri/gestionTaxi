package com.moham.taxi.ui.screens

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
import androidx.compose.material.icons.filled.DirectionsCar
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
import com.moham.taxi.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceScreen(navController: NavController) {
    var selectedTariff by remember { mutableStateOf<Tariff?>(null) }
    var isDay by remember { mutableStateOf(true) }
    var kilometers by remember { mutableStateOf("") }
    var selectedTraffic by remember { mutableStateOf<TrafficLevel?>(null) }
    var price by remember { mutableStateOf<Double?>(null) }

    val tariffs = listOf(
        Tariff.T1, Tariff.T2, Tariff.T3, Tariff.T4, Tariff.T7
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calcular Precio") },
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
            // Selector de tarifa
            Text("Selecciona la tarifa", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tariffs.forEach { tariff ->
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
                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                            Text(tariff.displayName, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            // Selector diurno/nocturno si aplica
            if (selectedTariff == Tariff.T3 || selectedTariff == Tariff.T7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { isDay = true },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Brightness7, contentDescription = "Diurna")
                            Text("Diurna", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { isDay = false },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = "Nocturna")
                            Text("Nocturna", modifier = Modifier.padding(start = 4.dp))
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
                label = { Text("Kilómetros") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // Selector de tráfico
            val trafficEnabled = selectedTariff != Tariff.T4 && selectedTariff != null
            Text("Nivel de tráfico", fontWeight = FontWeight.Bold)
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
                            Icon(level.icon, contentDescription = level.label)
                            Text(level.label, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            // Botón calcular
            Button(
                onClick = {
                    price = calculatePrice(
                        tariff = selectedTariff,
                        isDay = isDay,
                        km = kilometers.toDoubleOrNull() ?: 0.0,
                        traffic = selectedTraffic
                    )
                },
                enabled = selectedTariff != null && kilometers.isNotEmpty() && (selectedTariff == Tariff.T4 || selectedTraffic != null)
            ) {
                Text("Calcular precio")
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
                        text = "Precio estimado",
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatCurrency(price!!),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 36.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Tarifas y lógica
sealed class Tariff(val displayName: String) {
    object T1 : Tariff("Tarifa 1")
    object T2 : Tariff("Tarifa 2")
    object T3 : Tariff("Tarifa 3")
    object T4 : Tariff("Tarifa 4 (Fija)")
    object T7 : Tariff("Tarifa 7")
}

enum class TrafficLevel(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val surcharge: Double) {
    NONE("Sin tráfico", Icons.Default.Traffic, 1.0),
    LIGHT("Leve", Icons.Default.Traffic, 2.5),
    MODERATE("Mucho", Icons.Default.Traffic, 5.0),
    HEAVY("Demasiado", Icons.Default.Traffic, 10.0)
}

fun calculatePrice(
    tariff: Tariff?,
    isDay: Boolean,
    km: Double,
    traffic: TrafficLevel?
): Double? {
    if (tariff == null || km <= 0) return null
    return when (tariff) {
        Tariff.T1 -> 2.55 + km * 1.35 + (traffic?.surcharge ?: 0.0)
        Tariff.T2 -> 3.20 + km * 1.50 + (traffic?.surcharge ?: 0.0)
        Tariff.T3 -> {
            val included = 9.0
            val base = 22.0
            val extraKm = (km - included).coerceAtLeast(0.0)
            val perKm = if (isDay) 1.35 else 1.50
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
        Tariff.T4 -> 33.0
        Tariff.T7 -> {
            val included = 1.45
            val base = 7.5
            val extraKm = (km - included).coerceAtLeast(0.0)
            val perKm = if (isDay) 1.35 else 1.50
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

// Función eliminada - usar formatCurrency de FinancialComponents
