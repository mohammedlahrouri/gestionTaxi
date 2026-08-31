package com.moham.taxi.data.online

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moham.taxi.GestionTaxiApplication

/**
 * Worker encargado de realizar la sincronización de carreras y gastos locales
 * hacia Firebase Firestore en segundo plano, reintentando si no hay conectividad.
 */
class FirebaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GestionTaxiApplication
        val repository = app.firebaseSyncRepository

        if (!repository.isFleetConnected()) {
            // Si no está conectado a una flota, no hace falta reintentar
            return Result.failure()
        }

        val success = repository.syncRidesAndExpenses()
        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
