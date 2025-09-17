// Archivo de debug para investigar el problema con los gastos
// Este archivo ayudará a identificar qué está pasando con las fechas

/*
PROBLEMA REPORTADO:
- Los gastos no aparecen en la pantalla principal cuando se registran en fechas seleccionadas

ANÁLISIS DEL CÓDIGO:
1. HomeScreen.normalizeDate() - Normaliza fechas a medianoche ✓
2. HomeScreen.navigateWithDate() - Usa fecha normalizada ✓
3. ExpenseFormScreen.saveExpense() - Usa DateUtils.assignProperDate() ✓
4. DateUtils.assignProperDate() - Para fechas pasadas, normaliza a medianoche ✓
5. ExpenseRepository.getExpensesForDate() - Usa DateUtils.getDayRange() ✓

POSIBLES CAUSAS:
1. Problema en la sincronización de datos entre pantallas
2. Problema en el refreshKey de HomeScreen
3. Problema en la invalidación de caché
4. Problema en las consultas de base de datos

SOLUCIONES A PROBAR:
1. Verificar que el refreshKey se actualice correctamente
2. Verificar que la navegación de vuelta actualice los datos
3. Agregar logs de debug para rastrear el flujo de datos
4. Verificar que las fechas se estén comparando correctamente

PRUEBAS RECOMENDADAS:
1. Registrar un gasto en una fecha pasada
2. Verificar en la base de datos que se guardó correctamente
3. Verificar que la consulta en HomeScreen devuelva el gasto
4. Verificar que el refreshKey se incremente
*/

// Función de debug para verificar fechas
fun debugDateComparison() {
    val selectedDate = Date() // Fecha actual
    val normalizedDate = normalizeDate(selectedDate)
    val assignedDate = DateUtils.assignProperDate(normalizedDate)
    
    println("DEBUG: Fecha seleccionada: $selectedDate")
    println("DEBUG: Fecha normalizada: $normalizedDate")
    println("DEBUG: Fecha asignada: $assignedDate")
    
    val dayRange = DateUtils.getDayRange(assignedDate)
    println("DEBUG: Rango del día: ${dayRange.first} - ${dayRange.second}")
}

// Función para normalizar fecha (copia de HomeScreen)
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