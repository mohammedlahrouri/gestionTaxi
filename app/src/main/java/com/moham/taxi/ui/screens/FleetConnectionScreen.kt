package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetConnectionScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val isConnected by application.isFleetConnected().collectAsState(initial = false)
    val fleetName by application.getFleetName().collectAsState(initial = null)
    val fleetCode by application.getFleetCode().collectAsState(initial = null)
    
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var showErrorMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    var firebaseUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    
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
                try {
                    val autoConnected = application.firebaseSyncRepository.tryAutoConnectFleet()
                    if (autoConnected) {
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
                } catch (e: Exception) {
                    showErrorMsg = "Error al intentar conectar automáticamente: ${e.localizedMessage}"
                } finally {
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
        val previousRoute = navController.previousBackStackEntry?.destination?.route
        if (previousRoute == AppScreens.OnboardingStep1.route) {
            navController.popBackStack()
        } else {
            navController.navigate(AppScreens.Settings.route) {
                popUpTo(AppScreens.Settings.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_fleet_connect_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                            
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 6) {
                                        codeInput = input
                                        showErrorMsg = null
                                    }
                                },
                                label = { Text("Código de 6 números de la flota") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                    when {
                                        nameInput.isBlank() -> {
                                            showErrorMsg = "El nombre del conductor no puede estar vacío."
                                        }
                                        codeInput.length != 6 -> {
                                            showErrorMsg = "El código debe tener exactamente 6 números."
                                        }
                                        else -> {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val success = application.firebaseSyncRepository.linkFleet(codeInput, nameInput.trim())
                                                    if (success) {
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
                                                        showErrorMsg = "Error al enlazar con la flota. Verifica el código e inténtalo de nuevo."
                                                    }
                                                } catch (e: Exception) {
                                                    showErrorMsg = "Error al conectar: ${e.localizedMessage}"
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Vincular con flota")
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
                // Button: Desconectar
                Button(
                    onClick = { showDisconnectDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.fleet_btn_disconnect))
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
