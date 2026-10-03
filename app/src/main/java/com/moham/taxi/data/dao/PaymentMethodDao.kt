package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moham.taxi.data.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(paymentMethod: PaymentMethod): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(paymentMethod: PaymentMethod)

    @Delete
    suspend fun delete(paymentMethod: PaymentMethod)

    @Query(
        """
        SELECT * FROM payment_methods 
        ORDER BY 
            CASE 
                WHEN lower(name) IN ('efectivo', 'cash') THEN 1 
                WHEN lower(name) IN ('tarjeta', 'card') THEN 2 
                WHEN lower(replace(name, ' ', '')) = 'viaapp' THEN 3 
                WHEN lower(name) IN ('cancelado', 'rechazado', 'cancelada') THEN 4 
                ELSE 5 
            END ASC, 
            name COLLATE NOCASE ASC
        """
    )
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Query("SELECT * FROM payment_methods WHERE id = :id")
    suspend fun getPaymentMethodById(id: Long): PaymentMethod?

    @Query("SELECT * FROM payment_methods WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun getPaymentMethodByName(name: String): PaymentMethod?

    @Query("SELECT EXISTS(SELECT 1 FROM payment_methods WHERE name = :name LIMIT 1)")
    suspend fun paymentMethodExists(name: String): Boolean

    @Query("SELECT * FROM payment_methods")
    suspend fun getAllPaymentMethodsList(): List<PaymentMethod>
}
