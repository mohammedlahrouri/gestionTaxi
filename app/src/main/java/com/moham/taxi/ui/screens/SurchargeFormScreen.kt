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
import com.moham.taxi.data.model.Surcharge
import com.moham.taxi.ui.viewmodel.TariffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurchargeFormScreen(navController: NavController, surchargeId: Long) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val viewModel: TariffViewModel = viewModel(
        factory = TariffViewModel.Factory(application)
    )

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var isPerItem by remember { mutableStateOf(false) }

    LaunchedEffect(surchargeId) {
        if (surchargeId != -1L) {
            val s = viewModel.getSurchargeById(surchargeId)
            if (s != null) {
                name = s.name
                price = s.price.toString()
                isPerItem = s.isPerItem
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(if (surchargeId == -1L) R.string.title_add_surcharge else R.string.title_edit_surcharge)) })
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
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.price)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(R.string.label_is_per_item))
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isPerItem, onCheckedChange = { isPerItem = it })
            }

            Button(
                onClick = {
                    val surcharge = Surcharge(
                        id = if (surchargeId == -1L) 0L else surchargeId,
                        name = name,
                        price = price.toDoubleOrNull() ?: 0.0,
                        isPerItem = isPerItem
                    )
                    if (surchargeId == -1L) {
                        viewModel.insertSurcharge(surcharge)
                    } else {
                        viewModel.updateSurcharge(surcharge)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}