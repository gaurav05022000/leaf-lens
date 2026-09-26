import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_block = """        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {
                com.example.ui.AdManager.loadInterstitial(this)
                com.example.ui.AdManager.loadRewarded(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }"""
        
new_block = """        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {
                runOnUiThread {
                    com.example.ui.AdManager.loadInterstitial(this)
                    com.example.ui.AdManager.loadRewarded(this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
    print("Patched AdMob init")
else:
    print("Pattern not found in MainActivity for AdMob")
