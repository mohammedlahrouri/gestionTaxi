package com.moham.taxi.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.service.GasStation
import com.moham.taxi.data.service.GasStationService
import com.moham.taxi.data.service.Province
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelPricesScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()

    val currentCity by application.getCurrentCity().collectAsState(initial = "Madrid")
    
    // UI State
    var selectedProvince by remember { mutableStateOf<Province?>(null) }
    var selectedFuelType by remember { mutableStateOf("gasolina 95") } // Default fuel type
    var stationsList by remember { mutableStateOf<List<GasStation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showProvinceDialog by remember { mutableStateOf(false) }
    var isFuelDropdownExpanded by remember { mutableStateOf(false) }

    // Resolve initial province based on user's settings city
    LaunchedEffect(currentCity) {
        if (selectedProvince == null) {
            val provinceId = GasStationService.getProvinceIdByCity(currentCity) ?: "28" // Default to Madrid
            val matchedProvince = GasStationService.PROVINCES_LIST.find { it.id == provinceId }
            selectedProvince = matchedProvince ?: Province("28", "Madrid")
        }
    }

    // Load data when selectedProvince or selectedFuelType changes
    val loadPrices = {
        val province = selectedProvince
        if (province != null) {
            scope.launch {
                isLoading = true
                hasError = false
                try {
                    val results = GasStationService.getCheapestStationsByProvince(
                        context = context,
                        provinceId = province.id,
                        fuelType = selectedFuelType,
                        forceRefresh = false // Load from cache if same day
                    )
                    stationsList = results
                } catch (e: Exception) {
                    e.printStackTrace()
                    hasError = true
                    stationsList = emptyList()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(selectedProvince, selectedFuelType) {
        loadPrices()
    }

    BackHandler {
        navController.navigate(AppScreens.Other.route) {
            popUpTo(AppScreens.Other.route) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_fuel_prices),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(AppScreens.Other.route) {
                            popUpTo(AppScreens.Other.route) { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadPrices() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_refresh),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Province selector (OutlinedCard style with a down arrow)
            selectedProvince?.let { province ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProvinceDialog = true },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.select_province),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = province.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Fuel Selector Dropdown Selector
            val fuelTypes = listOf(
                Pair("gasolina 95", stringResource(R.string.fuel_gasolina95)),
                Pair("gasolina 98", stringResource(R.string.fuel_gasolina98)),
                Pair("diesel", stringResource(R.string.fuel_diesel)),
                Pair("glp", stringResource(R.string.fuel_glp)),
                Pair("gnc", stringResource(R.string.fuel_gnc))
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFuelDropdownExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.select_fuel_type),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val currentLabel = fuelTypes.find { it.first == selectedFuelType }?.second ?: selectedFuelType
                            Text(
                                text = currentLabel,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isFuelDropdownExpanded,
                    onDismissRequest = { isFuelDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(DarkCard)
                ) {
                    fuelTypes.forEach { (typeKey, typeLabel) ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = typeLabel,
                                    color = if (selectedFuelType == typeKey) NewStatsFuelIcon else Color.White,
                                    fontWeight = if (selectedFuelType == typeKey) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                selectedFuelType = typeKey
                                isFuelDropdownExpanded = false
                            }
                        )
                    }
                }
            }


            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NewStatsFuelIcon)
                    }
                } else if (hasError) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.error_loading_fuel_prices),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { loadPrices() },
                                colors = ButtonDefaults.buttonColors(containerColor = NewStatsFuelIcon)
                            ) {
                                Text(stringResource(R.string.btn_refresh), color = Color.White)
                            }
                        }
                    }
                } else if (stationsList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_stations_found),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(stationsList) { station ->
                            GasStationCard(
                                station = station,
                                fuelType = selectedFuelType,
                                onClick = {
                                    val uri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${Uri.encode(station.address)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // Fallback if no map application is installed
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProvinceDialog) {
        ProvinceSelectionDialog(
            onDismiss = { showProvinceDialog = false },
            onProvinceSelected = { province ->
                selectedProvince = province
                showProvinceDialog = false
            }
        )
    }
}

@Composable
fun GasStationCard(
    station: GasStation,
    fuelType: String,
    onClick: () -> Unit
) {
    val price = station.prices[fuelType] ?: 0.0
    val formattedPrice = String.format(Locale.getDefault(), "%.3f", price)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NewStatsFuelBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = NewStatsFuelIcon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (station.schedule.isNotBlank()) {
                    Text(
                        text = station.schedule,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$formattedPrice €",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = NewStatsFuelIcon
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvinceSelectionDialog(
    onDismiss: () -> Unit,
    onProvinceSelected: (Province) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredProvinces = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            GasStationService.PROVINCES_LIST
        } else {
            val normalizedQuery = searchQuery.lowercase(Locale.ROOT)
                .trim()
            GasStationService.PROVINCES_LIST.filter {
                it.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                it.id.contains(normalizedQuery)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .clip(RoundedCornerShape(24.dp)),
            color = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.select_province),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_province),
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = NewStatsFuelIcon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {})
                )

                // Provinces List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredProvinces) { province ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onProvinceSelected(province) }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NewStatsFuelIcon.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = province.id,
                                        color = NewStatsFuelIcon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = province.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
