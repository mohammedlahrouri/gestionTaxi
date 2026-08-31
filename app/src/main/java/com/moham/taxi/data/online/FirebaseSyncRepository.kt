package com.moham.taxi.data.online

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Repositorio encargado de sincronizar los datos locales con Firebase Firestore
 * y gestionar la autenticación y enlace con la flota.
 */
class FirebaseSyncRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Verifica si el usuario actual está enlazado a una flota local y remotamente.
     */
    suspend fun isFleetConnected(): Boolean {
        val app = context.applicationContext as GestionTaxiApplication
        val isConnectedLocal = app.isFleetConnected().first()
        return isConnectedLocal && auth.currentUser != null
    }

    /**
     * Enlaza al conductor con una flota en Firestore mediante el código de 6 dígitos.
     * Descarga el historial financiero del conductor si es un nuevo dispositivo y sube el local.
     */
    suspend fun linkFleet(inviteCode: String, driverName: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val uid = currentUser.uid

        try {
            // 1. Buscamos la flota por el inviteCode
            val fleetSnapshot = firestore.collection("fleets")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            if (fleetSnapshot.isEmpty) return@withContext false

            val fleetDoc = fleetSnapshot.documents.first()
            val fleetId = fleetDoc.id
            val fleetName = fleetDoc.getString("name") ?: ""

            // Verificar si el código ha expirado
            val expiresAt = fleetDoc.getTimestamp("inviteCodeExpiresAt")
            if (expiresAt != null && expiresAt.toDate().before(Date())) {
                // Código expirado
                return@withContext false
            }

            // 2. Registramos el conductor en la colección `/drivers`
            val driverData = mutableMapOf<String, Any>(
                "id" to uid,
                "fleetId" to fleetId,
                "name" to driverName,
                "inviteCode" to inviteCode,
                "connectedAt" to Timestamp.now()
            )
            currentUser.email?.let { driverData["email"] = it }
            firestore.collection("drivers").document(uid).set(driverData).await()

            // 3. Guardamos localmente el estado de conexión
            val app = context.applicationContext as GestionTaxiApplication
            app.saveFleetConnection(true, fleetName, inviteCode)
            app.saveFleetId(fleetId)

            // 4. Descargamos histórico primero si estamos en un dispositivo nuevo
            downloadHistoricalData()

            // 5. Sincronizamos las carreras y gastos locales pendientes de subir
            syncRidesAndExpenses()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Sube todas las carreras y gastos locales no sincronizados a Firestore.
     */
    suspend fun syncRidesAndExpenses(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val uid = currentUser.uid
        val app = context.applicationContext as GestionTaxiApplication
        val fleetId = app.getFleetId().first() ?: return@withContext false

        var allOk = true

        try {
            // Sincronizar carreras
            val unsyncedRides = database.taxiRideDao().getUnsyncedRides()
            for (ride in unsyncedRides) {
                try {
                    val docRef = if (ride.firestoreId.isNullOrEmpty()) {
                        firestore.collection("rides").document()
                    } else {
                        firestore.collection("rides").document(ride.firestoreId)
                    }

                    val data = mapOf(
                        "driverId" to uid,
                        "fleetId" to fleetId,
                        "origin" to ride.origin,
                        "destination" to ride.destination,
                        "price" to ride.price,
                        "tip" to (ride.tip ?: 0.0),
                        "paymentMethod" to ride.paymentMethod,
                        "date" to Timestamp(ride.date),
                        "rideTime" to ride.rideTime,
                        "serviceType" to (ride.serviceType ?: ""),
                        "servicePlatform" to (ride.servicePlatform ?: "Directo")
                    )

                    docRef.set(data).await()
                    database.taxiRideDao().updateSyncStatus(ride.id, true, docRef.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    allOk = false
                }
            }

            // Sincronizar gastos
            val unsyncedExpenses = database.expenseDao().getUnsyncedExpenses()
            for (expense in unsyncedExpenses) {
                try {
                    val docRef = if (expense.firestoreId.isNullOrEmpty()) {
                        firestore.collection("expenses").document()
                    } else {
                        firestore.collection("expenses").document(expense.firestoreId)
                    }

                    val data = mapOf(
                        "driverId" to uid,
                        "fleetId" to fleetId,
                        "type" to expense.type.name,
                        "description" to (expense.description ?: ""),
                        "amount" to expense.amount,
                        "date" to Timestamp(expense.date)
                    )

                    docRef.set(data).await()
                    database.expenseDao().updateSyncStatus(expense.id, true, docRef.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    allOk = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            allOk = false
        }

        allOk
    }

    /**
     * Descarga el historial completo de carreras y gastos del conductor desde Firestore
     * y lo inserta en la base de datos local (Room) marcándolo como sincronizado.
     */
    suspend fun downloadHistoricalData(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val uid = currentUser.uid

        try {
            // Descargar carreras
            val ridesSnapshot = firestore.collection("rides")
                .whereEqualTo("driverId", uid)
                .get()
                .await()

            for (doc in ridesSnapshot.documents) {
                val firestoreId = doc.id
                val exists = database.taxiRideDao().getRideByFirestoreId(firestoreId) != null
                if (!exists) {
                    val origin = doc.getString("origin") ?: ""
                    val destination = doc.getString("destination") ?: ""
                    val price = doc.getDouble("price") ?: 0.0
                    val tip = doc.getDouble("tip") ?: 0.0
                    val paymentMethod = doc.getString("paymentMethod") ?: "Efectivo"
                    val timestamp = doc.getTimestamp("date")
                    val date = timestamp?.toDate() ?: Date()
                    val rideTime = doc.getString("rideTime") ?: "00:00"
                    val serviceType = doc.getString("serviceType")
                    val servicePlatform = doc.getString("servicePlatform") ?: "Directo"

                    val ride = TaxiRide(
                        origin = origin,
                        destination = destination,
                        price = price,
                        tip = tip,
                        paymentMethod = paymentMethod,
                        date = date,
                        rideTime = rideTime,
                        serviceType = serviceType,
                        servicePlatform = servicePlatform,
                        isSynced = true,
                        firestoreId = firestoreId
                    )
                    database.taxiRideDao().insert(ride)
                }
            }

            // Descargar gastos
            val expensesSnapshot = firestore.collection("expenses")
                .whereEqualTo("driverId", uid)
                .get()
                .await()

            for (doc in expensesSnapshot.documents) {
                val firestoreId = doc.id
                val exists = database.expenseDao().getExpenseByFirestoreId(firestoreId) != null
                if (!exists) {
                    val typeStr = doc.getString("type") ?: "OTHER"
                    val type = try {
                        ExpenseType.valueOf(typeStr)
                    } catch (e: Exception) {
                        ExpenseType.OTHER
                    }
                    val description = doc.getString("description") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val timestamp = doc.getTimestamp("date")
                    val date = timestamp?.toDate() ?: Date()

                    val expense = Expense(
                        type = type,
                        description = description,
                        amount = amount,
                        date = date,
                        isSynced = true,
                        firestoreId = firestoreId
                    )
                    database.expenseDao().insert(expense)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Intenta conectar automáticamente al conductor si ya existe un registro de conductor
     * en Firestore asociado a su UID.
     * Retorna true si se conectó con éxito, false en caso contrario.
     */
    suspend fun tryAutoConnectFleet(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val uid = currentUser.uid

        try {
            val driverDoc = firestore.collection("drivers").document(uid).get().await()
            if (!driverDoc.exists()) return@withContext false

            val fleetId = driverDoc.getString("fleetId") ?: return@withContext false
            val driverName = driverDoc.getString("name") ?: currentUser.displayName ?: ""
            val inviteCode = driverDoc.getString("inviteCode") ?: ""

            // Obtener el nombre de la flota
            val fleetDoc = firestore.collection("fleets").document(fleetId).get().await()
            val fleetName = if (fleetDoc.exists()) fleetDoc.getString("name") ?: "" else ""

            // Guardamos localmente el estado de conexión
            val app = context.applicationContext as GestionTaxiApplication
            app.saveFleetConnection(true, fleetName, inviteCode)
            app.saveFleetId(fleetId)

            // Descargamos histórico primero si estamos en un dispositivo nuevo
            downloadHistoricalData()

            // Sincronizamos las carreras y gastos locales pendientes de subir
            syncRidesAndExpenses()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cierra la sesión de Firebase y desvincula al conductor de la flota localmente,
     * reiniciando el estado de sincronización de la DB local Room.
     */
    suspend fun disconnectFromFleet(): Boolean = withContext(Dispatchers.IO) {
        try {
            auth.signOut()

            val app = context.applicationContext as GestionTaxiApplication
            app.saveFleetConnection(false, null, null)
            app.saveFleetId(null)

            database.taxiRideDao().resetSyncStatus()
            database.expenseDao().resetSyncStatus()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
