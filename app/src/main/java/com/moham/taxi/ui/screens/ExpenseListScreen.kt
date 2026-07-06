package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.ui.components.TicketPhotoThumbnail
import com.moham.taxi.ui.components.TicketPhotoDialog
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.AccentRed
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.utils.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(navController: NavHostController, selectedDate: Long = -1L) {
    BackHandler {
        navController.navigate(AppScreens.Home.route) {
            popUpTo(AppScreens.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(application.taxiRideRepository)
    )
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(application.expenseRepository)
    )
    
    // Iniciar en Gastos (índice 1) para esta pantalla
    var selectedTabIndex by remember { mutableStateOf(1) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var rideToDelete by remember { mutableStateOf<TaxiRide?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
    // Sync with DataStore selected date flow
    val initialDate = remember(selectedDate) {
        if (selectedDate > 0) Date(selectedDate) else Date()
    }
    val selectedDateFromStore by application.getSelectedDate().collectAsState(initial = initialDate)
    
    LaunchedEffect(selectedDate) {
        if (selectedDate > 0) {
            application.saveSelectedDate(Date(selectedDate))
        }
    }
    
    val useDate = selectedDateFromStore
    val isToday = remember(useDate) {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { time = useDate }
        today.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
        today.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val formattedDate = remember(useDate) { dateFormat.format(useDate) }
    val dayOfWeek = remember(useDate) { 
        dayOfWeekFormat.format(useDate).replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        } 
    }
    
    val headerDateText = remember(useDate, isToday, dayOfWeek) {
        val locale = Locale.getDefault()
        val pattern = when (locale.language) {
            "es" -> "d 'de' MMMM"
            "fr" -> "d MMMM"
            "de" -> "d. MMMM"
            else -> "MMMM d"
        }
        val detailFormatter = SimpleDateFormat(pattern, locale)
        val formattedDetail = detailFormatter.format(useDate)
        
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
    
    val ridesToShow by taxiRideViewModel.getSelectedDateRides(useDate).collectAsState(initial = emptyList())
    val expensesToShow by expenseViewModel.getSelectedDateExpenses(useDate).collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.dateToUtcStartOfDayMillis(useDate)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = DateUtils.utcStartOfDayMillisToLocalDate(millis)
                            scope.launch {
                                application.saveSelectedDate(newDate)
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
                    selectedDayContainerColor = Color(0xFF3B9C5C),
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = Color(0xFF3B9C5C),
                    todayContentColor = Color(0xFF3B9C5C)
                )
            )
        }
    }
    
    Scaffold(
        containerColor = Color(0xFF1E2124),
        bottomBar = {
            BottomBar(
                selectedItem = 1,
                onItemSelected = { index ->
                    when (index) {
                        0 -> navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        1 -> {}
                        2 -> navController.navigate(AppScreens.Statistics.route)
                        3 -> navController.navigate(AppScreens.Other.route)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clickable Date Selector in Header
            DateSelector(
                dateText = headerDateText,
                onClick = { showDatePicker = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Segmented Tabs (Rides / Expenses)
            SegmentedTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content List
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    if (ridesToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.no_rides_registered),
                                color = Color(0xFF9AA0A6)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            itemsIndexed(ridesToShow) { index, ride ->
                                RideListItem(
                                    ride = ride,
                                    onEdit = {
                                        val timestamp = ride.date.time
                                        val route = AppScreens.TaxiRideForm.createRouteWithDateAndId(timestamp, ride.id)
                                        navController.navigate(route)
                                    },
                                    onItemClick = {
                                        val route = AppScreens.TaxiRideDetail.createRouteWithId(ride.id)
                                        navController.navigate(route)
                                    },
                                    onDelete = {
                                        rideToDelete = ride
                                        showDeleteConfirmDialog = true
                                    }
                                )
                                if (index < ridesToShow.lastIndex) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (expensesToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.no_expenses_registered),
                                color = Color(0xFF9AA0A6)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            itemsIndexed(expensesToShow) { index, expense ->
                                ExpenseListItem(
                                    expense = expense,
                                    onEdit = {
                                        val timestamp = expense.date.time
                                        val route = AppScreens.ExpenseForm.createRouteWithDateAndId(timestamp, expense.id)
                                        navController.navigate(route)
                                    },
                                    onItemClick = {
                                        val route = AppScreens.ExpenseDetail.createRouteWithId(expense.id)
                                        navController.navigate(route)
                                    },
                                    onDelete = {
                                        expenseToDelete = expense
                                        showDeleteConfirmDialog = true
                                    }
                                )
                                if (index < expensesToShow.lastIndex) {
                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirmDialog) {
        if (rideToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; rideToDelete = null },
                containerColor = Color(0xFF2C2F33),
                title = { Text(stringResource(R.string.delete_ride_title), color = Color(0xFFF5F5F5)) },
                text = { Text(stringResource(R.string.delete_ride_confirm), color = Color(0xFFF5F5F5).copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = {
                         val item = rideToDelete
                         showDeleteConfirmDialog = false
                         rideToDelete = null
                         scope.launch {
                             item?.let { taxiRideViewModel.delete(it) }
                             snackbarHostState.showSnackbar(context.getString(R.string.ride_deleted_success))
                         }
                    }) { Text(stringResource(R.string.delete), color = AccentRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false; rideToDelete = null }) {
                        Text(stringResource(R.string.cancel), color = Color(0xFFF5F5F5))
                    }
                }
            )
        }
        if (expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; expenseToDelete = null },
                containerColor = Color(0xFF2C2F33),
                title = { Text(stringResource(R.string.delete_expense_title), color = Color(0xFFF5F5F5)) },
                text = { Text(stringResource(R.string.delete_expense_confirm), color = Color(0xFFF5F5F5).copy(alpha = 0.8f)) },
                confirmButton = {
                    TextButton(onClick = {
                         val item = expenseToDelete
                         showDeleteConfirmDialog = false
                         expenseToDelete = null
                         scope.launch {
                             item?.let { expenseViewModel.delete(it) }
                             snackbarHostState.showSnackbar(context.getString(R.string.expense_deleted_success))
                         }
                    }) { Text(stringResource(R.string.delete), color = AccentRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false; expenseToDelete = null }) {
                        Text(stringResource(R.string.cancel), color = Color(0xFFF5F5F5))
                    }
                }
            )
        }
    }
}
