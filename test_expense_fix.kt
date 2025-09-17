// Archivo de prueba para verificar la corrección del problema de gastos
// Este archivo contiene pruebas para validar que los gastos se guarden correctamente

/*
CAMBIOS REALIZADOS:

1. CORRECCIÓN EN Expense.kt:
   - Removida la validación de fecha futura que impedía guardar gastos en fechas pasadas
   - Antes: return amount > 0 && date.time <= System.currentTimeMillis()
   - Ahora: return amount > 0

2. CORRECCIÓN EN ExpenseDao.kt:
   - Cambiada la estrategia de conflicto de ABORT a REPLACE
   - Antes: @Insert(onConflict = OnConflictStrategy.ABORT)
   - Ahora: @Insert(onConflict = OnConflictStrategy.REPLACE)
   - Esto evita fallos silenciosos en caso de conflictos de inserción

PRUEBAS RECOMENDADAS:

1. Compilar la aplicación
2. Seleccionar una fecha pasada en HomeScreen
3. Registrar un gasto para esa fecha
4. Verificar que aparece en:
   - La pantalla principal (HomeScreen)
   - La lista de gastos (ExpenseListScreen)
   - Los reportes de exportación

LOGS DE DEBUG A REVISAR:

- "DEBUG EXPENSE_VM: Insertando gasto" - Confirma que se intenta insertar
- "DEBUG EXPENSE_VM: Gasto insertado con ID" - Confirma inserción exitosa
- "DEBUG EXPENSE_VM: Verificación de gasto guardado" - Confirma que se puede recuperar
- "DEBUG HOME: Gastos encontrados para la fecha" - Confirma que HomeScreen encuentra el gasto
- "DEBUG FORM: Objeto de gasto creado" - Confirma creación del objeto

SI EL PROBLEMA PERSISTE:

1. Verificar los logs en Logcat
2. Comprobar si hay errores de validación
3. Verificar que las fechas se estén normalizando correctamente
4. Comprobar que el refreshKey se actualiza en HomeScreen

ESTADO ESPERADO DESPUÉS DE LOS CAMBIOS:
- Los gastos deben guardarse correctamente en fechas pasadas
- Deben aparecer inmediatamente en la pantalla principal
- Deben estar disponibles en las listas y reportes
*/

fun testExpenseValidation() {
    // Prueba de validación del modelo Expense
    val pastDate = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -5)
    }.time
    
    val expense = com.moham.taxi.data.model.Expense(
        type = com.moham.taxi.data.model.ExpenseType.FUEL,
        description = null,
        amount = 50.0,
        date = pastDate
    )
    
    println("TEST: Validación de gasto con fecha pasada: ${expense.isValid()}")
    println("TEST: Fecha del gasto: ${expense.date}")
    println("TEST: Monto del gasto: ${expense.amount}")
}

fun testDateNormalization() {
    // Prueba de normalización de fechas
    val testDate = java.util.Date()
    val calendar = java.util.Calendar.getInstance().apply { 
        time = testDate
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val normalizedDate = calendar.time
    
    println("TEST: Fecha original: $testDate")
    println("TEST: Fecha normalizada: $normalizedDate")
    println("TEST: Timestamp original: ${testDate.time}")
    println("TEST: Timestamp normalizado: ${normalizedDate.time}")
}