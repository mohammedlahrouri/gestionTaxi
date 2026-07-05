package com.moham.taxi.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.model.ServiceMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DriveLinkChoice {
    NOW,
    LATER
}

data class PlatformFormState(
    val name: String = "",
    val applyCommission: Boolean = false,
    val commissionPercent: String = "",
    val taxPercent: String = "",
    val useUsMath: Boolean = false,
    val serviceMode: ServiceMode = ServiceMode.BOTH,
    val paymentMethods: Map<String, Boolean> = emptyMap(),
    val otherPaymentMethodInput: String = "",
    val otherPaymentMethods: List<String> = emptyList(),
    val nameError: Boolean = false,
    val commissionError: Boolean = false
)

data class BillingFormState(
    val name: String = "",
    val nif: String = "",
    val license: String = "",
    val street: String = "",
    val city: String = "",
    val postalCode: String = ""
)

data class OnboardingUiState(
    val isBusy: Boolean = false,
    val driveSignedIn: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(private val application: GestionTaxiApplication) : ViewModel() {
    private val cashLabel = application.getString(R.string.payment_cash)
    private val cardLabel = application.getString(R.string.payment_card)
    private val viaAppLabel = application.getString(R.string.payment_via_app)
    private val subscribersLabel = application.getString(R.string.payment_subscribers)
    private val pendingLabel = application.getString(R.string.payment_pending)
    private val fixedPaymentMethods = listOf(cashLabel, cardLabel, viaAppLabel, subscribersLabel, pendingLabel)

    val servicePlatforms: StateFlow<List<ServicePlatform>> = application.servicePlatformRepository.allServicePlatforms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val currentCity: StateFlow<String> = application.getCurrentCity().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "Madrid"
    )

    private val _platformForm = MutableStateFlow(
        PlatformFormState(
            paymentMethods = fixedPaymentMethods.associateWith { it == cashLabel || it == cardLabel }
        )
    )
    val platformForm = _platformForm.asStateFlow()

    private val _billingForm = MutableStateFlow(BillingFormState())
    val billingForm = _billingForm.asStateFlow()

    private val _cityDraft = MutableStateFlow("Madrid")
    val cityDraft = _cityDraft.asStateFlow()

    private val _driveLinkChoice = MutableStateFlow(DriveLinkChoice.LATER)
    val driveLinkChoice = _driveLinkChoice.asStateFlow()

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensureDefaultDirectPlatform()
            refreshDriveSignedIn()
        }
        viewModelScope.launch {
            currentCity.collect { city ->
                if (_cityDraft.value.isBlank() || _cityDraft.value == "Madrid") {
                    _cityDraft.value = city
                }
            }
        }
    }

    fun setDriveLinkChoice(choice: DriveLinkChoice) {
        _driveLinkChoice.value = choice
    }

    fun updatePlatformName(value: String) {
        _platformForm.value = _platformForm.value.copy(name = value, nameError = false)
    }

    fun setApplyCommission(enabled: Boolean) {
        val current = _platformForm.value
        _platformForm.value = current.copy(
            applyCommission = enabled,
            commissionPercent = if (enabled) current.commissionPercent else "",
            taxPercent = if (enabled) current.taxPercent else "",
            useUsMath = if (enabled) current.useUsMath else false,
            commissionError = false
        )
    }

    fun updateCommissionPercent(value: String) {
        _platformForm.value = _platformForm.value.copy(commissionPercent = value, commissionError = false)
    }

    fun updateTaxPercent(value: String) {
        _platformForm.value = _platformForm.value.copy(taxPercent = value)
    }

    fun setUseUsMath(enabled: Boolean) {
        _platformForm.value = _platformForm.value.copy(useUsMath = enabled)
    }

    fun setServiceMode(mode: ServiceMode) {
        _platformForm.value = _platformForm.value.copy(serviceMode = mode)
    }

    fun togglePaymentMethod(name: String) {
        val current = _platformForm.value
        val next = current.paymentMethods.toMutableMap()
        next[name] = !(next[name] ?: false)
        _platformForm.value = current.copy(paymentMethods = next)
    }

    fun updateOtherPaymentMethodInput(value: String) {
        _platformForm.value = _platformForm.value.copy(otherPaymentMethodInput = value)
    }

    fun addOtherPaymentMethodFromInput() {
        val current = _platformForm.value
        val name = current.otherPaymentMethodInput.trim()
        if (name.isBlank()) return
        val next = (current.otherPaymentMethods + name).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        _platformForm.value = current.copy(
            otherPaymentMethods = next,
            otherPaymentMethodInput = ""
        )
    }

    fun removeOtherPaymentMethod(name: String) {
        val current = _platformForm.value
        val next = current.otherPaymentMethods.filterNot { it.equals(name, ignoreCase = true) }
        _platformForm.value = current.copy(otherPaymentMethods = next)
    }

    fun updateBillingName(value: String) {
        _billingForm.value = _billingForm.value.copy(name = value)
    }

    fun updateBillingNif(value: String) {
        _billingForm.value = _billingForm.value.copy(nif = value)
    }

    fun updateBillingLicense(value: String) {
        _billingForm.value = _billingForm.value.copy(license = value)
    }

    fun updateBillingStreet(value: String) {
        _billingForm.value = _billingForm.value.copy(street = value)
    }

    fun updateBillingCity(value: String) {
        _billingForm.value = _billingForm.value.copy(city = value)
    }

    fun updateBillingPostalCode(value: String) {
        _billingForm.value = _billingForm.value.copy(postalCode = value)
    }

    fun updateCityDraft(value: String) {
        _cityDraft.value = value
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    suspend fun restoreFromDrive(): Boolean {
        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
        return try {
            val ok = application.onlineBackupRepository.restoreFromDrive()
            _uiState.value = _uiState.value.copy(isBusy = false)
            ok
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                errorMessage = e.message ?: application.getString(R.string.onboarding_restore_drive_failed)
            )
            false
        }
    }

    suspend fun restoreFromLocal(uri: Uri): Boolean {
        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
        return try {
            kotlinx.coroutines.delay(5000)
            val ok = application.backupService.restoreBackup(uri)
            _uiState.value = _uiState.value.copy(isBusy = false)
            ok
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                errorMessage = e.message ?: application.getString(R.string.onboarding_restore_local_failed)
            )
            false
        }
    }

    suspend fun submitPlatform(): Boolean {
        val current = _platformForm.value
        val name = current.name.trim()
        var nameError = false
        var commissionError = false

        if (name.isBlank()) {
            nameError = true
        }

        val commission = if (current.applyCommission) parsePercent(current.commissionPercent) else null
        val tax = if (current.applyCommission) parsePercent(current.taxPercent) else null
        if (current.applyCommission) {
            if (commission == null) commissionError = true
        }

        if (nameError || commissionError) {
            _platformForm.value = current.copy(
                nameError = nameError,
                commissionError = commissionError
            )
            return false
        }

        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
        return try {
            val selectedFixed = current.paymentMethods.filterValues { it }.keys
            val others = current.otherPaymentMethods.map { it.trim() }.filter { it.isNotBlank() }
            val allMethods = (selectedFixed + others).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val paymentMethodIds = allMethods.map { method ->
                application.paymentMethodRepository.getOrCreatePaymentMethodId(method)
            }

            val platformId = application.servicePlatformRepository.insert(
                ServicePlatform(
                    name = name,
                    commissionPercentage = commission,
                    commissionVat = tax,
                    useAlternativeMath = current.useUsMath,
                    serviceMode = current.serviceMode
                )
            )
            application.paymentMethodRepository.setPaymentMethodsForPlatform(platformId, paymentMethodIds)

            _platformForm.value = PlatformFormState(
                paymentMethods = fixedPaymentMethods.associateWith { it == cashLabel || it == cardLabel }
            )
            _uiState.value = _uiState.value.copy(isBusy = false)
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                errorMessage = e.message ?: application.getString(R.string.onboarding_save_platform_failed)
            )
            false
        }
    }

    suspend fun saveBillingData() {
        val current = _billingForm.value
        application.saveBillingData(
            name = current.name,
            nif = current.nif,
            license = current.license,
            street = current.street,
            city = current.city,
            postalCode = current.postalCode
        )
    }

    suspend fun applyCityIfChanged() {
        val draft = _cityDraft.value.trim()
        val existing = currentCity.value.trim()
        if (draft.isBlank() || draft.equals(existing, ignoreCase = true)) return
        application.saveCurrentCity(draft)
        application.tariffRepository.deleteAllTariffs()
        application.surchargeRepository.deleteAllSurcharges()
    }

    suspend fun enableOnlineBackup() {
        application.saveOnlineBackupEnabled(true)
        refreshDriveSignedIn()
    }

    suspend fun refreshDriveSignedIn() {
        val signedIn = application.googleDriveAuthManager.isSignedIn()
        _uiState.value = _uiState.value.copy(driveSignedIn = signedIn)
    }

    suspend fun completeOnboarding() {
        application.setHasCompletedOnboarding(true)
        application.setFirstRunCompleted()
    }

    private suspend fun ensureDefaultDirectPlatform() {
        val exists = application.servicePlatformRepository.servicePlatformExists("Directo")
        if (exists) return
        application.servicePlatformRepository.insert(ServicePlatform(name = "Directo"))
    }

    private fun parsePercent(value: String): Double? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isBlank()) return null
        return normalized.toDoubleOrNull()
    }

    class Factory(private val application: GestionTaxiApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OnboardingViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
