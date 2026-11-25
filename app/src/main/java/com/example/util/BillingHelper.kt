package com.example.util

import android.content.Context
import android.content.Intent

object BillingHelper {
    fun isBillingServiceAvailable(context: Context): Boolean {
        return try {
            val intent = Intent("com.android.vending.billing.InAppBillingService.BIND")
            intent.setPackage("com.android.vending")
            val resolveInfo = context.packageManager.queryIntentServices(intent, 0)
            resolveInfo.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
