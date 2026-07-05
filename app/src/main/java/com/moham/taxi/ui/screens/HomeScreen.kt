package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.SyncAlt
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Expense
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.FinancialDetail
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.*
import com.moham.taxi.ui.theme.IncomeWidgetBackground
import com.moham.taxi.ui.theme.ExpenseWidgetBackground
import com.moham.taxi.ui.theme.IncomeIconColor
import com.moham.taxi.ui.theme.ExpenseIconColor
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.utils.DateUtils
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
    
    val privacyPolicyAccepted by application.isPrivacyPolicyAccepted().collectAsState(initial = true)
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val enabled = application.isOnlineBackupEnabled().first()
                    val signedIn = application.googleDriveAuthManager.isSignedIn()
                    val pending = application.isPendingBackup().first()
                    if (enabled && signedIn && pending) {
                        application.scheduleOnlineBackupImmediate()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
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
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    val dayOfWeek = remember(selectedDate) { 
        dayOfWeekFormat.format(selectedDate).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } 
    }
    
    // Estado para controlar el diálogo de selección de fecha
    var showDatePicker by remember { mutableStateOf(false) }
    var showCalendarOverlay by remember { mutableStateOf(false) }

    var showTargetDialog by remember { mutableStateOf(false) }
    var targetInput by rememberSaveable(key = "daily_target_input") { mutableStateOf("") }
    var targetInputError by remember { mutableStateOf(false) }
    
    val dailyChallengeEnabled by application.isDailyChallengeEnabled().collectAsState(initial = false)
    val oldVersionEnabled by application.isOldVersionEnabled().collectAsState(initial = false)
    val modernThemeEnabled by application.isModernThemeEnabled().collectAsState(initial = false)
    
    // ViewModels
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    val targetAmount by taxiRideViewModel.dailyTarget.collectAsState(initial = null)
    
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    val recentActivity by remember(taxiRideViewModel, expenseViewModel, selectedDate) {
        kotlinx.coroutines.flow.combine(
            taxiRideViewModel.allTaxiRides,
            expenseViewModel.allExpenses
        ) { rides, expenses ->
            val activities = mutableListOf<ActivityItem>()
            val dayRange = com.moham.taxi.utils.DateUtils.getDayRange(selectedDate)
            val dayRides = rides.filter { it.date.time >= dayRange.first.time && it.date.time <= dayRange.second.time }
            val dayExpenses = expenses.filter { it.date.time >= dayRange.first.time && it.date.time <= dayRange.second.time }
            
            dayRides.forEach { ride ->
                val hasRoute = ride.origin.isNotBlank() || ride.destination.isNotBlank()
                val (title, description) = if (hasRoute) {
                    val routeStr = if (ride.origin.isNotBlank() && ride.destination.isNotBlank()) {
                        "${ride.origin} → ${ride.destination}"
                    } else {
                        ride.origin.ifBlank { ride.destination }
                    }
                    Pair(routeStr, "")
                } else {
                    val platformStr = ride.servicePlatform ?: "Directo"
                    Pair(platformStr, "")
                }
                activities.add(
                    ActivityItem(
                        id = ride.id,
                        isIncome = true,
                        title = title,
                        description = description,
                        amount = ride.netPrice ?: ride.price,
                        date = ride.realDate
                    )
                )
            }
            dayExpenses.forEach { expense ->
                val title = when (expense.type) {
                    com.moham.taxi.data.model.ExpenseType.FUEL -> application.getString(R.string.expense_type_fuel)
                    com.moham.taxi.data.model.ExpenseType.MAINTENANCE -> application.getString(R.string.expense_type_maintenance)
                    com.moham.taxi.data.model.ExpenseType.OTHER -> if (!expense.description.isNullOrBlank()) {
                        expense.description
                    } else {
                        application.getString(R.string.expense_type_other)
                    }
                }
                val description = when (expense.type) {
                    com.moham.taxi.data.model.ExpenseType.FUEL -> expense.description ?: ""
                    com.moham.taxi.data.model.ExpenseType.MAINTENANCE -> expense.description ?: ""
                    com.moham.taxi.data.model.ExpenseType.OTHER -> if (!expense.description.isNullOrBlank()) {
                        application.getString(R.string.expense_type_other)
                    } else {
                        ""
                    }
                }
                activities.add(
                    ActivityItem(
                        id = expense.id,
                        isIncome = false,
                        title = title,
                        description = description,
                        amount = expense.amount,
                        date = expense.realDate
                    )
                )
            }
            activities.sortedWith(compareByDescending<ActivityItem> { it.date }.thenByDescending { it.id }).take(5)
        }
    }.collectAsState(initial = emptyList())
    
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
    
    var showDateWarningNotification by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        if (!isToday && !oldVersionEnabled) {
            showDateWarningNotification = true
            kotlinx.coroutines.delay(4000)
            showDateWarningNotification = false
        } else {
            showDateWarningNotification = false
        }
    }
    
    // Verificar si es la fecha de ayer
    val isYesterday = remember(selectedDate) {
        val yesterday = Calendar.getInstance().apply { 
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val selectedCal = Calendar.getInstance().apply { time = selectedDate }
        yesterday.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
        yesterday.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH)
    }
    
    val headerDateText = remember(selectedDate, isToday, dayOfWeek) {
        val locale = Locale.getDefault()
        val pattern = when (locale.language) {
            "es" -> "d 'de' MMMM"
            "fr" -> "d MMMM"
            "de" -> "d. MMMM"
            else -> "MMMM d"
        }
        val detailFormatter = SimpleDateFormat(pattern, locale)
        val formattedDetail = detailFormatter.format(selectedDate)
        
        val prefix = if (isToday) {
            when (locale.language) {
                "es" -> "Hoy"
                "fr" -> "Aujourd'hui"
                "de" -> "Heute"
                else -> "Today"
            }
        } else {
            dayOfWeek
        }
        "$prefix, $formattedDetail"
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
    
    // Efecto para detectar cuando se regresa a la pantalla y forzar actualización
    LaunchedEffect(Unit) {
        val navBackStackEntry = navController.currentBackStackEntry
        navBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_data")?.observeForever { shouldRefresh ->
            if (shouldRefresh == true) {
                refreshKey++
                navBackStackEntry.savedStateHandle.set("refresh_data", false)
            }
        }
    }
    
    // Recargar datos cuando cambie la fecha seleccionada
    LaunchedEffect(selectedDate, refreshKey) {
        val normalizedSelectedDate = normalizeDate(selectedDate)
        println("DEBUG HOME: Cargando datos para fecha: ${dateFormat.format(normalizedSelectedDate)}")
        println("DEBUG HOME: Fecha normalizada timestamp: ${normalizedSelectedDate.time}")
        println("DEBUG HOME: refreshKey: $refreshKey")
        
        try {
            withContext(Dispatchers.IO) {
                dateIncome = taxiRideViewModel.getIncomeForDate(normalizedSelectedDate)
                dateExpenses = expenseViewModel.getExpensesTotalForDate(normalizedSelectedDate)
                println("DEBUG HOME: Gastos encontrados para la fecha: $dateExpenses")
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
        // Usar la fecha normalizada para mantener consistencia con las consultas
        val normalizedDate = normalizeDate(selectedDate)
        val timestamp = normalizedDate.time
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
            // Las animaciones se manejan en AppNavigation.kt
        }
    }
    
    // Diálogo de selección de fecha
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToUtcStartOfDayMillis(selectedDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = DateUtils.utcStartOfDayMillisToLocalDate(millis)
                            refreshKey++
                            scope.launch {
                                application.saveSelectedDate(selectedDate)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
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

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = {
                showTargetDialog = false
                targetInputError = false
            },
            title = { Text(stringResource(R.string.define_challenge)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = {
                            targetInput = it
                            targetInputError = false
                        },
                        label = { Text(stringResource(R.string.daily_goal)) },
                        isError = targetInputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (targetInputError) {
                        Text(
                            text = stringResource(R.string.enter_valid_amount),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = targetInput.replace(',', '.').toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            taxiRideViewModel.setDailyTarget(parsed)
                            showTargetDialog = false
                            targetInputError = false
                        } else {
                            targetInputError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTargetDialog = false
                        targetInputError = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Estado para controlar la navegación inferior
    var selectedNavItem by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(
                selectedItem = selectedNavItem,
                onItemSelected = { index ->
                    selectedNavItem = index
                    when (index) {
                        0 -> {} // Ya estamos en Home
                        1 -> navigateWithDate(AppScreens.TaxiRideList)
                        2 -> navController.navigate(AppScreens.Statistics.route)
                        3 -> {
                            // Navegar a la pantalla de Otras opciones
                            navController.navigate(AppScreens.Other.route) {
                                popUpTo(AppScreens.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Discrete Top Notification Banner
            AnimatedVisibility(
                visible = showDateWarningNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .zIndex(99f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2C2C2E).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { showDateWarningNotification = false }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.date_different_warning),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (oldVersionEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.app_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCalendarOverlay = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = headerDateText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.action_change_date),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isToday && !oldVersionEnabled) {
                        IconButton(
                            onClick = { showDateWarningNotification = !showDateWarningNotification },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = stringResource(R.string.date_different_warning),
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { 
                            navController.navigate(AppScreens.Settings.route) {
                                popUpTo(AppScreens.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ingreso Button
                QuickActionButton(
                    text = stringResource(R.string.quick_action_add_income),
                    icon = Icons.Filled.AttachMoney,
                    backgroundColor = PrimaryBlue,
                    onClick = { navigateWithDate(AppScreens.TaxiRideForm) },
                    modifier = Modifier.weight(1f)
                )
                
                // Gasto Button
                QuickActionButton(
                    text = stringResource(R.string.quick_action_add_expense),
                    icon = Icons.Filled.TrendingDown,
                    backgroundColor = AccentRed, // Using defined red for destructive
                    onClick = { navigateWithDate(AppScreens.ExpenseForm) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (oldVersionEnabled) {
                DateSelectorCard(
                    selectedDate = selectedDate,
                    dayOfWeek = dayOfWeek,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    onDateSelected = {
                        selectedDate = it
                        refreshKey++
                    },
                    onChangeDateClick = { showDatePicker = true },
                    taxiRideViewModel = taxiRideViewModel,
                    application = application,
                    normalizeDate = ::normalizeDate
                )
            }

            // Daily Challenge
            if (dailyChallengeEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (modernThemeEnabled) Color.Transparent else DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = if (modernThemeEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                 Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CenterFocusStrong, // Make sure Target is imported or use similar
                                        contentDescription = stringResource(R.string.daily_challenge),
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    stringResource(R.string.daily_challenge),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            
                            Button(
                                onClick = {
                                    targetInput = targetAmount?.toString() ?: ""
                                    showTargetDialog = true
                                },
                                 colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(stringResource(R.string.define_challenge), fontSize = 11.sp)
                            }
                        }
    
                        val target = targetAmount ?: 0.0
                        val progressValue = if (target > 0) (dateIncome / target).coerceIn(0.0, 1.0).toFloat() else 0f
                        val progressPercent = (progressValue * 100).toInt()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.progress),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$progressPercent%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
    
                        Spacer(modifier = Modifier.height(6.dp))
    
                        LinearProgressIndicator(
                            progress = progressValue,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)),
                            color = PrimaryBlue,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val diff = target - dateIncome
                        if (target > 0) {
                             Text(
                                text = if (diff > 0) stringResource(R.string.goal_remaining, formatCurrency(diff)) else stringResource(R.string.goal_exceeded, formatCurrency(-diff)),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
    
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                stringResource(R.string.income) to formatCurrency(dateIncome),
                                stringResource(R.string.goal) to formatCurrency(target),
                                stringResource(R.string.status) to if (target > 0) {
                                    if (diff > 0) stringResource(R.string.on_track) else stringResource(R.string.achieved)
                                } else {
                                    stringResource(R.string.no_goal)
                                }
                            ).forEach { (label, value) ->
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!oldVersionEnabled) {
                TodaySummaryCard(
                    income = formatCurrency(dateIncome),
                    expenses = formatCurrency(dateExpenses),
                    modernThemeEnabled = modernThemeEnabled,
                    showIcon = oldVersionEnabled
                )
            }

            ResumenCard(
                items = buildList {
                    if (oldVersionEnabled) {
                        add(
                            ResumenRow(
                                label = stringResource(R.string.tab_day),
                                icon = Icons.Filled.CalendarToday,
                                income = formatCurrency(dateIncome),
                                expenses = formatCurrency(dateExpenses)
                            )
                        )
                    }
                    add(
                        ResumenRow(
                            label = stringResource(R.string.tab_week),
                            icon = Icons.Filled.BarChart,
                            income = formatCurrency(weekIncome),
                            expenses = formatCurrency(weekExpenses)
                        )
                    )
                    add(
                        ResumenRow(
                            label = stringResource(R.string.tab_month),
                            icon = Icons.Filled.CalendarMonth,
                            income = formatCurrency(monthIncome),
                            expenses = formatCurrency(monthExpenses)
                        )
                    )
                },
                modernThemeEnabled = modernThemeEnabled,
                showIcon = oldVersionEnabled
            )
            
            RecentActivityCard(
                activities = recentActivity,
                onActivityClick = { item ->
                    val route = if (item.isIncome) {
                        AppScreens.TaxiRideDetail.createRouteWithId(item.id)
                    } else {
                        AppScreens.ExpenseDetail.createRouteWithId(item.id)
                    }
                    navController.navigate(route)
                },
                modernThemeEnabled = modernThemeEnabled,
                showIcon = oldVersionEnabled
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showCalendarOverlay && !oldVersionEnabled) {
            // Capa de fondo oscurecida (dimmed background)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showCalendarOverlay = false
                    }
            )

            // Contenedor del calendario
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Espaciador para alinear el calendario justo encima de "Añadir ingreso o gasto"
                    Spacer(modifier = Modifier.height(if (!isToday) 116.dp else 68.dp))

                    DateSelectorCard(
                        selectedDate = selectedDate,
                        dayOfWeek = dayOfWeek,
                        formattedDate = formattedDate,
                        isToday = isToday,
                        onDateSelected = {
                            selectedDate = it
                            refreshKey++
                        },
                        onChangeDateClick = {
                            showDatePicker = true
                            showCalendarOverlay = false
                        },
                        taxiRideViewModel = taxiRideViewModel,
                        application = application,
                        normalizeDate = ::normalizeDate,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Prevenir que los clics dentro de la tarjeta cierren el overlay
                        },
                        forceCardBackground = true
                    )
                }
            }
        }
    }
}

    if (!privacyPolicyAccepted) {
        AlertDialog(
            onDismissRequest = { /* No-op: No descartar al pulsar fuera */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Text(
                        text = stringResource(R.string.privacy_dialog_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.privacy_dialog_msg),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            application.savePrivacyPolicyAccepted(true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.action_accept),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/taxxipoliticaprivacidad/inicio"))
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.view_privacy_policy),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

data class ResumenRow(
    val label: String,
    val icon: ImageVector,
    val income: String,
    val expenses: String
)

@Composable
fun ResumenCard(
    items: List<ResumenRow>,
    modernThemeEnabled: Boolean = false,
    showIcon: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (modernThemeEnabled) Color.Transparent else DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (modernThemeEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    Icon(
                        imageVector = Icons.Filled.Balance,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.home_summary_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(0.45f))
                Text(
                    text = stringResource(R.string.income),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.275f)
                )
                Text(
                    text = stringResource(R.string.expenses),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.275f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.45f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        AutoSizeText(
                            text = item.income,
                            modifier = Modifier.weight(0.275f),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.End
                        )
                        AutoSizeText(
                            text = item.expenses,
                            modifier = Modifier.weight(0.275f),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.End
                        )
                    }
                    if (index < items.size - 1) {
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.heightIn(min = 128.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
        contentPadding = PaddingValues(0.dp) // Reset padding
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(modifier = Modifier.weight(1f, fill = true))
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                color = Color.White,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DayItem(
    dayNumber: String,
    amount: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .width(66.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.05f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = dayNumber,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.White
        )
        AutoSizeText(
            text = amount,
            modifier = Modifier.fillMaxWidth(),
            maxFontSize = 13.sp,
            minFontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SummaryCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    items: List<Triple<String, String, String>>,
    modernThemeEnabled: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (modernThemeEnabled) Color.Transparent else DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = if (modernThemeEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.third, fontSize = 20.sp) // Emoji or Icon
                            Text(
                                item.first,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        AutoSizeText(
                            text = item.second,
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.End
                        )
                    }
                    if (index < items.size - 1) {
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val items = listOf(
        Triple(stringResource(R.string.nav_home), Icons.Filled.Home, 0),
        Triple(stringResource(R.string.nav_edit), Icons.Filled.Edit, 1),
        Triple(stringResource(R.string.nav_stats), Icons.Filled.BarChart, 2),
        Triple(stringResource(R.string.nav_other), Icons.Filled.MoreHoriz, 3)
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            color = DarkBackground.copy(alpha = 0.94f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (text, icon, _) ->
                    val isSelected = selectedItem == index

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryBlue else Color.White.copy(alpha = 0.65f),
                        animationSpec = tween(180),
                        label = "nav_color"
                    )
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryBlue.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = tween(180),
                        label = "nav_bg"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_scale"
                    )
                    val underlineAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(180),
                        label = "nav_underline"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(backgroundColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, color = PrimaryBlue.copy(alpha = 0.35f))
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onItemSelected(index)
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = text,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp).scale(iconScale)
                            )
                            AutoSizeText(
                                text = text,
                                color = contentColor,
                                maxFontSize = 11.sp,
                                minFontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(width = 22.dp, height = 2.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                PrimaryBlue.copy(alpha = underlineAlpha),
                                                PrimaryBlue.copy(alpha = 0.6f * underlineAlpha)
                                            )
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DateSelectorCard(
    selectedDate: Date,
    dayOfWeek: String,
    formattedDate: String,
    isToday: Boolean,
    onDateSelected: (Date) -> Unit,
    onChangeDateClick: () -> Unit,
    taxiRideViewModel: TaxiRideViewModel,
    application: GestionTaxiApplication,
    normalizeDate: (Date) -> Date,
    modifier: Modifier = Modifier,
    forceCardBackground: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val modernThemeEnabled by application.isModernThemeEnabled().collectAsState(initial = false)
    val useTransparent = modernThemeEnabled && !forceCardBackground
    Card(
        colors = CardDefaults.cardColors(containerColor = if (useTransparent) Color.Transparent else DarkCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        border = if (useTransparent) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Day/Date
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dayOfWeek,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                TextButton(
                    onClick = onChangeDateClick,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = stringResource(R.string.calendar),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_change_date),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!isToday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f)) // Amber-500 approx
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Text(
                        text = stringResource(R.string.date_different_warning),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            // Horizontal Days List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                 // Cargar el primer día de la semana configurado
                var firstDayOfWeek by remember { mutableStateOf(2) } // Por defecto, lunes (2)
                
                // Cargar la configuración del primer día de la semana
                LaunchedEffect(Unit) {
                    application.getFirstDayOfWeek().collect { newFirstDay ->
                        firstDayOfWeek = newFirstDay
                    }
                }
                
                val selectedWeekRange = remember(selectedDate, firstDayOfWeek) {
                    com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
                }
                
                 val selectedWeekStartCalendar = remember(selectedWeekRange) {
                    Calendar.getInstance().apply {
                        time = selectedWeekRange.first
                    }
                }

                for (dayOffset in 0..6) {
                     val calendar = Calendar.getInstance()
                    calendar.time = selectedWeekStartCalendar.time
                    calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                    
                    val isSelected = calendar.get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().apply { 
                        time = selectedDate 
                    }.get(Calendar.DAY_OF_MONTH) &&
                    calendar.get(Calendar.MONTH) == Calendar.getInstance().apply { 
                        time = selectedDate 
                    }.get(Calendar.MONTH)
                    
                    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
                    
                    val day = dayFormat.format(calendar.time)
                    
                    var dayIncome by remember { mutableStateOf(0.0) }
                    val calendarTime = calendar.time
                    
                    LaunchedEffect(calendarTime) {
                        dayIncome = taxiRideViewModel.getIncomeForDate(calendarTime)
                    }

                    val symbol = com.moham.taxi.utils.CurrencyUtils.getCurrencySymbol()
                    val formattedAmount = if (com.moham.taxi.utils.CurrencyUtils.isEuCountry()) {
                        "${dayIncome.toInt()} $symbol"
                    } else {
                        "$symbol${dayIncome.toInt()}"
                    }

                    DayItem(
                        dayNumber = day,
                        amount = if (dayIncome > 0) formattedAmount else "",
                        isSelected = isSelected,
                        onClick = {
                            val newDate = normalizeDate(calendar.time)
                            onDateSelected(newDate)
                            scope.launch {
                                application.saveSelectedDate(newDate)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodaySummaryCard(
    income: String,
    expenses: String,
    modifier: Modifier = Modifier,
    modernThemeEnabled: Boolean = false,
    showIcon: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (modernThemeEnabled) Color.Transparent else DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        border = if (modernThemeEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Balance del día
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Balance,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.balance_of_the_day),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Income Display (Large)
            AutoSizeText(
                text = income,
                maxFontSize = 36.sp,
                minFontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Expense Display (Smaller)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.015f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.expenses),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                AutoSizeText(
                    text = expenses,
                    maxFontSize = 16.sp,
                    minFontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

data class ActivityItem(
    val id: Long,
    val isIncome: Boolean,
    val title: String,
    val description: String,
    val amount: Double,
    val date: Date
)

@Composable
fun RecentActivityCard(
    activities: List<ActivityItem>,
    onActivityClick: (ActivityItem) -> Unit,
    modifier: Modifier = Modifier,
    modernThemeEnabled: Boolean = false,
    showIcon: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (modernThemeEnabled) Color.Transparent else DarkCard),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        border = if (modernThemeEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = stringResource(R.string.recent_activity),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_data_available),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activities.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .clickable { onActivityClick(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val displayDate = remember(item.date) {
                                    val todayCalendar = Calendar.getInstance()
                                    val itemCalendar = Calendar.getInstance().apply { time = item.date }
                                    val isToday = todayCalendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                                             todayCalendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)
                                    if (isToday) {
                                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        "Hoy, ${timeFormat.format(item.date)}"
                                    } else {
                                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        dateFormat.format(item.date)
                                    }
                                }
                                Text(
                                    text = displayDate,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Amount
                            val amountTextColor = if (item.isIncome) Color.White else Color.White.copy(alpha = 0.6f)
                            val prefix = if (item.isIncome) "+" else "-"
                            Text(
                                text = "$prefix${formatCurrency(item.amount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = amountTextColor,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

