package com.moham.taxi.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un método de pago
 */
@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ticketPhotoPolicy: String = POLICY_OPTIONAL
) {
    fun isDefault(): Boolean = isDefaultPaymentMethod(name)
    fun isNoPhoto(): Boolean = ticketPhotoPolicy == POLICY_NO_PHOTO
    fun isPhotoOptional(): Boolean = ticketPhotoPolicy == POLICY_OPTIONAL
    fun isPhotoMandatory(): Boolean = ticketPhotoPolicy == POLICY_MANDATORY

    companion object {
        const val POLICY_NO_PHOTO = "NO_PHOTO"
        const val POLICY_OPTIONAL = "OPTIONAL"
        const val POLICY_MANDATORY = "MANDATORY"

        const val METHOD_CASH = "Efectivo"
        const val METHOD_CARD = "Tarjeta"
        const val METHOD_APP = "Via App"
        const val METHOD_CANCELLED = "Cancelado"

        val DEFAULT_METHODS = listOf(
            METHOD_CASH,
            METHOD_CARD,
            METHOD_APP,
            METHOD_CANCELLED
        )

        fun isDefaultPaymentMethod(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val normalized = name.trim().lowercase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace(" ", "")
            return when (normalized) {
                "efectivo", "cash" -> true
                "tarjeta", "card" -> true
                "viaapp" -> true
                "cancelado", "cancelada", "rechazado" -> true
                else -> false
            }
        }

        fun getPaymentMethodSortOrder(name: String?): Int {
            if (name.isNullOrBlank()) return 99
            val normalized = name.trim().lowercase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace(" ", "")
            return when (normalized) {
                "efectivo", "cash" -> 1
                "tarjeta", "card" -> 2
                "viaapp" -> 3
                "cancelado", "cancelada", "rechazado" -> 4
                else -> 5
            }
        }
    }
}
