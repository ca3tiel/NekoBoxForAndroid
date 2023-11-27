package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.WelcomeActivity
import io.nekohasekai.sagernet.vpn.repositories.SocialAuthRepository

class AccountFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivAccountIconAngle)
        val emailTextView: TextView = view.findViewById(R.id.tvEmail)
        val signOutButton: Button = view.findViewById(R.id.btnExitAccount)

        // Your existing code for the iconAngle click listener
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Add the provided code
        val currentUser = SocialAuthRepository.firebaseAuth.currentUser
        if (currentUser != null) {
            emailTextView.text = "${currentUser.email}"
        }

        signOutButton.setOnClickListener {
            SocialAuthRepository.facebookLoginManager.logOut()
            SocialAuthRepository.firebaseAuth.signOut()
            SocialAuthRepository.googleSignInClient.signOut()
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}
