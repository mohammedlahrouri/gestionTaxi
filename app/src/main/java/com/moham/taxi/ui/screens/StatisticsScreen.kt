package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import androidx.compose.ui.text.font.FontFamily
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.FinancialDetail
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.theme.*
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.screens.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(navController: NavHostController) {
    // Configurar el manejo del botón Atrás para volver a Home
    BackHandler {
        navController.navigate(AppScreens.Home.route) {
            popUpTo(AppScreens.Home.route) {
                inclusive = false
            }
            launchSingleTop = true
            // Las animaciones se manejan en AppNavigation.kt
        }
    }
    
    // Obtener el contexto y la aplicación
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val tipsEnabled by application.isTipsEnabled().collectAsState(initial = false)
    
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
    var appPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var totalPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var netPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekAppPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekTotalPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekNetPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthAppPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthTotalPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthNetPlatformBreakdown by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var dateMeterIncome by remember { mutableStateOf(0.0) }
    var dateFixedIncome by remember { mutableStateOf(0.0) }
    var weekMeterIncome by remember { mutableStateOf(0.0) }
    var weekFixedIncome by remember { mutableStateOf(0.0) }
    var monthMeterIncome by remember { mutableStateOf(0.0) }
    var monthFixedIncome by remember { mutableStateOf(0.0) }
    var dateTips by remember { mutableStateOf(0.0) }
    var weekTips by remember { mutableStateOf(0.0) }
    var monthTips by remember { mutableStateOf(0.0) }
    var tipsByMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var weekTipsByMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var monthTipsByMethod by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    // Cargar datos financieros
    LaunchedEffect(tipsEnabled) {
        withContext(Dispatchers.IO) {
            // Cargar la fecha seleccionada desde DataStore
            selectedDate = application.getSelectedDate().first() ?: Date()
            
            // Datos diarios
            dateIncome = taxiRideViewModel.getIncomeForDate(selectedDate)
            dateExpenses = expenseViewModel.getExpensesTotalForDate(selectedDate)
            dateNet = dateIncome
            rideCount = taxiRideViewModel.getRideCountForDate(selectedDate)
            expenseCount = expenseViewModel.getExpenseCountForDate(selectedDate)
            fuelExpenses = expenseViewModel.getFuelExpensesForDate(selectedDate)
            paymentMethodBreakdown = taxiRideViewModel.getIncomeByPaymentMethodForDate(selectedDate)
            appPlatformBreakdown = taxiRideViewModel.getAppIncomeByPlatformForDate(selectedDate)
            totalPlatformBreakdown = taxiRideViewModel.getIncomeByPlatformForDate(selectedDate)
            netPlatformBreakdown = taxiRideViewModel.getNetIncomeByPlatformForDate(selectedDate)
            if (tipsEnabled) {
                dateTips = taxiRideViewModel.getTipsForDate(selectedDate)
                tipsByMethod = taxiRideViewModel.getTipsByMethodForDate(selectedDate)
            } else {
                dateTips = 0.0
                tipsByMethod = emptyMap()
            }
            taxiRideViewModel.getServiceTypeTotalsForDate(selectedDate).let { totals ->
                dateMeterIncome = totals.first
                dateFixedIncome = totals.second
            }
            
            // Datos semanales basados en la fecha seleccionada
            weekIncome = taxiRideViewModel.getWeekIncomeForDate(selectedDate)
            weekExpenses = expenseViewModel.getWeekExpensesForDate(selectedDate)
            weekNet = weekIncome
            weekRideCount = taxiRideViewModel.getWeekRideCountForDate(selectedDate)
            weekExpenseCount = expenseViewModel.getWeekExpenseCountForDate(selectedDate)
            weekFuelExpenses = expenseViewModel.getWeekFuelExpensesForDate(selectedDate)
            weekPaymentMethodBreakdown = taxiRideViewModel.getWeekIncomeByPaymentMethodForDate(selectedDate)
            weekAppPlatformBreakdown = taxiRideViewModel.getWeekAppIncomeByPlatformForDate(selectedDate)
            weekTotalPlatformBreakdown = taxiRideViewModel.getWeekIncomeByPlatformForDate(selectedDate)
            weekNetPlatformBreakdown = taxiRideViewModel.getWeekNetIncomeByPlatformForDate(selectedDate)
            if (tipsEnabled) {
                weekTips = taxiRideViewModel.getWeekTipsForDate(selectedDate)
                weekTipsByMethod = taxiRideViewModel.getWeekTipsByMethodForDate(selectedDate)
            } else {
                weekTips = 0.0
                weekTipsByMethod = emptyMap()
            }
            taxiRideViewModel.getWeekServiceTypeTotalsForDate(selectedDate).let { totals ->
                weekMeterIncome = totals.first
                weekFixedIncome = totals.second
            }
            
            // Datos mensuales basados en la fecha seleccionada
            monthIncome = taxiRideViewModel.getMonthIncomeForDate(selectedDate)
            monthExpenses = expenseViewModel.getMonthExpensesForDate(selectedDate)
            monthNet = monthIncome
            monthRideCount = taxiRideViewModel.getMonthRideCountForDate(selectedDate)
            monthExpenseCount = expenseViewModel.getMonthExpenseCountForDate(selectedDate)
            monthFuelExpenses = expenseViewModel.getMonthFuelExpensesForDate(selectedDate)
            monthPaymentMethodBreakdown = taxiRideViewModel.getMonthIncomeByPaymentMethodForDate(selectedDate)
            monthAppPlatformBreakdown = taxiRideViewModel.getMonthAppIncomeByPlatformForDate(selectedDate)
            monthTotalPlatformBreakdown = taxiRideViewModel.getMonthIncomeByPlatformForDate(selectedDate)
            monthNetPlatformBreakdown = taxiRideViewModel.getMonthNetIncomeByPlatformForDate(selectedDate)
            if (tipsEnabled) {
                monthTips = taxiRideViewModel.getMonthTipsForDate(selectedDate)
                monthTipsByMethod = taxiRideViewModel.getMonthTipsByMethodForDate(selectedDate)
            } else {
                monthTips = 0.0
                monthTipsByMethod = emptyMap()
            }
            taxiRideViewModel.getMonthServiceTypeTotalsForDate(selectedDate).let { totals ->
                monthMeterIncome = totals.first
                monthFixedIncome = totals.second
            }
        }
    }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val weekFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    val firstDayOfWeek = application.getFirstDayOfWeek().collectAsState(initial = Calendar.MONDAY).value
    val (startOfWeek, endOfWeek) = com.moham.taxi.utils.DateUtils.getWeekRange(selectedDate, firstDayOfWeek)
    val weekRangeText = "${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"

    val calMonth = Calendar.getInstance().apply { time = selectedDate }
    calMonth.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = calMonth.time
    calMonth.set(Calendar.DAY_OF_MONTH, calMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
    val endOfMonth = calMonth.time
    val monthRangeText = "${dateFormat.format(startOfMonth)} - ${dateFormat.format(endOfMonth)}"

    var lastSevenDaysIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
    var lastSevenDaysExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
    var lastSevenWeeksIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
    var lastSevenWeeksExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
    var lastSevenMonthsIncome by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }
    var lastSevenMonthsExpenses by remember { mutableStateOf(listOf<Pair<Date, Double>>()) }

    LaunchedEffect(selectedDate) {
        withContext(Dispatchers.IO) {
            val incData = mutableListOf<Pair<Date, Double>>()
            val expData = mutableListOf<Pair<Date, Double>>()
            val cal = Calendar.getInstance().apply { time = selectedDate; add(Calendar.DAY_OF_MONTH, -6) }
            repeat(7) {
                val d = cal.time
                incData.add(d to taxiRideViewModel.getIncomeForDate(d))
                expData.add(d to expenseViewModel.getExpensesTotalForDate(d))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            lastSevenDaysIncome = incData
            lastSevenDaysExpenses = expData
        }
    }

    LaunchedEffect(selectedDate) {
        withContext(Dispatchers.IO) {
            val incData = mutableListOf<Pair<Date, Double>>()
            val expData = mutableListOf<Pair<Date, Double>>()
            val cal = Calendar.getInstance().apply { time = selectedDate; add(Calendar.WEEK_OF_YEAR, -6) }
            repeat(7) {
                val d = cal.time
                incData.add(d to taxiRideViewModel.getWeekIncomeForDate(d))
                expData.add(d to expenseViewModel.getWeekExpensesForDate(d))
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
            lastSevenWeeksIncome = incData
            lastSevenWeeksExpenses = expData
        }
    }

    LaunchedEffect(selectedDate) {
        withContext(Dispatchers.IO) {
            val incData = mutableListOf<Pair<Date, Double>>()
            val expData = mutableListOf<Pair<Date, Double>>()
            val cal = Calendar.getInstance().apply { time = selectedDate; add(Calendar.MONTH, -6) }
            repeat(7) {
                val d = cal.time
                incData.add(d to taxiRideViewModel.getMonthIncomeForDate(d))
                expData.add(d to expenseViewModel.getMonthExpensesForDate(d))
                cal.add(Calendar.MONTH, 1)
            }
            lastSevenMonthsIncome = incData
            lastSevenMonthsExpenses = expData
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val dayTrend = buildTrendPoints(lastSevenDaysIncome, lastSevenDaysExpenses, dayFormat)
    val weekTrend = buildTrendPoints(lastSevenWeeksIncome, lastSevenWeeksExpenses, weekFormat)
    val monthTrend = buildTrendPoints(lastSevenMonthsIncome, lastSevenMonthsExpenses, monthFormat)

    val dayGross = totalPlatformBreakdown.values.sum()
    val weekGross = weekTotalPlatformBreakdown.values.sum()
    val monthGross = monthTotalPlatformBreakdown.values.sum()

    val dayCommission = (dayGross - dateIncome).coerceAtLeast(0.0)
    val weekCommission = (weekGross - weekIncome).coerceAtLeast(0.0)
    val monthCommission = (monthGross - monthIncome).coerceAtLeast(0.0)

    val periodData = when (selectedTab) {
        0 -> StatsPeriodData(
            period = stringResource(R.string.chart_title_day),
            range = formattedDate,
            gross = dayGross,
            commissions = dayCommission,
            net = dateNet,
            expenses = dateExpenses,
            fuel = fuelExpenses,
            otherExpenses = dateExpenses - fuelExpenses,
            paymentMethods = buildPaymentMethods(paymentMethodBreakdown, appPlatformBreakdown),
            platforms = buildPlatformItems(totalPlatformBreakdown, netPlatformBreakdown),
            meterTotal = dateMeterIncome,
            fixedTotal = dateFixedIncome,
            tipsTotal = dateTips,
            tipsByMethod = tipsByMethod,
            trend = dayTrend,
            trendTitle = stringResource(R.string.trend_daily)
        )
        1 -> StatsPeriodData(
            period = stringResource(R.string.chart_title_week),
            range = weekRangeText,
            gross = weekGross,
            commissions = weekCommission,
            net = weekNet,
            expenses = weekExpenses,
            fuel = weekFuelExpenses,
            otherExpenses = weekExpenses - weekFuelExpenses,
            paymentMethods = buildPaymentMethods(weekPaymentMethodBreakdown, weekAppPlatformBreakdown),
            platforms = buildPlatformItems(weekTotalPlatformBreakdown, weekNetPlatformBreakdown),
            meterTotal = weekMeterIncome,
            fixedTotal = weekFixedIncome,
            tipsTotal = weekTips,
            tipsByMethod = weekTipsByMethod,
            trend = weekTrend,
            trendTitle = stringResource(R.string.trend_weekly)
        )
        else -> StatsPeriodData(
            period = stringResource(R.string.chart_title_month),
            range = monthRangeText,
            gross = monthGross,
            commissions = monthCommission,
            net = monthNet,
            expenses = monthExpenses,
            fuel = monthFuelExpenses,
            otherExpenses = monthExpenses - monthFuelExpenses,
            paymentMethods = buildPaymentMethods(monthPaymentMethodBreakdown, monthAppPlatformBreakdown),
            platforms = buildPlatformItems(monthTotalPlatformBreakdown, monthNetPlatformBreakdown),
            meterTotal = monthMeterIncome,
            fixedTotal = monthFixedIncome,
            tipsTotal = monthTips,
            tipsByMethod = monthTipsByMethod,
            trend = monthTrend,
            trendTitle = stringResource(R.string.trend_monthly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.statistics_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StatsBackground,
                    titleContentColor = StatsTextPrimary
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedItem = 2,
                onItemSelected = { index ->
                    when (index) {
                        0 -> navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        1 -> navController.navigate(AppScreens.TaxiRideList.createRouteWithDate(System.currentTimeMillis()))
                        2 -> {}
                        3 -> navController.navigate(AppScreens.Other.route)
                    }
                }
            )
        },
        containerColor = StatsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(StatsBackground)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatsCardBackground),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StatsBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(
                        stringResource(R.string.tab_day),
                        stringResource(R.string.tab_week),
                        stringResource(R.string.tab_month)
                    ).forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) StatsBackground else Color.Transparent,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (selected) StatsTextPrimary else StatsTextSecondary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            StatsHeroCard(
                net = periodData.net,
                gross = periodData.gross,
                commissions = periodData.commissions,
                expenses = periodData.expenses,
                period = periodData.period,
                dates = periodData.range
            )
            StatsPaymentMethods(methods = periodData.paymentMethods)
            StatsPlatformBreakdown(
                platforms = periodData.platforms,
                meterTotal = periodData.meterTotal,
                fixedTotal = periodData.fixedTotal
            )
            StatsExpenseDetails(
                fuel = periodData.fuel,
                otherExpenses = periodData.otherExpenses
            )
            StatsTrendChart(points = periodData.trend, title = periodData.trendTitle)
            if (tipsEnabled) {
                StatsTipsDetails(total = periodData.tipsTotal, byMethod = periodData.tipsByMethod)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun NewStatisticsCard(
    title: String,
    subtitle: String,
    income: Double,
    expenses: Double,
    net: Double,
    fuelExpenses: Double,
    otherExpenses: Double,
    paymentMethods: Map<String, Double> = emptyMap(),
    appPlatformBreakdown: Map<String, Double> = emptyMap(),
    totalPlatformBreakdown: Map<String, Double> = emptyMap(),
    serviceTypeMeterTotal: Double = 0.0,
    serviceTypeFixedTotal: Double = 0.0,
    chartData: List<Pair<Date, Double>>? = null,
    chartExpensesData: List<Pair<Date, Double>>? = null,
    chartLabels: List<String> = emptyList(),
    chartTitle: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NewStatsCardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        border = BorderStroke(1.dp, NewStatsBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = NewStatsTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NewStatsTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Main Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.income),
                        style = MaterialTheme.typography.bodySmall,
                        color = NewStatsTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AutoSizeText(
                        text = formatCurrency(income),
                        modifier = Modifier.fillMaxWidth(),
                        maxFontSize = 16.sp,
                        minFontSize = 10.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = NewStatsIncome,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(NewStatsBorder))

                // Expenses
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.expenses),
                        style = MaterialTheme.typography.bodySmall,
                        color = NewStatsTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AutoSizeText(
                        text = formatCurrency(expenses),
                        modifier = Modifier.fillMaxWidth(),
                        maxFontSize = 16.sp,
                        minFontSize = 10.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = NewStatsExpense,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(NewStatsBorder))

                // Net
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.net),
                        style = MaterialTheme.typography.bodySmall,
                        color = NewStatsTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AutoSizeText(
                        text = formatCurrency(net),
                        modifier = Modifier.fillMaxWidth(),
                        maxFontSize = 16.sp,
                        minFontSize = 10.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = NewStatsIncome,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Expense Breakdown
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = NewStatsBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Fuel
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NewStatsFuelBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = NewStatsFuelIcon, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.fuel_expenses),
                        color = NewStatsTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AutoSizeText(
                    text = formatCurrency(fuelExpenses),
                    maxFontSize = 14.sp,
                    minFontSize = 10.sp,
                    color = NewStatsTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            // Others
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(NewStatsOtherBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Receipt, contentDescription = null, tint = NewStatsOtherIcon, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.other_expenses),
                        color = NewStatsTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AutoSizeText(
                    text = formatCurrency(otherExpenses),
                    maxFontSize = 14.sp,
                    minFontSize = 10.sp,
                    color = NewStatsTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            // Payment Methods
            if (paymentMethods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NewStatsBorder)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    stringResource(R.string.breakdown_payment_methods),
                    style = MaterialTheme.typography.titleMedium,
                    color = NewStatsTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                paymentMethods.entries.sortedByDescending { it.value }.forEach { (method, amount) ->
                    val methodLower = method.lowercase(Locale.getDefault())
                    val (bg, iconColor, icon) = when {
                        methodLower.contains("efectivo") -> Triple(NewStatsCashBg, NewStatsCashIcon, Icons.Default.Money) // Banknote equivalent
                        methodLower.contains("tarjeta") -> Triple(NewStatsCardBg, NewStatsCardIcon, Icons.Default.CreditCard)
                        else -> Triple(NewStatsAppBg, NewStatsAppIcon, Icons.Default.Smartphone) // Frenow/App
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(bg, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(method, color = NewStatsTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        AutoSizeText(
                            text = formatCurrency(amount),
                            maxFontSize = 14.sp,
                            minFontSize = 10.sp,
                            color = iconColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    if (method.equals("Via App", ignoreCase = true) && appPlatformBreakdown.isNotEmpty()) {
                        Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp)) {
                            appPlatformBreakdown.entries.sortedByDescending { it.value }.forEach { (platform, platformAmount) ->
                                PlatformRow(platform, platformAmount, isNested = true)
                            }
                        }
                    }
                }
            }

            if (totalPlatformBreakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = NewStatsBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.breakdown_platform),
                    style = MaterialTheme.typography.titleMedium,
                    color = NewStatsTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                totalPlatformBreakdown.entries.sortedByDescending { it.value }.forEach { (platform, amount) ->
                    PlatformRow(platform, amount, isNested = false)
                }
            }
            
            // Chart Section
            if (chartData != null && chartData.isNotEmpty() && chartTitle != null) {
                 Spacer(modifier = Modifier.height(16.dp)) 
                 // Chart implementation similar to design
                 Card(
                    colors = CardDefaults.cardColors(containerColor = NewStatsCardBackground), // Same bg
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top=8.dp),
                    border = BorderStroke(1.dp, NewStatsBorder)
                 ) {
                     Column(modifier = Modifier.padding(16.dp)) {
                         Text(chartTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NewStatsTextPrimary)
                         Spacer(modifier = Modifier.height(16.dp))
                         
                         // Legend
                         Row(modifier = Modifier.padding(bottom = 16.dp)) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Box(Modifier.size(8.dp).background(NewStatsIncome, CircleShape))
                                 Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.income),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NewStatsTextSecondary
                                )
                             }
                             Spacer(Modifier.width(16.dp))
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 Box(Modifier.size(8.dp).background(NewStatsExpense, CircleShape))
                                 Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.expenses),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NewStatsTextSecondary
                                )
                             }
                         }

                         Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                             Canvas(modifier = Modifier.fillMaxSize()) {
                                 val w = size.width
                                 val h = size.height
                                 
                                 if (chartData.isNotEmpty()) {
                                     // Calculate Max
                                     val maxVal = (chartData + (chartExpensesData ?: emptyList())).maxOfOrNull { it.second } ?: 1.0
                                      
                                     // Draw Grid
                                     val lines = 5
                                     for (i in 0..lines) {
                                         val y = h * (i.toFloat() / lines)
                                         drawLine(
                                             color = NewStatsBorder,
                                             start = Offset(0f, y),
                                             end = Offset(w, y),
                                             strokeWidth = 1f
                                         )
                                     }
                                     
                                     // Function to draw line
                                     fun drawTrend(data: List<Pair<Date, Double>>, color: Color) {
                                         if (data.isEmpty()) return
                                         val stepX = w / (data.size - 1).coerceAtLeast(1)
                                         
                                         val points = data.mapIndexed { index, pair ->
                                             val x = index * stepX
                                             val y = h - ((pair.second / maxVal) * h).toFloat()
                                             Offset(x, y)
                                         }
                                         
                                         // Draw lines
                                         for (i in 0 until points.size - 1) {
                                             drawLine(
                                                 color = color,
                                                 start = points[i],
                                                 end = points[i+1],
                                                 strokeWidth = 5f,
                                                 cap = androidx.compose.ui.graphics.StrokeCap.Round
                                             )
                                         }
                                         // Draw points
                                         points.forEach { 
                                             drawCircle(color = color, center = it, radius = 6f)
                                         }
                                     }
                                     
                                     if (!chartExpensesData.isNullOrEmpty()) {
                                         drawTrend(chartExpensesData, NewStatsExpense)
                                     }
                                     drawTrend(chartData, NewStatsIncome)
                                 }
                             }
                             
                            // X Labels
                            Row(
                               modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter),
                               horizontalArrangement = Arrangement.SpaceBetween,
                               verticalAlignment = Alignment.Bottom
                            ) {
                                // Simple logic to show labels
                                chartLabels.forEach { label ->
                                    Text(label, style = MaterialTheme.typography.bodySmall, color = NewStatsTextSecondary, fontSize = 10.sp)
                                }
                            }
                         }
                     }
                 }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.total_taximeter),
                        color = NewStatsTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AutoSizeText(
                        text = formatCurrency(serviceTypeMeterTotal),
                        maxFontSize = 14.sp,
                        minFontSize = 10.sp,
                        color = NewStatsTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.total_fixed_price),
                        color = NewStatsTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AutoSizeText(
                        text = formatCurrency(serviceTypeFixedTotal),
                        maxFontSize = 14.sp,
                        minFontSize = 10.sp,
                        color = NewStatsTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformRow(platform: String, amount: Double, isNested: Boolean) {
    val directLabel = stringResource(R.string.platform_direct)
    val displayPlatform = if (
        platform.equals(directLabel, ignoreCase = true) ||
        platform.equals("Directo", ignoreCase = true) ||
        platform.equals("Direct", ignoreCase = true)
    ) {
        directLabel
    } else {
        platform
    }

    val (bg, iconColor, icon) = when (displayPlatform) {
        "FreeNow" -> Triple(Color(0xFFFF6B00).copy(alpha = 0.1f), Color(0xFFFF6B00), Icons.Filled.Smartphone)
        "Cabify" -> Triple(Color(0xFF6B00FF).copy(alpha = 0.1f), Color(0xFF6B00FF), Icons.Filled.DirectionsCar)
        "Uber" -> Triple(Color(0xFF000000).copy(alpha = 0.1f), Color(0xFF000000), Icons.Filled.DirectionsCar)
        directLabel -> Triple(Color(0xFF059669).copy(alpha = 0.1f), Color(0xFF059669), Icons.Filled.LocationOn)
        else -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.1f), Color(0xFF8B5CF6), Icons.Filled.Apps)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isNested) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isNested) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(iconColor.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Box(
                modifier = Modifier.size(36.dp).background(bg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))
            Text(displayPlatform, color = NewStatsTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }

        AutoSizeText(
            text = formatCurrency(amount),
            maxFontSize = 14.sp,
            minFontSize = 10.sp,
            color = if (isNested) iconColor else NewStatsTextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
