package io.nekohasekai.sagernet.vpn

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.withTimeoutOrNull
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.databinding.ForceUpdateDialogBinding
import io.nekohasekai.sagernet.databinding.LayoutMainBinding
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.group.RawUpdater
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.vpn.components.ForceUpdateDialog
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import io.nekohasekai.sagernet.vpn.repositories.SocialAuthRepository
import io.nekohasekai.sagernet.vpn.serverlist.ListItem
import io.nekohasekai.sagernet.vpn.serverlist.ListSubItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

class SplashActivity : BaseThemeActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tryAgainButton: AppCompatButton
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var checkUpdate: AlertDialog
    fun ProxyGroup.init() {
//        DataStore.groupName = name ?: AppRepository.appName
        DataStore.groupName = "Ungrouped"
        DataStore.groupType = 1
        DataStore.groupOrder = order
        DataStore.groupIsSelector = isSelector
        DataStore.frontProxy = frontProxy
        DataStore.landingProxy = landingProxy
        DataStore.frontProxyTmp = if (frontProxy >= 0) 3 else 0
        DataStore.landingProxyTmp = if (landingProxy >= 0) 3 else 0

        val subscription = subscription ?: SubscriptionBean().applyDefaultValues()
        DataStore.subscriptionLink = AppRepository.getSubscriptionLink()
//        DataStore.subscriptionLink = subscription.link
        DataStore.subscriptionForceResolve = subscription.forceResolve
        DataStore.subscriptionDeduplication = subscription.deduplication
        DataStore.subscriptionUpdateWhenConnectedOnly = subscription.updateWhenConnectedOnly
        DataStore.subscriptionUserAgent = subscription.customUserAgent
        DataStore.subscriptionAutoUpdate = subscription.autoUpdate
        DataStore.subscriptionAutoUpdateDelay = subscription.autoUpdateDelay
    }

    fun ProxyGroup.serialize() {
        name = DataStore.groupName.takeIf { it.isNotBlank() } ?: "My group"
        type = DataStore.groupType
        order = DataStore.groupOrder
        isSelector = DataStore.groupIsSelector

        frontProxy = if (DataStore.frontProxyTmp == 3) DataStore.frontProxy else -1
        landingProxy = if (DataStore.landingProxyTmp == 3) DataStore.landingProxy else -1

        val isSubscription = type == GroupType.SUBSCRIPTION
        if (isSubscription) {
            subscription = (subscription ?: SubscriptionBean().applyDefaultValues()).apply {
                link = DataStore.subscriptionLink
                forceResolve = DataStore.subscriptionForceResolve
                deduplication = DataStore.subscriptionDeduplication
                updateWhenConnectedOnly = DataStore.subscriptionUpdateWhenConnectedOnly
                customUserAgent = DataStore.subscriptionUserAgent
                autoUpdate = DataStore.subscriptionAutoUpdate
                autoUpdateDelay = DataStore.subscriptionAutoUpdateDelay
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize the ProgressBar and Try Again Button
        progressBar = findViewById(R.id.progressBar)
        tryAgainButton = findViewById(R.id.btn_Try_Again)

        // Set the Try Again button click listener
        tryAgainButton.setOnClickListener {
            retryLoading()
        }

        // Start the loading process
        startLoading()

        loadFcmToken()
        //Show AdMob Interstitial
//        loadInterstitialAd()

        AppRepository.sharedPreferences = getSharedPreferences("CountdownPrefs", Context.MODE_PRIVATE)

        GlobalScope.launch(Dispatchers.Main) {
            // Check Ad Consent
            getSettings()
            checkForUpdate()

            if (!AppRepository.appShouldForceUpdate) {
                getServers()
            }
//              startWelcomeActivity()
//            showInterstitialAd()
        }

//        runOnDefaultDispatcher {
//            val entity = SagerDatabase.groupDao.getById(1)
//            ProxyGroup().init()
//            var subscription = ProxyGroup().apply { serialize() }
//            if (entity == null) {
//                GroupManager.createGroup(subscription)
//            }
//            GroupUpdater.startUpdate(subscription, true)
//        }


    }

    private fun checkForUpdate() {
        if (AppRepository.appShouldForceUpdate) {
            showForceUpdateDialog()
        }
    }

    private fun showForceUpdateDialog() {
        val intent = Intent(this, ForceUpdateActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppRepository.debugLog("Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            AppRepository.debugLog("FCM token: $token")
        })
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                }
                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    mInterstitialAd = null
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    mInterstitialAd = null
                }
                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                }
            }
            mInterstitialAd?.show(this)
        } else {
//            startNewActivity()
        }
    }

    private fun loadInterstitialAd() {

        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun startLoading() {
        // Show the progress bar and hide the Try Again button
        progressBar.visibility = View.VISIBLE
        tryAgainButton.visibility = View.GONE

        // Launch a coroutine to handle loading
        GlobalScope.launch(Dispatchers.Main) {
            val result = withTimeoutOrNull(15000) { // 15 seconds timeout
                try {
                    getSettings()
                    // Update progress bar to 50% after getSettings() completes successfully
                    progressBar.progress = 50

                    if (!AppRepository.appShouldForceUpdate) {
                        getServers()
                        // Update progress bar to 80% after getServers() completes successfully
                        progressBar.progress = 80
                    }
                    true // Indicate success
                } catch (e: Exception) {
                    false // Indicate failure
                }
            }

            if (result == true) {
                startWelcomeActivity() // Start next activity if loading is successful
            } else {
                showRetryOption() // Show the Try Again button if loading fails or times out
            }
        }
    }

    private fun retryLoading() {
        startLoading() // Restart the loading process when Try Again is clicked
    }

    private fun showRetryOption() {
        // Hide the progress bar and show the Try Again button
        progressBar.visibility = View.GONE
        tryAgainButton.visibility = View.VISIBLE
    }

    private fun startWelcomeActivity() {
        // Determine which activity to start based on user authentication status
        val intent = if (AuthRepository.getUserToken() == null) {
            Intent(this, WelcomeActivity::class.java)
        } else {
            Intent(this, DashboardActivity::class.java)
        }
        startActivity(intent)
        finish() // Finish the SplashActivity
    }

    private suspend fun getServers(): String {
        return withContext(Dispatchers.IO) {
            var response = AppRepository.getServersListSync()
            if(response == 200) {
                var serversString = AppRepository.getRawServersConfigAsString()
                val proxies = RawUpdater.parseRaw(serversString)
                if(!proxies.isNullOrEmpty()) {
                    import(proxies)
                }
            }
            "Finished"
        }
    }

    private suspend fun getSettings(): String {
        return withContext(Dispatchers.IO) {
            AppRepository.getSettingsSync()
            "Finished"
        }
    }

    @SuppressLint("DiscouragedApi")
    suspend fun import(proxies: List<AbstractBean>) {
        removeAllProfiles()
        val targetId = DataStore.selectedGroupForImport()
        var counter = 0
        AppRepository.allServers = mutableListOf()
        AppRepository.allServers.clear()
        AppRepository.allServersOriginal = mutableListOf()
        AppRepository.allServersOriginal.clear()
        AppRepository.setAllServer(AppRepository.allServers)
        AppRepository.debugLog(AppRepository.allServersRaw.toString())
        AppRepository.allServersRaw.entrySet().forEach { entry ->
            val serverSubItems: MutableList<ListSubItem> = mutableListOf()
            val countryCode = entry.key
            val resourceName = "ic_${countryCode}_flag"
            val countryName = AppRepository.flagNameMapper(countryCode)
            entry.value.asJsonArray.forEach { it ->
                var profile = ProfileManager.createProfile(targetId, proxies[counter])
                var serverId = it.asJsonObject.get("id").asInt
                val tagsArray = it.asJsonObject.getAsJsonArray("tags")
                val tags = Array(tagsArray.size()) { tagsArray[it].asString }
                serverSubItems.add(
                    ListSubItem(profile.id, serverId, it.asJsonObject.get("name").asString, profile.status, profile.error, profile.ping, tags = tags)
                )
                counter++;
            }
            AppRepository.allServers.add(
                ListItem(
                    countryName,
                    serverSubItems,
                    iconResId = resources.getIdentifier(resourceName, "drawable", this.packageName)
                )
            )
        }
        AppRepository.allServersOriginal = AppRepository.allServers
        AppRepository.setAllServer(AppRepository.allServers)
        AdRepository.checkAdConsent(this@SplashActivity)

        onMainDispatcher {
            DataStore.editingGroup = targetId
        }
    }

    suspend fun removeAllProfiles() {
        val groupId = DataStore.selectedGroupForImport()
        val profilesUnfiltered = SagerDatabase.proxyDao.getByGroup(groupId)
        val profiles = ConcurrentLinkedQueue(profilesUnfiltered)
        profiles.forEach { it ->
            ProfileManager.deleteProfile2(
                it.groupId, it.id
            )
        }

    }
}
