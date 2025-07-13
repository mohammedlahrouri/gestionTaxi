package com.moham.taxi.data.repository

import java.util.concurrent.ConcurrentHashMap

/**
 * Clase base para repositorios que implementan sistema de caché
 * Proporciona funcionalidad común de caché y limpieza
 */
abstract class BaseRepository {
    companion object {
        const val CACHE_EXPIRY_TIME = 5 * 60 * 1000L
        const val BATCH_SIZE = 100
        const val MAX_CACHE_SIZE = 50
        const val CACHE_CLEANUP_THRESHOLD = 40
        
        // Pagination constants
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
        const val INITIAL_LOAD_SIZE = 60
        const val PREFETCH_DISTANCE = 5
        
        // Query optimization constants
        const val RECENT_DAYS_LIMIT = 30
        const val SUMMARY_CACHE_SIZE = 20
        const val LAZY_LOAD_THRESHOLD = 1000
    }
    
    /**
     * Clase para manejar entradas de caché con tiempo de expiración
     */
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isValid(): Boolean = System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME
    }
    
    /**
     * Limpia un mapa de caché específico eliminando entradas expiradas
     * y manteniendo el tamaño bajo control
     */
    protected fun <K, V> cleanupCacheMap(cache: ConcurrentHashMap<K, CacheEntry<V>>) {
        // Eliminar entradas expiradas
        cache.entries.removeAll { !it.value.isValid() }
        
        // Si el caché sigue siendo muy grande, eliminar las entradas más antiguas
        if (cache.size > CACHE_CLEANUP_THRESHOLD) {
            val entriesToRemove = cache.size - (MAX_CACHE_SIZE / 2)
            cache.entries.sortedBy { it.value.timestamp }
                .take(entriesToRemove)
                .forEach { cache.remove(it.key) }
        }
    }
}