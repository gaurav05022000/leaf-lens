package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private const val TAG = "AnalyticsHelper"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
            Log.d(TAG, "Firebase Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Analytics", e)
        }
    }

    fun setUserId(userId: String?) {
        try {
            firebaseAnalytics?.setUserId(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting user ID: ${e.message}")
        }
    }

    fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics?.setUserProperty(name, value)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting user property $name: ${e.message}")
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d(TAG, "Logged event: $eventName, params: $params")
        } catch (e: Exception) {
            Log.w(TAG, "Error logging event $eventName: ${e.message}")
        }
    }

    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logPlantScanned(diseaseOrHealth: String, confidence: Float) {
        val bundle = Bundle().apply {
            putString("plant_health_status", diseaseOrHealth)
            putDouble("confidence_score", confidence.toDouble())
        }
        logEvent("plant_scanned", bundle)
    }

    fun logSubscriptionEvent(action: String, plan: String? = null) {
        val bundle = Bundle().apply {
            putString("action", action)
            plan?.let { putString("plan", it) }
        }
        logEvent("subscription_interaction", bundle)
    }
}
