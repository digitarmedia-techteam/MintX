package com.appslabs.mintx.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

object AdManager {

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var interstitialAd: InterstitialAd? = null

//     Ad Unit IDs
//    private const val REWARDED_INTERSTITIAL_AD_ID = ""
//    private const val BANNER_AD_ID = ""
//    private const val INTERSTITIAL_AD_ID = ""

    private const val REWARDED_INTERSTITIAL_AD_ID = "ca-app-pub-7084079995330446/9316348004"
    private const val BANNER_AD_ID = "ca-app-pub-7084079995330446/6665008207"
    private const val INTERSTITIAL_AD_ID = "ca-app-pub-7084079995330446/4056282468"

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    // --- Rewarded Interstitial Ad ---

    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(context, REWARDED_INTERSTITIAL_AD_ID, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedInterstitialAd = null
                android.util.Log.e("AdManager", "Rewarded Interstitial Ad failed to load: ${adError.message}")
            }

            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                rewardedInterstitialAd = ad
                android.util.Log.d("AdManager", "Rewarded Interstitial Ad loaded")
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedInterstitialAd != null) {
            var rewardEarned = false
            
            rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    loadRewardedAd(activity) // Preload next
                    
                    // Call appropriate callback based on whether reward was earned
                    if (rewardEarned) {
                        onRewardEarned()
                    } else {
                        onAdClosed()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdClosed()
                }
            }

            rewardedInterstitialAd?.show(activity) { rewardItem ->
                // Mark that reward was earned, but don't call callback yet
                rewardEarned = true
            }
        } else {
            android.util.Log.d("AdManager", "Rewarded Interstitial Ad not ready, reloading...")
            loadRewardedAd(activity)
            onAdClosed() // Or handle as error
        }
    }
    
    fun isRewardedAdReady(): Boolean = rewardedInterstitialAd != null

    // --- Interstitial Ad ---

    fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity) // Preload next
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
             loadInterstitialAd(activity)
             onAdClosed()
        }
    }

    // --- Banner Ad ---

    fun loadBannerAd(activity: Activity, adContainer: ViewGroup, onAdLoaded: (() -> Unit)? = null) {
        val adView = AdView(activity)
        adView.adUnitId = BANNER_AD_ID
        
        val adSize = getAdSize(activity)
        adView.setAdSize(adSize)

        adContainer.removeAllViews()
        adContainer.addView(adView)

        // Add listener to show container when ad loads
        adView.adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                onAdLoaded?.invoke()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                // Don't show container if ad fails to load
            }
        }

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun getAdSize(activity: Activity): AdSize {
        // Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density

        val adWidth = (widthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}

