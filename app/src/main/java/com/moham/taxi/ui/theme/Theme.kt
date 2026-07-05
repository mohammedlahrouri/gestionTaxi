package com.moham.taxi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

@Composable
fun GestionTaxiTheme(
    appTheme: String = "blue",
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Desactivar colores dinámicos
    content: @Composable () -> Unit
) {
    // Reassign color tokens based on theme
    when (appTheme) {
        "green" -> {
            PrimaryBlue = Color(0xFF2E7D32)
            BlueAccent = Color(0xFF2E7D32)
            PurpleAccent = Color(0xFF2E7D32)
            YellowAccent = Color(0xFF2E7D32)
            OrangeAccent = Color(0xFF2E7D32)
            RidesButtonColor = Color(0xFF2E7D32)
            StatsButtonColor = Color(0xFF2E7D32)
            InvoiceButtonColor = Color(0xFF2E7D32)
            PriceButtonColor = Color(0xFF2E7D32)
            CalendarAccent = Color(0xFF2E7D32)
        }

        else -> { // "blue"
            PrimaryBlue = Color(0xFF1565C0)
            BlueAccent = Color(0xFF1565C0)
            PurpleAccent = Color(0xFF1565C0)
            YellowAccent = Color(0xFF1565C0)
            OrangeAccent = Color(0xFF1565C0)
            RidesButtonColor = Color(0xFF1565C0)
            StatsButtonColor = Color(0xFF1565C0)
            InvoiceButtonColor = Color(0xFF1565C0)
            PriceButtonColor = Color(0xFF1565C0)
            CalendarAccent = Color(0xFF1565C0)
        }
    }

    val DarkColorScheme = when (appTheme) {
        "green" -> darkColorScheme(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF4CAF50),
            tertiary = Color(0xFF81C784),
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = DarkCard,
            onSurfaceVariant = Color.White,
            error = Error,
            onError = Color.White
        )

        else -> darkColorScheme( // "blue"
            primary = PrimaryBlue,
            secondary = SecondaryPurple,
            tertiary = AccentGreen,
            background = DarkBackground,
            surface = DarkSurface,
            onPrimary = TextPrimary,
            onSecondary = TextPrimary,
            onTertiary = TextPrimary,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            surfaceVariant = DarkCard,
            onSurfaceVariant = TextPrimary,
            error = Error,
            onError = TextPrimary
        )
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> DarkColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        // statusBarColor está obsoleto. Considera usar WindowInsetsControllerCompat o la API de barras del sistema de Android 11+ para cambiar el color de la barra de estado de forma segura.
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
