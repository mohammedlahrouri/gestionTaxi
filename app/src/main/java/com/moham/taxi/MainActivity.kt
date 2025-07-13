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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.moham.taxi.ui.navigation.AppNavigation
import com.moham.taxi.ui.screens.SplashScreen
import com.moham.taxi.ui.screens.SplashScreenPreloadData
import com.moham.taxi.ui.theme.GestionTaxiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // Registro para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permisos concedidos para exportar datos", Toast.LENGTH_SHORT).show()
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
            GestionTaxiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var preloadData by remember { mutableStateOf<SplashScreenPreloadData?>(null) }
                    var showSplash by remember { mutableStateOf(true) }
                    val navController = rememberNavController()
                    
                    if (showSplash) {
                        SplashScreen(
                            onLoadingComplete = { data ->
                                preloadData = data
                                showSplash = false
                            }
                        )
                    } else {
                        if (preloadData != null) {
                            AppNavigation(navController = navController, preloadData = preloadData)
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
            // Todos los permisos ya están concedidos
            Toast.makeText(this, "Permisos de almacenamiento ya concedidos", Toast.LENGTH_SHORT).show()
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
                "La aplicación necesita permisos de almacenamiento para exportar datos. Por favor, concede los permisos.", 
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
                "Los permisos fueron denegados permanentemente. Para habilitar la exportación de datos, ve a Configuración > Aplicaciones > Gestión Taxi > Permisos y activa los permisos de almacenamiento.", 
                Toast.LENGTH_LONG
            ).show()
            
            // No abrir automáticamente la configuración para dar al usuario más control
            // El usuario puede ir manualmente si lo desea
        } else {
            Toast.makeText(
                this, 
                "Se necesitan permisos de almacenamiento para exportar datos. La aplicación funcionará normalmente, pero no podrás exportar datos hasta conceder los permisos.", 
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
            Toast.makeText(this, "No se pudo abrir la configuración", Toast.LENGTH_SHORT).show()
        }
    }
}
