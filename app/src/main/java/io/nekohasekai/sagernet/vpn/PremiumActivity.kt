package io.nekohasekai.sagernet.vpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.nekohasekai.sagernet.R

class PremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_premium)

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.darkPurple)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.darkPurple)

        val ivIconAngle = findViewById<ImageView>(R.id.ivIconAngle)
        ivIconAngle.setOnClickListener { navigateToDashboard() }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}