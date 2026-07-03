package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.core.content.ContextCompat

class PlantNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val plantName = inputData.getString("plantName") ?: "your plant"
        val action = inputData.getString("action") ?: "water"
        val diseaseName = inputData.getString("disease") // may be null
        val notificationId = inputData.getInt("notificationId", System.currentTimeMillis().toInt())

        showNotification(plantName, action, diseaseName, notificationId)

        return Result.success()
    }

    private fun showNotification(plantName: String, action: String, diseaseName: String?, notificationId: Int) {
        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) ?: return

        val channelId = "plant_reminders_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Plant Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for watering, feeding, and checking disease status of your plants"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = when (action) {
            "water" -> "Time to water $plantName!"
            "feed" -> "Time to feed $plantName!"
            "checkup" -> "Checkup time for $plantName!"
            else -> "Reminder for $plantName!"
        }

        val text = when (action) {
            "water" -> "Your $plantName is thirsty. Please check its soil before watering."
            "feed" -> "Your $plantName needs feeding."
            "checkup" -> "Follow up on $plantName's treatment for ${diseaseName ?: "its condition"}. Are the care tips working?"
            else -> "Your plant needs attention."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon, better to use an app icon but we'll use android.R.drawable.ic_dialog_info for now
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
