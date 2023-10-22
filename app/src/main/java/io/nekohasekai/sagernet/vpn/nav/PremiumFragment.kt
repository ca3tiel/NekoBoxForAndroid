package io.nekohasekai.sagernet.vpn.nav

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.nekohasekai.sagernet.R
class PremiumFragment : Fragment() {
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_premium, container, false)
        val iconAngle = view.findViewById<ImageView>(R.id.ivPremiumIconAngle)
        val cl30Days = view.findViewById<ConstraintLayout>(R.id.cl30Days)
        val cl90Days = view.findViewById<ConstraintLayout>(R.id.cl90Days)
        val cl180Days = view.findViewById<ConstraintLayout>(R.id.cl180Days)
        val cl365Days = view.findViewById<ConstraintLayout>(R.id.cl365Days)

        // Add click listener to iconAngle
        iconAngle.setOnClickListener {
            // Pop the back stack to navigate back to the previous fragment
            requireActivity().supportFragmentManager.popBackStack()
        }


        cl30Days.setOnClickListener {
            cl30Days.setBackgroundResource(R.drawable.background_premium_select)
        }

        cl90Days.setOnClickListener {
            cl90Days.setBackgroundResource(R.drawable.background_premium_select)
        }

        cl180Days.setOnClickListener {
            cl180Days.setBackgroundResource(R.drawable.background_premium_select)
        }

        cl365Days.setOnClickListener {
            cl365Days.setBackgroundResource(R.drawable.background_premium_select)
        }

        return view
    }
}