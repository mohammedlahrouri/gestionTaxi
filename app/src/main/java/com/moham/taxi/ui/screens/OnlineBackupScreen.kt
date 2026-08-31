package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.OnlineBackupViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineBackupScreen(navController: NavHostController) {
    BackHandler {
        navController.navigate(AppScreens.Settings.route) {
            popUpTo(AppScreens.Settings.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as GestionTaxiApplication
    val viewModel: OnlineBackupViewModel = viewModel(factory = OnlineBackupViewModel.Factory(app))
    val signedIn by viewModel.signedIn.collectAsState()
    val lastBackupDate by viewModel.lastBackupDate.collectAsState()
    val restorePoints by viewModel.restorePoints.collectAsState()
    val inProgress by viewModel.inProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val enabled by app.isOnlineBackupEnabled().collectAsState(initial = false)
    val snackbarHostState = remember { SnackbarHostState() }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestorePointId by remember { mutableStateOf<String?>(null) }
    var showPermissionExplanationDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            try {
                GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                viewModel.refreshStatus()
                viewModel.enableAutoBackup(true)
                return@rememberLauncherForActivityResult
            } catch (e: ApiException) {
                val reason = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> context.getString(R.string.backup_status_cancelled)
                    GoogleSignInStatusCodes.NETWORK_ERROR -> context.getString(R.string.backup_status_network_error)
                    GoogleSignInStatusCodes.DEVELOPER_ERROR -> context.getString(R.string.backup_status_developer_error)
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> context.getString(R.string.backup_status_sign_in_failed)
                    GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> context.getString(R.string.backup_status_sign_in_required)
                    GoogleSignInStatusCodes.INTERNAL_ERROR -> context.getString(R.string.backup_status_internal_error)
                    else -> context.getString(R.string.backup_status_code_error, e.statusCode)
                }
                if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    viewModel.setErrorMessage("${context.getString(R.string.error_google_connection)} ($reason - Código: ${e.statusCode})")
                    return@rememberLauncherForActivityResult
                }
            } catch (e: Exception) {
                viewModel.setErrorMessage("${context.getString(R.string.error_google_connection)} (${e.localizedMessage ?: "Error"})")
                return@rememberLauncherForActivityResult
            }
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        val hasScope = account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
        if (hasScope) {
            viewModel.refreshStatus()
            viewModel.enableAutoBackup(true)
        } else {
            viewModel.setErrorMessage("${context.getString(R.string.error_google_connection)} (${context.getString(R.string.backup_status_cancelled)})")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_online_backup)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (signedIn) stringResource(R.string.status_connected) else stringResource(R.string.status_disconnected),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.label_last_backup_available),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    val dateText = lastBackupDate?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: stringResource(R.string.label_backup_not_available)
                    Text(text = dateText, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (!signedIn) {
                Button(
                    onClick = { showPermissionExplanationDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = activity != null
                ) {
                    Text(stringResource(R.string.action_connect_google))
                }
            } else {
                Button(
                    onClick = { viewModel.signOut(activity) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !inProgress
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_auto_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.desc_auto_backup),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.enableAutoBackup(it) },
                        enabled = signedIn
                    )
                }
            }
            
            if (enabled || signedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.backup_photos_cloud_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.forceSync() },
                modifier = Modifier.fillMaxWidth(),
                enabled = signedIn && !inProgress
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.weight(1f))
                Text(stringResource(R.string.action_create_point_now))
            }

            Button(
                onClick = {
                    pendingRestorePointId = null
                    showRestoreConfirm = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = signedIn && !inProgress
            ) {
                Text(stringResource(R.string.action_restore_latest))
            }

            if (restorePoints.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.label_restore_points), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        restorePoints.forEachIndexed { index, point ->
                            val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(point.createdTime)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (index == 0) stringResource(R.string.label_latest_point, dateText) else dateText, modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        pendingRestorePointId = point.id
                                        showRestoreConfirm = true
                                    }
                                ) {
                                    Text(stringResource(R.string.action_restore))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(R.string.dialog_restore_confirm_title)) },
            text = { Text(stringResource(R.string.dialog_restore_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        val id = pendingRestorePointId
                        if (id == null) {
                            viewModel.restoreLatest()
                        } else {
                            viewModel.restorePoint(id)
                        }
                    }
                ) { Text(stringResource(R.string.action_yes_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = false },
            title = { Text(stringResource(R.string.dialog_permissions_title)) },
            text = {
                Text(
                    stringResource(R.string.dialog_permissions_msg)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanationDialog = false
                        activity?.let {
                            val playServices = GoogleApiAvailability.getInstance()
                            val status = playServices.isGooglePlayServicesAvailable(context)
                            if (status != ConnectionResult.SUCCESS) {
                                viewModel.setErrorMessage("${context.getString(R.string.error_google_connection)} (Google Play Services)")
                                return@let
                            }
                            launcher.launch(app.googleDriveAuthManager.getSignInClient().signInIntent)
                        }
                    }
                ) { Text(stringResource(R.string.action_understood_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
