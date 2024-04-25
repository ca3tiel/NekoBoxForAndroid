package io.nekohasekai.sagernet.vpn

import android.os.Bundle
import android.webkit.WebViewClient
import io.nekohasekai.sagernet.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : BaseThemeActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Enable JavaScript (if needed)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true

        // Set a WebViewClient to handle page navigation within the WebView
        binding.webView.webViewClient = WebViewClient()

        // Load a web page
        val url = "https://api.unitavpn.com/password/reset"
        binding.webView.loadUrl(url)
    }
}