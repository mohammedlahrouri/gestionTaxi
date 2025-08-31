package com.moham.taxi.ui.screens

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.utils.DateUtils
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiRideListScreen(navController: NavHostController, selectedDate: Long = -1L) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    // Estado para las tabs y confirmación de eliminación
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
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
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }
    val formattedDate = remember(useDate) { dateFormat.format(useDate) }
    
    // Obtener listas de carreras
    val allRides by taxiRideViewModel.allTaxiRides.collectAsState(initial = emptyList())
    val todayRides by taxiRideViewModel.todayRides.collectAsState(initial = emptyList())
    val selectedDateRides by taxiRideViewModel.getSelectedDateRides(useDate).collectAsState(initial = emptyList())
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carreras") },
                actions = {
                    TextButton(
                        onClick = { 
                            // Usar la fecha normalizada para mantener consistencia
                            val normalizedTimestamp = useDate.time
                            val route = AppScreens.ExpenseList.createRouteWithDate(normalizedTimestamp)
                            navController.navigate(route)
                        }
                    ) {
                        Text("Ver Gastos")
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
                        text = "Mostrando carreras del: $formattedDate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Tabs para filtrar carreras
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    text = { Text(if (isToday || selectedDate <= 0) "Hoy" else "Fecha seleccionada") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                
                Tab(
                    text = { Text("Todas") },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }
            
            // Lista de carreras según la tab seleccionada
            val ridesToShow = if (selectedTabIndex == 0) {
                if (selectedDate > 0 && !isToday) {
                    // Si hay una fecha seleccionada que no es hoy, mostrar las carreras de esa fecha
                    selectedDateRides
                } else {
                    // Si no hay fecha seleccionada o es hoy, mostrar las carreras de hoy
                    todayRides
                }
            } else {
                // Si se selecciona "Todas", mostrar todas las carreras
                allRides
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
                        text = "No hay carreras registradas",
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
                            taxiRide = ride,
                            onEdit = {
                                // Navegar a la pantalla de edición con ID de carrera
                                val timestamp = ride.date.time
                                val route = AppScreens.TaxiRideForm.createRouteWithDateAndId(timestamp, ride.id)
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
        }
        
        // Diálogo de confirmación para eliminar
        if (showDeleteConfirmDialog && rideToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    rideToDelete = null
                },
                title = { Text("Eliminar Carrera") },
                text = { Text("¿Está seguro de que desea eliminar esta carrera?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Guardar una referencia local a la carrera que se va a eliminar
                            val rideToDeleteLocal = rideToDelete
                            
                            // Cerrar el diálogo
                            showDeleteConfirmDialog = false
                            rideToDelete = null
                            
                            // Realizar la operación de eliminación con la referencia local
                            scope.launch {
                                rideToDeleteLocal?.let { taxiRideViewModel.delete(it) }
                                snackbarHostState.showSnackbar("Carrera eliminada correctamente")
                            }
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            rideToDelete = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun TaxiRideItem(
    taxiRide: TaxiRide,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtils.formatDate(taxiRide.date, "dd/MM/yyyy HH:mm"),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(taxiRide.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar"
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar"
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Método de pago: ${taxiRide.paymentMethod}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "De: ${taxiRide.origin}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "A: ${taxiRide.destination}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
