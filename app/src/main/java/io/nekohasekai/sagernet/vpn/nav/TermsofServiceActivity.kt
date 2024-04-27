package io.nekohasekai.sagernet.vpn.nav

import android.os.Bundle
import android.widget.ImageView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.BaseThemeActivity

class TermsofServiceActivity : BaseThemeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termsof_service)

        val ivTermsofServiceIconAngle: ImageView = findViewById(R.id.ivTermsofServiceIconAngle)

        ivTermsofServiceIconAngle.setOnClickListener {
            // Handle click event
            // Set result OK to indicate the action was successful
            setResult(RESULT_OK)
            // Finish the current activity (PrivacyPolicyActivity)
            finish()
        }
    }
}
