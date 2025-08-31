package com.moham.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import com.moham.taxi.utils.DateUtils
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.theme.BlueAccent
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.DarkCard
import com.moham.taxi.ui.theme.Warning
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    navController: NavHostController,
    expenseId: Long = -1L, // Si es -1, es un nuevo gasto, si no, estamos editando
    selectedDate: Long = -1L // Fecha seleccionada desde la pantalla principal, -1 significa usar fecha actual
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
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
    
    // Estado para los campos del formulario
    var selectedExpenseType by remember { mutableStateOf(ExpenseType.FUEL) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    // Estado para errores
    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    
    // Estado para manejar si el formulario está siendo enviado
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Si estamos editando, cargar los datos del gasto
    LaunchedEffect(expenseId) {
        if (expenseId > 0) {
            expenseViewModel.getExpenseById(expenseId)?.let { expense ->
                selectedExpenseType = expense.type
                description = expense.description ?: ""
                amount = expense.amount.toString()
            }
        }
    }
    
    // Función para guardar o actualizar el gasto
    fun saveExpense() {
        // Validar campos
        var isValid = true
        
        if (selectedExpenseType == ExpenseType.OTHER && description.isBlank()) {
            descriptionError = true
            isValid = false
        }
        
        if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            amountError = true
            isValid = false
        }
        
        if (isValid && !isSubmitting) {
            isSubmitting = true // Prevenir múltiples envíos
            
            scope.launch {
                try {
                    println("DEBUG FORM: Guardando gasto para fecha: ${dateFormat.format(useDate)}")
                    println("DEBUG FORM: useDate timestamp: ${useDate.time}")
                    
                    // Determinar la fecha final usando la función de utilidad
                    val finalDate = DateUtils.assignProperDate(useDate)
                    println("DEBUG FORM: Fecha final asignada: ${dateFormat.format(finalDate)}")
                    println("DEBUG FORM: Fecha final timestamp: ${finalDate.time}")
                    
                    val expense = Expense(
                        id = if (expenseId > 0) expenseId else 0,
                        type = selectedExpenseType,
                        description = if (selectedExpenseType == ExpenseType.FUEL) null else description,
                        amount = amount.toDouble(),
                        date = finalDate
                    )
                    
                    println("DEBUG FORM: Objeto de gasto creado: $expense")
                    
                    if (expenseId > 0) {
                        expenseViewModel.update(expense)
                        println("DEBUG FORM: Gasto actualizado con ID: $expenseId")
                    } else {
                        expenseViewModel.insert(expense)
                        println("DEBUG FORM: Gasto nuevo insertado")
                    }
                    
                    // Actualizar inmediatamente la UI para debug
                    try {
                        val todayExpenses = expenseViewModel.getExpensesTotalForDate(useDate)
                        println("DEBUG FORM: Gastos actualizados para hoy: $todayExpenses")
                    } catch (e: Exception) {
                        println("ERROR FORM al obtener gastos: ${e.message}")
                    }
                    
                    // Notificar a HomeScreen que debe actualizar los datos
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_data", true)
                    
                    // Navegar inmediatamente de vuelta
                    navController.popBackStack()
                    
                    // Mostrar snackbar después de navegar
                    val message = if (expenseId > 0) "Gasto actualizado correctamente" else "Gasto guardado correctamente"
                    snackbarHostState.showSnackbar(message)
                } catch (e: Exception) {
                    // Mostrar error
                    println("ERROR FORM: ${e.message}")
                    e.printStackTrace()
                    snackbarHostState.showSnackbar("Error al guardar el gasto: ${e.message}")
                    isSubmitting = false // Permitir reintento en caso de error
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Gasto") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Mostrar advertencia si es la fecha de hoy
            if (isToday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Warning.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Advertencia",
                        tint = Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Estás registrando un gasto para la fecha actual",
                        color = Warning,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Mostrar mensaje con la fecha seleccionada
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            BlueAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Información",
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Estás registrando un gasto para el $formattedDate",
                        color = BlueAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = "Tipo de Gasto",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )
            
            // Radio buttons para seleccionar el tipo de gasto
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedExpenseType == ExpenseType.FUEL,
                                onClick = { selectedExpenseType = ExpenseType.FUEL }
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = selectedExpenseType == ExpenseType.FUEL,
                            onClick = null, // null porque el onClick está en el Row
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BlueAccent,
                                unselectedColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = "Combustible",
                            tint = if (selectedExpenseType == ExpenseType.FUEL) BlueAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Combustible",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selectedExpenseType == ExpenseType.FUEL) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedExpenseType == ExpenseType.FUEL) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Divider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedExpenseType == ExpenseType.OTHER,
                                onClick = { selectedExpenseType = ExpenseType.OTHER }
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = selectedExpenseType == ExpenseType.OTHER,
                            onClick = null, // null porque el onClick está en el Row
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BlueAccent,
                                unselectedColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Otros",
                            tint = if (selectedExpenseType == ExpenseType.OTHER) BlueAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Otros",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selectedExpenseType == ExpenseType.OTHER) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedExpenseType == ExpenseType.OTHER) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de descripción (solo visible para tipo "Otros")
            if (selectedExpenseType == ExpenseType.OTHER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Descripción",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TaxiTextField(
                        value = description,
                        onValueChange = { 
                            description = it
                            descriptionError = false
                        },
                        label = "Descripción",
                        isError = descriptionError,
                        errorMessage = "Por favor, ingrese una descripción",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Campo de importe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = "Importe",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                TaxiTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amount = newValue
                            amountError = false
                        }
                    },
                    label = "Importe (€)",
                    isError = amountError,
                    errorMessage = "Por favor, ingrese un importe válido",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón Guardar
            TaxiButton(
                text = if (isSubmitting) "Guardando..." else if (expenseId > 0) "Actualizar Gasto" else "Guardar Gasto",
                onClick = {
                    if (!isSubmitting) {
                        saveExpense()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                enabled = !isSubmitting,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Guardar",
                        tint = Color.White
                    )
                }
            )
        }
    }
}
