package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.databinding.FragmentMenuBinding
import io.nekohasekai.sagernet.vpn.DashboardActivity
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class MenuFragment : Fragment() {

    private lateinit var binding: FragmentMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuBinding.inflate(inflater, container, false)
        val view = binding.root

        // Add click listener for iconAngle
        binding.ivPreferencesIconAngle.setOnClickListener {
            startActivity(Intent(activity, DashboardActivity::class.java))
        }

        // Add click listener for llGeneral
        binding.llGeneral.setOnClickListener {
            loadFragment(GeneralFragment())
        }

        // Add click listener for llAccount
        binding.llAccount.setOnClickListener {
            loadFragment(AccountFragment())
        }

        // Add click listener for llConnection
        binding.llConnection.setOnClickListener {
            loadFragment(ConnectionFragment())
        }

        // Add click listener for llVip
        binding.llVip.setOnClickListener {
            loadFragment(PremiumFragment())
        }

        // Add click listener for llComment
        binding.llComment.setOnClickListener {
            loadFragment(CommentFragment())
        }

        // Add click listener for llSettings
        binding.llSettings.setOnClickListener {
            loadFragment(SettingsFragment())
        }

        // Add click listener Message for llShare
        binding.llShare.setOnClickListener {
            shareLinkWithMessage(AppRepository.ShareCustomMessage)
        }

        // Add click listener for llTelegram
        binding.llTelegram.setOnClickListener {
            val telegramUri = Uri.parse("https://t.me/holyproxy")
            val telegramIntent = Intent(Intent.ACTION_VIEW, telegramUri)
            startActivity(telegramIntent)
        }

        // Add click listener for llAbout
        binding.llAbout.setOnClickListener {
            loadFragment(AboutFragment())
        }

        // Handle back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do whatever you want when the back button is pressed in the fragment
                val dashboardIntent = Intent(activity, DashboardActivity::class.java)
                startActivity(dashboardIntent)
            }
        })

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(android.R.id.content, fragment)
        transaction.addToBackStack(null) // Optional: Allows you to navigate back to the previous fragment
        transaction.commit()
    }

    // Function to share link with a custom message
    private fun shareLinkWithMessage(message: String) {
        // Create an Intent with ACTION_SEND
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "$message\n" + AppRepository.ShareApplicationLink
            )
        }

        // Start the system's chooser to share the content
        startActivity(Intent.createChooser(sendIntent, "Share link with:"))
    }

    companion object {
        @JvmStatic
        fun newInstance() = MenuFragment()
    }
}
