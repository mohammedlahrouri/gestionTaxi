package com.moham.taxi.data.repository

import androidx.room.withTransaction
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.dao.PaymentMethodDao
import com.moham.taxi.data.model.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PaymentMethodRepository(private val paymentMethodDao: PaymentMethodDao, private val database: AppDatabase) : BaseRepository() {
    
    // Sistema de caché para métodos de pago
    private val paymentMethodCache = ConcurrentHashMap<Long, CacheEntry<PaymentMethod>>()
    
    /**
     * Limpia las entradas de caché expiradas
     */
    private fun cleanupCache() {
        cleanupCacheMap(paymentMethodCache)
    }
    
    val allPaymentMethods: Flow<List<PaymentMethod>> = paymentMethodDao.getAllPaymentMethods()
    
    suspend fun insert(paymentMethod: PaymentMethod): Long = withContext(Dispatchers.IO) {
        val id = paymentMethodDao.insert(paymentMethod)
        paymentMethodCache[id] = CacheEntry(paymentMethod.copy(id = id))
        cleanupCache()
        id
    }
    
    suspend fun update(paymentMethod: PaymentMethod) = withContext(Dispatchers.IO) {
        paymentMethodDao.update(paymentMethod)
        paymentMethodCache[paymentMethod.id] = CacheEntry(paymentMethod)
        cleanupCache()
    }
    
    suspend fun delete(paymentMethod: PaymentMethod) = withContext(Dispatchers.IO) {
        paymentMethodDao.delete(paymentMethod)
        paymentMethodCache.remove(paymentMethod.id)
    }
    
    suspend fun insertMultiple(paymentMethods: List<PaymentMethod>): List<Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            paymentMethods.chunked(BATCH_SIZE).flatMap { batch ->
                batch.map { paymentMethod ->
                    val id = paymentMethodDao.insert(paymentMethod)
                    paymentMethodCache[id] = CacheEntry(paymentMethod.copy(id = id))
                    id
                }
            }
        }.also {
            cleanupCache()
        }
    }
    
    suspend fun getPaymentMethodById(id: Long): PaymentMethod? {
        // Verificar caché primero
        paymentMethodCache[id]?.let { cacheEntry ->
            if (cacheEntry.isValid()) {
                return cacheEntry.data
            } else {
                paymentMethodCache.remove(id)
            }
        }
        
        // Si no está en caché o expiró, obtener de la base de datos
        return paymentMethodDao.getPaymentMethodById(id)?.also { paymentMethod ->
            paymentMethodCache[id] = CacheEntry(paymentMethod)
        }
    }
    
    suspend fun paymentMethodExists(name: String): Boolean {
        return paymentMethodDao.paymentMethodExists(name)
    }

    fun getPaymentMethodsForPlatform(platformId: Long): Flow<List<PaymentMethod>> {
        return database.platformPaymentMethodDao().getPaymentMethodsForPlatform(platformId)
    }

    suspend fun setPaymentMethodsForPlatform(platformId: Long, paymentMethodIds: List<Long>) = withContext(Dispatchers.IO) {
        database.platformPaymentMethodDao().replaceForPlatform(platformId, paymentMethodIds)
    }

    suspend fun getOrCreatePaymentMethodId(name: String): Long = withContext(Dispatchers.IO) {
        val existing = paymentMethodDao.getPaymentMethodByName(name)
        if (existing != null) return@withContext existing.id
        insert(PaymentMethod(name = name))
    }
}
