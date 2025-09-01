package com.moham.taxi.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.moham.taxi.GestionTaxiApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlinx.coroutines.flow.first
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import kotlinx.coroutines.withTimeoutOrNull
import com.moham.taxi.ui.theme.PrimaryBlue

// Definir un data class para los datos precargados
data class SplashScreenPreloadData(
    val selectedDate: Date,
    val dateIncome: Double,
    val weekIncome: Double,
    val monthIncome: Double,
    val dateExpenses: Double,
    val weekExpenses: Double,
    val monthExpenses: Double,
    val dateNet: Double,
    val weekNet: Double,
    val monthNet: Double,
    val rideCount: Int,
    val weekRideCount: Int,
    val monthRideCount: Int,
    val expenseCount: Int,
    val weekExpenseCount: Int,
    val monthExpenseCount: Int,
    val fuelExpenses: Double,
    val weekFuelExpenses: Double,
    val monthFuelExpenses: Double,
    val paymentMethodBreakdown: Map<String, Double>
)

@Composable
fun SplashScreen(
    onLoadingComplete: (SplashScreenPreloadData) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadingText by remember { mutableStateOf("Iniciando aplicación...") }
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Animación para el logo (bounce)
    val infiniteTransition = rememberInfiniteTransition()
    val scaleLogo by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    // Animación para el texto de carga
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(key1 = true) {
        try {
            val startTime = System.currentTimeMillis()
            loadingText = "Cargando datos principales..."

            // Intentar cargar los datos, pero con un máximo de 6 segundos
            val preloadData = withTimeoutOrNull(6000) {
                val selectedDate = application.getSelectedDate().first()
                val taxiRepo = application.taxiRideRepository
                val expenseRepo = application.expenseRepository
                val dateIncome = withContext(Dispatchers.IO) { taxiRepo.getIncomeForDate(selectedDate) }
                val weekIncome = withContext(Dispatchers.IO) { taxiRepo.getWeekIncomeForDate(selectedDate) }
                val monthIncome = withContext(Dispatchers.IO) { taxiRepo.getMonthIncomeForDate(selectedDate) }
                val dateExpenses = withContext(Dispatchers.IO) { expenseRepo.getExpensesTotalForDate(selectedDate) }
                val weekExpenses = withContext(Dispatchers.IO) { expenseRepo.getWeekExpensesForDate(selectedDate) }
                val monthExpenses = withContext(Dispatchers.IO) { expenseRepo.getMonthExpensesForDate(selectedDate) }
                val dateNet = dateIncome - dateExpenses
                val weekNet = weekIncome - weekExpenses
                val monthNet = monthIncome - monthExpenses
                val rideCount = withContext(Dispatchers.IO) { taxiRepo.getRideCountForDate(selectedDate) }
                val weekRideCount = withContext(Dispatchers.IO) { taxiRepo.getWeekRideCountForDate(selectedDate) }
                val monthRideCount = withContext(Dispatchers.IO) { taxiRepo.getMonthRideCountForDate(selectedDate) }
                val expenseCount = withContext(Dispatchers.IO) { expenseRepo.getExpenseCountForDate(selectedDate) }
                val weekExpenseCount = withContext(Dispatchers.IO) { expenseRepo.getWeekExpenseCountForDate(selectedDate) }
                val monthExpenseCount = withContext(Dispatchers.IO) { expenseRepo.getMonthExpenseCountForDate(selectedDate) }
                val fuelExpenses = withContext(Dispatchers.IO) { expenseRepo.getFuelExpensesForDate(selectedDate) }
                val weekFuelExpenses = withContext(Dispatchers.IO) { expenseRepo.getCurrentWeekFuelExpenses() }
                val monthFuelExpenses = withContext(Dispatchers.IO) { expenseRepo.getMonthFuelExpenses() }
                val paymentMethodBreakdown = withContext(Dispatchers.IO) { taxiRepo.getIncomeByPaymentMethodForDate(selectedDate) }
                SplashScreenPreloadData(
                    selectedDate,
                    dateIncome, weekIncome, monthIncome,
                    dateExpenses, weekExpenses, monthExpenses,
                    dateNet, weekNet, monthNet,
                    rideCount, weekRideCount, monthRideCount,
                    expenseCount, weekExpenseCount, monthExpenseCount,
                    fuelExpenses, weekFuelExpenses, monthFuelExpenses,
                    paymentMethodBreakdown
                )
            }

            // Calcular el tiempo transcurrido
            val elapsed = System.currentTimeMillis() - startTime
            val minTime = 3000L
            if (elapsed < minTime) {
                delay(minTime - elapsed)
            }

        isLoading = false
            if (preloadData != null) {
                onLoadingComplete(preloadData)
            } else {
                loadingText = "No se pudo cargar la información."
                
            }
        } catch (e: Exception) {
            loadingText = "Error al cargar datos: ${e.message}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Fondo negro mate
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(40.dp)
        ) {
            // Logo minimalista con animación sutil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleLogo)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryBlue.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 100f
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = "Logo Taxi",
                    tint = PrimaryBlue, // Azul corporativo
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Título principal con tipografía elegante
            Text(
                text = "GESTION TAXI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            
            // Subtítulo Group Fasata
            Text(
                text = "GROUP FASATA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue.copy(alpha = 0.9f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Línea decorativa
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                PrimaryBlue,
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subtítulo profesional
            Text(
                text = "Solución profesional para taxistas",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Indicador de progreso minimalista
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(2.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = PrimaryBlue,
                    trackColor = Color.Transparent
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Texto de estado minimalista
            Text(
                text = loadingText,
                modifier = Modifier.alpha(alpha),
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}