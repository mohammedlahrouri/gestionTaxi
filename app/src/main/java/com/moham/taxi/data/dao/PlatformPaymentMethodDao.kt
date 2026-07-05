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
        SELECT pm.* FROM payment_methods pm
        INNER JOIN platform_payment_methods ppm ON ppm.paymentMethodId = pm.id
        WHERE ppm.platformId = :platformId
        ORDER BY pm.name ASC
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

