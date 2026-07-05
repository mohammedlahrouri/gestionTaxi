package com.moham.taxi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moham.taxi.GestionTaxiApplication
import com.moham.taxi.R
import com.moham.taxi.data.model.ExpenseType
import com.moham.taxi.ui.components.formatCurrency
import com.moham.taxi.ui.theme.NewStatsBackground
import com.moham.taxi.ui.theme.NewStatsBorder
import com.moham.taxi.ui.theme.NewStatsCardBackground
import com.moham.taxi.ui.theme.NewStatsTextPrimary
import com.moham.taxi.ui.theme.NewStatsTextSecondary
import com.moham.taxi.ui.viewmodel.ExpenseViewModel
import com.moham.taxi.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(navController: NavController) {
    BackHandler {
        navController.popBackStack()
    }

    val context = LocalContext.current
    val application = context.applicationContext as GestionTaxiApplication
    val expenseViewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModel.ExpenseViewModelFactory(
            repository = application.expenseRepository
        )
    )

    val expenses by expenseViewModel.maintenanceExpenses.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.option_maintenance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NewStatsBackground,
                    titleContentColor = NewStatsTextPrimary
                )
            )
        },
        containerColor = NewStatsBackground
    ) { paddingValues ->
        if (expenses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.msg_no_expenses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NewStatsTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    if (expense.type == ExpenseType.MAINTENANCE) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NewStatsCardBackground),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NewStatsBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DateUtils.formatDate(expense.date, "dd/MM/yyyy"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NewStatsTextSecondary
                                    )
                                    Text(
                                        text = formatCurrency(expense.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NewStatsTextPrimary
                                    )
                                }

                                val kmText = expense.maintenanceKilometers?.let { "$it km" } ?: "-"
                                val detailsText = expense.maintenanceDetails?.ifBlank { "-" } ?: "-"

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_kilometers),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NewStatsTextSecondary
                                    )
                                    Text(
                                        text = kmText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NewStatsTextPrimary
                                    )
                                }

                                Text(
                                    text = stringResource(R.string.label_maintenance_details),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NewStatsTextSecondary,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    text = detailsText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NewStatsTextPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
