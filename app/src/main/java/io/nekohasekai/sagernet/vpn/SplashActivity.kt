package io.nekohasekai.sagernet.vpn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.group.RawUpdater
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.vpn.ads.GoogleMobileAdsConsentManager
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.serverlist.ListItem
import io.nekohasekai.sagernet.vpn.serverlist.ListSubItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : ThemedActivity() {
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var secondsRemaining: Long = 0L

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

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        AppRepository.sharedPreferences = getSharedPreferences("CountdownPrefs", Context.MODE_PRIVATE)

        GlobalScope.launch(Dispatchers.Main) {
            getServers()
            startNewActivity()
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

    private fun startNewActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
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

    @SuppressLint("DiscouragedApi")
    suspend fun import(proxies: List<AbstractBean>) {
        removeAllProfiles()
        val targetId = DataStore.selectedGroupForImport()
        var counter = 0
        AppRepository.allServers = mutableListOf()
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

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}

        // Load an ad.
        (application as MyApplication).loadAd(this)
    }

    /** Start the MainActivity. */
    fun startMainActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
    }
}
