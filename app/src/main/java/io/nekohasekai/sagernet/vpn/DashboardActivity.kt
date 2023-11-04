package io.nekohasekai.sagernet.vpn

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.navigation.NavigationView
import com.google.android.ump.ConsentInformation
import com.google.gson.Gson
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.bg.proto.UrlTest
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.databinding.ActivityDashboardBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.plugin.PluginManager
import io.nekohasekai.sagernet.ui.ConfigurationFragment
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.ads.GoogleMobileAdsConsentManager
import io.nekohasekai.sagernet.vpn.nav.MenuFragment
import io.nekohasekai.sagernet.vpn.repositories.AdRepository
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.serverlist.ListItem
import io.nekohasekai.sagernet.vpn.serverlist.ListSubItem
import io.nekohasekai.sagernet.vpn.serverlist.MyFragment
import io.nekohasekai.sagernet.vpn.utils.InternetConnectionChecker
import io.nekohasekai.sagernet.vpn.utils.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
const val COUNTER_TIME = 10L
const val GAME_OVER_REWARD = 1
const val TAG = "DashboardActivity"
class DashboardActivity : ThemedActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener {

    lateinit var binding: ActivityDashboardBinding
    private lateinit var PowerIcon: LottieAnimationView
    private lateinit var ivAll: ImageView
    private lateinit var ivMtn: ImageView
    private lateinit var ivMci: ImageView
    private lateinit var stateTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var appTitle: TextView
    private lateinit var addTimeTextView: TextView
    private var timerRunning = false
    private var timeRemainingMillis: Long = 0
    private var ivAllClicked = true // Set IVall as clicked by default
    private var ivMtnClicked = false // Add a variable to track IVMTN click state
    private var ivMciClicked = false // Add a variable to track IVMCI click state
    private lateinit var checkPingDialog: AlertDialog
    private var bestServer: ListItem? = null
    private var countDownTimer: CountDownTimer? = null
    lateinit var mAdView : AdView
//    private var rewardedAd: RewardedAd? = null
    private lateinit var internetChecker: InternetConnectionChecker
    private lateinit var consentInformation: ConsentInformation

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private var coinCount: Int = 0
    private var countdownTimer: CountDownTimer? = null
    private var gameOver = false
    private var gamePaused = false
    private var isLoading = false
    private var rewardedAd: RewardedAd? = null
    private var timeRemaining: Long = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // EU Consent
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(this)
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Log.d(TAG, "${error.errorCode}: ${error.message}")
            }

//            startGame()

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }

            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu()
            }
        }

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }


        AdRepository.internetChecker = InternetConnectionChecker(this)

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navyBlue)

//        loadRewardedAd()
        initializeMobileAdsSdk()

        //load BannerAd and RewardedAd
//        loadBannerAd()
//        AdRepository.loadRewardedAd(this)

        AppRepository.sharedPreferences = getSharedPreferences("CountdownPrefs", Context.MODE_PRIVATE)

        val telegramIcon = findViewById<ImageView>(R.id.ivTelegramIcon)
        val connection = SagerConnection(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND, true)

        val clPremium = findViewById<ConstraintLayout>(R.id.clPremium)
        clPremium.setOnClickListener {navigateToPremiumActivity()}

        // Set an OnClickListener to MainActivity
        telegramIcon.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Start the MainActivity
                val intent = Intent(this@DashboardActivity, MainActivity::class.java)
                startActivity(intent)
            }
        })

        // Initialize the fragment container
        val fragmentContainer = findViewById<View>(R.id.flFragmentContainer)

        val pingBtn = findViewById<ConstraintLayout>(R.id.clIconPing)
        pingBtn.setOnClickListener {
            urlTest()
            showNotConnectedState()
            stopTimer()
        }

        // Find the NavMenuIcon ImageView and set an OnClickListener
        val navMenuIcon = findViewById<ImageView>(R.id.ivNavMenuIcon)
        navMenuIcon.setOnClickListener {
            // Create an instance of the NavMenuFragment
            val navMenuFragment = MenuFragment()

            // Get the FragmentManager and start a transaction
            val fragmentManager: FragmentManager = supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()

            // Replace the entire activity content with the NavMenuFragment
            transaction.replace(android.R.id.content, navMenuFragment)

            // Commit the transaction
            transaction.commit()
        }

        PowerIcon = findViewById(R.id.laPulseButton)
        ivAll = findViewById(R.id.ivAll)
        ivMtn = findViewById(R.id.ivMtn)
        ivMci = findViewById(R.id.ivMci)
        stateTextView = findViewById(R.id.tvPowerState)
        timerTextView = findViewById(R.id.tvTimer)
        appTitle = findViewById(R.id.tvApplicationName)
        addTimeTextView = findViewById(R.id.tvAddTime)

        // Check if returning from a fragment
        if (savedInstanceState != null) {
            // Handle the initial state
            val initialState = savedInstanceState.getString("currentState", "Connect")
            stateTextView.text = initialState

            // Handle the fragment visibility
            val isFragmentVisible = savedInstanceState.getBoolean("isFragmentVisible", true)
            if (!isFragmentVisible) {
                fragmentContainer.visibility = View.INVISIBLE
            }

            // Handle IVall, IVMTN, and IVMCI click states
            ivAllClicked = savedInstanceState.getBoolean("ivAllClicked", true)
            ivMtnClicked = savedInstanceState.getBoolean("ivMtnClicked", false)
            ivMciClicked = savedInstanceState.getBoolean("ivMciClicked", false)

            updateIVAllIcon()
            updateIVMtnIcon()
            updateIVMciIcon()
        }

        // Ensure IVall is selected and fragmentContainer is visible when the activity starts
        fragmentContainer.visibility = if (ivAllClicked) View.VISIBLE else View.INVISIBLE
        if (ivAllClicked) {
            val fragment = MyFragment()
            val bundle = Bundle()
            bundle.putString("iconClicked", "IVAll") // Pass the clicked icon value to the fragment
            fragment.arguments = bundle
            val fragmentManager: FragmentManager = supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.flFragmentContainer, fragment)
            transaction.commit()
        }

        PowerIcon.setOnClickListener {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                // Internet is connected, proceed with your code
                if (DataStore.serviceState.canStop) SagerNet.stopService() else connect.launch(null)
            } else {
                // Internet is not connected, show a toast
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            }
        }

        connection.connect(this, this)
        DataStore.configurationStore.registerChangeListener(this)
//        GroupManager.userInterface = GroupInterfaceAdapter(this)

        // Set an OnClickListener for IVall
        ivAll.setOnClickListener {
            ivAllClicked = !ivAllClicked // Toggle the IVall click state
            updateIVAllIcon() // Update the IVall icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivAllClicked) View.VISIBLE else View.INVISIBLE
            if (ivAllClicked) {
                AppRepository.filterServersByTag("all")
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVAll") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVMTN and IVMCI click states
            ivMtnClicked = false
            ivMciClicked = false
            updateIVMtnIcon() // Update the IVMTN icon
            updateIVMciIcon() // Update the IVMCI icon
        }

        // Set an OnClickListener for IVMTN
        ivMtn.setOnClickListener {
            ivMtnClicked = !ivMtnClicked // Toggle the IVMTN click state
            updateIVMtnIcon() // Update the IVMTN icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivMtnClicked) View.VISIBLE else View.INVISIBLE
            if (ivMtnClicked) {
                AppRepository.filterServersByTag("mtn")
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMTN") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVall and IVMCI click states
            ivAllClicked = false
            ivMciClicked = false
            updateIVAllIcon() // Update the IVall icon
            updateIVMciIcon() // Update the IVMCI icon
        }

        // Set an OnClickListener for IVMCI
        ivMci.setOnClickListener {
            ivMciClicked = !ivMciClicked // Toggle the IVMCI click state
            updateIVMciIcon() // Update the IVMCI icon
            // Show/hide the MyFragment based on the click state
            fragmentContainer.visibility = if (ivMciClicked) View.VISIBLE else View.INVISIBLE
            if (ivMciClicked) {
                AppRepository.filterServersByTag("mci")
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMCI") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.flFragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVall and IVMTN click states
            ivAllClicked = false
            ivMtnClicked = false
            updateIVAllIcon() // Update the IVall icon
            updateIVMtnIcon() // Update the IVMTN icon
        }
    }

    private fun loadBannerAd() {
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun navigateToPremiumActivity() {
        val intent = Intent(this, PremiumActivity::class.java)
        startActivity(intent)
    }

    private fun updateIVAllIcon() {
        ivAll.setImageResource(if (ivAllClicked) R.drawable.ic_all_colorfull else R.drawable.ic_all_gray)
    }

    private fun updateIVMtnIcon() {
        ivMtn.setImageResource(if (ivMtnClicked) R.drawable.ic_mtn_irancell_colorfull else R.drawable.ic_mtn_irancell_gray)
    }

    private fun updateIVMciIcon() {
        ivMci.setImageResource(if (ivMciClicked) R.drawable.ic_mci_hamrahe_aval_colorfull else R.drawable.ic_mci_hamrahe_aval_gray)
    }

    private fun add30MinutesToTimer() {
        if(!AppRepository.isConnected) {
            var remainTime = AppRepository.sharedPreferences.getLong("remainingTime", 0)
            timeRemainingMillis = remainTime + 1800000
            AppRepository.sharedPreferences.edit().putLong("remainingTime", timeRemainingMillis).apply()
        }
        startTimer()
    }

    private fun stopTimer() {
        timerRunning = false
        countDownTimer?.cancel()
    }

    private fun startTimer() {
        var initialTimeMillis = AppRepository.sharedPreferences.getLong("remainingTime", 0)
        if(initialTimeMillis.toInt() === 0) {
            initialTimeMillis = 1800000;
        }

        countDownTimer = object : CountDownTimer(initialTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                AppRepository.sharedPreferences.edit().putLong("remainingTime", timeRemainingMillis).apply()
                updateTimerText(timeRemainingMillis)
            }

            override fun onFinish() {
                // Stop the service if isConnected is true and timer is 0
                if (timerTextView.text == "00:00") {
                    AppRepository.sharedPreferences.edit().remove("remainingTime").apply()
                    stopService()
                }
            }
        }
        if(!timerRunning) {
            timerRunning = true
            countDownTimer?.start()
        }
    }

    private fun updateTimerText(remainedTime: Long) {
        var initialTimeMillis = AppRepository.sharedPreferences.getLong("remainingTime", 0)
        val minutes = (initialTimeMillis / 1000) / 60
        val seconds = (initialTimeMillis / 1000) % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = formattedTime
    }


    private fun showConnectingState() {
        timerTextView.visibility = View.INVISIBLE
        addTimeTextView.visibility = View.INVISIBLE
        PowerIcon.setAnimation(R.raw.pulse_button_yellow)
        PowerIcon.playAnimation()
        stateTextView.text = "Connecting..."
        PowerIcon.isEnabled = false
    }

    private fun showConnectedState() {
        timerTextView.visibility = View.VISIBLE
        addTimeTextView.visibility = View.INVISIBLE
        PowerIcon.setAnimation(R.raw.pulse_button_green)
        PowerIcon.playAnimation()
        stateTextView.text = "Connected"
        PowerIcon.isEnabled = true
    }

    private fun showNotConnectedState() {
        timerTextView.visibility = View.INVISIBLE
        addTimeTextView.visibility = View.VISIBLE
        PowerIcon.setAnimation(R.raw.pulse_button_gray)
        PowerIcon.playAnimation()
        stateTextView.text = "Connect"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentState", stateTextView.text.toString())
        outState.putBoolean("isFragmentVisible", findViewById<View>(R.id.flFragmentContainer).visibility == View.VISIBLE)
        outState.putBoolean("ivAllClicked", ivAllClicked)
        outState.putBoolean("ivMtnClicked", ivMtnClicked)
        outState.putBoolean("ivMciClicked", ivMciClicked)
    }

    private val connect = registerForActivityResult(VpnRequestActivity.StartService()) {
        if (it) println("HAMED_LOG_" + R.string.vpn_permission_denied)
    }

    override fun onResume() {
        super.onResume()
        if(DataStore.serviceState.connected) {
            showConnectedState()
            AdRepository.showRewardedAd(this)
        } else {
            showNotConnectedState()
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, msg, true)
    }

    override fun onServiceConnected(service: ISagerNetService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.SERVICE_MODE -> onBinderDied()
            Key.PROXY_APPS, Key.BYPASS_MODE, Key.INDIVIDUAL -> {
                if (DataStore.serviceState.canStop) {
                    snackbar(getString(R.string.need_reload)).setAction(R.string.apply) {
                        SagerNet.reloadService()
                    }.show()
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//        if (item.isChecked) binding.drawerLayout.closeDrawers() else {
//            return displayFragmentWithId(item.itemId)
//        }
        return true
    }

    private fun changeState(
        state: BaseService.State,
        msg: String? = null,
        animate: Boolean = false,
    ) {
        DataStore.serviceState = state
        if (state.toString() === "Connected") {
            println("HAMED_LOG_CONNECT")
            showRewardedVideo()
//            if(rewardedAd === null) {
//                AdRepository.loadRewardedAd(this)
//            }
//            AdRepository.showRewardedAd(this)
            add30MinutesToTimer()
            AppRepository.isConnected = true
            val profile = SagerDatabase.proxyDao.getById(DataStore.selectedProxy)
            val tvSelectedServer = findViewById<TextView>(R.id.tvSelectedServer)
            tvSelectedServer.text = profile?.displayName()
            showConnectedState()
        } else if(state.toString() === "Connecting") {
            showConnectingState()
        } else if(state.toString() === "Stopped") {
            AppRepository.isConnected = false
            showNotConnectedState()
            stopTimer()
        }
//        binding.fab.changeState(state, DataStore.serviceState, animate)
//        binding.stats.changeState(state)
//        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()
    }

    fun stopService() {
        if (DataStore.serviceState.started) SagerNet.stopService()
    }

    override fun onStop() {
        super.onStop()
        val gson = Gson()
        val allServersInJson = gson.toJson(AppRepository.allServers)
        AppRepository.sharedPreferences.edit().putString("allServers", allServersInJson).apply()
    }

    @SuppressLint("DiscouragedApi")
    private fun urlTest() {
        val customDialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null)
        val dialogServerName = customDialogView.findViewById<TextView>(R.id.tv_dialog_server_name)
        val dialogServerPing = customDialogView.findViewById<TextView>(R.id.tv_dialog_server_ping)
        val dialogButton = customDialogView.findViewById<TextView>(R.id.btn_dialog_cancel)

        val builder = AlertDialog.Builder(this)
        builder.setView(customDialogView)
        checkPingDialog = builder.create()

        dialogButton.setOnClickListener {
            checkPingDialog.dismiss()
        }
        checkPingDialog.show()

        var bestPing: Int = 9999999

        val testJobs = mutableListOf<Job>()

        val mainJob = CoroutineScope(Dispatchers.Main).launch {
            if (DataStore.serviceState.started) {
                stopService()
                delay(500) // wait for service stop
            }
            val group = DataStore.currentGroup()
            val profilesUnfiltered = SagerDatabase.proxyDao.getByGroup(group.id)
            val profiles = ConcurrentLinkedQueue(profilesUnfiltered)
            val testPool = newFixedThreadPoolContext(
                DataStore.connectionTestConcurrent,
                "urlTest"
            )
            repeat(DataStore.connectionTestConcurrent) {
                testJobs.add(launch(testPool) {
                    val urlTest = UrlTest()

                    while (isActive) {
                        val profile = profiles.poll() ?: break
                        profile.status = 0
                        withContext(Dispatchers.Main) {
                            dialogServerName.text = profile.displayName()
                            dialogServerPing.text = ""
                        }

                        try {
                            val result = urlTest.doTest(profile)
                            setServerStatus(profile, result, 1, null)
                            profile.status = 1
                            profile.ping = result
                            if (result <= 600) {
                                withContext(Dispatchers.Main) {
                                    dialogServerPing.setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.material_green_500))
                                    dialogServerPing.text = result.toString() + "ms"
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    dialogServerPing.setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.material_red_500))
                                    dialogServerPing.text = result.toString() + "ms"
                                }
                            }
                            if (result < bestPing) {
                                var countryCode = ""
                                var serverName = ""
                                AppRepository.allServers.forEach { item ->
                                    item.dropdownItems.forEach{
                                        if(it.id == profile.id) {
                                            countryCode = AppRepository.countryCodeMapper(item.name)
                                            serverName = it.name
                                        }
                                    }
                                }
                                val emptyList: MutableList<ListSubItem> = mutableListOf()
                                val resourceName = "ic_${countryCode}_flag"
                                val iconResId = resources.getIdentifier(
                                    resourceName,
                                    "drawable",
                                    this@DashboardActivity.packageName
                                )
                                bestPing = result
                                AppRepository.isBestServerSelected = true
                                bestServer = ListItem(
                                    serverName + " [Best Location]",
                                    emptyList,
                                    false,
                                    iconResId,
                                    true,
                                    profile.id
                                )
                            }
                        } catch (e: PluginManager.PluginNotFoundException) {
                            setServerStatus(profile, 0, 2, e.readableMessage)
                            profile.status = 2
                            profile.error = e.readableMessage
                            withContext(Dispatchers.Main) {
                                dialogServerPing.setTextColor(Color.RED)
                                dialogServerPing.text = "Unavailable!"
                            }
                        } catch (e: Exception) {
                            setServerStatus(profile, 0, 3, e.readableMessage)
                            profile.status = 3
                            profile.error = e.readableMessage
                            withContext(Dispatchers.Main) {
                                dialogServerPing.setTextColor(Color.RED)
                                dialogServerPing.text = "Unavailable!"
                            }
                        }
                    }
                })
            }

            testJobs.joinAll()

            checkPingDialog.dismiss()

            bestServer?.let {
                if (AppRepository.allServers[0].isBestServer) {
                    AppRepository.allServers.removeAt(0)
                }

                AppRepository.allServers.add(0, it)
                AppRepository.refreshServersListView()
            }

            // Start service after urlTest() processing
            if (DataStore.serviceState.canStop) SagerNet.stopService() else connect.launch(null)
        }

        // Utility function to switch coroutine context to the main thread
        suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T =
            withContext(Dispatchers.Main, block)
    }

    private suspend fun setServerStatus(profile: ProxyEntity, ping: Int, status: Int, error: String?) {
        val serverName = profile.displayName()
        val countryCode = serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
        val foundItem = AppRepository.allServers.find {
            it.name == AppRepository.flagNameMapper(countryCode)
        }
        val foundSubItem = foundItem?.dropdownItems?.find { it.id == profile.id}
        foundSubItem?.status = status
        foundSubItem?.ping = ping
        foundSubItem?.error = error

        withContext(Dispatchers.Main) {
            val serverName = profile.displayName()
            val countryCode = serverName.substring(serverName.length - 5, serverName.length).substring(0, 2).lowercase()
            val foundItem = AppRepository.allServers.find {
                it.name == AppRepository.flagNameMapper(countryCode)
            }
            val foundSubItem = foundItem?.dropdownItems?.find { it.id == profile.id}
            foundSubItem?.status = status
            foundSubItem?.ping = ping
            foundSubItem?.error = error
        }
    }

    fun checkInternet() {
        if (Network.checkConnectivity(this)) {
            println("HAMED_LOG_HAS_INTERNET!")
        }  else {
            println("HAMED_LOG_NO_INTERNET")
        }
    }

    fun getCurrentGroupFragment(): ConfigurationFragment.GroupFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f" + DataStore.selectedGroup) as ConfigurationFragment.GroupFragment?
        } catch (e: Exception) {
            Logs.e(e)
            null
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}
        // Load an ad.
        loadRewardedAd()
    }

    private fun loadRewardedAd() {
        if (rewardedAd == null) {
            isLoading = true
            var adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                this,
                AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        println("HAMED_LOG_EU_REWARD_AD_FAILED_ON_LOAD: " + adError.message)
                        if (googleMobileAdsConsentManager.canRequestAds) {
                            println("HAMED_LOG_EU_LOADED_NEW")
                            Handler().postDelayed(
                                Runnable { loadRewardedAd() },
                                5000
                            )

                        }
                        Log.d(TAG, adError.message)
                        isLoading = false
                        rewardedAd = null
                    }

                    override fun onAdLoaded(ad: RewardedAd) {
                        println("HAMED_LOG_EU_REWARD_AD_LOADED")
                        Log.d(TAG, "Ad was loaded.")
                        rewardedAd = ad
                        isLoading = false
                    }
                }
            )
        }
    }

    private fun showRewardedVideo() {
//        binding.showVideoButton.visibility = View.INVISIBLE
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null
                        if (googleMobileAdsConsentManager.canRequestAds) {
                            loadRewardedAd()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        rewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }

            rewardedAd?.show(
                this,
                OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
//                    addCoins(rewardAmount)
                    Log.d("TAG", "User earned the reward.")
                }
            )
        }
    }
}
