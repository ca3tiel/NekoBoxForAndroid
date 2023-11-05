package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.AD_REWARDED_UNIT_ID
import io.nekohasekai.sagernet.vpn.TAG
import io.nekohasekai.sagernet.vpn.utils.InternetConnectionChecker

object AdRepository {
    private var rewardedAd: RewardedAd? = null
    private var rewardedAdToken: String? = null
    @SuppressLint("StaticFieldLeak")
    lateinit var internetChecker: InternetConnectionChecker

    fun loadRewardedAd(context: Context) {
        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(context,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                println("HAMED_LOG_Rewards_onAdFailedToLoad: " + adError.message)
//                checkInternet()
                // Start periodic checks
                internetChecker?.startChecking()
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                // Stop periodic checks when no longer needed, e.g., in onDestroy()
                internetChecker?.stopChecking()
                println("HAMED_LOG_Rewards_onAdLoaded")
            }
        })
    }

    fun showRewardedAd(context: Context) {
        if(!AppRepository.isInternetAvailable()) {
            return;
        }
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    rewardedAd = null
//                    loadRewardedAd(context)
                    loadRewardedAd(context)
                    println("HAMED_LOG_REWARDED_onAdDismissedFullScreenContent")
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    rewardedAd = null
                    println("HAMED_LOG_REWARDS_onAdFailedToShowFullScreenContent")
//                    loadRewardedAd(context)
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                }
            }
            rewardedAd?.let { ad ->
                println("HAMED_LOG_REWARDS_SHOW_1")
                ad.show(context as Activity, OnUserEarnedRewardListener { rewardItem ->
                    println("HAMED_LOG_REWARDS_SHOW_2")
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                })
            } ?: run {

            }
        }
    }

//    fun loadBannerAd(context: Context) {
//        MobileAds.initialize(context) {}
//        mAdView = findViewById(R.id.adView)
//        val adRequest = AdRequest.Builder().build()
//        mAdView.loadAd(adRequest)
//    }
}
