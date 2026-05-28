package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.moham.taxi.ui.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingDataScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as com.moham.taxi.GestionTaxiApplication
    val scope = rememberCoroutineScope()
    val billingDataFlow = application.getBillingData().collectAsState(initial = com.moham.taxi.BillingData("", "", "", "", "", ""))

    var name by remember { mutableStateOf("") }
    var nif by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }

    // Cargar datos al iniciar
    LaunchedEffect(billingDataFlow.value) {
        name = billingDataFlow.value.name
        nif = billingDataFlow.value.nif
        license = billingDataFlow.value.license
        street = billingDataFlow.value.street
        city = billingDataFlow.value.city
        postalCode = billingDataFlow.value.postalCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_billing_data)) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.label_name_surname)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nif,
                onValueChange = { nif = it },
                label = { Text(stringResource(R.string.label_owner_nif)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = license,
                onValueChange = { license = it },
                label = { Text(stringResource(R.string.label_license)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text(stringResource(R.string.label_street_number)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text(stringResource(R.string.label_city)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = postalCode,
                onValueChange = { postalCode = it },
                label = { Text(stringResource(R.string.label_owner_postal_code)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        application.saveBillingData(name, nif, license, street, city, postalCode)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
            if (showSaved) {
                Text(stringResource(R.string.msg_data_saved), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
} 
