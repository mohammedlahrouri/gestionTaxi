package com.moham.taxi.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.components.FinancialDetail
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.theme.CalendarBackground
import com.moham.taxi.ui.theme.ChartBackgroundColor
import com.moham.taxi.ui.theme.ChartExpenseColor
import com.moham.taxi.ui.theme.ChartGridColor
import com.moham.taxi.ui.theme.ChartIncomeColor
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.GreenAccent
import com.moham.taxi.ui.theme.RedAccent
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.moham.taxi.ui.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavHostController) {
    // Obtener el contexto y la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Scope para operaciones de coroutine
    val scope = rememberCoroutineScope()
    
    // ViewModels
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    // Estado para almacenar los datos financieros
    var selectedDate by remember { mutableStateOf(Date()) }
    var dateIncome by remember { mutableStateOf(0.0) }
    var weekIncome by remember { mutableStateOf(0.0) }
    var monthIncome by remember { mutableStateOf(0.0) }
    var dateExpenses by remember { mutableStateOf(0.0) }
    var weekExpenses by remember { mutableStateOf(0.0) }
    var monthExpenses by remember { mutableStateOf(0.0) }
    var dateNet by remember { mutableStateOf(0.0) }
    var weekNet by remember { mutableStateOf(0.0) }
    var monthNet by remember { mutableStateOf(0.0) }
    var rideCount by remember { mutableStateOf(0) }
    var weekRideCount by remember { mutableStateOf(0) }
    var monthRideCount by remember { mutableStateOf(0) }
    var expenseCount by remember { mutableStateOf(0) }
    var weekExpenseCount by remember { mutableStateOf(0) }
    var monthExpenseCount by remember { mutableStateOf(0) }
    var fuelExpenses by remember { mutableStateOf(0.0) }
    var weekFuelExpenses by remember { mutableStateOf(0.0) }
    var monthFuelExpenses by remember { mutableStateOf(0.0) }
    var paymentMethodBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekPaymentMethodBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthPaymentMethodBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    // Cargar datos financieros
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Cargar la fecha seleccionada desde DataStore
            selectedDate = application.getSelectedDate().first() ?: Date()
            
            // Datos diarios
            dateIncome = taxiRideViewModel.getIncomeForDate(selectedDate)
            dateExpenses = expenseViewModel.getExpensesTotalForDate(selectedDate)
            dateNet = dateIncome - dateExpenses
            rideCount = taxiRideViewModel.getRideCountForDate(selectedDate)
            expenseCount = expenseViewModel.getExpenseCountForDate(selectedDate)
            fuelExpenses = expenseViewModel.getFuelExpensesForDate(selectedDate)
            paymentMethodBreakdown = taxiRideViewModel.getIncomeByPaymentMethodForDate(selectedDate)
            
            // Datos semanales basados en la fecha seleccionada
            weekIncome = taxiRideViewModel.getWeekIncomeForDate(selectedDate)
            weekExpenses = expenseViewModel.getWeekExpensesForDate(selectedDate)
            weekNet = weekIncome - weekExpenses
            weekRideCount = taxiRideViewModel.getWeekRideCountForDate(selectedDate)
            weekExpenseCount = expenseViewModel.getWeekExpenseCountForDate(selectedDate)
            weekFuelExpenses = expenseViewModel.getWeekFuelExpensesForDate(selectedDate)
            weekPaymentMethodBreakdown = taxiRideViewModel.getWeekIncomeByPaymentMethodForDate(selectedDate)
            
            // Datos mensuales basados en la fecha seleccionada
            monthIncome = taxiRideViewModel.getMonthIncomeForDate(selectedDate)
            monthExpenses = expenseViewModel.getMonthExpensesForDate(selectedDate)
            monthNet = monthIncome - monthExpenses
            monthRideCount = taxiRideViewModel.getMonthRideCountForDate(selectedDate)
            monthExpenseCount = expenseViewModel.getMonthExpenseCountForDate(selectedDate)
            monthFuelExpenses = expenseViewModel.getMonthFuelExpensesForDate(selectedDate)
            monthPaymentMethodBreakdown = taxiRideViewModel.getMonthIncomeByPaymentMethodForDate(selectedDate)
        }
    }
    
    // Formato para las fechas
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Estadísticas Detalladas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sección de estadísticas diarias
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Estadísticas del Día (${formattedDate})",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Resumen principal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Ingresos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(dateIncome),
                                style = MaterialTheme.typography.titleMedium,
                                color = GreenAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Gastos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(dateExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                color = RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Neto
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Neto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(dateNet),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (dateNet >= 0) GreenAccent else RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Detalles adicionales - Mostrar solo los valores monetarios
                    FinancialDetail(
                        label = "Gastos en combustible",
                        amount = fuelExpenses,
                        icon = Icons.Filled.LocalGasStation
                    )
                    
                    FinancialDetail(
                        label = "Otros gastos",
                        amount = dateExpenses - fuelExpenses,
                        icon = Icons.Filled.Receipt
                    )
                    
                    // Añadir sección de métodos de pago en la misma tarjeta
                    if (paymentMethodBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Desglose por Métodos de Pago",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mostrar cada método de pago y su valor
                        paymentMethodBreakdown.entries.sortedByDescending { it.value }.forEach { (method, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (method.lowercase(Locale.getDefault())) {
                                            "efectivo" -> Icons.Filled.Money
                                            "tarjeta" -> Icons.Filled.CreditCard
                                            else -> Icons.Filled.Payment
                                        },
                                        contentDescription = method,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                
                                Text(
                                    text = formatCurrency(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "No hay datos de métodos de pago para esta fecha",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // Añadir gráfica de tendencia de los últimos 7 días dentro de la Card de estadísticas diarias
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tendencia de los últimos 7 días",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Variables para almacenar datos históricos
                    var lastSevenDaysIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    var lastSevenDaysExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    
                    // Cargar datos de los últimos 7 días
                    LaunchedEffect(selectedDate) {
                        withContext(Dispatchers.IO) {
                            val lastSevenDaysData = mutableListOf<Pair<Date, Double>>()
                            val lastSevenDaysExpensesData = mutableListOf<Pair<Date, Double>>()
                            
                            // Calendario para iterar por los últimos 7 días
                            val calendar = Calendar.getInstance().apply { time = selectedDate }
                            
                            // Retroceder al inicio (6 días atrás)
                            calendar.add(Calendar.DAY_OF_MONTH, -6)
                            
                            // Recopilar datos de los últimos 7 días
                            repeat(7) {
                                val currentDate = calendar.time
                                val dayIncome = taxiRideViewModel.getIncomeForDate(currentDate)
                                val dayExpenses = expenseViewModel.getExpensesTotalForDate(currentDate)
                                
                                // Solo agregar días con actividad (ingresos o gastos > 0)
                                if (dayIncome > 0 || dayExpenses > 0) {
                                    lastSevenDaysData.add(Pair(currentDate, dayIncome))
                                    lastSevenDaysExpensesData.add(Pair(currentDate, dayExpenses))
                                }
                                
                                // Avanzar al siguiente día
                                calendar.add(Calendar.DAY_OF_MONTH, 1)
                            }
                            
                            lastSevenDaysIncome = lastSevenDaysData
                            lastSevenDaysExpenses = lastSevenDaysExpensesData
                        }
                    }
                    
                    // Gráfica de líneas para la tendencia
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ChartBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            // Evitar división por cero o datos vacíos
                            if (lastSevenDaysIncome.isNotEmpty() && lastSevenDaysExpenses.isNotEmpty()) {
                                // Encontrar el valor máximo para la escala
                                val allValues = lastSevenDaysIncome.map { it.second } + lastSevenDaysExpenses.map { it.second }
                                val maxValue = allValues.maxOrNull() ?: 1.0
                                
                                // Dibujar ejes
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, canvasHeight),
                                    end = Offset(canvasWidth, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                // Dibujar líneas de cuadrícula horizontales
                                val gridCount = 4
                                for (i in 1..gridCount) {
                                    val y = canvasHeight - (i * (canvasHeight / gridCount))
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Dibujar líneas verticales para cada día con actividad
                                val dayWidth = canvasWidth / (lastSevenDaysIncome.size - 1)
                                for (i in 1 until lastSevenDaysIncome.size) {
                                    val x = i * dayWidth
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Factor de escala
                                val scaleFactor = 0.85f
                                
                                // Dibujar línea de ingresos
                                val incomePoints = lastSevenDaysIncome.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / (lastSevenDaysIncome.size - 1))
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor)
                                    Offset(x, y.toFloat())
                                }
                                
                                for (i in 0 until incomePoints.size - 1) {
                                    drawLine(
                                        color = ChartIncomeColor,
                                        start = incomePoints[i],
                                        end = incomePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para ingresos
                                incomePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartIncomeColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                                
                                // Dibujar línea de gastos
                                val expensePoints = lastSevenDaysExpenses.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / (lastSevenDaysExpenses.size - 1))
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor)
                                    Offset(x, y.toFloat())
                                }
                                
                                for (i in 0 until expensePoints.size - 1) {
                                    drawLine(
                                        color = ChartExpenseColor,
                                        start = expensePoints[i],
                                        end = expensePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para gastos
                                expensePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartExpenseColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                            }
                        }
                        
                        // Leyenda para la gráfica de tendencia
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Formato para las fechas abreviadas
                            val dayFormat = SimpleDateFormat("dd/MM", Locale("es", "ES"))
                            
                            // Mostrar fechas como eje X
                            if (lastSevenDaysIncome.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    lastSevenDaysIncome.forEachIndexed { index, (date, _) ->
                                        Text(
                                            text = dayFormat.format(date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Leyenda específica para la gráfica de tendencia
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Leyenda de Ingresos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartIncomeColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        
                        // Leyenda de Gastos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartExpenseColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Sección de la semana actual
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Calcular el rango de fechas de la semana usando la misma lógica que los repositorios
                    val firstDayOfWeek = application.getFirstDayOfWeek().collectAsState(initial = Calendar.MONDAY).value
                    val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
                    
                    val weekRangeText = "${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"
                    
                    Text(
                        text = "Estadísticas de la Semana ($weekRangeText)",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Resumen principal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Ingresos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(weekIncome),
                                style = MaterialTheme.typography.titleMedium,
                                color = GreenAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Gastos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(weekExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                color = RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Neto
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Neto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(weekNet),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (weekNet >= 0) GreenAccent else RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Detalles adicionales
                    FinancialDetail(
                        label = "Gastos en combustible",
                        amount = weekFuelExpenses,
                        icon = Icons.Filled.LocalGasStation
                    )
                    
                    FinancialDetail(
                        label = "Otros gastos",
                        amount = weekExpenses - weekFuelExpenses,
                        icon = Icons.Filled.Receipt
                    )
                    
                    // Añadir sección de métodos de pago para la semana
                    if (weekPaymentMethodBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Desglose por Métodos de Pago",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mostrar cada método de pago y su valor
                        weekPaymentMethodBreakdown.entries.sortedByDescending { it.value }.forEach { (method, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (method.lowercase(Locale.getDefault())) {
                                            "efectivo" -> Icons.Filled.Money
                                            "tarjeta" -> Icons.Filled.CreditCard
                                            else -> Icons.Filled.Payment
                                        },
                                        contentDescription = method,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                
                                Text(
                                    text = formatCurrency(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Añadir gráfica de tendencia de las últimas 7 semanas
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tendencia de las últimas 7 semanas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Variables para almacenar datos históricos de semanas
                    var lastSevenWeeksIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    var lastSevenWeeksExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    
                    // Cargar datos de las últimas 7 semanas
                    LaunchedEffect(selectedDate) {
                        withContext(Dispatchers.IO) {
                            val lastSevenWeeksIncomeData = mutableListOf<Pair<Date, Double>>()
                            val lastSevenWeeksExpensesData = mutableListOf<Pair<Date, Double>>()
                            
                            // Calendario para iterar por las últimas 7 semanas
                            val calendar = Calendar.getInstance().apply { time = selectedDate }
                            
                            // Retroceder al inicio (6 semanas atrás)
                            calendar.add(Calendar.WEEK_OF_YEAR, -6)
                            
                            // Recopilar datos de las últimas 7 semanas
                            repeat(7) {
                                val weekStart = calendar.time
                                // Guardar el inicio de semana para la etiqueta
                                val weekLabel = calendar.time
                                
                                // Calcular ingresos y gastos para esta semana
                                val weekIncome = taxiRideViewModel.getWeekIncomeForDate(weekStart)
                                val weekExpenses = expenseViewModel.getWeekExpensesForDate(weekStart)
                                
                                lastSevenWeeksIncomeData.add(Pair(weekLabel, weekIncome))
                                lastSevenWeeksExpensesData.add(Pair(weekLabel, weekExpenses))
                                
                                // Avanzar a la siguiente semana
                                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                            }
                            
                            lastSevenWeeksIncome = lastSevenWeeksIncomeData
                            lastSevenWeeksExpenses = lastSevenWeeksExpensesData
                        }
                    }
                    
                    // Gráfica de líneas para la tendencia semanal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ChartBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            // Evitar división por cero o datos vacíos
                            if (lastSevenWeeksIncome.isNotEmpty() && lastSevenWeeksExpenses.isNotEmpty()) {
                                // Encontrar el valor máximo para la escala
                                val allValues = lastSevenWeeksIncome.map { it.second } + lastSevenWeeksExpenses.map { it.second }
                                val maxValue = allValues.maxOrNull() ?: 1.0
                                
                                // Dibujar ejes
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, canvasHeight),
                                    end = Offset(canvasWidth, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                // Dibujar líneas de cuadrícula horizontales
                                val gridCount = 4
                                for (i in 1..gridCount) {
                                    val y = canvasHeight - (i * (canvasHeight / gridCount))
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Dibujar líneas verticales para cada semana
                                val weekWidth = canvasWidth / 7
                                for (i in 1..6) {
                                    val x = i * weekWidth
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Factor de escala
                                val scaleFactor = 0.85f
                                
                                // Dibujar línea de ingresos
                                val incomePoints = lastSevenWeeksIncome.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / 6)
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor).toFloat()
                                    Offset(x, y)
                                }
                                
                                for (i in 0 until incomePoints.size - 1) {
                                    drawLine(
                                        color = ChartIncomeColor,
                                        start = incomePoints[i],
                                        end = incomePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para ingresos
                                incomePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartIncomeColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                                
                                // Dibujar línea de gastos
                                val expensePoints = lastSevenWeeksExpenses.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / 6)
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor).toFloat()
                                    Offset(x, y)
                                }
                                
                                for (i in 0 until expensePoints.size - 1) {
                                    drawLine(
                                        color = ChartExpenseColor,
                                        start = expensePoints[i],
                                        end = expensePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para gastos
                                expensePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartExpenseColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                            }
                        }
                        
                        // Leyenda para la gráfica de tendencia
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Formato para las fechas abreviadas
                            val weekFormat = SimpleDateFormat("dd/MM", Locale("es", "ES"))
                            
                            // Mostrar fechas como eje X
                            if (lastSevenWeeksIncome.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    lastSevenWeeksIncome.forEachIndexed { index, (date, _) ->
                                        Text(
                                            text = weekFormat.format(date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Leyenda específica para la gráfica de tendencia
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Leyenda de Ingresos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartIncomeColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        
                        // Leyenda de Gastos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartExpenseColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Sección del mes actual
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Calcular el rango de fechas del mes (similar a getMonthIncomeForDate)
                    val calendar = Calendar.getInstance().apply { time = selectedDate }
                    
                    // Ajustar al primer día del mes
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val startOfMonth = calendar.time
                    
                    // Ajustar al último día del mes
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    val endOfMonth = calendar.time
                    
                    val monthRangeText = "${dateFormat.format(startOfMonth)} - ${dateFormat.format(endOfMonth)}"
                    
                    Text(
                        text = "Estadísticas del Mes ($monthRangeText)",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Resumen principal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Ingresos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(monthIncome),
                                style = MaterialTheme.typography.titleMedium,
                                color = GreenAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Gastos
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(monthExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                color = RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Neto
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Neto",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = formatCurrency(monthNet),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (monthNet >= 0) GreenAccent else RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Detalles adicionales
                    FinancialDetail(
                        label = "Gastos en combustible",
                        amount = monthFuelExpenses,
                        icon = Icons.Filled.LocalGasStation
                    )
                    
                    FinancialDetail(
                        label = "Otros gastos",
                        amount = monthExpenses - monthFuelExpenses,
                        icon = Icons.Filled.Receipt
                    )
                    
                    // Añadir sección de métodos de pago para el mes
                    if (monthPaymentMethodBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Desglose por Métodos de Pago",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mostrar cada método de pago y su valor
                        monthPaymentMethodBreakdown.entries.sortedByDescending { it.value }.forEach { (method, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (method.lowercase(Locale.getDefault())) {
                                            "efectivo" -> Icons.Filled.Money
                                            "tarjeta" -> Icons.Filled.CreditCard
                                            else -> Icons.Filled.Payment
                                        },
                                        contentDescription = method,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                
                                Text(
                                    text = formatCurrency(amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Añadir gráfica de tendencia de los últimos 7 meses
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tendencia de los últimos 7 meses",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Variables para almacenar datos históricos de meses
                    var lastSevenMonthsIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    var lastSevenMonthsExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
                    
                    // Cargar datos de los últimos 7 meses
                    LaunchedEffect(selectedDate) {
                        withContext(Dispatchers.IO) {
                            val lastSevenMonthsIncomeData = mutableListOf<Pair<Date, Double>>()
                            val lastSevenMonthsExpensesData = mutableListOf<Pair<Date, Double>>()
                            
                            // Calendario para iterar por los últimos 7 meses
                            val calendar = Calendar.getInstance().apply { time = selectedDate }
                            
                            // Retroceder al inicio (6 meses atrás)
                            calendar.add(Calendar.MONTH, -6)
                            
                            // Recopilar datos de los últimos 7 meses
                            repeat(7) {
                                val monthStart = calendar.time
                                // Guardar el inicio de mes para la etiqueta
                                val monthLabel = calendar.time
                                
                                // Calcular ingresos y gastos para este mes
                                val monthIncome = taxiRideViewModel.getMonthIncomeForDate(monthStart)
                                val monthExpenses = expenseViewModel.getMonthExpensesForDate(monthStart)
                                
                                lastSevenMonthsIncomeData.add(Pair(monthLabel, monthIncome))
                                lastSevenMonthsExpensesData.add(Pair(monthLabel, monthExpenses))
                                
                                // Avanzar al siguiente mes
                                calendar.add(Calendar.MONTH, 1)
                            }
                            
                            lastSevenMonthsIncome = lastSevenMonthsIncomeData
                            lastSevenMonthsExpenses = lastSevenMonthsExpensesData
                        }
                    }
                    
                    // Gráfica de líneas para la tendencia mensual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(8.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ChartBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            // Evitar división por cero o datos vacíos
                            if (lastSevenMonthsIncome.isNotEmpty() && lastSevenMonthsExpenses.isNotEmpty()) {
                                // Encontrar el valor máximo para la escala
                                val allValues = lastSevenMonthsIncome.map { it.second } + lastSevenMonthsExpenses.map { it.second }
                                val maxValue = allValues.maxOrNull() ?: 1.0
                                
                                // Dibujar ejes
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, canvasHeight),
                                    end = Offset(canvasWidth, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                drawLine(
                                    color = ChartGridColor.copy(alpha = 0.7f),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, canvasHeight),
                                    strokeWidth = 2f
                                )
                                
                                // Dibujar líneas de cuadrícula horizontales
                                val gridCount = 4
                                for (i in 1..gridCount) {
                                    val y = canvasHeight - (i * (canvasHeight / gridCount))
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Dibujar líneas verticales para cada mes
                                val monthWidth = canvasWidth / 7
                                for (i in 1..6) {
                                    val x = i * monthWidth
                                    drawLine(
                                        color = ChartGridColor.copy(alpha = 0.3f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 1f
                                    )
                                }
                                
                                // Factor de escala
                                val scaleFactor = 0.85f
                                
                                // Dibujar línea de ingresos
                                val incomePoints = lastSevenMonthsIncome.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / 6)
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor).toFloat()
                                    Offset(x, y)
                                }
                                
                                for (i in 0 until incomePoints.size - 1) {
                                    drawLine(
                                        color = ChartIncomeColor,
                                        start = incomePoints[i],
                                        end = incomePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para ingresos
                                incomePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartIncomeColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                                
                                // Dibujar línea de gastos
                                val expensePoints = lastSevenMonthsExpenses.mapIndexed { index, (_, value) ->
                                    val x = index * (canvasWidth / 6)
                                    val y = canvasHeight - ((value / maxValue) * canvasHeight * scaleFactor).toFloat()
                                    Offset(x, y)
                                }
                                
                                for (i in 0 until expensePoints.size - 1) {
                                    drawLine(
                                        color = ChartExpenseColor,
                                        start = expensePoints[i],
                                        end = expensePoints[i + 1],
                                        strokeWidth = 5f
                                    )
                                }
                                
                                // Dibujar puntos para gastos
                                expensePoints.forEach { point ->
                                    drawCircle(
                                        color = ChartExpenseColor,
                                        radius = 8f,
                                        center = point
                                    )
                                }
                            }
                        }
                        
                        // Leyenda para la gráfica de tendencia
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Formato para las fechas abreviadas (mes/año)
                            val monthFormat = SimpleDateFormat("MM/yy", Locale("es", "ES"))
                            
                            // Mostrar fechas como eje X
                            if (lastSevenMonthsIncome.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    lastSevenMonthsIncome.forEachIndexed { index, (date, _) ->
                                        Text(
                                            text = monthFormat.format(date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Leyenda específica para la gráfica de tendencia
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Leyenda de Ingresos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartIncomeColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        
                        // Leyenda de Gastos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(
                                    color = ChartExpenseColor,
                                    radius = size.minDimension / 2
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Espacio adicional al final
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/*
@Composable
fun StatisticsSection(
    title: String,
    incomes: Double,
    expenses: Double,
    net: Double,
    rideCount: Int,
    expenseCount: Int,
    fuelExpenses: Double,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resumen principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ingresos
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatCurrency(incomes),
                        style = MaterialTheme.typography.titleMedium,
                        color = GreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Gastos
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Gastos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatCurrency(expenses),
                        style = MaterialTheme.typography.titleMedium,
                        color = RedAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Neto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Neto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatCurrency(net),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (net >= 0) GreenAccent else RedAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detalles adicionales
            FinancialDetail(
                label = "Carreras realizadas",
                amount = rideCount.toDouble(),
                icon = Icons.Filled.DirectionsCar
            )
            
            FinancialDetail(
                label = "Gastos registrados",
                amount = expenseCount.toDouble(),
                icon = Icons.Filled.Receipt
            )
            
            FinancialDetail(
                label = "Gastos en combustible",
                amount = fuelExpenses,
                icon = Icons.Filled.LocalGasStation
            )
            
            if (incomes > 0 && rideCount > 0) {
                FinancialDetail(
                    label = "Ingreso promedio por carrera",
                    amount = incomes / rideCount,
                    icon = Icons.Filled.ShowChart
                )
            }
        }
    }
}
*/
