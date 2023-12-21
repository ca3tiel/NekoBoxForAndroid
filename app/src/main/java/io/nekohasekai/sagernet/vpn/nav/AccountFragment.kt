package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository

class AccountFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivAccountIconAngle)


        // Your existing code for the iconAngle click listener
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

    // Retrieve user's email and set it to tvEmail
    val userEmail = AuthRepository.getUserEmail()
    if (userEmail != null) {
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        tvEmail.text = userEmail
    }

    return view
    }
}