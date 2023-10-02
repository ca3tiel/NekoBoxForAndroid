package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceDataStore
import com.google.android.material.navigation.NavigationView
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.databinding.ActivityDashboardBinding
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ThemedActivity
import io.nekohasekai.sagernet.ui.VpnRequestActivity
import io.nekohasekai.sagernet.vpn.nav.MenuFragment
import io.nekohasekai.sagernet.vpn.serverlist.MyFragment

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
        println("HAMED_LOG_TEST2: " + state)
        DataStore.serviceState = state

//        binding.fab.changeState(state, DataStore.serviceState, animate)
//        binding.stats.changeState(state)
//        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()
    }
}
