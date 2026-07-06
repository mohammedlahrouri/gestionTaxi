package com.moham.taxi.ui.screens

// import androidx.compose.ui.res.stringResource // Already likely imported or redundant if not used, but adding to be safe or just checking
import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.*
import android.content.Intent
import android.net.Uri
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(navController: NavController) {
    val context = LocalContext.current
    var showCommunityDialog by remember { mutableStateOf(false) }
    val isSpain = Locale.getDefault().country.equals("ES", ignoreCase = true)

    val openCommunityLink = {
        val uri = Uri.parse("https://chat.whatsapp.com/F7XN8IGAw9s99m4W8ax9Ri")
        val whatsappIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp")
        try {
            context.startActivity(whatsappIntent)
        } catch (_: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    // Configurar el manejo del botón Atrás para volver a Home
    BackHandler {
        navController.navigate(AppScreens.Home.route) {
            popUpTo(AppScreens.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }
    
    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedItem = 3,
                onItemSelected = { index ->
                    when (index) {
                        0 -> navController.navigate(AppScreens.Home.route) {
                            popUpTo(AppScreens.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        1 -> navController.navigate(AppScreens.TaxiRideList.createRouteWithDate(System.currentTimeMillis()))
                        2 -> navController.navigate(AppScreens.Statistics.route)
                        3 -> {} 
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        if (showCommunityDialog) {
            AlertDialog(
                onDismissRequest = { showCommunityDialog = false },
                title = { Text(stringResource(R.string.community_dialog_title)) },
                text = { Text(stringResource(R.string.community_dialog_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCommunityDialog = false
                            openCommunityLink()
                        }
                    ) {
                        Text(stringResource(R.string.community_dialog_open))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCommunityDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Opción de Ajustes
            NewOptionRow(
                title = stringResource(R.string.settings_title),
                icon = Icons.Filled.Settings,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Settings.route) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

            NewOptionRow(
                title = stringResource(R.string.settings_export_data_card_title),
                icon = Icons.Filled.Download,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Export.route) }
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            
            // Opción de Precios
            NewOptionRow(
                title = stringResource(R.string.option_prices),
                icon = Icons.Filled.Calculate,
                gradientStart = PriceGradientStart,
                gradientEnd = PriceGradientEnd,
                onClick = { navController.navigate(AppScreens.Price.route) }
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            
            // Opción de Facturas
            NewOptionRow(
                title = stringResource(R.string.option_invoices),
                icon = Icons.Filled.Description,
                gradientStart = InvoiceGradientStart,
                gradientEnd = InvoiceGradientEnd,
                onClick = { navController.navigate(AppScreens.Invoice.route) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

            NewOptionRow(
                title = stringResource(R.string.option_maintenance),
                icon = Icons.Filled.Build,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Maintenance.route) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

            NewOptionRow(
                title = stringResource(R.string.option_whatsapp),
                icon = Icons.Filled.Lightbulb,
                gradientStart = NewStatsAppIcon,
                gradientEnd = NewStatsOtherIcon,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/message/2J2FQ4D5IR75P1?src=qr")
                    )
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

            NewOptionRow(
                title = stringResource(R.string.option_community),
                icon = Icons.Filled.People,
                gradientStart = PriceGradientStart,
                gradientEnd = PriceGradientEnd,
                onClick = {
                    if (isSpain) {
                        showCommunityDialog = true
                    }
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

            NewOptionRow(
                title = stringResource(R.string.option_donations),
                icon = Icons.Filled.Favorite,
                gradientStart = InvoiceGradientStart,
                gradientEnd = InvoiceGradientEnd,
                onClick = { navController.navigate(AppScreens.Donations.route) }
            )
        }
    }
}

@Composable
fun NewOptionRow(
    title: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icono con fondo degradado
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = gradientStart.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Texto
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        // Chevron
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
