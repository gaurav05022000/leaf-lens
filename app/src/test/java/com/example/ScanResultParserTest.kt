package com.example

import com.example.data.ScanResultParser
import com.example.data.isStatusHealthy
import com.example.data.careItems
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScanResultParserTest {
    @Test fun rejectsEmptyDiagnosis() {
        assertThrows(IllegalArgumentException::class.java) { ScanResultParser.parse("{}") }
    }

    @Test fun rejectsNonPlantImage() {
        assertThrows(IllegalArgumentException::class.java) {
            ScanResultParser.parse("""{"isPlant":false,"plantName":"Chair"}""")
        }
    }

    @Test fun clampsUnsafeScoresAndCareIntervals() {
        val plant = ScanResultParser.parse("""{"plantName":"Rose","healthScore":999,
            "sunlightScore":-5,"wateringScore":500,"wateringIntervalDays":-10,
            "fertilizingIntervalDays":2147483647,"disease":null}""")
        assertEquals(100, plant.healthScore)
        assertEquals(0, plant.sunlightScore)
        assertEquals(100, plant.wateringScore)
        assertEquals(1, plant.wateringIntervalDays)
        assertEquals(365, plant.fertilizingIntervalDays)
        assertNull(plant.disease)
    }

    @Test fun acceptsFencedJsonAndPreservesSteps() {
        val plant = ScanResultParser.parse("```json\n" +
            """{"plantName":"Rose","careTips":["Check soil","Give light"]}""" + "\n```")
        assertEquals("Rose", plant.name)
        assertEquals("Check soil\nGive light", plant.careTips)
    }

    @Test fun unhealthyMustNotSuppressDiseaseReminders() {
        assertFalse("Unhealthy".isStatusHealthy())
        assertFalse("Not healthy".isStatusHealthy())
        assertTrue(" Healthy ".isStatusHealthy())
        assertTrue("Good".isStatusHealthy())
    }

    @Test fun careListsPreserveCommasWithinNewStepsAndReadLegacyFormats() {
        assertEquals(listOf("Use moist, well-draining soil", "Give light"),
            "Use moist, well-draining soil\nGive light".careItems())
        assertEquals(listOf("Water", "Feed"), "Water|Feed".careItems())
        assertEquals(listOf("Water", "Feed"), "Water,Feed".careItems())
    }
}
