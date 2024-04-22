package io.nekohasekai.sagernet.vpn

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.nekohasekai.sagernet.R

open class BaseThemeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navyBlue)
    }
}
