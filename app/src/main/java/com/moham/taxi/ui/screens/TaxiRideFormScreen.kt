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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.moham.taxi.utils.DateUtils
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.ui.components.TaxiDropdown
import com.moham.taxi.ui.theme.BlueAccent
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.DarkCard
import com.moham.taxi.ui.theme.GreenAccent
import com.moham.taxi.ui.theme.Warning
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import com.moham.taxi.utils.PriceUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiRideFormScreen(
    navController: NavHostController,
    rideId: Long = -1L, // Si es -1, es una nueva carrera, si no, estamos editando
    selectedDate: Long = -1L // Fecha seleccionada desde la pantalla principal, -1 significa usar fecha actual
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModels
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.PaymentMethodViewModelFactory(
            repository = application.paymentMethodRepository
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
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    
    // Estado para errores
    var originError by remember { mutableStateOf(false) }
    var destinationError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var paymentMethodError by remember { mutableStateOf(false) }
    
    // Estado para manejar si el formulario está siendo enviado
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Obtener métodos de pago
    val paymentMethods by paymentMethodViewModel.allPaymentMethods.collectAsState(initial = emptyList())
    val paymentMethodNames = paymentMethods.map { it.name }
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Si estamos editando, cargar los datos de la carrera
    LaunchedEffect(rideId) {
        if (rideId > 0) {
            scope.launch {
                val ride = taxiRideViewModel.getTaxiRideById(rideId)
                ride?.let {
                    origin = it.origin
                    destination = it.destination
                    price = it.price.toString()
                    selectedPaymentMethod = it.paymentMethod
                }
            }
        }
    }
    
    // Función para guardar o actualizar la carrera
    fun saveTaxiRide() {
        // Validar campos
        var isValid = true
        
        // Origen y destino son opcionales, no se validan
        
        if (price.isBlank() || price.toDoubleOrNull() == null || price.toDouble() <= 0) {
            priceError = true
            isValid = false
        }
        
        if (selectedPaymentMethod.isBlank()) {
            paymentMethodError = true
            isValid = false
        }
        
        if (isValid && !isSubmitting) {
            isSubmitting = true // Prevenir múltiples envíos
            
            scope.launch {
                try {
                    // Usar el precio exacto ingresado por el usuario
                    val finalPrice = price.toDouble()
                    
                    // Determinar la fecha final usando la función de utilidad
                    val finalDate = DateUtils.assignProperDate(useDate)
                    
                    val taxiRide = TaxiRide(
                        id = if (rideId > 0) rideId else 0,
                        origin = origin,
                        destination = destination,
                        price = finalPrice,
                        paymentMethod = selectedPaymentMethod,
                        date = finalDate
                    )
                    
                    if (rideId > 0) {
                        taxiRideViewModel.update(taxiRide)
                    } else {
                        taxiRideViewModel.insert(taxiRide)
                    }
                    
                    // Notificar a HomeScreen que debe refrescar los datos
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_data", true)
                    
                    // Navegar inmediatamente de vuelta
                    navController.popBackStack()
                    
                    // Mostrar snackbar después de navegar
                    val message = if (rideId > 0) "Carrera actualizada correctamente" else "Carrera guardada correctamente"
                    snackbarHostState.showSnackbar(message)
                } catch (e: Exception) {
                    // Mostrar error
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                    isSubmitting = false // Permitir reintento en caso de error
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (rideId > 0) "Editar Carrera" else "Nueva Carrera") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de título con fecha
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Mostrar la fecha seleccionada y advertencia si no es hoy
                    if (!isToday) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Warning.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Advertencia",
                                tint = Warning,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Text(
                                text = "Atención: estás registrando una carrera para el $formattedDate",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    

                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tarjeta de formulario
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Campo de Origen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Origen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        TextField(
                            value = origin,
                            onValueChange = { 
                                origin = it
                                originError = false
                            },
                            label = { Text("Origen (Opcional)") },
                            isError = originError,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2A2A2A),
                                focusedContainerColor = Color(0xFF2A2A2A),
                                focusedIndicatorColor = BlueAccent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = BlueAccent,
                                cursorColor = BlueAccent,
                                errorIndicatorColor = Color.Red
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                    
                    if (originError) {
                        Text(
                            text = "Por favor, ingrese el origen",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Campo de Destino
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = "Destino",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        TextField(
                            value = destination,
                            onValueChange = { 
                                destination = it
                                destinationError = false
                            },
                            label = { Text("Destino (Opcional)") },
                            isError = destinationError,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2A2A2A),
                                focusedContainerColor = Color(0xFF2A2A2A),
                                focusedIndicatorColor = BlueAccent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = BlueAccent,
                                cursorColor = BlueAccent,
                                errorIndicatorColor = Color.Red
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }
                    
                    if (destinationError) {
                        Text(
                            text = "Por favor, ingrese el destino",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Campo de Precio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = "Precio",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        TextField(
                            value = price,
                            onValueChange = { newValue ->
                                // Permitir entrada de números decimales
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    price = newValue
                                    priceError = false
                                }
                            },
                            label = { Text("Precio (€)") },
                            isError = priceError,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2A2A2A),
                                focusedContainerColor = Color(0xFF2A2A2A),
                                focusedIndicatorColor = BlueAccent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = BlueAccent,
                                cursorColor = BlueAccent,
                                errorIndicatorColor = Color.Red
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    
                    if (priceError) {
                        Text(
                            text = "Por favor, ingrese un precio válido",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Dropdown de Método de Pago
                    TaxiDropdown(
                        label = "Método de Pago",
                        options = paymentMethodNames,
                        selectedOption = selectedPaymentMethod,
                        onOptionSelected = { 
                            selectedPaymentMethod = it
                            paymentMethodError = false
                        },
                        isError = paymentMethodError,
                        errorMessage = "Por favor, seleccione un método de pago",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botón Guardar
                    Button(
                        onClick = {
                            if (!isSubmitting) {
                                saveTaxiRide()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenAccent,
                            disabledContainerColor = GreenAccent.copy(alpha = 0.5f)
                        ),
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = "Guardar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Text(
                                text = if (isSubmitting) "GUARDANDO..." else if (rideId > 0) "ACTUALIZAR" else "GUARDAR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botón Cancelar
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CANCELAR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
