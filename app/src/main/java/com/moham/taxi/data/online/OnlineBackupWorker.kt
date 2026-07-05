package com.moham.taxi.data.online

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moham.taxi.GestionTaxiApplication
import kotlinx.coroutines.flow.first

class OnlineBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GestionTaxiApplication
        val repository = app.onlineBackupRepository
        val ok = repository.createRestorePoint()
        return if (ok) {
            app.saveLastOnlineBackupSync(System.currentTimeMillis())
            app.savePendingBackup(false)
            Result.success()
        } else {
            Result.retry()
        }
    }
}
