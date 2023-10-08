package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.app.AlertDialog
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceDataStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.bg.proto.UrlTest
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProfileManager
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.databinding.ActivityDashboardBinding
import io.nekohasekai.sagernet.databinding.LayoutProgressListBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.ktx.getColour
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import io.nekohasekai.sagernet.plugin.PluginManager
import io.nekohasekai.sagernet.ui.ConfigurationFragment
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.nav.MenuFragment
import io.nekohasekai.sagernet.vpn.repositories.AppRepository
import io.nekohasekai.sagernet.vpn.serverlist.MyAdapter
import io.nekohasekai.sagernet.vpn.serverlist.MyFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import moe.matsuri.nb4a.Protocols
import moe.matsuri.nb4a.Protocols.getProtocolColor
import moe.matsuri.nb4a.proxy.neko.NekoJSInterface
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class DashboardActivity : ThemedActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener {

    lateinit var binding: ActivityDashboardBinding
    private lateinit var iconImageView: ImageView
    private lateinit var ivAll: ImageView
    private lateinit var ivMtn: ImageView
    private lateinit var ivMci: ImageView
    private lateinit var stateTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var addtimeTextView: TextView
    private var isConnected = false
    private var timerHandler: Handler = Handler()
    private var timerRunning = false
    private var timeRemainingMillis = 30 * 60 * 1000 // 30 minutes in milliseconds
    private var ivAllClicked = true // Set IVall as clicked by default
    private var ivMtnClicked = false // Add a variable to track IVMTN click state
    private var ivMciClicked = false // Add a variable to track IVMCI click state
    private lateinit var checkPingDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val telegramIcon = findViewById<ImageView>(R.id.TelegramIcon)
        val connection = SagerConnection(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND, true)

        // Set an OnClickListener to MainActivity
        telegramIcon.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                // Start the MainActivity
                val intent = Intent(this@DashboardActivity, MainActivity::class.java)
                startActivity(intent)
            }
        })

        // Initialize the fragment container
        val fragmentContainer = findViewById<View>(R.id.fragmentContainer)

        val pingBtn = findViewById<ConstraintLayout>(R.id.clIconPing)

        pingBtn.setOnClickListener {
            urlTest()
        }

        // Find the NavMenuIcon ImageView and set an OnClickListener
        val navMenuIcon = findViewById<ImageView>(R.id.NavMenuIcon)
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

        iconImageView = findViewById(R.id.PowerIcon)
        ivAll = findViewById(R.id.IVall)
        ivMtn = findViewById(R.id.IVMTN)
        ivMci = findViewById(R.id.IVMCI)
        stateTextView = findViewById(R.id.PowerState)
        timerTextView = findViewById(R.id.TVtimer)
        addtimeTextView = findViewById(R.id.TVaddTime)

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
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.commit()
        }

        iconImageView.setOnClickListener {
            if (DataStore.serviceState.canStop) SagerNet.stopService() else connect.launch(
                null
            )
            if (!isConnected) {
                showConnectingState()
                Handler().postDelayed({
                    showConnectedState()
                    startTimer()
                }, 2000) // Delay of 2 seconds
            } else {
                showNotConnectedState()
                stopTimer()
                add30MinutesToTimer() // Add 30 minutes
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
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVAll") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, fragment)
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
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMTN") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, fragment)
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
                val fragment = MyFragment()
                val bundle = Bundle()
                bundle.putString("iconClicked", "IVMCI") // Pass the clicked icon value to the fragment
                fragment.arguments = bundle
                val fragmentManager: FragmentManager = supportFragmentManager
                val transaction: FragmentTransaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, fragment)
                transaction.commit()
            }
            // Reset IVall and IVMTN click states
            ivAllClicked = false
            ivMtnClicked = false
            updateIVAllIcon() // Update the IVall icon
            updateIVMtnIcon() // Update the IVMTN icon
        }
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
        if (!timerRunning) {
            timeRemainingMillis += 30 * 60 * 1000
            updateTimerText()
        }
    }

    private fun startTimer() {
        timerRunning = true
        timerHandler.post(object : Runnable {
            override fun run() {
                if (timeRemainingMillis <= 0) {
                    showNotConnectedState()
                } else {
                    timeRemainingMillis -= 1000 // Decrease by 1 second
                    updateTimerText()
                    timerHandler.postDelayed(this, 1000) // Run every 1 second
                }
            }
        })
    }

    private fun updateTimerText() {
        val minutes = (timeRemainingMillis / 1000) / 60
        val seconds = (timeRemainingMillis / 1000) % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = formattedTime
    }

    private fun stopTimer() {
        timerRunning = false
        timerHandler.removeCallbacksAndMessages(null)
    }

    private fun showConnectingState() {
        iconImageView.setImageResource(R.drawable.connecting)
        stateTextView.text = "Connecting..."
        timerTextView.visibility = View.INVISIBLE
        addtimeTextView.visibility = View.INVISIBLE
        iconImageView.isEnabled = false
    }

    private fun showConnectedState() {
        iconImageView.setImageResource(R.drawable.connected)
        stateTextView.text = "Connected"
        isConnected = true
        iconImageView.isEnabled = true
        timerTextView.visibility = View.VISIBLE
        addtimeTextView.visibility = View.INVISIBLE
    }

    private fun showNotConnectedState() {
        iconImageView.setImageResource(R.drawable.connect)
        stateTextView.text = "Connect"
        isConnected = false
        timerTextView.visibility = View.INVISIBLE
        addtimeTextView.visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentState", stateTextView.text.toString())
        outState.putBoolean("isFragmentVisible", findViewById<View>(R.id.fragmentContainer).visibility == View.VISIBLE)
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
            startTimer()
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
//        binding.fab.changeState(state, DataStore.serviceState, animate)
//        binding.stats.changeState(state)
//        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()
    }

    fun stopService() {
        if (DataStore.serviceState.started) SagerNet.stopService()
    }

    private fun urlTest() {
        val customDialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog, null)
        val dialogServerName = customDialogView.findViewById<TextView>(R.id.tv_dialog_server_name)
        val dialogServerPing = customDialogView.findViewById<TextView>(R.id.tv_dialog_server_ping)
        val dialogButton = customDialogView.findViewById<Button>(R.id.btn_dialog_cancel)

        val builder = AlertDialog.Builder(this)
        builder.setView(customDialogView)
        checkPingDialog = builder.create()

        dialogButton.setOnClickListener {
            checkPingDialog.dismiss()
        }
        checkPingDialog.show()


        val testJobs = mutableListOf<Job>()

        val mainJob = runOnDefaultDispatcher {
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
                    val urlTest = UrlTest() // note: this is NOT in bg process
                    while (isActive) {
                        val profile = profiles.poll() ?: break
                        profile.status = 0
                        dialogServerName.text = profile.displayName()
                        dialogServerPing.text = ""

                        try {
                            val result = urlTest.doTest(profile)
                            setServerStatus(profile, result, 1, null)
                            profile.status = 1
                            profile.ping = result
                            dialogServerPing.setTextColor(Color.BLACK)
                            dialogServerPing.text = result.toString() + "ms"
                        } catch (e: PluginManager.PluginNotFoundException) {
                            setServerStatus(profile, 0, 2, e.readableMessage)
                            profile.status = 2
                            profile.error = e.readableMessage
                            dialogServerPing.setTextColor(Color.RED)
                            dialogServerPing.text = "Unavailable!"
                        } catch (e: Exception) {
                            setServerStatus(profile, 0, 3, e.readableMessage)
                            profile.status = 3
                            profile.error = e.readableMessage
                            dialogServerPing.setTextColor(Color.RED)
                            dialogServerPing.text = "Unavailable!"
                        }
                    }
                })
            }

            testJobs.joinAll()

            onMainDispatcher {
                checkPingDialog.dismiss()
                val adapter = MyAdapter(AppRepository.allServers) { }
                AppRepository.recyclerView.adapter = adapter
            }
        }
    }

    private fun setServerStatus(profile: ProxyEntity, ping: Int, status: Int, error: String?) {
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

    inner class TestDialog {
        val binding = LayoutProgressListBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(this@DashboardActivity).setView(binding.root)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                cancel()
            }
            .setOnDismissListener {
                cancel()
            }
            .setCancelable(false)

        lateinit var cancel: () -> Unit
        val fragment by lazy { getCurrentGroupFragment() }
        val results = Collections.synchronizedList(mutableListOf<ProxyEntity?>())
        var proxyN = 0
        val finishedN = AtomicInteger(0)

        fun insert(profile: ProxyEntity?) {
            results.add(profile)
        }

        fun update(profile: ProxyEntity) {
            fragment?.configurationListView?.post {
                val context = this@DashboardActivity ?: return@post
//                if (!isAdded) return@post

                var profileStatusText: String? = null
                var profileStatusColor = 0

                when (profile.status) {
                    -1 -> {
                        profileStatusText = profile.error
                        profileStatusColor = context.getColorAttr(android.R.attr.textColorSecondary)
                    }

                    0 -> {
                        profileStatusText = getString(R.string.connection_test_testing)
                        profileStatusColor = context.getColorAttr(android.R.attr.textColorSecondary)
                    }

                    1 -> {
                        profileStatusText = getString(R.string.available, profile.ping)
                        profileStatusColor = context.getColour(R.color.material_green_500)
                    }

                    2 -> {
                        profileStatusText = profile.error
                        profileStatusColor = context.getColour(R.color.material_red_500)
                    }

                    3 -> {
                        val err = profile.error ?: ""
                        val msg = Protocols.genFriendlyMsg(err)
                        profileStatusText = if (msg != err) msg else getString(R.string.unavailable)
                        profileStatusColor = context.getColour(R.color.material_red_500)
                    }
                }

                val text = SpannableStringBuilder().apply {
                    append("\n" + profile.displayName())
                    append("\n")
                    append(
                        profile.displayType(),
                        ForegroundColorSpan(context.getProtocolColor(profile.type)),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ")
                    append(
                        profileStatusText,
                        ForegroundColorSpan(profileStatusColor),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append("\n")
                }

                binding.nowTesting.text = text
                binding.progress.text = "${finishedN.addAndGet(1)} / $proxyN"
            }
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
}
