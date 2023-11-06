package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.vpn.utils.InternetConnectionChecker
import java.util.concurrent.atomic.AtomicBoolean

object AdRepository {
    private var rewardedAd: RewardedAd? = null
    private var rewardedAdToken: String? = null
    private lateinit var consentInformation: ConsentInformation
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)
    lateinit var bannerAdView : AdView

    @SuppressLint("StaticFieldLeak")
    lateinit var internetChecker: InternetConnectionChecker

    fun loadRewardedAd(context: Context) {
        if(!canRequestAds()) {
            return
        }
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
        if(!canRequestAds()) {
            return
        }
        if(!AppRepository.isInternetAvailable()) {
            return
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

    fun checkAdConsent(context: Context) {
        // Set tag for under age of consent. false means users are not under age
        // of consent.
        val params = ConsentRequestParameters
            .Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation.requestConsentInfoUpdate(
            (context as Activity),
            params,
            ConsentInformation.OnConsentInfoUpdateSuccessListener {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    context,
                    ConsentForm.OnConsentFormDismissedListener {
                            loadAndShowError ->
                        // Consent gathering failed.
                        println("HAMED_LOG_loadAndShowError: " + loadAndShowError?.message)
                        // Consent has been gathered.
                        if (consentInformation.canRequestAds()) {
                            initializeMobileAdsSdk(context)
                        }
                    }
                )
            },
            ConsentInformation.OnConsentInfoUpdateFailureListener {
                    requestConsentError ->
                // Consent gathering failed.
                println("HAMED_LOG_requestConsentError: " + requestConsentError.message)
//                Log.w(TAG, String.format("%s: %s",
//                    requestConsentError.errorCode(),
//                    requestConsentError.message()))
            })
    }

    private fun initializeMobileAdsSdk(context: Context) {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(context)

        // Request load ads after user granted consent
        loadBannerAd(context as Activity)
        loadRewardedAd(context)
    }

    fun checkInitializeMobileAdsSdk(context: Context) {
        // Check if you can initialize the Google Mobile Ads SDK in parallel
        // while checking for new consent information. Consent obtained in
        // the previous session can be used to request ads.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk(context)
        }
    }

    private fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    fun loadBannerAd(activity: Activity) {
        if(!canRequestAds()) {
            return
        }
        MobileAds.initialize(activity) {}
        val adRequest = AdRequest.Builder().build()
        bannerAdView.loadAd(adRequest)
    }
}
