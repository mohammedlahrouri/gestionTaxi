package com.moham.taxi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.data.model.Surcharge
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TariffViewModel(private val application: GestionTaxiApplication) : ViewModel() {
    private val repository = application.tariffRepository
    private val surchargeRepo = application.surchargeRepository

    val currentCity: StateFlow<String> = application.getCurrentCity().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Madrid"
    )

    val madridCalculationsCount: StateFlow<Int> = application.getMadridCalculationsCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun incrementMadridCalculationsCount() = viewModelScope.launch {
        application.incrementMadridCalculationsCount()
    }

    val allTariffs: StateFlow<List<Tariff>> = repository.allTariffs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allSurcharges: StateFlow<List<Surcharge>> = surchargeRepo.allSurcharges.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun changeCity(newCity: String) = viewModelScope.launch {
        application.saveCurrentCity(newCity)
        repository.deleteAllTariffs()
        surchargeRepo.deleteAllSurcharges()
    }

    fun restoreDefaults() = viewModelScope.launch {
        application.saveCurrentCity("Madrid")
        repository.deleteAllTariffs()
        surchargeRepo.deleteAllSurcharges()
        repository.insertTariff(Tariff(name = "Tarifa 1", isFixed = false, baseFare = 2.50, pricePerKm = 1.30, surcharge = 0.0))
        repository.insertTariff(Tariff(name = "Tarifa 2", isFixed = false, baseFare = 3.15, pricePerKm = 1.50, surcharge = 0.0))
        repository.insertTariff(Tariff(name = "Aeropuerto", isFixed = true, fixedPrice = 33.0))
    }

    fun insert(tariff: Tariff) = viewModelScope.launch {
        repository.insertTariff(tariff)
    }

    fun update(tariff: Tariff) = viewModelScope.launch {
        repository.updateTariff(tariff)
    }

    fun delete(tariff: Tariff) = viewModelScope.launch {
        repository.deleteTariff(tariff)
    }

    suspend fun getTariffById(id: Long): Tariff? {
        return repository.getTariffById(id)
    }

    fun insertSurcharge(surcharge: Surcharge) = viewModelScope.launch {
        surchargeRepo.insertSurcharge(surcharge)
    }

    fun updateSurcharge(surcharge: Surcharge) = viewModelScope.launch {
        surchargeRepo.updateSurcharge(surcharge)
    }

    fun deleteSurcharge(surcharge: Surcharge) = viewModelScope.launch {
        surchargeRepo.deleteSurcharge(surcharge)
    }

    suspend fun getSurchargeById(id: Long): Surcharge? {
        return surchargeRepo.getSurchargeById(id)
    }

    class Factory(private val application: GestionTaxiApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TariffViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TariffViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
