package com.moham.taxi.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel

@Composable
fun TaxiRideDetailScreen(
    navController: NavHostController,
    rideId: Long
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    // Redirigir directamente al formulario para editar la carrera
    LaunchedEffect(rideId) {
        if (rideId > 0) {
            // Simplemente usamos el TaxiRideFormScreen con el id pasado como parámetro
            // Esto permite reutilizar el formulario para crear y editar
        }
    }
    
    // Reutilizamos el formulario para editar
    TaxiRideFormScreen(navController = navController, rideId = rideId)
} 
