package com.moham.taxi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.moham.taxi.ui.screens.AcknowledgmentScreen
import com.moham.taxi.ui.screens.ExpenseFormScreen
import com.moham.taxi.ui.screens.ExpenseListScreen
import com.moham.taxi.ui.screens.HomeScreen
import com.moham.taxi.ui.screens.MaintenanceScreen
import com.moham.taxi.ui.screens.DonationsScreen
import com.moham.taxi.ui.screens.TariffsScreen
import com.moham.taxi.ui.screens.TariffFormScreen
import com.moham.taxi.ui.screens.SurchargeFormScreen
import com.moham.taxi.ui.screens.QuoteFormScreen
import com.moham.taxi.ui.screens.OtherScreen
import com.moham.taxi.ui.screens.ExtraModulesScreen
import com.moham.taxi.ui.screens.PaymentMethodScreen
import com.moham.taxi.ui.screens.ServicePlatformScreen
import com.moham.taxi.ui.screens.SettingsScreen
import com.moham.taxi.ui.screens.StatisticsScreen
import com.moham.taxi.ui.screens.TaxiRideFormScreen
import com.moham.taxi.ui.screens.TaxiRideListScreen
import com.moham.taxi.ui.screens.OnlineBackupScreen
import com.moham.taxi.ui.screens.OnboardingStep1Screen
import com.moham.taxi.ui.screens.OnboardingStep2Screen
import com.moham.taxi.ui.screens.OnboardingStep3Screen
import com.moham.taxi.ui.screens.OnboardingStep4Screen
import com.moham.taxi.ui.screens.OnboardingStep5Screen
import com.moham.taxi.ui.screens.StartupScreen
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moham.taxi.ui.screens.SplashScreenPreloadData
@Composable
fun AppNavigation(navController: NavHostController, preloadData: SplashScreenPreloadData?) {
    NavHost(
        navController = navController,
        startDestination = AppScreens.Startup.route
    ) {

        composable(route = AppScreens.Startup.route) {
            StartupScreen(navController)
        }

        composable(route = AppScreens.OnboardingStep1.route) {
            OnboardingStep1Screen(navController)
        }

        composable(route = AppScreens.OnboardingStep2.route) {
            OnboardingStep2Screen(navController)
        }

        composable(route = AppScreens.OnboardingStep3.route) {
            OnboardingStep3Screen(navController)
        }

        composable(route = AppScreens.OnboardingStep4.route) {
            OnboardingStep4Screen(navController)
        }

        composable(route = AppScreens.OnboardingStep5.route) {
            OnboardingStep5Screen(navController)
        }

        
        // Pantalla principal
        composable(
            route = AppScreens.Home.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
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
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
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
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
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
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
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
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: Date().time
            ExpenseListScreen(navController, timestamp)
        }
        
        // Detalle de carrera
        composable(
            route = "${AppScreens.TaxiRideDetail.route}/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            com.moham.taxi.ui.screens.TaxiRideDetailScreen(navController, id)
        }

        // Detalle de gasto
        composable(
            route = "${AppScreens.ExpenseDetail.route}/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            com.moham.taxi.ui.screens.ExpenseDetailScreen(navController, id)
        }
        
        // Pantalla de configuración
        composable(
            route = AppScreens.Settings.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            SettingsScreen(navController)
        }
        
        // Pantalla de estadísticas detalladas
        composable(
            route = AppScreens.Statistics.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            StatisticsScreen(navController)
        }
        
        // Pantalla de métodos de pago
        composable(
            route = AppScreens.PaymentMethod.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            PaymentMethodScreen(navController)
        }

        composable(
            route = AppScreens.ServicePlatform.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            ServicePlatformScreen(navController)
        }
        
        // Pantalla de agradecimiento
        composable(
            route = AppScreens.Acknowledgment.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            AcknowledgmentScreen(navController)
        }
        composable(
            route = AppScreens.Other.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            OtherScreen(navController)
        }
        
        // Pantalla de módulos extras
        composable(
            route = AppScreens.ExtraModules.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            ExtraModulesScreen(navController)
        }

        composable(
            route = AppScreens.Maintenance.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            MaintenanceScreen(navController)
        }

        composable(
            route = AppScreens.Donations.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            DonationsScreen(navController)
        }
        
        // Pantalla de cálculo de precios
        composable(
            route = AppScreens.Price.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.moham.taxi.ui.screens.PriceScreen(navController)
        }
        
        // Pantalla de datos de facturación
        composable(
            route = AppScreens.BillingData.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.moham.taxi.ui.screens.BillingDataScreen(navController)
        }
        
        // Pantalla de creación de facturas
        composable(
            route = AppScreens.Invoice.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.moham.taxi.ui.screens.InvoiceScreen(navController)
        }

        composable(
            route = AppScreens.OnlineBackup.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            OnlineBackupScreen(navController)
        }

        composable(
            route = AppScreens.Tariffs.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            TariffsScreen(navController)
        }

        composable(
            route = AppScreens.Export.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            com.moham.taxi.ui.screens.ExportScreen(navController)
        }

        composable(
            route = "${AppScreens.TariffForm.route}/{tariffId}",
            arguments = listOf(
                navArgument("tariffId") { type = NavType.LongType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val tariffId = backStackEntry.arguments?.getLong("tariffId") ?: -1L
            TariffFormScreen(navController, tariffId)
        }

        composable(
            route = "${AppScreens.SurchargeForm.route}/{surchargeId}",
            arguments = listOf(
                navArgument("surchargeId") { type = NavType.LongType }
            ),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val surchargeId = backStackEntry.arguments?.getLong("surchargeId") ?: -1L
            SurchargeFormScreen(navController, surchargeId)
        }
        composable(
            route = AppScreens.QuoteForm.route,
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            QuoteFormScreen(navController)
        }
    }
}
