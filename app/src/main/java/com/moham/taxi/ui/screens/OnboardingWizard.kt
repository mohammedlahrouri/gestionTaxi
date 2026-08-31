package com.moham.taxi.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.moham.taxi.data.model.ServicePlatform
import com.moham.taxi.ui.components.TaxiButton
import com.moham.taxi.ui.components.TaxiTextField
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.viewmodel.DriveLinkChoice
import com.moham.taxi.ui.viewmodel.OnboardingViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun StartupScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication

    LaunchedEffect(Unit) {
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        val currentVersionCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val currentFirstInstallTime = packageInfo.firstInstallTime
        val storedFirstInstallTime = application.getStoredFirstInstallTimeOrNull()
        if (storedFirstInstallTime == null) {
            application.setStoredFirstInstallTime(currentFirstInstallTime)
        } else if (storedFirstInstallTime != currentFirstInstallTime) {
            application.setHasCompletedOnboarding(false)
            application.setIsFirstRun(true)
            application.setStoredFirstInstallTime(currentFirstInstallTime)
        }

        val completedOrNull = application.getHasCompletedOnboardingOrNull()
        val completed = if (completedOrNull != null) {
            completedOrNull
        } else {
            val platforms = application.servicePlatformRepository.allServicePlatforms.first()
            val hasCustomPlatforms = platforms.any { it.name != "Directo" }
            val hasRides = application.taxiRideRepository.getTotalRideCount() > 0
            val hasData = hasCustomPlatforms || hasRides
            application.setHasCompletedOnboarding(hasData)
            hasData
        }
        application.setLastSeenVersionCode(currentVersionCode)
        val target = if (completed) AppScreens.Home.route else AppScreens.OnboardingStep1.route
        navController.navigate(target) {
            popUpTo(AppScreens.Startup.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

private enum class DataSourceChoice {
    FLEET,
    DRIVE,
    LOCAL,
    SCRATCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingScaffold(
    title: String,
    snackbarHostState: SnackbarHostState?,
    primaryText: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    secondaryText: String?,
    secondaryEnabled: Boolean,
    onSecondary: (() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = { TopAppBar(title = { Text(title) }) },
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(color = scheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (secondaryText != null && onSecondary != null) {
                        Button(
                            onClick = onSecondary,
                            enabled = secondaryEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.surfaceVariant,
                                contentColor = scheme.onSurfaceVariant
                            )
                        ) {
                            Text(secondaryText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onPrimary,
                        enabled = primaryEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary
                        )
                    ) {
                        Text(primaryText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        content = content
    )
}

@Composable
private fun DataSourceCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (selected) scheme.primaryContainer else scheme.surface
    val content = if (selected) scheme.onPrimaryContainer else scheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = scheme.surfaceVariant,
                contentColor = scheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { icon() }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = content)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.85f))
            }
        }
    }
}

private fun suggestedPlatforms(country: String): List<String> {
    val map = mapOf(
        "ES" to listOf("FreeNow", "Joinup"),
        "MX" to listOf("Uber", "DiDi"),
        "AR" to listOf("Uber", "Cabify"),
        "CO" to listOf("Cabify", "InDrive"),
        "US" to listOf("Uber", "Lyft"),
        "DE" to listOf("FreeNow", "Uber"),
        "GB" to listOf("Uber", "Bolt"),
        "FR" to listOf("Uber", "Bolt"),
        "PT" to listOf("Uber", "Bolt"),
        "IT" to listOf("FreeNow", "ItTaxi"),
        "BR" to listOf("Uber", "99"),
        "CL" to listOf("Cabify", "Uber"),
        "PE" to listOf("Cabify", "InDrive"),
        "UY" to listOf("Cabify", "Uber"),
        "EC" to listOf("Uber", "Cabify"),
        "IN" to listOf("Ola", "Uber"),
        "CA" to listOf("Uber", "Lyft"),
        "AU" to listOf("Uber", "DiDi"),
        "AE" to listOf("Careem", "Uber"),
        "ZA" to listOf("Uber", "Bolt")
    )
    return map[country]?.take(2) ?: listOf("Uber", "Bolt")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep1Screen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(application))

    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val restoreDriveFailedText = stringResource(R.string.onboarding_restore_drive_failed)
    val restoreLocalFailedText = stringResource(R.string.onboarding_restore_local_failed)
    val signInCancelledText = stringResource(R.string.onboarding_sign_in_cancelled)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                vm.refreshDriveSignedIn()
                val ok = vm.restoreFromDrive()
                if (ok) {
                    vm.completeOnboarding()
                    navController.navigate(AppScreens.Home.route) {
                        popUpTo(AppScreens.Startup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    snackbarHostState.showSnackbar(restoreDriveFailedText)
                }
            } else {
                snackbarHostState.showSnackbar(signInCancelledText)
            }
        }
    }

    val localPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = vm.restoreFromLocal(uri)
            if (ok) {
                vm.completeOnboarding()
                navController.navigate(AppScreens.Home.route) {
                    popUpTo(AppScreens.Startup.route) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                snackbarHostState.showSnackbar(restoreLocalFailedText)
            }
        }
    }

    var choice by rememberSaveable { mutableStateOf<DataSourceChoice?>(null) }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_step_title, 1, 5),
        snackbarHostState = snackbarHostState,
        primaryText = stringResource(R.string.onboarding_continue),
        primaryEnabled = choice != null && !uiState.isBusy,
        onPrimary = {
            when (choice) {
                DataSourceChoice.FLEET -> navController.navigate(AppScreens.FleetConnection.route)
                DataSourceChoice.DRIVE -> {
                    if (uiState.driveSignedIn) {
                        scope.launch {
                            val ok = vm.restoreFromDrive()
                            if (ok) {
                                vm.completeOnboarding()
                                navController.navigate(AppScreens.Home.route) {
                                    popUpTo(AppScreens.Startup.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                snackbarHostState.showSnackbar(restoreDriveFailedText)
                            }
                        }
                    } else {
                        driveSignInLauncher.launch(application.googleDriveAuthManager.getSignInClient().signInIntent)
                    }
                }
                DataSourceChoice.LOCAL -> localPickerLauncher.launch(arrayOf("*/*"))
                DataSourceChoice.SCRATCH -> navController.navigate(AppScreens.OnboardingStep2.route)
                null -> Unit
            }
        },
        secondaryText = null,
        secondaryEnabled = false,
        onSecondary = null
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.onboarding_data_source_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.onboarding_data_source_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DataSourceCard(
                    title = stringResource(R.string.onboarding_connect_fleet_title),
                    subtitle = stringResource(R.string.onboarding_connect_fleet_subtitle),
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    selected = choice == DataSourceChoice.FLEET,
                    onClick = { choice = DataSourceChoice.FLEET }
                )
                DataSourceCard(
                    title = stringResource(R.string.onboarding_restore_drive_title),
                    subtitle = stringResource(R.string.onboarding_restore_drive_subtitle),
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                    selected = choice == DataSourceChoice.DRIVE,
                    onClick = { choice = DataSourceChoice.DRIVE }
                )
                DataSourceCard(
                    title = stringResource(R.string.onboarding_import_local_title),
                    subtitle = stringResource(R.string.onboarding_import_local_subtitle),
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                    selected = choice == DataSourceChoice.LOCAL,
                    onClick = { choice = DataSourceChoice.LOCAL }
                )
                DataSourceCard(
                    title = stringResource(R.string.onboarding_setup_scratch_title),
                    subtitle = stringResource(R.string.onboarding_setup_scratch_subtitle),
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    selected = choice == DataSourceChoice.SCRATCH,
                    onClick = { choice = DataSourceChoice.SCRATCH }
                )
            }

            if (uiState.isBusy) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { },
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(stringResource(R.string.backup_import_progress_message))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep2Screen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(application))

    val platforms by vm.servicePlatforms.collectAsState()
    val form by vm.platformForm.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val addedPlatformsCount = platforms.count { it.name != "Directo" }
    val primaryLabel = if (addedPlatformsCount == 0) stringResource(R.string.onboarding_skip) else stringResource(R.string.onboarding_next)
    val country = remember { Locale.getDefault().country.uppercase(Locale.ROOT) }
    val suggestions = remember(country) { suggestedPlatforms(country) }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_step_title, 2, 5),
        snackbarHostState = snackbarHostState,
        primaryText = primaryLabel,
        primaryEnabled = !uiState.isBusy,
        onPrimary = { navController.navigate(AppScreens.OnboardingStep3.route) },
        secondaryText = stringResource(R.string.onboarding_add),
        secondaryEnabled = !uiState.isBusy,
        onSecondary = { showAddDialog = true }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.onboarding_smart_platforms_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.onboarding_smart_platforms_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { suggestion ->
                        AssistChip(
                            onClick = {
                                vm.updatePlatformName(suggestion)
                                showAddDialog = true
                            },
                            label = { Text(suggestion) }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val percentFormatter = remember {
                        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                            minimumFractionDigits = 0
                            maximumFractionDigits = 2
                        }
                    }
                    val sorted = platforms.sortedWith(compareBy<ServicePlatform> { if (it.name == "Directo") 0 else 1 }.thenBy { it.name })
                    sorted.forEach { platform ->
                        val displayName = if (platform.name == "Directo") {
                            stringResource(R.string.platform_direct_no_platform)
                        } else {
                            platform.name
                        }
                        val commissionPercent = platform.commissionPercentage
                        val commissionVat = platform.commissionVat
                        val subtitle = when {
                            commissionPercent != null && commissionVat != null -> {
                                stringResource(
                                    R.string.platform_commission_format,
                                    percentFormatter.format(commissionPercent),
                                    percentFormatter.format(commissionVat)
                                )
                            }
                            commissionPercent != null -> {
                                stringResource(
                                    R.string.platform_commission_only_format,
                                    percentFormatter.format(commissionPercent)
                                )
                            }
                            commissionVat != null -> {
                                stringResource(
                                    R.string.platform_vat_only_format,
                                    percentFormatter.format(commissionVat)
                                )
                            }
                            else -> ""
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isBusy) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { },
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                ) {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (showAddDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { if (!uiState.isBusy) showAddDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.onboarding_platform_dialog_title)) },
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = { if (!uiState.isBusy) showAddDialog = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                            actions = {
                                TextButton(
                                    enabled = !uiState.isBusy,
                                    onClick = {
                                        scope.launch {
                                            val ok = vm.submitPlatform()
                                            if (ok) showAddDialog = false
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
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TaxiTextField(
                            value = form.name,
                            onValueChange = vm::updatePlatformName,
                            label = stringResource(R.string.onboarding_platform_name_label),
                            isError = form.nameError,
                            errorMessage = if (form.nameError) stringResource(R.string.onboarding_platform_name_error) else ""
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.onboarding_platform_apply_commission), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = form.applyCommission, onCheckedChange = vm::setApplyCommission)
                        }

                        Text(stringResource(R.string.label_service_mode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = form.serviceMode == com.moham.taxi.data.model.ServiceMode.BOTH,
                                onClick = { vm.setServiceMode(com.moham.taxi.data.model.ServiceMode.BOTH) }
                            )
                            Text(stringResource(R.string.service_mode_both))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = form.serviceMode == com.moham.taxi.data.model.ServiceMode.METER_ONLY,
                                onClick = { vm.setServiceMode(com.moham.taxi.data.model.ServiceMode.METER_ONLY) }
                            )
                            Text(stringResource(R.string.service_mode_meter_only))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(
                                selected = form.serviceMode == com.moham.taxi.data.model.ServiceMode.FIXED_ONLY,
                                onClick = { vm.setServiceMode(com.moham.taxi.data.model.ServiceMode.FIXED_ONLY) }
                            )
                            Text(stringResource(R.string.service_mode_fixed_only))
                        }

                        if (form.applyCommission) {
                            TaxiTextField(
                                value = form.commissionPercent,
                                onValueChange = vm::updateCommissionPercent,
                                label = stringResource(R.string.onboarding_platform_commission_percent_label),
                                isError = form.commissionError,
                                errorMessage = if (form.commissionError) stringResource(R.string.onboarding_platform_commission_error) else ""
                            )
                            TaxiTextField(
                                value = form.taxPercent,
                                onValueChange = vm::updateTaxPercent,
                                label = stringResource(R.string.onboarding_platform_tax_percent_label)
                            )
                            Text(
                                text = stringResource(R.string.onboarding_platform_tax_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.onboarding_platform_us_math), style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = form.useUsMath, onCheckedChange = vm::setUseUsMath)
                            }
                        }

                        Text(stringResource(R.string.onboarding_payment_methods_title), style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            form.paymentMethods.forEach { (method, checked) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { vm.togglePaymentMethod(method) })
                                    Text(method)
                                }
                            }
                        }

                        Text(stringResource(R.string.onboarding_other_title), style = MaterialTheme.typography.titleSmall)
                        if (form.otherPaymentMethods.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(form.otherPaymentMethods) { method ->
                                    InputChip(
                                        selected = true,
                                        onClick = { vm.removeOtherPaymentMethod(method) },
                                        label = { Text(method) },
                                        trailingIcon = { Icon(imageVector = Icons.Default.Close, contentDescription = null) }
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                TaxiTextField(
                                    value = form.otherPaymentMethodInput,
                                    onValueChange = vm::updateOtherPaymentMethodInput,
                                    label = stringResource(R.string.onboarding_other_label)
                                )
                            }
                            Button(
                                onClick = vm::addOtherPaymentMethodFromInput,
                                enabled = form.otherPaymentMethodInput.trim().isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep3Screen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(application))
    val billing by vm.billingForm.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_step_title, 3, 5),
        snackbarHostState = snackbarHostState,
        primaryText = stringResource(R.string.onboarding_next),
        primaryEnabled = true,
        onPrimary = {
            scope.launch {
                vm.saveBillingData()
                navController.navigate(AppScreens.OnboardingStep4.route)
            }
        },
        secondaryText = stringResource(R.string.onboarding_skip),
        secondaryEnabled = true,
        onSecondary = { navController.navigate(AppScreens.OnboardingStep4.route) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.onboarding_billing_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.onboarding_billing_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TaxiTextField(value = billing.name, onValueChange = vm::updateBillingName, label = stringResource(R.string.onboarding_billing_name_label))
                    TaxiTextField(value = billing.nif, onValueChange = vm::updateBillingNif, label = stringResource(R.string.onboarding_billing_nif_label))
                    TaxiTextField(value = billing.license, onValueChange = vm::updateBillingLicense, label = stringResource(R.string.onboarding_billing_license_label))
                    TaxiTextField(value = billing.street, onValueChange = vm::updateBillingStreet, label = stringResource(R.string.onboarding_billing_street_label))
                    TaxiTextField(value = billing.city, onValueChange = vm::updateBillingCity, label = stringResource(R.string.onboarding_billing_city_label))
                    TaxiTextField(value = billing.postalCode, onValueChange = vm::updateBillingPostalCode, label = stringResource(R.string.onboarding_billing_postal_code_label))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep4Screen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(application))

    val currentCity by vm.currentCity.collectAsState()
    val cityDraft by vm.cityDraft.collectAsState()
    val scope = rememberCoroutineScope()

    val showInfo = !cityDraft.trim().equals("Madrid", ignoreCase = true)

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_step_title, 4, 5),
        snackbarHostState = null,
        primaryText = stringResource(R.string.onboarding_next),
        primaryEnabled = true,
        onPrimary = {
            scope.launch {
                vm.applyCityIfChanged()
                navController.navigate(AppScreens.OnboardingStep5.route)
            }
        },
        secondaryText = null,
        secondaryEnabled = false,
        onSecondary = null
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.onboarding_rates_city_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.onboarding_current_city_format, currentCity), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TaxiTextField(
                        value = cityDraft,
                        onValueChange = vm::updateCityDraft,
                        label = stringResource(R.string.onboarding_city_label)
                    )
                }
            }

            if (showInfo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_rates_info),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingStep5Screen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(application))

    val uiState by vm.uiState.collectAsState()
    val driveChoice by vm.driveLinkChoice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val driveLinkedText = stringResource(R.string.onboarding_drive_linked)
    val signInCancelledText = stringResource(R.string.onboarding_sign_in_cancelled)
    val driveConnectedText = stringResource(R.string.onboarding_drive_connected)
    val driveConnectText = stringResource(R.string.onboarding_drive_connect)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val driveSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                vm.refreshDriveSignedIn()
                vm.enableOnlineBackup()
                snackbarHostState.showSnackbar(driveLinkedText)
            } else {
                snackbarHostState.showSnackbar(signInCancelledText)
            }
        }
    }

    OnboardingScaffold(
        title = stringResource(R.string.onboarding_welcome_step_title, 5, 5),
        snackbarHostState = snackbarHostState,
        primaryText = stringResource(R.string.onboarding_finish),
        primaryEnabled = !uiState.isBusy,
        onPrimary = {
            scope.launch {
                vm.completeOnboarding()
                navController.navigate(AppScreens.Home.route) {
                    popUpTo(AppScreens.OnboardingStep1.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
        secondaryText = null,
        secondaryEnabled = false,
        onSecondary = null
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.onboarding_completion_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.onboarding_drive_link_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = driveChoice == DriveLinkChoice.NOW,
                                onClick = { vm.setDriveLinkChoice(DriveLinkChoice.NOW) }
                            )
                            Text(stringResource(R.string.onboarding_drive_connect_now))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = driveChoice == DriveLinkChoice.LATER,
                                onClick = { vm.setDriveLinkChoice(DriveLinkChoice.LATER) }
                            )
                            Text(stringResource(R.string.onboarding_drive_later))
                        }
                    }
                }

                if (driveChoice == DriveLinkChoice.NOW) {
                    Button(
                        onClick = { driveSignInLauncher.launch(application.googleDriveAuthManager.getSignInClient().signInIntent) },
                        enabled = !uiState.driveSignedIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(if (uiState.driveSignedIn) driveConnectedText else driveConnectText, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (uiState.isBusy) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { },
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                ) {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
