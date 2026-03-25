package com.odom.motivationlocker

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("ad_counter", Context.MODE_PRIVATE)

    companion object {
        private const val GENERATE_COUNT_KEY = "generate_count"
        private const val SCAN_COUNT_KEY = "scan_count"
        private const val GENERATE_THRESHOLD = 3
        private const val SCAN_THRESHOLD = 5
    }

    init {
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, context.getString(R.string.TEST_ADMOB_FULLSCREEN_ID), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdDismissed()
                    loadInterstitialAd() // Load next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadInterstitialAd() // Load next ad
                }
            }
            ad.show(activity)
        } ?: run {
            onAdDismissed()
            loadInterstitialAd() // Try to load ad for next time
        }
    }
}
