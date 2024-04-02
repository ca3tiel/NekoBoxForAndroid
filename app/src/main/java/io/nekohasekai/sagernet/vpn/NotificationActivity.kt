package io.nekohasekai.sagernet.vpn

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebViewClient
import io.nekohasekai.sagernet.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Enable JavaScript (if needed)
        binding.notificationWebView.settings.javaScriptEnabled = true
        binding.notificationWebView.settings.domStorageEnabled = true

        // Set a WebViewClient to handle page navigation within the WebView
        binding.notificationWebView.webViewClient = WebViewClient()

        // Load a web page
        val url = "https://google.com"
        binding.notificationWebView.loadUrl(url)
    }
}