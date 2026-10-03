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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.data.model.PaymentMethod
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.viewmodel.PaymentMethodViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.moham.taxi.ui.navigation.AppScreens

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(navController: NavHostController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    
    // ViewModel
    val paymentMethodViewModel: PaymentMethodViewModel = viewModel(
        factory = PaymentMethodViewModel.PaymentMethodViewModelFactory(
            repository = application.paymentMethodRepository
        )
    )
    
    // Estado para el formulario de nuevo método de pago
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showConfigureDialog by remember { mutableStateOf(false) }
    var methodToConfigure by remember { mutableStateOf<PaymentMethod?>(null) }
    var selectedConfigurePolicy by remember { mutableStateOf(PaymentMethod.POLICY_OPTIONAL) }
    var methodToDelete by remember { mutableStateOf<PaymentMethod?>(null) }
    var newMethodName by remember { mutableStateOf("") }
    var newMethodPhotoPolicy by remember { mutableStateOf(PaymentMethod.POLICY_OPTIONAL) }
    var nameError by remember { mutableStateOf(false) }
    var nameErrorMessage by remember { mutableStateOf("") }
    
    // Obtener métodos de pago
    val paymentMethods by paymentMethodViewModel.allPaymentMethods.collectAsState(initial = emptyList())
    val sortedPaymentMethods = remember(paymentMethods) {
        paymentMethods
            .distinctBy { it.name.trim().lowercase() }
            .sortedWith(
                compareBy<PaymentMethod> { PaymentMethod.getPaymentMethodSortOrder(it.name) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
    }
    
    // Coroutine scope y SnackbarHostState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_payment_methods)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                newMethodName = ""
                newMethodPhotoPolicy = PaymentMethod.POLICY_OPTIONAL
                nameError = false
                nameErrorMessage = ""
                showAddDialog = true 
            }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_payment_method)
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
                text = stringResource(R.string.title_available_payment_methods),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (sortedPaymentMethods.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_payment_methods_msg),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedPaymentMethods) { method ->
                        PaymentMethodItem(
                            paymentMethod = method,
                            onClick = {
                                methodToConfigure = method
                                selectedConfigurePolicy = method.ticketPhotoPolicy
                                showConfigureDialog = true
                            },
                            onDelete = {
                                if (!method.isDefault()) {
                                    methodToDelete = method
                                    showDeleteConfirmDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // Diálogo para añadir nuevo método de pago
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { 
                    newMethodName = ""
                    newMethodPhotoPolicy = PaymentMethod.POLICY_OPTIONAL
                    nameError = false
                    nameErrorMessage = ""
                    showAddDialog = false 
                },
                title = { Text(stringResource(R.string.dialog_new_payment_method_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(stringResource(R.string.label_enter_payment_method_name))
                            Spacer(modifier = Modifier.height(8.dp))
                            TaxiTextField(
                                value = newMethodName,
                                onValueChange = { 
                                    newMethodName = it
                                    nameError = false
                                    nameErrorMessage = ""
                                },
                                label = stringResource(R.string.label_name),
                                isError = nameError,
                                errorMessage = nameErrorMessage
                            )
                        }

                        PhotoPolicySelector(
                            selectedPolicy = newMethodPhotoPolicy,
                            onSelectPolicy = { newMethodPhotoPolicy = it }
                        )
                    }
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            val trimmed = newMethodName.trim()
                            if (trimmed.isBlank()) {
                                nameError = true
                                nameErrorMessage = context.getString(R.string.error_name_empty)
                            } else if (PaymentMethod.isDefaultPaymentMethod(trimmed)) {
                                nameError = true
                                nameErrorMessage = context.getString(R.string.error_payment_method_default)
                            } else if (paymentMethods.any { it.name.trim().equals(trimmed, ignoreCase = true) }) {
                                nameError = true
                                nameErrorMessage = context.getString(R.string.error_payment_method_exists)
                            } else {
                                scope.launch {
                                    paymentMethodViewModel.insert(
                                        PaymentMethod(
                                            name = trimmed,
                                            ticketPhotoPolicy = newMethodPhotoPolicy
                                        )
                                    )
                                    newMethodName = ""
                                    newMethodPhotoPolicy = PaymentMethod.POLICY_OPTIONAL
                                    nameError = false
                                    nameErrorMessage = ""
                                    showAddDialog = false
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_payment_method_added))
                                }
                            }
                        },
                        text = stringResource(R.string.save)
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            newMethodName = ""
                            newMethodPhotoPolicy = PaymentMethod.POLICY_OPTIONAL
                            nameError = false
                            nameErrorMessage = ""
                            showAddDialog = false 
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        // Diálogo para configurar política de foto de método existente
        if (showConfigureDialog && methodToConfigure != null) {
            val method = methodToConfigure!!
            AlertDialog(
                onDismissRequest = { 
                    showConfigureDialog = false 
                    methodToConfigure = null
                },
                title = { 
                    Text("${stringResource(R.string.dialog_configure_payment_method)}: ${method.name}")
                },
                text = {
                    Column {
                        PhotoPolicySelector(
                            selectedPolicy = selectedConfigurePolicy,
                            onSelectPolicy = { selectedConfigurePolicy = it }
                        )
                    }
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            scope.launch {
                                paymentMethodViewModel.update(
                                    method.copy(ticketPhotoPolicy = selectedConfigurePolicy)
                                )
                                showConfigureDialog = false
                                methodToConfigure = null
                                snackbarHostState.showSnackbar(context.getString(R.string.msg_payment_method_updated))
                            }
                        },
                        text = stringResource(R.string.save)
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showConfigureDialog = false
                            methodToConfigure = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // Diálogo de confirmación para eliminar método de pago
        if (showDeleteConfirmDialog && methodToDelete?.isDefault() == false) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_payment_method_title)) },
                text = { 
                    Text(stringResource(R.string.dialog_delete_payment_method_msg, methodToDelete?.name ?: "")) 
                },
                confirmButton = {
                    TaxiButton(
                        onClick = {
                            methodToDelete?.let { method ->
                                scope.launch {
                                    paymentMethodViewModel.delete(method)
                                    showDeleteConfirmDialog = false
                                    snackbarHostState.showSnackbar(context.getString(R.string.msg_payment_method_deleted))
                                }
                            }
                        },
                        text = stringResource(R.string.delete)
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun PhotoPolicySelector(
    selectedPolicy: String,
    onSelectPolicy: (String) -> Unit
) {
    val options = listOf(
        Triple(
            PaymentMethod.POLICY_NO_PHOTO,
            stringResource(R.string.photo_policy_no_photo),
            stringResource(R.string.photo_policy_no_photo_desc)
        ),
        Triple(
            PaymentMethod.POLICY_OPTIONAL,
            stringResource(R.string.photo_policy_optional),
            stringResource(R.string.photo_policy_optional_desc)
        ),
        Triple(
            PaymentMethod.POLICY_MANDATORY,
            stringResource(R.string.photo_policy_mandatory),
            stringResource(R.string.photo_policy_mandatory_desc)
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.payment_method_photo_policy_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        options.forEach { (policy, title, description) ->
            val isSelected = selectedPolicy == policy
            OutlinedCard(
                onClick = { onSelectPolicy(policy) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectPolicy(policy) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(
    paymentMethod: PaymentMethod,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDefault = paymentMethod.isDefault()
    val policyBadgeText = when (paymentMethod.ticketPhotoPolicy) {
        PaymentMethod.POLICY_NO_PHOTO -> stringResource(R.string.badge_no_photo)
        PaymentMethod.POLICY_MANDATORY -> stringResource(R.string.badge_photo_mandatory)
        else -> stringResource(R.string.badge_photo_optional)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = paymentMethod.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isDefault) {
                        SuggestionChip(
                            onClick = onClick,
                            label = {
                                Text(
                                    text = stringResource(R.string.payment_method_default_badge),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = when (paymentMethod.ticketPhotoPolicy) {
                            PaymentMethod.POLICY_MANDATORY -> MaterialTheme.colorScheme.error
                            PaymentMethod.POLICY_NO_PHOTO -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = policyBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (paymentMethod.ticketPhotoPolicy) {
                            PaymentMethod.POLICY_MANDATORY -> MaterialTheme.colorScheme.error
                            PaymentMethod.POLICY_NO_PHOTO -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = if (paymentMethod.ticketPhotoPolicy == PaymentMethod.POLICY_MANDATORY) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (!isDefault) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_payment_method),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

