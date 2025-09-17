package com.moham.taxi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.repository.TaxiRideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TaxiRideViewModel(private val repository: TaxiRideRepository) : ViewModel() {
    
    val allTaxiRides: Flow<List<TaxiRide>> = repository.allTaxiRides
    val todayRides: Flow<List<TaxiRide>> = repository.getTodayRides()
    val monthRides: Flow<List<TaxiRide>> = repository.getMonthRides()
    
    // Función para obtener las carreras de una fecha específica
    fun getSelectedDateRides(date: Date): Flow<List<TaxiRide>> {
        return repository.getRidesForDate(date)
    }
    
    fun insert(taxiRide: TaxiRide) = viewModelScope.launch {
        repository.insert(taxiRide)
    }
    
    fun update(taxiRide: TaxiRide) = viewModelScope.launch {
        repository.update(taxiRide)
    }
    
    fun delete(taxiRide: TaxiRide) = viewModelScope.launch {
        repository.delete(taxiRide)
    }
    
    suspend fun getTaxiRideById(id: Long): TaxiRide? {
        return repository.getTaxiRideById(id)
    }
    
    suspend fun getTodayIncome(): Double {
        return repository.getTodayIncome()
    }
    
    suspend fun getMonthIncome(): Double {
        return repository.getMonthIncome()
    }
    
    suspend fun getTodayIncomeByPaymentMethod(): Map<String, Double> {
        return repository.getTodayIncomeByPaymentMethod()
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para el mes actual.
     */
    suspend fun getTaxiMonthIncomeByPaymentMethod(): Map<String, Double> {
        val monthRange = com.moham.taxi.utils.DateUtils.getCurrentMonthRange()
        val monthRides = repository.getTaxiRidesByDateRange(monthRange.first, monthRange.second).first()
        
        // Calcular ingresos por método de pago
        val incomeByMethod = mutableMapOf<String, Double>()
        monthRides.forEach { ride ->
            val method = ride.paymentMethod
            incomeByMethod[method] = (incomeByMethod[method] ?: 0.0) + ride.price
        }
        
        return incomeByMethod
    }
    
    fun getRidesForDate(date: Date): Flow<List<TaxiRide>> {
        return repository.getRidesForDate(date)
    }
    
    suspend fun getIncomeForDate(date: Date): Double {
        return repository.getIncomeForDate(date)
    }
    
    suspend fun getIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        return repository.getIncomeByPaymentMethodForDate(date)
    }
    
    suspend fun getRideCountForDate(date: Date): Int {
        return repository.getRideCountForDate(date)
    }
    
    suspend fun getMonthRideCount(): Int {
        return repository.getMonthRideCount()
    }
    
    // Nuevas funciones para estadísticas más detalladas
    
    // Calcular el ingreso promedio por carrera para el día actual
    suspend fun getAverageIncomePerRide(date: Date): Double {
        val income = getIncomeForDate(date)
        val rideCount = getRideCountForDate(date)
        return if (rideCount > 0) income / rideCount else 0.0
    }
    
    // Calcular el ingreso promedio por carrera para el mes actual
    suspend fun getMonthAverageIncomePerRide(): Double {
        val income = getMonthIncome()
        val rideCount = getMonthRideCount()
        return if (rideCount > 0) income / rideCount else 0.0
    }
    
    // Obtener ingresos agrupados por día de la semana (para el mes actual)
    suspend fun getIncomeByDayOfWeek(): Map<String, Double> {
        val calendar = Calendar.getInstance()
        val rides = monthRides.first()
        
        val daysOfWeek = mapOf(
            Calendar.MONDAY to "Lunes",
            Calendar.TUESDAY to "Martes",
            Calendar.WEDNESDAY to "Miércoles",
            Calendar.THURSDAY to "Jueves",
            Calendar.FRIDAY to "Viernes",
            Calendar.SATURDAY to "Sábado",
            Calendar.SUNDAY to "Domingo"
        )
        
        val incomeByDay = mutableMapOf<String, Double>()
        
        // Inicializar todos los días con 0.0
        daysOfWeek.values.forEach { incomeByDay[it] = 0.0 }
        
        rides.forEach { ride ->
            calendar.time = ride.date
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = daysOfWeek[dayOfWeek] ?: "Desconocido"
            incomeByDay[dayName] = (incomeByDay[dayName] ?: 0.0) + ride.price
        }
        
        return incomeByDay
    }
    
    /**
     * Obtiene el ingreso total de la semana actual.
     */
    suspend fun getCurrentWeekIncome(): Double {
        return repository.getCurrentWeekIncome()
    }

    /**
     * Obtiene el número de carreras de la semana actual.
     */
    suspend fun getCurrentWeekRideCount(): Int {
        return repository.getCurrentWeekRideCount()
    }

    /**
     * Obtiene el ingreso total del mes actual.
     * Método con nombre específico para evitar ambigüedad con getMonthIncome() de ExpenseViewModel
     */
    suspend fun getTaxiMonthIncome(): Double {
        return repository.getMonthIncome()
    }
    
    /**
     * Obtiene el ingreso de la semana que contiene la fecha específica.
     */
    suspend fun getWeekIncomeForDate(date: Date): Double {
        return repository.getWeekIncomeForDate(date)
    }
    
    /**
     * Obtiene el número de carreras de la semana que contiene la fecha específica.
     */
    suspend fun getWeekRideCountForDate(date: Date): Int {
        return repository.getWeekRideCountForDate(date)
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para la semana que contiene la fecha específica.
     */
    suspend fun getWeekIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        return repository.getWeekIncomeByPaymentMethodForDate(date)
    }
    
    /**
     * Obtiene el ingreso del mes que contiene la fecha específica.
     */
    suspend fun getMonthIncomeForDate(date: Date): Double {
        return repository.getMonthIncomeForDate(date)
    }
    
    /**
     * Obtiene el número de carreras del mes que contiene la fecha específica.
     */
    suspend fun getMonthRideCountForDate(date: Date): Int {
        return repository.getMonthRideCountForDate(date)
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para el mes que contiene la fecha específica.
     */
    suspend fun getMonthIncomeByPaymentMethodForDate(date: Date): Map<String, Double> {
        return repository.getMonthIncomeByPaymentMethodForDate(date)
    }
    
    // Obtener comparativa de ingresos respecto a la semana anterior
    suspend fun getWeekOverWeekComparison(): Pair<Double, Double> {
        // Obtener el primer día de la semana configurado
        val context = repository.getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeek = application.getFirstDayOfWeek().first()
        
        // Semana actual
        val currentWeekRange = com.moham.taxi.utils.DateUtils.getWeekRange(Date(), firstDayOfWeek)
        val currentWeekRides = repository.getTaxiRidesByDateRange(currentWeekRange.first, currentWeekRange.second).first()
        val currentWeekIncome = currentWeekRides.sumOf { it.price }
        
        // Semana anterior
        val calendar = Calendar.getInstance()
        calendar.time = currentWeekRange.first
        calendar.add(Calendar.DATE, -7)
        val previousWeekStart = calendar.time
        val previousWeekRange = com.moham.taxi.utils.DateUtils.getWeekRange(previousWeekStart, firstDayOfWeek)
        
        val previousWeekRides = repository.getTaxiRidesByDateRange(previousWeekRange.first, previousWeekRange.second).first()
        val previousWeekIncome = previousWeekRides.sumOf { it.price }
        
        return Pair(currentWeekIncome, previousWeekIncome)
    }
    
    // Obtener las 5 rutas (origen-destino) más rentables
    suspend fun getTopRoutes(limit: Int = 5): List<Pair<String, Double>> {
        val rides = monthRides.first()
        val routeIncomes = mutableMapOf<String, Double>()
        
        rides.forEach { ride ->
            val route = "${ride.origin} - ${ride.destination}"
            routeIncomes[route] = (routeIncomes[route] ?: 0.0) + ride.price
        }
        
        return routeIncomes.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Pair(it.key, it.value) }
    }
    
    suspend fun getIncomeByHourOfDay(): Map<Int, Double> {
        val rides = monthRides.first()
        val incomeByHour = mutableMapOf<Int, Double>()
        
        // Inicializar todas las horas con 0.0
        (0..23).forEach { hour -> incomeByHour[hour] = 0.0 }
        
        rides.forEach { ride ->
            val calendar = Calendar.getInstance().apply { time = ride.date }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            incomeByHour[hour] = (incomeByHour[hour] ?: 0.0) + ride.price
        }
        
        return incomeByHour
    }
    
    /**
     * Obtiene el desglose de ingresos por método de pago para la semana actual.
     */
    suspend fun getCurrentWeekIncomeByPaymentMethod(): Map<String, Double> {
        // Obtener el primer día de la semana configurado
        val context = repository.getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeek = application.getFirstDayOfWeek().first()
        
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfWeek = calendar.time
        
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfWeek = calendar.time
        
        val weekRides = repository.getTaxiRidesByDateRange(startOfWeek, endOfWeek).first()
        
        // Calcular ingresos por método de pago
        val incomeByMethod = mutableMapOf<String, Double>()
        weekRides.forEach { ride ->
            val method = ride.paymentMethod
            incomeByMethod[method] = (incomeByMethod[method] ?: 0.0) + ride.price
        }
        
        return incomeByMethod
    }
    
    class TaxiRideViewModelFactory(private val repository: TaxiRideRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaxiRideViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaxiRideViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
