package com.vaultaire.android.data.pricing.model

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Google Play Billing entitlements for Pro features.
 * 
 * Queries the Play Store for active subscriptions and caches entitlements.
 * Provides synchronous hasEntitlement() checks for UI gating.
 */
class EntitlementManager(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // Entitlement cache
    private val _entitlements = MutableStateFlow<Set<Entitlement>>(emptySet())
    val entitlements = _entitlements.asStateFlow()

    private val _isBillingReady = MutableStateFlow<Boolean>(false)
    val isBillingReady = _isBillingReady.asStateFlow()

    private val purchaseUpdatesChannel = Channel<List<Purchase>>()

    init {
        startConnection()
        observePurchases()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingReady.value = false
                // Retry connection after delay
                scope.launch {
                    delay(5000)
                    startConnection()
                }
            }
        })
    }

    private fun queryPurchases() {
        scope.launch {
            val result = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    updateEntitlementsFromPurchases(purchasesList)
                }
            }
        }
    }

    private fun updateEntitlementsFromPurchases(purchases: List<Purchase>) {
        val activeEntitlements = mutableSetOf<Entitlement>()
        
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Map product IDs to entitlements
                val productId = purchase.products.firstOrNull()
                when (productId) {
                    "pro_monthly", "pro_yearly", "pro_lifetime" -> {
                        activeEntitlements.addAll(Entitlement.values())
                    }
                    // Add specific product ID mappings if needed
                }
            }
        }

        _entitlements.value = activeEntitlements
    }

    private fun observePurchases() {
        scope.launch {
            for (purchases in purchaseUpdatesChannel) {
                updateEntitlementsFromPurchases(purchases)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchaseUpdatesChannel.trySend(purchases)
        }
    }

    /**
     * Checks if the user has a specific entitlement.
     * Returns true for Free users for entitlements that don't require Pro.
     * 
     * For sharing: Pro required to CREATE, Free can OPEN.
     * This method is for CREATE gating only.
     */
    fun hasEntitlement(entitlement: Entitlement): Boolean {
        return _entitlements.value.contains(entitlement)
    }

    /**
     * Launches the billing flow for a Pro subscription.
     * Call from an Activity context.
     * @param productDetails The ProductDetails queried from Play Store
     */
    fun launchBillingFlow(activity: android.app.Activity, productDetails: com.android.billingclient.api.ProductDetails) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            ))
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    /**
     * Refreshes entitlements from Play Store.
     * Call on app resume or after purchase.
     */
    fun refreshEntitlements() {
        if (_isBillingReady.value) {
            queryPurchases()
        }
    }

    fun shutdown() {
        billingClient.endConnection()
        purchaseUpdatesChannel.close()
    }
}
