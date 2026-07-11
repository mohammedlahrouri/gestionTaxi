package com.moham.taxi.utils

import kotlin.math.round

/**
 * Utilidades para el manejo de precios en el taxímetro
 */
object PriceUtils {
    
    /**
     * Redondea un precio a la cifra más cercana terminada en 0 o 5
     * Ejemplo: 15.14 -> 15.15, 15.12 -> 15.10, 15.17 -> 15.20
     * 
     * @param price El precio original
     * @return El precio redondeado
     */
    fun roundToNearestFiveCents(price: Double): Double {
        // Multiplicar por 20 para trabajar con incrementos de 0.05
        // (0.05 * 20 = 1, entonces cada unidad representa 0.05)
        val multiplied = price * 20
        
        // Redondear al entero más cercano
        val rounded = round(multiplied)
        
        // Dividir por 20 para volver al precio original
        return rounded / 20
    }
    
    /**
     * Formatea un precio para mostrar siempre 2 decimales
     * 
     * @param price El precio a formatear
     * @return El precio formateado como string
     */
    fun formatPrice(price: Double): String {
        return String.format("%.2f", price)
    }
    
    /**
     * Redondea y formatea un precio en una sola operación
     * 
     * @param price El precio original
     * @return El precio redondeado y formateado
     */
    fun roundAndFormatPrice(price: Double): String {
        return formatPrice(roundToNearestFiveCents(price))
    }
    
    /**
     * Limpia el nombre de una tarifa para que solo muestre el número si tiene formato de "Tarifa X"
     */
    fun formatTariffNameForDisplay(name: String): String {
        val regex = Regex("^(?i)(tarifa|tariff|tarif)\\s*(.*)$")
        val match = regex.matchEntire(name)
        return if (match != null) {
            val suffix = match.groupValues[2]
            if (suffix.isNotBlank()) suffix else name
        } else {
            name
        }
    }
}