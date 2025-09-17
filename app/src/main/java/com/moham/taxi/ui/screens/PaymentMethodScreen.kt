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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.moham.taxi.ui.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.PaymentMethodViewModelFactory(
            repository = application.paymentMethodRepository
        )
    )
    
    // Estado para el formulario de nuevo método de pago
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var methodToDelete by remember { mutableStateOf<PaymentMethod?>(null) }
    var newMethodName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    
    // Obtener métodos de pago
    val paymentMethods by paymentMethodViewModel.allPaymentMethods.collectAsState(initial = emptyList())
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métodos de Pago") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir Método de Pago"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Métodos de pago disponibles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (paymentMethods.isEmpty()) {
                Text(
                    text = "No hay métodos de pago configurados",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paymentMethods) { method ->
                        PaymentMethodItem(
                            paymentMethod = method,
                            onDelete = {
                                methodToDelete = method
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Diálogo para añadir nuevo método de pago
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Nuevo Método de Pago") },
                text = {
                    Column {
                        Text("Introduce el nombre del método de pago:")
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = newMethodName,
                            onValueChange = { 
                                newMethodName = it
                                nameError = false
                            },
                            label = "Nombre",
                            isError = nameError,
                            errorMessage = if (nameError) "El nombre no puede estar vacío" else ""
                        )
                    }
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            if (newMethodName.isBlank()) {
                                nameError = true
                            } else {
                                scope.launch {
                                    paymentMethodViewModel.insert(PaymentMethod(name = newMethodName))
                                    newMethodName = ""
                                    showAddDialog = false
                                    snackbarHostState.showSnackbar("Método de pago añadido")
                                }
                            }
                        },
                        text = "Guardar"
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            newMethodName = ""
                            showAddDialog = false 
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo de confirmación para eliminar método de pago
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Eliminar Método de Pago") },
                text = { 
                    Text("¿Estás seguro de que deseas eliminar el método de pago '${methodToDelete?.name}'?") 
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            methodToDelete?.let { method ->
                                scope.launch {
                                    paymentMethodViewModel.delete(method)
                                    showDeleteConfirmDialog = false
                                    snackbarHostState.showSnackbar("Método de pago eliminado")
                                }
                            }
                        },
                        text = "Eliminar"
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun PaymentMethodItem(
    paymentMethod: PaymentMethod,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = paymentMethod.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar Método de Pago",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
