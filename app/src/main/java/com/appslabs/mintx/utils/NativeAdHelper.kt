package com.appslabs.mintx.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.appslabs.mintx.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Helper class for loading and displaying Native Ads
 */
class NativeAdHelper(
    private val context: Context,
    private val adUnitId: String = "ca-app-pub-7084079995330446/1629429671"
) {

    private var currentNativeAd: NativeAd? = null

    /**
     * Load a native ad
     * @param onAdLoaded Callback when ad is successfully loaded
     * @param onAdFailed Callback when ad loading fails
     */
    fun loadAd(
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // Clean up old ad before storing new one
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    android.util.Log.e("NativeAdHelper", "Failed to load ad: ${loadAdError.message}")
                    onAdFailed?.invoke(loadAdError)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    android.util.Log.d("NativeAdHelper", "Ad clicked")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    android.util.Log.d("NativeAdHelper", "Ad impression")
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Populate the native ad view with ad content
     * @param nativeAd The loaded native ad
     * @param adContainer The container to add the ad view to
     */
    fun populateNativeAdView(nativeAd: NativeAd, adContainer: FrameLayout) {
        // Inflate the ad view
        val adView = LayoutInflater.from(context)
            .inflate(R.layout.ad_native_template, null) as NativeAdView

        // Set the ad view components
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Populate the ad view with ad content
        (adView.headlineView as TextView).text = nativeAd.headline
        (adView.bodyView as TextView).text = nativeAd.body
        (adView.callToActionView as TextView).text = nativeAd.callToAction

        // Icon
        if (nativeAd.icon != null) {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        } else {
            adView.iconView?.visibility = View.GONE
        }

        // Star rating
        if (nativeAd.starRating != null) {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        } else {
            adView.starRatingView?.visibility = View.GONE
        }

        // Advertiser
        if (nativeAd.advertiser != null) {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        } else {
            adView.advertiserView?.visibility = View.GONE
        }

        // Media view
        if (nativeAd.mediaContent != null) {
            (adView.mediaView as MediaView).mediaContent = nativeAd.mediaContent
            adView.mediaView?.visibility = View.VISIBLE
        } else {
            adView.mediaView?.visibility = View.GONE
        }

        // Register the native ad with the ad view
        adView.setNativeAd(nativeAd)

        // Add the ad view to the container
        adContainer.removeAllViews()
        adContainer.addView(adView)
        adContainer.visibility = View.VISIBLE
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
    }
}
