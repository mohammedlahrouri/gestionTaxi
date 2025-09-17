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
import com.moham.taxi.ui.viewmodel.ExpenseViewModel

@Composable
fun ExpenseDetailScreen(
    navController: NavHostController,
    expenseId: Long
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    // Redirigir directamente al formulario para editar el gasto
    LaunchedEffect(expenseId) {
        if (expenseId > 0) {
            // Simplemente usamos el ExpenseFormScreen con el id pasado como parámetro
            // Esto permite reutilizar el formulario para crear y editar
        }
    }
    
    // Reutilizamos el formulario para editar
    ExpenseFormScreen(navController = navController, expenseId = expenseId)
} 
