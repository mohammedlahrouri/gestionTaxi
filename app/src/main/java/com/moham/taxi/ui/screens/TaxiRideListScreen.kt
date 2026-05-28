package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
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
import com.moham.taxi.ui.components.DateFloatingActionButton
import com.moham.taxi.ui.components.AutoSizeText
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.components.TicketPhotoThumbnail
import com.moham.taxi.ui.components.TicketPhotoDialog
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.*
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
    
    val useDate = remember(selectedDate) {
        if (selectedDate > 0) Date(selectedDate) else Date()
    }
    val isToday = remember(useDate) {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { time = useDate }
        today.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
        today.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }
    
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(useDate) { dateFormat.format(useDate) }
    
    val selectedDateRides by taxiRideViewModel.getSelectedDateRides(useDate).collectAsState(initial = emptyList())
    val todayRides by taxiRideViewModel.todayRides.collectAsState(initial = emptyList())
    val ridesToShow = if (selectedDate > 0 && !isToday) selectedDateRides else todayRides
    
    val selectedDateExpenses by expenseViewModel.getSelectedDateExpenses(useDate).collectAsState(initial = emptyList())
    val todayExpenses by expenseViewModel.todayExpenses.collectAsState(initial = emptyList())
    val expensesToShow = if (selectedDate > 0 && !isToday) selectedDateExpenses else todayExpenses
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            BottomNavBar(
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
        floatingActionButton = {
            DateFloatingActionButton(
                currentDate = useDate,
                onDateSelected = { newDate ->
                    val route = AppScreens.TaxiRideList.createRouteWithDate(newDate.time)
                    navController.navigate(route) {
                        popUpTo(AppScreens.TaxiRideList.route) { inclusive = true }
                        launchSingleTop = true
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
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.rides_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.showing_data_for, formattedDate),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Custom Segmented Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryBlue else Color.Transparent)
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (selectedTabIndex == 0) {
                    // Carreras List
                    if (ridesToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.no_rides_registered),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(ridesToShow) { ride ->
                                TaxiRideItem(
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
                            }
                        }
                    }
                } else {
                    // Gastos List
                    if (expensesToShow.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.no_expenses_registered),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(expensesToShow) { expense ->
                                ExpenseItem(
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
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete Dialogs (Logic unchanged, UI style tweaked for consistency)
    if (showDeleteConfirmDialog) {
        if (rideToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; rideToDelete = null },
                containerColor = DarkCard,
                title = { Text(stringResource(R.string.delete_ride_title), color = Color.White) },
                text = { Text(stringResource(R.string.delete_ride_confirm), color = Color.White.copy(0.8f)) },
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
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            )
        }
        if (expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false; expenseToDelete = null },
                containerColor = DarkCard,
                title = { Text(stringResource(R.string.delete_expense_title), color = Color.White) },
                text = { Text(stringResource(R.string.delete_expense_confirm), color = Color.White.copy(0.8f)) },
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
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun TaxiRideItem(
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
    
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        DateUtils.formatDate(ride.date, "dd/MM/yyyy"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    if (ride.rideTime.isNotBlank()) {
                        Text(
                            ride.rideTime,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Menú
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            tint = Color.White.copy(0.6f)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Filled.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) }
                        )
                    }
                }
            }
            
            // Central: Origin and Destination
            val originText = ride.origin.ifBlank { stringResource(R.string.no_origin) }
            val destinationText = ride.destination.ifBlank { stringResource(R.string.no_destination) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = PrimaryBlue.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = originText,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = AccentRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = destinationText,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (ride.ticketPhotoPath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TicketPhotoThumbnail(
                    photoPath = ride.ticketPhotoPath,
                    onClick = { showPhotoDialog = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fila Inferior: Badges y Precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Badges a la izquierda
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                stringResource(R.string.ride_type_taxi),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        ride.servicePlatform?.let { platform ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    platform.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6)
                                )
                            }
                        }
                    }
                    
                    // Payment Method Chip
                    val (bgColor, textColor, icon) = when (ride.paymentMethod.lowercase()) {
                        "efectivo" -> Triple(PrimaryBlue.copy(alpha = 0.1f), PrimaryBlue, Icons.Filled.AttachMoney)
                        "tarjeta" -> Triple(Color(0xFF0EA5E9).copy(alpha = 0.1f), Color(0xFF0EA5E9), Icons.Filled.CreditCard)
                        else -> Triple(Color(0xFF8B5CF6).copy(alpha = 0.1f), Color(0xFF8B5CF6), Icons.Filled.Smartphone)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(bgColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
                        Text(ride.paymentMethod, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                }
                
                // Precio a la derecha
                val hasCommission = ride.netPrice != null && ride.netPrice != ride.price
                if (hasCommission) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 120.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.label_gross),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = formatCurrency(ride.price),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.label_net),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981) // Verde esmeralda para el neto
                            )
                            AutoSizeText(
                                text = formatCurrency(ride.netPrice!!),
                                modifier = Modifier.weight(1f, fill = false),
                                maxFontSize = 18.sp,
                                minFontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                } else {
                    AutoSizeText(
                        text = formatCurrency(ride.price),
                        modifier = Modifier.widthIn(max = 100.dp),
                        maxFontSize = 18.sp,
                        minFontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(
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
    
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
             Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        DateUtils.formatDate(expense.date, "dd/MM/yyyy"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                     Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (expense.type == ExpenseType.FUEL) stringResource(R.string.short_fuel) else stringResource(R.string.short_other), 
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatCurrency(expense.amount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                     Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp).offset(x = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.options),
                                tint = Color.White.copy(0.6f)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) }
                            )
                        }
                    }
                }
            }
            
            // Type/Description Badge
             val (bgColor, textColor, icon) = when (expense.type) {
                ExpenseType.FUEL -> Triple(Color(0xFFEF4444).copy(alpha = 0.1f), Color(0xFFEF4444), Icons.Filled.LocalGasStation)
                else -> Triple(Color(0xFFF59E0B).copy(alpha = 0.1f), Color(0xFFF59E0B), Icons.Filled.Payments)
            }
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(bgColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Text(if (expense.type == ExpenseType.FUEL) stringResource(R.string.label_fuel) else stringResource(R.string.label_misc), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
            }
            
            if (expense.type == ExpenseType.OTHER && !expense.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                     // Using a description icon or similar
                     Text(expense.description, fontSize = 12.sp, color = Color.White.copy(0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (expense.ticketPhotoPath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TicketPhotoThumbnail(
                    photoPath = expense.ticketPhotoPath,
                    onClick = { showPhotoDialog = true }
                )
            }
        }
    }
}
