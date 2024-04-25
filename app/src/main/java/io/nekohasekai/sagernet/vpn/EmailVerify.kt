package io.nekohasekai.sagernet.vpn

import android.content.Intent
import android.os.Bundle
import android.view.View
import io.nekohasekai.sagernet.databinding.ActivityEmailVerifyBinding
import io.nekohasekai.sagernet.vpn.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EmailVerify : BaseThemeActivity() {
    private lateinit var binding: ActivityEmailVerifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailVerifyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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