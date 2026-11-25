package com.example

import com.example.ui.SubscriptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionSecurityAndAuditTest {

    @Test
    fun testDefaultScanLimits() {
        assertEquals(3, SubscriptionManager.MAX_FREE_DAILY_SCANS)
        assertEquals(3, SubscriptionManager.FREE_TRIAL_DURATION_DAYS)
    }

    @Test
    fun testUserIsolationOnReset() {
        SubscriptionManager.resetUser()
        assertFalse(SubscriptionManager.isPremium.value)
        assertFalse(SubscriptionManager.isTrialActive.value)
        assertFalse(SubscriptionManager.hasFullAccess.value)
        assertEquals(0, SubscriptionManager.trialDaysRemaining.value)
    }
}
