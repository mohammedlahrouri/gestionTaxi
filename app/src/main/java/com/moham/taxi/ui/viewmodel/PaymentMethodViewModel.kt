package com.moham.taxi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.data.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PaymentMethodViewModel(private val repository: PaymentMethodRepository) : ViewModel() {
    
    val allPaymentMethods: Flow<List<PaymentMethod>> = repository.allPaymentMethods
    
    fun insert(paymentMethod: PaymentMethod) = viewModelScope.launch {
        repository.insert(paymentMethod)
    }
    
    fun update(paymentMethod: PaymentMethod) = viewModelScope.launch {
        repository.update(paymentMethod)
    }
    
    fun delete(paymentMethod: PaymentMethod) = viewModelScope.launch {
        repository.delete(paymentMethod)
    }
    
    suspend fun paymentMethodExists(name: String): Boolean {
        return repository.paymentMethodExists(name)
    }
    
    class PaymentMethodViewModelFactory(private val repository: PaymentMethodRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentMethodViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PaymentMethodViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
