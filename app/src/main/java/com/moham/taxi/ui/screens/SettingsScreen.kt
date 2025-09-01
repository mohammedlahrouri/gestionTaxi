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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
// Importación de DateRange eliminada
import androidx.compose.material.icons.filled.Download

import androidx.compose.material.icons.filled.Info
// Importación de LocationCity eliminada
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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


    
    // Variables para controlar los diálogos de exportación
    var showExportDayDialog by remember { mutableStateOf(false) }
    var showExportWeekDialog by remember { mutableStateOf(false) }
    var showExportMonthDialog by remember { mutableStateOf(false) }
    var exportInProgress by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }

    // Variables para controlar los diálogos de backup
    var showBackupDialog by remember { mutableStateOf(false) }
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var showCreateMonthlyBackupDialog by remember { mutableStateOf(false) }
    var showCreateFullBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var backupInProgress by remember { mutableStateOf(false) }
    var showBackupConfirmDialog by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    
    // Variable para controlar el diálogo de selección de idioma
    var showLanguageDialog by remember { mutableStateOf(false) }
    

    
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
                title = { Text("Ajustes") },
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
                text = "Configuración General",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Tarjeta para selección de idioma
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLanguageDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Idioma",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = context.getString(R.string.language_settings),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Mostrar el idioma actual
                        val currentLanguage by application.getLanguage().collectAsState(initial = "es")
                        val languageName = when (currentLanguage) {
                            "en" -> context.getString(R.string.language_english)
                            "fr" -> context.getString(R.string.language_french)
                            "de" -> context.getString(R.string.language_german)
                            else -> context.getString(R.string.language_spanish)
                        }
                        
                        Text(
                            text = languageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Seleccionar idioma",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Gestión de Datos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            // Tarjeta para métodos de pago
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(AppScreens.PaymentMethod.route) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Métodos de pago",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Métodos de pago",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Gestionar los métodos de pago disponibles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a métodos de pago",
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
                        contentDescription = "Datos de facturación",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Datos facturación",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Gestiona tus datos fiscales para facturación",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sección de exportación de datos
            Text(
                text = "Exportación de Datos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            // Tarjeta única para exportar datos
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showExportOptionsDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Exportar datos",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exportar datos",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Descargar resumen de datos financieros",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Opciones de exportación",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sección de backup de datos
            Text(
                text = "Copia de Seguridad",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            
            // Tarjeta única para backup de datos
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
                        contentDescription = "Backup de datos",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup de datos",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Crear o restaurar copias de seguridad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Opciones de backup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sección Acerca de
            Text(
                text = "Acerca de",
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
                        contentDescription = "Agradecimiento",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Agradecimiento",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Sobre el desarrollador y el proyecto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Ir a agradecimiento",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
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
                        contentDescription = "Versión",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Versión de la aplicación",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        val context = LocalContext.current
                        val versionName = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (e: PackageManager.NameNotFoundException) {
            "1.20"
        }
                        Text(
                            text = "v$versionName - Group Fasata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        
        // Diálogo para seleccionar opciones de exportación
        if (showExportOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showExportOptionsDialog = false },
                title = { Text("Exportar datos") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Seleccione el período de tiempo para exportar:")
                        
                        // Opción para exportar día
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showExportOptionsDialog = false
                                showExportDayDialog = true 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Exportar día",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Opción para exportar semana
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showExportOptionsDialog = false
                                showExportWeekDialog = true 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Exportar semana",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Opción para exportar mes
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { 
                                showExportOptionsDialog = false
                                showExportMonthDialog = true 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Exportar mes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportOptionsDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para exportar datos del día
        if (showExportDayDialog) {
            AlertDialog(
                onDismissRequest = { showExportDayDialog = false },
                title = { Text("Exportar datos del día") },
                text = {
                    Column {
                        Text("Se exportarán los datos financieros del día seleccionado en la pantalla principal. El archivo se guardará en la carpeta de descargas.")
                        
                        if (exportInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Exportando datos, por favor espere...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            exportInProgress = true
                            scope.launch {
                                try {
                                    val selectedDate = application.getSelectedDate().first()
                                    val exportResult = application.exportService.exportDayData(selectedDate)
                                    
                                    when (exportResult) {
                                        is com.moham.taxi.data.service.ExportResult.Success -> {
                                            snackbarHostState.showSnackbar("Datos exportados correctamente")
                                            // Compartir el archivo exportado
                                            val shareResult = application.exportService.shareFile(exportResult.uri, exportResult.fileName)
                                            when (shareResult) {
                                                is com.moham.taxi.data.service.ShareResult.Error -> {
                                                    snackbarHostState.showSnackbar("Archivo exportado pero error al compartir: ${shareResult.message}")
                                                }
                                                else -> { }
                                            }
                                        }
                                        is com.moham.taxi.data.service.ExportResult.Error -> {
                                            snackbarHostState.showSnackbar("Error al exportar: ${exportResult.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    exportInProgress = false
                                    showExportDayDialog = false
                                }
                            }
                        },
                        enabled = !exportInProgress
                    ) {
                        Text("Exportar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExportDayDialog = false },
                        enabled = !exportInProgress
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para exportar datos de la semana
        if (showExportWeekDialog) {
            AlertDialog(
                onDismissRequest = { showExportWeekDialog = false },
                title = { Text("Exportar datos de la semana") },
                text = {
                    Column {
                        Text("Se exportarán los datos financieros de la semana que contiene el día seleccionado en la pantalla principal. El archivo se guardará en la carpeta de descargas.")
                        
                        if (exportInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Exportando datos, por favor espere...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            exportInProgress = true
                            scope.launch {
                                try {
                                    val selectedDate = application.getSelectedDate().first()
                                    val firstDayOfWeek = application.getFirstDayOfWeek().first()
                                    val exportResult = application.exportService.exportWeekData(selectedDate, firstDayOfWeek)
                                    
                                    when (exportResult) {
                                        is com.moham.taxi.data.service.ExportResult.Success -> {
                                            snackbarHostState.showSnackbar("Datos exportados correctamente")
                                            // Compartir el archivo exportado
                                            val shareResult = application.exportService.shareFile(exportResult.uri, exportResult.fileName)
                                            when (shareResult) {
                                                is com.moham.taxi.data.service.ShareResult.Error -> {
                                                    snackbarHostState.showSnackbar("Archivo exportado pero error al compartir: ${shareResult.message}")
                                                }
                                                else -> { }
                                            }
                                        }
                                        is com.moham.taxi.data.service.ExportResult.Error -> {
                                            snackbarHostState.showSnackbar("Error al exportar: ${exportResult.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    exportInProgress = false
                                    showExportWeekDialog = false
                                }
                            }
                        },
                        enabled = !exportInProgress
                    ) {
                        Text("Exportar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExportWeekDialog = false },
                        enabled = !exportInProgress
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para selección de idioma
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text(context.getString(R.string.language_settings)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Obtener el idioma actual
                        val currentLanguage by application.getLanguage().collectAsState(initial = "es")
                        
                        // Opción para español
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        application.saveLanguage("es")
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == "es",
                                onClick = {
                                    scope.launch {
                                        application.saveLanguage("es")
                                        showLanguageDialog = false
                                    }
                                }
                            )
                            Text(
                                text = context.getString(R.string.language_spanish),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Opción para inglés
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        application.saveLanguage("en")
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == "en",
                                onClick = {
                                    scope.launch {
                                        application.saveLanguage("en")
                                        showLanguageDialog = false
                                    }
                                }
                            )
                            Text(
                                text = context.getString(R.string.language_english),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Opción para francés
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        application.saveLanguage("fr")
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == "fr",
                                onClick = {
                                    scope.launch {
                                        application.saveLanguage("fr")
                                        showLanguageDialog = false
                                    }
                                }
                            )
                            Text(
                                text = context.getString(R.string.language_french),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        // Opción para alemán
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        application.saveLanguage("de")
                                        showLanguageDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLanguage == "de",
                                onClick = {
                                    scope.launch {
                                        application.saveLanguage("de")
                                        showLanguageDialog = false
                                    }
                                }
                            )
                            Text(
                                text = context.getString(R.string.language_german),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para exportar datos del mes
        if (showExportMonthDialog) {
            AlertDialog(
                onDismissRequest = { showExportMonthDialog = false },
                title = { Text("Exportar datos del mes") },
                text = {
                    Column {
                        Text("Se exportarán los datos financieros del mes que contiene el día seleccionado en la pantalla principal. El archivo se guardará en la carpeta de descargas.")
                        
                        if (exportInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Exportando datos, por favor espere...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            exportInProgress = true
                            scope.launch {
                                try {
                                    val selectedDate = application.getSelectedDate().first()
                                    val exportResult = application.exportService.exportMonthData(selectedDate)
                                    
                                    when (exportResult) {
                                        is com.moham.taxi.data.service.ExportResult.Success -> {
                                            snackbarHostState.showSnackbar("Datos exportados correctamente")
                                            // Compartir el archivo exportado
                                            val shareResult = application.exportService.shareFile(exportResult.uri, exportResult.fileName)
                                            when (shareResult) {
                                                is com.moham.taxi.data.service.ShareResult.Error -> {
                                                    snackbarHostState.showSnackbar("Archivo exportado pero error al compartir: ${shareResult.message}")
                                                }
                                                else -> { }
                                            }
                                        }
                                        is com.moham.taxi.data.service.ExportResult.Error -> {
                                            snackbarHostState.showSnackbar("Error al exportar: ${exportResult.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage ?: e.message}")
                                } finally {
                                    exportInProgress = false
                                    showExportMonthDialog = false
                                }
                            }
                        },
                        enabled = !exportInProgress
                    ) {
                        Text("Exportar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExportMonthDialog = false },
                        enabled = !exportInProgress
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para seleccionar opciones de backup
        if (showBackupOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showBackupOptionsDialog = false },
                title = { Text("Backup de datos") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Seleccione una opción de backup:")
                        
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
                                    text = "Backup Mensual",
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
                                    text = "Backup Completo",
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
                                selectBackupLauncher.launch("application/json")
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Importar backup",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBackupOptionsDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para crear backup mensual
        if (showCreateMonthlyBackupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateMonthlyBackupDialog = false },
                title = { Text("Crear backup mensual") },
                text = {
                    Column {
                        Text("Se creará una copia de seguridad de los datos del mes seleccionado en la pantalla principal. El archivo se guardará en la carpeta de descargas.")
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Creando backup, por favor espere...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupInProgress = true
                            scope.launch {
                                try {
                                    val selectedDate = application.getSelectedDate().first()
                                    val uri = application.backupService.createMonthlyBackup(selectedDate)
                                    
                                    if (uri != null) {
                                        snackbarHostState.showSnackbar("Backup mensual creado correctamente")
                                        // Compartir el archivo de backup
                                        val monthYearFormat = SimpleDateFormat("MMMM_yyyy", Locale("es", "ES"))
                                        val fileName = "Taxi_Backup_Mes_${monthYearFormat.format(selectedDate)}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                        application.backupService.shareFile(uri, fileName)
                                    } else {
                                        snackbarHostState.showSnackbar("Error al crear el backup mensual")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                } finally {
                                    backupInProgress = false
                                    showCreateMonthlyBackupDialog = false
                                }
                            }
                        },
                        enabled = !backupInProgress
                    ) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateMonthlyBackupDialog = false },
                        enabled = !backupInProgress
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // Diálogo para crear backup completo
        if (showCreateFullBackupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFullBackupDialog = false },
                title = { Text("Crear backup completo") },
                text = {
                    Column {
                        Text("Se creará una copia de seguridad con TODOS los datos de la aplicación. El archivo se guardará en la carpeta de descargas.")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Este backup incluirá todos los meses y puede ser usado para transferir todos los datos a otro dispositivo.",
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Creando backup, por favor espere...")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupInProgress = true
                            scope.launch {
                                try {
                                    val uri = application.backupService.createFullBackup()
                                    
                                    if (uri != null) {
                                        snackbarHostState.showSnackbar("Backup completo creado correctamente")
                                        // Compartir el archivo de backup
                                        val fileName = "Taxi_Backup_Completo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                        application.backupService.shareFile(uri, fileName)
                                    } else {
                                        snackbarHostState.showSnackbar("Error al crear el backup completo")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                } finally {
                                    backupInProgress = false
                                    showCreateFullBackupDialog = false
                                }
                            }
                        },
                        enabled = !backupInProgress
                    ) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCreateFullBackupDialog = false },
                        enabled = !backupInProgress
                    ) {
                        Text("Cancelar")
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
                title = { Text("Importar backup") },
                text = {
                    Column {
                        Text("¿Está seguro de que desea importar este backup?")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Si es un backup mensual, se reemplazarán todos los datos del mes incluido en el backup. Si es un backup completo, se reemplazarán TODOS los datos de la aplicación.",
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ADVERTENCIA: Esta acción no se puede deshacer. Se recomienda crear un backup de sus datos actuales antes de importar.",
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (backupInProgress) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Importando backup, por favor espere...")
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
                                        val success = application.backupService.restoreBackup(currentUri)
                                        
                                        if (success) {
                                            snackbarHostState.showSnackbar("Backup importado correctamente")
                                        } else {
                                            snackbarHostState.showSnackbar("Error al importar el backup")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
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
                        Text("Sí, importar")
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
                        Text("Cancelar")
                    }
                }
            )
        }
        
        // El diálogo de selección de ciudad ha sido eliminado
        

    }
}
