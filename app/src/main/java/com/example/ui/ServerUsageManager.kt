package com.example.ui

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ServerUsageManager {
    private const val TAG = "ServerUsageManager"
    const val MAX_FREE_DAILY_SCANS = 3
    const val FREE_TRIAL_DURATION_DAYS = 3

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * Verifies if the user is currently eligible to scan:
     * 1. Premium subscribers: always allowed (unlimited scans).
     * 2. Free Trial users: allowed up to 3 scans per day while the 3-day trial is active.
     * 3. Expired trial / Non-subscribers: strictly denied.
     *
     * IMPORTANT: This method only CHECKS eligibility. It does NOT decrement or consume
     * the user's scan count. The count is only reduced after a scan succeeds.
     */
    suspend fun canScan(): Boolean {
        if (SubscriptionManager.isPremium.value) {
            return true
        }

        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = auth?.currentUser ?: return false
        val uid = user.uid
        val db = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
            ?: return SubscriptionManager.canScan()

        val today = getTodayDateString()
        val userDocRef = db.collection("users").document(uid)
        val usageDocRef = userDocRef.collection("usage").document(today)

        return try {
            val userSnapshot = userDocRef.get().await()
            val trialStart = if (userSnapshot.exists()) {
                userSnapshot.getLong("trialStartTime")
            } else {
                null
            }

            val effectiveTrialStart = trialStart ?: user.metadata?.creationTimestamp ?: System.currentTimeMillis()
            val elapsedMs = System.currentTimeMillis() - effectiveTrialStart
            val trialDurationMs = FREE_TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L

            // Deny if 3-day free trial duration has expired
            if (elapsedMs >= trialDurationMs) {
                return false
            }

            // Persist trialStartTime in Firestore if not yet set
            if (trialStart == null) {
                userDocRef.set(
                    mapOf(
                        "trialStartTime" to effectiveTrialStart,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }

            val usageSnapshot = usageDocRef.get().await()
            val currentCount = if (usageSnapshot.exists()) {
                usageSnapshot.getLong("scanCount")?.toInt() ?: 0
            } else {
                0
            }

            currentCount < MAX_FREE_DAILY_SCANS
        } catch (e: Exception) {
            Log.e(TAG, "Error checking scan eligibility in Firestore: ${e.message}")
            SubscriptionManager.canScan()
        }
    }

    /**
     * Atomically records a successful scan in Firestore and reduces the daily free trial scan count.
     * Must be called strictly AFTER the plant scan has succeeded and generated results.
     */
    suspend fun recordSuccessfulScan(expectedUid: String? = null) {
        if (SubscriptionManager.isPremium.value) {
            return
        }

        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val user = auth?.currentUser ?: return
        val uid = user.uid
        if (expectedUid != null && uid != expectedUid) {
            return
        }
        val db = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

        val today = getTodayDateString()

        if (db != null) {
            try {
                val usageDocRef = db.collection("users").document(uid).collection("usage").document(today)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(usageDocRef)
                    val currentCount = if (snapshot.exists()) {
                        snapshot.getLong("scanCount")?.toInt() ?: 0
                    } else {
                        0
                    }
                    val newCount = currentCount + 1
                    transaction.set(
                        usageDocRef,
                        mapOf(
                            "scanCount" to newCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }.await()
            } catch (e: Exception) {
                Log.e(TAG, "Error recording successful scan usage in Firestore: ${e.message}")
            }
        }

        // Reduce scan count in local SubscriptionManager reactively
        SubscriptionManager.consumeScan()
    }

    /**
     * Legacy method for backward compatibility.
     */
    suspend fun verifyAndConsumeScan(): Boolean {
        val eligible = canScan()
        if (eligible) {
            recordSuccessfulScan()
        }
        return eligible
    }
}
