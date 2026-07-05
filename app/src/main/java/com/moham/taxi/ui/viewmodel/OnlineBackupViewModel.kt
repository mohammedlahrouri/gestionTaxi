package com.moham.taxi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.online.RestorePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Date

class OnlineBackupViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as GestionTaxiApplication

    private val _signedIn = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = _signedIn

    private val _lastBackupDate = MutableStateFlow<Date?>(null)
    val lastBackupDate: StateFlow<Date?> = _lastBackupDate
    private val _restorePoints = MutableStateFlow<List<RestorePoint>>(emptyList())
    val restorePoints: StateFlow<List<RestorePoint>> = _restorePoints

    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun refreshStatus() {
        viewModelScope.launch {
            try {
                _signedIn.value = application.googleDriveAuthManager.isSignedIn()
                if (_signedIn.value) {
                    _lastBackupDate.value = application.onlineBackupRepository.getLastBackupDate()
                    _restorePoints.value = application.onlineBackupRepository.getRestorePoints()
                } else {
                    _lastBackupDate.value = null
                    _restorePoints.value = emptyList()
                }
            } catch (e: UserRecoverableAuthIOException) {
                _errorMessage.value = "Necesitas conceder permisos de Google Drive para continuar."
            } catch (e: GoogleJsonResponseException) {
                _errorMessage.value = e.details?.message ?: "Error de Google Drive. Inténtalo de nuevo."
            } catch (e: IOException) {
                _errorMessage.value = "No hay conexión. Revisa tu red e inténtalo de nuevo."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error inesperado al sincronizar."
            }
        }
    }

    fun enableAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            application.saveOnlineBackupEnabled(enabled)
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                val ok = application.onlineBackupRepository.createRestorePoint()
                if (ok) {
                    application.saveLastOnlineBackupSync(System.currentTimeMillis())
                    _lastBackupDate.value = application.onlineBackupRepository.getLastBackupDate()
                    _restorePoints.value = application.onlineBackupRepository.getRestorePoints()
                }
            } catch (e: UserRecoverableAuthIOException) {
                _errorMessage.value = "Necesitas conceder permisos de Google Drive para continuar."
            } catch (e: GoogleJsonResponseException) {
                _errorMessage.value = e.details?.message ?: "Error de Google Drive. Inténtalo de nuevo."
            } catch (e: IOException) {
                _errorMessage.value = "No hay conexión. Revisa tu red e inténtalo de nuevo."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error inesperado al crear el backup."
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun restoreLatest() {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                application.onlineBackupRepository.restoreFromDrive()
            } catch (e: UserRecoverableAuthIOException) {
                _errorMessage.value = "Necesitas conceder permisos de Google Drive para continuar."
            } catch (e: GoogleJsonResponseException) {
                _errorMessage.value = e.details?.message ?: "Error de Google Drive. Inténtalo de nuevo."
            } catch (e: IOException) {
                _errorMessage.value = "No hay conexión. Revisa tu red e inténtalo de nuevo."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error inesperado al restaurar."
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun restorePoint(pointId: String) {
        viewModelScope.launch {
            _inProgress.value = true
            try {
                application.onlineBackupRepository.restoreFromRestorePoint(pointId)
            } catch (e: UserRecoverableAuthIOException) {
                _errorMessage.value = "Necesitas conceder permisos de Google Drive para continuar."
            } catch (e: GoogleJsonResponseException) {
                _errorMessage.value = e.details?.message ?: "Error de Google Drive. Inténtalo de nuevo."
            } catch (e: IOException) {
                _errorMessage.value = "No hay conexión. Revisa tu red e inténtalo de nuevo."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error inesperado al restaurar."
            } finally {
                _inProgress.value = false
            }
        }
    }

    fun signOut(activity: android.app.Activity?) {
        if (activity == null) {
            _errorMessage.value = "No se pudo cerrar la sesión."
            return
        }
        application.googleDriveAuthManager.signOut(activity) {
            viewModelScope.launch {
                application.saveOnlineBackupEnabled(false)
                _signedIn.value = false
                _lastBackupDate.value = null
                _restorePoints.value = emptyList()
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnlineBackupViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OnlineBackupViewModel(app) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
