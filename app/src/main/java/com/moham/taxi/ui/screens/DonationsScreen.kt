package com.moham.taxi.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.moham.taxi.R
import com.moham.taxi.ui.theme.NewStatsBackground
import com.moham.taxi.ui.theme.NewStatsBorder
import com.moham.taxi.ui.theme.NewStatsCardBackground
import com.moham.taxi.ui.theme.NewStatsTextPrimary
import com.moham.taxi.ui.theme.NewStatsTextSecondary
import kotlinx.coroutines.launch

private data class DonationOption(
    val productId: String,
    val titleResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationsScreen(navController: NavController) {
    BackHandler {
        navController.popBackStack()
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val options = remember {
        listOf(
            DonationOption(productId = "donation_499", titleResId = R.string.donation_499_title),
            DonationOption(productId = "donation_199", titleResId = R.string.donation_199_title),
            DonationOption(productId = "donation_099", titleResId = R.string.donation_099_title)
        )
    }

    var billingClient by remember { mutableStateOf<BillingClient?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var productDetailsById by remember { mutableStateOf<Map<String, ProductDetails>>(emptyMap()) }

    val purchasesUpdatedListener = remember {
        PurchasesUpdatedListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        billingClient?.let { client ->
                            scope.launch {
                                val consumeResult = consumePurchase(client, purchase)
                                if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.donation_thanks))
                                } else {
                                    snackbarHostState.showSnackbar(context.getString(R.string.donation_failed))
                                }
                            }
                        }
                    }
                }
            } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.donation_canceled)) }
            } else {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.donation_failed)) }
            }
        }
    }

    DisposableEffect(Unit) {
        val client = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient = client

        onDispose {
            billingClient?.endConnection()
            billingClient = null
        }
    }

    LaunchedEffect(billingClient) {
        val client = billingClient ?: return@LaunchedEffect
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isReady = billingResult.responseCode == BillingClient.BillingResponseCode.OK
            }

            override fun onBillingServiceDisconnected() {
                isReady = false
            }
        })
    }

    LaunchedEffect(isReady) {
        val client = billingClient ?: return@LaunchedEffect
        if (!isReady) return@LaunchedEffect

        val products = options.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        client.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsById = productDetailsList.associateBy { it.productId }
            }
        }
    }

    fun startPurchase(option: DonationOption) {
        val client = billingClient ?: return
        if (!isReady) return
        val details = productDetailsById[option.productId] ?: return
        val act = activity ?: return

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        client.launchBillingFlow(act, flowParams)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.donations_title),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NewStatsBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.donations_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = NewStatsTextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { option ->
                    val details = productDetailsById[option.productId]
                    val price = details?.oneTimePurchaseOfferDetails?.formattedPrice ?: "—"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NewStatsCardBackground),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NewStatsBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(option.titleResId),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NewStatsTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = price,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NewStatsTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            TextButton(
                                onClick = { startPurchase(option) },
                                enabled = isReady && details != null && activity != null
                            ) {
                                Text(stringResource(R.string.donation_action))
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun consumePurchase(
    billingClient: BillingClient,
    purchase: Purchase
): BillingResult {
    val params = ConsumeParams.newBuilder()
        .setPurchaseToken(purchase.purchaseToken)
        .build()
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        billingClient.consumeAsync(params) { billingResult, _ ->
            if (cont.isActive) {
                cont.resume(billingResult) {}
            }
        }
    }
}
