package com.moham.taxi.ui.navigation

sealed class AppScreens(val route: String) {
    object Login : AppScreens("login") // Pantalla de inicio de sesión
    object Home : AppScreens("home")
    object TaxiRideForm : AppScreens("taxi_ride_form")
    object ExpenseForm : AppScreens("expense_form")
    object Settings : AppScreens("settings")
    object TaxiRideList : AppScreens("taxi_rides")
    object ExpenseList : AppScreens("expenses")
    object Statistics : AppScreens("statistics")  // Nueva pantalla de estadísticas
    object PaymentMethod : AppScreens("payment_methods") // Pantalla de métodos de pago
    object Acknowledgment : AppScreens("acknowledgment") // Pantalla de agradecimiento
    object Price : AppScreens("price") // Pantalla de cálculo de precios
    object BillingData : AppScreens("billing_data") // Pantalla de datos de facturación
    object Invoice : AppScreens("invoice") // Pantalla de creación de facturas
    
    fun createRouteWithDate(timestamp: Long): String {
        return "$route/$timestamp"
    }
    
    fun createRouteWithDateAndId(timestamp: Long, id: Long = -1L): String {
        return "$route/$timestamp/$id"
    }
    
    companion object {
        fun fromRoute(route: String?): AppScreens {
            return when(route?.substringBefore("/")) {
                Login.route -> Login
                Home.route -> Home
                TaxiRideForm.route -> TaxiRideForm
                ExpenseForm.route -> ExpenseForm
                Settings.route -> Settings
                TaxiRideList.route -> TaxiRideList
                ExpenseList.route -> ExpenseList
                Statistics.route -> Statistics
                PaymentMethod.route -> PaymentMethod
                Acknowledgment.route -> Acknowledgment
                Price.route -> Price
                BillingData.route -> BillingData
                Invoice.route -> Invoice
                null -> Login
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
        }
    }
}
