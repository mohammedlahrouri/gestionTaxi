// Debug para rastrear el flujo completo de fechas
// Este archivo ayudará a identificar dónde está la inconsistencia

import java.util.*
import java.text.SimpleDateFormat

/*
PROBLEMA IDENTIFICADO:
Posible inconsistencia entre:
1. HomeScreen.normalizeDate() - normaliza a medianoche
2. DateUtils.assignProperDate() - para fechas pasadas, normaliza a medianoche
3. Consultas de base de datos que usan DateUtils.getDayRange()

FLUJO ACTUAL:
1. Usuario selecciona fecha en HomeScreen
2. HomeScreen normaliza la fecha (normalizeDate)
3. HomeScreen pasa timestamp normalizado a ExpenseFormScreen
4. ExpenseFormScreen convierte timestamp a Date (useDate)
5. ExpenseFormScreen llama assignProperDate(useDate)
6. assignProperDate normaliza nuevamente si no es hoy
7. Se guarda en BD con fecha normalizada
8. HomeScreen consulta con fecha normalizada

POSIBLE PROBLEMA:
Doble normalización o diferencias en la normalización
*/

fun debugDateFlow() {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault())
    
    // Simular fecha seleccionada (ayer)
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 14) // 2 PM
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 45)
        set(Calendar.MILLISECOND, 123)
    }.time
    
    println("=== DEBUG FLUJO DE FECHAS ===")
    println("1. Fecha original seleccionada: ${dateFormat.format(yesterday)}")
    
    // Paso 1: HomeScreen normaliza
    val normalizedByHome = normalizeDate(yesterday)
    println("2. Normalizada por HomeScreen: ${dateFormat.format(normalizedByHome)}")
    
    // Paso 2: Se pasa como timestamp
    val timestamp = normalizedByHome.time
    println("3. Timestamp pasado: $timestamp")
    
    // Paso 3: ExpenseFormScreen convierte de vuelta
    val useDate = Date(timestamp)
    println("4. useDate en ExpenseFormScreen: ${dateFormat.format(useDate)}")
    
    // Paso 4: assignProperDate procesa
    val finalDate = assignProperDate(useDate)
    println("5. Fecha final (assignProperDate): ${dateFormat.format(finalDate)}")
    
    // Paso 5: Consulta en HomeScreen
    val queryRange = getDayRange(normalizedByHome)
    println("6. Rango de consulta en HomeScreen:")
    println("   Inicio: ${dateFormat.format(queryRange.first)}")
    println("   Fin: ${dateFormat.format(queryRange.second)}")
    
    // Verificar si la fecha final está en el rango
    val isInRange = finalDate >= queryRange.first && finalDate <= queryRange.second
    println("7. ¿Fecha final está en rango de consulta? $isInRange")
    
    if (!isInRange) {
        println("❌ PROBLEMA ENCONTRADO: La fecha guardada NO está en el rango de consulta")
    } else {
        println("✅ OK: La fecha guardada está en el rango de consulta")
    }
}

// Funciones copiadas para debug
fun normalizeDate(date: Date): Date {
    val calendar = Calendar.getInstance().apply { 
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.time
}

fun assignProperDate(selectedDate: Date): Date {
    val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }
    val todayCalendar = Calendar.getInstance()
    
    val sameDay = selectedCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                  selectedCalendar.get(Calendar.MONTH) == todayCalendar.get(Calendar.MONTH) &&
                  selectedCalendar.get(Calendar.DAY_OF_MONTH) == todayCalendar.get(Calendar.DAY_OF_MONTH)
    
    return if (sameDay) {
        Date()
    } else {
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedCalendar.set(Calendar.MINUTE, 0)
        selectedCalendar.set(Calendar.SECOND, 0)
        selectedCalendar.set(Calendar.MILLISECOND, 0)
        selectedCalendar.time
    }
}

fun getDayRange(date: Date): Pair<Date, Date> {
    val startCalendar = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val endCalendar = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    
    return Pair(startCalendar.time, endCalendar.time)
}