package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.navigation.AppScreens
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import android.app.Activity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import com.moham.taxi.data.online.FleetConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetConnectionScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val isConnected by application.isFleetConnected().collectAsState(initial = false)
    val isFleetUnlinkedNoticePending by application.isFleetUnlinkedNoticePending().collectAsState(initial = false)
    val fleetName by application.getFleetName().collectAsState(initial = null)
    val fleetCode by application.getFleetCode().collectAsState(initial = null)
    
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showLeaveFleetDialog by remember { mutableStateOf(false) }
    var pendingFleetChange by remember { mutableStateOf<FleetConnectionStatus.ChangedFleet?>(null) }
    var showWorkSection by remember { mutableStateOf(true) }
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var showErrorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    var firebaseUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    
    val openFleetManagerWeb = {
        val uri = Uri.parse("https://taximanagementconsole.com")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
        }
    }
    
    val openWhatsappHelp = {
        val uri = Uri.parse("https://chat.whatsapp.com/F7XN8IGAw9s99m4W8ax9Ri")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }
    
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            if (nameInput.isEmpty()) {
                nameInput = firebaseUser?.displayName ?: ""
            }
            val alreadyConnected = application.isFleetConnected().first()
            if (!alreadyConnected) {
                isLoading = true
            }
            try {
                val status = application.firebaseSyncRepository.checkAndConnectFleet(nameInput.ifBlank { null })
                when (status) {
                    is FleetConnectionStatus.Connected -> {
                        showErrorMsg = null
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute == AppScreens.OnboardingStep1.route) {
                            application.setHasCompletedOnboarding(true)
                            application.setFirstRunCompleted()
                            navController.navigate(AppScreens.Home.route) {
                                popUpTo(AppScreens.Startup.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    is FleetConnectionStatus.ChangedFleet -> {
                        showErrorMsg = null
                        pendingFleetChange = status
                    }
                    is FleetConnectionStatus.NotFound -> {
                        if (alreadyConnected) {
                            application.saveFleetUnlinkedNotice(true)
                            application.firebaseSyncRepository.disconnectFromFleet()
                        }
                    }
                    is FleetConnectionStatus.Error -> {
                        if (!alreadyConnected) {
                            showErrorMsg = "Error al intentar conectar automáticamente: ${status.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                if (!alreadyConnected) {
                    showErrorMsg = "Error al intentar conectar automáticamente: ${e.localizedMessage}"
                }
            } finally {
                if (!alreadyConnected) {
                    isLoading = false
                }
            }
        }
    }
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                isLoading = true
                scope.launch {
                    try {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance().signInWithCredential(credential).await()
                        showErrorMsg = null
                    } catch (e: Exception) {
                        showErrorMsg = "Error al iniciar sesión en Firebase: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            } catch (e: ApiException) {
                showErrorMsg = "Error en Google Sign-In: ${e.message} (Código: ${e.statusCode})"
                isLoading = false
            }
        } else {
            var extraError = ""
            if (data != null) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    task.getResult(ApiException::class.java)
                } catch (e: ApiException) {
                    val errorStr = when (e.statusCode) {
                        GoogleSignInStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR (10)"
                        GoogleSignInStatusCodes.SIGN_IN_FAILED -> "SIGN_IN_FAILED (12500)"
                        GoogleSignInStatusCodes.NETWORK_ERROR -> "NETWORK_ERROR (7)"
                        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "CANCELLED (12501)"
                        else -> "Código: ${e.statusCode}"
                    }
                    extraError = " - $errorStr"
                }
            }
            showErrorMsg = "Inicio de sesión con Google cancelado$extraError"
            isLoading = false
        }
    }
    
    BackHandler {
        if (!navController.popBackStack()) {
            navController.navigate(AppScreens.Home.route) {
                popUpTo(AppScreens.Home.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_fleet_connect_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(AppScreens.Home.route) {
                                popUpTo(AppScreens.Home.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- OPCIÓN 1: TRABAJO CON UNA FLOTA ---
            OutlinedCard(
                onClick = { showWorkSection = !showWorkSection },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fleet_option_work_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.fleet_option_work_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isConnected) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.fleet_connected_status_connected),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (showWorkSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // CONTENIDO DESPLEGABLE DE TRABAJO CON FLOTA
            AnimatedVisibility(visible = showWorkSection) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fleet Status Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            
                            Text(
                                text = if (isConnected) {
                                    stringResource(R.string.fleet_connected_status_connected)
                                } else {
                                    stringResource(R.string.fleet_connected_status_disconnected)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (isConnected) {
                                if (!fleetName.isNullOrEmpty()) {
                                    Text(
                                        text = stringResource(R.string.fleet_connected_name_label, fleetName!!),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!fleetCode.isNullOrEmpty()) {
                                    Text(
                                        text = stringResource(R.string.fleet_connected_code_label, fleetCode!!),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Text(
                                text = stringResource(R.string.fleet_connection_explanation),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    
                    if (!isConnected) {
                        if (firebaseUser == null) {
                            // STEP 1: Google Sign-In
                            Button(
                                onClick = {
                                    showErrorMsg = null
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    scope.launch {
                                        try {
                                            googleSignInClient.signOut().await()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Iniciar sesión con Google")
                            }
                            
                            if (showErrorMsg != null) {
                                Text(
                                    text = showErrorMsg!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            // STEP 2: Fleet pairing form
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Sesión iniciada como: ${firebaseUser?.email ?: firebaseUser?.displayName ?: "Usuario"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it; showErrorMsg = null },
                                        label = { Text("Nombre del conductor") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = showErrorMsg != null
                                    )

                                    if (showErrorMsg != null) {
                                        Text(
                                            text = showErrorMsg!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (nameInput.isBlank()) {
                                                showErrorMsg = "El nombre del conductor no puede estar vacío."
                                            } else {
                                                scope.launch {
                                                    isLoading = true
                                                    try {
                                                        val status = application.firebaseSyncRepository.checkAndConnectFleet(nameInput.trim())
                                                        when (status) {
                                                            is FleetConnectionStatus.Connected -> {
                                                                showErrorMsg = null
                                                                val previousRoute = navController.previousBackStackEntry?.destination?.route
                                                                if (previousRoute == AppScreens.OnboardingStep1.route) {
                                                                    application.setHasCompletedOnboarding(true)
                                                                    application.setFirstRunCompleted()
                                                                    navController.navigate(AppScreens.Home.route) {
                                                                        popUpTo(AppScreens.Startup.route) { inclusive = true }
                                                                        launchSingleTop = true
                                                                    }
                                                                }
                                                            }
                                                            is FleetConnectionStatus.ChangedFleet -> {
                                                                showErrorMsg = null
                                                                pendingFleetChange = status
                                                            }
                                                            is FleetConnectionStatus.NotFound -> {
                                                                showErrorMsg = "No se encontró ninguna invitación por correo para esta cuenta (${firebaseUser?.email}). Pídele al gestor que añada tu correo desde Ajustes en la Web App."
                                                            }
                                                            is FleetConnectionStatus.Error -> {
                                                                showErrorMsg = "Error al conectar: ${status.message}"
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        showErrorMsg = "Error al conectar: ${e.localizedMessage}"
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "Comprobar y Aceptar Invitación de Flota")
                                    }
                                    
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    FirebaseAuth.getInstance().signOut()
                                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                                    GoogleSignIn.getClient(context, gso).signOut().await()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(text = "Cerrar sesión de Google")
                                    }
                                }
                            }
                        }
                    } else {
                        // Button: Abandonar flota actual (Desvinculación en la nube y local)
                        Button(
                            onClick = { showLeaveFleetDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.fleet_btn_leave))
                        }

                        // Button: Desconectar sesión (solo local)
                        OutlinedButton(
                            onClick = { showDisconnectDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.fleet_btn_disconnect))
                        }
                    }
                }
            }

            // --- OPCIÓN 2: GESTIONO UNA FLOTA ---
            OutlinedCard(
                onClick = { openFleetManagerWeb() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fleet_option_manage_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.fleet_option_manage_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // --- OPCIÓN 3: WHATSAPP AYUDA ---
            OutlinedCard(
                onClick = { openWhatsappHelp() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fleet_option_whatsapp_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.fleet_option_whatsapp_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    
    // Disconnect Confirm Dialog
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text(text = stringResource(R.string.fleet_disconnect_confirm_title)) },
            text = { Text(text = stringResource(R.string.fleet_disconnect_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            application.firebaseSyncRepository.disconnectFromFleet()
                            try {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                                GoogleSignIn.getClient(context, gso).signOut().await()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            isLoading = false
                        }
                        showDisconnectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.fleet_btn_disconnect))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    // Leave Fleet Confirm Dialog
    if (showLeaveFleetDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveFleetDialog = false },
            title = { Text(text = stringResource(R.string.fleet_leave_confirm_title)) },
            text = { Text(text = stringResource(R.string.fleet_leave_confirm_desc, fleetName ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveFleetDialog = false
                        scope.launch {
                            isLoading = true
                            try {
                                application.firebaseSyncRepository.unlinkFromCurrentFleet()
                            } catch (e: Exception) {
                                showErrorMsg = "Error al desvincularse: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(text = stringResource(R.string.fleet_btn_confirm_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveFleetDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isFleetUnlinkedNoticePending) {
        AlertDialog(
            onDismissRequest = {
                scope.launch { application.saveFleetUnlinkedNotice(false) }
            },
            title = {
                Text(
                    text = stringResource(R.string.fleet_unlinked_notice_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.fleet_unlinked_notice_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { application.saveFleetUnlinkedNotice(false) }
                    }
                ) {
                    Text(text = stringResource(R.string.fleet_unlinked_notice_button))
                }
            }
        )
    }

    // Diálogo de Cambio de Flota (Opción 2)
    pendingFleetChange?.let { change ->
        AlertDialog(
            onDismissRequest = { pendingFleetChange = null },
            title = {
                Text(
                    text = "Cambio de Flota Detectado",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Has estado conectado previamente con otra flota. Te estás conectando a \"${change.newFleetName}\".",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Elige cómo gestionar tus carreras y gastos:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Card Opción A: Transferir Historial
                    OutlinedCard(
                        onClick = {
                            val c = change
                            pendingFleetChange = null
                            scope.launch {
                                isLoading = true
                                try {
                                    val ok = application.firebaseSyncRepository.completeFleetConnection(
                                        fleetId = c.newFleetId,
                                        fleetName = c.newFleetName,
                                        email = c.email,
                                        isFleetChange = true,
                                        transferHistory = true,
                                        invitationId = c.invitationId
                                    )
                                    if (ok) {
                                        showErrorMsg = null
                                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                                        if (previousRoute == AppScreens.OnboardingStep1.route) {
                                            application.setHasCompletedOnboarding(true)
                                            application.setFirstRunCompleted()
                                            navController.navigate(AppScreens.Home.route) {
                                                popUpTo(AppScreens.Startup.route) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        showErrorMsg = "Error al completar la conexión con la nueva flota."
                                    }
                                } catch (e: Exception) {
                                    showErrorMsg = "Error: ${e.localizedMessage}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sincronizar mi historial",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Transfiere tus carreras y gastos existentes al panel de la nueva flota. Tus datos locales se conservan íntegros.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card Opción B: Empezar desde hoy
                    OutlinedCard(
                        onClick = {
                            val c = change
                            pendingFleetChange = null
                            scope.launch {
                                isLoading = true
                                try {
                                    val ok = application.firebaseSyncRepository.completeFleetConnection(
                                        fleetId = c.newFleetId,
                                        fleetName = c.newFleetName,
                                        email = c.email,
                                        isFleetChange = true,
                                        transferHistory = false,
                                        invitationId = c.invitationId
                                    )
                                    if (ok) {
                                        showErrorMsg = null
                                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                                        if (previousRoute == AppScreens.OnboardingStep1.route) {
                                            application.setHasCompletedOnboarding(true)
                                            application.setFirstRunCompleted()
                                            navController.navigate(AppScreens.Home.route) {
                                                popUpTo(AppScreens.Startup.route) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        showErrorMsg = "Error al completar la conexión con la nueva flota."
                                    }
                                } catch (e: Exception) {
                                    showErrorMsg = "Error: ${e.localizedMessage}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Empezar desde hoy",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Conserva todo tu historial previo en tu teléfono y sincroniza con la nueva flota solo a partir de este momento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "🔒 Tus datos locales en la app NUNCA se sobrescriben ni se eliminan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingFleetChange = null }) {
                    Text(text = "Cancelar")
                }
            }
        )
    }
    
    if (isLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Procesando") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text(text = "Conectando...")
                }
            },
            confirmButton = {}
        )
    }
}
