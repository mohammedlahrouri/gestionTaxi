package com.moham.taxi.ui.navigation

sealed class AppScreens(val route: String) {
    object Startup : AppScreens("startup")
    object Login : AppScreens("login") // Pantalla de inicio de sesión
    object Home : AppScreens("home")
    object OnboardingStep1 : AppScreens("onboarding/step1")
    object OnboardingStep2 : AppScreens("onboarding/step2")
    object OnboardingStep3 : AppScreens("onboarding/step3")
    object OnboardingStep4 : AppScreens("onboarding/step4")
    object OnboardingStep5 : AppScreens("onboarding/step5")
    object TaxiRideForm : AppScreens("taxi_ride_form")
    object ExpenseForm : AppScreens("expense_form")
    object Settings : AppScreens("settings")
    object TaxiRideList : AppScreens("taxi_rides")
    object TaxiRideDetail : AppScreens("taxi_ride_detail") {
        fun createRouteWithId(id: Long): String {
            return "$route/$id"
        }
    }
    object ExpenseList : AppScreens("expenses")
    object ExpenseDetail : AppScreens("expense_detail") {
        fun createRouteWithId(id: Long): String {
            return "$route/$id"
        }
    }
    object Statistics : AppScreens("statistics")  // Nueva pantalla de estadísticas
    object PaymentMethod : AppScreens("payment_methods") // Pantalla de métodos de pago
    object ServicePlatform : AppScreens("service_platform")
    object Acknowledgment : AppScreens("acknowledgment") // Pantalla de agradecimiento
    object Price : AppScreens("price") // Pantalla de cálculo de precios
    object BillingData : AppScreens("billing_data") // Pantalla de datos de facturación
    object Invoice : AppScreens("invoice") // Pantalla de creación de facturas
    object Other : AppScreens("other") // Pantalla de opciones adicionales
    object ExtraModules : AppScreens("extra_modules") // Pantalla de módulos extras
    object Maintenance : AppScreens("maintenance")
    object Donations : AppScreens("donations")
    object OnlineBackup : AppScreens("online_backup")
    object FleetConnection : AppScreens("fleet_connection")
    object Tariffs : AppScreens("tariffs")
    object Export : AppScreens("export")
    object QuoteForm : AppScreens("quote_form")
    object ManualQuote : AppScreens("manual_quote")
    object FuelPrices : AppScreens("fuel_prices")
    object TariffForm : AppScreens("tariff_form") {
        fun createRouteWithId(id: Long): String {
            return "$route/$id"
        }
    }
    object SurchargeForm : AppScreens("surcharge_form") {
        fun createRouteWithId(id: Long): String {
            return "$route/$id"
        }
    }
    
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
                Startup.route -> Startup
                Home.route -> Home
                OnboardingStep1.route.substringBefore("/") -> OnboardingStep1
                TaxiRideForm.route -> TaxiRideForm
                ExpenseForm.route -> ExpenseForm
                Settings.route -> Settings
                TaxiRideList.route -> TaxiRideList
                ExpenseList.route -> ExpenseList
                Statistics.route -> Statistics
                PaymentMethod.route -> PaymentMethod
                ServicePlatform.route -> ServicePlatform
                Acknowledgment.route -> Acknowledgment
                Price.route -> Price
                BillingData.route -> BillingData
                Invoice.route -> Invoice
                Other.route -> Other
                ExtraModules.route -> ExtraModules
                Maintenance.route -> Maintenance
                Donations.route -> Donations
                OnlineBackup.route -> OnlineBackup
                FleetConnection.route -> FleetConnection
                Tariffs.route -> Tariffs
                Export.route -> Export
                TariffForm.route -> TariffForm
                ManualQuote.route -> ManualQuote
                FuelPrices.route -> FuelPrices
                null -> Login
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
        }
    }
}
