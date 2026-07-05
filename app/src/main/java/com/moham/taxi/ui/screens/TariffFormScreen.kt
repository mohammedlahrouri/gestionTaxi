package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.Tariff
import com.moham.taxi.ui.viewmodel.TariffViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariffFormScreen(navController: NavController, tariffId: Long) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val viewModel: TariffViewModel = viewModel(
        factory = TariffViewModel.Factory(application)
    )
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var isFixed by remember { mutableStateOf(false) }
    var fixedPrice by remember { mutableStateOf("") }
    var baseFare by remember { mutableStateOf("") }
    var pricePerKm by remember { mutableStateOf("") }
    var surcharge by remember { mutableStateOf("") }

    LaunchedEffect(tariffId) {
        if (tariffId != -1L) {
            val t = viewModel.getTariffById(tariffId)
            if (t != null) {
                name = t.name
                isFixed = t.isFixed
                fixedPrice = t.fixedPrice?.toString() ?: ""
                baseFare = t.baseFare?.toString() ?: ""
                pricePerKm = t.pricePerKm?.toString() ?: ""
                surcharge = t.surcharge?.toString() ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (tariffId == -1L) stringResource(R.string.title_add_tariff) else stringResource(R.string.title_edit_tariff)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_tariff_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(R.string.label_is_fixed_price))
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isFixed, onCheckedChange = { isFixed = it })
            }

            if (isFixed) {
                OutlinedTextField(
                    value = fixedPrice,
                    onValueChange = { fixedPrice = it },
                    label = { Text(stringResource(R.string.label_fixed_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = baseFare,
                    onValueChange = { baseFare = it },
                    label = { Text(stringResource(R.string.label_base_fare)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pricePerKm,
                    onValueChange = { pricePerKm = it },
                    label = { Text(stringResource(R.string.label_price_per_km)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val tariff = Tariff(
                        id = if (tariffId == -1L) 0L else tariffId,
                        name = name,
                        isFixed = isFixed,
                        fixedPrice = fixedPrice.toDoubleOrNull(),
                        baseFare = baseFare.toDoubleOrNull(),
                        pricePerKm = pricePerKm.toDoubleOrNull(),
                        surcharge = surcharge.toDoubleOrNull()
                    )
                    if (tariffId == -1L) {
                        viewModel.insert(tariff)
                    } else {
                        viewModel.update(tariff)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}