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
    val tips = listOf(
            stringResource(R.string.tip_1),
            stringResource(R.string.tip_2),
            stringResource(R.string.tip_3),
            stringResource(R.string.tip_4),
            stringResource(R.string.tip_5)
        )
    val selectedTip = remember { tips.first() }
    var loadingText by remember { mutableStateOf(selectedTip) }

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
            
            // Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp) // Espacio para animaciones
            ) {
                // Outer glow ring (animate-pulse)
                Box(
                    modifier = Modifier
                        .size(120.dp) // Base size
                        .scale(1.5f) // scale 1.5
                        .alpha(pulseAlpha) // animate alpha
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f))
                )
                
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(1.2f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )

                // Animated ring (ping)
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawCircle(
                        color = PrimaryBlue.copy(alpha = pingAlpha),
                        radius = size.minDimension / 2 * pingScale,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Icon container actual
                Box(
                    modifier = Modifier
                        .size(112.dp) // h-28 w-28 = 7rem = 112dp
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
                        // Border simulated with another box or stroke, simplify with simple background
                        .padding(1.dp) // Border thickness
                ) {
                    // Inner content for border effect if needed, usually clean without border is fine or use border modifier
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Transparent), // Already handled by gradient
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = "Logo",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))

            // Title "GESTIÓN TAXI"
            val titleText = stringResource(R.string.app_name_title)
            Text(
                text = titleText,
                fontSize = 32.sp, // text-4xl
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = if (titleText.length > 12) 2.sp else 4.sp,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Decorative Lines
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Linea izquierda (fading in)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, PrimaryBlue.copy(alpha = 0.6f))
                            )
                        )
                )
                // Punto central
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
                // Linea derecha (fading out)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryBlue.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.app_subtitle),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.7f), // muted-foreground
                letterSpacing = 0.5.sp, // tracking-wide
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))

            // Progress Section
            Column(
                modifier = Modifier.width(300.dp), // max-w-xs approx
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Bar Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)) // secondary bg
                ) {
                    // Progress Fill with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        PrimaryBlue,
                                        Color(0xFF34D399) // Emerald-400 approx for gradient end
                                    )
                                )
                            )
                    )
                    
                    // Shimmer Effect (Simple animation moving across)
                    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerTranslate by shimmerTransition.animateFloat(
                        initialValue = -0.2f, // Start before the bar
                        targetValue = 1.2f, // End after the bar
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmerTranslate"
                    )
                    
                    // Only show shimmer if progress < 100
                    if (progress < 100) {
                         Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .graphicsLayer {
                                    translationX = size.width * shimmerTranslate
                                }
                                .width(80.dp) // w-20
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loading Text
                Text(
                    text = loadingText,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 18.sp
                )
                
                // Percentage
                Text(
                    text = "${progress.toInt()}%",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }


    }
}
