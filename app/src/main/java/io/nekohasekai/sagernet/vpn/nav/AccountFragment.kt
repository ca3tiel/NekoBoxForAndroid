package io.nekohasekai.sagernet.vpn.nav

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.WelcomeActivity
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository

class AccountFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivAccountIconAngle)
        val ExitAccountButton = view.findViewById<AppCompatButton>(R.id.btnExitAccount)


        // iconAngle click listener
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }

        // set user's email
        val userEmail = AuthRepository.getUserEmail()
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        tvEmail.text = userEmail

        // ExitAccountButton
        ExitAccountButton.setOnClickListener {
            AuthRepository.clearUserToken()
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}