package com.moham.taxi.ui.screens

import androidx.compose.ui.res.stringResource
import com.moham.taxi.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.data.model.ServiceMode
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import com.moham.taxi.ui.viewmodel.ServicePlatformViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicePlatformScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication

    val servicePlatformViewModel: ServicePlatformViewModel = viewModel(
        factory = ServicePlatformViewModel.ServicePlatformViewModelFactory(
            repository = application.servicePlatformRepository
        )
    )
    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.PaymentMethodViewModelFactory(
            repository = application.paymentMethodRepository
        )
    )

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var platformToDelete by remember { mutableStateOf<ServicePlatform?>(null) }
    var newPlatformName by remember { mutableStateOf("") }
    var newPlatformCommission by remember { mutableStateOf("") }
    var newPlatformVat by remember { mutableStateOf("") }
    var newPlatformHasCommission by remember { mutableStateOf(false) }
    var newPlatformAlternativeMath by remember { mutableStateOf(false) }
    var newPlatformServiceMode by remember { mutableStateOf(ServiceMode.BOTH) }
    var nameError by remember { mutableStateOf(false) }
    var commissionError by remember { mutableStateOf(false) }
    var vatError by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var platformToEdit by remember { mutableStateOf<ServicePlatform?>(null) }
    var editPlatformName by remember { mutableStateOf("") }
    var editPlatformCommission by remember { mutableStateOf("") }
    var editPlatformVat by remember { mutableStateOf("") }
    var editPlatformHasCommission by remember { mutableStateOf(false) }
    var editPlatformAlternativeMath by remember { mutableStateOf(false) }
    var editPlatformServiceMode by remember { mutableStateOf(ServiceMode.BOTH) }
    var editNameError by remember { mutableStateOf(false) }
    var editCommissionError by remember { mutableStateOf(false) }
    var editVatError by remember { mutableStateOf(false) }
    var newPlatformPaymentMethodIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var newPaymentMethodName by remember { mutableStateOf("") }
    var editPlatformPaymentMethodIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var editPaymentMethodName by remember { mutableStateOf("") }

    val servicePlatforms by servicePlatformViewModel.allServicePlatforms.collectAsState(initial = emptyList())
    val allPaymentMethods by paymentMethodViewModel.allPaymentMethods.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun parseCommission(text: String): Double? {
        return text.replace(',', '.').toDoubleOrNull()
    }

    LaunchedEffect(showAddDialog, allPaymentMethods) {
        if (showAddDialog && newPlatformPaymentMethodIds.isEmpty() && allPaymentMethods.isNotEmpty()) {
            val defaults = allPaymentMethods
                .filter { it.name.equals("Efectivo", ignoreCase = true) || it.name.equals("Tarjeta", ignoreCase = true) }
                .map { it.id }
                .toSet()
            newPlatformPaymentMethodIds = defaults
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_service_platforms_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_platform)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.title_available_platforms),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (servicePlatforms.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_platforms_msg),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servicePlatforms) { platform ->
                        ServicePlatformItem(
                            servicePlatform = platform,
                            canDelete = platform.name != "Directo",
                            onClick = {
                                platformToEdit = platform
                                editPlatformName = platform.name
                                editPlatformCommission = platform.commissionPercentage?.let { formatCommission(it) } ?: ""
                                editPlatformVat = platform.commissionVat?.let { formatCommission(it) } ?: ""
                                editPlatformHasCommission = platform.commissionPercentage != null
                                editPlatformAlternativeMath = platform.useAlternativeMath
                                editPlatformServiceMode = platform.serviceMode
                                editNameError = false
                                editCommissionError = false
                                editVatError = false
                                editPaymentMethodName = ""
                                scope.launch {
                                    editPlatformPaymentMethodIds = application.paymentMethodRepository
                                        .getPaymentMethodsForPlatform(platform.id)
                                        .first()
                                        .map { it.id }
                                        .toSet()
                                }
                                showEditDialog = true
                            },
                            onDelete = {
                                platformToDelete = platform
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.help_add_more_platforms),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (showAddDialog) {
            Dialog(
                onDismissRequest = { showAddDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.dialog_new_platform_title)) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    newPlatformName = ""
                                    newPlatformCommission = ""
                                    newPlatformVat = ""
                                    newPlatformHasCommission = false
                                    newPlatformAlternativeMath = false
                                    newPlatformPaymentMethodIds = emptySet()
                                    newPaymentMethodName = ""
                                    showAddDialog = false
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                            actions = {
                                TextButton(
                                    onClick = {
                                        if (newPlatformName.isBlank()) {
                                            nameError = true
                                            return@TextButton
                                        }
                                        val commissionValue = if (newPlatformHasCommission) {
                                            val parsed = parseCommission(newPlatformCommission)
                                            if (parsed == null) {
                                                commissionError = true
                                                return@TextButton
                                            }
                                            parsed
                                        } else {
                                            null
                                        }
                                        val vatValue = if (newPlatformHasCommission && newPlatformVat.isNotBlank()) {
                                            val parsedVat = parseCommission(newPlatformVat)
                                            if (parsedVat == null) {
                                                vatError = true
                                                return@TextButton
                                            }
                                            parsedVat
                                        } else {
                                            null
                                        }
                                        scope.launch {
                                            val platformId = application.servicePlatformRepository.insert(
                                                ServicePlatform(
                                                    name = newPlatformName,
                                                    commissionPercentage = commissionValue,
                                                    commissionVat = vatValue,
                                                    useAlternativeMath = newPlatformAlternativeMath,
                                                    serviceMode = newPlatformServiceMode
                                                )
                                            )
                                            application.paymentMethodRepository.setPaymentMethodsForPlatform(
                                                platformId = platformId,
                                                paymentMethodIds = newPlatformPaymentMethodIds.toList()
                                            )
                                            newPlatformName = ""
                                            newPlatformCommission = ""
                                            newPlatformVat = ""
                                            newPlatformHasCommission = false
                                            newPlatformAlternativeMath = false
                                            newPlatformServiceMode = ServiceMode.BOTH
                                            newPlatformPaymentMethodIds = emptySet()
                                            newPaymentMethodName = ""
                                            showAddDialog = false
                                            snackbarHostState.showSnackbar(context.getString(R.string.msg_platform_added))
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.label_enter_platform_name))
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = newPlatformName,
                            onValueChange = {
                                newPlatformName = it
                                nameError = false
                            },
                            label = stringResource(R.string.label_name),
                            isError = nameError,
                            errorMessage = if (nameError) stringResource(R.string.error_name_empty) else "",
                            imeAction = ImeAction.Next
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.label_apply_commission))
                            Switch(
                                checked = newPlatformHasCommission,
                                onCheckedChange = {
                                    newPlatformHasCommission = it
                                    if (!it) {
                                        newPlatformCommission = ""
                                        commissionError = false
                                        newPlatformVat = ""
                                        vatError = false
                                    }
                                }
                            )
                        }
                        if (newPlatformHasCommission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TaxiTextField(
                                value = newPlatformCommission,
                                onValueChange = {
                                    newPlatformCommission = it
                                    commissionError = false
                                },
                                label = stringResource(R.string.label_commission_percentage),
                                isError = commissionError,
                                errorMessage = if (commissionError) stringResource(R.string.error_commission_invalid) else "",
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TaxiTextField(
                                value = newPlatformVat,
                                onValueChange = {
                                    newPlatformVat = it
                                    vatError = false
                                },
                                label = stringResource(R.string.label_vat_percentage),
                                isError = vatError,
                                errorMessage = if (vatError) stringResource(R.string.error_vat_invalid) else "",
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.help_vat_info))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.label_alternative_math))
                                Switch(
                                    checked = newPlatformAlternativeMath,
                                    onCheckedChange = { newPlatformAlternativeMath = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.help_alternative_math),
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.label_service_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = newPlatformServiceMode == ServiceMode.BOTH,
                                onClick = { newPlatformServiceMode = ServiceMode.BOTH }
                            )
                            Text(stringResource(R.string.service_mode_both))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = newPlatformServiceMode == ServiceMode.METER_ONLY,
                                onClick = { newPlatformServiceMode = ServiceMode.METER_ONLY }
                            )
                            Text(stringResource(R.string.service_mode_meter_only))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = newPlatformServiceMode == ServiceMode.FIXED_ONLY,
                                onClick = { newPlatformServiceMode = ServiceMode.FIXED_ONLY }
                            )
                            Text(stringResource(R.string.service_mode_fixed_only))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Métodos de pago",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val cashLabel = stringResource(R.string.payment_cash)
                        val cardLabel = stringResource(R.string.payment_card)
                        val appLabel = stringResource(R.string.payment_via_app)
                        fun displayPaymentMethodName(value: String): String {
                            val lower = value.trim().lowercase()
                            return when {
                                lower == "efectivo" || lower == "cash" -> cashLabel
                                lower == "tarjeta" || lower == "card" -> cardLabel
                                lower.replace(" ", "") == "viaapp" || lower == "via app" -> appLabel
                                else -> value
                            }
                        }
                        allPaymentMethods.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = newPlatformPaymentMethodIds.contains(method.id),
                                    onCheckedChange = { checked ->
                                        newPlatformPaymentMethodIds = if (checked) {
                                            newPlatformPaymentMethodIds + method.id
                                        } else {
                                            newPlatformPaymentMethodIds - method.id
                                        }
                                    }
                                )
                                Text(displayPaymentMethodName(method.name))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = newPaymentMethodName,
                            onValueChange = { newPaymentMethodName = it },
                            label = "Añadir método",
                            imeAction = ImeAction.Done
                        )
                        TextButton(
                            enabled = newPaymentMethodName.trim().isNotBlank(),
                            onClick = {
                                val name = newPaymentMethodName.trim()
                                scope.launch {
                                    val id = application.paymentMethodRepository.getOrCreatePaymentMethodId(name)
                                    newPlatformPaymentMethodIds = newPlatformPaymentMethodIds + id
                                    newPaymentMethodName = ""
                                }
                            }
                        ) {
                            Text("Añadir", color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            Dialog(
                onDismissRequest = { showEditDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.dialog_edit_platform_title)) },
                            navigationIcon = {
                                IconButton(onClick = { showEditDialog = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                            actions = {
                                TextButton(
                                    onClick = {
                                        if (editPlatformName.isBlank()) {
                                            editNameError = true
                                            return@TextButton
                                        }
                                        val commissionValue = if (editPlatformHasCommission) {
                                            val parsed = parseCommission(editPlatformCommission)
                                            if (parsed == null) {
                                                editCommissionError = true
                                                return@TextButton
                                            }
                                            parsed
                                        } else {
                                            null
                                        }
                                        val vatValue = if (editPlatformHasCommission && editPlatformVat.isNotBlank()) {
                                            val parsedVat = parseCommission(editPlatformVat)
                                            if (parsedVat == null) {
                                                editVatError = true
                                                return@TextButton
                                            }
                                            parsedVat
                                        } else {
                                            null
                                        }
                                        platformToEdit?.let { platform ->
                                            scope.launch {
                                                servicePlatformViewModel.update(
                                                    platform.copy(
                                                        name = editPlatformName,
                                                        commissionPercentage = commissionValue,
                                                        commissionVat = vatValue,
                                                        useAlternativeMath = editPlatformAlternativeMath,
                                                        serviceMode = editPlatformServiceMode
                                                    )
                                                )
                                                application.paymentMethodRepository.setPaymentMethodsForPlatform(
                                                    platformId = platform.id,
                                                    paymentMethodIds = editPlatformPaymentMethodIds.toList()
                                                )
                                                showEditDialog = false
                                                snackbarHostState.showSnackbar(context.getString(R.string.msg_platform_updated))
                                            }
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.action_save_changes), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.label_update_platform_info))
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = editPlatformName,
                            onValueChange = {
                                editPlatformName = it
                                editNameError = false
                            },
                            label = stringResource(R.string.label_name),
                            isError = editNameError,
                            errorMessage = if (editNameError) stringResource(R.string.error_name_empty) else "",
                            imeAction = ImeAction.Next
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.label_apply_commission))
                            Switch(
                                checked = editPlatformHasCommission,
                                onCheckedChange = {
                                    editPlatformHasCommission = it
                                    if (!it) {
                                        editPlatformCommission = ""
                                        editCommissionError = false
                                        editPlatformVat = ""
                                        editVatError = false
                                    }
                                }
                            )
                        }
                        if (editPlatformHasCommission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TaxiTextField(
                                value = editPlatformCommission,
                                onValueChange = {
                                    editPlatformCommission = it
                                    editCommissionError = false
                                },
                                label = stringResource(R.string.label_commission_percentage),
                                isError = editCommissionError,
                                errorMessage = if (editCommissionError) stringResource(R.string.error_commission_invalid) else "",
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TaxiTextField(
                                value = editPlatformVat,
                                onValueChange = {
                                    editPlatformVat = it
                                    editVatError = false
                                },
                                label = stringResource(R.string.label_vat_percentage),
                                isError = editVatError,
                                errorMessage = if (editVatError) stringResource(R.string.error_vat_invalid) else "",
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.help_vat_info))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.label_alternative_math))
                                Switch(
                                    checked = editPlatformAlternativeMath,
                                    onCheckedChange = { editPlatformAlternativeMath = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.help_alternative_math),
                                style = MaterialTheme.typography.bodySmall,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.label_service_mode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = editPlatformServiceMode == ServiceMode.BOTH,
                                onClick = { editPlatformServiceMode = ServiceMode.BOTH }
                            )
                            Text(stringResource(R.string.service_mode_both))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = editPlatformServiceMode == ServiceMode.METER_ONLY,
                                onClick = { editPlatformServiceMode = ServiceMode.METER_ONLY }
                            )
                            Text(stringResource(R.string.service_mode_meter_only))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = editPlatformServiceMode == ServiceMode.FIXED_ONLY,
                                onClick = { editPlatformServiceMode = ServiceMode.FIXED_ONLY }
                            )
                            Text(stringResource(R.string.service_mode_fixed_only))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Métodos de pago",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val cashLabel = stringResource(R.string.payment_cash)
                        val cardLabel = stringResource(R.string.payment_card)
                        val appLabel = stringResource(R.string.payment_via_app)
                        fun displayPaymentMethodName(value: String): String {
                            val lower = value.trim().lowercase()
                            return when {
                                lower == "efectivo" || lower == "cash" -> cashLabel
                                lower == "tarjeta" || lower == "card" -> cardLabel
                                lower.replace(" ", "") == "viaapp" || lower == "via app" -> appLabel
                                else -> value
                            }
                        }
                        allPaymentMethods.forEach { method ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = editPlatformPaymentMethodIds.contains(method.id),
                                    onCheckedChange = { checked ->
                                        editPlatformPaymentMethodIds = if (checked) {
                                            editPlatformPaymentMethodIds + method.id
                                        } else {
                                            editPlatformPaymentMethodIds - method.id
                                        }
                                    }
                                )
                                Text(displayPaymentMethodName(method.name))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = editPaymentMethodName,
                            onValueChange = { editPaymentMethodName = it },
                            label = "Añadir método",
                            imeAction = ImeAction.Done
                        )
                        TextButton(
                            enabled = editPaymentMethodName.trim().isNotBlank(),
                            onClick = {
                                val name = editPaymentMethodName.trim()
                                scope.launch {
                                    val id = application.paymentMethodRepository.getOrCreatePaymentMethodId(name)
                                    editPlatformPaymentMethodIds = editPlatformPaymentMethodIds + id
                                    editPaymentMethodName = ""
                                }
                            }
                        ) {
                            Text("Añadir", color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_platform_title)) },
                text = {
                    Text(stringResource(R.string.dialog_delete_platform_msg, platformToDelete?.name ?: ""))
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            platformToDelete?.let { platform ->
                                scope.launch {
                                    servicePlatformViewModel.delete(platform)
                                    showDeleteConfirmDialog = false
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_platform_deleted))
                                }
                            }
                        },
                        text = stringResource(R.string.delete)
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

private fun formatCommission(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

@Composable
fun ServicePlatformItem(
    servicePlatform: ServicePlatform,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val commissionLabel = servicePlatform.commissionPercentage?.let { commission ->
                servicePlatform.commissionVat?.let { vat ->
                    "${formatCommission(commission)}% + ${formatCommission(vat)}% IVA"
                } ?: "${formatCommission(commission)}%"
            } ?: stringResource(R.string.label_no_commission)
            Text(
                text = "${servicePlatform.name} - $commissionLabel",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_delete_platform),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
