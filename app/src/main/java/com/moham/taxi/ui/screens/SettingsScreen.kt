package com.moham.taxi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Cloud
// Importación de DateRange eliminada
import androidx.compose.material.icons.filled.Download

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AttachMoney
// Importación de LocationCity eliminada
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.pm.PackageManager
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.ui.navigation.AppScreens
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.ArrowForward
 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication


    
    // Variables para controlar los diálogos de backup
    var showBackupDialog by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var showCreateMonthlyBackupDialog by remember { mutableStateOf(false) }
    var showCreateFullBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var backupInProgress by remember { mutableStateOf(false) }
    var showBackupConfirmDialog by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    
    // Variables para el tema
    val appTheme by application.getAppTheme().collectAsState(initial = "blue")
    var showThemeDialog by remember { mutableStateOf(false) }

    
    // Launcher para seleccionar archivo de backup a importar
    val selectBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            backupUri = uri
            showBackupConfirmDialog = true
        }
    }
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // La carga del tipo de ingreso en resumen ha sido eliminada
    
    // La opción de día de inicio de la semana ha sido eliminada y se usará el valor por defecto (lunes)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_general_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tarjeta para cambiar de Tema
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showThemeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.settings_themes_title),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_themes_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (appTheme == "blue") stringResource(R.string.theme_blue_name) else stringResource(R.string.theme_green_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_themes_change_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.translation_notice_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.ExtraModules.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_active_options_title),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_active_options_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_active_options_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_active_options_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = stringResource(R.string.settings_data_management_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.ServicePlatform.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = stringResource(R.string.settings_service_platforms_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_service_platforms_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_service_platforms_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_service_platforms_arrow_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Tarjeta para datos de facturación
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.BillingData.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.settings_billing_data_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_billing_data_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_billing_data_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.Tariffs.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = stringResource(R.string.settings_tariffs_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_tariffs_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_tariffs_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_tariffs_content_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = stringResource(R.string.settings_backup_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showBackupOptionsDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = stringResource(R.string.settings_local_backup_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_local_backup_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_local_backup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_local_backup_arrow_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.OnlineBackup.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = stringResource(R.string.settings_online_backup_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_online_backup_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_online_backup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_online_backup_arrow_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sección Acerca de
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            // Tarjeta de agradecimiento
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.Acknowledgment.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.settings_acknowledgment_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_acknowledgment_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_acknowledgment_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.settings_acknowledgment_arrow_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Información de versión
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_app_version_content_desc),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_app_version_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        val versionName = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (e: PackageManager.NameNotFoundException) {
                            stringResource(R.string.settings_default_version)
                        }
                        Text(
                            text = stringResource(R.string.settings_app_version_desc, versionName ?: "Unknown"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }


        // Diálogo para seleccionar opciones de copia local
        if (showBackupOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showBackupOptionsDialog = false },
                title = { Text(stringResource(R.string.dialog_backup_options_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.dialog_backup_options_message))
                        
                        // Opción para crear backup mensual
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showBackupOptionsDialog = false
                                showCreateMonthlyBackupDialog = true 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.backup_option_monthly),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Opción para crear backup completo
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showBackupOptionsDialog = false
                                showCreateFullBackupDialog = true 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.backup_option_full),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Opción para importar backup
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showBackupOptionsDialog = false
                                selectBackupLauncher.launch("*/*")
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.backup_option_import),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBackupOptionsDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        
        // Diálogo para crear backup mensual
        if (showCreateMonthlyBackupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateMonthlyBackupDialog = false },
                title = { Text(stringResource(R.string.dialog_backup_monthly_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_backup_monthly_message))
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.backup_progress_message))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupInProgress = true
                            scope.launch {
                                try {
                                    kotlinx.coroutines.delay(5000)
                                    val selectedDate = application.getSelectedDate().first()
                                    val uri = application.backupService.createMonthlyBackup(selectedDate)
                                    
                                    if (uri != null) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_monthly_success))
                                        // Compartir el archivo de backup
                                        val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale.getDefault())
                                        val fileName = "Taxi_Backup_Mes_${monthYearFormat.format(selectedDate)}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                        application.backupService.shareFile(uri, fileName)
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_monthly_error))
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.snackbar_generic_error,
                                            e.message.orEmpty()
                                        )
                                    )
                                } finally {
                                    backupInProgress = false
                                    showCreateMonthlyBackupDialog = false
                                }
                            }
                        },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateMonthlyBackupDialog = false },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        
        // Diálogo para crear backup completo
        if (showCreateFullBackupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFullBackupDialog = false },
                title = { Text(stringResource(R.string.dialog_backup_full_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_backup_full_message))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.dialog_backup_full_note),
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.backup_progress_message))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupInProgress = true
                            scope.launch {
                                try {
                                    kotlinx.coroutines.delay(5000)
                                    val uri = application.backupService.createFullBackup()
                                    
                                    if (uri != null) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_full_success))
                                        // Compartir el archivo de backup
                                        val fileName = "Taxi_Backup_Completo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                        application.backupService.shareFile(uri, fileName)
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_full_error))
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        context.getString(
                                            R.string.snackbar_generic_error,
                                            e.message.orEmpty()
                                        )
                                    )
                                } finally {
                                    backupInProgress = false
                                    showCreateFullBackupDialog = false
                                }
                            }
                        },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.action_create))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateFullBackupDialog = false },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        
        // Diálogo de confirmación para importar backup
        if (showBackupConfirmDialog && backupUri != null) {
            AlertDialog(
                onDismissRequest = { 
                    showBackupConfirmDialog = false
                    backupUri = null
                },
                title = { Text(stringResource(R.string.dialog_backup_import_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_backup_import_message))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.dialog_backup_import_note),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.dialog_backup_import_warning),
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.backup_import_progress_message))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentUri = backupUri
                            if (currentUri != null) {
                                backupInProgress = true
                                scope.launch {
                                    try {
                                        kotlinx.coroutines.delay(5000)
                                        val success = application.backupService.restoreBackup(currentUri)
                                        
                                        if (success) {
                                            snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_import_success))
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.snackbar_backup_import_error))
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(
                                                R.string.snackbar_generic_error,
                                                e.message.orEmpty()
                                            )
                                        )
                                    } finally {
                                        backupInProgress = false
                                        showBackupConfirmDialog = false
                                        backupUri = null
                                    }
                                }
                            }
                        },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.action_import_yes))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showBackupConfirmDialog = false
                            backupUri = null
                        },
                        enabled = !backupInProgress
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
        // El diálogo de selección de ciudad ha sido eliminado
        
        // Diálogo para seleccionar el tema
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text(stringResource(R.string.settings_themes_dialog_title)) },
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { application.saveAppTheme("blue") }
                                    showThemeDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            RadioButton(
                                selected = appTheme == "blue",
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.theme_blue_name))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch { application.saveAppTheme("green") }
                                    showThemeDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            RadioButton(
                                selected = appTheme == "green",
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.theme_green_name))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }
}
