package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.theme.PrimaryBlue
import com.moham.taxi.ui.theme.DarkBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.util.Date

// Data class para los datos precargados
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
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Estado de progreso (0 a 100)
    var progress by remember { mutableStateOf(0f) }
    var loadingText by remember { mutableStateOf("") }

    // Animación de pulso para el logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Animación "Ping" (anillo que se expande y desvanece)
    val pingTransition = rememberInfiniteTransition(label = "ping")
    val pingScale by pingTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingScale"
    )
    val pingAlpha by pingTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pingAlpha"
    )

    // Lógica principal
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        
        // 1. Iniciar carga de datos en segundo plano
        val dataDeferred = async(Dispatchers.IO) {
            try {
                val selectedDate = application.getSelectedDate().first()
                val taxiRepo = application.taxiRideRepository
                val expenseRepo = application.expenseRepository
                
                // Cargas paralelas de datos
                val dateIncome = taxiRepo.getIncomeForDate(selectedDate)
                val weekIncome = taxiRepo.getWeekIncomeForDate(selectedDate)
                val monthIncome = taxiRepo.getMonthIncomeForDate(selectedDate)
                val dateExpenses = expenseRepo.getExpensesTotalForDate(selectedDate)
                val weekExpenses = expenseRepo.getWeekExpensesForDate(selectedDate)
                val monthExpenses = expenseRepo.getMonthExpensesForDate(selectedDate)
                val rideCount = taxiRepo.getRideCountForDate(selectedDate)
                val weekRideCount = taxiRepo.getWeekRideCountForDate(selectedDate)
                val monthRideCount = taxiRepo.getMonthRideCountForDate(selectedDate)
                val expenseCount = expenseRepo.getExpenseCountForDate(selectedDate)
                val weekExpenseCount = expenseRepo.getWeekExpenseCountForDate(selectedDate)
                val monthExpenseCount = expenseRepo.getMonthExpenseCountForDate(selectedDate)
                val fuelExpenses = expenseRepo.getFuelExpensesForDate(selectedDate)
                val weekFuelExpenses = expenseRepo.getCurrentWeekFuelExpenses()
                val monthFuelExpenses = expenseRepo.getMonthFuelExpenses()
                val paymentMethodBreakdown = taxiRepo.getIncomeByPaymentMethodForDate(selectedDate)

                SplashScreenPreloadData(
                    selectedDate = selectedDate,
                    dateIncome = dateIncome,
                    weekIncome = weekIncome,
                    monthIncome = monthIncome,
                    dateExpenses = dateExpenses,
                    weekExpenses = weekExpenses,
                    monthExpenses = monthExpenses,
                    dateNet = dateIncome - dateExpenses,
                    weekNet = weekIncome - weekExpenses,
                    monthNet = monthIncome - monthExpenses,
                    rideCount = rideCount,
                    weekRideCount = weekRideCount,
                    monthRideCount = monthRideCount,
                    expenseCount = expenseCount,
                    weekExpenseCount = weekExpenseCount,
                    monthExpenseCount = monthExpenseCount,
                    fuelExpenses = fuelExpenses,
                    weekFuelExpenses = weekFuelExpenses,
                    monthFuelExpenses = monthFuelExpenses,
                    paymentMethodBreakdown = paymentMethodBreakdown
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null // Retornar null en caso de error
            }
        }

        // 2. Controlar barra de progreso (simulada)
        // Incremento de 2 cada 50ms => 100 pasos * 50ms = 5000ms = 5 segundos estricto
        // Ajustamos para ser un poco más rápido si se desea, pero el TSX dice 50ms
        launch {
            while (progress < 100) {
                delay(50)
                progress += 2
                // Sincronizar visualmente para que no pase de 100
                if (progress > 100) progress = 100f
            }
        }
        
        // Esperar a que el progreso llegue a 100 (aprox 2.5 seg con step de 2 cada 50ms)
        // Corrección: 100 / 2 = 50 steps. 50 * 50ms = 2500ms.
        while (progress < 100) {
            delay(50)
        }

        // Asegurar espera mínima de 4 segundos
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 4000) {
            delay(4000 - elapsed)
        }

        // 3. Esperar resultados y finalizar
        val data = dataDeferred.await()
        
        if (data != null) {
            // Pequeña pausa final al 100%
            delay(300) 
            onLoadingComplete(data)
        } else {
             // Manejo de error si falla la carga (podríamos mostrar un botón de reintentar, pero por ahora...)
            loadingText = context.getString(R.string.loading_error)
            delay(2000)
             // Intentar de nuevo o permitir entrar vacío? Por ahora llamamos con datos vacíos o reintentamos
             // Para simplificar, si falla mucho, podríamos pasar un objeto vacío o salir.
             // Aquí simplemente nos quedamos en error o llamamos onLoadingComplete con null si la firma lo permitiera.
             // Al no permitir null, reintentamos la actividad (simplemente dejando al usuario aquí o reiniciando el proceso).
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Fondo oscuro compatible
    ) {
        // Efectos de fondo (blobs/gradientes)
        // Blob superior
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 100.dp) // top-1/4 aprox
                .size(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        ),

                        radius = 200f
                    )
                )
                // En Compose standard el blur fuerte es costoso, usamos gradiente suave que ya simula el blur
        )
        
        // Blob inferior
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-150).dp) // bottom-1/4 aprox
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.1f),
                            Color.Transparent
                        ),

                        radius = 150f
                    )
                )
        )

        // Contenido Central
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Logo Container (Aumentado de tamaño)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp) // Espacio para animaciones (anteriormente 200.dp)
            ) {
                // Outer glow ring (animate-pulse)
                Box(
                    modifier = Modifier
                        .size(180.dp) // Base size (anteriormente 120.dp)
                        .scale(1.5f)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f))
                )
                
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(180.dp) // Anteriormente 120.dp
                        .scale(1.2f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )

                // Animated ring (ping)
                Canvas(modifier = Modifier.size(180.dp)) { // Anteriormente 120.dp
                    drawCircle(
                        color = PrimaryBlue.copy(alpha = pingAlpha),
                        radius = size.minDimension / 2 * pingScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Icon container actual
                Box(
                    modifier = Modifier
                        .size(168.dp) // Anteriormente 112.dp
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    PrimaryBlue.copy(alpha = 0.2f),
                                    PrimaryBlue.copy(alpha = 0.05f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(1.dp) // Border thickness
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = "Logo",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(84.dp) // Anteriormente 56.dp
                        )
                    }
                }
            }
        }


    }
}
