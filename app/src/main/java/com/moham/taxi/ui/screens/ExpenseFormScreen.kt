package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Build
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.moham.taxi.utils.ImageUtils
import java.io.File
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.navigation.AppScreens
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
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )

    val ticketPhotosEnabled by application.isTicketPhotosEnabled().collectAsState(initial = false)

    
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
    
    // Estado para los campos del formulario
    var selectedExpenseType by remember { mutableStateOf(ExpenseType.FUEL) }
    var description by remember { mutableStateOf("") }
    var maintenanceKilometers by remember { mutableStateOf("") }
    var maintenanceDetails by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var ticketPhotoPath by remember { mutableStateOf<String?>(null) }
    var existingRealDate by remember { mutableStateOf<Date?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                val uri = android.net.Uri.fromFile(file)
                val relativePath = ImageUtils.compressAndSaveTicketPhoto(context, uri)
                if (relativePath != null) {
                    ImageUtils.deleteTicketPhoto(context, ticketPhotoPath)
                    ticketPhotoPath = relativePath
                }
                file.delete()
            }
        } else {
            tempPhotoFile?.delete()
        }
        tempPhotoFile = null
    }

    // Estado para errores
    var descriptionError by remember { mutableStateOf(false) }
    var maintenanceKilometersError by remember { mutableStateOf(false) }
    var maintenanceDetailsError by remember { mutableStateOf(false) }
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
                maintenanceKilometers = expense.maintenanceKilometers?.toString() ?: ""
                maintenanceDetails = expense.maintenanceDetails ?: ""
                amount = expense.amount.toString()
                ticketPhotoPath = expense.ticketPhotoPath
                existingRealDate = expense.realDate
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

        if (selectedExpenseType == ExpenseType.MAINTENANCE) {
            val kmValue = maintenanceKilometers.toIntOrNull()
            if (maintenanceKilometers.isBlank() || kmValue == null || kmValue <= 0) {
                maintenanceKilometersError = true
                isValid = false
            }
            if (maintenanceDetails.isBlank()) {
                maintenanceDetailsError = true
                isValid = false
            }
        }
        
        if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            amountError = true
            isValid = false
        }
        
        if (isValid && !isSubmitting) {
            isSubmitting = true // Prevenir múltiples envíos
            
            scope.launch {
                try {
                    // Determinar la fecha final usando la función de utilidad
                    val finalDate = DateUtils.assignProperDate(useDate)
                    
                    val expense = Expense(
                        id = if (expenseId > 0) expenseId else 0,
                        type = selectedExpenseType,
                        description = when (selectedExpenseType) {
                            ExpenseType.FUEL -> null
                            ExpenseType.MAINTENANCE -> null
                            else -> description
                        },
                        maintenanceKilometers = if (selectedExpenseType == ExpenseType.MAINTENANCE) maintenanceKilometers.toInt() else null,
                        maintenanceDetails = if (selectedExpenseType == ExpenseType.MAINTENANCE) maintenanceDetails else null,
                        amount = amount.toDouble(),
                        date = finalDate,
                        ticketPhotoPath = ticketPhotoPath,
                        realDate = existingRealDate ?: Date()
                    )
                    
                    if (expenseId > 0) {
                        expenseViewModel.update(expense)
                    } else {
                        expenseViewModel.insert(expense)
                    }
                    

                    
                    // Notificar a HomeScreen que debe actualizar los datos
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_data", true)
                    
                    // Navegar inmediatamente de vuelta
                    navController.popBackStack()
                    
                    // Mostrar snackbar después de navegar
                    val message = if (expenseId > 0) {
                        context.getString(R.string.expense_updated_success)
                    } else {
                        context.getString(R.string.expense_saved_success)
                    }
                    snackbarHostState.showSnackbar(message)
                } catch (e: Exception) {
                    // Mostrar error
                    // Mostrar error
                    e.printStackTrace()
                    e.printStackTrace()
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.expense_save_error, e.message ?: "")
                    )
                    isSubmitting = false // Permitir reintento en caso de error
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_expense_title)) },
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
                        contentDescription = stringResource(R.string.warning),
                        tint = Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.expense_warning_today),
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
                        contentDescription = stringResource(R.string.info),
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.expense_warning_date, formattedDate),
                        color = BlueAccent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = stringResource(R.string.expense_type_label),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            )
            
            // Radio buttons para seleccionar el tipo de gasto
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
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
                                onClick = {
                                    selectedExpenseType = ExpenseType.FUEL
                                    descriptionError = false
                                    maintenanceKilometersError = false
                                    maintenanceDetailsError = false
                                }
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
                            contentDescription = stringResource(R.string.expense_type_fuel),
                            tint = if (selectedExpenseType == ExpenseType.FUEL) BlueAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.expense_type_fuel),
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
                                selected = selectedExpenseType == ExpenseType.MAINTENANCE,
                                onClick = {
                                    selectedExpenseType = ExpenseType.MAINTENANCE
                                    descriptionError = false
                                    maintenanceKilometersError = false
                                    maintenanceDetailsError = false
                                }
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = selectedExpenseType == ExpenseType.MAINTENANCE,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BlueAccent,
                                unselectedColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(R.string.expense_type_maintenance),
                            tint = if (selectedExpenseType == ExpenseType.MAINTENANCE) BlueAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.expense_type_maintenance),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (selectedExpenseType == ExpenseType.MAINTENANCE) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedExpenseType == ExpenseType.MAINTENANCE) Color.White else Color.White.copy(alpha = 0.7f),
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
                                onClick = {
                                    selectedExpenseType = ExpenseType.OTHER
                                    descriptionError = false
                                    maintenanceKilometersError = false
                                    maintenanceDetailsError = false
                                }
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
                            contentDescription = stringResource(R.string.expense_type_other),
                            tint = if (selectedExpenseType == ExpenseType.OTHER) BlueAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.expense_type_other),
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
                        contentDescription = stringResource(R.string.label_description),
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
                        label = stringResource(R.string.label_description),
                        isError = descriptionError,
                        errorMessage = stringResource(R.string.error_description_required),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedExpenseType == ExpenseType.MAINTENANCE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = stringResource(R.string.label_kilometers),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TaxiTextField(
                        value = maintenanceKilometers,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                maintenanceKilometers = newValue
                                maintenanceKilometersError = false
                            }
                        },
                        label = stringResource(R.string.label_kilometers),
                        isError = maintenanceKilometersError,
                        errorMessage = stringResource(R.string.error_valid_kilometers),
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = stringResource(R.string.label_maintenance_details),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TaxiTextField(
                        value = maintenanceDetails,
                        onValueChange = {
                            maintenanceDetails = it
                            maintenanceDetailsError = false
                        },
                        label = stringResource(R.string.label_maintenance_details),
                        isError = maintenanceDetailsError,
                        errorMessage = stringResource(R.string.error_maintenance_details_required),
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
                    contentDescription = stringResource(R.string.amount),
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
                    label = stringResource(R.string.label_amount_euro),
                    isError = amountError,
                    errorMessage = stringResource(R.string.error_valid_amount),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Guardar
                TaxiButton(
                    text = if (isSubmitting) {
                        stringResource(R.string.saving)
                    } else if (expenseId > 0) {
                        stringResource(R.string.update_expense_button)
                    } else {
                        stringResource(R.string.save_expense_button)
                    },
                    onClick = {
                        if (!isSubmitting) {
                            saveExpense()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.save),
                            tint = Color.White
                        )
                    }
                )

                if (ticketPhotosEnabled) {
                    Button(
                        onClick = {
                            val file = ImageUtils.createTempImageFile(context)
                            tempPhotoFile = file
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${application.packageName}.provider",
                                file
                            )
                            cameraLauncher.launch(uri)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ticketPhotoPath != null) BlueAccent else DarkBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (ticketPhotoPath != null) Icons.Filled.CheckCircle else Icons.Filled.PhotoCamera,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Button(
                    onClick = { 
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            navController.popBackStack() 
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.6f)),
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
