package com.moham.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.TaxiRide
import com.moham.taxi.ui.components.InlineTicketPhoto
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.AccentRed
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.DarkCard
import com.moham.taxi.ui.theme.PrimaryBlue
import com.moham.taxi.ui.viewmodel.TaxiRideViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiRideDetailScreen(
    navController: NavHostController,
    rideId: Long
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()
    
    val taxiRideViewModel: TaxiRideViewModel = viewModel(
        factory = TaxiRideViewModel.TaxiRideViewModelFactory(
            repository = application.taxiRideRepository
        )
    )
    
    var ride by remember { mutableStateOf<TaxiRide?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(rideId) {
        if (rideId > 0) {
            ride = taxiRideViewModel.getTaxiRideById(rideId)
        }
    }

    val cashLabel = stringResource(R.string.payment_cash)
    val cardLabel = stringResource(R.string.payment_card)
    val appLabel = stringResource(R.string.payment_via_app)
    val subscribersLabel = stringResource(R.string.payment_subscribers)
    val pendingLabel = stringResource(R.string.payment_pending)
    val directLabel = stringResource(R.string.platform_direct)

    fun translatePaymentMethod(method: String): String {
        val lower = method.trim().lowercase()
        return when {
            lower == "efectivo" || lower == "cash" -> cashLabel
            lower == "tarjeta" || lower == "card" -> cardLabel
            lower.replace(" ", "") == "viaapp" || lower == "via app" -> appLabel
            lower == "abonados" || lower == "subscribers" || lower == "account" -> subscribersLabel
            lower == "pendientes" || lower == "pending" -> pendingLabel
            else -> method
        }
    }

    fun translatePlatform(platform: String): String {
        val lower = platform.trim().lowercase()
        return when {
            lower == "directo" || lower == "direct" -> directLabel
            else -> platform
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rides_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navController.currentDestination?.route?.startsWith(AppScreens.TaxiRideDetail.route) == true) {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ride?.let { currentRide ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date & Time
                    val dateStr = com.moham.taxi.utils.DateUtils.formatDate(currentRide.date, "dd/MM/yyyy")
                    val dateTimeStr = if (currentRide.rideTime.isNotBlank()) "$dateStr  ${currentRide.rideTime}" else dateStr
                    Text(dateTimeStr, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    // Origin & Destination Route
                    val hasOrigin = currentRide.origin.isNotBlank()
                    val hasDestination = currentRide.destination.isNotBlank()
                    if (hasOrigin || hasDestination) {
                        val routeText = if (hasOrigin && hasDestination) {
                            "${currentRide.origin.trim()} → ${currentRide.destination.trim()}"
                        } else if (hasOrigin) {
                            currentRide.origin.trim()
                        } else {
                            currentRide.destination.trim()
                        }
                        Text(routeText, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                    }

                    // Price
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val hasCommission = currentRide.netPrice != null && currentRide.netPrice != currentRide.price
                        if (hasCommission) {
                            Text(
                                text = formatCurrency(currentRide.price),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                        val displayPrice = currentRide.netPrice ?: currentRide.price
                        Text(
                            text = formatCurrency(displayPrice),
                            color = Color(0xFF10B981),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Service Platform (Uber, Lyft, Directo, etc.)
                    currentRide.servicePlatform?.let { platform ->
                        if (platform.isNotBlank()) {
                            Text(translatePlatform(platform).uppercase(), color = Color.White, fontSize = 16.sp)
                        }
                    }

                    // Payment Method
                    Text(
                        text = stringResource(R.string.paid_via, translatePaymentMethod(currentRide.paymentMethod)),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    
                    // Ticket photo at the end if present
                    if (!currentRide.ticketPhotoPath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InlineTicketPhoto(photoPath = currentRide.ticketPhotoPath)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.delete), color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                val route = AppScreens.TaxiRideForm.createRouteWithDateAndId(currentRide.date.time, currentRide.id)
                                navController.navigate(route)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.edit), color = Color.White)
                        }
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = DarkCard,
            title = { Text(stringResource(R.string.delete_ride_title), color = Color.White) },
            text = { Text(stringResource(R.string.delete_ride_confirm), color = Color.White.copy(0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    ride?.let { 
                        scope.launch { 
                            taxiRideViewModel.delete(it) 
                            navController.popBackStack()
                        } 
                    }
                }) { Text(stringResource(R.string.delete), color = AccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        )
    }
}
