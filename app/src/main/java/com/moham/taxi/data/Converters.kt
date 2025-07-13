package com.moham.taxi.data

import androidx.room.TypeConverter
import com.moham.taxi.data.model.ExpenseType
import java.util.Date

/**
 * Conversores para tipos de datos en Room
 * Estos convertidores permiten que Room almacene correctamente tipos complejos como Date y ExpenseType
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromExpenseType(value: ExpenseType): String {
        return value.name
    }

    @TypeConverter
    fun toExpenseType(value: String): ExpenseType {
        return try {
            ExpenseType.valueOf(value)
        } catch (e: Exception) {
            // En caso de error, retornar un valor por defecto
            ExpenseType.OTHER
        }
    }
} 
