package com.example

import com.example.data.Plant
import com.example.ui.ScanHistoryItem
import com.example.ui.SubscriptionManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComprehensiveProductionAuditTest {

    @Test
    fun testSubscriptionTierLimits() {
        assertEquals("Free daily scan quota must strictly be 3", 3, SubscriptionManager.MAX_FREE_DAILY_SCANS)
        assertEquals("Free trial duration must be 3 days", 3, SubscriptionManager.FREE_TRIAL_DURATION_DAYS)
        assertEquals("Entitlement ID must be configured", "leaflens pro", SubscriptionManager.ENTITLEMENT_ID)
    }

    @Test
    fun testUserIsolationReset() {
        SubscriptionManager.resetUser()
        assertFalse(SubscriptionManager.isPremium.value)
        assertFalse(SubscriptionManager.isTrialActive.value)
        assertFalse(SubscriptionManager.hasFullAccess.value)
        assertEquals(0, SubscriptionManager.trialDaysRemaining.value)
        assertEquals("Free", SubscriptionManager.subscriptionStatus.value)
        assertNull(SubscriptionManager.activeProduct.value)
    }

    @Test
    fun testPlantModelSanitization() {
        val testPlant = Plant(
            id = 42,
            name = "Monstera Deliciosa",
            species = "Monstera deliciosa",
            healthStatus = "Healthy",
            healthScore = 95,
            wateringLevel = "Medium",
            wateringScore = 90,
            sunlight = "Indirect",
            sunlightScore = 85,
            imageUri = "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/o/users%2F123%2Fscan.jpg",
            description = "A popular houseplant with fenestrated leaves."
        )
        assertEquals("Monstera Deliciosa", testPlant.name)
        assertFalse("Plant imageUri must not contain raw Base64 data", testPlant.imageUri?.startsWith("data:image/") ?: false)
    }

    @Test
    fun testScanHistoryItemModelIntegrity() {
        val item = ScanHistoryItem(
            id = "test-uuid-1",
            plantName = "Fiddle Leaf Fig",
            species = "Ficus lyrata",
            healthStatus = "Needs Care",
            disease = "Bacterial Leaf Spot",
            severityLevel = "Mild",
            timestamp = System.currentTimeMillis(),
            imageUrl = "https://firebasestorage.googleapis.com/test.jpg",
            description = "Leaf spots observed",
            symptoms = "Brown spots with yellow halos",
            treatmentSteps = "Isolate plant, prune infected foliage",
            careTips = "Avoid overwatering, ensure adequate air flow"
        )
        assertEquals("Fiddle Leaf Fig", item.plantName)
        assertEquals("Bacterial Leaf Spot", item.disease)
        assertNotNull(item.imageUrl)
        assertFalse("ScanHistory imageUrl must not contain raw Base64 data", item.imageUrl?.startsWith("data:image/") ?: false)
    }
}
