package com.moham.taxi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.model.PlatformPaymentMethodCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformPaymentMethodDao {
    @Query(
        """
        SELECT DISTINCT pm.* FROM payment_methods pm
        INNER JOIN platform_payment_methods ppm ON ppm.paymentMethodId = pm.id
        WHERE ppm.platformId = :platformId
        ORDER BY 
            CASE 
                WHEN lower(pm.name) IN ('efectivo', 'cash') THEN 1 
                WHEN lower(pm.name) IN ('tarjeta', 'card') THEN 2 
                WHEN lower(replace(pm.name, ' ', '')) = 'viaapp' THEN 3 
                WHEN lower(pm.name) IN ('cancelado', 'rechazado', 'cancelada') THEN 4 
                ELSE 5 
            END ASC, 
            pm.name COLLATE NOCASE ASC
        """
    )
    fun getPaymentMethodsForPlatform(platformId: Long): Flow<List<PaymentMethod>>

    @Query("DELETE FROM platform_payment_methods WHERE platformId = :platformId")
    suspend fun deleteForPlatform(platformId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(refs: List<PlatformPaymentMethodCrossRef>)

    @Transaction
    suspend fun replaceForPlatform(platformId: Long, paymentMethodIds: List<Long>) {
        deleteForPlatform(platformId)
        if (paymentMethodIds.isEmpty()) return
        insertAll(paymentMethodIds.distinct().map { id ->
            PlatformPaymentMethodCrossRef(platformId = platformId, paymentMethodId = id)
        })
    }
}

