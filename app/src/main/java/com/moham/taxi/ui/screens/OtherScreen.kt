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
import com.moham.taxi.GestionTaxiApplication
import androidx.compose.material.icons.filled.LocalGasStation
import com.moham.taxi.data.service.GasStationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val currentCity by application.getCurrentCity().collectAsState(initial = "Madrid")
    var showCommunityDialog by remember { mutableStateOf(false) }
    val isSpain = Locale.getDefault().country.equals("ES", ignoreCase = true) || 
                  GasStationService.getProvinceIdByCity(currentCity) != null

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
        containerColor = DarkBackground
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. CABECERA
            Text(
                text = stringResource(R.string.title_other_options),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            // 2. SECCIÓN DESTACADA (Herramientas principales)
            Text(
                text = stringResource(R.string.category_tools),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeaturedCard(
                    title = stringResource(R.string.option_prices),
                    subtitle = stringResource(R.string.desc_prices),
                    icon = Icons.Filled.Calculate,
                    gradientStart = PriceGradientStart,
                    gradientEnd = PriceGradientEnd,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(AppScreens.Price.route) }
                )
                FeaturedCard(
                    title = stringResource(R.string.option_invoices),
                    subtitle = stringResource(R.string.desc_invoices),
                    icon = Icons.Filled.Description,
                    gradientStart = InvoiceGradientStart,
                    gradientEnd = InvoiceGradientEnd,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(AppScreens.Invoice.route) }
                )
            }

            // 3. SECCIÓN UTILIDADES (Configuración y datos)
            Text(
                text = stringResource(R.string.category_config),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GridOptionCard(
                        title = stringResource(R.string.settings_title),
                        icon = Icons.Filled.Settings,
                        iconColor = NewStatsOtherIcon,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreens.Settings.route) }
                    )
                    GridOptionCard(
                        title = stringResource(R.string.settings_export_data_card_title),
                        icon = Icons.Filled.Download,
                        iconColor = NewStatsAppIcon,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreens.Export.route) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GridOptionCard(
                        title = stringResource(R.string.option_maintenance),
                        icon = Icons.Filled.Build,
                        iconColor = NewStatsFuelIcon,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreens.Maintenance.route) }
                    )
                    if (isSpain) {
                        GridOptionCard(
                            title = stringResource(R.string.option_fuel_prices),
                            icon = Icons.Filled.LocalGasStation,
                            iconColor = NewStatsFuelIcon,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(AppScreens.FuelPrices.route) }
                        )
                    } else {
                        // Hueco reservado para futuras utilidades
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 4. SECCIÓN COMUNIDAD Y SOPORTE
            Text(
                text = stringResource(R.string.category_community),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SupportListRow(
                        title = stringResource(R.string.option_whatsapp),
                        icon = Icons.Filled.Lightbulb,
                        iconColor = Color(0xFF25D366),
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/message/2J2FQ4D5IR75P1?src=qr")
                            )
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                    SupportListRow(
                        title = stringResource(R.string.option_community),
                        icon = Icons.Filled.People,
                        iconColor = Color(0xFF128C7E),
                        onClick = {
                            if (isSpain) {
                                showCommunityDialog = true
                            }
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                    SupportListRow(
                        title = stringResource(R.string.option_donations),
                        icon = Icons.Filled.Favorite,
                        iconColor = Color(0xFFEF5350),
                        onClick = { navController.navigate(AppScreens.Donations.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientStart: Color,
    gradientEnd: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(130.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = gradientStart.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Icono decorativo gigante en el fondo
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun GridOptionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SupportListRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}
