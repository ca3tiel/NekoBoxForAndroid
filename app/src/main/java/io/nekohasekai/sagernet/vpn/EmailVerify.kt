package io.nekohasekai.sagernet.vpn

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.databinding.ActivityEmailVerifyBinding
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EmailVerify : AppCompatActivity() {
    private lateinit var binding: ActivityEmailVerifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Change status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        // Change navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navyBlue)

        binding.btnVerify.setOnClickListener {
            val verifyCode = binding.txtVerifyCode.text.toString()

            if (verifyCode.isNotEmpty()) {
                performVerify(verifyCode)
            }
        }
    }

    private fun performVerify(verifyCode: String) {
        runBlocking {
            try {
                withContext(Dispatchers.IO) {
                    val email = AuthRepository.getUserEmail()
                    val responseCode = AuthRepository.verify(email, verifyCode)
                    when (responseCode) {
                        200 -> {
                            runOnUiThread {
                                binding.tvValidationError.visibility = View.INVISIBLE
                                navigateToDashboardActivity()
                            }
                        }
                        else -> {
                            runOnUiThread {
                                binding.tvValidationError.text = "Verify Code was wrong!"
                                binding.tvValidationError.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Request failed: ${e.message}")
            }
        }
    }

    private fun navigateToDashboardActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}