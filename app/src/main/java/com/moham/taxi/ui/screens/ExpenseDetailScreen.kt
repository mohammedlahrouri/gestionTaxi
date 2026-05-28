package com.moham.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.moham.taxi.data.model.Expense
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.InlineTicketPhoto
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.navigation.AppScreens
import com.moham.taxi.ui.theme.AccentRed
import com.moham.taxi.ui.theme.DarkBackground
import com.moham.taxi.ui.theme.DarkCard
import com.moham.taxi.ui.theme.PrimaryBlue
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    navController: NavHostController,
    expenseId: Long
) {
    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val scope = rememberCoroutineScope()
    
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )
    
    var expense by remember { mutableStateOf<Expense?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        if (expenseId > 0) {
            expense = expenseViewModel.getExpenseById(expenseId)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expenses), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            expense?.let { currentExpense ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Fecha: ${com.moham.taxi.utils.DateUtils.formatDate(currentExpense.date, "dd/MM/yyyy")}", color = Color.White, fontSize = 16.sp)
                            
                            val typeLabel = if (currentExpense.type == ExpenseType.FUEL) stringResource(R.string.label_fuel) else stringResource(R.string.label_misc)
                            Text("Tipo: $typeLabel", color = Color.White, fontSize = 16.sp)
                            
                            Text("Monto: ${formatCurrency(currentExpense.amount)}", color = AccentRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            
                            if (currentExpense.type == ExpenseType.OTHER && !currentExpense.description.isNullOrBlank()) {
                                Text("Descripción: ${currentExpense.description}", color = Color.White, fontSize = 16.sp)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            InlineTicketPhoto(photoPath = currentExpense.ticketPhotoPath)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
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
                                val route = AppScreens.ExpenseForm.createRouteWithDateAndId(currentExpense.date.time, currentExpense.id)
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
            title = { Text(stringResource(R.string.delete_expense_title), color = Color.White) },
            text = { Text(stringResource(R.string.delete_expense_confirm), color = Color.White.copy(0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    expense?.let { 
                        scope.launch { 
                            expenseViewModel.delete(it) 
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
