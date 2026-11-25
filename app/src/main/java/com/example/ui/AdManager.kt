package com.example.ui

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private const val TAG = "AdManager"
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private val isShowingAd = AtomicBoolean(false)
    
    // Sample AdMob unit IDs
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1350477690991328/1140772950"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-1350477690991328/7447959765"

    fun loadInterstitial(activity: Activity) {
        try {
            if (activity.isFinishing || activity.isDestroyed || SubscriptionManager.isPremium.value) return
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, "Interstitial not loaded: ${adError.message}")
                        interstitialAd = null
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "Interstitial exception: ${e.message}")
        }
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed || SubscriptionManager.isPremium.value) {
            onAdDismissed()
            return
        }
        val ad = interstitialAd
        if (ad != null && isShowingAd.compareAndSet(false, true)) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isShowingAd.set(false)
                    loadInterstitial(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    isShowingAd.set(false)
                    onAdDismissed()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onAdDismissed()
        }
    }

    fun loadRewarded(activity: Activity) {
        try {
            if (activity.isFinishing || activity.isDestroyed || SubscriptionManager.isPremium.value) return
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d(TAG, "Rewarded not loaded: ${adError.message}")
                        rewardedAd = null
                    }
                })
        } catch(e: Exception) {
            Log.d(TAG, "Rewarded exception: ${e.message}")
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdDismissed: () -> Unit = {}) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdDismissed()
            return
        }
        val ad = rewardedAd
        if (ad != null && isShowingAd.compareAndSet(false, true)) {
            val rewardGranted = AtomicBoolean(false)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    isShowingAd.set(false)
                    loadRewarded(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    isShowingAd.set(false)
                    onAdDismissed()
                }
            }
            ad.show(activity) { _ ->
                if (rewardGranted.compareAndSet(false, true)) {
                    onRewardEarned()
                }
            }
        } else {
            onAdDismissed()
        }
    }
}
