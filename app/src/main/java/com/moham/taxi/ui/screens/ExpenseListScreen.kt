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
import androidx.compose.material.icons.filled.LocalGasStation
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
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.utils.DateUtils
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
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
    
    // ViewModel
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    // Estado para las tabs y confirmación de eliminación
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    
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
    
    // Obtener listas de gastos
    val allExpenses by expenseViewModel.allExpenses.collectAsState(initial = emptyList())
    val todayExpenses by expenseViewModel.todayExpenses.collectAsState(initial = emptyList())
    val selectedDateExpenses by expenseViewModel.getSelectedDateExpenses(useDate).collectAsState(initial = emptyList())
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos") },
                actions = {
                    TextButton(
                        onClick = { 
                            // Usar la fecha normalizada para mantener consistencia
                            val normalizedTimestamp = useDate.time
                            val route = AppScreens.TaxiRideList.createRouteWithDate(normalizedTimestamp)
                            // Primero remover la pantalla actual de la pila
                            navController.popBackStack()
                            // Luego navegar a la pantalla de carreras con fecha
                            navController.navigate(route) {
                                // Evitar múltiples copias de la misma pantalla en la pila
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Text("Ver Carreras")
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
                        text = "Mostrando gastos del: $formattedDate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Tabs para filtrar gastos
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    text = { Text(if (isToday || selectedDate <= 0) "Hoy" else "Fecha seleccionada") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                
                Tab(
                    text = { Text("Todos") },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
            }
            
            // Lista de gastos según la tab seleccionada
            val expensesToShow = if (selectedTabIndex == 0) {
                if (selectedDate > 0 && !isToday) {
                    // Si hay una fecha seleccionada que no es hoy, mostrar los gastos de esa fecha
                    selectedDateExpenses
                } else {
                    // Si no hay fecha seleccionada o es hoy, mostrar los gastos de hoy
                    todayExpenses
                }
            } else {
                // Si se selecciona "Todos", mostrar todos los gastos
                allExpenses
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
                        text = "No hay gastos registrados",
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
                                // Navegar a la pantalla de edición con ID de gasto
                                val timestamp = expense.date.time
                                val route = AppScreens.ExpenseForm.createRouteWithDateAndId(timestamp, expense.id)
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
        
        // Diálogo de confirmación para eliminar
        if (showDeleteConfirmDialog && expenseToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    expenseToDelete = null
                },
                title = { Text("Eliminar Gasto") },
                text = { Text("¿Está seguro de que desea eliminar este gasto?") },
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
                                snackbarHostState.showSnackbar("Gasto eliminado correctamente")
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
                            expenseToDelete = null
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
fun ExpenseItem(
    expense: Expense,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (expense.type == ExpenseType.FUEL) 
                            Icons.Default.LocalGasStation 
                        else 
                            Icons.Default.Payments,
                        contentDescription = expense.type.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Text(
                        text = if (expense.type == ExpenseType.FUEL) "Combustible" else "Otro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(expense.amount),
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = DateUtils.formatDate(expense.date, "dd/MM/yyyy HH:mm"),
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Mostrar descripción solo si es de tipo OTHER
            if (expense.type == ExpenseType.OTHER && !expense.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Descripción: ${expense.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
