import re

with open('app/src/main/java/com/example/ui/AdManager.kt', 'r') as f:
    content = f.read()

old_block = """    fun loadInterstitial(activity: Activity) {
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
    }"""
    
new_block = """    fun loadInterstitial(activity: Activity) {
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
    }"""

content = content.replace(old_block, new_block)

old_block2 = """    fun loadRewarded(activity: Activity) {
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
    }"""

new_block2 = """    fun loadRewarded(activity: Activity) {
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
    }"""

content = content.replace(old_block2, new_block2)

with open('app/src/main/java/com/example/ui/AdManager.kt', 'w') as f:
    f.write(content)
print("Patched AdManager")
