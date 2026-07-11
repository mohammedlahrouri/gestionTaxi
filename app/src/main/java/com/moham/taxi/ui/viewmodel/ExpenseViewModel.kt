package com.moham.taxi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {
    
    val allExpenses: Flow<List<Expense>> = repository.allExpenses
    val todayExpenses: Flow<List<Expense>> = repository.getTodayExpenses()
    val monthExpenses: Flow<List<Expense>> = repository.getMonthExpenses()
    val maintenanceExpenses: Flow<List<Expense>> = repository.getExpensesByType(ExpenseType.MAINTENANCE)
    
    // Función para obtener los gastos de una fecha específica
    fun getSelectedDateExpenses(date: Date): Flow<List<Expense>> {
        return repository.getExpensesForDate(date)
    }
    
    fun insert(expense: Expense) = viewModelScope.launch {
        try {
            println("DEBUG EXPENSE_VM: Insertando gasto: tipo=${expense.type}, monto=${expense.amount}, fecha=${expense.date}")
            if (!expense.isValid()) {
                println("ERROR EXPENSE_VM: El gasto no es válido: ${expense}")
                return@launch
            }
            
            val id = repository.insert(expense)
            println("DEBUG EXPENSE_VM: Gasto insertado con ID: $id")
            
            // Verificar que se haya guardado correctamente
            val saved = repository.getExpenseById(id)
            if (saved != null) {
                println("DEBUG EXPENSE_VM: Verificación de gasto guardado: ID=${saved.id}, tipo=${saved.type}, monto=${saved.amount}, fecha=${saved.date}")
            } else {
                println("ERROR EXPENSE_VM: No se pudo verificar el gasto guardado con ID: $id")
            }
        } catch (e: Exception) {
            println("ERROR EXPENSE_VM: Error al insertar gasto: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun update(expense: Expense) = viewModelScope.launch {
        repository.update(expense)
    }
    
    fun delete(expense: Expense) = viewModelScope.launch {
        repository.delete(expense)
    }
    
    suspend fun getExpenseById(id: Long): Expense? {
        return repository.getExpenseById(id)
    }
    
    suspend fun getTodayExpensesTotal(): Double {
        return repository.getTodayExpensesTotal()
    }
    
    suspend fun getMonthExpensesTotal(): Double {
        return repository.getMonthExpensesTotal()
    }
    
    suspend fun getTodayFuelExpenses(): Double {
        return repository.getTodayFuelExpenses()
    }
    
    /**
     * Obtiene el total de gastos en combustible para el mes actual.
     */
    suspend fun getMonthFuelExpenses(): Double {
        return repository.getMonthFuelExpenses()
    }
    
    fun getExpensesForDate(date: Date): Flow<List<Expense>> {
        return repository.getExpensesForDate(date)
    }
    
    suspend fun getExpensesTotalForDate(date: Date): Double {
        return repository.getExpensesTotalForDate(date)
    }
    
    suspend fun getFuelExpensesForDate(date: Date): Double {
        return repository.getFuelExpensesForDate(date)
    }
    
    suspend fun getExpenseCountForDate(date: Date): Int {
        return repository.getExpenseCountForDate(date)
    }
    
    // Nuevas funciones para estadísticas detalladas
    
    // Obtener gastos agrupados por tipo para el mes actual
    suspend fun getExpensesByCategory(): Map<String, Double> {
        val expenses = monthExpenses.first()
        val expensesByCategory = mutableMapOf<String, Double>()
        
        expenses.forEach { expense ->
            val category = when (expense.type) {
                ExpenseType.FUEL -> ExpenseType.FUEL.name
                else -> ExpenseType.OTHER.name
            }
            expensesByCategory[category] = (expensesByCategory[category] ?: 0.0) + expense.amount
        }
        
        return expensesByCategory
    }
    
    // Obtener gastos por tipo para una fecha específica
    suspend fun getExpensesByCategoryForDate(date: Date): Map<String, Double> {
        val expenses = repository.getExpensesForDate(date).first()
        val expensesByCategory = mutableMapOf<String, Double>()
        
        expenses.forEach { expense ->
            val category = when (expense.type) {
                ExpenseType.FUEL -> ExpenseType.FUEL.name
                else -> ExpenseType.OTHER.name
            }
            expensesByCategory[category] = (expensesByCategory[category] ?: 0.0) + expense.amount
        }
        
        return expensesByCategory
    }
    
    // Obtener el porcentaje de gastos por combustible frente al total (mes)
    suspend fun getFuelExpensesPercentage(): Double {
        val fuelExpenses = getMonthFuelExpenses()
        val totalExpenses = getMonthExpensesTotal()
        
        return if (totalExpenses > 0) (fuelExpenses / totalExpenses) * 100 else 0.0
    }
    
    // Obtener el porcentaje de gastos por combustible frente al total (día)
    suspend fun getFuelExpensesPercentageForDate(date: Date): Double {
        val fuelExpenses = getFuelExpensesForDate(date)
        val totalExpenses = getExpensesTotalForDate(date)
        
        return if (totalExpenses > 0) (fuelExpenses / totalExpenses) * 100 else 0.0
    }
    
    // Obtener gastos para la semana actual
    suspend fun getCurrentWeekExpenses(): Double {
        return repository.getCurrentWeekExpenses()
    }
    
    // Obtener el número de gastos para la semana actual
    suspend fun getCurrentWeekExpenseCount(): Int {
        return repository.getCurrentWeekExpenseCount()
    }
    
    // Obtener el total de gastos en combustible para la semana actual
    suspend fun getCurrentWeekFuelExpenses(): Double {
        return repository.getCurrentWeekFuelExpenses()
    }
    
    // Obtener comparativa de gastos respecto a la semana anterior
    suspend fun getWeekOverWeekExpenseComparison(): Pair<Double, Double> {
        // Obtener el primer día de la semana configurado
        val context = repository.getContext()
        val application = context.applicationContext as GestionTaxiApplication
        val firstDayOfWeek = application.getFirstDayOfWeek().first()
        
        // Semana actual
        val currentWeekRange = com.moham.taxi.utils.DateUtils.getWeekRange(Date(), firstDayOfWeek)
        val currentWeekExpenses = repository.getExpensesByDateRange(currentWeekRange.first, currentWeekRange.second).first()
        val currentWeekTotal = currentWeekExpenses.sumOf { it.amount }
        
        // Semana anterior
        val calendar = Calendar.getInstance()
        calendar.time = currentWeekRange.first
        calendar.add(Calendar.DATE, -7)
        val previousWeekStart = calendar.time
        val previousWeekRange = com.moham.taxi.utils.DateUtils.getWeekRange(previousWeekStart, firstDayOfWeek)
        
        val previousWeekExpenses = repository.getExpensesByDateRange(previousWeekRange.first, previousWeekRange.second).first()
        val previousWeekTotal = previousWeekExpenses.sumOf { it.amount }
        
        return Pair(currentWeekTotal, previousWeekTotal)
    }
    
    // Obtener los 5 gastos más grandes del mes
    suspend fun getTopExpenses(limit: Int = 5): List<Expense> {
        val expenses = monthExpenses.first()
        return expenses.sortedByDescending { it.amount }.take(limit)
    }
    
    /**
     * Obtiene el número de gastos para el mes actual.
     */
    suspend fun getMonthExpenseCount(): Int {
        return repository.getMonthExpenseCount()
    }
    
    /**
     * Obtiene el total de gastos para la semana que contiene la fecha específica.
     */
    suspend fun getWeekExpensesForDate(date: Date): Double {
        return repository.getWeekExpensesForDate(date)
    }
    
    /**
     * Obtiene el número de gastos para la semana que contiene la fecha específica.
     */
    suspend fun getWeekExpenseCountForDate(date: Date): Int {
        return repository.getWeekExpenseCountForDate(date)
    }
    
    /**
     * Obtiene el total de gastos de combustible para la semana que contiene la fecha específica.
     */
    suspend fun getWeekFuelExpensesForDate(date: Date): Double {
        return repository.getWeekFuelExpensesForDate(date)
    }
    
    /**
     * Obtiene el total de gastos para el mes que contiene la fecha específica.
     */
    suspend fun getMonthExpensesForDate(date: Date): Double {
        return repository.getMonthExpensesForDate(date)
    }
    
    /**
     * Obtiene el número de gastos para el mes que contiene la fecha específica.
     */
    suspend fun getMonthExpenseCountForDate(date: Date): Int {
        return repository.getMonthExpenseCountForDate(date)
    }
    
    /**
     * Obtiene el total de gastos de combustible para el mes que contiene la fecha específica.
     */
    suspend fun getMonthFuelExpensesForDate(date: Date): Double {
        return repository.getMonthFuelExpensesForDate(date)
    }

    suspend fun getYearExpensesForDate(date: Date): Double {
        return repository.getYearExpensesForDate(date)
    }

    suspend fun getYearExpenseCountForDate(date: Date): Int {
        return repository.getYearExpenseCountForDate(date)
    }

    suspend fun getYearFuelExpensesForDate(date: Date): Double {
        return repository.getYearFuelExpensesForDate(date)
    }
    
    class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
