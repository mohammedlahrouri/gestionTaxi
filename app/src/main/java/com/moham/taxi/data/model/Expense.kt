package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Enum que representa los tipos de gastos
 */
enum class ExpenseType {
    FUEL,
    OTHER
}

/**
 * Modelo de datos para los gastos
 * Incluye validación de datos para asegurar la integridad
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["date", "type"]), // Compound index for date range + type queries
        Index(value = ["amount"]), // Index for amount-based queries
        Index(value = ["date", "amount"]) // Compound index for date range + amount queries
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: ExpenseType,
    val description: String?,
    val amount: Double,
    val date: Date = Date()
) {
    /**
     * Valida que los datos del gasto sean correctos
     * @return true si los datos son válidos, false en caso contrario
     */
    fun isValid(): Boolean {
        return amount > 0
        // Removida la validación de fecha futura para permitir gastos en fechas pasadas
    }
}
