package com.moham.taxi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.moham.taxi.ui.navigation.AppNavigation
import com.moham.taxi.ui.screens.SplashScreen
import com.moham.taxi.ui.screens.SplashScreenPreloadData
import com.moham.taxi.ui.theme.GestionTaxiTheme

class MainActivity : ComponentActivity() {
    
    // Registro para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, getString(R.string.toast_permissions_granted), Toast.LENGTH_SHORT).show()
        } else {
            handlePermissionDenied(permissions)
        }
    }
    

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        

        
        // Solicitar permisos necesarios
        requestStoragePermissions()
        
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as GestionTaxiApplication
            val appTheme by application.getAppTheme().collectAsState(initial = "blue")
            
            LaunchedEffect(appTheme) {
                if (appTheme == "green") {
                    val green = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                    com.moham.taxi.ui.theme.PrimaryBlue = green
                    com.moham.taxi.ui.theme.BlueAccent = green
                    com.moham.taxi.ui.theme.PurpleAccent = green
                    com.moham.taxi.ui.theme.YellowAccent = green
                    com.moham.taxi.ui.theme.OrangeAccent = green
                    com.moham.taxi.ui.theme.RidesButtonColor = green
                    com.moham.taxi.ui.theme.StatsButtonColor = green
                    com.moham.taxi.ui.theme.InvoiceButtonColor = green
                    com.moham.taxi.ui.theme.PriceButtonColor = green
                    com.moham.taxi.ui.theme.CalendarAccent = green
                } else {
                    val blue = androidx.compose.ui.graphics.Color(0xFF1565C0)
                    com.moham.taxi.ui.theme.PrimaryBlue = blue
                    com.moham.taxi.ui.theme.BlueAccent = blue
                    com.moham.taxi.ui.theme.PurpleAccent = blue
                    com.moham.taxi.ui.theme.YellowAccent = blue
                    com.moham.taxi.ui.theme.OrangeAccent = blue
                    com.moham.taxi.ui.theme.RidesButtonColor = blue
                    com.moham.taxi.ui.theme.StatsButtonColor = blue
                    com.moham.taxi.ui.theme.InvoiceButtonColor = blue
                    com.moham.taxi.ui.theme.PriceButtonColor = blue
                    com.moham.taxi.ui.theme.CalendarAccent = blue
                }
            }

            GestionTaxiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var preloadData by remember { mutableStateOf<SplashScreenPreloadData?>(null) }
                    var showSplash by remember { mutableStateOf(true) }
                    val navController = rememberNavController()
                    
                    when {
                        showSplash -> {
                            SplashScreen(
                                onLoadingComplete = { data ->
                                    preloadData = data
                                    showSplash = false
                                }
                            )
                        }
                        else -> {
                            if (preloadData != null) {
                                AppNavigation(navController = navController, preloadData = preloadData)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Solicita los permisos de almacenamiento necesarios según la versión de Android
     * Verifica primero si los permisos ya están concedidos
     */
    private fun requestStoragePermissions() {
        val permissions = getRequiredPermissions()
        
        // Si no hay permisos requeridos para esta versión de Android, no hacer nada
        if (permissions.isEmpty()) {
            return
        }
        
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            return
        }
        
        // Verificar si algún permiso fue denegado permanentemente antes de solicitar
        val shouldShowRationale = missingPermissions.any { permission ->
            shouldShowRequestPermissionRationale(permission)
        }
        
        if (shouldShowRationale) {
            // Mostrar explicación antes de solicitar permisos
            Toast.makeText(
                this,
                getString(R.string.toast_permissions_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
        
        requestPermissionLauncher.launch(missingPermissions.toTypedArray())
    }
    
    /**
     * Obtiene los permisos requeridos según la versión de Android
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa permisos granulares de medios
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Android 12 y anteriores
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * Maneja la denegación de permisos, incluyendo denegación permanente
     */
    private fun handlePermissionDenied(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        val permanentlyDenied = deniedPermissions.any { permission ->
            !shouldShowRequestPermissionRationale(permission)
        }
        
        if (permanentlyDenied) {
            Toast.makeText(
                this,
                getString(R.string.toast_permissions_permanently_denied, getString(R.string.app_name)),
                Toast.LENGTH_LONG
            ).show()
            
            // No abrir automáticamente la configuración para dar al usuario más control
            // El usuario puede ir manualmente si lo desea
        } else {
            Toast.makeText(
                this,
                getString(R.string.toast_permissions_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Abre la configuración de la aplicación para que el usuario pueda habilitar permisos manualmente
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_open_settings_error), Toast.LENGTH_SHORT).show()
        }
    }
}
