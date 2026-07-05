package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraModulesScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()

    val dailyChallengeEnabled by application.isDailyChallengeEnabled().collectAsState(initial = false)
    val originDestinationEnabled by application.isRideOriginDestinationEnabled().collectAsState(initial = true)
    val tipsEnabled by application.isTipsEnabled().collectAsState(initial = false)
    val ticketPhotosEnabled by application.isTicketPhotosEnabled().collectAsState(initial = false)
    val oldVersionEnabled by application.isOldVersionEnabled().collectAsState(initial = false)
    val modernThemeEnabled by application.isModernThemeEnabled().collectAsState(initial = false)
    
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_active_options_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fotos tickets
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.module_photo_tickets), fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = ticketPhotosEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                showPrivacyDialog = true
                            } else {
                                scope.launch { application.saveTicketPhotosEnabled(false) }
                            }
                        }
                    )
                }
            }

            // Daily Challenge
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.daily_challenge), fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.settings_daily_challenge_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dailyChallengeEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { application.saveDailyChallengeEnabled(isChecked) }
                        }
                    )
                }
            }

            // Origin / Destination
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_origin_destination_title), fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.settings_origin_destination_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = originDestinationEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { application.saveRideOriginDestinationEnabled(isChecked) }
                        }
                    )
                }
            }

            // Tips
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.tips), fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.settings_tips_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = tipsEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { application.saveTipsEnabled(isChecked) }
                        }
                    )
                }
            }

            // Diseño Antiguo
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_old_version_title), fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.settings_old_version_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = oldVersionEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { application.saveOldVersionEnabled(isChecked) }
                        }
                    )
                }
            }

            // Diseño Moderno
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.settings_modern_theme_title), fontWeight = FontWeight.Medium)
                        Text(
                            text = stringResource(R.string.settings_modern_theme_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = modernThemeEnabled,
                        onCheckedChange = { isChecked ->
                            scope.launch { application.saveModernThemeEnabled(isChecked) }
                        }
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Proximamente modules
            // Gps automático destino
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.module_gps_dest), fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = false,
                        onCheckedChange = null,
                        enabled = false
                    )
                }
            }
            
            // KM inicio y Final
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.module_km_start_end), fontWeight = FontWeight.Medium)
                    }
                    Switch(
                        checked = false,
                        onCheckedChange = null,
                        enabled = false
                    )
                }
            }
        }
        
        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text(stringResource(R.string.photo_tickets_privacy_title)) },
                text = { Text(stringResource(R.string.photo_tickets_privacy_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showPrivacyDialog = false
                        scope.launch { application.saveTicketPhotosEnabled(true) }
                    }) {
                        Text(stringResource(R.string.photo_tickets_privacy_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPrivacyDialog = false
                    }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
