package com.moham.taxi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.repository.ServicePlatformRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ServicePlatformViewModel(
    private val repository: ServicePlatformRepository
) : ViewModel() {
    val allServicePlatforms: Flow<List<ServicePlatform>> = repository.allServicePlatforms

    fun insert(servicePlatform: ServicePlatform) = viewModelScope.launch {
        repository.insert(servicePlatform)
    }

    fun update(servicePlatform: ServicePlatform) = viewModelScope.launch {
        repository.update(servicePlatform)
    }

    fun delete(servicePlatform: ServicePlatform) = viewModelScope.launch {
        repository.delete(servicePlatform)
    }

    class ServicePlatformViewModelFactory(
        private val repository: ServicePlatformRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ServicePlatformViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ServicePlatformViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
