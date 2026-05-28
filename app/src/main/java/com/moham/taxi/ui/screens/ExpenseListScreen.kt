package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.components.DateFloatingActionButton
import com.moham.taxi.ui.components.TicketPhotoThumbnail
import com.moham.taxi.ui.components.TicketPhotoDialog
import com.moham.taxi.utils.DateUtils
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.ui.screens.BottomNavBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(navController: NavHostController, selectedDate: Long = -1L) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModels
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    // Estado para las tabs y confirmación de eliminación
    var selectedTabIndex by remember { mutableStateOf(0) } // Iniciar en carreras (índice 0)
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var rideToDelete by remember { mutableStateOf<TaxiRide?>(null) }
    
    // Determinar la fecha a usar
    val useDate = remember(selectedDate) {
        if (selectedDate > 0) Date(selectedDate) else Date()
    }
    
    // Verificar si es la fecha de hoy
    val isToday = remember(useDate) {
        val today = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { time = useDate }
        today.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH) &&
        today.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }
    
    // Formato para mostrar la fecha
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(useDate) { dateFormat.format(useDate) }
    
    // Obtener listas de gastos y carreras
    val allExpenses by expenseViewModel.allExpenses.collectAsState(initial = emptyList())
    val todayExpenses by expenseViewModel.todayExpenses.collectAsState(initial = emptyList())
    val selectedDateExpenses by expenseViewModel.getSelectedDateExpenses(useDate).collectAsState(initial = emptyList())
    
    val allRides by taxiRideViewModel.allTaxiRides.collectAsState(initial = emptyList())
    val todayRides by taxiRideViewModel.todayRides.collectAsState(initial = emptyList())
    val selectedDateRides by taxiRideViewModel.getSelectedDateRides(useDate).collectAsState(initial = emptyList())
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_expenses)) }
            )
        },
        floatingActionButton = {
            DateFloatingActionButton(
                currentDate = useDate,
                onDateSelected = { newDate ->
                    // Navegar a la misma pantalla con la nueva fecha
                    val route = AppScreens.ExpenseList.createRouteWithDate(newDate.time)
                    navController.navigate(route) {
                        popUpTo(AppScreens.ExpenseList.route) {
                            inclusive = true
                        }
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
            // Mostrar la fecha seleccionada
            if (!isToday && selectedDate > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.msg_showing_data_for, formattedDate),
                    )
                }
            }
            
            // Tabs para alternar entre carreras y gastos
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    text = { Text(stringResource(R.string.tab_expenses)) },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                
                Tab(
                    text = { Text(stringResource(R.string.tab_rides)) },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }
            
            // Contenido basado en la pestaña seleccionada
            when (selectedTabIndex) {
                0 -> {
                    // Mostrar carreras
                    val ridesToShow = if (selectedDate > 0 && !isToday) {
                        selectedDateRides
                    } else {
                        todayRides
                    }
                    
                    if (ridesToShow.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_rides),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ridesToShow) { ride ->
                                TaxiRideItem(
                                    ride = ride,
                                    onDeleteClick = {
                                        rideToDelete = ride
                                        showDeleteConfirmDialog = true
                                    },
                                    onItemClick = {
                                        val route = AppScreens.TaxiRideDetail.createRouteWithId(ride.id)
                                        navController.navigate(route)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Mostrar gastos
                    val expensesToShow = if (selectedDate > 0 && !isToday) {
                        selectedDateExpenses
                    } else {
                        todayExpenses
                    }
                    
                    if (expensesToShow.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.msg_no_expenses),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(expensesToShow) { expense ->
                                ExpenseItem(
                                    expense = expense,
                                    onEdit = {
                                        val timestamp = expense.date.time
                                        val route = AppScreens.ExpenseForm.createRouteWithDateAndId(timestamp, expense.id)
                                        navController.navigate(route)
                                    },
                                    onDelete = {
                                        expenseToDelete = expense
                                        showDeleteConfirmDialog = true
                                    },
                                    onItemClick = {
                                        val route = AppScreens.ExpenseDetail.createRouteWithId(expense.id)
                                        navController.navigate(route)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
        
        // Diálogo de confirmación para eliminar gastos
        if (showDeleteConfirmDialog && expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    expenseToDelete = null
                },
                title = { Text(stringResource(R.string.delete_expense_title)) },
                text = { Text(stringResource(R.string.delete_expense_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Guardar referencia local del gasto a eliminar
                            val gastoAEliminar = expenseToDelete
                            
                            // Cerrar el diálogo inmediatamente
                            showDeleteConfirmDialog = false
                            expenseToDelete = null
                            
                            // Realizar la operación de eliminación con la referencia local
                            scope.launch {
                                gastoAEliminar?.let { expenseViewModel.delete(it) }
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_expense_deleted))
                            }
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            expenseToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // Diálogo de confirmación para eliminar carreras
        if (showDeleteConfirmDialog && rideToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    rideToDelete = null
                },
                title = { Text(stringResource(R.string.delete_ride_title)) },
                text = { Text(stringResource(R.string.delete_ride_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Guardar referencia local de la carrera a eliminar
                            val carreraAEliminar = rideToDelete
                            
                            // Cerrar el diálogo inmediatamente
                            showDeleteConfirmDialog = false
                            rideToDelete = null
                            
                            // Realizar la operación de eliminación con la referencia local
                            scope.launch {
                                carreraAEliminar?.let { taxiRideViewModel.delete(it) }
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_ride_deleted))
                            }
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            rideToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

@Composable
fun TaxiRideItem(
    ride: TaxiRide,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit = {}
) {
    var showPhotoDialog by remember { mutableStateOf(false) }

    if (showPhotoDialog && ride.ticketPhotoPath != null) {
        TicketPhotoDialog(
            photoPath = ride.ticketPhotoPath,
            onDismiss = { showPhotoDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val originText = ride.origin.ifBlank { stringResource(R.string.no_origin) }
                val destinationText = ride.destination.ifBlank { stringResource(R.string.no_destination) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = originText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = destinationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.label_rate, ride.price),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.label_date_format, SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(ride.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.label_time_format, ride.rideTime.ifBlank { "00:00" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (ride.ticketPhotoPath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TicketPhotoThumbnail(
                        photoPath = ride.ticketPhotoPath,
                        onClick = { showPhotoDialog = true }
                    )
                }
            }
            
            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_ride),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
