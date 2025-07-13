package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar

import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.Expense
import com.moham.taxi.ui.components.FinancialDetail
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.*
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.moham.taxi.ui.screens.SplashScreenPreloadData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, preloadData: SplashScreenPreloadData?) {
    // Obtener el contexto y la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Scope para operaciones de coroutine
    val scope = rememberCoroutineScope()
    
    // Usar los datos precargados si existen
    val initialSelectedDate = preloadData?.selectedDate ?: Date()
    var selectedDate by rememberSaveable(key = "selected_date") { 
        mutableStateOf(initialSelectedDate)
    }
    // Estados para los datos financieros
    var dateIncome by remember { mutableStateOf(preloadData?.dateIncome ?: 0.0) }
    var weekIncome by remember { mutableStateOf(preloadData?.weekIncome ?: 0.0) }
    var monthIncome by remember { mutableStateOf(preloadData?.monthIncome ?: 0.0) }
    var dateExpenses by remember { mutableStateOf(preloadData?.dateExpenses ?: 0.0) }
    var weekExpenses by remember { mutableStateOf(preloadData?.weekExpenses ?: 0.0) }
    var monthExpenses by remember { mutableStateOf(preloadData?.monthExpenses ?: 0.0) }
    var dateNet by remember { mutableStateOf(preloadData?.dateNet ?: 0.0) }
    var weekNet by remember { mutableStateOf(preloadData?.weekNet ?: 0.0) }
    var monthNet by remember { mutableStateOf(preloadData?.monthNet ?: 0.0) }
    var rideCount by remember { mutableStateOf(preloadData?.rideCount ?: 0) }
    var weekRideCount by remember { mutableStateOf(preloadData?.weekRideCount ?: 0) }
    var monthRideCount by remember { mutableStateOf(preloadData?.monthRideCount ?: 0) }
    var expenseCount by remember { mutableStateOf(preloadData?.expenseCount ?: 0) }
    var weekExpenseCount by remember { mutableStateOf(preloadData?.weekExpenseCount ?: 0) }
    var monthExpenseCount by remember { mutableStateOf(preloadData?.monthExpenseCount ?: 0) }
    var fuelExpenses by remember { mutableStateOf(preloadData?.fuelExpenses ?: 0.0) }
    var weekFuelExpenses by remember { mutableStateOf(preloadData?.weekFuelExpenses ?: 0.0) }
    var monthFuelExpenses by remember { mutableStateOf(preloadData?.monthFuelExpenses ?: 0.0) }
    var paymentMethodBreakdown by remember { mutableStateOf(preloadData?.paymentMethodBreakdown ?: emptyMap()) }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEEE", Locale("es", "ES")) }
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    val dayOfWeek = remember(selectedDate) { 
        dayOfWeekFormat.format(selectedDate).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() 
        } 
    }
    
    // Estado para controlar el diálogo de selección de fecha
    var showDatePicker by remember { mutableStateOf(false) }
    
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
    
    // Función para normalizar una fecha (quitar la parte de hora)
    fun normalizeDate(date: Date): Date {
        val calendar = Calendar.getInstance().apply { 
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    // Verificar si es la fecha de hoy
    val isToday = remember(selectedDate) {
        val today = normalizeDate(Date())
        val selected = normalizeDate(selectedDate)
        today == selected
    }
    
    // Verificar si es la fecha de ayer
    val isYesterday = remember(selectedDate) {
        val yesterday = Calendar.getInstance().apply { 
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val selectedCal = Calendar.getInstance().apply { time = selectedDate }
        yesterday.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
    }
    
    var refreshKey by remember { mutableStateOf(0) }
    
    // Función para cambiar de día manualmente
    fun changeDay(daysToAdd: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        selectedDate = calendar.time
        refreshKey++
        // Guardar la fecha seleccionada en DataStore
        scope.launch {
            application.saveSelectedDate(selectedDate)
        }
    }
    
    // Recargar datos cuando cambie la fecha seleccionada
    LaunchedEffect(selectedDate, refreshKey) {
        val normalizedSelectedDate = normalizeDate(selectedDate)
        
        try {
            withContext(Dispatchers.IO) {
                dateIncome = taxiRideViewModel.getIncomeForDate(normalizedSelectedDate)
                dateExpenses = expenseViewModel.getExpensesTotalForDate(normalizedSelectedDate)
                dateNet = dateIncome - dateExpenses
                rideCount = taxiRideViewModel.getRideCountForDate(normalizedSelectedDate)
                expenseCount = expenseViewModel.getExpenseCountForDate(normalizedSelectedDate)
                fuelExpenses = expenseViewModel.getFuelExpensesForDate(normalizedSelectedDate)
                paymentMethodBreakdown = taxiRideViewModel.getIncomeByPaymentMethodForDate(normalizedSelectedDate)
                weekIncome = taxiRideViewModel.getWeekIncomeForDate(normalizedSelectedDate)
                weekExpenses = expenseViewModel.getWeekExpensesForDate(normalizedSelectedDate)
                weekNet = weekIncome - weekExpenses
                weekRideCount = taxiRideViewModel.getWeekRideCountForDate(normalizedSelectedDate)
                weekExpenseCount = expenseViewModel.getWeekExpenseCountForDate(normalizedSelectedDate)
                weekFuelExpenses = expenseViewModel.getCurrentWeekFuelExpenses()
                monthIncome = taxiRideViewModel.getMonthIncomeForDate(normalizedSelectedDate)
                monthExpenses = expenseViewModel.getMonthExpensesForDate(normalizedSelectedDate)
                monthNet = monthIncome - monthExpenses
                monthRideCount = taxiRideViewModel.getMonthRideCountForDate(normalizedSelectedDate)
                monthExpenseCount = expenseViewModel.getMonthExpenseCountForDate(normalizedSelectedDate)
                monthFuelExpenses = expenseViewModel.getMonthFuelExpenses()
            }
        } catch (e: Exception) {
            println("ERROR en la carga de datos financieros: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Función para navegar a formularios con la fecha seleccionada
    fun navigateWithDate(screen: AppScreens) {
        val timestamp = selectedDate.time
        val route = when (screen) {
            AppScreens.TaxiRideForm -> screen.createRouteWithDateAndId(timestamp)
            AppScreens.ExpenseForm -> screen.createRouteWithDateAndId(timestamp)
            else -> screen.createRouteWithDate(timestamp)
        }
        
        navController.navigate(route) {
            // Evitar múltiples copias de la misma pantalla en la pila
            launchSingleTop = true
            // Restaurar el estado si la pantalla ya existe en la pila
            restoreState = true
            // Limpiar la pila de navegación hasta la pantalla principal
            popUpTo(AppScreens.Home.route) {
                saveState = true
                inclusive = false
            }
        }
    }
    
    // Diálogo de selección de fecha
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = normalizeDate(selectedDate).time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Date(millis)
                            selectedDate = normalizeDate(newDate)
                            refreshKey++
                            scope.launch {
                                application.saveSelectedDate(selectedDate)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CalendarBackground, // Fondo azul gris oscuro
                    titleContentColor = CalendarText, // Texto blanco
                    headlineContentColor = CalendarText, // Texto blanco
                    weekdayContentColor = CalendarText, // Texto blanco
                    subheadContentColor = CalendarText, // Texto blanco
                    yearContentColor = CalendarText, // Texto blanco
                    currentYearContentColor = CalendarAccent, // Azul acero
                    selectedYearContainerColor = CalendarAccent, // Azul acero
                    selectedYearContentColor = CalendarText, // Texto blanco
                    selectedDayContainerColor = CalendarAccent, // Azul acero
                    selectedDayContentColor = CalendarText, // Texto blanco
                    todayContentColor = CalendarAccent, // Azul acero
                    todayDateBorderColor = CalendarAccent, // Azul acero
                    dayContentColor = CalendarText, // Texto blanco
                    dividerColor = CalendarText.copy(alpha = 0.2f) // Texto blanco con transparencia
                )
            )
        }
    }
    
    var showComingSoonDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Logo minimalista
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF4A90E2).copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        radius = 50f
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                 imageVector = Icons.Filled.DirectionsCar,
                                 contentDescription = "Logo",
                                 tint = Color.White,
                                 modifier = Modifier.size(18.dp)
                             )
                        }
                        
                        // Título con tipografía elegante
                        Column {
                            Text(
                                "GESTIÓN TAXI",
                                fontWeight = FontWeight.Light,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                            // Línea decorativa sutil
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                             colors = listOf(
                                                 Color.White,
                                                 Color.Transparent
                                             )
                                         )
                                    )
                            )
                        }
                    }
                },
                navigationIcon = {}, // Sin botón de atrás
                actions = {
                    IconButton(
                        onClick = { 
                            navController.navigate(AppScreens.Settings.route) {
                                popUpTo(AppScreens.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Ajustes",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(DarkBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botones de acción principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Añadir Ingreso
                ModernButton(
                    text = "Añadir Ingreso",
                    icon = Icons.Filled.AttachMoney,
                    colors = listOf(
                        IncomeButtonColor,
                        IncomeButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navigateWithDate(AppScreens.TaxiRideForm) },
                    modifier = Modifier.weight(1f)
                )
                
                // Botón Añadir Gasto
                ModernButton(
                    text = "Añadir Gasto",
                    icon = Icons.Filled.MoneyOff,
                    colors = listOf(
                        ExpenseButtonColor,
                        ExpenseButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navigateWithDate(AppScreens.ExpenseForm) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Botones secundarios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Carreras
                ModernButton(
                    text = "Carreras",
                    icon = Icons.Filled.DirectionsCar,
                    colors = listOf(
                        RidesButtonColor,
                        RidesButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navigateWithDate(AppScreens.TaxiRideList) },
                    modifier = Modifier.weight(1f)
                )
                
                // Botón de Estadísticas
                ModernButton(
                    text = "Estadísticas",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    colors = listOf(
                        StatsButtonColor,
                        StatsButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navController.navigate(AppScreens.Statistics.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Nuevos botones de funciones próximas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de Facturas
                ModernButton(
                    text = "Facturas",
                    icon = Icons.Filled.Description,
                    colors = listOf(
                        InvoiceButtonColor,
                        InvoiceButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navController.navigate(AppScreens.Invoice.route) },
                    modifier = Modifier.weight(1f)
                )
                
                // Botón de Precio
                ModernButton(
                    text = "Precio",
                    icon = Icons.Filled.Euro,
                    colors = listOf(
                        PriceButtonColor,
                        PriceButtonColor.copy(alpha = 0.8f)
                    ),
                    onClick = { navController.navigate(AppScreens.Price.route) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (showComingSoonDialog) {
                AlertDialog(
                    onDismissRequest = { showComingSoonDialog = false },
                    title = {
                        Text(
                            text = "Funciones próximamente",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = "Esta funcionalidad estará disponible en próximas actualizaciones.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showComingSoonDialog = false }
                        ) {
                            Text("Aceptar")
                        }
                    }
                )
            }
            
            // Fecha con calendario mejorado - Diseño más moderno
            Card(
                colors = CardDefaults.cardColors(containerColor = CalendarBackground),
                shape = RoundedCornerShape(24.dp), // Bordes más redondeados
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Mayor elevación
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    CalendarBackground,
                                    CalendarBackground.copy(alpha = 0.95f),
                                    CalendarBackground.copy(alpha = 0.9f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                ) {
                    // Cabecera con día actual
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dayOfWeek,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CalendarText
                            )
                            Text(
                                text = formattedDate,
                                fontSize = 16.sp,
                                color = CalendarText.copy(alpha = 0.8f)
                            )
                            
                            // Indicador si no es la fecha actual
                            if (!isToday) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            Warning.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Advertencia",
                                        tint = Warning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Fecha diferente a la actual",
                                        fontSize = 12.sp,
                                        color = Warning,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón para ver calendario completo - Diseño moderno
                            Surface(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = CalendarAccent.copy(alpha = 0.2f),
                                tonalElevation = 4.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = "Ver calendario",
                                        tint = CalendarAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Selector de fechas horizontal (siempre visible)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Cargar el primer día de la semana configurado
                        var firstDayOfWeek by remember { mutableStateOf(2) } // Por defecto, lunes (2)
                        
                        // Cargar la configuración del primer día de la semana
                        LaunchedEffect(Unit) {
                            // Usar collectAsState en lugar de first() para estar al tanto de cambios
                            application.getFirstDayOfWeek().collect { newFirstDay ->
                                firstDayOfWeek = newFirstDay
                            }
                        }
                        
                        // Calcular el inicio de la semana del día seleccionado usando la misma lógica que los cálculos de ingresos
                        val selectedWeekRange = remember(selectedDate, firstDayOfWeek) {
                            com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
                        }
                        
                        val selectedWeekStartCalendar = remember(selectedWeekRange) {
                            Calendar.getInstance().apply {
                                time = selectedWeekRange.first
                            }
                        }
                        
                        // Mostrar los 7 días de la semana del día seleccionado
                        for (dayOffset in 0..6) {
                            val calendar = Calendar.getInstance()
                            calendar.time = selectedWeekStartCalendar.time
                            calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                            
                            // Verificar si este día está dentro del rango de la semana calculada
                            val dayTime = calendar.time
                            val isWithinWeekRange = dayTime >= selectedWeekRange.first && dayTime <= selectedWeekRange.second
                            
                            // Solo mostrar días que están dentro del rango de la semana
                            if (!isWithinWeekRange && dayOffset > 0) {
                                break
                            }
                            
                            val isSelected = calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { 
                                time = selectedDate 
                            }.get(Calendar.DAY_OF_YEAR) &&
                            calendar.get(Calendar.YEAR) == Calendar.getInstance().apply { 
                                time = selectedDate 
                            }.get(Calendar.YEAR)
                            
                            val isToday = calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) &&
                                       calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
                            
                            val dayFormat = SimpleDateFormat("dd", Locale("es"))
                            val weekDayFormat = SimpleDateFormat("E", Locale("es"))
                            
                            val day = dayFormat.format(calendar.time)
                            val weekDay = weekDayFormat.format(calendar.time).take(1).uppercase()
                            
                            // Estado para almacenar los ingresos del día
                            var dayIncome by remember { mutableStateOf(0.0) }
                            val calendarTime = calendar.time
                            
                            // Obtener ingresos para este día usando LaunchedEffect
                            LaunchedEffect(calendarTime) {
                                dayIncome = taxiRideViewModel.getIncomeForDate(calendarTime)
                            }
                            
                            Surface(
                                onClick = {
                                    selectedDate = normalizeDate(calendar.time)
                                    refreshKey++
                                    // Guardar la fecha seleccionada
                                    scope.launch {
                                        application.saveSelectedDate(selectedDate)
                                    }
                                },
                                modifier = Modifier.animateContentSize(),
                                shape = RoundedCornerShape(16.dp), // Bordes más redondeados
                                color = when {
                                    isSelected -> CalendarAccent
                                    isToday -> CalendarAccent.copy(alpha = 0.7f)
                                    else -> CalendarDayBackground.copy(alpha = 0.6f)
                                },
                                tonalElevation = when {
                                    isSelected -> 6.dp
                                    isToday -> 4.dp
                                    else -> 2.dp
                                },
                                shadowElevation = when {
                                    isSelected -> 4.dp
                                    isToday -> 2.dp
                                    else -> 1.dp
                                }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                Text(
                                    text = weekDay,
                                    fontSize = 12.sp,
                                    color = when {
                                        isSelected || isToday -> CalendarText
                                        else -> CalendarDayText
                                    }
                                )
                                Text(
                                    text = day,
                                    fontSize = 18.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected || isToday -> CalendarText
                                        else -> CalendarDayText
                                    }
                                )
                                Text(
                                    text = if (dayIncome > 0) "${dayIncome.toInt()}€" else "-",
                                    fontSize = 12.sp,
                                    fontWeight = if (dayIncome > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        dayIncome > 0 && (isSelected || isToday) -> CalendarText.copy(alpha = 0.9f)
                                        dayIncome > 0 -> GreenAccent
                                        isSelected || isToday -> CalendarText.copy(alpha = 0.6f)
                                        else -> CalendarDayText.copy(alpha = 0.6f)
                                    }
                                )
                                }
                            }
                        }
                    }
                }
            }
            
            // Tarjeta de ingresos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = IncomeButtonColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    IncomeButtonColor, // Verde bosque principal
                                    IncomeButtonColor.copy(alpha = 0.9f)  // Verde ligeramente más oscuro
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ingresos",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = "Ingresos",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Ingresos del día
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "Día",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Día",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(dateIncome),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (dateIncome > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                contentDescription = "Tendencia",
                                tint = if (dateIncome > 0) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ingresos de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Semana",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Semana",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(weekIncome),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (weekIncome > dateIncome * 7) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                contentDescription = "Tendencia",
                                tint = if (weekIncome > dateIncome * 7) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ingresos del mes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Mes",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Mes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(monthIncome),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (monthIncome > weekIncome * 4) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                contentDescription = "Tendencia",
                                tint = if (monthIncome > weekIncome * 4) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Tarjeta de gastos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ExpenseButtonColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ExpenseButtonColor, // Rojo vino principal
                                    ExpenseButtonColor.copy(alpha = 0.9f)  // Rojo ligeramente más oscuro
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gastos",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.MoneyOff,
                            contentDescription = "Gastos",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Gastos del día
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "Día",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Día",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(dateExpenses),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (dateExpenses > 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = "Tendencia",
                                tint = if (dateExpenses > 0) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Gastos de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Semana",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Semana",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(weekExpenses),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (weekExpenses > dateExpenses * 7) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = "Tendencia",
                                tint = if (weekExpenses > dateExpenses * 7) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Gastos del mes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Mes",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Mes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatCurrency(monthExpenses),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (monthExpenses > weekExpenses * 4) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = "Tendencia",
                                tint = if (monthExpenses > weekExpenses * 4) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Espacio adicional al final para asegurar que todo sea visible
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModernButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val isPressed = interactionSource.collectIsPressedAsState()
    
    // Animación de escala al presionar
    val scale by animateFloatAsState(
        targetValue = if (isPressed.value) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    // Animación de elevación
    val elevation by animateDpAsState(
        targetValue = if (isPressed.value) 2.dp else 6.dp,
        animationSpec = tween(durationMillis = 150),
        label = "elevation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp) // Altura ligeramente reducida para un aspecto más moderno
            .scale(scale) // Aplicar escala animada
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Quitamos el ripple por defecto para usar nuestras animaciones
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp), // Bordes más redondeados
        color = colors[0],
        tonalElevation = 0.dp,
        shadowElevation = elevation // Elevación animada
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        tileMode = TileMode.Clamp
                    )
                )
        ) {
            // Efecto de brillo en la esquina
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(0f, 0f),
                            radius = 400f
                        )
                    )
            )
            
            // Contenido del botón
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold, // Menos pesado para un aspecto más moderno
                    fontSize = 12.sp, // Tamaño más pequeño para evitar que se corten las letras
                    color = Color.White,
                    letterSpacing = 0.5.sp, // Mejor espaciado de letras
                    maxLines = 1 // Asegurar que el texto esté en una sola línea
                )
            }
        }
    }
}
