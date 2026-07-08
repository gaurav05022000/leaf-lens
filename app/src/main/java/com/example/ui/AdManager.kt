package com.example.ui

import android.app.Activity
import android.util.Log
import com.example.ui.PointsManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    // Sample AdMob unit IDs for testing
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    fun loadInterstitial(activity: Activity) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    // Preload the next one
                    loadInterstitial(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            // Ad not ready, just proceed
            loadInterstitial(activity)
            onAdDismissed()
        }
    }

    fun loadRewarded(activity: Activity) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                }
            })
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdDismissed: () -> Unit = {}) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    // Preload the next one
                    loadRewarded(activity)
                    onAdDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onAdDismissed()
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                // Reward the user!
                PointsManager.addPoints(5)
                onRewardEarned()
            }
        } else {
            // Ad not ready - simulate ad popup
            loadRewarded(activity)
            android.app.AlertDialog.Builder(activity)
                .setTitle("Simulated Ad")
                .setMessage("Watching a simulated ad to earn 5 points...\n(Test Ad Failed to Load)")
                .setPositiveButton("Claim Reward") { _, _ ->
                    PointsManager.addPoints(5)
                    onRewardEarned()
                    onAdDismissed()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    onAdDismissed()
                }
                .setCancelable(false)
                .show()
        }
    }
}
