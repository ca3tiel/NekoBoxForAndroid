package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import io.nekohasekai.sagernet.R

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1000 // 1 seconds in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hide the Action Bar
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        // Use a Handler to post a delayed Runnable
        Handler().postDelayed({
            // After the delay, start the main activity
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)

            // Finish the splash activity to prevent returning to it later
            finish()
        }, SPLASH_DELAY)
    }
}

