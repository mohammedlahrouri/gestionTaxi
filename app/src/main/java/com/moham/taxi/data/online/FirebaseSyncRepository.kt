package com.moham.taxi.data.online

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.AppDatabase
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

sealed class FleetConnectionStatus {
    object Connected : FleetConnectionStatus()
    data class ChangedFleet(
        val oldFleetId: String,
        val newFleetId: String,
        val newFleetName: String,
        val email: String,
        val invitationId: String? = null
    ) : FleetConnectionStatus()
    object NotFound : FleetConnectionStatus()
    data class Error(val message: String) : FleetConnectionStatus()
}

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
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val syncMutex = Mutex()
    private var driverProfileListenerRegistration: ListenerRegistration? = null

    /**
     * Inicia la escucha en tiempo real del documento propio del conductor en Firestore (drivers/{uid}).
     * Si el gestor de la flota expulsa al conductor (borrando el documento, cambiando fleetId o pasando a desvinculado),
     * la app lo detecta al instante y desconecta al conductor en tiempo real sin requerir interacción manual.
     */
    @Synchronized
    fun startDriverProfileListener() {
        val currentUser = auth.currentUser ?: return
        val app = context.applicationContext as GestionTaxiApplication

        // Evitar duplicar listeners
        driverProfileListenerRegistration?.remove()

        driverProfileListenerRegistration = firestore.collection("drivers")
            .document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // Acceso denegado: el gestor ha borrado la ficha o revocado permisos
                        app.applicationScope.launch {
                            handleExpulsionOrUnlink()
                        }
                    }
                    return@addSnapshotListener
                }

                app.applicationScope.launch {
                    val wasConnected = app.isFleetConnected().first()
                    val localFleetId = app.getFleetId().first()

                    if (snapshot == null || !snapshot.exists()) {
                        // El documento fue eliminado por el gestor de la flota
                        if (wasConnected || !localFleetId.isNullOrEmpty()) {
                            handleExpulsionOrUnlink()
                        }
                    } else {
                        val remoteFleetId = snapshot.getString("fleetId")
                        val status = snapshot.getString("status")
                        if (remoteFleetId.isNullOrEmpty() || status == "desvinculado" || status == "expulsado") {
                            if (wasConnected || !localFleetId.isNullOrEmpty()) {
                                handleExpulsionOrUnlink()
                            }
                        } else if (!localFleetId.isNullOrEmpty() && remoteFleetId != localFleetId) {
                            // Cambio de flota realizado desde el backend
                            handleExpulsionOrUnlink()
                        }
                    }
                }
            }
    }

    @Synchronized
    fun stopDriverProfileListener() {
        driverProfileListenerRegistration?.remove()
        driverProfileListenerRegistration = null
    }

    suspend fun handleExpulsionOrUnlink() = withContext(Dispatchers.IO) {
        val app = context.applicationContext as GestionTaxiApplication
        val wasConnected = app.isFleetConnected().first()
        val localFleetId = app.getFleetId().first()
        if (wasConnected || !localFleetId.isNullOrEmpty()) {
            app.saveFleetUnlinkedNotice(true)
            disconnectFromFleet()
        }
    }

    /**
     * Sube la foto de ticket local (si existe) a Storage en
     * tickets/{fleetId}/{driverId}/{collectionName}/{docId}.{ext} y devuelve
     * la URL de descarga a guardar como 'ticketUrl' en el documento de
     * Firestore. Devuelve null si no hay ticket adjunto o si la subida falla
     * — un fallo al subir la foto nunca debe impedir que se sincronicen los
     * datos financieros del viaje/gasto.
     */
    private suspend fun uploadTicketPhoto(
        fleetId: String,
        driverId: String,
        collectionName: String,
        docId: String,
        relativePath: String?
    ): String? {
        val file = ImageUtils.getTicketPhotoFile(context, relativePath) ?: return null
        return try {
            kotlinx.coroutines.withTimeoutOrNull(15000L) {
                val extension = file.extension.ifBlank { "webp" }
                val metadata = StorageMetadata.Builder()
                    .setContentType(if (extension == "webp") "image/webp" else "image/jpeg")
                    .build()
                val ref = storage.reference.child("tickets/$fleetId/$driverId/$collectionName/$docId.$extension")
                ref.putFile(Uri.fromFile(file), metadata).await()
                ref.downloadUrl.await().toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private data class PendingTicketUpload(
        val docRef: DocumentReference,
        val photoPath: String,
        val collectionName: String
    )

    /**
     * Sube una lista de fotos de tickets pendientes de manera concurrente (hasta 3 conexiones simultáneas)
     * utilizando un Semaphore y coroutineScope sin bloquear la confirmación previa de los datos numéricos
     * de los viajes/gastos en Firestore. Actualiza el documento correspondiente con 'ticketUrl' al completarse.
     */
    private suspend fun uploadTicketPhotosConcurrently(
        fleetId: String,
        driverId: String,
        pendingList: List<PendingTicketUpload>
    ) = coroutineScope {
        if (pendingList.isEmpty()) return@coroutineScope
        val semaphore = Semaphore(3)
        pendingList.map { item ->
            async {
                semaphore.withPermit {
                    try {
                        val ticketUrl = uploadTicketPhoto(
                            fleetId = fleetId,
                            driverId = driverId,
                            collectionName = item.collectionName,
                            docId = item.docRef.id,
                            relativePath = item.photoPath
                        )
                        if (ticketUrl != null) {
                            item.docRef.set(
                                mapOf(
                                    "ticketUrl" to ticketUrl,
                                    "fleetId" to fleetId
                                ),
                                SetOptions.merge()
                            ).await()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }.awaitAll()
    }

    /**
     * Verifica si el usuario actual está enlazado a una flota local y remotamente.
     */
    suspend fun isFleetConnected(): Boolean {
        val app = context.applicationContext as GestionTaxiApplication
        val isConnectedLocal = app.isFleetConnected().first()
        return isConnectedLocal && auth.currentUser != null
    }

    /**
     * Enlaza al conductor con una flota en Firestore mediante la invitación enviada a su correo.
     */
    suspend fun linkFleetByEmail(driverName: String): Boolean = withContext(Dispatchers.IO) {
        val status = checkAndConnectFleet(driverName)
        status is FleetConnectionStatus.Connected
    }

    /**
     * Sube todas las carreras y gastos locales no sincronizados a Firestore,
     * y procesa las eliminaciones pendientes en la nube.
     */
    suspend fun syncRidesAndExpenses(): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val currentUser = auth.currentUser ?: return@withLock false
            val uid = currentUser.uid
            val app = context.applicationContext as GestionTaxiApplication
            val fleetId = app.getFleetId().first() ?: return@withLock false

            // Sincronizar SOLO si hay conexión de red real disponible
            if (!app.isNetworkAvailable()) {
                return@withLock false
            }

        // 1. Sincronizar siempre las plataformas de servicio y sus comisiones con Firestore
        syncServicePlatforms()

        var allOk = true

        try {
            // 0. Procesar eliminaciones pendientes en Firestore
            val pendingDeletions = database.pendingDeletionDao().getAllPendingDeletions()
            for (p in pendingDeletions) {
                try {
                    firestore.collection(p.collectionName).document(p.firestoreId).delete().await()
                    // Por firestoreId (no por fila): puede haber más de una
                    // entrada en cola para el mismo documento (p. ej. dos
                    // intentos de borrado antes de que el primero se
                    // completara). Limpiar solo `p` dejaba viva la fila
                    // duplicada, que a partir de ahí fallaba para siempre al
                    // reintentar borrar un documento que ya no existía.
                    database.pendingDeletionDao().deleteByFirestoreId(p.firestoreId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    allOk = false
                }
            }

            // Sincronizar carreras en lotes resilientes de 50 con fallback unitario
            val unsyncedRides = database.taxiRideDao().getUnsyncedRides()
            for (chunk in unsyncedRides.chunked(50)) {
                val batch = firestore.batch()
                val syncedRidesList = mutableListOf<Triple<Long, DocumentReference, Map<String, Any>>>()
                val pendingTickets = mutableListOf<PendingTicketUpload>()

                for (ride in chunk) {
                    try {
                        val fId = ride.firestoreId
                        if (!fId.isNullOrEmpty() && database.pendingDeletionDao().isPendingDeletion(fId)) {
                            continue
                        }
                        if (database.taxiRideDao().getTaxiRideById(ride.id) == null) {
                            continue
                        }

                        val docRef = if (ride.firestoreId.isNullOrEmpty()) {
                            firestore.collection("rides").document()
                        } else {
                            firestore.collection("rides").document(ride.firestoreId)
                        }

                        // Las fotos se encolan para subida concurrente y no bloqueante tras el commit numérico
                        if (!ride.ticketPhotoPath.isNullOrBlank()) {
                            pendingTickets.add(PendingTicketUpload(docRef, ride.ticketPhotoPath, "rides"))
                        }

                        val data = mutableMapOf<String, Any>(
                            "driverId" to uid,
                            "fleetId" to fleetId,
                            "origin" to ride.origin,
                            "destination" to ride.destination,
                            "price" to ride.price,
                            "paymentMethod" to ride.paymentMethod,
                            "date" to Timestamp(ride.date),
                            "rideTime" to ride.rideTime,
                            "serviceType" to (ride.serviceType ?: ""),
                            "servicePlatform" to (ride.servicePlatform ?: "Directo"),
                            "realDate" to Timestamp(ride.realDate),
                            "isSplitPayment" to ride.isSplitPayment
                        )
                        if (!ride.notes.isNullOrBlank()) {
                            data["notes"] = ride.notes
                        }
                        if (ride.isSplitPayment && !ride.splitSecondaryMethod.isNullOrBlank()) {
                            data["splitSecondaryMethod"] = ride.splitSecondaryMethod
                            data["splitSecondaryPrice"] = ride.splitSecondaryPrice ?: 0.0
                        }
                        ride.netPrice?.let { data["netPrice"] = it }
                        ride.commissionPercentAtTime?.let { data["commissionPercentAtTime"] = it }
                        ride.commissionVatAtTime?.let { data["commissionVatAtTime"] = it }
                        ride.workingDate?.let { data["workingDate"] = Timestamp(it) }

                        batch.set(docRef, data, SetOptions.merge())
                        syncedRidesList.add(Triple(ride.id, docRef, data))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        allOk = false
                    }
                }

                val confirmedTickets = mutableListOf<PendingTicketUpload>()

                if (syncedRidesList.isNotEmpty()) {
                    try {
                        batch.commit().await()
                        for ((rideId, docRef, _) in syncedRidesList) {
                            database.taxiRideDao().updateSyncStatus(rideId, true, docRef.id)
                        }
                        confirmedTickets.addAll(pendingTickets)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            checkDriverExpulsionOrUnlink()
                            allOk = false
                        } else {
                            // Fallback documento a documento si el batch falla por un problema puntual
                            for ((rideId, docRef, data) in syncedRidesList) {
                                try {
                                    docRef.set(data, SetOptions.merge()).await()
                                    database.taxiRideDao().updateSyncStatus(rideId, true, docRef.id)
                                    pendingTickets.find { it.docRef.id == docRef.id }?.let {
                                        confirmedTickets.add(it)
                                    }
                                } catch (singleEx: Exception) {
                                    singleEx.printStackTrace()
                                    allOk = false
                                }
                            }
                        }
                    }
                }

                // Subir fotos concurrentemente (máx. 3 conexiones simultáneas) de los documentos confirmados
                if (confirmedTickets.isNotEmpty()) {
                    uploadTicketPhotosConcurrently(fleetId, uid, confirmedTickets)
                }
            }

            // Sincronizar gastos en lotes resilientes de 50 con fallback unitario
            val unsyncedExpenses = database.expenseDao().getUnsyncedExpenses()
            for (chunk in unsyncedExpenses.chunked(50)) {
                val batch = firestore.batch()
                val syncedExpensesList = mutableListOf<Triple<Long, DocumentReference, Map<String, Any>>>()
                val pendingExpenseTickets = mutableListOf<PendingTicketUpload>()

                for (expense in chunk) {
                    try {
                        val fId = expense.firestoreId
                        if (!fId.isNullOrEmpty() && database.pendingDeletionDao().isPendingDeletion(fId)) {
                            continue
                        }
                        if (database.expenseDao().getExpenseById(expense.id) == null) {
                            continue
                        }

                        val docRef = if (expense.firestoreId.isNullOrEmpty()) {
                            firestore.collection("expenses").document()
                        } else {
                            firestore.collection("expenses").document(expense.firestoreId)
                        }

                        // Las fotos se encolan para subida concurrente y no bloqueante tras el commit numérico
                        if (!expense.ticketPhotoPath.isNullOrBlank()) {
                            pendingExpenseTickets.add(PendingTicketUpload(docRef, expense.ticketPhotoPath, "expenses"))
                        }

                        val data = mutableMapOf<String, Any>(
                            "driverId" to uid,
                            "fleetId" to fleetId,
                            "type" to expense.type.name,
                            "description" to (expense.description ?: ""),
                            "amount" to expense.amount,
                            "date" to Timestamp(expense.date),
                            "realDate" to Timestamp(expense.realDate),
                            "expenseTime" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(expense.realDate)
                        )
                        expense.maintenanceKilometers?.let { data["maintenanceKilometers"] = it }
                        expense.maintenanceDetails?.let { data["maintenanceDetails"] = it }

                        batch.set(docRef, data, SetOptions.merge())
                        syncedExpensesList.add(Triple(expense.id, docRef, data))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        allOk = false
                    }
                }

                val confirmedExpenseTickets = mutableListOf<PendingTicketUpload>()

                if (syncedExpensesList.isNotEmpty()) {
                    try {
                        batch.commit().await()
                        for ((expenseId, docRef, _) in syncedExpensesList) {
                            database.expenseDao().updateSyncStatus(expenseId, true, docRef.id)
                        }
                        confirmedExpenseTickets.addAll(pendingExpenseTickets)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            checkDriverExpulsionOrUnlink()
                            allOk = false
                        } else {
                            // Fallback documento a documento si el batch falla por un problema puntual
                            for ((expenseId, docRef, data) in syncedExpensesList) {
                                try {
                                    docRef.set(data, SetOptions.merge()).await()
                                    database.expenseDao().updateSyncStatus(expenseId, true, docRef.id)
                                    pendingExpenseTickets.find { it.docRef.id == docRef.id }?.let {
                                        confirmedExpenseTickets.add(it)
                                    }
                                } catch (singleEx: Exception) {
                                    singleEx.printStackTrace()
                                    allOk = false
                                }
                            }
                        }
                    }
                }

                // Subir fotos concurrentemente (máx. 3 conexiones simultáneas) de los gastos confirmados
                if (confirmedExpenseTickets.isNotEmpty()) {
                    uploadTicketPhotosConcurrently(fleetId, uid, confirmedExpenseTickets)
                }
            }

            // Actualizar presencia del conductor (Heartbeat) en Firestore
            try {
                firestore.collection("drivers").document(uid).set(
                    mapOf(
                        "lastActiveAt" to Timestamp.now(),
                        "status" to "activo"
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } catch (presenceEx: Exception) {
                presenceEx.printStackTrace()
            }

            // Reconciliar/Limpiar registros fantasmas en Firestore que fueron eliminados localmente
            reconcileWithServer()
        } catch (e: Exception) {
            e.printStackTrace()
            allOk = false
        }

            allOk
        }
    }

    /**
     * Limpia registros huérfanos/fantasmas en Firestore que no existen en la base de datos local (Room)
     * del conductor ni están en la cola de borrado.
     */
    suspend fun reconcileWithServer(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val app = context.applicationContext as GestionTaxiApplication
        if (!app.isNetworkAvailable()) return@withContext false

        try {
            // Sincronizar plataformas de servicio del conductor hacia su documento en Firestore
            syncServicePlatforms()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Sincroniza y fusiona las plataformas de servicio de forma bidireccional
     * e insensible a mayúsculas/minúsculas entre SQLite y Firestore.
     */
    suspend fun syncServicePlatforms(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        val uid = currentUser.uid
        val app = context.applicationContext as GestionTaxiApplication
        if (!app.isNetworkAvailable()) return@withContext false

        try {
            // 1. Descargar y fusionar plataformas remotas desde Firestore hacia SQLite (sin duplicar)
            try {
                val driverDoc = firestore.collection("drivers").document(uid).get().await()
                if (driverDoc.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val remoteList = driverDoc.get("servicePlatforms") as? List<Map<String, Any>>
                    if (!remoteList.isNullOrEmpty()) {
                        for (raw in remoteList) {
                            val rawName = (raw["name"] as? String)?.trim() ?: continue
                            if (rawName.isEmpty()) continue

                            // Comprobar existencia en SQLite de forma case-insensitive
                            val existing = database.servicePlatformDao().getServicePlatformByName(rawName)
                            if (existing == null) {
                                val hasComm = raw["hasCommission"] as? Boolean ?: false
                                val commPct = (raw["commissionPercentage"] as? Number)?.toDouble()
                                val commVat = (raw["commissionVat"] as? Number)?.toDouble() ?: 21.0
                                val useAlt = raw["useAlternativeMath"] as? Boolean ?: false
                                val newPlatform = com.moham.taxi.data.model.ServicePlatform(
                                    name = rawName,
                                    commissionPercentage = if (hasComm) commPct else null,
                                    commissionVat = commVat,
                                    useAlternativeMath = useAlt
                                )
                                database.servicePlatformDao().insert(newPlatform)
                            }
                        }
                    }
                }
            } catch (mergeEx: Exception) {
                mergeEx.printStackTrace()
            }

            // 2. Obtener todas las plataformas locales y deduplicar case-insensitively para Firestore
            val localPlatforms = database.servicePlatformDao().getAllServicePlatformsList()
            val uniqueMap = mutableMapOf<String, com.moham.taxi.data.model.ServicePlatform>()
            for (p in localPlatforms) {
                val key = p.name.trim().lowercase(java.util.Locale.ROOT)
                if (key.isNotEmpty()) {
                    val current = uniqueMap[key]
                    if (current == null) {
                        uniqueMap[key] = p
                    } else if (current.commissionPercentage == null && p.commissionPercentage != null) {
                        // Si una versión tiene comisiones configuradas y la otra no, conservar la que tiene datos
                        uniqueMap[key] = p
                    }
                }
            }

            val platformsList = uniqueMap.values.map { p ->
                mapOf(
                    "name" to p.name.trim(),
                    "hasCommission" to (p.commissionPercentage != null),
                    "commissionPercentage" to (p.commissionPercentage ?: 0.0),
                    "commissionVat" to (p.commissionVat ?: 21.0),
                    "useAlternativeMath" to p.useAlternativeMath
                )
            }
            firestore.collection("drivers").document(uid).set(
                mapOf("servicePlatforms" to platformsList),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Descarga el historial de carreras y gastos del conductor desde Firestore
     * ordenado de más reciente a más antiguo (date DESC) y lo fusiona en la base de datos local (Room)
     * emparejando por huella digital para no duplicar ningún dato existente.
     * Si limitCount != null, descarga únicamente los primeros N registros más recientes.
     */
    suspend fun downloadHistoricalData(limitCount: Int? = null): Boolean = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val currentUser = auth.currentUser ?: return@withLock false
            val uid = currentUser.uid

            try {
                // Deduplicar previamente por si acaso
                database.taxiRideDao().deduplicateRides()
                database.expenseDao().deduplicateExpenses()

                // Descargar carreras con consulta indexada optimizada o fallback seguro
                val rideDocsToProcess = try {
                    var q = firestore.collection("rides")
                        .whereEqualTo("driverId", uid)
                        .orderBy("date", Query.Direction.DESCENDING)
                    if (limitCount != null) {
                        q = q.limit(limitCount.toLong())
                    }
                    q.get().await().documents
                } catch (e: Exception) {
                    val ridesSnapshot = firestore.collection("rides")
                        .whereEqualTo("driverId", uid)
                        .get()
                        .await()
                    val sorted = ridesSnapshot.documents.sortedByDescending { doc ->
                        doc.getTimestamp("date")?.seconds ?: 0L
                    }
                    if (limitCount != null) sorted.take(limitCount) else sorted
                }

                for (doc in rideDocsToProcess) {
                    val firestoreId = doc.id
                    val exists = database.taxiRideDao().getRideByFirestoreId(firestoreId) != null
                    val isPendingDelete = database.pendingDeletionDao().isPendingDeletion(firestoreId)
                    if (!exists && !isPendingDelete) {
                        val origin = doc.getString("origin") ?: ""
                        val destination = doc.getString("destination") ?: ""
                        val price = doc.getDouble("price") ?: 0.0
                        val tip = doc.getDouble("tip") ?: 0.0
                        val netPrice = doc.getDouble("netPrice")
                        val commissionPercentAtTime = doc.getDouble("commissionPercentAtTime")
                        val commissionVatAtTime = doc.getDouble("commissionVatAtTime")
                        val paymentMethod = doc.getString("paymentMethod") ?: "Efectivo"
                        val timestamp = doc.getTimestamp("date")
                        val date = timestamp?.toDate() ?: Date()
                        val workingDate = doc.getTimestamp("workingDate")?.toDate()
                        val rideTime = doc.getString("rideTime") ?: "00:00"
                        val serviceType = doc.getString("serviceType")
                        val servicePlatform = doc.getString("servicePlatform") ?: "Directo"
                        val notes = doc.getString("notes")
                        val isSplitPayment = doc.getBoolean("isSplitPayment") ?: false
                        val splitSecondaryMethod = doc.getString("splitSecondaryMethod")
                        val splitSecondaryPrice = doc.getDouble("splitSecondaryPrice")

                        // Comprobar si ya existe localmente para enlazarlo en vez de duplicarlo
                        val matchingRide = database.taxiRideDao().findMatchingRide(
                            minDate = Date(date.time - 2000),
                            maxDate = Date(date.time + 2000),
                            price = price,
                            paymentMethod = paymentMethod,
                            origin = origin,
                            destination = destination,
                            rideTime = rideTime
                        )

                        if (matchingRide != null) {
                            database.taxiRideDao().updateSyncStatus(matchingRide.id, true, firestoreId)
                        } else {
                            val ride = TaxiRide(
                                origin = origin,
                                destination = destination,
                                price = price,
                                tip = tip,
                                netPrice = netPrice,
                                commissionPercentAtTime = commissionPercentAtTime,
                                commissionVatAtTime = commissionVatAtTime,
                                paymentMethod = paymentMethod,
                                date = date,
                                workingDate = workingDate,
                                rideTime = rideTime,
                                serviceType = serviceType,
                                servicePlatform = servicePlatform,
                                notes = notes,
                                isSplitPayment = isSplitPayment,
                                splitSecondaryMethod = splitSecondaryMethod,
                                splitSecondaryPrice = splitSecondaryPrice,
                                isSynced = true,
                                firestoreId = firestoreId
                            )
                            database.taxiRideDao().insert(ride)
                        }
                    }
                }

                // Descargar gastos con consulta indexada optimizada o fallback seguro
                val expenseDocsToProcess = try {
                    var q = firestore.collection("expenses")
                        .whereEqualTo("driverId", uid)
                        .orderBy("date", Query.Direction.DESCENDING)
                    if (limitCount != null) {
                        q = q.limit(limitCount.toLong())
                    }
                    q.get().await().documents
                } catch (e: Exception) {
                    val expensesSnapshot = firestore.collection("expenses")
                        .whereEqualTo("driverId", uid)
                        .get()
                        .await()
                    val sorted = expensesSnapshot.documents.sortedByDescending { doc ->
                        doc.getTimestamp("date")?.seconds ?: 0L
                    }
                    if (limitCount != null) sorted.take(limitCount) else sorted
                }

                for (doc in expenseDocsToProcess) {
                    val firestoreId = doc.id
                    val exists = database.expenseDao().getExpenseByFirestoreId(firestoreId) != null
                    val isPendingDelete = database.pendingDeletionDao().isPendingDeletion(firestoreId)
                    if (!exists && !isPendingDelete) {
                        val typeStr = doc.getString("type") ?: "OTHER"
                        val type = try {
                            ExpenseType.valueOf(typeStr)
                        } catch (e: Exception) {
                            ExpenseType.OTHER
                        }
                        val description = doc.getString("description") ?: ""
                        val amount = doc.getDouble("amount") ?: 0.0
                        val maintenanceKm = doc.getLong("maintenanceKilometers")?.toInt()
                        val maintenanceDetails = doc.getString("maintenanceDetails")
                        val timestamp = doc.getTimestamp("date")
                        val date = timestamp?.toDate() ?: Date()

                        // Comprobar si ya existe localmente
                        val matchingExpense = database.expenseDao().findMatchingExpense(
                            minDate = Date(date.time - 2000),
                            maxDate = Date(date.time + 2000),
                            amount = amount,
                            type = type
                        )

                        if (matchingExpense != null) {
                            database.expenseDao().updateSyncStatus(matchingExpense.id, true, firestoreId)
                        } else {
                            val expense = Expense(
                                type = type,
                                description = description,
                                maintenanceKilometers = maintenanceKm,
                                maintenanceDetails = maintenanceDetails,
                                amount = amount,
                                date = date,
                                isSynced = true,
                                firestoreId = firestoreId
                            )
                            database.expenseDao().insert(expense)
                        }
                    }
                }

                // Deduplicar después de la descarga para garantizar consistencia total
                database.taxiRideDao().deduplicateRides()
                database.expenseDao().deduplicateExpenses()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Comprueba el estado de conexión a la flota:
     * - Comprueba SIEMPRE si hay invitaciones pendientes para el conductor (incluso si ya pertenece a una flota).
     * - Si detecta una invitación de una flota diferente a la actual, devuelve ChangedFleet para consultar al usuario.
     * - Si no hay invitaciones, reconecta a la flota actual si existe, o devuelve NotFound.
     */
    suspend fun checkAndConnectFleet(driverName: String? = null): FleetConnectionStatus = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext FleetConnectionStatus.NotFound
        val uid = currentUser.uid
        val email = currentUser.email?.lowercase()?.trim() ?: ""
        val app = context.applicationContext as GestionTaxiApplication

        try {
            val driverDoc = firestore.collection("drivers").document(uid).get().await()
            val currentFleetId = if (driverDoc.exists()) driverDoc.getString("fleetId") else null
            val driverEmail = if (driverDoc.exists()) driverDoc.getString("email") ?: email else email

            // 1. Comprobar SIEMPRE si existe una invitación pendiente en pending_driver_invitations
            val invSnapshot = if (email.isNotEmpty()) {
                firestore.collection("pending_driver_invitations")
                    .whereEqualTo("driverEmail", email)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()
            } else null

            if (invSnapshot != null && !invSnapshot.isEmpty) {
                val invDoc = invSnapshot.documents.first()
                val fleetIdFromInv = invDoc.getString("fleetId") ?: return@withContext FleetConnectionStatus.NotFound

                // Si el conductor ya estaba vinculado a otra flota -> Detección de cambio/transferencia de flota
                if (!currentFleetId.isNullOrEmpty() && currentFleetId != fleetIdFromInv) {
                    val invFleetName = invDoc.getString("fleetName")
                    val newFleetName = try {
                        val newFleetDoc = firestore.collection("fleets").document(fleetIdFromInv).get().await()
                        if (newFleetDoc.exists()) newFleetDoc.getString("name") ?: invFleetName ?: "Taxi Flota" else invFleetName ?: "Taxi Flota"
                    } catch (_: Exception) {
                        invFleetName ?: "Taxi Flota"
                    }
                    return@withContext FleetConnectionStatus.ChangedFleet(
                        oldFleetId = currentFleetId,
                        newFleetId = fleetIdFromInv,
                        newFleetName = newFleetName,
                        email = driverEmail,
                        invitationId = invDoc.id
                    )
                }

                // Si no tenía flota asignada o la invitación es de su misma flota: vincular directamente
                val resolvedName = if (!driverName.isNullOrBlank()) {
                    driverName.trim()
                } else {
                    currentUser.displayName ?: invDoc.getString("driverName") ?: email
                }

                val driverData = mutableMapOf<String, Any>(
                    "id" to uid,
                    "fleetId" to fleetIdFromInv,
                    "name" to resolvedName,
                    "email" to email,
                    "status" to "activo",
                    "connectedAt" to Timestamp.now()
                )
                firestore.collection("drivers").document(uid).set(driverData, SetOptions.merge()).await()
                invDoc.reference.update("status", "ACCEPTED").await()

                val invFleetName = invDoc.getString("fleetName")
                val newFleetName = try {
                    val newFleetDoc = firestore.collection("fleets").document(fleetIdFromInv).get().await()
                    if (newFleetDoc.exists()) newFleetDoc.getString("name") ?: invFleetName ?: "Taxi Flota" else invFleetName ?: "Taxi Flota"
                } catch (_: Exception) {
                    invFleetName ?: "Taxi Flota"
                }

                val lastSyncedFleetId = app.getLastSyncedFleetId().first()
                if (lastSyncedFleetId != null && lastSyncedFleetId != fleetIdFromInv) {
                    return@withContext FleetConnectionStatus.ChangedFleet(
                        oldFleetId = lastSyncedFleetId,
                        newFleetId = fleetIdFromInv,
                        newFleetName = newFleetName,
                        email = driverEmail,
                        invitationId = invDoc.id
                    )
                }

                completeFleetConnection(
                    fleetId = fleetIdFromInv,
                    fleetName = newFleetName,
                    email = driverEmail,
                    isFleetChange = false,
                    transferHistory = false,
                    invitationId = invDoc.id
                )
                return@withContext FleetConnectionStatus.Connected
            }

            // 2. Si NO hay invitaciones pendientes, comprobar la vinculación existente en drivers/{uid}
            if (currentFleetId.isNullOrEmpty()) {
                val wasConnected = app.isFleetConnected().first()
                if (wasConnected) {
                    app.saveFleetUnlinkedNotice(true)
                    disconnectFromFleet()
                }
                return@withContext FleetConnectionStatus.NotFound
            }

            val fleetName = try {
                val fleetDoc = firestore.collection("fleets").document(currentFleetId).get().await()
                if (fleetDoc.exists()) fleetDoc.getString("name") ?: "Taxi Flota" else "Taxi Flota"
            } catch (_: Exception) {
                "Taxi Flota"
            }

            val lastSyncedFleetId = app.getLastSyncedFleetId().first()
            if (lastSyncedFleetId != null && lastSyncedFleetId != currentFleetId) {
                return@withContext FleetConnectionStatus.ChangedFleet(
                    oldFleetId = lastSyncedFleetId,
                    newFleetId = currentFleetId,
                    newFleetName = fleetName,
                    email = driverEmail
                )
            }

            completeFleetConnection(
                fleetId = currentFleetId,
                fleetName = fleetName,
                email = driverEmail,
                isFleetChange = false,
                transferHistory = false
            )
            FleetConnectionStatus.Connected
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val wasConnected = app.isFleetConnected().first()
                if (wasConnected) {
                    app.saveFleetUnlinkedNotice(true)
                    disconnectFromFleet()
                }
                return@withContext FleetConnectionStatus.NotFound
            }
            e.printStackTrace()
            FleetConnectionStatus.Error(e.localizedMessage ?: "Error de conexión")
        }
    }

    /**
     * Completa la conexión con la flota:
     * - Guarda la conexión en la app.
     * - Actualiza Firestore asegurando que drivers/{uid} apunte a la nueva flota.
     * - Si viene de una invitación pendiente, la marca como ACCEPTED.
     * - Si el usuario cambió de flota y eligió transferir su historial, re-activa la sincronización.
     * - Si eligió empezar de cero, conserva el 100% de los datos locales sin subirlos a la nueva flota.
     * - Descarga inicial rápida de los 50 servicios más recientes (~1-2s).
     * - Lanza la descarga y sincronización restante en segundo plano.
     */
    suspend fun completeFleetConnection(
        fleetId: String,
        fleetName: String,
        email: String,
        isFleetChange: Boolean,
        transferHistory: Boolean,
        invitationId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext as GestionTaxiApplication
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val driverUpdates = mutableMapOf<String, Any>(
                    "id" to currentUser.uid,
                    "fleetId" to fleetId,
                    "email" to email,
                    "status" to "activo",
                    "connectedAt" to Timestamp.now()
                )
                currentUser.displayName?.let { driverUpdates["name"] = it }
                firestore.collection("drivers").document(currentUser.uid)
                    .set(driverUpdates, SetOptions.merge())
                    .await()
            }

            if (!invitationId.isNullOrBlank()) {
                try {
                    firestore.collection("pending_driver_invitations")
                        .document(invitationId)
                        .update("status", "ACCEPTED")
                        .await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            app.saveFleetConnection(true, fleetName, email)
            app.saveFleetId(fleetId)
            app.saveLastSyncedFleetId(fleetId)
            startDriverProfileListener()

            if (isFleetChange && transferHistory) {
                // Opción 2 [1]: Transferir historial a la nueva flota
                // (Los datos locales NO se borran)
                database.taxiRideDao().resetSyncStatus()
                database.expenseDao().resetSyncStatus()
            }

            // Sincronizar y fusionar plataformas de servicio inmediatamente
            syncServicePlatforms()

            // Descarga inicial rápida de los 50 servicios más recientes (~1-2s)
            downloadHistoricalData(limitCount = 50)

            // Continuar en segundo plano con el resto del histórico y subidas
            app.applicationScope.launch {
                try {
                    downloadHistoricalData()
                    syncRidesAndExpenses()
                } catch (e: Exception) {
                    e.printStackTrace()
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
        val status = checkAndConnectFleet()
        status is FleetConnectionStatus.Connected
    }

    /**
     * Desvincula voluntariamente al conductor de su flota actual tanto en Firestore
     * como en el almacenamiento local de la aplicación.
     * Pone fleetId a null en drivers/{uid} y status a 'desvinculado'.
     */
    suspend fun unlinkFromCurrentFleet(): Boolean = withContext(Dispatchers.IO) {
        try {
            stopDriverProfileListener()
            val currentUser = auth.currentUser ?: return@withContext false
            val uid = currentUser.uid

            val updates = mapOf<String, Any?>(
                "fleetId" to null,
                "status" to "desvinculado",
                "unlinkedAt" to Timestamp.now()
            )
            firestore.collection("drivers").document(uid).set(updates, SetOptions.merge()).await()

            val app = context.applicationContext as GestionTaxiApplication
            app.saveFleetConnection(false, null, null)
            app.saveFleetId(null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cierra la sesión de Firebase y desvincula al conductor de la flota localmente,
     * manteniendo intactos los identificadores de sincronización locales para evitar duplicados en caso de reconexión.
     */
    suspend fun disconnectFromFleet(): Boolean = withContext(Dispatchers.IO) {
        try {
            stopDriverProfileListener()
            auth.signOut()

            val app = context.applicationContext as GestionTaxiApplication
            app.saveFleetConnection(false, null, null)
            app.saveFleetId(null)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verifica si el conductor fue desvinculado o expulsado de la flota por el gestor
     * en el panel web. Si el documento drivers/{uid} ya no existe o su fleetId cambió,
     * limpia la vinculación local para evitar errores continuos de PERMISSION_DENIED.
     */
    suspend fun checkDriverExpulsionOrUnlink(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext false
            val app = context.applicationContext as GestionTaxiApplication
            val currentLocalFleetId = app.getFleetId().first() ?: return@withContext false
            val driverDoc = firestore.collection("drivers").document(currentUser.uid).get().await()

            val remoteFleetId = if (driverDoc.exists()) driverDoc.getString("fleetId") else null
            val status = if (driverDoc.exists()) driverDoc.getString("status") else null

            if (!driverDoc.exists() || remoteFleetId != currentLocalFleetId || status == "desvinculado" || status == "expulsado") {
                app.saveFleetUnlinkedNotice(true)
                disconnectFromFleet()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                val app = context.applicationContext as GestionTaxiApplication
                app.saveFleetUnlinkedNotice(true)
                disconnectFromFleet()
                true
            } else {
                e.printStackTrace()
                false
            }
        }
    }
}
