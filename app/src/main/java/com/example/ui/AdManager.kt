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
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1350477690991328/1140772950"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-1350477690991328/7447959765"

    fun loadInterstitial(activity: Activity) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        try {
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
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }


    fun canWatchRewardedAd(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences("flora_points_prefs", android.content.Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_ad_date", "")
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastDate != currentDate) {
            return true
        }
        val count = prefs.getInt("ad_watch_count", 0)
        return count < 2
    }

    fun recordRewardedAdWatched(activity: Activity) {
        val prefs = activity.getSharedPreferences("flora_points_prefs", android.content.Context.MODE_PRIVATE)
        val lastDate = prefs.getString("last_ad_date", "")
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (lastDate != currentDate) {
            prefs.edit()
                .putString("last_ad_date", currentDate)
                .putInt("ad_watch_count", 1)
                .apply()
        } else {
            val count = prefs.getInt("ad_watch_count", 0)
            prefs.edit().putInt("ad_watch_count", count + 1).apply()
        }
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
                recordRewardedAdWatched(activity)
                PointsManager.addPoints(5, "Watched Ad")
                onRewardEarned()
            }
        } else {
            // Ad not ready - simulate ad popup
            android.app.AlertDialog.Builder(activity)
                .setTitle("Ad Not Ready")
                .setMessage("No video ad is currently available. Please try again in a moment.")
                .setPositiveButton("OK") { _, _ ->
                    onAdDismissed()
                }
                .setCancelable(false)
                .show()
        }
    }
}
