package com.digitar.mintx.utils

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
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    // Test Ad Unit IDs
    private const val REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val BANNER_AD_ID = "ca-app-pub-3940256099942544/9214589741"
    private const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
    }

    // --- Rewarded Ad ---

    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
                android.util.Log.e("AdManager", "Rewarded Ad failed to load: ${adError.message}")
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                android.util.Log.d("AdManager", "Rewarded Ad loaded")
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            var rewardEarned = false
            
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
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

            rewardedAd?.show(activity) { rewardItem ->
                // Mark that reward was earned, but don't call callback yet
                rewardEarned = true
            }
        } else {
            android.util.Log.d("AdManager", "Rewarded Ad not ready, reloading...")
            loadRewardedAd(activity)
            onAdClosed() // Or handle as error
        }
    }
    
    fun isRewardedAdReady(): Boolean = rewardedAd != null

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
