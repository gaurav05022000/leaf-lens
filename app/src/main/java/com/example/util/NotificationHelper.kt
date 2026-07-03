package com.example.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.Plant
import com.example.worker.PlantNotificationWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

object NotificationHelper {

    fun schedulePlantReminders(context: Context, plant: Plant) {
        val workManager = WorkManager.getInstance(context)
        val timeNow = System.currentTimeMillis()

        // 1. Water Reminder
        val waterWorkName = "water_${plant.id}"
        if (plant.nextWateringTimeMs > timeNow) {
            val waterDelay = plant.nextWateringTimeMs - timeNow
            val waterData = Data.Builder()
                .putString("plantName", plant.name)
                .putString("action", "water")
                .putInt("notificationId", (plant.id * 10) + 1)
                .build()

            val waterWorkRequest = OneTimeWorkRequestBuilder<PlantNotificationWorker>()
                .setInitialDelay(waterDelay, TimeUnit.MILLISECONDS)
                .setInputData(waterData)
                .build()

            workManager.enqueueUniqueWork(
                waterWorkName,
                ExistingWorkPolicy.REPLACE,
                waterWorkRequest
            )
        } else {
            workManager.cancelUniqueWork(waterWorkName)
        }

        // 2. Feed Reminder (if applicable, though maybe we just check if nextFeedingTimeMs > 0)
        val feedWorkName = "feed_${plant.id}"
        if (plant.nextFeedingTimeMs > timeNow) {
            val feedDelay = plant.nextFeedingTimeMs - timeNow
            val feedData = Data.Builder()
                .putString("plantName", plant.name)
                .putString("action", "feed")
                .putInt("notificationId", (plant.id * 10) + 2)
                .build()

            val feedWorkRequest = OneTimeWorkRequestBuilder<PlantNotificationWorker>()
                .setInitialDelay(feedDelay, TimeUnit.MILLISECONDS)
                .setInputData(feedData)
                .build()

            workManager.enqueueUniqueWork(
                feedWorkName,
                ExistingWorkPolicy.REPLACE,
                feedWorkRequest
            )
        } else {
            workManager.cancelUniqueWork(feedWorkName)
        }

        // 3. Daily Disease Checkup Reminder
        val diseaseWorkName = "disease_${plant.id}"
        if (plant.disease != null && plant.disease.isNotBlank() && plant.disease != "null" && plant.healthStatus != "Healthy") {
            val diseaseData = Data.Builder()
                .putString("plantName", plant.name)
                .putString("action", "checkup")
                .putString("disease", plant.disease)
                .putInt("notificationId", (plant.id * 10) + 3)
                .build()

            val diseaseWorkRequest = PeriodicWorkRequestBuilder<PlantNotificationWorker>(1, TimeUnit.DAYS)
                .setInputData(diseaseData)
                .build()

            workManager.enqueueUniquePeriodicWork(
                diseaseWorkName,
                ExistingPeriodicWorkPolicy.KEEP,
                diseaseWorkRequest
            )
        } else {
            workManager.cancelUniqueWork(diseaseWorkName)
        }
    }

    fun cancelPlantReminders(context: Context, plantId: Int) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("water_$plantId")
        workManager.cancelUniqueWork("feed_$plantId")
        workManager.cancelUniqueWork("disease_$plantId")
    }
}
