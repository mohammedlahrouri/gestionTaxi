package com.moham.taxi.data.repository

import com.moham.taxi.data.dao.SurchargeDao
import com.moham.taxi.data.model.Surcharge
import kotlinx.coroutines.flow.Flow

class SurchargeRepository(private val surchargeDao: SurchargeDao) {
    val allSurcharges: Flow<List<Surcharge>> = surchargeDao.getAllSurcharges()

    suspend fun getSurchargeById(id: Long): Surcharge? {
        return surchargeDao.getSurchargeById(id)
    }

    suspend fun insertSurcharge(surcharge: Surcharge) {
        surchargeDao.insert(surcharge)
    }

    suspend fun updateSurcharge(surcharge: Surcharge) {
        surchargeDao.update(surcharge)
    }

    suspend fun deleteSurcharge(surcharge: Surcharge) {
        surchargeDao.delete(surcharge)
    }

    suspend fun deleteAllSurcharges() {
        surchargeDao.deleteAll()
    }
}
