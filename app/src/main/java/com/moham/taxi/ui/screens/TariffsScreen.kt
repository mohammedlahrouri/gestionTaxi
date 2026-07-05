package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.data.model.Surcharge
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.TariffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffsScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val tariffViewModel: TariffViewModel = viewModel(
        factory = TariffViewModel.Factory(application)
    )
    val tariffs by tariffViewModel.allTariffs.collectAsState()
    val currentCity by tariffViewModel.currentCity.collectAsState()
    val allSurcharges by tariffViewModel.allSurcharges.collectAsState()
    var tariffToDelete by remember { mutableStateOf<Tariff?>(null) }
    var surchargeToDelete by remember { mutableStateOf<Surcharge?>(null) }
    var showChangeCityDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var newCityName by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Tarifas, 1 = Suplementos

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_tariffs)) },
                actions = {
                    TextButton(onClick = { showRestoreDialog = true }) {
                        Text(stringResource(R.string.action_restore_defaults), color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentCity != "Madrid") {
                FloatingActionButton(onClick = { 
                    if (selectedTabIndex == 0) {
                        navController.navigate(AppScreens.TariffForm.createRouteWithId(-1L)) 
                    } else {
                        navController.navigate(AppScreens.SurchargeForm.createRouteWithId(-1L))
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.onboarding_add))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.city_label, currentCity),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        newCityName = ""
                        showChangeCityDialog = true
                    }) {
                        Text(stringResource(R.string.action_change_city))
                    }
                }
            }

            if (currentCity == "Madrid") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.madrid_configured_msg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.tab_tariffs)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.surcharges_and_packages)) }
                    )
                }

                if (selectedTabIndex == 0) {
                    if (tariffs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_tariffs_msg))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tariffs) { tariff ->
                                TariffItem(
                                    tariff = tariff,
                                    isEditable = currentCity != "Madrid",
                                    onEdit = { navController.navigate(AppScreens.TariffForm.createRouteWithId(tariff.id)) },
                                    onDelete = { tariffToDelete = tariff }
                                )
                            }
                        }
                    }
                } else {
                    if (allSurcharges.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.empty_surcharges_msg))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allSurcharges) { surcharge ->
                                SurchargeItem(
                                    surcharge = surcharge,
                                    onEdit = { navController.navigate(AppScreens.SurchargeForm.createRouteWithId(surcharge.id)) },
                                    onDelete = { surchargeToDelete = surcharge }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (tariffToDelete != null) {
            AlertDialog(
                onDismissRequest = { tariffToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_tariff_title)) },
                text = { Text(stringResource(R.string.dialog_delete_tariff_msg, tariffToDelete?.name ?: "")) },
                confirmButton = {
                    TextButton(onClick = {
                        tariffToDelete?.let { tariffViewModel.delete(it) }
                        tariffToDelete = null
                    }) {
                        Text(stringResource(R.string.action_accept))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { tariffToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (surchargeToDelete != null) {
            AlertDialog(
                onDismissRequest = { surchargeToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_surcharge_title)) },
                text = { Text(stringResource(R.string.dialog_delete_surcharge_msg, surchargeToDelete?.name ?: "")) },
                confirmButton = {
                    TextButton(onClick = {
                        surchargeToDelete?.let { tariffViewModel.deleteSurcharge(it) }
                        surchargeToDelete = null
                    }) {
                        Text(stringResource(R.string.action_accept))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { surchargeToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showChangeCityDialog) {
            AlertDialog(
                onDismissRequest = { showChangeCityDialog = false },
                title = { Text(stringResource(R.string.dialog_change_city_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.dialog_change_city_msg))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCityName,
                            onValueChange = { newCityName = it },
                            label = { Text(stringResource(R.string.label_new_city)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newCityName.isNotBlank()) {
                                tariffViewModel.changeCity(newCityName)
                                showChangeCityDialog = false
                            }
                        },
                        enabled = newCityName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeCityDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text(stringResource(R.string.dialog_restore_defaults_title)) },
                text = { Text(stringResource(R.string.dialog_restore_defaults_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        tariffViewModel.restoreDefaults()
                        showRestoreDialog = false
                    }) {
                        Text(stringResource(R.string.action_save)) // Podemos reusar o usar un string mejor, ej "Restaurar"
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun SurchargeItem(surcharge: Surcharge, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = surcharge.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = "${stringResource(R.string.price)}: ${formatCurrency(surcharge.price)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = if (surcharge.isPerItem) stringResource(R.string.label_package_multiple) else stringResource(R.string.label_surcharge_once), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TariffItem(tariff: Tariff, isEditable: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tariff.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (tariff.isFixed) {
                    Text(text = stringResource(R.string.label_fixed_price_short, formatCurrency(tariff.fixedPrice ?: 0.0)), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val base = formatCurrency(tariff.baseFare ?: 0.0)
                    val km = formatCurrency(tariff.pricePerKm ?: 0.0)
                    val sur = tariff.surcharge?.let { formatCurrency(it) } ?: formatCurrency(0.0)
                    Text(text = stringResource(R.string.label_tariff_breakdown, base, km, sur), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (isEditable) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}