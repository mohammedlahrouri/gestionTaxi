package com.moham.taxi.data.repository

import com.moham.taxi.data.dao.TariffDao
import com.moham.taxi.data.model.Tariff
import kotlinx.coroutines.flow.Flow

class TariffRepository(private val tariffDao: TariffDao) {
    val allTariffs: Flow<List<Tariff>> = tariffDao.getAllTariffs()

    suspend fun getTariffById(id: Long): Tariff? {
        return tariffDao.getTariffById(id)
    }

    suspend fun insertTariff(tariff: Tariff) {
        tariffDao.insert(tariff)
    }

    suspend fun updateTariff(tariff: Tariff) {
        tariffDao.update(tariff)
    }

    suspend fun deleteTariff(tariff: Tariff) {
        tariffDao.delete(tariff)
    }

    suspend fun deleteAllTariffs() {
        tariffDao.deleteAll()
    }
}
