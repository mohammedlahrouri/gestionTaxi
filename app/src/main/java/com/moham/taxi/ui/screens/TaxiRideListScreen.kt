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
fun TaxiRideListScreen(navController: NavHostController, selectedDate: Long = -1L) {
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
    
    var selectedTabIndex by remember { mutableStateOf(0) }
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
        containerColor = Color(0xFF121212),
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

@Composable
fun DateSelector(
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dateText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(R.string.action_change_date),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SegmentedTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2C2F33),
                shape = RoundedCornerShape(50)
            )
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val tabs = listOf(
                stringResource(R.string.rides_title),
                stringResource(R.string.expenses)
            )
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color(0xFF3B9C5C) else Color.Transparent)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color(0xFFF5F5F5) else Color(0xFF9AA0A6),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RideListItem(
    ride: TaxiRide,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    if (showPhotoDialog && ride.ticketPhotoPath != null) {
        TicketPhotoDialog(
            photoPath = ride.ticketPhotoPath,
            onDismiss = { showPhotoDialog = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 12.dp)
    ) {
        // Fila superior: Hora a la izquierda, importe en verde vivo `#4CD07D`, 24sp bold a la derecha + MoreVert
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ride.rideTime.ifBlank { "00:00" },
                fontSize = 16.sp,
                color = Color(0xFFF5F5F5)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasCommission = ride.netPrice != null && ride.netPrice != ride.price
                if (hasCommission) {
                    Text(
                        text = formatCurrency(ride.price),
                        fontSize = 14.sp,
                        color = Color(0xFF9AA0A6),
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = formatCurrency(ride.netPrice!!),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CD07D),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                } else {
                    Text(
                        text = formatCurrency(ride.price),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CD07D),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            tint = Color(0xFF9AA0A6)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        }
        
        // Ruta condicional: solo si hay origen y destino
        if (ride.origin.isNotBlank() && ride.destination.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF3B9C5C),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${ride.origin} → ${ride.destination}",
                    fontSize = 14.sp,
                    color = Color(0xFF9AA0A6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Fila inferior de Chips y miniatura de foto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chips en FlowRow
            val isDirect = ride.servicePlatform.isNullOrBlank() || ride.servicePlatform.equals("directo", ignoreCase = true)
            val platformText = if (isDirect) "DIRECTO" else ride.servicePlatform!!.uppercase()
            val platformBg = if (isDirect) Color(0xFF3B9C5C).copy(alpha = 0.15f) else Color(0xFF8B5CF6).copy(alpha = 0.15f)
            val platformTextColor = if (isDirect) Color(0xFF3B9C5C) else Color(0xFF8B5CF6)
            
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(
                    label = platformText,
                    backgroundColor = platformBg,
                    textColor = platformTextColor
                )
                
                val paymentIcon = when (ride.paymentMethod.lowercase()) {
                    "efectivo" -> Icons.Filled.Payments
                    "tarjeta" -> Icons.Filled.CreditCard
                    else -> Icons.Filled.Smartphone
                }
                Chip(
                    label = ride.paymentMethod,
                    icon = paymentIcon
                )
                
                val isTaximeter = ride.serviceType == TaxiRide.SERVICE_TYPE_METER || ride.serviceType == "METER" || ride.tariffId != null
                val tariffLabel = if (isTaximeter) "Taxímetro" else "Cerrado"
                Chip(
                    label = tariffLabel
                )
            }
            
            if (ride.ticketPhotoPath != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TicketPhotoThumbnail(
                    photoPath = ride.ticketPhotoPath,
                    modifier = Modifier.size(36.dp),
                    onClick = { showPhotoDialog = true }
                )
            }
        }
        
        // Propina condicional
        if (ride.tip != null && ride.tip > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Propina: +${formatCurrency(ride.tip)}",
                fontSize = 12.sp,
                color = Color(0xFF4CD07D).copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun Chip(
    label: String,
    icon: ImageVector? = null,
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    textColor: Color = Color(0xFFF5F5F5),
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun TaxiRideItem(
    ride: TaxiRide,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    RideListItem(
        ride = ride,
        onEdit = onEdit,
        onDelete = onDelete,
        onItemClick = onItemClick
    )
}

@Composable
fun ExpenseItem(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    ExpenseListItem(
        expense = expense,
        onEdit = onEdit,
        onDelete = onDelete,
        onItemClick = onItemClick
    )
}

@Composable
fun ExpenseListItem(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    if (showPhotoDialog && expense.ticketPhotoPath != null) {
        TicketPhotoDialog(
            photoPath = expense.ticketPhotoPath,
            onDismiss = { showPhotoDialog = false }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateUtils.formatDate(expense.date, "HH:mm").ifBlank { "00:00" },
                fontSize = 16.sp,
                color = Color(0xFFF5F5F5)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCurrency(expense.amount),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentRed,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            tint = Color(0xFF9AA0A6)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (bgColor, textColor, icon) = when (expense.type) {
                ExpenseType.FUEL -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444), Icons.Filled.LocalGasStation)
                else -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B), Icons.Filled.Payments)
            }
            
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(
                    label = if (expense.type == ExpenseType.FUEL) stringResource(R.string.label_fuel) else stringResource(R.string.label_misc),
                    icon = icon,
                    backgroundColor = bgColor,
                    textColor = textColor
                )
                
                if (expense.type == ExpenseType.OTHER && !expense.description.isNullOrBlank()) {
                    Text(
                        text = expense.description,
                        fontSize = 12.sp,
                        color = Color(0xFF9AA0A6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (expense.ticketPhotoPath != null) {
                Spacer(modifier = Modifier.width(8.dp))
                TicketPhotoThumbnail(
                    photoPath = expense.ticketPhotoPath,
                    modifier = Modifier.size(36.dp),
                    onClick = { showPhotoDialog = true }
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BottomNavBar(
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
