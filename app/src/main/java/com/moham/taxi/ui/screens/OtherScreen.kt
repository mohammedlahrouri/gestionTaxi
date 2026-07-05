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
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.title_other_options),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NewStatsBackground,
                    titleContentColor = NewStatsTextPrimary
                )
            )
        },
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
        containerColor = NewStatsBackground
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
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
             // Header Text
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.label_select_option),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NewStatsTextSecondary
                )
            }
            
            // Opción de Ajustes
            NewOptionCard(
                title = stringResource(R.string.settings_title),
                description = stringResource(R.string.settings_general_title),
                icon = Icons.Filled.Settings,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Settings.route) }
            )

            NewOptionCard(
                title = stringResource(R.string.settings_export_data_card_title),
                description = stringResource(R.string.settings_export_data_card_desc),
                icon = Icons.Filled.Download,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Export.route) }
            )
            
            // Opción de Precios
            NewOptionCard(
                title = stringResource(R.string.option_prices),
                description = stringResource(R.string.desc_prices),
                icon = Icons.Filled.Calculate,
                gradientStart = PriceGradientStart,
                gradientEnd = PriceGradientEnd,
                onClick = { navController.navigate(AppScreens.Price.route) }
            )
            
            // Opción de Facturas
            NewOptionCard(
                title = stringResource(R.string.option_invoices),
                description = stringResource(R.string.desc_invoices),
                icon = Icons.Filled.Description,
                gradientStart = InvoiceGradientStart,
                gradientEnd = InvoiceGradientEnd,
                onClick = { navController.navigate(AppScreens.Invoice.route) }
            )

            NewOptionCard(
                title = stringResource(R.string.option_maintenance),
                description = "",
                icon = Icons.Filled.Build,
                gradientStart = NewStatsOtherIcon,
                gradientEnd = NewStatsAppIcon,
                onClick = { navController.navigate(AppScreens.Maintenance.route) }
            )

            NewOptionCard(
                title = stringResource(R.string.option_whatsapp),
                description = stringResource(R.string.desc_whatsapp),
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

            NewOptionCard(
                title = stringResource(R.string.option_community),
                description = if (isSpain) stringResource(R.string.desc_community) else stringResource(R.string.region_not_available),
                icon = Icons.Filled.People,
                gradientStart = PriceGradientStart,
                gradientEnd = PriceGradientEnd,
                onClick = {
                    if (isSpain) {
                        showCommunityDialog = true
                    }
                }
            )

            NewOptionCard(
                title = stringResource(R.string.option_donations),
                description = stringResource(R.string.desc_donations),
                icon = Icons.Filled.Favorite,
                gradientStart = InvoiceGradientStart,
                gradientEnd = InvoiceGradientEnd,
                onClick = { navController.navigate(AppScreens.Donations.route) }
            )
        }
    }
}

@Composable
fun NewOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = NewStatsCardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NewStatsBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono con fondo degradado
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = gradientStart.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(16.dp))
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = NewStatsTextPrimary
                )
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NewStatsTextSecondary
                    )
                }
            }
            
            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward, // ChevronRight alternative
                contentDescription = null,
                tint = NewStatsTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
