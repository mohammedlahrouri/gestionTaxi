package com.moham.taxi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.moham.taxi.ui.screens.AcknowledgmentScreen
import com.moham.taxi.ui.screens.ExpenseFormScreen
import com.moham.taxi.ui.screens.ExpenseListScreen
import com.moham.taxi.ui.screens.HomeScreen
import com.moham.taxi.ui.screens.PaymentMethodScreen
import com.moham.taxi.ui.screens.SettingsScreen
import com.moham.taxi.ui.screens.StatisticsScreen
import com.moham.taxi.ui.screens.TaxiRideFormScreen
import com.moham.taxi.ui.screens.TaxiRideListScreen
import java.util.Date
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.moham.taxi.ui.screens.SplashScreenPreloadData

@Composable
fun AppNavigation(navController: NavHostController, preloadData: SplashScreenPreloadData?) {
    NavHost(
        navController = navController,
        startDestination = AppScreens.Home.route
    ) {

        
        // Pantalla principal
        composable(
            route = AppScreens.Home.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            HomeScreen(navController, preloadData)
        }
        
        // Pantalla de formulario de taxis con parámetro de fecha opcional
        composable(
            route = "${AppScreens.TaxiRideForm.route}/{timestamp}/{taxiRideId}",
            arguments = listOf(
                navArgument("timestamp") {
                    type = NavType.LongType
                    defaultValue = Date().time
                },
                navArgument("taxiRideId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: Date().time
            val taxiRideId = backStackEntry.arguments?.getLong("taxiRideId") ?: -1L
            TaxiRideFormScreen(navController, taxiRideId, timestamp)
        }
        
        // Pantalla de formulario de gastos con parámetro de fecha opcional
        composable(
            route = "${AppScreens.ExpenseForm.route}/{timestamp}/{expenseId}",
            arguments = listOf(
                navArgument("timestamp") {
                    type = NavType.LongType
                    defaultValue = Date().time
                },
                navArgument("expenseId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: Date().time
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
            ExpenseFormScreen(navController, expenseId, timestamp)
        }
        
        // Pantalla de listado de taxis con parámetro de fecha opcional
        composable(
            route = "${AppScreens.TaxiRideList.route}/{timestamp}",
            arguments = listOf(
                navArgument("timestamp") {
                    type = NavType.LongType
                    defaultValue = Date().time
                }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: Date().time
            TaxiRideListScreen(navController, timestamp)
        }
        
        // Pantalla de listado de gastos con parámetro de fecha opcional
        composable(
            route = "${AppScreens.ExpenseList.route}/{timestamp}",
            arguments = listOf(
                navArgument("timestamp") {
                    type = NavType.LongType
                    defaultValue = Date().time
                }
            )
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: Date().time
            ExpenseListScreen(navController, timestamp)
        }
        
        // Pantalla de configuración
        composable(
            route = AppScreens.Settings.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            SettingsScreen(navController)
        }
        
        // Pantalla de estadísticas detalladas
        composable(AppScreens.Statistics.route) {
            StatisticsScreen(navController)
        }
        
        // Pantalla de métodos de pago
        composable(AppScreens.PaymentMethod.route) {
            PaymentMethodScreen(navController)
        }
        
        // Pantalla de agradecimiento
        composable(AppScreens.Acknowledgment.route) {
            AcknowledgmentScreen(navController)
        }
        // Pantalla de cálculo de precios
        composable(AppScreens.Price.route) {
            com.moham.taxi.ui.screens.PriceScreen(navController)
        }
        
        // Pantalla de datos de facturación
        composable(AppScreens.BillingData.route) {
            com.moham.taxi.ui.screens.BillingDataScreen(navController)
        }
        
        // Pantalla de creación de facturas
        composable(AppScreens.Invoice.route) {
            com.moham.taxi.ui.screens.InvoiceScreen(navController)
        }
    }
}
