package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.components.SummaryCard
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedSummaryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
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
    
    // Estado para las pestañas (Día / Semana / Mes)
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Estado para el tipo de ingreso a mostrar (0 = Semana, 1 = Mes)
    var summaryIncomeType by remember { mutableStateOf(0) }
    
    // Estado para almacenar los datos de resumen
    var todayIncome by remember { mutableStateOf(0.0) }
    var todayExpenses by remember { mutableStateOf(0.0) }
    var todayNet by remember { mutableStateOf(0.0) }
    
    var weekIncome by remember { mutableStateOf(0.0) }
    var weekExpenses by remember { mutableStateOf(0.0) }
    var weekNet by remember { mutableStateOf(0.0) }
    
    var monthIncome by remember { mutableStateOf(0.0) }
    var monthExpenses by remember { mutableStateOf(0.0) }
    var monthNet by remember { mutableStateOf(0.0) }
    
    var todayIncomeByPaymentMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekIncomeByPaymentMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthIncomeByPaymentMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    // Cargar datos al iniciar la pantalla
    LaunchedEffect(key1 = true) {
        // Cargar la preferencia del tipo de ingreso a mostrar
        summaryIncomeType = application.getSummaryIncomeType().first()
        withContext(Dispatchers.IO) {
            // Datos diarios
            todayIncome = taxiRideViewModel.getTodayIncome()
            todayExpenses = expenseViewModel.getTodayExpensesTotal()
            todayNet = todayIncome - todayExpenses
            todayIncomeByPaymentMethod = taxiRideViewModel.getTodayIncomeByPaymentMethod()
            
            // Datos semanales
            weekIncome = taxiRideViewModel.getCurrentWeekIncome()
            weekExpenses = expenseViewModel.getCurrentWeekExpenses()
            weekNet = weekIncome - weekExpenses
            
            // Obtener ingresos por método de pago para la semana
            val weekComparison = taxiRideViewModel.getWeekOverWeekComparison()
            val weekIncomeByDay = taxiRideViewModel.getIncomeByDayOfWeek()
            
            // Calcular ingresos por método de pago para la semana
            // Esto es una aproximación basada en los datos disponibles
            val todayMethods = todayIncomeByPaymentMethod.keys
            val weekMethods = mutableMapOf<String, Double>()
            
            // Inicializar con los métodos del día
            todayMethods.forEach { method ->
                weekMethods[method] = 0.0
            }
            
            // Obtener datos de la semana actual
            val calendar = java.util.Calendar.getInstance()
            val today = calendar.time
            
            // Obtener el primer día de la semana configurado
            val firstDayOfWeek = application.getFirstDayOfWeek().first()
            val (weekStart, weekEnd) = com.moham.taxi.utils.DateUtils.getWeekRange(today, firstDayOfWeek)
            
            // Iterar por cada día de la semana usando el rango calculado
            val weekCalendar = java.util.Calendar.getInstance().apply { time = weekStart }
            while (weekCalendar.time <= weekEnd) {
                val date = weekCalendar.time
                val methodsForDay = taxiRideViewModel.getIncomeByPaymentMethodForDate(date)
                
                // Sumar a los totales semanales
                methodsForDay.forEach { (method, amount) ->
                    weekMethods[method] = (weekMethods[method] ?: 0.0) + amount
                }
                
                // Avanzar al siguiente día
                weekCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            
            weekIncomeByPaymentMethod = weekMethods
            
            // Datos mensuales
            monthIncome = taxiRideViewModel.getTaxiMonthIncome()
            monthExpenses = expenseViewModel.getMonthExpensesTotal()
            monthNet = monthIncome - monthExpenses
            monthIncomeByPaymentMethod = taxiRideViewModel.getTaxiMonthIncomeByPaymentMethod()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_detailed_summary)) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Tabs para seleccionar entre día, semana y mes
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    text = { Text(stringResource(R.string.tab_day)) },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                
                Tab(
                    text = { Text(stringResource(R.string.tab_week)) },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
                
                Tab(
                    text = { Text(stringResource(R.string.tab_month)) },
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contenido según la tab seleccionada
            when (selectedTabIndex) {
                0 -> DailySummaryDetailed(todayIncome, todayExpenses, todayNet, todayIncomeByPaymentMethod)
                1 -> WeeklySummary(weekIncome, weekExpenses, weekNet, weekIncomeByPaymentMethod)
                2 -> MonthlySummaryDetailed(monthIncome, monthExpenses, monthNet, monthIncomeByPaymentMethod)
            }
        }
    }
}

@Composable
fun DailySummaryDetailed(
    income: Double,
    expenses: Double,
    net: Double,
    incomeByPaymentMethod: Map<String, Double>
) {
    // Obtener el contexto para acceder a la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Obtener la preferencia del tipo de ingreso a mostrar
    val summaryIncomeType = remember { mutableStateOf(0) }
    
    // Cargar la preferencia del tipo de ingreso
    LaunchedEffect(Unit) {
        summaryIncomeType.value = application.getSummaryIncomeType().first()
    }
    
    // Obtener los valores de ingresos semanales y mensuales
    var weekIncome by remember { mutableStateOf(0.0) }
    var monthIncome by remember { mutableStateOf(0.0) }
    
    // Cargar los valores de ingresos semanales y mensuales
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val taxiRideViewModel = TaxiRideViewModel(application.taxiRideRepository)
            weekIncome = taxiRideViewModel.getCurrentWeekIncome()
            monthIncome = taxiRideViewModel.getTaxiMonthIncome()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cards con los totales
        SummaryCard(
            title = stringResource(R.string.gross_income_day),
            amount = income
        )
        
        SummaryCard(
            title = stringResource(R.string.expenses_day),
            amount = expenses
        )
        
        SummaryCard(
            title = stringResource(R.string.net_income_day),
            amount = net
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Detalle por método de pago
        PaymentMethodBreakdown(
            title = stringResource(R.string.breakdown_payment_day),
            incomeByPaymentMethod = incomeByPaymentMethod
        )
    }
}

@Composable
fun WeeklySummary(
    income: Double,
    expenses: Double,
    net: Double,
    incomeByPaymentMethod: Map<String, Double>
) {
    // Obtener el contexto para acceder a la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Obtener la preferencia del tipo de ingreso a mostrar
    val summaryIncomeType = remember { mutableStateOf(0) }
    
    // Cargar la preferencia del tipo de ingreso
    LaunchedEffect(Unit) {
        summaryIncomeType.value = application.getSummaryIncomeType().first()
    }
    
    // Obtener los valores de ingresos semanales y mensuales
    var weekIncome by remember { mutableStateOf(income) } // Ya tenemos el ingreso semanal como parámetro
    var monthIncome by remember { mutableStateOf(0.0) }
    
    // Cargar el valor de ingreso mensual
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val taxiRideViewModel = TaxiRideViewModel(application.taxiRideRepository)
            monthIncome = taxiRideViewModel.getTaxiMonthIncome()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cards con los totales
        SummaryCard(
            title = stringResource(R.string.gross_income_week),
            amount = income
        )
        
        SummaryCard(
            title = stringResource(R.string.expenses_week),
            amount = expenses
        )
        
        SummaryCard(
            title = stringResource(R.string.net_income_week),
            amount = net
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Detalle por método de pago
        PaymentMethodBreakdown(
            title = stringResource(R.string.breakdown_payment_week),
            incomeByPaymentMethod = incomeByPaymentMethod
        )
    }
}

@Composable
fun MonthlySummaryDetailed(
    income: Double,
    expenses: Double,
    net: Double,
    incomeByPaymentMethod: Map<String, Double>
) {
    // Obtener el contexto para acceder a la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // Obtener la preferencia del tipo de ingreso a mostrar
    val summaryIncomeType = remember { mutableStateOf(0) }
    
    // Cargar la preferencia del tipo de ingreso
    LaunchedEffect(Unit) {
        summaryIncomeType.value = application.getSummaryIncomeType().first()
    }
    
    // Obtener los valores de ingresos semanales y mensuales
    var weekIncome by remember { mutableStateOf(0.0) }
    var monthIncome by remember { mutableStateOf(income) } // Ya tenemos el ingreso mensual como parámetro
    
    // Cargar el valor de ingreso semanal
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val taxiRideViewModel = TaxiRideViewModel(application.taxiRideRepository)
            weekIncome = taxiRideViewModel.getCurrentWeekIncome()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cards con los totales
        SummaryCard(
            title = stringResource(R.string.gross_income_month),
            amount = income
        )
        
        SummaryCard(
            title = stringResource(R.string.expenses_month),
            amount = expenses
        )
        
        SummaryCard(
            title = stringResource(R.string.net_income_month),
            amount = net
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Detalle por método de pago
        PaymentMethodBreakdown(
            title = stringResource(R.string.breakdown_payment_month),
            incomeByPaymentMethod = incomeByPaymentMethod
        )
    }
}

// Utilizamos la función PaymentMethodBreakdown de SummaryScreen.kt
// No es necesario definirla aquí ya que tiene la misma implementación
