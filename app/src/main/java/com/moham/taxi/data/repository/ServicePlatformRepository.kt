package com.moham.taxi.data.repository

import com.moham.taxi.data.dao.ServicePlatformDao
import com.moham.taxi.data.model.ServicePlatform
import kotlinx.coroutines.flow.Flow

class ServicePlatformRepository(private val servicePlatformDao: ServicePlatformDao) {
    val allServicePlatforms: Flow<List<ServicePlatform>> = servicePlatformDao.getAllServicePlatforms()

    suspend fun insert(servicePlatform: ServicePlatform): Long {
        return servicePlatformDao.insert(servicePlatform)
    }

    suspend fun update(servicePlatform: ServicePlatform) {
        servicePlatformDao.update(servicePlatform)
    }

    suspend fun delete(servicePlatform: ServicePlatform) {
        servicePlatformDao.delete(servicePlatform)
    }

    suspend fun getServicePlatformById(id: Long): ServicePlatform? {
        return servicePlatformDao.getServicePlatformById(id)
    }

    suspend fun servicePlatformExists(name: String): Boolean {
        return servicePlatformDao.servicePlatformExists(name)
    }
}
