package com.moham.taxi.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.ui.theme.DarkBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    val paymentMethodBreakdown: Map<String, Double>,
    val isLoadedSuccessfully: Boolean = true,
    var isConsumed: Boolean = false
)

@Composable
fun SplashScreen(
    onLoadingComplete: (SplashScreenPreloadData) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication

    // Animatable progress from 0f to 1f
    val progressAnim = remember { Animatable(0f) }

    // Obtener icono oficial de la aplicación
    val appIconBitmap = remember(context) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, 0)
            val drawable = pm.getApplicationIcon(appInfo)
            val bmp = Bitmap.createBitmap(
                256,
                256,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        // 1. Iniciar carga de datos en segundo plano
        val dataDeferred = async(Dispatchers.IO) {
            try {
                application.awaitDateReady()
                val selectedDate = application.getSelectedDate().first()
                val taxiRepo = application.taxiRideRepository
                val expenseRepo = application.expenseRepository

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
                null
            }
        }

        // 2. Animación de la barra de progreso:
        // Fase 1: Rápida hasta casi el final (~88% en 1.3s)
        progressAnim.animateTo(
            targetValue = 0.88f,
            animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing)
        )

        // Fase 2: Ahí se queda un poco hasta cargar (~88% a 92% en 2.4s)
        progressAnim.animateTo(
            targetValue = 0.92f,
            animationSpec = tween(durationMillis = 2400, easing = LinearOutSlowInEasing)
        )

        // Esperar a que los datos estén listos y asegurar tiempo total mínimo de 4 segundos
        val data = dataDeferred.await()
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 4000) {
            delay(4000 - elapsed)
        }

        // Fase 3: Rápido remate al 100% (250ms)
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )

        // Breve pausa para visualizar la barra completada antes de entrar a la app
        delay(200)

        val finalData = data ?: SplashScreenPreloadData(
            selectedDate = Date(),
            dateIncome = 0.0,
            weekIncome = 0.0,
            monthIncome = 0.0,
            dateExpenses = 0.0,
            weekExpenses = 0.0,
            monthExpenses = 0.0,
            dateNet = 0.0,
            weekNet = 0.0,
            monthNet = 0.0,
            rideCount = 0,
            weekRideCount = 0,
            monthRideCount = 0,
            expenseCount = 0,
            weekExpenseCount = 0,
            monthExpenseCount = 0,
            fuelExpenses = 0.0,
            weekFuelExpenses = 0.0,
            monthFuelExpenses = 0.0,
            paymentMethodBreakdown = emptyMap(),
            isLoadedSuccessfully = false
        )
        onLoadingComplete(finalData)
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Resplandor ambiental muy suave detrás del icono central
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFC107).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 260f
                    )
                )
        )

        // Contenido Central: Icono de la App + Barra de Carga
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Contenedor del Icono de la App
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFFC107))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = "Icono de la app",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Icono de la app",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Barra de Carga
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFB300),
                                    Color(0xFFFFD54F)
                                )
                            )
                        )
                )
            }
        }

        // Elemento inferior: Distintivo sutil con el "11"
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "11",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
