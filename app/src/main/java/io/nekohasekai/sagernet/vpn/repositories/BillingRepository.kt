package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("StaticFieldLeak")
object BillingRepository {
    private lateinit var purchasesUpdatedListener: PurchasesUpdatedListener
    private lateinit var billingClient: BillingClient

    fun initialize(context: Context)
    {
        purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.
            }

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

    }

    fun startConnection()
    {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode ==  BillingClient.BillingResponseCode.OK) {
                    AppRepository.debugLog("BillingClient is ready")
                    // The BillingClient is ready. You can query purchases here.
                }
            }
            override fun onBillingServiceDisconnected() {
                AppRepository.debugLog("BillingClient is NOT ready!")
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    suspend fun processPurchases() {
        val productList = ArrayList<String>()
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("product_id_example")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
                .toString()
        )
        val params = QueryProductDetailsParams.newBuilder()
        params.setProductList(productList)

        // leverage queryProductDetails Kotlin extension function
        val productDetailsResult = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
        }

        // Process the result.
    }


}
